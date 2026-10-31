package dev.stxt.pdf.model;

import java.util.OptionalDouble;

/**
 * <em>Specified</em> value of a property, as the author writes it
 * (reference/common/values.md). The layout turns it into a <em>used</em> value, in points,
 * with {@link #resolve(OptionalDouble)}.
 *
 * <p>Specified values are kept apart from used values because stack and grid
 * need to know whether a value was originally {@code auto} (reference/common/values.md).</p>
 */
public sealed interface Value {

    /** Absolute length in typographic points. */
    record Points(double pt) implements Value {}

    /** Percentage of the reference container (or of the font size in {@code Line height}). */
    record Percentage(double percent) implements Value {}

    /** Fraction of free space in grid tracks ({@code *}, {@code 2*}), with its weight. */
    record Fraction(double weight) implements Value {}

    /** {@code between(min, max)} in grid tracks. */
    record Between(Value min, Value max) implements Value {}

    /** Keywords that depend on the content. */
    enum Keyword implements Value { AUTO, NARROWEST, NATURAL, FIT }

    Value AUTO = Keyword.AUTO;

    static Value pt(double points) {
        return new Points(points);
    }

    default boolean isAuto() {
        return this == Keyword.AUTO;
    }

    /** {@code narrowest}, {@code natural} or {@code fit}. */
    default boolean isIntrinsic() {
        return this instanceof Keyword k && k != Keyword.AUTO;
    }

    /**
     * Resolves the value to points.
     *
     * @param base reference size (usually that of the reference container); empty if it is indefinite
     * @return the points, or empty if the value depends on the content ({@code auto}, intrinsic,
     *         fractions) or is a percentage of an indefinite base (reference/common/box.md)
     */
    default OptionalDouble resolve(OptionalDouble base) {
        if (this instanceof Points p) {
            return OptionalDouble.of(p.pt());
        }
        if (this instanceof Percentage p) {
            return base.isPresent()
                    ? OptionalDouble.of(base.getAsDouble() * p.percent() / 100.0)
                    : OptionalDouble.empty();
        }
        return OptionalDouble.empty();
    }

    default OptionalDouble resolve(double base) {
        return resolve(OptionalDouble.of(base));
    }
}
