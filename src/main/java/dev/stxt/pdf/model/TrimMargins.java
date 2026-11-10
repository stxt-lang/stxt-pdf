package dev.stxt.pdf.model;

/**
 * Which of the margins that touch a box from inside it drops ({@code Trim margins}, reference/components/block.md):
 * the top margin of its first in-flow child, the bottom margin of its last one, both or none. A dropped margin neither
 * stays inside the box nor comes out of it.
 */
public enum TrimMargins {
    NONE, BOTH, TOP, BOTTOM;

    /** Whether the margins that touch the top of the box are dropped. */
    public boolean top() {
        return this == BOTH || this == TOP;
    }

    /** Whether the margins that touch the bottom of the box are dropped. */
    public boolean bottom() {
        return this == BOTH || this == BOTTOM;
    }
}
