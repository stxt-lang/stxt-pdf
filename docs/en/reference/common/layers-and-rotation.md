# Layers and rotation

## Stacking contexts

Painting is organized in **stacking contexts**: groups of boxes that are painted as a unit and order their children by
`Layer`. The following create one: each section (the page area), the `Page overlay` elements, the `Frame` and `Overlay`
elements with a declared `Layer`, and the items of a `Stack` or `Grid` with a `Layer` declared in their `Item` or their
`Area`.

`Layer` is an integer (`auto` by default) that only exists in positioned elements, in `Item` and in `Area`
([positioned elements](../components/frame-and-overlays.md), [Stack](../components/stack.md#item),
[Grid](../components/grid.md#area)).

## Painting order

Inside each context, the painting order from bottom to top is:

1. Background and border of the box that creates the context.
2. Child contexts with a negative `Layer`, from lowest to highest.
3. In-flow boxes in tree order: first all the backgrounds and borders, then all the text and the images.
4. Positioned boxes without `Layer` or with `Layer: 0`, in tree order.
5. Child contexts with a positive `Layer`, from lowest to highest.

With equal `Layer`, painting is in tree order. The tree order is the order of the document: an `Overlay` is painted in its
place among its siblings even if its reference container is higher up. **`Layer` only compares within the same
context:** the content of a context is painted as a block at the level of that context, even if its children have higher
`Layer` values. The `Page overlay` elements of the document are painted before those of the section
([Document](../components/document.md#page-overlay-of-the-document-and-of-the-section)).

## Rotation (`Rotate`)

`Rotate` is an element property in `Block`, `Text`, `Paragraph`, `Image`, `Barcode`, `Frame`, `Overlay` and `Page overlay`, and
`Rotate origin`, only in the three positioned elements:

| Property | Values | Default | Notes |
|---|---|---|---|
| `Rotate` | number between −360 and 360, without `0` | none | Rotates the box, in degrees clockwise. |
| `Rotate origin` | one or two of `top`, `bottom`, `left`, `right`, `center`, or two measurements (number or %) | `center` | The point of the border box around which it rotates (`top left`, `bottom` or `20 50%`). Only with `Rotate` (`ROTATE_ORIGIN_WITHOUT_ROTATE`) and only in `Frame`, `Overlay` and `Page overlay`. |

A box with `Rotate` is painted rotated by as many degrees as it says. **Everything painted of the box** rotates:
background, border, text, image, its descendants (also the `Overlay` elements that hang from it and the contexts with
`Layer` inside it) and the areas of its links. A rotation inside another one is added to the outer one. What changes
depending on the element is **how it takes up space**.

### Rotation in flow: `Block`, `Text`, `Paragraph` and `Image`

A rotated in-flow box **takes up the rectangle that encloses it rotated**: what surrounds it is placed around that
rectangle and never under what is rotated. A `Barcode` rotates like an `Image`. It is the way to write a vertical text in its place, like the header of a
narrow column:

```
Text: Monday
    Rotate: -90
```

- **Its unrotated size does not depend on the container.** Its width is that of its `Width` or, if it is `auto`, what its
  content measures without breaking lines (natural width), limited by `Min width` and `Max width`. An `Image` has its usual
  one. Its height comes from that width, as always. A long rotated paragraph needs a `Width` that breaks it into lines.
- **It rotates about its center** and takes up the rectangle that encloses its rotated border box, with the box centered
  inside. That is why `Rotate origin` makes no sense in flow and the schema does not allow it: any origin would give the
  same result.
- **What it takes up is that rectangle, with its margins around it.** With `Margin: 0 auto` it is centered by that
  rectangle. What it contributes to the width of its container (in an `auto` column, in an item that fits its content) is
  the width of that rectangle.
- **It isolates margins** ([Block](../components/block.md#boxes-that-isolate-margins)): the margins of its children do
  not come out of it, and `Isolate margins`, which would have no effect, is `ISOLATE_MARGINS_IN_ROTATED`. It is also the
  reference container of the `Overlay` elements inside it, which are placed in its unrotated box and rotate with it.
- **It is not split** across pages, like an `Image`: whole or, if it does not fit in what is left, on the next page.
  `Keep together` and `Edges` are `BREAK_IN_ROTATED`, and a `Break` inside it, `BREAK_IN_UNBREAKABLE` ([Breaks](breaks.md)).
- **It cannot be an item of a `Stack` or `Grid`** (`ROTATED_ITEM`), which takes up the space its container gives it: a box
  inside the item is rotated instead.

> **Decision:** in flow, the rotated box takes up the rectangle that encloses it, so that a vertical text has its space
> and does not overlap its neighbors. *Rejected alternative:* rotating only when painting, like a `Frame`, which leaves
> the box on top of what surrounds it.

> **Decision:** without `Width`, a rotated in-flow box measures its content without breaking lines and not the width of
> the container: the width of a column says how much it can take up rotated and not how much it measures unrotated.
> *Rejected alternative:* stretching it to the width of the container, which at −90 degrees would make a column of text
> as tall as the page is wide.

> **Decision:** an item of a `Stack` or `Grid` does not rotate. Its container gives it its size, and a size imposed on the
> rotated box would not say whether it is the size before or after rotating. *Rejected alternative:* the item taking up
> its rotated rectangle, which contradicts the `Base size`, the stretching and the `Width` of the item.

### Rotation when painting: `Frame`, `Overlay` and `Page overlay`

A rotated positioned element rotates around its `Rotate origin` (the center of its border box by default) and **does not
change the layout**: like the `Offset` of a `Frame`, it is painting only. The box takes up its place unrotated and its
siblings do not move. For a vertical text in the margin of the page, an `Overlay` or a `Page overlay` that contains it is
rotated.

A rotated `Frame` is not split across pages. `Keep together` and `Edges` have no effect on it and are an error
(`BREAK_IN_ROTATED`), also from a rule.

`Rotate origin` is written in one of two ways:

- **Keywords:** one or two, in any order (`top left` = `left top`). The axis that is not named is `center`.
- **Two measurements:** the horizontal and the vertical one, from the top left corner of the border box, as
  measurements or as a percentage of its width and its height (`Rotate origin: 20 50%`). They can be negative or exceed
  the size of the box: the center of rotation can be outside it.

Two words for the same axis, a single measurement, a keyword with a measurement, or something that is neither one thing
nor the other, is `VALUE_NOT_ALLOWED`.

> **Decision:** measurements always go in pairs and are not mixed with keywords: `left 20` would have to say whether the
> 20 is the other coordinate or a distance from the left. *Rejected alternative:* combining keywords and measurements.

> **Decision:** a rotated `Frame` is not split. *Rejected alternative:* rotating each fragment about its own center: a seal
> split across two pages makes no sense.

### What they have in common

- **It has to fit on the page.** The rectangle that encloses the rotated border box (with the rotations of its ancestors)
  cannot go outside the physical page ([Overflow](overflow.md)), and the same goes for an unrotated `Overlay` that hangs
  from a rotated box. A rotated box taller than the page area is `PAGE_OVERFLOW` at the line of its `Rotate`.
- **Painting order.** It does not change: the rotated box is painted where it would be painted unrotated.
- **Text.** It is still selectable and sharp text at any angle, and the reading order does not change. A link in a rotated
  box is clicked in the rotated area.

`Rotate` goes between −360 and 360 degrees, exclusive. `0`, `360` and `−360`, which do not rotate anything, are
`VALUE_NOT_ALLOWED`.

> **Decision:** `Rotate` is an element property, like `Offset`: it does not go in `Styles` and is not inherited (although
> what the box contains rotates with it). *Rejected alternative:* `Rotate` in `Box`, so that it can be given with a style.

## Opacity (`Opacity`)

`Opacity` is a property of the element in `Image`, `Frame`, `Overlay` and `Page overlay`:

| Property | Values | Default | Notes |
|---|---|---|---|
| `Opacity` | percentage from `0%` to `100%` | none (opaque) | How much the box covers what lies beneath it, with everything it contains. |

A box with `Opacity` lets what lies beneath it show through. It is how a logo is set as a watermark, or the background
of a certificate is made faint:

```
Page overlay:
    Layer: -1
    Offset: 250 auto auto 150
    Image: logos/acme.svg
        Opacity: 10%
        Box:
            Width: 300
```

- **The whole box is made faint, as a single thing.** The box is painted complete (background, border, text, images and
  its descendants, also the `Overlay` boxes that hang from it and the contexts with `Layer` inside) and the result is
  painted with that opacity. What overlaps inside does not show through: a text on the background of its box does not
  let that background be seen. In an `Image`, the box is the image with the background and the border of its `Box`.
- **Only the painting changes.** The layout, the painting order and the stacking contexts are the same as without it.
  An in-flow `Image` with `Opacity` paints its background and its border together with the image, in the step of the
  text and the images.
- **It is not inherited, and it accumulates.** A box with `Opacity` inside another is made faint twice: `50%` inside
  `50%` is seen at 25 %. The opacity of a color (`#12315A80`) is applied inside the box, before the box is made faint.
- **The value is a percentage**, from `0%` to `100%`: `15%` or `12.5%`. A number without `%` is `VALUE_SYNTAX`, and a
  negative one or one above `100%`, `VALUE_NOT_ALLOWED`.
- **The ends are allowed.** With `100%` the box is opaque, as without `Opacity`. With `0%` nothing of it is seen, but it
  is still in the PDF: it takes up its place, its text is selected and read, and its links are clicked.
- **It can come from a rule of `Styles`** ([Element properties](styles.md#element-properties)), unlike `Rotate`:
  `For style: watermark` with `Opacity: 10%`.
- **Text is still text**, selectable and with its place in the tagged PDF, and the links inside are clicked the same
  way.
- **Contrast.** The text and the backgrounds inside count with their opacity multiplied by that of the box
  ([Contrast](pdf.md#contrast)): a faint text that does not reach the contrast is `CONTRAST_LOW`, like one with a color
  with opacity. A faint image is still an unknown background, and the text of a box with `0%`, which is not seen, is not
  checked.
- **PDF/A.** It is allowed in every part that can be requested ([PDF](pdf.md#pdfa)).
- The debugging outlines of a box with `Opacity` are not made faint.

> **Decision:** the whole box is made faint, as a single thing. *Discarded alternative:* multiplying the opacity of each
> thing that is painted, which lets the background of the box be seen through its text and each shape of an SVG through
> the others.

> **Decision:** `Opacity` only exists in `Image`, `Frame`, `Overlay` and `Page overlay`, which is where it is used: a
> watermark, a stamp, a background. The positioned boxes are already painted as a unit. To make another box faint in
> its place, it is wrapped in a `Frame`. *Discarded alternative:* `Opacity` in `Box`, in any box: an in-flow box paints
> its backgrounds and its text in two steps, among those of its neighbors, and making it faint as a whole would change
> the painting order.

> **Decision:** `0%` and `100%` are allowed, although the first shows nothing and the second changes nothing: the value
> may come from the data that fills the document. *Discarded alternative:* making them `VALUE_NOT_ALLOWED`, like
> `Rotate: 0`, which forced whoever fills the document to remove the property in those two cases.

> **Decision:** the value is a percentage, which is not mistaken for a length. *Discarded alternative:* a number from 0
> to 1, which would be the only value of the namespace written as a fraction.

## What is not supported

- Opacity of an in-flow box that is not an `Image` (it is wrapped in a `Frame`), blend modes, transformations other than
  rotation (scale, skew) and boxes that float to one side of the text.
- Rotating the other in-flow elements (a table, a list, a `Stack`) directly: a `Block` that contains them is rotated.
- A rotated `Frame` being split across pages.
