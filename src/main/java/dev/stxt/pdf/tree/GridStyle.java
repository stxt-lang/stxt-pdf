package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Alignment;
import dev.stxt.pdf.model.Direction;
import dev.stxt.pdf.model.GridAreas;
import dev.stxt.pdf.model.Value;

import java.util.List;

/**
 * Las propiedades de los elementos {@code Grid} y {@code Table} (referencia/componentes/grid.md): las pistas, las
 * áreas, la colocación automática, la alineación de los ítems y el hueco entre pistas. No se heredan.
 */
public final class GridStyle extends StyleGroup {

    private List<Value> columnSizes = List.of();
    private List<Value> rowSizes = List.of();
    /** {@code Areas} ya interpretado; {@link GridAreas#NONE} si no se declara. */
    private GridAreas areas = GridAreas.NONE;
    private Direction flow = Direction.HORIZONTAL;
    private Alignment horizontal = Alignment.STRETCH;
    private Alignment vertical = Alignment.STRETCH;
    /** {@code Gap}: entre filas ({@code row}) y entre columnas ({@code column}). */
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
