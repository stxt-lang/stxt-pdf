# `Image`

An `Image` puts in the document a PNG, JPEG or SVG image, or a page of a PDF, from the `images/` folder of the resources
([Resources](../common/resources.md)). It is a box without children, like a `Text`, whose content is the image: it is placed,
sized, broken across pages and painted like any box, with its `Box` (margins, padding, border, background and
corners) and its `Break` (without `Keep together` or `Edges`, because it is not split). It also accepts `Id`, `Style` and `Link`. It does not accept
`Typography`, because it has no text.

```
Document (dev.stxt.pdf):
    Language: en
    Title: Report
    Page:
        Paper: A4
        Margin: 40
    Section:
        Page overlay:
            Offset: -30 0 auto auto
            Image: test/square.png
                Box:
                    Width: 20
        Image: test/wide.png
            Alt: A landscape with mountains and a sun
            Box:
                Width: 100%
                Margin: 0 0 10 0
        Stack:
            Gap: 10
            Item:
                Image: test/tall.jpg
                    Alt: A portrait
                    Box:
                        Height: 60
            Item:
                Grow: 1
                Image: test/wide.png
                    Fit: fill
                    Box:
                        Height: 60
```

(The example uses the test images of the repository, in `resources/images/test/`: a logo in the corner of each
page, an image across the full width and, below it, a portrait of 45 × 60 and the same image cropped to the remaining width.)

