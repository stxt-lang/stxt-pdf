# The box: `Box`

## The four boxes

Each box has four concentric rectangles: **content**, **padding** (content + `Padding`), **border**
(padding + `Border`) and **margin** (border + `Margin`).

`Width` and `Height` (and their minimums and maximums, and `Base size`) always measure the **border box**: a box with `Width: 200`
takes up 200 pt before the margins, with the padding and the border inside. There is no way for them to measure the content box.

## The reference container

The **reference container** of a box is the rectangle against which its percentages, its `auto` values and
its `Offset` are resolved:

| Box | Reference container |
|---|---|
| In flow (also `Frame`) | the **content** box of the parent |
| `Overlay` | the **padding** box of the nearest positioned element (`Frame`, `Overlay` or `Page overlay`) among its ancestors, or the page area if there is none |
| `Page overlay` | the page area of each page |
| `Grid` item (and `Table` cell) | its area: the tracks it takes up and the gaps inside |

## The `Box` group

The box model. It is not inherited.

| Property | Values | Default | Notes |
|---|---|---|---|
| `Width` | number, %, `auto`, `narrowest`, `natural`, `fit` | `auto` | |
| `Height` | number, %, `auto` | `auto` | `auto` is the height of the content. |
| `Min width`, `Min height` | number, %, `auto` | `auto` | `auto` = narrowest width in `Stack` and `Grid` items, and 0 elsewhere. |
| `Max width`, `Max height` | number, % | no maximum | |
| `Aspect ratio` | `width/height` or number | none | If one axis is `auto` and the other is known, it is derived. Example: `16/9`. With `Width` and `Height` declared it is an error (`ASPECT_RATIO_CONFLICT`). |
| `Margin` | 1 to 4 values (number, %, `auto`) | `0` | It can be negative. `auto` only centers (see [`auto` margins](#auto-margins)). |
| `Padding` | 1 to 4 values (number ≥ 0, % ≥ 0) | `0` | |
| `Border` | 1 to 4 numbers ≥ 0 | `0` | Thickness. |
| `Border color` | 1 to 4 colors | the text `Color` | Top, right, bottom, left ([Values](values.md#four-side-shorthands)). It does not affect the layout. |
| `Corner radius` | 1 to 4 values (number ≥ 0, % ≥ 0) | `0` | Rounded corners of the border box: top left, top right, bottom right, bottom left ([corners](#corners)). It does not affect the layout. |
| `Background` | color or `linear(...)` | transparent | It is painted in the padding box ([background](#background-border-and-corners)). It does not affect the layout. |

> **Decision:** `narrowest`, `natural` and `fit` are valid only in `Width` (and in the tracks of `Grid` and `Table`): an `auto`
> height is already that of its content, and in `Height` they would be another way of writing `auto`. *Rejected alternative:*
> accepting them in `Height` too, as synonyms of `auto`.

> **Decision:** what is inherited goes in `Typography` and what is not, in `Box` or `Break`: that is why `Background` and
> `Border color` go in `Box`, and `Widows` and `Orphans`, in `Typography`. *Rejected alternative:* a painting group.

### `auto` margins

`Margin: auto` only has an effect where it centers, and anywhere else it is an error (`MARGIN_AUTO_WITHOUT_EFFECT`):

- **in block flow**, horizontally and in a box with `Width` or `Max width` (or an `Image`, which measures its natural
  width): with both margins `auto`, the remaining space is shared between them and the box is **centered**, and if only one is
  `auto`, that one absorbs the remaining space;
- **in an `Overlay` or a `Page overlay`**, with start, size and end declared ([positioned elements](../components/frame-and-overlays.md#overlay)).

Vertical `auto` margins in block flow and the `auto` margins of `Stack` and `Grid` items (which are aligned
with `Horizontal` and `Vertical`) are errors.

> **Decision:** an `auto` margin that would center nothing is an error and not a silent 0: with an `auto` width the box takes up
> the whole line and there is no remaining space to share.

## Sizing

### Intrinsic sizes

The margin box widths that a box needs without anything overflowing (**narrowest width**) and without any line
break (**natural width**):

| Element | Narrowest width | Natural width |
|---|---|---|
| `Text` | widest word | longest paragraph on one line |
| `Image` | natural width or, with `Height` in points, the width that follows from it with the aspect ratio | same |
| `Block`, `Frame`, `Overlay`, `Page overlay` | maximum of the narrowest widths of the children | maximum of the natural widths of the children |
| Horizontal `Stack`, without `Wrap` | sum of the narrowest widths of the items + gaps | sum of the natural widths + gaps |
| Horizontal `Stack`, with `Wrap` | maximum of the narrowest widths of the items | sum of the natural widths + gaps |
| Vertical `Stack` | maximum of the items | maximum of the items |
| `Grid` | sum of the columns sized without free space, plus the gaps | sum of the columns sized with infinite space and each item contributing its natural width also as a minimum, plus the gaps |
| `Columns` | see [Columns](../components/columns.md#intrinsic-sizes) | |

A box with a fixed `Width` has that value as its narrowest width and natural width (the automatic minimum of a `Stack` item looks
at its content instead, [Stack](../components/stack.md#algorithm)). `Overlay` and `Page overlay` contribute nothing to their ancestors.
In `Stack` the intrinsic sizes of each item are used as they are, without taking `Base size`, `Grow` or `Shrink` into account. In
`Grid`, the natural width makes the fractions reach the natural width of their content.

### Definite and indefinite

A size is **definite** if it is known without laying out the content. Widths always are, at layout time. Which
heights are is in [Values](values.md#definite-height), and a percentage of an indefinite height is an error. The page area
is definite: a top-level box of a section with `Height: 100%` takes up the page.

### Minimums and maximums

The provisional size is computed, limited by the maximum and raised to the minimum. If they can be compared when the
document is read (both in points or both as percentages), a minimum greater than the maximum, or a `Width`/`Height` outside
them, is an error (`MIN_MAX_CONFLICT`). If they cannot be compared until layout (`Min width: 150` and `Max width: 20%`) and
they clash, **the minimum wins**.

> **Decision:** a clash between different units is not a layout error: the minimum wins.

### Aspect ratio

With `Aspect ratio` and an `auto` axis, that axis is derived from the other, on the border box. An `Image` always has an
aspect ratio: that of its file, or that of `Aspect ratio`, and in it the ratio applies to the content box
([Image](../components/image.md#sizing)).

> **Decision:** in an `Image`, the aspect ratio is that of the content box, so that padding and border do not distort
> the drawing. In the other boxes, that of the border box.

## Background, border and corners

`Background`, `Border color` and `Corner radius` only change the appearance: none of them moves a box or changes its size. The background
and the border go under the content ([Layers and rotation](layers-and-rotation.md#painting-order)). For someone who reads the PDF with a
screen reader they are decoration ([The PDF](pdf.md#accessibility)).

### Corners

`Corner radius` rounds the border box with a quarter ellipse per corner. A horizontal percentage is of the width of the border
box and a vertical one, of its height: `50%` makes a circle in a square box and an ellipse in the others. If two consecutive
radii of a side add up to more than the side, all of them are reduced by the same factor until they fit.

- **Inside the box.** The background, which goes in the padding box, has the radii of the border box minus the thickness of the
  border on each side, and a corner that is left without a radius is square. An `Image` is clipped with the radii of its content
  box (the border box minus the border and the padding).
- **What it contains.** A box with rounded corners clips, along the curve of its padding box, the **backgrounds, the
  borders and the images** of what it has in its flow, at any depth (also a `Frame` and an item with
  `Layer`): a child with a background that reaches the corner follows the curve without rounding the child. **Text is not clipped**, nor is an
  `Overlay` or a `Page overlay`, which are placed on purpose and can stick out (a badge that peeks over the corner). Several
  rounded boxes one inside another add up their clipping. Since nothing in the flow sticks out of its box (that would be an
  [overflow](overflow.md)), the clipping only changes what touches the corners.
- **Split boxes.** With `Edges: open`, only the first fragment of a box has the top corners and only
  the last one, the bottom corners. With `closed`, all of them have all the corners. Radii in % are resolved on the size of the
  fragment and not on that of the whole box.

> **Decision:** the rounded box always clips what it contains, without a property to ask for it, because what sticks out of
> the curve is never what is wanted. *Rejected alternative:* not clipping, so that every child that
> touches a corner also has to be rounded, or a `Clip` property.

> **Decision:** clipping does not reach the text: a text cut in half would be hard to read and would give no warning. Nor does it reach an
> `Overlay`, which leaves the flow to be placed wherever it wants. *Rejected alternative:* clipping everything, like a window.

> **Decision:** one radius per corner, and the ellipse is requested with %. The % of a split box are of each fragment: a very
> short one reduces its radii. *Rejected alternative:* two radii per corner (`10 / 20`).

### Border

It is the ring between the border box and the padding box. With a single color it is painted in one go. With different colors, each
side gets its color, and two consecutive sides meet on the **diagonal** that joins the corner of the border box with that of
the padding box (with rounded corners, the diagonal continues inward to cover the curve). A side without thickness does not
have a color: `Border: 0 0 0 3` with `Border color: #B5651D` is a strip on the left. The border is always a
solid line.

### Background

A color, plain or with opacity, or a linear gradient. It is painted in the padding box, with its corners.

#### Gradient

`linear(angle, ...)` passes through the center of the padding box and is as long as needed for the corners to have the
first and the last color (`|width · sin(angle)| + |height · cos(angle)|`). Beyond the ends the color of the
end is repeated. Each color is painted with its opacity: a gradient toward a transparent color lets what is underneath show through.

### Opacity

The color of a background, a border or a text with opacity lets what is underneath show through. In a single-color border,
the corners are not painted twice.

## What is not supported

- Shadows: PDF does not blur, and they would have to be approximated with semitransparent layers.
- Border styles (dotted or double), several background layers or background images.
- Clipping of the content along the outline of a square box: what sticks out of the box is an
  [overflow](overflow.md), and rounded corners only clip backgrounds, borders and images
  ([above](#corners)).
