# Change Log

All notable changes to `dev.stxt:stxt-pdf` are documented in this file, newest first.

The format follows [Keep a Changelog](http://keepachangelog.com/). While the version starts with `0.`, the names of the
`dev.stxt.pdf` namespace and the Java API may change between minor versions.

## [0.14.0] - 2026-11-10

A box that drops the margins of its children at its edges, where each box ended up on the paper, the author and the
producer of the PDF, and attachments in a PDF that is not PDF/A. **From 0.13.0**: an `Attachment` without `pdfA` is
embedded instead of being `ATTACHMENT_REQUIRES_PDFA3`, which stays only for PDF/A-2 and changes its message; and
`tree.Metadata` has a fourth component, `author`, so its constructor takes four arguments instead of three. The names
of the `dev.stxt.pdf` namespace and the Java API may still change before 1.0.

### Added

- **`Trim margins`** (`none`, `both`, `top`, `bottom`), in `Block`, `Frame`, `List item`, `Cell` and `Section`: the
  box drops the margins of its children that touch its top border (`top`: the top one of the first child), its bottom
  border (`bottom`: the bottom one of the last) or both (`both`), and those of their own first and last children that
  would merge with them. They neither stay inside the box nor come out of it, so the content of a box with a
  background, of a cell or of a section starts and ends at its edge whatever margins a rule gives its elements. It can
  come from a rule of `Styles`, except in a `Section`. "Trimming the margins" in the reference of `Block`.
- **Where each box ended up on the paper.** `LaidOutDocument.fragments(id)` gives the fragments of the box with that
  `Id`, one per page it falls on, as `PageFragment` (`fragment`): the fragment with its border box and its content box
  in points from the top left corner of the paper, which a `Fragment` only gives relative to its parent.
  `fragmentsById()` gives them for every box with an `Id` in one pass, and `Page.fragments()`, the roots of a page;
  `children()` goes down from each one. A `PageFragment` also says the rectangle that encloses it once rotated
  (`bounds()`), whether it is in the flow (`inFlow()`), the columns of a `Columns`, the empty ones too, each with its
  box and its content (`columns()`), and the rectangle that encloses what it and its flow really paint (`painted()`):
  where the content of a page or of a column ends, to draw over a laid out box or to fill the blank below it without
  adding up borders and paddings outside the engine. It is in the promised API. "Where each box ended up" in "Using from Java".
- **The author and the producer of the PDF.** `Author` in `Document` (optional, a non-empty text) is who writes the
  document, and `Converter.Builder.producer(String)` the software that makes the PDF. The engine writes each one in the
  document information (`Author`, `Producer`) and in the XMP packet (`dc:creator`, `pdf:Producer`), equal in both, as
  PDF/A requires; whoever integrates no longer has to rewrite the XMP packet after `toPdf`. Without them, the PDF has
  no author and no producer, as before. `Metadata` gains the `author` component. "`Author`" in the reference of
  `Document` and "Title, subject, author and producer" in "The PDF".

### Changed

- **Attachments without PDF/A.** An `Attachment` is embedded also in a PDF that is not PDF/A, with what a PDF/A-3
  carries (the file specification, the `EmbeddedFiles` tree and the `AF` array): any PDF admits files, and a PDF that
  is going to be encrypted cannot be PDF/A. Until now it was `ATTACHMENT_REQUIRES_PDFA3`, which stays only for
  PDF/A-2, where the standard forbids it; its message changes. "Embedded files (Factur-X)" in "The PDF".

## [0.13.0] - 2026-11-08

Rules of `Styles` that give the properties of an element (`Gap`, `Fit`, `Marker`, `Column sizes`...) and rules nested
in other rules, for what is repeated inside a container; a builder from a converter, for the options of one request;
the text of the schema, the fonts of the index and the version of the engine, for a service; and a single PDFBox in
the tree of a consumer. **From 0.12.0**: a `List` without `Marker` or a `Table` without `Column sizes` is
`LIST_MARKER_MISSING` or `TABLE_COLUMN_SIZES_MISSING` instead of `TOO_FEW_CHILDREN`. The names of the `dev.stxt.pdf`
namespace and the Java API may still change before 1.0.

### Added

- **Nested rules in `Styles`.** A `For element`, `For style` or `For column` written inside a `For style`, a `For id` or
  the `For element` of an element of the document applies to the elements it names that are inside an element the
  outer rule applies to, at any depth and one level deep. The priority levels do not change: within each level the
  nested rule goes after the loose one, and between nested rules the one of the closest ancestor wins. "Inside" is the
  document as written (`Section`, the elements and the `Row` of a cell count; `Item` and `Area` do not); the cell rules
  nested in the rule of a table style only its cells, styled runs take the nested rules of their context, and a style
  whose only rule is nested is `STYLE_WITHOUT_RULE` outside it. "Nested rules" in the reference of `Styles`.
- `STYLE_NESTED_TOO_DEEP`: a rule inside a nested rule. `STYLE_NOT_ALLOWED` also for a rule nested in one that cannot
  contain rules (`For element: Document`, a cell position, a generated element) or that names what is never inside an
  element (`Document`, `Section`, `TOC 1` to `TOC 6`, `Footnotes`); `STYLE_DUPLICATE` also inside a rule.
- **Element properties in the rules of `Styles`.** Besides `Box`, `Typography` and `Break`, `For element`, `For style`
  and `For id` give the properties of an element that say how it looks or how it distributes its content: those of
  `Stack`, `Grid`, `Table` (also `Column align`) and `Columns`, `Marker` and `Marker image`, `Fit`, the ones of
  `Barcode`, `Drop cap`, `Side`, `Clear`, `Isolate margins`, `Caption position`, `Figure counter`,
  `In table of contents`, `Levels`, `Leader` and the markers and `Table columns` of a `Markdown`; and, to the element an
  `Item` wraps, `Grow`, `Shrink` and `Base size`. They apply with the priority of the groups and are not inherited:
  what the element declares wins, and what its `Item` declares wins over a `Grow` from a rule. In a rule, `Horizontal`
  and `Vertical` are those of the `Stack` or the `Grid`. Identity, content, structure and position stay out (`Id`,
  `Link`, `Alt`, `Span`, `Row header`, `Start`, `Number`, `Pdf page`, `Of`, `Offset`, `Layer`, `Pages`, `Rotate`).
  "Element properties" in the reference of `Styles`.
- `STYLE_NOT_ALLOWED` also for an element property that the element cannot declare (a `Gap` to a `Text`), when a
  `For element` rule is read and when a `For style` or `For id` rule is applied, and for `Grow`, `Shrink` or
  `Base size` to an element that is not in an `Item`. The value of an element property is checked when the rule is
  read, against the elements that have it. `STYLE_NOT_ALLOWED_IN_RUN` for an element property in the rule of a styled
  run.
- `LIST_MARKER_MISSING` and `TABLE_COLUMN_SIZES_MISSING`: a `List` without `Marker`, or a `Table` without
  `Column sizes`, its own or from a rule.
- **The options of one request.** `Converter.toBuilder()` gives a builder with the configuration of a converter, to
  build one that differs in some options (the separate styles of a request, `debug`, the part of PDF/UA, PDF/A, the
  checks, the limits). What it builds shares the resources of the converter as it has read them, without looking at
  the folders again, unless the resources options change; the styles added go after those of the converter. The shared
  converter does not change. "Options of one request" in "API".
- `DocumentSchema.text()`, promised: the text of the schema `dev.stxt.pdf` as the engine has it, for those who publish it
  or give it to an editor without depending on where the jar keeps it. "Validation in the editor" in "API".
- `FontLibrary.all()` is public: every font of the index, in order of family and variant, for those who list the fonts
  of a server instead of asking for each weight. Like the rest of the indexes of `Resources`, it is not promised.
- `Converter.version()`: the version of the engine, from a resource that Maven writes when it builds the jar, so that
  it is right also in a jar that joins the engine with its dependencies. The manifest of the jar has
  `Implementation-Title` and `Implementation-Version` too. "The version of the engine" in "API".

### Changed

- **`Marker` in `List` and `Column sizes` in `Table` are no longer required by the schema**, so that they can come
  from a rule; the engine requires them. A list or a table without them was `TOO_FEW_CHILDREN`, from `stxt-core`, and
  is now `LIST_MARKER_MISSING` or `TABLE_COLUMN_SIZES_MISSING`, with the line of the element.
- `MARKDOWN_PROPERTY_MISSING` says that the property can also come from a rule of `Styles`.

### Fixed

- **A single PDFBox in the tree of a consumer.** `de.rototor.pdfbox:graphics2d` 3.0.5, used for SVG images, brought
  PDFBox 3.0.5 next to the engine's 3.0.8; the `dependencyManagement` that chose 3.0.8 only applied to the build of
  `stxt-pdf`, so a Maven project with the enforcer's `dependencyConvergence` failed and had to fix the version itself.
  PDFBox is now excluded from `graphics2d`, which runs on the engine's version, as it already did.

### Examples

- The menu (06), the CV (03), the catalog (09) and the timetable (11) take from their `styles.stxt` what they repeated:
  the `Gap` of the dishes, the `Marker` of the lists, the `Fit` of the photos and the alignment of the rows of the
  product cards, and the `Gap` and `Vertical` of the legend. Their PDFs are byte for byte the same.
- The menu (06), the annual report (04), the manual (08) and the catalog (09) take to nested rules what they repeated
  inside a container: the `Grow` of the block of each dish, the margin of the captions of the tiles, the `Margin: 0` of
  the tips of the grids and the `Box` of the photos and frames of the cards. Their PDFs are byte for byte the same.

## [0.12.0] - 2026-11-06

Resources put together from several places, with images given to a single conversion; several styles documents in one
conversion; references and page numbers that can be written as text; split rows whose cells end with a bottom margin
that no longer push what follows out of the page; a forced line break that ends the line in preformatted text; and
`GLYPH_MISSING` instead of an internal error for a Unicode space the font does not have. **From 0.11.0**: a second call
to `Converter.Builder.styles` adds a styles document instead of replacing the first one; `{{page}}` writes `{page}`
instead of the page number between braces, and in Markdown `\{page\}` is text; with PDF/A a JPEG in CMYK is
`IMAGE_FORMAT`; and `ImageFile.path()`, `FontFile.path()` and `Resources.root()` can be `null`. The names of the
`dev.stxt.pdf` namespace and the Java API may still change before 1.0.

### Added

- **References as text.** A reference or a page number inside another pair of braces is text: `{{page}}` writes
  `{page}`, and so do `{{pages}}`, `{{page of: id}}`, `{{number of: id}}` and `{{heading N}}`, in every text, whatever
  its `Markup`, also in preformatted text and in the code of a `Markdown`. Only one pair is removed (`{{{page}}}` writes
  `{{page}}`) and what is not a marker is left as it is (`{{name}}`). Until now a text could not show a marker: it was
  replaced, or it was an error (`REFERENCE_UNKNOWN`), also with `Markup: none`. `Reference.escape(text)` writes a text
  that comes from data so that it is shown as it is. The text of a box keeps the brace of such a marker as
  `Reference.LITERAL_BRACE`, a noncharacter (`Reference.shown` gives the text that is read); the lines of the laid out
  pages have the brace. "References as text" in the reference.
- **Resources from several places.** `Resources.builder()` puts the resources together from pieces instead of one
  folder: a folder of fonts (`fonts(Path)`), a folder of images for the paths without prefix (`images(Path)`) and one
  for each prefix (`images("img", Path)`: `Image: img/logos/acme.png` is the file `logos/acme.png` of that folder; a
  path goes to the folder with the longest prefix), and fonts and images given as bytes (`font(name, bytes)`,
  `image(path, bytes)`), for what an application carries in its classpath or generates. An image in memory goes before
  a file with the same path. `new Resources(Path)` is as before. `Resources.imagePaths()` lists the paths a document
  can write. "Several places" in "Using from Java".
- **The images of one conversion.** `Converter.withImages(Map<String, byte[]>)` gives a converter with the same
  configuration and the same resources, without reading anything again, whose conversions also have those images at
  those paths: the ones a document brings with it. The shared converter does not change. `Resources.withImages(Map)`
  is the same one level below.
- **Several separate styles documents.** A conversion takes more than one styles document, in an order: at each
  priority level the rules of the first one go first and those of the document at the end, so the one passed later
  wins, property by property, and the document wins over them all; the result is that of a single styles document with
  the rules of all of them. Named colors follow the same rule. A repeated rule or name is still an error within one
  styles document, not between two. With several, the message of an error says which one its line belongs to
  (`line 8 of styles 2: ...`; with a single one, `line 8 of the styles: ...`, as before), and
  `StxtPdfException.stylesDocument()` gives it: 1 for the first one, 0 for a line of the document.
  `DocumentReader.readStyles(text, place)` reads a styles document for its place, and `BoxTreeBuilder.build` and
  `Engine.layOutDocument` take a list. "Several styles documents" in the reference.

### Changed

- **`Converter.Builder.styles` adds a styles document.** Each call adds one, and they are applied in the order of the
  calls; until now a second call replaced the document of the first one.
- **An image or a font may have no file.** `ImageFile.path()` and `FontFile.path()` are `null` for one given as bytes:
  `ImageFile.fileName()` and `FontFile.name()` are what the messages call them. `Resources.root()` is `null`, and
  `fontsFolder()` and `imagesFolder()` can be, in resources built with `Resources.builder()`. The message of an image
  that is a link out of its folder names it from that folder when the resources are not a single folder.
- **In Markdown, an escaped brace is text.** `\{page\}` and `\{page}`, in a `Markdown` or in a text with
  `Markup: markdown`, now write `{page}`: until now the backslash was removed and the marker was replaced all the same.
  In inline code and in a code block the backslash is still text, and a marker is written as text with `{{page}}`.
- **`{{page}}` no longer writes the number of the page between braces.** A document that wrote a marker inside braces
  to get `{3}` now gets `{page}`; with a space, `{ {page} }`, it is as before.

### Fixed

- **A JPEG in CMYK no longer goes into a PDF/A.** With `Converter.builder().pdfA(...)`, a JPEG with four color
  components was embedded as it is, with the device CMYK color space, and the PDF declared a PDF/A it did not comply
  with (rule 6.2.4.3 of veraPDF: the output profile of the engine is sRGB). It is now `IMAGE_FORMAT`, with the line of
  the `Image`, of the text that carries it or of its `Marker image`, when the document is laid out. Without PDF/A
  nothing changes: the PDF is valid and the JPEG is embedded as before. `ImageFile.cmyk()` says whether a JPEG has four
  components, read from its frame header. "PDF/A" in the reference, which now also says that the content of the page of
  a PDF used as an image is not checked against PDF/A.
- **A split row whose cell ends with a bottom margin no longer pushes what follows out of the page.** A cell of a
  `Table` or an item of a `Grid` (a box stretched to its row) whose last child fitted in what was left of the page, but
  not with its bottom margin, ended on that page taller than the room it had: the footer of the table, or the row after
  it, was placed beyond the page area (`PAGE_OVERFLOW`, "extends 12.34 pt beyond the page area"), depending on where the
  row fell. The rule for a box that contains the bottom margin of its last child now applies to a stretched item on the
  page where it ends with its content: the break goes before that child. And at the start of a page, where nothing can
  go before the box, the last child is broken inside with the room of its margin kept free, also in a box with border,
  padding or `Isolate margins` that continues from the page before: until now it overflowed the page. "Margins at
  breaks" in the reference.
- **A forced line break ends the line in a preformatted text.** With `Preformatted: true` or `wrap`, the character
  U+2028, written as it is or left by the `\` at the end of a line of a text with `Markup: markdown`, stayed inside its
  line: the text was not broken there, and the conversion failed with an internal error (`No glyph for U+2028`) if the
  font did not have that character. It now ends the line like any other line break, as in the rest of the texts.
  "Preformatted text" in the reference.
- **A Unicode space the font does not have is `GLYPH_MISSING`.** The check of the characters of a text took every
  white-space character as a space, but only the space, the tab, the line breaks, U+000B and U+000C are turned into one
  (in a preformatted text, only the first three). Any other one, like an em space (U+2003) or an ideographic space
  (U+3000), is painted with its glyph, and with a font that lacks it the conversion failed with an internal error when
  the PDF was written. It is now `GLYPH_MISSING`, with the line of the character, when the document is laid out. With
  a font that has the character nothing changes. "Spaces and paragraphs" in the reference.

## [0.11.0] - 2026-11-05

Resources that stay as they were when the `Converter` was created and are reloaded when whoever converts asks for it
(`Converter.reloaded()`), with the options of reading them all when the converter is created or keeping the images in
memory; stretched items that end with their split row, and split rows that no longer overflow the box that contains
them. **From 0.10.0**: by default a `Converter` no longer sees the files changed in the resources folder until
`reloaded()`; `Converter.builder().reloadResourcesEachConversion(true)` keeps the behavior of 0.10.0. The `parent()` of
the box of a `Section` is now the root of the document. The names of the `dev.stxt.pdf` namespace and the Java API may
still change before 1.0.

### Changed

- **A stretched item ends with its split row.** An item of a `Grid` or a `Table` (or of a line of a horizontal `Stack`)
  stretched to its row, without a height of its own, now measures on each page what the row does: it reaches the end
  of the page while the row goes on and, on the last page, ends where the longest of its siblings ends. Until 0.10.0 it
  distributed across the pages the height of the unsplit row, and since the lines of a text leave a gap at the end of
  each page that is not taken from its height, the split row measured more than that: two columns with a background
  ended at different heights on the last page (a stretched empty cell of 240 next to a text of 240 that leaves 4 at the
  end of each page measured 100, 100 and 40 against 100, 100 and 48), and with more gaps the stretched item ran out one
  page before the row and was missing from its last page. Its bottom margin is taken off the end of the row. An item
  of a row that is split together with the following ones (an item takes up several rows), one that `Max height` keeps
  shorter than its row and a box that is not broken (`Rotate`, `Overflow: clip`) are not lengthened: they keep the
  height they had unsplit. "Breaks in `Stack`, `Grid` and `Table`" in the reference.
  `UnitBreaker.PageFragment` has a new component, `stretched` (the constructor without it stays), and `ConstraintSpace`, the
  method `withFixedHeight`.

### Fixed

- **A split row no longer overflows the box that contains it.** A row of a `Grid` or a `Table` (or a line of a
  horizontal `Stack`) split inside a box with a definite height (a column stretched to its own row, a box with
  `Height`) consumed of that height what the item that advanced most took up on each page, and the box ran out of
  height for an item that advanced less: a text next to a stretched empty item, two texts with lines of different
  heights, a text next to an item that ends on the page beyond its last line, or rows split together because an item
  takes up several, ended in `BOX_OVERFLOW`. The row now consumes as far as the slowest of the items that go on.
  "Breaks in `Stack`, `Grid` and `Table`" in the reference.

### Java API

- **The resources of a `Converter` do not change until it is reloaded.** A `Converter` reads each font and each image
  the first time it uses it and keeps it, whatever happens to its file; `Converter.reloaded()` (new) gives a converter
  with the same configuration that sees the files added, removed or replaced, and does not read again what has not
  changed. It is called once the new files are all in place, so no conversion sees the folder half copied. Until
  0.10.0 each conversion looked at the folder when it started; that is still available with
  `Converter.builder().reloadResourcesEachConversion(true)` (new, and `reloadsResourcesEachConversion()` to ask). In
  the default mode, a PNG, a JPEG or a PDF replaced in `images/` makes the conversions that use it fail with
  `IMAGE_FORMAT` until `reloaded()`, and its message now says so. "Files that change" in `api.md`.
- **The resources can be read when the `Converter` is created.** With `Converter.builder().preloadResources(true)`
  (new, and `preloadsResources()` to ask), the converter indexes `fonts/` and reads every image of `images/` it can
  when it is created and in each `reloaded()`, instead of each file the first time a conversion uses it: it keeps the
  folder as it was then, and a font that cannot be indexed makes `build()` or `reloaded()` fail with `RESOURCE`
  instead of the first conversion. An image it cannot read, or a PDF beyond `Limits.maxPdfBytes`, is left for the
  document that uses it, which reports it with its line. It does not go together with
  `reloadResourcesEachConversion(true)` (`IllegalArgumentException` in `build()`).
- **The images can be kept in memory.** `new Resources(folder).withImagesInMemory()` (new, and `imagesInMemory()` to
  ask) keeps the bytes of every image it reads, also of a PNG, a JPEG or a PDF, and paints it with them, without going
  back to the disk: a document already laid out is painted the same even if its file changes or is removed, and the
  `Converter` goes on with the image read until `reloaded()` instead of failing with `IMAGE_FORMAT`. With
  `preloadResources(true)`, the converter keeps the whole folder as it was when it was created or reloaded. It costs
  keeping every image read in memory. A PNG or a JPEG is still embedded as before (a JPEG as it is, a PNG decoded and
  compressed again), so the PDF is the same with or without it. `ImageFile.content()` and `ImageFile.inMemory()` are
  new.
- **A snapshot that reads an image again no longer changes the one a `Resources` already had.** A `Resources` used
  directly (with `Engine` or `PdfPainter`) shared its images with its snapshots (`Resources.snapshot()`), and a
  snapshot that read again an image whose file had changed replaced it for that `Resources` too, which from then on saw
  the new file under a spelling of the path it had not used yet (`./logo.png`) and the old one under those it had, even
  in the same document. Each `Resources` now keeps the images it found;
  a `Resources` and its snapshots only share them to avoid reading again a file that has not changed. A `Resources`
  created with a constructor and one given by `snapshot()` now behave the same.
- **The box of a `Section` has the root of the document as its parent.** Until 0.10.0 it was among the `children()` of
  the root and its `parent()` was `null`. Now the parent of every box in `children()` is the box that lists it, and
  `parent()` is `null` only in the root. `Box.isLayoutRoot()` is new: whether the box is the root of its layout (the
  box of a `Section`), which is what `parent() == null` said of a section.
- **The `Page overlay` boxes of the document go before the sections in the `children()` of the root**, also when the
  document writes them after a `Section`; until 0.10.0 they were in the order of the document. They were already
  painted before those of the sections, so the pages do not change.

## [0.10.0] - 2026-11-04

Hyphenation in five more languages, a resources folder whose files can change while the `Converter` lives, an error for
barcodes too small to be read (`BARCODE_TOO_SMALL`), long documents laid out and tagged several times faster, and the
list of what the Java API will promise from 1.0, with some engine API that nothing used removed. The names of the
`dev.stxt.pdf` namespace and the Java API may still change before 1.0.

### Added

- **Hyphenation in German, French, Italian, Dutch and Portuguese.** `Hyphenate: true` now works with `Language: de`,
  `fr`, `it`, `nl` and `pt` (any region or variant), besides `es`, `ca`, `en-US` and `en-GB`, with the TeX patterns of
  `hyph-utf8` built into the jar (about 175 KB more, 120 of them German). German uses the patterns of the reformed
  spelling of 1996, also for `de-CH` and `de-1901`. In French and Italian the apostrophe between the letters of a word
  is a letter, as in TeX: `l'information` is split `l'in-for-ma-tion`; in the other languages a word with an apostrophe
  is still not split. The licenses of the new patterns are in "Hyphenation patterns" of the reference (resources).
- **Manual page "Debugging a document"** (`docs/en/manual/common/debugging.md`, and its Spanish original): the SVG
  preview page by page (`Converter.toSvg`), the outlines of the boxes (`debug`), the dump of the boxes with an `Id`
  (`StxtDump`) and how to read an error, with a complete document. Until now they were only told in the reference.

### Changed

- **The files of the resources folder can change while the `Converter` lives.** Each conversion sees the fonts and the
  images as they are when it starts: a font added, removed or replaced in `fonts/`, and an image replaced in `images/`,
  are seen by the next conversion without creating another `Converter` or `Resources`. Until 0.9.0 a `Resources` read
  each file once and did not look at the folder again. What has not changed is not read again: each conversion walks
  `fonts/` once and looks at each image it uses once, by its size and modification time. A document already laid out
  is painted with the fonts and images it was laid out with ("Files that change" in `api.md`). A `Resources` used
  without `Converter` (with `Engine`, `PdfDocumentWriter` or `PdfPainter`) keeps the old behavior unless it is given
  `resources.snapshot()`.
- **A barcode too small to be read is an error.** A `Barcode` whose content box, once laid out, leaves its modules
  narrower than the minimum of its format is `BARCODE_TOO_SMALL` (new code), with the line of the `Barcode`, the module
  it is left with and the content box it needs. The minimum is 0.25 mm in a QR code and a Code 128, about three dots of a
  300 dpi printer, and 0.264 mm in an EAN-13, 80 % of its nominal module. A QR code counts its shorter side; a barcode,
  only its width. Until 0.9.0 the code was drawn as small as its box without a warning. Whoever converts can remove the
  check with `Converter.builder().exclude(Check.BARCODE_SIZE)`. The golden case `32-barcodes` now draws its last QR code
  in a box of 60 × 24 instead of 60 × 20, whose modules (0.21 mm) are below the minimum.
- **The message of a `Break` inside a rotated box that also clips or is a side box names the rotation.** The
  `BREAK_IN_UNBREAKABLE` error now says "inside a rotated box" (or "a rotated Frame"), as `BREAK_IN_ROTATED` already did
  for a `Keep together` on that same box, instead of "a box with Overflow: clip" or "a box with Side". The code and the
  line do not change.
- **A block child moved down by the margins that come out of it is laid out once.** When the top margin of the first
  descendant of a child (a chapter `Block` that starts with a `Heading 1` with a `Margin`) is larger than the margins
  around it, the child ends up lower than its own margin puts it, and until 0.9.0 it was laid out there and again at
  its real position. The engine now predicts from the tree the margins that will come out of the child at the top
  (those of its first descendants, while nothing separates them from its top edge) and lays it out where they put it.
  The layout still decides: when the margins it returns differ from the predicted ones, the child is moved and, if that
  can change the result, laid out again. The layouts per page go from 52 to 32 in a document of short chapters (a
  quarter of a page each) and from 33 to 26 in one of chapters longer than a page, the same as with the margin on the
  block itself. The output does not change.
- **Tagging a long table no longer costs the square of its cells.** Building the structure tree of the tagged PDF
  asked, for every cell and for every marked content of a cell, how many rows its table has by going through all the
  cells of the table. The rows are now counted once per table, and the `Ref` of each table of contents entry in
  PDF/UA-2 finds its heading in a map built once instead of going through every element. The table of 2000 rows and
  8000 cells of `tools/qa/ScaleProbe.java` paints in 0.24 s instead of 8.7 s. The output does not change.
- **A running head no longer costs a pass over the whole cache on every page where it changes.** When the value of
  a `{heading N}`, a `{page of: id}` or the digits of `{pages}` change, the engine forgets what it stored of the texts
  that write them and of their ancestors, and to find it went through everything stored for every box. The cache and
  the line breaking are now indexed by box first. In a book of 681 pages with a chapter every three pages and
  `{heading 1}` in a `Page overlay` (the new `heads` probe of `tools/qa/ScaleProbe.java`), the layout takes 0.6 s
  instead of 2.3 s, against 0.5 s without the running head. The output does not change.
- **The contrast check of a dense page is about seven times faster.** The check that `layOut` makes on every page
  looks, for each run, at every background, image and line painted before it, and kept each of them as a path, whose
  test for a point counts crossings. An unrotated one is now kept as a rectangle; only what is rotated is still a path.
  On the dense pages of the new `contrast` probe of `tools/qa/ScaleProbe.java` (a table in size 6 with a background in
  every cell, `Merge borders` and underlined and struck words: about 1550 layers and 1860 runs per page), 2000 rows and
  33 pages are checked in 0.34 s instead of 2.4 s. The output does not change.
- **A long `Stack`, `Grid` or `Table` is arranged once, not once per page.** What a container computes before breaking
  it between its rows, lines or items (placing the items, sizing the tracks, laying out each item at its size, the bands
  of the rows and, in a table, the repeated header and footer and the lines of `Merge borders`) does not depend on the
  page, and until 0.9.0 it was computed again for every fragment and for every pass of the pagination. It is now kept by
  container and forgotten with the layouts of its items when paginating again. Measured with the container probes of
  `tools/qa/ScaleProbe.java` (`units`), after the change below: a table of 2000 rows and 47 pages, with a repeated header
  and `Merge borders`, lays out in 0.4 s instead of 1.8 s; a grid of 2000 rows and 32 pages, in 0.3 s instead of 0.7 s; a
  vertical stack of 2000 items and 32 pages, in 0.09 s instead of 0.23 s. The output does not change. For whoever
  implements `LayoutContext`: it has a new method, `arranged`.
- **The break quality between the rows of a grid is computed in one pass over its items.** Until 0.9.0 every row of a
  `Grid` or `Table` went through all the items to find the ones that end before it and start after it, and compared
  every pair of them: quadratic in the rows, and once per page. The same table of 2000 rows laid out in 1.8 s instead of
  7.7 s, and the grid of 2000 rows in 0.7 s instead of 3.0 s, before the change above. The output does not change.

### Fixed

- The top margin of the first child of a `Footnote` (a `Margin` on its first `Paragraph`, or a `For element: Paragraph`
  rule with one) was lost: the notes of a page touched each other, while two blocks with the same content in the flow
  are separated by that margin. The notes are now stacked like the children of a block, and the margins that come out
  of a note at the top merge with those of the note above (or count below the padding of the footnote area, for the
  first one).
- A `Columns` with a definite height (its `Height`, or the height of the grid row or stack line that stretches it) whose
  content left a gap at the end of a column (a box with a definite height, or a stretched grid item, broken inside the
  column) consumed that gap from its height and could run out of room on the next page (`BOX_OVERFLOW`). As any other
  box, it now consumes only what its columns take up ("Definite height and breaks" in the reference).
- A table with `Merge borders` lost its lines when a text inside it (or inside a `Page overlay` that contains it) was
  numbered with `{page}` or `{pages}`: the numbering rebuilt the fragments without them.
- `PdfPainter` with a `Cancellation` consulted it only at each image, not before each page as documented. It now stops
  before painting a page once the cancellation is requested.
- The message of `MARKDOWN_UNSUPPORTED` for a text with `Markup: markdown` lists `` `code` `` and `![images](path)`
  among what it allows, which it already did.
- `Link: #id` to the `Id` of a `Footnote` ended the conversion with an `InternalException` ("Link without a
  destination"), and with `PdfPainter` the link was removed without a warning: the destinations were only looked for in
  the flow of each page, and a note is in its footnote area. The link now goes to the note, in the footnote area of the
  page of its call, as the reference says.
- An inline image inside a styled run of Markdown (`[see ![logo](logo.svg) here]{brand}`) was `MARKDOWN_UNSUPPORTED`
  ("A styled run ... starts and ends in the same paragraph"): reading the alternative text of the image closed the run.
  The text before and after the image now has the style of the run, and the image is not affected by it, as the
  reference says.
- Numbers written in a form the document does not use are `VALUE_SYNTAX`, as the reference says (`12`, `12.5`, `-30`):
  `Rotate` accepted `+45`, `45.` and `4.5e1` (forms the schema allows); `Aspect ratio` and the angle of `linear(...)`
  accepted any number Java reads, such as `0x1p4` (16), `1e3` or, in `Aspect ratio`, `Infinity`. `Aspect ratio` also
  ignored what followed a second `/` (`16/9/3` was `16/9`) and accepted `-16/-9`; now it needs two positive numbers or
  one. A number with more digits than a `double` holds (more than 308) is `VALUE_SYNTAX` in any property, where it was
  read as infinite.
- A file of the resources folder that could not be read or resolved (a link to nothing in `fonts/`, a font folder that
  cannot be walked, a file removed while it was being looked up) reached whoever converts as an `InternalException` (a
  defect of the engine) or, while painting, as the `IOException` of the destination. It is now a `ResourceException`
  with the path of the file, as `api.md` promises.
- An image that changed on disk after it was read was laid out with the old file and painted with the new one, without
  a warning, and the limits had been checked on the old one. An SVG is now painted with the bytes that were checked
  when it was read. A PNG, a JPEG or a PDF, which is read again when painting, is compared with the size and the
  modification time it had: if they changed between laying out and painting, the conversion ends in `IMAGE_FORMAT`
  ("The image logo.png changed after it was read..."). An image that can no longer be read when painting (removed, for
  example) is a `ResourceException` with its path, where it was the `IOException` of the destination in `toPdf` and an
  `InternalException` in `toSvg`.
- The images of a `Resources` are kept once per file: two spellings of the same path (`logo.png` and `./logo.png`, or
  `Logo.png` on a file system that does not tell capitals from small letters) were read twice and kept twice, so a
  service that shares its `Resources` could see its memory grow with what the documents wrote.

### Java API

- `Check.BARCODE_SIZE` and `ErrorCode.BARCODE_TOO_SMALL`.
- `Modules` has a sixth component, `minModule` (the narrowest module that can be read, 0 for none; the constructor of
  five components stays), and `usedModule(width, height)` and `minimumSize()`.
- `layout.ModuleSizes`, the check that `Engine` runs after `Overflow` unless `Check.BARCODE_SIZE` is removed.
- `ImageFile.name()`, `svgContent()` (the bytes of an SVG as they were read), `checkUnchanged()` and
  `unreadable(IOException)`.
- `Resources.snapshot()`, a view of the folder as it is now that shares what was already read, and
  `LaidOutDocument.resources()`, the one it was laid out with (a fifth component; the constructor of four stays and
  leaves it `null`). `Converter.toPdf(LaidOutDocument, ...)` and `toSvg(LaidOutDocument)` paint with it when it is
  there.
- **What the jar will promise from 1.0 is written down.** "What is promised" in `reference/implementation/api.md` lists
  the types and members that are the contract: `Converter`, `DocumentReader`, `Resources` (the folder and its
  snapshot), the configuration and the errors, and what `layOut` returns (`LaidOutDocument`, `Page`, `Fragment`,
  `TextLine` and `Rule` whole, and of the `Box` of each fragment what identifies it: its `Id`, element, role and line).
  Everything else public belongs to the engine (`Engine`, `PdfDocumentWriter`, `Box.style()`, `Resources.fonts()`...):
  it can be used and it is not promised. `PublicApiTest` reads the table and fails when a promised signature exposes a
  type that is not in it.
- Removed, as internal API nothing used: the `Engine` constructors of three, four and five arguments (the ones with
  none, one and two arguments and the full one stay), the `PdfDocumentWriter.write` overload with a `Cancellation` and
  no `Progress`, and the `TextFormat` constructors of five to eleven arguments (the one with the four marks and the
  canonical one stay; the rest is `withLink`, `withSmallCaps`, `asCode`...). `FontLibrary.all()`, `Xmp.serialize` and
  the class `PdfInspector` are now package-private.

### Examples

- **Only the first page of each example is versioned as an image.** `test/examples/NN-name/<language>/` keeps
  `page-1.png`, the one the README galleries show, and no longer the other pages: the byte-for-byte comparison of
  `output.pdf` already checks them, and they added about 8 MB to the history with every accepted change. `ExamplesTest`
  still writes the images of every page to `target/reference/examples/`, and when the PDF is not the versioned one it
  also writes the versioned PDF and its pages next to the new ones, in `before/`, to compare both.

## [0.9.0] - 2026-11-02

Everything the engine lacked to replace an HTML-and-CSS pipeline in production: three new elements (`Figure`,
`Footnote`, `Barcode`), hyphenation, font weights and kerning, styled runs and images inside a text, side boxes and drop
caps, rows split in parallel across pages, PDF/A with embedded files, lengths in millimeters, and a Java API that can
be canceled, reports its progress and writes into an open PDFBox document. The names of the `dev.stxt.pdf` namespace
and the Java API may still change before 1.0.

### Added

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
  `Quiet zone` sets the light modules on each side of the code, from 0 to 40 (`VALUE_NOT_ALLOWED` outside that range),
  in the three formats (in an EAN-13 the same number goes on both sides); without it, the zones of the standards stay
  (4 modules in a QR code, 10 in a Code 128, 11 and 7 in an EAN-13). The zone is part of the natural size: in a box of a
  given width, fewer modules of zone give wider modules.
- **Hyphenation.** `Hyphenate: true` in `Typography` (inherited, `false` by default) splits words at the end of a
  line with a hyphen, following the TeX patterns (hyph-utf8) of the language of the box with Liang's algorithm and no
  dependencies: Spanish, Catalan, American English and British English, as resources of the jar (their licenses are
  in `docs/*/reference/common/resources.md`). Both `Line breaking` algorithms use the break points (`balanced`, with
  TeX's hyphen penalties), and the narrowest width of a text counts the parts of its words. A soft hyphen (U+00AD) in
  the text is always a break point, with or without `Hyphenate`, and is never painted where the word is not split. In
  the tagged PDF the hyphen of a split word is read as a soft hyphen (`ActualText`), so the copied text carries no
  hyphens and the two parts are one word. `Hyphenate: true` on a text in another language is
  `HYPHENATION_LANGUAGE_UNSUPPORTED`, and a font without the hyphen, `GLYPH_MISSING`.
- **Font weights.** `Weight` in `Typography` (inherited, `400` by default) chooses the weight of the font from 100 to 900
  in steps of 100; another number is `VALUE_NOT_ALLOWED`, and a weight the family does not have, `FONT_VARIANT_MISSING`.
  `Bold` stays as another way of writing it (`true` is 700 and `false`, 400): the last declaration wins between rules,
  ancestors and the box, and both in the same `Typography` are `WEIGHT_CONFLICT` (new code). A bold run has weight 700,
  or that of the box if it is already 700 or more. The fonts folder indexes files of any weight (`usWeightClass` as it
  is), not only 400 and 700, and the variants of a family are listed in the messages from the lightest to the heaviest
  (`weight 300, normal, italic, weight 600, bold`). The text of an SVG image accepts `font-weight` from 100 to 900 in
  steps of 100 as well. IBM Plex Sans Light (300), Medium (500) and SemiBold (600) are in `resources/fonts/`.
- **OTF fonts.** The fonts folder indexes `.otf` files too. Those with TrueType outlines are embedded as a subset, like
  a `.ttf`; those with CFF outlines, whole, as a `CIDFontType0` with the file in `FontFile3` (`OpenType`), because PDFBox
  cannot subset them. A font present as `.ttf` and as `.otf` is two files for the same font, a `RESOURCE` error.
- **Kerning.** `Kerning: true` in `Typography` (inherited, `false` by default) measures and paints the text with the
  kerning pairs of the font: those of the `kern` feature of its `GPOS` table, which the engine reads itself (pair
  adjustment lookups in both formats, also inside extension lookups), or, in a font without it, those of its `kern`
  table. A pair does not cross a space or the boundary between two runs; with `Ligatures: true` it is between the glyphs
  left after `GSUB`, never inside a ligature. The adjustments are rounded to thousandths of the font size and written as
  shifts of `TJ`, so what is measured is what is painted, and the embedded font and the copied text are the same as
  without kerning. A font without pairs (IBM Plex Mono) is painted as without `Kerning`; a `GPOS` or `kern` table that
  cannot be read is a `RESOURCE` error. The debugging SVG writes the pairs as `dx`.
- **Styled runs.** With `Markup: markdown`, or in a `Markdown`, `[text]{name}` is a run that takes from the
  `For style: name` rule what changes how its letters look and not the height of the line: `Font`, `Code font` (for the
  code inside it), `Bold`, `Weight`, `Italic`, `Color`, `Case`, `Small caps` and `Letter spacing`. What the rule declares
  replaces what the box has (a styled run can take the bold away with `Weight: 400`), and the Markdown marks add to it
  wherever they are written. A link can contain a styled run and a styled run can contain a link. A name without its
  rule is `STYLE_WITHOUT_RULE`, with the line where the text starts; a rule used in a run that declares anything else
  (`Size`, `Line height`, `Align`, a `Box`, a `Break`...) is the new `STYLE_NOT_ALLOWED_IN_RUN`, with the line of what it
  declares; a styled run inside another, or one that starts and ends in different blocks, is `MARKDOWN_UNSUPPORTED`.
  Brackets followed by anything other than a single word in braces stay text, and so do `[x]{page}` and `[x]{pages}`,
  whose braces are the page numbers. An unknown family and a low contrast of a styled run report the line of the
  property in its rule. A text with Markdown that has a Unicode noncharacter from U+FDD0 to U+FDEF is
  `MARKDOWN_UNSUPPORTED`: the engine uses them to mark the runs before parsing.
- **Images inside a text.** With `Markup: markdown`, or in a `Markdown`, `![alt](path)` inside a text is an inline
  image (an icon in a sentence, a small logo in a heading) instead of `MARKDOWN_UNSUPPORTED`: a run that is the image,
  as tall as the size the line is set at and as wide as its proportion gives, measured as a word (glued to letters it
  is part of their word; wider than the line it overflows like one), with no letter spacing, painted centered on the
  capitals on the baseline like the image marker of a list, without changing the height of the line. Only PNG, JPEG
  and SVG (the page of a PDF is `IMAGE_FORMAT`; a path that does not exist, `IMAGE_UNKNOWN`, with the line of the text),
  checked before the layout like the `Image` boxes. In the PDF it is a `Figure` with its `Alt` and its `BBox` inside the
  element of the text, in reading order, or under a `Link` if it is inside a link (`[![alt](path)](target)`, clickable,
  with the `Alt` as what is read); without `Alt` it is an artifact. It is never a character of the text: not copied, not
  searched, not in the bookmarks or the running heads. An image with a title is still an error, and one alone in its
  paragraph of a `Markdown` is still an `Image`.
- **Forced line breaks in Markdown.** A `\` at the end of a line, in a `Markdown` or in a text with `Markup: markdown`,
  is now a forced line break within the paragraph instead of `MARKDOWN_UNSUPPORTED`. Both line breakings end the line
  there, and that line is not justified. It reaches the text as U+2028, which written as is in any text does the same;
  in the text of a heading that the table of contents, the bookmarks and the running heads copy, and in the
  alternative text of an image, it is a space. A break without text before it on its line (at the start of the
  paragraph or after another one) is `MARKDOWN_UNSUPPORTED`. The two trailing spaces of CommonMark are not a break,
  because STXT removes the spaces at the end of the lines of a `>>` block.
- **Drop caps.** `Drop cap: N` (from 2 to 5) in a `Paragraph` sets its first letter, in the font of the paragraph, at
  the size whose capitals span the first `N` lines (from the cap height of the first one to the baseline of the last
  one, with the cap height of the font or 70 % of the size without it), and those lines start at its right, a space
  away; the paragraph is at least `N` lines tall and a page break does not fall among them. The text that is read and
  copied does not change. Not in a preformatted paragraph (`DROP_CAP_NOT_ALLOWED`, new code).
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
- **Image markers in lists.** `Marker: image` and the new `Marker image` node, in the `List` (for all its items) or in
  a `List item` (its own, which replaces that of the list whole), with the path of the image relative to `images/`
  and, as children, `Alt` and `Height`. The image is as tall as `Height` or the `Size` of the item, as wide as its
  proportion gives, followed by the space of a text marker, and centered on the capitals of the item's typography on
  the baseline of its first line; the same list can mix images (a check mark and a cross). With `Alt`, the tagged PDF
  carries it as a `Figure` inside the `Lbl` of the item; without it, the image is an artifact and the item has no
  `Lbl`. A list with images does not number. New errors `LIST_IMAGE_NOT_ALLOWED` (`Marker image` without `Marker:
  image`) and `LIST_IMAGE_MISSING` (an item without an image, its own or the list's); the errors of the file are those
  of an `Image`, with the line of the `Marker image`. `image` is not allowed in `Bullet markers`.
- **Merged borders in tables.** New optional `Merge borders` in the `Box` group, `false` by default, which only has an
  effect on a `Table`: being in a group, a rule of `Styles` gives it to every table (`For element: Table`), also to those
  of a `Markdown`. Written in the `Box` of another element it is `MERGE_BORDERS_WITHOUT_EFFECT`; from a rule, the other
  boxes ignore it. With `true` on a table, the border between two cells is drawn once, centered on the line between
  them, with the wider of their two sides and its color (with the same width, that of the cell above or on the left),
  and the outer border of the table goes whole inside it. The `Border` of each cell is then the room it leaves for the
  lines: half of the widest line of a side between two cells, or the whole border on the edge of the table. When a
  table breaks between two rows, the line between them is drawn inside the table on both pages; through a split row,
  only with `Edges: closed`; under a repeated header, the wider of the header's and that of the first row of the body
  on that page. `Gap` on the table or `Corner radius` on a cell is `MERGE_BORDERS_CONFLICT`, also when `Merge borders`
  comes from a rule (`Merge borders: false` in the `Box` of the table turns it off), and a table that writes it in its
  own `Box` while none of its cells has a border, `MERGE_BORDERS_WITHOUT_EFFECT`. Without `Merge borders` no document
  changes.
- **Rows and lines are split on the page they are on.** New `Split rows` (`Grid`, `Table`) and `Split items` (`Stack`),
  `true` by default: a row of a grid or a table, or a line of a horizontal stack (in a vertical one, an item), that does
  not fit in what is left of the page is now broken in parallel on that page, each item on its own, and continues on the
  next one with the items that have not finished, instead of moving whole to the next page. Until 0.8.0 a row was only
  broken when it did not fit on a whole page, and a row that fit left the page before it almost empty (two parallel
  columns of text, for instance). Splitting the row is one more break, which competes with the breaks between rows by the
  usual rules: a row whose items cannot respect their widows and orphans or a `Keep together` (in a table, that of the
  `Row`, which its cells carry), or in which some item cannot place anything, still moves whole. With `Split rows: false`
  (`Split items: false`) every row that does not fit moves whole, as before. `Split items` in a vertical `Stack` with
  `Wrap`, which is never split, is an error (`SPLIT_ITEMS_WITHOUT_EFFECT`).
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
  `PAGE_SIZE`. With equal left and right margins it is `MIRROR_MARGINS_WITHOUT_EFFECT` (new code).
- **Running heads.** `{heading N}` (N from 1 to 6) in a text of a `Page overlay` writes the text of the last `Heading N`
  that has started on that page or before, with its number if it is numbered, and `{heading N: first}`, the first one
  that starts on the page (or the same as `{heading N}` if none does): the chapter or the section in the header of
  each page. A heading of a higher level leaves the levels below it empty until their first heading, and before the
  first heading of the level the text is empty and takes up no line. A `Page overlay` with a running head is laid out
  on each page with the heading of that page; the flow does not change. `Case` applies to it as to the rest of the
  text, and the font of the text needs the characters of every heading of that level. Outside a `Page overlay` it is
  `HEADING_REFERENCE_OUTSIDE_PAGE_OVERLAY` (new code), and a level or a variant that does not exist, `REFERENCE_UNKNOWN`.
- **Named colors.** `Named color: brand #12315A`, a repeatable child of `Styles`, gives a name to a hexadecimal color,
  and the name is then written wherever a color goes: `Color`, `Border color`, `Background` (also inside `linear(...)`)
  and `Separator color`, in the rules and in the elements. The name is replaced by its color when the document is read,
  so the PDF is the same as with the color written out. The names of the separate styles document and of the document
  form a single set, and the document wins: redeclaring a name in the document changes it also in the rules of the
  styles document. New codes `COLOR_DUPLICATE` (a name repeated in one source) and `COLOR_UNKNOWN` (a color with the form
  of a name that no `Named color` declares, so `Color: red` now gives this code instead of `VALUE_SYNTAX`); a malformed
  `Named color` is `VALUE_SYNTAX`, and the name `linear`, `VALUE_NOT_ALLOWED`.
- **Subject of the PDF.** New optional `Subject` in `Document`: a non-empty text (`VALUE_NOT_ALLOWED` if empty) that
  goes to the `Subject` of the document information and to the `dc:description` of the XMP metadata, equal in both as
  PDF/A requires. Without it, the PDF has no subject, as before.
- **Lengths in millimeters.** A length can be written with the unit attached to the number: `Margin: 20mm`,
  `Width: 42.5mm`, `Column sizes: 30mm *`. A number alone is still points, and both can be mixed (`Margin: 20mm 40`).
  Any other unit (`5cm`, `12pt`) and a unit separated from the number (`12 mm`) are `VALUE_SYNTAX`. `Grow` and `Shrink`
  are factors and do not allow a unit. The error messages give measurements with their unit: «it measures 600 pt and
  the content of its container measures 515 pt».
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
  invoice XML or its metadata.

### Changed

- **Table columns sized by their content.** The columns of a `Table` share the free space in proportion to their
  content instead of in equal parts: what is left over after the `auto` columns reach their natural width goes to them
  in proportion to that width (with `auto auto`, a column of long texts takes more than one of short labels), and when
  there is not enough room for all of them, each one grows from its narrowest width in proportion to what it lacks to
  reach its natural width. Fixed widths, percentages, `between(a, b)` and fractions do not change, and neither does
  `Grid`, whose `auto` tracks still share the space in equal parts. Markdown tables with `Table columns: auto` change
  too.
- **The ISO 216 paper sizes are exact in millimeters**: `Paper: A4` measures 210 × 297 mm, 595.28 × 841.89 pt. Before
  they were rounded to whole points (595 × 842), so a box with `Width: 210mm` did not fit in an A4. A document with
  `Paper` A3 to B5 now has a page up to half a point larger, and a box that measured the rounded page by hand
  (`Width: 595`, `Height: 842`) is smaller than it.
- The `Keep with next` of the last in-flow child of a block, and the `Keep with previous` of the first one, also apply to
  the block, like the forced breaks: a heading that ends a `Block` is no longer left alone at the end of the page.
- The `After: page` (or `column`) of an empty box, and the forced break before the child that follows it, apply also
  when the empty box opens its block. Before, they were lost unless something with content had been placed before.
- The letter spacing of a line is measured run by run, and the last character of a line drops that of its own run.

### Fixed

- The bottom margin of the last child has to fit in the box that contains it. In a box that contains the margins of
  its children (border or padding, `Isolate margins`, the column of a `Columns`), when the last child fits on the page
  and its bottom margin does not, the break now goes before that child, as it does before a child whose top margin
  pushes it past the page; before, the box ended past the page by that margin. This fixes the balancing of `Columns`:
  a column whose content fit but whose last margin did not measured more than the height probed, the balancing took
  the probe as failed and, depending on the height left on the page, the `Columns` was left with a single unbalanced
  column. The page area is the exception: the margin of its last child is truncated at the end of the page, as before.
- The gap of a broken box with a definite height is not consumed by the boxes that contain it either: a broken
  fragment of a box with a definite height reaches the end of the page but only consumes what its content takes up;
  the boxes that contained it counted the whole fragment, so a column stretched to its row that contained a text
  stretched to its own row (a list item with an icon and a text, for instance) ran out of room on the next page
  (`BOX_OVERFLOW`).
- A child whose merged top margins push it past the end of the page is no longer laid out there: the block breaks
  before it. Before, an empty box with a fixed `Height` in that position was `PAGE_OVERFLOW` «it is 0 high».
- An empty box (no height, border, padding or content) never overflows vertically: its merged margins can leave it past
  the end of its parent's content, at the end of a block or just before a page break, and that was `BOX_OVERFLOW`.
- A `Frame` item of a `Stack` line or a `Grid` row that is split in parallel across pages is broken at its place in
  the flow, as in block flow: its `Offset` moves its fragments afterwards. Before, a vertical `Offset` took that much
  space away from the item on the first page, and its continuation lost the offset.
- The height of the content of a box is never negative. A box whose negative margins take away more than its children
  measure measures its border and its padding, and the child that sticks out is `BOX_OVERFLOW` at the line of the
  negative `Margin`. Before, the box was lower than its own border and the message gave a negative height.
- An in-flow box that the `Offset` of a `Frame` or a negative `Margin` takes off the physical page is `BOX_OVERFLOW`, at
  the line of that `Offset` or `Margin`. Before, it was laid out without an error and did not show.

### Java API

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
- **Progress of a conversion.** `Progress` (`model`), passed with the call after the `Cancellation` (`layOut`, `toPdf`
  and `toSvg` have an overload with both): the engine tells it, on the thread that converts, each page of the flow laid
  out (`pageLaidOut`, counting from 1 again in each pagination pass), the end of the layout with the number of pages
  (`laidOut`), each page painted to the PDF or made as SVG (`pagePainted`) and the end (`done`). Every method has an
  empty default; `Progress.NONE` listens to nothing. What a listener throws ends the conversion as an
  `InternalException`. `Engine` has a constructor with it and `PdfDocumentWriter.write` an overload.
- **Writing into an open PDFBox document.** `Converter.toPdf(document, PDDocument)` (from an `InlineNode` or a
  `LaidOutDocument`, with the long overload taking `Attachments`, `Cancellation` and `Progress`) writes the document into
  a `PDDocument` without pages instead of into a file: the same pages, fonts, structure tree, bookmarks, links, page
  labels, metadata, PDF/A declaration and attachments, but it does not save it. The caller adds what the engine does
  not write (its own `Info` keys, a watermark, encryption) and saves; the file identifier is then the one PDFBox gives
  when saving, not the one of the content. A `PDDocument` with pages is an `IllegalArgumentException`.
  `PdfDocumentWriter.write` has the same overload.
- **A page on a page of another document.** `PdfPainter` (`paint`), a `Closeable` over a `PDDocument` with the
  `Resources` and `Limits` of the conversion (and a `Cancellation`): `paint(page, PDPage)` appends the content of a
  laid-out `Page` to a page of that document, after what it already has, with the top left corner of the page at the
  top left corner of the media box of the `PDPage`, and adds its link annotations; fonts and images are embedded once
  per painter. Nothing is tagged (every content is an artifact; the structure tree of the other document is not
  touched). The links to a URL work at once; those to a box of the document get their destination when the painter is
  closed if the page that has it was painted with the same painter, and are removed otherwise. Closing the painter
  closes the PDFs it drew pages of and does not save the document.
- New components, with the old constructors kept: `TextFormat` has `style` (a `RunStyle`) and `image` (an
  `InlineImage`), `TextLine` has `left` and `right` (the room taken from the content box on each side of the line),
  `LineBreaker.Line` has `forced`, `Metadata` has `subject`, `PageSetup` has `mirrorMargins` (and `forPage(number)`
  gives the medium of each page, the one `Page.pageSetup()` carries), `VerticalMetrics` has the cap height,
  `ConstraintSpace` has the exclusions and `BreakToken.Line` the position and the exclusions of the line breaking it
  refers to. `Box.isImage()` is also true for an image marker, `ListStyle` has `markerImage()`, `markerAlt()` and
  `markerHeight()`, `ImageStyle.quietZone()` gives the declared quiet zone, `Box.runStyle(name)` the style of a styled
  run, `TextMeasurer.imageWidth` measures an inline image and `LayoutContext.imageRatio` gives its proportion,
  `Canvas.inlineImage` draws it, `TrackSizing.size` has a variant with a `Distribution`, `LayoutResult.occupied(from)`
  gives what a fragment takes up for its parent, `UnitBreaker.Units` has a `split` component and `Fragment.rules`
  carries the merged borders of a table. The title, the language and the XMP packet are written by `PdfMetadata`.

### Examples

- The manual (`test/examples/08-manual`) has its figures and tables as `Figure` elements ("Figura 1.", "Tabla 1."),
  refers to the wiring diagram with `{number of: id}`, ends with a list of figures and a list of tables, and writes the
  section in effect on each page in its header with `{heading 2}`.
- The newsletter (`05-newsletter`) has the photo of the trails story as a side box, with the text around it, and is
  hyphenated. The contract (`07-contract`) has three footnotes. The résumé (`03-resume`) uses the weights 300, 500 and
  600. The annual report (`04-annual-report`) uses kerning. The invoice (`01-invoice`) carries a payment QR code, and
  the certificate (`10-certificate`), a QR code with its verification address. The annual report, the newsletter and
  the contract declare their palette with `Named color`; their PDFs are byte for byte the same.

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
