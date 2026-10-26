package dev.stxt.boxes.layout.inline;

/**
 * Métrica aproximada para enseñar y para tests deterministas: cada carácter mide
 * {@code factor × tamaño}. Con el factor 0.5 se aproxima el ancho medio de Helvetica.
 *
 * <p>Es la métrica por defecto de {@link dev.stxt.boxes.layout.Engine} y la de todos los tests
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
    public double width(String text, String font, double fontSize) {
        return text.codePointCount(0, text.length()) * factor * fontSize;
    }
}
