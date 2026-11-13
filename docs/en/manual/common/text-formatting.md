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
| `[text]{price}` | the text with the color, the font and the rest of the `For style: price` rule ([below](#parts-in-another-color-or-font)) |
| `![alt text](path)` | a small image within the sentence, as tall as the letters ([below](#images-in-the-sentence)) |

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
the box, except in a [styled part](#parts-in-another-color-or-font).

## Parts in another color or font

With `Markup: markdown`, a text in brackets followed by the name of a style in braces, `[€12.50]{price}`, takes what the
`For style: price` rule of `Styles` declares: the color, the font, the weight, the italic, the capitalization, the small
caps or the letter spacing. The size and the line height are still those of the box, so the line does not change height.

```
Document (dev.stxt.pdf):
    Language: en
    Title: Styled parts
    Page:
        Paper: A5
        Margin: 15mm
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 11
                Markup: markdown
        For style: price
            Typography:
                Color: #B3261E
                Bold: true
        For style: brand
            Typography:
                Font: IBM Plex Serif
                Italic: true
    Section:
        Paragraph: A subscription to [The Gazette]{brand} costs [€12.50]{price} a month, VAT included.
```

- The rule is a `For style` rule like the others: the same one can style a box (`Style: price`) and a part of a text. In
  a part only `Font`, `Code font`, `Bold`, `Weight`, `Italic`, `Color`, `Case`, `Small caps`, `Letter spacing` and
  `Position` are valid. If the rule declares something else, such as `Size` or a `Box`, and it is used in a part, it is an error
  (`STYLE_NOT_ALLOWED_IN_RUN`).
- The marks add to the style: `[**€12.50**]{price}` and `**[€12.50]{price}**` are bold even if the rule says
  `Weight: 300`. A link can have a styled part (`[[see price]{price}](https://...)`) and a styled part can have a link,
  but a styled part cannot go inside another.
- A name without its rule is an error (`STYLE_WITHOUT_RULE`). Brackets that are not followed by a name in braces are
  text, and `\[` writes them as they are.

## Superscripts and subscripts (`Position`)

A styled part whose rule declares `Position: superscript` or `subscript` is painted smaller (at 70 % of the size) and
above or below the baseline: ordinals, units, formulas and trademarks.

```
Document (dev.stxt.pdf):
    Language: en
    Title: Superscripts and subscripts
    Page:
        Paper: A5
        Margin: 15mm
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 11
                Markup: markdown
        For style: sup
            Typography:
                Position: superscript
        For style: sub
            Typography:
                Position: subscript
    Section:
        Paragraph: The 1[st]{sup} floor, of 85 m[2]{sup}, emits 12 kg of CO[2]{sub} a year.
```

- The line does not change its height, and what is copied from the PDF is the text as written (`85 m2`).
- The rule can also carry a color or a font, and the marks add to it: `[**2**]{sub}` is a subscript in bold.
- `Position` is only valid in a part of a text. In a box (in its `Typography`, or with `Style: sup`) it is an error
  (`POSITION_OUTSIDE_RUN`).
- The number of a [footnote](../components/footnote.md) is already a superscript, with no rule.

## Images in the sentence

An icon between the words, or a small logo in a heading, is written as in Markdown, with the path of the image in
`images/` of the resources folder ([Resources](resources.md)):

```
Paragraph: Included ![Yes](icons/check.svg) in every plan.
	Typography:
		Markup: markdown
```

The image is as tall as the letters and aligned with them; its width comes from its proportion. It behaves like one
more word: if it does not fit, it goes to the next line. A PNG, a JPEG or an SVG works. The text between the brackets
is what a screen reader reads; with empty brackets, `![](path)`, the image is decorative and is not read. Inside a
link, `[![text](path)](target)`, it can be clicked. An image with a size of its own, or alone, is an
[`Image`](../components/image.md).

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
a text with code is an error. Inside a [styled part](#parts-in-another-color-or-font) the `Code font` of its rule
applies, if it declares one.

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

Some fonts have special glyphs for pairs of letters (`fi`, `fl`): **ligatures**. Those of the font are used (IBM
Plex Sans has `fi`, and IBM Plex Serif, `fi` and `fl`); with `Ligatures: false`, each letter is its own. They do not change what is read or what is copied, and they are not applied with letter spacing.

## Preformatted text

For code, terminal output or columns aligned with spaces. With `Preformatted: keep`, the text **keeps its spaces and its
line breaks**: each written line is a line in the PDF, including empty ones. With `Preformatted: wrap`, in addition, a
line that is too long is broken between words. The usual approach is to use a monospaced font and write the text in a
`Content >>`:

```
Text:
    Typography:
        Font: IBM Plex Mono
        Size: 9
        Preformatted: keep
    Content >>
        $ thermostat --status
        temperature   21.5 °C
        setpoint      22.0 °C
```

- With `Preformatted: keep`, a line wider than the box is an error. It has to be shortened, or `Preformatted: wrap` used.
- `Tab size` (4 by default) is the number of columns of a tab: each tab moves to the next column that is a multiple of
  `Tab size`, as in an editor.
- Preformatted lines are not justified.
- A long preformatted block is broken across pages like any text, and counts as a single paragraph for widows and
  orphans.

## Underline and strikethrough

They are lines in the color of the text, with the position and the thickness the font specifies. They are written with
`Markup: markdown` or in a `Markdown`. There are no line styles.

More detail in the [reference](../../reference/common/text-formatting.md).
