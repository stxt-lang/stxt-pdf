# Text formatting

All these properties belong to `Typography`: they are inherited and can be put in a `Styles` rule.

## Bold, italic and more within a sentence (`Markup`)

By default text is literal: an asterisk is an asterisk. With `Markup: markdown`, the text of a `Text`, a `Paragraph` or a
`Cell` understands the Markdown marks within the line:

| Written | Result |
|---|---|
| `**bold**` | **bold** |
| `*italic*` | *italic* |
| `++underline++` | underline |
| `~~strikethrough~~` | ~~strikethrough~~ |
| `[text](https://...)` | a link, underlined ([References and links](references-and-links.md)) |
| `` `code` `` | code, in the font of `Code font` ([below](#code)) |

```
Styles:
    For element: Paragraph
        Typography:
            Markup: markdown
Paragraph: Payment is made in **30 days**, unless otherwise *agreed*.
```

The marks can be combined (`***bold and italic***`) and are not visible in the PDF. A literal asterisk is written with a
backslash before it (`\*`). Only what fits within a line is valid: a title, a list or a table go in a
[`Markdown`](../components/markdown.md). Everything else is text as it is: `2024. A good year` or `- 5% off` are shown as
they are written, without escaping anything. A particular text becomes literal again with `Markup: none`.

Bold and italic add to those of the box: inside a bold title, `**x**` is still bold, and in a light text
(`Weight: 300`), bold is weight 700 ([Typography](typography.md#weights)). The size, the color and the font are those of
the box.

## Code

With `Markup: markdown`, what goes between backticks is code and is written in the font of `Code font`, which is usually
a monospaced one. `Code font` goes in `Typography` and is inherited: it is declared once for the whole document, and it
also applies to the code of a [`Markdown`](../components/markdown.md).

```
Styles:
    For element: Document
        Typography:
            Font: IBM Plex Sans
            Size: 10
            Markup: markdown
            Code font: IBM Plex Mono
Paragraph: Run `mvn test` before publishing.
```

Code keeps the bold and the italic of what surrounds it and does not change the height of the line. Without `Code font`,
a text with code is an error.

## Capitalization (`Case`)

`Case: uppercase` paints the text in uppercase, `lowercase` in lowercase and `capitalize` capitalizes the first letter of
each word. The text is written normally and the rule transforms it, so a screen reader reads "Emisor" and not "EMISOR".
The rules for each letter are those of the language of the box.

```
Styles:
    For style: label
        Typography:
            Case: uppercase
            Letter spacing: 1
            Size: 8
Text: Customer details
    Style: label
```

## Small caps (`Small caps`)

`Small caps: true` paints lowercase letters as smaller capitals (at 80% of the size). Capitals, digits and symbols do not
change.

## Letter spacing (`Letter spacing`)

Points added between every two letters. It can be negative to tighten the text. The last letter of each line does not
get it, so a right-aligned text ends exactly at the last letter.

## Ligatures (`Ligatures`)

Some fonts have special glyphs for pairs of letters (`fi`, `fl`): **ligatures**. By default they are not used and each
letter is its own. With `Ligatures: true` those of the font are used (IBM Plex Sans and Serif have `fi`, `fl`, `ffi` and
`ffl`). They do not change what is read or what is copied, and they are not applied with letter spacing.

## Preformatted text

For code, terminal output or columns aligned with spaces. With `Preformatted: true`, the text **keeps its spaces and its
line breaks**: each written line is a line in the PDF, including empty ones. With `Preformatted: wrap`, in addition, a
line that is too long is broken between words. The usual approach is to use a monospaced font and write the text in a
`Content >>`:

```
Text:
    Typography:
        Font: IBM Plex Mono
        Size: 9
        Preformatted: true
    Content >>
        $ thermostat --status
        temperature   21.5 °C
        setpoint      22.0 °C
```

- With `Preformatted: true`, a line wider than the box is an error. It has to be shortened, or `Preformatted: wrap` used.
- `Tab size` (4 by default) is the number of columns of a tab: each tab moves to the next column that is a multiple of
  `Tab size`, as in an editor.
- Preformatted lines are not justified.
- A long preformatted block is broken across pages like any text, and counts as a single paragraph for widows and
  orphans.

## Underline and strikethrough

They are lines in the color of the text, with the position and the thickness the font specifies. They are written with
`Markup: markdown` or in a `Markdown`. There are no line styles.

More detail in the [reference](../../reference/common/text-formatting.md).
