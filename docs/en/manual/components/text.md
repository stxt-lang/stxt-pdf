# `Text` and `Paragraph`

`Text` and `Paragraph` are text boxes: they break their text into lines of the width they have. The difference is their
role: a `Paragraph` is **a paragraph** of the content, and a `Text` is **standalone text**: a label, a figure, a line of a
header.

```
Document (dev.stxt.pdf):
    Language: en
    Title: Letter
    Page:
        Paper: A4
        Margin: 60
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Serif
                Size: 11
                Line height: 16
        For element: Paragraph
            Box:
                Margin: 0 0 10 0
            Typography:
                Align: justify
                Line breaking: balanced
                Markup: markdown
    Section:
        Text: Madrid, March 3
            Typography:
                Align: right
            Box:
                Margin: 0 0 24 0
        Paragraph: Dear Ms. García,
        Paragraph:
            Content >>
                We are writing to confirm the order of **twelve walnut chairs**,
                which will be delivered in *two weeks* to the address provided.
        Paragraph: Kind regards.
```

## The text

It goes in the value if it is short (`Paragraph: Kind regards.`) or in a `Content >>` block if it is long. One of the
two, never both. Inside the block:

- Single line breaks are spaces, so the lines can be broken wherever is convenient.
- A blank line is an error: for several paragraphs, several `Paragraph` elements. Only in preformatted text
  (`Preformatted: true`, [Text formatting](../common/text-formatting.md)) is it one more line of the text.

The text is literal except with `Markup: markdown` ([Text formatting](../common/text-formatting.md)).
It can contain page numbers and references: `{page}`, `{pages}`, `{page of: id}`, `{number of: id}`
([References and links](../common/references-and-links.md)).

## Properties

They accept `Id`, `Style`, `Link`, `Box`, `Typography`, `Break` and `Rotate`. They need `Font` and `Size`, their own or
inherited ([Typography](../common/typography.md)). With `Rotate: -90`, the text is set vertically and takes up its place
rotated ([Layers and rotation](../common/layers-and-rotation.md#rotating-a-box-rotate)).

## How they behave

- They take up all the available width, unless they declare `Width`, and their height is that of their lines.
- They are split across pages between two lines, without leaving stray lines (`Widows` and `Orphans`).
- A word wider than the line is an error: words are not broken.
- As an item of a `Stack` in a row, a text starts from its size without breaking any line and becomes narrower if there is
  no room ([Stack](stack.md)).

## `Text` or `Paragraph`?

`Paragraph` is for the paragraphs of the content and `Text`, for everything else: a label, a figure, a line of a header,
the text of a heading. This way the tagged PDF has the correct structure, and a `For element: Paragraph` rule styles all
the paragraphs without touching the rest.

More detail in the [reference](../../reference/components/text.md).
