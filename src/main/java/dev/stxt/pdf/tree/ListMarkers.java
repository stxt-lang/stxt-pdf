package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Marker;
import dev.stxt.pdf.model.NumberFormat;

/**
 * Text of the marker of a list item (reference/components/list.md): a symbol ({@code •},
 * {@code –}) or a number with its suffix ({@code 3.}, {@code c.}, {@code iii.}), followed by the
 * space that separates it from the content.
 */
final class ListMarkers {

    /** The largest Roman numeral that is written without special symbols. */
    static final int MAX_ROMAN = 3999;

    private ListMarkers() {
    }

    /**
     * Text of the marker, with the trailing space; {@code null} with {@code none}. The number only
     * counts in numberings, and it must be within their range ({@link #inRange}).
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
     * The number of an item as its numbering writes it, without the suffix ({@code 3}, {@code c}, {@code III}), which
     * is what {@code {number of: id}} gives (reference/common/references.md); {@code null} if the marker does not
     * number.
     */
    static String number(Marker marker, int number) {
        return marker.numbered() ? NumberFormat.valueOf(marker.name()).format(number) : null;
    }

    /**
     * Whether the numbering can write the number: {@code decimal} from 0, letters from 1, Roman numerals from 1 to
     * 3999.
     */
    static boolean inRange(Marker marker, int number) {
        return switch (marker) {
            case DECIMAL -> number >= 0;
            case LOWER_ALPHA, UPPER_ALPHA -> number >= 1;
            case LOWER_ROMAN, UPPER_ROMAN -> number >= 1 && number <= MAX_ROMAN;
            default -> true;
        };
    }

    /** The range of a numbering, for the messages. */
    static String range(Marker marker) {
        return switch (marker) {
            case DECIMAL -> "from 0";
            case LOWER_ALPHA, UPPER_ALPHA -> "from 1";
            default -> "from 1 to " + MAX_ROMAN;
        };
    }
}
