# Pages and breaks: `Break`

## How the content is split

The engine fills each page with the content in order and, when it does not fit, it **breaks** and continues on the next
page. It can break:

- Between two boxes that go one below the other.
- Between two lines of a text.
- Between two rows of a table or of a `Grid`, or between two lines of a `Stack`.

It always breaks as high up in the tree as possible: before splitting a box inside, it tries to break between it and its
siblings. And it chooses the **last** place on the page that meets all the rules:

1. Not leaving fewer lines of a paragraph than `Orphans` at the end of the page or than `Widows` at the start of the next
   one ([Typography](typography.md#widows-and-orphans)).
2. Not breaking between two boxes that ask to stay together (`Keep with next`, `Keep with previous`).
3. Not splitting a box that asks for `Keep together: true`.

If there is no place that meets everything, it relaxes the rules in that order. At the start of a page it always places
something, even if it has to be split, so that pagination ends.

Some things are never split: an image, a rotated box, a positioned box, and the header and the footer of a table. If they
do not fit in what is left of the page, they move whole to the next one, and if they do not fit even on an empty page, it
is an error.

## The `Break` group

| Property | Values | Default | What it does |
|---|---|---|---|
| `Before` | `auto`, `page`, `column` | `auto` | `page` starts the box on a new page |
| `After` | `auto`, `page`, `column` | `auto` | `page` makes what follows start on a new page |
| `Keep with next` | `true`, `false` | `false` | Keeps the box on the same page as the next one |
| `Keep with previous` | `true`, `false` | `false` | Keeps the box on the same page as the previous one |
| `Keep together` | `true`, `false` | `false` | Tries not to split the box |
| `Edges` | `open`, `closed` | `open` | What happens to the border and the padding of a split box |

`column` is the same as `page` but inside a [`Columns`](../components/columns.md): it jumps to the next column. A box
cannot stay with the next one and break after it at the same time: `Keep with next: true` with `After: page` is an error,
and so is `Keep with previous: true` with `Before: page`.

```
Block:
    Break:
        Keep together: true
    Paragraph: Customer signature
    Paragraph: Supplier signature
```

- A forced break at the start of a page does nothing: it never leaves blank pages.
- Headings already avoid being left alone at the end of a page: `Keep with next: true` is not needed
  ([Headings](../components/heading.md)).
- `Keep together: true` does not prevent splitting a box that does not fit even on a whole page.
- The positioned elements `Overlay` and `Page overlay` do not accept `Break`, because they are not split and are not in
  the flow. Neither does what is inside them or inside a rotated box: a `Break` written there is an error (except in the
  columns of a `Columns`). One that comes from a `Styles` rule has no effect.

## Split boxes

When a box with a border or padding is split across two pages:

- **`Edges: open`** (default): the box is cut like a sheet of paper. The first piece has no border or padding at the
  bottom and the next one has none at the top.
- **`Edges: closed`**: each piece is painted as a complete box, with its border and its padding on all four sides.

A fixed `Height` is the total height of the box, adding up all its pieces. Margins that touch a break disappear: the
content of the next page starts at the very top.

## Page numbers

In any text, `{page}` writes the number of the page and `{pages}` the total:

```
Page overlay:
    Offset: auto 0 -30 0
    Text: Page {page} of {pages}
        Typography:
            Align: right
```

A page footer like this one is a `Page overlay`, which is repeated on each page
([positioned elements](../components/frame-and-overlays.md#page-overlay)). The number each page carries is decided by its
section: it can continue the count, start again at 1 or have no number, like a cover
([Document](../components/document.md#page-numbering)). `{pages}` is the last number of the current count.
With a cover without a number and six body pages, the first one says "Page 1 of 6".

To write the page where another part of the document is ("see page 12") there is `{page of: id}`
([References and links](references-and-links.md)).

More detail in the [reference](../../reference/common/breaks.md).
