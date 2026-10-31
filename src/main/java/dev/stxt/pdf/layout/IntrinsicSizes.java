package dev.stxt.pdf.layout;

/**
 * Intrinsic sizes of the width of a margin box (reference/common/box.md).
 */
public record IntrinsicSizes(double narrowest, double natural) {

    public static final IntrinsicSizes ZERO = new IntrinsicSizes(0, 0);

    /** The natural width, not exceeding what is available nor going below the narrowest. */
    public double fit(double available) {
        return Math.min(natural, Math.max(narrowest, available));
    }

    public IntrinsicSizes plus(double extra) {
        return new IntrinsicSizes(narrowest + extra, natural + extra);
    }
}
