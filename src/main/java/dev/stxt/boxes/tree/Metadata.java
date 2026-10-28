package dev.stxt.boxes.tree;

/**
 * Datos del documento que no afectan al layout (ESPECIFICACION.md §5.13): van a la salida PDF.
 *
 * @param title    título ({@code Title}), o {@code null} solo en {@link #NONE}
 * @param language idioma del contenido ({@code Language}), una etiqueta BCP 47, o {@code null}
 */
public record Metadata(String title, String language) {

    /** Sin título ni idioma: un resultado de layout construido a mano, que no sale de un documento. */
    public static final Metadata NONE = new Metadata(null, null);
}
