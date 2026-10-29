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

/** Páginas de 180 pt con márgenes de 40: área útil de 515 × 100 desde (40, 40). */
class PageNumbersTest {

    private static final double PAGE = 180;
    private static final FontFace F = new FontFace("F", FontWeight.NORMAL, FontStyle.NORMAL);

    /** Pie fijo; el {@code Id} está en su {@code Text}, que ocupa todo el ancho del pie. */
    private static final String[] FOOTER = {"\tFixed:", "\t\tOffset: auto 0 -30 0", "\t\tBox:", "\t\t\tHeight: 20",
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

    /** El número de prueba solo crece: el más largo de los dos, con la letra más ancha ({@code 0}, {@code m}, {@code M}). */
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
        // A ocupa 100 páginas. El pie mide 35 pt (7 caracteres): "Hoja 00" cabe, pero "Hoja 100"
        // no. Al repaginar reservando tres cifras, el número pasa a su propia línea.
        LayoutFixture l = layOutPaged(PAGE,
                "\tFixed:", "\t\tOffset: auto auto -30 0", "\t\tBox:", "\t\t\tWidth: 35",
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
        // A ocupa 8 páginas, numeradas en romanos. El pie mide 35 pt (7 caracteres): "Hoja 00" cabe, pero "Hoja viii"
        // no. Al repaginar reservando cuatro letras (las de viii), el número pasa a su propia línea.
        LayoutFixture l = layOutPaged(PAGE,
                "\tFixed:", "\t\tOffset: auto auto -30 0", "\t\tBox:", "\t\t\tWidth: 35",
                "\t\tText: Hoja {page}", "\t\t\tId: footer",
                "\tSection:", "\t\tPage style: lower-roman",
                "\t\tBlock:", "\t\t\tId: A", "\t\t\tBox:", "\t\t\t\tHeight: 800");
        assertEquals(8, l.pages());
        assertEquals(List.of("Hoja", "viii"), l.fragment(8, "footer").lines().stream().map(TextLine::text).toList());
        assertEquals(List.of("Hoja", "i"), l.fragment(1, "footer").lines().stream().map(TextLine::text).toList());
    }

    @Test
    void fixedFooterShowsEachPageNumber() {
        // A ocupa tres páginas. "Página 2 de 3" mide 13 × 5 = 65: alineada a la derecha, en 450.
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
        // N cae en la página 2, detrás de los 50 pt que quedan de A.
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