| Property | Values | Default | Notes |
|---|---|---|---|
| *(value)* | path relative to `images/` | required | The PNG, JPEG, SVG or PDF file: `Image: logos/acme.png`. |
| `Alt` | non-empty text | none | Alternative text: with it, the image is a figure that is read ([accessibility](#accessibility)). Without it, it is decorative. |
| `Fit` | `inside`, `fill`, `stretch` | `inside` | How the image fills its content box if they have different proportions. |
| `Pdf page` | integer ≥ 1 | `1` | The page of a PDF that is drawn ([PDF](#pdf)). In another format it is an error. |
| `Rotate` | see [Layers and rotation](../common/layers-and-rotation.md#rotation-in-flow-block-text-paragraph-and-image) | none | Rotates the image around its center (a seal) and takes up the rectangle that encloses it rotated. |

They do not go in the rules of `Styles`, which only accept the groups.

## File

The value is the path of the file, relative to `images/` and always downward ([Resources](../common/resources.md)):
`Image: logos/acme.png` is `resources/images/logos/acme.png`. PNG (with transparency), JPEG, SVG and PDF are accepted, and the
format is recognized by the content and not by the extension (an SVG is a file that starts with `<`, and a PDF, one that
starts with `%PDF-`). An `Image` without a path, a file that does not exist or a path that goes outside `images/` are an error
(`IMAGE_UNKNOWN`). A file that is not PNG, JPEG, PDF or an accepted SVG (such as a GIF or a WebP), or that cannot be read,
is `IMAGE_FORMAT`. The errors point to the line of the `Image`.

An XML file that is not an SVG is also `IMAGE_FORMAT`. An empty `Alt` is `VALUE_NOT_ALLOWED`.

> **Decision:** the format is recognized by the content. *Rejected alternative:* requiring the `.svg` extension.

## Natural size

Each pixel measures 0.75 pt (96 pixels per inch), regardless of the resolution the file declares: an image of
200 × 100 pixels measures 150 × 75 pt. For another size, `Width` or `Height` is declared. That of an SVG comes from its `width` and
`height` (in `px`, `pt`, `pc`, `in`, `cm` or `mm`, and without a unit, in pixels) or, if they are missing, from its `viewBox`, also at 96 pixels
per inch. With only one of the two sides and a `viewBox`, the other comes from its proportion. Without a size, or with relative
measurements (`%`, `em`), an SVG has no natural size and is an error (`IMAGE_FORMAT`). A PDF is already in points: its natural
size is that of the page that is drawn, that of its crop (what a viewer shows), and rotated if the page is ([PDF](#pdf)).

## Sizing

The content box of an image has the **proportion** of the file, or that of `Aspect ratio` if it is declared, which
replaces it (it is not an error). The padding and the border go outside: they do not distort it.

- With `Width` and `Height` at `auto`, it measures its natural size, **also in block flow**: it is not stretched to the width of the parent.
  If it does not fit, it is an overflow (`BOX_OVERFLOW`). For it to take up the available width, `Width: 100%` is declared.
  `Margin: 0 auto` centers it, because it does not take up the whole width.
- With one axis declared, or imposed by the parent (an item of `Stack` with `Grow`, a column of `Grid` with
  `Horizontal: stretch`), the other comes from the proportion. With both, the box measures what is declared and `Fit` decides how the image
  fills it.
- `Min width`, `Max width`, `Min height` and `Max height` apply as in any box. If both axes are `auto` and the
  height runs into its minimum or its maximum, the width is deduced again from the limited height: the proportion is kept.
- Its height is always definite, so a `Stack` does not stretch it in the cross axis of a row, nor does a `Grid` vertically. Horizontally
  it is stretched, and the height follows the proportion.
- **Intrinsic sizes:** the narrowest width and the natural width are both the width of its natural size or, if it declares
  `Height` in points, the one that comes from it with the proportion. It is a real minimum: in a `*` track of a grid or in an
  item of `Stack` without `Min width`, the image does not shrink below it.

If the minimums and the maximums cannot be met at the same time with the proportion, the width wins and `Fit` places the image.

> **Decision:** the height of an image always comes from the proportion. *Rejected alternative:* stretching it and letting `Fit`
> place it.

> **Decision:** the intrinsic sizes are a real minimum, also with a `Width` in percent. *Rejected
> alternative:* a narrowest width of 0, which lets the image shrink.

## `Fit`

If the content box and the image have different proportions:

- `inside` (default) draws the whole image, as large as fits and centered, and lets the background show at the sides.
- `fill` draws it as small as covers the box, centered, and crops what is left over.
- `stretch` stretches it to the box and distorts it.

> **Decision:** the values say what happens to the image: it fits inside (`inside`), it fills the box (`fill`) or it is stretched
> (`stretch`). *Rejected alternative:* `contain` and `cover`, which cannot be understood without knowing them beforehand.

With the same proportion, the three give the same result. An image is also clipped by the rounded corners of its content box ([The box](../common/box.md#corners)).

## Page breaks

An image is not split: if it does not fit in what is left of the page (or of the column of a `Columns`), it moves whole to the
next one, like a line of text. If it does not fit even on a whole page, it is `PAGE_OVERFLOW`, with the line of its `Height` (or,
without it, of its `Width`). `Before` and `After` work as in any box. `Keep together` and `Edges`, which only matter when
splitting a box, are an error (`BREAK_IN_IMAGE`), also if a rule of `Styles` gives them.

## Where it goes

Wherever an in-flow element goes: in a section, `Block`, `Frame`, `Overlay`, `Page overlay` (a logo repeated on every
page), `Columns`, `List item` (but not at the beginning: the marker needs a line to align with,
`LIST_ITEM_WITHOUT_LINE`), `Cell` and as the element of an `Item` or an `Area`. In a `Markdown`, an image alone in its paragraph
(`![Alt](path)`) is an `Image` ([Markdown](markdown.md)).

## SVG

An SVG is a vector drawing: the PDF carries it as paths, fills, gradients and clips and not as pixels, so it looks
sharp at any size and is small. It is sized, placed (`Fit` included) and broken like any image. It is an
XML file in the SVG namespace, and a closed subset is accepted: `svg` (without another `svg` inside), `g`, `defs`,
`title`, `desc`, `rect`, `circle`, `ellipse`, `line`, `polyline`, `polygon`, `path`, `linearGradient`, `radialGradient`,
`stop`, `clipPath`, `use`, `text` and `tspan`, with their presentation attributes (plain or gradient fills and strokes,
opacity, `transform`, clips). Any other element (such as scripts, animation, filters, masks, patterns, markers, `image`,
`style`, `symbol` or `metadata`), a property with an effect outside the subset (`filter`, `mask`, `marker`), an attribute
from another namespace (except `xlink` and `xml`) or an event handler (such as `onclick`) is an error (`IMAGE_FORMAT`,
with the line of the SVG in the message): nothing is silently ignored. Nothing from outside the file is accepted either: an `href` or a
`url(...)` can only lead to a `#id` of the SVG itself, and a DTD or an entity are an error.

The text of an SVG (`text`, `tspan`) uses only the fonts of `fonts/`, without substitution: its `font-family` (only the
first of the list counts, and it cannot be a generic one such as `sans-serif`), its `font-weight` (`normal` or `bold`) and its `font-style`
(`normal` or `italic`), its own or inherited from its ancestors in the file (a `g`, or the `text` of a `tspan`), must be
a variant that exists, with all the characters of the text. That variant is the one that draws it, always: a text repeated by
a `use` keeps the font of its place in the file and does not inherit that of the `use`. It is drawn with the outlines of the font: it
looks like any text of the document, but it cannot be selected or copied.

> **Decision:** the SVG subset is closed and strict: nothing is silently ignored, at the cost that an SVG exported
> from an editor usually needs cleaning up. *Rejected alternative:* ignoring what is not drawn, or the metadata of the
> editors.

> **Decision:** the text of an SVG uses the first family of `font-family`, without substitution, and without `font-family` it is an error.
> *Rejected alternative:* trying the families of the list in order.

> **Decision:** the text of an SVG is not text of the PDF: it looks the same, but it cannot be selected. In an accessible
> document, the text goes in the document, the SVG is a drawing and `Alt` gives the alternative text.

## PDF

An `Image` of a PDF draws **one of its pages**, that of `Pdf page` (the first one, if it is not declared), as one more image: it is
sized, placed, cropped and rotated like any other. It serves to put in the document a plan, a chart or
an illustration that come as PDF, or a drawing that the [SVG](#svg) subset does not accept, converted to PDF. It also serves
as a **page background**: a letterhead paper or a preprinted form, in a `Page overlay` behind the content.

The page goes as a vector drawing, with its content as it is: its paths, its images and its text, which looks sharp at
any size and **can be selected and copied**, with its fonts. Each page is copied once into the PDF, even if it is
used on several pages of the document. What is not content of the page is not copied:

- **The annotations.** A link of the page is not copied (what is seen of it, the text, is). An annotation that is visible, such as
  a filled-in form field, a comment or a stamp, would not be drawn, so it is an error (`IMAGE_FORMAT`): the PDF
  is flattened before using it, for example by printing it to PDF. The caller can remove this check
  (`Check.PDF_IMAGE_ANNOTATIONS`, [Using from Java](../implementation/api.md#checks)), and then the page is
  drawn without them.
- **The tagging.** The page is an image: with `Alt` it is a figure and a screen reader announces that text and not
  that of the page. Without `Alt` it is decoration, like a background ([Accessibility](#accessibility)). Its bookmarks are not copied either.

The **layers** of the page (its optional content) are copied, because they decide what is seen: a layer that is off stays
off, and the viewer shows them in its list of layers. A layer configuration without a name receives that of the file
(`planos.pdf`), which PDF/UA requires, and none keeps its automatic states (`AS`, which turn a layer on or off when
zooming, printing or exporting), which PDF/UA does not allow: each layer stays in the state the configuration gives it.

`Pdf page` is the physical page, from 1, and not the number printed on it. A page that the PDF does not have is an error
(`IMAGE_PAGE`, with the line of `Pdf page`), and so is `Pdf page` in a PNG, a JPEG or an SVG, which has no pages. An
encrypted PDF, even if it opens without a password, is an error (`IMAGE_FORMAT`, with the line of the `Image`), and so is a
page with a font that does not comply with PDF/UA:

- not embedded: the viewer would replace it with another one;
- that does not say which character a glyph it draws is, neither with its `ToUnicode` nor, in a simple font that is not
  symbolic, with its encoding: its text could not be copied or read aloud;
- that draws a glyph it does not have (`.notdef`), or with a width different from that of its font program by more than
  one unit in 1000 (invisible text does not count);
- a TrueType font that is not symbolic without the encoding `WinAnsiEncoding` or `MacRomanEncoding`, or with glyph names
  outside the Adobe Glyph List;
- a symbolic TrueType font with `Encoding`, or with a `cmap` table of several encodings and without the Microsoft Symbol
  one (3, 0);
- a Type 2 CIDFont without `CIDToGIDMap`.

The fonts of the page and those of its forms, its patterns and its Type 3 fonts are looked at, and the glyphs they draw.
The caller can remove this check (`Check.PDF_IMAGE_FONTS`), and the PDF may not comply with PDF/UA. That of an encrypted
PDF cannot be removed. A PDF of more bytes than the limit is `PDF_TOO_LARGE` ([Limits](#limits)).

A letter paper with the full letterhead on the first page and the small logo on the others (pages 1 and 2 of
`letterhead.pdf`), behind the content and full-bleed: a `Page overlay` with a negative `Layer` that goes out of the page area with a
negative `Offset` equal to the margin, and an image with `Width` and `Height` at 100% of it. The margins of the document leave the
room for the letterhead and the footer, which the content cannot overlap:

```
Document (dev.stxt.pdf):
    Language: en
    Title: Letter
    Page:
        Paper: A4
        Margin: 140 72 80 72
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Serif
                Size: 10.5
                Line height: 16
        For style: letterhead
            Box:
                Width: 100%
                Height: 100%
    Section:
        Page overlay:
            Pages: first
            Offset: -140 -72 -80 -72
            Layer: -1
            Image: examples/letterhead/letterhead.pdf
                Style: letterhead
        Page overlay:
            Pages: not-first
            Offset: -140 -72 -80 -72
            Layer: -1
            Image: examples/letterhead/letterhead.pdf
                Pdf page: 2
                Style: letterhead
        Paragraph: Olot, October 20, 2025
        Paragraph: We are sending you the proposal for renewing the dining room furniture.
        Paragraph: The general terms are on the next page.
            Break:
                After: page
        Paragraph: General terms of the proposal.
```

> **Decision:** a page of a PDF is an image, with everything an image has, and a letterhead paper is a `Page overlay` with
> that image. *Rejected alternative:* an element of its own to insert whole pages of another PDF into the document, or a
> page background of the section's own.

> **Decision:** the content of the page is copied, without its tagging: the `Image` tags it as a figure or as
> decoration. *Rejected alternative:* grafting the structure tree of the source PDF, which does not always exist or fit
> into that of the document.

> **Decision:** an annotation that is visible is an error and its appearance is not drawn. *Rejected alternative:* flattening it when
> copying it, which forces interpreting each type of annotation, or silently ignoring it.

> **Decision:** what prevents the page from looking the same or being accessible (fonts, annotations) is checked by
> default, and the caller can remove it. What prevents reading it (an encrypted or damaged PDF) cannot be removed. *Rejected alternative:* a
> property of the document, which puts a decision of the caller in the document.

> **Decision:** of the PDF/UA rules about fonts (ISO 14289-1, 7.21), the ones checked are those of the list above, which are
> the ones veraPDF rejects in PDF files from other programs. Left out are those of a Type 0 font that only a full validator
> checks: the `CIDSystemInfo` of its CMap, its `CIDSet` and that a CMap that is not predefined is embedded.
> *Rejected alternative:* checking all of them, which is repeating a PDF/UA validator.

> **Decision:** the automatic states of the layers (`AS`) are removed when copying them, because PDF/UA does not allow them:
> a layer no longer turns on or off by itself when zooming or printing. *Rejected alternative:* rejecting the PDF
> (`IMAGE_FORMAT`), which forces it to be made again for something that does not change what is seen when it is opened.

## Limits

An image cannot ask for more than the [limits](../common/limits.md) allow: a PNG or a JPEG, more pixels than
`maxImagePixels` (`IMAGE_TOO_LARGE`), and an SVG, more elements or levels than `maxSvgElements` and `maxSvgDepth`
(`SVG_TOO_COMPLEX`), and a PDF, more bytes than `maxPdfBytes` (`PDF_TOO_LARGE`, 20 MB by default), always with the line of
the `Image` and before layout. A `<use>` counts everything it repeats each time it is used, and a `<use>` that points to an element that contains it is `IMAGE_FORMAT`.

## Accessibility

With `Alt`, a non-empty text, the image is a figure that a screen reader announces with that text. Without `Alt` it is
decorative and is not read, like the backgrounds. Inside a `Page overlay` it is always decorative, and in the repeated rows of a
table it is only read the first time. A page of a PDF, the same: its text can be selected, but the screen reader
announces the `Alt` and does not read the page.

> **Decision:** an `Image` without `Alt` is decorative, without a warning. *Rejected alternative:* making `Alt` required, with an
> empty `Alt` for the decorative ones, if an oversight of the author turns out to be a problem.

## Output

The PDF carries each file only once, even if it is used on several pages, with the resolution of the file: the JPEG as it is and
the PNG lossless and with its transparency. An SVG or a page of a PDF are scaled to their box like a PNG, but they are still
a vector drawing, sharp at any size (`stretch` distorts the drawing).

## What is not supported

- GIF, WebP, TIFF and other formats.
- Images inside a line of text.
- Several resolutions of one image.
- Placing the image inside its box somewhere other than the center, or at its natural size without scaling.
- In SVG files: patterns, masks, filters, markers, images, style sheets and links to other files.
- In PDF files: inserting their pages as pages of the document, with their size, and copying their links, form
  fields, comments, bookmarks and tagging.
