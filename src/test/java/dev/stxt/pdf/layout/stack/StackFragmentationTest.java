package dev.stxt.pdf.layout.stack;

import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.Rect;
import org.junit.jupiter.api.Test;

import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Páginas de 180 pt con márgenes de 40: área útil de 515 × 100 desde (40, 40). */
class StackFragmentationTest {

    private static final double PAGE = 180;

    private static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(), new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height())).toString());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    /** Ítem de 40 de alto; {@code box} son más propiedades de su {@code Box} ({@code "Width: 300"}). */
    private static String[] item(String id, String... box) {
        String[] base = {"\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: " + id, "\t\t\t\tBox:", "\t\t\t\t\tHeight: 40"};
        String[] all = new String[base.length + box.length];
        System.arraycopy(base, 0, all, 0, base.length);
        for (int i = 0; i < box.length; i++) {
            all[base.length + i] = "\t\t\t\t\t" + box[i];
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
                new String[]{"\tStack:", "\t\tId: F", "\t\tWrap: true", "\t\tGap: 10"},
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
                new String[]{"\tStack:", "\t\tId: F", "\t\tDirection: vertical"},
                item("A"), item("B"), item("C")));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 80, l.rect(1, "F"));
        assertRect(40, 80, 515, 40, l.rect(1, "B"));
        assertRect(40, 40, 515, 40, l.rect(2, "F"));
        assertRect(40, 40, 515, 40, l.rect(2, "C"));
    }

    @Test
    void columnRespectsKeepsBetweenItems() {
        // Cortar antes de C incumple su Keep with previous: se corta antes de B.
        LayoutFixture l = layOutPaged(PAGE, concat(
                new String[]{"\tStack:", "\t\tId: F", "\t\tDirection: vertical"},
                item("A"), item("B"), item("C"), new String[]{"\t\t\t\tBreak:", "\t\t\t\t\tKeep with previous: true"}));
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
                "\tStack:", "\t\tId: F",
                "\t\tItem:", "\t\t\tText: " + text, "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 30",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 100");
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
                new String[]{"\tStack:", "\t\tId: F", "\t\tDirection: vertical"},
                new String[]{"\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 150"}, item("B")));
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
                new String[]{"\tBlock:", "\t\tId: X", "\t\tBox:", "\t\t\tHeight: 70"},
                new String[]{"\tStack:", "\t\tId: F"},
                item("A")));
        assertEquals(2, l.pages());
        assertFalse(l.has(1, "F"));
        assertRect(40, 40, 515, 40, l.rect(2, "F"));
    }

    /**
     * Una columna con Wrap se corta entera (es una sola unidad). Tras X (50), F (Height: 60, líneas de 40) cabe
     * con sus ítems en la página 1 y su alto sigue en la 2.
     */
    @Test
    void aWrappedColumnIsASingleUnit() {
        LayoutFixture l = layOutPaged(PAGE, concat(
                new String[]{"\tBlock:", "\t\tBox:", "\t\t\tHeight: 50"},
                new String[]{"\tStack:", "\t\tId: F", "\t\tBox:", "\t\t\tHeight: 60", "\t\tDirection: vertical",
                        "\t\tWrap: true", "\t\tLine distribution: start"},
                item("A", "Width: 50"), item("B", "Width: 50")));
        assertRect(40, 90, 50, 40, l.rect(1, "A"));
        assertRect(90, 90, 50, 40, l.rect(1, "B"));
    }
}
