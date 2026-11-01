# `Stack` and `Item`

A `Stack` places its items in lines: one after another along its **main axis**, the horizontal or the vertical one according to
`Direction`, and aligned along the **cross axis**, the other one. Each in-flow child is an `Item`, which wraps the element that acts as
the item. It also accepts `Overlay` and `Page overlay`.
The `Stack` accepts `Id`, `Style`, `Link`, `Box`, `Typography` and `Break`, and goes wherever an in-flow element goes. Unlike
a `Block`, which stacks its children vertically with its width, a `Stack` distributes among its items the space
that is left over or missing.

> **Decision:** the element is called `Stack`, as in design tools, and its names speak of layout:
> `Base size` is the starting size of an item, `Line distribution` the distribution of the lines, and `justify` and
> `distribute` the distributions that leave the ends at the edges or all the gaps equal. *Rejected alternative:*
> `Flex`, `Basis`, `Lines`, `between` and `evenly`, which sound like a styling language and not like layout.

```
Stack:
    Gap: 10
    Item:
        Grow: 1
        Text: Grows
    Item:
        Block:
            Box:
                Width: 100
```

## Properties of the `Stack`

| Property | Values | Default | Notes |
|---|---|---|---|
| `Direction` | `horizontal`, `vertical` | `horizontal` | The main axis: in `horizontal` the items go side by side, and in `vertical`, one below another. |
| `Wrap` | boolean | `false` | Allows the items to be distributed over several lines. |
| `Gap` | 1 or 2 values (number, %) | `0` | One value for both axes, or `rows columns`, only with `Wrap: true` (`GAP_WITHOUT_WRAP`). |
| `Horizontal` | on the main axis: `start`, `end`, `center`, `justify`, `around`, `distribute`; on the cross axis: `start`, `end`, `center`, `stretch` | `start` on the main axis; `stretch` on the cross axis | With `Direction: horizontal`, the distribution of the leftover space. With `vertical`, the alignment of the items within their line. |
| `Vertical` | as `Horizontal` | as `Horizontal` | With `Direction: vertical`, the distribution of the leftover space. With `horizontal`, the alignment of the items within their line. |
| `Line distribution` | `start`, `end`, `center`, `justify`, `around`, `stretch` | `stretch` | Distribution of the lines in the container. Only with `Wrap: true` (`LINE_DISTRIBUTION_WITHOUT_WRAP`). |

> **Decision:** the axes are named by their direction on the page, `Horizontal` and `Vertical`, as in `Grid`, and not by their
> role. What each one does is set by `Direction`: the one on the main axis distributes the leftover space and the one on the cross axis aligns the items,
> and a value that does not belong to its role is an error (`VALUE_NOT_ALLOWED`): `stretch` on the main axis, and `justify`, `around` or
> `distribute` on the cross axis. *Rejected alternative:* `Main axis` and `Cross axis`, which do not change meaning with `Direction`
> but force thinking in abstract axes.

> **Decision:** without `Wrap` there is a single line, so of a two-value `Gap` only one would act, the gap between
> items (that of the columns in horizontal, that of the rows in vertical): it is an error (`GAP_WITHOUT_WRAP`), like `Line distribution` without
> `Wrap`. A single value is valid, because it is the gap between items and, if there are several lines, also between lines. That is why
> `Gap: 5%` in a single-line horizontal `Stack` without a definite height is not an error: the gap between rows, which would need that
> height, is not used. *Rejected alternative:* accepting both values and not using the extra one, which leaves in the document a
> measurement without effect.

## `Item`

