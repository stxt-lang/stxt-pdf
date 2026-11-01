# Overflow

**Nothing can overflow.** The engine checks the already paginated result and gives an error with the first of these
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
