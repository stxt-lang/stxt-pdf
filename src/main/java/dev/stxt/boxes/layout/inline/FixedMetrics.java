package dev.stxt.boxes.layout.inline;

/**
 * Métrica aproximada para enseñar y para tests deterministas: cada carácter mide
 * {@code factor × tamaño}. Con el factor 0.5 se aproxima el ancho medio de Helvetica.
 *
 * <p>En el hito 6 se sustituye por una métrica basada en PDFBox:
 * {@code font.getStringWidth(text) / 1000 * fontSize}.</p>
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
