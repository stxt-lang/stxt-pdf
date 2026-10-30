package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.tree.Element;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code Table of contents} (referencia/componentes/table-of-contents.md). Páginas de 180 pt con márgenes de 40: área
 * útil de 515 × 100; con {@code FixedMetrics} y {@code Size: 10}, cada carácter mide 5 pt y cada línea, 12.
 */
class TableOfContentsTest {

    private static final double PAGE = 180;

    /** El índice y, detrás, tres encabezados separados por 100 pt: caen en las páginas 1, 2 y 3. */
    private static String[] document(String contents, String... before) {
        String[] body = {
                "\tTable of contents:", "\t\tId: toc", contents,
                "\tHeading 1:", "\t\tText: Intro",
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 100",
                "\tHeading 2:", "\t\tText: Detalle",
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 100",
                "\tHeading 1:", "\t\tId: last", "\t\tText: Final"};
        return java.util.stream.Stream.concat(java.util.Arrays.stream(before), java.util.Arrays.stream(body))
                .toArray(String[]::new);
    }

    private static List<Fragment> entries(LayoutFixture l) {
        return l.fragment("toc").children();
    }

    private static String text(Fragment f) {
        return f.lines().stream().map(x -> x.text()).reduce((a, b) -> a + "|" + b).orElse("");
    }

    /**
     * Un encabezado con {@code In table of contents: false} sigue siendo un encabezado (el índice lo ve en su sitio y
     * las páginas no cambian) pero no tiene entrada (referencia/componentes/table-of-contents.md).
     */
    @Test
    void aHeadingWithInTableOfContentsFalseHasNoEntry() {
        LayoutFixture l = layOutPaged(PAGE, document("\t\tLevels: 1 2",
                "\tHeading 1:", "\t\tIn table of contents: false", "\t\tText: Portada"));
        List<Fragment> entries = entries(l);
        assertEquals(3, entries.size());
        assertEquals(List.of("Intro", "Detalle", "Final"), entries.stream().map(e -> text(e.children().get(0))).toList());
    }

    @Test
    void oneEntryPerHeadingWithItsTitleALeaderAndItsPage() {
        LayoutFixture l = layOutPaged(PAGE, document("\t\tLevels: 1 2"));
        assertEquals(3, l.pages());
        List<Fragment> entries = entries(l);
        assertEquals(3, entries.size());
        String[] titles = {"Intro", "Detalle", "Final"};
        String[] pages = {"1", "2", "3"};
        for (int i = 0; i < 3; i++) {
            Fragment entry = entries.get(i);
            assertEquals(Element.tocEntry(i == 1 ? 2 : 1), entry.box().element());
            assertEquals(12 * i, entry.y(), 1e-9);
            assertEquals(3, entry.children().size());
            Fragment title = entry.children().get(0);
            Fragment leader = entry.children().get(1);
            Fragment page = entry.children().get(2);
            assertEquals(titles[i], text(title));
            assertEquals(pages[i], text(page));
            assertEquals(Element.TOC_LEADER, leader.box().element());
            // El título y el número miden lo que escriben; la guía llena el hueco, con 4 pt a cada lado.
            double titleWidth = 5 * titles[i].length();
            assertEquals(0, title.x(), 1e-9);
            assertEquals(titleWidth, title.width(), 1e-9);
            assertEquals(titleWidth + 4, leader.x(), 1e-9);
            assertEquals(515 - 5 - titleWidth - 8, leader.width(), 1e-9);
            assertEquals(12, leader.height(), 1e-9);
            assertEquals(510, page.x(), 1e-9);
        }
    }

    @Test
    void theLevelsLimitTheEntries() {
        LayoutFixture l = layOutPaged(PAGE, document("\t\tLevels: 1 1"));
        assertEquals(List.of("Intro", "Final"),
                entries(l).stream().map(e -> text(e.children().get(0))).toList());
    }

    @Test
    void withoutALeaderTheTitleFillsTheGapAndThePageStaysOnTheRight() {
        LayoutFixture l = layOutPaged(PAGE, document("\t\tLevels: 1 1\n\t\tLeader: none"));
        Fragment entry = entries(l).get(0);
        assertEquals(2, entry.children().size());
        assertEquals(510, entry.children().get(0).width() + entry.children().get(0).x(), 1e-9);
        assertEquals(510, entry.children().get(1).x(), 1e-9);
    }

    @Test
    void aHeadingWithoutIdIsReferredWithAGeneratedName() {
        LayoutFixture l = layOutPaged(PAGE, document("\t\tLevels: 1 2"));
        // El último tiene Id (last): su entrada lo usa; los demás, un nombre generado.
        assertEquals("(heading 1)", l.fragment("toc").box().children().get(0).children().get(2).text()
                .replace("{page of: ", "").replace("}", ""));
        assertEquals("{page of: last}", l.fragment("toc").box().children().get(2).children().get(2).text());
    }

    @Test
    void entryRulesStyleEachLevel() {
        LayoutFixture l = layOutPaged(PAGE, document("\t\tLevels: 1 2",
                "\tStyles:", "\t\tFor element: TOC 2", "\t\t\tBox:", "\t\t\t\tPadding: 0 0 0 20",
                "\t\t\tTypography:", "\t\t\t\tSize: 10"));
        List<Fragment> entries = entries(l);
        // Solo el nivel 2 lleva sangría: sus hijos, relativos a su caja de contenido, quedan 20 pt más
        // estrechos y el número sigue pegado al borde derecho.
        assertEquals(20, entries.get(1).padding().left(), 1e-9);
        assertEquals(0, entries.get(0).padding().left(), 1e-9);
        assertEquals(510, entries.get(0).children().get(2).x(), 1e-9);
        assertEquals(490, entries.get(1).children().get(2).x(), 1e-9);
    }

    @Test
    void theIndexMayComeAfterItsHeadings() {
        LayoutFixture l = layOutPaged(PAGE,
                "\tHeading 1:", "\t\tText: Uno",
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 100",
                "\tTable of contents:", "\t\tId: toc");
        // El encabezado está en la página 1; el índice, que no cabe, en la 2.
        assertTrue(l.has(2, "toc"));
        assertEquals("1", text(l.fragment(2, "toc").children().get(0).children().get(2)));
    }
}
