# Using from Java

The engine is a Java 17 library (`dev.stxt:stxt-pdf`). The caller uses `Converter` and `DocumentReader`.

## Converting

```java
InlineNode doc = new DocumentReader().read(Path.of("report.stxt"));
try (OutputStream out = Files.newOutputStream(Path.of("report.pdf"))) {
    Converter.create().toPdf(doc, out);   // lays out with the real font metrics and paints
}
```

`Converter` holds the configuration of a conversion and is immutable and thread-safe: it is created once and shared.
To use the pages before painting them, or to paint them to PDF and to SVG at the same time:

```java
Converter converter = Converter.create();
LaidOutDocument laidOut = converter.layOut(doc);
converter.toPdf(laidOut, out);
List<String> svg = converter.toSvg(laidOut);   // one string per page
```

| `Converter.builder()` | What it changes |
|---|---|
| `resources(Resources)` | The resources folder (by default, `resources/` in the working directory) |
| `styles(InlineNode)`, `styles(Path)`, `styles(String)` | The separate styles document |
| `limits(Limits)` | The limits of the conversion |
| `debug(boolean)` | Paints the outlines of the boxes |
| `pdfUa(int)` | The part of PDF/UA the PDF conforms to: 1 (PDF 1.7, by default) or 2 (PDF 2.0) ([The PDF inside](pdf.md#pdfua-2)). Any other value is an `IllegalArgumentException` |
| `exclude(Check...)` | Removes checks. By default all of them are made ([Checks](#checks)) |

> **Decision:** the configuration is an immutable object, `Converter`, thread-safe because each conversion creates its
> own `Engine`, with `layOut`, `toPdf` (of a document or of one already laid out) and `toSvg`. The styles are never `null`: if
> there are none, `styles` is not called. It lives in a package of its own, above all the others. `PdfDocumentWriter.write` and `Engine`
> remain public as a low-level layer. *Rejected alternative:* a method with one overload for each
> combination of options.

## Checks

Some checks are not rules of the document but of what is seen or of what the files contain, and the caller
can remove them with `exclude`, which accumulates across calls. By default all of them are made, and `Converter.excluded()` gives
the ones that have been removed. They are the values of the enum `Check` (`model`):

| `Check` | What it checks | Without it |
|---|---|---|
| `CONTRAST` | The contrast of the text with its background, `CONTRAST_LOW` ([The PDF](../common/pdf.md#contrast)). `layOut` checks it with `ContrastCheck`, a canvas that records what is painted | A text may be unreadable |
| `PDF_IMAGE_FONTS` | That the fonts of the page of a PDF drawn by an `Image` are embedded and state which character each glyph is, `IMAGE_FORMAT` ([Image](../components/image.md#pdf)) | The viewer may substitute a font, its text may not be copyable and the PDF may not conform to PDF/UA |
| `PDF_IMAGE_ANNOTATIONS` | That the page of a PDF has no visible annotations, `IMAGE_FORMAT` | The page is drawn without them |

```java
Converter converter = Converter.builder().exclude(Check.CONTRAST, Check.PDF_IMAGE_ANNOTATIONS).build();
```

`Engine` receives the removed ones and passes them to `Images`, and the contrast is checked by `Converter.layOut` after
layout.

> **Decision:** a single way to remove checks, an enum and `exclude`, instead of one boolean for each: by
> default everything is done and what is removed is named. *Rejected alternative:* `checkContrast(boolean)` and one more method for
> each new check.

> **Decision:** what prevents reading a PDF (encrypted, damaged, without the page) cannot be removed: it is not about what is seen, but
> there is nothing to draw. *Rejected alternative:* one more check for the encrypted PDF.

## Separate styles

```java
InlineNode styles = new DocumentReader().readStyles(Path.of("styles.stxt"));
Converter converter = Converter.builder().styles(styles).build();
converter.toPdf(doc, out);
```

Underneath, `BoxTreeBuilder.build(document, styles)` and `Engine.layOutDocument(document, styles)` receive it in the same way. In
those two it can be `null`. See [Styles](../common/styles.md#separate-styles-document).

## Resources

`new Resources(Path.of("other"))` uses another resources folder. Every path goes through `Resources.resolve`, which never goes
backwards. `Resources` is shared across threads and across conversions. It indexes `fonts/` once and keeps the images it has read.

`FontLibrary` identifies each TTF by its contents:

- **family:** the typographic family of the `name` table (`nameID` 16) or, if it has none, the family (`nameID` 1);
- **weight:** `usWeightClass` of `OS/2` (400 or 700; the others are ignored without error);
- **style:** the italic bit of `fsSelection`.

> **Decision:** the fonts are only checked with `PdfMetrics` (`Engine.layOutDocument` calls `validate`). With
> `FixedMetrics` the fonts folder is not looked at: it is enough for `Font` to be declared. *Rejected alternative:* always validating
> the fonts when building the tree, which would require the folder also to lay out without PDF.

## Errors

Every error of the engine is a `StxtPdfException` (thrown by `DocumentReader` and `Converter`), so a single `catch` is enough.
`kind()` says whose fault it is, `code()` what fails and `line()` where (`NO_LINE`, 0, if it does not come from a line).
`inStyles()` says whether that line belongs to the [separate styles document](../common/styles.md#separate-styles-document) and not to the document:

```java
try {
    converter.toPdf(doc, out);
} catch (StxtPdfException e) {
    switch (e.kind()) {
        case INVALID_DOCUMENT -> reply(422, e.code() + " on line " + e.line()
                + (e.inStyles() ? " of the styles" : "") + ": " + e.getMessage());
        case LIMIT -> reply(413, e.getMessage());
        case RESOURCE, INTERNAL -> reply(500, e.getMessage());   // from the configuration or from the engine
    }
}
```

| Class | What it is |
|---|---|
| `InvalidModelException` (`tree`) | The rules of the model, the overflows and the resources the document names. `errorCode()` gives the `ErrorCode` and `ErrorCode.kind()` its kind (`INVALID_DOCUMENT` except `PAGE_LIMIT`, `IMAGE_TOO_LARGE`, `SVG_TOO_COMPLEX` and `PDF_TOO_LARGE`, which are `LIMIT`) |
| `DocumentSyntaxException` (`tree`) | The syntax and the schema of `stxt-core`, with its code and its line and the original exception as the cause. A `LimitException` from the library is `LIMIT` |
| `ResourceException` (`resources`) | The resources folder (`RESOURCE`), with `ImageFormatException` |
| `InternalException` (`model`) | A failure of the engine. The original cause is in `getCause()` |

Input and output failures when writing to the destination remain the `IOException` of `toPdf`. `Converter` lets
the errors of the engine through, turns any other runtime exception into an `InternalException` and does not touch the `IOException`
of the destination. An `IllegalStateException` or `IllegalArgumentException` is only valid inside the engine for internal
misuse. What can happen to the caller comes with one of these types. The codes and the kinds are in
[Errors](../common/errors.md).

> **Decision:** `code()` is the code as text, common to all of them, and the enum moves to `errorCode()` in `InvalidModelException`:
> the errors of the library have an open set of codes that do not fit in `ErrorCode`. *Rejected alternative:*
> adding each code of `stxt-core` to `ErrorCode`, which ties the engine to the list of the library.

> **Decision:** `Converter` is the boundary of the errors: it lets through those of the engine and the `IOException` of the destination, and
> turns any other runtime exception into an `InternalException` with the original as the cause, so a single
> `catch` also covers a defect of the engine or of PDFBox. *Rejected alternative:* also wrapping the `IOException`
> of the destination, which is not an error of the engine.

## Limits

`Limits` is immutable. `Limits.DEFAULT` has the default values ([Limits](../common/limits.md)) and is changed with
the `withMax...` methods:

```java
Converter converter = Converter.builder()
        .limits(Limits.DEFAULT.withMaxPages(500).withMaxImagePixels(60_000_000))
        .build();
```

It reaches the `Engine` (which passes it to `Images` and to the `Paginator`) and `PdfDocumentWriter.write` (the drawing limits of JSVG).
`Images.validate` checks them before layout with what `ImageFile` read without decoding anything: the pixels of the
header and, in an SVG, the elements and the levels that `SvgInspector` counts without recursion. The bytes of a PDF are checked by
`ImageLibrary.pdfBytes` before reading it with `find`, which opens it with PDFBox: an oversized PDF is rejected without opening it.

> **Decision:** the default values were chosen by measuring: a PNG with transparency of 144 Mpx (547 KB on disk) exhausts
> a 512 MB heap, one of 900 Mpx and 106 KB takes 23 s, 20,000 SVG levels overflow the JVM stack and a chain of
> `<use>` of 10⁹ elements exhausts the memory after 51 s. A higher set (10,000 pages, 100 Mpx, 100,000 elements and
> 256 levels) left a service exposed to each of them. *Rejected alternative:* system properties, or
> constants without configuration.

> **Decision:** the limits belong to each conversion and not to `Resources`: two conversions with different limits share the
> same resources. *Rejected alternative:* tying the image cache to a single set of limits.

> **Decision:** `SvgInspector` overcounts what JSVG will count, so what meets the limit does not fail when drawing.
> JSVG comes with its own limits (2000 elements and 30 levels, and 15 levels of nested `<use>`), which rejected legitimate
> SVGs: the engine gives it its own, and if an SVG still does not load it is `IMAGE_FORMAT`. *Rejected alternative:* letting
> JSVG count when drawing, which fails late, without a line and without a kind.

> **Decision:** the limit of a PDF is the bytes of the file, which are checked without opening it, and 20 MB by default: PDFBox reads
> its whole structure when opening it, and a plan or a letterhead fits with room to spare. *Rejected alternative:* counting its pages or
> its objects, which requires opening it before knowing whether it can be opened.

> **Decision:** the limits of the STXT text (10 MB, 100 levels, 10,000 characters per line) are those of `stxt-core`, which already
> gives a `LIMIT` error. `Limits` only has what the engine checks on its own.

## Threads

The recommended way is to create **one `Converter` and share it**: it is immutable and each conversion creates its own engine, so
two threads converting at the same time share no state that changes.

| Piece | Shared across threads? |
|---|---|
| `Converter`, `Resources`, `DocumentReader` | Yes. They are created once and used from all threads |
| `LaidOutDocument` (what `layOut` returns) | Yes, to paint it: the same laid-out document can be written to PDF and to SVG from several threads. It is handed to the other threads with a `Future`, an executor or a `volatile` field |
| `Engine` | **No.** It keeps the layout cache and the numbering state. One per conversion, and reusing it in sequence is safe (it releases everything at the end of each document) |
| `Box` and `Style` (the box tree) | Read-only once built |

`ConcurrentConversionTest` checks, with eight threads converting the eleven examples, that the PDF is the same, byte for byte, as
in the sequential conversion, and `EngineContractTest`, that a reused `Engine` gives the same as a new one. The text of an
SVG is drawn with the fonts of the resources folder without registering them in the JVM, so it depends neither on its locale nor on
other folders. JSVG draws it with AWT, so on a server without a display the JVM is started with
`-Djava.awt.headless=true`.

> **Decision:** the thread contract is written down and not only observed. The shared schema of `stxt-core` relies on two
> facts of version 1.0.3, checked in its bytecode: the schema provider only reads from a map and the validator only
> writes fields in its constructor. A change of version requires reviewing it (it is noted in the `pom.xml` and in
> `DocumentSchema`). *Rejected alternative:* one copy of the schema per thread.

## Validation in the editor

The STXT extension for VS Code loads the schemas of the `.stxt/` folder of the workspace. By copying the schema there
([`dev.stxt.pdf.stxt`](../../../../src/main/resources/stxt/dev.stxt.pdf.stxt)), documents get validation and contextual help
while they are written.
