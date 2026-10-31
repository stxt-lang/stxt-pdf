# The PDF inside

What the engine writes in the PDF and why: the structure tree of the tagged PDF, the *artifacts*, the link
annotations, the bookmarks and the metadata. What the reader of the document notices is in [The PDF](../common/pdf.md). How the
code writes it is in [Painting](painting.md#the-pdf).

## Pages, fonts and metadata

- Each page has the size of the `Page` of its section. The page labels (`PageLabels`) carry the printed number
  with its style (`D`, `r` or `R`), or none on the pages without a number, and are only written if some page is not its
  physical number in digits: a document numbered in the usual way does not have them.
- The fonts are the TTF of the resources folder, embedded as a subset (Type 0, Identity-H) with the glyphs that
  are used.
- `Title` goes to the document information and to the XMP metadata, with `DisplayDocTitle` so that the viewer shows it,
  and `Language` is the `Lang` of the catalog. Every PDF declares PDF/UA-1 (`pdfuaid:part`) in its XMP metadata, or PDF/UA-2 if
  the caller asks for it ([below](#pdfua-2)).
- The PDF carries no dates, and its identifier (`/ID` of the trailer) comes from the content: the same document, with the
  same resources and options, gives the same bytes. `PdfId` saves the document with a placeholder identifier of zeros,
  computes the SHA-256 of what was saved and puts its first 16 bytes in place of the zeros, in both halves, without moving
  anything else.

> **Decision:** the identifier comes from the content and not from the time. This way a conversion can be repeated and
> compared byte for byte, and two different documents have different identifiers, which is what the standard asks for.
> *Rejected alternative:* the identifier that PDFBox computes from the time, which changes on every conversion.

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
| `Image` with `Alt` | `Figure`, with `Alt` and `BBox` |
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

## Images

The PDF embeds each file only once, even if it is used on several pages, with the resolution of the file: the JPEG as
it is and the PNG lossless and with its transparency. An SVG is a form (*XObject*) of its natural size, drawn once
per file, which is scaled to its box with `Fit` like a PNG ([Painting](painting.md#images)). The page of a PDF is also
a form, copied once per page: its content and its resources, without its annotations or its tagging.
