# Overflow

**Nothing can overflow**, except what a box decides with [`Overflow`](#policy-per-box-overflow). The engine checks the
already paginated result and gives an error with the first of these
situations:

- **a word wider than its line, or a line of a text with `Preformatted: true` wider than its box**
  (`TEXT_OVERFLOW`), at the line of the document where the word is;
- **a box that does not fit horizontally in the content box of its parent**, counting its horizontal margins
  (`BOX_OVERFLOW`), at the line of its `Width` (or `Min width`) if it declares one;
- **content that exceeds the height of its box**: lines or children that do not fit in the height set by its `Height`, its
  `Max height`, its `Aspect ratio` or, in a `Grid`, its fixed-size rows (`BOX_OVERFLOW`), at the line of that property
  (`Row sizes` for the rows, and if it declares several, the first one in this list). If nothing sets the height, what
  is missing is taken away by a negative `Margin` inside, and that `Margin` is named;
- **something in the flow that does not fit in the page area of a page and cannot be split** (`PAGE_OVERFLOW`), at the
  line of what prevents splitting it:
  - its `Padding` or its `Border`, if the padding and the border of the fragment no longer fit on the page;
  - if not, and it is a line of text, its `Line height` (or, without it, its `Size`), its own or inherited;
  - if not, its `Height` (in an `Image` or a `Barcode` without it, its `Width`, from which the height comes);
  - in a rotated box, which is not split, its `Rotate`;
- **an `Overlay` or a `Page overlay` that goes outside the physical page** (`BOX_OVERFLOW`), at the line of its `Offset`.
  They can take up the margins of the page, but not go outside the page;
- **an in-flow box that goes outside the physical page** (`BOX_OVERFLOW`): it fits in its parent, but the `Offset` of a
  `Frame` or a negative `Margin` takes it off the page; at the line of its `Offset` or, if it has none, of its `Margin`.
  Like an `Overlay`, it can take up the margins of the page;
- **a rotated box that goes outside the physical page** (`BOX_OVERFLOW`), at the line of its `Rotate`: what is checked is
  the rectangle that encloses what is painted, with the rotations of its ancestors
  ([Layers and rotation](layers-and-rotation.md#rotation-rotate)). A rotated `Overlay` or `Page overlay` is checked only this
  way (it can go outside unrotated and fit rotated), and an unrotated one inside a rotated box, too, at the line of the
  element;
- **a rotated in-flow box wider than its container** (`BOX_OVERFLOW`): the rectangle that encloses it, with its margins,
  does not fit in its content box. Its `Width` is reported or, without it, its `Rotate`: without `Width` it does not break
  its text into lines;
- **the marker of a list that does not fit in its indent** (`LIST_MARKER_OVERFLOW`, [List](../components/list.md#position));
- **a table whose header and footer leave no room for the body** at the start of a page (`TABLE_REPEAT_OVERFLOW`,
  [Table](../components/table.md#breaks)), at the line of the `Table`;
- **the content of a `Columns` that does not fit in its columns** and cannot continue on another page (`BOX_OVERFLOW`,
  [Columns](../components/columns.md#what-does-not-fit)): at the line of its `Height` (or `Max height`, or `Aspect ratio`) if
  it is its height that runs out, or at that of its `Count` if, with no page or height to fill, it has more column breaks
  than columns.

The error points to the **innermost** culprit: if a box goes outside because something inside it goes outside, what is
inside is named. `Frame` elements are checked in their place, without the shift of `Offset` or the rotation of `Rotate`
(what is rotated is also checked against the page), and a negative margin counts as space gained: they are not
overflows, but decisions of the author, as long as the box does not go outside the physical page. Vertical margins,
which merge, do not count either, nor does an empty box (no height, border, padding or content), which paints nothing
even if its merged margins leave it past the end of its parent's content. A box can go up to 0.01 pt outside without it
being an overflow, which is what the font measurements in thousandths accumulate.

A box that declares its width is the culprit for its children that only fill it: the engine does not go down to a child
unless it also goes outside the box. In a box with `auto` width it does go down, because its width comes from its content.
A box whose height is imposed by the parent (an item stretched to a fixed row) points to itself and not to the
`Row sizes` of the `Grid`.

What is rotated is checked as it is seen: the rectangle that encloses the rotated box, with the `Offset` of a `Frame`
included. A rotated in-flow box is checked in its container with that rectangle, which is what it takes up. The in-flow
boxes inside a rotated one are checked against their unrotated parent, and the engine does not go down to them to look for
the culprit of what goes outside the width of the rotated one.

> **Decision:** the error points to the property that has to be changed (the `Width`, the `Height`, the `Line height`) and
> not to the box. *Rejected alternative:* the line of the box, or always comparing with the immediate parent, which in an
> `auto` box points to the box and not to what widens it.

## Policy per box: `Overflow`

`Overflow`, in the `Box` group, says what the engine does when what a box contains does not fit in it. It is not
inherited.

| Value | What it does | Where it is valid |
|---|---|---|
| `error` | The above: the first overflow stops the conversion. It is the default. | In any box |
| `shrink` | Reduces the `Size` of the text until it fits, without going below `Min size` ([shrinking](#shrinking-the-text-shrink)). | In a box with text: `Text`, `Paragraph` and a `Cell` with a value |
| `clip` | Paints what sticks out of the padding box clipped and does not check it ([clipping](#clipping-clip)). | In a box without text inside |

### Shrinking the text: `shrink`

With `Overflow: shrink` the text is broken into lines at the `Size` of the box and, if some line is wider than the
content box or the lines do not fit in its height, it is broken again at a smaller size: in steps of 0.5 pt down from
`Size`, with `Min size` as the last attempt if the step skips it. The largest size at which it fits is kept. `Min size`
is a `Typography` property ([Typography](typography.md)): it is inherited and is 6 by default.

- **In width**, always: the width of a box is definite when laying out, and what has to fit is each line (in a normal
  text, its widest word) in the width of the content box.
- **In height**, only if the box has a height that does not come from its content: its `Height`, the one its parent
  imposes (a cell of a fixed-height row, a stretched item) or the one that follows from `Aspect ratio`; otherwise, its
  `Max height`. With an `auto` height the box grows with its lines and there is nothing to shrink.
- The `Line height` follows the size if it is a percentage (also the default 120%); a length in points does not change.
- Nothing else changes: the font, the color, the `Case` and the tagging of the PDF are the same. The contrast is checked
  with the reduced size: a text that goes below 18 pt stops being large ([The PDF](pdf.md#contrast)).
- If it does not fit even at `Min size`, it is the usual error (`TEXT_OVERFLOW` or `BOX_OVERFLOW`), and the message says
  that the text is already at `Min size`. Where the text could shrink and does not, the message of those two errors says
  that `shrink` exists.
- The intrinsic sizes (the narrowest and the natural width, [The box](box.md#intrinsic-sizes)) are measured at `Size`,
  not at `Min size`: a box with an `auto` width, an `auto` column of a table or an item with its automatic minimum grow
  to their content and do not shrink the text. `shrink` acts where the size is fixed.

`Overflow: shrink` only has an effect on a box with text. Written on an element without text (a `Block`, a `Cell` with
elements inside) it is an error (`SHRINK_WITHOUT_TEXT`); if it reaches a box without text from a `Styles` rule, it does
nothing and is not an error, like a `Break` that reaches a box that is not split from a rule
([Errors](errors.md#what-has-no-effect)): this way `For element: Cell` serves every cell of a table that is filled from
data. `Min size` greater than `Size` in a box that shrinks is a contradiction (`MIN_SIZE_CONFLICT`); equal to `Size` is
not.

```stxt
Styles:
    For element: Cell
        Box:
            Height: 16
            Overflow: shrink
```

### Clipping: `clip`

With `Overflow: clip` the box is a window: what it contains is painted clipped by its padding box (with its corners, if
they are rounded), the `Overlay` boxes that hang from it or from its descendants included, and nothing that sticks out
of it is checked, neither against the box nor against the physical page. What is inside is checked as always against its
own parent: a grandchild that sticks out of a child is still an overflow. So is the box itself: it fits in its parent and
on the page.

- Only in a box without text inside, at any depth: clipped text would be content that is not read and would give no
  warning. With text inside it is an error (`CLIP_WITH_TEXT`). To clip a decoration in a card with text, the decoration
  is wrapped in an `Overlay` with `Offset: 0` and `Overflow: clip`, and the text goes outside it.
- It is not split across pages: like a rotated box, it moves whole to the next page if it does not fit in what is left,
  and if it does not fit on a page it is `PAGE_OVERFLOW` ([Breaks](breaks.md#what-is-not-split)). `Keep together` and
  `Edges` on it have no effect and are an error (`BREAK_IN_CLIPPED`), and a `Break` written by an element inside it,
  `BREAK_IN_UNBREAKABLE`.
- In a box with nothing that could stick out (an `Image`, an empty `Block`) `clip` does nothing and is not an error, like
  a `Corner radius` with nothing to round.

```stxt
Frame:
    Box:
        Width: 120
        Height: 50
        Background: #EEF2F7
        Overflow: clip
    Overlay:
        Offset: 20 auto auto 90
        Box:
            Width: 60
            Height: 60
            Corner radius: 50%
            Background: #C8102E
```

> **Decision:** `error` by default: the engine stays strict, and shrinking or clipping is the author's decision, box by
> box or by type with a rule. *Rejected alternative:* shrinking by default in cells, which would change existing documents
> without warning.

> **Decision:** `shrink` goes down in steps of 0.5 pt and tries `Min size` as the last step, instead of searching for the
> exact size at which it fits: it gives readable sizes (`8`, `7.5`) and the same result after a minimal change of the
> text. *Rejected alternative:* a binary search down to the thousandth.

> **Decision:** the intrinsic sizes are measured at `Size`, so `shrink` only acts where the size is fixed and an `auto`
> column grows instead of shrinking its text. *Rejected alternative:* measuring the narrowest width at `Min size`, with
> which a table with `auto` columns would shrink the text without anyone asking for it.

> **Decision:** `clip` also clips the `Overlay` boxes inside and admits no text, the opposite of rounded corners, which
> clip neither the text nor the `Overlay` boxes: its use is the decoration that peeks out over the edge of a box, and what
> is not seen cannot be content. *Rejected alternative:* not clipping the text and still checking it, a mixed rule in
> which the text of a box that sticks out would stay in view outside the window.

> **Decision:** a box with `clip` is not split: a window split across pages would have to decide which part of what is
> clipped goes in each fragment. *Rejected alternative:* splitting it and clipping each fragment on its own.
