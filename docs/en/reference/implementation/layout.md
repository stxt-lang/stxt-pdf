# The layout

How the engine computes the position and the size of each box (packages `layout` and `fragment`). The behavior is in the
pages of common behavior and of each component; here, how it is built.

## `Engine` and `LayoutContext`

`Engine` dispatches by `Style.layout` to the corresponding algorithm (an `EnumMap`, and the table, which is a grid, has its own).
The algorithms do not know the engine: they depend on **`LayoutContext`** (`layout`, `intrinsic`, `contentIntrinsic`,
`measurer`, `natural`, `imageRatio`, `arranged`), which `Engine` implements and which a test can replace with a double. **Every recursive call for
layout or intrinsic sizes goes through `context.layout(...)` / `context.intrinsic(...)`**, never directly to the
algorithm, so that the `LayoutCache` is used (without it, nested `Stack` and `Grid` grow exponentially).

`Engine.layOutDocument` is the entry point: it validates the fonts, the images and the hyphenation languages, paginates
with the `Paginator`, checks the
overflows with `Overflow` and the minimum module of the codes with `ModuleSizes`, and returns a `LaidOutDocument`. When it finishes (also if it fails) it releases everything it memoizes.
The protocol with the `Paginator` (`reservePageNumbers`, `assume`, `headings`, `number`, `overlays()`) is package-private.
What that protocol sets lives in two objects of the `Engine` that `InlineLayout` receives in its constructor and only
reads: `TextValues` (the probe number of `{page}` and `{pages}`, the assumed pages, the numbers and the running heads)
and `LineBreakingMemo` (the memoized line breaking). The algorithms keep no state of the document.

> **Decision:** the layout algorithms depend on the interface `LayoutContext` and not on `Engine`. The pagination
> protocol stays in `Engine`, package-private, because moving it to another class did not remove any dependency; what it
> sets goes in `TextValues` and `LineBreakingMemo`, which the engine injects into `InlineLayout`, so the algorithm is
> tested with references without a whole `Engine`. *Rejected alternative:* `InlineLayout` with that state as fields,
> which the engine had to know by its concrete type inside the map of algorithms.

> **Decision:** the pagination protocol is not public: whoever drove an `Engine` from outside could break the
> numbering of another document. *Rejected alternative:* a session object per document.

> **Decision:** `layOutDocument` releases the cache, the line breaking and the assumed pages when it finishes, so a stored
> engine does not retain the tree of the last document and a reused one gives the same as a new one. It costs nothing measurable: the
> cache is only useful within one document.

## `LayoutAlgorithm`

It has two methods: `contentIntrinsic(box)`, the narrowest and natural widths of the content (without border, padding, margin
or `Width`, and independent of the space), and `layout(box, space, from)`. Those of the margin box are given by
`context.intrinsic(box)` with `BoxModel.intrinsic`, and those of the content by `context.contentIntrinsic(box)` (the automatic minimum of a `Stack` item).

## `ConstraintSpace` and `LayoutResult`

**`ConstraintSpace`** is the immutable input of the layout and part of the cache key: available width, height (empty =
indefinite), sizes imposed by the parent, percentage bases, remaining space on the page from the top edge of
the box (empty = no fragmentation), whether the box starts the page and whether the height of its content is measured ignoring its `Height`
(`measuringContentHeight()`).

**`LayoutResult`**: the `Fragment`, the `BreakToken` (`null` = the box has finished), the quality of the break
(from `BreakAppeal.PERFECT_BREAK` to `FORCED`), the `PendingOverlay` that go up to their reference container (with the static
position relative to the content box of whoever returns the result) and the `AdjoiningMargins`. The parent places a result
with `placedAt(x, y)` (or with the offset of a `Frame`), which gives a `LayoutResult.Placement`: the moved fragment and its
`PendingOverlay` translated to its own content box. `BlockLayout` and `UnitBreaker` use it.

## `BoxModel`

Margin, border, padding, width of the border box and height if it is definite, from the `ConstraintSpace`. It is the common part of
all the algorithms. `childSpace()` gives the space of the in-flow children, `exclusionsInContent(space, first)` moves the
exclusions of the space to the content box of the fragment and `BoxModel.intrinsic(...)` goes from the intrinsic
sizes of the content to those of the margin box. `auto` margins are 0: centering is done by the parent, which is the one that
places the child. The sizing of an image is in `BoxModel.replaced`, so that the probes of `Stack`, `Grid` and the positioned elements measure
the image correctly: `BoxModel.resolve(box, space, context)` is always used, which passes it the natural size. The maximum is applied first
and then the minimum. If they clash, the minimum wins (`Sizing.constrain`).

