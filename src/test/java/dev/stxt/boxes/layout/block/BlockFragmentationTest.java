package dev.stxt.boxes.layout.block;

import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.layout.LayoutFixture;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Rect;
import org.junit.jupiter.api.Test;

import static dev.stxt.boxes.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Páginas de 180 pt con márgenes de 40: área útil de 515 × 100 desde (40, 40). */
class BlockFragmentationTest {

    private static final double PAGE = 180;

    private static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(), new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height())).toString());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    @Test
    void fixedHeightIsTheTotalOfAllFragments() {
        // 250 = 100 + 100 + 50.
        LayoutFixture l = layOutPaged(PAGE, "\tBox:", "\t\tId: A", "\t\tHeight: 250");
        assertEquals(3, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "A"));
        assertRect(40, 40, 515, 100, l.rect(2, "A"));
        assertRect(40, 40, 515, 50, l.rect(3, "A"));
    }

    @Test
    void boxThatDoesNotFitIsSliced() {
        // B empieza en 60: caben 40 de sus 60 pt y el resto pasa a la página 2.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBox:", "\t\tId: A", "\t\tHeight: 60",
                "\tBox:", "\t\tId: B", "\t\tHeight: 60");
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 60, l.rect(1, "A"));
        assertRect(40, 100, 515, 40, l.rect(1, "B"));
        assertRect(40, 40, 515, 20, l.rect(2, "B"));
    }

    @Test
    void avoidInsideMovesTheBox() {
        LayoutFixture l = layOutPaged(PAGE,
                "\tBox:", "\t\tId: A", "\t\tHeight: 60",
                "\tBox:", "\t\tId: B", "\t\tHeight: 60", "\t\tBreak inside: avoid");
        assertEquals(2, l.pages());
        assertFalse(l.has(1, "B"));
        assertRect(40, 40, 515, 60, l.rect(2, "B"));
    }

    @Test
    void sliceDecorationDropsBorderAtTheBreak() {
        // Q: borde 2 y relleno 3. Primer fragmento: 5 + C1 (40) + 55 de C2 hasta el final de la página.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBox:", "\t\tId: Q", "\t\tBorder: 2", "\t\tPadding: 3",
                "\t\tBox:", "\t\t\tId: C1", "\t\t\tHeight: 40",
                "\t\tBox:", "\t\t\tId: C2", "\t\t\tHeight: 80");
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "Q"));
        assertRect(45, 45, 505, 40, l.rect(1, "C1"));
        assertRect(45, 85, 505, 55, l.rect(1, "C2"));
        // Segundo fragmento: sin borde ni relleno superiores; 25 de C2 + 5 abajo.
        assertRect(40, 40, 515, 30, l.rect(2, "Q"));
        assertRect(45, 40, 505, 25, l.rect(2, "C2"));
        Fragment first = l.fragment(1, "Q");
        Fragment second = l.fragment(2, "Q");
        assertEquals(new Insets(2, 2, 0, 2), first.border());
        assertEquals(new Insets(0, 2, 2, 2), second.border());
        assertTrue(first.first() && !first.last());
        assertTrue(!second.first() && second.last());
    }

    @Test
    void unforcedBreakTruncatesTheMargin() {
        LayoutFixture l = layOutPaged(PAGE,
                "\tBox:", "\t\tId: A", "\t\tHeight: 95",
                "\tBox:", "\t\tId: B", "\t\tHeight: 10", "\t\tMargin: 10 0 0 0", "\t\tBreak inside: avoid");
        assertRect(40, 40, 515, 10, l.rect(2, "B"));
    }

    @Test
    void forcedBreakKeepsTheMargin() {
        LayoutFixture l = layOutPaged(PAGE,
                "\tBox:", "\t\tId: A", "\t\tHeight: 10",
                "\tBox:", "\t\tId: B", "\t\tHeight: 10", "\t\tMargin: 10 0 0 0", "\t\tBreak before: page");
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 10, l.rect(1, "A"));
        assertRect(40, 50, 515, 10, l.rect(2, "B"));
    }

    @Test
    void forcedBreakOfTheFirstChildPropagates() {
        // El corte forzado de C sale de P: P entera pasa a la página 2.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBox:", "\t\tId: A", "\t\tHeight: 10",
                "\tBox:", "\t\tId: P",
                "\t\tBox:", "\t\t\tId: C", "\t\t\tHeight: 10", "\t\t\tBreak before: page",
                "\tBox:", "\t\tId: D", "\t\tHeight: 10", "\t\tBreak after: page");
        assertEquals(2, l.pages());
        assertFalse(l.has(1, "P"));
        assertRect(40, 40, 515, 10, l.rect(2, "P"));
        assertRect(40, 50, 515, 10, l.rect(2, "D"));
    }

    @Test
    void forcedBreakAtTheStartOfThePageIsIgnored() {
        LayoutFixture l = layOutPaged(PAGE, "\tBox:", "\t\tId: A", "\t\tHeight: 10", "\t\tBreak before: page");
        assertEquals(1, l.pages());
        assertRect(40, 40, 515, 10, l.rect(1, "A"));
    }

    @Test
    void avoidAfterKeepsTheTitleWithTheNextBox() {
        // T no puede quedar sola al final: se corta antes de T, que es un corte sin reglas incumplidas.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBox:", "\t\tId: X", "\t\tHeight: 50",
                "\tBox:", "\t\tId: T", "\t\tHeight: 20", "\t\tBreak after: avoid",
                "\tBox:", "\t\tId: P", "\t\tHeight: 60", "\t\tBreak inside: avoid");
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 50, l.rect(1, "X"));
        assertFalse(l.has(1, "T"));
        assertRect(40, 40, 515, 20, l.rect(2, "T"));
        assertRect(40, 60, 515, 60, l.rect(2, "P"));
    }

    @Test
    void avoidIsRelaxedWhenThereIsNoOtherBreak() {
        // T es lo primero de la página: no hay corte mejor que el que incumple avoid.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBox:", "\t\tId: T", "\t\tHeight: 20", "\t\tBreak after: avoid",
                "\tBox:", "\t\tId: P", "\t\tHeight: 90", "\t\tBreak inside: avoid");
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 20, l.rect(1, "T"));
        assertRect(40, 40, 515, 90, l.rect(2, "P"));
    }

    @Test
    void avoidInsideIsRelaxedWhenTheBoxIsTallerThanThePage() {
        // B pasa a la página 2 y, como tampoco cabe ahí entera, se parte igualmente.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBox:", "\t\tId: A", "\t\tHeight: 10",
                "\tBox:", "\t\tId: B", "\t\tHeight: 150", "\t\tBreak inside: avoid",
                "\tBox:", "\t\tId: C", "\t\tHeight: 10");
        assertEquals(3, l.pages());
        assertRect(40, 40, 515, 10, l.rect(1, "A"));
        assertRect(40, 40, 515, 100, l.rect(2, "B"));
        assertRect(40, 40, 515, 50, l.rect(3, "B"));
        assertRect(40, 90, 515, 10, l.rect(3, "C"));
    }

    @Test
    void absoluteChildFallsOnThePageOfItsPosition() {
        LayoutFixture l = layOutPaged(PAGE,
                "\tBox:", "\t\tId: A", "\t\tHeight: 90",
                "\tBox:", "\t\tId: P", "\t\tPosition: relative", "\t\tBreak inside: avoid",
                "\t\tBox:", "\t\t\tHeight: 30",
                "\t\tBox:", "\t\t\tId: S", "\t\t\tPosition: absolute", "\t\t\tOffset: 0 0 auto auto",
                "\t\t\tWidth: 20", "\t\t\tHeight: 20");
        assertFalse(l.has(1, "S"));
        assertRect(535, 40, 20, 20, l.rect(2, "S"));
    }
}
