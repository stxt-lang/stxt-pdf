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

/**
 * Destino de pintado: {@link SvgCanvas} (depuración) o {@link PdfCanvas}. Coordenadas absolutas en pt. Solo
 * dibuja; lo que dice qué es lo dibujado (el texto marcado del PDF etiquetado y los enlaces) lo hace su
 * {@link #tagging()}, que en un lienzo que no lo entiende es {@link Tagging#NONE} (AR-05).
 */
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

    /**
     * Lo que hace el lienzo con el significado de lo que pinta: el texto marcado del PDF etiquetado y los
     * enlaces; {@link Tagging#NONE} si solo dibuja.
     */
    Tagging tagging();

    /** Contorno de depuración de una caja de borde, si el lienzo pinta en modo de depuración. */
    void outline(Rect borderBox);

    /**
     * Texto con la línea base en {@code baselineY}.
     *
     * @param wordSpacing   espacio que se añade a cada espacio entre palabras ({@code justify})
     * @param letterSpacing espacio que se añade tras cada carácter menos el último ({@code Letter spacing})
     */
    void text(double x, double baselineY, String text, FontFace font, double fontSize, String color,
              double wordSpacing, double letterSpacing);

    /**
     * La imagen de una {@code Image} (ESPECIFICACION.md §5.18) en su caja de contenido
     * {@code content}, colocada según su {@code Fit} ({@link ImageFit}) y recortada a la caja con
     * las esquinas {@code radii} (las de la caja de contenido).
     */
    void image(Box box, Rect content, Radii radii);

    /**
     * Empieza a pintar girado (ESPECIFICACION.md §5.6): todo lo que se pinte hasta
     * {@link #endRotation()}, también las zonas de los enlaces, gira {@code degrees} grados en el
     * sentido de las agujas del reloj alrededor del punto ({@code x}, {@code y}) de la página. Los
     * giros se anidan: uno dentro de otro se suma al de fuera.
     */
    void beginRotation(double degrees, double x, double y);

    /** Termina el giro empezado con {@link #beginRotation}. */
    void endRotation();

    /**
     * Ascendente y descendente con que se pinta la fuente, para colocar la línea base
     * (ESPECIFICACION.md §5.4); sin la fuente, {@link VerticalMetrics#APPROXIMATE}.
     */
    VerticalMetrics vertical(FontFace font);

    /**
     * Posición y grosor del subrayado y del tachado de la fuente (§5.4); sin la fuente,
     * {@link DecorationMetrics#APPROXIMATE}.
     */
    DecorationMetrics decoration(FontFace font);
}
