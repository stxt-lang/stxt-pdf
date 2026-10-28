package dev.stxt.boxes.tree;

/**
 * Texto del marcador de un ítem de lista (ESPECIFICACION.md §5.14): un símbolo ({@code •},
 * {@code –}) o un número con su sufijo ({@code 3.}, {@code c.}, {@code iii.}), seguido del
 * espacio que lo separa del contenido.
 */
final class ListMarkers {

    /** El mayor número romano que se escribe sin símbolos especiales. */
    static final int MAX_ROMAN = 3999;

    private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] ROMAN_DIGITS = {"m", "cm", "d", "cd", "c", "xc", "l", "xl", "x", "ix", "v", "iv", "i"};

    private ListMarkers() {
    }

    /**
     * Texto del marcador, con el espacio final; {@code null} con {@code none}. El número solo
     * cuenta en las numeraciones, y debe estar en su rango ({@link #inRange}).
     */
    static String text(Style.Marker marker, int number) {
        String label = switch (marker) {
            case NONE -> null;
            case DISC -> "•";
            case DASH -> "–";
            default -> number(marker, number) + ".";
        };
        return label == null ? null : label + " ";
    }

    /**
     * El número de un ítem tal como lo escribe su numeración, sin el sufijo ({@code 3}, {@code c},
     * {@code III}), que es lo que da {@code {number of: id}} (§5.19); {@code null} si el marcador
     * no numera.
     */
    static String number(Style.Marker marker, int number) {
        return switch (marker) {
            case DECIMAL -> Integer.toString(number);
            case LOWER_ALPHA -> alpha(number);
            case UPPER_ALPHA -> alpha(number).toUpperCase();
            case LOWER_ROMAN -> roman(number);
            case UPPER_ROMAN -> roman(number).toUpperCase();
            default -> null;
        };
    }

    /** Si la numeración puede escribir el número: {@code decimal} desde 0, letras desde 1, romanos de 1 a 3999. */
    static boolean inRange(Style.Marker marker, int number) {
        return switch (marker) {
            case DECIMAL -> number >= 0;
            case LOWER_ALPHA, UPPER_ALPHA -> number >= 1;
            case LOWER_ROMAN, UPPER_ROMAN -> number >= 1 && number <= MAX_ROMAN;
            default -> true;
        };
    }

    /** El rango de una numeración, para los mensajes. */
    static String range(Style.Marker marker) {
        return switch (marker) {
            case DECIMAL -> "desde 0";
            case LOWER_ALPHA, UPPER_ALPHA -> "desde 1";
            default -> "de 1 a " + MAX_ROMAN;
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
