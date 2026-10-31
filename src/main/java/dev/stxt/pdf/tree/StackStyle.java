package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Alignment;
import dev.stxt.pdf.model.Direction;
import dev.stxt.pdf.model.Distribution;
import dev.stxt.pdf.model.Value;

/**
 * Las propiedades del elemento {@code Stack} (referencia/componentes/stack.md): el eje, el reparto y el hueco entre ítems
 * y líneas. No se heredan.
 */
public final class StackStyle extends StyleGroup {

    private Direction direction = Direction.HORIZONTAL;
    private boolean wrap;
    /** {@code Horizontal} y {@code Vertical} como se escribieron; {@code null} = sin declarar. */
    private Distribution horizontal;
    private Distribution vertical;
    private Distribution lineDistribution = Distribution.STRETCH;
    /** {@code Gap}: entre filas y entre columnas. */
    private Value rowGap = Value.pt(0);
    private Value columnGap = Value.pt(0);
    /** Si {@code Gap} se escribió con dos valores. */
    private boolean gapPair;

    StackStyle() {
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

    /** Si el eje principal es el horizontal ({@code Direction: horizontal}). */
    public boolean isHorizontal() {
        return direction == Direction.HORIZONTAL;
    }

    /**
     * {@code Horizontal} o {@code Vertical}, la del eje principal: el reparto del sobrante. Por defecto, {@code start}.
     */
    public Distribution mainAxis() {
        Distribution v = isHorizontal() ? horizontal : vertical;
        return v != null ? v : Distribution.START;
    }

    /**
     * {@code Horizontal} o {@code Vertical}, la del eje cruzado: la alineación de los ítems. Por defecto, {@code
     * stretch}. Que sea una alineación lo comprueba {@code ModelValidator.validateStack}.
     */
    public Alignment crossAxis() {
        Distribution v = isHorizontal() ? vertical : horizontal;
        return v != null ? Alignment.valueOf(v.name()) : Alignment.STRETCH;
    }

    /** La clave de la propiedad del eje principal ({@code stack.horizontal} o {@code stack.vertical}), para su línea. */
    String mainAxisKey() {
        return isHorizontal() ? PropertyKey.STACK_HORIZONTAL : PropertyKey.STACK_VERTICAL;
    }

    /** La clave de la propiedad del eje cruzado, para su línea. */
    String crossAxisKey() {
        return isHorizontal() ? PropertyKey.STACK_VERTICAL : PropertyKey.STACK_HORIZONTAL;
    }

    /** Lo que se escribió en el eje cruzado; {@code null} si no se declaró. */
    Distribution crossDeclared() {
        return isHorizontal() ? vertical : horizontal;
    }

    void setHorizontal(Distribution v) {
        writable();
        horizontal = v;
    }

    void setVertical(Distribution v) {
        writable();
        vertical = v;
    }

    public Distribution lineDistribution() {
        return lineDistribution;
    }

    void setLineDistribution(Distribution v) {
        writable();
        lineDistribution = v;
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
