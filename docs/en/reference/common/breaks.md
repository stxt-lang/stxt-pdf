# Pages and breaks: `Break`

## The `Break` group

Page breaks. They are not inherited. Not allowed in `Overlay` or `Page overlay`, which are not split and are not in the
flow of the page, nor in headings or cells ([Headings](../components/heading.md#breaks),
[Table](../components/table.md#rows)).

| Property | Values | Default | Notes |
|---|---|---|---|
| `Before` | `auto`, `page`, `column` | `auto` | `page` forces a page break before the box, and `column`, a break to the next column (only inside a `Columns`, [Columns](../components/columns.md#breaks)). |
| `After` | `auto`, `page`, `column` | `auto` | Like `Before`, after the box. |
| `Keep with next` | boolean | `false` | Avoids the break between the box and the next one (titles). With `After: page` or `column` it is `KEEP_CONFLICT`. |
| `Keep with previous` | boolean | `false` | Avoids the break between the box and the previous one. With `Before: page` or `column` it is `KEEP_CONFLICT`. |
| `Keep together` | boolean | `false` | Tries not to split the box. |
| `Edges` | `open`, `closed` | `open` | What happens to the border and the padding at a break. |

> **Decision:** breaks are stated with `Before` and `After`, and what is kept together, with `Keep with next`,
> `Keep with previous` and `Keep together`, the names used by word processors. *Rejected alternative:* an `avoid` value
> in `Before`, `After` and `Inside`, which does not say what is kept with what.

## Model

A **fragment container** is where the pieces of the boxes fall: one per page, with the height of the page area, and one
per column inside a `Columns` ([Columns](../components/columns.md)). Each box produces one **fragment** per fragment
container in which it appears.

## Break opportunities

- between sibling boxes in block flow;
- between lines of text;
- between lines of a `Stack` and between rows of a `Grid` or a `Table`.

The engine always tries to break at the highest possible point of the tree. Only if that does not fit does it break inside
a child.

## Choosing the break

The engine chooses the **last** break on the page that does not violate any rule. If there is none, it relaxes the rules
in this order:

1. `Widows` and `Orphans` (from `Typography`);
2. `Keep with next` and `Keep with previous`;
3. `Keep together`.

If there is still no possible break, it breaks where the space runs out.

A paragraph that cannot respect `Widows` and `Orphans` is split anyway if the alternative is to violate a `Keep with next` or a `Keep with previous`. At
that break, widows are respected if possible and, if not, the space is filled.

`Keep with next` and `Keep with previous` propagate like the forced breaks: the `Keep with next` of the last in-flow
child of a block (or of a `Columns`) also applies to the block, and so does the `Keep with previous` of the first one.
This way, a heading that ends a `Block` is not left alone at the end of the page.

If the best break comes before the child that does not fit, what follows moves to the next page, but an earlier child is
never split inside to make room.

## Guaranteed progress

Something is always placed at the start of a page, even if it has to be broken by force. What cannot be split and does not
fit on a whole page (a line taller than the page area, an image, a row with an item that cannot place anything) is an
error (`PAGE_OVERFLOW`, [Overflow](overflow.md)).

## Forced breaks

`Before: page` and `After: page` force a page break, and `Before: column` and `After: column`, inside a `Columns`, a break
to the next column (which, in the last column, is also a page break). They only apply to in-flow boxes: `Overlay` and
`Page overlay` do not allow `Break`.

- The `Before` of the first child of a block and the `After` of the last one propagate to the block. Only the page break
  comes out of a `Columns`.
- If a box has both, the page break wins.
- A forced break at the start of a page (or of a column) is ignored: it does not leave blank pages. The same goes for the
  `After: page` of the last box of a section.
- A page break inside a `Columns` leaves the remaining columns on that page unfilled.
- Inside a box that is not split (an `Overlay`, a `Page overlay`, a rotated `Frame` or a rotated in-flow box), no
  `Break` has any effect. If the element itself (or the `Row` of a cell) writes it, it is an error
  (`BREAK_IN_UNBREAKABLE`). If it comes from a `Styles` rule, it has no effect and is not an error. The columns of a `Columns` inside it are broken, and their `Break` counts.

> **Decision:** a page break goes through the columns of a `Columns`. *Limit:* inside an item of a `Stack` or a `Grid`
> split in parallel, a page break acts as a column break.

## Margins at breaks

At an unforced break, the margins that touch the break are truncated to zero: after the break, the top margin of the first
child of the page and the ones that come out of it at the top. A continuation fragment lets no margins out at the top. At
a forced break, the margin before the break is truncated and the one after it is kept.

## Borders and padding at breaks

With `Edges: open`, the first fragment has no bottom border or padding and the following ones have no top ones. With
`Edges: closed`, each fragment is painted as a complete box: the content of each page fits between its top and bottom
border and padding.

With `open`, the bottom border and padding are only reserved if the box would end on the page: if they do not fit then,
the box breaks earlier (or moves whole to the next page) and ends there with them. If the box breaks anyway, its fragment
reaches the end of the page, without leaving a gap.

## Definite height and breaks

A fixed `Height` is the total of all the fragments, not that of each one. With `closed`, the total of the content is that
height minus one border and one padding, and each fragment adds its own to it. `Min height` and `Max height` also apply to
the total (with `closed`, to the total content plus one border and one padding).

A box with a definite `Height` is split like any other, even if it has no content: what does not fit of its height moves
to the next page. To avoid this, `Keep together: true` is declared, which does not prevent splitting it if it does not fit
on a whole page.

A broken fragment of a box with `auto` height measures up to the break. With a definite height, it reaches the end of the
page without exceeding the height it has left, but it only **consumes** from that height what its content takes up (with
its top border and padding): the gap that the lines or the children leave at the end of the page is not taken from the
content that follows. This way, a box with a `Height` equal to the height of its content does not overflow when it is
split, and the visible height can exceed the `Height` by the sum of those gaps.

> **Decision:** a broken fragment consumes only what its content takes up, so that a box (or a stretched grid item) does
> not overflow because it is split, at the cost that the visible height can exceed the `Height`.
> *Rejected alternative:* a fragment that measures only what its content takes up (does not reach the end of the page), or letting the content overflow the last fragment.

## Breaks in `Stack`, `Grid` and `Table`

In a vertical `Stack`, the break is made between items. In a horizontal one and in a `Grid`, between lines or rows. A line,
row or item that does not fit in what is left of the page moves whole to the next one. If it does not fit at the start of
a page either, its items are split **in parallel**: each one is split on its own and the row continues on the next page
with the items that have not finished, all from the top, and with the height of the longest of them. The following rows
come after, with the gap that separated them. The rows taken up by the same item are split together, each item from its
position. A stretched item distributes across the pages the height its line or area imposes on it, so it can end before
or after the text of its siblings. If any item cannot place anything, the row is not split, and if it does not fit it is
an error.

When choosing the break, a split row counts as the worst of the breaks of its items. A vertical `Stack` with `Wrap` is not
split. Tables also repeat their header and their footer ([Table](../components/table.md#breaks)).

## What is not split

- **Images.** An `Image` or a `Barcode` is not split: if it does not fit in what is left of the page (or of the column),
  it moves whole to the next one ([Image](../components/image.md#page-breaks)).
- **Positioned elements.** An `Overlay` is placed on the page where its position falls and is not split. If its reference
  container is split, it is resolved against the fragment on that page: its height is that of that fragment and not the
  total. A `Page overlay` is repeated on the pages its `Pages` says. A `Frame` is split like any block, except when it is
  rotated: then, like an image, it moves whole to the next page if it does not fit in what is left of the current one
  ([Layers and rotation](layers-and-rotation.md#rotation-rotate)).
- **Rotated in-flow elements.** A rotated `Block`, `Text` or `Paragraph` is not split either: like an image, whole or on
  the next page, with the height of the rectangle that encloses it.

## Page numbers: `{page}` and `{pages}`

`{page}` and `{pages}` in a text are replaced with the printed number of the page and the last number of its page
sequence, according to the `Page numbering` and the `Page number format` of each section
([Document](../components/document.md#page-numbering)). Line breaking and page breaking are done with the
width of a two-digit number (`00`), or of as many digits as the longest one has if there are more (in Roman numerals, as
many `M` as letters in the longest one). The real number is then put in that space, and the line is realigned without
changing its breaks.

`{page of: id}`, on the other hand, is measured with the written page, so it can change the breaking
([References](references.md)).

## What is not supported

- **Blank pages for double-sided printing** (`Before: odd-page`): a page nobody wrote, and it would have to be decided
  whether it is numbered.
- Splitting an already placed child inside to make room for what follows.
