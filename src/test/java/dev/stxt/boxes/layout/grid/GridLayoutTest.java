package dev.stxt.boxes.layout.grid;

import dev.stxt.boxes.layout.LayoutFixture;
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
                "\t\tCell:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tHeight: 10",
                "\t\tCell:", "\t\t\tText: aaaa bbbb", "\t\t\t\tId: B",
                "\t\tCell:", "\t\t\tBlock:", "\t\t\t\tId: C", "\t\t\t\tHeight: 10");
        assertRect(40, 40, 100, 10, l.rect("A"));
        assertRect(140, 40, 45, 12, l.rect("B"));
        assertRect(185, 40, 370, 10, l.rect("C"));
    }

    @Test
    void alignmentInTheArea() {
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tColumns: 200 200", "\t\tRows: 100",
                "\t\tCell:", "\t\t\tBlock:", "\t\t\t\tId: S",
                "\t\tCell:", "\t\t\tHorizontal: end", "\t\t\tVertical: center",
                "\t\t\tBlock:", "\t\t\t\tId: E", "\t\t\t\tWidth: 50", "\t\t\t\tHeight: 20",
                "\tGrid:", "\t\tColumns: 200", "\t\tHorizontal: start",
                "\t\tCell:", "\t\t\tText: aaa", "\t\t\t\tId: T");
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
                "\t\tCell: 1 2 1 2", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tHeight: 10",
                "\t\tCell:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tHeight: 10",
                "\t\tCell:", "\t\t\tBlock:", "\t\t\t\tId: C", "\t\t\t\tHeight: 10");
        assertRect(140, 40, 200, 10, l.rect("A"));
        assertRect(40, 40, 100, 10, l.rect("B"));
        assertRect(40, 50, 100, 10, l.rect("C"));
    }

    @Test
    void flowColumn() {
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tColumns: 100 100", "\t\tRows: 20 20", "\t\tFlow: column",
                "\t\tCell:", "\t\t\tBlock:", "\t\t\t\tId: A",
                "\t\tCell:", "\t\t\tBlock:", "\t\t\t\tId: B",
                "\t\tCell:", "\t\t\tBlock:", "\t\t\t\tId: C");
        assertRect(40, 40, 100, 20, l.rect("A"));
        assertRect(40, 60, 100, 20, l.rect("B"));
        assertRect(140, 40, 100, 20, l.rect("C"));
    }

    @Test
    void fractionRowsWithAutoHeightMeasureTheTallest() {
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tId: G", "\t\tRows: 1fr 1fr",
                "\t\tCell:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tHeight: 10",
                "\t\tCell:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tHeight: 30");
        assertRect(40, 40, 515, 60, l.rect("G"));
        assertRect(40, 40, 515, 10, l.rect("A"));
        assertRect(40, 70, 515, 30, l.rect("B"));
    }

    @Test
    void fractionRowsWithDefiniteHeight() {
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tHeight: 100", "\t\tRows: 1fr 3fr",
                "\t\tCell:", "\t\t\tBlock:", "\t\t\t\tId: A",
                "\t\tCell:", "\t\t\tBlock:", "\t\t\t\tId: B");
        assertRect(40, 40, 515, 25, l.rect("A"));
        assertRect(40, 65, 515, 75, l.rect("B"));
    }

    @Test
    void autoRowsStretchInADefiniteHeight() {
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tHeight: 100",
                "\t\tCell:", "\t\t\tBlock:", "\t\t\t\tId: A",
                "\t\tCell:", "\t\t\tBlock:", "\t\t\t\tId: B");
        assertRect(40, 40, 515, 50, l.rect("A"));
        assertRect(40, 90, 515, 50, l.rect("B"));
    }

    @Test
    void percentagesUseTheArea() {
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tColumns: 200 300", "\t\tRows: 100",
                "\t\tCell:", "\t\t\tBlock:", "\t\t\t\tHeight: 10",
                "\t\tCell:", "\t\t\tBlock:", "\t\t\t\tId: P", "\t\t\t\tWidth: 50%", "\t\t\t\tHeight: 50%");
        assertRect(240, 40, 150, 50, l.rect("P"));
    }

    @Test
    void percentageRowsOfADefiniteHeight() {
        // Con Height: auto en el contenedor, las filas en porcentaje son un error (catálogo de errores).
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tId: G", "\t\tHeight: 100", "\t\tRows: 50% 50%",
                "\t\tCell:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tHeight: 10",
                "\t\tCell:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tHeight: 30");
        assertRect(40, 40, 515, 100, l.rect("G"));
        assertRect(40, 40, 515, 10, l.rect("A"));
        assertRect(40, 90, 515, 30, l.rect("B"));
    }

    @Test
    void percentageColumnsInIntrinsicWidthsAreAuto() {
        // A: min 10, max 25. B: min y max 15. Sin containing block, 50% cuenta como auto. Con
        // min-content (25) cada columna mediría 12.5 y B desbordaría: es un error.
        String items = "\t\tCell:\n\t\t\tText: aa aa\n\t\tCell:\n\t\t\tText: bbb";
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tId: max", "\t\tWidth: max-content", "\t\tColumns: 50% 50%", items);
        assertEquals(40, l.rect("max").width(), 1e-9);
    }

    // Un ítem que cruza fracciones y no cabe en el contenedor (las columnas no bajan de su
    // min-content) es un desbordamiento: está en el catálogo de errores.

    @Test
    void itemCrossingFractionsInIntrinsicWidths() {
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tId: G", "\t\tWidth: min-content", "\t\tColumns: 1fr 1fr",
                "\t\tCell: 1 1 1 2", "\t\t\tText: " + "a".repeat(60));
        assertEquals(300, l.rect("G").width(), 1e-9);
    }

    @Test
    void intrinsicWidths() {
        // A: min 10, max 25. B: min y max 15. Hueco 10.
        String items = "\t\tCell:\n\t\t\tText: aa aa\n\t\tCell:\n\t\t\tText: bbb";
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tId: max", "\t\tWidth: max-content", "\t\tColumns: auto auto", "\t\tGap: 10", items,
                "\tGrid:", "\t\tId: min", "\t\tWidth: min-content", "\t\tColumns: auto auto", "\t\tGap: 10", items,
                "\tGrid:", "\t\tId: fr", "\t\tWidth: max-content", "\t\tColumns: 150 1fr",
                "\t\tCell:", "\t\t\tBlock:", "\t\t\t\tHeight: 10",
                "\t\tCell:", "\t\t\tText: aaa");
        assertEquals(50, l.rect("max").width(), 1e-9);
        assertEquals(35, l.rect("min").width(), 1e-9);
        assertEquals(165, l.rect("fr").width(), 1e-9);
    }

    @Test
    void positionedChildren() {
        LayoutFixture l = layOut(
                // Un grid posicionado: Relative y, dentro, el Grid con el alto.
                "\tRelative:",
                "\t\tGrid:", "\t\t\tHeight: 50", "\t\t\tColumns: 100 100",
                "\t\t\tCell:", "\t\t\t\tRelative:", "\t\t\t\t\tId: R", "\t\t\t\t\tOffset: 3 auto auto 4",
                "\t\t\tAbsolute:", "\t\t\t\tId: A", "\t\t\t\tOffset: auto 0 0 auto", "\t\t\t\tWidth: 10",
                "\t\t\t\tHeight: 10",
                "\t\t\tCell:", "\t\t\t\tBlock:", "\t\t\t\t\tId: B");
        assertRect(44, 43, 100, 50, l.rect("R"));
        assertRect(140, 40, 100, 50, l.rect("B"));
        assertRect(545, 80, 10, 10, l.rect("A"));
    }
}
