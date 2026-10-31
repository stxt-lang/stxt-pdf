# The box: `Box`

The `Box` group is accepted by any element and is not inherited. It controls the size, the margins, the padding, the
border, the background and the corners.

```
Block:
    Box:
        Width: 300
        Margin: 0 0 12 0
        Padding: 10 14
        Border: 0 0 0 3
        Border color: #B5651D
        Background: #F1EFE8
        Corner radius: 0 6 6 0
    Paragraph: A note with a colored strip on the left.
```

| Property | What it does |
|---|---|
| `Width`, `Height` | The width and the height of the box, with its padding and its border. Default `auto`: the width the parent leaves and the height of the content |
| `Min width`, `Max width`, `Min height`, `Max height` | Limits of the size |
| `Aspect ratio` | The proportion: `16/9` or a number. With one side known, the other comes from it |
| `Margin` | The space outside, from 1 to 4 values. It can be negative or `auto` (to center) |
| `Padding` | The space inside, from 1 to 4 values |
| `Border` | The thickness of the border, from 1 to 4 values |
| `Border color` | The color of the border, from 1 to 4 colors. By default, that of the text |
| `Corner radius` | The rounded corners, from 1 to 4 values |
| `Background` | The background: a color or a gradient |

## Size

Without `Width`, a box takes up the full width its parent leaves it, minus its margins. Without `Height`, it measures
what its content measures. Width goes from the outside in and height, from the inside out.

`Width` and `Height` include the padding and the border. Percentages are of the width or the height of its container
([Values](values.md#percentages)).

**Minimums and maximums.** The maximum is applied first and then the minimum. A minimum larger than the maximum is an
error if both are in the same units. If one is in points and the other in a percentage and they clash during layout, the
minimum wins.

**Proportion.** With `Aspect ratio: 16/9` and `Width: 320`, the box is 180 high. With `Width` and `Height` at the same
time it is an error, because the proportion would have no effect.

## Margins

Margins separate a box from the ones around it. Between two boxes one below the other, vertical margins **are not added:
the larger one is kept**. With 20 below one box and 30 above the next, they are 30 apart, not 50. The same happens
between a box and its first or its last child if the box has no border or padding on that side: the margin of the child
comes out. To keep it from coming out, the box needs a padding, a border or `Isolate margins: true`
([Block](../components/block.md)). A negative margin brings the boxes closer together,
but it cannot take a box off the page or take more height from its parent than its content measures: both are an
error (`BOX_OVERFLOW`).

**Centering.** `Margin: 0 auto` centers horizontally a box with `Width` (or `Max width`): the two side margins share the
leftover space. An `auto` where there is nothing to center is an error: in a box without a width, which already takes up
everything, or vertically. The items of a `Stack` or a `Grid` are centered with their own alignment properties.

## Border and background

The border is painted between the padding and the margin. With one color per side, the sides meet diagonally at the
corners. A side with thickness 0 is not visible, so `Border: 0 0 0 3` is a strip on the left. The background is painted
behind the content and the padding, up to the border. A background or a color with opacity lets what is underneath show
through.

### Contrast

Text has to stand out from its background, so that people with low vision can read it: when converting, a text with low
contrast is an error (`CONTRAST_LOW`) that says how much it has and how much it needs. At least **4.5:1** is required, or
**3:1** for large text (`Size` of 18 or more, or of 14 in bold). As a reference, the gray `#767676` on white reaches
exactly 4.5:1, and black on white has 21:1.

What counts is what is really underneath the text: the background of its box, that of the boxes outside it or that of a
box behind it, blended if they have opacity. For a gradient, the worst of its colors counts. Text on top of an image is
not checked, because what is underneath is not known. The caller can turn the check off
([Using from Java](java-usage.md)).

## Corners

`Corner radius: 6` rounds the four corners with a radius of 6 points. With four values, each corner has its own.
`Corner radius: 50%` on a square box is a circle (a profile photo, a badge). The background and the border follow the
curve. An image is clipped with the corners of its box. What is inside a rounded box also follows its curve: the
background, the border or the image of a child that reaches the corner is clipped, without having to round it. Text is
not clipped, and neither is an `Overlay`, which can stick out of the corner on purpose.

## What does not change the size

The background, the border color and the corners only change how the box is painted, never where it goes or how big it
is.

More detail in the [reference](../../reference/common/box.md).
