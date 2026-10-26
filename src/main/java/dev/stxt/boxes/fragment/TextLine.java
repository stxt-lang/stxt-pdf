package dev.stxt.boxes.fragment;

/**
 * Una line box ya colocada, relativa a la caja de contenido de su fragmento.
 *
 * @param x           desplazamiento horizontal por la alineación
 * @param y           parte superior de la línea
 * @param width       ancho del texto de la línea, incluido el espaciado extra de {@code justify}
 * @param height      altura de la línea ({@code Line height})
 * @param text        contenido de la línea; las palabras van separadas por un solo espacio
 * @param wordSpacing espacio que se añade a cada espacio entre palabras ({@code justify}); 0 si no
 */
public record TextLine(double x, double y, double width, double height, String text, double wordSpacing) {

    /** Línea sin espaciado extra. */
    public TextLine(double x, double y, double width, double height, String text) {
        this(x, y, width, height, text, 0);
    }
}
