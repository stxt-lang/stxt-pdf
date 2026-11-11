# `Markdown`

A `Markdown` is a block whose content is written in Markdown, in its `Content`. That content is **translated into the
elements that already exist**, and what is generated is treated as if the author had written it: the same `Styles` rules, the
same errors ([Errors](../common/errors.md)), the bookmarks and the accessibility. The
`Markdown` is the box that contains it, a block like a `Block`: it accepts `Id`, `Style`, `Box`, `Typography` and `Break`, and what is
generated inherits its `Typography`. It does not accept `Link`: its content carries its own links.

```
Document (dev.stxt.pdf):
    Language: en
    Title: Report
    Page:
        Paper: A4
        Margin: 40
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For element: Heading 1
            Typography:
                Size: 18
        For element: List
            Box:
                Padding: 0 0 0 20
    Section:
        Markdown:
            Typography:
                Code font: IBM Plex Mono
            Bullet markers: bullet dash
            Number markers: decimal
            Table column sizes: equal
            Content >>
                # Results

                Sales **go up** and costs *go down*: a ++very good++ quarter,
                with ~~losses~~ profits. The report comes from `mvn test`.

                - Sales on the rise
                - Costs:
                  - staff
                  - materials

                1. Review
                2. Approve

                | Region | Sales  |
                |--------|-------:|
                | North  | 120    |
                | South  | **95** |
```

| Property | Values | Default | Notes |
|---|---|---|---|
| `Content` | `>>` block | none | The Markdown. Required. |
| `Bullet markers` | list of `bullet`, `dash`, `none` | none | The `Marker` of the bulleted lists (`-`, `*`, `+`), one per nesting level. The last one serves for the deeper levels. Required if there are bulleted lists. |
| `Number markers` | list of `decimal`, `lower-alpha`, `upper-alpha`, `lower-roman`, `upper-roman` | none | The same, for the numbered lists (`1.`, `1)`). Required if there are any. |
| `Table column sizes` | `equal`, `auto` | none | The `Column sizes` of the tables: `equal` is `repeat(n, *)`, and `auto`, `repeat(n, auto)`. Required if there are tables. |

