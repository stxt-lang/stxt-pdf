package dev.stxt.pdf.model;

/**
 * A side of the border of a cell in a table with {@code Merge borders} (reference/components/table.md#merged-borders):
 * its width and its color. The line between two cells is the wider of their two sides ({@link #merge}).
 *
 * @param width width in pt; 0 is no line
 * @param color its color ({@code #rrggbb}); {@code null} in {@link #NONE}
 */
public record Stroke(double width, String color) {

    /** No line: a side without border, or a cell that is not on the page. */
    public static final Stroke NONE = new Stroke(0, null);

    /** Whether it draws no line. */
    public boolean isNone() {
        return width <= Tolerance.EPSILON;
    }

    /**
     * The line between two sides: the wider one and, if they are equally wide, {@code first}, which is that of the
     * cell above or on the left.
     */
    public static Stroke merge(Stroke first, Stroke second) {
        return second.width() > first.width() + Tolerance.EPSILON ? second : first;
    }
}
