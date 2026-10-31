# `Markdown`

A `Markdown` element is a block whose content is written in Markdown. The engine translates it into the usual elements
(headings, paragraphs, lists, tables, images), which are styled with the same `Styles` rules as those written in STXT. It
is used for long formatted text.

```
Document (dev.stxt.pdf):
    Language: en
    Title: Terms
    Page:
        Paper: A4
        Margin: 50
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For element: Heading 1
            Typography:
                Size: 16
                Bold: true
            Box:
                Margin: 0 0 10 0
        For element: Paragraph
            Box:
                Margin: 0 0 8 0
        For element: List
            Box:
                Padding: 0 0 0 18
        For element: Code block
            Box:
                Padding: 8
                Background: #F1EFE8
    Section:
        Markdown:
            Typography:
                Code font: IBM Plex Mono
            Bullet markers: bullet dash
            Number markers: decimal
            Table columns: auto
            Content >>
                # Payment terms

                Payment is due within **30 days** of the invoice date, by
                bank transfer to the account `ES12 3456 7890`.

                1. Invoice sent.
                2. Payment:
                   - by bank transfer;
                   - or in cash, at the workshop.

                | Item   |   Amount |
                |--------|---------:|
                | Chairs | 1,740.00 |
                | Table  |   890.00 |

                ~~~
                Reference: 2026-014
                ~~~
```

## Properties

| Property | What it does |
|---|---|
| `Content` | The Markdown, in a `>>` block. Required |
| `Bullet markers` | The marker of bulleted lists, one per level: `bullet dash` uses `•` on the first level and `–` on the following ones. Required if there are bulleted lists |
| `Number markers` | The same for numbered lists: `decimal`, `lower-alpha`, `upper-alpha`, `lower-roman`, `upper-roman`. Required if there are numbered lists |
| `Table columns` | The columns of the tables: `equal` (all the same) or `auto` (according to their content). Required if there are tables |

It also accepts `Id`, `Style`, `Box`, `Typography` (which everything inside inherits) and `Break`. The code font is the
`Code font` of `Typography` ([Text formatting](../common/text-formatting.md#code)): it is declared in the `Typography`
of the `Markdown` or, once for the whole document, in `For element: Document`, and it is required if there is code.

## What is accepted

| Markdown | Result |
|---|---|
| `#` to `######` | `Heading 1` to `Heading 6`. With `{#id}` at the end (`## Ventas {#ventas}`), it has that `Id`, to link to it or cite it |
| A paragraph | `Paragraph` |
| `-`, `*`, `+` and `1.` | `List`, bulleted or numbered |
| A table | `Table`, with the first row as the header. The alignment of each column comes from the separator row (`:--`, `--:`). A row with fewer cells than the header gets empty ones added, and one with more is an error |
| `**bold**`, `*italic*`, `++underline++`, `~~strikethrough~~` | Inline formatting |
| `[text](target)` | A link, underlined ([References and links](../common/references-and-links.md)) |
| `` `code` `` | Inline code, with `Code font` |
| A code block (between ```` ``` ```` or `~~~`, or indented four spaces) | A preformatted block with `Code font`, styled with `For element: Code block` |
| `![alt text](path)`, alone in its paragraph | An `Image` |

Anything else is an error, so that nothing is lost silently. For example: quotes, HTML, horizontal rules, forced line breaks,
images inside a paragraph, links with a title or the language tag of a code block (there is no highlighting).

## Style

What a `Markdown` generates has no `Style`, and no `Id` except headings with `{#id}`: it is styled with the `For element`
rules of `Styles` (`For element: Heading 1`, `For element: Paragraph`, `For element: List`, `For element: Code block`),
which apply to the whole document, and with the `Typography` of the `Markdown`. Nothing has a default style: without
`For element: List` with a left `Padding`, the lists have no room for the marker.

## Things to keep in mind

- The headings of a `Markdown` count in the levels of the document: after a `Heading 1` written in STXT, a Markdown can
  start with `##`. They are numbered and appear in the table of contents like the others.
- What is generated has no `Id`, except a heading that writes it at the end, `## Ventas {#ventas}`: this way it is linked
  (`[ver ventas](#ventas)`) or cited (`{page of: ventas}`, `{number of: ventas}`).
- Errors give the line of the document where the failing Markdown block starts.
- For bold or italic in a single paragraph, a `Markdown` is not needed: `Markup: markdown` is enough
  ([Text formatting](../common/text-formatting.md)).

More detail in the [reference](../../reference/components/markdown.md).
