package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pages of 180 pt with margins of 40: page area of 515 × 100 from (40, 40). */
class PageNumbersTest {

    private static final double PAGE = 180;
    private static final FontFace F = new FontFace("F", FontWeight.NORMAL, FontStyle.NORMAL);

    /** Fixed footer; the {@code Id} is on its {@code Text}, which takes up the whole width of the footer. */
    private static final String[] FOOTER = {"\tPage overlay:", "\t\tOffset: auto 0 -30 0", "\t\tBox:", "\t\t\tHeight: 20",
            "\t\tTypography:", "\t\t\tAlign: right",
            "\t\tText: Página {page} de {pages}", "\t\t\tId: footer"};

    private static String[] concat(String[]... parts) {
        return java.util.Arrays.stream(parts).flatMap(java.util.Arrays::stream).toArray(String[]::new);
    }

    @Test
    void replacesTheMarkers() {
        assertEquals("Página 2 de 3", PageNumbers.apply("Página {page} de {pages}", "2", "3"));
        assertTrue(PageNumbers.contains("{pages}"));
        assertFalse(PageNumbers.contains("{página}"));
    }

    @Test
    void probeReservesTheDigits() {
        assertEquals("Página 00".length(),
                PageNumbers.probing(new FixedMetrics(1), "00").width("Página {page}", F, 1), 1e-9);
        assertEquals("Página 000".length(),
                PageNumbers.probing(new FixedMetrics(1), "000").width("Página {page}", F, 1), 1e-9);
    }

    /** The trial number only grows: the longer of the two, with the widest letter ({@code 0}, {@code m}, {@code M}). */
    @Test
    void theProbeCoversTheWidestNumber() {
        assertEquals("000", PageNumbers.cover("00", "000"));
        assertEquals("00", PageNumbers.cover("00", "0"));
        assertEquals("mmmm", PageNumbers.cover("00", "mmmm"));
        assertEquals("MMM", PageNumbers.cover("mmm", "M"));
        assertEquals("00", PageNumbers.cover("00", ""));
    }

    @Test
    void moreThan99PagesRepaginatesWithThreeDigits() {
        // A takes up 100 pages. The footer measures 35 pt (7 characters): "Hoja 00" fits, but "Hoja 100"
        // does not. When paginating again reserving three figures, the number moves to its own line.
        LayoutFixture l = layOutPaged(PAGE,
                "\tPage overlay:", "\t\tOffset: auto auto -30 0", "\t\tBox:", "\t\t\tWidth: 35",
                "\t\tText: Hoja {page}", "\t\t\tId: footer",
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 10000");
        assertEquals(100, l.pages());
        for (int page : new int[]{1, 7, 100}) {
            List<TextLine> lines = l.fragment(page, "footer").lines();
            assertEquals(List.of("Hoja", Integer.toString(page)), lines.stream().map(TextLine::text).toList());
            assertTrue(lines.get(1).width() <= 35);
        }
    }

    @Test
    void romanNumbersRepaginateWithTheWidestOne() {
        // A takes up 8 pages, numbered in Roman numerals. The footer measures 35 pt (7 characters): "Hoja 00" fits,
        // but "Hoja viii" does not. When paginating again reserving four letters (those of viii), the number moves to
        // its own line.
        LayoutFixture l = layOutPaged(PAGE,
                "\tPage overlay:", "\t\tOffset: auto auto -30 0", "\t\tBox:", "\t\t\tWidth: 35",
                "\t\tText: Hoja {page}", "\t\t\tId: footer",
                "\tSection:", "\t\tPage number format: lower-roman",
                "\t\tBlock:", "\t\t\tId: A", "\t\t\tBox:", "\t\t\t\tHeight: 800");
        assertEquals(8, l.pages());
        assertEquals(List.of("Hoja", "viii"), l.fragment(8, "footer").lines().stream().map(TextLine::text).toList());
        assertEquals(List.of("Hoja", "i"), l.fragment(1, "footer").lines().stream().map(TextLine::text).toList());
    }

    @Test
    void fixedFooterShowsEachPageNumber() {
        // A takes up three pages. "Página 2 de 3" measures 13 × 5 = 65: aligned to the right, at 450.
        LayoutFixture l = layOutPaged(PAGE, concat(FOOTER, new String[]{"\tBlock:", "\t\tId: A", "\t\tBox:",
                "\t\t\tHeight: 250"}));
        assertEquals(3, l.pages());
        for (int page = 1; page <= 3; page++) {
            TextLine line = l.fragment(page, "footer").lines().get(0);
            assertEquals("Página " + page + " de 3", line.text());
            assertEquals(450, line.x(), 1e-9);
            assertEquals(65, line.width(), 1e-9);
        }
    }

    @Test
    void inFlowTextIsNumberedToo() {
        // N falls on page 2, after the 50 pt left of A.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 150",
                "\tText: Hoja {page}", "\t\tId: N");
        assertEquals("Hoja 2", l.fragment(2, "N").lines().get(0).text());
        assertEquals(90, l.rect(2, "N").y(), 1e-9);
    }

    @Test
    void singlePageDocumentIsPageOneOfOne() {
        LayoutFixture l = layOut("\tText: Página {page} de {pages}", "\t\tId: N");
        assertEquals("Página 1 de 1", l.fragment("N").lines().get(0).text());
    }
}
