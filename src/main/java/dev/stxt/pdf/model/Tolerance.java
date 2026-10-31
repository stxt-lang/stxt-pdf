package dev.stxt.pdf.model;

/**
 * Las dos tolerancias del motor al comparar medidas en puntos:
 *
 * <ul>
 *   <li>{@link #EPSILON}, la de la aritmética de coma flotante: dos medidas que difieren en menos son la misma. Es la
 *       del layout ({@link #fits}, {@link #same}).</li>
 *   <li>{@link #MODEL}, la del modelo: lo que se ve. Una caja que se sale menos de su sitio no desborda
 *       ({@code Overflow}, reference/common/overflow.md), y dos volcados (reference/implementation/testing.md)
 *       que difieren menos son iguales. Es más ancha a propósito: absorbe lo que acumulan las medidas de las fuentes,
 *       en milésimas del cuerpo, y los decimales que escribe el autor.</li>
 * </ul>
 */
public final class Tolerance {

    public static final double EPSILON = 1e-9;

    /** 0,01 pt: lo que una caja puede salirse sin que sea un desbordamiento, y la precisión de los volcados. */
    public static final double MODEL = 0.01;

    private Tolerance() {
    }

    /** Si {@code size} cabe en {@code space}, salvo errores de redondeo. */
    public static boolean fits(double size, double space) {
        return size <= space + EPSILON;
    }

    /** Si dos medidas son iguales, salvo errores de redondeo. */
    public static boolean same(double a, double b) {
        return Math.abs(a - b) <= EPSILON;
    }
}
