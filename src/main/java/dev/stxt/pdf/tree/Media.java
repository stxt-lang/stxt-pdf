package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Rect;

/**
 * El medio de salida (referencia/componentes/document.md): una secuencia de páginas de tamaño fijo. Define
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

    /**
     * Los tamaños de papel con nombre de {@code Paper} (referencia/componentes/document.md), en vertical y en puntos
     * enteros: los de ISO 216 redondeados, como se usan siempre (A4 = 595 × 842), y los de EE. UU.
     */
    public enum Paper {
        A3(842, 1191), A4(595, 842), A5(420, 595), A6(298, 420), B4(709, 1001), B5(499, 709),
        LETTER(612, 792), LEGAL(612, 1008);

        /** Ancho y alto en vertical, en pt. */
        public final double width;
        public final double height;

        Paper(double width, double height) {
            this.width = width;
            this.height = height;
        }
    }

    /**
     * {@code Orientation} (referencia/componentes/document.md): {@code landscape} cambia el ancho por el alto del
     * papel.
     */
    public enum Orientation {
        PORTRAIT, LANDSCAPE
    }
}
