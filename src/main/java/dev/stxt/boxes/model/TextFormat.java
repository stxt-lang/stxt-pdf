package dev.stxt.boxes.model;

/**
 * Formato de un tramo de texto (ESPECIFICACION.md §5.4): lo que cambia dentro de una línea. La
 * negrita y la cursiva se suman a las de la caja (un tramo en negrita de un texto ya en negrita
 * sigue en negrita); la familia, el tamaño, el color y {@code Line height} son siempre los de la
 * caja.
 */
public record TextFormat(boolean bold, boolean italic, boolean underline, boolean strike) {

    /** Sin formato: el texto tal como lo declara la caja. */
    public static final TextFormat PLAIN = new TextFormat(false, false, false, false);

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
}
