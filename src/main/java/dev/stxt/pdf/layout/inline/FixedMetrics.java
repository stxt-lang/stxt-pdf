package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.model.FontFace;

/**
 * Métrica aproximada para enseñar y para tests deterministas: cada carácter mide
 * {@code factor × tamaño}, sea cual sea la fuente. No necesita la carpeta de fuentes.
 *
 * <p>Es la métrica por defecto de {@link dev.stxt.pdf.layout.Engine} y la de todos los tests
 * de layout. Para la salida PDF se usan las métricas reales de {@link PdfMetrics}.</p>
 */
public final class FixedMetrics implements TextMeasurer {

    private final double factor;

    public FixedMetrics() {
        this(0.5);
    }

    public FixedMetrics(double factor) {
        this.factor = factor;
    }

    @Override
    public double width(String text, FontFace font, double fontSize) {
        return text.codePointCount(0, text.length()) * factor * fontSize;
    }
}
