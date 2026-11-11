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
    private boolean wrapItems;
    /** {@code Horizontal} and {@code Vertical} as they were written; {@code null} = not declared. */
    private Distribution horizontal;
    private Distribution vertical;
    private Distribution lineDistribution = Distribution.STRETCH;
    /**
     * The axis of the side {@code Line distribution} was written with: {@code HORIZONTAL} for {@code left} or
     * {@code right}, {@code VERTICAL} for {@code top} or {@code bottom}; {@code null} with a value that names no side.
     */
    private Direction lineDistributionSide;
    /** {@code Gap}: between rows and between columns. */
    private Value rowGap = Value.pt(0);
    private Value columnGap = Value.pt(0);
    /** Whether {@code Gap} was written with two values. */
    private boolean gapPair;
    /**
     * {@code Split items}: a line (in vertical, an item) that does not fit in what is left of the page is broken in
     * parallel on that page instead of moving whole to the next one (reference/common/breaks.md).
     */
    private boolean splitItems = true;

    StackStyle() {
    }

    public Direction direction() {
        return direction;
    }

    void setDirection(Direction v) {
        writable();
        direction = v;
    }

    public boolean wrapItems() {
        return wrapItems;
    }

    void setWrapItems(boolean v) {
        writable();
        wrapItems = v;
    }

    public boolean splitItems() {
        return splitItems;
    }

    void setSplitItems(boolean v) {
        writable();
        splitItems = v;
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

    /** The axis of the side {@code Line distribution} names, or {@code null} ({@link ModelValidator}). */
    Direction lineDistributionSide() {
        return lineDistributionSide;
    }

    void setLineDistribution(Distribution v, Direction side) {
        writable();
        lineDistribution = v;
        lineDistributionSide = side;
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
