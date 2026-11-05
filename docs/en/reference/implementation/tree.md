# The box tree

How the document that has been read becomes a tree of `Box` (package `tree`). What each rule says is in the behavior
pages; here, where it lives in the code.

## The nodes of `stxt-core`

The engine works directly with `dev.stxt.Node`, `InlineNode` (inline value and children) and `TextNode` (`>>` block);
`getText()` gives the content in both cases. Names are compared by `getCanonicalName()`: lowercase and the spaces
turned into hyphens (`Min width` → `min-width`). Syntax and schema errors are `ParseException` from the
library, with a code (such as `INVALID_VALUE` or `CHILD_NOT_DECLARED`) and a line, which `DocumentReader` and `DocumentSchema` turn
into `DocumentSyntaxException` ([Using from Java](api.md#errors)).

## Validating against the schema

The structure is only in the schema: `DocumentSchema` compiles it once, `DocumentReader` validates with it when reading and
`BoxTreeBuilder` validates the tree again when it starts, so the rest of the builder takes the structure as correct.

In the schema:

- the container elements and the groups are `GROUP` (no value);
- `Text`, `Paragraph`, `Cell`, `Image`, `Barcode`, `Area`, `Caption`, `Marker image` and the rules of `Styles`, `INLINE` with
  children;
- `Content`, `BLOCK`, and `Areas`, `TEXT`;
- the switches (such as `Isolate margins`, `Wrap`, `Row header`, `In table of contents`, `Merge borders` or `Hyphenate`),
  `BOOLEAN`;
- the integers (such as `Layer`, `Widows`, `Orphans`, `Start`, `Number`, `Count`, `Weight`, `Drop cap` or `Quiet zone`),
  `INTEGER`, and `Rotate`, `NUMBER`;
- those with closed values (such as `Fit`, `Leader`, `Pages` or `Paper`), `ENUM`;
- the rest, `INLINE` without children, whose form `ValueReader` checks.

What the schema cannot express is checked by `BoxTreeBuilder`: that `Page` declares its size in a single way, that
`Item`, `Area` and `Figure` have exactly one element and a `List item` at least one (the schema counts by type), that a `Text`
has its text in one place and only one, that the root is `Document`, that the `Text` of a heading has no `Break`, the
hierarchy and the place of the headings and, in a `Table`, the order `Header rows`, `Body`, `Footer rows`, that a `Cell`
does not have both a value and elements and that each row fills its columns. What a `Markdown` generates goes through the schema
like what the author writes.

> **Decision:** what the schema already rejects has no code of the engine's own: `Direction: row`, `Before: avoid` or a
> `>>` block where children go are schema errors, with their code from `stxt-core`.

> **Decision:** `BoxTreeBuilder` validates against the schema when it starts, even if the document comes from `DocumentReader`: a tree
> built without validation (`STXT.rawParser()`) gives the same errors as the reader, with the codes of `stxt-core`, instead of
> misinterpreting a property out of place. The cost is validating twice what comes from the reader. *Rejected
> alternative:* a copy of the children table in the builder, or checking nothing without validation.

## `BoxTreeBuilder`

It translates each element into a `Box` (`Style.layout` and `Style.position` come from the element, and the properties of `Item` and
`Area` pass to the element they wrap), interprets values, applies inheritance and checks the rules that the schema cannot
express. The errors are `InvalidModelException` with an `ErrorCode` and the **line of the offending node**: that of the property
(`Style.line(HEIGHT)`, with the keys of `PropertyKey`) and not that of its box. `PropertyKey.internal` gives the internal name of
the short names (`Size` → `font-size`), which is the key of `Style.line`. The line is a `SourceLine`: its number and whether it belongs
to the separate styles document, because a property that comes from one of its rules has the line of that document
(`StyleRules` marks the `Style` while reading it). It is written in the messages as is (`line 8 of the styles`), and the error
built with it also states it in `inStyles()`.

`BoxTreeBuilder` is the orchestrator and keeps no state: what belongs to each call goes in a `BuildContext`. `BoxTreeBuilder.element`
is the order common to all elements (the place, inheritance, the rules, the properties and their checks, the box and
the content), and what is specific to each one is done by its family, `ElementReader` (an enum: `TEXT`, `CODE_BLOCK`, `IMAGE`,
`BARCODE`, `CELL`, `TABLE`, `GRID`, `STACK`, `LIST`, `COLUMNS`, `MARKDOWN`, `TABLE_OF_CONTENTS`, `FOOTNOTE`, `FIGURE` and `CONTAINER`,
chosen with `ElementReader.of`): the layout of the
box, what its wrapper gives it, the properties of its type, what it checks before creating the box, and its content. A
new element with nothing of its own is a `CONTAINER`, and one with something of its own, a new family, without touching `element`. The
`FIGURE` family builds the element and the `Caption` of a `Figure`, orders them by `Caption position`, numbers it
(`Figures`) and keeps it from being split. Each family claims the children that are its own (`ElementReader.property`): the
properties of its type, which it reads, and those its content reads, such as the row groups and `Column align` of a
`Table`, the `Span` of a `Cell` or the markers and `Table columns` of a `Markdown`. The orchestrator only sets aside those
of any element (`Item`, `Area`, `Content` and `Style`).

Inside which boxes each content is built (a `Page overlay`, a heading, a `Columns`, a `Markdown`, a `Footnote` or a box
that is not split) is said by `BuildContext.Scope`, a `record` that only changes while the content of a box is built
(`BuildContext.within`) and is restored afterwards. Why a box is not split across pages is said by a single
classification, `Style.unsplittable()` (the rotation, `Overflow: clip`, `Side` or the position, in that order): it is used
by `ModelValidator`, to reject `Keep together` and `Edges` on it, and by the `Scope`, to name it in
`BREAK_IN_UNBREAKABLE`.

The pieces:

| Class | What it does |
|---|---|
| `PageSetupReader` | `Page` (with the inheritance by groups of the sections), `Title`, `Language` and `Subject` |
| `SectionReader` | The properties of a `Section` (`Id`, `Page`, the page and footnote numbering, `Inherit page overlays`, `Begin on`) and their rules; the box, the footnote area and `SECTION_EMPTY` stay in `BoxTreeBuilder.section` |
| `ValueReader` | Interprets the values of each property and checks their forms |
| `PropertyReader` | The groups, the properties of `Stack`, `Grid` and `Item` and those specific to the elements, with `Map<String, Property>` tables |
| `StyleRules` | The rules of `Styles` from the two origins and their priority |
| `NamedColors` | The named colors (`Named color`) of the two origins, read before the rules and replaced by their color when each property is read (`COLOR_UNKNOWN`, `COLOR_DUPLICATE`) |
| `TableBuilder`, `CellPosition`, `CellSelector` | Tables: the placement of the cells, their position for the rules and the box of each `Row` with `Id` or `Box` (`Element.ROW`, in `Box.rows()` of the table: it has the table as parent, but is not its child) |
| `TableBorders` | The merged borders of a table with `Merge borders`: it keeps the border each cell declares (`Box.mergedBorder()`, with `model.Stroke`) and changes its `Border` into the room it leaves for the lines |
| `ListBuilder`, `ListMarkers` | Lists: the numbering and the box of the marker |
| `MarkdownBuilder`, `RunMarks` | Translates a `Markdown` (and text with `Markup: markdown`) into STXT nodes; `RunMarks` marks its styled runs first |
| `TableOfContentsBuilder` | The entries of the tables of contents (of headings, figures or tables), at the end of the construction |
| `HeadingNumbers` | The numbers of the headings, and the writing of the number before the title or the caption |
| `Figures` | The figures: the two counters, whether a `Figure` is numbered, where its caption goes, which counter it counts in and its `Alt` |
| `References`, `LinkTargets` | Check the references and the `#id` links of the whole tree |
| `Footnotes` | The footnotes: the area of each section, the calls `[^id]` numbered as they are read and, at the end, that each call has its note and each note its call, and the number before the first text of each note |
| `ModelValidator` | The rules that look at the box already read (such as margins, minimums and maximums, definite height or rotation) |
| `Nodes` | Access to the nodes and the errors |

No method in `tree` exceeds 65 lines or a complexity of 20.

> **Decision:** the element families are an enum with default methods and not an interface with one class per family:
> there are few of them, they are closed (the schema gives them) and they share almost everything. *Rejected alternative:* an interface with one class
> per family, or the families as private methods of the builder.

## `Box` and `Style`

`Box` has private fields, is read with methods (such as `text()`, `children()` or `formattedRuns()`) and is written with
package-private `set...` methods. `Style` is split into one group for each group of the document (`TypographyStyle`, `BoxStyle`, `BreakStyle`)
and for each element type with properties of its own (`StackStyle`, `GridStyle`, `ItemStyle`, `ListStyle`, `ColumnsStyle`,
`ImageStyle`, `FigureStyle`, `TableOfContentsStyle`). `Style` keeps the properties that define the element and the lines. Outside `tree` it is
read through the group: `style.typography().fontSize()`, `style.stack().direction()`, `style.box().margin()`. Only
`TypographyStyle` is inherited: `Style.inheriting` copies the group of the parent. A new property goes in the group of its place in the
document, with its reading and its `set...`.

`BoxTreeBuilder.build` closes the tree when it finishes (`Box.close`, which also closes the style): the children move to an immutable
list, those in flow are computed once, and writing to a closed box or style is an
`IllegalStateException`. A test in another package that needs a hand-made style uses `StyleEditor` (in the
`tree` package of the tests). Equality of `Box` is by identity: it is the key of the layout cache.

> **Decision:** the box tree is closed when it is built and only `tree` writes it: this way "the layout does not write to the tree"
> stops being a convention and a laid-out document can be read from several threads. *Rejected alternative:* accessors
> without closing, or a separate `StyleBuilder`.

> **Decision:** the groups of `Style` are classes that are closed with the box and not `record`: the builder writes the properties
> one at a time (the groups, then the rules by priority and then what belongs to the element), and with `record` each write
> rebuilds a record of up to 19 components. *Rejected alternative:* immutable `record` per group.

> **Decision:** the enums of the values are in `model`, one per file, because several groups use them and `model` cannot
> depend on `tree`. The constants are named like the value in the document (`TextAlign.JUSTIFY` ↔ `justify`), because
> they are read with `Enum.valueOf` on the text in uppercase and with the hyphen as an underscore.

> **Decision:** `Box.describe()` is the only way to name a box in the messages (`main (line 12)`), and the dump
> writes the `Id`.

## The sections and the root

`BoxTreeBuilder.Result` gives the root of the document and the list of `Section` (`tree/Section`: box, `PageSetup`, `Page numbering`,
`Page number format`, `Start`, `Inherit page overlays`, `Begin on`, `Footnote numbering`, `Footnote number format` and the
footnote area, `footnotes()`). The **box of each section is a child of the root of the document** (`Element.SECTION`, added after
the `Page overlay` elements of the document so that they precede it in preorder traversal) and is the **root of its layout**
(`Box.isLayoutRoot()`): the traversals of the whole tree (references, links, tables of contents, fonts, images, `Id`, PDF
structure) start from the root and see a uniform tree where every child has its parent in `parent()`, while the layout, margin
isolation and the reference container of `Overlay` boxes stop at `isLayoutRoot()`. The `Page overlay` children of `Document` are
not laid out in the flow either: the `Paginator` places them.

> **Decision:** the sections are children of the root of the document (`section.parent() == root`) and `Box.isLayoutRoot()`
> identifies the root of each layout, so the invariant "my child's parent is me" holds across the whole tree without the layout
> or the `Overlay` boxes leaving their section. *Rejected alternative:* hanging the sections from `root.children()` with
> `parent == null`, which broke the invariant at the first level under `Document` and required special-casing `SECTION` when
> walking up `parent()`.

## Text and runs

`Box.text()` is always the whole text and `Box.formattedRuns()` (`null` without formatting) splits it into `TextRun` with a
`TextFormat`. `Box.runs()` gives a single run if there is no formatting. The layout tests put runs in the tree before
closing it with `TextRuns`.

A **styled run** (`[x]{name}`, [Text formatting](../common/text-formatting.md#styled-runs)) comes out of
`MarkdownBuilder` with its `TextFormat.style()` half done, a `RunStyle` with only the name (`RunStyle.named`), because
`MarkdownBuilder` does not know the rules. `ElementReader.runStyles` completes it when the box is built, before the
`Code font` of the code: `StyleRules.runStyle` reads the `For style` rules of that name on a `Style` that inherits
nothing (so what is declared in it is what the rules declare, each property with its line), rejects what is not in
`StyleRules.RUN_PROPERTIES` (`STYLE_NOT_ALLOWED_IN_RUN`), and the `RunStyle` keeps what is declared (`null` the rest).
The family of `Font` also goes in `TextFormat.family`, like that of code. The box keeps the `Style` of each name
(`Box.runStyle`) for the errors that come later with the line of the rule: `FONT_UNKNOWN` in `PdfMetrics` and
`CONTRAST_LOW` in `ContrastCheck`. Outside `tree` nobody looks at the name: `TextFormat.face`, `color`,
`letterSpacing`, `textCase` and `smallCaps` give the value of the run or, if its style does not declare it, that of the
box that is passed to them.

> **Decision:** the text of the box is still a string and the runs are a partition of it: the lines of the document,
> the messages, the PDF bookmarks and `{page}` work on the whole text, and a text without formatting is measured and painted
> as before. *Rejected alternative:* storing only the runs and deriving the text.

## Generated elements

- **List marker:** `ListBuilder` creates the box of the marker in `Box.marker`, generated and outside `children`
  (`Element.MARKER`, `Box.isMarker()`): text with the typography of the item and the final space included (`"3. "`) or,
  with `Marker: image`, an image box (`Box.isImage()`, with the path in `Box.image()` and the `Alt` and `Height` of the
  `Marker image` in its `ListStyle`), with the line of the `Marker image` of the item or, if it has none, of the list.
  `PropertyReader.markerImage` reads the `Marker image` with its children, `ListBuilder.validateList` and
  `validateItem` check that it only goes with `Marker: image` (`LIST_IMAGE_NOT_ALLOWED`) and `ListBuilder.marker`, that with
  `Marker: image` there is an image (`LIST_IMAGE_MISSING`).
- **Column flow:** the content of a `Columns` goes in a generated block box (`Element.COLUMN_FLOW`), its only child.
- **Table of contents entries:** `TableOfContentsBuilder` generates at the end one `Stack` box per heading (`Element.TOC_1` to `Element.TOC_6`)
  or, with `Of: figures` or `tables`, per `Figure` of that counter (`Element.TOC_1`), with the title or the caption, the
  leader and the number. A heading or a figure without `Id` receives a generated name (`Box.anchor`), and `Box.key()`
  gives the `Id` or that name.
- **Footnote area:** `Footnotes.beginSection` creates in each section the `Footnotes` box (`Element.FOOTNOTES`,
  `Position.FOOTNOTES`, out of the flow: `Style.isOutOfFlow()`), a child of the section after its content, with the
  default look of the rule and the `For element: Footnotes` rules on top. Each `Footnote` written in the section
  (`buildContent`, `BoxTreeBuilder.footnote`) is built with the `FOOTNOTE` family of `ElementReader` and the area as
  parent, from which it inherits, and is a child of the area and not of the box where it is written. `BlockLayout`
  does not see it; `PdfStructure` places it after the text that calls it ([The PDF inside](pdf.md#structure-tree)).
- **Code block:** `MarkdownBuilder` marks the `Text` node it generates (`Translation.codeBlocks`) and
  `BoxTreeBuilder.element` gives it the element `Element.CODE_BLOCK`.

The elements a `For element` rule can name are `Element.selectable()`: the written ones and, of the generated ones, the
code blocks, the table of contents entries and the footnote area; `Element.isWritten()` tells the written ones apart.

## Generated images

A `Barcode` is an image that does not come from a file: the tree generates it from its value when it is built. The
`BARCODE` family of `ElementReader` checks its properties (`ModelValidator.validateBarcode`) and asks `Barcodes` for the
drawing, which encodes the data with ZXing 3.5.4 and returns a `Modules` (`model`): the matrix of dark and light modules,
with the quiet zone, the natural size of a module and the narrowest one that can be read (`Modules.minModule()`, which
`ModuleSizes` checks after layout). The box keeps it in `Box.modules()`; its element has
`Layout.IMAGE` and the role `FIGURE`, and `Box.isImage()` is true, so the layout sizes it with the natural size of the
modules (`Images.natural`) and the painting draws it without knowing which element it comes from (`Canvas.image`, which
looks at `Box.modules()`).

It is the **extension point** of the generated images: `ImageGenerator` (`tree`) receives the node and its style already
read and returns a `Modules`, and `Barcodes` is its first implementation. Another generator is another element with its
family in `ElementReader`, and nothing changes outside `tree`.

> **Decision:** the drawing is generated when the tree is built, in `tree`, which is where the node and its line are: the
> errors of the data (`BARCODE_DATA`) come out with the other model errors, before the layout, and the layout and the
> painting receive a ready matrix. ZXing is pure Java, like commonmark-java in `MarkdownBuilder`. *Rejected alternative:*
> generating it in `layout` or in `resources`, like the file images, which means carrying the format and the data through
> the tree to use them later and giving the line of the error from outside.

> **Decision:** the contract is a matrix of modules, without a list of shapes or a plugin system (`ServiceLoader`). It
> covers one- and two-dimensional codes, and it is extended the day something else is needed. *Rejected alternative:* a
> public interface for generators from outside the engine, which fixes an API before there is a second case.

## Markdown

> **Decision:** Markdown is translated into STXT nodes and not into boxes: what is generated is built like any other element, so
> the rules of `Styles`, validation, the tagged PDF and the bookmarks have no special case, and what is generated
> is also validated against the schema. *Rejected alternative:* building the boxes from the tree of the parser.

commonmark-java discards the extra cells of a table row, so `MarkdownBuilder` counts them in the text
of the row. Nor does it give the line of each cell, so the errors of a cell point to the line of its row.

`MarkdownBuilder` parses the `Content` with commonmark-java 0.30.1 (with the tables, strikethrough and underline extensions) and
translates it into STXT nodes (`InlineNode` with the line of its block), which it validates with the schema. The runs of each text go separately,
per node (`Translation.runs`, in `BuildContext.runs`). The `MARKDOWN` family of `ElementReader` builds those nodes with `element(...)` as
children of the box of the `Markdown`. Text with `Markup: markdown` goes through `MarkdownBuilder.inline`, the same parser and the
same translation in `inlineOnly` mode. Inside a `Markdown` it does not act (`BuildContext.Scope.inMarkdown`).

Before parsing, `RunMarks.mark` replaces each styled run with marks, Unicode noncharacters that commonmark-java takes
as letters: `RunMarks.OPEN`, the digits of the number of the name and `RunMarks.END` in place of the `[`, and
`RunMarks.CLOSE` in place of `]{name}`. It walks the text like inline Markdown (escapes, code between backticks, the target of a link, images and
blank lines) to leave out what is not text. `Translator.text` finds them in the `Text` nodes and gives the style to
everything between them, also to marks, links and code, and code, inline or in a block, gets its text back with
`RunMarks.unmark`.

> **Decision:** marks before the parser and not a `LinkProcessor` of commonmark-java, which treats the run as a link: it
> does not let a link contain it (`disallowPreviousLinks`) nor processes it if its text has brackets, and
> `[[x]{a}](target)` and `[[x](target)]{a}` would come out as text without any warning. *Rejected alternative:* a
> `LinkProcessor`, or recognizing the brackets in the `Text` nodes already parsed, where an escaped `\[` cannot be told
> apart from an unescaped one.
