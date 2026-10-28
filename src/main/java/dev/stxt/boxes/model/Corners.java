package dev.stxt.boxes.model;

import java.util.List;

/**
 * Un valor por esquina, en el orden de CSS: arriba a la izquierda, arriba a la derecha, abajo a la
 * derecha y abajo a la izquierda. Se usa para {@code Radius} (ESPECIFICACION.md §3.3).
 */
public record Corners<T>(T topLeft, T topRight, T bottomRight, T bottomLeft) {

    public static <T> Corners<T> all(T value) {
        return new Corners<>(value, value, value, value);
    }

    /** Abreviado de 1 a 4 valores, con el mismo convenio que CSS. */
    public static <T> Corners<T> shorthand(List<T> v) {
        return switch (v.size()) {
            case 1 -> new Corners<>(v.get(0), v.get(0), v.get(0), v.get(0));
            case 2 -> new Corners<>(v.get(0), v.get(1), v.get(0), v.get(1));
            case 3 -> new Corners<>(v.get(0), v.get(1), v.get(2), v.get(1));
            case 4 -> new Corners<>(v.get(0), v.get(1), v.get(2), v.get(3));
            default -> throw new IllegalArgumentException("Se esperan de 1 a 4 valores y hay " + v.size());
        };
    }

    public List<T> asList() {
        return List.of(topLeft, topRight, bottomRight, bottomLeft);
    }
}
