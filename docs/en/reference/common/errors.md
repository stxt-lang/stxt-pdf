# Errors

The engine is **strict**: a document that asks for something that has no effect, or for two things that contradict each
other, is an error and not a case resolved with a default rule. This way the result is predictable: what is read in the
document is what is obtained.

- **Each error has a stable code** and a message that says what fails and how to fix it. Messages can improve, but codes
  do not change.
- **The line is that of the culprit node:** that of the property (and not that of its box or its group), that of the
  character inside a `Content >>` block or, if the family does not exist, that of the `Font` declared in an ancestor. If
  the problem is the combination of two properties, the message names the line of the other one. Messages with a line
  start with `line N:`.
- **It stops at the first error**: accumulating them would give cascading errors (a badly defined box causes others in its
  children).

## What has no effect

What an element declares and cannot have an effect because of how it is written is an error: `Line distribution` or a two-value `Gap`
without `Wrap`, `Rotate origin` without `Rotate`, `Separator color` without `Separator`, an `auto` margin that does not center, or a `Break` inside
an `Overlay`. These are not errors, on purpose:

- **Writing the default value:** `Span: 1 1`, `Isolate margins: false`, `Start: 1`. It contradicts nothing.
- **What is inherited:** a `Typography` property applies to the descendants, so it is not checked whether the box that
  declares it uses it (`Numbered` in a `Block`, `Small caps` with `Case: uppercase`).
- **The appearance that depends on another property:** `Border color` without `Border` and `Corner radius` without a
  background, border or image. They usually come from a style rule shared by boxes with and without a border.
