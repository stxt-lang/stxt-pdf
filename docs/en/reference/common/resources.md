# Resources and fonts

## The resources folder

The files a document uses are in a single folder, `resources/` by default (relative to the working directory of the
engine), which the application can change ([Using from Java](../implementation/api.md#resources)). Inside there are two
fixed subfolders, which can in turn be organized in subfolders:

```
resources/
    fonts/      TTF fonts
    images/     PNG, JPEG, SVG and PDF images
```

Resources are looked up **always downward**, from the root of their subfolder. A resource path is relative to that
subfolder: that of an `Image`, to `images/` (`logos/acme.png` is `resources/images/logos/acme.png`,
[Image](../components/image.md#file)). It allows `/` and `\` as separators and cannot:

- be absolute (`/...`, `\...`, `C:...`) or start with `~`;
- contain a `..` segment, even if it would end up inside again when normalized;
- reach, through a symbolic link, a file outside its subfolder.

> **Decision:** symbolic links that do not leave the resources folder are allowed. *Rejected alternative:* rejecting any
> link.

## Fonts

Only the TTF fonts in `fonts/` are used. There are no system fonts, nor the 14 standard PDF fonts, nor fallback fonts. The
PDF output embeds them (only the glyphs used), so that the document looks the same on any machine. That is why they must
have a license that allows embedding them, such as the SIL Open Font License. The repository includes IBM Plex Sans, IBM
Plex Serif and IBM Plex Mono, each in regular, bold, italic and bold italic.

Each file is identified by what its contents declare and not by its name
([details](../implementation/api.md#resources)):

- **family:** the one that font programs show (`IBM Plex Sans` and not `IBM Plex Sans Bold`). It is compared exactly,
  with uppercase and spaces. If it only differs in case, the error suggests it
  (`The closest name is 'IBM Plex Sans'.`);
- **weight:** 400 is regular and 700 is bold (`Bold: true`), and files of other weights (such as 300 or 500) are ignored;
- **style:** normal or italic.

Variable fonts are not allowed: the static files of each variant are used. Kerning is not applied, and ligatures, only
with `Ligatures: true` ([Text formatting](text-formatting.md#ligatures-ligatures)). `Font` and `Size` have no default
value: they are required in every text ([Typography](typography.md)).

> **Decision:** only weights 400 and 700, and the family is compared exactly. *Rejected alternative:* a numeric `Bold`
> (100 to 900), or a case-insensitive comparison.

## Checks

Before layout, the engine checks that each box with text has its font (family, weight and style) in `fonts/` and that the
font has all the characters of its text (white space characters are checked as spaces, and `{page}`, `{pages}` and the
references to other boxes, as digits). A text with runs also needs the variant of each run (`Bold: false` with a bold run
needs the bold), and each run, its characters in it. An unknown font (`FONT_UNKNOWN`), a missing variant
(`FONT_VARIANT_MISSING`) or a character the font does not have (`GLYPH_MISSING`) are errors with the line of the culprit
node:

- if the family does not exist, that of the declared `Font`, even if it is in an ancestor or in a rule;
- if a variant is missing, that of the text or that of the run, and the message names where `Font`, `Bold` and `Italic`
  are declared;
- if a character is missing, that of the character inside a `Content >>` block.

These are also errors, without a line because they do not belong to the document (`RESOURCE`): `fonts/` not existing, a
file that cannot be read, a variable font, or two files with the same family, weight and style.

> **Decision:** a character the font does not have is an error, with its line. No empty box is painted and no other font
> is looked for. *Rejected alternative:* the replacement glyph, or a list of fallback fonts.

## Images

Images are in `images/` and are checked before layout, along with the fonts ([Image](../components/image.md)).

## Hyphenation patterns

The patterns with which `Hyphenate` splits words ([Typography](typography.md#hyphenation-hyphenate)) are not in the
resources folder: they are inside the jar, in `dev/stxt/pdf/hyphenation/`, one per language, and are neither changed
nor added from outside. They are those of `hyph-utf8` (the TeX ones), converted to a plain-text format of the engine's
own by `tools/HyphenationPatterns.java`: the header of the original as comments, the minima (`left`, `right`), one
pattern per line and one exception per line. Each file thus keeps the copyright notice and the license of its
original:

| Language | Source file | Authors | License |
|---|---|---|---|
| `es` | `hyph-es.tex` (5.0, 2019-09-24) | Javier Bezos, CervanTeX | MIT/X11 |
| `ca` | `hyph-ca.tex` (1.11, 2003-07-15) | Gonçal Badenes, Francina Turon | LPPL 1.0 or later |
| `en-US` | `hyph-en-us.tex` (2005-05-30) | Gerard D.C. Kuiken, on those of Knuth and Liang | Copying and distribution permitted, with or without modification, preserving the notice |
| `en-GB` | `hyph-en-gb.tex` (2005-10-18) | Dominik Wujastyk, Graham Toal | MIT |

The LPPL allows distributing a derived version if it has another name, is identified as derived and refers to the
original; the header of `ca.txt` does so. The files are regenerated with `java tools/HyphenationPatterns.java
<folder>`, with the `hyph-*.tex` files of https://github.com/hyphenation/tex-hyphen in that folder.

> **Decision:** the patterns go in the jar and not in the resources folder: the language is not a resource of the
> document, and the patterns of a language are the same for everyone. *Rejected alternative:* a `hyphenation/`
> subfolder in `resources/`, which would allow other languages but would give a different line breaking per machine.

## What is not supported

- Fonts that are not a `.ttf` file (`.otf`, WOFF), system fonts or downloaded fonts: only the `.ttf` files of `fonts/` and
  its subfolders are indexed.
- Synthesized bold or italics: the variant must exist as a file.
- Hyphenation patterns of the document's own, or for languages other than those in the jar.
