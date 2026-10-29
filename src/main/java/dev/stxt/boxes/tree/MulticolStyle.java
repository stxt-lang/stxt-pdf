package dev.stxt.boxes.tree;

import dev.stxt.boxes.model.ColumnFill;

/**
 * Las propiedades de un {@code Multicol} (referencia/componentes/multicol.md): cuántas columnas y de qué ancho, el
 * hueco y la línea entre ellas y cómo se llenan. No se heredan.
 */
public final class MulticolStyle extends StyleGroup {

    /** {@code Count}; {@code null} = sin declarar ({@code Column width} fija entonces las columnas). */
    private Integer count;
    /** {@code Column width}, en puntos; {@code null} = sin declarar. */
    private Double columnWidth;
    /** {@code Column gap}, en puntos. */
    private double columnGap = 0;
    /** Grosor de la línea entre columnas ({@code Rule}); 0 = sin línea. */
    private double rule = 0;
    /** {@code Rule color}; {@code null} = el {@code Color} del texto. */
    private String ruleColor;
    private ColumnFill fill = ColumnFill.BALANCE;

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

    public ColumnFill fill() {
        return fill;
    }

    void setFill(ColumnFill v) {
        writable();
        fill = v;
    }
}
