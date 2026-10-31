package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Alignment;
import dev.stxt.pdf.model.Direction;
import dev.stxt.pdf.model.GridAreas;
import dev.stxt.pdf.model.Value;

import java.util.List;

/**
 * The properties of the {@code Grid} and {@code Table} elements (reference/components/grid.md): the tracks, the
 * areas, automatic placement, the alignment of the items and the gap between tracks. They are not inherited.
 */
public final class GridStyle extends StyleGroup {

    private List<Value> columnSizes = List.of();
    private List<Value> rowSizes = List.of();
    /** {@code Areas} already interpreted; {@link GridAreas#NONE} if it is not declared. */
    private GridAreas areas = GridAreas.NONE;
    private Direction flow = Direction.HORIZONTAL;
    private Alignment horizontal = Alignment.STRETCH;
    private Alignment vertical = Alignment.STRETCH;
    /** {@code Gap}: between rows ({@code row}) and between columns ({@code column}). */
    private Value rowGap = Value.pt(0);
    private Value columnGap = Value.pt(0);

    GridStyle() {
    }

    public List<Value> columnSizes() {
        return columnSizes;
    }

    void setColumnSizes(List<Value> v) {
        writable();
        columnSizes = v;
    }

    public List<Value> rowSizes() {
        return rowSizes;
    }

    void setRowSizes(List<Value> v) {
        writable();
        rowSizes = v;
    }

    public GridAreas areas() {
        return areas;
    }

    void setAreas(GridAreas v) {
        writable();
        areas = v;
    }

    public Direction flow() {
        return flow;
    }

    void setFlow(Direction v) {
        writable();
        flow = v;
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
