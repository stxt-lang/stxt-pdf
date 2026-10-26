package dev.stxt.boxes.layout.inline;

/**
 * Numeración de páginas en el texto: {@code {page}} y {@code {pages}} (ESPECIFICACION.md §5.4 y
 * §5.10). Se resuelve en dos fases:
 *
 * <ol>
 *   <li>Se pagina midiendo cada marcador como el número de prueba {@value #PROBE}; las líneas
 *       conservan los marcadores.</li>
 *   <li>Con el total de páginas ya conocido, {@link InlineLayout#number} sustituye los valores
 *       reales en las líneas que los contienen y vuelve a calcular su ancho, su alineación y su
 *       justificación. Los saltos de línea no cambian.</li>
 * </ol>
 */
public final class PageNumbers {

    public static final String PAGE = "{page}";
    public static final String PAGES = "{pages}";
    /** Número de prueba de la primera fase: dos cifras reservan el ancho hasta 99 páginas. */
    public static final String PROBE = "00";

    private PageNumbers() {
    }

    public static boolean contains(String text) {
        return text != null && (text.contains(PAGE) || text.contains(PAGES));
    }

    /** El texto con los valores de una página. */
    public static String apply(String text, int page, int pages) {
        return text.replace(PAGES, Integer.toString(pages)).replace(PAGE, Integer.toString(page));
    }

    /** Mide los marcadores como el número de prueba (primera fase). */
    public static TextMeasurer probing(TextMeasurer measurer) {
        return (text, font, fontSize) -> measurer.width(
                contains(text) ? text.replace(PAGES, PROBE).replace(PAGE, PROBE) : text, font, fontSize);
    }
}
