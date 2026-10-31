package dev.stxt.pdf.layout.grid;

import dev.stxt.pdf.model.Value;

/**
 * Grid track with its minimum and maximum functions (reference/components/grid.md). {@code auto} is equivalent to
 * {@code between(auto, auto)} and {@code *} to {@code between(auto, *)}: a fraction never goes below
 * the narrowest width of its content.
 */
public record Track(Value min, Value max) {

    public static Track of(Value v) {
        if (v instanceof Value.Between b) {
            return new Track(b.min(), b.max());
        }
        if (v instanceof Value.Fraction) {
            return new Track(Value.AUTO, v);
        }
        return new Track(v, v);
    }

    public boolean flexible() {
        return max instanceof Value.Fraction;
    }

    public double fraction() {
        return max instanceof Value.Fraction f ? f.weight() : 0;
    }

    /**
     * The same track without a base for percentages: percentages behave as {@code auto}
     * (reference/components/grid.md).
     */
    Track withoutPercentBase() {
        return new Track(definiteOrAuto(min), definiteOrAuto(max));
    }

    private static Value definiteOrAuto(Value v) {
        return v instanceof Value.Percentage ? Value.AUTO : v;
    }

    /** The minimum function depends on the content. */
    boolean minIntrinsic() {
        return min.isAuto() || min.isIntrinsic();
    }

    /** The maximum function depends on the content. */
    boolean maxIntrinsic() {
        return max.isAuto() || max.isIntrinsic();
    }
}
