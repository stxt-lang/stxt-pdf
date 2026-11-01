# References

A text can write data that the engine computes:

| Marker | What it writes |
|---|---|
| `{page}` | The printed number of the page where the text is ([Breaks](breaks.md#page-numbers-page-and-pages)) |
| `{pages}` | The last printed number of its page sequence ([Document](../components/document.md#page-numbering)) |
| `{page of: id}` | The printed number of the page where the box with that `Id` starts |
| `{number of: id}` | The number of that box in the document: that of a numbered `List item` or a numbered heading |
| `{heading N}` | The text of the last `Heading N` that starts on that page or before, in a `Page overlay`: the running head |
| `{heading N: first}` | That of the first `Heading N` that starts on that page |

They are written in the text of a `Text`, a `Paragraph` or a `Cell` with a value (the running heads, only in a
`Page overlay`), and they can be mixed:

```
Text: The wiring diagram is on page {page of: wiring}; see clause {number of: pago}.
Block:
    Id: wiring
List:
    Marker: decimal
    List item:
        Id: pago
        Paragraph: ...
```

## `{page of: id}`

- **Which page it is.** That of the **first** box that comes out of the referred box: if it is split across several pages,
  the page where it starts. It is the **printed number** of that page and not the physical one: the same one that `{page}`
  says in its footer. If the page has no number (`Page numbering: none`), it is `REFERENCE_NOT_PLACEABLE`. A `Section` with
  an `Id` can be referred to: it is its first page. If the box is a repeated row of a table (header or footer), the first
  of its pages.
- **What can be referred to.** Any box with an `Id` that is not a `Page overlay` and is not inside one, since they are
  repeated on every page (`REFERENCE_NOT_PLACEABLE`). The reference can go before or after the box, and also in a
  `Page overlay` (which writes the same on every page).
- **The `Id` is that of the box**, as in `For id`: that of an `Item` or an `Area` goes in its element. An `Id` that does
  not exist, or a reference without an `Id` (`{page of: }`), is an error (`REFERENCE_UNKNOWN`) with the line of the text,
  or that of the character if the text is a `Content >>` block.
- **The text measures what is written.** `page 9` and `page 10` measure differently, and the text can take one more line.
  That can move the referred box to another page. The engine paginates again until the written page and the real one match
  ([how](../implementation/layout.md#pagination)). If it does not manage to, it is `REFERENCE_UNSTABLE`, with the line of
  the reference that keeps changing. With normal documents it does not happen, because a number only changes width when
  its number of digits changes (9 → 10).
- **The font needs the digits**, not the letters of the marker.

> **Decision:** the page of a box is that of its first fragment, also in the repeated rows of a table.
> *Rejected alternative:* that of the last fragment.

## `{number of: id}`

The `List item` elements of a numbered list have it, and it is what their marker writes without the suffix: `3`, `c`, `IV`
(`decimal`, `lower-alpha`, `upper-roman`), with whatever `Start` and `Number` there are. Each item gives only its own
number: to write "4.2" from a nested list, both are written, `{number of: clausula}.{number of: apartado}`. Headings with
`Numbered: true` also have it, and their number is the complete one (`4.2`: their levels joined by dots,
[Headings](../components/heading.md#numbering-numbered)). A `List item` of a bulleted list or of a list without a marker,
an unnumbered heading or any other box has no number (`REFERENCE_NOT_NUMBERED`). A heading written in a `Markdown` can be
referred to if it carries `{#id}` ([Markdown](../components/markdown.md#headings)). An item of a Markdown list cannot
be referred to, because it carries no `Id`.

## `{heading N}` and `{heading N: first}`

A **running head**: the text of a heading in the header or the footer of each page, like the current chapter or section
of a book or a manual.

- **Which heading it is.** `{heading N}`, with `N` from 1 to 6, writes the text of the last `Heading N` that **starts on
  that page or before**: the one in effect at the end of the page. `{heading N: first}` writes the **first one that
  starts on the page** or, if none starts on it, the same as `{heading N}` (in a dictionary, the first and the last entry
  of the page). A heading of a higher level (a lower number) leaves the levels below it without a heading: on the first
  page of a chapter with no section yet, `{heading 2}` is empty, and does not write the last section of the previous
  chapter. Before the first `Heading N` of the document it is empty as well.
- **Which text.** That of the heading, as in the table of contents and in the bookmarks of the PDF
  ([Headings](../components/heading.md)): its `Text` elements joined by spaces and, if it is numbered, with its number in
  front (`3 Installation`). The `Case` and `Small caps` of the text that writes it apply to it as to the rest. A text that
  is left empty takes up no line.
- **Where.** Only in a text of a `Page overlay`, or inside one: the heading in effect depends on the page, and a
  `Page overlay` that carries a running head **is laid out on each page** with the text of that page (the others, once,
  and repeated). In any other text it is `HEADING_REFERENCE_OUTSIDE_PAGE_OVERLAY`. Since a `Page overlay` takes up no
  space, the flow and the references to pages do not change because of it.
- **The form.** `{heading 2}` or `{heading 2: first}`; another level or another variant is `REFERENCE_UNKNOWN`. A level
  the document does not use is valid and is empty on every page.
- **The font needs the characters** of every heading of that level, in the font of the text that writes them
  (`GLYPH_MISSING`).

> **Decision:** a heading of a higher level empties the running heads of the levels below, instead of leaving the last
> one that started. This way the section of the previous chapter does not stay in the header of a new chapter. *Rejected
> alternative:* the last `Heading N` that started, regardless of the higher levels, which is what the `StyleRef` of a
> word processor does.

> **Decision:** `{heading N: first}` with no heading of that level on the page writes the same as `{heading N}`, instead
> of being empty: a page without a new section is still inside the current one. *Rejected alternative:* empty.

## What is not supported

- Counters of the author's own, or number formats other than those of the markers and those of `Number format`.
- Referring to the elements generated by a `Markdown`, except its headings with `{#id}`.