What decides the size of an item that its container places in a space of its own is in `ItemSizing`, once: the width on
the cross axis (the declared one, the one of the space minus the margins with `stretch` or the fit to the content, limited
by `Min width` and `Max width`), the stretched width and height, the item's own alignment or the container's, and the
out-of-flow children that wait at the corner of the content box. `StackLayout` and `GridLayout` use it, and `OverlayLayout`
for the height stretched between top and bottom. The rules are the same; the base of the percentages is not: each one
resolves the `BoxModel` of the item with its own ([Values](../common/values.md#definite-height)), the content box of the
`Stack`, the area of the grid item or the reference container of the `Overlay`.

## Margin merging

Each box returns in `AdjoiningMargins` the margins of its descendants that come out at the top or at the bottom (and whether it is empty and
lets its own through). The parent merges them with the child's own margins, which it resolves itself. `MarginStrut` is immutable and
keeps the largest positive and the most negative separately. `Box.isolatesMargins()` says which boxes cut it off. The
account of the stacking (the cursor, the margins waiting below it, those that come out of the box at the top and the
horizontal placement of a child with its side margins) is kept by `Stacking`, which `BlockLayout` uses with the children
of a block and `FootnoteLayout` with the notes of the footnote area, so that a rule of merging or of centering is the
same in both.

The margins that come out of a child at the top (the top margin of its first descendant, when it is larger than those
around it) move it down from where its own margin would put it. `BlockLayout` **predicts them before laying it out** with
`LeadingMargins`, a pure function of the tree: it walks the chain of first in-flow descendants while each box is open at
the top (block flow, without border or padding at the top, not isolating margins) and accumulates their top margins, plus
the two margins of the empty children that precede each of them, resolving the percentages against the content width of
each level, and skips the side boxes and the out-of-flow ones. It is a prediction: a text counts as non-empty (whether
it has lines is only known once laid out) and a block as empty only when it has no in-flow children. The child is placed
where the prediction says, and what decides is the `AdjoiningMargins` its layout returns: if they match, one layout; if
they differ and move it, what is left of the page for it and what is beside it change, and it is laid out again at its
position only if the result can change: if it moves up (it has more room), if what it placed does not fit in what is left
from there or if there are side boxes beside it (`Run.holdsAt`). If it moves down, fits and has nothing beside it, the
first layout holds, even if the child was broken: with less room nothing new fits and what fitted still fits, so the
algorithms take the same decisions. A prediction that overshoots (a negative margin that does not come out in the end)
does not break before the child: if there is no room where it puts it, the child starts from its own margin, as before.
Until 0.9.0 the child was laid out at the position of its own margin and always repeated when moved; afterwards, only
when the first layout did not hold lower down, which left two layouts per chapter longer than the page on the page where
it starts.

> **Decision:** the predicted margins are a prediction with a safety net and do not replace the ones the layout returns:
> a text is only known to be empty once laid out (a running head or a reference can leave it without lines), so a
> replacement could not be exact without laying out. *Rejected alternative:* replacing `adjoining.top()` with the
> prediction and removing `holdsAt`, which would change the output in those cases.

## Grid

`TrackSizing.size` shares the free space among the tracks according to a `Distribution`: `EQUAL`, equally among the
`auto` ones, in a `Grid`, and `CONTENT`, in proportion to the content of each column, in a `Table` (`GridLayout.arrange`
chooses it by the role of the box, [Table](../components/table.md)).

> **Decision:** a stretched grid item is always laid out again with its height fixed, even if it measures the same as when
> it was measured, so that its children have the definite base that the definite height rule promises. It costs one more layout per stretched
> item. *Rejected alternative:* repeating it only if some descendant uses vertical percentages.

**`Vertical` of a `Cell`** ([Table](../components/table.md#vertical-alignment-of-the-content-vertical)). The cell is a
stretched item like any other, and where its content goes is decided by its own algorithm: `BlockLayout` (the cell with
elements) and `InlineLayout` (the cell with a value) ask `Slice.contentOffset` how far down the content goes in the
fragment they have just computed, and shift the child fragments with their pending `Overlay` boxes, or the lines.
`Slice.contentOffset` gives what is left over in the content box of the fragment, or half of it, and 0 if the box does
not end in it. `GridLayout` and `UnitBreaker` know nothing about it: in a split row, the fragment the cell ends in is the
one `UnitBreaker` lays out with the height of the row on that page (`ConstraintSpace.withFixedHeight`).

> **Decision:** the shift is applied by the layout of the cell to its last fragment, and not by the grid when it places
> it. It works the same without fragmenting, in a split row and in a cell with `Height`, and the cache keeps the fragment
> already placed. *Rejected alternative:* a step of the grid that moved the children of each cell after stretching it,
> which would have to be repeated on each path of `UnitBreaker`.

## Positioned boxes

The algorithms do not lay out the `Overlay` and `Page overlay` children. They return them as `PendingOverlay` with their static position, and
the `Frame` children are shifted with `FrameOffset` after placing them. `Engine.layout` resolves with `OverlayLayout`
(on top of `OverlayEquation`) the `Overlay` whose reference container is the newly laid-out box (their fragments hang
from that box's fragment). The `Page overlay` go up to the root and the `Paginator` places them in `Page.pageOverlays` of each page, only on the
pages included by their `Pages`.

## Rotated boxes in flow

`Engine.layout` lays out separately a box with `Style.rotatesInFlow()` (a `Block`, `Text`, `Paragraph`, `Image` or `Barcode` with
`Rotate`): with `ConstraintSpace.fixed` and the width from `turnedWidth` (its resolved `Width` or, with `auto`, its natural width plus
border and padding), without fragmentation and without `from`. The result is wrapped with `Fragment.turned`: the fragment measures the
rectangle that encloses its border box rotated about its center, and `turned` keeps that box unrotated, centered in it. The
algorithms of the parents know nothing about the rotation: they see a box the size of the rectangle, which does not split (`BEFORE` if it does not
fit and is not at the start of the page) and whose inner margins do not come out (`AdjoiningMargins.NONE`: it isolates
margins). `Engine.intrinsic` does the same with the empty percentage base: its narrowest width and its
natural width are the width of the rectangle plus the margins. It is the reference container of its `Overlay`
(`Box.referenceContainer`), which are resolved on the unrotated box before wrapping it.

## Fragments

A **`Fragment`** has its position relative to the **content** box of the parent. `StxtDump` converts it to absolute page
coordinates. It carries its own `border` and `padding`, which in a split fragment are not those of the box: those of the
fragment are always used to reach its content box. It also carries the used `margin` (with the percentage base of its layout) and the
`Frame` offset already added to its position (`offsetX`/`offsetY`), which `Overflow` needs. The geometry only
exists in the fragments: `Box` stores neither position nor size.

In a box rotated in flow, `x`, `y`, `width` and `height` are those of the rectangle it occupies, and `turned`, its unrotated border box
relative to it. Its children and its lines refer to the unrotated box: whoever goes down to them uses
`Fragment.borderBox(x, y)`, `contentWidth()` and `contentHeight()`, which take it into account, and never `width` and `height`.

> **Decision:** the used geometry only exists in the fragments: a box is laid out many times (probes, measurements,
> stretching, each page) and the cache returns results without writing it again, so "the last layout" of a box would not
> correspond to any fragment. The used margin goes in the fragment, with the percentage base of the layout that
> produced it. *Rejected alternative:* having `Overflow` resolve the margins with the content width of the parent, which is not the
> base of grid items.

> **Decision:** `Overflow` checks a `Frame` in place with the offset the layout used, stored in the
> fragment, instead of recomputing it: in a parent split across pages the base of a vertical percentage is not the height of the
> fragment, and recomputing it rejected valid documents.

## Fragmentation

Each algorithm places what fits in `fragmentainerRemaining` and returns the break with its quality. The parent compares it with the
breaks between its children and keeps the **last one of highest quality** (`BreakRules.best`). Relaxing the rules in the order of
[Breaks](../common/breaks.md#choosing-the-break) comes from that comparison alone: that is why a child that cannot respect widows and
orphans still returns a break (`VIOLATES_WIDOWS`) instead of `BEFORE`.

**`BreakToken`** (sealed, records with `equals`): `Block`, `Line`, `Container`, with the height already consumed by the box (a
definite `Height` is the total of all the fragments, and a broken fragment only consumes what its content occupies), and
`BEFORE`: "nothing fits, break before me", which is never passed as `from`. The `Paginator` detects the lack of progress
by comparing tokens. The token of the root does not accumulate height: each page has the whole page area.

`Slice` computes the height, the border and the padding of each fragment (`Edges: open` and `closed`), and `Slice.contentRemaining`
gives what is left for the content. A fragment of a box with a definite height that does not end reaches the end of the
page but only consumes what its content takes up, and the same holds upwards: `LayoutResult.occupied(from)` gives what a
fragment takes up for its parent (what its token consumed in that fragment, not its height), and `BlockLayout` (in the
`Mark` of the broken child), `UnitBreaker` (in `Split`) and `ColumnsLayout` (in `Fill`: the tallest of its columns) pass
it to `Slice.of` as `occupied`, apart from the visible height of the content. Stack and grid break between units (lines, items or rows, which are `fragment.Unit`) with `UnitBreaker`: they are
laid out whole without fragmenting (`UnitBreaker.Arranged`: placed items and `Units`) and `UnitBreaker.fragment` keeps what
fits on the page. That unfragmented arrangement (the placement, the tracks, the items laid out at their sizes, the bands
and, in a table, the header, the footer and the lines of `Merge borders`) depends on the box and on its space without
fragmentation (`ConstraintSpace.unfragmented`, which keeps `atFragmentainerStart`), not on what is left of the page or on
the token: it is computed once per container and space (`LayoutContext.arranged`, one more memo of the `LayoutCache`, by
box, space and type of what is memoized, which `clear` and `Engine.forget` empty like the rest) and each fragment only
breaks it. What is memoized is immutable (`Arranged` and `Repeat` copy their lists). Until 0.9.0 every fragment of a table
placed and sized the whole table again, and the units of a grid were computed by going through all the items for each row
(now, one pass over the items); `ArrangementCacheTest` counts the arrangements. A unit that does not fit in what is left of the page splits its items in parallel on that page
(`Units.split`, from `Split rows` and `Split items`, and only where `Units.splittable` allows it, which the lines of a
horizontal `Stack` with `Wrap items` do not; with `false`, only the one that does not fit even at the start of a page),
repeating their layout with `ConstraintSpace.fragmented` and the space stored in `UnitBreaker.Placed`;
`UnitBreaker.splitOnThisPage` compares the quality of that break with that of the best break between units
(`BreakRules.cutUnits`) and keeps the last of the best ones, and the `BreakToken.Container` carries the token of each item. With `atFragmentainerStart` something has to be placed even if it overflows (progress guarantee). Tables repeat
header and footer with `UnitBreaker.Repeat`. The bands of the rows (`Box.rows()`, `Element.ROW`) are `UnitBreaker.Band`:
`GridLayout.arrange` creates them with the width of the columns, `TableLayout` places those of the header and the footer in the
`Repeat` and leaves those of the body in their unit, and `UnitBreaker` puts them in each fragment before the items (whole,
cut with `splitBands` if their group splits, or that of the last unit in the continuation of the group). They are fragments
without children: painting treats them like any in-flow box. With `Merge borders`
([Table](../components/table.md#merged-borders)), `UnitBreaker` also records what each fragment shows of each unit
(`UnitBreaker.UnitPiece`: whole, trimmed or the continuation of its group, like the bands) and passes it with its children
to `Repeat.rules`; `TableLayout` adds the rows of the header and the footer and `layout.table.MergedBorders` computes the
lines of that fragment over the columns of `GridLayout.arrangeWithColumns`, which go in `Fragment.rules`. Forced breaks are `PAGE` or `COLUMN`
(`BreakRules.forcedBetween` returns the type). The page break has the quality `BreakAppeal.FORCED_PAGE`, above `PERFECT_BREAK`.

> **Decision:** the memo of the arrangement lives in `LayoutContext` and `LayoutCache`, not inside each algorithm: so it
> is emptied with the cache (`clear` when finishing and `Engine.forget` when paginating again, which reaches the container
> as an ancestor of the text that changes) and the algorithms stay stateless. The type is part of the key because the
> table memoizes its preparation (body, header and footer) on top of the arrangement of its grid, both for the same box.
> *Rejected alternative:* a field in each algorithm, which `Engine.forget` would not reach and a reused `Engine` would
> carry along.

The content of a `Columns` goes in a generated box, the column flow (`Element.COLUMN_FLOW`), its only child: each
column is a fragment of that box, laid out with the usual algorithms. The columns are only at the start of the
fragment container (`atFragmentainerStart`) if the `Columns` is, and the breaks between columns do not count for the
parent: the page break has the quality of that of the last column.

> **Decision:** the column flow is a generated box. *Rejected alternative:* having the `Columns` stack the children itself,
> repeating the block flow.

`ColumnsLayout` lays out the column flow once per column with `context.layout`, with the height of the column as
`fragmentainerRemaining` and the token of the previous column as `from`, and each column is one of its fragments. Balancing is
a binary search for the minimum height, with a precision of 0.001 pt, which rejects the heights with breaks between columns worse
than those of the in-order distribution.

## Text

`InlineLayout` breaks the text into lines with `LineBreaker` (greedy or Knuth-Plass) or, with `Preformatted: keep` or `wrap`, with `PreformattedText`.
The line breaking goes in four pieces of `layout.inline`: `Words` splits the text into words with their break points,
`Paragraph` measures them and gives the positions where a line may end, `LineBreaker` chooses among them with one of the
two algorithms and `LinePlacer` places each line in the content box (alignment, `justify` and the measures of the runs).
`PreformattedText` only uses `Words.RunCursor` and the measures of `LinePlacer`.
The text of a box is always a single paragraph (blank lines are an error when reading it, except in a preformatted one,
where they are content): `BreakRules.cutLines` applies widows and orphans to all its lines and only the last one is not justified.
`InlineLayout.runs` replaces the references with their value (the one the `Paginator` left in `TextValues`) and applies `TextTransform` to the runs **before** breaking them into lines,
with the `Case` and the `Small caps` of each styled run (`TextFormat.textCase`, `smallCaps`). `LinePlacer` measures each
character at the size of its run (`TextFormat.scale()`: 0.8 in a small cap and 0.7 in a superscript or a subscript)
and with its letter spacing (`TextFormat.letterSpacing`, in `advance`) and takes from a line that of its
last character (`trailing`, `Paragraph.width`), which is that of its run. A run that is an image (`TextFormat.image`,
with the text `TextRun.IMAGE`, U+FFFC) measures in `advance` what `TextMeasurer.imageWidth` gives: as tall as the size of
the line and as wide as its proportion, which `TextValues.WithImages` reads from the file through
`LayoutContext.imageRatio` (so the image measures what it is also with `FixedMetrics`), and `PageNumbers.probing`
delegates. `Images.validate` checks the images of the runs beforehand like those of the `Image` boxes (format, limits),
with the line of the text.
`TextLine.runs` keeps each placed run, with the `justify` spacing included. `MarkerLayout` places the marker of a
`List item` in its first fragment, with the baseline of its first line (`TextMeasurer.vertical`); an image marker is as
tall as `Height` or the `Size` of the item and as wide as its proportion gives (`LayoutContext.natural`), carries the
space that separates it from the item as the right padding of the fragment, and is centered on the capitals
(`VerticalMetrics.capHeight`) of the typography of the item on that baseline.

**Side boxes** ([Block](../components/block.md#side-boxes-side)). `BlockLayout` places a child with `Side` whole, at its
side and at the position of the flow, without moving the cursor, and records it as a `Beside` (the band it takes up and
what it takes from each side, margins included). To each child that runs beside the boxes (`Box.wrapsBesideSideBoxes`:
texts that are not preformatted, `Block`, headings, `Markdown`, lists and list items without background or border that
are not side boxes and have no `Below side boxes`, position, `Rotate` or `Overflow: clip`; lists, only beside boxes on the right) it
passes in the `ConstraintSpace` the **exclusions** (`model.Exclusion`: band and what it
takes from each side, measured from the border box of the child), which are part of the key of the cache; the other
children go down to the bottom of the side boxes. A `Block` that receives exclusions treats them as inherited side boxes
for its own children, without counting them for its height. `BlockLayout` and `InlineLayout` move them to the content box
with `BoxModel.exclusionsInContent` and
`LineWidths` gives each line its line box (where it starts and how wide it is, with `varying()` as the first line with
the whole width); `LineBreaker` breaks with the width of each line, the greedy algorithm line by line and Knuth-Plass
with the line number in the state of the dynamic programming (up to `varying()`), and a word that does not fit alone in
a short line leaves it empty (`Line.isEmpty`) and goes to the next one. Each `Line` carries the position of the text it
starts at (`start`), and the `BreakToken.Line` carries the exclusions and the starting position of the line breaking
the fragment was placed with: the next fragment, if either has exclusions, breaks the rest of the text again from the
position of its first line with its own (usually none: the side boxes end with the page), and the memoized line
breaking is keyed by exclusions and position. The widows of a text cut beside a box are counted with the lines the
rest will take at the whole width. `TextLine.left` and `right` keep the line box to place the line again when the
pages are numbered.

**Drop cap** ([Text](../components/text.md#drop-cap-drop-cap)). `InlineLayout` takes the first letter out of the runs of
a `Paragraph` with `Drop cap` in every fragment (so the positions of the text do not change), sets it at the size the
cap height of the font gives (`VerticalMetrics.capHeight`, from the `OS/2` table, or 0.7 without it) and places it as the
first `TextLine` of the first fragment, with its baseline on that of line `N − 1`; the first `N` lines get its width as
one more exclusion (`LineWidths.withDropCap`), the paragraph is at least `N` lines tall and `cutLines` gets
`Orphans` ≥ `N`. The rest of the text that is broken again from a later position, beside a side box, has no drop
cap and no width taken from it. `TextPainter` does not write the invisible space after the line of the drop cap, so that the text
that is read is still one word.

**First line indent** ([Typography](../common/typography.md#first-line-indent-first-line-indent)). `InlineLayout` gives
it to `LineWidths` (`withFirstLineIndent`) only when the text is broken from its start (position 0): the rest of a
paragraph that is broken again beside a side box does not have it, and the one that goes on in another page with
the lines it already had starts at a later line. It is added to `left(0)`, which with a negative indent is
negative, and `varying()` is at least 1, so both algorithms treat it as one more line of another width.
`widerLater` does not count it: the word that does not fit in the indented line overflows instead of leaving it
empty. Nor does `shortened()`, which is what the widow count looks at to know whether the rest of the text will be
broken again. In a preformatted text, `PreformattedText` breaks the first line of `wrap` at what the indent leaves.
`LineBreaker.intrinsics` and `PreformattedText.intrinsics` add it to the first line. `Overflow` lets a line start
before the content box as far as its `TextLine.left`, if it is negative, and checks in each fragment with text that
a negative indent fits in its left padding (`FIRST_LINE_INDENT_OVERFLOW`): there the padding is in points, whether
the document wrote it in points or as a percentage. A paragraph with `Drop cap` arrives with no indent:
`ModelValidator.validateDropCap` leaves at 0 the one it inherits or gets from a rule and rejects the one it writes.

**Hyphenation.** With `Hyphenate: true`, `InlineLayout` gives `LineBreaker` the `Hyphenator` of the language of the
box (`Hyphenations.forLocale`, package `layout.inline.hyphenation`): Liang's algorithm over a trie with the patterns
of the resource of the language, with a cache per word. `Words.words` gives each word its break points (those of
its soft hyphens, which it removes from the text, or those of the patterns over its letters, which are those of
`Hyphenator.isLetter`: the Unicode ones and, if the patterns have it, the apostrophe), and both line breakings
work over the **positions** of the text (`Paragraph`): the start of each word and each break point. A
line goes from one position to another; if it ends at a break point it carries the hyphen, measured with the format of
the previous letter, and the `Line` (and the placed `TextLine`) says so with `hyphenated`. The greedy breaking tries,
when a word does not fit, its break points from the longest to the shortest; Knuth-Plass adds to the demerits TeX's
penalty per hyphen (50²) and per two consecutive hyphens (10,000). Widths are still sums: those of the whole words,
the spaces and the parts of a split word, each one measured once. `Hyphenations.validate` checks before the layout,
like the fonts, that every box with text and `Hyphenate: true` is in a language with patterns.

**Written hyphens and long words.** A line that ends at a break point ends inside a word (`Paragraph.inside`) in
one of three ways, which `Paragraph` keeps per position: with the hyphen the line breaking adds (`hyphen`), after a
hyphen written in the word, or at a cut of last resort (`lastResort`). `Words` adds to the break points of each word
those of its written hyphens (`afterWrittenHyphens`, `Word.written`), with or without a `Hyphenator`: there the line
carries no other hyphen, and its width and the narrowest width are measured without one. With `Break long words`,
`InlineLayout` gives `Paragraph` the width of the content box, and `cutLongParts` adds a position between each two
characters (a character `BreakIterator`, which does not separate a letter from its marks) of each part that, as a
line of its own, does not fit in it; a whole word that fits is not even looked at. The first part of the first word
is measured against that width minus the positive `First line indent` of the box, wherever the text is broken from,
so that the positions are always the same. Both algorithms still choose
among positions, with one condition: a line ends at a cut of last resort only if it starts inside its long part, at
its start or at an earlier cut (`Paragraph.reaches`), and so the part starts a line of its own. That is `new-line`; with `same-line` the
condition does not apply. The greedy one tries those of the word that does not fit from the first one on and keeps the last
one that fits; it stops trying when the line exceeds the available width by more than the font size
(`hyphenMargin`: what an added hyphen can make an earlier position measure more than a later one), so a word of
thousands of characters costs, per line, what the line reaches. Knuth-Plass charges the penalty of a hyphen to
every line that ends inside a word, and that of two consecutive ones only to those that end with a hyphen, added or
written; a line that ends at a cut of last resort is discarded if the next position fits in it, and has no
badness. It looks at the width before checking whether the starting position is reachable, to stop as soon as the
line does not fit: in a word cut at any character almost no position is. `Paragraph.single` is a single piece
between two consecutive positions, which is the only thing that may overflow. The `Line` and the `TextLine` say
with `splitsWord` that the line ends inside a word, whether or not it carries the hyphen of `hyphenated`.

**Forced line breaks.** The break of a Markdown (`\` at the end of the line) reaches the text as the character U+2028
(`TextRun.LINE_SEPARATOR`), which `MarkdownBuilder` writes in its place. `Words.words` treats it as a space that
separates words and marks the word before it (`Word.forced`); the position where the next one starts is a mandatory end
(`Paragraph.forced`). The greedy breaking closes the line there, and Knuth-Plass lets no line go past it and gives no
badness to the one that ends at it, like the last one. The `Line` says so with `forced`, and `InlineLayout` does not
justify it. The natural width of the text is that of its widest line between breaks. In a preformatted text,
`PreformattedText.sourceLines` ends the line at it as at a line break, so no line carries it.

> **Decision:** the language is checked when the layout starts and not when the tree is built, like the fonts:
> `tree` does not depend on `layout`, where the patterns live. *Rejected alternative:* the patterns in `model`.

> **Decision:** the cuts of `Break long words` are positions of `Paragraph`, computed with the width of the box when
> the text is measured, and not a separate step after the line breaking: this way both algorithms, the side boxes
> and `Overflow: shrink` handle them without knowing about them. *Rejected alternative:* splitting afterwards the
> lines that overflow, which does not let Knuth-Plass count on the cut or the words after it go on in the line of the last piece.

> **Decision:** both line breakings share the positions and the sums of `Paragraph`, instead of a breaking over atoms
> (parts of words) with prefix sums: with ligatures a whole word does not measure what its parts add up to, and this
> way each line is measured as it is painted.

**Shrinking.** With `Overflow: shrink` ([Overflow](../common/overflow.md#shrinking-the-text-shrink)), `InlineLayout.shrink`
breaks the text at the `Size` of the box and, while some line measures more than the available width (with the tolerance
of `Overflow`, `Tolerance.MODEL`) or the lines add up to more than the height limit (`InlineLayout.limit`: the content box
of the definite `BoxModel.height()` or, without one, of the `maxHeight()`), breaks it again at a size 0.5 pt smaller, with
`Min size` as the last step. The limit is part of the key of the memorized line breaking (`BrokenKey`), because the same
box is measured without a height (`measuringContentHeight`) and laid out stretched afterwards. The lines keep the size they
were broken at (`TextLine.fontSize`, the one `LinePlacer.place`, `Engine.number` and the painting use), and the line
height comes from `TypographyStyle.usedLineHeight(size)`. The intrinsic sizes (`contentIntrinsic`) are measured at `Size`.

**Measurement.** A `TextMeasurer` has to be **additive between words** (the width of a text is the sum of those of its words
and spaces): both line breaking algorithms add up. `PdfMetrics` measures with `FontFile.width(text, size, ligatures,
kerning)`, which applies `GSUB` and the kerning pairs word by word if they are requested; a pair does not cross a space,
so the sum is still exact. The part of a split word is measured together with its hyphen, as it is painted: with
kerning, the last letter and the hyphen are a pair. `Engine.layOutDocument` first calls `validate(root)`, which in `PdfMetrics`
checks that the fonts exist and have all the characters. Of the white space, only what the line breaking turns into a
space or a line end is taken as painted as a space (`Words.isSpace`, and in a preformatted text,
`PreformattedText.isSpace`): any other Unicode space reaches the canvas as it is and is checked as one more character.

**Long text.** Each page lays out the text from a different line, so the `LayoutCache` does not help. `InlineLayout` memoizes
the line breaking per box and available width in `LineBreakingMemo`, which belongs to the `Engine` like the cache
(without that memory, the cost grows with the square of the pages, and `LongTextTest` watches it). Greedy line breaking adds up the advances of the words and spaces of the current line and only measures the whole
line when the sum is within `Tolerance.MODEL` of the available width.

> **Decision:** the width of a text is additive between words and greedy line breaking adds up, measuring the whole line only near
> the limit: the floating-point sum can differ by about 10⁻¹³ pt from the measurement of the line, and without that check some
> break right at the limit changed. This way the line breaking is exactly that of always measuring the line. *Rejected alternative:*
> trusting the sum (it changes breaks at the limit) or measuring in integer thousandths, which would tie `TextMeasurer` to the form of
> `FontFile`.

> **Decision:** the baseline of a list marker is computed at layout time with the metrics of the measurer (with
> `FixedMetrics`, the 0.8/0.2 approximation, and with `PdfMetrics`, the `hhea` of the font) and not when painting: the marker is a
> normal fragment, with its rectangle in the dump. The SVG always paints with the approximation, so a marker of a different
> size from its first line may end up slightly shifted in the SVG of a document laid out with the real
> metrics.

## Pagination

`Paginator` makes one flow pass per section (`flow`) with **a single convergence** of the references, and computes with
`Numbering` the printed number of each page (the counter, `restart` with its `First number` and `none`), the last one of its page sequence
(`{pages}`) and its `Page number format`, with which it writes them (`label`, `totalLabel`). `Page` carries its `PageSetup`, its
section, its `printed` (0 = no number) and its style. The `PageSetup` of a page is that of its section passed through
`forPage(number)`: with `Mirror margins`, on the even physical pages it carries the left and right margins swapped, and
since the page area measures the same on both sides, the flow is laid out once and the painting, the dump and
`Overflow` know nothing about sides. A `Page overlay` of the document is placed with `PendingOverlay(box, 0, 0)`
on each page of each section that inherits it, and `Pages.includes` is evaluated with the pages in its scope.

**Blank pages.** After the flow, `blankAfter` looks at which sections leave the next one, with `Begin on: odd`, on an
even physical page. Those end with a blank page: `Numbering` counts it as one more page of the section, `pagesOf`
reserves its physical number for it and `pages` adds it as a `Page` with `blank`, an empty fragment of the box of the section and
without `Page overlay`. It is not part of the flow of the section, so the `Pages` of its `Page overlay` does not see it.

**Markers that are text.** A `{{page}}` or a `\{page}` of Markdown
([References](../common/references.md#references-as-text)) reaches the tree without its pair of braces and with
`Reference.LITERAL_BRACE` (a noncharacter, U+FDD3) in place of its brace: `ElementReader` puts it in the literal texts
(`Reference.literal`) and `RunMarks` and `MarkdownBuilder` in those of Markdown, run by run. With that character no
step takes it for a marker: neither `Reference`, nor `PageNumbers`, nor a running head or a table of contents that copy
the text of a heading. `PdfMetrics` measures it and checks its glyph as a brace. When `Engine.layOutDocument` finishes
paginating and nothing is replaced any more, `LiteralBraces.shown` redoes the lines that carry it with the brace,
without changing measurements, so the laid out pages and what is painted do not have it; the text of the boxes
(`Box.text()`) does, and whoever reads it to write it (the bookmarks of the PDF, the replacement text, the `Alt` of a
link) passes it through `Reference.shown`.

**`{page}` and `{pages}`.** `InlineLayout` measures the markers as a trial number (`00` at first, the measurer of
`TextValues`) and the lines keep them. When pagination finishes, the `Paginator` calls `Engine.number`
(`PageNumbers.number`, with the real measurer), which redoes only those lines with the values of each page already
written. If some printed number does not fit in the trial one (`Numbering.probe`:
as many characters as the longest, with `m`, `M` or `W` if there are letters or Roman numerals), pagination is done again with one that covers it
(`PageNumbers.cover`), which only grows.

**References.** `Engine.assume` sets the assumed pages in `TextValues` (the first time, `00`), pagination is done, the page on which the
first fragment of each referenced box (`Box.key()`) has ended up is checked, and pagination is done again if any differs, up to 10 passes that
change something. If they do not converge, it is `REFERENCE_UNSTABLE`. The passes caused by the digits of `{pages}` growing do not count, because
they only grow. With an additive measurer pagination always converges: the error is a safety net.

> **Decision:** `{page of: id}` is replaced before breaking the text into lines and not in a reserved slot like `{page}`:
> the marker has spaces and line breaking would split it into three words, and with the convergence a second
> step is not needed. Since the first pass assumes `00`, with references to pages there are always at least two. *Rejected alternative:*
> keeping the last pass without warning.

> **Decision:** an `Id` that does not exist (`REFERENCE_UNKNOWN`) and the page of a box of a `Page overlay`
> (`REFERENCE_NOT_PLACEABLE`) are detected when building the tree (`References`), because they depend only on its structure. That
> of a box that falls on a page without a number is detected when paginating. `{number of: id}` comes from the tree and does not depend on pagination.

> **Decision:** with `{pages}` and 100 pages or more, or with numbers in letters or in Roman numerals, pagination is done again reserving the width of the
> longest number, instead of letting the real number stick out of the reserved slot. In Roman numerals an `M` is reserved per
> letter, the widest of those that write a Roman numeral; in uppercase letters, a `W`, and in lowercase, in letters and in
> Roman numerals, an `m`. *Rejected alternative:* measuring each real number on its line, which
> would change the line breaks from one page to another.

**Paginating again does not empty the cache.** When the trial number grows (`Engine.reservePageNumbers`) or
the page of a reference changes (`Engine.assume`), the `Paginator` passes the texts whose width changes and the engine forgets only what
the `LayoutCache` and the line breaking (`LineBreakingMemo`) store about them and their ancestors (`Engine.forget`), also the arrangement of a
`Stack`, a `Grid` or a `Table` that contains them. The cache and the line breaking store everything by box first (a map
per box with the rest of the key), so forgetting costs what the forgotten boxes cost and not what is stored: a
`{heading 1}` in a `Page overlay` forgets its text on every page where the chapter changes. `PaginationCacheTest`,
`ArrangementCacheTest` and `LayoutCacheTest` watch it.

> **Decision:** paginating again only forgets what depends on the texts that change, because the result of a box
> depends only on its subtree and on the key (box, space and token): the algorithms read nothing from outside, except the style,
> which does not change. If a new algorithm read something from outside the subtree, it would have to go into the key. A document of 100
> pages went from costing twice as much as one of 99 to costing the same. *Rejected alternative:* emptying the whole cache.

**Footnotes.** `FootnoteLayout` lays out each page of a section with footnotes ([Footnote](../components/footnote.md)):
the flow with `ConstraintSpace.forPage(pageSetup, reserved)`, which takes from the root the height reserved at the
bottom, and the notes of the calls that fall in its lines (`TextFormat.footnote` in the runs of the `TextLine`s; those of a note
already placed on a previous page do not count, because the note goes with its first call) stacked
in the footnote area of the section (`Section.footnotes()`, the generated box `Footnote area`, a child of the section out of
its flow). Since the body is a function of the reservation and the notes depend on the body, the reservation is found
by probes: no reservation; if the notes do not fit, the full height of those notes (in which those of any larger
reservation fit, because less room is less flow), unless it leaves the body no room (it is placed by force by the
progress guarantee and overflows: then, the largest reservation that does not force it, by bisection); and from one that
fits, down to what its notes need and, where a smaller one brings back a call that no longer fits, by bisection between
the two (`PRECISION`, 0.001 pt). Notes that do not fit even with the whole reservation are `FOOTNOTE_TOO_TALL`, and a
reservation that does not settle in 60 probes (`MAX_PROBES`) is an `InternalException`. Each note is laid out with
`context.layout` like any block, so the cache answers the
repeated probes (`FootnoteLayout` gets the `LayoutContext`, not the engine), and they are stacked with `Stacking` like
the children of a block: a note moved down by the margins that come out of it at the top is laid out again with what
is left for it; only the last one may be left broken, with a break without violations (`BreakAppeal.PERFECT_BREAK`)
and only if the flow continues, and its rest (`FootnoteLayout.Carry`) heads the area of the next page. The area
measures what its notes measure and is anchored at the end of the page area: the surplus of the reservation stays
between the text and the rule. `Paginator.flow` keeps the fragment of the area of each page (`Flow.footnotes`), which
goes to `Page.footnotes`, numbered with `{page}` like the flow, and `pagesOf` walks it too: a note has an `Id` and can be
referred to.

> **Decision:** the reservation is found by probes over the whole page, with the layout algorithms untouched, instead
> of reserving line by line inside the line breaking, as TeX does with its insertions: the algorithms read nothing
> outside their subtree and the cache stays valid. The price is a few more probes per page with notes (three in the
> usual case, up to about twenty when a call falls right at the limit). *Rejected alternative:* a footnote budget in
> `ConstraintSpace`, which enters the cache key and changes every algorithm.

> **Decision:** a note is broken only if its break violates neither widows, orphans nor `Keep`, and only if the flow
> goes on to another page; otherwise the line that calls it moves whole to the next page. *Rejected alternative:*
> breaking it by force, which leaves a call with a one-line note, or a page with only the rest of the note.

**Running heads.** `RunningHeads` walks the pages in physical order and keeps the heading of each level in effect: the
last one that has started (the `Fragment` with `first()` of a box with `headingLevel() > 0`, with the text of
`Box.headingText()`, which already carries the number), until one of a higher level starts, which empties those below
it. For each page it gives the value of each reference of the document: `"2"`, the heading in effect, and `"2: first"`,
the first of that level that starts on the page or, if none starts, the same as `"2"`; `pages` passes it to
the engine (`Engine.headings`) before placing the `Page overlay` boxes of the page, only when it differs from that of the
previous one; the engine keeps it in `TextValues` like the assumed pages and forgets what the cache and the line
breaking hold of the texts that carry a running head and of their ancestors, so those `Page overlay` boxes are laid out
again on each page that changes, and the rest comes from the cache. The flow is already paginated and is not touched: a
`Page overlay` takes up no space.

> **Decision:** the running head is replaced in the text before breaking it into lines, like `{page of: id}`, and not in
> a reserved slot like `{page}`: its width changes from one page to the next and there is no trial number that covers
> it. The price is laying out the `Page overlay` once per page instead of once, only if it carries one. *Rejected
> alternative:* reserving the width of the longest heading, which would leave gaps or overflow.

## Overflows

`Overflow` walks the paginated result and points to the innermost culprit ([Overflow](../common/overflow.md)),
using the `PageSetup` of each page. What a box with `Overflow: clip` contains is not checked against it (neither its
lines nor its children) and, with the `clipped` flag that goes down through its descendants, not against the page either;
each box inside is still checked against its own parent. What is rotated is checked as it is painted, by the rectangle
that encloses it, with the rotations of its ancestors in a `Turns` ([Painting](painting.md#rotation)). `Engine.layout` lays out a box with `Overflow: clip` like a
rotated one: without fragmentation, and `BEFORE` if it does not fit in what is left of the page. Where a text could
shrink, `textOverflow` and `tooTall` say so, and where it already shrinks and does not fit at `Min size`, too. There are two tolerances, with names: `Tolerance.EPSILON` (10⁻⁹), that of the arithmetic of the
layout, and `Tolerance.MODEL` (0.01 pt), how far a box can stick out without overflowing and the precision of the dumps. Comparisons
use `Tolerance.fits(size, space)` and `Tolerance.same(a, b)`, never a `1e-9` of their own.

> **Decision:** overflows are looked for on the paginated result and not in each algorithm when placing: this way the
> measuring passes, in which a box is laid out with a provisional space, do not count. Only two are detected at layout time,
> because without room there is no progress: `TABLE_REPEAT_OVERFLOW` and the column breaks in excess of a `Columns`.

> **Decision:** two named tolerances and not one: with `EPSILON` in `Overflow` the engine would be stricter and a box that
> sticks out 0.005 pt would be an error. *Rejected alternative:* a single tolerance, with that change of behavior.

After `Overflow`, and unless whoever converts removes `Check.BARCODE_SIZE`, `ModuleSizes` goes through the same pages
(flow, footnote area and `Page overlay` boxes) and, in each fragment with `Box.modules()`, compares the content box with
`Modules.minimumSize()`, the size at which the modules measure `Modules.minModule()`, with the tolerance `Tolerance.MODEL`:
the width and the height in a matrix, and only the width in a stretched row (`Modules.fit()`). The first one that falls
short is `BARCODE_TOO_SMALL`, with the line of its box ([Barcode](../components/barcode.md#minimum-module)). The generator
puts the minimum in `Modules`, so `ModuleSizes` knows nothing about formats.

> **Decision:** the minimum module is checked on the paginated result, like the overflows, and not when measuring the
> image (`Images.natural`), because a code is laid out with provisional sizes when a `Stack` or a `Grid` is measured.
> *Rejected alternative:* checking it in `ImageLayout`, which would give errors from measuring passes.

## Threads

An `Engine` is **not** shared across threads (layout cache, line breaking and assumed pages without synchronization): one per
conversion, which is what `Converter` does. A stored `Engine` can be reused in sequence. See
[Using from Java](api.md#threads).
