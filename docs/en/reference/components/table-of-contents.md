# `Table of contents`

A `Table of contents` is an automatic table of contents: an in-flow block, without children, that generates **one entry per heading** of the
levels in `Levels` (1 to 3 by default), in document order, also those of a `Markdown` and those that come after the
table of contents itself, except those that declare `In table of contents: false` ([Headings](heading.md#outside-the-table-of-contents)). With
`Of: figures` or `Of: tables` it is a **table of figures or of tables**: one entry per `Figure` of that counter
([below](#what-it-lists-of)). It accepts `Id`, `Style`, `Box`, `Typography` and `Break`, like a `Block`, and goes
wherever a `Markdown` goes. It does not accept `Link`: its entries already carry their own.

```
Table of contents:
    Levels: 1 2
    Leader: dots
```

| Property | Values | Default | Notes |
|---|---|---|---|
| `Of` | `headings`, `figures`, `tables` | `headings` | What it lists: the headings of `Levels`, the figures or the tables ([below](#what-it-lists-of)). |
| `Levels` | two integers from 1 to 6: the first and the last level | `1 3` | The headings it lists: `Levels: 1 2` lists the `Heading 1` and `Heading 2` elements. Without any heading at those levels it is an error. Only with `Of: headings`. |
| `Leader` | `dots`, `line`, `none` | `dots` | What fills the gap between the title and the page of each entry: dots, a line or nothing. |

`Levels` and `Leader` can also come from a rule of `Styles` ([Element properties](../common/styles.md#element-properties)); `Of` cannot: it says what the table of contents
is.

## What it lists (`Of`)

With `Of: figures`, the table of contents lists the `Figure` elements numbered with the counter of the figures, and with
`Of: tables`, those numbered with that of the tables ([Figure](figure.md#numbering)): by default, a `Figure` with a
`Table` counts among the tables and the rest among the figures, and `Figure counter` changes it. They appear in document
order, also those after the table of contents, numbered or not, except those that declare `In table of contents: false`.
Each entry is like that of a heading: the **text of the caption**, with its number or its `Number label` before it, the
leader and the page of the figure, clickable as a whole. All the entries are `TOC 1`, and `For element: TOC 1` styles
them.

```
Heading 1:
    In table of contents: false
    Text: List of figures
Table of contents:
    Of: figures
Heading 1:
    In table of contents: false
    Text: List of tables
Table of contents:
    Of: tables
```

`Levels` has no effect with `Of: figures` or `tables`, and it is an error (`LEVELS_WITHOUT_HEADINGS`). A table of figures
or of tables without any to list is `TABLE_OF_CONTENTS_EMPTY`.

> **Decision:** a table of contents lists one single thing (`Of`), and a table of figures and one of tables are two
> `Table of contents` elements. The entries of both are `TOC 1`, with the same rule, because they have no levels.
> *Rejected alternative:* a mixed table of figures and tables, or generated elements of their own (`TOC figure`), which
> ask for more rules.

## Entries

Each entry is a row with three parts:

- the **title**, which is the text of the heading, with its number if it has one
  ([Headings](heading.md#numbering-numbered)), its `Text` elements joined by a space and without inline formatting;
- the **leader**, which grows until it fills the gap, with 4 pt on each side;
- the **page number**, which works like a `{page of: ...}` of the heading ([References](../common/references.md)),
  also if the heading has no `Id`.

Without a leader (`Leader: none`), the
title fills the gap. If the title does not fit on one line, it breaks, and the number stays on the first line.

Each entry is clickable as a whole and leads to its heading, like a `Link: #id` ([Links](../common/links.md)).

> **Decision:** with a long title, the number stays on the first line. *Rejected alternative:* aligning it with the
> last one, which would require aligning by the baseline.

## Leader

With `dots`, square dots of 0.09 times the font size, every half font size and flush with the number. With `line`, it is a line of 0.05
times the font size, from one side of the gap to the other. They are painted on the baseline of the entry and with the color of the text. They are
drawing and not text: they are not selected or read.

> **Decision:** the leader is a drawing that fills the gap exactly. *Rejected alternative:* the "." character repeated,
> which would be copied with the text.

## Style

The entry inherits the typography of the `Table of contents`, and its boxes, that of the entry. The rules `For element: TOC 1` to
`TOC 6` give `Box`, `Typography` and `Break` to the entries of a level: the indent of each level is its left
`Padding`, and the space between entries, its `Margin` or its vertical `Padding`. The entries themselves carry no `Id` or
`Style`.

> **Decision:** one rule per level and not per part (`TOC title`), which would be finer and longer to write.

## Errors

These are errors:

- a `Table of contents` without any heading at its levels, or without any figure or table with `Of: figures` or
  `tables` (`TABLE_OF_CONTENTS_EMPTY`, with the line of the `Table of contents`);
- a `Levels` that is not two levels from 1 to 6 in order (`VALUE_NOT_ALLOWED`), or a `Levels` with `Of: figures` or
  `tables` (`LEVELS_WITHOUT_HEADINGS`);
- a `Table of contents` without its own or inherited `Font` or `Size` (`TEXT_WITHOUT_FONT` and `TEXT_WITHOUT_FONT_SIZE`, with
  the line of the `Table of contents`).

A heading of a section without page
numbers cannot be in a table of contents: it is declared with `In table of contents: false`.

> **Decision:** `Levels` are always two levels, to avoid doubting between "up to 2" and "only 2".

## Accessibility

A screen reader announces it as a table of contents, with each entry as a link to its heading or its figure. The PDF
bookmarks do not change ([The PDF](../common/pdf.md#bookmarks)).

## What is not supported

- Tables of contents of things other than headings, figures and tables (listings, equations), alphabetical indexes and
  entries written by hand.
- A mixed table of figures and tables.
