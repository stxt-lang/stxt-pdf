package dev.stxt.pdf.layout.grid;

import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.Rect;
import org.junit.jupiter.api.Test;

import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Páginas de 180 pt con márgenes de 40: área útil de 515 × 100 desde (40, 40). */
class GridFragmentationTest {

    private static final double PAGE = 180;

    private static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(), new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height())).toString());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private static final String[] GRID = {"\tGrid:", "\t\tId: G", "\t\tColumn sizes: * *"};

    private static String[] cell(String id, String cell) {
        return new String[]{"\t\tArea: " + cell, "\t\t\tBlock:", "\t\t\t\tId: " + id, "\t\t\t\tBox:",
                "\t\t\t\t\tHeight: 40"};
    }

    private static String[] concat(String[]... parts) {
        return java.util.Arrays.stream(parts).flatMap(java.util.Arrays::stream).toArray(String[]::new);
    }

    @Test
    void breaksBetweenRows() {
        // Filas de 40 en 0, 40 y 80: la tercera pasa a la página 2.
        LayoutFixture l = layOutPaged(PAGE, concat(GRID,
                cell("A", "1 1"), cell("B", "1 2"), cell("C", "2 1"), cell("D", "2 2"), cell("E", "3 1")));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 80, l.rect(1, "G"));
        assertRect(297.5, 80, 257.5, 40, l.rect(1, "D"));
        assertRect(40, 40, 515, 40, l.rect(2, "G"));
        assertRect(40, 40, 257.5, 40, l.rect(2, "E"));
    }

    private static String[] box(String id, String height, String cell) {
        return new String[]{"\t\tArea: " + cell, "\t\t\tBlock:", "\t\t\t\tId: " + id, "\t\t\t\tBox:",
                "\t\t\t\t\tHeight: " + height};
    }

    @Test
    void rowTallerThanAPageBreaksItsItemsInParallel() {
        // La fila 1 mide 150: no cabe ni al principio de página. A se parte (100 + 50) y B cabe
        // entera. En la página 2 sigue A desde arriba, y la fila 2 va detrás de lo que queda
        // de A: 150 − 100 = 50.
        LayoutFixture l = layOutPaged(PAGE, concat(GRID,
                box("A", "150", "1 1"), box("B", "60", "1 2"), box("C", "40", "2 1")));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "G"));
        assertRect(40, 40, 257.5, 100, l.rect(1, "A"));
        assertRect(297.5, 40, 257.5, 60, l.rect(1, "B"));
        assertRect(40, 40, 515, 90, l.rect(2, "G"));
        assertRect(40, 40, 257.5, 50, l.rect(2, "A"));
        assertFalse(l.has(2, "B"));
        assertRect(40, 90, 257.5, 40, l.rect(2, "C"));
    }

    @Test
    void rowThatFitsInAPageIsNotBrokenInParallel() {
        // Tras X (30), la fila de 80 no cabe en lo que queda de página pero sí en una entera: pasa
        // entera a la página 2 (como breaksBetweenRows).
        LayoutFixture l = layOutPaged(PAGE, concat(
                new String[]{"\tBlock:", "\t\tId: X", "\t\tBox:", "\t\t\tHeight: 30"}, GRID,
                box("A", "80", "1 1"), box("B", "40", "1 2")));
        assertEquals(2, l.pages());
        assertFalse(l.has(1, "G"));
        assertRect(40, 40, 257.5, 80, l.rect(2, "A"));
    }

    @Test
    void parallelBreakCanSpanSeveralPages() {
        // A mide 250 y su fila no cabe en ninguna página: 100 + 100 + 50, siempre desde arriba.
        LayoutFixture l = layOutPaged(PAGE, concat(GRID, box("A", "250", "1 1"), box("B", "120", "1 2")));
        assertEquals(3, l.pages());
        assertRect(40, 40, 257.5, 100, l.rect(1, "A"));
        assertRect(297.5, 40, 257.5, 100, l.rect(1, "B"));
        assertRect(40, 40, 257.5, 100, l.rect(2, "A"));
        assertRect(297.5, 40, 257.5, 20, l.rect(2, "B"));
        assertRect(40, 40, 515, 50, l.rect(3, "G"));
        assertFalse(l.has(3, "B"));
    }

    @Test
    void doesNotBreakInsideASpanningItem() {
        // S ocupa las filas 2 y 3: no se puede cortar entre ellas, así que se corta antes de la 2.
        LayoutFixture l = layOutPaged(PAGE, concat(GRID,
                cell("A", "1 1"), cell("B", "1 2"), cell("S", "2 1 2 1"), cell("D", "2 2"), cell("E", "3 2")));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 40, l.rect(1, "G"));
        assertRect(40, 40, 515, 80, l.rect(2, "G"));
        assertRect(40, 40, 257.5, 40, l.rect(2, "S"));
        assertRect(297.5, 40, 257.5, 40, l.rect(2, "D"));
        assertRect(297.5, 80, 257.5, 40, l.rect(2, "E"));
    }

    @Test
    void relativePercentOffsetUsesTheWholeHeightOfAFragmentedGrid() {
        // Como en el flujo de bloque: el 10 % es del Height entero del grid (30).
        LayoutFixture l = layOutPaged(PAGE,
                "\tGrid:", "\t\tId: G", "\t\tBox:", "\t\t\tHeight: 300", "\t\tRow sizes: 80 15",
                "\t\tArea: 2 1", "\t\t\tFrame:", "\t\t\t\tId: R", "\t\t\t\tOffset: 10% auto auto auto",
                "\t\t\t\tBox:", "\t\t\t\t\tHeight: 15");
        assertEquals(3, l.pages());
        assertRect(40, 150, 515, 15, l.rect(1, "R"));
    }

    /**
     * La segunda fila (20 líneas de 12 en una columna de 30) no cabe en ninguna página: empieza en la 2, donde se
     * parte en paralelo desde arriba aunque en el grid entero empiece en 30 (pruebas de mutación). T no se estira (8, 8 y
     * 4 líneas); B se estira a la fila y se parte con ella: 100, 100 y 40.
     */
    @Test
    void aTallRowAfterTheFirstPageIsSplitFromTheTop() {
        String text = String.join(" ", java.util.Collections.nCopies(20, "aaaaa"));
        LayoutFixture l = layOutPaged(PAGE, "\tGrid:", "\t\tColumn sizes: 30 100",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 30",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 30",
                "\t\tArea:", "\t\t\tVertical: start", "\t\t\tText: " + text, "\t\t\t\tId: T",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: B");
        assertEquals(4, l.pages());
        assertFalse(l.has(1, "T"));
        assertRect(40, 40, 30, 96, l.rect(2, "T"));
        assertRect(70, 40, 100, 100, l.rect(2, "B"));
        assertRect(70, 40, 100, 40, l.rect(4, "B"));
    }

    /**
     * Un texto estirado a una fila que se parte tiene el alto de la fila (240) y sus líneas dejan 4 al final de cada
     * página: ese hueco no se le quita, y sus 4 últimas líneas caben en la página 4 (antes era BOX_OVERFLOW).
     */
    @Test
    void aStretchedTextInATallRowFitsItsLines() {
        String text = String.join(" ", java.util.Collections.nCopies(20, "aaaaa"));
        LayoutFixture l = layOutPaged(PAGE, "\tGrid:", "\t\tColumn sizes: 30 100",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 30",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 30",
                "\t\tArea:", "\t\t\tText: " + text, "\t\t\t\tId: T",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: B");
        assertEquals(4, l.pages());
        assertEquals(4, l.fragment(4, "T").lines().size());
        assertRect(40, 40, 30, 100, l.rect(2, "T"));
    }
}
