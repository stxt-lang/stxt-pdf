package dev.stxt.pdf.layout.grid;

import dev.stxt.pdf.model.Area;
import dev.stxt.pdf.model.Cell;
import dev.stxt.pdf.model.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Placement of items in the grid (reference/components/grid.md).
 *
 * <ol>
 *   <li>The items with explicit row and column are placed first.</li>
 *   <li>The rest, in tree order, with a cursor that walks the grid by rows and only
 *       moves forward (sparse mode): gaps may be left behind.</li>
 *   <li>If an item does not fit in the existing rows, implicit rows are created.</li>
 * </ol>
 *
 * <p>An item with a fixed row and an automatic column goes to the first free column of that row,
 * without moving the cursor; if none is left, there is no placement ({@link NoRoom}): the row does not grow. With
 * a fixed column and an automatic row, it goes to the first free row from the cursor.</p>
 *
 * <p>{@code Flow: vertical} is the same algorithm with the axes swapped: the grid and the cells are
 * transposed, the placement goes by rows and the transposition is undone.</p>
 */
public final class Placement {

    private Placement() {
    }

    /** What the placement gives: the area of each item or the first one that does not fit. */
    public sealed interface Result permits Placed, NoRoom {
    }

    /** @param areas the area assigned to each item, in the order of the cells */
    public record Placed(List<Area> areas) implements Result {
        public Placed {
            areas = List.copyOf(areas);
        }
    }

    /**
     * An item with a fixed row (the column, with {@code Flow: vertical}) does not fit in it.
     *
     * @param item   index of the item
     * @param track  the row (or the column) it asks for
     * @param span   columns (or rows) it occupies
     * @param tracks columns (or rows) of the grid
     */
    public record NoRoom(int item, int track, int span, int tracks) implements Result {
    }

    /**
     * @param cells   requested cells, in tree order
     * @param columns columns of the explicit grid
     * @param rows    rows of the explicit grid (they only count with {@code Flow: vertical})
     * @param flow    direction of the automatic placement
     * @return the area assigned to each item, in the same order, or {@link NoRoom} if an item with a fixed row (the
     *         column, in {@code column}) does not fit in it
     */
    public static Result place(List<Cell> cells, int columns, int rows, Direction flow) {
        if (flow == Direction.HORIZONTAL) {
            return place(cells, columns);
        }
        List<Cell> transposed = cells.stream()
                .map(c -> new Cell(c.column(), c.row(), c.columns(), c.rows()))
                .toList();
        Result result = place(transposed, rows);
        if (!(result instanceof Placed placed)) {
            return result;
        }
        return new Placed(placed.areas().stream()
                .map(a -> new Area(a.column(), a.row(), a.columns(), a.rows()))
                .toList());
    }

    /**
     * @param cells   requested cells, in tree order
     * @param columns columns of the explicit grid
     * @return the area assigned to each item, in the same order, or {@link NoRoom} if an item with a fixed row does
     *         not fit in it
     */
    public static Result place(List<Cell> cells, int columns) {
        int cols = Math.max(1, columns);
        for (Cell c : cells) {
            int last = c.column() != null ? c.column() + c.columns() - 1 : c.columns();
            cols = Math.max(cols, last);
        }
        Occupancy occupancy = new Occupancy(cols);
        Area[] result = new Area[cells.size()];

        for (int i = 0; i < cells.size(); i++) {
            Cell c = cells.get(i);
            if (c.isExplicit()) {
                result[i] = new Area(c.row(), c.column(), c.rows(), c.columns());
                occupancy.mark(result[i]);
            }
        }

        int cursorRow = 1;
        int cursorColumn = 1;
        for (int i = 0; i < cells.size(); i++) {
            Cell c = cells.get(i);
            if (c.isExplicit()) {
                continue;
            }
            if (c.row() != null) {
                // Fixed row: first free column of that row.
                int col = 1;
                while (col + c.columns() - 1 <= cols && !occupancy.isFree(c.row(), col, c.rows(), c.columns())) {
                    col++;
                }
                if (col + c.columns() - 1 > cols) {
                    return new NoRoom(i, c.row(), c.columns(), cols);
                }
                result[i] = new Area(c.row(), col, c.rows(), c.columns());
            } else if (c.column() != null) {
                // Fixed column: from the cursor row downwards.
                int row = cursorRow;
                if (c.column() < cursorColumn) {
                    row++;
                }
                while (!occupancy.isFree(row, c.column(), c.rows(), c.columns())) {
                    row++;
                }
                result[i] = new Area(row, c.column(), c.rows(), c.columns());
                cursorRow = row;
                cursorColumn = c.column() + c.columns();
            } else {
                // Fully automatic: next gap from the cursor.
                int row = cursorRow;
                int col = cursorColumn;
                while (true) {
                    if (col + c.columns() - 1 > cols) {
                        row++;
                        col = 1;
                        continue;
                    }
                    if (occupancy.isFree(row, col, c.rows(), c.columns())) {
                        break;
                    }
                    col++;
                }
                result[i] = new Area(row, col, c.rows(), c.columns());
                cursorRow = row;
                cursorColumn = col + c.columns();
            }
            occupancy.mark(result[i]);
        }
        return new Placed(List.of(result));
    }

    /**
     * Occupancy matrix with rows that grow on demand. The areas it receives always fit in
     * its columns: {@code place} counts them all before starting and lets none go past them.
     */
    private static final class Occupancy {
        private final int columns;
        private final List<boolean[]> rows = new ArrayList<>();

        Occupancy(int columns) {
            this.columns = columns;
        }

        boolean isFree(int row, int col, int nRows, int nCols) {
            for (int r = row; r < row + nRows; r++) {
                if (r - 1 >= rows.size()) {
                    continue;
                }
                boolean[] cells = rows.get(r - 1);
                for (int c = col; c < col + nCols; c++) {
                    if (cells[c - 1]) {
                        return false;
                    }
                }
            }
            return true;
        }

        void mark(Area a) {
            while (rows.size() < a.rowEnd()) {
                rows.add(new boolean[columns]);
            }
            for (int r = a.row(); r <= a.rowEnd(); r++) {
                for (int c = a.column(); c <= a.columnEnd(); c++) {
                    rows.get(r - 1)[c - 1] = true;
                }
            }
        }
    }
}
