# Headings: `Heading 1` to `Heading 6`

A `Heading N` is a **block** whose content is one or more `Text`, stacked as in a `Block`. It also accepts `Id`,
`Style`, `Link`, `In table of contents`, `Box` and `Typography`, and nothing else. Its `Text` elements inherit its `Typography`. It is laid out with the
usual block flow. Its meaning shows in the `For element` rules, in the breaks, in the numbering, in the tables of contents,
in the bookmarks of the PDF and in what a screen reader announces.

```
Styles:
    For element: Heading 1
        Typography:
            Size: 20
            Bold: true
        Box:
            Margin: 0 0 10 0
    For element: Paragraph
        Box:
            Margin: 0 0 6 0
Heading 1:
    Id: results
    Text: Results for the quarter
Paragraph:
    Content >>
        Sales grew by 12%.
```

**No implicit styles.** A `Heading 1` is not larger than a `Heading 2` by itself: the style of each level is
declared once in `Styles`, like that of any other element.

Its text, in the table of contents and in the bookmarks, is that of its `Text` elements joined by spaces.

## Breaks

The engine **always avoids** breaking inside a heading and after it, as if it declared `Keep together: true` and
`Keep with next: true` ([Breaks](../common/breaks.md)): a title is not left alone at the end of a page and is not split. They are relaxed
like the declared ones when there is no better break. That is why a heading does not accept `Break`, nor do its `Text`
elements (`BREAK_IN_HEADING`), nor from a rule (`STYLE_NOT_ALLOWED`). The break before the heading does not change.

> **Decision:** the engine always avoids it, instead of leaving it to the author. *Rejected alternative:* a default value
> that the author can change, which contradicts there being no implicit styles.

## Hierarchy

The levels have to be correct (`HEADING_LEVEL`). In document order, across the sections and the
`Markdown` elements, the first heading is `Heading 1`, and each one goes up at most one level with respect to the previous one: a `Heading 1`
can be followed by a `Heading 2`, but not by a `Heading 3`. Any number of levels can be dropped at once.

> **Decision:** level skips are an error, so that the bookmarks are always a well-formed tree.
> *Rejected alternative:* allowing them and hanging the bookmark from the last one of a lower level.

## Where it goes

Wherever an in-flow element goes, also as an item of `Stack` or `Grid`, but never inside a `Page overlay` (`HEADING_IN_PAGE_OVERLAY`), which
is repeated on each page and is not part of the content, nor inside a `List item` or a `Cell`.

> **Decision:** inside a `Page overlay` it would be a heading without a bookmark that a screen reader does not announce: it is an error.
> *Rejected alternative:* allowing it as a block without meaning.

## Outside the table of contents

`In table of contents: false` leaves a heading out of the tables of contents (`Table of contents`) without it ceasing to be a heading: it still has a
bookmark, counts in the hierarchy and is a target of `#id`. It is the usual thing for the title of the cover or that of the table of contents
itself. It is a property of the element, `true` by default, which is not inherited and does not go in `Styles`.

> **Decision:** it is chosen on each heading. *Rejected alternative:* the table of contents choosing by level (`Levels`) the ones it
> does not list, which does not work when the cover and the chapters are of the same level.

## Numbering (`Numbered`)

With `Numbered: true` the heading carries a number: `1`, `1.1`, `1.1.1`. Since no style is implicit, it is declared. It is a
property of `Typography`, so **it is inherited** and is written wherever it suits: in `For element: Document` it numbers all the
levels, in a rule `For element: Heading 1` only those of that level, in a `For style` rule those of a style, and in the
`Typography` of a heading, only in it (`Numbered: false` leaves it without a number).

```
Styles:
    For element: Document
        Typography:
            Numbered: true
Heading 1:
    Id: cover
    Text: Quarterly report
    Typography:
        Numbered: false
Heading 1:
    Text: Results               # "1 Results"
Heading 2:
    Id: sales
    Text: Sales                 # "1.1 Sales"
Paragraph: The data in section {number of: sales} confirm it.
```

