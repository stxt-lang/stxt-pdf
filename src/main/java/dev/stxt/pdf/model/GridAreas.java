package dev.stxt.pdf.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The parsed {@code Areas} block of a grid (reference/components/grid.md): one row of the
 * grid per line, names separated by spaces and {@code .} for an empty cell. Each name
 * must form a rectangle.
 *
 * @param named   the named areas, in order of appearance
 * @param rows    number of rows the block declares
 * @param columns number of columns the block declares
 */
public record GridAreas(Map<String, Area> named, int rows, int columns) {

    /** A grid without {@code Areas}. */
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
            throw new IllegalArgumentException("Areas is empty");
        }
        int columns = rows.get(0).length;
        for (int r = 0; r < rows.size(); r++) {
            if (rows.get(r).length != columns) {
                throw new IllegalArgumentException("Row " + (r + 1) + " of Areas has "
                        + rows.get(r).length + " cells and the first one has " + columns);
            }
        }

        Map<String, int[]> bounds = new LinkedHashMap<>(); // name -> {r0, c0, r1, c1}
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
                        throw new IllegalArgumentException("The area '" + e.getKey()
                                + "' is not rectangular (row " + (r + 1) + ", column " + (c + 1) + ")");
                    }
                }
            }
            areas.put(e.getKey(), new Area(b[0] + 1, b[1] + 1, b[2] - b[0] + 1, b[3] - b[1] + 1));
        }
        return new GridAreas(areas, rows.size(), columns);
    }
}
