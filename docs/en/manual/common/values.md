# Values

## Measurements

| Written | Means | Example |
|---|---|---|
| a number | points, with a decimal point and never a comma | `Width: 120`, `Size: 10.5` |
| a number with `mm` | millimeters, with the unit attached to the number | `Width: 42mm`, `Margin: 12.5mm` |
| a percentage | a part of another measurement (below) | `Width: 50%` |
| `auto` | the engine works it out | `Width: auto` |
| `narrowest` | the narrowest it can be without anything overflowing: the longest word | `Width: narrowest` |
| `natural` | what it measures without breaking any line | `Width: natural` |
| `fit` | what its content needs, without going beyond the available space | `Width: fit` |

Points and millimeters can be mixed, also in a single value (`Margin: 20mm 40`). There are no other units: `5cm`,
`12pt` or `12 mm`, with a space, are an error. `Grow`, `Shrink`, the degrees of `Rotate` and `Aspect ratio` are not
measurements and carry no unit.

`narrowest`, `natural` and `fit` are widths: they are valid in `Width` and in the columns and rows of a `Grid`. Not in
`Height`, because `auto` is already the height of the content. Each property accepts only some of these forms, and its
table says which. Any other is an error. Only margins, the offsets of positioned boxes and letter spacing can be
negative. A size between two limits is written with its minimum and its maximum: `Width: 50%`, `Min width: 200` and
`Max width: 400`.

The columns of a `Grid` also accept **fractions** (`*`, `2*`: parts of the leftover space), `between(a, b)` and
`repeat(n, ...)` ([Grid](../components/grid.md)).

## Percentages

A percentage is a part of another measurement, almost always of its reference container ([Concepts](concepts.md)):

- **Widths, margins and padding** (also top and bottom): of the **width** of the container.
- **Heights**: of the **height** of the container, which has to be known before laying out what is inside.
- **`Line height`**: of the font size of the box itself.

**A height that is known** is that of the page area, that of a box with `Height` in points or millimeters (or in a
percentage of another height that is known) and that of some boxes that stretch (an `Overlay` anchored at the top and
the bottom, a `Grid` cell). The height of a box that measures what its content measures is not known before laying it
out, so a `Height: 50%` inside it is an error. To use the percentage, the container needs a `Height`.

## Four sides

`Margin`, `Padding`, `Border` and `Offset` accept from 1 to 4 values, in the order **top, right, bottom, left**:

| Written | Top | Right | Bottom | Left |
|---|---|---|---|---|
| `10` | 10 | 10 | 10 | 10 |
| `10 20` | 10 | 20 | 10 | 20 |
| `10 20 5` | 10 | 20 | 5 | 20 |
| `10 20 5 0` | 10 | 20 | 5 | 0 |

`Border color` also accepts from 1 to 4 colors, in the same order. `Corner radius` accepts from 1 to 4 values, one per
**corner**: top left, top right, bottom right and bottom left.

## Colors

In hexadecimal: `#RGB` or `#RRGGBB` (`#000`, `#A32D2D`). With one more digit or pair, the **opacity**: `#RGBA` or
`#RRGGBBAA` (`#12315A80` is half-transparent blue, `00` is transparent, `FF` opaque). They are valid in the color of the
text, of the border, of the background and of the rule between columns.

A color that repeats can be declared once with a name in `Styles` (`Named color: brand #12315A`) and then written as
`Color: brand` ([Named colors](styles.md#named-colors)).

## Gradients

A background can be a gradient: `linear(angle, color, color, ...)`. The angle is in degrees: `0` goes from bottom to
top, `90` from left to right and `180` from top to bottom. The colors are spread out evenly and can have opacity. This
one paints a blue that appears from top to bottom, starting from transparent:

```
Box:
    Background: linear(180, #12315A00, #12315A)
```

## Others

- **Booleans:** `true` or `false`.
- **Integers:** without decimals (`Widows: 3`).
- **Lists of names:** separated by spaces (`Style: note wide`).
