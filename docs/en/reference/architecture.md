# Architecture

What is in the code, how it is related and which are the main classes. It is a map: the detail of each part is in the
[implementation](README.md#implementation) pages and in the javadoc.

Java 17 and Maven: artifact `dev.stxt:stxt-pdf`, root package `dev.stxt.pdf`. The third-party dependencies are
`stxt-core` (reading and validating STXT), PDFBox (writing the PDF), commonmark-java (Markdown), JSVG and `pdfbox-graphics2d`
(drawing SVG in the PDF) and ZXing `core` (encoding the QR codes and barcodes). In the tests, JUnit 5, veraPDF and PIT.

## Data flow

```
STXT text ──DocumentReader──▶ InlineNode (stxt-core)
          ──BoxTreeBuilder──▶ tree of Box (with its Style), PageSetup and the Section
          ──Engine.layOutDocument──▶ Paginator ──▶ Overflow ──▶ ModuleSizes ──▶ LaidOutDocument (pages with their Fragment)
          ──▶ StackingContext + Canvas ──▶ PdfCanvas (PDF) · SvgCanvas (debug SVG)
          ──▶ StxtDump (dump for the tests)
```

1. **Read.** `DocumentReader` parses the text with `stxt-core`, validates it against the schema and requires a single
   `Document` root (or `Styles`, in a separate styles document).
2. **Build the tree.** `BoxTreeBuilder` translates each element into a `Box` with its `Style`, applies inheritance and the
   `Styles` rules, checks the rules that the schema cannot express and closes the tree, which from then on is
   read-only.
3. **Lay out.** `Engine` dispatches each box to its layout algorithm. The `Paginator` splits each section into pages,
   repeats pagination until the references converge and places the `Page overlay` elements. `Overflow` checks that nothing overflows,
   and `ModuleSizes`, that no code is left too small to be read.
   The result is a `LaidOutDocument`: a list of `Page` with their tree of `Fragment`.
4. **Paint.** `StackingContext` walks the fragments of each page in painting order and calls a `Canvas`:
   `PdfCanvas` writes the PDF (with its tagging, bookmarks and links) and `SvgCanvas`, the debug SVG.
   `ContrastCheck` is a canvas that does not draw: `Converter.layOut` paints each page with it to check the contrast of
   the text ([The PDF](common/pdf.md#contrast)).

`Converter` wraps the four steps for the caller ([Using from Java](implementation/api.md)).

## Packages

Dependencies go in a single direction and without cycles. `ArchitectureTest` states what each package can use:

| Package | Can use | Contents |
|---|---|---|
| `model` | nothing | Pure, shared types: the values (`Value`, `ValueParser`), the enums of the values (one per file, such as `TextAlign`, `Marker` or `Break`), geometry (`Rect`, `Sides`, `Insets`), formatted text (`TextRun`, `TextFormat`, `RunStyle`, `FontFace`), references (`Reference`), links (`Links`), grid placement (`AreaMap`, `Area`, `Cell`), colors and backgrounds (`Colors`, `Paint`, `Radii`), rotation (`Rotation`) and the rotations a box is painted inside (`Turns`), the modules of a generated image (`Modules`, with `BarcodeFormat` and `ErrorCorrection`), tolerance (`Tolerance`), limits (`Limits`) and the root of the errors (`StxtPdfException`) |
| `resources` | `model` | The resources folder (`Resources`), fonts (`FontLibrary`, `FontFile`, `KerningPairs`) and images (`ImageLibrary`, `ImageFile`, `SvgInspector`, `PdfInspector`, `PdfGlyphs`) |
| `tree` | `model` | The box tree (`Box`, `Element`, `Style` and its groups) and its construction (`BoxTreeBuilder` and its parts), the model errors (`InvalidModelException`, `ErrorCode`) |
| `stxt` | `model`, `resources`, `tree` | `DocumentReader` |
| `fragment` | `model`, `tree` | The result of the layout: `Fragment`, `Page`, `TextLine`, `Rule` (the lines of a table with merged borders), the break tokens (`BreakToken`, `BreakAppeal`, `Unit`), the break rules (`BreakRules`) and `PageFragment`, a fragment with its boxes on the paper |
| `layout` | `fragment`, `model`, `resources`, `tree` | The engine (`Engine`, `Paginator`, `Overflow`, `ModuleSizes`) and one subpackage per algorithm: `block`, `inline` (with `inline.hyphenation`, the hyphenation patterns), `stack`, `grid`, `table`, `columns`, `position` and `image` |
| `output` | `fragment`, `model` | `StxtDump`, the dump for the tests |
| `paint` | `fragment`, `layout`, `model`, `resources`, `tree` | Painting (`StackingContext`, `Canvas`) and the two canvases: the PDF (`PdfDocumentWriter`, `PdfCanvas` and its parts) and the SVG (`SvgCanvas`) |
| `convert` | all except `tree` and `output` | `Converter`, the entry point of the engine. Nothing depends on it |

A type needed by `tree` and `layout` goes in `model`. Among the `layout` algorithms, block uses `inline` (list markers
are text) and `position`. `stack` and `grid` use `position`, and table uses `grid`. No algorithm names `Engine`: they
depend on the `LayoutContext` interface.

## The main classes

### Entry point

- **`Converter`** (`convert`): the facade. Immutable and thread-safe. It holds the resources folder, the separate
  styles, the limits and the debug mode (`Converter.builder()`), and provides `layOut`, `toPdf` and `toSvg`. Each conversion creates
  its own `Engine`.
- **`DocumentReader`** (`stxt`): reads a document or a styles document and validates it against the schema.

### Box tree (`tree`)

- **`Box`**: a box of the tree. It holds its `Element`, its `Style`, its children, its text and its runs, its image or the
  modules of its barcode, its list marker, its number, its `Id` (or the generated name that stands in for it), the named
  rows of a table, the border a cell declares with `Merge borders` and the style of its named runs. It holds no geometry:
  position and size exist only in the fragments. It is closed when construction ends.
- **`Element`**: the enum of the elements (such as `BLOCK`, `HEADING_2` or `CELL`), with its `Layout`, its `Position` and its role
  (`Element.Role`), which is the only thing that layout and painting look at.
- **`Style`** and its groups: the properties of a box, split as in the document (`TypographyStyle`, the only one that
  is inherited, `BoxStyle`, `BreakStyle`) and by element type (`StackStyle`, `GridStyle`, `ItemStyle`, `ListStyle`,
  `ColumnsStyle`, `ImageStyle`, `FigureStyle`, `TableOfContentsStyle`), with the document line of each property.
- **`BoxTreeBuilder`**: the orchestrator of the construction ([The box tree](implementation/tree.md)). It splits the
  work among its parts:
  - `ElementReader`, what is specific to each family of elements;
  - `ValueReader`, the values, and `PropertyReader`, the properties;
  - `StyleRules`, the `Styles` rules;
  - `TableBuilder`, `ListBuilder`, `MarkdownBuilder` and `TableOfContentsBuilder`, tables, lists, Markdown and
    tables of contents, and `RunMarks`, the styled runs and the escaped braces of the Markdown ([The box tree](implementation/tree.md#text-and-runs));
  - `TableBorders`, the merged borders of a table, and `NamedColors`, the named colors of `Styles`;
  - `Barcodes`, the drawing of a `Barcode`: the first implementation of `ImageGenerator`, the extension point of the
    generated images ([The box tree](implementation/tree.md#generated-images)), with `Gs1` (GS1 data) and
    `Gs1DataMatrix` (the matrix of a `gs1-data-matrix`);
  - `ModelValidator`, the rules that look at the box once read;
  - `Footnotes`, the footnotes: the area of each section, the numbering of the calls and their checks;
  - `Figures`, the figures with a caption: their counters, where the caption goes and their `Alt`.

### Layout (`layout`)

- **`Engine`**: dispatches each box to the algorithm of its `Layout` and keeps the layout cache and, for the text,
  `TextValues` and `LineBreakingMemo`. It implements `LayoutContext`, which
  is what the algorithms see. It is not shared between threads.
- **`LayoutAlgorithm`**: the interface of the algorithms (`BlockLayout`, `InlineLayout`, `StackLayout`, `GridLayout`,
  `TableLayout`, `ColumnsLayout`, `ImageLayout`): `layout(box, space, from)` and `contentIntrinsic(box)`.
- **`ConstraintSpace`** and **`LayoutResult`**: the immutable input and output of the layout of a box. The result carries
  the `Fragment`, the `BreakToken` to continue on the next page and the quality of the break.
- **`FootnoteLayout`**: each page of a section with footnotes: the flow with the bottom reservation and the notes of
  its calls stacked in the footnote area ([Layout](implementation/layout.md#pagination)), with the same `Stacking` that
  stacks the children of a block in `BlockLayout`.
- **`Paginator`**: splits each section into pages, makes the references converge, numbers the pages and places the
  `Page overlay` elements.
- **`Overflow`**: looks for overflows in the already paginated result.
- **`ModuleSizes`**: checks in the already paginated result that the modules of each code are not below their minimum,
  and that the readable text of a barcode, which `BarcodeTextLayout` places, fits under its bars.
- Pure parts of each algorithm: `LineBreaker` (line breaking, with the `Hyphenator` of `inline.hyphenation` to split
  words; the words in `Words`, their measures and positions in `Paragraph` and the placing of each line in
  `LinePlacer`), `StackResolver`, `TrackSizing` and `Placement` (grid), `ItemSizing` (the size of a `Stack` or `Grid` item in
  its space and the stretched height of an `Overlay`),
  `Columns` (how many columns and how wide), `OverlayEquation`, `UnitBreaker` (breaks of `Stack`, `Grid` and tables), `Slice` (height, border and
  padding of each fragment), `LineWidths` (the width of each line beside the side boxes), `MergedBorders` (the lines of a
  table with merged borders) and `RunningHeads` (the heading in effect on each page)
  ([The layout](implementation/layout.md)).
- **`TextMeasurer`**: measures text. `FixedMetrics` (each character measures half the font size) is the one of the layout tests.
  `PdfMetrics` provides the real metrics of the fonts and is the one of the PDF.

### Painting (`paint`)

- **`StackingContext`**: orders the painting of a page into stacking contexts and calls the canvas. It is the only one that
  computes where each fragment lies on the page: the destinations of the PDF take it from its traversal (`visit`). The
  text lines are painted by **`TextPainter`**, which also computes the link areas of the runs.
- **`Canvas`**: the interface of the canvases, which only draw. Each canvas provides its `Tagging`: the tagging of the PDF, and in the
  SVG, nothing.
- **`PdfDocumentWriter`**: writes the PDF document: pages, tagging (`PdfStructure`, with its links in
  `PdfStructureLinks`, the parent tree in `PdfParentTree` and the differences of PDF/UA-2 in `UaProfile`), bookmarks
  (`PdfOutline`), destinations
  (`PdfDestinations`), links (`PdfLinks`), metadata and PDF/A (`PdfMetadata`), attachments (`PdfAttachments`) and saving,
  or leaves it in an open `PDDocument` that the caller saves. `PdfCanvas` draws a page, with `PdfContent`, `PdfFonts`,
  `PdfGradients` and `PdfImages`. **`PdfPainter`** paints a laid-out page on a page of another document, untagged
  ([Using from Java](implementation/api.md#integration-with-pdfbox)).
- **`SvgCanvas`**: the debug SVG of the tests ([Painting](implementation/painting.md)).
- **`ContrastCheck`**: the contrast of the text with what is underneath it ([Painting](implementation/painting.md#contrast)).

## Architecture rules

- **Dependencies in a single direction**, checked by `ArchitectureTest`. A new dependency is decided and recorded
  there.
- **The box tree is read-only outside `tree`**: `Box` and `Style` are written only in `tree` and are closed when
  construction ends.
- **Geometry exists only in the fragments**: a box is laid out many times (measurements, probes, each page), so it does not
  keep "its" position.
- **Every recursive layout call goes through `LayoutContext`** (`context.layout`, `context.intrinsic`), to use the cache.
- **Outside `tree`, layout and painting ask for the role of a box, never for its element** (except the dispatch of the
  table in `Engine`), so a new element with a role that already exists does not touch `paint`.
- **The inputs and outputs of the layout are immutable `record` types.**
- **What the jar promises is written down**: the table of "What is promised" in
  [Using from Java](implementation/api.md#what-is-promised). Everything else public is so because another package uses
  it, and `PublicApiTest` checks that no promised signature exposes it.
