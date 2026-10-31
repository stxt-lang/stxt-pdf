package dev.stxt.pdf.model;

/** Resolved widths, in points, of margin, border or padding. */
public record Insets(double top, double right, double bottom, double left) {

    public static final Insets ZERO = new Insets(0, 0, 0, 0);

    public static Insets all(double v) {
        return new Insets(v, v, v, v);
    }

    public double horizontal() {
        return left + right;
    }

    public double vertical() {
        return top + bottom;
    }

    /**
     * Resolves four values against the <strong>width</strong> of the reference container. The
     * percentages of margin and padding, the vertical ones too, are computed on the width
     * (reference/common/values.md). An {@code auto} is 0 here; the layout distributes the {@code auto} margins.
     */
    public static Insets resolve(Sides<Value> sides, double containerWidth) {
        return new Insets(
                sides.top().resolve(containerWidth).orElse(0),
                sides.right().resolve(containerWidth).orElse(0),
                sides.bottom().resolve(containerWidth).orElse(0),
                sides.left().resolve(containerWidth).orElse(0));
    }
}
