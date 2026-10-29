package dev.stxt.boxes.layout;

import dev.stxt.boxes.tree.Box;

import java.util.Locale;

/** Piezas comunes de los mensajes de error del layout ({@link Overflow}, {@code TableLayout}). */
public final class Messages {

    private Messages() {
    }

    /** La caja de un mensaje: {@code main (línea 12)} si tiene {@code Id}, o {@code de la línea 12}. */
    public static String name(Box box) {
        return box.id() != null ? box.id() + " (línea " + box.line() + ")" : "de la línea " + box.line();
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
