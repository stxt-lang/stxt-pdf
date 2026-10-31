package dev.stxt.pdf.model;

import java.util.regex.Pattern;

/**
 * Los colores del documento (reference/common/values.md): {@code #RGB}, {@code #RGBA}, {@code #RRGGBB}
 * o {@code #RRGGBBAA}, con el último par (o dígito) como opacidad. Un color sigue siendo el texto
 * que escribió el autor; aquí se valida y se pasa a componentes.
 */
public final class Colors {

    private static final Pattern COLOR = Pattern.compile("#([0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})");

    private Colors() {
    }

    public static boolean isValid(String color) {
        return COLOR.matcher(color).matches();
    }

    /**
     * Los componentes rojo, verde, azul y opacidad de un color ya validado, de 0 a 1. Sin opacidad
     * escrita, 1.
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

    /** Si el color deja ver lo que hay debajo. */
    public static boolean isTranslucent(String color) {
        return rgba(color)[3] < 1f;
    }
}
