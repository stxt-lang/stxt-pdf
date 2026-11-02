package dev.stxt.pdf.model;

/**
 * Ascent, descent and cap height of a font, in fractions of the font size (all positive). They place
 * the baseline within a line (reference/common/typography.md) and size a drop cap (reference/components/text.md).
 */
public record VerticalMetrics(double ascent, double descent, double capHeight) {

    /** Cap height assumed when the font does not say it: 70% of the font size. */
    public static final double APPROXIMATE_CAP_HEIGHT = 0.7;

    /**
     * Approximation without a font: 80% of the font size above the baseline and 20% below, and capitals of
     * {@value #APPROXIMATE_CAP_HEIGHT} of the size.
     */
    public static final VerticalMetrics APPROXIMATE = new VerticalMetrics(0.8, 0.2, APPROXIMATE_CAP_HEIGHT);

    /** Metrics with the approximate cap height. */
    public VerticalMetrics(double ascent, double descent) {
        this(ascent, descent, APPROXIMATE_CAP_HEIGHT);
    }

    /**
     * Baseline of a line that starts at {@code lineTop} and measures {@code lineHeight}: the box
     * of the font (ascent plus descent) is centered in the line: the
     * leading is shared in halves above and below.
     */
    public double baseline(double lineTop, double lineHeight, double fontSize) {
        return lineTop + (lineHeight - (ascent + descent) * fontSize) / 2 + ascent * fontSize;
    }
}
