package dev.stxt.pdf.model;

/**
 * Resolved widths, in points, of margin, border or padding.
 *
 * @param top    the width on the top side, in points
 * @param right  the width on the right side, in points
 * @param bottom the width on the bottom side, in points
 * @param left   the width on the left side, in points
 */
public record Insets(double top, double right, double bottom, double left) {

    /** No width on any side. */
    public static final Insets ZERO = new Insets(0, 0, 0, 0);

    /**
     * The same width on the four sides.
     *
     * @param v the width, in points
     * @return the insets with {@code v} on every side
     */
    public static Insets all(double v) {
        return new Insets(v, v, v, v);
    }

    /** {@return the sum of the left and right widths} */
    public double horizontal() {
        return left + right;
    }

    /** {@return the sum of the top and bottom widths} */
    public double vertical() {
        return top + bottom;
    }

    /**
     * Resolves four values against the <strong>width</strong> of the reference container. The
     * percentages of margin and padding, the vertical ones too, are computed on the width
     * (reference/common/values.md). An {@code auto} is 0 here; the layout distributes the {@code auto} margins.
     *
     * @param sides          the specified values of the four sides
     * @param containerWidth the width of the reference container, in points: the base of every percentage
     * @return the widths in points; 0 on a side whose value does not resolve to points ({@code auto}, intrinsic,
     *         fraction)
     */
    public static Insets resolve(Sides<Value> sides, double containerWidth) {
        return new Insets(
                sides.top().resolve(containerWidth).orElse(0),
                sides.right().resolve(containerWidth).orElse(0),
                sides.bottom().resolve(containerWidth).orElse(0),
                sides.left().resolve(containerWidth).orElse(0));
    }
}
