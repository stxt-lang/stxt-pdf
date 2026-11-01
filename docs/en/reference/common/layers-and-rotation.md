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

## What is not supported

- Opacity of a whole box (only of colors, [Values](values.md#colors)), transformations other than rotation (scale, skew)
  and boxes that float to one side of the text.
- Rotating the other in-flow elements (a table, a list, a `Stack`) directly: a `Block` that contains them is rotated.
- A rotated `Frame` being split across pages.
