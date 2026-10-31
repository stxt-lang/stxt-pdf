# `Table`

A `Table` places its cells in rows and columns: its rows (`Row`) go in a header (`Header rows`, optional), a
body (`Body`, required) and a footer (`Footer rows`, optional), **in that order** (`TABLE_ORDER`), and each row has its
cells (`Cell`) from left to right. The `Table` accepts `Id`, `Style`, `Link`, `Box`, `Typography` and `Break`, and goes
wherever an in-flow element goes, also inside a cell.

```
Styles:
    For element: Cell
        Box:
            Padding: 2 4
            Border: 0 0 0.5 0
Table:
    Column sizes: 120 * *
    Header rows:
        Row:
            Typography:
                Bold: true
            Cell: Item
            Cell: Q1
            Cell: Q2
    Body:
        Row:
            Cell: Sales
            Cell: 100
            Cell: 120
    Footer rows:
        Row:
            Cell: Total
                Span: 1 3
```

| Property | Values | Default | Elements | Notes |
|---|---|---|---|---|
| `Column sizes` | list of tracks | required | `Table` | The columns, as in `Grid` ([Grid](grid.md#track-sizing)). |
| `Column align` | one alignment per column: `left`, `center`, `right` or `justify` | none | `Table` | The default alignment of the cells of each column. As many as `Column sizes` (`TABLE_COLUMN_ALIGN`). |
| `Gap` | 1 or 2 lengths (rows columns) | `0` | `Table` | Spacing between cells, as in `Grid`. |
| `Span` | `rows columns`, two positive integers | `1 1` | `Cell` | Rows and columns that the cell takes up. |
| `Row header` | `true`, `false` | `false` | `Cell` | The cell is the header of its row ([accessibility](#accessibility)). Not valid in `Header rows` (`ROW_HEADER_IN_HEADER_ROWS`). |
| `Id` | text | none | `Row` | Target of links and of `{page of: id}`. Its `For id` rule styles the cells of the row ([Rows](#rows)). |
| `Box` | `Background`, `Border`, `Border color`, `Corner radius` | none | `Row` | The look of the band of the row ([Rows](#rows)). Nothing else (`ROW_BOX`). |

They do not go in the rules of `Styles`, which only accept the groups.

> **Decision:** the groups of rows that are repeated are called `Header rows` and `Footer rows`, the names of the
> layout tools. *Rejected alternative:* `Header` and `Footer`, which in a word processor are the
> header and the footer of the page (here, a `Page overlay`) and would leave that name taken in the whole schema.

## A table is a grid

`Column sizes` (required) and `Gap` are those of `Grid`: the columns are sized the same way, with `auto`, fractions or
`between`, and `Gap` separates the cells. The rows are `auto` tracks: each one measures what its tallest cell measures, and the cells
are stretched to their row. Each cell is a grid item placed explicitly: it goes in the first free column of its row,
and takes up the rows and columns of its `Span` (`Span: 1 3` takes up three columns). A column is taken if it is taken by a
cell of a row above with a `Span` of several rows. Each row fills **exactly** all the columns
(`TABLE_ROW_CELLS`): an empty cell is written `Cell:`. A `Span` does not go outside the table, does not pass from one group to another
(from `Header rows` to `Body`, from `Body` to `Footer rows`) and does not overlap another cell (`TABLE_SPAN`).

An explicit `Span: 1 1` is accepted: it is the default value written out and not a contradiction.

## Cells

A `Cell` is a box with two forms, like a `Text`: with a value (`Cell: 100`), it is a text box of one line, and without
a value, a block with elements (any except the headings), or empty if it has none. Both at the same time are
an error (`CELL_CONTENT`). A long text goes in a `Paragraph`. It accepts `Id`, `Style`, `Link`, `Box`, `Typography`, `Span` and
`Row header`. The borders are those of each cell, separate: a line between rows is drawn with the bottom border of the cells.
The margins of a cell separate it from its area, as in a `Grid`.

> **Decision:** `Row header: true` goes in each cell that is the header of its row (the item of an invoice, the time of a
> timetable): it also serves for the label of a totals footer or the third column of a schedule. *Rejected
> alternative:* `Row header columns: N` in the `Table`, which only serves for whole columns.

## Cell style

A rule of `Styles` ([Styles](../common/styles.md)) styles the cells of a table without repeating a style in each
one:

- **By position.** `For element: Header cell`, `Body cell` and `Footer cell` are the cells of each group, and
  `Odd row cell` and `Even row cell`, those of the odd and even rows of the **body**, counting from the first row of
  `Body` as 1 (the zebra striping, which the header and the footer do not take part in). The row of a cell that takes up several is the
  **first** of those it takes up. A cell matches at the same time that of its group and that of its row, and the usual rule
  `For element: Cell`.
- **By column.** `For column: 3` (or `For column: 2 4`, from 2 to 4) applies to the cells that **begin** in those
  columns, in any group: a cell with a `Span` of several columns belongs to the column where it begins.
- **`Column align`.** A `Column align: left right right` of the `Table` gives an alignment to each column (as many as
  `Column sizes`). It is only the default value of the `Align` of the cells: the position, column,
  style and `Id` rules override it, and so does what the cell itself declares.
- **By row.** The `Style` of a `Row` gives each of its cells the `For style` rules of those styles (the totals footer
  of an invoice: `Style: total` in the `Row` and not in each cell), and the `For id` rule of its `Id`, its own.

The priority of all of them is in [Styles](../common/styles.md#priority). The cells of the tables of a `Markdown`
receive these same rules. Their `Align` comes from the delimiter row of the table (`:--`, `--:`), which each cell declares
and which wins over everything else.

If two `For column` overlap, they are applied in the order of `Styles`, like the styles.

> **Decision:** inside a cell, from weakest to strongest: `For element: Cell`, `Column align`, the position rules (group and
> then row), `For column`, the styles of the `Row`, the styles of the cell, the `For id` of the `Row`, the `For id` of the
> cell and what belongs to the cell itself. That way the totals row wins over the zebra striping and the column, a centered header wins over the alignment of its column and a single cell can
> break it with its style. *Rejected alternative:* the position winning over the column, or `Column align` below
> `For element: Cell`.

## Rows

A `Row` is not a box of the layout: it takes up no space of its own and is not named in `For element`, like `Header rows`, `Body` and
`Footer rows`. The rows of the table have no size property and measure what their tallest cell measures. What the
row declares is for its cells or for its **band**:

- **`Style` and `Id`.** The `For style` rules of its styles and the `For id` rule of its `Id` apply to each of its
  cells ([Cell styling](#cell-style)).
- **`Typography`.** Its cells inherit it (the `Styles` rules of the cell win over it, as over any inheritance).
- **`Break`.** It is that of its cells: with it a break is forced or avoided before or after the row, or inside it. A
  cell does not declare `Break`, not even from a rule (`STYLE_NOT_ALLOWED`, also that of the style or the `Id` of its
  `Row`: the `Break` of the row is declared by the `Row`).
- **`Box`.** Only the look of the band: `Background`, `Border`, `Border color` and `Corner radius`. Any other property
  (`Padding`, `Margin` or `Height`) would have no effect on a row and is `ROW_BOX`.

A row with `Box` or `Id` has a **band**: a rectangle that goes from the first column to the last, with the `Gap`
between them, and from the beginning of the row to its end. The `Gap` between rows belongs to neither. It is painted **below its
cells**: the background of a cell covers that of the row, and a cell that takes up several rows, the bands of all of them. The color
of its border, if it does not declare `Border color`, is that of the text of the row (its `Typography`, or that of the table). The band of
a row of `Header rows` or `Footer rows` is repeated with it.

```
Footer rows:
    Row:
        Id: total
        Style: total
        Box:
            Background: #F4F2EC
            Border: 1 0 0 0
        Cell: Total
            Span: 1 3
        Cell: 2,630.00
```

The `Id` of a row is an `Id` of the document, unique like any other: it is the target of `Link: #id` and of `{page of: id}`
([References](../common/references.md)), which lead to its band. That of a repeated row leads to the first of its pages.

> **Decision:** the row is not a box of the layout, but its look is its own: a band below its cells. That way a
> row `Border` is a whole line and not a border in each cell, and a background also covers the `Gap`. *Rejected
> alternative:* the `Box` of the `Row` repeated in each cell (a row `Border` would appear between cells), or a row
> with `Padding` and size, which would contradict the columns.

> **Decision:** the `Id` of a row styles its cells, like its `Style`: the look of the band is given only by its `Box`.
> *Rejected alternative:* `For id` with the `Id` of a row giving the look of the band, which would make the
> `For id` of the row and its `Style` different.

> **Decision:** no selectors by row position (`Last row cell`), which identify the totals row by its
> place and not by what it is: its `Style` and its `Id` are there for that.

## Breaks

A table breaks between rows, like a grid ([Breaks](../common/breaks.md#breaks-in-stack-grid-and-table)): a row that does not
fit in what is left of the page moves whole to the next one, and it is only split, with its cells in parallel, if it does not fit even on an
empty page.

A row that is split across pages also splits its band: on each page it goes from the beginning to the end of what there is of
the row, and its border is broken like that of any box, according to the `Edges` of its `Break` ([Breaks](../common/breaks.md)).
If a group of rows that a cell takes up at once is split, each band is trimmed on the first page, and on the next one
the continuation carries the band of the last row of the group.

> **Decision:** the continuation of a split group of rows carries the band of its last row: what continues on the
> next page is not shared out among the rows of the group. *Rejected alternative:* sharing it out by the heights before
> splitting, which do not correspond to what is placed.

The rows of `Header rows` and `Footer rows` are **repeated** on each page of the table: the header at the top and the footer right
after the last body row of that page, each one with the `Gap` that separates it from the body. They are never split and
do not accept `Break` (`TABLE_ROW_BREAK`). The body rows are broken within what the header and the footer leave: if
none fits, the whole table moves to the next page, and the header is not left alone. If at the beginning of a page the
header and the footer leave no room for the body, the table cannot advance and it is an error (`TABLE_REPEAT_OVERFLOW`). A body
row that does not fit in the room they leave is split in parallel, like any row, and if it cannot be split it overflows
the page (`PAGE_OVERFLOW`). The `Overlay` and `Page overlay` boxes of the cells of the header and the footer only go in the
first fragment of the table.

> **Decision:** a cell with a long paragraph in a table with a header is not an error: it is split. *Rejected
> alternative:* requiring a whole body row on each page.

> **Decision:** the footer goes after the last body row and not stuck to the end of the page area. *Rejected alternative:*
> stretching the table to the end of each page.

## Accessibility

A screen reader reads the table as a table: the cells of `Header rows` are the headers of their column and those that
declare `Row header: true`, those of their row, and it announces them with each cell. The repeated header and footer are read only
once, on the first page where they appear.

## What is not supported

- Merged borders between cells: each cell has its own.
- `Box` on a column, a row with its own size or padding, and a separate table title (a heading or a `Paragraph`
  before the table).
- Rows with fewer cells than columns, or `Span` values that pass from one group to another: they are errors.
