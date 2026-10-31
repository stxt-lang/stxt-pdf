package dev.stxt.pdf.layout.inline;

/**
 * Numeración de páginas en el texto: {@code {page}} y {@code {pages}} (referencia/comun/cortes.md y
 * referencia/comun/cortes.md). Se resuelve en dos fases:
 *
 * <ol>
 *   <li>Se pagina midiendo cada marcador como un número de prueba de {@value #DIGITS} cifras
 *       (o más ancho, ver abajo); las líneas conservan los marcadores.</li>
 *   <li>Con el total de páginas ya conocido, {@link InlineLayout#number} sustituye los valores
 *       reales en las líneas que los contienen y vuelve a calcular su ancho, su alineación y su
 *       justificación. Los saltos de línea no cambian.</li>
 * </ol>
 *
 * <p>Si algún número impreso es más ancho que el de prueba (más cifras, o romanos), el {@link
 * dev.stxt.pdf.layout.Paginator} vuelve a paginar con un número de prueba que lo cubre, hasta que basta: tantos
 * caracteres como el más largo, con la letra más ancha de su estilo ({@code M} en romanos).</p>
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

    /** El texto con los valores de una página, ya escritos con su estilo. */
    public static String apply(String text, String page, String pages) {
        return text.replace(PAGES, pages).replace(PAGE, page);
    }

    /** El número de prueba de la primera paginación. */
    public static String firstProbe() {
        return "0".repeat(DIGITS);
    }

    /** El carácter más ancho de dos de los que forman un número de prueba: {@code 0} &lt; {@code m} &lt; {@code M}. */
    public static char wider(char a, char b) {
        String order = "0mM";
        return order.indexOf(a) >= order.indexOf(b) ? a : b;
    }

    /**
     * El número de prueba que cubre a {@code current} y a {@code needed}: el más largo de los dos, con el carácter más
     * ancho. Solo crece, para que la paginación converja.
     */
    public static String cover(String current, String needed) {
        if (needed.isEmpty()) {
            return current;
        }
        char c = wider(current.charAt(0), needed.charAt(0));
        return String.valueOf(c).repeat(Math.max(current.length(), needed.length()));
    }

    /** Mide los marcadores como el número de prueba {@code probe} (primera fase). */
    public static TextMeasurer probing(TextMeasurer measurer, String probe) {
        return (text, font, fontSize) -> measurer.width(
                contains(text) ? text.replace(PAGES, probe).replace(PAGE, probe) : text, font, fontSize);
    }
}
