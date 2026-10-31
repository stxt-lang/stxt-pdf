package dev.stxt.pdf.model;

import java.util.List;

/**
 * Cuatro valores por lado en el orden de CSS: arriba, derecha, abajo, izquierda.
 * Se usa para {@code Margin}, {@code Padding}, {@code Border} y {@code Offset}
 * (reference/common/values.md).
 */
public record Sides<T>(T top, T right, T bottom, T left) {

    public static <T> Sides<T> all(T value) {
        return new Sides<>(value, value, value, value);
    }

    /** Abreviado de 1 a 4 valores, con el mismo convenio que CSS. */
    public static <T> Sides<T> shorthand(List<T> v) {
        return switch (v.size()) {
            case 1 -> new Sides<>(v.get(0), v.get(0), v.get(0), v.get(0));
            case 2 -> new Sides<>(v.get(0), v.get(1), v.get(0), v.get(1));
            case 3 -> new Sides<>(v.get(0), v.get(1), v.get(2), v.get(1));
            case 4 -> new Sides<>(v.get(0), v.get(1), v.get(2), v.get(3));
            default -> throw new IllegalArgumentException("Se esperan de 1 a 4 valores y hay " + v.size());
        };
    }

    public List<T> asList() {
        return List.of(top, right, bottom, left);
    }
}
