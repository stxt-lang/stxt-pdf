# Glossary

| Term | Definition |
|---|---|
| **Border / padding / content / margin box** | The four concentric rectangles of a box ([The box](box.md#the-four-boxes)). |
| **Box that isolates margins** | Box out of which the margins of its children do not pass: it stops margin merging ([Block](../components/block.md#boxes-that-isolate-margins)). |
| **Column** | Each fragment container into which a `Columns` splits its content on a page ([Columns](../components/columns.md)). |
| **Definite** | Size that is known without laying out the content ([Values](values.md#definite-height)). |
| **Element** | A node of the document that is a box: `Block`, `Stack`, `Grid`, `Text`, `Paragraph`, the headings, `List`, `Table`, `Columns`, `Markdown`, `Table of contents`, `Image`, `Frame`, `Overlay` or `Page overlay` ([Structure](structure.md#elements)). |
| **Fragment** | Part of a box that falls in a fragment container. |
| **Fragment container** | Where the fragments of the boxes fall: each page and, inside a `Columns`, each column ([Breaks](breaks.md#model)). |
| **Generated element** | A box that the engine creates and the document does not write, but that `For element` rules name: `Code block` and `TOC 1` to `TOC 6`. |
| **Gradient** | Background that goes from one color to another along a line: `linear(angle, color, color, ...)` ([The box](box.md#gradient)). |
| **Image** | `Image` element: a box without children with a PNG, a JPEG, an SVG or a page of a PDF from the resources folder, its natural size and its aspect ratio ([Image](../components/image.md)). |
| **In-flow element** | Element that can be an item: all except `Overlay` and `Page overlay`. |
| **Inline formatting** | The runs of a text written in Markdown inside a `Text`, a `Paragraph` or a `Cell`, with `Markup: markdown` ([Text formatting](text-formatting.md#inline-formatting-markup)). |
| **Link** | `Link` of a box or Markdown `[text](target)`: an area of the PDF that leads to a URL or to another box ([Links](links.md)). |
| **Markdown** | Element whose content is written in a subset of Markdown and is translated into headings, paragraphs, lists, tables, code blocks and images ([Markdown](../components/markdown.md)). |
| **Narrowest width / natural width** | The smallest width at which nothing sticks out / the width without any line break ([The box](box.md#intrinsic-sizes)). |
| **Orphan** (`Orphans`) | Line of a paragraph that is left alone at the bottom of a page. |
| **Page area** | Rectangle of the page minus its margins. It is the reference container of the top-level elements of a section and of the `Page overlay` elements. Each section has its own ([Document](../components/document.md#page)). |
| **Page sequence** | The pages numbered since the last `Page numbering: restart`: `{pages}` is the last number of its sequence. |
| **Paper** (`Paper`) | Named page size (`A4` or `Letter`), in portrait or, with `Orientation: landscape`, in landscape. |
| **Positioned element** | `Frame`, `Overlay` or `Page overlay`. |
| **Preformatted text** | Text that keeps its spaces and its line breaks (`Preformatted: true` or `wrap`). |
| **Reference** | `{page of: id}` or `{number of: id}` in a text: the page where the box with that `Id` starts, or its number ([References](references.md)). |
| **Reference container** | Rectangle against which the percentages, the `auto` values and the offsets of a box are resolved ([The box](box.md#the-reference-container)). |
| **Resources folder** | Folder of the fonts and the images. Resources are looked up only downward ([Resources](resources.md)). |
| **Rotation** (`Rotate`) | A box that is painted rotated: in flow, around its center and taking up the rectangle that encloses it, and positioned, around its `Rotate origin` and without changing the layout. Rotated content has to fit on the page ([Layers and rotation](layers-and-rotation.md#rotation-rotate)). |
| **Run** | Part of the text of a box with its formatting (bold, italic, underline, strikethrough, link and, in code, another family) ([Text formatting](text-formatting.md#runs)). |
| **Section** | Independent part of the document (`Section`): it starts a page and has its own `Page`, its `Page overlay` elements and its numbering ([Document](../components/document.md#section)). |
| **Small caps** | Lowercase letters painted as smaller capitals (`Small caps`). |
| **Stacking context** | Group of boxes that is painted as a unit and orders its children by `Layer` ([Layers and rotation](layers-and-rotation.md)). |
| **Static position** | Where a positioned box would have been if it were in flow ([positioned elements](../components/frame-and-overlays.md#overlay)). |
| **Table of contents** | `Table of contents` element: one entry per heading with its title, a leader and its page ([Table of contents](../components/table-of-contents.md)). |
| **Track** | Column or row of a `Grid` or a `Table`. |
| **Widow** (`Widows`) | Line of a paragraph that goes alone to the next page. |
| **Wrapper** | `Item` or `Area`. It is not a box: it adds to the element it contains how that element takes part in its `Stack` or its `Grid`. |
