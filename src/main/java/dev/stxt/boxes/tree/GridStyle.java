package dev.stxt.boxes.tree;

import dev.stxt.boxes.model.Alignment;
import dev.stxt.boxes.model.Direction;
import dev.stxt.boxes.model.GridAreas;
import dev.stxt.boxes.model.Value;

import java.util.List;

/**
 * Las propiedades de los elementos {@code Grid} y {@code Table} (referencia/componentes/grid.md): las pistas, las
 * áreas, la colocación automática, la alineación de los ítems y el hueco entre pistas. No se heredan.
 */
public final class GridStyle extends StyleGroup {

    private List<Value> columns = List.of();
    private List<Value> rows = List.of();
    /** {@code Areas} ya interpretado; {@link GridAreas#NONE} si no se declara. */
    private GridAreas areas = GridAreas.NONE;
    private Direction flow = Direction.ROW;
    private Alignment horizontal = Alignment.STRETCH;
    private Alignment vertical = Alignment.STRETCH;
    /** {@code Gap}: entre filas ({@code row}) y entre columnas ({@code column}). */
    private Value rowGap = Value.pt(0);
    private Value columnGap = Value.pt(0);

    GridStyle() {
    }

    public List<Value> columns() {
        return columns;
    }

    void setColumns(List<Value> v) {
        writable();
        columns = v;
    }

    public List<Value> rows() {
        return rows;
    }

    void setRows(List<Value> v) {
        writable();
        rows = v;
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
