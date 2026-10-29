package dev.stxt.pdf.layout;

/**
 * Tamaños intrínsecos del ancho de una caja de margen (referencia/comun/caja.md).
 */
public record IntrinsicSizes(double minContent, double maxContent) {

    public static final IntrinsicSizes ZERO = new IntrinsicSizes(0, 0);

    /** {@code min(max-content, max(min-content, disponible))}. */
    public double fitContent(double available) {
        return Math.min(maxContent, Math.max(minContent, available));
    }

    public IntrinsicSizes plus(double extra) {
        return new IntrinsicSizes(minContent + extra, maxContent + extra);
    }
}
