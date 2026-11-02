# `Block`

A box that stacks its children vertically. It accepts `Id`, `Style`, `Link`, `Box`, `Typography`, `Break`, `Isolate margins`,
`Rotate`, `Side` and `Clear`, and any element as a child. It goes wherever an in-flow element goes. With `Rotate`, it rotates with everything it contains
and takes up the rectangle that encloses it ([Layers and rotation](../common/layers-and-rotation.md#rotation-in-flow-block-text-paragraph-and-image)).
With `Side`, it is a [side box](#side-boxes-side): the text that follows it runs beside it.

```
Block:
    Box:
        Padding: 10
        Background: #F1EFE8
    Paragraph: First paragraph.
    Paragraph: Second paragraph.
```

## Block flow

It is how a `Block`, each section (the page area), `Frame`, `Overlay`, `Page overlay`, the
headings, `List`, `List item`, `Markdown`, `Table of contents`, a `Cell` with elements and each column of a `Columns` place their in-flow children. The in-flow
children are stacked vertically:

1. **Width.** With `Width: auto`, the box takes the width of the reference container minus its horizontal margins. With
   a known width and both horizontal margins `auto`, the leftover space is split between them and the box is **centered**,
   and if only one is `auto`, that one absorbs the leftover space ([The box](../common/box.md#auto-margins)).
2. **Position.** The box is placed at the position of the vertical cursor, after applying its top margin merged with the
   previous one.
3. **Height.** Its layout is computed and the cursor moves forward.

With `Height: auto`, the height of the container is the final position of the cursor plus its padding and its border. **Width is
propagated from parents to children, and height, from children to parents.** An `Overlay` or a `Page overlay` among the children takes up no space
([positioned elements](frame-and-overlays.md)).

## Margin merging

Adjacent **vertical** margins in block flow merge:

- between consecutive siblings;
- between a parent and its first or last child, if the parent has no border or padding on that side and does not isolate margins
  (the child's margin "comes out" of the parent);
- through empty boxes (no height, border, padding or content).

The result of merging several margins is **the largest positive one plus the most negative one**: `20` and `30` give 30, `20` and `−5`
give 15, and `−10` and `−4` give −10. At a page break, the margins that touch the break are truncated
([Breaks](../common/breaks.md#margins-at-breaks)).

The height of the content of a box does not go below 0: if negative margins take away more than the children measure,
the box measures its border and its padding, and the child that sticks out at the bottom is `BOX_OVERFLOW` at the line
of the negative `Margin` ([Overflow](../common/overflow.md)).

> **Decision:** the height of the content is never negative. *Rejected alternative:* letting the box measure less than
> its border and its padding, which paints a box lower than its own border.

## Boxes that isolate margins

A box that isolates margins stops their merging: the margins inside do not come out. These isolate them: each section, `Overlay` and `Page overlay`, the items
of `Stack` and `Grid` (also the cells of a `Table`), the columns of a `Columns`, and the `Block` and `Frame` with
`Isolate margins: true`. The margins of the items of `Stack` and `Grid` never merge.

| Property | Values | Default | Notes |
|---|---|---|---|
| `Isolate margins` | boolean | `false` | Isolates the margins of the box: stops margin merging with the children. Only in `Block` and `Frame`, and not in an item of `Stack` or `Grid` (`ISOLATE_MARGINS_IN_ITEM`). |

> **Decision:** `Isolate margins` exists only where it can have an effect: `Overlay`, `Page overlay` and the items already isolate, and `Stack`, `Grid`
> and `Text` have no children in block flow. In a `Block` or a `Frame` that is an item, `Isolate margins: true` is an error.

## Side boxes (`Side`)

An `Image`, a `Barcode` or a `Block` in block flow with `Side: left` or `Side: right` is a **side box**: it is placed at
that side of the container, at the position the flow has when it is reached, and takes no vertical room. The siblings
that follow it start where they would start without it and, while they overlap it vertically, the text **shortens its
lines** beside it. It is the figure beside the text of a manual, or the photo in a column of a newsletter.

```
Image: examples/newsletter/trail.jpg
    Side: left
    Alt: A path between meadows with mountains in the background
    Box:
        Width: 180
        Margin: 0 12 6 0
Paragraph: The text of this paragraph, and of the ones that follow while the photo reaches their height, runs at its right.
```

- **The margin is the separation.** What is taken from the text is the margin box of the side box: `Margin: 0 12 6 0`
  leaves 12 between the photo and the lines and 6 below. An `auto` margin has no effect and is an error
  (`MARGIN_AUTO_WITHOUT_EFFECT`).
- **What runs beside it.** The `Text` and `Paragraph` elements that follow it and, with the text they hold, the
  `Block` elements, the headings, the `Markdown` elements and the lists: a line is shortened if some part of its height
  falls at the height of the box. A `List` only runs beside a side box on the right: its markers hang at its left,
  where one on the left would be, so with that one it goes below. Everything else **goes below** the side boxes: a
  `Table`, a `Stack`, a `Grid`, a `Columns`, a `Table of contents`, a `Frame`, a rotated or clipped box, preformatted
  text and a text, block or heading with `Background` or `Border`, which would be painted over the box. What has
  `Clear: true` goes below too.
- **`Clear`.** `Clear: true` on a `Text`, a `Paragraph`, a `Block`, a heading, a `Markdown`, an `Image` or a `Barcode`
  moves it below the side boxes before it, instead of running beside them. Without a side box before it, it is an
  error (`CLEAR_WITHOUT_SIDE`).
- **A word that does not fit** beside the box leaves that line empty and goes to the next one, up to the first line
  with the whole width; it is an overflow only if it does not fit there either (`TEXT_OVERFLOW`). With
  `Line breaking: balanced`, the line breaking takes the width of each line into account.
- **Several side boxes.** A second side box on the same side that overlaps the previous one goes below it; one on the
  other side goes at its height, and the text runs between the two.
- **The height of the container** reaches the bottom of its side boxes if the text ends before, and then the bottom
  margin of its last child does not come out of it.
- **Where it goes.** In block flow: a section, a `Block`, a `Frame`, an `Overlay`, a `Page overlay`, a `Cell` with
  elements, a `List item`, a column of a `Columns`. Not as an item of a `Stack` or a `Grid`, which places the box itself
  (`SIDE_NOT_IN_BLOCK_FLOW`). A `Block` with `Side` needs `Width` or `Max width` (`SIDE_WITHOUT_WIDTH`).
- **Breaks.** A side box is not split: if it does not fit in what is left of the page it moves to the next one, and
  the first line that runs beside it goes with it (the break between the box and the child that follows it is as bad
  as separating two boxes with `Keep with next`). Side boxes end with the page: the text that goes on on the next page
  has the whole width and is broken into lines again from where it was cut. `Keep together` and `Edges` on a side
  `Block` are an error (`BREAK_IN_SIDE_BOX`), and a `Break` inside it too (`BREAK_IN_UNBREAKABLE`).

| Property | Values | Default | Notes |
|---|---|---|---|
| `Side` | `left`, `right` | none | Makes the box a side box. In `Image`, `Barcode` and `Block` (with `Width` or `Max width`). |
| `Clear` | boolean | `false` | Moves the box below the side boxes before it. In `Text`, `Paragraph`, `Block`, the headings, `Markdown`, `Image` and `Barcode`. |

> **Decision:** the side box is the only thing that makes text run around another box; it is placed at its place in
> the flow and does not move. *Rejected alternative:* floats as a general placement mechanism, which `Stack`, `Grid`,
> `Frame` and `Overlay` already cover.

> **Decision:** what is not text goes below the side box, and so does what has a background or a border, because it
> would be painted over it. *Rejected alternative:* narrowing a table or a `Stack` beside the box, or painting the side
> box over what runs beside it.

> **Decision:** the side box and the first line that runs beside it go together, and side boxes end with the page: the
> text that goes on on the other page is broken again at the whole width. *Rejected alternative:* keeping the lines of
> the next page short as if the box were there.

## Intrinsic sizes

The narrowest width of a `Block` is the maximum of the narrowest widths of its children, and its natural width, the maximum of
their natural widths ([The box](../common/box.md#intrinsic-sizes)).

## Breaks

A `Block` breaks between its children, or inside the child that does not fit ([Breaks](../common/breaks.md)).

## Accessibility

A `Block` groups its content without announcing itself: a screen reader reads what it contains ([The PDF](../common/pdf.md#accessibility)).

## What is not supported

- A table, a `Stack`, a text with background or border, or a list beside a side box on the left, running beside the
  box: they go below it ([Side boxes](#side-boxes-side)). Nor a side box as an item of a `Stack` or a `Grid`.
- Clipping of what sticks out (what sticks out is an [overflow](../common/overflow.md)).