It contains **exactly one in-flow element** (`SINGLE_ELEMENT`), which is the item: its `Box`, its `Typography`, its `Id` and
other properties go on that element. The `Item` only adds how it takes part in the `Stack`. It is not a box and does not accept `Id`
([Structure](../common/structure.md#elements)).

| Property | Values | Default | Notes |
|---|---|---|---|
| `Grow` | number ≥ 0 | `0` | Share of the leftover space it receives. |
| `Shrink` | number ≥ 0 | `1` | Share of the deficit it gives up, weighted by `Base size`. |
| `Base size` | number, %, `auto` | `auto` | Starting size on the main axis. |
| `Horizontal`, `Vertical` | `start`, `end`, `center`, `stretch` | that of the container | The alignment of the item on the cross axis, which overrides that of the container: `Vertical` in a horizontal `Stack`, `Horizontal` in a vertical one. The one on the main axis is an error (`ITEM_MAIN_AXIS`): there the `Stack` distributes, and the item grows or shrinks with `Grow` and `Shrink`. |
| `Layer` | integer | `auto` | Stacking order of the item ([Layers and rotation](../common/layers-and-rotation.md)). |

## Algorithm

1. Available space: the content box of the container on the main axis.
2. **Base** of each item: `Base size` if it has a value. If it is `auto`, the declared size on the main axis, and if there is none
   either, its natural width.
3. **Hypothetical size:** the base limited by the minimum and the maximum of the item. With `Min width: auto` (`Min height: auto` in
   vertical), the minimum is the **size of its content**: the narrowest width in horizontal and the height of the content in vertical, without
   exceeding the maximum of the item and never larger than its declared `Width` (or `Height`).
4. **Lines:** without `Wrap`, a single one. With `Wrap`, the items are added to the line while their hypothetical sizes and the
   gaps fit.
5. **Flexible sizes** (per line), described below.
6. **Cross size:** layout of each item with its final main size (in horizontal, the text is redistributed with the final
   width). The line measures what the tallest item does. Without `Wrap` and with a definite cross size on the container, the only line
   measures what the container does.
7. **Stretch:** the items with `stretch` and without a declared cross size come to measure what the line does, and their layout is repeated.
   In vertical, if the container has a single line, these items are measured from the start with the width of the container.
   With `Wrap`, they are measured with their fit to the content and then stretched to the width of their line without recomputing their height.
8. **Align** on the main axis, on the cross axis and the lines in the container.

With an `auto` cross size, the container measures the sum of its lines plus the gaps.

### Distribution of space

Free space = available − sum of hypothetical sizes − gaps.

- **Positive:** each item receives a share proportional to its `Grow`.
- **Negative:** each item gives up a share proportional to **`Shrink × Base size`**, without its margins.
- An item that hits its minimum or maximum is **frozen** at that limit and the distribution is repeated among the rest.
- If the sum of the factors of the non-frozen items is less than 1, only that fraction of the free space is distributed.

> **Decision:** margins do not count when shrinking: two items with `Base size: 300`, one with a margin of 10 on each side, in 515,
> give up the same (52.5 each). *Rejected alternative:* weighting with the base plus the margins.

### Alignment on the main axis

With a leftover space `L` and `n` items (it only acts if nothing grows):

| Main axis (`Horizontal` or `Vertical`) | Before the first | Between items | After the last |
|---|---|---|---|
| `start` | 0 | gap | L |
| `end` | L | gap | 0 |
| `center` | L/2 | gap | L/2 |
| `justify` | 0 | gap + L/(n−1) | 0 |
| `around` | L/2n | gap + L/n | L/2n |
| `distribute` | L/(n+1) | gap + L/(n+1) | L/(n+1) |

A negative leftover space after shrinking means that the items do not fit: it is an error
([Overflow](../common/overflow.md)). The `auto` margins of an item are an error: alignment is done with
`Horizontal` and `Vertical`.

## Behaviors to keep in mind

- In a vertical `Stack`, `Grow` needs the container to have a definite height ([Values](../common/values.md#definite-height)):
  otherwise, there is no free space to distribute and it is an error (`GROW_WITHOUT_EFFECT`).
- `Base size: 0` with `Grow: 1` distributes all the space (equal columns). `Base size: auto` with `Grow: 1` only distributes the
  space left over beyond the size of the content.
- An item does not shrink below the narrowest width of its content unless it declares `Min width: 0`. A declared `Width` does not
  stop it: an item with `Width: 400` and little content shrinks if it does not fit.
- A stretched item does **not** give a definite height to its children ([Values](../common/values.md#definite-height)).
- An `Image` or a `Barcode` is not stretched on the cross axis of a row: its height comes from its proportions
  ([Image](image.md#sizing)).

## Intrinsic sizes

In horizontal and without `Wrap`, the narrowest width is the sum of the narrowest widths of the items plus the gaps, and with `Wrap`, the maximum of
them. The natural width is the sum of the natural widths plus the gaps. In vertical, both are the maximum of the items. The
intrinsic sizes of each item are used as they are, without `Base size`, `Grow` or `Shrink` ([The box](../common/box.md#intrinsic-sizes)).

## Breaks

In vertical, the break is made between items, and in horizontal, between lines. A line or an item that does not fit on a whole page
breaks in parallel ([Breaks](../common/breaks.md#breaks-in-stack-grid-and-table)). A vertical `Stack` with `Wrap` does not
break.

## Positioned elements

The static position of an `Overlay` or a `Page overlay` that is a child of a `Stack` is the top left corner of its content
box ([positioned elements](frame-and-overlays.md#overlay)).

## What is not supported

- A visual order different from the document order, reversed directions and baseline alignment.
- `auto` margins on the items: they are errors.
