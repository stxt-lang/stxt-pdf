package dev.stxt.pdf.model;

/**
 * Placement of a grid item, the one requested by its {@code Area} (reference/components/grid.md).
 *
 * <p>Syntax: an area name ({@code Area: side}) or {@code row column [rows columns]},
 * where {@code row} and {@code column} can be {@code auto}. The spans are 1 if they are not
 * given.</p>
 *
 * @param row     starting row (from 1), or {@code null} = automatic
 * @param column  starting column (from 1), or {@code null} = automatic
 * @param rows    number of rows it spans
 * @param columns number of columns it spans
 */
public record Cell(Integer row, Integer column, int rows, int columns) {

    public static final Cell AUTO = new Cell(null, null, 1, 1);

    public boolean isExplicit() {
        return row != null && column != null;
    }

    /** Parses the value of {@code Area}; names are looked up in the {@code Areas} of the grid. */
    public static Cell parse(String text, GridAreas areas) {
        if (text == null || text.isBlank()) {
            return AUTO;
        }
        String[] t = text.trim().split("\\s+");
        if (t.length == 1 && !t[0].equals("auto") && !t[0].matches("\\d+")) {
            Area a = areas.named().get(t[0]);
            if (a == null) {
                throw new IllegalArgumentException("Unknown area in Area: " + t[0]);
            }
            return new Cell(a.row(), a.column(), a.rows(), a.columns());
        }
        if (t.length != 2 && t.length != 4) {
            throw new IllegalArgumentException("Area expects an area or 'row column [rows columns]': " + text);
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
            throw new IllegalArgumentException("Invalid position in Area (counted from 1): " + original);
        }
        return Integer.parseInt(s);
    }

    private static int span(String s, String original) {
        if (!s.matches("\\d+") || Integer.parseInt(s) < 1) {
            throw new IllegalArgumentException("Invalid span in Area: " + original);
        }
        return Integer.parseInt(s);
    }
}
