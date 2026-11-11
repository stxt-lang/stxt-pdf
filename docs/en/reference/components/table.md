# `Table`

A `Table` places its cells in rows and columns: its rows (`Row`) go in a header (`Header rows`, optional), a
body (`Body rows`, required) and a footer (`Footer rows`, optional), **in that order** (`TABLE_ORDER`), and each row has its
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
    Body rows:
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
| `Column sizes` | list of tracks | required | `Table` | The columns, as in `Grid` ([Grid](grid.md#track-sizing)), with the space shared by their content ([Column widths](#column-widths)). |
| `Column align` | one alignment per column: `left`, `center`, `right` or `justify` | none | `Table` | The default alignment of the cells of each column. As many as `Column sizes` (`TABLE_COLUMN_ALIGN`). |
| `Gap` | 1 or 2 lengths (rows columns) | `0` | `Table` | Spacing between cells, as in `Grid`. |
| `Split rows` | `true`, `false` | `true` | `Table` | A row that does not fit in what is left of the page is split on that page, with its cells in parallel; with `false`, it moves whole to the next one ([Breaks](#breaks)). |
| `Span` | `rows columns`, two positive integers | `1 1` | `Cell` | Rows and columns that the cell takes up. |
| `Row header` | `true`, `false` | `false` | `Cell` | The cell is the header of its row ([accessibility](#accessibility)). Not valid in `Header rows` (`ROW_HEADER_IN_HEADER_ROWS`). |
| `Vertical` | `top`, `center`, `bottom` | `top` | `Cell` | Where the content of the cell goes in the height it has left over ([Vertical alignment of the content](#vertical-alignment-of-the-content-vertical)). |
| `Id` | text | none | `Row` | Target of links and of `{page of: id}`. Its `For id` rule styles the cells of the row ([Rows](#rows)). |
| `Box` | `Background`, `Border`, `Border color`, `Corner radius` | none | `Row` | The look of the band of the row ([Rows](#rows)). Nothing else (`ROW_BOX`). |

`Column sizes`, `Column align`, `Gap`, `Split rows` and the `Vertical` of the cells can also come from a rule of `Styles` ([Element properties](../common/styles.md#element-properties)); `Span` and
`Row header` cannot. Without `Column sizes`, its own or from a rule, it is an error (`TABLE_COLUMN_SIZES_MISSING`).
`Merge borders` (`true` or `false`, by default
`false`) goes in the `Box` of the `Table`, so it is also valid in a rule: the border between two cells is drawn once,
the wider of the two ([Merged borders](#merged-borders)).

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
(from `Header rows` to `Body rows`, from `Body rows` to `Footer rows`) and does not overlap another cell (`TABLE_SPAN`).

An explicit `Span: 1 1` is accepted: it is the default value written out and not a contradiction.

### Column widths

The columns are sized with the steps of `Grid` ([track sizing](grid.md#track-sizing)), except for how the space is
shared, which in a table goes **in proportion to the content** and not in equal parts:

- **Maximize** (step 3): the free space is shared among the non-flexible columns in proportion to what each one lacks to
  reach its limit, so that all of them cover the same fraction of the way from their narrowest width to their natural
  width. With two `auto` columns that go from 60 to 140 and from 50 to 90 and 60 free, the first one receives 40 and the
  second one 20.
- **Stretch** (step 5): without fractions, what is left over is shared among the `auto` columns in proportion to their
  natural width. With `auto auto`, natural widths of 20 and 60 and 180 left over, the columns measure 65 and 195. If all
  of them are empty, in equal parts.

Fixed widths, percentages, `between(a, b)` and fractions do not change: `Column sizes: 40mm auto auto` keeps the first
column at 40 mm and shares the rest between the other two by their content, and `auto *` gives the `auto` one its
natural width and the fraction what remains. The rows are sized as in `Grid`.

> **Decision:** the columns of a table share the space in proportion to their content, and those of a `Grid`, in equal
> parts. In a table, a column with long texts needs more room than one with short labels; in a layout `Grid`, two equal
> `auto` columns are expected to be equal. There is no property to choose: whoever wants another distribution fixes the
> widths or uses fractions. *Rejected alternative:* a property in `Table` to choose the distribution, or the same
> distribution in `Grid`.

## Cells

A `Cell` is a box with two forms, like a `Text`: with a value (`Cell: 100`), it is a text box of one line, and without
a value, a block with elements (any except the headings), or empty if it has none. Both at the same time are
an error (`CELL_CONTENT`). A long text goes in a `Paragraph`. It accepts `Id`, `Style`, `Link`, `Box`, `Typography`, `Span`,
`Row header`, `Trim margins` and `Vertical`. The borders are those of each cell, separate: a line between rows is drawn with the bottom border of the cells.
With `Merge borders: true`, the border between two cells is drawn once ([Merged borders](#merged-borders)).
The margins of a cell separate it from its area, as in a `Grid`.

> **Decision:** `Row header: true` goes in each cell that is the header of its row (the item of an invoice, the time of a
> timetable): it also serves for the label of a totals footer or the third column of a schedule. *Rejected
> alternative:* `Row header columns: N` in the `Table`, which only serves for whole columns.

## Vertical alignment of the content (`Vertical`)

A cell is stretched to the height of its row and its content starts at the top. `Vertical` says where the content goes
when the row is taller than it:

```
Styles:
    For element: Header cell
        Vertical: bottom
Table:
    Column sizes: * 60
    Header rows:
        Row:
            Cell: Item
            Cell:
                Paragraph: Amount before tax
    Body rows:
        Row:
            Cell:
                Paragraph: Dining table in solid walnut, with two drop leaves.
            Cell: 640.00
                Vertical: center
```

- **Values.** `top` (the default) leaves the content at the top, `center` centers it and `bottom` takes it to the
  bottom. The other values of `Vertical` (`stretch`, `justify`, `around`, `distribute`) are `VALUE_NOT_ALLOWED` in a
  cell, which is always stretched and has a single content to place.
- **What moves.** The box of the cell stays stretched to its row, with its whole `Background` and `Border`. Its
  content moves inside its content box: the line of a cell with a value, or all its elements together. What is left
  over is the height of the content box minus that of the content: with `center` half of it goes above, and with
  `bottom`, all of it. A content as tall as the cell, or taller, does not move.
- **It is the `Vertical` of a vertical `Stack`**, which says where the leftover goes ([Stack](stack.md)), and not that
  of an `Area` of a `Grid`, with which the box stops being stretched ([Grid](grid.md)).
- **The margins** of the elements of the cell that stay inside it count as content: the top one of the first and the
  bottom one of the last. `Trim margins` drops them ([Block](block.md#trimming-the-margins-trim-margins)).
- **From a rule.** `For element: Cell`, the position rules, `For column`, `For style` and `For id` give it, with the
  priority of [Cell style](#cell-style). `Table` and `Row` do not declare it: a `For element: Cell` rule inside the
  rule of the table does the same, and the rules of the `Style` and the `Id` of a `Row` reach its cells.
- **`Span` and `Height`.** A cell that takes up several rows uses the height of all of them. A cell with a `Height` of
  its own has no special case: the content is placed in its content box.
- **Split rows.** In a row that is split across pages ([Breaks](#breaks)), the content is placed in the fragment the
  cell **ends** in, with the height of the content box of that fragment and that of the content in it. In the previous
  fragments the content reaches the break and does not move. The content that ended on a previous page stays where it
  is: if the row goes on because of another cell, what the cell measures on the next page is empty.

> **Decision:** `Vertical` in `Cell`, with `top`, `center` and `bottom`: the name and the values the vertical
> alignment already has in `Stack`, `Grid`, `Item` and `Area`. *Rejected alternative:* a new property
> (`Vertical align: top | middle | bottom`), another name for the same thing and with values that are not those of the
> namespace.

> **Decision:** the cell does not stop being stretched, and what moves is its content. Its background and its border
> still fill the row, and the merged borders do not change. *Rejected alternative:* the `Vertical` of an `Area`, which
> shrinks the box to its content and leaves the background of the cell in the middle of the row.

> **Decision:** in a split row the content does not move from one page to another to be aligned: it is placed in the
> fragment the cell ends in, with what is in it. *Rejected alternative:* aligning against the height of the unsplit
> row, which is that of no page.

## Cell style

A rule of `Styles` ([Styles](../common/styles.md)) styles the cells of a table without repeating a style in each
one:

- **By position.** `For element: Header cell`, `Body cell` and `Footer cell` are the cells of each group, and
  `Odd row cell` and `Even row cell`, those of the odd and even rows of the **body**, counting from the first row of
  `Body rows` as 1 (the zebra striping, which the header and the footer do not take part in). The row of a cell that takes up several is the
  **first** of those it takes up. A cell matches at the same time that of its group and that of its row, and the usual rule
  `For element: Cell`.
- **By column.** `For column: 3` (or `For column: 2 4`, from 2 to 4) applies to the cells that **begin** in those
  columns, in any group: a cell with a `Span` of several columns belongs to the column where it begins.
- **`Column align`.** A `Column align: left right right` of the `Table` gives an alignment to each column (as many as
  `Column sizes`). It is only the default value of the `Align` of the cells: the position, column,
  style and `Id` rules override it, and so does what the cell itself declares.
- **By row.** The `Style` of a `Row` gives each of its cells the `For style` rules of those styles (the totals footer
  of an invoice: `Style: total` in the `Row` and not in each cell), and the `For id` rule of its `Id`, its own.
- **By table.** A cell rule written inside the rule of a `Table` (`For style: pricing` → `For element: Header cell`)
  styles only the cells of that table ([Styles](../common/styles.md#nested-rules)).

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

A `Row` is not a box of the layout: it takes up no space of its own and is not named in `For element`, like `Header rows`, `Body rows` and
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

## Merged borders

By default each cell paints its border, and two neighboring cells with `Border: 0.5` draw two lines side by side, 1 in
all. With `Merge borders: true` in the `Box` of the `Table`, the border between two cells is drawn **once**:

```
Styles:
    For element: Table
        Box:
            Merge borders: true
    For element: Cell
        Box:
            Padding: 3 5
            Border: 0.5
            Border color: #8A8478
    For element: Header cell
        Box:
            Border: 0.5 0.5 1.5 0.5
            Border color: #12315A
Table:
    Column sizes: * 60 60
    Header rows:
        Row:
            Cell: Rate
            Cell: Month
            Cell: Year
    Body rows:
        Row:
            Cell: Basic
            Cell: 9.00
            Cell: 90.00
```

- **Where it is written.** `Merge borders` belongs to the `Box` group, next to `Border`: it is written in the `Box` of
  the `Table` or in a rule of `Styles` ([Styles](../common/styles.md)), which also reaches the tables of a `Markdown`. In
  the example, `For element: Table` merges the borders of every table of the document, and a table separates them with
  `Merge borders: false` in its `Box`. It only has an effect on a `Table`: written in the `Box` of another element it is
  `MERGE_BORDERS_WITHOUT_EFFECT`, and if it reaches another box from a rule (a `For style` that a table and a `Block`
  use), that box does nothing with it.
- **The line.** Between two neighboring cells, the line is the wider of the two sides that touch, with its color
  (`Border color`, or that of the text of the cell). With the same width, that of the cell above or that of the one on
  the left wins. A side without border contributes nothing: the line is that of the other one, and if neither has one,
  there is no line. In the example, the line under the header is that of the header cells (1.5), and the others, 0.5.
- **By segments.** A cell that takes up several rows or columns has a segment of line for each cell next to it, each one
  resolved with that cell. Inside a cell that takes up several rows or columns there is no line.
- **Where it goes.** The lines go on the lines of the grid: the one between two cells, centered on it; that of the outer
  edge of the table, whole inside the table. Where two lines cross, the horizontal one goes into the vertical one by half
  the width of the latter, and the wider one is painted on top.
- **The room of each cell.** The `Border` of a cell is no longer painted: it is the room the cell leaves for the lines.
  On a side between two cells, half of the widest line of that side; on a side on the edge of the table, its whole
  border. The padding and the content start after it, so the height of the rows and the width of the columns count it.
- **What covers what.** The lines are painted over the backgrounds of the cells and of what they contain, and under
  their text. The band of a row (its `Box`, [Rows](#rows)) is not merged: it is painted under the cells, as always.
- **What it does not allow.** `Gap` on the table or `Corner radius` on one of its cells, also when `Merge borders` or
  the `Corner radius` come from a rule, are `MERGE_BORDERS_CONFLICT`: the border between two cells would not be one
  line. `Merge borders: true` in the `Box` of a table none of whose cells has a border is `MERGE_BORDERS_WITHOUT_EFFECT`;
  from a rule, that table stays as it is.

Page breaks have their rules in [Breaks](#breaks).

> **Decision:** the outer border of the table, and the top and bottom ones of each page it goes through, go whole inside
> the table: the table does not paint outside its box. *Rejected alternative:* centering them on the edge, as word
> processors do, which leaves half a line outside the table, in the margin or over what follows it.

> **Decision:** the room of each cell is computed with the cells next to it in the table as written. A line that only
> appears at a page break (that of the fragment, or the one under a repeated header) and is wider than that room goes
> into the padding of the cell by what it lacks. *Rejected alternative:* reserving on each side the widest line possible
> on any page, which would widen every row for a line that only appears on some.

> **Decision:** `Merge borders` is a boolean, `false` by default: without it, no table changes. *Rejected alternative:*
> `Borders: separate | collapse`, with a term that does not come from layout, and always merging the borders, as a word
> processor does, which would move every table with borders.

> **Decision:** `Merge borders` goes in the `Box` group, although it only has an effect on a table, so that a rule of
> `Styles` gives it to every table of a document or of a separate styles document, also to those of a `Markdown`. As with
> `Overflow: shrink`, what the element itself writes is checked, and what reaches a box where it has no effect from a
> rule does nothing. *Rejected alternative:* a property of the `Table` itself, like `Split rows`, which the rules do not
> accept and which left the tables of a `Markdown` without merged borders.

## Breaks

A table breaks between rows, like a grid ([Breaks](../common/breaks.md#breaks-in-stack-grid-and-table)): a row that does not
fit in what is left of the page is split on that page, with its cells in parallel, unless its `Row` declares
`Keep together: true` (the `Break` of the row is that of its cells) or some cell cannot place anything: then it moves whole
to the next one. With `Split rows: false` on the `Table`, every row that does not fit moves whole, and it is only split if it
does not fit even on an empty page.

A row that is split across pages also splits its band: on each page it goes from the beginning to the end of what there is of
the row, and its border is broken like that of any box, according to the `Edges` of its `Break` ([Breaks](../common/breaks.md)).
If a group of rows that a cell takes up at once is split, each band is trimmed on the first page, and on the next one
the continuation carries the band of the last row of the group.

> **Decision:** the continuation of a split group of rows carries the band of its last row: what continues on the
> next page is not shared out among the rows of the group. *Rejected alternative:* sharing it out by the heights before
> splitting, which do not correspond to what is placed.

With `Merge borders` ([Merged borders](#merged-borders)), the top and bottom lines of each page go inside the table, like
its outer border. If the break falls between two rows, the line between them is drawn on both pages, the same on each.
If it falls inside a row that is split, the side of each split cell is only drawn with `Edges: closed` (that of the
`Break` of its `Row`). Under the repeated header, the line is the wider one between the header and the body row that
follows it on that page. A cell that already ended on a previous page draws nothing in the continuation of its group.

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

- `Box` on a column and a row with its own size or padding. The title of a table ("Table 3. Terminals") is the
  `Caption` of a [`Figure`](figure.md) with the table inside.
- Rows with fewer cells than columns, or `Span` values that pass from one group to another: they are errors.
