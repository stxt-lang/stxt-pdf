# Typography: `Typography`

Text and how it is broken into lines. **It is the only group that is inherited**: what an element declares applies to it and
to all its descendants that do not change it. The base inherited by the whole document is declared in the
`For element: Document` rule of `Styles` ([Styles](styles.md#the-base-typography)), and each element declares only what changes.
What a `Styles` rule gives an element in `Typography` is inherited by its descendants as if the element had declared
it.

`Typography` is accepted in all box elements except `Image`, which has no text, and in `Row` (its cells
inherit it). In a `Barcode` it only accepts `Color`, that of its modules ([Barcode](../components/barcode.md#the-modules)).

| Property | Values | Default | Notes |
|---|---|---|---|
| `Font` | family name | none | **Required** in all text, own or inherited (`TEXT_WITHOUT_FONT`). It is the family declared by the TTF or OTF files in the fonts folder ([Resources](resources.md#fonts)). |
| `Code font` | family name | none | The family of code, inline and in blocks ([Text formatting](text-formatting.md#inline-code)). Usually a monospaced one. **Required** if there is code, own or inherited (`CODE_WITHOUT_FONT`). |
| `Bold` | `true`, `false` | `false` | Bold: `true` is `Weight: 700` and `false`, `Weight: 400`. Not together with `Weight` in the same `Typography` (`WEIGHT_CONFLICT`). |
| `Weight` | 100 to 900, in steps of 100 | `400` | Weight of the font ([Weights](#weights-weight)). |
| `Italic` | `true`, `false` | `false` | Italic: the italic variant of the font. |
| `Size` | number > 0 | none | Font size. **Required** in all text, own or inherited (`TEXT_WITHOUT_FONT_SIZE`). |
| `Min size` | number > 0 | `6` | The smallest `Size` that `Overflow: shrink` reduces the text to ([Overflow](overflow.md#shrinking-the-text-shrink)). |
| `Line height` | number or % | `120%` | **A number is a measurement** (`15` = 15 pt). For a value relative to `Size`, a percentage (`120%`). |
| `Align` | `left`, `right`, `center`, `justify` | `left` | The last line of a justified paragraph is aligned to the left. |
| `Color` | color | `#000000` | Color of the text ([Values](values.md#colors)). |
| `Line breaking` | `simple`, `optimal` | `simple` | Line breaking: greedy (`simple`) or Knuth-Plass (`optimal`). |
| `Hyphenate` | `true`, `false` | `false` | Split words with a hyphen at the end of the line, with the patterns of the language ([Hyphenation](#hyphenation-hyphenate)). |
| `Break long words` | `never`, `new-line`, `same-line` | `never` | Cut, with no hyphen, the part of a word that does not fit in a whole line, and where it starts ([Long words](#long-words-break-long-words)). |
| `First line indent` | length, no % | `0` | Indent of the first line of the paragraph; it can be negative ([First line indent](#first-line-indent-first-line-indent)). |
| `Markup` | `none`, `markdown` | `none` | Formatting within the line ([Text formatting](text-formatting.md#inline-formatting-markup)). |
| `Case` | `none`, `uppercase`, `lowercase`, `capitalize` | `none` | Capitalization when painting ([Text formatting](text-formatting.md#capitalization-case)). |
| `Letter spacing` | number | `0` | Letter spacing; it can be negative ([Text formatting](text-formatting.md#letter-spacing-letter-spacing)). |
| `Small caps` | `true`, `false` | `false` | Small caps ([Text formatting](text-formatting.md#small-caps-small-caps)). |
| `Position` | `normal`, `superscript`, `subscript` | `normal` | Superscript or subscript; only in the rule of a styled run, and not inherited ([Text formatting](text-formatting.md#superscript-and-subscript-position)). |
| `Ligatures` | `true`, `false` | `false` | Ligatures of the font ([Text formatting](text-formatting.md#ligatures-ligatures)). |
| `Kerning` | `true`, `false` | `false` | Kerning pairs of the font ([Kerning](#kerning-kerning)). |
| `Preformatted` | `none`, `keep`, `wrap` | `none` | Preformatted text ([Text formatting](text-formatting.md#preformatted-text)). |
| `Tab size` | integer ≥ 1 | `4` | With `Preformatted: keep` or `wrap`, the columns of a tab. |
| `Numbered` | `true`, `false` | `false` | Whether headings carry a number ([Headings](../components/heading.md#numbering-numbered)). A `Figure` is numbered unless it, or a rule of its own, declares otherwise ([Figure](../components/figure.md#numbering)). |
| `Number format` | `decimal`, `lower-alpha`, `upper-alpha`, `lower-roman`, `upper-roman` | `decimal` | How the number of the level of a heading, or that of a figure, is written ([Headings](../components/heading.md#format-and-label-number-format-number-label)). |
| `Number label` | text with `{number}` | the number | What goes in front of the title of a numbered heading, or of the caption of a figure ([Headings](../components/heading.md#format-and-label-number-format-number-label)). |
| `Widows` | integer ≥ 1 | `2` | Minimum number of lines of a paragraph that go to the next page ([Breaks](breaks.md#choosing-the-break)). |
| `Orphans` | integer ≥ 1 | `2` | Minimum number of lines of a paragraph that stay at the bottom of a page. |
| `Language` | BCP 47 tag (`es`, `en-GB`, `ca`) | the `Language` of `Document` | Language of the text of the box, when it is not that of the document ([language](#language-language)). |

What has no text ignores what it inherits without an error: a `Block` inherits `Size` for its children, and a `Text` that inherits
`Numbered: true` does nothing, because only the headings and the figures read it.

## Where there is text

`Text`, `Paragraph` and a `Cell` with a value have text ([Text](../components/text.md)), and text is generated by the headings
(their `Text`), the entries of a `Table of contents` and what a `Markdown` translates. The text of a `Text` goes in its value
(`Text: A title`) or in a `Content >>` child with an indented block, without blank lines: each box is a paragraph.

## Spaces and paragraphs

Sequences of spaces and single line breaks are equivalent to a space. A **blank line** is an error
(`TEXT_BLANK_LINE`): each `Text` and each `Paragraph` is a single paragraph. Within it, a line can be ended on purpose
with the Markdown line break, `\` at the end of the line
([Markdown](../components/markdown.md#forced-line-breaks)). With `Preformatted: keep` or `wrap`
spaces and line breaks are kept, including the blank lines of a `Text`
([Text formatting](text-formatting.md#preformatted-text)).

The space, the tab, the line breaks, U+000B and U+000C are white space. Any other Unicode space, like the em space
(U+2003) or the ideographic space (U+3000), is one more character: it is not collapsed, it does not separate words and
the font has to have it (`GLYPH_MISSING`).

## Line breaking

Text is broken into **lines** as wide as the content box:

- **`Line breaking: simple`**, greedy algorithm: words are added to the line while they fit.
- **`Line breaking: optimal`**, **Knuth-Plass**: the breaks of the paragraph that minimize the sum of the demerits of its
  lines are chosen, so that the lines are more even. Spaces stretch up to half their width and do not shrink,
  and the last line does not count.

The lines beside a side box ([Block](../components/block.md#side-boxes-side)) or a drop cap
([Text](../components/text.md#drop-cap-drop-cap)) are shorter than the content box: both algorithms work with the width
of each line, and a word that does not fit in a short line leaves it empty and goes to the next one. The first line
of a paragraph with `First line indent` has another width too ([First line indent](#first-line-indent-first-line-indent)).

A word is split after a hyphen written in it, where it has a soft hyphen or, with `Hyphenate`, by the patterns of
the language ([Hyphenation](#hyphenation-hyphenate)). In both algorithms, a word wider than the line, or a part of it
that cannot be split further, is an error (`TEXT_OVERFLOW`, [Overflow](overflow.md)), except with
`Break long words`, which cuts it where it fits ([Long words](#long-words-break-long-words)).

## Weights (`Weight`)

`Weight` chooses the weight of the font: from 100, the thinnest, to 900, the heaviest, in steps of 100; any other number
is an error (`VALUE_NOT_ALLOWED`). 400 is regular and 700, bold. Each weight is a file of the family in the fonts
folder: the repository includes IBM Plex Sans in 300, 400, 500, 600 and 700 ([Resources](resources.md#fonts)). A weight
the family does not have is an error (`FONT_VARIANT_MISSING`): it is not synthesized and the closest one is not used.

```stxt
Typography:
    Font: IBM Plex Sans
    Weight: 300
```

- **`Bold` is another way of writing the weight**: `Bold: true` is `Weight: 700`, and `Bold: false`, `Weight: 400`.
  They are the same property: between a rule, an ancestor and the box the last declaration wins, as with the rest, so
  `Bold: true` in a box that inherits `Weight: 300` gives 700. Both in the same `Typography` are an error
  (`WEIGHT_CONFLICT`), with the line of the `Bold`.
- **A bold run** (`**x**`) has weight 700, or that of the box if it is already 700 or more
  ([Text formatting](text-formatting.md#runs)).
- **Large bold text**, for the contrast, is text of weight 700 or more ([The PDF](pdf.md#contrast)).

> **Decision:** a numeric `Weight`, as fonts declare it (`usWeightClass`), and `Bold` as a shortcut for 700.
> *Rejected alternative:* weight names (`light`, `medium`, `semibold`), which each family applies to different weights,
> or a numeric `Bold`.

> **Decision:** `Bold` and `Weight` in the same `Typography` are an error, because either they contradict each other or
> one of them is not needed; between rules, ancestors and the box the last declaration wins. *Rejected alternative:* the
> error also when they come from different places, which would force removing the `Bold` of a rule to give a box a
> `Weight`.

## Hyphenation (`Hyphenate`)

With `Hyphenate: true` the line breaking may split a word at the end of the line with a hyphen, following the
hyphenation patterns of the language of the box (`Language`). It belongs to `Typography`, is inherited and is `false`
by default.

- **The patterns are those of TeX** (`hyph-utf8`), with Liang's algorithm, and are built into the engine for `es`,
  `ca`, `en-US`, `en-GB`, `de`, `fr`, `it`, `nl` and `pt` ([Resources](resources.md#hyphenation-patterns)). `en`
  without a region, and any other region of English, use the American ones; the other languages, with any region or
  variant, their own. Those of German are the ones of the reformed spelling (`de-1996`), also with `de-CH`, `de-AT` or
  `de-1901`. `Hyphenate: true` on a box with text in another language is an error
  (`HYPHENATION_LANGUAGE_UNSUPPORTED`), with the line of the `Hyphenate`, even if it comes from an ancestor or from a
  rule. A box without text is not checked.
- **The minima are those of the patterns**: at least 2 letters stay before the hyphen and 2 after it in `es`, `ca`,
  `de`, `fr`, `it` and `nl`, and 2 and 3 in English and in `pt`; a shorter word is not split. A word is split only
  through its letters, which have to be all together: what goes before them (a quotation mark, a parenthesis) and after
  them (a period, a comma) stays with the first and the last part and does not count for the minima. A word with
  something that is not a letter among its letters (a digit, a hyphen, the middle dot of `col·lecció`) or with
  `{page}`, `{pages}` or a reference is not split by the patterns. Capitals are split like lowercase letters.
- **The apostrophe** (`'` or `’`) among the letters of a word is one more letter in the languages whose patterns
  include it, `fr` and `it`: `l'information` is split `l'in-for-ma-tion` and `dell'amministrazione`,
  `del-l'am-mi-ni-stra-zio-ne`, and it counts for the minima. In the other languages a word with an apostrophe is not
  split. At the start or at the end of the word it is punctuation, like a quotation mark.
- **A soft hyphen** (U+00AD) written in the text is an explicit break point, with or without `Hyphenate`: a word that
  has one is split only there, and the patterns add no more. It is neither painted nor measured where the word is not
  split. In a text with `Preformatted: keep` or `wrap` it is discarded.
- **A written hyphen** (`-`, U+002D) with a letter or a digit on each side is a break point, with or without
  `Hyphenate`: the word is split **after it** and no other hyphen is added (`REF-2026-` and `009812`, `socio-` and
  `economic`). It is not one at the start or at the end of the word, nor beside another hyphen or a punctuation
  mark. The parts of such a word are not split by the patterns; if it also has soft hyphens, both count. The
  non-breaking hyphen (U+2011) is not a break point: it is the way to keep the word whole. It does not apply in a
  text with `Preformatted: keep` or `wrap`.
- **The hyphen** is the one of the font (`-`, U+002D), which has to have it (`GLYPH_MISSING`), measures what it
  measures in it and is painted with the font and the format of the letter before it (bold if that letter is bold;
  never in small caps). It counts in the width of the line.
- **In the line breaking**, with `simple`, when a word does not fit whole the longest part of it that fits with its
  hyphen is added and the rest starts the next line; with `optimal`, each break point is a possible break with TeX's
  penalty (50, squared, in the demerits of the line, and 10,000 more if two consecutive lines end with a hyphen). A
  word wider than the line is split in as many parts as needed. The break after a written hyphen is one more break
  point in both algorithms, and in `optimal` it has the same penalty and counts as a line that ends with a hyphen.
- **Intrinsic sizes**: the narrowest width is the widest part that cannot be split further, with its hyphen: the one
  the line breaking adds or, after a written hyphen, its own.
- **What is read**: the text that is copied or that a screen reader reads does not carry the hyphen the line
  breaking adds; in its place goes a soft hyphen (U+00AD), which is how a tagged PDF says that the hyphen was added
  by the line breaking, and the split line does not carry the invisible space at its end
  ([The PDF inside](../implementation/pdf.md#text-that-is-read)). A line that ends after a written hyphen is read as
  it is painted, with its hyphen, and does not carry that space either.

> **Decision:** the TeX patterns as resources of the engine, converted to a plain-text format of its own and without
> dependencies; nine languages, and the rest are an error and not a silence. *Rejected alternative:* a hyphenation
> library, or splitting without patterns.

> **Decision:** German only with the reformed spelling of 1996, the official one, for any region and variant: the
> traditional one is hardly used in new documents and its patterns would cost another 120 KB in the jar. *Rejected
> alternative:* the patterns of the traditional spelling (`hyph-de-1901.tex` and `hyph-de-ch-1901.tex`) for the
> variants `de-1901` and `de-CH-1901`.

> **Decision:** the apostrophe is a letter where the patterns include it, as in TeX, so that French and Italian split
> the words with elision, which are very frequent. *Rejected alternative:* the general rule (a word with something
> that is not a letter among its letters is not split), which would leave `l'information` or `dell'amministrazione`
> whole.

> **Decision:** a single property, `Hyphenate`, with the minima the patterns of each language bring. *Rejected
> alternative:* properties for the minimum length of the word and for the letters on each side, which almost nobody
> changes.

> **Decision:** a word with a soft hyphen is split only where it has one, as in TeX, where whoever writes the break
> decides. *Rejected alternative:* adding the break points of the patterns.

> **Decision:** a hyphen written between letters or digits is always a break point, with no property, as in any text
> composition: a reference or a compound term does not stop fitting for having hyphens. *Rejected alternative:* a
> property to turn it on, or breaking also after `/`, `.`, `_` and `@`, which is what `Break long words` covers.

## Long words (`Break long words`)

With `Break long words: new-line` or `same-line`, the part of a word that cannot be split further and is **wider
than a whole line of its box** is cut at the last character that fits, with no hyphen, as many times as needed.
With `never`, the default, it is not cut and it is an error (`TEXT_OVERFLOW`). It belongs to `Typography` and it is
inherited. It is for what arrives in the data with no place to be
split at: an IBAN, an email address, a URL or an identifier in a narrow cell.

```stxt
Styles:
    For element: Cell
        Typography:
            Break long words: new-line
```

The two values that cut differ in where the long part starts. In a line of ten characters, `ID ES762077002400`
becomes:

| Value | Lines | |
|---|---|---|
| `new-line` | `ID` / `ES76207700` / `2400` | The long part starts a line of its own, as in a word processor |
| `same-line` | `ID ES76207` / `7002400` | It starts in the current line, after the words before it |

- **It is the last resort.** A word that fits in an empty line goes to the next line, as without the property, and
  is never cut. Written hyphens, soft hyphens and the patterns of `Hyphenate` come first: only the part that, between
  two of those break points, still does not fit in the width of the content box is cut.
- **Where it is cut.** The long part may end a line after any of its characters, and ends it after the last one
  that fits. With `new-line` it starts a line of its own: the words before it, and the parts of its word that come
  before it (`REF-`), end the previous line. With `same-line` it starts after them, if any of its characters fits
  in their line. With both, the words after it go on in the line of its last piece. Beside a
  side box or a drop cap it is cut at the width of each line.
- **A character** is what is read as one: a letter is not separated from its combining marks, nor the two halves of
  a surrogate pair from each other.
- **No hyphen.** The cut adds nothing, and the text that is copied or that a screen reader reads is the whole
  word: the cut line does not carry the invisible space at its end
  ([The PDF inside](../implementation/pdf.md#text-that-is-read)).
- **In the line breaking**, with `optimal`, a line ends at such a cut only if one more character does not fit in
  it. That line has no badness, because it is full, and it has the penalty of a hyphen, but not that of two
  consecutive lines that end with a hyphen.
- **With `First line indent`**, the first word of the paragraph is measured against its line, the indented one: if
  its first part does not fit in it, it is cut there, even if it fits in a whole line
  ([below](#first-line-indent-first-line-indent)).
- **What is not cut.** A word with `{page}`, `{pages}` or a reference. If not even one character fits in the line,
  it is `TEXT_OVERFLOW`.
- **Intrinsic sizes**: they do not change ([below](#intrinsic-sizes)).
- **With `Overflow: shrink`**, no word is wider than the box any more, so the text only shrinks if its lines do not
  fit in the height ([Overflow](overflow.md#shrinking-the-text-shrink)).
- **With `Preformatted: keep` or `wrap`** it has no effect: the lines are the ones written, or are broken only
  between words. Written by the preformatted text itself it is an error (`BREAK_LONG_WORDS_IN_PREFORMATTED`);
  inherited or from a rule it is not checked, like the rest of `Typography`
  ([Errors](errors.md#what-has-no-effect)).

> **Decision:** a property, `never` by default: cutting a word at any letter is the author's decision. *Rejected
> alternative:* doing it always, which would turn an error into an ugly cut nobody asked for, and one more value of
> `Overflow`, which belongs to the box and is not inherited.

> **Decision:** the threshold is the whole line of the box and not what is left of the current line: a word that
> fits in a line of its own is never cut, not beside a side box either, where it leaves the short lines empty like
> any other. The exception is the first word of an indented paragraph, which has no line before it to leave a gap
> in: its threshold is the indented line. *Rejected alternative:* cutting every word that does not fit in what is
> left of the line, which would split ordinary words.

> **Decision:** where the long part starts is chosen by the author with the value: `new-line`, in a line of its
> own, as in a word processor and with `overflow-wrap` in CSS, so that the data always starts at the edge of its
> box; `same-line`, in the current line, which leaves no short line before the data. *Rejected alternative:* a
> boolean with only one of the two: both have their use and neither is right for every document.

> **Decision:** `Break long words` does not change the narrowest width: an `auto` column does not shrink to one
> letter for having it, and it only acts when the final width is not enough. *Rejected alternative:* the narrowest
> width being that of the widest character, with which a table of `auto` columns would cut words that fit.

> **Decision:** in a preformatted text it is an error only if the text itself writes it, with `Preformatted: keep`
> and with `wrap`. *Rejected alternative:* cutting the long words of `wrap`, which no document has asked for, and the error
> also when it is inherited, which would reach every code block of a document that declares it in
> `For element: Document`.

## First line indent (`First line indent`)

`First line indent` moves the start of the **first line** of each box of text and shortens that line by as much. It
belongs to `Typography`, it is inherited and it is `0` by default. It is a length in points or millimeters, with no
percentages (`VALUE_NOT_ALLOWED`).

```stxt
Styles:
    For element: Paragraph
        Typography:
            First line indent: 14
```

With a negative value and a left `Padding` it is the **hanging indent** of clauses, bibliographies and glossaries:
the first line starts before the others.

```stxt
Styles:
    For style: clause
        Box:
            Padding: 0 0 0 30
        Typography:
            First line indent: -30
```

- **Where it is declared.** It is inherited, so it applies to every box with text that gets it: from
  `For element: Document` it also indents headings, cells and the text of list items. `For element: Paragraph`
  gives it only to paragraphs. The marker of a `List item` does not get it: it stays where it would be without it
  ([List](../components/list.md#position)).
- **Only the first line of the paragraph.** Not the one after a forced line break
  ([Markdown](../components/markdown.md#forced-line-breaks)), nor the first one of the fragment when the paragraph
  goes on in another page or column.
- **Alignment.** The line is aligned in what is left after the indent: with `Align: center` or `right` it is
  centered or taken to the right in that width, and with `justify` it fills it.
- **Negative.** The first line starts to the left of the content box, inside the left `Padding` of its box, and is
  that much longer. If the absolute value is greater than that `Padding`, the line would start outside the box: it
  is an error (`FIRST_LINE_INDENT_OVERFLOW`), at the line of the `First line indent`, even if it is in an ancestor
  or in a rule. It is compared with the padding of the laid out box, so a `Padding` in percentage counts for what it
  measures.
- **Beside a side box** ([Block](../components/block.md#side-boxes-side)), the indent is added to what the box takes
  from the first line. The rest of a paragraph that is broken again on the next page is not indented.
- **With `Drop cap`**, the paragraph is not indented: the indent it inherits or gets from a rule is not applied to
  it, and its first line starts after the drop cap. An indent other than 0 written in the paragraph itself is an
  error (`FIRST_LINE_INDENT_WITH_DROP_CAP`), at the line of the `Drop cap`: both move the start of the paragraph.
- **With `Preformatted: keep` or `wrap`** it applies the same way, to the first line of the text; with `wrap`,
  that line is broken at what the indent leaves.
- **What does not fit.** An indent that leaves no room for the first word is `TEXT_OVERFLOW`: the word does not go
  to the next line. With `Break long words: new-line` or `same-line`, that word is cut at the last character that fits after the
  indent and goes on in the next line; if not even one character fits, it is `TEXT_OVERFLOW`.
- **Intrinsic sizes**: the natural width adds the indent to the first line, and the narrowest width is the greater
  of the widest word and the first word plus the indent ([below](#intrinsic-sizes)).

> **Decision:** a property of `Typography`, inherited like its whole group, which a `For element: Paragraph` rule
> gives to paragraphs without touching headings or cells. *Rejected alternative:* a property of `Paragraph` that is
> not inherited, which would not serve the clauses written as `Text` nor work from a `Block`.

> **Decision:** the marker of a `List item` does not get the indent its item inherits: it is a generated label, not
> a paragraph, and it has no `Padding` for a negative indent to hang in. *Rejected alternative:* inheriting it like
> the rest of the typography of the item, with which a hanging indent declared over a list would be
> `FIRST_LINE_INDENT_OVERFLOW` because of its marker.

> **Decision:** a negative indent fits in the left `Padding` of its box, and otherwise it is an error: the text
> does not leave its box. It is checked on the laid out box, like what overflows, because the padding may be a
> percentage. *Rejected alternative:* letting the line leave the box, and checking it when the document is read,
> which does not reach a `Padding` in percentage.

> **Decision:** the drop cap cancels the indent the paragraph inherits or gets from a rule, and only the one it
> writes is an error, like `Break long words` in a preformatted text: a paragraph with a drop cap is not indented,
> and the indent usually comes from `For element: Paragraph`. *Rejected alternative:* the inherited one being an
> error too, which means writing `First line indent: 0` in each paragraph with a drop cap of a document with
> indented paragraphs.

> **Decision:** a first word that does not fit after the indent overflows, and with `Break long words` it is cut in
> the indented line. *Rejected alternatives:* leaving the first line empty and moving the word down, as beside a
> side box, which would give a paragraph with no indent after a blank line; and `Break long words` measuring that
> word against the whole line too, with which an indented paragraph that starts with a URL would be an error with
> the property set.

## Kerning (`Kerning`)

With `Kerning: true` the text is measured and painted with the kerning pairs of the font, which bring two specific
letters closer or move them apart according to their shape (`AV`, `To`, `P.`). It belongs to `Typography`, is inherited
and is `false` by default: it is declared once in `For element: Document`.

- **The pairs are those of the font**: those of the `kern` feature of its `GPOS` table (that of the default script,
  `DFLT`, or, if it has none, that of Latin, `latn`) or, in a font without it, those of its `kern` table. A font without
  pairs is measured and painted as without `Kerning`, without an error. IBM Plex Sans and Serif have them in `GPOS`;
  IBM Plex Mono, monospaced, has none.
- **A pair** is two consecutive characters of the same word and the same run: it does not cross a space or the boundary
  between two runs (`**A**V`). The last letter of a split word and its hyphen are a pair too, since they go in the same
  run except in small caps. With `Ligatures: true` the pair is between the glyphs that are left (the `o` and the `fi`
  ligature of `Tofi`), never inside a ligature.
- **Measurement**: the adjustment of each pair is rounded to thousandths of the font size, like the advance of the glyphs,
  and is added to the width of the word; what is measured is what is painted. With `Letter spacing`, the letter spacing is
  added to the adjustment.
- **What is read**: kerning does not change the text that is copied or that a screen reader reads.

> **Decision:** the engine reads the pairs of `GPOS`, which the font library (fontbox) does not read, because current
> fonts, IBM Plex included, only have them there; the `kern` table is left for old fonts. *Rejected alternative:* only
> the `kern` table, with which `Kerning` would do nothing with the fonts of the repository.

> **Decision:** `false` by default, so that no existing document changes, and a single property, without choosing tables
> or pairs. *Rejected alternative:* kerning always, which would move the lines of every document.

> **Decision:** of `GPOS` only horizontal kerning is applied: the advance of the first glyph of each pair, without
> skipping the marks that separate two letters or the pairs specific to a language. *Rejected alternative:* a complete
> shaping engine, with every feature, context and script.

## Line height and baseline

Each line measures `Line height`, and the height of the content is the number of lines times `Line height`. Inside the line, the
box of the font (its ascent plus its descent) is centered vertically, which gives the baseline:

```
baseline = top + (Line height − (ascent + descent) · Size) / 2 + ascent · Size
```

## Alignment

`left`, `right` and `center` shift each line. `justify` shares the remaining space evenly among the spaces of the
line. The last line of each paragraph, lines with a single word and lines that overflow (spaces
are not compressed) are not justified. The last line of a page is not the last of its paragraph if the paragraph continues on the next
page, so it is justified.

## Text measurement

The width of a text is the sum of the advances of its glyphs, rounded to thousandths of the font size, with the kerning
adjustments only if they are requested ([Kerning](#kerning-kerning)) and with ligatures only if they are requested
([Text formatting](text-formatting.md#ligatures-ligatures)). A word with parts from
several runs measures the sum of its parts. What is measured is exactly what is painted
([details](../implementation/painting.md#text)).

## Intrinsic sizes

The narrowest width is the widest word (if it has break points, from `Hyphenate`, from a soft hyphen or from a
written hyphen, the widest of its parts, with its hyphen), and the natural width, the longest paragraph on a single
line (with `Preformatted: keep` or `wrap`, see [Text formatting](text-formatting.md#intrinsic-sizes)).
`Break long words` changes neither ([Long words](#long-words-break-long-words)). With `First line indent`, the
natural width adds the indent to the first line and the narrowest width is the greater of the widest word and the
first word (or its first part) plus the indent; with a negative indent, the natural width does not go below the
widest word ([First line indent](#first-line-indent-first-line-indent)).

## Language (`Language`)

A box whose text is not in the language of the document declares its own in `Typography` (`Language: en-GB`), also from
a `Styles` rule (`For style: english`). It is inherited. A screen reader changes voice when reading that box, and
`Case` uses the capitalization rules of that language. It is a BCP 47 tag (`VALUE_NOT_ALLOWED`) and `For element: Document`
does not accept it (`STYLE_NOT_ALLOWED`): the language of the document is the `Language` of `Document`
([Document](../components/document.md#title-and-language)).

> **Decision:** language belongs to the box and not to a run within a line or to a part of a `Markdown`.

## What is not supported

- Fallback fonts, synthesized bold or italic, and the closest weight when the requested one is missing
  ([Resources](resources.md#fonts)).
- Units relative to the font: everything is measured in points, millimeters or percentages.
- Vertical or right-to-left writing, and, of `GPOS`, what is not horizontal kerning (marks, contextual positioning).
- Hyphenation patterns for languages other than `es`, `ca`, `en-US`, `en-GB`, `de`, `fr`, `it`, `nl` and `pt`, those
  of the traditional German spelling, patterns or exceptions of the document's own, and changing the minimum letters
  on each side of the hyphen.
- Break points after characters other than the hyphen (`/`, `.`, `_`, `@`), preventing the break after a written
  hyphen in any other way than with the non-breaking hyphen, and cutting the long words of a preformatted text.
- A first line indent in percentage, and indenting the line after a forced line break.
- A different size within a line (except the drop cap, the call of a footnote and a superscript or subscript run): a
  run changes the family, the weight, the style, the color, the capitalization and the letter spacing
  ([Text formatting](text-formatting.md#styled-runs)), but not the size.
