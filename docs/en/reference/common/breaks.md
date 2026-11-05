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
- Inside a box that is not split (an `Overlay`, a `Page overlay`, a rotated `Frame`, a rotated in-flow box or a box with
  `Overflow: clip`), no `Break` has any effect. If the element itself (or the `Row` of a cell) writes it, it is an error
  (`BREAK_IN_UNBREAKABLE`). If it comes from a `Styles` rule, it has no effect and is not an error. The columns of a `Columns` inside it are broken, and their `Break` counts.

> **Decision:** a page break goes through the columns of a `Columns`. *Limit:* inside an item of a `Stack` or a `Grid`
> split in parallel, a page break acts as a column break.

## Margins at breaks

At an unforced break, the margins that touch the break are truncated to zero: after the break, the top margin of the first
child of the page and the ones that come out of it at the top. A continuation fragment lets no margins out at the top. At
a forced break, the margin before the break is truncated and the one after it is kept.

A box that contains the margins of its children (a box with border or padding, with `Isolate margins`, the column of a
`Columns`) also contains the bottom margin of its last child in its height, so that margin has to fit on the page as the
child does: if the child fits and its margin does not, the break goes before the child (or earlier, if that break is
better), and before the box if nothing inside it can break. The page area is the exception: its bottom is the end of the
page, and the bottom margin of its last child is truncated there. A box with `Height` has the height it has, whatever its
content does.

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

That gap is not taken from the boxes that contain it either: for them, what the fragment takes up is what its content
takes up, even if it is seen up to the end of the page. So a column stretched to its row that contains a text
stretched to its own row still has room on the next page for the lines it has left.

> **Decision:** a broken fragment consumes only what its content takes up, so that a box (or a stretched grid item) does
> not overflow because it is split, at the cost that the visible height can exceed the `Height`.
> *Rejected alternative:* a fragment that measures only what its content takes up (does not reach the end of the page), or letting the content overflow the last fragment.

## Breaks in `Stack`, `Grid` and `Table`

In a vertical `Stack`, the break is made between items. In a horizontal one and in a `Grid`, between lines or rows. A line,
row or item that does not fit in what is left of the page is split **in parallel** on that page (`Split rows` in `Grid` and
`Table`, `Split items` in `Stack`; both `true` by default): each item is split on its own and the row continues on the next
page with the items that have not finished, all from the top, and with the height of the longest of them. The following
rows come after, with the gap that separated them. The rows taken up by the same item are split together, each item from
its position. A stretched item (`stretch`, without a height of its own) measures on each page what its row or line does:
it reaches the end of the page while the row goes on and, on the last one, it ends where the row ends, which is where
the longest of the items ends with its bottom margin, even if the height the unsplit row imposes on it gives it less.
The bottom margin of the stretched item is taken off. So two columns with a background end at the same height on each
page, also on the last one.

Three stretched items are not lengthened and keep the height they had unsplit: that of a row that
is split together with the following ones, which ends with its own row and not with the last one; the one that
`Max height` keeps shorter than its row; and the box that is not broken (`Rotate`, `Overflow: clip`).

> **Decision:** a stretched item ends with its row, like the cells of a table. The lines of a text leave a gap at the
> end of each page that is not taken from its height ([Definite height and breaks](#definite-height-and-breaks)), so
> the split row measures more than unsplit; a sibling stretched to the height of the unsplit row was shorter on the
> last page, or ran out one page earlier. *Rejected alternative:* distributing across the pages the height of the
> unsplit row, which was the rule until 0.10.0.

While the row goes on, each item advances at its own pace: a text with taller lines places less height on each page, and
a stretched empty item reaches the end. For the boxes that contain the row, the row has consumed of their height as far
as **the slowest of the items that go on** has got, not as far as the fragment of the one that advances most reaches
([Definite height and breaks](#definite-height-and-breaks)). The room the unsplit row leaves under an item counts as
advanced by it. So a column with a definite height that contains the row has room on its last page for what the slowest
item has left.

> **Decision:** the split row consumes what the slowest of its items does, so that the box that contains it does not
> overflow. *Rejected alternative:* what the item that advances most takes up, which was the rule until 0.10.0: next to
> a stretched empty item, or to a text with shorter lines, the column ran out of the height the other item still needed
> (`BOX_OVERFLOW`).

Splitting the row is one more break, which competes with the breaks between rows by the rules of the [choice of the
break](#choice-of-the-break): its quality is the worst of that of its items, so a row whose items cannot respect their
widows and orphans or a `Keep together` (in a `Table`, that of the `Row`, which is that of its cells) moves whole to the
next page if the break before it is better. If any item cannot place anything, the row is not split either. With
`Split rows: false` (`Split items: false`), every row that does not fit moves whole to the next page, and it is only split
in parallel if it does not fit at the start of a page either; if then some item cannot place anything, it is an error.

> **Decision:** rows are split on the page they are on, by default, as in word processors and as the children of a
> `Block` are: two parallel columns of text each go on on their own and do not leave half a page blank, and a short row
> is not split anyway, because its widows and orphans prevent it. Whoever wants whole rows (a grid of cards) says so with
> `Split rows: false`. *Rejected alternative:* splitting only the row that does not fit on a whole page, which was the
> rule until 0.8.0: a row that fit on a page left the previous one almost empty.

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
- **Clipped boxes.** A box with `Overflow: clip` is not split either: whole or on the next page
  ([Overflow](overflow.md#clipping-clip)).
- **Side boxes.** An `Image`, a `Barcode` or a `Block` with `Side` is not split either: it moves to the next page with
  the first line that runs beside it, and the lines shortened beside it end with the page: on the next one, the text
  has the whole width ([Block](../components/block.md#side-boxes-side)).

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
