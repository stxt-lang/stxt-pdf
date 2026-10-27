package dev.stxt.boxes.layout.inline;

import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.VerticalMetrics;
import dev.stxt.boxes.tree.Box;

/** Mide el ancho de un texto en puntos. */
public interface TextMeasurer {

    double width(String text, FontFace font, double fontSize);

    /**
     * Ascendente y descendente de la fuente, para alinear líneas base al maquetar (el marcador de
     * una lista, ESPECIFICACION.md §5.14). Por defecto, la aproximación sin fuente, que es la que
     * usa el pintado SVG.
     */
    default VerticalMetrics vertical(FontFace font) {
        return VerticalMetrics.APPROXIMATE;
    }

    /**
     * Comprueba, antes de maquetar, que puede medir todo el texto del árbol: que existen sus
     * fuentes y tienen todos sus caracteres. Los errores llevan la línea del documento.
     *
     * @throws dev.stxt.boxes.tree.InvalidModelException si algún texto no se puede medir
     */
    default void validate(Box root) {
    }
}
