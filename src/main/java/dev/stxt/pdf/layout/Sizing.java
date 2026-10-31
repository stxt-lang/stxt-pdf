package dev.stxt.pdf.layout;

import dev.stxt.pdf.model.Alignment;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Value;

import java.util.OptionalDouble;

/**
 * Common sizing rules (modules 1 and 7, reference/common/box.md).
 */
public final class Sizing {

    private Sizing() {
    }

    /** Applies the maximum first and then the minimum: if they clash, the minimum wins. */
    public static double constrain(double provisional, double min, OptionalDouble max) {
        double v = max.isPresent() ? Math.min(provisional, max.getAsDouble()) : provisional;
        return Math.max(v, min);
    }

    /**
     * Border box width of a box in block flow with {@code Width: auto}: it takes up the
     * reference container minus its horizontal margins.
     */
    public static double autoWidthInFlow(double containingBlockWidth, Insets margin) {
        return Math.max(0, containingBlockWidth - margin.horizontal());
    }

    /**
     * Horizontal margins of a box in block flow with a known width: if both are
     * {@code auto}, the leftover is shared and the box is centered; if only one is {@code auto},
     * it absorbs the leftover.
     *
     * @return {left, right}
     */
    public static double[] horizontalMargins(double containingBlockWidth, double borderBoxWidth,
                                             Value marginLeft, Value marginRight) {
        double left = marginLeft.resolve(containingBlockWidth).orElse(0);
        double right = marginRight.resolve(containingBlockWidth).orElse(0);
        double remaining = containingBlockWidth - borderBoxWidth - left - right;
        if (marginLeft.isAuto() && marginRight.isAuto()) {
            double each = Math.max(0, remaining) / 2;
            return new double[]{each, each};
        }
        if (marginLeft.isAuto()) {
            return new double[]{Math.max(0, remaining), right};
        }
        if (marginRight.isAuto()) {
            return new double[]{left, Math.max(0, remaining)};
        }
        return new double[]{left, right};
    }

    /**
     * Offset of a box of size {@code size} aligned in a space {@code space}
     * (reference/components/stack.md and reference/components/grid.md). With {@code stretch} the box already takes
     * up the space and starts at the beginning.
     */
    public static double align(Alignment alignment, double space, double size) {
        return switch (alignment) {
            case START, STRETCH -> 0;
            case END -> space - size;
            case CENTER -> (space - size) / 2;
        };
    }

    public static double sum(double[] v) {
        double r = 0;
        for (double x : v) {
            r += x;
        }
        return r;
    }
}
