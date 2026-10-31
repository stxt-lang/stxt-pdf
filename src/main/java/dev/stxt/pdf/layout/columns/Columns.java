package dev.stxt.pdf.layout.columns;

import static dev.stxt.pdf.model.Tolerance.EPSILON;

/**
 * Number and width of the columns of a {@code Columns} (reference/components/columns.md), with the
 * CSS pseudo-algorithm ({@code column-count} and {@code column-width}):
 *
 * <ul>
 *   <li>only {@code Count}: that number of columns, which share the width;</li>
 *   <li>only {@code Min column width}: as many columns of at least that width as fit, and at
 *       least one;</li>
 *   <li>both: as many as fit, without exceeding {@code Count}.</li>
 * </ul>
 *
 * <p>The columns widen to fill the content width between the gaps. If the
 * gaps leave no room, they measure 0 and the content overflows.</p>
 *
 * @param count number of columns
 * @param width width of each column
 * @param gap   gap between two columns
 */
record Columns(int count, double width, double gap) {

    /**
     * @param count     {@code Count}; {@code null} if it is not declared
     * @param width     {@code Min column width}; {@code null} if it is not declared
     * @param gap       {@code Gap}
     * @param available width of the content box of the {@code Columns}
     */
    static Columns of(Integer count, Double width, double gap, double available) {
        int n;
        if (width == null) {
            n = count;
        } else {
            n = Math.max(1, (int) Math.floor((available + gap + EPSILON) / (width + gap)));
            if (count != null) {
                n = Math.min(n, count);
            }
        }
        return new Columns(n, Math.max(0, (available - (n - 1) * gap) / n), gap);
    }

    /** Position of the column {@code column} (from 0), from the content box of the {@code Columns}. */
    double x(int column) {
        return column * (width + gap);
    }
}
