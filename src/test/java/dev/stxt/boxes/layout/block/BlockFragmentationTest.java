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
        LayoutFixture l = layOutPaged(PAGE, "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 250");
        assertEquals(3, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "A"));
        assertRect(40, 40, 515, 100, l.rect(2, "A"));
        assertRect(40, 40, 515, 50, l.rect(3, "A"));
    }

    @Test
    void boxThatDoesNotFitIsSliced() {
        // B empieza en 60: caben 40 de sus 60 pt y el resto pasa a la página 2.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 60",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 60");
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 60, l.rect(1, "A"));
        assertRect(40, 100, 515, 40, l.rect(1, "B"));
        assertRect(40, 40, 515, 20, l.rect(2, "B"));
    }

    @Test
    void avoidInsideMovesTheBox() {
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 60",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 60", "\t\tBreak:", "\t\t\tInside: avoid");
        assertEquals(2, l.pages());
        assertFalse(l.has(1, "B"));
        assertRect(40, 40, 515, 60, l.rect(2, "B"));
    }

    @Test
    void sliceDecorationDropsBorderAtTheBreak() {
        // Q: borde 2 y relleno 3. Primer fragmento: 5 + C1 (40) + 55 de C2 hasta el final de la página.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: Q", "\t\tBox:", "\t\t\tBorder: 2", "\t\t\tPadding: 3",
                "\t\tBlock:", "\t\t\tId: C1", "\t\t\tBox:", "\t\t\t\tHeight: 40",
                "\t\tBlock:", "\t\t\tId: C2", "\t\t\tBox:", "\t\t\t\tHeight: 80");
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
    void sliceReservesBottomDecorationWhenTheBoxEnds() {
        // El contenido (40 + 55) cabe en 100, pero el relleno inferior (10) no: con 90 para el
        // contenido, C2 (avoid) pasa a la página 2 y Q termina allí con su relleno.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: Q", "\t\tBox:", "\t\t\tPadding: 0 0 10 0",
                "\t\tBlock:", "\t\t\tId: C1", "\t\t\tBox:", "\t\t\t\tHeight: 40",
                "\t\tBlock:", "\t\t\tId: C2", "\t\t\tBox:", "\t\t\t\tHeight: 55", "\t\t\tBreak:",
                "\t\t\t\tInside: avoid");
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 40, l.rect(1, "Q"));
        assertRect(40, 40, 515, 65, l.rect(2, "Q"));
        assertRect(40, 40, 515, 55, l.rect(2, "C2"));
    }

    @Test
    void sliceReservesBottomDecorationForText() {
        // Cinco líneas de 20 caben en 100, pero el relleno inferior no. Con 90 caben cuatro, que
        // dejarían una viuda: se cortan tres y las dos últimas pasan con el relleno.
        LayoutFixture l = layOutPaged(PAGE,
                "\tText: aaaaaaaa bbbbbbbb cccccccc dddddddd eeeeeeee", "\t\tId: Q", "\t\tBox:", "\t\t\tWidth: 50",
                "\t\t\tPadding: 0 0 10 0", "\t\tTypography:", "\t\t\tLine height: 20");
        assertEquals(2, l.pages());
        assertRect(40, 40, 50, 60, l.rect(1, "Q"));
        assertRect(40, 40, 50, 50, l.rect(2, "Q"));
    }

    @Test
    void sliceDoesNotReserveWhenTheBoxContinues() {
        // Q se parte de todos modos: el primer fragmento llega al final de la página, sin hueco.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: Q", "\t\tBox:", "\t\t\tPadding: 0 0 10 0",
                "\t\tBlock:", "\t\t\tId: C1", "\t\t\tBox:", "\t\t\t\tHeight: 150");
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "Q"));
        assertRect(40, 40, 515, 60, l.rect(2, "Q"));
    }

    @Test
    void cloneDecorationRepeatsBorderInEveryFragment() {
        // Como el anterior, con clone: el contenido de cada página cabe entre 5 arriba y 5 abajo
        // (90 pt). Página 1: C1 (40) + 50 de C2. Página 2: 5 + los 30 que faltan de C2 + 5.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: Q", "\t\tBox:", "\t\t\tBorder: 2", "\t\t\tPadding: 3", "\t\tBreak:",
                "\t\t\tDecoration: clone",
                "\t\tBlock:", "\t\t\tId: C1", "\t\t\tBox:", "\t\t\t\tHeight: 40",
                "\t\tBlock:", "\t\t\tId: C2", "\t\t\tBox:", "\t\t\t\tHeight: 80");
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "Q"));
        assertRect(45, 85, 505, 50, l.rect(1, "C2"));
        assertRect(40, 40, 515, 40, l.rect(2, "Q"));
        assertRect(45, 45, 505, 30, l.rect(2, "C2"));
        assertEquals(new Insets(2, 2, 2, 2), l.fragment(1, "Q").border());
        assertEquals(new Insets(3, 3, 3, 3), l.fragment(2, "Q").padding());
    }

    @Test
    void cloneFixedHeightIsTheTotalOfTheContent() {
        // Height 250 con borde 5: 240 de contenido en trozos de 90, cada uno con 10 de borde.
        LayoutFixture l = layOutPaged(PAGE, "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 250", "\t\t\tBorder: 5",
                "\t\tBreak:", "\t\t\tDecoration: clone");
        assertEquals(3, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "A"));
        assertRect(40, 40, 515, 100, l.rect(2, "A"));
        assertRect(40, 40, 515, 70, l.rect(3, "A"));
    }

    @Test
    void cloneTextKeepsItsDecorationOnEveryPage() {
        // Relleno 10 con clone: caben 80 / 12 = 6 líneas por página; 10 líneas = 6 + 4.
        LayoutFixture l = layOutPaged(PAGE, "\tText: " + String.join(" ", java.util.Collections.nCopies(10, "aaaaa")),
                "\t\tId: P", "\t\tBox:", "\t\t\tWidth: 50", "\t\t\tPadding: 10", "\t\tBreak:",
                "\t\t\tDecoration: clone");
        assertEquals(2, l.pages());
        assertRect(40, 40, 50, 92, l.rect(1, "P"));
        assertRect(40, 40, 50, 68, l.rect(2, "P"));
        assertEquals(6, l.fragment(1, "P").lines().size());
    }

    @Test
    void unforcedBreakTruncatesTheMargin() {
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 95",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tMargin: 10 0 0 0", "\t\tBreak:",
                "\t\t\tInside: avoid");
        assertRect(40, 40, 515, 10, l.rect(2, "B"));
    }

    @Test
    void forcedBreakKeepsTheMargin() {
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 10",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tMargin: 10 0 0 0", "\t\tBreak:",
                "\t\t\tBefore: page");
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 10, l.rect(1, "A"));
        assertRect(40, 50, 515, 10, l.rect(2, "B"));
    }

    @Test
    void forcedBreakOfTheFirstChildPropagates() {
        // El corte forzado de C sale de P: P entera pasa a la página 2.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 10",
                "\tBlock:", "\t\tId: P",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 10", "\t\t\tBreak:", "\t\t\t\tBefore: page",
                "\tBlock:", "\t\tId: D", "\t\tBox:", "\t\t\tHeight: 10", "\t\tBreak:", "\t\t\tAfter: page");
        assertEquals(2, l.pages());
        assertFalse(l.has(1, "P"));
        assertRect(40, 40, 515, 10, l.rect(2, "P"));
        assertRect(40, 50, 515, 10, l.rect(2, "D"));
    }

    @Test
    void forcedBreakAtTheStartOfThePageIsIgnored() {
        LayoutFixture l = layOutPaged(PAGE, "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 10", "\t\tBreak:",
                "\t\t\tBefore: page");
        assertEquals(1, l.pages());
        assertRect(40, 40, 515, 10, l.rect(1, "A"));
    }

    @Test
    void avoidAfterKeepsTheTitleWithTheNextBox() {
        // T no puede quedar sola al final: se corta antes de T, que es un corte sin reglas incumplidas.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: X", "\t\tBox:", "\t\t\tHeight: 50",
                "\tBlock:", "\t\tId: T", "\t\tBox:", "\t\t\tHeight: 20", "\t\tBreak:", "\t\t\tAfter: avoid",
                "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tHeight: 60", "\t\tBreak:", "\t\t\tInside: avoid");
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
                "\tBlock:", "\t\tId: T", "\t\tBox:", "\t\t\tHeight: 20", "\t\tBreak:", "\t\t\tAfter: avoid",
                "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tHeight: 90", "\t\tBreak:", "\t\t\tInside: avoid");
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 20, l.rect(1, "T"));
        assertRect(40, 40, 515, 90, l.rect(2, "P"));
    }

    @Test
    void avoidInsideIsRelaxedWhenTheBoxIsTallerThanThePage() {
        // B pasa a la página 2 y, como tampoco cabe ahí entera, se parte igualmente.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 10",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 150", "\t\tBreak:", "\t\t\tInside: avoid",
                "\tBlock:", "\t\tId: C", "\t\tBox:", "\t\t\tHeight: 10");
        assertEquals(3, l.pages());
        assertRect(40, 40, 515, 10, l.rect(1, "A"));
        assertRect(40, 40, 515, 100, l.rect(2, "B"));
        assertRect(40, 40, 515, 50, l.rect(3, "B"));
        assertRect(40, 90, 515, 10, l.rect(3, "C"));
    }

    @Test
    void absoluteChildFallsOnThePageOfItsPosition() {
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 90",
                "\tRelative:", "\t\tId: P", "\t\tBreak:", "\t\t\tInside: avoid",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 30",
                "\t\tAbsolute:", "\t\t\tId: S", "\t\t\tOffset: 0 0 auto auto", "\t\t\tBox:", "\t\t\t\tWidth: 20",
                "\t\t\t\tHeight: 20");
        assertFalse(l.has(1, "S"));
        assertRect(535, 40, 20, 20, l.rect(2, "S"));
    }
}
