package dev.stxt.pdf.tree;


/**
 * Las propiedades de un {@code Columns} (reference/components/columns.md): cuántas columnas y de qué ancho, el
 * hueco y la línea entre ellas y cómo se llenan. No se heredan.
 */
public final class ColumnsStyle extends StyleGroup {

    /** {@code Count}; {@code null} = sin declarar ({@code Min column width} fija entonces las columnas). */
    private Integer count;
    /** {@code Min column width}, en puntos; {@code null} = sin declarar. */
    private Double minColumnWidth;
    /** {@code Gap}, en puntos. */
    private double columnGap = 0;
    /** Grosor de la línea entre columnas ({@code Separator}); 0 = sin línea. */
    private double separator = 0;
    /** {@code Separator color}; {@code null} = el {@code Color} del texto. */
    private String separatorColor;
    /** {@code Balance}: iguala las columnas del último fragmento. */
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
