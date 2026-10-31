# `Block`

A box that stacks its children vertically. It accepts `Id`, `Style`, `Link`, `Box`, `Typography`, `Break`, `Isolate margins` and
`Rotate`, and any element as a child. It goes wherever an in-flow element goes. With `Rotate`, it rotates with everything it contains
and takes up the rectangle that encloses it ([Layers and rotation](../common/layers-and-rotation.md#rotation-in-flow-block-text-paragraph-and-image)).

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

## Intrinsic sizes

The narrowest width of a `Block` is the maximum of the narrowest widths of its children, and its natural width, the maximum of
their natural widths ([The box](../common/box.md#intrinsic-sizes)).

## Breaks

A `Block` breaks between its children, or inside the child that does not fit ([Breaks](../common/breaks.md)).

## Accessibility

A `Block` groups its content without announcing itself: a screen reader reads what it contains ([The PDF](../common/pdf.md#accessibility)).

## What is not supported

- Boxes that float to one side of the text, or text that wraps around another box.
- Clipping of what sticks out (what sticks out is an [overflow](../common/overflow.md)).
