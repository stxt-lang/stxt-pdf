package dev.stxt.boxes.paint;

import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.model.VerticalMetrics;
import dev.stxt.boxes.tree.Box;

/** Destino de pintado: {@link SvgCanvas} (depuración) o {@link PdfCanvas}. Coordenadas absolutas en pt. */
public interface Canvas {

    /** Fondo de la caja de relleno (puede ser {@code null}). */
    void background(Rect paddingBox, String color);

    /** Borde alrededor de la caja de borde. */
    void border(Rect borderBox, Insets widths, String color);

    /**
     * Texto con la línea base en {@code baselineY}.
     *
     * @param wordSpacing espacio que se añade a cada espacio entre palabras ({@code justify})
     */
    void text(double x, double baselineY, String text, FontFace font, double fontSize, String color, double wordSpacing);

    /**
     * Empieza el texto de un fragmento de la caja: las líneas que siguen, hasta {@link #endText()},
     * son su contenido (el PDF etiquetado lo marca, §5.13). Por defecto no hace nada.
     */
    default void beginText(Box box) {
    }

    /** Termina el texto empezado con {@link #beginText(Box)}. */
    default void endText() {
    }

    /**
     * Ascendente y descendente con que se pinta la fuente, para colocar la línea base
     * (ESPECIFICACION.md §5.4). Por defecto, la aproximación sin fuente.
     */
    default VerticalMetrics vertical(FontFace font) {
        return VerticalMetrics.APPROXIMATE;
    }
}
