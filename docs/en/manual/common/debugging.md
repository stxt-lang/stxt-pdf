# Debugging a document

To see what the engine has done with a document while it is being written there are four aids, all from Java
([Using from Java](java-usage.md)):

- **The SVG preview**: an image of each page, which opens in a browser.
- **The outlines**: the rectangle of each box, drawn over the page.
- **The dump**: the position and the size of the boxes with an `Id`, as text.
- **The errors**: the first problem of the document, with its line.

The examples on this page use this notice:

```stxt
Document (dev.stxt.pdf):
    Language: en
    Title: Water outage
    Page:
        Paper: A5
        Margin: 15mm
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For element: Heading 1
            Typography:
                Size: 16
                Bold: true
            Box:
                Margin: 0 0 8 0
    Section:
        Heading 1:
            Id: title
            Text: Water outage on Tuesday the 18th
        Paragraph: On Tuesday, November 18, from 9:00 to 13:00, there will be no water in buildings 2 and 4 while the main pipe is replaced.
            Id: notice
        Stack:
            Id: footer
            Box:
                Margin: 12 0 0 0
            Item:
                Grow: 1
                Block:
                    Id: contact
                    Text: Calle Mayor 2-4 owners association
                    Text: Incidents: 600 123 456
            Item:
                Barcode: https://comunidad.example/avisos/2025-11
                    Id: qr
                    Alt: QR code with the notice on the website of the association
                    Box:
                        Width: 25mm
```

## The SVG preview

```java
Converter converter = Converter.builder().debug(true).build();
LaidOutDocument laidOut = converter.layOut(doc);
List<String> pages = converter.toSvg(laidOut);   // one string per page
for (int i = 0; i < pages.size(); i++) {
    Files.writeString(Path.of("notice-" + (i + 1) + ".svg"), pages.get(i));
}
```

`layOut` lays out the document once, and with the result the SVG, the PDF or both can be written. Each page is an SVG
file that a browser opens as it is.

In the SVG, the boxes, the line breaks and the page breaks are those of the PDF. The text is not: the SVG does not carry
the fonts and is drawn with those of the viewer, so the shape of the letters may change and a line may look somewhat
longer or shorter. The images go inside the SVG. The links do not.

## The outlines

With `debug(true)`, the engine draws the outline of each box with a dashed red line, in the SVG and in the PDF. The
outline is the outer edge of the box: it includes the padding and the border, and leaves out the margin.

In the notice, the outlines show that the `contact` block is as high as the QR code: a `Stack` stretches its items to
the height of the row (`Vertical: stretch`, by default; [Stack](../components/stack.md)). Without outlines, that block
is not seen, because it has neither a background nor a border.

In the PDF, the outlines are marked as decoration, and a screen reader does not read them. Even so, a PDF with `debug`
is not meant to be published.

## The dump

```java
String dump = StxtDump.dump(laidOut.pages(), false);
```

The dump of the notice:

```
Result:
    Page: 1
        Fragment: title
            Rect: 42.52 42.52 334.49 19.2
        Fragment: notice
            Rect: 42.52 69.72 334.49 24
        Fragment: footer
            Rect: 42.52 105.72 334.49 70.87
            Fragment: contact
                Rect: 42.52 105.72 263.62 70.87
            Fragment: qr
                Rect: 306.14 105.72 70.87 70.87
```

- **Only the boxes with an `Id`**, each one inside the box with an `Id` that contains it. With `true` instead of
  `false`, all of them come out.
- **`Rect`** is `x y width height` of the outer edge of the box, in points, from the top left corner of the page. The
  42.52 points of `x` are the 15 mm of the margin.
- **A box split across pages** comes out once on each page, with what it takes up on it.

It serves to check measurements that cannot be appreciated in the image (the QR code measures 70.87 points, its 25 mm)
and to compare two versions of a document: if the dump does not change, the boxes with an `Id` have not moved.

## The errors

If the document has a problem, the conversion stops at the first one. With the QR code at 5 mm instead of 25:

```
line 35: The modules of Barcode qr (line 35) measure 0.135 mm and the narrowest that can be read is 0.25 mm: its content box measures 5 × 5 mm (14.17 × 14.17 pt) and needs at least 9.25 × 9.25 mm (26.22 × 26.22 pt) (or the check BARCODE_SIZE is removed when converting)
```

- **`line 35`** is the line of the document where the problem is: that of the property that fails or, as here, that of
  the element.
- **`line N of the styles`**, instead of `line N`, is a line of the separate styles file. With several styles files,
  `line N of styles 2` is a line of the second one.
- **Boxes are named by their `Id`** (`Barcode qr`) or, without one, by their line (`Box at line 12`). That is why it is
  worth giving an `Id` to what is being adjusted.
- **The code** (`BARCODE_TOO_SMALL`) is given by `e.code()`, and it does not change even if the message changes.

The most common codes and what to do about them are in [Errors](errors.md). From Java, the kind of error (`kind()`)
says whether the problem belongs to the document, to a limit or to the configuration
([Using from Java](java-usage.md#errors)).
