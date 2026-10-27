package dev.stxt.boxes.model;

import java.util.Objects;

/**
 * Fuente de un texto: familia, peso y estilo (ESPECIFICACION.md §4.4). En la salida PDF
 * identifica un fichero TTF de la carpeta de fuentes (§5.11).
 */
public record FontFace(String family, FontWeight weight, FontStyle style) {

    public FontFace {
        Objects.requireNonNull(family, "family");
        Objects.requireNonNull(weight, "weight");
        Objects.requireNonNull(style, "style");
    }

    /** Variante para los mensajes: {@code normal}, {@code bold}, {@code italic} o {@code bold italic}. */
    public String variant() {
        boolean bold = weight == FontWeight.BOLD;
        boolean italic = style == FontStyle.ITALIC;
        if (bold && italic) {
            return "bold italic";
        }
        return bold ? "bold" : italic ? "italic" : "normal";
    }

    @Override
    public String toString() {
        return family + " " + variant();
    }
}
