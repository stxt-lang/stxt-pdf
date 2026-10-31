# `List`

A bulleted or numbered list. Each `List item` is an item, with a marker (the bullet or the number) that the engine adds.

```
Document (dev.stxt.pdf):
    Language: en
    Title: Steps
    Page:
        Paper: A4
        Margin: 50
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For element: List
            Box:
                Padding: 0 0 0 18
        For element: List item
            Box:
                Margin: 0 0 4 0
    Section:
        List:
            Marker: decimal
            List item:
                Paragraph: Switch off the power.
            List item:
                Paragraph: Remove the cover of the thermostat.
                List:
                    Marker: dash
                    List item:
                        Paragraph: Keep the screws.
                    List item:
                        Paragraph: Do not touch the wires.
            List item:
                Paragraph: Connect the red wire to terminal L.
```

## Properties

| Property | Where | What it does |
|---|---|---|
| `Marker` | `List` | **Required.** `bullet` (•), `dash` (–), `none`, `decimal` (1.), `lower-alpha` (a.), `upper-alpha` (A.), `lower-roman` (i.), `upper-roman` (I.) |
| `Start` | `List` | The number of the first item (1 by default). Only in numbered lists |
| `Number` | `List item` | The number of this item. The following ones continue from it |

`List` and `List item` also accept `Id`, `Style`, `Link`, `Box`, `Typography` and `Break`. An item has one or more elements
(a `Paragraph`, a `Text`, another `List`), but not headings.

## The indent

The marker hangs **to the left of the item**, in the space left by the left `Padding` of the `List`. Without that padding
there is no room for the marker and it is an error, so the usual approach is to give it in a `For element: List` rule, as
in the example. The markers are aligned to the right, next to the text with a space.

## How it behaves

- The marker uses the typography of the item and is aligned with its first line. That is why an item must start with a
  text: if it starts with a `Stack`, a `Grid` or an image, it is an error.
- Each list numbers its items. A list inside another one starts again.
- If the item is split across pages, the marker goes on the first one.
- `{number of: id}` writes the number of an item with an `Id` (`3`, `c`, `IV`): "see clause {number of: pago}".

More detail in the [reference](../../reference/components/list.md).
