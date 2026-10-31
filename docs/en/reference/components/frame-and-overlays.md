# `Frame`, `Overlay` and `Page overlay`

The three **positioned elements** are blocks: they stack their children in block flow ([Block](block.md)). To position
a `Stack` or a `Grid`, it is put inside one of them. They accept `Id`, `Style`, `Link`, `Box` and `Typography`, and
`Frame`, also `Break` and `Isolate margins`.

| Property | Values | Default | Elements | Notes |
|---|---|---|---|---|
| `Offset` | 1 to 4 values (number, %, `auto`) | `auto` | All three | Top, right, bottom, left ([Values](../common/values.md#four-side-shorthands)). |
| `Layer` | integer | `auto` | All three | Stacking order ([Layers and rotation](../common/layers-and-rotation.md)). Also in `Item` and `Area`. |
| `Pages` | `all`, `first`, `not-first`, `odd`, `even`, `last` | `all` | `Page overlay` | The pages on which it appears ([Pages](#pages)). |
| `Rotate`, `Rotate origin` | described in [Layers and rotation](../common/layers-and-rotation.md#rotation-rotate) | none | All three | Rotates the box when it is painted. Also in `Image`. |

`Offset`, `Layer`, `Pages` and `Rotate` are properties of the element: they do not go in `Styles` and are not inherited.

> **Decision:** `Frame` is an element and not a property of any box: this way `Offset` and `Layer` exist only in the
> positioned elements and in `Item`/`Area`, and the schema detects their misuse without rules of its own. A `Frame` without `Offset`
> marks the reference container of the `Overlay` elements. *Rejected alternative:* an `Offset` on any in-flow element,
> which would need another way to mark the reference container without offsetting.

> **Decision:** the positioned elements are blocks, and a positioned `Stack` or `Grid` is written inside
> (`Overlay` > `Stack`), with one more box. *Rejected alternative:* making them wrappers of an element, like `Area`, which
> would read worse in the usual case (a seal, a header).

> **Decision:** `Overlay` and `Page overlay` do not accept `Break`, because they do not break and are not in the flow of the page. A
> `Break` written on an element inside is an error (`BREAK_IN_UNBREAKABLE`, [Breaks](../common/breaks.md#forced-breaks)).
> One that comes from a rule is not, so that `For element: Paragraph` with `Keep with next: true` does not give an error on each paragraph of a footer.
> *Rejected alternative:* checking also what comes from the rules.

## `Frame`

The box is placed like any in-flow box and then shifted visually according to `Offset`. Its original space is
kept and its siblings do not move. Declaring top and bottom, or left and right, at the same time is an error
(`OFFSET_CONFLICT`): one of the two has to stay `auto`. A `Frame` without `Offset` is not shifted and serves as the reference
container of its `Overlay` descendants. The percentages of `Offset` are resolved against the width or the height of the
reference container. A vertical one needs its height to be definite ([Values](../common/values.md#definite-height)).
A `Frame` is checked in its place, without its offset ([Overflow](../common/overflow.md)).

## `Overlay`

The box leaves the flow: it takes up no space and does not contribute to the `auto` height of its parent. Its reference container is the
padding box of the nearest positioned element among its ancestors, or the page area ([The box](../common/box.md#the-reference-container)).
On each axis, the following holds:

```
start + start margin + size + end margin + end = size of the reference container
```

where *start/end* are left/right or top/bottom of `Offset`. Depending on which terms are `auto`:

| `auto` | Result |
|---|---|
| start, size and end | start = **static position**, automatic size, and end is solved for |
| none, with both margins `auto` | the margins share the leftover space: **centered** |
| none, without `auto` margins | **error** (`OVERCONSTRAINED`): the position is fixed twice |
| start and size | automatic size and start is solved for (anchored to the end) |
| start and end | start = static position and end is solved for |
| size and end | automatic size and end is solved for |
| start only | start is solved for |
| size only | it is solved for: the box **stretches** between start and end |
| end only | end is solved for |

An `auto` margin only makes sense when start, size and end are declared (it centers the box or, if only one
of the two is `auto`, it absorbs the leftover space). In the other cases it is an error. The **automatic size** is the fit to the content
(`fit`) horizontally and the height of the content vertically. The **static position** is where the
box would have been if it were in flow:

- in block flow, the vertical cursor plus the merged margins of the previous boxes, without merging with the top
  margin of the box itself, which is added afterwards according to the equation;
- inside a `Stack` or a `Grid`, the top left corner of its content box.

An `Overlay` is placed when its reference container already has its size, with the in-flow content placed. It is
painted at its place in the document among its siblings, even if its reference container is higher up ([Layers and rotation](../common/layers-and-rotation.md#painting-order)). It does not break: it is placed on the page where
its position falls, and if its reference container is broken, it is resolved against the fragment on that page
([Breaks](../common/breaks.md#what-is-not-split)). It can occupy the margins of the page, but not go outside it
(`BOX_OVERFLOW`).

## `Page overlay`

Like `Overlay`, but its reference container is the page area of the page, and **it is painted on the pages that
`Pages` says**, all of them by default (headers, footers, watermarks). A `Page overlay` that is a child of `Document` appears in all the sections that
inherit it, and one that is a child of a `Section`, only in its own ([Document](document.md#page-overlay-of-the-document-and-of-the-section)).
`Page overlay` elements do not reserve space: the author must leave room with the `Margin` of `Page`. To place them in the margin of the
page, negative offsets are used (`Offset: -30 0 auto 0`). If a `Page overlay` depends on its static position
(`auto` offsets), the one on the first page on which the flow that contains it appears is taken, and it is repeated unchanged on
all the pages where it appears. What is inside a `Page overlay` is decoration of the page and not content: a screen reader does not
read it, and it cannot have headings (`HEADING_IN_PAGE_OVERLAY`) or be the target of a link or a reference.

> **Decision:** a `Page overlay` is accepted in any container, not only as a child of `Document` or of a section: its static
> position is that of its place in the document. *Rejected alternative:* only as a child of `Document` and of `Section`, which is
> where a header or a footer makes sense.

### `Pages`

A property of the `Page overlay` only (on another element it is a schema error, and so is a value that is not in the list). It counts
the pages from 1 **within its scope**: those of the whole document, in physical order, if the `Page overlay` is a child of
`Document`, and those of its section, if it is a child of a `Section` (`first` is the first page of the section). It is not the
printed number of `{page}`, which can restart or not exist ([Document](document.md#page-numbering)):

| Value | Pages |
|---|---|
| `all` | all (default) |
| `first` | only the first |
| `not-first` | all except the first |
| `odd` / `even` | the odd ones / the even ones |
| `last` | only the last |

It is a painting condition and not a space condition: a `Page overlay` reserves no room either where it appears or where it does not, so the flow is the
same with it or without it. A full-bleed cover, with another margin and without numbering, is a section. A `Page overlay` that does not appear on
any page (`not-first` in a single-page document) is valid and is not checked (`BOX_OVERFLOW` only looks at those that
appear). If a `Page overlay` contains another, the inner one appears only where both appear, each one with its `Pages`.

> **Decision:** `Pages` is a property of the `Page overlay` and not a system of named pages. In a single-page
> document, `first` and `last` coincide. *Rejected alternative:* named pages and rules by page type.

> **Decision:** a `Page overlay` does not push the flow on the pages where it appears. *Rejected alternative:* reserving its space,
> which breaks the model of the out-of-flow box.

## What is not supported

- Boxes that stay stuck to the edge while the pages turn inside their container, or that float to one side of the
  text.
- Positioning a `Stack` or a `Grid` without a positioned element around it.
