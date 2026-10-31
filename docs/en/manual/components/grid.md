# `Grid`

A `Grid` is a **grid**: its columns (and, if needed, its rows) are declared and each item goes in a cell or in an area of
several. It is used for page compositions: a sidebar and the body, cards in two columns, a résumé.

```
Document (dev.stxt.pdf):
    Language: en
    Title: Product sheet
    Page:
        Paper: A4
        Margin: 40
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For style: panel
            Box:
                Padding: 10
                Background: #F1EFE8
    Section:
        Grid:
            Column sizes: 150 *
            Gap: 20
            Areas >>
                head head
                side main
            Area: head
                Text: Walnut chair
                    Typography:
                        Size: 18
                        Bold: true
            Area: side
                Text: Ref. 0412
                    Style: panel
            Area: main
                Paragraph:
                    Content >>
                        Solid walnut, oil finish. Seat upholstered
                        in natural wool.
```

## `Area`

Each child of a `Grid` goes inside an `Area`, which wraps **exactly one element**. The value of the `Area` says where it
goes:

| Value | Where it goes |
|---|---|
| *(nothing)* | In the next free slot, going through the grid by rows |
| `side` | In the area with that name in `Areas` |
| `2 3` | In row 2, column 3 (counting from 1) |
| `1 1 1 2` | In row 1, from column 1, spanning 1 row and 2 columns |
| `auto auto 1 2` | In the next slot where 2 columns fit |

The `Area` also accepts `Horizontal` and `Vertical` (the alignment of the item in its area) and `Layer`. The size, the
margins and the `Id` go on the element inside.

## Properties of the `Grid`

| Property | Default | What it does |
|---|---|---|
| `Column sizes` | one column | The width of each column |
| `Row sizes` | whatever each row measures | The height of each row |
| `Gap` | `0` | The gap between cells (with two values: between rows and between columns) |
| `Areas` | none | A drawing of the grid with names: one line per row, one name per cell, `.` for an empty cell |
| `Flow` | `horizontal` | Whether the items without a fixed position are placed by rows (`horizontal`) or by columns (`vertical`) |
| `Horizontal`, `Vertical` | `stretch` | The alignment of the items in their cell: `start`, `end`, `center` or `stretch` (taking up the whole cell) |

Each name in `Areas` must form a rectangle. If an item falls outside the declared rows, new rows are added.

## The sizes of the columns and rows

| Written | Size |
|---|---|
| `150` | 150 points |
| `25%` | a share of the width of the `Grid` (without subtracting the gaps) |
| `auto` | what its content asks for |
| `narrowest`, `natural` | the narrowest its content can be, or its size without breaking any line |
| `*`, `2*` | a share of what is left over, never less than the longest word of its content |
| `between(100, *)` | between 100 and a share of what is left over |
| `repeat(3, *)` | three `*` columns |

The fixed columns and the `auto` ones are placed first, and what is left over is shared among the fractions. The rows
measure as much as their tallest item, except with `Row sizes`. Percentage rows need a `Height` on the `Grid`.

## How it behaves

- By default, each item is stretched to its whole cell or area. With `start`, `end` or `center`, it takes its own size and
  is aligned.
- The items do not accept `Margin: auto`: they are aligned with `Horizontal` and `Vertical`.
- Across pages, a `Grid` is broken between rows. A row that does not fit on a whole page is split together with its items.
- A fixed-size row whose content does not fit is an error.

More detail in the [reference](../../reference/components/grid.md).
