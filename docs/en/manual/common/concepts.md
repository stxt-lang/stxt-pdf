# Concepts

## Pages and points

A document is always laid out in **pages** of a fixed size, like a printed PDF. The size and the margins of the page are
given by `Page` ([Document](../components/document.md)). What lies inside the margins is the **page area**: the place
where the content goes.

All measurements are in **points** (pt): 1 pt = 1/72 of an inch, about 0.35 mm. An A4 measures 595 × 842 pt. With
`Margin: 40`, the page area measures 515 × 762. Measurements are written as numbers: `Width: 200` is 200 points.

## Boxes

Each element of the document is a **box**: a rectangle with its content. A box has, from the inside out:

```
┌──────────── margin ────────────┐
│  ┌─────────── border ────────┐ │
│  │  ┌──────── padding ─────┐ │ │
│  │  │       content        │ │ │
│  │  └──────────────────────┘ │ │
│  └───────────────────────────┘ │
└────────────────────────────────┘
```

- **Content**: the text, the image or the boxes inside.
- **Padding** (`Padding`): the space between the content and the border. It has the background of the box.
- **Border** (`Border`): the line around it.
- **Margin** (`Margin`): the space that separates the box from the ones around it. It is always transparent.

`Width` and `Height` measure the box **with** its padding and its border, without the margin: a box with `Width: 200`
and `Padding: 10` takes up 200 points of width and leaves 180 for the content. More in [The box](box.md).

## Elements and properties

An element accepts only the properties that make sense in it, and the engine checks this when reading: `Column sizes` in
a `Stack` is an error, because columns belong to a `Grid`. The properties of any box go in three **groups**:

| Group | What it has | Inherited |
|---|---|---|
| `Box` | Size, margins, padding, border, background and corners | No |
| `Typography` | Font, size, line height, alignment, color and language of the text | **Yes**: what a box declares applies to everything it contains |
| `Break` | How the box is broken across pages | No |

```
Paragraph: Quarterly results
    Box:
        Margin: 0 0 16 0
    Typography:
        Size: 20
        Bold: true
    Break:
        Keep with next: true
```

Outside the groups go the properties of the element itself: its `Id`, its `Style`, the columns of a table or the offset
of a positioned box.

**Inheritance.** Only `Typography` is inherited. If a box says `Size: 12`, all the text inside it measures 12 unless
something else says otherwise. That is why the font of the document is declared once, in the `For element: Document`
rule of `Styles`, and each element declares only what changes.

## How boxes are placed

Each element places its children in one way:

| Element | Places its children |
|---|---|
| `Block` (and almost all the others: sections, headings, lists, cells...) | **One below another**, each with the full available width |
| `Stack` | In a **row** (or a column), sharing the space |
| `Grid` and `Table` | In a **grid** of rows and columns |
| `Columns` | In **columns** through which the content flows |
| `Text` and `Paragraph` | They have no children: they break their text into **lines** |

Placing the boxes one after another according to these rules is the **flow**. Width goes from the outside in: a box
takes up the width its parent leaves it (or the one it declares). Height goes from the inside out: a box measures what
its content measures (or what it declares).

**Outside the flow** are the **positioned** boxes: `Overlay` is placed at an exact position without taking up space,
`Page overlay` is repeated on each page (headers and footers) and `Frame` is shifted without moving the others
([positioned elements](../components/frame-and-overlays.md)).

**The reference container** of a box is the rectangle against which its percentages and its offsets are measured. For a
box in flow, it is the content space of its parent. For a positioned one, it depends on which one it is
([positioned elements](../components/frame-and-overlays.md)). `Width: 50%` is half the width of its reference container.

## Sections

All the content goes inside one or more **sections** (`Section`). A section always starts on a new page and can have
another page size, other margins, other headers and footers and another numbering: a full-bleed cover without a page
number, the body numbered from 1 and a landscape appendix are three sections
([Document](../components/document.md#section)).

## Pages and breaks

When the content does not fit on a page, the engine **breaks** it and continues on the next page. It breaks between
boxes, between lines of a text or between rows of a table. When choosing where, it avoids leaving a single line of a
paragraph at the end of a page and separating a title from what follows it, and it respects what `Break` says
([Pages and breaks](pages-and-breaks.md)).

## Strict and predictable

Anything that has no effect, contradicts something else or does not fit is an **error**, with a code, the line of the
document and a message that says how to fix it ([Errors](errors.md)). For example:

- A property in an element that does not accept it.
- A `Margin: auto` that cannot center anything.
- A percentage of a height that is not known.
- A text that does not fit in its box, or an image wider than the page.

Nothing overflows its box or the page, and the same document always gives the same layout.

## No hidden styles

No element has a default look that is not visible in the document. A `Heading 1` is not larger than a `Paragraph` unless
a style says so, a list has no indent unless it is given one, and a link is not blue. The only thing the engine does on
its own is avoid breaking the page right after a title.
