package dev.stxt.pdf.layout.table;

import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.Rect;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Cabecera y pie repetidos (referencia/componentes/table.md). Páginas de 180 pt con márgenes de 40: área
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
                ? new String[]{"\tTable:", "\t\tId: T", "\t\tColumn sizes: *"}
                : new String[]{"\tTable:", "\t\tId: T", "\t\tColumn sizes: *", "\t\tGap: " + gap};
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
    void headerAndFooterRowsRepeatOnEveryPage() {
        // La cabecera (20) y el pie (10) dejan 70 al cuerpo: dos filas de 30 por página. El pie va
        // detrás de la última fila colocada.
        LayoutFixture l = layOutPaged(PAGE, concat(table(null),
                group("Header rows"), row("H", 20),
                group("Body"), row("B1", 30), row("B2", 30), row("B3", 30), row("B4", 30),
                group("Footer rows"), row("F", 10)));
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

    /**
     * La franja de una fila con {@code Id} o {@code Box} (referencia/componentes/table.md#filas) va de la primera
     * columna a la última, con el hueco entre ellas, y tiene el alto de su fila; el hueco entre filas no es de
     * ninguna.
     */
    @Test
    void theBandOfARowSpansItsColumnsAndItsHeight() {
        LayoutFixture l = LayoutFixture.layOut("\tTable:", "\t\tId: T", "\t\tColumn sizes: 100 100", "\t\tGap: 10",
                "\t\tBody:",
                "\t\t\tRow:", "\t\t\t\tId: R1", "\t\t\t\tCell:", "\t\t\t\t\tBox:", "\t\t\t\t\t\tHeight: 20",
                "\t\t\t\tCell:",
                "\t\t\tRow:", "\t\t\t\tBox:", "\t\t\t\t\tBackground: #EEEEEE", "\t\t\t\tId: R2",
                "\t\t\t\tCell:", "\t\t\t\t\tId: C", "\t\t\t\t\tBox:", "\t\t\t\t\t\tHeight: 30",
                "\t\t\t\tCell:");
        assertRect(40, 40, 210, 20, l.rect("R1"));
        assertRect(40, 70, 210, 30, l.rect("R2"));
        assertRect(40, 70, 100, 30, l.rect("C"));
        // Va delante de sus celdas en el fragmento de la tabla, para pintarse debajo.
        assertEquals("R2", firstChildWithId(l, "R2", "C"));
    }

    /** El primero de los dos Id que sale entre los hijos del fragmento de la tabla. */
    private static String firstChildWithId(LayoutFixture l, String a, String b) {
        return l.fragment(1, "T").children().stream().map(f -> f.box().id())
                .filter(id -> a.equals(id) || b.equals(id)).findFirst().orElseThrow();
    }

    /** Las franjas de la cabecera y del pie se repiten en cada página, como sus celdas. */
    @Test
    void bandsOfHeaderAndFooterRowsRepeat() {
        LayoutFixture l = layOutPaged(PAGE, concat(table(null),
                group("Header rows"), row("H", 20, "\t\t\t\tId: HR"),
                group("Body"), row("B1", 30), row("B2", 30, "\t\t\t\tId: R2"), row("B3", 30, "\t\t\t\tId: R3"),
                row("B4", 30),
                group("Footer rows"), row("F", 10, "\t\t\t\tId: FR")));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 20, l.rect(1, "HR"));
        assertRect(40, 90, 515, 30, l.rect(1, "R2"));
        assertRect(40, 120, 515, 10, l.rect(1, "FR"));
        assertFalse(l.has(1, "R3"));
        assertRect(40, 40, 515, 20, l.rect(2, "HR"));
        assertRect(40, 60, 515, 30, l.rect(2, "R3"));
        assertRect(40, 120, 515, 10, l.rect(2, "FR"));
    }

    /**
     * Una fila que no cabe en una página se parte, y su franja con ella: hasta el final de su celda en la primera, que
     * llega al final del área útil, y desde arriba en la siguiente, sin el borde del corte (con {@code Edges:
     * slice}).
     */
    @Test
    void theBandOfASplitRowIsSplitToo() {
        LayoutFixture l = layOutPaged(PAGE, concat(table(null), group("Body"),
                new String[]{"\t\t\tRow:", "\t\t\t\tId: R", "\t\t\t\tBox:", "\t\t\t\t\tBorder: 1",
                        "\t\t\t\tCell:", "\t\t\t\t\tBlock:", "\t\t\t\t\t\tBox:", "\t\t\t\t\t\t\tHeight: 60",
                        "\t\t\t\t\tBlock:", "\t\t\t\t\t\tBox:", "\t\t\t\t\t\t\tHeight: 60"}));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "R"));
        assertEquals(0, l.fragment(1, "R").border().bottom());
        assertEquals(1, l.fragment(1, "R").border().top());
        // La celda pone 60 + 40 en la primera página y los 20 que faltan en la segunda.
        assertRect(40, 40, 515, 20, l.rect(2, "R"));
        assertEquals(0, l.fragment(2, "R").border().top());
        assertEquals(1, l.fragment(2, "R").border().bottom());
    }

    @Test
    void gapsSeparateHeaderAndFooterRowsFromTheBody() {
        // Con 5 de hueco, la cabecera ocupa 25 y el pie 15: quedan 60, y B1 (30) y B2 (que acaba
        // en 65) no caben juntas.
        LayoutFixture l = layOutPaged(PAGE, concat(table("5"),
                group("Header rows"), row("H", 20),
                group("Body"), row("B1", 30), row("B2", 30),
                group("Footer rows"), row("F", 10)));
        assertEquals(2, l.pages());
        assertRect(40, 65, 515, 30, l.rect(1, "B1"));
        assertRect(40, 100, 515, 10, l.rect(1, "F"));
        assertRect(40, 40, 515, 70, l.rect(1, "T"));
        assertRect(40, 65, 515, 30, l.rect(2, "B2"));
        assertRect(40, 100, 515, 10, l.rect(2, "F"));
    }

    @Test
    void tableThatDoesNotFitMovesWithItsHeaderRows() {
        // Tras X (50), la cabecera, la primera fila y el pie (60) no caben: la tabla entera pasa
        // a la página 2, sin dejar la cabecera sola.
        LayoutFixture l = layOutPaged(PAGE, concat(
                new String[]{"\tBlock:", "\t\tId: X", "\t\tBox:", "\t\t\tHeight: 50"}, table(null),
                group("Header rows"), row("H", 20), group("Body"), row("B1", 30), group("Footer rows"), row("F", 10)));
        assertEquals(2, l.pages());
        assertFalse(l.has(1, "T"));
        assertRect(40, 40, 515, 20, l.rect(2, "H"));
        assertRect(40, 60, 515, 30, l.rect(2, "B1"));
    }

    @Test
    void tallBodyRowBreaksBetweenHeaderAndFooterRows() {
        // B1 (100) no cabe en los 70 que dejan la cabecera y el pie: se parte, 70 + 30, y la
        // cabecera y el pie se repiten alrededor de cada parte.
        LayoutFixture l = layOutPaged(PAGE, concat(table(null),
                group("Header rows"), row("H", 20), group("Body"), row("B1", 100), group("Footer rows"), row("F", 10)));
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
                group("Header rows"), row("H", 20),
                group("Body"), row("B1", 30), row("B2", 30, "\t\t\t\tBreak:", "\t\t\t\t\tBefore: page")));
        assertEquals(2, l.pages());
        assertFalse(l.has(1, "B2"));
        assertRect(40, 40, 515, 50, l.rect(1, "T"));
        assertRect(40, 40, 515, 20, l.rect(2, "H"));
        assertRect(40, 60, 515, 30, l.rect(2, "B2"));
    }

    @Test
    void withoutHeaderAndFooterRowsItBreaksLikeAGrid() {
        LayoutFixture l = layOutPaged(PAGE, concat(table(null),
                group("Body"), row("B1", 40), row("B2", 40), row("B3", 40)));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 80, l.rect(1, "T"));
        assertRect(40, 40, 515, 40, l.rect(2, "B3"));
    }
}
