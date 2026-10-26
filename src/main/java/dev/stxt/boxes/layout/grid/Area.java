package dev.stxt.boxes.layout.grid;

/** Rectángulo de celdas en la rejilla. Filas y columnas empiezan en 1, como las líneas de CSS. */
public record Area(int fila, int columna, int filas, int columnas) {

    public Area {
        if (fila < 1 || columna < 1 || filas < 1 || columnas < 1) {
            throw new IllegalArgumentException("Área no válida: " + fila + " " + columna + " " + filas + " " + columnas);
        }
    }

    public int filaFin() {
        return fila + filas - 1;
    }

    public int columnaFin() {
        return columna + columnas - 1;
    }
}
