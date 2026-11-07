# `Figure`

A `Figure` is a **figure with a caption**: a block with exactly one element (`Image`, `Barcode`, `Table` or `Block`) and
a `Caption`, which goes below the content or, in a table, above it. The engine numbers it with a counter of its own,
writes the number before the caption ("1 Wiring diagram", or "Figure 1. Wiring diagram" with `Number label`), gives it
with `{number of: id}` and lists it in a `Table of contents` with `Of: figures` or `Of: tables`
([Table of contents](table-of-contents.md#what-it-lists-of)). It accepts `Id`, `Style`, `Box`, `Typography`, `Break`,
`In table of contents`, `Side` and `Clear`, and the properties below. It does not accept `Link`: the link goes in the
element inside.

```
Styles:
    For element: Figure
        Typography:
            Number label: Figure {number}.
        Box:
            Margin: 8 0 12 0
    For element: Caption
        Typography:
            Size: 8
            Color: #5E7479
        Box:
            Margin: 3 0 0 0
Figure:
    Id: wiring
    Caption: Wiring diagram of the four terminals.
    Image: examples/manual/en/wiring.svg
        Alt: Wiring diagram of terminals L, N, C and NO with the wire colors
        Box:
            Width: 100%
Paragraph: The wires are connected as figure {number of: wiring} shows.
```

| Property | Values | Default | Notes |
|---|---|---|---|
| `Caption position` | `top`, `bottom` | `bottom`; `top` with a `Table` | Where the caption goes: above or below the content, wherever it is written ([the caption](#the-caption-caption)). |
| `Figure counter` | `figures`, `tables` | `tables` with a `Table`; `figures` with the rest | Which counter numbers it and which tables of contents list it ([numbering](#numbering)). |
| `Alt` | non-empty text | none; required with a `Block` | The alternative text of the whole figure, only with a `Block` inside: with an image, the `Alt` goes in the image ([accessibility](#accessibility)). |
| `In table of contents` | `true`, `false` | `true` | Whether the figure has an entry in the tables of contents of figures or tables. With `false` it is still numbered. |

`Caption position`, `Figure counter` and `In table of contents` can also come from a rule of `Styles`
([Element properties](../common/styles.md#element-properties)); `Alt` cannot.

`Numbered`, `Number format` and `Number label` belong to `Typography` and apply to figures too ([numbering](#numbering)).

## The caption (`Caption`)

The `Caption` is a text, in the value (one line) or in a `Content >>` block, like a `Text` ([Text](text.md)): it accepts
`Id`, `Style`, `Box` and `Typography`, it inherits the typography of the `Figure`, and `Markup: markdown` gives it
formatted runs. `For element: Caption` styles all the captions; a `Figure` has exactly one (the schema requires it).

- **Where it goes.** It is written anywhere among the children of the `Figure`, and the engine places it **below** the
  content or, if the content is a `Table`, **above**; `Caption position` changes it. It is the editorial convention: the
  caption of a figure goes below and the title of a table, above.
- **The number goes first.** As in a heading ([Headings](heading.md#numbering-numbered)): the number, or its
  `Number label`, and a space are written before the text of the caption, as if the author had written them. They
  measure what they write, the caption may take another line, and they appear in the tables of contents and in what is
  read.

> **Decision:** the caption is placed by `Caption position` and not by the order in which it is written, with a default
> that depends on the content (below, and above a table). *Rejected alternative:* the document order, which requires
> remembering the convention in every figure.

## Numbering

A `Figure` **is numbered by default**: a figure with a caption almost always is. `Numbered: false` declared in its
`Typography` or in a rule that reaches it (`For element: Figure`, `For style` or `For id`) removes the number; what it
inherits from an outer box does not count, so a document with `Numbered: false` in `For element: Document` still
numbers its figures. A figure without a number does not count: the next one continues the counter.

- **Two counters.** Figures and tables are numbered apart: `Figure counter: figures` or `tables`. Without declaring it, a
  `Figure` with a `Table` counts among the tables, and the rest, among the figures. Each counter goes up in document
  order, across the sections, and never starts again.
- **The format and the label.** `Number format` writes the number in digits, letters or Roman numerals, and
  `Number label` sets what goes before the caption, with `{number}` where the number goes:
  `Number label: Figure {number}.` gives "Figure 3. Diagram". They belong to `Typography`, are inherited and are given in
  `For element: Figure` (or in a rule for the tables, with `For style`). Without `Number label`, the caption starts with
  the number and a space, like a heading.
- **It can be referred to.** `{number of: id}` of a numbered figure gives its number, without the label: "figure
  {number of: wiring}" writes "figure 3" ([References](../common/references.md#number-of-id)). That of a figure without a
  number is `REFERENCE_NOT_NUMBERED`.

```
Styles:
    For element: Figure
        Typography:
            Number label: Figure {number}.
    For style: table
        Typography:
            Number label: Table {number}.
Figure:
    Caption: Front view                   # "Figure 1. Front view"
    Image: examples/manual/en/thermostat.svg
        Alt: The thermostat seen from the front
Figure:
    Style: table
    Caption: Terminal reference           # "Table 1. Terminal reference", above the table
    Table:
        Column sizes: 56 *
        Body:
            Row:
                Cell: L
                Cell: Live
```

> **Decision:** the figure is numbered by default, and only a `Numbered: false` declared in it or in a rule of its own
> leaves it without a number; what is inherited does not count. `Numbered` is inherited so that a rule numbers all the
> headings of a level, and that same inherited value (almost always false) would leave every figure without a number.
> *Rejected alternative:* `Numbered` inherited as in the headings, which requires declaring it in every document with
> figures.

> **Decision:** without `Number label`, the caption starts with the bare number, like the title of a heading, and the label
> ("Figure", "Figura", "Table") is declared once in `For element: Figure`. *Rejected alternative:* a default label by
> `Language`, which would be the first natural-language text the engine writes.

> **Decision:** the content decides the counter, and `Figure counter` only forces it (a `Block` with a drawn table that
> counts among the tables, a table that counts as a figure). *Rejected alternative:* a single counter, or one that is
> always declared.

## The content

A `Figure` contains **exactly one element** besides its `Caption`: an `Image`, a `Barcode`, a `Table` or a `Block`
(`FIGURE_CONTENT`, with the line of the second element, or that of the `Figure` if there is none). A `Block` serves for a
composite figure: two photos in a `Stack`, a drawing with `Overlay` elements on top, a table with a note below.

The content and the caption are stacked in block flow, as in a `Block` ([Block](block.md)): the size, the margins and
the `Alt` of the image go in the `Image`; the `Box` of the `Figure` gives the margins, the background and the border of
the whole, and its `Typography`, that of the caption.

## Breaks

A `Figure` with an image, a `Barcode` or a `Block` **is not split**: the engine keeps it together as if it declared
`Keep together: true` ([Breaks](../common/breaks.md)), which is relaxed like the declared ones when there is no better
break. A `Figure` with a `Table` lets the table break as always, and the caption goes with its first part if it is above
and with the last if it is below (`Keep with next` or `Keep with previous`, without declaring them). `Break` is accepted
in the `Figure` for the rest: `Before`, `After`, `Keep with next`...

> **Decision:** the figure is not split, except for the table it contains, and the caption stays with the part of the
> table it belongs to. *Rejected alternative:* leaving the breaks to the author, with a caption that may stay alone at the
> top of a page.

## Where it goes

Wherever an in-flow element goes, also as the element of an `Item` or an `Area`, but not inside a `Page overlay`, which
is repeated on every page, nor inside a `Footnote`. With `Side` it is a side box, like a `Block`: it needs `Width` or
`Max width` (`SIDE_WITHOUT_WIDTH`) and the text that follows it runs beside it, caption included
([Block](block.md#side-boxes-side)). `Clear` moves it below the side boxes before it. A `Figure` does not run beside a
side box: it goes below it, like a table.

## Tables of contents

A `Table of contents` with `Of: figures` lists the figures that count among the figures, and with `Of: tables`, those
that count among the tables, in document order and with the text of their caption (number or label included), a leader
and the page, in clickable `TOC 1` entries ([Table of contents](table-of-contents.md#what-it-lists-of)).
`In table of contents: false` leaves a figure out without removing its number.

## References and links

A `Figure` with `Id` is referred to with `{number of: id}` (its number) and `{page of: id}` (its page), and `Link: #id`
leads to it ([References](../common/references.md), [Links](../common/links.md)).

## Errors

| Code | When |
|---|---|
| `FIGURE_CONTENT` | A `Figure` without exactly one element besides its `Caption` |
| `FIGURE_WITHOUT_ALT` | The `Image` or the `Barcode` of a `Figure` without `Alt`, or a `Figure` with a `Block` without its own `Alt` |
| `FIGURE_PROPERTY_WITHOUT_EFFECT` | `Alt` in a `Figure` with an image (the `Alt` goes in the image) or with a `Table` (it is read by itself) |
| `SIDE_WITHOUT_WIDTH` | A `Figure` with `Side` without `Width` or `Max width` |
| `REFERENCE_NOT_NUMBERED` | `{number of: id}` of a `Figure` with `Numbered: false` |

## Accessibility

A figure with a caption is **content** and not decoration: the `Image` or the `Barcode` inside carries a required `Alt`,
and a `Figure` with a `Block`, its own, which describes the whole. In the tagged PDF the figure is a `Figure` with that
`Alt` and its caption in a `Caption`; with a `Table`, the table carries the `Caption` as its first or last child
([The PDF inside](../implementation/pdf.md#figures)). A screen reader reads the caption with what it describes. Figures
have no PDF bookmark.

## What is not supported

- Restarting the numbering per chapter (`2.3`) and counters of the author's own (listings, equations).
- A default label by language: it is declared with `Number label`.
- Subfigures (`1a`, `1b`) with a caption each under a common caption.
- A caption on the images of a `Markdown`, which remain `Image` elements without a caption.
- PDF bookmarks for the figures.
