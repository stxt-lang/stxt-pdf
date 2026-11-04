package dev.stxt.pdf.model;

import java.util.List;

/**
 * Four values, one per side, clockwise from the top: top, right, bottom, left.
 * Used for {@code Margin}, {@code Padding}, {@code Border} and {@code Offset}
 * (reference/common/values.md).
 *
 * @param <T>    the type of the values
 * @param top    the value of the top side
 * @param right  the value of the right side
 * @param bottom the value of the bottom side
 * @param left   the value of the left side
 */
public record Sides<T>(T top, T right, T bottom, T left) {

    /**
     * The same value on the four sides.
     *
     * @param <T>   the type of the value
     * @param value the value of every side
     * @return the four sides with {@code value}
     */
    public static <T> Sides<T> all(T value) {
        return new Sides<>(value, value, value, value);
    }

    /**
     * Shorthand of 1 to 4 values (reference/common/values.md): one value is every side; two are top and bottom, then
     * right and left; three are top, then right and left, then bottom; four go clockwise from the top.
     *
     * @param <T> the type of the values
     * @param v   the values as written, 1 to 4
     * @return the four sides
     * @throws IllegalArgumentException if there are not 1 to 4 values
     */
    public static <T> Sides<T> shorthand(List<T> v) {
        return switch (v.size()) {
            case 1 -> new Sides<>(v.get(0), v.get(0), v.get(0), v.get(0));
            case 2 -> new Sides<>(v.get(0), v.get(1), v.get(0), v.get(1));
            case 3 -> new Sides<>(v.get(0), v.get(1), v.get(2), v.get(1));
            case 4 -> new Sides<>(v.get(0), v.get(1), v.get(2), v.get(3));
            default -> throw new IllegalArgumentException("Expected 1 to 4 values, found " + v.size());
        };
    }

    /** {@return the four values in the order top, right, bottom, left} */
    public List<T> asList() {
        return List.of(top, right, bottom, left);
    }
}
