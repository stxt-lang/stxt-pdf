# The layout

How the engine computes the position and the size of each box (packages `layout` and `fragment`). The behavior is in the
pages of common behavior and of each component; here, how it is built.

## `Engine` and `LayoutContext`

`Engine` dispatches by `Style.layout` to the corresponding algorithm (an `EnumMap`, and the table, which is a grid, has its own).
The algorithms do not know the engine: they depend on **`LayoutContext`** (`layout`, `intrinsic`, `contentIntrinsic`,
`measurer`, `natural`), which `Engine` implements and which a test can replace with a double. **Every recursive call for
layout or intrinsic sizes goes through `context.layout(...)` / `context.intrinsic(...)`**, never directly to the
algorithm, so that the `LayoutCache` is used (without it, nested `Stack` and `Grid` grow exponentially).

`Engine.layOutDocument` is the entry point: it validates the fonts and the images, paginates with the `Paginator`, checks the
overflows with `Overflow` and returns a `LaidOutDocument`. When it finishes (also if it fails) it releases everything it memoizes.
The protocol with the `Paginator` (`reservePageNumbers`, `assume`, `number`, `overlays()`) is package-private.

> **Decision:** the layout algorithms depend on the interface `LayoutContext` and not on `Engine`. The pagination
> protocol stays in `Engine`, package-private, because moving it to another class did not remove any dependency.

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
position relative to the content box of whoever returns the result) and the `AdjoiningMargins`.

## `BoxModel`

Margin, border, padding, width of the border box and height if it is definite, from the `ConstraintSpace`. It is the common part of
all the algorithms. `childSpace()` gives the space of the in-flow children and `BoxModel.intrinsic(...)` goes from the intrinsic
sizes of the content to those of the margin box. `auto` margins are 0: centering is done by the parent, which is the one that
places the child. The sizing of an image is in `BoxModel.replaced`, so that the probes of `Stack`, `Grid` and the positioned elements measure
the image correctly: `BoxModel.resolve(box, space, context)` is always used, which passes it the natural size. The maximum is applied first
and then the minimum. If they clash, the minimum wins (`Sizing.constrain`).

## Margin merging

Each box returns in `AdjoiningMargins` the margins of its descendants that come out at the top or at the bottom (and whether it is empty and
lets its own through). The parent merges them with the child's own margins, which it resolves itself. `MarginStrut` is immutable and
keeps the largest positive and the most negative separately. `Box.isolatesMargins()` says which boxes cut it off.

## Grid

> **Decision:** a stretched grid item is always laid out again with its height fixed, even if it measures the same as when
> it was measured, so that its children have the definite base that the definite height rule promises. It costs one more layout per stretched
> item. *Rejected alternative:* repeating it only if some descendant uses vertical percentages.

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
gives what is left for the content. Stack and grid break between units (lines, items or rows, which are `fragment.Unit`) with `UnitBreaker`: they are
laid out whole without fragmenting (`UnitBreaker.Arranged`: placed items and `Units`) and `UnitBreaker.fragment` keeps what
fits on the page. A unit that does not fit even at the start of a page splits its items in parallel, repeating their layout with
`ConstraintSpace.fragmented` and the space stored in `UnitBreaker.Placed`, and the `BreakToken.Container` carries the token of
each item. With `atFragmentainerStart` something has to be placed even if it overflows (progress guarantee). Tables repeat
header and footer with `UnitBreaker.Repeat`. The bands of the rows (`Box.rows()`, `Element.ROW`) are `UnitBreaker.Band`:
`GridLayout.arrange` creates them with the width of the columns, `TableLayout` places those of the header and the footer in the
`Repeat` and leaves those of the body in their unit, and `UnitBreaker` puts them in each fragment before the items (whole,
cut with `splitBands` if their group splits, or that of the last unit in the continuation of the group). They are fragments
without children: painting treats them like any in-flow box. Forced breaks are `PAGE` or `COLUMN`
(`BreakRules.forcedBetween` returns the type). The page break has the quality `BreakAppeal.FORCED_PAGE`, above `PERFECT_BREAK`.

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

