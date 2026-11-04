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
 * @param rules    lines it draws over the backgrounds of its children and under their text: the merged borders of a
 *                 table (reference/components/table.md#merged-borders); empty in the other boxes
 */
public record Fragment(Box box, double x, double y, double width, double height, Insets margin,
                       Insets border, Insets padding, List<Fragment> children, List<TextLine> lines,
                       boolean first, boolean last, double offsetX, double offsetY, Rect turned, List<Rule> rules) {

    /**
     * Copies {@code children}, {@code lines} and {@code rules} into unmodifiable lists; none can be {@code null}.
     *
     * @param box      source box
     * @param x        position of the border box, in points relative to the content box of the parent; with
     *                 {@code turned}, that of the rectangle that encloses it
     * @param y        likewise, vertically
     * @param width    width of the border box or, with {@code turned}, of the rectangle that encloses it
     * @param height   height of the border box in this fragment or, with {@code turned}, of the rectangle that
     *                 encloses it
     * @param margin   used margins of the box in the layout that produced the fragment ({@code auto} = 0)
     * @param border   borders painted in this fragment
     * @param padding  padding of this fragment
     * @param children child fragments, in tree order, relative to the content box of this one
     * @param lines    lines of text, relative to the content box; empty if the box has no text
     * @param first    whether it is the first fragment of the box
     * @param last     whether it is the last fragment of the box
     * @param offsetX  horizontal {@code Frame} offset, already added to {@code x}; 0 in the other boxes
     * @param offsetY  vertical {@code Frame} offset, already added to {@code y}; 0 in the other boxes
     * @param turned   in a box rotated in flow, its unrotated border box relative to ({@code x}, {@code y});
     *                 {@code null} in the other boxes
     * @param rules    lines it draws over the backgrounds of its children: the merged borders of a table; empty in
     *                 the other boxes
     */
    public Fragment {
        children = List.copyOf(children);
        lines = List.copyOf(lines);
        rules = List.copyOf(rules);
    }

    /**
     * A fragment without lines of its own ({@link #rules}).
     *
     * @param box      source box
     * @param x        position of the border box, in points relative to the content box of the parent; with
     *                 {@code turned}, that of the rectangle that encloses it
     * @param y        likewise, vertically
     * @param width    width of the border box or, with {@code turned}, of the rectangle that encloses it
     * @param height   height of the border box in this fragment or, with {@code turned}, of the rectangle that
     *                 encloses it
     * @param margin   used margins of the box in the layout that produced the fragment ({@code auto} = 0)
     * @param border   borders painted in this fragment
     * @param padding  padding of this fragment
     * @param children child fragments, in tree order, relative to the content box of this one
     * @param lines    lines of text, relative to the content box; empty if the box has no text
     * @param first    whether it is the first fragment of the box
     * @param last     whether it is the last fragment of the box
     * @param offsetX  horizontal {@code Frame} offset, already added to {@code x}; 0 in the other boxes
     * @param offsetY  vertical {@code Frame} offset, already added to {@code y}; 0 in the other boxes
     * @param turned   in a box rotated in flow, its unrotated border box relative to ({@code x}, {@code y});
     *                 {@code null} in the other boxes
     */
    public Fragment(Box box, double x, double y, double width, double height, Insets margin, Insets border,
                    Insets padding, List<Fragment> children, List<TextLine> lines, boolean first, boolean last,
                    double offsetX, double offsetY, Rect turned) {
        this(box, x, y, width, height, margin, border, padding, children, lines, first, last, offsetX, offsetY,
                turned, List.of());
    }

    /**
     * A fragment that does not belong to a box rotated in flow.
     *
     * @param box      source box
     * @param x        position of the border box, in points relative to the content box of the parent
     * @param y        likewise, vertically
     * @param width    width of the border box
     * @param height   height of the border box in this fragment
     * @param margin   used margins of the box in the layout that produced the fragment ({@code auto} = 0)
     * @param border   borders painted in this fragment
     * @param padding  padding of this fragment
     * @param children child fragments, in tree order, relative to the content box of this one
     * @param lines    lines of text, relative to the content box; empty if the box has no text
     * @param first    whether it is the first fragment of the box
     * @param last     whether it is the last fragment of the box
     * @param offsetX  horizontal {@code Frame} offset, already added to {@code x}; 0 in the other boxes
     * @param offsetY  vertical {@code Frame} offset, already added to {@code y}; 0 in the other boxes
     */
    public Fragment(Box box, double x, double y, double width, double height, Insets margin, Insets border,
                    Insets padding, List<Fragment> children, List<TextLine> lines, boolean first, boolean last,
                    double offsetX, double offsetY) {
        this(box, x, y, width, height, margin, border, padding, children, lines, first, last, offsetX, offsetY,
                null);
    }

    /**
     * A fragment without {@code Frame} offset: the one the layout of a box returns.
     *
     * @param box      source box
     * @param x        position of the border box, in points relative to the content box of the parent
     * @param y        likewise, vertically
     * @param width    width of the border box
     * @param height   height of the border box in this fragment
     * @param margin   used margins of the box in the layout that produced the fragment ({@code auto} = 0)
     * @param border   borders painted in this fragment
     * @param padding  padding of this fragment
     * @param children child fragments, in tree order, relative to the content box of this one
     * @param lines    lines of text, relative to the content box; empty if the box has no text
     * @param first    whether it is the first fragment of the box
     * @param last     whether it is the last fragment of the box
     */
    public Fragment(Box box, double x, double y, double width, double height, Insets margin, Insets border,
                    Insets padding, List<Fragment> children, List<TextLine> lines, boolean first, boolean last) {
        this(box, x, y, width, height, margin, border, padding, children, lines, first, last, 0, 0);
    }

    /**
     * {@return an empty fragment of a box that does not fit in what is left of the page ({@link BreakToken.Before})}
     *
     * @param box the box that does not fit; the fragment is neither its first nor its last
     */
    public static Fragment empty(Box box) {
        return new Fragment(box, 0, 0, 0, 0, Insets.ZERO, Insets.ZERO, Insets.ZERO, List.of(), List.of(), false, false);
    }

    /**
     * {@return the border box of this fragment if ({@code absX}, {@code absY}) is the position of the fragment: the
     * rectangle {@code x}, {@code y}, {@code width}, {@code height} or, in a box rotated in flow, its unrotated box}
     *
     * @param absX horizontal position of the fragment in the caller's coordinates (page coordinates in the dump and
     *             the painting): {@code x} plus the left of the content box of the parent
     * @param absY vertical position of the fragment, likewise
     */
    public Rect borderBox(double absX, double absY) {
        return turned == null ? new Rect(absX, absY, width, height)
                : new Rect(absX + turned.x(), absY + turned.y(), turned.width(), turned.height());
    }

    /**
     * {@return the width of the content box of this fragment: the border box (unrotated, in a box rotated in flow)
     * minus its border and its padding}
     */
    public double contentWidth() {
        return (turned == null ? width : turned.width()) - border.horizontal() - padding.horizontal();
    }

    /** {@return the height of the content box of this fragment, measured like {@link #contentWidth()}} */
    public double contentHeight() {
        return (turned == null ? height : turned.height()) - border.vertical() - padding.vertical();
    }

    /**
     * The fragment of a box rotated in flow: it takes up the {@code width} x {@code height} rectangle that encloses its
     * rotated border box, which is {@code box} inside it.
     *
     * @param box    the unrotated border box, relative to ({@code x}, {@code y}) and centered in the rectangle that
     *               encloses it; it becomes {@link #turned()}
     * @param width  width of the rectangle that encloses the rotated border box, in points
     * @param height height of the rectangle that encloses the rotated border box, in points
     * @return this fragment with that rectangle as its size and {@code box} as its unrotated border box
     */
    public Fragment turned(Rect box, double width, double height) {
        return new Fragment(this.box, x, y, width, height, margin, border, padding, children, lines, first, last,
                offsetX, offsetY, box, rules);
    }

    /**
     * {@return the same fragment with these lines of its own ({@link #rules})}
     *
     * @param rules the lines, relative to its content box, in the order they are painted; they replace the ones it had
     */
    public Fragment withRules(List<Rule> rules) {
        return new Fragment(box, x, y, width, height, margin, border, padding, children, lines, first, last, offsetX,
                offsetY, turned, rules);
    }

    /**
     * {@return the same fragment at ({@code newX}, {@code newY}), with the {@code Frame} offset it had}
     *
     * @param newX the new {@code x}, in points relative to the content box of the parent
     * @param newY the new {@code y}, likewise
     */
    public Fragment moved(double newX, double newY) {
        return moved(newX, newY, offsetX, offsetY);
    }

    /**
     * {@return the fragment at ({@code newX}, {@code newY}), which already includes the {@code Frame}
     * offset ({@code dx}, {@code dy})}
     *
     * @param newX the new {@code x}, in points relative to the content box of the parent, {@code dx} included
     * @param newY the new {@code y}, likewise, {@code dy} included
     * @param dx   the new {@link #offsetX()}
     * @param dy   the new {@link #offsetY()}
     */
    public Fragment moved(double newX, double newY, double dx, double dy) {
        return new Fragment(box, newX, newY, width, height, margin, border, padding, children, lines, first, last,
                dx, dy, turned, rules);
    }

    /**
     * {@return the same fragment with more children at the end (the Overlay boxes this box resolves)}
     *
     * @param more the fragments to add after its children, in order, relative to its content box
     */
    public Fragment withMoreChildren(List<Fragment> more) {
        List<Fragment> all = new ArrayList<>(children);
        all.addAll(more);
        return new Fragment(box, x, y, width, height, margin, border, padding, all, lines, first, last, offsetX,
                offsetY, turned, rules);
    }
}
