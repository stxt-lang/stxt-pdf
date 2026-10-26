package dev.stxt.boxes.paint;

import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Rect;

/** Destino de pintado: {@link SvgCanvas} (depuración) o {@link PdfCanvas}. Coordenadas absolutas en pt. */
public interface Canvas {

    /** Fondo de la caja de relleno (puede ser {@code null}). */
    void background(Rect paddingBox, String color);

    /** Borde alrededor de la caja de borde. */
    void border(Rect borderBox, Insets widths, String color);

    /** Texto con la línea base en {@code baselineY}. */
    void text(double x, double baselineY, String text, String font, double fontSize, String color);
}
