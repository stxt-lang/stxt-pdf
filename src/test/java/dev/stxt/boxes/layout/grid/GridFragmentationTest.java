package dev.stxt.boxes.layout.grid;

import dev.stxt.boxes.layout.LayoutFixture;
import dev.stxt.boxes.model.Rect;
import org.junit.jupiter.api.Test;

import static dev.stxt.boxes.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Páginas de 180 pt con márgenes de 40: área útil de 515 × 100 desde (40, 40). */
class GridFragmentationTest {

    private static final double PAGE = 180;

    private static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(), new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height())).toString());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private static final String[] GRID = {"\tBox:", "\t\tId: G", "\t\tLayout: grid", "\t\tGrid:", "\t\t\tColumns: 1fr 1fr"};

    private static String[] cell(String id, String cell) {
        return new String[]{"\t\tBox:", "\t\t\tId: " + id, "\t\t\tHeight: 40", "\t\t\tItem:", "\t\t\t\tCell: " + cell};
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
}
