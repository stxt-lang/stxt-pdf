package dev.stxt.pdf.fragment;

import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.tree.Box;

import java.util.ArrayList;
import java.util.List;

/**
 * The piece of a box that falls in a fragmentainer (reference/common/breaks.md). A box that is
 * broken across pages produces several.
 *
 * @param box      source box (its painting properties are read from its style)
 * @param x        position of the border box, relative to the content box of the parent; in a box rotated in
 *                 flow, that of the rectangle that encloses it ({@code turned})
 * @param y        likewise
 * @param width    width of the border box; in a box rotated in flow, that of the rectangle that encloses it, which is
 *                 what it takes up
 * @param height   height of the border box <em>in this fragment</em>; in a box rotated in flow, that of the rectangle
 *                 that encloses it
 * @param margin   used margins of the box in the layout that produced the fragment
 *                 ({@code auto} = 0, as in {@link dev.stxt.pdf.layout.BoxModel}): the
 *                 percentages depend on the base of that layout, which only the algorithm knows
 * @param border   borders painted in this fragment (with {@code Edges: open}, the
 *                 first one has no bottom border and the following ones have no top border)
 * @param padding  padding of this fragment, with the same criterion as {@code border}; the content
 *                 box of the fragment, to which children and lines refer, is the border
 *                 box minus {@code border} and {@code padding}
 * @param children child fragments, in tree order
 * @param lines    lines of text, if the box contains text
 * @param first    it is the first fragment of the box
 * @param last     it is the last fragment of the box
 * @param offsetX  offset of a {@code Frame} (reference/components/frame-and-overlays.md), already
 *                 added to {@code x}: its place in the flow is {@code x - offsetX}. The parent sets it when placing
 *                 it, with the percentage base of its layout; 0 in the other boxes
 * @param offsetY  likewise, vertically
 * @param turned   in a box rotated in flow (reference/common/layers-and-rotation.md), its unrotated border box,
 *                 relative to ({@code x}, {@code y}) and centered in the rectangle that encloses it; its children and
 *                 its lines refer to it. {@code null} in the other boxes
 */
public record Fragment(Box box, double x, double y, double width, double height, Insets margin,
                       Insets border, Insets padding, List<Fragment> children, List<TextLine> lines,
                       boolean first, boolean last, double offsetX, double offsetY, Rect turned) {

    public Fragment {
        children = List.copyOf(children);
        lines = List.copyOf(lines);
    }

    /** A fragment that does not belong to a box rotated in flow. */
    public Fragment(Box box, double x, double y, double width, double height, Insets margin, Insets border,
                    Insets padding, List<Fragment> children, List<TextLine> lines, boolean first, boolean last,
                    double offsetX, double offsetY) {
        this(box, x, y, width, height, margin, border, padding, children, lines, first, last, offsetX, offsetY,
                null);
    }

    /** A fragment without {@code Frame} offset: the one the layout of a box returns. */
    public Fragment(Box box, double x, double y, double width, double height, Insets margin, Insets border,
                    Insets padding, List<Fragment> children, List<TextLine> lines, boolean first, boolean last) {
        this(box, x, y, width, height, margin, border, padding, children, lines, first, last, 0, 0);
    }

    /** Empty fragment of a box that does not fit in what is left of the page ({@link BreakToken.Before}). */
    public static Fragment empty(Box box) {
        return new Fragment(box, 0, 0, 0, 0, Insets.ZERO, Insets.ZERO, Insets.ZERO, List.of(), List.of(), false, false);
    }

    /**
     * The border box of this fragment if ({@code absX}, {@code absY}) is the position of the fragment: the rectangle
     * {@code x}, {@code y}, {@code width}, {@code height} or, in a box rotated in flow, its unrotated box.
     */
    public Rect borderBox(double absX, double absY) {
        return turned == null ? new Rect(absX, absY, width, height)
                : new Rect(absX + turned.x(), absY + turned.y(), turned.width(), turned.height());
    }

    /** Width of the content box of this fragment: the border box minus its border and its padding. */
    public double contentWidth() {
        return (turned == null ? width : turned.width()) - border.horizontal() - padding.horizontal();
    }

    /** Height of the content box of this fragment. */
    public double contentHeight() {
        return (turned == null ? height : turned.height()) - border.vertical() - padding.vertical();
    }

    /**
     * The fragment of a box rotated in flow: it takes up the {@code width} × {@code height} rectangle that encloses its
     * rotated border box, which is {@code box} inside it.
     */
    public Fragment turned(Rect box, double width, double height) {
        return new Fragment(this.box, x, y, width, height, margin, border, padding, children, lines, first, last,
                offsetX, offsetY, box);
    }

    public Fragment moved(double newX, double newY) {
        return moved(newX, newY, offsetX, offsetY);
    }

    /**
     * The fragment at ({@code newX}, {@code newY}), which already includes the {@code Frame}
     * offset ({@code dx}, {@code dy}).
     */
    public Fragment moved(double newX, double newY, double dx, double dy) {
        return new Fragment(box, newX, newY, width, height, margin, border, padding, children, lines, first, last,
                dx, dy, turned);
    }

    /** The same fragment with more children at the end (the Overlay boxes this box resolves). */
    public Fragment withMoreChildren(List<Fragment> more) {
        List<Fragment> all = new ArrayList<>(children);
        all.addAll(more);
        return new Fragment(box, x, y, width, height, margin, border, padding, all, lines, first, last, offsetX,
                offsetY, turned);
    }
}
