package dev.stxt.boxes.tree;

import dev.stxt.boxes.model.Alignment;
import dev.stxt.boxes.model.Direction;
import dev.stxt.boxes.model.Distribution;
import dev.stxt.boxes.model.Value;

/**
 * Las propiedades del elemento {@code Flex} (referencia/componentes/flex.md): el eje, el reparto y el hueco entre ítems
 * y líneas. No se heredan.
 */
public final class FlexStyle extends StyleGroup {

    private Direction direction = Direction.ROW;
    private boolean wrap;
    private Distribution mainAxis = Distribution.START;
    private Alignment crossAxis = Alignment.STRETCH;
    private Distribution lines = Distribution.STRETCH;
    /** {@code Gap}: entre líneas ({@code row}) y entre ítems de una línea ({@code column}). */
    private Value rowGap = Value.pt(0);
    private Value columnGap = Value.pt(0);

    FlexStyle() {
    }

    public Direction direction() {
        return direction;
    }

    void setDirection(Direction v) {
        writable();
        direction = v;
    }

    public boolean wrap() {
        return wrap;
    }

    void setWrap(boolean v) {
        writable();
        wrap = v;
    }

    public Distribution mainAxis() {
        return mainAxis;
    }

    void setMainAxis(Distribution v) {
        writable();
        mainAxis = v;
    }

    public Alignment crossAxis() {
        return crossAxis;
    }

    void setCrossAxis(Alignment v) {
        writable();
        crossAxis = v;
    }

    public Distribution lines() {
        return lines;
    }

    void setLines(Distribution v) {
        writable();
        lines = v;
    }

    public Value rowGap() {
        return rowGap;
    }

    public Value columnGap() {
        return columnGap;
    }

    void setGap(Value row, Value column) {
        writable();
        rowGap = row;
        columnGap = column;
    }
}
