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
| `styles(InlineNode)`, `styles(Path)`, `styles(String)` | Adds a separate styles document: each call adds one, and they are applied in the order of the calls ([below](#separate-styles)) |
| `limits(Limits)` | The limits of the conversion |
| `debug(boolean)` | Paints the outlines of the boxes |
| `pdfUa(int)` | The part of PDF/UA the PDF conforms to: 1 (PDF 1.7, by default) or 2 (PDF 2.0) ([The PDF inside](pdf.md#pdfua-2)). Any other value is an `IllegalArgumentException` |
| `pdfA(PdfA)` | The PDF/A the PDF also conforms to: `A2B`, `A2A`, `A3B` or `A3A` ([below](#pdfa-and-attachments)); by default, none. With `pdfUa(2)`, `build()` is an `IllegalArgumentException` |
| `exclude(Check...)` | Removes checks. By default all of them are made ([Checks](#checks)) |
| `cancellation(Cancellation)` | The one that stops every conversion of the `Converter`; by default, none ([Canceling a conversion](#canceling-a-conversion)) |

> **Decision:** the configuration is an immutable object, `Converter`, thread-safe because each conversion creates its
> own `Engine`, with `layOut`, `toPdf` (of a document or of one already laid out) and `toSvg`. The styles are never `null`: if
> there are none, `styles` is not called. It lives in a package of its own, above all the others. `PdfDocumentWriter.write` and `Engine`
> remain public as a low-level layer. *Rejected alternative:* a method with one overload for each
> combination of options.

## What is promised

While the version starts with `0.`, the API may change between minor versions. From 1.0 on, what the jar promises is
what this table lists, and only that: a type or a member that is not in it may change or disappear in a minor version. A
type promised whole promises all its public members; of the others, only those the row names. The constants of an enum
are always included. Of an exception, what is read from it is promised, not its constructors.

| Type | What is promised |
|---|---|
| `convert.Converter`, `convert.Converter.Builder` | All |
| `stxt.DocumentReader` | All |
| `tree.DocumentSchema` | `NAMESPACE`, `text()` |
| `resources.Resources` | `Resources()`, `Resources(Path)`, `DEFAULT_ROOT`, `root()`, `fontsFolder()`, `imagesFolder()`, `resolve(...)`, `snapshot()`, `withImagesInMemory()`, `imagesInMemory()`, `builder()`, `withImages(...)`, `imagePaths()` |
| `resources.Resources.Builder` | All |
| `model.Limits`, `model.Check`, `model.PdfA`, `model.Cancellation`, `model.Progress`, `model.Attachment`, `model.Attachment.Relationship`, `model.Attachments` | All |
| `model.StxtPdfException`, `model.StxtPdfException.Kind`, `tree.ErrorCode` | All |
| `tree.InvalidModelException` | `errorCode()` and what it inherits |
| `tree.DocumentSyntaxException` | What it inherits |
| `resources.ResourceException`, `resources.ImageFormatException`, `model.InternalException`, `model.CancelledException` | `CODE` and what it inherits |
| `layout.LaidOutDocument` | All |
| `fragment.Page`, `fragment.Fragment`, `fragment.TextLine`, `fragment.TextLine.Run`, `fragment.Rule` | All |
| `tree.PageSetup`, `tree.PageSetup.Paper`, `tree.PageSetup.Orientation`, `tree.Metadata` | All |
| `tree.Box` | `id()`, `element()`, `role()`, `line()`, `anonymous()`, `parent()`, `children()`, `text()` |
| `tree.Element` | Its constants, `canonicalName()`, `label()`, `role()`, `headingLevel()` |
| `tree.Element.Role` | All |
| `model.Insets`, `model.Rect`, `model.Sides`, `model.Value`, `model.Value.Points`, `model.Value.Keyword`, `model.Value.Percentage`, `model.Value.Between`, `model.Value.Fraction`, `model.NumberFormat` | All |
| `model.TextRun` | `text()`, `format()`, `LINE_SEPARATOR`, `OBJECT_REPLACEMENT`, `IMAGE`, `plain(...)`, `text(...)`, `formatted(...)` |
| `model.Reference` | `escape(...)`, `shown(...)`, `LITERAL_BRACE` |
| `model.TextFormat`, `model.FontFace`, `model.FontWeight`, `model.FontStyle`, `model.TextCase`, `model.RunStyle`, `model.InlineImage` | All |
| `output.StxtDump` | All |
| `paint.PdfPainter` | All |

What `layOut` returns is part of the contract: `LaidOutDocument`, its pages and its fragments, with the geometry of each
one, its lines and the text of each run, and, of the box of each fragment, what identifies it and places it in the
document. A service can count the pages or measure how full each one is without painting anything. They are `record`s
that only gain components: what a component says today it will always say.

Everything else the jar exposes as public is so because another package of the engine uses it, not for the caller:
`Engine`, `PdfDocumentWriter`, `BoxTreeBuilder`, `Box.style()` and the groups of `Style`, `Resources.fonts()` and
`images()` with their indexes (`FontLibrary.all()` lists the fonts, for a service that shows them), the `layout` algorithms, the `paint` canvases. It can be used, with the version pinned,
and it is not promised.

`PublicApiTest` reads this table and checks that every type and every member exists and is public, that no promised
signature (parameters, return type, components, supertypes) exposes a type of the engine that is not in the table nor a
foreign one other than those of `stxt-core` (`InlineNode`, `ParseException`) or `PDDocument` and `PDPage` of PDFBox, and
that the table in `docs/es/` says the same. The boundary does not move without touching this page.

> **Decision:** the boundary is written in this table and watched by a test, not by a `module-info.java` with
> `exports`: in Java without modules everything that crosses packages has to be public, and with modules
> `Fragment.box()` would force exporting the whole of `tree`. `LaidOutDocument` stays a `record`: turning it into a class
> would not hide `root()`, which `paint` needs. Of `Box`, what identifies a box is promised, not its style nor what the
> engine asks it; of `Resources`, the folder and its snapshot, not its indexes, so that fontbox does not reach the
> contract through `FontFile.parse`, which `paint` needs to embed each font from a fresh `TrueTypeFont`. *Rejected
> alternatives:* promising everything public, which fixes more than 160 types forever (what this table names drags,
> following its signatures, 64); leaving the fragments out of the contract until the service exists, which would need an
> exception to the closure rule from the first day, because `LaidOutDocument.pages()` exposes them anyway.

## PDF/A and attachments

```java
Converter converter = Converter.builder().pdfA(PdfA.A3B).build();
Attachment xml = Attachment.of("factur-x.xml", bytes, "text/xml", Attachment.Relationship.ALTERNATIVE)
        .withDescription("Factur-X invoice");
converter.toPdf(doc, out, Attachments.of(xml).withXmp(facturXmp));   // a Factur-X invoice
```

`pdfA` belongs to the `Converter`, like `pdfUa`: a decision of whoever publishes ([The PDF](../common/pdf.md#pdfa)).
The attachments belong to each document and go with the call: `toPdf(document, target, Attachments)`, also with a
`LaidOutDocument`. `Attachments.of(Attachment...)` gathers the files (with different names) and `withXmp` adds the XMP
that goes with them (one or more `rdf:Description`, with their namespaces declared; `rdf` already is).
`Attachment.of(name, bytes, type, relationship)` is immutable and copies the bytes; `withDescription` and
`withModified` give the description and the modification date. All of it is in `model`. The two `toPdf` without
attachments are the same as `Attachments.NONE`.

An attachment without `pdfA(A3B)` or `pdfA(A3A)` is `ATTACHMENT_REQUIRES_PDFA3`, a `StxtPdfException` of kind `RESOURCE`
(it is the configuration of the caller, not the document) and without a line. An XMP that is not XML, two attachments
with the same name, a blank name or a type that is not `type/subtype` are an `IllegalArgumentException` when they are
built, before converting. `PdfDocumentWriter.write` receives the same (`ua`, `pdfA` and `Attachments`) as the low-level
layer.

With `pdfA`, `layOut` also checks that no image of the document is a JPEG in CMYK (`IMAGE_FORMAT`, with its line;
[The PDF](../common/pdf.md#pdfa)). The `Converter` that lays out checks it: a document is laid out and painted with the
same one.

> **Decision:** the attachments and their XMP are passed with the call and not in the `builder`: a `Converter` is
> shared between documents and the XML of an invoice belongs to one invoice. *Rejected alternative:* `attach(...)` in
> the `builder`.

## Checks

Some checks are not rules of the document but of what is seen or of what the files contain, and the caller
can remove them with `exclude`, which accumulates across calls. By default all of them are made, and `Converter.excluded()` gives
the ones that have been removed. They are the values of the enum `Check` (`model`):

| `Check` | What it checks | Without it |
|---|---|---|
| `CONTRAST` | The contrast of the text with its background, `CONTRAST_LOW` ([The PDF](../common/pdf.md#contrast)). `layOut` checks it with `ContrastCheck`, a canvas that records what is painted | A text may be unreadable |
| `PDF_IMAGE_FONTS` | That the fonts of the page of a PDF drawn by an `Image` comply with PDF/UA: embedded, stating which character each glyph is and the other rules of the list, `IMAGE_FORMAT` ([Image](../components/image.md#pdf)) | The viewer may substitute a font, its text may not be copyable and the PDF may not conform to PDF/UA |
| `PDF_IMAGE_ANNOTATIONS` | That the page of a PDF has no visible annotations, `IMAGE_FORMAT` | The page is drawn without them |
| `BARCODE_SIZE` | That the modules of a `Barcode` are not narrower than the minimum of its format, `BARCODE_TOO_SMALL` ([Barcode](../components/barcode.md#minimum-module)) | The code is drawn as small as its box and may not be read |

```java
Converter converter = Converter.builder().exclude(Check.CONTRAST, Check.PDF_IMAGE_ANNOTATIONS).build();
```

`Engine` receives the removed ones and passes them to `Images`; unless `BARCODE_SIZE` is removed, it checks the modules
with `ModuleSizes` after `Overflow`. The contrast is checked by `Converter.layOut` after layout.

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

With several styles documents, each call to `styles` adds one:

```java
Converter converter = Converter.builder()
        .styles(Path.of("company.stxt"))
        .styles(Path.of("invoices.stxt"))   // wins over company.stxt
        .build();
```

They are applied in the order of the calls ([Several styles documents](../common/styles.md#several-styles-documents)).
`BoxTreeBuilder.build` and `Engine.layOutDocument` also receive them in a list, which can be empty.
`DocumentReader.readStyles(text, place)` reads the styles document that goes in that place, from 1, so that an error of
reading it says which one it belongs to ([Errors](#errors)); `styles(Path)` and `styles(String)` read it that way.

> **Decision:** `styles` adds instead of replacing, like `exclude`. *Rejected alternative:* a list in a single call,
> with which a second call would have to replace or be an error.

## Texts that come from data

```java
String line = "\tParagraph: For the attention of " + Reference.escape(customer.name());
```

A text of the document replaces its markers (`{page}`, `{page of: id}`…), and a piece of data that has them written
would be replaced too, or would give an error. `Reference.escape(text)` (in `model`) returns the text with each marker
inside another pair of braces, which is how it is written so that it comes out as it is
([References as text](../common/references.md#references-as-text)); it touches nothing else. It serves for the
literal texts (`Markup: none`), preformatted or not. In a text with `Markup: markdown` or in a `Markdown` the data is
escaped as Markdown, with a backslash before each punctuation sign, and that is enough: a brace with a backslash is
already text, and passing the data through `Reference.escape` as well would double its braces.

## Resources

`new Resources(Path.of("other"))` uses another resources folder. Every path goes through `Resources.resolve`, which never goes
backwards. `Resources` is shared across threads and across conversions. It keeps the index of `fonts/` and the images it
has read, one per file: `ImageLibrary` keeps them by their real path, so `logo.png`, `./logo.png` and, on a file system
that does not tell capitals from small letters, `Logo.png` are the same image, and what the documents write does not make
the memory of a service grow. The spellings already seen are a shortcut to them, without going to the disk, that is
emptied when it reaches 1024.

`FontLibrary` indexes the `.ttf` and `.otf` files and identifies each one by its contents:

- **family:** the typographic family of the `name` table (`nameID` 16) or, if it has none, the family (`nameID` 1);
- **weight:** `usWeightClass` of `OS/2` as it is, from 1 to 1000 (outside that range the file is ignored without error).
  Only a weight the document can write can be requested, from 100 to 900 in steps of 100: a file of 450 is indexed and
  appears in the list of variants of the error, but it is not used;
- **style:** the italic bit of `fsSelection`.

`FontFile.parse` reads a file that starts with `OTTO` (CFF outlines) with the OpenType parser of fontbox and the rest
with the TrueType one, whatever its extension; `FontFile.cff()` says which ones are CFF, which are embedded whole
([The PDF inside](pdf.md#pages-fonts-and-metadata)).

> **Decision:** the fonts are only checked with `PdfMetrics` (`Engine.layOutDocument` calls `validate`). With
> `FixedMetrics` the fonts folder is not looked at: it is enough for `Font` to be declared. *Rejected alternative:* always validating
> the fonts when building the tree, which would require the folder also to lay out without PDF.

### Several places

```java
Resources resources = Resources.builder()
        .fonts(Path.of("/data/fonts"))                     // the folder of the fonts
        .font("Brand-Regular.ttf", bytesOfTheFont)         // and a font that is not in it
        .images("img", Path.of("/data/img"))               // Image: img/logos/acme.png
        .images("pdf", Path.of("/data/templates"))         // Image: pdf/receipt.pdf
        .image("app/logo.svg", bytesOfTheLogo)             // Image: app/logo.svg
        .build();
Converter converter = Converter.builder().resources(resources).build();
```

`Resources.builder()` puts the resources together from pieces
([Resources](../common/resources.md#resources-from-several-places)):

- `fonts(Path)`, the folder of the fonts, a single one, which is walked with its subfolders like `fonts/`;
- `images(Path)`, the folder of the paths without prefix, like `images/`, and `images(prefix, Path)`, a folder for the
  paths that start with that prefix (one or more segments). A path goes to the one with the longest prefix;
- `font(name, bytes)` and `image(path, bytes)`, a font and an image in memory: what the application carries in its
  classpath or generates. The bytes are copied. The name of the font only appears in the messages; the path of the
  image is the one the document writes, and it goes before a file with the same path;
- `imagesInMemory(true)`, the same as `withImagesInMemory()`.

None in particular is needed: resources with only fonts and images in memory do not look at the disk. Two folders of
fonts, two folders with the same prefix, two images with the same path or a prefix or a path that is absolute or has
`..` are an `IllegalArgumentException` when building. Resources without any font give `ResourceException` when a
document asks for one. `root()` is `null` in these resources, `fontsFolder()` and `imagesFolder()` (the folder without
prefix) can be, and `resolve(...)`, which looks in the single folder, fails. `imagePaths()` gives the paths a document
can write now, those of the folders with their prefix and those in memory.

An image in memory is an `ImageFile` without a file (`path()` is `null`, `fileName()` is the end of its path) that keeps
its bytes, and a font in memory, a `FontFile` without a file (`name()` is its name and "(in memory)"). Reloading does
not touch them: a view (`snapshot()`) shares the ones already read.

### Images of one conversion

```java
Converter own = converter.withImages(Map.of("attached/signature.png", bytesOfTheSignature));
own.toPdf(document, out);                                  // Image: attached/signature.png
```

`Converter.withImages(Map<String, byte[]>)` gives a `Converter` with the same configuration and the same resources, read
or not, whose conversions also have those images: the ones a document brings with it (a `data:` from whoever sends it,
an image the application already read and checked). The shared `Converter` does not change, and the new one is used for
that document and thrown away: creating it only costs copying the bytes, and it does not look at the folders again
(with `reloadResourcesEachConversion`, it looks at them in each conversion, like the shared one). An image given this
way goes before one of the resources with the same path. It is read when the document uses it, and what is wrong with
it is an error of that document, with the line of its `Image`. It serves for `layOut`, `toPdf` and `toSvg`: the
`LaidOutDocument` keeps the resources it was laid out with, with their images. `Resources.withImages(Map)` is the same
one level below, without copying the bytes.

> **Decision:** the images of a document go in a derived `Converter` and not in the call, like the attachments: they
> are needed when laying out and when painting, which can be two calls, and one more parameter in each `layOut`, `toPdf`
> and `toSvg` would double their variants. *Rejected alternative:* a new `Resources` per document, which would index
> the fonts again.

### Options of one request

```java
Converter request = converter.toBuilder().styles(stylesOfTheRequest).debug(true).build();
List<String> preview = request.toSvg(request.layOut(document));
```

`Converter.toBuilder()` gives a `Builder` with the configuration of the `Converter`, to build another one that differs
in some options: the separate styles of a request, its `debug`, its part of PDF/UA, its PDF/A, its `Check`s or its
`Limits`. What is built shares the resources of the `Converter` as it has read them (its view), without looking at the
folders again, as long as `resources`, `reloadResourcesEachConversion` and `preloadResources` do not change: if they do,
it takes a view of its own, like `Converter.builder()`. The styles of the `Converter` are in the `Builder`, and `styles`
adds after them, so what is added weighs more and is numbered after them (`stylesDocument()`); so are its images
(`withImages`). The shared `Converter` does not change. It is what a service does with a shared `Converter` and the
options of each request: it builds one on it for that request and throws it away.

> **Decision:** the options of a request are given with a `Builder` that comes from the `Converter`, and not with a
> `withStyles`, a `withDebug` and one per option, nor with an options parameter in each call: a single method, the
> checks of the `Builder` (`pdfUa(2)` with `pdfA`, the reading and the numbering of the styles) and no more variants of
> `layOut`, `toPdf` and `toSvg`. *Rejected alternative:* `Converter.builder().resources(converter.resources())`, which
> takes a view again and, with `preloadResources`, reads the whole folder again.

### Files that change

```java
Converter converter = Converter.create();
// ... files of resources/ are added, removed or replaced ...
converter = converter.reloaded();                                        // the new one sees them

Converter live = Converter.builder().reloadResourcesEachConversion(true).build();   // each conversion sees them
Converter eager = Converter.builder().preloadResources(true).build();              // reads the folder when created
Converter photo = Converter.builder().resources(new Resources().withImagesInMemory())
        .preloadResources(true).build();                                         // and keeps every image
```

A `Converter` reads each font and each image the first time it uses it and keeps it, whatever happens to its file: all
its conversions work with the same resources. The files added, removed or replaced afterwards are seen by the
`Converter` that `reloaded()` gives, with the same configuration. The first one does not change, and what it converts
afterwards still uses what it had read. It is called when the new files are all in place, so no conversion finds part
of them or one half written. Whoever shares the `Converter` among threads keeps the current one in an
`AtomicReference`.

With `reloadResourcesEachConversion(true)`, each conversion sees the folder as it is when it starts, without calling
anything. It costs a walk of `fonts/` and one look at the disk per image and conversion, and a conversion that starts
while the files are being copied can find some of them and not others, or one half written. In this mode `reloaded()`
gives an equivalent `Converter`.

With `preloadResources(true)`, the `Converter` reads the folder when it is created and in each `reloaded()`, instead of
reading each file the first time a conversion uses it. It indexes `fonts/` and reads every image of `images/` (also
those of its subfolders) that can be read, so it keeps the folder as it was at that moment. A font that cannot be
indexed makes `build()` or `reloaded()` fail with `RESOURCE`, instead of the first conversion, and the `Converter` that
was there goes on converting. An image that cannot be read (it is not an image, its format is not supported, it leaves
the folder through a link) or a PDF beyond `Limits.maxPdfBytes` is left for when a document uses it, which gives the
error with the line of its `Image`. It costs reading the whole folder, also the images that no document uses. It does
not go together with `reloadResourcesEachConversion(true)`, which would read the whole folder in each conversion:
`build()` fails with `IllegalArgumentException`.

Underneath, both use `Resources.snapshot()`: another `Resources` of the same folder, which shares with the first one and
with its other views what was already read. Like any `Resources`, it looks at the folder the first time it is asked for
the fonts or for each image and then keeps what it found:

- **Fonts.** It walks `fonts/` looking at the size and the modification time of each file, without reading it. If they
  are those of the last index of the family, it uses it; if a file was added, removed or replaced, it indexes the folder
  again.
- **Images.** It looks at the size and the date of the file. If they are those of the image already read, it uses it;
  if not, it reads it again and the new one replaces the old one for the `Resources` that ask for it afterwards, without
  changing the one of those that already had it. An SVG with text is also read again if the fonts were indexed again,
  because its text was checked with them.

The `Converter` takes a view when it is created and another one in `reloaded()`, or one per conversion in the other
mode. The `LaidOutDocument` keeps its own (`LaidOutDocument.resources()`), and `toPdf` and `toSvg` paint it with the
same fonts and images it was laid out with, also with a `Converter` reloaded afterwards. Without
`preloadResources(true)`, a file not used yet is read when it is first used, not when `reloaded()` is called.

The fonts and the SVGs are in memory, so a document already laid out is painted the same even if its file changes or
disappears. A PNG, a JPEG or a PDF is read again when painting, and before that `ImageFile.checkUnchanged` compares its
size and its date with those it had when it was read: if they do not match, the conversion ends in `IMAGE_FORMAT`,
without a line, instead of painting an image with the size of another; if the file can no longer be read (because it was
removed, for example), in `RESOURCE`, with its path. By default the `Converter` goes on laying out with the old image,
so its conversions with it fail the same way until `reloaded()`; in the other mode, the next conversion reads the new
file.

With `new Resources(folder).withImagesInMemory()`, a PNG, a JPEG or a PDF is also kept whole when it is read and is
painted with those bytes, without going back to the disk: a document already laid out is painted the same even if its
file changes or is removed, and the `Converter` goes on laying out and painting with the image read until `reloaded()`,
without failing. `imagesInMemory()` says whether a `Resources` does it. It costs keeping in memory every image read, as
large as its file: `maxPdfBytes` limits each PDF, not the total. It belongs to the `Resources` and not to the
`Converter`, because a `Resources` and its views share the images read and all of them have to keep them the same way;
the `Resources` that `withImagesInMemory()` gives does not share what the original had already read. With
`preloadResources(true)` it gives the complete photo of the folder when the `Converter` is created and in each
`reloaded()`.

A `Resources` used with `Engine`, `PdfDocumentWriter` or `PdfPainter` directly behaves the same: it reads each file the
first time and does not look at it again. To see the changes, they are given `resources.snapshot()`.

> **Decision:** by default the resources do not change while the `Converter` lives, and they are reloaded with
> `reloaded()`, which whoever changes the files calls: a conversion does not depend on the state of the disk at that
> moment, does not look at the disk in each conversion and does not see a folder half updated. `reloaded()` gives a new
> `Converter`, and does not change the existing one, so that the `Converter` stays immutable. Reloading in each
> conversion remains as an option of the `builder` for whoever does not know when the files change (a preview of files
> edited by hand). Reading is lazy by default, because a folder can have many images that no document uses, and
> preloading remains as another option for whoever wants to see the folder as it is when reloading and the errors of the
> fonts before converting. The preload skips the images it cannot read, because `images/` can hold files that are not
> images and their error is clearer with the line of the document that uses them. Not even with it is it a complete
> photo: of the PNG, the JPEG and the PDF it keeps the format and the size, and their content is read again when
> painting, except with `withImagesInMemory()`, which is optional because those files can be large, and in a small
> folder updated underneath (a synchronized volume, a temporary folder) it pays off. It goes in `Resources` because
> what is kept is shared by the whole family of views. Without it, the SVG is kept in memory anyway, and the PNG, the
> JPEG and the PDF are compared by their size and their date. A file replaced by another of the same size with the date of the original
> (as `cp -p`, `rsync -t` or a zip when it is extracted do) is not detected, nor a change between the check and the
> reading. *Rejected alternatives:* only reloading in each conversion (0.10.0), which made everyone pay the look at the
> disk and see folders half copied; only the explicit reload, which made a preview call it in each request; a `reload()`
> that changes the `Converter`, which stops being immutable; a `reloaded()` that reads everything again, which would close
> the `cp -p` gap at the cost of indexing all the fonts in each reload; looking at the disk at each use of a file, which
> is in each measurement; reading the bytes once when painting and checking on them the format and the size that were
> measured; `keepImagesInMemory(true)` in the `builder`, which made the `Converter` create underneath another
> `Resources` that does not share what it read with the one it receives.

## Errors

Every error of the engine is a `StxtPdfException` (thrown by `DocumentReader` and `Converter`), so a single `catch` is enough.
`kind()` says whose fault it is, `code()` what fails and `line()` where (`NO_LINE`, 0, if it does not come from a line).
`inStyles()` says whether that line belongs to a [separate styles document](../common/styles.md#separate-styles-document) and not to the document,
and `stylesDocument()`, to which one: 1 for the first one that was passed, 2 for the second one, and 0 if the line belongs to the document:

```java
try {
    converter.toPdf(doc, out);
} catch (StxtPdfException e) {
    switch (e.kind()) {
        case INVALID_DOCUMENT -> reply(422, e.code() + " on line " + e.line()
                + (e.inStyles() ? " of styles " + e.stylesDocument() : "") + ": " + e.getMessage());
        case LIMIT -> reply(413, e.getMessage());
        case CANCELLED -> reply(503, e.getMessage());            // the service itself asked to stop
        case RESOURCE, INTERNAL -> reply(500, e.getMessage());   // from the configuration or from the engine
    }
}
```

| Class | What it is |
|---|---|
| `InvalidModelException` (`tree`) | The rules of the model, the overflows and the resources the document names. `errorCode()` gives the `ErrorCode` and `ErrorCode.kind()` its kind (`INVALID_DOCUMENT` except `PAGE_LIMIT`, `IMAGE_TOO_LARGE`, `SVG_TOO_COMPLEX` and `PDF_TOO_LARGE`, which are `LIMIT`) |
| `DocumentSyntaxException` (`tree`) | The syntax and the schema of `stxt-core`, with its code and its line and the original exception as the cause. A `LimitException` from the library is `LIMIT` |
| `ResourceException` (`resources`) | The resources folder (`RESOURCE`), with `ImageFormatException`: also a file that cannot be read or resolved (a damaged font, a broken link, a folder without read permission), with its path in the message |
| `InternalException` (`model`) | A failure of the engine. The original cause is in `getCause()` |
| `CancelledException` (`model`) | The conversion stopped by its `Cancellation` (`CANCELLED`), without a line ([Canceling a conversion](#canceling-a-conversion)) |

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

## Canceling a conversion

```java
converter.toPdf(doc, out, Attachments.NONE, Cancellation.timeout(Duration.ofSeconds(60)));   // a deadline per PDF
```

`Cancellation` (`model`) says whether the conversion has to stop: a functional interface with `requested()`, which the
engine asks wherever it works. It is passed with the call (`layOut`, `toPdf` and `toSvg` have an overload with it, and
another with it and a [`Progress`](#progress)) or,
for every conversion of a `Converter`, with `Converter.builder().cancellation(...)`; with both, it stops when either
asks. The first time it answers `true`, the conversion ends with a `CancelledException`, of kind `CANCELLED` and without
a line ([Errors](../common/errors.md#error-types)), which goes through the engine without anything wrapping it, and
nothing has been written to the destination: the PDF is written whole at the end.

| `Cancellation` | When it stops |
|---|---|
| `NONE` | Never; it is the default |
| `deadline(Instant)` | From that moment on, measured with the monotonic clock of the JVM from the moment it is created: a later change of the system time does not move it |
| `timeout(Duration)` | Once that time has passed since it is created; zero or negative, at once |
| `whenInterrupted()` | While the thread that converts is interrupted (`Thread.isInterrupted()`), without clearing the flag |
| `a.or(b)` | When either of the two asks |
| Any `() -> boolean` | When it returns `true`; it has to be cheap and must not block, because it is asked thousands of times |

Where it is asked: `BoxTreeBuilder`, at each element (a `Barcode` is generated when the tree is built); `Engine`, at
each layout that is not in the cache (`layoutOnce` and `contentIntrinsic`); `Paginator`, at each pass of the convergence
of the references and at each page, when paginating the flow and when placing the `Page overlay` boxes; `Overflow`,
`ModuleSizes` and `ContrastCheck`, at each page; `PdfDocumentWriter`, at each page it paints and before saving; `PdfImages`, at each image
it embeds or draws for the first time, and `SvgCanvas.pages` (that of `Converter.toSvg`), at each page. Each question costs one reading of the
monotonic clock or of the flag of the thread. What is not cut short is a single call to PDFBox, JSVG or ZXing (drawing an
SVG, generating a code, saving the PDF), which finishes on its own; the [limits](#limits) bound how long each can take.

> **Decision:** the cancellation is an object of its own that the engine asks, and not only the interruption of the
> thread: a deadline is expressed without threads and tested without them, it does not depend on who interrupts, and
> `whenInterrupted()` gives the thread's one to whoever wants it. *Rejected alternative:* looking only at
> `Thread.interrupted()`, which requires one thread per conversion and someone to interrupt it in time.

> **Decision:** it is passed with the call, like the attachments, because a deadline belongs to each conversion and the
> `Converter` is shared; the `builder` also takes it as a default, for the thread's one, which is the same for all.
> *Rejected alternative:* only in the `builder`, which would require one `Converter` per deadline.

> **Decision:** `CANCELLED` is a kind of error of its own and not a `LIMIT`: it is neither the document's fault nor the
> environment's, but a decision of the caller, and a service does not respond the same way (its own deadline is a 503 or
> a 504; a client that left, nothing). A `switch` over `Kind` has to cover it. *Rejected alternative:* a `LIMIT` without
> a line, which a service would answer as a 413.

> **Decision:** it is asked where the work is and not between phases: a single page with thousands of QR codes can take
> seconds to build, and checking only between pages would not cut it short. *Rejected alternative:* checking only
> between pages in the `Paginator`.

## Progress

```java
converter.toPdf(doc, out, Attachments.NONE, Cancellation.NONE, new Progress() {
    @Override
    public void pagePainted(int number, int pages) {
        bar.set(number, pages);   // a progress bar
    }
});
```

`Progress` (`model`) is who the conversion tells how it goes. It is passed with the call (`layOut`, `toPdf` and `toSvg`
have an overload with it, after the `Cancellation`) and the engine calls it, on the thread that converts, as the work
advances. All its methods have an empty implementation: only what is listened to is overridden, and `Progress.NONE`
listens to nothing.

| `Progress` | When it is called |
|---|---|
| `pageLaidOut(number)` | The `Paginator` has laid out a page of the flow: `number`, from 1, within the current pass. A document with references (`{page of: id}`) or whose total of pages (`{pages}`) changes what has been laid out is paginated again ([References](../common/references.md)), and the count starts from 1 again in each pass |
| `laidOut(pages)` | The layout has finished, with that total of pages, before anything is painted. It is the end of `layOut` |
| `pagePainted(number, pages)` | A page is painted in the PDF, or its SVG made |
| `done()` | The conversion has finished: the PDF is written to the destination (or into the `PDDocument`), or every SVG is made. `layOut` alone does not call it |

What an implementation throws ends the conversion as an `InternalException`, with what was thrown as its cause: it is
neither an error of the engine nor of the document, and a progress bar has no reason to stop anything.

> **Decision:** it is passed only with the call and not in the `builder`: a `Converter` is shared across conversions
> and across threads, and a notice that does not say which conversion it is about is of no use to a progress bar.
> *Rejected alternative:* a `progress(...)` in the `builder`, like the cancellation, which does make sense there because
> the thread's one is the same for all.

> **Decision:** `pageLaidOut` counts every page of every pass and not only those of the last one: during the first pass
> it is not known whether there will be another, and a bar that advances and starts over tells the truth. *Rejected
> alternative:* telling only in the final phase of the `Paginator`, which is fast because the flow is already in the
> cache, and would not count what takes time.

## Integration with PDFBox

Two ways of using the engine from a program that already works with PDFBox: writing the document into an open
`PDDocument`, which the caller saves after adding what the engine does not write, and painting a laid-out page on a
page of any document.

### Into an open `PDDocument`

```java
try (PDDocument pdf = new PDDocument()) {
    converter.toPdf(laidOut, pdf);                                      // or toPdf(doc, pdf)
    pdf.getDocumentInformation().setCustomMetadataValue("poliza", "123");   // what the engine does not write
    stamp(pdf);                                                         // a watermark on every page
    pdf.protect(policy);                                                // encryption
    pdf.save(out);
}
```

`toPdf(document, PDDocument)` writes into the document the same as into a file: the pages, the fonts, the structure
tree, the bookmarks, the links, the page labels, the metadata and the PDF/A declaration, and with the long overload
(`Attachments`, `Cancellation`, `Progress`), the attachments. Two differences: **it does not save it**, the caller does,
and so the file identifier (`/ID`) is the one PDFBox gives it when saving and not the one of the content
([The PDF inside](pdf.md#pages-fonts-and-metadata)): two saves do not give the same bytes. The `PDDocument` has to be
**without pages** (anything else is an `IllegalArgumentException`): a document is written whole, because its structure
tree, its page labels and its metadata belong to the whole PDF. What the `PDDocument` had (keys of the `Info`
dictionary) is kept, except what the document writes: the title, the subject, the language and the XMP packet, which is
replaced whole; that is why the caller's own keys are set afterwards. If the conversion is canceled, the document is
left half written. Underneath, `PdfDocumentWriter.write` has the same overload with a `PDDocument`.

> **Decision:** the `PDDocument` has to be empty, and what already has pages is done with `PdfPainter`: adding a tagged
> document to another that already has a structure tree is merging the two trees, their `ParentTree`, their `RoleMap`
> and their `IDTree`, which is what PDFBox's `PDFMergerUtility` does from two PDFs already saved. *Rejected alternative:*
> adding the pages at the end of a document with pages, with or without tagging.

> **Decision:** the caller's own `Info` keys and the blend mode of a watermark do not go into the engine: with the open
> `PDDocument` they are written with PDFBox before saving, without reopening the file, and the engine does not have to
> know about document management systems or watermarks. *Rejected alternative:* a `Meta` in `Document` and a `Blend` in
> `Page overlay`, which is what the first consumer of the engine asked for.

### A page on a page of another document

```java
try (PDDocument form = Loader.loadPDF(formFile);
     PdfPainter painter = new PdfPainter(form, converter.resources(), converter.limits())) {
    painter.paint(laidOut.pages().get(0), form.getPage(0));   // the first page, on the form
    form.save(out);
}
```

`PdfPainter` (`paint`) paints laid-out pages, one at a time, on pages of a document that is not the engine's: to stamp a
page on a form, or to put the pages of a document in another PDF. Each `paint(page, PDPage)` appends the content of the
page **after** what the PDF page already has, with the top left corner of the laid-out page at the top left corner of
the `MediaBox` of the `PDPage` (its `Rotate` is not taken into account), and adds its link annotations to those of the
page. The fonts and the images are embedded once per painter. The page is laid out by the engine (`laidOut.pages()`)
with the same `Resources` the painter is given.

**Nothing is tagged.** The document the pages are painted on has its own structure tree, or none, and the engine does
not touch it: every content is an *artifact* (the text is still copied and searched) and the link annotations hang from
no `Link`. A document that has to be PDF/UA is written whole, with `toPdf`.

The links to a URL work from the start. The links to a box of the document (`#id`) get their destination when the
painter is **closed**, if the page that has it was painted with that same painter; otherwise their annotation is
removed. Closing the painter also closes the PDFs it drew pages of; the document is not saved, the caller saves it. A
painter belongs to one document and one thread. It takes a `Cancellation`, which is asked before each page and at each
image.

> **Decision:** on a page of another document nothing is tagged: tagging would require merging the structure tree of
> that document with the engine's, and without a tree the PDF is still valid (an *artifact* is legitimate content of a
> tagged PDF). Whoever wants PDF/UA writes the whole document. *Rejected alternative:* creating the structure tree when
> the other document has none, which would leave a half-tagged PDF.

> **Decision:** an internal link whose destination was not painted is removed and not left without a destination: a
> link annotation without an action or a destination looks like a link and leads nowhere. *Rejected alternative:* an
> error, which would prevent painting a single page of a document with links between its pages.

## Threads

The recommended way is to create **one `Converter` and share it**: it is immutable and each conversion creates its own engine, so
two threads converting at the same time share no state that changes.

| Piece | Shared across threads? |
|---|---|
| `Converter`, `Resources`, `DocumentReader` | Yes. They are created once and used from all threads |
| `LaidOutDocument` (what `layOut` returns) | Yes, to paint it: the same laid-out document can be written to PDF and to SVG from several threads. It is handed to the other threads with a `Future`, an executor or a `volatile` field |
| `Engine` | **No.** It keeps the layout cache and the numbering state. One per conversion, and reusing it in sequence is safe (it releases everything at the end of each document) |
| `Box` and `Style` (the box tree) | Read-only once built |
| `PdfPainter` | **No.** It belongs to a `PDDocument`, which PDFBox does not share across threads |

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

`DocumentSchema.text()` gives the text of that schema as the jar has it, the same one the engine validates with: it is
for those who publish it or give it to an editor (`GET /schema` of `stxt-press`), without depending on where the jar
keeps it.

## The version of the engine

`Converter.version()` gives the version of the engine in use (`0.13.0`): a service can say which version made a PDF.
Maven writes it when it builds the jar, in a resource of the engine, `dev/stxt/pdf/version.properties`.

> **Decision:** the version comes from a resource of its own and not from the manifest. A jar that joins the engine with
> its dependencies (the one of `stxt-press`) keeps a single manifest, its own, and
> `Converter.class.getPackage().getImplementationVersion()` gives there the version of that jar. The manifest has
> `Implementation-Title` and `Implementation-Version` anyway. *Rejected alternative:* reading
> `META-INF/maven/dev.stxt/stxt-pdf/pom.properties`, which is a detail of Maven and does not exist when another tool
> builds the jar.
