package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Marker;

/**
 * The properties of lists (reference/components/list.md): the {@code Marker} and the {@code Start} of a {@code
 * List} and the {@code Number} of a {@code List item}. They are not inherited.
 */
public final class ListStyle extends StyleGroup {

    /** Marker of the items of a {@code List}; {@code null} in the other boxes. */
    private Marker marker;
    /** Number of the first item of a {@code List}. */
    private int start = 1;
    /** Number that a {@code List item} sets; {@code null} = the one after that of the previous item. */
    private Integer number;

    ListStyle() {
    }

    public Marker marker() {
        return marker;
    }

    void setMarker(Marker v) {
        writable();
        marker = v;
    }

    public int start() {
        return start;
    }

    void setStart(int v) {
        writable();
        start = v;
    }

    public Integer number() {
        return number;
    }

    void setNumber(Integer v) {
        writable();
        number = v;
    }
}
