package dev.stxt.pdf.model;

/**
 * What follows the number in the marker of a numbered list ({@code Marker suffix}, reference/components/list.md): a
 * period ({@code 3.}, {@code c.}) or a closing parenthesis ({@code 3)}, {@code c)}).
 */
public enum MarkerSuffix {
    PERIOD("."), PARENTHESIS(")");

    private final String text;

    MarkerSuffix(String text) {
        this.text = text;
    }

    /** The suffix as it is written after the number. */
    public String text() {
        return text;
    }
}
