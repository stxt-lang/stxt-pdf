package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.VerticalMetrics;
import dev.stxt.pdf.tree.Box;

/**
 * Measures the width of a text in points.
 *
 * <p>The width has to be additive between words: that of a text is the sum of those of its words and of the
 * spaces that separate them (without kerning between them, and the ligatures within each word). {@link LineBreaker}
 * breaks the lines by adding them up, and only measures the whole line when the sum is very close to the available
 * width.</p>
 */
public interface TextMeasurer {

    double width(String text, FontFace font, double fontSize);

    /**
     * Ascent and descent of the font, to align baselines during the layout (the marker of
     * a list, reference/components/list.md). By default, the approximation without a font, which is the one
     * the SVG painting uses.
     */
    default VerticalMetrics vertical(FontFace font) {
        return VerticalMetrics.APPROXIMATE;
    }

    /**
     * Checks, before the layout, that it can measure all the text of the tree: that its
     * fonts exist and have all their characters. The errors carry the line of the document.
     *
     * @throws dev.stxt.pdf.tree.InvalidModelException if some text cannot be measured
     */
    default void validate(Box root) {
    }
}
