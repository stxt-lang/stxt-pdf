# Painting

How a laid-out document is drawn (package `paint`). What is drawn is in [Layers and rotation](../common/layers-and-rotation.md),
[The box](../common/box.md#background-border-and-corners) and [The PDF](../common/pdf.md).

## `StackingContext` and `Canvas`

`StackingContext` walks the fragments of a page in painting order (stacking contexts, `Layer`, backgrounds
before text) and calls a `Canvas`. It resolves the radii of each fragment (`Radii.resolve` on its border box, and with
`Edges: open`, `forFragment(first, last)` keeps only the corners on its side) and calls `Canvas.fill` (the background, with
`radii.inset(border)`), `Canvas.border` (one color per side, already resolved) and `image` (with the radii of the content box).
`Canvas.background(Rect, String)` remains for plain rectangles (leaders, lines between columns, text lines). It computes the
baseline with `Canvas.vertical(font)` (`hhea` in PDF, the 0.8/0.2 approximation in SVG). `Shapes` gives what both
canvases paint the same: the path of a rounded box (quarter ellipses as Bézier curves), the ring of the border (`ring`),
the trapezoid of each side and the color of a border painted whole (`uniformColor`).

Each `Piece` carries in `frames` the chain of rotations and clips of its ancestors, from the page inwards, and each step of
painting applies it with `Canvas.beginRotation`/`endRotation` and `beginClip`/`endClip`, and then the rotation of the box itself:
a rotated box is painted rotated with everything it contains, whether it is a context or rotates in flow. A box with rounded
corners adds to what it has in flow (also to a `Frame`) the clip along the curve of its padding box (`Frame.Kind.CORNERS`),
and a box with `Overflow: clip` ([Overflow](../common/overflow.md#clipping-clip)), the clip by its padding box to everything
it contains (`Frame.Kind.WINDOW`). An `Overlay` inherits the rotations and the windows, not the corners, and a
`Page overlay`, nothing. The background, the border and the image are painted with all the clips (`Clips.ALL`). The text is
painted with the rotations and the windows (`Clips.WINDOWS`), each line at its size (`TextLine.fontSize`, that of the box or
the one `Overflow: shrink` left it), and the debug outline (`Canvas.outlines()`), without clips (`Clips.NONE`), so that the
whole box is visible. The backgrounds step of a box that paints nothing, and the content step of a box without text or
image (except the leader of a table of contents, which clips by the windows), do not clip. The areas of the links are
computed when painting and call `Tagging.link`: the border box of each fragment with `Link`, in
`StackingContext.paintLinks`, and the rectangle of each run with a link on each line, joining consecutive ones with the
same destination, in `TextPainter`. The footnote area of a page (`Page.footnotes`) is painted as one more in-flow box,
after the flow and before the `Page overlay` boxes. The lines of a fragment (`Fragment.rules`, the merged borders of a
table) are one more in-flow `Piece` (`rules`), after all the descendants of the fragment: they are painted with
`Canvas.background` over the backgrounds of the cells and under their text, with the rotations and clips of what the
fragment contains. A cell with merged borders (`Box.mergedBorder()`) does not paint its border, which is only the room
for those lines.

`StackingContext` is the only one that computes where each fragment lies on the page. Whoever needs those positions
outside painting takes them from its traversal, `visit(FragmentVisitor)`: the fragments of the flow and of the footnote
area in document order, each one with its unrotated border box and the rotations within which it is painted (a
`Turns`, those of its ancestors and its own). The `Page overlay` boxes are not included, because they repeat on every
page. `PdfDestinations` uses it for the corner of each named box, also that of a note in the footnote area.

`TextPainter` paints the text lines of a fragment in the content step of its box. It first splits the runs of each line
into sequences (`Sequence`: the consecutive runs that are one marked content, with the same destination, the call or the
number of a note, or an inline image, which goes alone) and paints each one; two consecutive text sequences of the same
kind and destination, also from one line to the next, share their marked content. It adds the invisible space at the
end of the lines that end between words (not of those that split one, `TextLine.splitsWord`) and the `ActualText` of
those that end in a hyphen added by the line breaking or have letter spacing; the call and the
number of a footnote are sequences of their own (`Tagging.beginFootnoteCall`, `beginFootnoteLabel`), and a superscript
run is painted raised `TextFormat.rise()` of the size, and a subscript one, lowered (a negative `rise()`).

`Canvas` only draws and each canvas gives its `Tagging` with `Canvas.tagging()` (`PdfTagging` in the PDF and `Tagging.NONE` in the
SVG): the marked text of the tagged PDF and the links. The methods of `Tagging` do nothing by default: `NONE` overrides
none of them and `ContrastCheck`, only those that tell it which box the painted text belongs to.

> **Decision:** `Canvas` only draws and each canvas gives its `Tagging` with an explicit method and not with `instanceof`: a new
> canvas has to state what it does with the tagging. The primitives do not declare `IOException` and a single point
> of the PDF converts it. *Rejected alternative:* primitives with `throws IOException`, which force it on every canvas for a failure that
> only the PDF produces.

> **Decision:** the area of a link is computed when painting and not at layout time: the layout knows nothing about links, and the debug
> SVG ignores them. *Rejected alternative:* storing the areas in the fragment.

## The PDF

| Class | What it does |
|---|---|
| `PdfDocumentWriter` | The document: creates all the pages before painting, paints each one, writes the structure, the bookmarks, the destinations and the page labels, and saves, or leaves it in an open `PDDocument` that the caller saves. Its `write` is the entry point |
| `PdfPainter` | A laid-out page on a page of another document, untagged ([Using from Java](api.md#integration-with-pdfbox)) |
| `PdfPageWriter` | What the pages of a document share (the fonts, the images, the structure and the links) and the painting of one of them, used by `PdfDocumentWriter` and `PdfPainter` |
| `PdfCanvas` | The drawing of a page |
| `PdfContent` | The stream of a page with the origin at the bottom: paths, color, rotations and `io`, the only point that converts the `IOException` |
| `PdfTagging` | The marked content and the annotations of a page |
| `PdfFonts` | The embedded fonts and the arguments of `TJ` |
| `PdfGradients` | The shadings of the gradients and their soft masks |
| `PdfImages` | The images: embedded PNG and JPEG, SVG drawn as vectors, copied PDF pages |
| `PdfPages` | The copy of a page of another PDF into a form, without its tagging |
| `SvgFonts` | The fonts of `fonts/` with which the text of an SVG is drawn |
| `PdfStructure` | The tagged PDF: the structure tree and the marked content of each page |
| `PdfStructureLinks` | The `Link` elements and the annotations that hang from them |
| `PdfParentTree` | The parent tree: from each page and each annotation to its element |
| `UaProfile` | What PDF/UA-2 does differently from PDF/UA-1 in the structure tree |
| `PdfOutline` | The bookmarks of the headings |
| `PdfMetadata` | The title, the language, the XMP packet (PDF/UA, PDF/A and the XMP of the attachments) and the output intent of PDF/A |
| `XmpFragment` | The XMP fragment of the attachments, split into its extension schemas and the rest of its descriptions |
| `PdfAttachments` | The embedded files (PDF/A-3 or no PDF/A): the file specifications, the `EmbeddedFiles` tree and the `AF` array |
| `PdfId` | The identifier of the trailer (`/ID`), computed from the content when saving |
| `PdfLinks`, `PdfDestinations` | The link annotations and the corner of each named box (for bookmarks and internal links) |
| `ImageFit`, `Leaders`, `Shapes` | Where an image is drawn according to `Fit`, the leader of a table of contents and the paths of the boxes |

> **Decision:** internal destinations are resolved after all the pages have been painted, because a link can point
> forward: all the pages are created before any is painted and the corner of the first fragment of each named box is looked up,
> the same as for the bookmarks. *Rejected alternative:* named destinations in the catalog of the PDF.

### Text

The fonts are Type 0 Identity-H (each character takes two bytes), so `justify`, letter spacing and kerning are written
with `TJ` adjustments between the characters and not with `Tw` or `Tc`. `PdfFonts.font` embeds one font per combination with
`TrueTypeFont.setEnableGsub(font.ligatures())`: without ligatures PDFBox does not apply `GSUB` (one glyph per character, 17 to 30
times faster) and what is measured is what is painted.

The width of each glyph is rounded to thousandths of the font size, which is what the PDF writes in the widths table of the embedded
font: this way layout and painting measure the same with fonts of any `unitsPerEm`. The baseline comes from the ascender and
the descender of the `hhea` table.

With `Kerning: true`, `FontFile.kerning(text, ligatures)` gives, for each position of the text, the adjustment in
thousandths of the font size that goes before the character that starts there, and `PdfFonts.spaced` splits the text at
those positions and writes the adjustment in `TJ`, with the sign changed (in `TJ` a positive number brings closer). The
embedded font is the same with and without kerning: `PdfFonts.font` looks it up without that option. The pairs are read
by `KerningPairs` the first time they are requested: from `GPOS`, the pair adjustment *lookups* (type 2, formats 1 and 2,
also inside extensions, type 9) of the `kern` features; if there are none, the `kern` table, through fontbox. A table
that cannot be read is a `ResourceException`.

> **Decision:** with ligatures, there is a pair only where the word can be split without changing its glyphs: where what
> `GSUB` leaves of the two parts, one after the other, are the glyphs of the whole word. PDFBox applies `GSUB` to each
> piece of `TJ` separately, so splitting inside a ligature would undo it. If the pieces of all the positions with kerning
> together do not give the glyphs of the word, the word goes without kerning. *Rejected alternative:* writing the glyphs
> one by one without going through `showText`, which would require redoing the `ToUnicode` of the ligatures.

> **Decision:** the ascender and the descender come from `hhea` and not from `OS/2`: neither from `sTypo*`, even if the font
> sets `USE_TYPO_METRICS`, nor from `usWin*`. In IBM Plex Sans, `hhea` gives 1025 and 275 per 1000, the same as `usWin*`, and
> `sTypo*`, 780 and 220.

> **Decision:** justification and letter spacing go with `TJ`: with the Identity-H encoding, `Tw` has no effect. In the
> SVG they are `word-spacing` and `letter-spacing`.

With `Ligatures: true`, `FontFile.width` applies `GSUB` word by word with the same processor that PDFBox uses when painting
(`ccmp`, `liga` and `clig` for Latin script), so a ligature does not cross a space or a change of run and what is
measured is the glyphs that are written.

> **Decision:** the same `GSUB` processor as PDFBox. *Rejected alternative:* reading only the `liga` feature
> of the table, with an algorithm different from that of the PDF.

Underline and strikethrough use the metrics of the variant of the run: `underlinePosition` and `underlineThickness` of the
`post` table, and `yStrikeoutPosition` and `yStrikeoutSize` of `OS/2`, measured from the baseline to the top of
the line, which is what the OpenType specification says. They are painted as *artifacts*. The strikethrough of a
superscript or a subscript is painted from the baseline of the run and at its size; the underline, from that of the
line.

> **Decision:** the ligatures and kerning options travel in `FontFace` (its fourth and fifth components, and `base()` is
> the font without them): this way `TextMeasurer`, `Canvas.text` and the font cache of the PDF use them without changes,
> and a file is identified the same with and without them. *Rejected alternative:* one more parameter in `TextMeasurer.width` and in `Canvas.text`.

> **Decision:** each font file is read once, when indexing the folder: what is embedded and drawn comes from those bytes
> (`FontFile.content()`), so it is what was measured even if the file changes or disappears, and painting cannot fail with an
> `IOException` from a font. *Rejected alternative:* a copy of the bytes of its own, which doubles the memory, or reading the
> file in each conversion.

### Borders, backgrounds and gradients

A plain, opaque border with square corners is four rectangles. Otherwise, it is an even-odd ring, clipped to each trapezoid
with distinct colors. The gradient is an axial shading (type 2 function, or type 3 with more than two colors) clipped
to the box, with a luminosity soft mask if some color is not opaque, because a PDF shading has no opacity per
color. The opacity of a plain color is a graphics state `ca` inside a `q`/`Q` of its own.

> **Decision:** a plain, opaque border with square corners is painted as four rectangles and not as a ring, because the
> even-odd ring leaves a different antialiasing on the edge of the box and would change the reference images. The ring is
> used with rounded corners, with opacity and with distinct colors.

> **Decision:** `Canvas.border` receives one color per side, already resolved, `Canvas.fill` paints the background of a box with its
> radii and `image` receives the corners to clip with. `background(Rect, String)` remains for plain rectangles.

### Rotation

`PdfCanvas` writes a `cm` inside `q`/`Q` and keeps the composed rotation for the `QuadPoints` of the links and the `BBox` of
the figures. It keeps it in a `Turns` (`model`), the stack of the rotations a box is painted inside: each rotated box
pushes its own when it starts and pops it when it ends, and `envelope` gives the rectangle that encloses a rotated one.
With the same stack `StackingContext.visit` gives the rotations of each fragment (from which `PdfDestinations` takes the
corner of a rotated box), `ContrastCheck` where each background lies and
`Overflow` whether what is rotated fits on the page. The coordinates of `Rotation` are those of the page, with y pointing down: a positive angle rotates
clockwise (in the PDF, with the origin at the bottom, the angle changes sign).

> **Decision:** the annotation of a rotated link has as `Rect` the rectangle that encloses the rotated area and as
> `QuadPoints` the rotated area. The `BBox` of a rotated figure and the destination of a rotated box are those of the rectangle that
> encloses it. *Rejected alternative:* only the enclosing rectangle, without `QuadPoints`.

### Images

`PdfImages` embeds the PNG and the JPEG with PDFBox and the SVG with JSVG 2.2.0 (`ResourcePolicy.DENY_ALL`) drawing on a
`PdfBoxGraphics2D` (`pdfbox-graphics2d` 3.0.5), which gives a form of the natural size, once per file.
It draws the SVG with the bytes `ImageFile` kept when it read it, and reads the PNG, the JPEG and the PDF again from the
file after checking with `ImageFile.checkUnchanged` that it has not changed ([Using from Java](api.md#files-that-change)). If it
cannot read them, the error is a `ResourceException` with the path. `SvgCanvas` follows the same rules for the `data:`
of each image.
`PdfCanvas.image` places it with a scaling `cm` matrix where `ImageFit` says and resets the dash pattern before
drawing it. JSVG draws the text as outlines. `PdfCanvas.inlineImage` draws an inline image
([Text formatting](../common/text-formatting.md#images-inside-a-text)) in the rectangle of its run, which
`TextPainter` computes like that of the image marker (centered on the capitals on the baseline), outside the text
sequences and without underline or strikethrough; the `Figure` or the *artifact* is opened by
`PdfTagging.beginInlineImage`.

> **Decision:** the text of an SVG is outlines and not PDF text, because JSVG allows nothing else. *Rejected
> alternative:* having the engine draw the text of the SVGs with its own fonts, which required reimplementing
> SVG text layout.

> **Decision:** a form inherits the graphics state, and debug mode leaves the dash pattern of the
> outlines active: `PdfCanvas.image` resets the dash pattern (`[] 0 d`) inside its `q ... Q` before drawing the form.

The page of a PDF is copied by `PdfPages` with PDFBox's `LayerUtility.importPageAsForm`, which copies its content and its
resources (those shared between pages, once) and not its annotations. The form carries the crop box of the page as
`BBox` and a matrix of its own that rotates it according to `Rotate` and brings it to the origin (`PdfPages.placement`), so it measures what
`ImageFile.width(page)` says and is placed like an SVG. Then the tagging of the source PDF is removed from it: the content
marks (`BMC`, `BDC`, `EMC`, `MP`, `DP`) of its content and of that of the forms, the tiling patterns and the glyphs of
the Type 3 fonts it uses, except those of optional content (`/OC`), and the `StructParent` and `StructParents` entries. The source PDFs are opened once per file and
closed after saving. `PdfInspector` (`resources`) reads, when the document is loaded, the size of each page, with the
same rotation (`PdfInspector.rotation`), and what prevents drawing it the same, separately for each `Check`: a visible annotation
that is not a link (`PDF_IMAGE_ANNOTATIONS`) and a font that does not comply with PDF/UA (`PDF_IMAGE_FONTS`). In the
resources of the page and in those of its forms, its patterns and its Type 3 fonts it looks for a font that is not
embedded, without `ToUnicode` (except a simple one that is not symbolic), a TrueType font with an encoding that PDF/UA does
not allow or a Type 2 CIDFont without `CIDToGIDMap`. Then `PdfGlyphs` (a PDFBox `PDFStreamEngine`) walks the content of
the page, its forms, the Type 3 glyphs it draws and its tiling patterns, and looks once at each code of each font: that it
is not `.notdef`, that it says which character it is and that its width is that of the font program. `Images` throws the
error of those that have not been removed. `LayerUtility` also copies the layers (`OCProperties`) to the catalog, and
`PdfPages` sets the name of the file on each configuration without `Name` (the `D` and those of `Configs`) and removes its
`AS`: PDF/UA requires the first and does not allow the second (ISO 14289-1, 7.10).

> **Decision:** the tagging of the source PDF is removed from the copied content: its `MCID` do not belong to this document, and an
> *artifact* or a marked content inside the figure of the `Image` violates PDF/UA. The optional content marks are
> kept because they decide what is seen. *Rejected alternative:* copying the content as it is.

> **Decision:** the rotation matrix is our own and not that of `LayerUtility`, which keeps the unrotated size and distorts a
> page rotated 90°.

A generated image, such as a `Barcode` (`Box.modules()`), does not go through `PdfImages`: `PdfCanvas.modules` places the
matrix with `ImageFit.modules` (`Modules.fit()`: `inside` if it has several rows, `stretch` if it has one) and writes one
rectangle for each run of consecutive dark modules of each row (`Modules.rects`), all in a single path that is filled once (`f`) with
the color of the typography, inside the clip of the corners and the marked content of its figure.

> **Decision:** a single path and a single fill, and not one fill per module: two rectangles filled separately show a thin
> line between them in some viewers because of anti-aliasing, which in a QR code reads as a light module.

> **Decision:** JSVG and `pdfbox-graphics2d` for the SVGs, without dependencies of their own. *Rejected alternative:* Apache Batik, which
> drags in dozens of jars.

**Nothing is registered in AWT.** `SvgInspector` records the font of each `<text>` and `<tspan>` with the inheritance of the file
(`ImageFile.textFonts`) and `SvgFonts`, the `DomProcessor` of JSVG, gives it a key family (`stxt-font-` and the digest of the
content, `FontFile.digest`) with its weight and its style explicit, and `PlatformSupport.customFont` provides it with `Font.createFont`.
The font cache of JSVG is JVM-wide and not synchronized: an SVG with text is drawn inside
`PdfImages.JSVG_FONTS`.

> **Decision:** the key of each font is the digest of its content because JSVG keeps the fonts in a JVM-wide cache
> whose key does not include the `PlatformSupport`: this way what it keeps is valid for any folder with that same file and grows
> with the distinct files and not with the conversions.

> **Decision:** the SVGs with text are drawn one at a time, inside a class lock, because the font cache of
> JSVG is a static, unsynchronized `HashMap`. Measured with eight threads: the same time as without the lock. *Rejected
> alternative:* resolving the fonts without a cache, which JSVG does not allow choosing.

### Tagged PDF

`PdfStructure` builds the structure tree with the marked content (`Tagging.beginText`/`endText`, and the backgrounds, the
borders and the `Page overlay` are *artifacts*). `PdfStructure.type` is an exhaustive `switch` over `Element.Role`, so a
new element with a role that already exists does not touch `paint`. The `Link` elements and the annotations that hang
from them belong to `PdfStructureLinks`: `wrap` creates the `Link` of a box with `Link` as a child of its element and
parent of its content (in a table, which cannot wrap its own, `link` creates an empty `Link` that is a child of the
`Table`), `beginInline`, that of a text link (from `PdfStructure.markLink`), and `outside` finds the loose `Link` of
what is not content, which `standIns` left in its place when building the tree. `PdfStructure.markInlineImage` is the
`Figure` of an inline image when it is painted, with its `Alt` and its `BBox`, under the element of the text or under a
new `Link` if it is in a link. The other pieces of `PdfStructure`: `figure` builds the `Figure` with its `Caption` (or
the `Caption` inside the `Table`), `children` hangs each footnote after the text that calls it and `captionNotes`, after
the figure whose caption calls it, `markFootnoteCall` and `markFootnoteLabel` mark the call (a `Reference` with `Ref`
both ways) and the number of the note (its `Lbl`), `goTo` gives the action with the structure destination of PDF/UA-2,
and `PdfStructure.none()` is the untagged structure that `PdfPainter` paints with. `PdfParentTree` is the parent tree,
shared by the tree and its links: the key of each page with the elements of its `MCID`s, and that of each annotation
with its `Link`. `UaProfile` gathers what PDF/UA-2 does differently from PDF/UA-1 (the namespace, `FENote`, the
`Reference` of a `TOCI`, the `ListNumbering`, the `Contents` of a link with `Alt` and the structure destinations):
`PdfStructure` and `PdfStructureLinks` ask it and do not know which part they write. `PdfOutline` writes the bookmarks
with the headings that `PdfStructure` collected when building the tree (`headings()`). What each one writes is in
[The PDF inside](pdf.md).

> **Decision:** `Element.Role` is the role of a box, shared by several elements, and it is the only thing painting looks at:
> the names of the PDF tags are in `PdfStructure` (and those that change with the part of PDF/UA, in `UaProfile`).
> *Rejected alternative:* PDF methods in `Element`, which put the PDF in `tree`.

## Contrast

`ContrastCheck` is a `Canvas` that does not draw: it records, in painting order, the area and the colors of each background
(`fill` and `background`) and the area of each image, in page coordinates (with the rotation applied: without rotation the
area is a rectangle, and with one, a path, which costs much more to ask whether it contains a point). In each `text` it looks at
the point of the first letter (a quarter of the font size to the right and a third above the baseline, where the
underline does not reach) and goes through what has been recorded that contains it: an opaque color replaces what is underneath, a translucent one is mixed with each
possible color underneath and an image leaves it unknown until an opaque color covers it. It knows the box of the text through its
`Tagging` (`beginText`). The color of a styled run comes in the `text`, and the error reports the `Color` of its rule
(`Box.runStyle`). Borders are not recorded and rounded corners count as square. It is used by
`Converter.layOut`, on the pages already laid out ([The PDF](../common/pdf.md#contrast)).

## The debug SVG

`SvgCanvas` writes the SVG of the tests, one page with `page` and the whole document with `pages`, which is what
`Converter.toSvg` calls (it asks the cancellation and tells `Progress` at each page, as `PdfDocumentWriter` does): `<path>` for the rounded boxes, `linearGradient` and `clipPath`, and `<rect>` for
what is plain and square, a `<g transform>` for the rotation, the modules of a `Barcode` as a `<path>` with one rectangle per
run and the images as `data:` (the page of a PDF, drawn into a PNG at
144 dpi with `PDFRenderer`, because a browser does not draw a PDF inside an SVG). It does not paint links or distinguish ligatures.
With `Kerning` and the resources folder, it writes the adjustments of the pairs as `dx`, with `font-kerning: none` so
that the viewer does not add its own.

> **Decision:** the SVG keeps the spaces with `xml:space="preserve"` when a text has two consecutive spaces or starts
> or ends with a space, and writes the lines with `\n` on any system.
