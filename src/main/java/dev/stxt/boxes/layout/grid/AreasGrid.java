package dev.stxt.boxes.layout.grid;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Interpreta el bloque {@code Areas} (ESPECIFICACION.md §4.8): una fila de la rejilla por línea,
 * nombres separados por espacios y {@code .} para una celda vacía. Cada nombre debe formar un
 * rectángulo.
 */
public final class AreasGrid {

    private AreasGrid() {
    }

    public static Map<String, Area> parsear(String bloque) {
        List<String[]> filas = bloque.lines()
                .filter(l -> !l.isBlank())
                .map(l -> l.trim().split("\\s+"))
                .toList();
        if (filas.isEmpty()) {
            throw new IllegalArgumentException("Areas está vacío");
        }
        int columnas = filas.get(0).length;
        for (int f = 0; f < filas.size(); f++) {
            if (filas.get(f).length != columnas) {
                throw new IllegalArgumentException("La fila " + (f + 1) + " de Areas tiene "
                        + filas.get(f).length + " celdas y la primera tiene " + columnas);
            }
        }

        Map<String, int[]> limites = new LinkedHashMap<>(); // nombre -> {f0, c0, f1, c1}
        for (int f = 0; f < filas.size(); f++) {
            for (int c = 0; c < columnas; c++) {
                String nombre = filas.get(f)[c];
                if (nombre.equals(".")) {
                    continue;
                }
                int[] l = limites.computeIfAbsent(nombre, k -> new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, -1, -1});
                l[0] = Math.min(l[0], f);
                l[1] = Math.min(l[1], c);
                l[2] = Math.max(l[2], f);
                l[3] = Math.max(l[3], c);
            }
        }

        Map<String, Area> areas = new LinkedHashMap<>();
        for (Map.Entry<String, int[]> e : limites.entrySet()) {
            int[] l = e.getValue();
            for (int f = l[0]; f <= l[2]; f++) {
                for (int c = l[1]; c <= l[3]; c++) {
                    if (!filas.get(f)[c].equals(e.getKey())) {
                        throw new IllegalArgumentException("El área '" + e.getKey()
                                + "' no es rectangular (fila " + (f + 1) + ", columna " + (c + 1) + ")");
                    }
                }
            }
            areas.put(e.getKey(), new Area(l[0] + 1, l[1] + 1, l[2] - l[0] + 1, l[3] - l[1] + 1));
        }
        return areas;
    }

    /** Número de columnas que declara el bloque. */
    public static int columnas(String bloque) {
        return bloque.lines().filter(l -> !l.isBlank()).findFirst()
                .map(l -> l.trim().split("\\s+").length).orElse(0);
    }

    /** Número de filas que declara el bloque. */
    public static int filas(String bloque) {
        return (int) bloque.lines().filter(l -> !l.isBlank()).count();
    }
}
