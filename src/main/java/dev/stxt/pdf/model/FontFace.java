package dev.stxt.pdf.model;

import java.util.Objects;

/**
 * Font of a text: family, weight and style (reference/common/typography.md). In the PDF output it
 * identifies a TTF or OTF file of the fonts folder (reference/common/resources.md).
 *
 * <p>{@code ligatures} says whether the text is measured and painted with the ligatures of the font
 * ({@code Ligatures}, reference/common/text-formatting.md), and {@code kerning}, whether with the kerning pairs of the
 * font ({@code Kerning}, reference/common/typography.md). They do not change the file: two fonts that only differ in
 * them are the same file ({@link #base()}), but the text measures and paints differently.</p>
 *
 * @param family    the family name ({@code Font}), as the font files declare it
 * @param weight    the weight ({@code Weight}, or {@code Bold})
 * @param style     normal or italic ({@code Italic})
 * @param ligatures whether the text is measured and painted with the ligatures of the font
 * @param kerning   whether the text is measured and painted with the kerning pairs of the font
 */
public record FontFace(String family, FontWeight weight, FontStyle style, boolean ligatures, boolean kerning) {

    /**
     * A font with or without ligatures and kerning.
     *
     * @param family    the family name ({@code Font}); not {@code null}
     * @param weight    the weight; not {@code null}
     * @param style     normal or italic; not {@code null}
     * @param ligatures whether the text uses the ligatures of the font
     * @param kerning   whether the text uses the kerning pairs of the font
     * @throws NullPointerException if the family, the weight or the style is {@code null}
     */
    public FontFace {
        Objects.requireNonNull(family, "family");
        Objects.requireNonNull(weight, "weight");
        Objects.requireNonNull(style, "style");
    }

    /**
     * A font without ligatures or kerning, which is what is requested by default.
     *
     * @param family the family name ({@code Font}); not {@code null}
     * @param weight the weight; not {@code null}
     * @param style  normal or italic; not {@code null}
     */
    public FontFace(String family, FontWeight weight, FontStyle style) {
        this(family, weight, style, false, false);
    }

    /**
     * The same font with or without ligatures.
     *
     * @param ligatures whether the text uses the ligatures of the font
     * @return this font if it already has that value, or a copy with it
     */
    public FontFace withLigatures(boolean ligatures) {
        return this.ligatures == ligatures ? this : new FontFace(family, weight, style, ligatures, kerning);
    }

    /**
     * The same font with or without kerning.
     *
     * @param kerning whether the text uses the kerning pairs of the font
     * @return this font if it already has that value, or a copy with it
     */
    public FontFace withKerning(boolean kerning) {
        return this.kerning == kerning ? this : new FontFace(family, weight, style, ligatures, kerning);
    }

    /** {@return the font as it identifies a font file: without the ligatures and kerning options} */
    public FontFace base() {
        return withLigatures(false).withKerning(false);
    }

    /**
     * {@return the variant for the messages: {@code normal}, {@code bold}, {@code italic}, {@code bold italic} and,
     * with another weight, {@code weight 300} or {@code weight 300 italic}}
     */
    public String variant() {
        if (style != FontStyle.ITALIC) {
            return weight.toString();
        }
        return weight.equals(FontWeight.NORMAL) ? "italic" : weight + " italic";
    }

    @Override
    public String toString() {
        return family + " " + variant();
    }
}
