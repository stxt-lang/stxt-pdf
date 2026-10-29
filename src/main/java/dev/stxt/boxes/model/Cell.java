package dev.stxt.boxes.model;

/**
 * Colocación de un ítem de grid, la pedida por su {@code Area} (referencia/componentes/grid.md).
 *
 * <p>Sintaxis: un nombre de área ({@code Area: side}) o {@code fila columna [filas columnas]},
 * donde {@code fila} y {@code columna} pueden ser {@code auto}. Las extensiones son 1 si no se
 * indican.</p>
 *
 * @param row     fila inicial (desde 1), o {@code null} = automática
 * @param column  columna inicial (desde 1), o {@code null} = automática
 * @param rows    número de filas que ocupa
 * @param columns número de columnas que ocupa
 */
public record Cell(Integer row, Integer column, int rows, int columns) {

    public static final Cell AUTO = new Cell(null, null, 1, 1);

    public boolean isExplicit() {
        return row != null && column != null;
    }

    /** Interpreta el valor de {@code Area}; los nombres se buscan en las {@code Areas} del grid. */
    public static Cell parse(String text, GridAreas areas) {
        if (text == null || text.isBlank()) {
            return AUTO;
        }
        String[] t = text.trim().split("\\s+");
        if (t.length == 1 && !t[0].equals("auto") && !t[0].matches("\\d+")) {
            Area a = areas.named().get(t[0]);
            if (a == null) {
                throw new IllegalArgumentException("Área desconocida en Area: " + t[0]);
            }
            return new Cell(a.row(), a.column(), a.rows(), a.columns());
        }
        if (t.length != 2 && t.length != 4) {
            throw new IllegalArgumentException("Area espera un área o 'fila columna [filas columnas]': " + text);
        }
        Integer row = position(t[0], text);
        Integer column = position(t[1], text);
        int rows = t.length == 4 ? span(t[2], text) : 1;
        int columns = t.length == 4 ? span(t[3], text) : 1;
        return new Cell(row, column, rows, columns);
    }

    private static Integer position(String s, String original) {
        if (s.equals("auto")) {
            return null;
        }
        if (!s.matches("\\d+") || Integer.parseInt(s) < 1) {
            throw new IllegalArgumentException("Posición no válida en Area (se cuenta desde 1): " + original);
        }
        return Integer.parseInt(s);
    }

    private static int span(String s, String original) {
        if (!s.matches("\\d+") || Integer.parseInt(s) < 1) {
            throw new IllegalArgumentException("Extensión no válida en Area: " + original);
        }
        return Integer.parseInt(s);
    }
}
