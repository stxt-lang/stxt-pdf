package dev.stxt.pdf.model;

import java.util.Locale;

/**
 * Cómo se escribe un número: en cifras, en letras o en números romanos. Es el de las numeraciones de las listas
 * ({@code Marker}, reference/components/list.md), el de un nivel de los encabezados ({@code Number format},
 * reference/components/heading.md) y el de las páginas de una sección ({@code Page number format},
 * reference/components/document.md), que solo admite cifras y romanos.
 */
public enum NumberFormat {
    DECIMAL, LOWER_ALPHA, UPPER_ALPHA, LOWER_ROMAN, UPPER_ROMAN;

    private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] ROMAN_DIGITS = {"m", "cm", "d", "cd", "c", "xc", "l", "xl", "x", "ix", "v", "iv", "i"};

    /**
     * El número escrito así ({@code 3}, {@code c}, {@code III}). Las letras siguen tras la {@code z} con {@code aa};
     * los romanos, pasado 3999, con más {@code m}. Quien llama comprueba el rango si lo necesita.
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

    /** Letras como en CSS: 1 → a, 26 → z, 27 → aa. */
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
