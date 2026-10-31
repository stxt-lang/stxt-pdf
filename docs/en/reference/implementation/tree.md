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
- `Text`, `Paragraph`, `Cell`, `Image`, `Area` and the rules of `Styles`, `INLINE`;
- `Content`, `BLOCK`, and `Areas`, `TEXT`;
- `Isolate margins`, `Wrap`, `Row header` and `In table of contents`, `BOOLEAN`;
- `Layer`, `Widows`, `Orphans`, `Start`, `Number` and `Count`, `INTEGER`, and `Rotate`, `NUMBER`;
- those with closed values (such as `Fit`, `Leader`, `Pages` or `Paper`), `ENUM`;
- the rest, `INLINE` without children, whose form `ValueReader` checks.

What the schema cannot express is checked by `BoxTreeBuilder`: that `Page` declares its size in a single way, that
`Item` and `Area` have exactly one element and a `List item` at least one (the schema counts by type), that a `Text`
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
(`StyleRules` marks the `Style` while reading it). It is written in the messages as is (`línea 8 de los estilos`), and the error
built with it also states it in `inStyles()`.

`BoxTreeBuilder` is the orchestrator and keeps no state: what belongs to each call goes in a `BuildContext`. `BoxTreeBuilder.element`
is the order common to all elements (the place, inheritance, the rules, the properties and their checks, the box and
the content), and what is specific to each one is done by its family, `ElementReader` (an enum: `TEXT`, `IMAGE`, `CELL`, `TABLE`,
`GRID`, `STACK`, `LIST`, `COLUMNS`, `MARKDOWN`, `TABLE_OF_CONTENTS` and `CONTAINER`, chosen with `ElementReader.of`): the layout of the
box, what its wrapper gives it, the properties of its type, what it checks before creating the box, and its content. A
new element with nothing of its own is a `CONTAINER`, and one with something of its own, a new family, without touching `element`.

The pieces:

| Class | What it does |
|---|---|
| `PageSetupReader` | `Page` (with the inheritance by groups of the sections), `Title` and `Language` |
| `ValueReader` | Interprets the values of each property and checks their forms |
| `PropertyReader` | The groups, the properties of `Stack`, `Grid` and `Item` and those specific to the elements, with `Map<String, Property>` tables |
| `StyleRules` | The rules of `Styles` from the two origins and their priority |
| `TableBuilder`, `CellPosition`, `CellSelector` | Tables: the placement of the cells, their position for the rules and the box of each `Row` with `Id` or `Box` (`Element.ROW`, in `Box.rows()` of the table: it has the table as parent, but is not its child) |
| `ListBuilder`, `ListMarkers` | Lists: the numbering and the box of the marker |
| `MarkdownBuilder` | Translates a `Markdown` (and text with `Markup: markdown`) into STXT nodes |
| `TableOfContentsBuilder` | The entries of the tables of contents, at the end of the construction |
| `HeadingNumbers` | The numbers of the headings |
| `References`, `LinkTargets` | Check the references and the `#id` links of the whole tree |
| `ModelValidator` | The rules that look at the box already read (such as margins, minimums and maximums, definite height or rotation) |
| `Nodes` | Access to the nodes and the errors |

No method in `tree` exceeds 60 lines or a complexity of 20.

> **Decision:** the element families are an enum with default methods and not an interface with one class per family:
> there are few of them, they are closed (the schema gives them) and they share almost everything. *Rejected alternative:* an interface with one class
> per family, or the families as private methods of the builder.

## `Box` and `Style`

`Box` has private fields, is read with methods (such as `text()`, `children()` or `formattedRuns()`) and is written with
package-private `set...` methods. `Style` is split into one group for each group of the document (`TypographyStyle`, `BoxStyle`, `BreakStyle`)
and for each element type with properties of its own (`StackStyle`, `GridStyle`, `ItemStyle`, `ListStyle`, `ColumnsStyle`,
`ImageStyle`, `TableOfContentsStyle`). `Style` keeps the properties that define the element and the lines. Outside `tree` it is
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

