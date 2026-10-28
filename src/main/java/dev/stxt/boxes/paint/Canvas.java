package dev.stxt.boxes.paint;

import dev.stxt.boxes.model.DecorationMetrics;
import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.model.VerticalMetrics;
import dev.stxt.boxes.tree.Box;

/** Destino de pintado: {@link SvgCanvas} (depuración) o {@link PdfCanvas}. Coordenadas absolutas en pt. */
public interface Canvas {

    /** Fondo de la caja de relleno (puede ser {@code null}). */
    void background(Rect paddingBox, String color);

    /**
     * Borde alrededor de la caja de borde {@code r}: un fondo por cada lado con ancho, y después
     * el {@link #outline contorno} de depuración.
     */
    default void border(Rect r, Insets w, String color) {
        if (w.top() > 0) {
            background(new Rect(r.x(), r.y(), r.width(), w.top()), color);
        }
        if (w.bottom() > 0) {
            background(new Rect(r.x(), r.bottom() - w.bottom(), r.width(), w.bottom()), color);
        }
        if (w.left() > 0) {
            background(new Rect(r.x(), r.y(), w.left(), r.height()), color);
        }
        if (w.right() > 0) {
            background(new Rect(r.right() - w.right(), r.y(), w.right(), r.height()), color);
        }
        outline(r);
    }

    /** Contorno de depuración de una caja de borde. Por defecto no pinta nada. */
    default void outline(Rect borderBox) {
    }

    /**
     * Texto con la línea base en {@code baselineY}.
     *
     * @param wordSpacing   espacio que se añade a cada espacio entre palabras ({@code justify})
     * @param letterSpacing espacio que se añade tras cada carácter menos el último ({@code Letter spacing})
     */
    void text(double x, double baselineY, String text, FontFace font, double fontSize, String color,
              double wordSpacing, double letterSpacing);

    /**
     * Empieza el texto de un fragmento de la caja: las líneas que siguen, hasta {@link #endText()},
     * son su contenido (el PDF etiquetado lo marca, §5.13). Por defecto no hace nada.
     */
    default void beginText(Box box) {
    }

    /**
     * La imagen de una {@code Image} (ESPECIFICACION.md §5.18) en su caja de contenido
     * {@code content}, colocada según su {@code Fit} ({@link ImageFit}) y recortada a la caja.
     * Por defecto no pinta nada.
     */
    default void image(Box box, Rect content) {
    }

    /**
     * Una zona clicable (ESPECIFICACION.md §5.20): el área {@code area} de la página lleva a
     * {@code destination} ({@link dev.stxt.boxes.model.Links}). {@code box} es la caja a la que
     * pertenece (el PDF etiquetado cuelga el enlace de ella) y {@code description} el texto que
     * se lee en su lugar, que puede ser {@code null}. No cambia lo que se ve. Por defecto no hace
     * nada.
     */
    default void link(Box box, Rect area, String destination, String description) {
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

    /** Posición y grosor del subrayado y del tachado de la fuente (§5.4). Por defecto, la aproximación sin fuente. */
    default DecorationMetrics decoration(FontFace font) {
        return DecorationMetrics.APPROXIMATE;
    }
}
