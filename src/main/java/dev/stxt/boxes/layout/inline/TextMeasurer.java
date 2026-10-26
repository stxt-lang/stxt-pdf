package dev.stxt.boxes.layout.inline;

import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.FontFace;

/** Mide el ancho de un texto en puntos. */
public interface TextMeasurer {

    double width(String text, FontFace font, double fontSize);

    /**
     * Comprueba, antes de maquetar, que puede medir todo el texto del árbol: que existen sus
     * fuentes y tienen todos sus caracteres. Los errores llevan la línea del documento.
     *
     * @throws dev.stxt.boxes.tree.InvalidModelException si algún texto no se puede medir
     */
    default void validate(Box root) {
    }
}
