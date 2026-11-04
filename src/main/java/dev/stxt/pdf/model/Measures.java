package dev.stxt.pdf.model;

import java.util.Locale;

/**
 * How the error messages write a measure: {@code tree} and {@code layout} give the lengths in points, with the unit,
 * because the document may write them in millimeters (reference/common/values.md).
 */
public final class Measures {

    private Measures() {
    }

    /**
     * A measure in points with at most two decimals, no trailing zeros and the unit: {@code 12.5 pt}, {@code 100 pt}.
     */
    public static String pt(double v) {
        return String.format(Locale.ROOT, "%.2f", v).replaceAll("\\.?0+$", "") + " pt";
    }
}
