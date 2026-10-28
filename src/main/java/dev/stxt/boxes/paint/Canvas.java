package dev.stxt.boxes.paint;

import dev.stxt.boxes.model.DecorationMetrics;
import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Paint;
import dev.stxt.boxes.model.Radii;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.model.Sides;
import dev.stxt.boxes.model.VerticalMetrics;
import dev.stxt.boxes.tree.Box;

/** Destino de pintado: {@link SvgCanvas} (depuración) o {@link PdfCanvas}. Coordenadas absolutas en pt. */
public interface Canvas {

    /** Un rectángulo liso de un color: fondos de las columnas, guías, rayas de texto. */
    void background(Rect r, String color);

    /**
     * El fondo de una caja (ESPECIFICACION.md §4.3, §5.6): un color o un degradado que llena
     * {@code paddingBox} con las esquinas {@code radii}, que son las de la caja de relleno.
     */
    void fill(Rect paddingBox, Radii radii, Paint paint);

    /**
     * El borde alrededor de la caja de borde {@code r}: el anillo entre ella (con las esquinas
     * {@code radii}) y la caja de relleno, que se reparte entre los lados con ancho en {@code w}
     * (uno con su color, en la diagonal de cada esquina); después, el {@link #outline contorno} de
     * depuración.
     *
     * @param colors el color de cada lado, ya resuelto
     */
    void border(Rect r, Insets w, Radii radii, Sides<String> colors);

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
     * Empieza el texto de un enlace dentro del texto de la caja (ESPECIFICACION.md §5.20): como
     * {@link #beginText(Box)}, pero lo que sigue, hasta {@link #endText()}, es el contenido de un
     * {@code Link} del PDF etiquetado. Las zonas clicables de ese texto ({@link #link}) se piden
     * después de empezarlo. Por defecto no hace nada.
     */
    default void beginLinkText(Box box) {
    }

    /**
     * La imagen de una {@code Image} (ESPECIFICACION.md §5.18) en su caja de contenido
     * {@code content}, colocada según su {@code Fit} ({@link ImageFit}) y recortada a la caja con
     * las esquinas {@code radii} (las de la caja de contenido). Por defecto no pinta nada.
     */
    default void image(Box box, Rect content, Radii radii) {
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

    /**
     * Empieza a pintar girado (ESPECIFICACION.md §5.6): todo lo que se pinte hasta
     * {@link #endRotation()}, también las zonas de los enlaces, gira {@code degrees} grados en el
     * sentido de las agujas del reloj alrededor del punto ({@code x}, {@code y}) de la página. Los
     * giros se anidan: uno dentro de otro se suma al de fuera. Por defecto no hace nada.
     */
    default void beginRotation(double degrees, double x, double y) {
    }

    /** Termina el giro empezado con {@link #beginRotation}. */
    default void endRotation() {
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
