package dev.stxt.boxes.modelo;

import java.util.List;

/**
 * Cuatro valores por lado en el orden de CSS: arriba, derecha, abajo, izquierda.
 * Se usa para {@code Margen}, {@code Relleno}, {@code Borde} y {@code Desplazamiento}
 * (ESPECIFICACION.md §3.3).
 */
public record Lados<T>(T arriba, T derecha, T abajo, T izquierda) {

    public static <T> Lados<T> todos(T valor) {
        return new Lados<>(valor, valor, valor, valor);
    }

    /** Abreviado de 1 a 4 valores, con el mismo convenio que CSS. */
    public static <T> Lados<T> abreviado(List<T> v) {
        return switch (v.size()) {
            case 1 -> new Lados<>(v.get(0), v.get(0), v.get(0), v.get(0));
            case 2 -> new Lados<>(v.get(0), v.get(1), v.get(0), v.get(1));
            case 3 -> new Lados<>(v.get(0), v.get(1), v.get(2), v.get(1));
            case 4 -> new Lados<>(v.get(0), v.get(1), v.get(2), v.get(3));
            default -> throw new IllegalArgumentException("Se esperan de 1 a 4 valores y hay " + v.size());
        };
    }

    public List<T> comoLista() {
        return List.of(arriba, derecha, abajo, izquierda);
    }
}
