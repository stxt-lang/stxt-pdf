package dev.stxt.pdf.model;

/**
 * Weight of a font: {@code Weight} of {@code Typography}, or {@code Bold}, which is 700
 * (reference/common/typography.md). The document writes it from 100 to 900 in steps of 100; a font file may declare
 * any value from 1 to 1000 ({@code usWeightClass}, reference/common/resources.md), but only those of the document can
 * be requested.
 *
 * @param value the weight, from {@link #MIN} to {@link #MAX}: 400 is regular and 700, bold
 */
public record FontWeight(int value) {

    /** The lowest weight a font file can declare. */
    public static final int MIN = 1;
    /** The highest weight a font file can declare. */
    public static final int MAX = 1000;

    /** 400: the weight without {@code Bold} or {@code Weight}. */
    public static final FontWeight NORMAL = new FontWeight(400);
    /** 700: that of {@code Bold: true} and of a bold run. */
    public static final FontWeight BOLD = new FontWeight(700);

    /**
     * A weight that a font file can declare.
     *
     * @param value the weight
     * @throws IllegalArgumentException if the value is not from {@link #MIN} to {@link #MAX}
     */
    public FontWeight {
        if (value < MIN || value > MAX) {
            throw new IllegalArgumentException("A font weight goes from " + MIN + " to " + MAX + ": " + value);
        }
    }

    /**
     * {@return whether it is bold: 700 or more, which is what the contrast of large text asks
     * (reference/common/pdf.md)}
     */
    public boolean bold() {
        return value >= BOLD.value;
    }

    /**
     * {@return the weight of a bold run in a text of this weight (reference/common/text-formatting.md): 700, or this
     * one if it is already bold}
     */
    public FontWeight bolder() {
        return bold() ? this : BOLD;
    }

    /** For the messages: {@code normal} (400), {@code bold} (700) or {@code weight} and the number. */
    @Override
    public String toString() {
        return value == NORMAL.value ? "normal" : value == BOLD.value ? "bold" : "weight " + value;
    }
}
