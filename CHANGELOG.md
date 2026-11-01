# Change Log

All notable changes to `dev.stxt:stxt-pdf` are documented in this file, newest first.

The format follows [Keep a Changelog](http://keepachangelog.com/). While the version starts with `0.`, the names of the
`dev.stxt.pdf` namespace and the Java API may change between minor versions.

## [Unreleased]

- **Hyphenation.** `Hyphenate: true` in `Typography` (inherited, `false` by default) splits words at the end of a
  line with a hyphen, following the TeX patterns (hyph-utf8) of the language of the box with Liang's algorithm and no
  dependencies: Spanish, Catalan, American English and British English, as resources of the jar (their licenses are
  in `docs/*/reference/common/resources.md`). Both `Line breaking` algorithms use the break points (`balanced`, with
  TeX's hyphen penalties), and the narrowest width of a text counts the parts of its words. A soft hyphen (U+00AD) in
  the text is always a break point, with or without `Hyphenate`, and is never painted where the word is not split. In
  the tagged PDF the hyphen of a split word is read as a soft hyphen (`ActualText`), so the copied text carries no
  hyphens and the two parts are one word. `Hyphenate: true` on a text in another language is
  `HYPHENATION_LANGUAGE_UNSUPPORTED`, and a font without the hyphen, `GLYPH_MISSING`.
- The newsletter example (`test/examples/05-newsletter`) is hyphenated.
- **Font weights.** `Weight` in `Typography` (inherited, `400` by default) chooses the weight of the font from 100 to 900
  in steps of 100; another number is `VALUE_NOT_ALLOWED`, and a weight the family does not have, `FONT_VARIANT_MISSING`.
  `Bold` stays as another way of writing it (`true` is 700 and `false`, 400): the last declaration wins between rules,
  ancestors and the box, and both in the same `Typography` are `WEIGHT_CONFLICT` (new code). A bold run has weight 700,
  or that of the box if it is already 700 or more. The fonts folder indexes files of any weight (`usWeightClass` as it
  is), not only 400 and 700, and the variants of a family are listed in the messages from the lightest to the heaviest
  (`weight 300, normal, italic, weight 600, bold`). The text of an SVG image accepts `font-weight` from 100 to 900 in
  steps of 100 as well.
- **OTF fonts.** The fonts folder indexes `.otf` files too. Those with TrueType outlines are embedded as a subset, like
  a `.ttf`; those with CFF outlines, whole, as a `CIDFontType0` with the file in `FontFile3` (`OpenType`), because PDFBox
  cannot subset them. A font present as `.ttf` and as `.otf` is two files for the same font, a `RESOURCE` error.
- IBM Plex Sans Light (300), Medium (500) and SemiBold (600) are in `resources/fonts/`, and the résumé example
  (`test/examples/03-resume`) uses 300, 500 and 600.
- **Kerning.** `Kerning: true` in `Typography` (inherited, `false` by default) measures and paints the text with the
  kerning pairs of the font: those of the `kern` feature of its `GPOS` table, which the engine reads itself (pair
  adjustment lookups in both formats, also inside extension lookups), or, in a font without it, those of its `kern`
  table. A pair does not cross a space or the boundary between two runs; with `Ligatures: true` it is between the glyphs
  left after `GSUB`, never inside a ligature. The adjustments are rounded to thousandths of the font size and written as
  shifts of `TJ`, so what is measured is what is painted, and the embedded font and the copied text are the same as
  without kerning. A font without pairs (IBM Plex Mono) is painted as without `Kerning`; a `GPOS` or `kern` table that
  cannot be read is a `RESOURCE` error. The debugging SVG writes the pairs as `dx`.
- The annual report example (`test/examples/04-annual-report`) uses kerning.
- **QR codes and barcodes.** New element `Barcode`: an image that the engine generates from its data instead of reading
  it from a file, in the `Format` `qr` (by default), `code-128` or `ean-13`, encoded with ZXing `core` 3.5.4 (a new
  dependency, Apache 2.0, pure Java). The data goes in the value or, if it has several lines (the EPC payment QR code),
  in a `Content` block; a QR code with data outside ASCII goes in UTF-8 with its ECI designator, and an EAN-13 takes 12
  digits (the check digit is added) or 13 (it is checked). `Error correction` (`low`, `medium` by default, `quartile`,
  `high`) only goes in a QR code. It is laid out and broken like an `Image`, with a natural size that comes from its
  modules and includes the quiet zone (0.5 mm per module in a QR code; 0.33 × 22.85 mm in the barcodes); a QR code is
  drawn square and centered in its box, and a barcode is stretched to it. The modules have the `Color` of the
  typography, the only property its `Typography` accepts, and are painted as a single vector path. `Alt` is required:
  it is a `Figure` of the tagged PDF. New codes `BARCODE_DATA`, `BARCODE_WITHOUT_ALT` and
  `BARCODE_PROPERTY_WITHOUT_EFFECT`; `Keep together` and `Edges` in a `Barcode` are `BREAK_IN_IMAGE`, as in an `Image`.
- The invoice example (`test/examples/01-invoice`) carries a payment QR code, and the certificate
  (`test/examples/10-certificate`), a QR code with its verification address.
- **Named colors.** `Named color: brand #12315A`, a repeatable child of `Styles`, gives a name to a hexadecimal color,
  and the name is then written wherever a color goes: `Color`, `Border color`, `Background` (also inside `linear(...)`)
  and `Separator color`, in the rules and in the elements. The name is replaced by its color when the document is read,
  so the PDF is the same as with the color written out. The names of the separate styles document and of the document
  form a single set, and the document wins: redeclaring a name in the document changes it also in the rules of the
  styles document. New codes `COLOR_DUPLICATE` (a name repeated in one source) and `COLOR_UNKNOWN` (a color with the form
  of a name that no `Named color` declares, so `Color: red` now gives this code instead of `VALUE_SYNTAX`); a malformed
  `Named color` is `VALUE_SYNTAX`, and the name `linear`, `VALUE_NOT_ALLOWED`.
- The annual report, newsletter and contract examples (`test/examples/04-annual-report`, `05-newsletter` and
  `07-contract`) declare their palette with `Named color`; their PDFs are byte for byte the same.
- **Running heads.** `{heading N}` (N from 1 to 6) in a text of a `Page overlay` writes the text of the last `Heading N`
  that has started on that page or before, with its number if it is numbered, and `{heading N: first}`, the first one
  that starts on the page (or the same as `{heading N}` if none does): the chapter or the section in the header of
  each page. A heading of a higher level leaves the levels below it empty until their first heading, and before the
  first heading of the level the text is empty and takes up no line. A `Page overlay` with a running head is laid out
  on each page with the heading of that page; the flow does not change. `Case` applies to it as to the rest of the
  text, and the font of the text needs the characters of every heading of that level. Outside a `Page overlay` it is
  `HEADING_REFERENCE_OUTSIDE_PAGE_OVERLAY` (new code), and a level or a variant that does not exist, `REFERENCE_UNKNOWN`.
- The manual example (`test/examples/08-manual`) writes the section in effect on each page in its header with
  `{heading 2}`.
- Lengths can be written in millimeters, with the unit attached to the number: `Margin: 20mm`, `Width: 42.5mm`,
  `Column sizes: 30mm *`. A number alone is still points, and both can be mixed (`Margin: 20mm 40`). Any other unit
  (`5cm`, `12pt`) and a unit separated from the number (`12 mm`) are `VALUE_SYNTAX`. `Grow` and `Shrink` are factors and
  do not allow a unit.
- **The ISO 216 paper sizes are exact in millimeters**: `Paper: A4` measures 210 × 297 mm, 595.28 × 841.89 pt. Before
  they were rounded to whole points (595 × 842), so a box with `Width: 210mm` did not fit in an A4. A document with
  `Paper` A3 to B5 now has a page up to half a point larger, and a box that measured the rounded page by hand
  (`Width: 595`, `Height: 842`) is smaller than it.
- The error messages give measurements with their unit: «it measures 600 pt and the content of its container measures
  515 pt».
- A child whose merged top margins push it past the end of the page is no longer laid out there: the block breaks
  before it. Before, an empty box with a fixed `Height` in that position was `PAGE_OVERFLOW` «it is 0 high».
- An empty box (no height, border, padding or content) never overflows vertically: its merged margins can leave it past
  the end of its parent's content, at the end of a block or just before a page break, and that was `BOX_OVERFLOW`.
- A `Frame` item of a `Stack` line or a `Grid` row that is split in parallel across pages is broken at its place in
  the flow, as in block flow: its `Offset` moves its fragments afterwards. Before, a vertical `Offset` took that much
  space away from the item on the first page, and its continuation lost the offset.
- The `After: page` (or `column`) of an empty box, and the forced break before the child that follows it, apply also
  when the empty box opens its block. Before, they were lost unless something with content had been placed before.
- The `Keep with next` of the last in-flow child of a block, and the `Keep with previous` of the first one, also apply to
  the block, like the forced breaks: a heading that ends a `Block` is no longer left alone at the end of the page.
- The height of the content of a box is never negative. A box whose negative margins take away more than its children
  measure measures its border and its padding, and the child that sticks out is `BOX_OVERFLOW` at the line of the
  negative `Margin`. Before, the box was lower than its own border and the message gave a negative height.
- An in-flow box that the `Offset` of a `Frame` or a negative `Margin` takes off the physical page is `BOX_OVERFLOW`, at
  the line of that `Offset` or `Margin`. Before, it was laid out without an error and did not show.

## [0.8.0] - 2026-10-31

First version published on Maven Central.

- Reads documents in the `dev.stxt.pdf` namespace, validates them against their `@stxt.schema` with `stxt-core` 1.0.3,
  lays them out on fixed-size pages and writes a tagged PDF (PDF/UA-1, or PDF/UA-2 with `pdfUa(2)`), with embedded
  fonts and one bookmark per heading.
- Elements: `Document`, `Section`, `Block`, `Text`, `Paragraph`, `Heading 1` to `Heading 6`, `List`, `Table`, `Stack`,
  `Grid`, `Columns`, `Frame`, `Overlay`, `Page overlay`, `Image` (PNG, JPEG, SVG and a page of a PDF), `Markdown` and
  `Table of contents`.
- Reusable styles (`Styles`, `For element`, `For style`), in the document or in a separate file.
- Internal references and links, heading and page numbering, rotation, rounded corners and balanced columns.
- The same document, with the same resources and options, gives the same bytes: the PDF carries no dates and its file
  identifier (`/ID`) is made from its content.
- Errors with a code and a line (`InvalidModelException` with its `ErrorCode`), a WCAG AA contrast check
  (`CONTRAST_LOW`) and limits for hostile input (`PAGE_LIMIT`, `IMAGE_TOO_LARGE`, `SVG_TOO_COMPLEX`, `PDF_TOO_LARGE`).
- Messages, comments and javadoc in English. The documentation is in English (`docs/en`) and Spanish (`docs/es`), and
  so are the eleven examples of `test/examples` (one `en/` and one `es/` folder per example).
- The jar includes no fonts: they are read from the resources folder (`resources/` in the working directory, or the one
  passed with `Converter.builder().resources(...)`).
