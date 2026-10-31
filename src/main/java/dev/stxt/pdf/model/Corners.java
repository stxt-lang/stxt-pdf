package dev.stxt.pdf.model;

import java.util.List;

/**
 * One value per corner, clockwise from top left: top left, top right, bottom right and bottom left. Used for
 * {@code Corner radius} (reference/common/values.md).
 */
public record Corners<T>(T topLeft, T topRight, T bottomRight, T bottomLeft) {

    public static <T> Corners<T> all(T value) {
        return new Corners<>(value, value, value, value);
    }

    /** Shorthand of 1 to 4 values (reference/common/values.md). */
    public static <T> Corners<T> shorthand(List<T> v) {
        return switch (v.size()) {
            case 1 -> new Corners<>(v.get(0), v.get(0), v.get(0), v.get(0));
            case 2 -> new Corners<>(v.get(0), v.get(1), v.get(0), v.get(1));
            case 3 -> new Corners<>(v.get(0), v.get(1), v.get(2), v.get(1));
            case 4 -> new Corners<>(v.get(0), v.get(1), v.get(2), v.get(3));
            default -> throw new IllegalArgumentException("Expected 1 to 4 values, found " + v.size());
        };
    }

    public List<T> asList() {
        return List.of(topLeft, topRight, bottomRight, bottomLeft);
    }
}
