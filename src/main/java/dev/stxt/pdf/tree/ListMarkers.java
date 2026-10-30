package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Marker;
import dev.stxt.pdf.model.NumberStyle;

/**
 * Texto del marcador de un ítem de lista (referencia/componentes/list.md): un símbolo ({@code •},
 * {@code –}) o un número con su sufijo ({@code 3.}, {@code c.}, {@code iii.}), seguido del
 * espacio que lo separa del contenido.
 */
final class ListMarkers {

    /** El mayor número romano que se escribe sin símbolos especiales. */
    static final int MAX_ROMAN = 3999;

    private ListMarkers() {
    }

    /**
     * Texto del marcador, con el espacio final; {@code null} con {@code none}. El número solo
     * cuenta en las numeraciones, y debe estar en su rango ({@link #inRange}).
     */
    static String text(Marker marker, int number) {
        String label = switch (marker) {
            case NONE -> null;
            case BULLET -> "•";
            case DASH -> "–";
            default -> number(marker, number) + ".";
        };
        return label == null ? null : label + " ";
    }

    /**
     * El número de un ítem tal como lo escribe su numeración, sin el sufijo ({@code 3}, {@code c}, {@code III}), que es
     * lo que da {@code {number of: id}} (referencia/comun/referencias.md); {@code null} si el marcador no numera.
     */
    static String number(Marker marker, int number) {
        return marker.numbered() ? NumberStyle.valueOf(marker.name()).format(number) : null;
    }

    /** Si la numeración puede escribir el número: {@code decimal} desde 0, letras desde 1, romanos de 1 a 3999. */
    static boolean inRange(Marker marker, int number) {
        return switch (marker) {
            case DECIMAL -> number >= 0;
            case LOWER_ALPHA, UPPER_ALPHA -> number >= 1;
            case LOWER_ROMAN, UPPER_ROMAN -> number >= 1 && number <= MAX_ROMAN;
            default -> true;
        };
    }

    /** El rango de una numeración, para los mensajes. */
    static String range(Marker marker) {
        return switch (marker) {
            case DECIMAL -> "desde 0";
            case LOWER_ALPHA, UPPER_ALPHA -> "desde 1";
            default -> "de 1 a " + MAX_ROMAN;
        };
    }
}
