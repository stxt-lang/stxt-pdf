package dev.stxt.boxes.layout.grid;

import dev.stxt.boxes.layout.LayoutFixture;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Rect;
import org.junit.jupiter.api.Test;

import static dev.stxt.boxes.layout.LayoutFixture.layOut;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GridLayoutTest {

    private static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(), new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height())).toString());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    @Test
    void columnsAndRowsSizing() {
        // auto llega al max-content de B (45) y la fracción recibe el resto: 515 - 100 - 45.
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tColumns: 100 auto 1fr",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10",
                "\t\tArea:", "\t\t\tText: aaaa bbbb", "\t\t\t\tId: B",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: C", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10");
        assertRect(40, 40, 100, 10, l.rect("A"));
        assertRect(140, 40, 45, 12, l.rect("B"));
        assertRect(185, 40, 370, 10, l.rect("C"));
    }

    @Test
    void tableCellsAreGridItems() {
        // Columnas 100 y 1fr con 5 de hueco: 515 - 100 - 5 = 410. La fila del cuerpo mide lo que
        // la celda con relleno (12 + 8) y estira a la otra.
        LayoutFixture l = layOut(
                "\tTable:", "\t\tColumns: 100 1fr", "\t\tGap: 0 5",
                "\t\tHead:", "\t\t\tRow:", "\t\t\t\tCell: aaa", "\t\t\t\t\tId: H1", "\t\t\t\tCell: bbb", "\t\t\t\t\tId: H2",
                "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: ccc", "\t\t\t\t\tId: B1", "\t\t\t\t\tBox:",
                "\t\t\t\t\t\tPadding: 4", "\t\t\t\tCell:", "\t\t\t\t\tId: B2");
        assertRect(40, 40, 100, 12, l.rect("H1"));
        assertRect(145, 40, 410, 12, l.rect("H2"));
        assertRect(40, 52, 100, 20, l.rect("B1"));
        assertRect(145, 52, 410, 20, l.rect("B2"));
    }

    @Test
    void alignmentInTheArea() {
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tColumns: 200 200", "\t\tRows: 100",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: S",
                "\t\tArea:", "\t\t\tHorizontal: end", "\t\t\tVertical: center",
                "\t\t\tBlock:", "\t\t\t\tId: E", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 50", "\t\t\t\t\tHeight: 20",
                "\tGrid:", "\t\tColumns: 200", "\t\tHorizontal: start",
                "\t\tArea:", "\t\t\tText: aaa", "\t\t\t\tId: T");
        assertRect(40, 40, 200, 100, l.rect("S"));
        assertRect(390, 80, 50, 20, l.rect("E"));
        // Sin stretch, el ítem conserva su ajuste al contenido.
        assertRect(40, 140, 15, 12, l.rect("T"));
    }

    @Test
    void explicitCellsAndAutoPlacement() {
        // A ocupa las columnas 2 y 3 de la fila 1; C no cabe detrás de B y pasa a la fila 2.
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tColumns: 100 100 100",
                "\t\tArea: 1 2 1 2", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: C", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10");
        assertRect(140, 40, 200, 10, l.rect("A"));
        assertRect(40, 40, 100, 10, l.rect("B"));
        assertRect(40, 50, 100, 10, l.rect("C"));
    }

    @Test
    void flowColumn() {
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tColumns: 100 100", "\t\tRows: 20 20", "\t\tFlow: column", "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: C");
        assertRect(40, 40, 100, 20, l.rect("A"));
        assertRect(40, 60, 100, 20, l.rect("B"));
        assertRect(140, 40, 100, 20, l.rect("C"));
    }

    @Test
    void fractionRowsWithAutoHeightMeasureTheTallest() {
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tId: G", "\t\tRows: 1fr 1fr",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 30");
        assertRect(40, 40, 515, 60, l.rect("G"));
        assertRect(40, 40, 515, 10, l.rect("A"));
        assertRect(40, 70, 515, 30, l.rect("B"));
    }

    @Test
    void fractionRowsWithDefiniteHeight() {
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tBox:", "\t\t\tHeight: 100", "\t\tRows: 1fr 3fr",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: A",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: B");
        assertRect(40, 40, 515, 25, l.rect("A"));
        assertRect(40, 65, 515, 75, l.rect("B"));
    }

    @Test
    void autoRowsStretchInADefiniteHeight() {
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tBox:", "\t\t\tHeight: 100",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: A",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: B");
        assertRect(40, 40, 515, 50, l.rect("A"));
        assertRect(40, 90, 515, 50, l.rect("B"));
    }

    @Test
    void percentagesUseTheArea() {
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tColumns: 200 300", "\t\tRows: 100",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: P", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 50%",
                "\t\t\t\t\tHeight: 50%");
        assertRect(240, 40, 150, 50, l.rect("P"));
    }

    @Test
    void percentageRowsOfADefiniteHeight() {
        // Con Height: auto en el contenedor, las filas en porcentaje son un error (catálogo de errores).
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tId: G", "\t\tBox:", "\t\t\tHeight: 100", "\t\tRows: 50% 50%",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 30");
        assertRect(40, 40, 515, 100, l.rect("G"));
        assertRect(40, 40, 515, 10, l.rect("A"));
        assertRect(40, 90, 515, 30, l.rect("B"));
    }

    @Test
    void percentageColumnsInIntrinsicWidthsAreAuto() {
        // A: min 10, max 25. B: min y max 15. Sin containing block, 50% cuenta como auto. Con
        // min-content (25) cada columna mediría 12.5 y B desbordaría: es un error.
        String items = "\t\tArea:\n\t\t\tText: aa aa\n\t\tArea:\n\t\t\tText: bbb";
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tId: max", "\t\tBox:", "\t\t\tWidth: max-content", "\t\tColumns: 50% 50%", items);
        assertEquals(40, l.rect("max").width(), 1e-9);
    }

    // Un ítem que cruza fracciones y no cabe en el contenedor (las columnas no bajan de su
    // min-content) es un desbordamiento: está en el catálogo de errores.

    @Test
    void itemCrossingFractionsInIntrinsicWidths() {
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tId: G", "\t\tBox:", "\t\t\tWidth: min-content", "\t\tColumns: 1fr 1fr",
                "\t\tArea: 1 1 1 2", "\t\t\tText: " + "a".repeat(60));
        assertEquals(300, l.rect("G").width(), 1e-9);
    }

    @Test
    void intrinsicWidths() {
        // A: min 10, max 25. B: min y max 15. Hueco 10.
        String items = "\t\tArea:\n\t\t\tText: aa aa\n\t\tArea:\n\t\t\tText: bbb";
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tId: max", "\t\tBox:", "\t\t\tWidth: max-content", "\t\tColumns: auto auto",
                "\t\tGap: 10", items,
                "\tGrid:", "\t\tId: min", "\t\tBox:", "\t\t\tWidth: min-content", "\t\tColumns: auto auto",
                "\t\tGap: 10", items,
                "\tGrid:", "\t\tId: fr", "\t\tBox:", "\t\t\tWidth: max-content", "\t\tColumns: 150 1fr",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10",
                "\t\tArea:", "\t\t\tText: aaa");
        assertEquals(50, l.rect("max").width(), 1e-9);
        assertEquals(35, l.rect("min").width(), 1e-9);
        assertEquals(165, l.rect("fr").width(), 1e-9);
    }

    @Test
    void positionedChildren() {
        LayoutFixture l = layOut(
                // Un grid posicionado: Relative y, dentro, el Grid con el alto.
                "\tRelative:", "\t\tGrid:", "\t\t\tBox:", "\t\t\t\tHeight: 50", "\t\t\tColumns: 100 100",
                "\t\t\tArea:", "\t\t\t\tRelative:", "\t\t\t\t\tId: R", "\t\t\t\t\tOffset: 3 auto auto 4",
                "\t\t\tAbsolute:", "\t\t\t\tId: A", "\t\t\t\tOffset: auto 0 0 auto", "\t\t\t\tBox:",
                "\t\t\t\t\tWidth: 10", "\t\t\t\t\tHeight: 10",
                "\t\t\tArea:", "\t\t\t\tBlock:", "\t\t\t\t\tId: B");
        assertRect(44, 43, 100, 50, l.rect("R"));
        assertRect(140, 40, 100, 50, l.rect("B"));
        assertRect(545, 80, 10, 10, l.rect("A"));
    }

    @Test
    void itemMarginsUseTheAreaWidth() {
        // El 10 % es del área (200), no del contenedor (515): 20 por lado. El fragmento lleva el
        // margen usado, que es el que comprueba Overflow.
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tColumns: 200 1fr",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: M", "\t\t\t\tBox:", "\t\t\t\t\tMargin: 10%",
                "\t\t\t\t\tHeight: 10");
        assertRect(60, 60, 160, 10, l.rect("M"));
        assertEquals(Insets.all(20), l.fragment("M").margin());
    }

    // Casos que faltaban según las pruebas de mutación (ROADMAP-QA-ARQ.md, QA-08): ítems con márgenes y tamaños
    // intrínsecos con más de dos columnas.

    @Test
    void alignmentAndStretchKeepTheItemMargins() {
        // Áreas de 200 × 100. E (50 × 20, margen 10) al final y centrado: x = 40 + 200 − 10 − 50, y = 40 + 30 + 10.
        // S, sin tamaño, se estira dentro de sus márgenes: 180 × 80 desde (250, 50).
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tColumns: 200 200", "\t\tRows: 100",
                "\t\tArea:", "\t\t\tHorizontal: end", "\t\t\tVertical: center",
                "\t\t\tBlock:", "\t\t\t\tId: E", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 50", "\t\t\t\t\tHeight: 20",
                "\t\t\t\t\tMargin: 10",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: S", "\t\t\t\tBox:", "\t\t\t\t\tMargin: 10");
        assertRect(180, 80, 50, 20, l.rect("E"));
        assertRect(250, 50, 180, 80, l.rect("S"));
    }

    @Test
    void notStretchedItemsFitTheirContentInsideTheirMargins() {
        // Horizontal: start: el texto (15) en un área de 200 con 10 de margen a cada lado.
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tColumns: 200", "\t\tHorizontal: start",
                "\t\tArea:", "\t\t\tText: aaa", "\t\t\t\tId: T", "\t\t\t\tBox:", "\t\t\t\t\tMargin: 10");
        assertRect(50, 50, 15, 12, l.rect("T"));
    }

    @Test
    void intrinsicWidthsWithThreeColumns() {
        // 5 + 10 + 15 y dos huecos de 10.
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tId: G", "\t\tBox:", "\t\t\tWidth: max-content", "\t\tColumns: auto auto auto",
                "\t\tGap: 10", "\t\tArea:", "\t\t\tText: a", "\t\tArea:", "\t\t\tText: bb", "\t\tArea:", "\t\t\tText: ccc");
        assertEquals(50, l.rect("G").width(), 1e-9);
    }
}
