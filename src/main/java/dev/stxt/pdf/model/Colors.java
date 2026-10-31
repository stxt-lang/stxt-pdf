package dev.stxt.pdf.model;

import java.util.regex.Pattern;

/**
 * The colors of the document (reference/common/values.md): {@code #RGB}, {@code #RGBA}, {@code #RRGGBB}
 * or {@code #RRGGBBAA}, with the last pair (or digit) as the opacity. A color is still the text
 * the author wrote; here it is validated and converted to components.
 */
public final class Colors {

    private static final Pattern COLOR = Pattern.compile("#([0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})");

    private Colors() {
    }

    public static boolean isValid(String color) {
        return COLOR.matcher(color).matches();
    }

    /**
     * The red, green, blue and opacity components of an already validated color, from 0 to 1. Without a
     * written opacity, 1.
     */
    public static float[] rgba(String color) {
        String hex = color.substring(1);
        if (hex.length() <= 4) {
            StringBuilder doubled = new StringBuilder();
            for (char c : hex.toCharArray()) {
                doubled.append(c).append(c);
            }
            hex = doubled.toString();
        }
        float[] rgba = new float[4];
        for (int i = 0; i < 4; i++) {
            rgba[i] = i * 2 < hex.length() ? Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16) / 255f : 1f;
        }
        return rgba;
    }

    /** Whether the color lets what is underneath show through. */
    public static boolean isTranslucent(String color) {
        return rgba(color)[3] < 1f;
    }
}
