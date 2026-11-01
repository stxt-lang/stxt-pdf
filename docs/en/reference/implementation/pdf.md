# The PDF inside

What the engine writes in the PDF and why: the structure tree of the tagged PDF, the *artifacts*, the link
annotations, the bookmarks and the metadata. What the reader of the document notices is in [The PDF](../common/pdf.md). How the
code writes it is in [Painting](painting.md#the-pdf).

## Pages, fonts and metadata

- Each page has the size of the `Page` of its section. The page labels (`PageLabels`) carry the printed number
  with its style (`D`, `r` or `R`), or none on the pages without a number, and are only written if some page is not its
  physical number in digits: a document numbered in the usual way does not have them.
- The fonts are those of the resources folder, in Type 0 with Identity-H. Those with TrueType outlines (the `.ttf` and
  the `.otf` that carry them) are embedded as a subset, with the glyphs that are used, in a `CIDFontType2` with the file in
  `FontFile2`. Those with CFF outlines (the `.otf` with a `CFF ` table) are embedded whole, in a `CIDFontType0` with the
  file in `FontFile3` of subtype `OpenType`: PDFBox cannot subset CFF, and it declares them as TrueType, so `PdfFonts`
  changes the subtype and the key of the file when loading them. The codes of the text are the glyph numbers in both: in a
  CFF that is not CID-keyed, like that of an `.otf`, a CID is the glyph number.
- `Title` goes to the document information and to the XMP metadata, with `DisplayDocTitle` so that the viewer shows it,
  and `Language` is the `Lang` of the catalog (`PdfMetadata`). Every PDF declares PDF/UA-1 (`pdfuaid:part`) in its XMP
  metadata, or PDF/UA-2 if the caller asks for it ([below](#pdfua-2)), and PDF/A if the caller asks for it
  ([below](#pdfa)).
- The PDF carries no dates, and its identifier (`/ID` of the trailer) comes from the content: the same document, with the
  same resources and options, gives the same bytes. `PdfId` saves the document with a placeholder identifier of zeros,
  computes the SHA-256 of what was saved and puts its first 16 bytes in place of the zeros, in both halves, without moving
  anything else.

> **Decision:** the identifier comes from the content and not from the time. This way a conversion can be repeated and
> compared byte for byte, and two different documents have different identifiers, which is what the standard asks for.
> *Rejected alternative:* the identifier that PDFBox computes from the time, which changes on every conversion.

> **Decision:** CFF fonts are embedded whole and declared as what they are. IBM Plex Sans SemiBold as an `.otf`
> (143 KB) adds about 90 KB to the PDF that uses it, against a few KB for a subset, and that is why the fonts of the
> repository are TTF. *Rejected alternative:* subsetting the CFF in the engine, which means rewriting the whole `CFF `
> table, or leaving them as PDFBox declares them, which is not what the standard says (ISO 32000-1, 9.9) even though
> viewers tolerate it.

> **Decision:** every PDF declares PDF/UA-1. veraPDF checks it only in part (not the reading order or the meaning of the
> texts), so it also relies on the manual review of the tagging. *Rejected alternative:* not declaring it.

## Structure tree

One element per box, in the order of the document, which is the reading order:

| Box | Element |
|---|---|
| The document | `Document` (the root) |
| `Section` | `Sect`, with `Lang` if its language changes |
| `Heading 1` to `Heading 6` | `H1` to `H6`, and their `Text`, `Span` |
| `Text`, `Paragraph` | `P` |
| `List`, `List item` | `L` and `LI`, with the marker in `Lbl` and the content in `LBody` |
| `Table` and its parts | `Table`, `THead`, `TBody`, `TFoot`, `TR`, and `TH` or `TD` |
| `Table of contents` and its entries | `TOC` and `TOCI`, with two `Span` (the title and the number) |
| `Image` with `Alt`, `Barcode` | `Figure`, with `Alt` and `BBox` |
| A box with `Link`, a text link | `Link` ([below](#links)) |
| The rest (such as `Block`, `Stack`, `Grid`, `Columns`, `Markdown` or the positioned elements) | `Div` |

What a `Markdown` generates hangs from its `Div` with its own elements. The content of a `Columns` hangs from its `Div`
column by column. The text of each box is marked content of its element, including its formatted runs, which have no
element of their own. A box split across pages has marked content on several.

> **Decision:** a `Text` is a `P` and not a `Span`: a `Span` is inline text and a screen reader cannot tell where
> it ends. Only the `Text` of a heading and the two pieces of a table of contents entry are `Span`. *Rejected alternative:*
> letting a property of the `Text` decide.

> **Decision:** the `Markdown` is a `Div`, like a `Block`, and what is generated hangs from it. *Rejected alternative:* having what is
> generated hang from its parent.

### Language

The `Language` of `Document` is the `Lang` of the catalog. A box that declares another one carries `Lang` in its element, only where the
language changes with respect to the outside, and the elements inside inherit it.

> **Decision:** the language belongs to the box and not to a run: one language per run would split the marked content.

### Text that is read

An element with `Case` or `Small caps` carries `ActualText` with the written text, so that a screen reader reads
"Emisor" and not "EMISOR", unless its text contains `{page}`, `{pages}` or a reference, which change with the page.

> **Decision:** no `ActualText` in a text with `{page}` or references. *Rejected alternative:* giving the replaced text
> of each fragment, which does not fit in an element split across pages.

A normal text line that is not the last of its box (or of its fragment) ends with an invisible space after the last
word: the line break swallows the space, and without it whoever joins the text of the lines (a screen reader,
extraction, search) would glue the words together. It changes neither the width of the line nor the layout. The text of `Preformatted: true`
and `wrap` does not have it: its lines are lines.

> **Decision:** the invisible space at the end of each broken line. *Rejected alternative:* leaving it to the geometry,
> as some extractors do.

A line split inside a word (`Hyphenate` or a soft hyphen,
[Typography](../common/typography.md#hyphenation-hyphenate)) ends with the painted hyphen and **without** the
invisible space, and the sequence of runs that ends it goes in a `Span` with `ActualText` in which the hyphen is the
soft hyphen U+00AD: it is how a tagged PDF says that the hyphen was added by the line breaking, so whoever extracts the
text does not see the hyphen and can join the two parts. PDFBox drops it when extracting and gives each part on its
line; Acrobat copies the soft hyphen, which is not seen.

> **Decision:** `ActualText` with the soft hyphen in the sequence that ends the line, and neither an empty
> `ActualText` nor the whole word in both parts: the first would lose the signal of the break and the second would
> duplicate it when extracting.

With `Letter spacing`, the characters of a text are painted apart, and whoever extracts the text by looking at where each
one is guesses spaces between them (with PDFBox, `I n f o r m e` from 1.5 pt at a size of 10). That is why each sequence
of consecutive runs of a line with the same link goes in a `Span` marked content with `ActualText`: its text, with the
invisible space at the end if it reaches the end of the line. Acrobat, poppler (`pdftotext`) and PDFBox respect it, the last one leaving
spaces after it. It is not in the structure: that of an element with `Case` is still that of the written text.

> **Decision:** one `ActualText` per sequence of runs and line. *Rejected alternative:* one per run, which leaves between
> two runs the gap of the letter spacing and makes an extra space be guessed, or `Tc` instead of `TJ`, which spaces the
> characters apart in the same way and which the extractors treat the same.

### Lists

`L` carries the attribute `ListNumbering` according to the `Marker`: `Disc`, `Decimal`, `LowerAlpha`, `UpperAlpha`, `LowerRoman`,
`UpperRoman`, and `None` for `dash` and `none`.

> **Decision:** `dash` is `ListNumbering: None`, because PDF/UA-1 has no value for the dash. *Rejected
> alternative:* `Unordered`, from PDF 2.0, which is used with PDF/UA-2 ([below](#pdfua-2)).

### Tables

A cell of `Header rows` is `TH` with `Scope: Column`, one of `Body` or `Footer rows` with `Row header: true` is `TH` with
`Scope: Row`, and the rest, `TD`. A cell that spans several rows or columns carries `RowSpan` and `ColSpan`. The header and
the footer are content on the first page on which each cell appears. Where they repeat, they are *artifacts*, so the tree
has a single `THead` and a single `TFoot`.

> **Decision:** the repetitions are *artifacts*, and a screen reader does not read the header on each page.
> *Rejected alternative:* marking each repetition as content of the same cell.

### Figures

The `BBox` of the `Figure` of an image is its content box on the page and not the visible part with `Fit: inside`. That of
a rotated image is the rectangle that encloses it.

### Tables of contents

Each entry is a `TOCI` with the title and the number in two `Span`, and the leader is an *artifact*. The `Link` of the entry
wraps the two `Span`.

## *Artifacts*

They are not part of the content: backgrounds, borders, gradients, text lines (underline and strikethrough), the leaders
of a table of contents, the lines between columns, images without `Alt`, the repeated rows of a table from its second page on,
and everything inside a `Page overlay`.

## Links

Each area is a `Link` annotation without a border, with the `URI` action or the destination on the page, and as text (`Contents`) what
the box or the run says: the text of the box and of what it contains joined by spaces, or the `Alt` of an image. Without
text, it carries the destination itself. The references and the page markers of the text come out without being replaced. Internal destinations
are resolved after all the pages have been painted, so a link can point forward. The pages with annotations
carry `Tabs: S`, so that the viewer goes through the links in the order of the structure.

In a rotated box, the annotation has as `Rect` the rectangle that encloses the rotated area and as `QuadPoints` the rotated
area. The destination of a rotated box is the rectangle that encloses it.

The `Link` element wraps what is clicked and carries each annotation as an object reference (`OBJR`), as PDF/UA requires:

- *Box with `Link`:* the `Link` is a child of the element of the box and contains its content (`P → Link → text`,
  `Div → Link → P, P` or `TOCI → Link → title, number`). An image is the exception: its `Link` is the parent of its `Figure`
  (`Link → Figure`), because a `Figure` cannot contain its `Link`. A split box has a single `Link` with one `OBJR`
  per fragment.
- *Link inside the text:* the `Link` is a child of the element of the text (`P → text, Link, text`) and contains the marked
  content of the words of the link, which are painted in separate sequences. There is one per link and fragment, even if it spans
  several lines: one marked content and one `OBJR` per line. Two consecutive links with the same destination are one.
- *`Alt`:* a `Link` that contains something to read (text or a figure with its `Alt`) does not carry `Alt`, because the reader would read it
  instead of the content. One with nothing inside carries it with the text of the link.
- *What is not content* (a `Page overlay`, a decorative image) has nothing to wrap, but PDF/UA requires every
  link annotation to be in a `Link`. Each of its links (that of a box and one per destination of its text links) is a
  `Link` without content at the place of the box in the tree, with the text of the link as `Alt` and one `OBJR` per page on
  which it appears: a footer with a website is announced once, where the `Page overlay` is in the document. If the `Page overlay` does not appear on
  any page, there is no `Link`.
- *The repeated rows of a table:* their annotations go to the `Link` of the first page, the one that wraps the text of the
  cell.

> **Decision:** the `Link` wraps what is clicked. *Rejected alternative:* a `Link` with only the reference to the
> annotation, which does not conform to PDF/UA.

> **Decision:** a link on several lines is a single `Link` (the annotations are still one per line, which is what the
> viewer needs): a screen reader announces one link and not one per line.

> **Decision:** the link of what is not content is a `Link` without content at the place of the box, with the annotations
> of all the pages. *Rejected alternatives:* a loose `Link` of the `Document` per page, which a screen reader
> announces at the end of the structure and as many times as there are pages, or marking those annotations as hidden, which takes them away from the
> mouse.

## Bookmarks

One per heading (also those of a `Markdown`), with the text of its `Text` joined by spaces (with its number, if it
has one), nested by level and with a destination at the top left corner of its first fragment. The catalog asks the viewer
to show them when the document is opened (`PageMode: UseOutlines`).

## PDF/UA-2

With `Converter.builder().pdfUa(2)`, the PDF is PDF 2.0 (in the header of the file and in the `Version` of the catalog) and conforms to
PDF/UA-2 (ISO 14289-2), with `pdfuaid:part` 2 and `pdfuaid:rev` 2024 in the XMP metadata. Everything above applies in the same way, with
these differences:

- **Namespace:** the structure tree declares that of PDF 2.0 (`Namespaces`, `http://iso.org/pdf2/ssn`) and each
  element carries it in `NS`, except `TOC`, `TOCI` and `Reference`, which do not exist in PDF 2.0 and stay in that of PDF 1.7, the
  default one.
- **Tables of contents:** a `TOCI` cannot contain a `Link`: it is `TOCI → Reference → Link → title, number`, and the `TOCI` points with
  `Ref` to the element of the heading it leads to.
- **Structure destinations:** a link to a box of the document and a bookmark are a `GoTo` action with the element of the
  box in `SD` (or that of its nearest ancestor that has an element) and the same position. `D`, with the page, remains for the
  viewers that do not understand `SD`.
- **Lists:** `dash` is `ListNumbering: Unordered`.
- **Links with nothing to read** (those that carry `Alt`): their annotation does not carry `Contents`, because what is read is the `Alt`.

veraPDF validates it with its PDF/UA-2 profile, which adds the rules of ISO/TS 32005 on which element can contain
which.

> **Decision:** PDF/UA-2 is an option and PDF/UA-1 the usual: not every viewer opens PDF 2.0 well yet.
> *Rejected alternative:* moving everything to PDF/UA-2.

> **Decision:** the links with `Alt` do not carry `Contents` in PDF/UA-2. The standard requires that, if both are present, they say the
> same, and they do, but veraPDF compares the `Alt` without decoding when the text has accents and considers it different.
> *Rejected alternative:* removing the `Alt`, which is what a screen reader reads in an empty `Link`.

## PDF/A

With `Converter.builder().pdfA(...)` (`PdfA.A2B`, `A2A`, `A3B` or `A3A`), the PDF conforms to that part and level of
ISO 19005 besides PDF/UA-1. `PdfMetadata` writes what the standard asks for, and the rest of the PDF does not change:

- **XMP metadata:** `pdfaid:part` (2 or 3) and `pdfaid:conformance` (`A` or `B`) in their `rdf:Description`. PDF/A only
  allows in the packet the schemas it predefines or those the packet itself describes, and `pdfuaid` is not among the
  former, so the packet also carries a `pdfaExtension:schemas` with the description of the PDF/UA identification
  schema (`part`, an integer; the one the PDF Association gives).
- **Output intent:** an `OutputIntent` of subtype `GTS_PDFA1` with the sRGB profile of the ICC (`sRGB2014.icc`, 3 KB,
  [Resources](../common/resources.md#color-profile)) as `DestOutputProfile` and `sRGB IEC61966-2.1` as the condition.
  Every color of the PDF is `DeviceRGB` or `DeviceGray`, and with an RGB output profile both are allowed.
- **What it already met:** the fonts embedded (the subset ones, with `CIDSet`) and with `ToUnicode`; the transparency
  declared with `ca` in a graphics state and the soft masks of the gradients, allowed since PDF/A-2; the link
  annotations with the print flag and without an appearance, which PDF/A-2 does not require of a `Link`; and no
  JavaScript, encryption or forms. An SVG and the page of a PDF used as images are drawn with the same.
- **What is not checked:** the page of a PDF used as an image is copied as it is, with its colors
  (`Check.PDF_IMAGE_FONTS` looks at its fonts, not at its color spaces): a page with `DeviceCMYK`, or a CMYK JPEG, would
  give a PDF that declares PDF/A without conforming to it.

PDF/UA-2 is PDF 2.0 and PDF/A-2 and PDF/A-3 are PDF 1.7: `Converter.build()` and `PdfDocumentWriter.write` reject the
combination (`IllegalArgumentException`). PDF/A-4, on PDF 2.0, waits until PDF/UA-2 is the usual one.

veraPDF validates it with its `2b`, `2a`, `3b` and `3a` profiles on the eleven examples
([Tests](testing.md#examples-examplestest)) and in `PdfATest`, on a document with what PDF/A restricts: opacities, a
gradient with opacities, links, a table of contents, PNG, JPEG, an SVG with text, the page of a PDF and a QR code.

> **Decision:** the sRGB profile goes inside the jar and is always the same (`sRGB2014.icc`): the same document gives
> the same bytes on any JVM. *Rejected alternative:* the sRGB profile of the JDK (`ICC_Profile`), which changes with the
> version and the vendor of the JDK, or the one of colord (CC0), which weighs 16 KB.

> **Decision:** the `pdfuaid` schema is described as an extension schema only with PDF/A, and only with `part`:
> PDF/UA-2 adds `rev`, but it is PDF 2.0 and does not go with PDF/A-2 or PDF/A-3. *Rejected alternative:* describing it
> always, which PDF/UA does not ask for.

### Embedded files

`PdfAttachments` writes each `Attachment` as a file specification (`Filespec`) with the file embedded in `EF` (`F` and
`UF`, the same stream, Flate-compressed, with the type as `Subtype` and, in `Params`, `Size` and, if given, `ModDate`),
the name in `F` and `UF`, the relationship in `AFRelationship` and the description in `Desc`. All of them go in the
`EmbeddedFiles` name tree of the catalog, where the viewer finds them, and in the `AF` array of the catalog, which makes
them associated files of the document, as PDF/A-3 and Factur-X ask.

The XMP of `Attachments.withXmp` is written after the engine's own, as it is except for its `pdfaExtension:schemas`: an
XMP packet only allows that property once, so its schema descriptions move to the engine's bag, next to that of
`pdfuaid`, and the rest of each `rdf:Description` stays where it was. `Xmp` (`model`) does it: it parses the fragment
with the XML parser of the JDK, without DTD or entities, and writes it again declaring in each element the namespaces it
needs. A fragment that is not XML is an `IllegalArgumentException` when the `Attachments` are built.

> **Decision:** the XMP fragment is passed as text and the engine rearranges it, instead of asking for it in parts: the
> Factur-X specification gives the whole fragment, with its extension schemas, and so it is copied as it is.
> *Rejected alternative:* one call per property and another per schema, or `xmpbox`, which rewrites the packet its own
> way.

> **Decision:** an `Attachment` without a modification date carries no `ModDate`: the PDF carries no dates and the
> conversion can be repeated byte for byte. Whoever needs it gives it with `withModified`. *Rejected alternative:* the
> time of the conversion.

## Images

The PDF embeds each file only once, even if it is used on several pages, with the resolution of the file: the JPEG as
it is and the PNG lossless and with its transparency. An SVG is a form (*XObject*) of its natural size, drawn once
per file, which is scaled to its box with `Fit` like a PNG ([Painting](painting.md#images)). The page of a PDF is also
a form, copied once per page: its content and its resources, without its annotations or its tagging.
