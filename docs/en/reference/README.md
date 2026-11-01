# stxt-pdf reference

**Model version:** 0.1 · **Namespace:** `dev.stxt.pdf` · **Units:** typographic points (pt) and millimeters (mm)

The reference is the **source of truth** for the behavior of the engine: it says what each element accepts, what each
property means, how the engine computes the position and the size of each box, what is an error and what is written to
the PDF. If the [manual](../manual/README.md) and the reference say different things, the reference prevails.

Each time the engine has had to choose between two reasonable interpretations, the choice is written where it applies,
in a block like this one:

> **Decision:** what was decided and what it implies. *Rejected alternative:* what could have been done instead.

The STXT schema of the model is [`src/main/resources/stxt/dev.stxt.pdf.stxt`](../../../src/main/resources/stxt/dev.stxt.pdf.stxt).
If it differs from the summary in [the structure](common/structure.md#schema), the file prevails.

## Common behavior

| Page | Contents |
|---|---|
| [Structure of a document](common/structure.md) | Conventions, elements and wrappers, the box tree, who places whom and the summary of the schema |
| [Values](common/values.md) | Numbers, percentages, fractions, four-side shorthands, colors and the percentage bases (definite height) |
| [The box: `Box`](common/box.md) | The four boxes, the reference container, the `Box` group, sizing (intrinsic sizes, minimums and maximums, aspect ratio) and appearance (background, border, corners) |
| [Typography: `Typography`](common/typography.md) | Inheritance, the font, line height and baseline, line breaking, alignment and language |
| [Text formatting](common/text-formatting.md) | Formatted runs, `Markup: markdown`, case, small caps, letter spacing, ligatures, preformatted text, underline and strikethrough |
| [Pages and breaks: `Break`](common/breaks.md) | Fragmentation: break opportunities and the choice of break, widows and orphans, forced breaks, borders at breaks, `{page}` and `{pages}` |
| [Layers and rotation](common/layers-and-rotation.md) | Stacking contexts, `Layer`, the painting order and `Rotate` |
| [Reusable rules: `Styles`](common/styles.md) | `For element`, `For style`, `For id` and `For column`, priority and the separate styles document |
| [References](common/references.md) | `{page of: id}` and `{number of: id}`, and how pagination converges |
| [Links](common/links.md) | `Link` and Markdown links: targets, areas and tagged PDF |
| [Resources and fonts](common/resources.md) | The resources folder, safe paths and how fonts are identified and checked |
| [The PDF](common/pdf.md) | What every PDF carries: embedded fonts, accessibility, contrast, bookmarks, title and PDF/UA-1 or PDF/UA-2; PDF/A and embedded files (Factur-X) on request |
| [Overflow](common/overflow.md) | Nothing can overflow: what is checked and which box is reported |
| [Errors](common/errors.md) | The error policy, the validation levels, the codes and their types |
| [Limits](common/limits.md) | Pages, pixels and SVG complexity that a conversion accepts |
| [Complete example](common/complete-example.md) | A document with the computation of each box done by hand |
| [Glossary](common/glossary.md) | The terms of the reference |

## Components

| Page | Elements |
|---|---|
| [`Document`](components/document.md) | `Document`, `Page`, `Title`, `Language` and `Section` |
| [`Block`](components/block.md) | `Block` and block flow, shared by all blocks |
| [`Text`](components/text.md) | `Text` and `Paragraph` |
| [Headings](components/heading.md) | `Heading 1` to `Heading 6` and their numbering |
| [`List`](components/list.md) | `List` and `List item` |
| [`Table`](components/table.md) | `Table`, `Header rows`, `Body`, `Footer rows`, `Row` and `Cell` |
| [`Stack`](components/stack.md) | `Stack` and `Item` |
| [`Grid`](components/grid.md) | `Grid` and `Area` |
| [`Columns`](components/columns.md) | `Columns` |
| [`Frame`, `Overlay` and `Page overlay`](components/frame-and-overlays.md) | The positioned elements |
| [`Image`](components/image.md) | `Image`: PNG, JPEG and SVG |
| [`Barcode`](components/barcode.md) | `Barcode`: QR, Code 128 and EAN-13 |
| [`Markdown`](components/markdown.md) | `Markdown` and the generated element `Code block` |
| [`Table of contents`](components/table-of-contents.md) | `Table of contents` and the entries it generates |

## Implementation

| Page | Contents |
|---|---|
| [Architecture](architecture.md) | The packages, the data flow and the main classes |
| [The box tree](implementation/tree.md) | How the tree is built and closed: `BoxTreeBuilder`, `Style`, the schema |
| [The layout](implementation/layout.md) | `Engine`, `LayoutContext`, the cache, break tokens and pagination |
| [Painting](implementation/painting.md) | `StackingContext`, the canvases, text, borders and images |
| [The PDF inside](implementation/pdf.md) | The structure tree of the tagged PDF, the *artifacts*, the link annotations, the bookmarks and the metadata |
| [Using from Java](implementation/api.md) | `Converter`, `DocumentReader`, the error types, limits and threads |
| [Testing](implementation/testing.md) | Reference tests, examples, error catalog, the dump of results and mutation testing |
