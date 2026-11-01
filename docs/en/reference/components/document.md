# `Document`

The root of every document: `Document`, with the namespace `dev.stxt.pdf`. It has exactly one `Page`, one `Title` and one
`Language`, an optional `Styles` ([Styles](../common/styles.md)), the `Page overlay` elements of the whole document and one or more `Section`,
which hold the content. A document has no loose content and no `Typography`: the base typography goes in the rule
`For element: Document`.

```
Document (dev.stxt.pdf):
    Language: en
    Title: Report
    Page:
        Paper: A4
        Margin: 56
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
    Page overlay:
        Id: footer
        Offset: auto 0 -30 0
        Text: Page {page} of {pages}
    Section:
        Id: front-cover
        Page numbering: none
        Inherit page overlays: false
        Page:
            Margin: 0
        Block:
            Box:
                Height: 100%
                Background: #12315A
    Section:
        Id: body
        Paragraph: The body of the report starts on page 1.
    Section:
        Id: annex
        Page numbering: restart
        Page:
            Orientation: landscape
        Paragraph: The annex, in landscape, goes back to page 1.
```

> **Decision:** the document is always a sequence of pages, so the page area always has a definite height.
> *Rejected alternative:* a continuous mode for the screen.

## `Page`

Required, exactly one, child of `Document`: it is the page format of the sections that do not declare their own. A `Section`
can declare its own, which inherits from the document's by groups ([below](#the-page-of-a-section)).

| Property | Values | Default | Notes |
|---|---|---|---|
| `Paper` | `A3`, `A4`, `A5`, `A6`, `B4`, `B5`, `Letter`, `Legal` | none | Named paper size, in portrait (table below). |
| `Orientation` | `portrait`, `landscape` | `portrait` | Only with `Paper`: `landscape` swaps the width and the height. |
| `Width` | number > 0 | none | Width of the page, without `Paper`. |
| `Height` | number > 0 | none | Height of the page, without `Paper`. |
| `Margin` | 1 to 4 numbers | `0` | Page margins. Measurements only, without percentages. |

The size of the page is declared **in one way only**: a `Paper` (with its `Orientation`, if it is landscape) or a `Width` and
a `Height`. Declaring `Paper` with `Width` or `Height`, `Orientation` without `Paper`, only one of `Width` and `Height`, or neither
of the two ways, is `PAGE_SIZE`. So are margins that leave no page area. The named sizes:

| `Paper` | Width × height | In points | | `Paper` | Width × height | In points |
|---|---|---|---|---|---|---|
| `A3` | 297 × 420 mm | 841.89 × 1190.55 | | `B4` | 250 × 353 mm | 708.66 × 1000.63 |
| `A4` | 210 × 297 mm | 595.28 × 841.89 | | `B5` | 176 × 250 mm | 498.9 × 708.66 |
| `A5` | 148 × 210 mm | 419.53 × 595.28 | | `Letter` | 8.5 × 11 in | 612 × 792 |
| `A6` | 105 × 148 mm | 297.64 × 419.53 | | `Legal` | 8.5 × 14 in | 612 × 1008 |

```
Page:
    Paper: A4
    Orientation: landscape
    Margin: 40
```

is a page of 297 × 210 mm, the same as `Width: 297mm` and `Height: 210mm`.

The **page area** is the rectangle of the page minus its margins. It is the reference container of the top-level elements
of each section and of the `Page overlay` elements, and its height is always definite.

> **Decision:** the ISO 216 sizes are exact in millimeters (A4 = 210 × 297 mm, 595.28 × 841.89 pt), as in word
> processors and PDF viewers: a box with `Width: 210mm` measures the same as the sheet. Letter and Legal are exact in
> inches. *Rejected alternative:* rounding them to whole points (A4 = 595 × 842), which made 210 mm not fit in an A4.

## `Title` and `Language`