- **A `Break` that comes from a rule** to an element inside a box that is not split: a rule by type
  (`For element: Paragraph`) would give an error in each paragraph of a footer ([Breaks](breaks.md#forced-breaks)).
- **What can come later:** a rule that does not apply to any element, and `Inherit page overlays: false` without a `Page overlay` in the
  document. A template can add them.
- **What depends on layout:** a minimum and a maximum in different units that clash (the minimum wins,
  [The box](box.md#minimums-and-maximums)) and `Balance: false` in a `Columns` with no height to fill.
- **The properties of a `Markdown` that its content does not use** (`Bullet markers` without lists): the content changes
  often ([Markdown](../components/markdown.md)).

## Validation levels

1. **STXT syntax and schema** ([Structure](structure.md#schema)): which children each element allows, how many, empty
   values, closed values, integers and booleans. This is where a property outside its element is detected (`Offset` in a
   `Block`, `Column sizes` in a `Stack`), with the STXT codes (`CHILD_NOT_DECLARED`, `TOO_MANY_CHILDREN`,
   `TOO_FEW_CHILDREN` or `INVALID_VALUE`, among others).
2. **Model:** the rules that the schema cannot express, with the codes below.
3. **Resources** ([Resources](resources.md)): fonts and images ([Image](../components/image.md)).
4. **Overflows** ([Overflow](overflow.md)): on the laid-out result.
5. **Contrast** ([The PDF](pdf.md#contrast)): on what is painted, when converting. The caller can turn it off.

## Codes

| Code | Rule | Message (example) |
|---|---|---|
| `DOCUMENT_ROOT` | A single root node, `Document`, with the namespace `dev.stxt.pdf` (the schema accepts any element as root); in a separate styles document, `Styles` | `The root node must be Document (dev.stxt.pdf):` |
| `PAGE_SIZE` | The size of `Page`, with `Paper` (and `Orientation`) or with `Width` and `Height`, never both; `Width` and `Height`, positive measurements; `Margin`, only measurements, and it leaves a page area; a `Section` that only rotates (`Orientation`) a document with paper ([Document](../components/document.md#page)) | `The margins of the page leave no page area` |
| `SECTION_EMPTY` | A `Section` has content: some element that is not a `Page overlay` (line of the section) | `The section has no content: without elements (Page overlays do not count...) it has no page` |
| `PAGE_NUMBER_UNAVAILABLE` | A text with `{page}` or `{pages}` does not appear on a page without a number (`Page numbering: none`); line of the text | `The text of box pie has {page} or {pages} and appears on a page without a number...` |
| `VALUE_SYNTAX` | Syntax of the values ([Values](values.md)), colors (`#RGB`, `#RGBA`, `#RRGGBB` or `#RRGGBBAA`), malformed `linear(...)`, more than four colors in `Border color` or four values in `Corner radius`, `Aspect ratio`, `For column` that is not a number, a unit that is not `mm` or is separated from the number, a unit in `Grow` or `Shrink` | `Invalid value: 12,5 (the decimal separator is a point...)` |
| `VALUE_NOT_ALLOWED` | Forms and signs that each property allows ([Values](values.md#value-types)); `Gap` with 1 or 2 values (in `Columns`, 1); `Size` > 0; `Count` ≥ 1; `Min column width` and `Separator` > 0; `Letter spacing` without percentages; `Tab size` ≥ 1; `Title` and `Alt` not empty and `Language` a BCP 47 tag; `Bullet markers` with unnumbered markers and `Number markers` with numberings; `Levels` with two levels from 1 to 6, the first not greater than the last; `Rotate` between −360 and 360, without 0 or whole turns; `Rotate origin` with one or two keywords, at most one per axis; `For column` from lowest to highest; `Number label` with `{number}` once | `Width does not allow *: it allows a length or a percentage...` |
| `FIT_TRACK` | `fit` is not a grid track | `fit is not a grid track: the one that fits the content is auto` |
| `DUPLICATE_ID` | `Id` values are unique | `Duplicate Id: a (already used on line 6)` |
| `SINGLE_ELEMENT` | `Item` and `Area` contain exactly one element | `An Area contains exactly one element, and this one has 2` |
| `TEXT_CONTENT` | A `Text` or a `Paragraph` has its text in the value or in `Content`, one of the two; a `Markdown` has content, and its headings and paragraphs, text | `Text has the text in the value and in Content (line 9): it goes in only one` |
| `TEXT_WITHOUT_FONT` | Text, and a `Table of contents`, need `Font` in `Typography`, their own or inherited | `The text needs Font: it is declared in the Typography of the Text, of an ancestor or of Styles (For element: Document)` |
| `TEXT_WITHOUT_FONT_SIZE` | Text needs `Size` in `Typography`, its own or inherited | `The text needs Size: it is declared in the Typography of the Text, of an ancestor or of Styles (For element: Document)` |
| `STYLE_UNKNOWN_ELEMENT` | `For element` names a box element, a generated element or a position in a table ([Styles](styles.md)) | `For element: 'Item' is not a box element or a position in a table (Document, Section, Block, Stack...)` |
| `STYLE_DUPLICATE` | Two rules of the same source do not have the same selector | `Repeated rule: For element: block (already on line 6)` |
| `STYLE_NOT_ALLOWED` | A rule that reaches a `Section` only gives `Typography`; a rule (also a position rule, a column rule or a rule of the style of a `Row`) does not give `Break` to an `Overlay`, a `Page overlay`, a heading, a `Text` of a heading or a `Cell`; `For element: Document` only allows `Typography`, and in it not `Language` | `The rule For style: page applies Break to the Overlay (line 11)...` |
| `HEADING_LEVEL` | The first heading is `Heading 1` and each one goes up at most one level ([Headings](../components/heading.md#hierarchy)) | `Level skip: Heading 3 after Heading 1; at most, Heading 2` |
| `HEADING_IN_PAGE_OVERLAY` | A heading does not go inside a `Page overlay` | `A Heading 1 cannot be inside a Page overlay...: a Text belongs there` |
| `BREAK_IN_HEADING` | A `Text` of a heading does not declare `Break` | `A Text of a heading does not accept Break: the engine already avoids breaking...` |
| `TEXT_BLANK_LINE` | A `Paragraph` has no blank lines in its `Content`, nor does a `Text` that is not preformatted (line of the blank line) ([Text](../components/text.md)) | `A Paragraph is a single paragraph: the blank line is not needed...` |
| `LIST_ITEM_EMPTY` | A `List item` contains at least one element ([List](../components/list.md)) | `A List item needs at least one element: a Paragraph, a Text, a Block...` |
| `LIST_NUMBER_NOT_ALLOWED` | `Start` and `Number` only in numbered lists | `Start has no effect with Marker: bullet, which does not number the items` |
| `LIST_NUMBER_RANGE` | The number fits in the numbering: `decimal` from 0, letters from 1, Roman numerals from 1 to 3999 (line of the `Start`, of the `Number` or of the item) | `The number 4000 cannot be written with Marker: upper-roman, which numbers from 1 to 3999` |
| `LIST_ITEM_WITHOUT_LINE` | An item with a marker starts with a text to align it with (line of the box that prevents it) | `The marker of the List item at line 10 is aligned with its first line, and the item starts with a Stack...` |
| `TABLE_ORDER` | `Header rows`, `Body` and `Footer rows` go in that order ([Table](../components/table.md)) | `Body goes after Footer rows: the groups of a Table go in the order Header rows, Body, Footer rows` |
| `TABLE_ROW_CELLS` | Each row fills exactly the columns of `Column sizes`, counting the `Span` of the rows above (line of the row, or of the extra cell) | `The row leaves column 2 of 2 (Columns) free: a Cell is missing, empty if needed, or a Span` |
| `TABLE_SPAN` | A `Span` does not go outside the table, does not go from one group to another and does not overlap another cell | `The cell takes up from column 1 to 3, and the table has 2 (Columns)` |
| `TABLE_COLUMN_ALIGN` | `Column align` gives one alignment per column of `Column sizes` (line of the `Column align`) | `Column align gives 2 alignments and the table has 3 columns (Columns): one goes per column` |
| `ROW_HEADER_IN_HEADER_ROWS` | `Row header: true` is not declared in a cell of `Header rows`, whose cells are already column headers (line of the `Row header`) | `Row header has no effect on a cell of Header rows: the header cells are already column headers` |
| `CELL_CONTENT` | A `Cell` has text in the value or elements, not both | `A Cell has text in the value (line 12) or elements, not both: the text goes in a Paragraph` |
| `TABLE_ROW_BREAK` | The rows of `Header rows` and `Footer rows`, which are repeated on each page, do not declare `Break` | `The rows of Header rows are repeated on each page of the table and do not break: Break is not needed` |
| `COLUMNS_WITHOUT_COUNT` | A `Columns` declares `Count`, `Min column width` or both ([Columns](../components/columns.md)) | `A Columns needs Count, Min column width or both...` |
| `SEPARATOR_WIDTH` | `Separator` is not wider than `Gap` | `Separator is wider than Gap, which is 0 when not declared: the rule would be painted over the columns...` |
| `SINGLE_COLUMN` | A `Columns` with `Count: 1` does not declare `Gap` or `Separator`, which separate columns ([Columns](../components/columns.md)) | `Gap has no effect with Count: 1, because there are not two columns to separate: it is not needed` |
| `SEPARATOR_COLOR_WITHOUT_SEPARATOR` | `Separator color` goes with `Separator` | `Separator color has no effect without Separator, the thickness of the rule between columns...` |
| `COLUMN_BREAK_OUTSIDE_COLUMNS` | `Before` and `After: column` only inside a `Columns` (line of the `Before` or the `After`, also from a rule) | `After: column only has an effect inside a Columns: a page break uses page` |
| `KEEP_CONFLICT` | `Keep with next: true` does not go with `After: page` or `column`, nor `Keep with previous: true` with `Before: page` or `column` (line of the `Keep`, also from a rule) ([Breaks](breaks.md#the-break-group)) | `Keep with next keeps the box with the next one and After: page separates it from it: one of the two is not needed` |
| `OVERLAY_IN_COLUMNS` | An `Overlay` or a `Page overlay` does not go inside a `Columns` | `Page overlay cannot be inside a Columns...` |
| `MARKDOWN_UNSUPPORTED` | Markdown only uses the subset of [Markdown](../components/markdown.md#the-subset), and a table row has no more cells than the header; a text with `Markup: markdown` has no images, HTML or forced line breaks within the line ([Text formatting](text-formatting.md#inline-formatting-markup)) (line of the block or of the paragraph) | `Markdown does not allow images inside a text (an image goes alone in its paragraph): only headings, paragraphs, lists, tables, code blocks, images alone in their paragraph and...` |
| `MARKDOWN_PROPERTY_MISSING` | A `Markdown` with bulleted lists declares `Bullet markers`; with numbered lists, `Number markers`; with tables, `Table columns` (line of the list or the table) | `The list needs Bullet markers in its Markdown (line 8): the marker of each level, as in Bullet markers: bullet dash` |
| `CODE_WITHOUT_FONT` | Code, inline or in a block, needs `Code font` in `Typography`, its own or inherited ([Text formatting](text-formatting.md#inline-code)); line of the text, of the paragraph or of the block | `The code needs Code font: it is declared in the Typography of the text, of an ancestor or of Styles (For element: Document)...` |
| `LINK_SCHEME` | The target of a link is an `https://`, `http://` (with a host) or `mailto:` (with a recipient) URL or `#id`; it is not empty or malformed ([Links](links.md)) | `Link: 'javascript:alert(1)' does not have a supported scheme: only https://, http://, mailto: and #id` |
| `LINK_UNKNOWN` | An `#id` link leads to a box that exists and that is not in a `Page overlay` or inside one | `The link #nada leads to an Id that does not exist: no box has the Id nada` |
| `IMAGE_UNKNOWN` | An `Image` has a path, and it is that of a file in `images/` (line of the `Image`) | `The resource logos/acme.png does not exist in .../resources/images (the path of an Image is relative to the images/ folder of the resources)` |
| `IMAGE_FORMAT` | An `Image` is a PNG, a JPEG, an SVG of the supported subset or an unencrypted PDF, which can be read, and the page it draws from a PDF has no visible annotations or fonts that do not comply with PDF/UA, such as one that is not embedded or lacks `ToUnicode`, unless the caller removes those checks (line of the `Image`) ([Image](../components/image.md#svg)) | `The image dibujo.gif is not PNG, JPEG, SVG or PDF (it is GIF): it has to be converted to one of the four` |
| `IMAGE_PAGE` | `Pdf page` goes in the `Image` of a PDF and is a page that the PDF has (line of `Pdf page`) ([Image](../components/image.md#pdf)) | `The PDF planos.pdf has 3 pages and has no page 4` |
| `BREAK_IN_IMAGE` | An `Image` does not declare `Keep together` or `Edges`, not in a rule either | `Keep together has no effect on an Image, which does not split across pages: it is not needed` |
| `TABLE_OF_CONTENTS_EMPTY` | A `Table of contents` has some heading in the levels of `Levels` (line of the `Table of contents`) | `The table of contents has no heading of levels 2 to 3 (Levels): it would have no entries` |
| `REFERENCE_UNKNOWN` | A reference has an `Id`, and it is that of a box of the document (line of the reference, [References](references.md)) | `{page of: nada}: no box has the Id nada` |
| `REFERENCE_NOT_PLACEABLE` | The page of a box, which is not in a `Page overlay`, not inside one and not on a page without a number | `{page of: pie}: pie is in a Page overlay, which is repeated on every page` |
| `REFERENCE_NOT_NUMBERED` | The number of a box that has one: a `List item` of a numbered list or a heading with `Numbered: true` | `{number of: nota}: nota has no number (only the items of a numbered list and the headings with Numbered: true have one)` |
| `REFERENCE_UNSTABLE` | The page of the referred boxes stabilizes within 10 pagination passes | `The page of x does not stabilize: after 10 passes it keeps changing...` |
| `STYLE_WITHOUT_RULE` | Each `Style` name has its `For style` rule | `The style aside has no rule: 'For style: aside' is missing in Styles` |
| `OFFSET_CONFLICT` | A `Frame` is not shifted from the top and the bottom at the same time, nor from the left and the right | `A Frame cannot be offset both from the top and from the bottom...` |
| `OVERCONSTRAINED` | An `Overlay` or a `Page overlay` with start, size and end needs some `auto` margin | `Left, Width and right, all declared, set the position twice...` |
| `MARGIN_AUTO_WITHOUT_EFFECT` | `Margin: auto` only where it centers ([The box](box.md#auto-margins)) | `Margin auto does not center a box that takes up the whole width...` |
| `ISOLATE_MARGINS_IN_ITEM` | `Isolate margins: true` does not go in an item of a `Stack` or `Grid`, which already stops margin merging ([Block](../components/block.md#boxes-that-isolate-margins)) | `Isolate margins has no effect on an item of Stack or Grid, which already stops margin merging with its children: it is not needed` |
| `START_WITHOUT_RESTART` | `Start` in a `Section` only with `Page numbering: restart` ([Document](../components/document.md#page-numbering)) | `Start only has an effect with Page numbering: restart...` |
| `ROW_BOX` | The `Box` of a `Row` only gives its appearance: `Background`, `Border`, `Border color` and `Corner radius` ([Table](../components/table.md#rows)) | `Padding has no effect on a Row, which is not a layout box...` |
| `PAGE_NUMBER_FORMAT_WITHOUT_NUMBERS` | `Page number format` does not go in a `Section` with `Page numbering: none` | `Page number format has no effect with Page numbering: none...` |
| `BEGIN_ON_FIRST_SECTION` | `Begin on: odd` does not go in the first `Section`, which already starts on page 1 ([Document](../components/document.md#beginning-on-an-odd-page)) | `Begin on: odd has no effect in the first section...` |
| `LINE_DISTRIBUTION_WITHOUT_WRAP` | `Line distribution` only with `Wrap: true` | `Line distribution only has an effect with Wrap: true...` |
| `GAP_WITHOUT_WRAP` | A two-value `Gap` in a `Stack`, only with `Wrap: true` ([Stack](../components/stack.md#properties-of-the-stack)) | `Gap has two values, but without Wrap there is a single line and the first, the gap between rows, has no effect: one value is enough...` |
| `GROW_WITHOUT_EFFECT` | `Grow` in a vertical `Stack` needs a definite height in the container | `Grow has no effect: the vertical Stack of the parent has no definite height to distribute...` |
| `ITEM_MAIN_AXIS` | An `Item` is only aligned on the cross axis of its `Stack`: `Vertical` in a horizontal one and `Horizontal` in a vertical one ([Stack](../components/stack.md#item)) | `Horizontal has no effect on an item of a Stack with Direction: horizontal, because it is its main axis...` |
| `ASPECT_RATIO_CONFLICT` | `Aspect ratio` is not combined with declared `Width` and `Height` | `Aspect ratio has no effect if Width and Height are declared` |
| `MIN_MAX_CONFLICT` | Comparable minimum, maximum and size do not contradict each other ([The box](box.md#minimums-and-maximums)) | `Min width is greater than Max width (line 7)` |
| `INDEFINITE_PERCENTAGE` | A vertical percentage needs a definite height ([Values](values.md#definite-height)) | `Height uses a percentage of a height that is not definite...` |
| `ROTATE_ORIGIN_WITHOUT_ROTATE` | `Rotate origin` only with `Rotate` | `Rotate origin has no effect without Rotate: the rotation is missing or Rotate origin is not needed` |
| `BREAK_IN_ROTATED` | A rotated `Frame` or a rotated in-flow box does not declare `Keep together` or `Edges`, not in a rule either ([Layers and rotation](layers-and-rotation.md#rotation-rotate)) | `Keep together has no effect on a rotated Frame, which does not split across pages: it is not needed` |
| `BREAK_IN_UNBREAKABLE` | An element inside an `Overlay`, a `Page overlay`, a rotated `Frame` or a rotated in-flow box, which are not split, does not write `Break` (nor does the `Row` of a cell); what comes from a rule is not checked ([Breaks](breaks.md#forced-breaks)) | `Before has no effect inside an Overlay, which is not split across pages: it is not needed` |
| `ISOLATE_MARGINS_IN_ROTATED` | `Isolate margins: true` does not go in a rotated in-flow box, which already stops margin merging ([Layers and rotation](layers-and-rotation.md#rotation-in-flow-block-text-paragraph-and-image)) | `Isolate margins has no effect on a rotated box, which already stops margin merging with its children: it is not needed` |
| `ROTATED_ITEM` | A rotated `Block`, `Text`, `Paragraph` or `Image` is not an item of a `Stack` or `Grid` ([Layers and rotation](layers-and-rotation.md#rotation-in-flow-block-text-paragraph-and-image)) | `A Stack item cannot rotate: it takes up the place its container gives it. What rotates is a box inside the item...` |
| `GRID_PLACEMENT` | Rectangular `Areas` with rows of equal length; a valid `Area` value with existing areas; an item with a fixed row fits in it ([Grid](../components/grid.md#placement)) | `Unknown area in Area: foot` |
| `FONT_UNKNOWN` | `Font` exists in the fonts folder (line of the `Font`); also `Code font` (line of the `Code font`) | `Unknown font: Arial. Families in .../resources/fonts: IBM Plex Mono, IBM Plex Sans, IBM Plex Serif` |
| `FONT_VARIANT_MISSING` | The family has the variant of `Bold` and `Italic`, and that of each formatted run (line of the text, or that of the run) | `The font IBM Plex Sans has no variant bold italic... (Font on line 6, ...)` |
| `GLYPH_MISSING` | The font has all the characters of the text (line of the character) | `The font IBM Plex Sans normal has no character '漢' (U+6F22)` |
| `CONTRAST_LOW` | The text has a contrast of 4.5:1 with what is under it, or of 3:1 if it is large ([The PDF](pdf.md#contrast)); line of the `Color` of the text, or of its box if it has none | `The text of box at line 12 has a contrast of 2.84:1 with its background (#999999 on #FFFFFF) and needs 4.5:1...` |
| `TEXT_OVERFLOW` | No word is wider than its line, nor is a line of a text with `Preformatted: true` (line of the word) | `The word 'supercalifragilística' does not fit on the line: the line measures 138.69 pt and the box leaves 100 pt...` |
| `BOX_OVERFLOW` | Boxes fit in their container, content in its height (also in the columns of a `Columns`), and no box goes outside the physical page | `The content of box at line 7 does not fit in its height: its text reaches 24 pt and the box leaves 20 pt` |
| `LIST_MARKER_OVERFLOW` | The marker of an item, with its space, fits in the left `Padding` of the list plus the left margin of the item (line of the item) | `The marker '1.' of List item at line 12 measures 11.45 pt with its space, and the indent leaves 5 pt...` |
| `TABLE_REPEAT_OVERFLOW` | The header and the footer of a table leave room for the body on an empty page (line of the `Table`) | `The header and footer of the table at line 8 measure 900 pt and leave no room for the body on an empty page...` |
| `PAGE_OVERFLOW` | What cannot be split fits on a page | `Box at line 9 does not fit on the page and cannot be broken: it is 900 pt high...` |
| `PAGE_LIMIT` | The document does not take up more pages than the limit, 2000 by default ([Limits](limits.md); it belongs to no line) | `The document has more than 2000 pages, the maximum the engine allows (Limits.maxPages changes it)` |
| `IMAGE_TOO_LARGE` | A PNG or a JPEG does not have more pixels than the limit, 25 million by default (line of the `Image`) | `The image test/limit-huge.png has 6000 × 6000 pixels (36.0 Mpx) and the maximum is 25.0 Mpx: its resolution has to be reduced...` |
| `SVG_TOO_COMPLEX` | An SVG does not draw more elements (counting what each `<use>` repeats) or nest more levels than the limit, 20,000 and 64 by default (line of the `Image`) | `The SVG test/limit-depth.svg nests 72 levels and the maximum is 64: it has to be flattened...` |
| `PDF_TOO_LARGE` | A PDF does not take up more bytes than the limit, 20 MB by default (line of the `Image`) | `The PDF planos.pdf takes up 31.4 MB and the maximum is 20.0 MB: a smaller PDF is needed...` |

[`test/errors/`](../../../../test/errors) has an example document for each code, with the line it must give
([Testing](../implementation/testing.md#error-catalog-errorcatalogtest)).

## Error types

Each error says, besides its code and its line, **who is to blame**, which is what a service needs in order to respond:

| Type | What it is | A service responds with |
|---|---|---|
| `INVALID_DOCUMENT` | The document or its styles are not valid: syntax, schema, a model rule, an overflow, a resource that the document names and does not exist | a client error (4xx), with the line |
| `RESOURCE` | The resources folder is not usable (it does not exist, a broken font or image) or the document file cannot be read | a failure of its configuration (5xx) |
| `LIMIT` | The document is valid but asks for more than is allowed ([Limits](limits.md)): more pages, an image with too many pixels, an SVG with too many elements or levels, a PDF that is too large, or an STXT text that is too large or too deeply nested | a client error for excess (413 or 422) |
| `INTERNAL` | A failure of the engine and not of the document or the environment | a server failure (500), which should be reported |

The code is that of the table above, the STXT one for syntax and schema errors (`CHILD_NOT_DECLARED`), `RESOURCE`,
`IMAGE_FORMAT` or `INTERNAL`. The line is `0` if the error does not belong to a line (a resource, a limit or an internal
failure). If it belongs to the [separate styles document](styles.md#separate-styles-document), the message says so
(`line 8 of the styles: ...`). How they are seen from Java is in [Using from Java](../implementation/api.md#errors).

> **Decision:** a failure the engine should have avoided (a page that does not advance, a link without a target) is
> `INTERNAL`: it is a defect of the engine and not of the document.
