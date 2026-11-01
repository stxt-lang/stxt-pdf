# The PDF

What every PDF written by the engine carries. How it is built inside (the structure tree, the annotations, the metadata)
is in [The PDF inside](../implementation/pdf.md).

## Pages

Each page has the size of the `Page` of its section ([Document](../components/document.md#page)). The viewer shows the
printed number of each page (that of `{page}`, also in Roman numerals) instead of its position in the file, and none on
pages without a number. A blank page from `Begin on: odd` shows the number it would have, even though it does not print
it.

## Fonts

The fonts are those of the resources folder ([Resources](resources.md#fonts)), embedded with the characters that are
used: the PDF looks the same everywhere. The text is text: it can be selected, copied and searched.

## Accessibility

The PDF is **tagged**: besides what is seen, it carries the structure of the document, which is what a screen reader uses
and what gives the text when it is copied or when the view size changes.

- **Reading order:** that of the document. Each box is an element: headings are announced as headings of their level,
  lists as lists with their number of items, tables as tables with their headers, images with `Alt` as figures, and a
  `Table of contents` as a table of contents.
- **Decoration:** backgrounds, borders, text lines, the leaders of a table of contents, the lines between columns, images
  without `Alt`, the repeated rows of a table and everything in a `Page overlay` are not read.
- **Language:** the `Language` of `Document` is that of all the content. A box that declares another one is read in that
  one ([Typography](typography.md#language-language)).
- **Text that is read:** the written text, also with `Case` and `Small caps`
  ([Text formatting](text-formatting.md#capitalization-case)), and the words of two consecutive lines are not glued together
  when the text is copied.
- **Links:** they are announced with what they link ([Links](links.md#accessibility)).
- **Contrast:** the text is distinguishable from what is under it ([below](#contrast)).

`Title` and `Language` are required in `Document` ([Document](../components/document.md#title-and-language)), so every PDF
declares conformance to **PDF/UA-1**, the accessible PDF standard. The caller can request **PDF/UA-2**, its new version,
which writes a PDF 2.0 ([Using from Java](../implementation/api.md#converting)). What changes inside is in
[The PDF inside](../implementation/pdf.md#pdfua-2). The tests check both with veraPDF.

> **Decision:** `Title` and `Language` are required: the language is what a screen reader needs to read the content, and
> PDF/UA requires both. *Rejected alternative:* leaving them optional and not declaring PDF/UA without them.

## Contrast

When converting, the engine checks that each text is distinguishable from what is under it, with the contrast of the WCAG
accessibility guidelines (level AA): at least **4.5:1**, or **3:1** for large text (`Size` of 18 or more, or of 14 in
bold, which is weight 700 or more). If it falls short, it is an error (`CONTRAST_LOW`) with the line of the `Color` of the text, also if it is
inherited (without `Color`, the usual black, the line of the box).

- **What is under it** is what has been painted before the text at the point where it starts: the backgrounds
  (`Background`) of its box, of the ones outside it and of the ones behind it, blended with their opacity over the white
  of the page. Boxes that are not under that point do not count, nor do borders.
- **A gradient:** each of its colors counts, and the worst one applies.
- **A text with opacity** (`Color: #00000080`) is blended with what is under it before comparing.
- **On top of an image** it is not known what is underneath, so that text is not checked, unless an opaque background
  covers the image.
- All the text that is painted is checked, also that of a `Page overlay` and that of the entries of a table of contents.

The caller can turn the check off ([Using from Java](../implementation/api.md#converting)). It is on by default.

> **Decision:** contrast is checked on what is painted and not on the box tree: a text over a box that is not its own (an
> `Overlay` on top of a background) has that background under it. *Rejected alternative:* checking only the backgrounds
> of the box and of the ones outside it, which took what is under an `Overlay` over a photo to be white.

> **Decision:** text over an image is not checked. *Rejected alternative:* treating it as an error, which prevents the
> cover with the title over a photo even if it reads well.

> **Decision:** the point where each run of text starts is checked, not its whole surface. *Rejected alternative:* the
> worst color of everything the run touches, which requires measuring the text and counts the underline of the text
> itself as background.

> **Decision:** the check is on by default and is turned off when converting, not in the document. *Rejected
> alternative:* a property of the document, which would leave it to whoever writes and not to whoever publishes the PDF.

## Bookmarks

One per heading, with its text (and its number, if it has one), nested by level, and leading to the heading. The viewer
shows them when the document is opened.

## Title

`Title` is what the viewer shows instead of the file name.

## PDF/A

PDF/A is the PDF standard for archiving (ISO 19005), the one archives, public administrations and electronic invoicing
ask for. The caller requests it with `Converter.builder().pdfA(PdfA.A3B)`
([Using from Java](../implementation/api.md#pdfa-and-attachments)); by default the PDF does not declare it. There are
four options, parts 2 and 3 in their levels `b` and `a` (`A2B`, `A2A`, `A3B`, `A3A`):

- **PDF/A-2 and PDF/A-3** are PDF 1.7, like PDF/UA-1, so the PDF conforms to both standards at once. PDF/A-3 is PDF/A-2
  plus [embedded files](#embedded-files-factur-x).
- **Level `b`** promises that the PDF will look the same years from now: embedded fonts, defined colors, no encryption
  and no code. **Level `a`** adds the tagging and the machine-readable text, which every PDF of the engine already has:
  the two levels give the same PDF with a different declaration.

PDF/A adds to the PDF a 3 KB sRGB color profile and the metadata the standard asks for
([The PDF inside](../implementation/pdf.md#pdfa)). Every PDF of the engine already meets the rest: the fonts are
embedded, the colors are defined, and there is no JavaScript, encryption or forms. The tests validate the eleven
examples with veraPDF in the four variants.

PDF/UA-2 writes a PDF 2.0, and PDF/A-2 and PDF/A-3 are PDF 1.7: asking for both is an `IllegalArgumentException`.
PDF/A-4, on PDF 2.0, is not done.

> **Decision:** PDF/A is requested when converting and not in the document, like PDF/UA-2 and the contrast: it is a
> decision of whoever publishes. *Rejected alternative:* a property of `Document`.

> **Decision:** only parts 2 and 3. PDF/A-1 does not allow transparency, which the engine uses in the opacities and the
> gradients, and PDF/A-4 is PDF 2.0, which not every viewer opens well yet. *Rejected alternative:* PDF/A-1b, which
> would force rejecting the documents with opacity.

### Embedded files (Factur-X)

With PDF/A-3, the PDF carries files inside. It is how the Factur-X / ZUGFeRD electronic invoice works: a PDF/A-3 with
the XML of the invoice embedded. The attachments are passed with each call, because they belong to each document and
not to the configuration:

```java
Attachment xml = Attachment.of("factur-x.xml", bytes, "text/xml", Attachment.Relationship.ALTERNATIVE)
        .withDescription("Factur-X invoice");
converter.toPdf(document, out, Attachments.of(xml).withXmp(facturXmp));
```

Each attachment has a name (unique in the PDF; the viewer shows it), its content, its type (`text/xml`,
`application/pdf`…) and its relationship with the document: `DATA` (the data the document shows), `ALTERNATIVE`
(another representation of the document, such as the XML of an invoice), `SOURCE` (what it was made from) or
`SUPPLEMENT` (what completes it). It can carry a description and a modification date; without a date, the PDF still
carries none and the same document gives the same bytes. `withXmp` adds the XMP metadata that goes with the
attachments: Factur-X requires some (the file name, the document type, the version and the profile) and its
specification gives the fragment, which is passed as it is. The engine knows nothing about invoices: it does not
generate the XML or its metadata, it only embeds them.

An attachment in a PDF that is not PDF/A-3 is an error, `ATTACHMENT_REQUIRES_PDFA3` ([Errors](errors.md#codes)):
PDF/A-2 only allows embedding other PDF/A files.

> **Decision:** the attachments go in the call to `toPdf` and not in the `Converter`: the `Converter` is created once
> and shared, and the XML of an invoice belongs to that invoice. *Rejected alternative:* `attach(...)` in the
> `builder`, which forces one `Converter` per document.

> **Decision:** an attachment without PDF/A-3 is an error and not a PDF with the file inside: in PDF/A-2 it is
> forbidden, and outside PDF/A it would be one more case nobody asks for. *Rejected alternative:* allowing it in any
> PDF, which PDF 1.7 permits.

> **Decision:** the engine embeds the XML it is given and writes the metadata it is given: Factur-X has profiles and
> versions, and the invoices belong to the caller. *Rejected alternative:* `facturX(xml, profile)`, which would tie the
> engine to one version of the specification.

## What is not supported

- PDF/A-1 and PDF/A-4 ([above](#pdfa)).
- Language per run within a line ([Typography](typography.md#language-language)).
- Forms, digital signatures and encryption. Attachments outside PDF/A-3.
- Optional content layers of its own: only those of the page of a PDF used as an image are kept
  ([Image](../components/image.md#pdf)).
