package dev.stxt.pdf.tree;


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
}
