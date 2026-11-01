# Styles: `Styles`

The rules of `Styles` give properties to many elements at once, so that they are not repeated in each one. `Styles` goes
in the `Document`, before the sections, or in a [separate file](#styles-in-a-separate-file).

```
Styles:
    For element: Document
        Typography:
            Font: IBM Plex Sans
            Size: 10
    For element: Paragraph
        Box:
            Margin: 0 0 8 0
    For style: note
        Box:
            Background: #F1EFE8
            Padding: 8
    For id: title
        Typography:
            Size: 20
            Bold: true
```

## Rules

| Rule | What it applies to |
|---|---|
| `For element: Document` | The whole document: the base typography goes here. It only accepts `Typography` |
| `For element: Paragraph` (or any other element) | All the elements of that type |
| `For style: note` | The elements with `Style: note` (an element can have several styles: `Style: note wide`) |
| `For id: title` | The element with `Id: title` |
| `For element: Header cell`, `Body cell`, `Footer cell`, `Odd row cell`, `Even row cell` | The cells of a table according to where they are ([Table](../components/table.md)) |
| `For column: 3` (or `For column: 2 4`) | The cells that start in that column (or columns) of a table |
| `For element: Code block` | The code blocks of a `Markdown` |
| `For element: TOC 1` to `TOC 6` | The entries of a table of contents, by level ([Table of contents](../components/table-of-contents.md)) |

Each rule accepts the `Box`, `Typography` and `Break` groups. The properties specific to an element (the columns of a
table, the marker of a list, a link) do not go in rules: they are written in the element.

## Which one wins

If several things give the same property, the winner is, from strongest to weakest:

1. What the element itself declares.
2. Its `For id` rule.
3. Its `For style` rules (among several, the one that comes **later** in `Styles`).
4. In the cells of a table, the rules of their row, their column and their position.
5. Its `For element` rule.
6. What it inherits from its parent (only `Typography`).
7. The default value.

Rules are combined property by property: if a rule gives `Size` and the element declares `Bold`, both apply. And a rule
wins over inheritance: with `For element: Text` and `Size: 10`, a text measures 10 even if its parent says `Size: 20`.

A `Style` name without its `For style` rule is an error, so that a typo does not go unnoticed. A rule that applies to
nothing is not an error, so that the same `Styles` can be reused in many documents.

## Named colors

A color that repeats (that of the brand, that of the rules) is declared once in `Styles` with `Named color`: the name
and the color, separated by a space. Then the name is written where the color would go, in the rules and in the
elements: `Color`, `Border color`, `Background` (also inside a gradient) and `Separator color`.

```
Document (dev.stxt.pdf):
    Language: en
    Title: Named colors
    Page:
        Paper: A5
        Margin: 15mm
    Styles:
        Named color: brand #12315A
        Named color: mist #EEF3F8
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 11
                Color: brand
        For style: panel
            Box:
                Background: mist
                Padding: 10
                Border: 0 0 0 3
                Border color: brand
    Section:
        Heading 1:
            Text: Quarterly report
        Text: Sales have grown by 12% compared to the previous quarter.
            Style: panel
```

The name is lowercase, starts with a letter and joins its words with a hyphen (`brand`, `dark-gray`, `gray-2`). A name
that is not declared is an error (`COLOR_UNKNOWN`), as is declaring the same one twice (`COLOR_DUPLICATE`). Changing the
palette of a document is changing these lines.

## The base typography

`Font` and `Size` have no default value, so every document declares them, usually in `For element: Document`.
Everything else inherits from there.

## Styles in a separate file

The rules and the named colors can go in a document of their own, with `Styles` as the root, to share them between
several documents or to leave the document with only the content:

```
Styles (dev.stxt.pdf):
    Named color: brand #12315A
    For element: Document
        Typography:
            Font: IBM Plex Sans
            Size: 10
    For style: panel
        Box:
            Padding: 10
            Border: 0 0 0 3
            Border color: brand
```

The document does not say where its styles file is: it is passed when converting it ([Using from Java](java-usage.md)).
If the document also has its own `Styles`, at each level of the list above the one in the document wins, so moving the
`Styles` to a file changes nothing. The same goes for colors: if the document declares a `Named color` with the name of
one in the file, its own wins, and all the rules that use that name, also those of the file, take its color. An error in the styles file starts with `line 8 of the styles:`, also if it is
detected during layout, such as a font that does not exist or a box that does not fit.

More detail in the [reference](../../reference/common/styles.md).
