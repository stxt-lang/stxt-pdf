package dev.stxt.boxes.tree;

import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Rect;

import java.util.OptionalDouble;

/**
 * El medio de salida (ESPECIFICACION.md §4.1). Define el containing block inicial: el área útil.
 *
 * @param type   continuo (una franja de altura infinita) o paginado (una secuencia de páginas)
 * @param width  ancho de la página o de la franja, en pt
 * @param height alto de la página; obligatorio en modo paginado
 * @param margin márgenes de página, en pt
 */
public record Media(Type type, double width, OptionalDouble height, Insets margin) {

    public enum Type { CONTINUOUS, PAGED }

    public boolean paged() {
        return type == Type.PAGED;
    }

    /** Área útil: el containing block inicial y el de las cajas {@code fixed}. */
    public Rect pageArea() {
        double usable = paged()
                ? height.orElseThrow() - margin.vertical()
                : Double.POSITIVE_INFINITY;
        return new Rect(margin.left(), margin.top(), width - margin.horizontal(), usable);
    }

    /** Altura de cada fragmentainer; vacía en modo continuo. */
    public OptionalDouble fragmentainerHeight() {
        return paged() ? OptionalDouble.of(pageArea().height()) : OptionalDouble.empty();
    }
}
