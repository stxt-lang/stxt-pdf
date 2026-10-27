package dev.stxt.boxes.tree;

import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Rect;

/**
 * El medio de salida (ESPECIFICACION.md §4.1): una secuencia de páginas de tamaño fijo. Define
 * el containing block inicial: el área útil.
 *
 * @param width  ancho de la página, en pt
 * @param height alto de la página, en pt
 * @param margin márgenes de página, en pt
 */
public record Media(double width, double height, Insets margin) {

    /** Área útil: el containing block inicial y el de las cajas {@code fixed}. */
    public Rect pageArea() {
        return new Rect(margin.left(), margin.top(), width - margin.horizontal(), height - margin.vertical());
    }
}
