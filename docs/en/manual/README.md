# stxt-pdf manual

stxt-pdf turns an [STXT](https://stxt.dev) document into a PDF. The document describes what goes on each page and how it
looks. The engine works out where everything goes, splits the content into pages and writes the PDF with embedded fonts,
tagged for accessibility and with bookmarks.

Following this manual only requires knowing how to write STXT and what a PDF is. The exact detail of each rule is in the
[reference](../reference/README.md).

## How to read it

First, [getting started](common/getting-started.md), which writes a document from start to finish, and
[concepts](common/concepts.md), with the ideas the rest of the manual uses. After that, each page can be read on its own:

- **Common behavior**: what all the elements share (measurements, margins, text, page breaks, styles).
- **Components**: each element, with a complete document that can be copied and converted.

## Common behavior

| Page | What it explains |
|---|---|
| [Getting started](common/getting-started.md) | A complete document and how it is turned into a PDF |
| [Concepts](common/concepts.md) | Pages, boxes, flow, sections and why the engine is strict |
| [Values](common/values.md) | How measurements, percentages, colors and gradients are written |
| [The box](common/box.md) | Size, margins, padding, border, background and corners of any element (`Box`) |
| [Typography](common/typography.md) | Font, size, line height, alignment, color and language of the text (`Typography`) |
| [Text formatting](common/text-formatting.md) | Bold and italic within a sentence, uppercase, small caps, letter spacing, ligatures and preformatted text |
| [Pages and breaks](common/pages-and-breaks.md) | How the content is split into pages, how to control the breaks (`Break`) and page numbers |
| [Layers and rotation](common/layers-and-rotation.md) | What is painted on top of what and how to rotate a box |
| [Styles](common/styles.md) | Reusable rules (`Styles`) and styles in a separate file |
| [References and links](common/references-and-links.md) | "See page 12", section numbers and clickable links |
| [Resources](common/resources.md) | Where fonts and images go |
| [Errors](common/errors.md) | How to read an error, and the most common ones |
| [Using from Java](common/java-usage.md) | Converting documents from a program |

## Components

| Page | What it is for |
|---|---|
| [`Document` and `Section`](components/document.md) | The document, the page size, the sections and the page numbering |
| [`Block`](components/block.md) | Grouping elements one below another |
| [`Text` and `Paragraph`](components/text.md) | Text and paragraphs |
| [Headings](components/heading.md) | Chapter and section titles, numbered or not |
| [`List`](components/list.md) | Bulleted or numbered lists |
| [`Table`](components/table.md) | Tables with a header and a footer that repeat on each page |
| [`Stack`](components/stack.md) | Elements in a row or a column that share the space |
| [`Grid`](components/grid.md) | A grid of rows and columns |
| [`Columns`](components/columns.md) | Text in several columns, as in a magazine |
| [`Frame`, `Overlay` and `Page overlay`](components/frame-and-overlays.md) | Shifting a box, placing it at an exact position or repeating it on each page |
| [`Image`](components/image.md) | PNG, JPEG and SVG images, and pages of a PDF |
| [`Markdown`](components/markdown.md) | Writing long content in Markdown |
| [`Table of contents`](components/table-of-contents.md) | The automatic table of contents |

The [examples](../../../test/examples/README.md) are eleven real documents with their PDF, from an invoice to an annual
report.
