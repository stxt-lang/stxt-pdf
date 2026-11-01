# Layers and rotation

## What is painted on top of what

The content is painted in the order of the document: what comes later goes on top. Inside each box, first the
backgrounds and the borders of everything in the flow and then the texts and the images, so a background never covers a
text of another box. Positioned boxes (`Frame`, `Overlay`, `Page overlay`) are painted afterwards, on top of the flow.

**`Layer`** changes that order. It is an integer accepted by the positioned elements and by the `Item` and `Area`
wrappers:

- **Negative**: the box goes **behind** the flow (a watermark, a page background).
- **Positive**: **in front of** everything (a seal, a label).
- **With equal `Layer`**, the order of the document decides.

```
Page overlay:
    Layer: -1
    Offset: 0
    Block:
        Box:
            Height: 100%
            Background: #FBF7EF
```

A box with `Layer` groups everything it contains: its content is painted together, at the level of that box, even if a
box inside has a higher `Layer`.

## Rotating a box (`Rotate`)

`Rotate` rotates a box: an angle in degrees, clockwise (between −360 and 360, without 0 or whole turns, which do not
rotate anything). It rotates everything the box has: background, border, text, images and what it contains. Rotated text
is still text in the PDF: it can be selected and reads the same. What is rotated has to fit on the page. A rotated box is
not split across pages: if it does not fit in what is left, it moves whole to the next one.

There are two ways of rotating, depending on the box.

### In place: `Block`, `Text`, `Paragraph` and `Image`

A rotated box in the flow **takes up the rectangle that encloses it once rotated**, and what is around it moves aside. A
`Barcode` rotates like an `Image`.
This is what is needed for vertical text, such as the headers of some narrow columns:

```
Document (dev.stxt.pdf):
    Language: en
    Title: Shifts of the week
    Page:
        Paper: A5
        Orientation: landscape
        Margin: 40
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For element: Cell
            Box:
                Padding: 4 6
                Border: 0 0 0.5 0
                Border color: #C8C4BA
        For element: Header cell
            Typography:
                Bold: true
    Section:
        Table:
            Column sizes: * auto auto auto
            Column align: left center center center
            Header rows:
                Row:
                    Cell: Person
                    Cell:
                        Text: Morning
                            Rotate: -90
                    Cell:
                        Text: Afternoon
                            Rotate: -90
                    Cell:
                        Text: Night
                            Rotate: -90
            Body:
                Row:
                    Cell: Ana Ruiz
                    Cell: yes
                    Cell: no
                    Cell: no
                Row:
                    Cell: Pau Vidal
                    Cell: no
                    Cell: yes
                    Cell: yes
```

- **Without `Width`, it measures its text on one line** and does not break it by the width of its column. A long rotated
  paragraph needs a `Width`, which is the width of its lines before rotating.
- **It rotates around its center.** With `Margin: 0 auto` it is centered.
- **It cannot be directly an item of a `Stack` or a `Grid`**, which give it its size. What is rotated is a box inside the
  item.

### Only when painting: `Frame`, `Overlay` and `Page overlay`

A rotated positioned element **does not move anything**: the box takes up its place as if it were not rotated. It is for
what goes on top of everything, such as a seal or a diagonal label:

```
Overlay:
    Offset: 10 10 auto auto
    Rotate: 12
    Box:
        Padding: 4 10
        Border: 1
        Border color: #A32D2D
    Text: DRAFT
        Typography:
            Color: #A32D2D
            Bold: true
```

- **`Rotate origin`** is the point around which it rotates: one or two of `top`, `bottom`, `left`, `right` and `center`
  (by default, the center), or two measurements from the top left corner of the box. `Rotate origin: top left` rotates
  around that corner, and `Rotate origin: 20 50%`, around the point 20 pt to its right and at half height.
- A vertical text in the margin of the page is a rotated `Page overlay` that contains it.

More detail in the [reference](../../reference/common/layers-and-rotation.md).
