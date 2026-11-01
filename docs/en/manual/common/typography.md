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
| `Bold` | `true`, `false` | `false` | Bold: the same as `Weight: 700` |
| `Weight` | 100 to 900, in steps of 100 | `400` | The weight of the letters ([below](#weights)) |
| `Italic` | `true`, `false` | `false` | Italic |
| `Line height` | number or % | `120%` | The height of each line: `15` is 15 points and `140%`, 1.4 times the size |
| `Align` | `left`, `right`, `center`, `justify` | `left` | The alignment of the lines |
| `Color` | color | `#000000` | The color of the text |
| `Line breaking` | `simple`, `balanced` | `simple` | How the text is broken into lines |
| `Hyphenate` | `true`, `false` | `false` | Whether words are split with a hyphen at the end of the line |
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
in regular, bold, italic and bold italic, and the Sans also in weights 300, 500 and 600. If a text uses a character that
its font does not have, it is an error: the engine does not look for another font or paint a little box.

## Weights

`Weight` chooses how heavy the letters are, from 100 to 900: 400 is regular, 700 bold, and the others, the intermediate
weights the family includes (300 light, 500 medium, 600 semibold...). Each weight is a file of the font: one the family
does not have is an error. `Bold: true` is another way of writing `Weight: 700`, and the two do not go together in the
same `Typography`.

```stxt
Document (dev.stxt.pdf):
    Language: en
    Title: Font weights
    Page:
        Paper: A5
        Margin: 15mm
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 11
    Section:
        Text: Marta Ferrer Soler
            Typography:
                Size: 28
                Weight: 300
        Text: Product Designer
            Typography:
                Weight: 600
        Paragraph: Eight years across a studio, a cooperative and an **in-house product**.
            Typography:
                Markup: markdown
```

A bold within the sentence (`**in-house product**`) is 700, or the weight of the text if it is already 700 or more.

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
breaks that leave the most even lines, which shows above all in justified text. By default words are not split: a word
wider than the line is an error.

Consecutive spaces and single line breaks are a single space. A blank line is an error: each text is a single paragraph,
and several are written as several `Paragraph`.

## Hyphenation

With `Hyphenate: true` the engine splits words with a hyphen at the end of the line where the rules of the language of
the box (`Language`) allow it, as a word processor does, and narrow justified columns are left without gaps. It has
rules for Spanish, Catalan and American and British English; in another language, `Hyphenate: true` is an error. The
usual thing is to declare it once, in `For element: Document`:

```
Document (dev.stxt.pdf):
    Language: en
    Title: Hyphenation
    Page:
        Paper: A5
        Margin: 40
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Serif
                Size: 10
                Align: justify
                Hyphenate: true
    Section:
        Columns:
            Count: 3
            Gap: 12
            Paragraph: The hyphenation patterns divide the long words by their syllables, and so the narrow columns are left without gaps between the words.
```

A soft hyphen (U+00AD) written inside a word says where that word may be split, with or without `Hyphenate`, and is
not seen if it is not split. The text that is copied from the PDF, and the one a screen reader reads, does not carry
the hyphens the engine added.

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
