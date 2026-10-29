package dev.stxt.boxes.model;

/**
 * Ascendente y descendente de una fuente, en fracciones del cuerpo (los dos positivos). Colocan
 * la línea base dentro de una línea (referencia/comun/tipografia.md).
 */
public record VerticalMetrics(double ascent, double descent) {

    /** Aproximación sin fuente: el 80 % del cuerpo por encima de la línea base y el 20 % por debajo. */
    public static final VerticalMetrics APPROXIMATE = new VerticalMetrics(0.8, 0.2);

    /**
     * Línea base de una línea que empieza en {@code lineTop} y mide {@code lineHeight}: la caja
     * de la fuente (ascendente más descendente) queda centrada en la línea, como el
     * <em>half-leading</em> de CSS.
     */
    public double baseline(double lineTop, double lineHeight, double fontSize) {
        return lineTop + (lineHeight - (ascent + descent) * fontSize) / 2 + ascent * fontSize;
    }
}
