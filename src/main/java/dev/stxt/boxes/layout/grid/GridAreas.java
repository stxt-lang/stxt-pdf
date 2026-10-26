package dev.stxt.boxes.layout.grid;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Interpreta el bloque {@code Areas} (ESPECIFICACION.md §4.8): una fila de la rejilla por línea,
 * nombres separados por espacios y {@code .} para una celda vacía. Cada nombre debe formar un
 * rectángulo.
 */
public final class GridAreas {

    private GridAreas() {
    }

    public static Map<String, Area> parse(String block) {
        List<String[]> rows = block.lines()
                .filter(l -> !l.isBlank())
                .map(l -> l.trim().split("\\s+"))
                .toList();
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Areas está vacío");
        }
        int columns = rows.get(0).length;
        for (int r = 0; r < rows.size(); r++) {
            if (rows.get(r).length != columns) {
                throw new IllegalArgumentException("La fila " + (r + 1) + " de Areas tiene "
                        + rows.get(r).length + " celdas y la primera tiene " + columns);
            }
        }

        Map<String, int[]> bounds = new LinkedHashMap<>(); // nombre -> {r0, c0, r1, c1}
        for (int r = 0; r < rows.size(); r++) {
            for (int c = 0; c < columns; c++) {
                String name = rows.get(r)[c];
                if (name.equals(".")) {
                    continue;
                }
                int[] b = bounds.computeIfAbsent(name, k -> new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, -1, -1});
                b[0] = Math.min(b[0], r);
                b[1] = Math.min(b[1], c);
                b[2] = Math.max(b[2], r);
                b[3] = Math.max(b[3], c);
            }
        }

        Map<String, Area> areas = new LinkedHashMap<>();
        for (Map.Entry<String, int[]> e : bounds.entrySet()) {
            int[] b = e.getValue();
            for (int r = b[0]; r <= b[2]; r++) {
                for (int c = b[1]; c <= b[3]; c++) {
                    if (!rows.get(r)[c].equals(e.getKey())) {
                        throw new IllegalArgumentException("El área '" + e.getKey()
                                + "' no es rectangular (fila " + (r + 1) + ", columna " + (c + 1) + ")");
                    }
                }
            }
            areas.put(e.getKey(), new Area(b[0] + 1, b[1] + 1, b[2] - b[0] + 1, b[3] - b[1] + 1));
        }
        return areas;
    }

    /** Número de columnas que declara el bloque. */
    public static int columns(String block) {
        return block.lines().filter(l -> !l.isBlank()).findFirst()
                .map(l -> l.trim().split("\\s+").length).orElse(0);
    }

    /** Número de filas que declara el bloque. */
    public static int rows(String block) {
        return (int) block.lines().filter(l -> !l.isBlank()).count();
    }
}
