package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Alignment;
import dev.stxt.pdf.model.Area;
import dev.stxt.pdf.model.Cell;
import dev.stxt.pdf.model.Value;

/**
 * Las propiedades de {@code Item} y {@code Area} (reference/components/stack.md y reference/components/grid.md), que
 * pasan al elemento que envuelven: cómo crece y se encoge en un {@code Stack}, dónde va en un {@code Grid} (o la celda
 * que le da la tabla) y su alineación. No se heredan.
 */
public final class ItemStyle extends StyleGroup {

    private double grow = 0;
    private double shrink = 1;
    private Value baseSize = Value.AUTO;
    /**
     * Colocación pedida por {@code Area}, con los nombres ya resueltos, o la que da la tabla a
     * cada {@code Cell}; {@link Cell#AUTO} = colocación automática.
     */
    private Cell area = Cell.AUTO;
    /** {@code null} = la del contenedor. */
    private Alignment horizontal;
    /** {@code null} = la del contenedor. */
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
