package dev.stxt.pdf.tree;

import static dev.stxt.pdf.model.Tolerance.EPSILON;

/**
 * The properties of a {@code Columns} (reference/components/columns.md): how many columns and how wide, the
 * gap and the line between them and how they are filled. They are not inherited.
 */
public final class ColumnsStyle extends StyleGroup {

    /** {@code Count}; {@code null} = not declared ({@code Min column width} then sets the columns). */
    private Integer count;
    /** {@code Min column width}, in points; {@code null} = not declared. */
    private Double minColumnWidth;
    /** {@code Gap}, in points. */
    private double columnGap = 0;
    /** Thickness of the line between columns ({@code Separator}); 0 = no line. */
    private double separator = 0;
    /** {@code Separator color}; {@code null} = the {@code Color} of the text. */
    private String separatorColor;
    /** {@code Balance}: evens out the columns of the last fragment. */
    private boolean balance = true;

    ColumnsStyle() {
    }

    public Integer count() {
        return count;
    }

    void setCount(Integer v) {
        writable();
        count = v;
    }

    public Double minColumnWidth() {
        return minColumnWidth;
    }

    void setMinColumnWidth(Double v) {
        writable();
        minColumnWidth = v;
    }

    public double columnGap() {
        return columnGap;
    }

    void setColumnGap(double v) {
        writable();
        columnGap = v;
    }

    public double separator() {
        return separator;
    }

    void setSeparator(double v) {
        writable();
        separator = v;
    }

    public String separatorColor() {
        return separatorColor;
    }

    void setSeparatorColor(String v) {
        writable();
        separatorColor = v;
    }

    public boolean balance() {
        return balance;
    }

    void setBalance(boolean v) {
        writable();
        balance = v;
    }

    /**
     * {@return the number of columns of a {@code Columns} with this style whose content box measures {@code available}
     * points wide} ({@link #count(Integer, Double, double, double)}).
     *
     * @param available width of the content box of the {@code Columns}, in points
     */
    public int countIn(double available) {
        return count(count, minColumnWidth, columnGap, available);
    }

    /**
     * {@return the width of each column of a {@code Columns} with this style whose content box measures
     * {@code available} points wide} ({@link #width(int, double, double)}).
     *
     * @param available width of the content box of the {@code Columns}, in points
     */
    public double widthIn(double available) {
        return width(countIn(available), columnGap, available);
    }

    /**
     * The number of columns (reference/components/columns.md): with only {@code Count}, that number; with only
     * {@code Min column width}, as many columns of at least that width as fit, and at least one; with both, as many
     * as fit without exceeding {@code Count}. The only place that computes it: the layout and whoever asks where the
     * columns ended up ({@code PageFragment.columns()}) read it here.
     *
     * @param count     {@code Count}; {@code null} if it is not declared
     * @param minWidth  {@code Min column width}, in points; {@code null} if it is not declared
     * @param gap       {@code Gap}, in points
     * @param available width of the content box of the {@code Columns}, in points
     * @return the number of columns, at least 1
     */
    public static int count(Integer count, Double minWidth, double gap, double available) {
        if (minWidth == null) {
            return count;
        }
        int n = Math.max(1, (int) Math.floor((available + gap + EPSILON) / (minWidth + gap)));
        return count != null ? Math.min(n, count) : n;
    }

    /**
     * The width of each of {@code count} columns, which widen to fill the content width between the gaps; if the gaps
     * leave no room, 0.
     *
     * @param count     the number of columns, at least 1
     * @param gap       {@code Gap}, in points
     * @param available width of the content box of the {@code Columns}, in points
     * @return the width of a column, in points; never negative
     */
    public static double width(int count, double gap, double available) {
        return Math.max(0, (available - (count - 1) * gap) / count);
    }
}
