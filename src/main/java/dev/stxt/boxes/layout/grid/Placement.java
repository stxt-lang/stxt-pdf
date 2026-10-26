package dev.stxt.boxes.layout.grid;

import dev.stxt.boxes.tree.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * Colocación de ítems en la rejilla (módulo 6, ESPECIFICACION.md §5.8).
 *
 * <ol>
 *   <li>Los ítems con fila y columna explícitas se colocan primero.</li>
 *   <li>El resto, en orden del árbol, con un cursor que recorre la rejilla por filas y solo
 *       avanza (modo disperso): pueden quedar huecos atrás.</li>
 *   <li>Si un ítem no cabe en las filas existentes, se crean filas implícitas.</li>
 * </ol>
 *
 * <p>{@code Flow: column} es el mismo algoritmo con los ejes intercambiados: se trasponen la
 * rejilla y las celdas, se coloca por filas y se deshace la trasposición.</p>
 */
public final class Placement {

    private Placement() {
    }

    /**
     * @param cells   celdas pedidas, en orden del árbol
     * @param columns columnas de la rejilla explícita
     * @param rows    filas de la rejilla explícita (solo cuentan con {@code Flow: column})
     * @param flow    dirección de la colocación automática
     * @return el área asignada a cada ítem, en el mismo orden
     */
    public static List<Area> place(List<Cell> cells, int columns, int rows, Style.Direction flow) {
        if (flow == Style.Direction.ROW) {
            return place(cells, columns);
        }
        List<Cell> transposed = cells.stream()
                .map(c -> new Cell(c.column(), c.row(), c.columns(), c.rows()))
                .toList();
        return place(transposed, rows).stream()
                .map(a -> new Area(a.column(), a.row(), a.columns(), a.rows()))
                .toList();
    }

    /**
     * @param cells   celdas pedidas, en orden del árbol
     * @param columns columnas de la rejilla explícita
     * @return el área asignada a cada ítem, en el mismo orden
     */
    public static List<Area> place(List<Cell> cells, int columns) {
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
                // Fila fija: primera columna libre de esa fila.
                int col = 1;
                while (!occupancy.isFree(c.row(), col, c.rows(), c.columns())) {
                    col++;
                }
                result[i] = new Area(c.row(), col, c.rows(), c.columns());
            } else if (c.column() != null) {
                // Columna fija: desde la fila del cursor hacia abajo.
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
                // Totalmente automática: siguiente hueco desde el cursor.
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
        return List.of(result);
    }

    /** Matriz de ocupación con filas que crecen bajo demanda. */
    private static final class Occupancy {
        private final int columns;
        private final List<boolean[]> rows = new ArrayList<>();

        Occupancy(int columns) {
            this.columns = columns;
        }

        boolean isFree(int row, int col, int nRows, int nCols) {
            if (col + nCols - 1 > columns) {
                return false;
            }
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
                    if (c - 1 < columns) {
                        rows.get(r - 1)[c - 1] = true;
                    }
                }
            }
        }
    }
}
