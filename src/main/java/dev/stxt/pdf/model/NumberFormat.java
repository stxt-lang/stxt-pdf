package dev.stxt.pdf.model;

import java.util.Locale;

/**
 * How a number is written: in digits, in letters or in Roman numerals. It is the format of list numberings
 * ({@code Marker}, reference/components/list.md), of a heading level ({@code Number format},
 * reference/components/heading.md), of the footnotes of a section ({@code Footnote number format}) and of its pages
 * ({@code Page number format}, reference/components/document.md).
 */
public enum NumberFormat {
    /** {@code decimal}: in digits ({@code 1}, {@code 2}, {@code 3}). */
    DECIMAL,
    /** {@code lower-alpha}: in lowercase letters ({@code a}, {@code b}, {@code c}). */
    LOWER_ALPHA,
    /** {@code upper-alpha}: in uppercase letters ({@code A}, {@code B}, {@code C}). */
    UPPER_ALPHA,
    /** {@code lower-roman}: in lowercase Roman numerals ({@code i}, {@code ii}, {@code iii}). */
    LOWER_ROMAN,
    /** {@code upper-roman}: in uppercase Roman numerals ({@code I}, {@code II}, {@code III}). */
    UPPER_ROMAN;

    private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] ROMAN_DIGITS = {"m", "cm", "d", "cd", "c", "xc", "l", "xl", "x", "ix", "v", "iv", "i"};

    /**
     * {@return the number written this way ({@code 3}, {@code c}, {@code III})} Letters continue after {@code z} with
     * {@code aa}; Roman numerals, past 3999, with more {@code m}. The caller checks the range if it needs to.
     *
     * @param number the number to write; with letters and Roman numerals, 0 and the negative ones give an empty
     *               string
     */
    public String format(int number) {
        return switch (this) {
            case DECIMAL -> Integer.toString(number);
            case LOWER_ALPHA -> alpha(number);
            case UPPER_ALPHA -> alpha(number).toUpperCase(Locale.ROOT);
            case LOWER_ROMAN -> roman(number);
            case UPPER_ROMAN -> roman(number).toUpperCase(Locale.ROOT);
        };
    }

    /** Letters in bijective base 26: 1 → a, 26 → z, 27 → aa. */
    private static String alpha(int number) {
        StringBuilder s = new StringBuilder();
        for (int n = number; n > 0; n = (n - 1) / 26) {
            s.insert(0, (char) ('a' + (n - 1) % 26));
        }
        return s.toString();
    }

    private static String roman(int number) {
        StringBuilder s = new StringBuilder();
        int n = number;
        for (int i = 0; i < ROMAN_VALUES.length; i++) {
            for (; n >= ROMAN_VALUES[i]; n -= ROMAN_VALUES[i]) {
                s.append(ROMAN_DIGITS[i]);
            }
        }
        return s.toString();
    }
}
