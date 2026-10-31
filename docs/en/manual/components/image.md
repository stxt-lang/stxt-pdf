# `Image`

A PNG, JPEG or SVG image, or a page of a PDF, from the `images/` folder of the resources. It is a box like any other: it
has its size, its margins, its border, its corners, and it is placed in the flow or in a `Stack`, a `Grid` or a positioned
element.

```
Document (dev.stxt.pdf):
    Language: en
    Title: Catalog
    Page:
        Paper: A4
        Margin: 40
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
    Section:
        Image: examples/brand/nogal.svg
            Alt: Nogal Workshop
            Box:
                Width: 120
                Margin: 0 0 20 0
        Grid:
            Column sizes: * *
            Gap: 16
            Area:
                Image: examples/catalog/silla.jpg
                    Alt: Walnut chair with a wool seat
                    Fit: fill
                    Box:
                        Height: 160
                        Corner radius: 6
            Area:
                Image: examples/catalog/mesa.jpg
                    Alt: Walnut dining table
                    Fit: fill
                    Box:
                        Height: 160
                        Corner radius: 6
```

## Properties

| Property | What it does |
|---|---|
| *(value)* | The path of the file inside `images/`: `Image: logos/acme.png`. Required |
| `Alt` | The alternative text, for those who cannot see the image. Without `Alt`, the image is decorative and the screen reader skips it |
| `Fit` | How the image fills its box if they do not have the same proportions: `inside` (whole, with space at the sides), `fill` (fills the box and crops what is left over) or `stretch` (it is distorted) |
| `Pdf page` | The page of a PDF that is drawn, from 1. Without `Pdf page`, the first one. Only in a PDF |
| `Rotate` | Rotating the image around its center. It takes up the rectangle that encloses it rotated ([Layers and rotation](../common/layers-and-rotation.md#rotating-a-box-rotate)) |

It also accepts `Id`, `Style`, `Link`, `Box` and `Break` (without `Keep together` or `Edges`: it is not split). It does not
accept `Typography`.

## The size

- Without `Width` or `Height`, an image has its **natural size**: each pixel, 0.75 points. An image of 400 × 300 pixels
  measures 300 × 225 points. An SVG measures what its `width` and its `height` (or its `viewBox`) say, and the page of a
  PDF, what the page measures (an A4, 595 × 842).
- It does not stretch to the width of the page on its own: if it does not fit, it is an error. To take up the full width,
  `Width: 100%`.
- With `Width` or `Height`, the other side comes from the proportions of the image. With both, the box has that size and
  `Fit` decides how the image fills it.
- `Margin: 0 auto` centers it.
- The padding and the border go around it: they do not distort the image.
- In a `Stack` in a row, an image is not stretched vertically: it keeps its proportions.

## Across pages

An image is not split: if it does not fit in what is left of the page, it moves whole to the next one. An image taller than
an empty page is an error.

## SVG

An SVG goes into the PDF as a vector drawing: it looks sharp at any size and takes up little space. A closed subset is
accepted: shapes, paths, groups, gradients, clipping paths, `use` and text. Anything else (scripts, filters, masks, patterns,
images, style sheets, references to other files) is an error, so an SVG exported from an editor sometimes has to be cleaned
up. The text of an SVG must use a font from `fonts/`, and it is drawn as outlines: it looks the same, but it cannot be
selected.

## A page of a PDF

An `Image` of a PDF draws one of its pages, the one in `Pdf page`, like any other image: a plan, a chart or a drawing that
comes as a PDF, or an SVG that the engine does not accept, converted to PDF with another program. It goes in as a vector
drawing, sharp at any size, and its text can be selected.

```
Image: planos/sala.pdf
    Pdf page: 2
    Alt: Plan of the meeting room
    Box:
        Width: 100%
```

It also serves as **preprinted paper**: a letterhead, a form, a background. It is placed in a `Page overlay` behind the
content (a negative `Layer`) that reaches the edge of the page with a negative `Offset` equal to the margin, and the image
takes up the whole `Page overlay` with `Width` and `Height` at 100%. The margin of the document leaves free the room for
the letterhead and the footer:

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
    Section:
        Page overlay:
            Offset: -140 -72 -80 -72
            Layer: -1
            Image: examples/letterhead/letterhead.pdf
                Box:
                    Width: 100%
                    Height: 100%
        Paragraph: Olot, October 20, 2025
        Paragraph: We are sending you the proposal for renewing the dining room furniture, with the details of each piece and its delivery time.
```

With two `Page overlay` elements, one with `Pages: first` and another with `Pages: not-first` and `Pdf page: 2`, the first
sheet has one letterhead and the following ones, another.

Whatever is not the drawing of the page is not copied: its links, its bookmarks and its tagging. Its layers are. A PDF with
a filled-in form field or a visible comment is an error: it has to be flattened first, for example by printing it to PDF.
So is an encrypted PDF and one with fonts that are not embedded or that do not say which character each glyph is
(`ToUnicode`). The checks on annotations and fonts can be turned off when converting
([Usage from Java](../common/java-usage.md)). The one on encryption cannot.

## Accessibility

An image with `Alt` is a figure of the tagged PDF, with that alternative text. Without `Alt` it is decoration: a background,
an ornament. Every image that conveys something needs its `Alt`. So does the page of a PDF: even though its text can be
selected, the screen reader reads the `Alt`. Preprinted paper is decoration: it goes without `Alt`.

## Limits

For security there are some limits, which can be changed when converting ([Usage from Java](../common/java-usage.md)):

- **An image**: at most 25 million pixels.
- **An SVG**: at most 20,000 elements and 64 levels of nesting.
- **A PDF**: at most 20 MB.

More detail in the [reference](../../reference/components/image.md).
