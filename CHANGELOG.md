# Change Log

All notable changes to `dev.stxt:stxt-pdf` are documented in this file, newest first.

The format follows [Keep a Changelog](http://keepachangelog.com/). While the version starts with `0.`, the names of the
`dev.stxt.pdf` namespace and the Java API may change between minor versions.

## [Unreleased]

- **Subject of the PDF.** New optional `Subject` in `Document`: a non-empty text (`VALUE_NOT_ALLOWED` if empty) that
  goes to the `Subject` of the document information and to the `dc:description` of the XMP metadata, equal in both as
  PDF/A requires. Without it, the PDF has no subject, as before. `Metadata` has a third component, `subject` (the
  constructor with two remains).
- **Forced line breaks in Markdown.** A `\` at the end of a line, in a `Markdown` or in a text with `Markup: markdown`,
  is now a forced line break within the paragraph instead of `MARKDOWN_UNSUPPORTED`. Both line breakings end the line
  there, and that line is not justified. It reaches the text as U+2028 (`TextRun.LINE_SEPARATOR`), which written as is
  in any text does the same; in the text of a heading that the table of contents, the bookmarks and the running heads
  copy, and in the alternative text of an image, it is a space. A break without text before it on its line (at the
  start of the paragraph or after another one) is `MARKDOWN_UNSUPPORTED`. The two trailing spaces of CommonMark are not
  a break, because STXT removes the spaces at the end of the lines of a `>>` block. `LineBreaker.Line` has a fourth
  component, `forced` (the constructor with three remains).
- **Image markers in lists.** `Marker: image` and the new `Marker image` node, in the `List` (for all its items) or in
  a `List item` (its own, which replaces that of the list whole), with the path of the image relative to `images/`
  and, as children, `Alt` and `Height`. The image is as tall as `Height` or the `Size` of the item, as wide as its
  proportion gives, followed by the space of a text marker, and centered on the capitals of the item's typography on
  the baseline of its first line; the same list can mix images (a check mark and a cross). With `Alt`, the tagged PDF
  carries it as a `Figure` inside the `Lbl` of the item; without it, the image is an artifact and the item has no
  `Lbl`. A list with images does not number. New errors `LIST_IMAGE_NOT_ALLOWED` (`Marker image` without `Marker:
  image`) and `LIST_IMAGE_MISSING` (an item without an image, its own or the list's); the errors of the file are those
  of an `Image`, with the line of the `Marker image`. `image` is not allowed in `Bullet markers`. `Box.isImage()` is
  also true for an image marker, and `ListStyle` has `markerImage()`, `markerAlt()` and `markerHeight()`. Golden
  `42-marker-images`; the test icons `test/check.svg` and `test/cross.svg`.
- **Rows and lines are split on the page they are on.** New `Split rows` (`Grid`, `Table`) and `Split items` (`Stack`),
  `true` by default: a row of a grid or a table, or a line of a horizontal stack (in a vertical one, an item), that does
  not fit in what is left of the page is now broken in parallel on that page, each item on its own, and continues on the
  next one with the items that have not finished, instead of moving whole to the next page. Until 0.8.0 a row was only
  broken when it did not fit on a whole page, and a row that fit left the page before it almost empty (two parallel
  columns of text, for instance). Splitting the row is one more break, which competes with the breaks between rows by the
  usual rules: a row whose items cannot respect their widows and orphans or a `Keep together` (in a table, that of the
  `Row`, which its cells carry), or in which some item cannot place anything, still moves whole. With `Split rows: false`
  (`Split items: false`) every row that does not fit moves whole, as before. `Split items` in a vertical `Stack` with
  `Wrap`, which is never split, is an error (`SPLIT_ITEMS_WITHOUT_EFFECT`). `UnitBreaker.Units` has a new `split`
  component. Golden `41-split-rows`; goldens 06 and 07 keep their scenario with `Split rows: false`.
- **The gap of a broken box with a definite height is not consumed by the boxes that contain it either.** A broken
  fragment of a box with a definite height reaches the end of the page but only consumes what its content takes up;
  the boxes that contained it, however, counted the whole fragment, so a column stretched to its row that contained a
  text stretched to its own row (a list item with an icon and a text, for instance) ran out of room on the next page
  (`BOX_OVERFLOW`). `LayoutResult.occupied(from)` gives what a fragment takes up for its parent, and `Slice.of` takes
  it apart from the visible height of the content.
- **Canceling a conversion.** `Cancellation` (`model`), a `() -> boolean` the engine asks wherever it works: at each
  element when building the tree, at each layout that is not in the cache, at each page when paginating, checking and
  painting, and at each image it embeds. It goes with each call (`layOut(doc, cancellation)`, `toPdf(doc, out,
  attachments, cancellation)`, also with a `LaidOutDocument`, and `toSvg(laidOut, cancellation)`) or with every
  conversion of a `Converter` (`Converter.builder().cancellation(...)`); with both, either stops it.
  `Cancellation.deadline(Instant)`, `timeout(Duration)`, `whenInterrupted()` and `or` are the usual ones, and `NONE` the
  default. When it asks to stop, the conversion ends at once with a `CancelledException` of the new kind
  `StxtPdfException.Kind.CANCELLED`, code `CANCELLED`, without a line, and nothing has been written to the destination.
  A `switch` over `Kind` has to cover the new value. `Engine`, `BoxTreeBuilder`, `PdfDocumentWriter.write`,
  `Overflow.check` and `ContrastCheck.check` take it as a new parameter; the signatures without it still exist.
- **The bottom margin of the last child has to fit in the box that contains it.** In a box that contains the margins of
  its children (border or padding, `Isolate margins`, the column of a `Columns`), when the last child fits on the page
  and its bottom margin does not, the break now goes before that child, as it does before a child whose top margin
  pushes it past the page; before, the box ended past the page by that margin. This fixes the balancing of `Columns`:
  a column whose content fit but whose last margin did not measured more than the height probed, the balancing took
  the probe as failed and, depending on the height left on the page, the `Columns` was left with a single unbalanced
  column. The page area is the exception: the margin of its last child is truncated at the end of the page, as before.
- **Figures with a caption.** New element `Figure`: a block with exactly one element (an `Image`, a `Barcode`, a `Table`
  or a `Block`) and a `Caption`, a text (value or `Content >>`, with `Markup: markdown` if wanted) that goes below the
  content or above a table, wherever it is written (`Caption position: top | bottom` changes it). A figure is numbered
  by default, with the counter of the figures or, with a `Table` inside, of the tables (`Figure counter: figures |
  tables` changes it); `Numbered: false` declared in it or in a rule that reaches it (not inherited) leaves it without a
  number, `Number format` and `Number label` (`Number label: Figure {number}.`) apply as in the headings, the number goes
  before the caption and `{number of: id}` gives it. The figure is not split across pages (a table inside is, with the
  caption kept with its first or last part), it goes wherever an in-flow element goes except in a `Page overlay`, also
  as a side box with `Side`. The image inside needs `Alt`, and a figure with a `Block`, its own. `For element: Figure`
  and `For element: Caption` style them. In the tagged PDF a figure with an image or a block is a single `Figure` with
  the `Alt`, the `BBox` and a `Caption`; with a table, the `Table` carries the `Caption`. New codes `FIGURE_CONTENT`,
  `FIGURE_WITHOUT_ALT` and `FIGURE_PROPERTY_WITHOUT_EFFECT`.
- **Tables of figures and tables.** `Of: headings | figures | tables` in `Table of contents` (`headings` by default):
  with `figures` or `tables` it lists the `Figure` elements of that counter, with the text of their caption, in `TOC 1`
  entries, and `In table of contents: false` on a `Figure` leaves it out. `Levels` with `Of: figures` or `tables` is
  `LEVELS_WITHOUT_HEADINGS` (new code), and a table of figures or tables with nothing to list, `TABLE_OF_CONTENTS_EMPTY`.
- The manual example (`test/examples/08-manual`) has its figures and tables as `Figure` elements ("Figura 1.",
  "Tabla 1."), refers to the wiring diagram with `{number of: id}` and ends with a list of figures and a list of tables.
- **Side boxes.** `Side: left | right` on an `Image`, a `Barcode` or a `Block` (with `Width` or `Max width`) in block
  flow places it at that side of its container, at its position in the flow, and the text of the siblings that follow
  it shortens its lines beside it while they overlap it: the `Text` and `Paragraph` elements and, with the text inside
  them, the `Block` elements, the headings, the `Markdown` elements and the lists (a list only beside a box on the
  right, where its markers do not hang). The margin of the side box is the separation. Anything else (a table, a
  stack, a grid, a text with background or border...) goes below the side boxes, and so does a `Text`, `Paragraph`,
  `Block`, heading, `Markdown`, `Image` or `Barcode` with `Clear: true`. A word that does not fit beside the box leaves
  the line empty and goes on in the next one; a second side box on the same side goes below the first; the container
  reaches the bottom of its side boxes; a side box is not split across pages and moves to the next page with the first
  line that runs beside it, and the text that goes on on the next page has the whole width again. Both line breaking
  algorithms work with the width of each line. New codes `SIDE_NOT_IN_BLOCK_FLOW` (on an item of a `Stack` or a
  `Grid`), `SIDE_WITHOUT_WIDTH`, `BREAK_IN_SIDE_BOX`, `CLEAR_WITHOUT_SIDE`; `Margin: auto` on a side box is
  `MARGIN_AUTO_WITHOUT_EFFECT` and a `Break` inside one, `BREAK_IN_UNBREAKABLE`.
- **Drop caps.** `Drop cap: N` (from 2 to 5) in a `Paragraph` sets its first letter, in the font of the paragraph, at
  the size whose capitals span the first `N` lines (from the cap height of the first one to the baseline of the last
  one, with the cap height of the font or 70 % of the size without it), and those lines start at its right, a space
  away; the paragraph is at least `N` lines tall and a page break does not fall among them. The text that is read and
  copied does not change. Not in a preformatted paragraph (`DROP_CAP_NOT_ALLOWED`, new code).
- `TextLine` has two more components, `left` and `right`, the room taken from the content box on each side of the
  line, and `BreakToken.Line`, the position and the exclusions of the line breaking it refers to; `VerticalMetrics`
  has the cap height; `ConstraintSpace` has the exclusions. The constructors without them still exist.
- The newsletter example (`test/examples/05-newsletter`) has the photo of the trails story as a side box, with the
  text around it.
- **Footnotes.** New element `Footnote`: the content of a note, with a required `Id`, written anywhere in its section
  (where an in-flow element goes) without taking room there, and placed by the engine at the bottom of the page where
  the text calls it with `[^id]` (in a text with `Markup: markdown` or in a `Markdown`), under a rule. The call is a
  superscript run with the number of the note (70% of the size, raised a third), in the font and format of the text
  around it, and the note starts with the same number before its first text, which has to be a `Paragraph`, a `Text` or
  a `Markdown` paragraph; headings, images and code blocks are not allowed inside (`FOOTNOTE_CONTENT`). The calls are
  numbered in document order, per document or per section (`Footnote numbering: continue | restart` and
  `Footnote number format` in `Section`). A line enters a page only if its note fits under it, whole or at least its
  start: otherwise it moves to the next page with the note; a note may continue at the bottom of the next page when the
  break respects widows, orphans and keeps, and in a `Columns` the notes go at the bottom of the page. The notes of a
  page go in the generated element `Footnotes`, the footnote area, styled with `For element: Footnotes` (its top
  `Border` is the rule, 0.5 pt by default, with a top margin of 8 and a padding of 4; its typography is that of all the
  notes) and `For element: Footnote` (each note). In the tagged PDF the note is a `Note` (`FENote` with PDF/UA-2) with
  its `Lbl`, right after the paragraph that calls it, and the call a `Reference` linked to it. New codes
  `FOOTNOTE_UNKNOWN`, `FOOTNOTE_NOT_CALLED`, `FOOTNOTE_CALL_DUPLICATE`, `FOOTNOTE_CALL_NOT_ALLOWED`,
  `FOOTNOTE_NOT_ALLOWED`, `FOOTNOTE_CONTENT`, `FOOTNOTES_PROPERTY_WITHOUT_EFFECT` and `FOOTNOTE_TOO_TALL`.
- The contract example (`test/examples/07-contract`) has three footnotes.
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
- **PDF/A.** `Converter.builder().pdfA(PdfA.A2B | A2A | A3B | A3A)` writes a PDF that conforms to that part and level of
  ISO 19005 besides PDF/UA-1 (PDF/A-2 and PDF/A-3 are PDF 1.7 files; with `pdfUa(2)`, PDF 2.0, `build()` is an
  `IllegalArgumentException`): the `pdfaid` declaration in the XMP packet, the description of the `pdfuaid` schema as an
  extension schema, and an output intent with the sRGB v2 profile of the ICC (`sRGB2014.icc`, 3 KB, inside the jar;
  its license is in `docs/*/reference/common/resources.md`). Nothing else changes: the fonts, the transparency, the
  links and the images already met the rules. By default no PDF/A is declared. veraPDF validates the four variants on
  the eleven examples.
- **Embedded files (Factur-X).** `converter.toPdf(document, out, attachments)`, also with a `LaidOutDocument`, embeds
  the files of an `Attachments` (`Attachment.of(name, bytes, mimeType, relationship)`, with `DATA`, `ALTERNATIVE`,
  `SOURCE` or `SUPPLEMENT`, an optional description and an optional modification date; without it the PDF still
  carries no date) as associated files (`EmbeddedFiles` and `AF`), and `withXmp` adds the XMP fragment that goes with
  them (for Factur-X, the one its specification gives), whose extension schemas join the engine's own. An attachment
  needs PDF/A-3: otherwise, `ATTACHMENT_REQUIRES_PDFA3` (new code, kind `RESOURCE`). The engine does not generate the
  invoice XML or its metadata. `PdfDocumentWriter.write` has an overload with `PdfA` and `Attachments`.
- The title, the language and the XMP packet are written by `PdfMetadata` (before, `PdfStructure`).
- **Overflow policy per box.** `Overflow` in `Box` (`error` by default, `shrink` or `clip`) says what is done when what
  a box contains does not fit in it. `shrink`, in a box with text (`Text`, `Paragraph`, a `Cell` with a value), reduces
  the `Size` of the text in steps of 0.5 pt until every line fits in the width and, if the box has a definite height (its
  `Height`, one imposed by its parent, its `Aspect ratio`) or a `Max height`, the lines fit in it; it never goes below
  `Min size`, new in `Typography` (inherited, 6 by default), which it tries as the last step. A `Line height` in percent
  follows the size, one in points does not; the contrast is checked at the reduced size. If the text does not fit even
  at `Min size`, `TEXT_OVERFLOW` or `BOX_OVERFLOW` as before, and the message says so; where a text could shrink and does
  not, those messages say that `Overflow: shrink` exists. `Overflow: shrink` written by an element without text is
  `SHRINK_WITHOUT_TEXT` (new code); from a `Styles` rule it is ignored on boxes without text, so `For element: Cell`
  serves a whole table; `Min size` greater than `Size` in a box that shrinks is `MIN_SIZE_CONFLICT` (new code).
  `clip`, in a box without text inside it at any depth (otherwise `CLIP_WITH_TEXT`, new code), paints what sticks out of
  its padding box clipped, the `Overlay` boxes inside included, and does not check it against the box nor against the
  page; the box is not split across pages (`Keep together` or `Edges` on it are `BREAK_IN_CLIPPED`, new code, and a
  `Break` inside it, `BREAK_IN_UNBREAKABLE`). The sizes of the boxes do not change: `shrink` fits the text to them, and
  `clip` cuts what sticks out.
- **Mirror margins.** `Mirror margins: true` in `Page` (`false` by default) swaps the left and right margins on the even
  physical pages, for double-sided printing: `Margin` is that of the odd pages, the right-hand ones, so the inner margin
  is the left one. The page area measures the same on both sides, so the flow does not change; the `Page overlay`
  elements are placed in the page area of each page and do not change sides (a folio on the outside is two of them,
  with `Pages: odd` and `Pages: even`). A `Section` inherits it together with `Margin`: one that declares `Margin`
  starts afresh, without mirroring unless it says so, and `Mirror margins` without `Margin` in a section is
  `PAGE_SIZE`. With equal left and right margins it is `MIRROR_MARGINS_WITHOUT_EFFECT` (new code). In the Java API,
  `PageSetup` has a fourth component, `mirrorMargins`, and `forPage(number)` gives the medium of each page, which is
  the one `Page.pageSetup()` carries.

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
