# Typography: `Typography`

The `Typography` group controls the text. It is the only one that **is inherited**: what a box declares applies to all
the text it contains, except what a box inside changes. That is why the base is declared once for the whole document and
each element changes only its own:

```
Styles:
    For element: Document
        Typography:
            Font: IBM Plex Serif
            Size: 10.5
            Line height: 15
            Align: justify
    For element: Heading 1
        Typography:
            Font: IBM Plex Sans
            Size: 20
            Bold: true
            Align: left
```

| Property | Values | Default | What it does |
|---|---|---|---|
| `Font` | family name | none | The font. **Required** in all text, own or inherited |
| `Code font` | family name | none | The font of code ([Text formatting](text-formatting.md#code)). **Required** if there is code |
| `Size` | number > 0 | none | The size of the letters. **Required** in all text |
| `Bold` | `true`, `false` | `false` | Bold |
| `Italic` | `true`, `false` | `false` | Italic |
| `Line height` | number or % | `120%` | The height of each line: `15` is 15 points and `140%`, 1.4 times the size |
| `Align` | `left`, `right`, `center`, `justify` | `left` | The alignment of the lines |
| `Color` | color | `#000000` | The color of the text |
| `Line breaking` | `simple`, `balanced` | `simple` | How the text is broken into lines |
| `Widows`, `Orphans` | integer ≥ 1 | `2` | Lines of a paragraph that are not left alone when the page changes |
| `Language` | BCP 47 tag: `es`, `en-GB`, `ca` | that of the document | The language of the text, if it is not that of the document |
| `Numbered` | `true`, `false` | `false` | Whether headings carry a number ([Headings](../components/heading.md)) |
| `Number format`, `Number label` | | | How that number is written and what goes before the title ([Headings](../components/heading.md)) |

The rest (`Markup`, `Case`, `Small caps`, `Letter spacing`, `Ligatures`, `Preformatted` and `Tab size`) is in
[Text formatting](text-formatting.md).

## Font and size

There is no default font: without `Font` and `Size`, a text is an error. The font is named by its family, as its file
declares it (`IBM Plex Sans`, with capitals and spaces), and it has to be in the fonts folder
([Resources](resources.md)). The repository includes **IBM Plex Sans**, **IBM Plex Serif** and **IBM Plex Mono**, each
in regular, bold, italic and bold italic. If a text uses a character that its font does not have, it is an error: the
engine does not look for another font or paint a little box.

## Line height

Each line measures exactly `Line height`, and a text measures its lines times `Line height`. A number is **points**: with
`Size: 10`, `Line height: 14` leaves 4 points of air. A percentage is relative to the size of the letters (`120%`, the
default value, is 12 points with `Size: 10`). The letters are centered in the height of the line.

## Alignment

`left`, `right` and `center` align each line. `justify` stretches the spaces so that each line reaches both sides,
except the last one of each paragraph, those with a single word and preformatted lines.

## Line breaking

The text is broken into lines of the width of its box, always between words. With `Line breaking: simple`, each line is
filled as much as it can be. With `Line breaking: balanced`, the engine looks at the whole paragraph and chooses the
breaks that leave the most even lines, which shows above all in justified text. Words are not hyphenated: a word wider
than the line is an error.

Consecutive spaces and single line breaks are a single space. A blank line is an error: each text is a single paragraph,
and several are written as several `Paragraph`.

## Widows and orphans

When a paragraph changes page, `Orphans` is the minimum number of lines that stay at the end of the page and `Widows`,
the minimum that move to the next one. With both at 2 (the usual), a paragraph never leaves a single line on its own. If
there is no other way to break, the engine relaxes the rule rather than leave a page half empty
([Pages and breaks](pages-and-breaks.md)).

## Language

The language of the document is its `Language`. A part in another language (a quote in French, an appendix in Catalan)
declares its own in `Typography`. The screen reader changes voice there, and automatic capitals follow the rules of that
language.

```
Paragraph: « Je pense, donc je suis », écrit Descartes.
    Typography:
        Language: fr
```

The language belongs to the whole box, not to a word within a line.

More detail in the [reference](../../reference/common/typography.md).
