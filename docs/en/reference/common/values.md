# Values

## Value types

| Syntax | Meaning | Example |
|---|---|---|
| Number | Points. Decimal point, never a comma. It can be negative where stated. | `12`, `12.5`, `-30` |
| Percentage | Percentage of a base (see [percentage bases](#percentage-bases)). | `50%` |
| Fraction | A share of the free space, with its weight in front (`*` alone is `1*`). Only in grid tracks. | `*`, `2*`, `2.5*` |
| `auto` | The engine computes the value from the context. | `auto` |
| `narrowest` | The smallest possible size without overflowing (the longest word). | `narrowest` |
| `natural` | The size without any line break. | `natural` |
| `fit` | The natural width, without going over the available width or under the narrowest. | `fit` |
| `between(a, b)` | Grid track between `a` and `b`. `a` cannot be a fraction. | `between(100, *)` |

Each property accepts only the forms in the *Values* column of its table. Any other (`Width: *`,
`Max width: natural`, `Gap: auto`, a `between()` outside the tracks) is `VALUE_NOT_ALLOWED` and not a silent
`auto`. Only `Margin`, `Offset` and `Letter spacing` accept negative values. A percentage of a height that is not
[definite](#definite-height) is an error.

> **Decision:** a fraction is written with an asterisk and its weight: `120 * 2*` gives the third track twice as much of
> what is left over as the second. *Rejected alternative:* `1fr`, a unit known only to those who have already used it.

> **Decision:** the words that measure the content say which measurement they give: `narrowest` (the narrowest), `natural` (without
> breaking lines) and `fit` (fitted to the space). *Rejected alternative:* `min-content`, `max-content` and `fit-content`,
> names of the mechanism of web layout.

> **Decision:** a track between two sizes is written `between(a, b)`, which reads as it is: `between(100, *)`.
> *Rejected alternative:* `minmax(a, b)`, which names the two functions of the track and not what it measures.

> **Decision:** there are no functions that choose between measurements (the smallest, the largest or one between two limits): a size between
> two limits is written with `Min width` and `Max width` (`Width: 50%`, `Min width: 200`, `Max width: 400`), and each thing
> is said in a single way. *Rejected alternative:* `min()`, `max()` and `clamp()`, which repeated what the
> minimums and the maximums already do.

## Lists and `repeat`

Some properties accept several values separated by spaces (`Column sizes`, `Row sizes` or `Margin`). In track
lists, `repeat(n, pattern)` repeats a pattern:

```
Column sizes: 150 repeat(3, *) between(100, 2*)
```

is equivalent to `150 * * * between(100, 2*)`.

## Four-side shorthands

`Margin`, `Padding`, `Border` and `Offset` accept from 1 to 4 values in the order **top, right, bottom, left**:

| Value | Top | Right | Bottom | Left |
|---|---|---|---|---|
| `10` | 10 | 10 | 10 | 10 |
| `10 20` | 10 | 20 | 10 | 20 |
| `10 20 5` | 10 | 20 | 5 | 20 |
| `10 20 5 0` | 10 | 20 | 5 | 0 |

`Border color` also accepts from 1 to 4 colors in this order. `Corner radius` accepts from 1 to 4 values with the same convention,
but one per **corner**: top left, top right, bottom right and bottom left
(`Corner radius: 0 20` rounds only the top right corner and the bottom left one).

## Colors

`#RGB`, `#RGBA`, `#RRGGBB` or `#RRGGBBAA` in hexadecimal: `#000`, `#A32D2D`, `#12315A80`. The last digit or pair is the
**opacity** (`FF` or `F`, opaque, and `00`, transparent) and, if it is missing, the color is opaque. It is valid in all the color
properties (`Color`, `Border color`, `Background`, `Separator color`).

> **Decision:** opacity goes in each color. There is no `Opacity` of the box, which would affect everything it contains.

The background also accepts a **linear gradient**: `linear(angle, color, color, ...)`, with the angle in degrees (`0` points
up, `90` to the right, `180` down) and at least two colors, spread evenly:
`linear(180, #12315A00, #12315A)`. How it is painted is in [The box](box.md#gradient).

There are no named colors (`red`), color functions (`rgb()`), stops with a position, or radial, conic or
repeating gradients.

## Booleans and integers

Booleans are written `true` or `false`. Integers, without decimals: `Layer: -1`, `Widows: 3`.

## Percentage bases

| Property | Percentage of |
|---|---|
| `Width`, `Min width`, `Max width` | the width of the reference container |
| `Height`, `Min height`, `Max height` | the height of the reference container, which **must be definite** |
| `Margin`, `Padding` (all four sides) | the **width** of the reference container, also at the top and bottom |
| `Offset` top/bottom | the height of the reference container, which **must be definite** |
| `Offset` left/right | the width of the reference container |
| `Base size` (`Stack` item) | the size of the `Stack` on the main axis (in a vertical `Stack`, its height, which must be definite) |
| `Gap` | the size of the content box of the container on that axis (between rows, its height, which must be definite) |
| Grid tracks | the size of the content box of the container on that axis, without subtracting the gaps (`50% 50%` with `Gap` overflows, and is an error); for rows, its height, which must be definite |
| `Line height` | the `Size` of the box itself |
| `Corner radius` | the width of the border box horizontally and its height vertically ([The box](box.md#corners)) |

`Border` does not accept percentages. The reference container of each box is in [The box](box.md#the-reference-container).

## Definite height

Widths are always known. Heights are not. A vertical percentage is an error (`INDEFINITE_PERCENTAGE`) if its base is not
a **definite height**, which is decided when the document is read with this fixed rule:

- the page area;
- that of a box with `Height` in points, or as a percentage of a definite height;
- that of a box with `Height: auto` and `Aspect ratio`;
- that of an `Overlay` or a `Page overlay` with `Height: auto` stretched between top and bottom;
- that of a grid item with `Height: auto` stretched in its area (`Vertical: stretch`).

The base of `Height`, `Min height` and `Max height` of an `Overlay` or a `Page overlay` is its reference container, and that of a
grid item, its area: both are always definite. A stretched `Stack` item does **not** give a definite height to its children:
if they need it, the item must declare its `Height`. The height of the columns of a `Columns` is not definite either
([Columns](../components/columns.md#content)).

> **Decision:** the definite height is decided when the document is read and not when it is laid out. It is stricter (a stretched `Stack`
> item does not give a definite height even if its height ends up being known), but the error always appears, on the line of the
> property. *Rejected alternative:* detecting percentages without a base during layout.
