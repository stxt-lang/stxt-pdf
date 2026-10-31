# `Table`

A table of rows and columns. Its header and its footer are repeated on each page it goes through.

```
Document (dev.stxt.pdf):
    Language: en
    Title: Invoice
    Page:
        Paper: A4
        Margin: 50
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For element: Cell
            Box:
                Padding: 4 6
                Border: 0 0 0.5 0
                Border color: #C8C4BA
        For element: Header cell
            Typography:
                Bold: true
                Color: #FFFFFF
            Box:
                Background: #12315A
        For element: Even row cell
            Box:
                Background: #F4F2EC
        For style: total
            Typography:
                Bold: true
    Section:
        Table:
            Column sizes: * 60 80 80
            Column align: left right right right
            Header rows:
                Row:
                    Cell: Item
                    Cell: Qty
                    Cell: Price
                    Cell: Amount
            Body:
                Row:
                    Cell: Walnut chair
                        Row header: true
                    Cell: 12
                    Cell: 145.00
                    Cell: 1,740.00
                Row:
                    Cell: Dining table
                        Row header: true
                    Cell: 1
                    Cell: 890.00
                    Cell: 890.00
            Footer rows:
                Row:
                    Style: total
                    Cell: Total
                        Row header: true
                        Span: 1 3
                    Cell: 2,630.00
```

## The structure

- `Header rows` (optional), `Body` (required) and `Footer rows` (optional), in that order. Each one has one or more `Row`
  elements, and each row, its `Cell` elements from left to right.
- **`Column sizes`** (required) are the columns, with the same measurements as a `Grid`: points, percentages, `auto` (what
  its content asks for) and fractions (`*`: a share of what is left over) ([Grid](grid.md)).
- Each row fills **exactly** all the columns: an empty cell is written `Cell:`. A row with too many or too few cells is an
  error.
- **`Span: rows columns`** makes a cell span several: `Span: 1 3` is three columns. A cell that spans several rows keeps its
  column occupied in the rows below.
- The rows measure as much as their tallest cell, and all the cells of a row are stretched to that height.

## The cells

A cell with a value (`Cell: 145.00`) is a text. Without a value, it has elements inside (a `Paragraph`, a `List`, another
`Table`), but not both. Each cell has its own border: a line between rows is the bottom border of the cells. `Gap` on the
`Table` separates the cells.

**`Row header: true`** marks a cell as the header of its row (the item of an invoice, the time of a timetable): a screen
reader reads it with each cell of the row. The cells of `Header rows` are already column headers.

## Cell style

Without repeating a style on each cell:

| Rule | Cells |
|---|---|
| `For element: Cell` | All of them |
| `For element: Header cell`, `Body cell`, `Footer cell` | Those of the header, the body or the footer |
| `For element: Odd row cell`, `Even row cell` | Those of the odd or even rows of the body (zebra stripes) |
| `For column: 3` or `For column: 2 4` | Those that start in those columns |
| `For style: total` with `Style: total` on the `Row` | Those of that row |
| `Column align` on the `Table` | One alignment per column, the weakest of all |

The `Row` accepts `Style` and `Id` (its `For style` and `For id` rules apply to its cells), `Typography` (which its cells
inherit) and `Break` (that of the row). A cell does not accept `Break`: breaks belong to the row.

## The look of a row

With `Box`, a row has a background and a border of its own: a **band** from the first column to the last, under its
cells. A row border is a whole line, also over the gap between columns, and the background of a cell covers that of its
row. The `Box` of a `Row` only accepts `Background`, `Border`, `Border color` and `Corner radius`: the row measures what
its cells measure, so a `Padding` or a `Height` on it is an error. The `Id` of a row also serves to link to it
(`Link: #total`) or to cite its page (`{page of: total}`).

```
Document (dev.stxt.pdf):
    Language: en
    Title: Quote
    Page:
        Paper: A5
        Margin: 40
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For element: Cell
            Box:
                Padding: 4 6
        For style: total
            Typography:
                Bold: true
    Section:
        Table:
            Column sizes: * 80
            Column align: left right
            Gap: 0 8
            Header rows:
                Row:
                    Box:
                        Border: 0 0 1 0
                    Typography:
                        Bold: true
                    Cell: Item
                    Cell: Amount
            Body:
                Row:
                    Cell: Design
                    Cell: 1,200.00
                Row:
                    Cell: Layout
                    Cell: 800.00
            Footer rows:
                Row:
                    Id: total
                    Style: total
                    Box:
                        Background: #F4F2EC
                        Border: 1 0 0 0
                        Corner radius: 0 0 4 4
                    Cell: Total
                    Cell: 2,000.00
```

## Across pages

- The table is broken between rows: a row that does not fit in what is left moves whole to the next page.
- The header is repeated at the top of each page, and the footer, after the last row of each page. They are never left
  alone.
- A row that does not fit even on an empty page is split: its cells continue on the next page.
- A `Break` on a `Row` of the body forces or avoids a break before or after it.

More detail in the [reference](../../reference/components/table.md).
