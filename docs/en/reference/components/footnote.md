# `Footnote`

A `Footnote` is a **footnote**: the content of a note that the engine places at the bottom of the page where the text
first calls it, under a rule, numbered in the order of the calls. The call is written in the text as `[^id]`, with
`Markup: markdown` or inside a `Markdown` ([Text formatting](../common/text-formatting.md#inline-formatting-markup)), and
is painted as a superscript; the note starts with the same number. A `Footnote` accepts `Id` (required: it is the name
the call uses), `Style`, `Box` and `Typography`, and its content is `Paragraph`, `Text`, `Markdown`, `List`, `Table` and
`Block`. It is written wherever an in-flow element goes (in the `Section`, in a `Block`, in a `List item`, in a `Cell`,
in a `Columns` or in a `Frame`), before or after its call, and **takes no room there**: only its `Id` counts.

```
Paragraph: The deadline is six weeks[^deadline] from the signature.
    Typography:
        Markup: markdown
Footnote:
    Id: deadline
    Paragraph: Deadlines are counted in calendar weeks.
```

| Property | Values | Default | Notes |
|---|---|---|---|
| `Id` | text | required | The name of the note, that of its call `[^id]`. Unique in the document, like every `Id`: it is also the target of `Link: #id` and of `{page of: id}`. |
| `Style` | names separated by spaces | none | The `For style` rules with those names apply to it. |
| `Box`, `Typography` | the groups | none | The box and the typography of the note, over what it inherits from the area ([below](#the-footnote-area-footnotes)). |

It does not accept `Break`: it is not broken where it is written, but at the bottom of the page ([placement](#placement)).

## The call

`[^id]` is the call to the note with that `Id`. It is only interpreted where Markdown is interpreted: in a text with
`Markup: markdown` (a `Text`, a `Paragraph`, a `Cell`) or in a `Markdown`. In a literal text, `[^id]` is text.

- The call is a **superscript run** with the number of the note: it is measured and painted at 70% of the size, raised
  a third of the size, with the font, the color and the bold or italic of the text around it. It does not change the
  height of the line. It is attached to the previous word, as it is written: `weeks[^deadline]`.
- **The notes are numbered in the order of their first call**, with the format of the section
  ([numbering](#numbering)). The number is text: it measures what is painted and a screen reader reads it.
- **A note may be called several times.** The first call gives it its number, and the later ones repeat it: the cells
  of a table that refer to the same note all carry the same asterisk. The note is one, and goes at the bottom of the
  page of its first call ([placement](#placement)).
- A call without a `Footnote` with that `Id` in its section is `FOOTNOTE_UNKNOWN`, and so is a second call from another
  section; a `Footnote` nobody calls is `FOOTNOTE_NOT_CALLED`.
- **Where a call does not go** (`FOOTNOTE_CALL_NOT_ALLOWED`): in a `Page overlay`, which is repeated on every page; in a
  heading, whose text the tables of contents and the bookmarks copy; and inside another note.

> **Decision:** the call is the footnote syntax of Markdown, `[^id]`, and the note is an STXT element with that `Id`,
> not a definition `[^id]: ...` inside the Markdown: this way the note has boxes, styles and several paragraphs, like any
> content, and is written from a standalone `Paragraph` too. *Rejected alternative:* a marker of its own
> (`{note: id}`) in any text, which would not be a run and could not be a superscript.

> **Decision:** a note called several times is placed once, on the page of its first call, and the other calls repeat
> its number even if they fall on another page, like `\footnotemark` in LaTeX. The usual case is a table or a paragraph,
> on one page. *Rejected alternative:* repeating the note at the bottom of every page that calls it, which duplicates the
> note in the tagged PDF, where a `Note` has a unique identifier. Up to 0.16.0 a second call was an error
> (`FOOTNOTE_CALL_DUPLICATE`).

## The note

The note is a block: it stacks its content in block flow ([Block](block.md)). **It starts with a `Paragraph`, a `Text`
or a `Markdown` whose first block is a paragraph**, because that text gets the number: the engine writes before it the
number as a superscript and a space, like the call (`¹ Deadlines are...`). Something else in first place is
`FOOTNOTE_CONTENT`. What it accepts may follow: paragraphs, lists, tables and blocks; its `Markdown` cannot generate
headings, images or code blocks (`FOOTNOTE_CONTENT`).

The note inherits the typography of the **footnote area** of its section, not that of the box where it is written: a
note written in a right-aligned cell does not come out right-aligned. `For element: Footnote` styles all the notes (a
bottom `Margin` separates them), and `Style` or `Id`, one in particular. The notes of a page are stacked in the area
like the children of a block: their margins and those that come out of them (the top margin of their first paragraph,
for example) merge between notes ([Margin merging](block.md#margin-merging)).

A `Footnote` cannot be inside a `Page overlay`, which is repeated on every page, nor inside another `Footnote`
(`FOOTNOTE_NOT_ALLOWED`). The note goes in the **section of its call**: a call whose note is in another section is
`FOOTNOTE_UNKNOWN`.

> **Decision:** the number goes as a superscript at the start of the first text, without a hanging indent, as in Word
> and LaTeX: the following lines return to the margin. *Rejected alternative:* the number hanging on the left, like the
> marker of a `List item`, which asks for a `Padding` in the area and one more rule.

> **Decision:** the note inherits from the area and not from where it is written: what surrounds it in the document has
> nothing to do with the bottom of the page, where it is read.

## The footnote area (`Footnote area`)

The notes of a page go in the **footnote area**, a generated box at the bottom of the page area, with the width of the
page area, which the engine places on every page that has notes. The `For element: Footnote area` rules style it, and it
is what the notes inherit:

- its `Typography` is the typography of all the notes (`Size: 8`, for example);
- its `Box` gives the **rule** (the top `Border`, with its `Border color` and its `Border style`), the separation from the text (the top `Margin`) and the space down to
  the first note (the top `Padding`), plus the background, the other borders and the side margins, which narrow it as they narrow a block.

By default the area has a top margin of 8 pt, a rule of 0.5 pt in the color of the text across the whole width and a
top padding of 4 pt. `Height`, `Min height`, `Max height`, `Aspect ratio`, `Rotate` and `Overflow` have no effect on it
and are an error (`FOOTNOTES_PROPERTY_WITHOUT_EFFECT`): it measures what the notes of each page measure.

```
Styles:
    For element: Footnote area
        Typography:
            Size: 8
            Line height: 11
        Box:
            Margin: 12 0 0 0
            Border: 0.75 0 0 0
            Border color: #999999
            Padding: 4 0 0 0
    For element: Footnote
        Box:
            Margin: 0 0 3 0
```

> **Decision:** the rule is the top border of the area, across the whole width, and not a separate property
> (`Footnote rule`): with `Border`, `Border color`, `Margin` and `Padding` it is drawn and spaced like any box.
> *Rejected alternative:* a short rule, a third of the width, like that of Word and LaTeX, which asked for a property
> with its measurement.

## Placement

The notes take room from the text of the page, and which notes there are depends on how much text fits. The rule is
this:

- **A note goes on the page of its first call.** The later calls to that note, on the same page or on another, bring
  nothing to the bottom.
- **A line enters the page only if its note fits under it**, whole or at least its beginning. If it does not fit, the
  line moves to the next page with its note, and with it what follows. The line decides: a table or a `Block` with
  `Keep together` that is not broken moves whole with its note.
- **A note may be broken** between the bottom of its page and that of the next one, with the widows and orphans and
  `Keep` rules of its content ([Breaks](../common/breaks.md)); if the break violates any, the line moves to the next
  page. The rest of a note goes at the top of the area of the next page, before the notes of that page. A note is not
  broken if the text of its section ends on its page: it moves with its call.
- The area takes **the minimum**: the text fills the page as far as the notes of its calls fit under it, and the room
  left over, if any, stays between the text and the rule.
- A note that does not fit on a page with the text that calls it, not even broken, is `FOOTNOTE_TOO_TALL`.
- In a **`Columns`**, the notes go at the bottom of the page, across the whole width, not at the bottom of the column.
- The text of the notes is painted with the `Page overlay` boxes of the page, and `{page}` and `{pages}` in a note are
  worth what they are on the page.

> **Decision:** the notes go at the bottom of the page also inside a `Columns`, as in magazines: it is simpler and
> keeps the columns even. *Rejected alternative:* the bottom of each column.

> **Decision:** a note is not broken if the text of the section ends on its page: the whole note moves with its call to
> the next page, instead of opening a page with only the rest of the note.

## Numbering

The notes are numbered per document, in the order of their first call, with two properties of `Section`:

| Property | Values | Default | Notes |
|---|---|---|---|
| `Footnote numbering` | `continue`, `restart` | `continue` | `continue` follows the counter of the document and `restart` starts again at 1 in this section. |
| `Footnote number format` | `decimal`, `lower-alpha`, `upper-alpha`, `lower-roman`, `upper-roman`, `symbols` | `decimal` | How the numbers of the notes of the section are written, in the call and in the note: `1`, `a`, `A`, `i`, `I`, `*`. |

With `symbols`, the notes carry a symbol instead of a number: `*`, `†`, `‡` and `§`, in that order, and then doubled
(`**`, `††`, `‡‡`, `§§`), tripled, and so on. It is the note of a commercial document, with one or two per section:

```
Section:
    Footnote number format: symbols
    Paragraph: Monthly fee: €49[^vat]. Sign-up: €20[^vat].        # "€49*" and "€20*"
        Typography:
            Markup: markdown
    Footnote:
        Id: vat
        Paragraph: VAT not included.                              # "* VAT not included."
```

The symbol is a superscript, like the number, and the font has to have it (`GLYPH_MISSING` otherwise). With
`Footnote numbering: restart`, each section starts again with the asterisk.

> **Decision:** the symbols are four (`*`, `†`, `‡`, `§`) and then they are doubled, as in Word, and follow the counter
> of the section. *Rejected alternative:* restarting them on every page (`Footnote numbering: page`), which is how they
> are used in a long text and makes the symbol depend on the pagination.

## References and links

A `Footnote` has an `Id`, so `{page of: id}` gives the page it is on (that of its first call) and `Link: #id` leads to it
([References](../common/references.md), [Links](../common/links.md)).

## Errors

| Code | When |
|---|---|
| `FOOTNOTE_UNKNOWN` | A call without a `Footnote` with that `Id` in its section, or in another section than the first call to that note |
| `FOOTNOTE_NOT_CALLED` | A `Footnote` that no text calls |
| `FOOTNOTE_CALL_NOT_ALLOWED` | A call in a `Page overlay`, in a heading or inside a note |
| `FOOTNOTE_NOT_ALLOWED` | A `Footnote` inside a `Page overlay` or inside another `Footnote` |
| `FOOTNOTE_CONTENT` | A note that does not start with a paragraph, or whose `Markdown` generates headings, images or code blocks |
| `FOOTNOTES_PROPERTY_WITHOUT_EFFECT` | A property of `For element: Footnote area` without effect on the area: its height, `Rotate`, `Overflow` |
| `FOOTNOTE_TOO_TALL` | A note that does not fit on a page with the text that calls it |

## Accessibility

In the tagged PDF, the note is a `Note` (PDF/UA-1) or an `FENote` (PDF/UA-2), with the number in a `Lbl`, and goes
**right after the paragraph that first calls it**, not at the end of the page: a screen reader reads it after its
paragraph. Each call is a `Reference` inside its paragraph, which points to the note with `Ref`, and the note points to
all its calls
([The PDF inside](../implementation/pdf.md#structure-tree)).

## What is not supported

- **Numbering per page** (`Footnote numbering: page`): the number would depend on the pagination, like `{page}`.
- A style of its own for the call and the number of the note (`For element: Footnote mark`): the call takes the format
  of the text around it. Nor another list of symbols than that of `symbols`.
- Repeating a note at the bottom of every page that calls it, notes at the end of the document or of the section
  (endnotes) and notes at the foot of a table.
- The call in a literal text (without `Markup: markdown`), in a `Page overlay` or in a heading.
