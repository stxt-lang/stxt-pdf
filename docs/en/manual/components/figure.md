# `Figure`

A `Figure` is a **figure with a caption**: an image, a code, a table or a block with its `Caption`. The engine numbers
it ("Figure 1.", "Table 1."), puts the number before the caption, writes it where asked with `{number of: id}` and
lists it in a table of figures or of tables.

```
Document (dev.stxt.pdf):
    Language: en
    Title: Catalog
    Page:
        Paper: A4
        Margin: 50
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For element: Paragraph
            Box:
                Margin: 0 0 8 0
        For element: Figure
            Typography:
                Number label: Figure {number}.
            Box:
                Margin: 6 0 14 0
        For element: Caption
            Typography:
                Size: 8.5
                Color: #5E7479
            Box:
                Margin: 4 0 0 0
        For element: TOC 1
            Box:
                Margin: 0 0 3 0
    Section:
        Paragraph: The two pieces of the collection are shown in figures {number of: chair} and {number of: table}.
        Figure:
            Id: chair
            Caption: Walnut chair with a wool seat.
            Image: examples/catalog/silla.jpg
                Alt: Walnut chair with a wool seat
                Box:
                    Width: 160
        Figure:
            Id: table
            Caption: Walnut dining table, for six.
            Image: examples/catalog/mesa.jpg
                Alt: Walnut dining table
                Box:
                    Width: 160
        Figure:
            Caption: Measurements of the pieces.
            Table:
                Column sizes: * * *
                Header rows:
                    Row:
                        Cell: Piece
                        Cell: Width
                        Cell: Height
                Body:
                    Row:
                        Cell: Chair
                        Cell: 45 cm
                        Cell: 82 cm
                    Row:
                        Cell: Table
                        Cell: 180 cm
                        Cell: 75 cm
        Paragraph: List of figures
        Table of contents:
            Of: figures
```

## Properties

| Property | Default | What it does |
|---|---|---|
| `Caption position` | `bottom`; `top` with a table | Where the caption goes: `top`, above the content, or `bottom`, below |
| `Figure counter` | `tables` with a table; `figures` with the rest | Which counter numbers it: `figures` or `tables`. Figures and tables are numbered apart |
| `Alt` | required with a `Block` | The alternative text of the whole figure, only when it contains a `Block`. With an image, the `Alt` goes in the image |
| `In table of contents` | `true` | With `false`, the figure is not listed in the tables of contents, but it is still numbered |

It also accepts `Id`, `Style`, `Box`, `Typography`, `Break`, `Side` and `Clear`. `Numbered: false` (in its `Typography` or in
a rule) leaves it without a number; `Number label` and `Number format` write the number as in the headings
([Headings](heading.md#numbering-numbered)).

## How it behaves

- It contains **exactly one element** (`Image`, `Barcode`, `Table` or `Block`) and a `Caption`. For a figure with several
  images, they are grouped in a `Block`, and the `Alt` of the whole goes in the `Figure`.
- The `Caption` is a text like a `Text`: one line in the value or a `Content >>` block, with `Markup: markdown` if it has
  formatting. It is written anywhere inside the `Figure`: the engine places it below the content, or above a table.
- The number goes before the caption, with a space: "1 Diagram". With `Number label: Figure {number}.` in
  `For element: Figure`, "Figure 1. Diagram". Tables count apart: the first `Figure` with a `Table` is table 1.
- `{number of: id}` writes the number of a figure with `Id`, without the label: "figure 2".
- The figure **is not split** between pages: if it does not fit, it goes whole to the next one. A table inside is split,
  and the caption stays with its first part (or with the last, if it goes below).
- A `Table of contents` with `Of: figures` lists the figures, and with `Of: tables`, the tables, with the text of their
  caption and their page. It is styled with `For element: TOC 1`.
- The image or the code of a figure needs `Alt`: a figure with a caption is content and a screen reader reads it with
  its caption.
- It goes wherever an in-flow element goes, also in an `Item` or an `Area` and as a side box with `Side` (with `Width`),
  but not in a `Page overlay`.

More detail in the [reference](../../reference/components/figure.md).
