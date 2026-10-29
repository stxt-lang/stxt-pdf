package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Alignment;
import dev.stxt.pdf.model.Direction;
import dev.stxt.pdf.model.Distribution;
import dev.stxt.pdf.model.Value;

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
    /** Si {@code Gap} se escribió con dos valores. */
    private boolean gapPair;

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

    /** Si {@code Gap} tiene dos valores ({@code filas columnas}) y no uno para los dos ejes. */
    boolean gapPair() {
        return gapPair;
    }

    void setGap(Value row, Value column, boolean pair) {
        writable();
        rowGap = row;
        columnGap = column;
        gapPair = pair;
    }
}
