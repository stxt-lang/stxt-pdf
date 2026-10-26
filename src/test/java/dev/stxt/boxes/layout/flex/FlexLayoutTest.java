package dev.stxt.boxes.layout.flex;

import dev.stxt.boxes.layout.LayoutFixture;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.tree.Style;
import org.junit.jupiter.api.Test;

import static dev.stxt.boxes.layout.LayoutFixture.layOut;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FlexLayoutTest {

    private static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(), new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height())).toString());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    @Test
    void zeroBasisGivesEqualColumns() {
        LayoutFixture l = layOut(
                "\tBox:", "\t\tLayout: flex", "\t\tFlex:", "\t\t\tGap: 10",
                "\t\tBox:", "\t\t\tId: A", "\t\t\tHeight: 10", "\t\t\tItem:", "\t\t\t\tBasis: 0", "\t\t\t\tGrow: 1",
                "\t\tBox:", "\t\t\tId: B", "\t\t\tHeight: 10", "\t\t\tItem:", "\t\t\t\tBasis: 0", "\t\t\t\tGrow: 1",
                "\t\tBox:", "\t\t\tId: C", "\t\t\tHeight: 10", "\t\t\tItem:", "\t\t\t\tBasis: 0", "\t\t\t\tGrow: 1");
        assertRect(40, 40, 165, 10, l.rect("A"));
        assertRect(215, 40, 165, 10, l.rect("B"));
        assertRect(390, 40, 165, 10, l.rect("C"));
    }

    @Test
    void shrinkFreezesAtMinWidth() {
        // Faltan 85: cada uno cedería 42.5, pero A no baja de 280 y B absorbe el resto.
        LayoutFixture l = layOut(
                "\tBox:", "\t\tLayout: flex",
                "\t\tBox:", "\t\t\tId: A", "\t\t\tMin width: 280", "\t\t\tHeight: 10", "\t\t\tItem:", "\t\t\t\tBasis: 300",
                "\t\tBox:", "\t\t\tId: B", "\t\t\tHeight: 10", "\t\t\tItem:", "\t\t\t\tBasis: 300");
        assertRect(40, 40, 280, 10, l.rect("A"));
        assertRect(320, 40, 235, 10, l.rect("B"));
    }

    @Test
    void autoMinWidthIsTheMinContent() {
        // La palabra de A mide 300: A no encoge por debajo y B cede el resto.
        LayoutFixture l = layOut(
                "\tBox:", "\t\tLayout: flex",
                "\t\tBox:", "\t\t\tId: A", "\t\t\tText: " + "a".repeat(60), "\t\t\tItem:", "\t\t\t\tBasis: 500",
                "\t\tBox:", "\t\t\tId: B", "\t\t\tHeight: 10", "\t\t\tItem:", "\t\t\t\tBasis: 500");
        assertEquals(300, l.rect("A").width(), 1e-9);
        assertRect(340, 40, 215, 10, l.rect("B"));
    }

    @Test
    void itemWithWidthShrinksToItsContent() {
        // Faltan 285: los dos ceden 142.5. Su contenido mide 10, así que Width no los frena.
        LayoutFixture l = layOut(
                "\tBox:", "\t\tLayout: flex",
                "\t\tBox:", "\t\t\tId: A", "\t\t\tWidth: 400", "\t\t\tText: aa",
                "\t\tBox:", "\t\t\tId: B", "\t\t\tWidth: 400", "\t\t\tText: bb");
        assertRect(40, 40, 257.5, 12, l.rect("A"));
        assertRect(297.5, 40, 257.5, 12, l.rect("B"));
    }

    @Test
    void itemWithWidthDoesNotShrinkBelowItsContent() {
        // A cedería 142.5, pero su palabra mide 300: se congela ahí y B cede el resto.
        LayoutFixture l = layOut(
                "\tBox:", "\t\tLayout: flex",
                "\t\tBox:", "\t\t\tId: A", "\t\t\tWidth: 400", "\t\t\tText: " + "a".repeat(60),
                "\t\tBox:", "\t\t\tId: B", "\t\t\tWidth: 400", "\t\t\tText: b");
        assertEquals(300, l.rect("A").width(), 1e-9);
        assertRect(340, 40, 215, 12, l.rect("B"));
    }

    @Test
    void columnItemWithHeightShrinksToItsContent() {
        // En 100 de alto faltan 60: los dos ceden 30. Su contenido mide 12 (una línea).
        LayoutFixture l = layOut(
                "\tBox:", "\t\tLayout: flex", "\t\tHeight: 100", "\t\tFlex:", "\t\t\tDirection: column",
                "\t\tBox:", "\t\t\tId: A", "\t\t\tHeight: 80", "\t\t\tText: a",
                "\t\tBox:", "\t\t\tId: B", "\t\t\tHeight: 80", "\t\t\tText: b");
        assertRect(40, 40, 515, 50, l.rect("A"));
        assertRect(40, 90, 515, 50, l.rect("B"));
    }

    @Test
    void columnItemWithHeightDoesNotShrinkBelowItsContent() {
        // A cedería 30, pero sus tres líneas miden 36 (con 10 de ancho, una palabra por línea).
        LayoutFixture l = layOut(
                "\tBox:", "\t\tLayout: flex", "\t\tHeight: 50", "\t\tFlex:", "\t\t\tDirection: column",
                "\t\tBox:", "\t\t\tId: A", "\t\t\tHeight: 40", "\t\t\tWidth: 10", "\t\t\tText: aa bb cc",
                "\t\tBox:", "\t\t\tId: B", "\t\t\tHeight: 40", "\t\t\tText: d");
        assertRect(40, 40, 10, 36, l.rect("A"));
        assertRect(40, 76, 515, 14, l.rect("B"));
    }

    @Test
    void itemMarginsAreOutsideTheMainSize() {
        LayoutFixture l = layOut(
                "\tBox:", "\t\tLayout: flex",
                "\t\tBox:", "\t\t\tId: A", "\t\t\tWidth: 100", "\t\t\tHeight: 10", "\t\t\tMargin: 10",
                "\t\tBox:", "\t\t\tId: B", "\t\t\tWidth: 100", "\t\t\tHeight: 10");
        assertRect(50, 50, 100, 10, l.rect("A"));
        // B declara Height: no se estira a la línea (30).
        assertRect(160, 40, 100, 10, l.rect("B"));
    }

    @Test
    void mainAxisDistribution() {
        // Dos ítems de 100 en 515: sobran 315.
        LayoutFixture l = layOut(
                "\tBox:", "\t\tLayout: flex", "\t\tFlex:", "\t\t\tMain axis: center",
                "\t\tBox:", "\t\t\tId: c1", "\t\t\tWidth: 100", "\t\t\tHeight: 10",
                "\t\tBox:", "\t\t\tId: c2", "\t\t\tWidth: 100", "\t\t\tHeight: 10",
                "\tBox:", "\t\tLayout: flex", "\t\tFlex:", "\t\t\tMain axis: between",
                "\t\tBox:", "\t\t\tId: b1", "\t\t\tWidth: 100", "\t\t\tHeight: 10",
                "\t\tBox:", "\t\t\tId: b2", "\t\t\tWidth: 100", "\t\t\tHeight: 10",
                "\tBox:", "\t\tLayout: flex", "\t\tFlex:", "\t\t\tMain axis: evenly",
                "\t\tBox:", "\t\t\tId: e1", "\t\t\tWidth: 100", "\t\t\tHeight: 10",
                "\t\tBox:", "\t\t\tId: e2", "\t\t\tWidth: 100", "\t\t\tHeight: 10");
        assertEquals(197.5, l.rect("c1").x(), 1e-9);
        assertEquals(297.5, l.rect("c2").x(), 1e-9);
        assertEquals(40, l.rect("b1").x(), 1e-9);
        assertEquals(455, l.rect("b2").x(), 1e-9);
        assertEquals(145, l.rect("e1").x(), 1e-9);
        assertEquals(350, l.rect("e2").x(), 1e-9);
    }

    @Test
    void crossAxisAlignmentInADefiniteContainer() {
        // Una sola línea con alto definido: la línea mide lo que el contenedor (100).
        LayoutFixture l = layOut(
                "\tBox:", "\t\tLayout: flex", "\t\tHeight: 100",
                "\t\tBox:", "\t\t\tId: S", "\t\t\tWidth: 50",
                "\t\tBox:", "\t\t\tId: T", "\t\t\tWidth: 50", "\t\t\tHeight: 20", "\t\t\tItem:", "\t\t\t\tCross axis: start",
                "\t\tBox:", "\t\t\tId: C", "\t\t\tWidth: 50", "\t\t\tHeight: 20", "\t\t\tItem:", "\t\t\t\tCross axis: center",
                "\t\tBox:", "\t\t\tId: E", "\t\t\tWidth: 50", "\t\t\tHeight: 20", "\t\t\tItem:", "\t\t\t\tCross axis: end");
        assertRect(40, 40, 50, 100, l.rect("S"));
        assertRect(90, 40, 50, 20, l.rect("T"));
        assertRect(140, 80, 50, 20, l.rect("C"));
        assertRect(190, 120, 50, 20, l.rect("E"));
    }

    @Test
    void stretchToTheTallestItem() {
        LayoutFixture l = layOut(
                "\tBox:", "\t\tId: F", "\t\tLayout: flex",
                "\t\tBox:", "\t\t\tId: A", "\t\t\tWidth: 50",
                "\t\tBox:", "\t\t\tId: B", "\t\t\tWidth: 50", "\t\t\tHeight: 30",
                "\t\tBox:", "\t\t\tId: M", "\t\t\tWidth: 50", "\t\t\tMax height: 25");
        assertRect(40, 40, 515, 30, l.rect("F"));
        assertRect(40, 40, 50, 30, l.rect("A"));
        assertRect(140, 40, 50, 25, l.rect("M"));
    }

    @Test
    void wrapBreaksIntoLines() {
        // 200 + 10 + 200 caben en 515; el tercero pasa a la segunda línea.
        LayoutFixture l = layOut(
                "\tBox:", "\t\tId: F", "\t\tLayout: flex", "\t\tFlex:", "\t\t\tWrap: true", "\t\t\tGap: 10",
                "\t\tBox:", "\t\t\tId: A", "\t\t\tWidth: 200", "\t\t\tHeight: 20",
                "\t\tBox:", "\t\t\tId: B", "\t\t\tWidth: 200", "\t\t\tHeight: 20",
                "\t\tBox:", "\t\t\tId: C", "\t\t\tWidth: 200", "\t\t\tHeight: 20");
        assertRect(40, 40, 515, 50, l.rect("F"));
        assertRect(250, 40, 200, 20, l.rect("B"));
        assertRect(40, 70, 200, 20, l.rect("C"));
    }

    @Test
    void linesDistributionInADefiniteContainer() {
        // Líneas de 20 + hueco 10 + 20 en 100: sobran 50, centradas.
        LayoutFixture l = layOut(
                "\tBox:", "\t\tLayout: flex", "\t\tHeight: 100",
                "\t\tFlex:", "\t\t\tWrap: true", "\t\t\tGap: 10", "\t\t\tLines: center",
                "\t\tBox:", "\t\t\tId: A", "\t\t\tWidth: 300", "\t\t\tHeight: 20",
                "\t\tBox:", "\t\t\tId: B", "\t\t\tWidth: 300", "\t\t\tHeight: 20");
        assertRect(40, 65, 300, 20, l.rect("A"));
        assertRect(40, 95, 300, 20, l.rect("B"));
    }

    @Test
    void linesStretchByDefault() {
        // Líneas de 0 y 20 en 100 con un hueco de 10: sobran 70, 35 para cada línea.
        LayoutFixture l = layOut(
                "\tBox:", "\t\tLayout: flex", "\t\tHeight: 100",
                "\t\tFlex:", "\t\t\tWrap: true", "\t\t\tGap: 10",
                "\t\tBox:", "\t\t\tId: A", "\t\t\tWidth: 300",
                "\t\tBox:", "\t\t\tId: B", "\t\t\tWidth: 300", "\t\t\tHeight: 20");
        assertRect(40, 40, 300, 35, l.rect("A"));
        assertRect(40, 85, 300, 20, l.rect("B"));
    }

    @Test
    void columnDirection() {
        LayoutFixture l = layOut(
                "\tBox:", "\t\tId: F", "\t\tLayout: flex", "\t\tFlex:", "\t\t\tDirection: column", "\t\t\tGap: 5",
                "\t\tBox:", "\t\t\tId: A", "\t\t\tText: aaa",
                "\t\tBox:", "\t\t\tId: B", "\t\t\tWidth: 100", "\t\t\tHeight: 20",
                "\t\tBox:", "\t\t\tId: C", "\t\t\tText: aaaa", "\t\t\tItem:", "\t\t\t\tCross axis: center");
        assertRect(40, 40, 515, 54, l.rect("F"));
        assertRect(40, 40, 515, 12, l.rect("A"));
        assertRect(40, 57, 100, 20, l.rect("B"));
        assertRect(287.5, 82, 20, 12, l.rect("C"));
    }

    @Test
    void columnGrowsOnlyWithADefiniteHeight() {
        LayoutFixture l = layOut(
                "\tBox:", "\t\tLayout: flex", "\t\tHeight: 100", "\t\tFlex:", "\t\t\tDirection: column",
                "\t\tBox:", "\t\t\tHeight: 20",
                "\t\tBox:", "\t\t\tId: G", "\t\t\tItem:", "\t\t\t\tGrow: 1",
                "\tBox:", "\t\tLayout: flex", "\t\tFlex:", "\t\t\tDirection: column",
                "\t\tBox:", "\t\t\tHeight: 20",
                "\t\tBox:", "\t\t\tId: N", "\t\t\tItem:", "\t\t\t\tGrow: 1");
        assertRect(40, 60, 515, 80, l.rect("G"));
        assertRect(40, 160, 515, 0, l.rect("N"));
    }

    @Test
    void intrinsicWidths() {
        // A: min 10, max 25. B: min y max 15. Hueco 10.
        String a = "\t\tBox:\n\t\t\tId: %s\n\t\t\tText: aa aa";
        String b = "\t\tBox:\n\t\t\tId: %s\n\t\t\tText: bbb";
        LayoutFixture l = layOut(
                "\tBox:", "\t\tId: max", "\t\tLayout: flex", "\t\tWidth: max-content", "\t\tFlex:", "\t\t\tGap: 10",
                a.formatted("maxA"), b.formatted("maxB"),
                "\tBox:", "\t\tId: min", "\t\tLayout: flex", "\t\tWidth: min-content", "\t\tFlex:", "\t\t\tGap: 10",
                a.formatted("minA"), b.formatted("minB"),
                "\tBox:", "\t\tId: wrap", "\t\tLayout: flex", "\t\tWidth: min-content",
                "\t\tFlex:", "\t\t\tGap: 10", "\t\t\tWrap: true",
                a.formatted("wrapA"), b.formatted("wrapB"));
        assertEquals(50, l.rect("max").width(), 1e-9);
        assertEquals(35, l.rect("min").width(), 1e-9);
        assertEquals(15, l.rect("wrap").width(), 1e-9);
        // En min-content falta sitio: B se congela en su mínimo (15) y A baja al suyo (10).
        assertEquals(10, l.rect("minA").width(), 1e-9);
        assertEquals(15, l.rect("minB").width(), 1e-9);
    }

    @Test
    void textInAFlexContainerIsAnAnonymousItem() {
        LayoutFixture l = layOut(
                "\tBox:", "\t\tId: F", "\t\tLayout: flex", "\t\tText: aaa bbb",
                "\t\tBox:", "\t\t\tId: B", "\t\t\tWidth: 100", "\t\t\tHeight: 30");
        assertRect(75, 40, 100, 30, l.rect("B"));
    }

    @Test
    void positionedChildren() {
        LayoutFixture l = layOut(
                "\tBox:", "\t\tLayout: flex", "\t\tPosition: relative", "\t\tHeight: 50", "\t\tPadding: 5",
                "\t\tBox:", "\t\t\tId: R", "\t\t\tWidth: 100", "\t\t\tPosition: relative", "\t\t\tOffset: 3 auto auto 4",
                "\t\tBox:", "\t\t\tId: A", "\t\t\tPosition: absolute", "\t\t\tOffset: auto 0 0 auto",
                "\t\t\tWidth: 10", "\t\t\tHeight: 10",
                "\t\tBox:", "\t\t\tId: B", "\t\t\tWidth: 100");
        assertRect(49, 48, 100, 40, l.rect("R"));
        // B no se mueve con R; A no ocupa sitio en la línea.
        assertRect(145, 45, 100, 40, l.rect("B"));
        assertRect(545, 80, 10, 10, l.rect("A"));
    }

    @Test
    void distribution() {
        assertArrayEquals(new double[]{0, 0}, FlexLayout.distribute(Style.Distribution.START, 30, 3), 1e-9);
        assertArrayEquals(new double[]{30, 0}, FlexLayout.distribute(Style.Distribution.END, 30, 3), 1e-9);
        assertArrayEquals(new double[]{0, 15}, FlexLayout.distribute(Style.Distribution.BETWEEN, 30, 3), 1e-9);
        assertArrayEquals(new double[]{5, 10}, FlexLayout.distribute(Style.Distribution.AROUND, 30, 3), 1e-9);
        assertArrayEquals(new double[]{7.5, 7.5}, FlexLayout.distribute(Style.Distribution.EVENLY, 30, 3), 1e-9);
        // Sin sitio: between al principio, evenly centrado.
        assertArrayEquals(new double[]{0, 0}, FlexLayout.distribute(Style.Distribution.BETWEEN, -30, 3), 1e-9);
        assertArrayEquals(new double[]{-15, 0}, FlexLayout.distribute(Style.Distribution.EVENLY, -30, 3), 1e-9);
    }
}
