package dev.stxt.pdf.model;

/**
 * How the number of a footnote is written ({@code Footnote number format}, reference/components/footnote.md): as a
 * number ({@link NumberFormat}) or as a symbol.
 */
public enum FootnoteNumberFormat {
    /** {@code decimal}: in digits ({@code 1}, {@code 2}, {@code 3}). */
    DECIMAL,
    /** {@code lower-alpha}: in lowercase letters ({@code a}, {@code b}, {@code c}). */
    LOWER_ALPHA,
    /** {@code upper-alpha}: in uppercase letters ({@code A}, {@code B}, {@code C}). */
    UPPER_ALPHA,
    /** {@code lower-roman}: in lowercase Roman numerals ({@code i}, {@code ii}, {@code iii}). */
    LOWER_ROMAN,
    /** {@code upper-roman}: in uppercase Roman numerals ({@code I}, {@code II}, {@code III}). */
    UPPER_ROMAN,
    /** {@code symbols}: the asterisk, the dagger, the double dagger and the section sign, and then doubled. */
    SYMBOLS;

    /** The symbols, in order: {@code *}, {@code †}, {@code ‡} and {@code §}. */
    private static final String[] MARKS = {"*", "†", "‡", "§"};

    /**
     * {@return the number written this way ({@code 3}, {@code c}, {@code III}, {@code ‡})} The symbols go round: the
     * fifth note is {@code **}, the sixth {@code ††}, the ninth {@code ***}.
     *
     * @param number the number of the note, from 1
     */
    public String format(int number) {
        if (this != SYMBOLS) {
            return NumberFormat.valueOf(name()).format(number);
        }
        return MARKS[(number - 1) % MARKS.length].repeat((number - 1) / MARKS.length + 1);
    }
}
