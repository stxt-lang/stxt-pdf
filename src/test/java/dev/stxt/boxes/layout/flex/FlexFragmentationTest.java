package dev.stxt.boxes.layout.flex;

import dev.stxt.boxes.layout.LayoutFixture;
import dev.stxt.boxes.model.Rect;
import org.junit.jupiter.api.Test;

import static dev.stxt.boxes.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Páginas de 180 pt con márgenes de 40: área útil de 515 × 100 desde (40, 40). */
class FlexFragmentationTest {

    private static final double PAGE = 180;

    private static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(), new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height())).toString());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private static String[] item(String id, String... extra) {
        String[] base = {"\t\tBox:", "\t\t\tId: " + id, "\t\t\tHeight: 40"};
        String[] all = new String[base.length + extra.length];
        System.arraycopy(base, 0, all, 0, base.length);
        for (int i = 0; i < extra.length; i++) {
            all[base.length + i] = "\t\t\t" + extra[i];
        }
        return all;
    }

    private static String[] concat(String[]... parts) {
        return java.util.Arrays.stream(parts).flatMap(java.util.Arrays::stream).toArray(String[]::new);
    }

    @Test
    void rowBreaksBetweenLines() {
        // Una línea por ítem (300 + 10 + 300 > 515): líneas en 0, 50, 100 y 150.
        LayoutFixture l = layOutPaged(PAGE, concat(
                new String[]{"\tBox:", "\t\tId: F", "\t\tLayout: flex", "\t\tFlex:", "\t\t\tWrap: true", "\t\t\tGap: 10"},
                item("I1", "Width: 300"), item("I2", "Width: 300"),
                item("I3", "Width: 300"), item("I4", "Width: 300")));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 90, l.rect(1, "F"));
        assertRect(40, 40, 300, 40, l.rect(1, "I1"));
        assertRect(40, 90, 300, 40, l.rect(1, "I2"));
        assertRect(40, 40, 515, 90, l.rect(2, "F"));
        assertRect(40, 40, 300, 40, l.rect(2, "I3"));
        assertRect(40, 90, 300, 40, l.rect(2, "I4"));
    }

    @Test
    void columnBreaksBetweenItems() {
        LayoutFixture l = layOutPaged(PAGE, concat(
                new String[]{"\tBox:", "\t\tId: F", "\t\tLayout: flex", "\t\tFlex:", "\t\t\tDirection: column"},
                item("A"), item("B"), item("C")));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 80, l.rect(1, "F"));
        assertRect(40, 80, 515, 40, l.rect(1, "B"));
        assertRect(40, 40, 515, 40, l.rect(2, "F"));
        assertRect(40, 40, 515, 40, l.rect(2, "C"));
    }

    @Test
    void columnRespectsAvoidBetweenItems() {
        // Cortar antes de C incumple su avoid: se corta antes de B.
        LayoutFixture l = layOutPaged(PAGE, concat(
                new String[]{"\tBox:", "\t\tId: F", "\t\tLayout: flex", "\t\tFlex:", "\t\t\tDirection: column"},
                item("A"), item("B"), item("C", "Break before: avoid")));
        assertEquals(2, l.pages());
        assertFalse(l.has(1, "B"));
        assertRect(40, 40, 515, 40, l.rect(2, "B"));
        assertRect(40, 80, 515, 40, l.rect(2, "C"));
    }

    @Test
    void rowLineTallerThanAPageBreaksItsItemsInParallel() {
        // A: 20 líneas de 12 (240). B se estira a la línea (240) y reparte ese alto entre las
        // páginas. Líneas de A: 8 + 8 + 4; B: 100 + 100 + 40. La página 3 mide lo que A.
        String text = String.join(" ", java.util.Collections.nCopies(20, "aaaaa"));
        LayoutFixture l = layOutPaged(PAGE,
                "\tBox:", "\t\tId: F", "\t\tLayout: flex",
                "\t\tBox:", "\t\t\tId: A", "\t\t\tWidth: 30", "\t\t\tText: " + text,
                "\t\tBox:", "\t\t\tId: B", "\t\t\tWidth: 100");
        assertEquals(3, l.pages());
        assertRect(40, 40, 30, 96, l.rect(1, "A"));
        assertRect(70, 40, 100, 100, l.rect(1, "B"));
        assertRect(40, 40, 515, 100, l.rect(2, "F"));
        assertRect(70, 40, 100, 100, l.rect(2, "B"));
        assertRect(40, 40, 30, 48, l.rect(3, "A"));
        assertRect(70, 40, 100, 40, l.rect(3, "B"));
        assertRect(40, 40, 515, 48, l.rect(3, "F"));
    }

    @Test
    void columnItemTallerThanAPageIsBroken() {
        LayoutFixture l = layOutPaged(PAGE, concat(
                new String[]{"\tBox:", "\t\tId: F", "\t\tLayout: flex", "\t\tFlex:", "\t\t\tDirection: column"},
                new String[]{"\t\tBox:", "\t\t\tId: A", "\t\t\tHeight: 150"}, item("B")));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "A"));
        assertRect(40, 40, 515, 50, l.rect(2, "A"));
        assertRect(40, 90, 515, 40, l.rect(2, "B"));
        assertRect(40, 40, 515, 90, l.rect(2, "F"));
    }

    @Test
    void containerThatDoesNotFitMovesToTheNextPage() {
        // Tras X (70) no cabe ni la primera línea: el contenedor entero pasa a la página 2.
        LayoutFixture l = layOutPaged(PAGE, concat(
                new String[]{"\tBox:", "\t\tId: X", "\t\tHeight: 70"},
                new String[]{"\tBox:", "\t\tId: F", "\t\tLayout: flex"},
                item("A")));
        assertEquals(2, l.pages());
        assertFalse(l.has(1, "F"));
        assertRect(40, 40, 515, 40, l.rect(2, "F"));
    }
}
