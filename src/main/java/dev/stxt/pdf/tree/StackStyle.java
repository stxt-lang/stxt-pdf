package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Alignment;
import dev.stxt.pdf.model.Direction;
import dev.stxt.pdf.model.Distribution;
import dev.stxt.pdf.model.Value;

/**
 * The properties of the {@code Stack} element (reference/components/stack.md): the axis, the distribution and the gap
 * between items and lines. They are not inherited.
 */
public final class StackStyle extends StyleGroup {

    private Direction direction = Direction.HORIZONTAL;
    private boolean wrap;
    /** {@code Horizontal} and {@code Vertical} as they were written; {@code null} = not declared. */
    private Distribution horizontal;
    private Distribution vertical;
    private Distribution lineDistribution = Distribution.STRETCH;
    /** {@code Gap}: between rows and between columns. */
    private Value rowGap = Value.pt(0);
    private Value columnGap = Value.pt(0);
    /** Whether {@code Gap} was written with two values. */
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

    /** Whether the main axis is the horizontal one ({@code Direction: horizontal}). */
    public boolean isHorizontal() {
        return direction == Direction.HORIZONTAL;
    }

    /**
     * {@code Horizontal} or {@code Vertical}, the one of the main axis: the distribution of the free space. By default,
     * {@code start}.
     */
    public Distribution mainAxis() {
        Distribution v = isHorizontal() ? horizontal : vertical;
        return v != null ? v : Distribution.START;
    }

    /**
     * {@code Horizontal} or {@code Vertical}, the one of the cross axis: the alignment of the items. By default,
     * {@code stretch}. {@code ModelValidator.validateStack} checks that it is an alignment.
     */
    public Alignment crossAxis() {
        Distribution v = isHorizontal() ? vertical : horizontal;
        return v != null ? Alignment.valueOf(v.name()) : Alignment.STRETCH;
    }

    /** The key of the property of the main axis ({@code stack.horizontal} or {@code stack.vertical}), for its line. */
    String mainAxisKey() {
        return isHorizontal() ? PropertyKey.STACK_HORIZONTAL : PropertyKey.STACK_VERTICAL;
    }

    /** The key of the property of the cross axis, for its line. */
    String crossAxisKey() {
        return isHorizontal() ? PropertyKey.STACK_VERTICAL : PropertyKey.STACK_HORIZONTAL;
    }

    /** What was written for the cross axis; {@code null} if it was not declared. */
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

    /** Whether {@code Gap} has two values ({@code rows columns}) and not one for both axes. */
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