`InlineLayout` breaks the text into lines with `LineBreaker` (greedy or Knuth-Plass) or, with `Preformatted: true` or `wrap`, with `PreformattedText`.
The text of a box is always a single paragraph (blank lines are an error when reading it, except in a preformatted one,
where they are content): `BreakRules.cutLines` applies widows and orphans to all its lines and only the last one is not justified.
`InlineLayout.runs` replaces the references with their value and applies `TextTransform` to the runs **before** breaking them into lines.
`TextLine.runs` keeps each placed run, with the `justify` spacing included. `MarkerLayout` places the marker of a
`List item` in its first fragment, with the baseline of its first line (`TextMeasurer.vertical`).

**Hyphenation.** With `Hyphenate: true`, `InlineLayout` gives `LineBreaker` the `Hyphenator` of the language of the
box (`Hyphenations.forLocale`, package `layout.inline.hyphenation`): Liang's algorithm over a trie with the patterns
of the resource of the language, with a cache per word. `LineBreaker.words` gives each word its break points (those of
its soft hyphens, which it removes from the text, or those of the patterns over its letters), and both line breakings
work over the **positions** of the text (`LineBreaker.Paragraph`): the start of each word and each break point. A
line goes from one position to another; if it ends at a break point it carries the hyphen, measured with the format of
the previous letter, and the `Line` (and the placed `TextLine`) says so with `hyphenated`. The greedy breaking tries,
when a word does not fit, its break points from the longest to the shortest; Knuth-Plass adds to the demerits TeX's
penalty per hyphen (50²) and per two consecutive hyphens (10,000). Widths are still sums: those of the whole words,
the spaces and the parts of a split word, each one measured once. `Hyphenations.validate` checks before the layout,
like the fonts, that every box with text and `Hyphenate: true` is in a language with patterns.

> **Decision:** the language is checked when the layout starts and not when the tree is built, like the fonts:
> `tree` does not depend on `layout`, where the patterns live. *Rejected alternative:* the patterns in `model`.

> **Decision:** both line breakings share the positions and the sums of `Paragraph`, instead of a breaking over atoms
> (parts of words) with prefix sums: with ligatures a whole word does not measure what its parts add up to, and this
> way each line is measured as it is painted.

