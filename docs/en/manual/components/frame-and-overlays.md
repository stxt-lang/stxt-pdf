# `Frame`, `Overlay` and `Page overlay`

The three **positioned** elements are blocks (they stack their children like a `Block`) that are placed with `Offset`, an
offset of 1 to 4 values: top, right, bottom, left.

| Element | What it does |
|---|---|
| `Frame` | It goes in its place, like any box, and is then **shifted** without moving the others |
| `Overlay` | It **leaves the flow**: it takes up no space and is placed at an exact position relative to the `Frame`, `Overlay` or `Page overlay` that contains it (or, if there is none, the page area) |
| `Page overlay` | Like `Overlay`, but relative to the page area of **each page**, and it is repeated on all of them: headers, footers, watermarks |

```
Document (dev.stxt.pdf):
    Language: en
    Title: Quote
    Page:
        Paper: A4
        Margin: 60 50
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For element: Page overlay
            Typography:
                Size: 8
                Color: #5F5E5A
    Page overlay:
        Offset: -36 0 auto 0
        Text: Nogal Workshop · Quote 2026-014
    Page overlay:
        Pages: not-first
        Offset: auto 0 -36 0
        Text: Page {page} of {pages}
            Typography:
                Align: right
    Section:
        Frame:
            Box:
                Height: 120
                Background: #F1EFE8
            Paragraph: Quote for the kitchen on Calle Mayor.
            Overlay:
                Offset: 10 10 auto auto
                Rotate: 8
                Box:
                    Padding: 4 8
                    Border: 1
                    Border color: #A32D2D
                Text: DRAFT
                    Typography:
                        Color: #A32D2D
                        Bold: true
```

The header appears on every page and the footer on all of them except the first. Both go in the margin of the page (hence
the negative offset). The stamp is placed in the top right corner of the `Frame`, rotated.

## `Offset`

Each side of the `Offset` is the distance from that edge of its container, and `auto` means "not set". For an `Overlay` or
a `Page overlay`:

| Written | Result |
|---|---|
| `Offset: 10 10 auto auto` | 10 from the top edge and 10 from the right edge, with its own size |
| `Offset: auto 0 0 0` | At the bottom, from side to side |
| `Offset: 0` | Stretched over the whole container |
| `Offset: auto` (or nothing) | Where it would have been in the flow |

If a side and its opposite are both set, the box is stretched between the two. If it also has its size on that axis, it
is set twice and it is an error, except with `Margin: auto` on that axis, which centers it. Percentages are of the width
or the height of the container.

A `Frame` cannot set top and bottom at the same time, nor left and right: it is shifted from one of the two. A `Frame`
without `Offset` does not move: it serves as the box the `Overlay` elements inside it are placed against. Once shifted,
a `Frame` can take up the margins of the page, but not go outside it.

## Properties

| Property | Elements | What it does |
|---|---|---|
| `Offset` | All three | The offset |
| `Layer` | All three | What is painted on top ([Layers and rotation](../common/layers-and-rotation.md)) |
| `Rotate`, `Rotate origin` | All three | Rotating the box ([Layers and rotation](../common/layers-and-rotation.md#rotating-a-box-rotate)) |
| `Pages` | `Page overlay` | Which pages it appears on |

They also accept `Id`, `Style`, `Link`, `Box` and `Typography`. `Frame` also accepts `Break` and `Isolate margins`.
`Overlay` and `Page overlay` do not accept `Break`, because they are not split.

## `Page overlay`

- It takes up no space. The `Margin` of the page leaves room for the headers and footers, which are placed in the margin
  with a negative `Offset`.
- A `Page overlay` of the `Document` appears in every section (except those that say `Inherit page overlays: false`). One
  of a `Section` appears only on the pages of that section.
- What is inside a `Page overlay` is decoration for the tagged PDF: it is not read as content, it cannot have headings and
  it cannot be linked to or referenced.

### `Pages`

| Value | Pages |
|---|---|
| `all` | all of them (the default) |
| `first` | the first one |
| `not-first` | all except the first one |
| `odd`, `even` | the odd ones, the even ones |
| `last` | the last one |

Pages are counted from the first one of their document or their section, in order, regardless of the number printed on
them.

## Things to know

- An `Overlay` or a `Page overlay` can take up the margins of the page, but cannot go off the paper: that is an error.
- An `Overlay` is not split across pages: it goes whole on the page where it falls.
- A `Stack` or a `Grid` is placed at an exact position inside an `Overlay`.

More detail in the [reference](../../reference/components/frame-and-overlays.md).
