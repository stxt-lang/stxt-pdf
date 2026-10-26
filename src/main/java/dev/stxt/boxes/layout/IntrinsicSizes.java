package dev.stxt.boxes.layout;

/**
 * Tamaños intrínsecos del ancho de una caja de margen (módulo 7, ESPECIFICACION.md §5.9).
 */
public record IntrinsicSizes(double minContent, double maxContent) {

    public static final IntrinsicSizes CERO = new IntrinsicSizes(0, 0);

    /** {@code min(max-content, max(min-content, disponible))}. */
    public double fitContent(double disponible) {
        return Math.min(maxContent, Math.max(minContent, disponible));
    }

    public IntrinsicSizes mas(double extra) {
        return new IntrinsicSizes(minContent + extra, maxContent + extra);
    }
}
