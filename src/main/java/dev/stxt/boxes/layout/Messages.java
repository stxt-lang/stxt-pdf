package dev.stxt.boxes.layout;

import dev.stxt.boxes.tree.Box;

import java.util.Locale;

/** Piezas comunes de los mensajes de error del layout ({@link Overflow}, {@code TableLayout}). */
public final class Messages {

    private Messages() {
    }

    /** La caja de un mensaje: {@code main (línea 12)} si tiene {@code Id}, o {@code de la línea 12}. */
    public static String name(Box box) {
        return box.id != null ? box.id + " (línea " + box.line + ")" : "de la línea " + box.line;
    }

    /** Una medida en puntos con dos decimales como mucho y sin ceros finales: {@code 12.5}, {@code 100}. */
    public static String pt(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        return s.replaceAll("\\.?0+$", "");
    }
}
