package dev.stxt.boxes.layout.table;

import dev.stxt.boxes.layout.LayoutFixture;
import dev.stxt.boxes.model.Rect;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static dev.stxt.boxes.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Cabecera y pie repetidos (ESPECIFICACION.md §5.15). Páginas de 180 pt con márgenes de 40: área
 * útil de 515 × 100 desde (40, 40). Las tablas tienen una columna y celdas de alto fijo.
 */
class TableFragmentationTest {

    private static final double PAGE = 180;

    private static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(), new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height())).toString());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private static String[] table(String gap) {
        return gap == null
                ? new String[]{"\tTable:", "\t\tId: T", "\t\tColumns: 1fr"}
                : new String[]{"\tTable:", "\t\tId: T", "\t\tColumns: 1fr", "\t\tGap: " + gap};
    }

    private static String[] group(String name) {
        return new String[]{"\t\t" + name + ":"};
    }

    private static String[] row(String id, int height, String... rowLines) {
        String[] head = {"\t\t\tRow:"};
        String[] cell = {"\t\t\t\tCell:", "\t\t\t\t\tId: " + id, "\t\t\t\t\tBox:", "\t\t\t\t\t\tHeight: " + height};
        return concat(head, rowLines, cell);
    }

    private static String[] concat(String[]... parts) {
        return Arrays.stream(parts).flatMap(Arrays::stream).toArray(String[]::new);
    }

    @Test
    void headAndFootRepeatOnEveryPage() {
        // La cabecera (20) y el pie (10) dejan 70 al cuerpo: dos filas de 30 por página. El pie va
        // detrás de la última fila colocada.
        LayoutFixture l = layOutPaged(PAGE, concat(table(null),
                group("Head"), row("H", 20),
                group("Body"), row("B1", 30), row("B2", 30), row("B3", 30), row("B4", 30),
                group("Foot"), row("F", 10)));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 90, l.rect(1, "T"));
        assertRect(40, 40, 515, 20, l.rect(1, "H"));
        assertRect(40, 90, 515, 30, l.rect(1, "B2"));
        assertRect(40, 120, 515, 10, l.rect(1, "F"));
        assertFalse(l.has(1, "B3"));
        assertRect(40, 40, 515, 90, l.rect(2, "T"));
        assertRect(40, 40, 515, 20, l.rect(2, "H"));
        assertRect(40, 60, 515, 30, l.rect(2, "B3"));
        assertRect(40, 120, 515, 10, l.rect(2, "F"));
    }

    @Test
    void gapsSeparateHeadAndFootFromTheBody() {
        // Con 5 de hueco, la cabecera ocupa 25 y el pie 15: quedan 60, y B1 (30) y B2 (que acaba
        // en 65) no caben juntas.
        LayoutFixture l = layOutPaged(PAGE, concat(table("5"),
                group("Head"), row("H", 20),
                group("Body"), row("B1", 30), row("B2", 30),
                group("Foot"), row("F", 10)));
        assertEquals(2, l.pages());
        assertRect(40, 65, 515, 30, l.rect(1, "B1"));
        assertRect(40, 100, 515, 10, l.rect(1, "F"));
        assertRect(40, 40, 515, 70, l.rect(1, "T"));
        assertRect(40, 65, 515, 30, l.rect(2, "B2"));
        assertRect(40, 100, 515, 10, l.rect(2, "F"));
    }

    @Test
    void tableThatDoesNotFitMovesWithItsHead() {
        // Tras X (50), la cabecera, la primera fila y el pie (60) no caben: la tabla entera pasa
        // a la página 2, sin dejar la cabecera sola.
        LayoutFixture l = layOutPaged(PAGE, concat(
                new String[]{"\tBlock:", "\t\tId: X", "\t\tBox:", "\t\t\tHeight: 50"}, table(null),
                group("Head"), row("H", 20), group("Body"), row("B1", 30), group("Foot"), row("F", 10)));
        assertEquals(2, l.pages());
        assertFalse(l.has(1, "T"));
        assertRect(40, 40, 515, 20, l.rect(2, "H"));
        assertRect(40, 60, 515, 30, l.rect(2, "B1"));
    }

    @Test
    void tallBodyRowBreaksBetweenHeadAndFoot() {
        // B1 (100) no cabe en los 70 que dejan la cabecera y el pie: se parte, 70 + 30, y la
        // cabecera y el pie se repiten alrededor de cada parte.
        LayoutFixture l = layOutPaged(PAGE, concat(table(null),
                group("Head"), row("H", 20), group("Body"), row("B1", 100), group("Foot"), row("F", 10)));
        assertEquals(2, l.pages());
        assertRect(40, 60, 515, 70, l.rect(1, "B1"));
        assertRect(40, 130, 515, 10, l.rect(1, "F"));
        assertRect(40, 40, 515, 20, l.rect(2, "H"));
        assertRect(40, 60, 515, 30, l.rect(2, "B1"));
        assertRect(40, 90, 515, 10, l.rect(2, "F"));
    }

    @Test
    void rowBreakForcesAPageWithTheHeadRepeated() {
        LayoutFixture l = layOutPaged(PAGE, concat(table(null),
                group("Head"), row("H", 20),
                group("Body"), row("B1", 30), row("B2", 30, "\t\t\t\tBreak:", "\t\t\t\t\tBefore: page")));
        assertEquals(2, l.pages());
        assertFalse(l.has(1, "B2"));
        assertRect(40, 40, 515, 50, l.rect(1, "T"));
        assertRect(40, 40, 515, 20, l.rect(2, "H"));
        assertRect(40, 60, 515, 30, l.rect(2, "B2"));
    }

    @Test
    void withoutHeadAndFootItBreaksLikeAGrid() {
        LayoutFixture l = layOutPaged(PAGE, concat(table(null),
                group("Body"), row("B1", 40), row("B2", 40), row("B3", 40)));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 80, l.rect(1, "T"));
        assertRect(40, 40, 515, 40, l.rect(2, "B3"));
    }
}
