package dev.stxt.pdf.model;

/** Rectangle of cells in the grid. Rows and columns start at 1, like CSS lines. */
public record Area(int row, int column, int rows, int columns) {

    public Area {
        if (row < 1 || column < 1 || rows < 1 || columns < 1) {
            throw new IllegalArgumentException("Invalid area: " + row + " " + column + " " + rows + " " + columns);
        }
    }

    public int rowEnd() {
        return row + rows - 1;
    }

    public int columnEnd() {
        return column + columns - 1;
    }
}
