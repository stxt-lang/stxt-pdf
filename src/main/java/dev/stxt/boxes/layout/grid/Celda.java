package dev.stxt.boxes.layout.grid;

import java.util.Map;

/**
 * Colocación pedida por {@code Item > Celda} (ESPECIFICACION.md §4.9).
 *
 * <p>Sintaxis: un nombre de área ({@code Celda: lat}) o {@code fila columna [filas columnas]},
 * donde {@code fila} y {@code columna} pueden ser {@code auto}. Las extensiones son 1 si no se
 * indican.</p>
 *
 * @param fila     fila inicial (desde 1), o {@code null} = automática
 * @param columna  columna inicial (desde 1), o {@code null} = automática
 * @param filas    número de filas que ocupa
 * @param columnas número de columnas que ocupa
 */
public record Celda(Integer fila, Integer columna, int filas, int columnas) {

    public static final Celda AUTOMATICA = new Celda(null, null, 1, 1);

    public boolean esExplicita() {
        return fila != null && columna != null;
    }

    public static Celda parsear(String texto, Map<String, Area> areas) {
        if (texto == null || texto.isBlank()) {
            return AUTOMATICA;
        }
        String[] t = texto.trim().split("\\s+");
        if (t.length == 1 && !t[0].equals("auto") && !t[0].matches("\\d+")) {
            Area a = areas.get(t[0]);
            if (a == null) {
                throw new IllegalArgumentException("Área desconocida en Celda: " + t[0]);
            }
            return new Celda(a.fila(), a.columna(), a.filas(), a.columnas());
        }
        if (t.length != 2 && t.length != 4) {
            throw new IllegalArgumentException("Celda espera un área o 'fila columna [filas columnas]': " + texto);
        }
        Integer fila = posicion(t[0], texto);
        Integer columna = posicion(t[1], texto);
        int filas = t.length == 4 ? extension(t[2], texto) : 1;
        int columnas = t.length == 4 ? extension(t[3], texto) : 1;
        return new Celda(fila, columna, filas, columnas);
    }

    private static Integer posicion(String s, String original) {
        if (s.equals("auto")) {
            return null;
        }
        if (!s.matches("\\d+") || Integer.parseInt(s) < 1) {
            throw new IllegalArgumentException("Posición no válida en Celda (se cuenta desde 1): " + original);
        }
        return Integer.parseInt(s);
    }

    private static int extension(String s, String original) {
        if (!s.matches("\\d+") || Integer.parseInt(s) < 1) {
            throw new IllegalArgumentException("Extensión no válida en Celda: " + original);
        }
        return Integer.parseInt(s);
    }
}
