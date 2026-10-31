package dev.stxt.pdf.model;

import java.util.List;

/**
 * Four values per side in CSS order: top, right, bottom, left.
 * Used for {@code Margin}, {@code Padding}, {@code Border} and {@code Offset}
 * (reference/common/values.md).
 */
public record Sides<T>(T top, T right, T bottom, T left) {

    public static <T> Sides<T> all(T value) {
        return new Sides<>(value, value, value, value);
    }

    /** Shorthand of 1 to 4 values, with the same convention as CSS. */
    public static <T> Sides<T> shorthand(List<T> v) {
        return switch (v.size()) {
            case 1 -> new Sides<>(v.get(0), v.get(0), v.get(0), v.get(0));
            case 2 -> new Sides<>(v.get(0), v.get(1), v.get(0), v.get(1));
            case 3 -> new Sides<>(v.get(0), v.get(1), v.get(2), v.get(1));
            case 4 -> new Sides<>(v.get(0), v.get(1), v.get(2), v.get(3));
            default -> throw new IllegalArgumentException("Expected 1 to 4 values, found " + v.size());
        };
    }

    public List<T> asList() {
        return List.of(top, right, bottom, left);
    }
}
