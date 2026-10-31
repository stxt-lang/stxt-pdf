# Headings: `Heading 1` to `Heading 6`

The titles of chapters and sections. A heading is a block with one or more `Text` elements inside, and its level goes from
1 (the most important) to 6.

```
Document (dev.stxt.pdf):
    Language: en
    Title: Installation manual
    Page:
        Paper: A4
        Margin: 50
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For element: Heading 1
            Typography:
                Size: 20
                Bold: true
                Numbered: false
            Box:
                Margin: 0 0 16 0
        For element: Heading 2
            Typography:
                Size: 14
                Bold: true
                Numbered: true
            Box:
                Margin: 16 0 8 0
        For element: Heading 3
            Typography:
                Size: 11
                Bold: true
                Numbered: true
            Box:
                Margin: 10 0 6 0
    Section:
        Heading 1:
            Text: Installation manual
        Heading 2:
            Text: Before starting
        Paragraph: Check that all the parts are there.
        Heading 2:
            Id: wiring
            Text: Wiring
        Heading 3:
            Text: Cables
        Paragraph: Connect the red wire to terminal L.
        Paragraph: The steps of section {number of: wiring} are done with the power off.
```

The title of the document has no number and the sections do: "1 Before starting", "2 Wiring", "2.1 Cables".

## Properties

They accept `Id`, `Style`, `Link`, `Box`, `Typography` (which their `Text` elements inherit) and `In table of contents`.
They do not accept `Break`: the engine already avoids breaking inside a heading and right after it, so that a title is not
left alone at the end of a page.

## No default styles

A `Heading 1` is not larger than a `Heading 2` unless a style says so. The usual approach is to give the style of each level
in `Styles`, once for the whole document.

## The levels

Levels cannot be skipped: the first heading of the document is a `Heading 1`, and after a `Heading 1` there can be a
`Heading 2` but not a `Heading 3`. Going back up has no limit. With this, the bookmarks panel of the PDF and the screen
reader always have a correct tree.

## Numbering (`Numbered`)

With `Numbered: true` (from `Typography`, it is inherited) a heading has a number: `1`, `1.1`, `1.1.1`. It is declared per
level in a rule, in the whole document or on a specific heading.

- Each level counts in document order, also across sections, and starts again with each heading of a higher level.
- A level without a number does not contribute to the number of the ones below it: with the title unnumbered, the
  `Heading 2` elements are `1`, `2`, `3`.
- The number is written before the text, with a space, and also appears in the table of contents and in the bookmarks.
- `{number of: id}` writes the number of a numbered heading: "section {number of: wiring}" gives "section 2".
- `Number format` (also from `Typography`) writes the number of a level in letters or in roman numerals: `decimal` (the
  default), `lower-alpha`, `upper-alpha`, `lower-roman` or `upper-roman`. Each level uses its own: chapters in roman
  numerals and sections in digits give `II.1`.
- `Number label` changes what goes before the title: `Number label: Appendix {number}.` writes "Appendix A. Fees". The
  `{number}` is where the number goes, and `{number of: id}` still gives only the number ("appendix A").

```
Styles:
    For style: annex
        Typography:
            Numbered: true
            Number format: upper-alpha
            Number label: Appendix {number}.
Heading 1:
    Text: Appendices
Heading 2:
    Style: annex
    Text: Fees              # "Appendix A. Fees"
```

## Out of the table of contents (`In table of contents`)

With `In table of contents: false`, a heading is still a title (for the PDF, the bookmarks and the levels) but does not
appear in the table of contents ([Table of contents](table-of-contents.md)). This is the usual choice for the title of the
cover or that of the table of contents itself.

## Where it goes

Wherever any element goes, but not inside a `Page overlay` (which is repeated on every page), an item of a list or a cell of
a table.

More detail in the [reference](../../reference/components/heading.md).
