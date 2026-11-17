package dev.stxt.pdf.model;

/**
 * Marker of the items of a list: a symbol of the font, a shape the engine draws, none, an image
 * ({@code Marker image}) or a numbering.
 */
public enum Marker {
    BULLET, DASH, CIRCLE, SQUARE, CHECKBOX, NONE, IMAGE, DECIMAL, LOWER_ALPHA, UPPER_ALPHA, LOWER_ROMAN, UPPER_ROMAN;

    /** Whether it numbers the items: only then do {@code First number} and {@code Item number} have an effect. */
    public boolean numbered() {
        return compareTo(DECIMAL) >= 0;
    }

    /**
     * Whether the engine draws it as a shape, which does not depend on the font: a hollow circle, a filled square or
     * a checkbox, which is checked in the items with {@code Checked: true}.
     */
    public boolean drawn() {
        return this == CIRCLE || this == SQUARE || this == CHECKBOX;
    }

    /**
     * The character that stands for a drawn marker where it cannot be drawn, such as the text a screen reader gets:
     * {@code ◦}, {@code ▪}, and {@code ☐} or, if {@code checked}, {@code ☑}; {@code null} if it is not drawn.
     */
    public String symbol(boolean checked) {
        return switch (this) {
            case CIRCLE -> "◦";
            case SQUARE -> "▪";
            case CHECKBOX -> checked ? "☑" : "☐";
            default -> null;
        };
    }
}
