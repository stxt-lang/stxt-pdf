package dev.stxt.boxes.tree;

import dev.stxt.boxes.model.Alignment;
import dev.stxt.boxes.model.Area;
import dev.stxt.boxes.model.Cell;
import dev.stxt.boxes.model.Value;

/**
 * Las propiedades de {@code Item} y {@code Area} (referencia/componentes/flex.md y referencia/componentes/grid.md), que
 * pasan al elemento que envuelven: cómo crece y se encoge en un {@code Flex}, dónde va en un {@code Grid} (o la celda
 * que le da la tabla) y su alineación. No se heredan.
 */
public final class ItemStyle extends StyleGroup {

    private double grow = 0;
    private double shrink = 1;
    private Value basis = Value.AUTO;
    /** {@code null} = la del contenedor. */
    private Alignment crossAxis;
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

    public Value basis() {
        return basis;
    }

    void setBasis(Value v) {
        writable();
        basis = v;
    }

    public Alignment crossAxis() {
        return crossAxis;
    }

    void setCrossAxis(Alignment v) {
        writable();
        crossAxis = v;
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