- **How it is counted.** In document order, across the `Section` elements and the `Markdown` elements (their headings `#`, `##`, etc., are
  numbered like the others), with one counter per level: it goes up with each numbered heading of that level and goes back to 0 with
  any heading of a higher level, numbered or not. The number is that of levels 1 to N joined by dots,
  **without the levels that have no number**, each one written with the `Number format` of its heading (below). An
  unnumbered heading does not count and carries no number.
- **A title without a number.** The usual case: the title of the document is a `Heading 1` with `Numbered: false` and the sections,
  numbered `Heading 2`, which are `1`, `2`, `3` (and their `Heading 3`, `2.1`, `2.2`) because the title does not contribute a level. A new
  `Heading 1` without a number (another part, an appendix) makes the counters below start again.
- **It is written before the title.** The number (or its `Number label`, below) and a space go before the first `Text`
  of the heading, as if the author had written them: they measure what they write, the text can move to one more line
  (it continues under the number, without an indent) and they appear in the table of contents, in the bookmarks and in what is read. `Case`,
  `Letter spacing` and `Small caps` change them like the rest of the text (`Case: uppercase` writes `ANEXO A.`).
- **It can be referred to.** `{number of: id}` of a heading with an `Id` and numbered gives its complete number (`1.1`): "section
  {number of: sales}" writes "section 1.1" ([References](../common/references.md#number-of-id)).

### Format and label (`Number format`, `Number label`)

Two properties of `Typography`, which are inherited and given like `Numbered`: in `For element: Heading N` for a level or in
`For style` for some headings.

| Property | Values | Default | Notes |
|---|---|---|---|
| `Number format` | `decimal`, `lower-alpha`, `upper-alpha`, `lower-roman`, `upper-roman` | `decimal` | How the counter of the level of the heading is written: `1`, `a`, `A`, `i`, `I`. Letters continue after `z` with `aa`, and Roman numerals, after 3999, with more `M`. |
| `Number label` | text with `{number}` once | the number | What goes before the title: `{number}` is the complete number (`Number label: Appendix {number}.` gives `Appendix A.`). Without `{number}`, or with it twice, it is `VALUE_NOT_ALLOWED`. |

Each level of the complete number is written with the `Number format` of the last heading of that level: `upper-roman`
chapters and `decimal` sections give `II.1`. `{number of: id}` gives the number, without the label: "appendix
{number of: fees}" writes "appendix A". Appendices are numbered separately with an unnumbered heading above them, which
restarts the counters below: a `Heading 1` "Appendices" and its `Heading 2` with `Number format: upper-alpha` are `A`,
`B`, `C`.

```
Styles:
    For style: annex
        Typography:
            Numbered: true
            Number format: upper-alpha
            Number label: Appendix {number}.
Heading 1:
    Text: Appendices
Heading 2:
    Id: fees
    Style: annex
    Text: Fees                  # "Appendix A. Fees"
Paragraph: The amounts in appendix {number of: fees}.
```

> **Decision:** `Numbered`, `Number format` and `Number label` go in `Typography` so that they can be given in a rule and
> inherited. The format belongs to the level of each heading and the label, to the whole number. *Rejected alternative:* a
> template with the number of each level separately (`{1}.{2}`), harder to write and to read.

> **Decision:** Roman numerals beyond 3999 are written with more `M` (4000 is `MMMM`), instead of an error that would
> never occur in a real document. *Rejected alternative:* going back to digits, as other engines do, which mixes
> formats without warning.

> **Decision:** unnumbered headings do not contribute a level to the number of those below. *Rejected alternative:* an error
> if a numbered heading is below an unnumbered one, which forces numbering the title.

> **Decision:** the number is text of the first `Text` and not a separate box like the marker of a list. *Rejected
> alternative:* the number hanging to the left, which aligns the titles but forces a fixed gap.

`{number of: id}` gives the complete number (`4.2`) and not that of its last level like that of a `List item`. The headings of a
`Markdown` are numbered like the others, and they can be referred to if they carry an `Id`, which is written at the end of their text
(`## Sales {#sales}`, [Markdown](markdown.md#headings)).

## Bookmarks and accessibility

Each heading is a bookmark of the PDF, nested by level ([The PDF](../common/pdf.md#bookmarks)), and a screen reader
announces it as a heading of its level.

## What is not supported

- Default styles for the levels: they are declared in `Styles`.
- A number hanging to the left.
