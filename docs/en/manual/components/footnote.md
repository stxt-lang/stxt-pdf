# `Footnote`

A `Footnote` is a **footnote**. The call, `[^id]`, is written in the text, attached to the word, and the note with that
`Id` anywhere in the section. The engine numbers the calls in order, paints them as superscripts and places each note
at the bottom of the page where its call falls, under a rule.

```
Document (dev.stxt.pdf):
    Language: en
    Title: Report
    Page:
        Paper: A4
        Margin: 60
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Serif
                Size: 11
                Line height: 16
                Markup: markdown
        For element: Paragraph
            Box:
                Margin: 0 0 10 0
        For element: Footnotes
            Typography:
                Size: 8
                Line height: 11
            Box:
                Border color: #999999
    Section:
        Paragraph:
            Content >>
                Sales grew by 12% in the quarter[^source], above the forecast,
                and the margin stayed at 31%.
        Paragraph: The plan for next year is presented in the following chapter[^plan].
        Footnote:
            Id: source
            Paragraph: Figures from the annual report, closed on December 31.
        Footnote:
            Id: plan
            Paragraph:
                Content >>
                    The plan includes opening two stores and was approved at the October meeting,
                    with the votes in favor of all the partners present.
```

## Properties

| Property | Default | What it does |
|---|---|---|
| `Id` | required | The name of the note, the one written in the call `[^id]` |

It also accepts `Style`, `Box` and `Typography`. The call needs `Markup: markdown` in the text (or in a rule, as above),
or to be inside a `Markdown`. In the `Section`, `Footnote numbering: restart` starts the numbering again at 1, and
`Footnote number format` writes it in letters or Roman numerals (`lower-alpha`, `upper-roman`...).

## How it behaves

- The note starts with a `Paragraph`, a `Text` or a `Markdown`, which gets the number as a superscript; after it, more
  paragraphs, lists or tables may follow. No headings or images.
- Each note is called once, and every call has its note in the same section; otherwise it is an error.
- The notes of a page go together at the bottom, under a rule, with the look of `For element: Footnotes` (the rule is
  its top border; its typography is that of all the notes) and that of `For element: Footnote` (each note).
- A line enters the page only if its note fits under it; otherwise it moves to the next page with the note. A long note
  may continue at the bottom of the next page.
- In a `Columns`, the notes go at the bottom of the page, not of the column.
- The call cannot go in a `Page overlay`, in a heading or inside another note.

More detail in the [reference](../../reference/components/footnote.md).
