package dev.stxt.boxes.model;

/**
 * Tolerancia de los errores de redondeo al comparar medidas en puntos: dos medidas que difieren
 * en menos de {@link #EPSILON} son la misma. No es una tolerancia del modelo, solo de la
 * aritmética de coma flotante.
 */
public final class Tolerance {

    public static final double EPSILON = 1e-9;

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
