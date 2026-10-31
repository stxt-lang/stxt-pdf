package dev.stxt.pdf.model;

import java.util.Objects;

/**
 * Fuente de un texto: familia, peso y estilo (reference/common/typography.md). En la salida PDF
 * identifica un fichero TTF de la carpeta de fuentes (reference/common/resources.md).
 *
 * <p>{@code ligatures} dice si el texto se mide y se pinta con las ligaduras de la fuente
 * ({@code Ligatures}, reference/common/text-formatting.md). No cambia el fichero: dos fuentes que solo se distinguen
 * en esto son el mismo TTF ({@link #base()}), pero el texto mide y se pinta distinto.</p>
 */
public record FontFace(String family, FontWeight weight, FontStyle style, boolean ligatures) {

    public FontFace {
        Objects.requireNonNull(family, "family");
        Objects.requireNonNull(weight, "weight");
        Objects.requireNonNull(style, "style");
    }

    /** Una fuente sin ligaduras, que es lo que se pide por defecto. */
    public FontFace(String family, FontWeight weight, FontStyle style) {
        this(family, weight, style, false);
    }

    /** La misma fuente con o sin ligaduras. */
    public FontFace withLigatures(boolean ligatures) {
        return this.ligatures == ligatures ? this : new FontFace(family, weight, style, ligatures);
    }

    /** La fuente tal como identifica a un fichero TTF: sin la opción de las ligaduras. */
    public FontFace base() {
        return withLigatures(false);
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