`Document` requires exactly one `Title`, which is a non-empty text, and exactly one `Language`, which is a BCP 47
tag (`es`, `en-US` or `ca`). A value that does not meet this is `VALUE_NOT_ALLOWED`, and if either of the two is missing,
`TOO_FEW_CHILDREN`. They do not
affect the layout, except that `Language` gives the capitalization rules of `Case`
([Text formatting](../common/text-formatting.md#capitalization-case)). The title is the one the viewer shows, and the language,
the one a screen reader uses. Both are required for the PDF to be accessible ([The PDF](../common/pdf.md)).

## `Section`

A document is made up of **sections**: parts independent of each other that are laid out one after another. All the
content is in one of them.

| Property | Values | Default | Notes |
|---|---|---|---|
| `Id` | text | none | Unique in the document. It is the target of `Link: #id` and of `{page of: id}` (its first page). |
| `Style` | names separated by spaces | none | The `For style` rules with those names apply to it. Like `For id` and `For element: Section`, they can only give `Typography`, which all of its content inherits (`STYLE_NOT_ALLOWED` with `Box` or `Break`). |
| `Page` | like the document's | the document's | See [below](#the-page-of-a-section). |
| `Page numbering` | `continue`, `restart`, `none` | `continue` | How it numbers its pages ([numbering](#page-numbering)). |
| `Page number format` | `decimal`, `lower-roman`, `upper-roman` | `decimal` | How its page numbers are written: `1`, `i`, `I`. With `none` it is `PAGE_NUMBER_FORMAT_WITHOUT_NUMBERS`. |
| `Start` | integer ≥ 1 | `1` | With `restart`, the number of its first page. Without `restart` it is `START_WITHOUT_RESTART`. |
| `Inherit page overlays` | boolean | `true` | Whether it carries the `Page overlay` elements of the document. With `false`, only its own appear. |
| `Begin on` | `any`, `odd` | `any` | On which physical page it begins ([double-sided](#beginning-on-an-odd-page)). In the first section, `odd` is `BEGIN_ON_FIRST_SECTION`. |

Its content is in-flow elements, `Frame`, `Overlay` and `Page overlay`.

- **A section starts a page.** Always, even if the previous one ends halfway down a page, and the next one starts another. The
  content of one section is not mixed with that of another.
- **Each section is the root of its layout.** Its box is the page area of each of its pages (the reference container
  of its top-level elements): its margins and its height come from its `Page`, and each page carries its own. A
  `Section` is not a box with a `Box` of its own: it has no `Margin`, `Padding` or background, and it isolates margins: it
  does not merge with anything.
- **A section without content is `SECTION_EMPTY`**: without elements, or with only `Page overlay` elements, which take up no space, it would have
  no page. An `Overlay` does count: a cover made only of positioned boxes is valid.
- **Typography per section.** The rules that reach a section can only give `Typography` (`Language` included: an
  annex in English), which all of its content inherits.
- **A single document.** The heading hierarchy, the tables of contents (`Table of contents`, which list the headings of all the
  sections, in order), `Styles`, the `Id` values (unique in the whole document) and the convergence of the references belong to the
  document and not to the section.

> **Decision:** there is no implicit section for loose content: the separation is always in sight.

> **Decision:** a section without content is an error and not a blank page that nobody wrote.

### The `Page` of a section

It inherits from the `Page` of the document **by groups**: the size (`Paper` with its `Orientation`, or `Width` and `Height`) is inherited
whole or declared whole, and `Margin` is inherited or declared. A section that only declares `Orientation` rotates the `Paper`
of the document (`Orientation: landscape` on an A4 document is a landscape A4). If the document declares `Width` and
`Height`, there is no paper to rotate and it is `PAGE_SIZE`. Declaring `Paper` with `Width` or `Height` is still `PAGE_SIZE`.

> **Decision:** inheritance by groups, so that `Orientation: landscape` on its own gives the document's paper in landscape.
> *Rejected alternative:* a complete `Page` in each section.

### `Page overlay` of the document and of the section

A `Page overlay` of `Document` appears on the pages of all the sections except those that declare `Inherit page overlays: false`, and one
of a `Section`, only on its own. `Pages` counts within the scope where it is declared
([positioned elements](frame-and-overlays.md#pages)). They are placed in the page area of each page, which is that of its section.
Those of the document are painted before those of the section. `Inherit page overlays: false` in a document without a `Page overlay`, which has no
effect, is not an error: a template can add them later.

A `Page overlay` of the document is not in any flow: its static position is the corner of the page area.

> **Decision:** `Pages` counts within the scope where it is declared. *Rejected alternative:* always counting the physical pages
> of the document.

### Page numbering

There is one page counter per document. Each section says what it does with it:

- `continue` (default): its pages keep being numbered.
- `restart`: its first page is page 1 again.
- `none`: its pages have no number and do not advance the counter.

`{page}` is the value of the counter, and `{pages}`, the last value of its **page sequence**, the one that starts at each `restart`:
with a `none` cover and 6 body pages, the first inner page is "1 of 6" even though it is the second of the document.

**What is printed rules.** `{page of: id}`, the entries of a table of contents and the footer say the same number: the counter's. A
reference to something that is on a page without a number is `REFERENCE_NOT_PLACEABLE`, and a text with `{page}` or `{pages}` that
falls on a page without a number (the `Page overlay` of the document on a cover that inherits it) is `PAGE_NUMBER_UNAVAILABLE`: it is never
left blank. The bookmarks, the links and `Pages` follow the physical page. A heading of a `none` section cannot
be in a table of contents (`In table of contents: false`).

Since the counter does not advance in a `none` section, a `continue` section after a cover starts at 1. With
`Page numbering: restart`, `Start` gives the number of the first page of the section (`Start: 5`: a document split
across several files). The viewer also shows the printed number ([The PDF](../common/pdf.md#pages)).

**How it is written.** `Page number format` writes the page numbers of the section in digits (`decimal`), in lowercase Roman
numerals (`lower-roman`: `i`, `ii`, `iv`) or in uppercase (`upper-roman`). What is printed still rules: `{page}` and
`{pages}` are written in the style of the page where they appear, and `{page of: id}` and the entries of a table of contents, in that of
the page they refer to. A preface in Roman numerals and a body that starts again at 1:

```
Section:
    Id: preface
    Page number format: lower-roman
    Paragraph: The preface: its pages are i, ii, iii...
Section:
    Page numbering: restart
    Paragraph: The body starts on page 1.
```

A `Page number format` in a `none` section, which has no numbers, is `PAGE_NUMBER_FORMAT_WITHOUT_NUMBERS`. A `Start` without `restart`,
which would have no effect because the section continues the count of the previous one, is `START_WITHOUT_RESTART`.

> **Decision:** the style belongs to the section and not to the counter: a `continue` section in Roman numerals continues the count of the
> previous one and writes it in Roman numerals. Only digits and Roman numerals, which are the ones of book pages. *Rejected
> alternative:* letters (`a`, `b`), which are not used to number pages.

> **Decision:** what is printed rules, so that the table of contents and the footer match. What has no number is an error and never
> an empty gap. *Rejected alternative:* leaving the gap empty.

### Beginning on an odd page

For double-sided printing, `Begin on: odd` makes the section begin on an odd physical page, the right-hand one: if
the previous one ends on an odd page, it ends with a **blank page**. The physical page is the position in the document (the
first is 1) and not the printed number. With `any` (default), the section begins on the next page.

The blank page is **one more page of the previous section**:

- It has its `Page`, its `Page number format` and its numbering: if the section is numbered, it advances the counter and counts for `{pages}`
  like any other, even though it is not printed. If it is `none`, it has no number.
- It has no content and no `Page overlay`, neither those of the section nor those of the document.
- It does not count for the `Pages` of the `Page overlay` elements of its section (`Pages: last` is its last page with content). For those of the
  document, which count the physical pages, it does count, even though they do not appear on it.

```
Section:
    Id: chapter-2
    Begin on: odd
    Heading 1:
        Text: Chapter 2
```

The first section already begins on page 1: `Begin on: odd` in it would have no effect and is `BEGIN_ON_FIRST_SECTION`.

> **Decision:** the blank page belongs to the previous section and counts for the number, as in a book: that way the odd
> printed numbers fall on the right. *Rejected alternative:* a page without a number that does not count, which shifts the
> parity of the numbers.

> **Decision:** the parity is that of the physical page, which is what decides the side when printing. Only `odd`: beginning on an
> even page is hardly used and would force a blank page before the first section. *Rejected alternative:* the
> parity of the printed number, which does not exist in a `none` section and does not always match the side.

## What is not supported

- A page background of the section's own (a full-bleed cover paints a block the size of the page) and a landscape
  page that is not a whole section.
- Named pages or page rules: what changes from one part to another is a section.
