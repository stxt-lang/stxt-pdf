package dev.stxt.boxes.model;

/**
 * Formato de un tramo de texto (ESPECIFICACION.md §5.4): lo que cambia dentro de una línea. La
 * negrita y la cursiva se suman a las de la caja (un tramo en negrita de un texto ya en negrita
 * sigue en negrita); la familia, el tamaño, el color y {@code Line height} son siempre los de la
 * caja.
 *
 * <p>{@code smallCaps} marca las minúsculas que se pintan como versalitas (§4.4): el texto del
 * tramo ya está en mayúsculas y se mide y se pinta al {@link #SMALL_CAPS_SCALE} del tamaño. Lo
 * pone el layout al aplicar {@code Small caps}; el documento no lo escribe.</p>
 *
 * <p>{@code link} es el destino del enlace del tramo ({@link Links}, §5.20), o {@code null} si no
 * lo lleva. No cambia cómo se mide ni cómo se pinta el texto: el subrayado de un enlace de
 * Markdown es el {@code underline} de su tramo.</p>
 */
public record TextFormat(boolean bold, boolean italic, boolean underline, boolean strike, String link,
                         boolean smallCaps) {

    /** Tamaño de las versalitas respecto al de la caja. */
    public static final double SMALL_CAPS_SCALE = 0.8;

    /** Sin formato: el texto tal como lo declara la caja. */
    public static final TextFormat PLAIN = new TextFormat(false, false, false, false);

    /** Un formato sin enlace ni versalitas. */
    public TextFormat(boolean bold, boolean italic, boolean underline, boolean strike) {
        this(bold, italic, underline, strike, null);
    }

    /** Un formato sin versalitas. */
    public TextFormat(boolean bold, boolean italic, boolean underline, boolean strike, String link) {
        this(bold, italic, underline, strike, link, false);
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
        return new TextFormat(bold, italic, underline, strike, destination, smallCaps);
    }

    /** El mismo formato con las versalitas puestas o quitadas. */
    public TextFormat withSmallCaps(boolean small) {
        return small == smallCaps ? this : new TextFormat(bold, italic, underline, strike, link, small);
    }

    /** Lo que se multiplica el tamaño de la caja para pintar el tramo. */
    public double scale() {
        return smallCaps ? SMALL_CAPS_SCALE : 1;
    }
}
