# `Columns`

A `Columns` distributes its content into **columns** of the same width, as in a magazine: the content fills the first
column, continues in the second and, when the columns of a page run out, continues in the first column of the
next page. It accepts `Id`, `Style`, `Link`, `Box`, `Typography` and `Break`, like a block, and its own properties. It goes wherever an
in-flow element goes, also inside another `Columns`.

```
Columns:
    Count: 2
    Gap: 20
    Separator: 0.5
    Separator color: #999999
    Paragraph:
        Content >>
            Long text that flows through the two columns...
    Paragraph:
        Content >>
            ...and continues on the next page.
```

| Property | Values | Default | Notes |
|---|---|---|---|
| `Count` | integer ≥ 1 | none | Number of columns. With `Min column width`, the maximum. |
| `Min column width` | number > 0 | none | Minimum width of the columns: as many fit as the width allows. Points only. |
| `Gap` | number ≥ 0 | `0` | Space between columns: a single value, in points (two values are `VALUE_NOT_ALLOWED`). With `Count: 1` it is an error (`SINGLE_COLUMN`). |
| `Separator` | number > 0 | no line | Thickness of the line between columns, centered in the gap. It does not exceed `Gap` (`SEPARATOR_WIDTH`). With `Count: 1` it is an error (`SINGLE_COLUMN`). |
| `Separator color` | color | the text `Color` | Only with `Separator` (`SEPARATOR_COLOR_WITHOUT_SEPARATOR`). It does not affect the layout. |
| `Balance` | `true`, `false` | `true` | `true` evens out the columns of the last page, and `false` fills them in order. |

`Count`, `Min column width` or both are required (`COLUMNS_WITHOUT_COUNT`). They do not go in the `Styles` rules, which only
accept the groups.

## Columns

The number and the width come from `Count` and `Min column width`, over the width of the content box (`W`) and with `Gap`
(`g`) between each two:

- only `Count: n`: `n` columns;
- only `Min column width: w`: as many as fit, `⌊(W + g) / (w + g)⌋`, and at least one;
- both: those that fit, without exceeding `Count`.

The columns share the width left by the gaps: they measure `(W − (n − 1) · g) / n`.

## Content

Inside the columns the children are stacked in block flow ([Block](block.md#block-flow)): each column isolates
margins, so the margins inside do not come out of the `Columns`. It accepts all in-flow elements.
An `Overlay` or a `Page overlay` inside a `Columns`, at any depth, is an error (`OVERLAY_IN_COLUMNS`). The
height of the columns is not definite: a vertical percentage inside is an error (`INDEFINITE_PERCENTAGE`), even if the
`Columns` has `Height`.

> **Decision:** the height of the columns is not definite, even if the `Columns` has `Height`: it comes from the distribution and is not
> known when the document is read.

## Height of the columns

Each column is a fragment container ([Breaks](../common/breaks.md#model)), and the content breaks between columns
in the same way as between pages, with the same rules (widows and orphans, `Keep with next`, `Keep with previous` and `Keep together`, progress guarantee). On each page, the
columns measure what remains of the page for the content of the `Columns` and, if it has `Height` (or `Max height`), they do not exceed what
remains of it:

1. They are filled **in order**: the first column, from where the content was left, and each of the following ones, from
   where the previous one ends. Filling stops when the content or the columns run out, or at a forced page break.
2. If the content ends on this page and `Balance` is `true`, the columns are **balanced**: the minimum height is sought
   with which the content fits in them without breaks between columns that violate more rules than those of filling in order.
   With `Balance: false` they stay as in step 1. The pages on which the content does not end are not balanced.
3. On that page, the `Columns` measures what its tallest column does (with `Height`, what any box with a definite height does,
   [Breaks](../common/breaks.md#definite-height-and-breaks)).

Outside a page and without a definite height (a `Columns` that is an item of `Stack` or of `Grid`, or that is in an `Overlay`) there
is no height to fill: the columns are always balanced, also with `Balance: false`.

> **Decision:** balancing does not violate rules that filling in order respects: a block with `Keep together: true` does not
> break and a three-line paragraph with widows and orphans of 2 is not distributed. *Rejected alternative:* a balancing that
> violates rules to even out better, or balancing the intermediate pages as well.

## Breaks

A `Columns` breaks across pages: what does not fit in the columns of a page continues in the first column of the
next one. If the first column cannot place anything, the whole `Columns` moves to the next page, and if another
column cannot, the rest does. A column places something by force only if the `Columns` is at the start of the page. When the
page break is chosen, the break of the last column counts. `Before: column` and `After: column` (of `Break`) force a break to the
next column. Outside a `Columns` they are an error (`COLUMN_BREAK_OUTSIDE_COLUMNS`). A `Before: page` or `After: page`
inside ends the columns of the page.

## What does not fit

If the own height of the `Columns` (`Height`, `Max height`) ends on a page and its content does not fit in the columns of
that page, it is an error (`BOX_OVERFLOW`), on the line of that property. It is also an error, on the line of `Count`, when, with no page or height to
fill, its column breaks need more columns than it has ([Overflow](../common/overflow.md)).

> **Decision:** content that does not fit in the columns is an error. *Rejected alternative:* overflow columns
> to one side, or a last column without a limit.

## Line between columns

With `Separator`, a line of that thickness is painted between each two columns **with content**, centered in the gap, from the top of
the content box and with the height of the tallest column on that page. Its color is `Separator color` or, without it, the text
`Color`. It is painted with the background of the `Columns`, below the content, and cannot be wider than `Gap`.

> **Decision:** the line takes up no space, so a line wider than the gap would be painted over the columns.

## Intrinsic sizes

With `Count` and without `Min column width`, the narrowest width is `Count` columns of the narrowest width of the content plus the gaps. With
`Min column width`, it is one column, because the number of columns adapts to the width. The natural width is `Count` columns (or one, without
`Count`) of the natural width of the content, or of `Min column width` if it is larger, plus the gaps.

> **Decision:** with `Min column width`, the narrowest width is that of a single column. *Rejected alternative:* the smaller of
> `Min column width` and the narrowest width of the content.

## Accessibility

The content is read in order, column by column, and the line between columns is not read.

## What is not supported

- Content that spans all the columns at once (a full-width title in the middle): the `Columns` is split in two.
- Balancing all the pages: only the last one is balanced.
- Lines between columns with a style.
- `Overlay` and `Page overlay` inside the columns.
