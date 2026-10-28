package dev.stxt.boxes.model;

/**
 * Formato de un tramo de texto (ESPECIFICACION.md §5.4): lo que cambia dentro de una línea. La
 * negrita y la cursiva se suman a las de la caja (un tramo en negrita de un texto ya en negrita
 * sigue en negrita); la familia, el tamaño, el color y {@code Line height} son siempre los de la
 * caja.
 *
 * <p>{@code link} es el destino del enlace del tramo ({@link Links}, §5.20), o {@code null} si no
 * lo lleva. No cambia cómo se mide ni cómo se pinta el texto: el subrayado de un enlace de
 * Markdown es el {@code underline} de su tramo.</p>
 */
public record TextFormat(boolean bold, boolean italic, boolean underline, boolean strike, String link) {

    /** Sin formato: el texto tal como lo declara la caja. */
    public static final TextFormat PLAIN = new TextFormat(false, false, false, false);

    /** Un formato sin enlace. */
    public TextFormat(boolean bold, boolean italic, boolean underline, boolean strike) {
        this(bold, italic, underline, strike, null);
    }

    /** La fuente del tramo a partir de la de la caja. */
    public FontFace face(FontFace base) {
        if (!bold && !italic) {
            return base;
        }
        return new FontFace(base.family(), bold ? FontWeight.BOLD : base.weight(),
                italic ? FontStyle.ITALIC : base.style());
    }

    /** Si lleva subrayado o tachado, que se pintan aparte del texto. */
    public boolean decorated() {
        return underline || strike;
    }

    /** El mismo formato con ese enlace. */
    public TextFormat withLink(String destination) {
        return new TextFormat(bold, italic, underline, strike, destination);
    }
}
