package dev.stxt.pdf.model;

/**
 * The two tolerances of the engine when comparing measurements in points:
 *
 * <ul>
 *   <li>{@link #EPSILON}, that of floating-point arithmetic: two measurements that differ by less are the same. It is
 *       the one of the layout ({@link #fits}, {@link #same}).</li>
 *   <li>{@link #MODEL}, that of the model: what can be seen. A box that sticks out of its place by less does not
 *       overflow ({@code Overflow}, reference/common/overflow.md), and two dumps (reference/implementation/testing.md)
 *       that differ by less are equal. It is wider on purpose: it absorbs what the font measurements accumulate, in
 *       thousandths of the font size, and the decimals the author writes.</li>
 * </ul>
 */
public final class Tolerance {

    public static final double EPSILON = 1e-9;

    /** 0.01 pt: how far a box can stick out without it being an overflow, and the precision of the dumps. */
    public static final double MODEL = 0.01;

    private Tolerance() {
    }

    /** Whether {@code size} fits in {@code space}, up to rounding errors. */
    public static boolean fits(double size, double space) {
        return size <= space + EPSILON;
    }

    /** Whether two measurements are equal, up to rounding errors. */
    public static boolean same(double a, double b) {
        return Math.abs(a - b) <= EPSILON;
    }
}
