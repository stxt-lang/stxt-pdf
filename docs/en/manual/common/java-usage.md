# Using from Java

The engine is a Java 17 library (Maven: `dev.stxt:stxt-pdf`).

## Converting a document

```java
InlineNode doc = new DocumentReader().read(Path.of("report.stxt"));
try (OutputStream out = Files.newOutputStream(Path.of("report.pdf"))) {
    Converter.create().toPdf(doc, out);
}
```

`DocumentReader` reads and validates the document. `Converter` lays it out and writes the PDF. A `Converter` is created
once and can be used from several threads at the same time.

## Options

```java
Converter converter = Converter.builder()
        .resources(new Resources(Path.of("my-resources")))   // another resources folder
        .styles(Path.of("styles.stxt"))                       // a separate styles file
        .limits(Limits.DEFAULT.withMaxPages(500))             // other limits
        .build();
converter.toPdf(doc, out);
```

| Option | Default |
|---|---|
| `resources` | The `resources/` folder of the working directory ([Resources](resources.md)) |
| `styles` | None. The separate styles file of the document ([Styles](styles.md#styles-in-a-separate-file)) |
| `limits` | 2,000 pages, 25 million pixels per image, 20,000 elements and 64 levels per SVG and 20 MB per PDF |
| `debug` | `false`. With `true`, it paints the outline of each box |
| `pdfUa` | `1`: the PDF complies with PDF/UA-1. With `2`, PDF/UA-2, in PDF 2.0, which some old viewers do not open |
| `pdfA` | None. With `PdfA.A2B`, `A2A`, `A3B` or `A3A`, the PDF also complies with that PDF/A, the archiving standard ([below](#pdfa-and-attachments)) |
| `exclude` | None: everything is checked. It removes the checks it is given (below) |

By default everything is checked. With `exclude` checks are removed, and what they detect is no longer an error:

```java
Converter converter = Converter.builder().exclude(Check.CONTRAST).build();
```

| Check | What is an error if it is not removed |
|---|---|
| `Check.CONTRAST` | A text that barely stands out from its background ([Box](box.md#contrast)) |
| `Check.PDF_IMAGE_FONTS` | A page of a PDF with a font that does not comply with PDF/UA, such as one that is not embedded or that does not say which character each glyph is ([Image](../components/image.md#a-page-of-a-pdf)) |
| `Check.PDF_IMAGE_ANNOTATIONS` | A page of a PDF with a visible form field or comment, which would not be drawn |

## PDF/A and attachments

```java
Converter converter = Converter.builder().pdfA(PdfA.A3B).build();
Attachment xml = Attachment.of("factur-x.xml", bytes, "text/xml", Attachment.Relationship.ALTERNATIVE);
converter.toPdf(doc, out, Attachments.of(xml).withXmp(facturXmp));
```

With `pdfA`, the PDF declares PDF/A and carries what the standard asks for: a color profile and its metadata. PDF/A-3
(`A3B`, `A3A`) also allows embedded files, which is how a Factur-X electronic invoice is made: the XML of the invoice
goes inside the PDF. The attachments are passed with each call to `toPdf`, with their name, their bytes, their type and
their relationship with the document (`ALTERNATIVE` for the XML of an invoice), and `withXmp` adds the metadata
fragment Factur-X requires, as its specification gives it. Without PDF/A-3, an attachment is an error
(`ATTACHMENT_REQUIRES_PDFA3`).

## Errors

Every error is a `StxtPdfException`, with three pieces of data: `kind()` (whose fault it is), `code()` (the code) and
`line()` (the line, 0 if it does not come from a line). If the line is from the separate styles file, `inStyles()` is
`true`.

```java
try {
    converter.toPdf(doc, out);
} catch (StxtPdfException e) {
    switch (e.kind()) {
        case INVALID_DOCUMENT -> System.err.println(e.getMessage());   // the document has an error
        case LIMIT -> System.err.println("Too large: " + e.getMessage());
        case RESOURCE, INTERNAL -> throw e;                            // the configuration or the engine
    }
}
```

## More

- To use the pages before painting them, or to also write an SVG of each page: `converter.layOut(doc)` and
  `converter.toSvg(...)`.
- On a server without a display, the JVM is started with `-Djava.awt.headless=true`.

The detail (threads, error types, limits) is in the [reference](../../reference/implementation/api.md).