The three can also come from a rule of `Styles`, such as `For element: Markdown`, which gives them to every `Markdown`
of the document ([Element properties](../common/styles.md#element-properties)). Required means that the `Markdown` declares it or takes it from a rule.

> **Decision:** the properties that are not used are not an error (a `Bullet markers` without lists), even though in general what has no
> effect is: the content of a Markdown changes often and would force removing and adding the properties. Their value is
> always checked.

## The subset

The Markdown is [CommonMark](https://commonmark.org) with the tables and the strikethrough of GFM and the `++...++` underline. It is translated
as follows:

| Markdown | Element |
|---|---|
| `#` to `######` (and the ones underlined with `===` and `---`) | `Heading 1` to `Heading 6`, with a `Text`, and with `{#id}` at the end of the text (`## Sales {#sales}`), its `Id` |
| Paragraph | `Paragraph` |
| `-`, `*`, `+` | `List` with the `Marker` of `Bullet markers` for its level, with one `List item` per item |
| `1.`, `1)` | `List` with the `Marker` of `Number markers` for its level, `First number`, the first number if it is not 1, and, written with `1)`, `Marker suffix: parenthesis` ([Lists](#lists)) |
| Table | `Table` with the `Column sizes` of `Table column sizes`, with the header row in `Header rows` and the rest in `Body rows` |
| `**x**`, `__x__` | bold run ([Text formatting](../common/text-formatting.md#runs)) |
| `*x*`, `_x_` | italic run |
| `++x++` | underlined run |
| `~~x~~` | strikethrough run (a single tilde does not strike through) |
| `[x](target)`, `<target>` | underlined run with a link to that target ([Links](../common/links.md)) |
| `` `x` `` | code run, with the family of `Code font` |
| `[x]{name}` | styled run: with what the `For style: name` rule declares ([Text formatting](../common/text-formatting.md#styled-runs)) |
| `[^id]` | the call of the footnote with that `Id`: a superscript run with its number ([Footnote](footnote.md)) |
| `\` at the end of the line | a forced line break: the line ends there, within the same paragraph ([below](#forced-line-breaks)) |
| Code block (with ```` ``` ```` fences or indented four spaces) | `Code block`: a `Text` with `Preformatted: keep` whose font is its `Code font`, with the code in its `Content` |
| `> text` | `Block quote`: a block with what the quote contains, translated the same way ([below](#block-quotes-and-dividers)) |
| `---`, `***`, `___` | `Divider`: a line across the whole width ([below](#block-quotes-and-dividers)) |
| `![Alt](path)`, alone in its paragraph | `Image` with that path and its `Alt`, or decorative with `![](path)` ([Image](image.md)) |
| `![Alt](path)`, inside a text | inline image: a run that is the image, as tall as the letters ([Text formatting](../common/text-formatting.md#images-inside-a-text)) |

Everything else is an error (`MARKDOWN_UNSUPPORTED`): links with a title (`[x](target "title")`), images with a title
(`![Alt](path "title")`), the language tag of a code block
(```` ```java ````: there is no highlighting), a heading inside a block quote, HTML, a forced line break without text
before it on its line ([below](#forced-line-breaks)) and link definitions (`[x][ref]` with `[ref]: target`: the target is
written in the link itself).
A heading cannot go inside a list item either, as in a `List item`, nor can a table be left without rows below
the header.

**Empty blocks are ignored**: a heading without text (`#` alone), an empty code block, a quote without content (`>`
alone) and a paragraph without text generate no element, also inside a quote or an item. Two empty things are still
an error: a `Markdown` left without any element, and a heading without text and with an `Id` (`# {#sales}`), both
`TEXT_CONTENT`. A list item without content (`-` alone) is `LIST_ITEM_EMPTY`, like an empty `List item`: its
marker would be painted.

> **Decision:** an empty block is ignored. It is valid Markdown, it may arrive in a text that comes from outside
> (the data of a template) and it has nothing to paint, so ignoring it loses nothing. *Rejected alternative:* an
> error, like that of an empty `Text` the author writes: that element is declared, and this is a leftover of the
> text. It is not what "would have no effect" (below): there the author wrote something they expected to see.

A `Bullet markers` or a `Number markers` with a marker of the other type is `VALUE_NOT_ALLOWED`. A clickable image is an `Image` with
`Link` or, inside a text, an inline image inside a link (`[![Alt](path)](target)`).

> **Decision:** what would have no effect (the language tag of a code block, the title of a link) is an
> error and is not ignored.

## Text

A single line break inside a paragraph counts as a space, as in a `Text`. The formatting within the line adds up:
`***x***` is bold and italic. That same inline syntax, without the rest of the elements, is accepted by `Markup: markdown` in a
`Text`, a `Paragraph` or a `Cell` ([Text formatting](../common/text-formatting.md#inline-formatting-markup)), which inside
a `Markdown` has no effect: its texts come already interpreted.

The markers of the [references](../common/references.md) are replaced in the text of a `Markdown` as in any other, also
in its code. To write them as text, `\{page\}` or `{{page}}`
([References as text](../common/references.md#references-as-text)).

### Forced line breaks

A `\` at the end of a line is a **forced line break**: the line ends there and the text goes on in the next one, within
the same paragraph, without the margin of a new paragraph. It is for what goes on consecutive lines without being separate
paragraphs, like a signature with its position:

```
Marta Vidal\
Administration
```

- In both line breakings (`Line breaking: simple` and `optimal`) the break is a mandatory end of line, and no line goes
  past it.
- The line a forced line break ends is **not justified**, like the last one of the paragraph.
- It works in paragraphs, headings and list items, and with `Markup: markdown` in a `Text`, a `Paragraph` or a `Cell`.
- In the text of a heading that the table of contents, the bookmarks and the running heads copy, and in the alternative
  text of an image, the break is a space.
- A break without text before it on its line (at the start of the paragraph or right after another one) is
  `MARKDOWN_UNSUPPORTED`.
- The two spaces at the end of the line, the other form CommonMark accepts, are not a break: STXT removes the spaces at
  the end of each line of a `>>` block, so they do not reach the Markdown.
- In the text, the break is the character U+2028 (line separator). Written as is in any text, also without `Markup`, it
  does the same. In a preformatted text it is one more line break
  ([Text formatting](../common/text-formatting.md#preformatted-text)).

> **Decision:** a break without text before it on its line is an error. *Rejected alternative:* painting it as an empty
> line, which is mistaken for a blank line between paragraphs, which is something else (the text after it is a new
> paragraph, with its margin).

## Code

Inline code is a code run ([Text formatting](../common/text-formatting.md#inline-code)), with the
family of `Code font`: it keeps the weight and the style of the text around it and adds to the bold or the italic of the
paragraph (``**`x`**`` is bold code), and it can be inside a link. The white space of a line with
inline code is collapsed as in any paragraph. What keeps the spaces is the block.

A code block is a `Text` with `Preformatted: keep`
([Text formatting](../common/text-formatting.md#preformatted-text)) whose font is its `Code font`, so
it breaks between lines across pages and a line wider than the box is an error (`TEXT_OVERFLOW`). It has no `Id` or
`Style`, but it is the generated element `Code block` and the rule
`For element: Code block` names it ([Styles](../common/styles.md)), with `Box`, `Typography` and `Break` (a background, a padding, a
smaller size or `Keep together: true`). `For element: Text` does not reach it (it still applies to the `Text` elements of the headings of
a Markdown and to those that are written). A `Text` written by hand with `Preformatted: keep` is not a `Code block` either. Its
`Typography` is that of the `Markdown`, so `Size`, `Color`, `Tab size` or `Code font` are declared in the `Typography` of the
`Markdown`, of an ancestor or in `For element: Code block`, which can give it a different `Code font` from the inline code. A
`Font` in `For element: Code block` changes nothing, because its font is its `Code font`. The `Line height` of the box is used
and not that of the code font, so the lines with inline code do not change height.

`Code font` belongs to `Typography` ([Typography](../common/typography.md)) and is inherited: it is declared once, in
`For element: Document`, and it applies to the code of the `Markdown` elements and of the texts with `Markup: markdown`. Without it, the code
is an error (`CODE_WITHOUT_FONT`), with the line of the paragraph or that of the block. If the family does not exist, it is `FONT_UNKNOWN`,
with the line of `Code font`.

> **Decision:** `Code font` belongs to `Typography`, inheritable: that way the inline code of a text with `Markup: markdown`
> also has a font, and it is declared once for the whole document. *Rejected alternative:* a property of the
> `Markdown`, which left the texts with `Markup: markdown` without code (the invoice of the examples used a `Markdown`
> only to write the IBAN and the BIC).

> **Decision:** the code block is a generated element that only the rules name. *Rejected alternative:* a
> `Code block` that can also be written, or an automatic `code` style, which would reserve a style name from the author.

## Block quotes and dividers

```
Styles:
    For element: Block quote
        Box:
            Padding: 0 0 0 10
            Border: 0 0 0 3
            Border color: #8A8478
        Typography:
            Italic: true
Markdown:
    Content >>
        According to the minutes:

        > The budget is approved
        > unanimously.

        ---

        With no further business, the meeting is closed.
```

A block quote (`> ...`) and a horizontal rule (`---`, `***` or `___`) are two **generated elements**: they are not
written in the document and have no `Id` or `Style`, and the `For element` rules name them, like `Code block`.

| Element | What it is | Without a rule | Its rule allows |
|---|---|---|---|
| `Block quote` | a block with what the quote contains | a block with no border, padding or background | `Box`, `Typography`, `Break` |
| `Divider` | an empty block whose top border is the line | a line of 0.5 (its top `Border`), in the color of the text, across the whole width | `Box`, `Break` |

- **The content of a quote** is translated like the rest: paragraphs, lists, tables, code and another quote inside. A
  heading inside a quote is `MARKDOWN_UNSUPPORTED`, as inside a list, and a quote without content (`>` alone)
  is ignored ([above](#the-subset)).
- **Look.** A quote has no default look, like the rest of what a `Markdown` generates: without a rule it looks like
  the paragraphs it contains, and `For element: Block quote` gives it its indent, its line or its background, as in
  the example. The line of a `Divider` is what the element is: its rule changes its thickness (`Border`), its color
  (`Border color`) and its spacing (`Margin`).
- **Spacing.** Neither the quote nor the line has margins of its own: what separates them from what is above and below
  is what separates the blocks of the `Markdown` (the `Margin` of `For element: Paragraph`), or a `Margin` in their
  rule.
- **`Typography`.** That of `For element: Block quote` is inherited by what the quote contains.
  `For element: Divider` does not allow it (`STYLE_NOT_ALLOWED`): it has no text, and the color of its line is its
  `Border color`.
- **Rules.** `For element: Block` does not reach them. Their rules allow no element property and contain no nested
  rules, and they go inside the rule of an element, because both are inside their `Markdown`
  ([Styles](../common/styles.md#nested-rules)).
- **Breaks.** A quote is split across pages like a `Block`; `Keep together: true` in its rule prevents it.
- **`Markup: markdown`.** In the inline Markdown of a text, `>` and `---` are still text
  ([Text formatting](../common/text-formatting.md)).

> **Decision:** two generated elements, `Block quote` (the CommonMark term, like `Code block`) and `Divider`.
> *Rejected alternative:* translating them into an anonymous `Block`, which could not be styled, and `Rule` for the
> line, which in the API is already the name of the lines of the merged borders (`fragment.Rule`).

> **Decision:** a quote has no default look, like a heading or a list: its rule gives it one, and in the tagged PDF it
> is a quote even if it looks like a paragraph. The line of a `Divider` does have a thickness without a rule, like
> the rule of the footnote area: it is what the element is, and without it there would be nothing. *Rejected
> alternative:* a default indent and line in the quote, which would be the only style the engine gives to content,
> and requiring the rule of the `Divider`, which would make a text that comes from outside fail because of a line of
> hyphens.

## Lists

The level of a list is the number of lists of its same type that contain it: the first bulleted list uses the first
marker of `Bullet markers`, a bulleted one inside that one, the second, and so on up to the last, which serves for the deeper
levels. The numbered ones count separately, with `Number markers`. Each paragraph of an item is a `Paragraph`, and a nested list,
a `List` inside the `List item`. Tight lists and lists separated by blank lines are translated the same way: the space
between items is given by the `Styles` rules. As in any list, the indent that leaves room for the marker is its left
`Padding`, which is usually declared in the rule `For element: List` ([List](list.md)).

> **Decision:** the level is counted by type: a numbered list inside a bulleted one uses the first marker of
> `Number markers`. *Rejected alternative:* a level shared by the two types.

A list written with `1)` has `Marker suffix: parenthesis`, and its markers, the parenthesis: `1)`, `a)`
([List](list.md#markers)). One written with `1.` declares no `Marker suffix`: it has that of a rule that applies to it
(`For element: List` inside `For element: Markdown`) or, without one, the period. CommonMark starts a new list where
the delimiter changes, so each list has only one.

> **Decision:** the `1)` of the author asks for the parenthesis and `1.` asks for nothing. *Rejected alternative:* that
> `1.` declared `Marker suffix: period`, which would keep a rule from giving the parenthesis to the lists of a Markdown
> written with `1.`, the usual form; or a `Number suffixes` property per level, like `Number markers`, which repeats what
> the author already writes.

## Tables

Each column has the alignment of the delimiter row (`:--`, `:-:`, `--:`) as the `Align` of its cells. Without alignment,
they have that of the box. A cell with text is a `Cell` with a value, and an empty one, an empty `Cell`. A row with fewer cells than the
header gets empty cells added, as in GFM. One with more is `MARKDOWN_UNSUPPORTED`, with the line of the row (an escaped `\|`
does not separate cells). The cells receive the rules of the cells of any table
([Table](table.md#cell-style)). Markdown tables have no row headers (`Row header`).

> **Decision:** the missing cells are filled in, as in GFM, because an empty cell at the end of the row is often written
> without its bar. The extra ones are an error, because GFM discards them without warning. *Rejected alternative:* following
> GFM also with the extra ones.

## Style

What is generated has no `Style`, nor `Id` except the headings that write it ([below](#headings)): it is styled with
the `For element` rules of `Styles` (such as `For element: Heading 1`, `For element: Paragraph` or `For element: Code block`), which apply to
the whole document, and with the `Typography` of the `Markdown`, which it inherits.

## Headings

`#` is always `Heading 1`, and the hierarchy is checked in document order together with the headings written outside the
`Markdown` ([Headings](heading.md#hierarchy)): after a `Heading 1`, a Markdown can begin with `##`. They are numbered
like the others and go into the tables of contents. Inside a `Page overlay`, a Markdown cannot have headings.

A heading carries an `Id` if it writes it at the end of its text, `{#id}`: `## Sales {#sales}` is a `Heading 2` with
`Id: sales` and the text "Sales". It is its `Id` as if the author had written it: unique in the document
(`DUPLICATE_ID`), target of `Link: #id` and of `[text](#id)`, and `{page of: id}` and, if it is numbered,
`{number of: id}` refer to it. It is not painted. Empty or with spaces (`{#my sales}`) it is `VALUE_NOT_ALLOWED`, with the line of the
heading. It is the only thing among what is generated that carries an `Id`.

> **Decision:** the `Id` of a heading is written with the syntax of Pandoc and kramdown, `{#id}`, which those who
> write Markdown know. *Rejected alternative:* an `Id` taken from the text (`sales-of-the-year`), which changes when the
> title is corrected and can clash with those of the author.

## Where it goes

Wherever an in-flow element goes: in a section, `Block`, `Frame`, `Overlay`, `Page overlay`, `Columns`, `List item`, `Cell` and
as the element of an `Item` or an `Area`.

## Errors

The errors of what is generated point to the line of the document where its Markdown block starts (that of the item, that of the row of
the table). Those of the marker of a list point to that of `Bullet markers` or `Number markers`, and those of the columns of a table, to that of
`Table column sizes`. A list or a table without its property is an error with the line of the list or the table
(`MARKDOWN_PROPERTY_MISSING`). A property that is not used is not. Code without `Code font` is `CODE_WITHOUT_FONT`
([Code](#code)).

The errors of a paragraph of several lines point to the first one, and those of a cell, to that of its row.

## Accessibility

What is generated is read like what is written by hand: headings, paragraphs, lists and tables, and its headings are bookmarks.
A block quote is read as a quote, and a horizontal rule is not read.

## What is not supported

- HTML, task lists and code highlighting. Footnotes are called with
  `[^id]`, but their content is a `Footnote`, not a definition `[^id]: ...` ([Footnote](footnote.md)).
- Links with a title or with a separate definition, and images with a title.
- Two spaces at the end of the line as a forced line break: STXT removes them.
- `Id` in what is generated, except in the headings (`{#id}`), and `Style`.
