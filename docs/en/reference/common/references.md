# References

A text can write data that the engine computes:

| Marker | What it writes |
|---|---|
| `{page}` | The printed number of the page where the text is ([Breaks](breaks.md#page-numbers-page-and-pages)) |
| `{pages}` | The last printed number of its page sequence ([Document](../components/document.md#page-numbering)) |
| `{page of: id}` | The printed number of the page where the box with that `Id` starts |
| `{number of: id}` | The number of that box in the document: that of a numbered `List item`, a numbered heading or a numbered `Figure` |
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
[Headings](../components/heading.md#numbering-numbered)), and so do the numbered `Figure` elements, whose number is that
of their counter (`3`, without the label of `Number label`, [Figure](../components/figure.md#numbering)). A `List item`
of a bulleted list or of a list without a marker, an unnumbered heading, a figure with `Numbered: false` or any other box
has no number (`REFERENCE_NOT_NUMBERED`). A heading written in a `Markdown` can be
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

## References as text

A marker inside another pair of braces is text: `{{page}}` writes `{page}`, and not the number of the page.

```
Text: Page {page} of {pages}. The footer is written {{page}} of {{pages}}.
```

writes "Page 3 of 12. The footer is written {page} of {pages}.".

- **It applies to the six markers** of the table above, with whatever they carry inside: `{{page of: id}}`,
  `{{number of: id}}`, `{{heading 2: first}}`. What is left is text: its `Id` is not looked up and it does not have to go
  in a `Page overlay`.
- **It applies in every text**: with `Markup: none` and with `Markup: markdown`, in preformatted text, in a `Cell` and in
  a `Markdown`, also in its code and in its code blocks.
- **Only one pair is removed.** `{{{page}}}` writes `{{page}}`. The braces on each side have to belong to the same
  marker: `{{page}` is a brace and the number of the page.
- **What is not a marker does not change.** `{{name}}`, `{ {page} }` and any other text between braces are written as
  they are.
- **In Markdown, an escaped brace is text too**: `\{page\}` and `\{page}` write `{page}`. Not in its code, where the
  backslash is text and what follows it is replaced: there it is written `{{page}}`.
- **The text of a heading is copied as it is** to the table of contents, to the bookmarks of the PDF and to the running
  heads: a `{{page}}` of a heading is `{page}` in all of them.

A program that writes documents with texts that are not its own (a name, an address, what a user types) passes the
ones that do not go in Markdown through `Reference.escape`, which wraps each marker it finds
([Using from Java](../implementation/api.md#texts-that-come-from-data)).

> **Decision:** a marker becomes text by wrapping it in another pair of braces, and that applies the same in every text.
> It changes no text that did not already have a marker. *Rejected alternatives:* the backslash also with
> `Markup: none`, where the text is literal and a backslash is a backslash (a Windows path followed by a marker could
> not be written); and a property that turns off the markers of a text, which does not let one that is replaced and one
> that is not be mixed in the same text.

## What is not supported

- Counters of the author's own, or number formats other than those of the markers and those of `Number format`.
- Referring to the elements generated by a `Markdown`, except its headings with `{#id}`.
