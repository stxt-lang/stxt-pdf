package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Alignment;
import dev.stxt.pdf.model.FillOrder;
import dev.stxt.pdf.model.AreaMap;
import dev.stxt.pdf.model.Value;

import java.util.List;

/**
 * The properties of the {@code Grid} and {@code Table} elements (reference/components/grid.md): the tracks, the
 * areas, automatic placement, the alignment of the items and the gap between tracks. They are not inherited.
 */
public final class GridStyle extends StyleGroup {

    private List<Value> columnSizes = List.of();
    private List<Value> rowSizes = List.of();
    /** {@code Area map} already interpreted; {@link AreaMap#NONE} if it is not declared. */
    private AreaMap areaMap = AreaMap.NONE;
    private FillOrder fillOrder = FillOrder.ROWS;
    private Alignment horizontal = Alignment.STRETCH;
    private Alignment vertical = Alignment.STRETCH;
    /** {@code Gap}: between rows ({@code row}) and between columns ({@code column}). */
    private Value rowGap = Value.pt(0);
    private Value columnGap = Value.pt(0);
    /**
     * {@code Split rows}: a row that does not fit in what is left of the page is broken in parallel on that page
     * instead of moving whole to the next one (reference/common/breaks.md).
     */
    private boolean splitRows = true;

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

    public AreaMap areaMap() {
        return areaMap;
    }

    void setAreaMap(AreaMap v) {
        writable();
        areaMap = v;
    }

    public FillOrder fillOrder() {
        return fillOrder;
    }

    void setFillOrder(FillOrder v) {
        writable();
        fillOrder = v;
    }

    public boolean splitRows() {
        return splitRows;
    }

    void setSplitRows(boolean v) {
        writable();
        splitRows = v;
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
