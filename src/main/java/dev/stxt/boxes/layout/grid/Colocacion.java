package dev.stxt.boxes.layout.grid;

import java.util.ArrayList;
import java.util.List;

/**
 * Colocación de ítems en la rejilla (módulo 6, ESPECIFICACION.md §5.8), con {@code Flujo: fila}.
 *
 * <ol>
 *   <li>Los ítems con fila y columna explícitas se colocan primero.</li>
 *   <li>El resto, en orden del árbol, con un cursor que recorre la rejilla por filas y solo
 *       avanza (modo disperso): pueden quedar huecos atrás.</li>
 *   <li>Si un ítem no cabe en las filas existentes, se crean filas implícitas.</li>
 * </ol>
 *
 * <p>Pendiente: {@code Flujo: columna} (es el mismo algoritmo con los ejes intercambiados).</p>
 */
public final class Colocacion {

    private Colocacion() {
    }

    /**
     * @param celdas   celdas pedidas, en orden del árbol
     * @param columnas columnas de la rejilla explícita
     * @return el área asignada a cada ítem, en el mismo orden
     */
    public static List<Area> colocar(List<Celda> celdas, int columnas) {
        int cols = Math.max(1, columnas);
        for (Celda c : celdas) {
            int ultima = c.columna() != null ? c.columna() + c.columnas() - 1 : c.columnas();
            cols = Math.max(cols, ultima);
        }
        Ocupacion ocupacion = new Ocupacion(cols);
        Area[] resultado = new Area[celdas.size()];

        for (int i = 0; i < celdas.size(); i++) {
            Celda c = celdas.get(i);
            if (c.esExplicita()) {
                resultado[i] = new Area(c.fila(), c.columna(), c.filas(), c.columnas());
                ocupacion.marcar(resultado[i]);
            }
        }

        int filaCursor = 1;
        int columnaCursor = 1;
        for (int i = 0; i < celdas.size(); i++) {
            Celda c = celdas.get(i);
            if (c.esExplicita()) {
                continue;
            }
            if (c.fila() != null) {
                // Fila fija: primera columna libre de esa fila.
                int col = 1;
                while (!ocupacion.libre(c.fila(), col, c.filas(), c.columnas())) {
                    col++;
                }
                resultado[i] = new Area(c.fila(), col, c.filas(), c.columnas());
            } else if (c.columna() != null) {
                // Columna fija: desde la fila del cursor hacia abajo.
                int fila = filaCursor;
                if (c.columna() < columnaCursor) {
                    fila++;
                }
                while (!ocupacion.libre(fila, c.columna(), c.filas(), c.columnas())) {
                    fila++;
                }
                resultado[i] = new Area(fila, c.columna(), c.filas(), c.columnas());
                filaCursor = fila;
                columnaCursor = c.columna() + c.columnas();
            } else {
                // Totalmente automática: siguiente hueco desde el cursor.
                int fila = filaCursor;
                int col = columnaCursor;
                while (true) {
                    if (col + c.columnas() - 1 > cols) {
                        fila++;
                        col = 1;
                        continue;
                    }
                    if (ocupacion.libre(fila, col, c.filas(), c.columnas())) {
                        break;
                    }
                    col++;
                }
                resultado[i] = new Area(fila, col, c.filas(), c.columnas());
                filaCursor = fila;
                columnaCursor = col + c.columnas();
            }
            ocupacion.marcar(resultado[i]);
        }
        return List.of(resultado);
    }

    /** Matriz de ocupación con filas que crecen bajo demanda. */
    private static final class Ocupacion {
        private final int columnas;
        private final List<boolean[]> filas = new ArrayList<>();

        Ocupacion(int columnas) {
            this.columnas = columnas;
        }

        boolean libre(int fila, int col, int nFilas, int nCols) {
            if (col + nCols - 1 > columnas) {
                return false;
            }
            for (int f = fila; f < fila + nFilas; f++) {
                if (f - 1 >= filas.size()) {
                    continue;
                }
                boolean[] r = filas.get(f - 1);
                for (int c = col; c < col + nCols; c++) {
                    if (r[c - 1]) {
                        return false;
                    }
                }
            }
            return true;
        }

        void marcar(Area a) {
            while (filas.size() < a.filaFin()) {
                filas.add(new boolean[columnas]);
            }
            for (int f = a.fila(); f <= a.filaFin(); f++) {
                for (int c = a.columna(); c <= a.columnaFin(); c++) {
                    if (c - 1 < columnas) {
                        filas.get(f - 1)[c - 1] = true;
                    }
                }
            }
        }
    }
}
