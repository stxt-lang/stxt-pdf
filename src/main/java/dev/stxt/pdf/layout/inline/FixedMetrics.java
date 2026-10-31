package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.model.FontFace;

/**
 * Approximate metrics for teaching and for deterministic tests: each character measures
 * {@code factor × size}, whatever the font. It does not need the fonts folder.
 *
 * <p>It is the default metrics of {@link dev.stxt.pdf.layout.Engine} and the one of all the layout
 * tests. The PDF output uses the real metrics of {@link PdfMetrics}.</p>
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
    public double width(String text, FontFace font, double fontSize) {
        return text.codePointCount(0, text.length()) * factor * fontSize;
    }
}
