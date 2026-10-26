package dev.stxt.boxes.layout.linea;

/**
 * Métrica aproximada para enseñar y para tests deterministas: cada carácter mide
 * {@code factor × tamaño}. Con el factor 0.5 se aproxima el ancho medio de Helvetica.
 *
 * <p>En el hito 6 se sustituye por una métrica basada en PDFBox:
 * {@code font.getStringWidth(texto) / 1000 * tamano}.</p>
 */
public final class MetricaFija implements TextMeasurer {

    private final double factor;

    public MetricaFija() {
        this(0.5);
    }

    public MetricaFija(double factor) {
        this.factor = factor;
    }

    @Override
    public double ancho(String texto, String fuente, double tamano) {
        return texto.codePointCount(0, texto.length()) * factor * tamano;
    }
}
