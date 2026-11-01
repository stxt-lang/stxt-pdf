package dev.stxt.pdf.model;

/**
 * Position and thickness of the underline and the strikeout of a font, in fractions of the font size
 * (reference/common/text-formatting.md). The positions are those of the top of the stroke, measured
 * upward from the baseline (negative below), as in the {@code post} and {@code OS/2} tables of
 * the font file.
 */
public record DecorationMetrics(double underlinePosition, double underlineThickness, double strikeoutPosition,
                                double strikeoutThickness) {

    /** Approximation without a font (SVG output): close to the values of IBM Plex Sans. */
    public static final DecorationMetrics APPROXIMATE = new DecorationMetrics(-0.1, 0.06, 0.31, 0.06);

    /** Underline stroke of a run of {@code width} from {@code x}, with the baseline at {@code baseline}. */
    public Rect underline(double x, double baseline, double width, double fontSize) {
        return new Rect(x, baseline - underlinePosition * fontSize, width, underlineThickness * fontSize);
    }

    /** Strikeout stroke, like {@link #underline}. */
    public Rect strikeout(double x, double baseline, double width, double fontSize) {
        return new Rect(x, baseline - strikeoutPosition * fontSize, width, strikeoutThickness * fontSize);
    }
}
