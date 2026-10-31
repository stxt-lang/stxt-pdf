package dev.stxt.pdf.layout.inline;

/**
 * Page numbering in the text: {@code {page}} and {@code {pages}}
 * (reference/common/breaks.md#page-numbers-page-and-pages and reference/components/document.md#page-numbering). It is
 * resolved in two phases:
 *
 * <ol>
 *   <li>Pagination measures each marker as a probe number of {@value #DIGITS} digits
 *       (or wider, see below); the lines keep the markers.</li>
 *   <li>With the total number of pages known, {@link InlineLayout#number} puts the real values
 *       in the lines that contain them and recomputes their width, their alignment and their
 *       justification. The line breaks do not change.</li>
 * </ol>
 *
 * <p>If some printed number is wider than the probe one (more digits, or Roman numerals), the {@link
 * dev.stxt.pdf.layout.Paginator} paginates again with a probe number that covers it, until it is enough: as many
 * characters as the longest one, with the widest letter of its style ({@code M} in Roman numerals).</p>
 */
public final class PageNumbers {

    public static final String PAGE = "{page}";
    public static final String PAGES = "{pages}";
    /** Digits of the probe number of the first pagination: they reserve the width for up to 99 pages. */
    public static final int DIGITS = 2;

    private PageNumbers() {
    }

    public static boolean contains(String text) {
        return text != null && (text.contains(PAGE) || text.contains(PAGES));
    }

    /** The text with the values of a page, already written in their style. */
    public static String apply(String text, String page, String pages) {
        return text.replace(PAGES, pages).replace(PAGE, page);
    }

    /** The probe number of the first pagination. */
    public static String firstProbe() {
        return "0".repeat(DIGITS);
    }

    /** The wider of two of the characters that make up a probe number: {@code 0} &lt; {@code m} &lt; {@code M}. */
    public static char wider(char a, char b) {
        String order = "0mM";
        return order.indexOf(a) >= order.indexOf(b) ? a : b;
    }

    /**
     * The probe number that covers {@code current} and {@code needed}: the longer of the two, with the wider
     * character. It only grows, so that the pagination converges.
     */
    public static String cover(String current, String needed) {
        if (needed.isEmpty()) {
            return current;
        }
        char c = wider(current.charAt(0), needed.charAt(0));
        return String.valueOf(c).repeat(Math.max(current.length(), needed.length()));
    }

    /** Measures the markers as the probe number {@code probe} (first phase). */
    public static TextMeasurer probing(TextMeasurer measurer, String probe) {
        return (text, font, fontSize) -> measurer.width(
                contains(text) ? text.replace(PAGES, probe).replace(PAGE, probe) : text, font, fontSize);
    }
}
