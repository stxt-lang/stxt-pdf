package dev.stxt.pdf.model;

/** Rectángulo de celdas en la rejilla. Filas y columnas empiezan en 1, como las líneas de CSS. */
public record Area(int row, int column, int rows, int columns) {

    public Area {
        if (row < 1 || column < 1 || rows < 1 || columns < 1) {
            throw new IllegalArgumentException("Área no válida: " + row + " " + column + " " + rows + " " + columns);
        }
    }

    public int rowEnd() {
        return row + rows - 1;
    }

    public int columnEnd() {
        return column + columns - 1;
    }
}
