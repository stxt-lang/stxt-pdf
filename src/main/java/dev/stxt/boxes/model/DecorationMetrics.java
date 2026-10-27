package dev.stxt.boxes.model;

/**
 * Posición y grosor del subrayado y del tachado de una fuente, en fracciones del cuerpo
 * (ESPECIFICACION.md §5.4). Las posiciones son las de la parte de arriba de la raya, medidas
 * desde la línea base hacia arriba (negativas por debajo), como en las tablas {@code post} y
 * {@code OS/2} del TTF.
 */
public record DecorationMetrics(double underlinePosition, double underlineThickness, double strikeoutPosition,
                                double strikeoutThickness) {

    /** Aproximación sin fuente (salida SVG): cerca de los valores de IBM Plex Sans. */
    public static final DecorationMetrics APPROXIMATE = new DecorationMetrics(-0.1, 0.06, 0.31, 0.06);

    /** Raya del subrayado de un tramo de {@code width} desde {@code x}, con la línea base en {@code baseline}. */
    public Rect underline(double x, double baseline, double width, double fontSize) {
        return new Rect(x, baseline - underlinePosition * fontSize, width, underlineThickness * fontSize);
    }

    /** Raya del tachado, como {@link #underline}. */
    public Rect strikeout(double x, double baseline, double width, double fontSize) {
        return new Rect(x, baseline - strikeoutPosition * fontSize, width, strikeoutThickness * fontSize);
    }
}
