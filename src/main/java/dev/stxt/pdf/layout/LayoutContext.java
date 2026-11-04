package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.BreakToken;
import dev.stxt.pdf.layout.image.NaturalSize;
import dev.stxt.pdf.layout.inline.TextMeasurer;
import dev.stxt.pdf.tree.Box;

import java.util.function.Supplier;

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

    /**
     * Width divided by height of an image inside a text ({@code ![alt](path)}, reference/common/text-formatting.md),
     * already checked before the layout.
     */
    double imageRatio(String path);

    /**
     * What a container computes once before breaking it between its units and no fragment repeats
     * (reference/implementation/layout.md): the placed items, the tracks and the bands of a stack, a grid or a table.
     * It depends on the box and on its space <em>without fragmentation</em> ({@link ConstraintSpace#unfragmented},
     * which keeps {@code atFragmentainerStart}), never on what is left of the page or on the token, so {@code space}
     * is that one. It is memoized by box, space and {@code type}, with the layouts of the items, and forgotten with
     * them: each page of the container only breaks it. {@code computation} runs once per key and what it returns is
     * shared by every fragment, so it has to be immutable.
     */
    <T> T arranged(Box box, ConstraintSpace space, Class<T> type, Supplier<T> computation);
}
