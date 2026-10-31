# Errors

If the document has a problem, the conversion stops at the first one and gives a message like this:

```
línea 18: Margin auto no centra una caja que ocupa todo el ancho: declara Width o Max width
```

- **The line** is that of the property that fails, not that of its element: the line of the `Margin`, not that of the
  `Block`. If the problem is a combination of two properties, the message names the line of the other one. In a separate
  styles file it says `línea 8 de los estilos`.
- **The message** says what is wrong and how to fix it.
- **Each error has a code** (`MARGIN_AUTO_WITHOUT_EFFECT`) that does not change even if the message changes.

## The most common ones

| Code | What happens | Fix |
|---|---|---|
| `CHILD_NOT_DECLARED` | A property or an element where it is not accepted (`Column sizes` in a `Stack`, `Typography` in an `Image`) | Checking the property table of the element |
| `TEXT_WITHOUT_FONT`, `TEXT_WITHOUT_FONT_SIZE` | A text without a font or without a size | Declaring them in `For element: Document` |
| `FONT_UNKNOWN` | The font is not in `fonts/` | Checking the name of the family. The message lists the available ones |
| `GLYPH_MISSING` | A character that the font does not have | Using another font or removing the character |
| `CONTRAST_LOW` | A text that barely stands out from its background: a light gray on white | Darkening its `Color` or changing the background ([Box](box.md#contrast)) |
| `TEXT_OVERFLOW` | A word wider than its line | Widening the box, reducing the size or splitting the word |
| `BOX_OVERFLOW` | A box does not fit in its place: wider than its parent, content taller than its `Height`, or something that goes off the page | Checking the `Width`, the `Height` or the `Offset` the message names |
| `PAGE_OVERFLOW` | Something that cannot be split does not fit even on an empty page | Making it smaller or allowing it to be split |
| `INDEFINITE_PERCENTAGE` | A percentage of a height that is not known | Giving the container a `Height` in points ([Values](values.md#percentages)) |
| `MARGIN_AUTO_WITHOUT_EFFECT` | A `Margin: auto` that cannot center | Giving the box a `Width`, or removing the `auto` |
| `STYLE_WITHOUT_RULE` | A style without its `For style` rule | Adding the rule or correcting the typo |
| `HEADING_LEVEL` | A skipped level (`Heading 3` after `Heading 1`) | Using the next level |
| `TABLE_ROW_CELLS` | A row with too many or too few cells | Adding empty `Cell:` entries or using `Span` |
| `IMAGE_UNKNOWN`, `IMAGE_FORMAT` | The image does not exist or is not a PNG, a JPEG, a PDF or a supported SVG | Checking the path or converting the file |
| `IMAGE_PAGE` | `Pdf page` in an image that is not a PDF, or a page that the PDF does not have | Removing `Pdf page` or correcting the number |
| `REFERENCE_UNKNOWN`, `LINK_UNKNOWN` | A `{page of: id}` or an `#id` to an `Id` that does not exist | Checking the `Id` |
| `DUPLICATE_ID` | Two boxes with the same `Id` | Changing one of them |

The complete list, with an example of each one, is in the [reference](../../reference/common/errors.md#codes), and the
[error catalog](../../../../test/errors) has a test document for each code.

## Why so many errors

Anything that would have no effect or contradicts something else is an error, so that the document says exactly what
comes out in the PDF. A `Margin: auto` that does not center, a `Line distribution` or a two-measurement `Gap` in a
`Stack` without `Wrap`, or a `Rotate origin` without `Rotate` would do nothing.

There are exceptions, so as not to get in the way: writing the default value (`Span: 1 1`), what is inherited (a
`Typography` applies to what is inside), a `Border color` without `Border` or a `Corner radius` with nothing to round
(they usually come from a shared style), what comes from a rule to where it cannot act, a rule that does not apply to
anything yet and the properties of a `Markdown` that its content does not use. The complete list is in the
[reference](../../reference/common/errors.md#what-has-no-effect).

## Errors that are not in the document

A resources folder that does not exist, a damaged font file or a document that asks for too much (more than 2,000 pages,
an image of more than 25 million pixels) are also errors, without a line. From Java they are told apart by their type
([Using from Java](java-usage.md#errors)).
