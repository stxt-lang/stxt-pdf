package dev.stxt.boxes.layout.inline;

/**
 * Numeración de páginas en el texto: {@code {page}} y {@code {pages}} (ESPECIFICACION.md §5.4 y
 * §5.10). Se resuelve en dos fases:
 *
 * <ol>
 *   <li>Se pagina midiendo cada marcador como un número de prueba de {@value #DIGITS} cifras
 *       (o más, ver abajo); las líneas conservan los marcadores.</li>
 *   <li>Con el total de páginas ya conocido, {@link InlineLayout#number} sustituye los valores
 *       reales en las líneas que los contienen y vuelve a calcular su ancho, su alineación y su
 *       justificación. Los saltos de línea no cambian.</li>
 * </ol>
 *
 * <p>Si el total tiene más cifras que el número de prueba, el {@link dev.stxt.boxes.fragment.Paginator}
 * vuelve a paginar reservando las cifras del total, hasta que basten.</p>
 */
public final class PageNumbers {

    public static final String PAGE = "{page}";
    public static final String PAGES = "{pages}";
    /** Cifras del número de prueba de la primera paginación: reservan el ancho hasta 99 páginas. */
    public static final int DIGITS = 2;

    private PageNumbers() {
    }

    public static boolean contains(String text) {
        return text != null && (text.contains(PAGE) || text.contains(PAGES));
    }

    /** El texto con los valores de una página. */
    public static String apply(String text, int page, int pages) {
        return text.replace(PAGES, Integer.toString(pages)).replace(PAGE, Integer.toString(page));
    }

    /** Cifras de un número de páginas. */
    public static int digits(int pages) {
        return Integer.toString(pages).length();
    }

    /** Mide los marcadores como un número de prueba de {@code digits} cifras (primera fase). */
    public static TextMeasurer probing(TextMeasurer measurer, int digits) {
        String probe = "0".repeat(digits);
        return (text, font, fontSize) -> measurer.width(
                contains(text) ? text.replace(PAGES, probe).replace(PAGE, probe) : text, font, fontSize);
    }
}
