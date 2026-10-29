package dev.stxt.pdf.model;

/**
 * Formato de un tramo de texto (referencia/comun/formato-del-texto.md): lo que cambia dentro de una línea. La
 * negrita y la cursiva se suman a las de la caja (un tramo en negrita de un texto ya en negrita
 * sigue en negrita); el tamaño, el color y {@code Line height} son siempre los de la caja, y la
 * familia también salvo en el código de un {@code Markdown} ({@code family}).
 *
 * <p>{@code smallCaps} marca las minúsculas que se pintan como versalitas (referencia/comun/formato-del-texto.md): el
 * texto del tramo ya está en mayúsculas y se mide y se pinta al {@link #SMALL_CAPS_SCALE} del tamaño. Lo pone el layout
 * al aplicar {@code Small caps}; el documento no lo escribe.</p>
 *
 * <p>{@code family} es la familia del tramo cuando no es la de la caja: el código de un
 * {@code Markdown} (referencia/componentes/markdown.md), que se escribe con la fuente de su {@code Code font}; {@code
 * null} = la de la caja. Conserva el peso y el estilo de la caja y los de las marcas del tramo.</p>
 *
 * <p>{@code link} es el destino del enlace del tramo ({@link Links}, referencia/comun/enlaces.md), o {@code null} si no
 * lo lleva. No cambia cómo se mide ni cómo se pinta el texto: el subrayado de un enlace de
 * Markdown es el {@code underline} de su tramo.</p>
 */
public record TextFormat(boolean bold, boolean italic, boolean underline, boolean strike, String link,
                         boolean smallCaps, String family) {

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

    /** Un formato con la familia de la caja. */
    public TextFormat(boolean bold, boolean italic, boolean underline, boolean strike, String link,
                      boolean smallCaps) {
        this(bold, italic, underline, strike, link, smallCaps, null);
    }

    /** La fuente del tramo a partir de la de la caja. */
    public FontFace face(FontFace base) {
        if (!bold && !italic && family == null) {
            return base;
        }
        return new FontFace(family != null ? family : base.family(), bold ? FontWeight.BOLD : base.weight(),
                italic ? FontStyle.ITALIC : base.style(), base.ligatures());
    }

    /** Si lleva subrayado o tachado, que se pintan aparte del texto. */
    public boolean decorated() {
        return underline || strike;
    }

    /** El mismo formato con ese enlace. */
    public TextFormat withLink(String destination) {
        return new TextFormat(bold, italic, underline, strike, destination, smallCaps, family);
    }

    /** El mismo formato con las versalitas puestas o quitadas. */
    public TextFormat withSmallCaps(boolean small) {
        return small == smallCaps ? this : new TextFormat(bold, italic, underline, strike, link, small, family);
    }

    /** El mismo formato con esa familia de fuente. */
    public TextFormat withFamily(String fontFamily) {
        return new TextFormat(bold, italic, underline, strike, link, smallCaps, fontFamily);
    }

    /** Lo que se multiplica el tamaño de la caja para pintar el tramo. */
    public double scale() {
        return smallCaps ? SMALL_CAPS_SCALE : 1;
    }
}