**Shrinking.** With `Overflow: shrink` ([Overflow](../common/overflow.md#shrinking-the-text-shrink)), `InlineLayout.shrink`
breaks the text at the `Size` of the box and, while some line measures more than the available width (with the tolerance
of `Overflow`, `Tolerance.MODEL`) or the lines add up to more than the height limit (`InlineLayout.limit`: the content box
of the definite `BoxModel.height()` or, without one, of the `maxHeight()`), breaks it again at a size 0.5 pt smaller, with
`Min size` as the last step. The limit is part of the key of the memorized line breaking (`BrokenKey`), because the same
box is measured without a height (`measuringContentHeight`) and laid out stretched afterwards. The lines keep the size they
were broken at (`TextLine.fontSize`, the one `LineBreaker.place`, `Engine.number` and the painting use), and the line
height comes from `TypographyStyle.usedLineHeight(size)`. The intrinsic sizes (`contentIntrinsic`) are measured at `Size`.

**Measurement.** A `TextMeasurer` has to be **additive between words** (the width of a text is the sum of those of its words
and spaces): both line breaking algorithms add up. `PdfMetrics` measures with `FontFile.width(text, size, ligatures,
kerning)`, which applies `GSUB` and the kerning pairs word by word if they are requested; a pair does not cross a space,
so the sum is still exact. The part of a split word is measured together with its hyphen, as it is painted: with
kerning, the last letter and the hyphen are a pair. `Engine.layOutDocument` first calls `validate(root)`, which in `PdfMetrics`
checks that the fonts exist and have all the characters.

**Long text.** Each page lays out the text from a different line, so the `LayoutCache` does not help. `InlineLayout` memoizes
the line breaking per box and available width (without that memory, the cost grows with the square of the pages, and
`LongTextTest` watches it). Greedy line breaking adds up the advances of the words and spaces of the current line and only measures the whole
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
`Numbering` the printed number of each page (the counter, `restart` with its `Start` and `none`), the last one of its page sequence
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

**`{page}` and `{pages}`.** `InlineLayout` measures the markers as a trial number (`00` at first) and the lines
keep them. When pagination finishes, the `Paginator` calls `Engine.number`, which redoes only those lines with the
values of each page already written (`PageNumbers`). If some printed number does not fit in the trial one (`Numbering.probe`:
as many characters as the longest, with `M` or `m` if there are Roman numerals), pagination is done again with one that covers it
(`PageNumbers.cover`), which only grows.

**References.** `Engine.assume` sets the assumed pages (the first time, `00`), pagination is done, the page on which the
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

> **Decision:** with `{pages}` and 100 pages or more, or with Roman numerals, pagination is done again reserving the width of the
> longest number, instead of letting the real number stick out of the reserved slot. In Roman numerals an `M` is reserved per
> letter, the widest of those that write a Roman numeral. *Rejected alternative:* measuring each real number on its line, which
> would change the line breaks from one page to another.

**Paginating again does not empty the cache.** When the trial number grows (`Engine.reservePageNumbers`) or
the page of a reference changes (`Engine.assume`), the `Paginator` passes the texts whose width changes and the engine forgets only what
the `LayoutCache` and the line breaking store about them and their ancestors (`Engine.forget`).
`PaginationCacheTest` watches it.

> **Decision:** paginating again only forgets what depends on the texts that change, because the result of a box
> depends only on its subtree and on the key (box, space and token): the algorithms read nothing from outside, except the style,
> which does not change. If a new algorithm read something from outside the subtree, it would have to go into the key. A document of 100
> pages went from costing twice as much as one of 99 to costing the same. *Rejected alternative:* emptying the whole cache.

**Running heads.** `RunningHeads` walks the pages in physical order and keeps the heading of each level in effect: the
last one that has started (the `Fragment` with `first()` of a box with `headingLevel() > 0`, with the text of
`Box.headingText()`, which already carries the number), until one of a higher level starts, which empties those below
it. For each page it gives the value of each reference of the document (`"2"`, `"2: first"`), and `pages` passes it to
the engine (`Engine.headings`) before placing the `Page overlay` boxes of the page, only when it differs from that of the
previous one; the engine keeps it in `InlineLayout` like the assumed pages and forgets what the cache and the line
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
each box inside is still checked against its own parent. `Engine.layout` lays out a box with `Overflow: clip` like a
rotated one: without fragmentation, and `BEFORE` if it does not fit in what is left of the page. Where a text could
shrink, `textOverflow` and `tooTall` say so, and where it already shrinks and does not fit at `Min size`, too. There are two tolerances, with names: `Tolerance.EPSILON` (10⁻⁹), that of the arithmetic of the
layout, and `Tolerance.MODEL` (0.01 pt), how far a box can stick out without overflowing and the precision of the dumps. Comparisons
use `Tolerance.fits(size, space)` and `Tolerance.same(a, b)`, never a `1e-9` of their own.

> **Decision:** overflows are looked for on the paginated result and not in each algorithm when placing: this way the
> measuring passes, in which a box is laid out with a provisional space, do not count. Only two are detected at layout time,
> because without room there is no progress: `TABLE_REPEAT_OVERFLOW` and the column breaks in excess of a `Columns`.

> **Decision:** two named tolerances and not one: with `EPSILON` in `Overflow` the engine would be stricter and a box that
> sticks out 0.005 pt would be an error. *Rejected alternative:* a single tolerance, with that change of behavior.

## Threads

An `Engine` is **not** shared across threads (layout cache, line breaking and assumed pages without synchronization): one per
conversion, which is what `Converter` does. A stored `Engine` can be reused in sequence. See
[Using from Java](api.md#threads).
