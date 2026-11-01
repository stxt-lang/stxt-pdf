# Getting started

## A complete document

```
Document (dev.stxt.pdf):
    Language: en
    Title: Quarterly report
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
                Size: 18
                Bold: true
            Box:
                Margin: 0 0 12 0
        For element: Paragraph
            Box:
                Margin: 0 0 8 0
    Section:
        Heading 1:
            Text: Quarterly report
        Paragraph:
            Content >>
                Sales grew by 12% compared with the previous quarter,
                mainly in the northern region.
        Paragraph: Costs remain stable.
```

Line by line:

- **`Document (dev.stxt.pdf):`** is the root. The namespace `dev.stxt.pdf` says it is an stxt-pdf document.
- **`Language`** is the language of the document and **`Title`**, its title. Both are required: the PDF viewer shows the
  title, and a screen reader needs the language to read the text aloud.
- **`Page`** is the paper: an A4 with a 50-point margin on all four sides. A number alone is **points**: a point is
  1/72 of an inch, about 0.35 mm. With `mm` attached to the number it is millimeters: `Margin: 18mm` gives almost the
  same margin.
- **`Styles`** has the style rules. `For element: Document` gives the font and the size to the whole document. There is
  no default font, so it is always declared. The other two rules give a size to the titles and spacing to the paragraphs.
- **`Section`** has the content. Everything that is visible goes inside a section.
- **`Heading 1`** is a title and **`Paragraph`**, a paragraph. A short text goes in the value (`Paragraph: Costs...`)
  and a long one, in a `Content >>` block, where single line breaks are spaces.

The engine places the title at the top of the area left by the margins and each paragraph below the previous one. It
breaks the text into lines of the available width and, if the content does not fit on one page, it continues on the
next.

## Turning it into a PDF

The engine is a Java library. With the document in `report.stxt`:

```java
InlineNode doc = new DocumentReader().read(Path.of("report.stxt"));
try (OutputStream out = Files.newOutputStream(Path.of("report.pdf"))) {
    Converter.create().toPdf(doc, out);
}
```

Fonts and images are looked up in the `resources/` folder of the working directory ([Resources](resources.md)). The one
in the repository already includes IBM Plex Sans, Serif and Mono. More options in [Using from Java](java-usage.md).

If the document has an error, the conversion stops with a message that says what is wrong and on which line
([Errors](errors.md)):

```
line 18: Margin auto does not center a box that takes up the whole width: Width or Max width is missing
```

## What the PDF has

- The embedded fonts: it looks the same on any computer, and the text can be selected and searched.
- One bookmark per heading, which the viewer shows in its side panel.
- The tagging for accessibility (PDF/UA-1): the reading order, what is a title, a paragraph, a list or a table, and the
  alternative text of the images.

## Next steps

- [Concepts](concepts.md): the ideas the rest of the manual uses.
- [Styles](styles.md): rules so that the style is not repeated in each element.
- The [examples](../../../../test/examples/README.md): eleven real documents with their PDF.
