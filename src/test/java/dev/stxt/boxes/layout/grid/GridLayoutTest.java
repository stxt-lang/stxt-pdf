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
                "\tBox:", "\t\tLayout: grid", "\t\tGrid:", "\t\t\tColumns: 100 auto 1fr",
                "\t\tBox:", "\t\t\tId: A", "\t\t\tHeight: 10",
                "\t\tBox:", "\t\t\tId: B", "\t\t\tText: aaaa bbbb",
                "\t\tBox:", "\t\t\tId: C", "\t\t\tHeight: 10");
        assertRect(40, 40, 100, 10, l.rect("A"));
        assertRect(140, 40, 45, 12, l.rect("B"));
        assertRect(185, 40, 370, 10, l.rect("C"));
    }

    @Test
    void alignmentInTheArea() {
        LayoutFixture l = layOut(
                "\tBox:", "\t\tLayout: grid", "\t\tGrid:", "\t\t\tColumns: 200 200", "\t\t\tRows: 100",
                "\t\tBox:", "\t\t\tId: S",
                "\t\tBox:", "\t\t\tId: E", "\t\t\tWidth: 50", "\t\t\tHeight: 20",
                "\t\t\tItem:", "\t\t\t\tHorizontal: end", "\t\t\t\tVertical: center",
                "\tBox:", "\t\tLayout: grid", "\t\tGrid:", "\t\t\tColumns: 200", "\t\t\tHorizontal: start",
                "\t\tBox:", "\t\t\tId: T", "\t\t\tText: aaa");
        assertRect(40, 40, 200, 100, l.rect("S"));
        assertRect(390, 80, 50, 20, l.rect("E"));
        // Sin stretch, el ítem conserva su ajuste al contenido.
        assertRect(40, 140, 15, 12, l.rect("T"));
    }

    @Test
    void explicitCellsAndAutoPlacement() {
        // A ocupa las columnas 2 y 3 de la fila 1; C no cabe detrás de B y pasa a la fila 2.
        LayoutFixture l = layOut(
                "\tBox:", "\t\tLayout: grid", "\t\tGrid:", "\t\t\tColumns: 100 100 100",
                "\t\tBox:", "\t\t\tId: A", "\t\t\tHeight: 10", "\t\t\tItem:", "\t\t\t\tCell: 1 2 1 2",
                "\t\tBox:", "\t\t\tId: B", "\t\t\tHeight: 10",
                "\t\tBox:", "\t\t\tId: C", "\t\t\tHeight: 10");
        assertRect(140, 40, 200, 10, l.rect("A"));
        assertRect(40, 40, 100, 10, l.rect("B"));
        assertRect(40, 50, 100, 10, l.rect("C"));
    }

    @Test
    void flowColumn() {
        LayoutFixture l = layOut(
                "\tBox:", "\t\tLayout: grid",
                "\t\tGrid:", "\t\t\tColumns: 100 100", "\t\t\tRows: 20 20", "\t\t\tFlow: column",
                "\t\tBox:", "\t\t\tId: A",
                "\t\tBox:", "\t\t\tId: B",
                "\t\tBox:", "\t\t\tId: C");
        assertRect(40, 40, 100, 20, l.rect("A"));
        assertRect(40, 60, 100, 20, l.rect("B"));
        assertRect(140, 40, 100, 20, l.rect("C"));
    }

    @Test
    void fractionRowsWithAutoHeightMeasureTheTallest() {
        LayoutFixture l = layOut(
                "\tBox:", "\t\tId: G", "\t\tLayout: grid", "\t\tGrid:", "\t\t\tRows: 1fr 1fr",
                "\t\tBox:", "\t\t\tId: A", "\t\t\tHeight: 10",
                "\t\tBox:", "\t\t\tId: B", "\t\t\tHeight: 30");
        assertRect(40, 40, 515, 60, l.rect("G"));
        assertRect(40, 40, 515, 10, l.rect("A"));
        assertRect(40, 70, 515, 30, l.rect("B"));
    }

    @Test
    void fractionRowsWithDefiniteHeight() {
        LayoutFixture l = layOut(
                "\tBox:", "\t\tLayout: grid", "\t\tHeight: 100", "\t\tGrid:", "\t\t\tRows: 1fr 3fr",
                "\t\tBox:", "\t\t\tId: A",
                "\t\tBox:", "\t\t\tId: B");
        assertRect(40, 40, 515, 25, l.rect("A"));
        assertRect(40, 65, 515, 75, l.rect("B"));
    }

    @Test
    void autoRowsStretchInADefiniteHeight() {
        LayoutFixture l = layOut(
                "\tBox:", "\t\tLayout: grid", "\t\tHeight: 100",
                "\t\tBox:", "\t\t\tId: A",
                "\t\tBox:", "\t\t\tId: B");
        assertRect(40, 40, 515, 50, l.rect("A"));
        assertRect(40, 90, 515, 50, l.rect("B"));
    }

    @Test
    void percentagesUseTheArea() {
        LayoutFixture l = layOut(
                "\tBox:", "\t\tLayout: grid", "\t\tGrid:", "\t\t\tColumns: 200 300", "\t\t\tRows: 100",
                "\t\tBox:", "\t\t\tHeight: 10",
                "\t\tBox:", "\t\t\tId: P", "\t\t\tWidth: 50%", "\t\t\tHeight: 50%");
        assertRect(240, 40, 150, 50, l.rect("P"));
    }

    @Test
    void percentageRowsWithAutoHeightAreAuto() {
        LayoutFixture l = layOut(
                "\tBox:", "\t\tId: G", "\t\tLayout: grid", "\t\tGrid:", "\t\t\tRows: 50% 50%",
                "\t\tBox:", "\t\t\tId: A", "\t\t\tHeight: 10",
                "\t\tBox:", "\t\t\tId: B", "\t\t\tHeight: 30");
        assertRect(40, 40, 515, 40, l.rect("G"));
        assertRect(40, 40, 515, 10, l.rect("A"));
        assertRect(40, 50, 515, 30, l.rect("B"));
    }

    @Test
    void percentageColumnsInIntrinsicWidthsAreAuto() {
        // A: min 10, max 25. B: min y max 15. Sin containing block, 50% cuenta como auto.
        String items = "\t\tBox:\n\t\t\tText: aa aa\n\t\tBox:\n\t\t\tText: bbb";
        LayoutFixture l = layOut(
                "\tBox:", "\t\tId: max", "\t\tLayout: grid", "\t\tWidth: max-content",
                "\t\tGrid:", "\t\t\tColumns: 50% 50%", items,
                "\tBox:", "\t\tId: min", "\t\tLayout: grid", "\t\tWidth: min-content",
                "\t\tGrid:", "\t\t\tColumns: 50% 50%", items);
        assertEquals(40, l.rect("max").width(), 1e-9);
        assertEquals(25, l.rect("min").width(), 1e-9);
    }

    @Test
    void intrinsicWidths() {
        // A: min 10, max 25. B: min y max 15. Hueco 10.
        String items = "\t\tBox:\n\t\t\tText: aa aa\n\t\tBox:\n\t\t\tText: bbb";
        LayoutFixture l = layOut(
                "\tBox:", "\t\tId: max", "\t\tLayout: grid", "\t\tWidth: max-content",
                "\t\tGrid:", "\t\t\tColumns: auto auto", "\t\t\tGap: 10", items,
                "\tBox:", "\t\tId: min", "\t\tLayout: grid", "\t\tWidth: min-content",
                "\t\tGrid:", "\t\t\tColumns: auto auto", "\t\t\tGap: 10", items,
                "\tBox:", "\t\tId: fr", "\t\tLayout: grid", "\t\tWidth: max-content",
                "\t\tGrid:", "\t\t\tColumns: 150 1fr",
                "\t\tBox:", "\t\t\tHeight: 10", "\t\tBox:", "\t\t\tText: aaa");
        assertEquals(50, l.rect("max").width(), 1e-9);
        assertEquals(35, l.rect("min").width(), 1e-9);
        assertEquals(165, l.rect("fr").width(), 1e-9);
    }

    @Test
    void positionedChildren() {
        LayoutFixture l = layOut(
                "\tBox:", "\t\tLayout: grid", "\t\tPosition: relative", "\t\tHeight: 50",
                "\t\tGrid:", "\t\t\tColumns: 100 100",
                "\t\tBox:", "\t\t\tId: R", "\t\t\tPosition: relative", "\t\t\tOffset: 3 auto auto 4",
                "\t\tBox:", "\t\t\tId: A", "\t\t\tPosition: absolute", "\t\t\tOffset: auto 0 0 auto",
                "\t\t\tWidth: 10", "\t\t\tHeight: 10",
                "\t\tBox:", "\t\t\tId: B");
        assertRect(44, 43, 100, 50, l.rect("R"));
        assertRect(140, 40, 100, 50, l.rect("B"));
        assertRect(545, 80, 10, 10, l.rect("A"));
    }
}
