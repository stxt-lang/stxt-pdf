package dev.stxt.pdf.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * El bloque {@code Areas} de un grid ya interpretado (referencia/componentes/grid.md): una fila de la
 * rejilla por línea, nombres separados por espacios y {@code .} para una celda vacía. Cada nombre
 * debe formar un rectángulo.
 *
 * @param named   las áreas con nombre, en orden de aparición
 * @param rows    número de filas que declara el bloque
 * @param columns número de columnas que declara el bloque
 */
public record GridAreas(Map<String, Area> named, int rows, int columns) {

    /** Un grid sin {@code Areas}. */
    public static final GridAreas NONE = new GridAreas(Map.of(), 0, 0);

    public GridAreas {
        named = Collections.unmodifiableMap(new LinkedHashMap<>(named));
    }

    public static GridAreas parse(String block) {
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
        return new GridAreas(areas, rows.size(), columns);
    }
}
