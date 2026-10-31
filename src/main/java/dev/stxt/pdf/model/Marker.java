package dev.stxt.pdf.model;

/** Marker of the items of a list: a symbol, none or a numbering. */
public enum Marker {
    BULLET, DASH, NONE, DECIMAL, LOWER_ALPHA, UPPER_ALPHA, LOWER_ROMAN, UPPER_ROMAN;

    /** Whether it numbers the items: only then do {@code Start} and {@code Number} have an effect. */
    public boolean numbered() {
        return compareTo(DECIMAL) >= 0;
    }
}
