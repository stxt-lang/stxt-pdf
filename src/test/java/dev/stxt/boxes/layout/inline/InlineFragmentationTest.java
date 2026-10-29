package dev.stxt.boxes.layout.inline;

import dev.stxt.boxes.fragment.TextLine;
import dev.stxt.boxes.layout.LayoutFixture;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.model.TextFormat;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static dev.stxt.boxes.layout.LayoutFixture.layOutPaged;
import static dev.stxt.boxes.layout.LayoutFixture.layOutPagedRuns;
import static dev.stxt.boxes.tree.TextRuns.run;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Páginas de 180 pt con márgenes de 40: área útil de 515 × 100 desde (40, 40). Con
 * {@code Width: 30} cada palabra de 5 letras (25 pt) ocupa una línea de 12 pt: caben 8 por página.
 */
class InlineFragmentationTest {

    private static final double PAGE = 180;

    private static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(), new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height())).toString());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private static String words(int n) {
        return String.join(" ", java.util.Collections.nCopies(n, "aaaaa"));
    }

    private static int lines(LayoutFixture l, int page, String id) {
        return l.fragment(page, id).lines().size();
    }

    @Test
    void paragraphFillsThePage() {
        LayoutFixture l = layOutPaged(PAGE, "\tText: " + words(10), "\t\tId: P", "\t\tBox:", "\t\t\tWidth: 30");
        assertEquals(2, l.pages());
        assertRect(40, 40, 30, 96, l.rect(1, "P"));
        assertRect(40, 40, 30, 24, l.rect(2, "P"));
        assertEquals(0, l.fragment(2, "P").lines().get(0).y());
    }

    @Test
    void widowsTakeALineToTheNextPage() {
        // 8 + 1 dejaría una viuda: 7 + 2.
        LayoutFixture l = layOutPaged(PAGE, "\tText: " + words(9), "\t\tId: P", "\t\tBox:", "\t\t\tWidth: 30");
        assertEquals(7, lines(l, 1, "P"));
        assertEquals(2, lines(l, 2, "P"));
    }

    @Test
    void orphanMovesTheWholeParagraph() {
        // Solo cabe una línea: quedaría huérfana, y cortar antes del párrafo no incumple nada.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: X", "\t\tBox:", "\t\t\tHeight: 80",
                "\tText: " + words(9), "\t\tId: P", "\t\tBox:", "\t\t\tWidth: 30");
        assertEquals(3, l.pages());
        assertFalse(l.has(1, "P"));
        assertEquals(7, lines(l, 2, "P"));
        assertEquals(2, lines(l, 3, "P"));
    }

    @Test
    void orphansAreRelaxedBeforeAvoid() {
        // Cortar antes de P incumpliría el avoid de T; dejar una huérfana es menos grave (§5.10).
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: T", "\t\tBox:", "\t\t\tHeight: 80", "\t\tBreak:", "\t\t\tAfter: avoid",
                "\tText: " + words(9), "\t\tId: P", "\t\tBox:", "\t\t\tWidth: 30");
        assertEquals(2, l.pages());
        assertRect(40, 120, 30, 12, l.rect(1, "P"));
        assertRect(40, 40, 30, 96, l.rect(2, "P"));
    }

    @Test
    void breakBetweenParagraphs() {
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: X", "\t\tBox:", "\t\t\tHeight: 30",
                "\tText:", "\t\tId: P", "\t\tBox:", "\t\t\tWidth: 30", "\t\tContent >>", "\t\t\t" + words(5), "",
                "\t\t\t" + words(5));
        assertEquals(2, l.pages());
        assertRect(40, 70, 30, 60, l.rect(1, "P"));
        assertRect(40, 40, 30, 60, l.rect(2, "P"));
    }

    // Una línea más alta que la página no se puede partir: es un error (catálogo de errores).

    @Test
    void runsContinueOnTheNextPage() {
        // La novena palabra, en negrita, abre la página 2.
        LayoutFixture l = layOutPagedRuns(PAGE,
                Map.of("P", List.of(run(words(8) + " "), run("aaaaa", "b"), run(" aaaaa"))),
                "\tText: " + words(10), "\t\tId: P", "\t\tBox:", "\t\t\tWidth: 30");
        assertEquals(8, lines(l, 1, "P"));
        List<TextLine> lines = l.fragment(2, "P").lines();
        assertEquals(List.of(new TextLine.Run(0, 25, "aaaaa", run("aaaaa", "b").format())), lines.get(0).runs());
        assertEquals(List.of(new TextLine.Run(0, 25, "aaaaa", TextFormat.PLAIN)), lines.get(1).runs());
    }

    /**
     * Un texto con Height: 150 llega al final de la página 1 (100), pero sus 8 líneas solo consumen 96: en la 2 le
     * quedan 54 para las 2 líneas que siguen (§5.10).
     */
    @Test
    void theHeightOfATextIsTheTotal() {
        LayoutFixture l = layOutPaged(PAGE, "\tText: " + words(10), "\t\tId: P", "\t\tBox:", "\t\t\tWidth: 30",
                "\t\t\tHeight: 150");
        assertEquals(2, l.pages());
        assertRect(40, 40, 30, 100, l.rect(1, "P"));
        assertRect(40, 40, 30, 54, l.rect(2, "P"));
    }

    /**
     * Height igual al contenido (20 líneas, 240) en páginas de 100: cada página tiene 8 líneas (96) y el hueco de 4
     * del final no se le quita al texto, así que las 4 últimas caben en la página 3. Antes era BOX_OVERFLOW: la caja
     * consumía 100 por página y le quedaban 40 para 48 de líneas.
     */
    @Test
    void aHeightEqualToTheContentIsNotAnOverflowWhenItBreaks() {
        LayoutFixture l = layOutPaged(PAGE, "\tText: " + words(20), "\t\tId: P", "\t\tBox:", "\t\t\tWidth: 30",
                "\t\t\tHeight: 240",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 240",
                "\t\tText: " + words(20), "\t\t\tId: Q", "\t\t\tBox:", "\t\t\t\tWidth: 30");
        assertRect(40, 40, 30, 100, l.rect(1, "P"));
        assertRect(40, 40, 30, 100, l.rect(2, "P"));
        assertRect(40, 40, 30, 48, l.rect(3, "P"));
        assertEquals(4, lines(l, 3, "P"));
        // Un Block con el mismo alto y el texto dentro, detrás (empieza en la página 3): tampoco desborda, y sus 20
        // líneas están en sus fragmentos.
        int total = 0;
        for (int page = 3; page <= l.pages(); page++) {
            total += l.has(page, "Q") ? lines(l, page, "Q") : 0;
        }
        assertEquals(20, total);
    }
}
