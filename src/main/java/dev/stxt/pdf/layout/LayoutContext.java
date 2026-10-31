package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.BreakToken;
import dev.stxt.pdf.layout.image.NaturalSize;
import dev.stxt.pdf.layout.inline.TextMeasurer;
import dev.stxt.pdf.tree.Box;

/**
 * What a layout algorithm needs from the engine: laying out and measuring other boxes, which always goes through
 * here to use the cache, the text metrics and the natural size of images. Implemented by {@link Engine}; the
 * algorithms depend on this interface and not on the engine, which is what creates them.
 */
public interface LayoutContext {

    /**
     * Layout of a box with its algorithm, through the cache. Every recursive layout call goes through here,
     * never directly to another algorithm: without the cache, nested stack and grid grow exponentially.
     */
    LayoutResult layout(Box box, ConstraintSpace space, BreakToken from);

    /** Narrowest width and natural width of the margin box (reference/common/box.md). */
    IntrinsicSizes intrinsic(Box box);

    /**
     * Narrowest width and natural width of the box's content, without its {@code Width}: the automatic minimum of
     * a stack item depends on them (reference/components/stack.md).
     */
    IntrinsicSizes contentIntrinsic(Box box);

    /**
     * The text metrics: the marker of a list is measured and aligned with them (reference/components/list.md).
     */
    TextMeasurer measurer();

    /** Natural size of an {@code Image}, from its file (reference/components/image.md). */
    NaturalSize natural(Box box);
}
