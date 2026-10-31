package dev.stxt.pdf.model;

import java.util.Objects;

/**
 * Font of a text: family, weight and style (reference/common/typography.md). In the PDF output it
 * identifies a TTF file of the fonts folder (reference/common/resources.md).
 *
 * <p>{@code ligatures} says whether the text is measured and painted with the ligatures of the font
 * ({@code Ligatures}, reference/common/text-formatting.md). It does not change the file: two fonts that only differ
 * in this are the same TTF ({@link #base()}), but the text measures and paints differently.</p>
 */
public record FontFace(String family, FontWeight weight, FontStyle style, boolean ligatures) {

    public FontFace {
        Objects.requireNonNull(family, "family");
        Objects.requireNonNull(weight, "weight");
        Objects.requireNonNull(style, "style");
    }

    /** A font without ligatures, which is what is requested by default. */
    public FontFace(String family, FontWeight weight, FontStyle style) {
        this(family, weight, style, false);
    }

    /** The same font with or without ligatures. */
    public FontFace withLigatures(boolean ligatures) {
        return this.ligatures == ligatures ? this : new FontFace(family, weight, style, ligatures);
    }

    /** The font as it identifies a TTF file: without the ligatures option. */
    public FontFace base() {
        return withLigatures(false);
    }

    /** Variant for the messages: {@code normal}, {@code bold}, {@code italic} or {@code bold italic}. */
    public String variant() {
        boolean bold = weight == FontWeight.BOLD;
        boolean italic = style == FontStyle.ITALIC;
        if (bold && italic) {
            return "bold italic";
        }
        return bold ? "bold" : italic ? "italic" : "normal";
    }

    @Override
    public String toString() {
        return family + " " + variant();
    }
}
