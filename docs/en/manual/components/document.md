# `Document` and `Section`

`Document` is the root of every document. It has:

- **`Language`** and **`Title`**: required.
- **`Page`**: the paper and the margins.
- **`Styles`**: optional, with the style rules ([Styles](../common/styles.md)).
- **`Page overlay`**: optional, the headers and footers of the whole document.
- **`Section`**: one or more, with the content.

```
Document (dev.stxt.pdf):
    Language: en
    Title: Annual report
    Page:
        Paper: A4
        Margin: 56
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For element: Heading 1
            Typography:
                Size: 22
                Bold: true
    Page overlay:
        Offset: auto 0 -30 0
        Text: Page {page} of {pages}
            Typography:
                Size: 8
                Align: right
    Section:
        Page numbering: none
        Inherit page overlays: false
        Page:
            Margin: 0
        Block:
            Box:
                Height: 100%
                Padding: 300 56 0 56
                Background: #12315A
            Heading 1:
                In table of contents: false
                Text: Annual report
                Typography:
                    Color: #FFFFFF
    Section:
        Heading 1:
            Text: Results
        Paragraph: The body of the report starts on page 1.
    Section:
        Page numbering: restart
        Page:
            Orientation: landscape
        Heading 1:
            Text: Appendix
        Paragraph: The appendix, in landscape, goes back to page 1.
```

This document has three sections:

- A full-bleed cover, without a page number and without the footer.
- The body, which starts on page 1.
- A landscape appendix that counts from 1 again.

## `Title` and `Language`

`Title` is the title that the PDF viewer shows instead of the file name. `Language` is the language of the document,
a BCP 47 tag (`es`, `en-US`, `ca`). The screen reader needs it to read the text, and automatic capitalization follows its
rules. Both are required, because without them the PDF is not accessible.

## `Page`

The size of the page and its margins. The size is given in one of two ways, never both:

- **With a paper**: `Paper` (`A3`, `A4`, `A5`, `A6`, `B4`, `B5`, `Letter` or `Legal`) and, if it is landscape,
  `Orientation: landscape`.
- **With measurements**: `Width` and `Height`, in points or millimeters.

| `Paper` | Millimeters | | `Paper` | Millimeters |
|---|---|---|---|---|
| `A3` | 297 × 420 | | `B4` | 250 × 353 |
| `A4` | 210 × 297 | | `B5` | 176 × 250 |
| `A5` | 148 × 210 | | `Letter` | 215.9 × 279.4 |
| `A6` | 105 × 148 | | `Legal` | 215.9 × 355.6 |

`Letter` and `Legal` are 8.5 × 11 and 8.5 × 14 inches: 612 × 792 and 612 × 1008 points.

`Margin` is the margins of the page, from 1 to 4 measurements (top, right, bottom, left). What remains inside is the
**page area**: where the content goes. The headers and footers go in the margins, with a `Page overlay`
([positioned](frame-and-overlays.md#page-overlay)). For double-sided printing, `Mirror margins: true` swaps the left and
right margins on the even pages ([double-sided](#double-sided)).

## `Section`

A section is a part of the document that **always starts on a new page**. All the content goes inside one.

| Property | What it does |
|---|---|
| `Page` | Another paper, another orientation or other margins for this section. It inherits from the `Page` of the document what it does not declare: with only `Orientation: landscape`, it is the same paper in landscape, and with only `Margin`, the same paper with other margins (`Mirror margins` goes with `Margin`: they are inherited or declared together) |
| `Page numbering` | `continue` (the default) continues the page count, `restart` starts again at 1 and with `none` its pages have no number and do not count |
| `Page number format` | How its page numbers are written: `decimal` (the default), `lower-roman` (`i`, `ii`, `iii`) or `upper-roman` (`I`, `II`, `III`) |
| `Start` | With `Page numbering: restart`, the number of its first page (`Start: 5`) |
| `Inherit page overlays` | `true` (the default) carries the headers and footers of the document, and `false`, only its own |
| `Begin on` | `any` (the default) starts on the next page, and `odd`, on an odd page, the right-hand one when printing double-sided |
| `Id`, `Style` | To link to it (`#id`, its first page) and to give it typography with a rule (`Typography` and nothing else: an appendix in another language) |

A section can have its own `Page overlay` elements, which only appear on its pages. An empty section is an error: it would
have no page.

## Page numbering

`{page}` writes the page number and `{pages}` the last number of the current count, which starts again in each
section with `Page numbering: restart`. A page of a `none` section has no number: if a text with `{page}` falls on it
(a document footer on the cover), it is an error, so that section declares `Inherit page overlays: false`.

Tables of contents and references (`{page of: id}`) give the same number as the footer of the page: the printed one, also
in roman numerals if its section has `Page number format: lower-roman`. For a preface in roman numerals, its section
declares `Page number format` and the section of the body, `Page numbering: restart`. With `Start`, the numbering starts
at another number.

## Double-sided

In a book, each chapter starts on a right-hand page, which is odd. With `Begin on: odd`, if the previous section ends on
an odd page, the engine adds a **blank page** after it: without content, without header or footer. It counts for the
page number, as in a book, even though it is not printed: this way the odd pages stay on the right. The first section
already starts on page 1, so in it this is an error.

The margin at the spine, the inner one, is usually larger than the outer one, and it changes sides with the page. With
`Mirror margins: true` in `Page`, `Margin` gives the margins of the odd pages, the right-hand ones, and on the even ones
the left and the right are swapped. With the left margin equal to the right one it would do nothing, and it is an error.
Headers and footers do not change sides: a page number on the outside is two `Page overlay` elements, one with
`Pages: odd` on the right and another with `Pages: even` on the left.

```
Document (dev.stxt.pdf):
    Language: en
    Title: Two chapters
    Page:
        Paper: A5
        Margin: 40 30 50 60
        Mirror margins: true
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Serif
                Size: 11
    Page overlay:
        Offset: auto 0 -30 0
        Text: {page}
        Typography:
            Align: center
    Section:
        Heading 1:
            Text: Chapter 1
        Paragraph: The first chapter takes up a single page.
    Section:
        Begin on: odd
        Heading 1:
            Text: Chapter 2
        Paragraph: The second one starts on page 3: page 2 is left blank.
```

## Recipes

- **Full-bleed cover:** a section with a `Page` of `Margin: 0`, `Page numbering: none` and `Inherit page overlays: false`,
  and a block of `Height: 100%` with its background.
- **A landscape page in the middle:** a section with `Orientation: landscape`.
- **A title that does not go to the table of contents:** `In table of contents: false` on the heading ([Headings](heading.md)).

## What is not supported

- A page background of its own: it is painted with a block the size of the page or with a `Page overlay` behind
  everything ([Layers and rotation](../common/layers-and-rotation.md)).

More detail in the [reference](../../reference/components/document.md).
