# `Text` and `Paragraph`

Both are text boxes and are laid out in the same way, but each one has its role. A `Paragraph` is **a paragraph of the
content**. A `Text` is **standalone text** that is not a paragraph of the content: a label, a figure, a line of a
header or of a footer, the pieces of a heading, a code block. Neither of them has several paragraphs inside:
consecutive paragraphs are several `Paragraph` elements, in a `Block` if they go together.

> **Decision:** two elements with different roles and without overlap: the paragraph, which is styled with `For element:
> Paragraph` without touching the rest of the texts, and standalone text. *Rejected alternative:* a single text element, which
> would force styling the paragraphs with `Style`, or a `Text` with several paragraphs, which did the same as several
> `Paragraph` elements.

## `Text`

Standalone text, a box without children: it breaks its text into lines ([Typography](../common/typography.md#line-breaking)).
It accepts `Id`, `Style`, `Link`, `Box`, `Typography`, `Break`, `Content` and `Rotate`, and goes wherever an in-flow element goes. With
`Rotate` it is written rotated (a vertical text) and takes up the rectangle that encloses it. Without `Width`, it does not break its
lines ([Layers and rotation](../common/layers-and-rotation.md#rotation-in-flow-block-text-paragraph-and-image)).

The text goes in its value (`Text: A title`) or in a `Content >>` child with an indented block. One of the two is
required, and both are not accepted (`TEXT_CONTENT`). A blank line is an error (`TEXT_BLANK_LINE`, on the line of the
blank line), except in preformatted text (`Preformatted: true` or `wrap`), where it is one more line of the
text ([Text formatting](../common/text-formatting.md#preformatted-text)). By default the
text is literal, and with `Markup: markdown` it accepts formatting within the line
([Text formatting](../common/text-formatting.md#inline-formatting-markup)).

```
Text: A line, in the value.
Text:
    Content >>
        A long text, in a block. Single line breaks
        inside the block are equivalent to spaces.
```

`Font` and `Size` are required, own or inherited ([Typography](../common/typography.md)).

> **Decision:** `Content` always goes in a block (`Content: something` is an error): short text goes in the value. An empty `Text`
> is an error. *Rejected alternative:* `Content` also inline, two ways of writing the same thing.

## `Paragraph`

A paragraph of the content: it accepts the same as a `Text` (`Id`, `Style`, `Link`, `Box`, `Typography`, `Break`, `Rotate` and
the text in the value or in `Content`) and is laid out in the same way. It is **a single paragraph**: a blank line in its `Content` is an
error (`TEXT_BLANK_LINE`), also in preformatted text. For several paragraphs, several `Paragraph` elements are used. The
`For element: Paragraph` rules style the paragraphs without touching standalone text.

## Where text goes

Besides `Text` and `Paragraph`, text is held by a `Cell` with a value ([Table](table.md#cells)) and by the `Text` elements of a heading
([Headings](heading.md)). A `Text` is also the code block that a `Markdown` generates
([Markdown](markdown.md#code)).

## Intrinsic sizes

The narrowest width is the widest word, and the natural width, the longest paragraph on a single line
([Typography](../common/typography.md#intrinsic-sizes)).

## Breaks

A text, which is a single paragraph, breaks between lines respecting `Widows` and `Orphans` ([Breaks](../common/breaks.md)).

## Accessibility

A screen reader reads a `Text` and a `Paragraph` as a paragraph. The `Text` elements of a heading are the text of the
heading ([The PDF](../common/pdf.md#accessibility)).

## What is not supported

- Text outside a `Text`, a `Paragraph` or a `Cell`.
- Splitting words without `Hyphenate` or a soft hyphen: a word wider than the line is an error
  ([Typography](../common/typography.md#hyphenation-hyphenate)).
