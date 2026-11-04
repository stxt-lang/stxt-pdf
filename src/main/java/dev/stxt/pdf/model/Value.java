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

    /**
     * Absolute length in typographic points. A length written in millimeters ({@code 12mm}) is converted to points
     * when it is read (reference/common/values.md).
     *
     * @param pt the length in points; negative where the property allows it
     */
    record Points(double pt) implements Value {}

    /**
     * Percentage of the reference container (or of the font size in {@code Line height}).
     *
     * @param percent the percentage as written: {@code 50} for {@code 50%}
     */
    record Percentage(double percent) implements Value {}

    /**
     * Fraction of free space in grid tracks ({@code *}, {@code 2*}), with its weight.
     *
     * @param weight the weight written before the asterisk: 1 for {@code *}, 2 for {@code 2*}
     */
    record Fraction(double weight) implements Value {}

    /**
     * {@code between(min, max)} in grid tracks.
     *
     * @param min the smallest size of the track; never a fraction
     * @param max the largest size of the track
     */
    record Between(Value min, Value max) implements Value {}

    /** Keywords that depend on the content. */
    enum Keyword implements Value {
        /** {@code auto}: the engine computes the value from the context. */
        AUTO,
        /** {@code narrowest}: the smallest size without overflowing (the longest word). */
        NARROWEST,
        /** {@code natural}: the size without any line break. */
        NATURAL,
        /** {@code fit}: the natural width, without going over the available width or under the narrowest. */
        FIT
    }

    /** {@code auto}: {@link Keyword#AUTO}. */
    Value AUTO = Keyword.AUTO;

    /** Points in a millimeter: 72 points per inch, 25.4 millimeters per inch. */
    double POINTS_PER_MM = 72 / 25.4;

    /**
     * A length in points.
     *
     * @param points the length, in points
     * @return the value {@link Points}
     */
    static Value pt(double points) {
        return new Points(points);
    }

    /**
     * A length written in millimeters, converted to points.
     *
     * @param millimeters the length, in millimeters
     * @return the value {@link Points}, at {@link #POINTS_PER_MM}
     */
    static Value mm(double millimeters) {
        return new Points(millimeters * POINTS_PER_MM);
    }

    /** {@return whether it is {@code auto}} */
    default boolean isAuto() {
        return this == Keyword.AUTO;
    }

    /** {@return whether it is {@code narrowest}, {@code natural} or {@code fit}} */
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

    /**
     * Resolves the value to points against a definite base, as {@link #resolve(OptionalDouble)} does.
     *
     * @param base reference size, in points (usually that of the reference container)
     * @return the points, or empty if the value depends on the content ({@code auto}, intrinsic, fractions)
     */
    default OptionalDouble resolve(double base) {
        return resolve(OptionalDouble.of(base));
    }
}
