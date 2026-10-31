package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Alignment;
import dev.stxt.pdf.model.Cell;
import dev.stxt.pdf.model.Value;

/**
 * The properties of {@code Item} and {@code Area} (reference/components/stack.md and reference/components/grid.md),
 * which pass to the element they wrap: how it grows and shrinks in a {@code Stack}, where it goes in a {@code Grid}
 * (or the cell the table gives it) and its alignment. They are not inherited.
 */
public final class ItemStyle extends StyleGroup {

    private double grow = 0;
    private double shrink = 1;
    private Value baseSize = Value.AUTO;
    /**
     * Placement requested by {@code Area}, with the names already resolved, or the one the table gives to
     * each {@code Cell}; {@link Cell#AUTO} = automatic placement.
     */
    private Cell area = Cell.AUTO;
    /** {@code null} = that of the container. */
    private Alignment horizontal;
    /** {@code null} = that of the container. */
    private Alignment vertical;

    ItemStyle() {
    }

    public double grow() {
        return grow;
    }

    void setGrow(double v) {
        writable();
        grow = v;
    }

    public double shrink() {
        return shrink;
    }

    void setShrink(double v) {
        writable();
        shrink = v;
    }

    public Value baseSize() {
        return baseSize;
    }

    void setBaseSize(Value v) {
        writable();
        baseSize = v;
    }

    public Cell area() {
        return area;
    }

    void setArea(Cell v) {
        writable();
        area = v;
    }

    public Alignment horizontal() {
        return horizontal;
    }

    void setHorizontal(Alignment v) {
        writable();
        horizontal = v;
    }

    public Alignment vertical() {
        return vertical;
    }

    void setVertical(Alignment v) {
        writable();
        vertical = v;
    }
}
