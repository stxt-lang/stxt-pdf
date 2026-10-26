package dev.stxt.boxes.pintado;

import dev.stxt.boxes.modelo.Insets;
import dev.stxt.boxes.modelo.Rect;

/** Destino de pintado: SVG de depuración o PDF (hito 6). Coordenadas absolutas en pt. */
public interface Canvas {

    /** Fondo de la caja de relleno (puede ser {@code null}). */
    void fondo(Rect cajaRelleno, String color);

    /** Borde alrededor de la caja de borde. */
    void borde(Rect cajaBorde, Insets grosores, String color);

    /** Texto con la línea base en {@code yBase}. */
    void texto(double x, double yBase, String texto, String fuente, double tamano, String color);
}
