package dev.stxt.pdf.layout.stack;

/**
 * Data of an item for the distribution along the main axis (reference/components/stack.md). Margin box sizes.
 *
 * @param basis   base size ({@code Base size}, the declared size or the natural width)
 * @param grow    {@code Grow} factor
 * @param shrink  {@code Shrink} factor
 * @param min     minimum; with {@code Min width: auto} it is the size of its content, not exceeding the
 *                declared size (reference/components/stack.md)
 * @param max     maximum; {@link Double#POSITIVE_INFINITY} if there is none
 * @param margins the margins of the item on the main axis, included in the other sizes
 */
public record StackItem(double basis, double grow, double shrink, double min, double max, double margins) {

    public static StackItem of(double basis, double grow, double shrink, double min) {
        return new StackItem(basis, grow, shrink, min, Double.POSITIVE_INFINITY, 0);
    }

    /**
     * Its weight when shrinking: {@code Shrink × Base size}, with the base without the margins
     * (reference/components/stack.md).
     */
    double scaledShrink() {
        return shrink * (basis - margins);
    }

    double hypothetical() {
        return Math.max(min, Math.min(basis, max));
    }

    double clamp(double v) {
        return Math.max(min, Math.min(v, max));
    }
}
