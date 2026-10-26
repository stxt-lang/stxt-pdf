package dev.stxt.boxes.layout.inline;

import dev.stxt.boxes.fragment.TextLine;
import dev.stxt.boxes.layout.LayoutFixture;
import dev.stxt.boxes.tree.FontFace;
import dev.stxt.boxes.tree.Style;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.stxt.boxes.layout.LayoutFixture.layOut;
import static dev.stxt.boxes.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Páginas de 180 pt con márgenes de 40: área útil de 515 × 100 desde (40, 40). */
class PageNumbersTest {

    private static final double PAGE = 180;
    private static final FontFace F = new FontFace("F", Style.FontWeight.NORMAL, Style.FontStyle.NORMAL);

    private static final String[] FOOTER = {"\tBox:", "\t\tId: footer", "\t\tPosition: fixed",
            "\t\tOffset: auto 0 -30 0", "\t\tHeight: 20", "\t\tText align: right",
            "\t\tText: Página {page} de {pages}"};

    private static String[] concat(String[]... parts) {
        return java.util.Arrays.stream(parts).flatMap(java.util.Arrays::stream).toArray(String[]::new);
    }

    @Test
    void replacesTheMarkers() {
        assertEquals("Página 2 de 3", PageNumbers.apply("Página {page} de {pages}", 2, 3));
        assertTrue(PageNumbers.contains("{pages}"));
        assertFalse(PageNumbers.contains("{página}"));
    }

    @Test
    void probeReservesTheDigits() {
        assertEquals("Página 00".length(),
                PageNumbers.probing(new FixedMetrics(1), 2).width("Página {page}", F, 1), 1e-9);
        assertEquals("Página 000".length(),
                PageNumbers.probing(new FixedMetrics(1), 3).width("Página {page}", F, 1), 1e-9);
        assertEquals(2, PageNumbers.digits(99));
        assertEquals(3, PageNumbers.digits(100));
    }

    @Test
    void moreThan99PagesRepaginatesWithThreeDigits() {
        // A ocupa 100 páginas. El pie mide 35 pt (7 caracteres): "Hoja 00" cabe, pero "Hoja 100"
        // no. Al repaginar reservando tres cifras, el número pasa a su propia línea.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBox:", "\t\tId: footer", "\t\tPosition: fixed", "\t\tOffset: auto auto -30 0",
                "\t\tWidth: 35", "\t\tText: Hoja {page}",
                "\tBox:", "\t\tId: A", "\t\tHeight: 10000");
        assertEquals(100, l.pages());
        for (int page : new int[]{1, 7, 100}) {
            List<TextLine> lines = l.fragment(page, "footer").lines();
            assertEquals(List.of("Hoja", Integer.toString(page)), lines.stream().map(TextLine::text).toList());
            assertTrue(lines.get(1).width() <= 35);
        }
    }

    @Test
    void fixedFooterShowsEachPageNumber() {
        // A ocupa tres páginas. "Página 2 de 3" mide 13 × 5 = 65: alineada a la derecha, en 450.
        LayoutFixture l = layOutPaged(PAGE, concat(FOOTER, new String[]{"\tBox:", "\t\tId: A", "\t\tHeight: 250"}));
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
                "\tBox:", "\t\tId: A", "\t\tHeight: 150",
                "\tBox:", "\t\tId: N", "\t\tText: Hoja {page}");
        assertEquals("Hoja 2", l.fragment(2, "N").lines().get(0).text());
        assertEquals(90, l.rect(2, "N").y(), 1e-9);
    }

    @Test
    void continuousDocumentIsPageOneOfOne() {
        LayoutFixture l = layOut("\tBox:", "\t\tId: N", "\t\tText: Página {page} de {pages}");
        assertEquals("Página 1 de 1", l.fragment("N").lines().get(0).text());
    }
}
