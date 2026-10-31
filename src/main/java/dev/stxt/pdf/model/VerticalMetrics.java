package dev.stxt.pdf.model;

/**
 * Ascent and descent of a font, in fractions of the font size (both positive). They place
 * the baseline within a line (reference/common/typography.md).
 */
public record VerticalMetrics(double ascent, double descent) {

    /** Approximation without a font: 80% of the font size above the baseline and 20% below. */
    public static final VerticalMetrics APPROXIMATE = new VerticalMetrics(0.8, 0.2);

    /**
     * Baseline of a line that starts at {@code lineTop} and measures {@code lineHeight}: the box
     * of the font (ascent plus descent) is centered in the line: the
     * leading is shared in halves above and below.
     */
    public double baseline(double lineTop, double lineHeight, double fontSize) {
        return lineTop + (lineHeight - (ascent + descent) * fontSize) / 2 + ascent * fontSize;
    }
}