> **Decision:** `Box.describe()` is the only way to name a box in the messages (`main (línea 12)`), and the dump
> writes the `Id`.

## The sections and the root

`BoxTreeBuilder.Result` gives the root of the document and the list of `Section` (`tree/Section`: box, `PageSetup`, `Page numbering`,
`Page number format`, `Start`, `Inherit page overlays` and `Begin on`). The **box of each section is the root of its layout** (`Element.SECTION`, a `Box`
**without `parent`**) and **hangs from `root.children()` without being a child** (`root.addSection`, not `addChild`): the traversals of the whole tree
(references, links, tables of contents, fonts, images, `Id`) start from the root and see them, and the layout, the `Page overlay` and going up through
`parent` stay inside the section. The `Page overlay` children of `Document` are also children of the root (with `parent`) and are not
laid out in the flow: the `Paginator` places them.

> **Decision:** a section is the root of its layout, without a parent, and hangs from the root of the document without being a child, so the
> layout, the cache and everything that looked at `parent == null` stay the same, and the traversals of the whole tree see it. What
> breaks is the invariant "my child's parent is me": it only holds for the sections. *Rejected alternative:* passing the
> list of sections to the traversals.

## Text and runs

`Box.text()` is always the whole text and `Box.formattedRuns()` (`null` without formatting) splits it into `TextRun` with a
`TextFormat`. `Box.runs()` gives a single run if there is no formatting. The layout tests put runs in the tree before
closing it with `TextRuns`.

> **Decision:** the text of the box is still a string and the runs are a partition of it: the lines of the document,
> the messages, the PDF bookmarks and `{page}` work on the whole text, and a text without formatting is measured and painted
> as before. *Rejected alternative:* storing only the runs and deriving the text.

## Generated elements

- **List marker:** `ListBuilder` creates the box of the marker in `Box.marker`, generated and outside `children`
  (`Element.MARKER`, `Box.isMarker()`): text with the typography of the item and the final space included (`"3. "`).
- **Column flow:** the content of a `Columns` goes in a generated block box (`Element.COLUMN_FLOW`), its only child.
- **Table of contents entries:** `TableOfContentsBuilder` generates at the end one `Stack` box per heading (`Element.TOC_1` to `Element.TOC_6`)
  with the title, the leader and the number. A heading without `Id` receives a generated name (`Box.anchor`), and `Box.key()` gives the
  `Id` or that name.
- **Code block:** `MarkdownBuilder` marks the `Text` node it generates (`Translation.codeBlocks`) and
  `BoxTreeBuilder.element` gives it the element `Element.CODE_BLOCK`.

The generated elements that are named in `Styles` without being written in the document are `Element.selectable()`.

## Markdown

> **Decision:** Markdown is translated into STXT nodes and not into boxes: what is generated is built like any other element, so
> the rules of `Styles`, validation, the tagged PDF and the bookmarks have no special case, and what is generated
> is also validated against the schema. *Rejected alternative:* building the boxes from the tree of the parser.

commonmark-java discards the extra cells of a table row, so `MarkdownBuilder` counts them in the text
of the row. Nor does it give the line of each cell, so the errors of a cell point to the line of its row.

`MarkdownBuilder` parses the `Content` with commonmark-java 0.30.1 (with the tables, strikethrough and underline extensions) and
translates it into STXT nodes (`InlineNode` with the line of its block), which it validates with the schema. The runs of each text go separately,
per node (`Translation.runs`, in `BuildContext.runs`). `BoxTreeBuilder.markdown` builds those nodes with `element(...)` as
children of the box of the `Markdown`. Text with `Markup: markdown` goes through `MarkdownBuilder.inline`, the same parser and the
same translation in `inlineOnly` mode. Inside a `Markdown` it does not act (`BuildContext.insideMarkdown`).
