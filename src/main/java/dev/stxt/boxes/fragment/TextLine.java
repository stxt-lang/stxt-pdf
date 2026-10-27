package dev.stxt.boxes.fragment;

import dev.stxt.boxes.model.TextFormat;

import java.util.List;

/**
 * Una line box ya colocada, relativa a la caja de contenido de su fragmento.
 *
 * @param x           desplazamiento horizontal por la alineación
 * @param y           parte superior de la línea
 * @param width       ancho del texto de la línea, incluido el espaciado extra de {@code justify}
 * @param height      altura de la línea ({@code Line height})
 * @param text        contenido de la línea; las palabras van separadas por un solo espacio
 * @param wordSpacing espacio que se añade a cada espacio entre palabras ({@code justify}); 0 si no
 * @param runs        tramos de la línea en orden (ESPECIFICACION.md §5.4); su texto seguido es
 *                    {@code text}
 */
public record TextLine(double x, double y, double width, double height, String text, double wordSpacing,
                       List<Run> runs) {

    /**
     * Un tramo de la línea con su formato.
     *
     * @param x     desplazamiento desde el principio de la línea
     * @param width ancho, incluido el espaciado extra de {@code justify} de sus espacios
     */
    public record Run(double x, double width, String text, TextFormat format) {
    }

    public TextLine {
        runs = List.copyOf(runs);
    }

    /** Línea sin formato: un único tramo con todo el texto. */
    public TextLine(double x, double y, double width, double height, String text, double wordSpacing) {
        this(x, y, width, height, text, wordSpacing, List.of(new Run(0, width, text, TextFormat.PLAIN)));
    }

    /** Línea sin formato ni espaciado extra. */
    public TextLine(double x, double y, double width, double height, String text) {
        this(x, y, width, height, text, 0);
    }
}
