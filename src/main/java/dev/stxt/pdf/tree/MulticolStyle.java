package dev.stxt.pdf.tree;


/**
 * Las propiedades de un {@code Columns} (referencia/componentes/columns.md): cuántas columnas y de qué ancho, el
 * hueco y la línea entre ellas y cómo se llenan. No se heredan.
 */
public final class MulticolStyle extends StyleGroup {

    /** {@code Count}; {@code null} = sin declarar ({@code Min column width} fija entonces las columnas). */
    private Integer count;
    /** {@code Min column width}, en puntos; {@code null} = sin declarar. */
    private Double columnWidth;
    /** {@code Gap}, en puntos. */
    private double columnGap = 0;
    /** Grosor de la línea entre columnas ({@code Separator}); 0 = sin línea. */
    private double rule = 0;
    /** {@code Separator color}; {@code null} = el {@code Color} del texto. */
    private String ruleColor;
    /** {@code Balance}: iguala las columnas del último fragmento. */
    private boolean balance = true;

    MulticolStyle() {
    }

    public Integer count() {
        return count;
    }

    void setCount(Integer v) {
        writable();
        count = v;
    }

    public Double columnWidth() {
        return columnWidth;
    }

    void setColumnWidth(Double v) {
        writable();
        columnWidth = v;
    }

    public double columnGap() {
        return columnGap;
    }

    void setColumnGap(double v) {
        writable();
        columnGap = v;
    }

    public double rule() {
        return rule;
    }

    void setRule(double v) {
        writable();
        rule = v;
    }

    public String ruleColor() {
        return ruleColor;
    }

    void setRuleColor(String v) {
        writable();
        ruleColor = v;
    }

    public boolean balance() {
        return balance;
    }

    void setBalance(boolean v) {
        writable();
        balance = v;
    }
}
