package dev.stxt.boxes.layout;

import java.util.Locale;

/** Piezas comunes de los mensajes de error del layout ({@link Overflow}, {@code TableLayout}). */
public final class Messages {

    private Messages() {
    }

    /** Máximo de caracteres de un texto del documento que se cita en un mensaje. */
    private static final int EXCERPT = 40;

    /**
     * Un texto del documento para citarlo en un mensaje: entero si es corto y, si no, sus primeros
     * caracteres con la longitud total, para que el mensaje no crezca con el texto.
     */
    public static String excerpt(String text) {
        int length = text.codePointCount(0, text.length());
        if (length <= EXCERPT) {
            return text;
        }
        return text.substring(0, text.offsetByCodePoints(0, EXCERPT)) + "… (" + length + " caracteres)";
    }

    /** Una medida en puntos con dos decimales como mucho y sin ceros finales: {@code 12.5}, {@code 100}. */
    public static String pt(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        return s.replaceAll("\\.?0+$", "");
    }
}
