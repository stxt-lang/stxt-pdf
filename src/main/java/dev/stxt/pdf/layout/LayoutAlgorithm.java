package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.BreakToken;
import dev.stxt.pdf.tree.Box;

/**
 * Common interface of the layout algorithms: block, line, stack and grid (modules 7 and 8).
 */
public interface LayoutAlgorithm {

    /**
     * Narrowest width and natural width of the box's content, without its border, padding, margin or
     * {@code Width}. It does not depend on the available space. Those of the margin box are given by
     * {@link Engine#intrinsic} with {@link BoxModel#intrinsic}.
     */
    IntrinsicSizes contentIntrinsic(Box box);

    /**
     * Computes the geometry of the box and its descendants.
     *
     * @param from token to resume from, or {@code null} to start
     */
    LayoutResult layout(Box box, ConstraintSpace space, BreakToken from);
}
