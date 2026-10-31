# `Block`

A `Block` groups elements **one below the other**. It is used to give a group a background, a border or a margin, to keep a
group from being split across pages or to let its children inherit a typography.

```
Document (dev.stxt.pdf):
    Language: en
    Title: Notice
    Page:
        Paper: A4
        Margin: 40
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For style: warning
            Box:
                Margin: 12 0
                Padding: 10 14
                Border: 0 0 0 4
                Border color: #C0392B
                Background: #FBEAE8
                Corner radius: 0 6 6 0
            Break:
                Keep together: true
    Section:
        Paragraph: Before installing the thermostat, switch off the power.
        Block:
            Style: warning
            Paragraph: Warning
                Typography:
                    Bold: true
            Paragraph: Working with the power on can cause an electric shock.
        Paragraph: Then remove the cover carefully.
```

It accepts `Id`, `Style`, `Link`, `Box`, `Typography`, `Break`, `Isolate margins` and `Rotate` (rotating it with everything
it contains, [Layers and rotation](../common/layers-and-rotation.md#rotating-a-box-rotate)), and any element inside.

## How it places its children

- Each child takes up **the full width** of the `Block` (minus its margins), unless it declares its `Width`. One with `Width`
  and `Margin: 0 auto` is centered.
- The children are stacked from top to bottom, in document order.
- The height of the `Block` is the sum of its children, plus its padding and its border, unless it declares `Height`.
- Between two children, the vertical margins are not added: the larger one is kept ([The box](../common/box.md#margins)).

Sections, headings, lists, cells with elements and positioned elements place their children in the same way.

## `Isolate margins`

The top margin of the first child (and the bottom margin of the last one) goes outside the `Block` if the `Block` has
neither border nor padding on that side: it becomes a margin of the `Block` itself. With `Isolate margins: true`, the
margins of the children stay inside. A `Block` that is an item of a `Stack` or a `Grid` already keeps them inside, so
there `Isolate margins: true` is an error.

## Recipes

- **A boxed panel:** a `Block` with `Padding`, `Border` and `Background`.
- **Not split:** `Break` with `Keep together: true` (the signature block of a contract).
- **Start on a new page:** `Break` with `Before: page`.

More detail in the [reference](../../reference/components/block.md).
