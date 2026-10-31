# `Stack`

A `Stack` places its items **in a row** (or in a column) and shares out among them the space that is left over or missing.
It is used to put things side by side: a logo and some details, three figures, a price to the right of a dish.

```
Document (dev.stxt.pdf):
    Language: en
    Title: Figures
    Page:
        Paper: A4
        Margin: 50
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For style: figure
            Box:
                Padding: 10
                Background: #F1EFE8
                Corner radius: 4
            Typography:
                Align: center
    Section:
        Stack:
            Gap: 12
            Item:
                Grow: 1
                Base size: 0
                Text: 12% more sales
                    Style: figure
            Item:
                Grow: 1
                Base size: 0
                Text: 3 new stores
                    Style: figure
            Item:
                Grow: 1
                Base size: 0
                Text: 98% of customers satisfied
                    Style: figure
        Stack:
            Horizontal: justify
            Box:
                Margin: 20 0 0 0
            Item:
                Text: Mushroom risotto
            Item:
                Text: €14.50
```

At the top, three items of the same width. At the bottom, a dish on the left and its price on the right.

## `Item`

Each child of a `Stack` goes inside an `Item`, which wraps **exactly one element**. The `Item` says how it takes part in the
`Stack`, and the rest (size, margins, `Id`, style) goes on the element inside.

| Property of the `Item` | Default | What it does |
|---|---|---|
| `Grow` | `0` | What share of the space left over it receives. With `Grow: 1` on all of them, it is shared out equally |
| `Shrink` | `1` | What share of the missing space it gives up |
| `Base size` | `auto` | The starting size: `auto` is its size (the declared one or that of its content) and `0` makes all the space be shared out according to `Grow` |
| `Horizontal`, `Vertical` | that of the `Stack` | Its alignment on the other axis: `Vertical` in a `Stack` in a row, `Horizontal` in one in a column. In the direction of the `Stack` it is an error: there the item grows or shrinks with `Grow` and `Shrink` |
| `Layer` | `auto` | What is painted on top ([Layers and rotation](../common/layers-and-rotation.md)) |

## Properties of the `Stack`

| Property | Values | Default | What it does |
|---|---|---|---|
| `Direction` | `horizontal`, `vertical` | `horizontal` | In a row (`horizontal`) or in a column (`vertical`) |
| `Gap` | 1 or 2 measurements | `0` | The gap between items, and between lines if there are several. With `Wrap`, it can have two: between rows and between columns |
| `Wrap` | `true`, `false` | `false` | Whether the items that do not fit move to another line |
| `Horizontal` | `start`, `end`, `center`, `justify`, `around`, `distribute`. In a column: `start`, `end`, `center`, `stretch` | `start`. In a column: `stretch` | In a row, where what is left over goes: at the start, at the end, on both sides or between the items (`justify`). In a column, how the items are aligned: `stretch` stretches them to the width of the `Stack` |
| `Vertical` | `start`, `end`, `center`, `stretch`. In a column: `start`, `end`, `center`, `justify`, `around`, `distribute` | `stretch`. In a column: `start` | The reverse of `Horizontal`. In a row, how the items are aligned (`stretch` stretches them to the height of the row). In a column, where what is left over goes |
| `Line distribution` | `start`, `end`, `center`, `justify`, `around`, `stretch` | `stretch` | With `Wrap`, how the lines are distributed |

## How the space is shared out

1. Each item starts from its `Base size`: its declared size or, if it has none, what its content measures without
   breaking lines.
2. If there is space left over, it is shared out according to `Grow`. If space is missing, the items give it up according
   to `Shrink` (and according to their size: a large one gives up more).
3. An item never becomes narrower than its longest word, unless it declares `Min width: 0`.
4. If they still do not fit, it is an error.

The distribution of what is left over only applies if no item grows. In a row, the items are stretched to the height of
the row (`Vertical: stretch`), except images, which keep their proportions.

## Things to keep in mind

- `Grow` in a `Stack` in a column needs the `Stack` to have a height in points. Otherwise there is nothing to share out and
  it is an error.
- The items do not accept `Margin: auto`: they are aligned with `Horizontal` and `Vertical`.
- Across pages, a `Stack` in a column is broken between items, and one in a row, between lines. A row that does not fit on
  a whole page is split together with its items.

More detail in the [reference](../../reference/components/stack.md).
