package dev.stxt.pdf.layout;

/**
 * Margins of the descendants that touch the top or bottom border of a box and therefore merge with the
 * box's own margins, outside it (reference/components/block.md: the child's margin "comes out" of the parent).
 * They do not include the box's own margins: the parent resolves those, since it places the box.
 *
 * @param top              margins that come out at the top (first child, and the empty ones before it)
 * @param bottom           margins that come out at the bottom (last child)
 * @param collapsesThrough the box is empty and its top and bottom margins merge with each other;
 *                         then everything goes in {@code top} and {@code bottom} is empty
 */
public record AdjoiningMargins(MarginStrut top, MarginStrut bottom, boolean collapsesThrough) {

    /** Nothing comes out of the box: boxes that isolate margins, boxes with border or padding... */
    public static final AdjoiningMargins NONE = new AdjoiningMargins(MarginStrut.EMPTY, MarginStrut.EMPTY, false);

    /** Empty box that contributes no descendant margins, but lets its own merge. */
    public static final AdjoiningMargins EMPTY_BOX = new AdjoiningMargins(MarginStrut.EMPTY, MarginStrut.EMPTY, true);
}
