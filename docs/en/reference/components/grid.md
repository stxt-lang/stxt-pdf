# `Grid` and `Area`

In a `Grid`, whose items are the element of each `Area`, the engine first resolves **where** each item goes and then
**how much** each track (each column and each row) measures. Each in-flow child is an `Area`. It also accepts `Overlay` and `Page overlay`.
The `Grid` accepts `Id`, `Style`, `Link`, `Box`, `Typography` and `Break`, and goes wherever an in-flow element goes.

```
Grid:
    Column sizes: * *
    Area: 1 2
        Text: Row 1, column 2
    Area:
        Text: Placed automatically
```

## Properties of the `Grid`

| Property | Values | Default | Notes |
|---|---|---|---|
| `Column sizes` | list of tracks | one `auto` column | Described in [track sizing](#track-sizing). |
| `Row sizes` | list of tracks | implicit `auto` rows | |
| `Gap` | 1 or 2 values (number, %) | `0` | One value for both axes, or `rows columns`. |
| `Areas` | text block | none | One row per line, with the names separated by spaces and `.` for an empty cell. |
| `Flow` | `horizontal`, `vertical` | `horizontal` | Direction of automatic placement: `horizontal` fills the rows one by one, and `vertical`, the columns. |
| `Horizontal` | `start`, `end`, `center`, `stretch` | `stretch` | Horizontal alignment of the items in their area. |
| `Vertical` | `start`, `end`, `center`, `stretch` | `stretch` | Vertical alignment of the items in their area. |

Valid tracks: number, %, `auto`, `narrowest`, `natural`, fraction, `between(a, b)` and `repeat(n, pattern)`.
`fit` is not a track (`FIT_TRACK`), because `auto` already fits the content.

## `Area`

It contains **exactly one in-flow element** (`SINGLE_ELEMENT`), which is the item. The `Area` only says where it goes and how it is
aligned: it is not a box and does not accept `Id`.

| Property | Values | Default | Notes |
|---|---|---|---|
| *(value)* | area or `row column [rows columns]` | `auto` | Where it goes, counting from 1. Without a value, it is placed automatically. |
| `Horizontal` | as in `Grid` | that of the container | |
| `Vertical` | as in `Grid` | that of the container | |
| `Layer` | integer | `auto` | Stacking order of the item ([Layers and rotation](../common/layers-and-rotation.md)). |

**Value of `Area`:** an area name (`Area: side`) or `row column [rows columns]` counting from 1, where row and
column can be `auto`: `Area: 1 1 1 2` occupies row 1 and columns 1 and 2, and `Area: auto auto 1 2` is placed
automatically occupying two columns. `Area:` without a value is equivalent to `Area: auto auto`.

## Placement

1. The explicit grid is built with `Column sizes`, `Row sizes` and `Areas`. Each name in `Areas` must form a rectangle and
   all the rows must have the same number of cells.
2. The items whose `Area` has a row and a column, or an area name, are placed.
3. The rest are placed in tree order with a **cursor** that goes through the grid by rows (or by columns, with
   `Flow: vertical`). Each item goes to the first free slot where it fits. The cursor only moves forward, so free slots can be left behind.
4. If an item falls outside the declared grid, implicit `auto` tracks are created.

An item with **only the row fixed** (`Area: 2 auto`) goes to the first column of that row where it fits, without moving the cursor. If
none is left, it is an error (`GRID_PLACEMENT`): the row does not create implicit columns. One with **only the column fixed**
(`Area: auto 2`) goes to the first free row from the cursor, to the next one if the column is behind it, and moves the
cursor behind itself. With `Flow: vertical` rows and columns are swapped. These items are placed in step 3, in tree
order with the automatic ones. An area name that does not exist or `Areas` that do not form rectangles are also
`GRID_PLACEMENT`.

> **Decision:** an item with the row fixed and no room in it is an error, with the line of the `Area`. The grid does not create
> implicit columns halfway through placement, with the previous automatic items already placed with fewer columns. The items
> with only the row or only the column fixed are placed in tree order with the automatic ones and not before, and the one with a fixed row
> takes the first free column of its row. *Rejected alternative:* creating the implicit column.

## Track sizing

Each track has a minimum and a maximum function:

- `150` is fixed in both.
- `auto` is equivalent to `between(narrowest, natural)`.
- `*` is equivalent to `between(auto, *)`, so **a fraction does not go below the narrowest width of its content**.

1. **Initialize:** each track receives a base (its minimum) and a limit (its maximum, which is infinite for fractions).
2. **Content contributions:** the intrinsic tracks take the narrowest width or the natural width of their items. The excess of
   the items that span several tracks is distributed among the intrinsic tracks they span: first what they lack to reach their
   narrowest width, to the bases, and then, counting the bases already raised, what they lack to reach their natural width, to the limits
   (the area of the item does not exceed its natural width because of this distribution). If any of them is a fraction, what the item lacks
   to reach its narrowest width is distributed only among the fractions, in proportion to their weight: an item that spans `*` columns does not
   overflow its area.
3. **Maximize:** the free space is shared equally among the non-flexible tracks up to their limit. An `auto`
   track reaches its natural width before the fractions receive anything.
4. **Fractions:** the rest is divided among the fractions proportionally. If a fraction needs more than its share
   because of its narrowest width, it is fixed at its base and the distribution is repeated.
5. **Stretch:** if there are no fractions and there is space left over, it is distributed among the `auto` tracks.

The **columns** are sized first, with the narrowest and natural widths of the items. The **rows** afterwards, with
the height of each item laid out at the width of its columns. With `Height: auto` on the container, the fraction rows have
no space to distribute and measure in proportion to their content: all the `*` rows measure what the tallest one does.

The percentages of the tracks are of the content box of the container, without subtracting the gaps
([Values](../common/values.md#percentage-bases)). Percentage rows need a definite height on the
container. Percentage columns behave as `auto` only when the intrinsic sizes of the container are computed,
when there is no width yet. The `auto` margins of an item are an error: alignment is done with `Horizontal` and
`Vertical`.

> **Decision:** what an item that spans some fraction lacks is received only by the fractions, in proportion to their
> weight: this way an item that spans `*` columns does not overflow its area.

> **Decision:** the natural width is distributed counting the bases already raised: with columns `50 auto *`, gap 10 and an item of
> natural width 400 in the first two, the `auto` one measures 340 and the area of the item, 400 (and not 480 and 540).

## Alignment in the area

Each item occupies an area formed by its tracks and the internal gaps. With `stretch` (the default) and without a declared `Width`/`Height`,
it occupies the whole area and its layout is repeated. With `start`, `end` or `center`, it keeps its content size and is
shifted. The height percentages of an item are resolved against the height of its area in the final layout. When the
rows are sized, still without an area, they count as `auto`. An item stretched vertically has a definite height
([Values](../common/values.md#definite-height)).

## Intrinsic sizes

The narrowest width of a `Grid` is the sum of the columns sized without free space, plus the gaps. The natural width is the
sum of the columns sized with infinite space and each item contributing its natural width also as a minimum, plus the
gaps ([The box](../common/box.md#intrinsic-sizes)).

## Breaks

A `Grid` breaks between rows. A row that does not fit on a whole page breaks with its items in parallel, and the rows
occupied by the same item break together ([Breaks](../common/breaks.md#breaks-in-stack-grid-and-table)).

## Positioned elements

The static position of an `Overlay` or a `Page overlay` that is a child of a `Grid` is the top left corner of its content
box ([positioned elements](frame-and-overlays.md#overlay)).

## What is not supported

- Dense placement (going back to fill empty slots), nested grids that share tracks with their parent, and `fit`
  as a track.
- `auto` margins on the items: they are errors.
