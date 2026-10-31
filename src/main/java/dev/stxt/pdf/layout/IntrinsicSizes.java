package dev.stxt.pdf.layout;

/**
 * Tamaños intrínsecos del ancho de una caja de margen (reference/common/box.md).
 */
public record IntrinsicSizes(double narrowest, double natural) {

    public static final IntrinsicSizes ZERO = new IntrinsicSizes(0, 0);

    /** El ancho natural, sin pasar de lo disponible ni bajar del más estrecho. */
    public double fit(double available) {
        return Math.min(natural, Math.max(narrowest, available));
    }

    public IntrinsicSizes plus(double extra) {
        return new IntrinsicSizes(narrowest + extra, natural + extra);
    }
}
