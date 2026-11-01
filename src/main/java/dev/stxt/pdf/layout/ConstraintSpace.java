package dev.stxt.pdf.layout;

import dev.stxt.pdf.tree.PageSetup;

import java.util.OptionalDouble;

/**
 * Immutable input of the layout of a box (modules 7 and 8). Being a {@code record}, it serves as
 * part of the key of {@link LayoutCache}.
 *
 * @param availableWidth          available width; always known
 * @param availableHeight         available height; empty = indefinite
 * @param fixedWidth              the parent (stack or grid) imposes the exact width
 * @param fixedHeight             the parent imposes the exact height (for example, when stretching)
 * @param percentBaseWidth        base for horizontal percentages
 * @param percentBaseHeight       base for vertical percentages; empty = indefinite
 * @param fragmentainerRemaining  space left on the current page from the top border of
 *                                the box (it can be negative); empty = no fragmentation (a
 *                                box that is not broken, like a stack item)
 * @param atFragmentainerStart    there is nothing before the box on this page: it must place something
 *                                even if it does not fit (progress guarantee)
 * @param contentHeight           the height of the content is measured: the box ignores its {@code Height}
 *                                and its {@code Aspect ratio} (the automatic minimum of a stack item
 *                                in {@code column}, reference/components/stack.md); it is not passed to its children
 */
public record ConstraintSpace(
        double availableWidth,
        OptionalDouble availableHeight,
        boolean fixedWidth,
        boolean fixedHeight,
        double percentBaseWidth,
        OptionalDouble percentBaseHeight,
        OptionalDouble fragmentainerRemaining,
        boolean atFragmentainerStart,
        boolean contentHeight) {

    /** Space of the root box when a page starts: the whole page area. */
    public static ConstraintSpace forPage(PageSetup pageSetup) {
        return forPage(pageSetup, 0);
    }

    /**
     * Space of the root box when a page starts, with {@code reserved} points at the bottom of the page area taken by
     * the footnotes of the page (reference/components/footnote.md): the flow gets the rest.
     */
    public static ConstraintSpace forPage(PageSetup pageSetup, double reserved) {
        double width = pageSetup.pageArea().width();
        OptionalDouble height = OptionalDouble.of(pageSetup.pageArea().height() - reserved);
        return new ConstraintSpace(width, height, true, true, width, height, height, true, false);
    }

    /**
     * Space of the in-flow children of a box: its content box is both the available space
     * and the base of the percentages, and nothing imposes the size.
     */
    public static ConstraintSpace forChildren(double width, OptionalDouble height, OptionalDouble remaining,
                                              boolean atStart) {
        return new ConstraintSpace(width, height, false, false, width, height, remaining, atStart, false);
    }

    /**
     * No imposed sizes, with indefinite height and no fragmentation: the probes that resolve
     * margins, border, padding and declared sizes ({@link BoxModel#resolve}).
     */
    public static ConstraintSpace free(double width, double baseWidth, OptionalDouble baseHeight, boolean atStart) {
        return new ConstraintSpace(width, OptionalDouble.empty(), false, false, baseWidth, baseHeight,
                OptionalDouble.empty(), atStart, false);
    }

    /**
     * The parent imposes the width and, if there is one, the height, without fragmentation: stack and grid items
     * and {@code Overlay} boxes.
     */
    public static ConstraintSpace fixed(double width, OptionalDouble height, double baseWidth,
                                        OptionalDouble baseHeight, boolean atStart) {
        return new ConstraintSpace(width, height, true, height.isPresent(), baseWidth, baseHeight,
                OptionalDouble.empty(), atStart, false);
    }

    public boolean fragmenting() {
        return fragmentainerRemaining.isPresent();
    }

    /**
     * The same space with a different remainder of page. It only makes sense if the space fragments:
     * {@link #fragmented} adds the fragmentation.
     */
    public ConstraintSpace withRemaining(double remaining, boolean atStart) {
        if (!fragmenting()) {
            throw new IllegalStateException("withRemaining on a space without fragmentation");
        }
        return fragmented(remaining, atStart);
    }

    /** The same space, to measure the height of the content without the box's {@code Height}. */
    public ConstraintSpace measuringContentHeight() {
        return new ConstraintSpace(availableWidth, availableHeight, fixedWidth, fixedHeight,
                percentBaseWidth, percentBaseHeight, fragmentainerRemaining, atFragmentainerStart, true);
    }

    /**
     * The same space, without fragmentation: a box that is not broken (a rotated {@code Frame},
     * reference/common/breaks.md).
     */
    public ConstraintSpace unfragmented() {
        return new ConstraintSpace(availableWidth, availableHeight, fixedWidth, fixedHeight,
                percentBaseWidth, percentBaseHeight, OptionalDouble.empty(), atFragmentainerStart, contentHeight);
    }

    /** The same space, with fragmentation even if it had none (the items that are broken in parallel). */
    public ConstraintSpace fragmented(double remaining, boolean atStart) {
        return new ConstraintSpace(availableWidth, availableHeight, fixedWidth, fixedHeight,
                percentBaseWidth, percentBaseHeight, OptionalDouble.of(remaining), atStart, contentHeight);
    }
}
