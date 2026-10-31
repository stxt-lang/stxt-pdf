# `Columns`

A `Columns` element distributes its content in **columns**, like a newspaper: the text fills the first column, continues in
the second and, when the columns of a page run out, continues on the next page.

```
Document (dev.stxt.pdf):
    Language: en
    Title: Newsletter
    Page:
        Paper: A4
        Margin: 50
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Serif
                Size: 10
                Align: justify
        For element: Paragraph
            Box:
                Margin: 0 0 8 0
    Section:
        Columns:
            Count: 2
            Gap: 20
            Separator: 0.5
            Separator color: #999999
            Paragraph:
                Content >>
                    The Saturday market returns to the main square with more stalls than ever:
                    vegetables from the market gardens, cheese from the hills and wood-fired bread.
            Paragraph:
                Content >>
                    The residents' association is also organizing a walk along the river, starting
                    from the old bridge at ten in the morning.
            Paragraph:
                Content >>
                    Registration is open until Friday at the public library.
```

## Properties

| Property | Default | What it does |
|---|---|---|
| `Count` | none | The number of columns |
| `Min column width` | none | The minimum width of a column: as many fit as the width allows. With `Count`, at most `Count` |
| `Gap` | `0` | The gap between columns, a single value. With `Count: 1` it is an error, because there are no columns to separate |
| `Separator` | no line | The thickness of a line between columns, in the middle of the gap. It cannot be wider than the gap, and with `Count: 1` it is an error |
| `Separator color` | the text color | The color of that line. Only with `Separator` |
| `Balance` | `true` | With `true`, on the last page the columns are equally tall. With `false`, they are filled in order |

`Count`, `Min column width` or both are required. It also accepts `Id`, `Style`, `Link`, `Box`, `Typography` and `Break`.

## How it behaves

- All the columns have the same width: that of the `Columns` minus the gaps, shared out.
- Inside, the elements are stacked as in a `Block`. The content is broken between columns with the same rules as between
  pages: without stray lines and respecting `Break`.
- `Break` with `Before: column` or `After: column` jumps to the next column, and with `page`, to the next page.
- With `Balance: true`, the last page does not leave one column full and another empty: the engine looks for the height at
  which they are even without violating any break rule.
- A `Height` on the `Columns` limits the height of the columns. If the content does not fit, it is an error.
- Inside a `Columns` there can be no `Overlay` or `Page overlay`, and no height percentages.

A full-width title between two stretches of columns goes outside them, between two `Columns`.

More detail in the [reference](../../reference/components/columns.md).
