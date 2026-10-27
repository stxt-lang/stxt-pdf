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
                "\tFlex:", "\t\tGap: 10",
                "\t\tItem:", "\t\t\tBasis: 0", "\t\t\tGrow: 1",
                "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10",
                "\t\tItem:", "\t\t\tBasis: 0", "\t\t\tGrow: 1",
                "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10",
                "\t\tItem:", "\t\t\tBasis: 0", "\t\t\tGrow: 1",
                "\t\t\tBlock:", "\t\t\t\tId: C", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10");
        assertRect(40, 40, 165, 10, l.rect("A"));
        assertRect(215, 40, 165, 10, l.rect("B"));
        assertRect(390, 40, 165, 10, l.rect("C"));
    }

    @Test
    void shrinkFreezesAtMinWidth() {
        // Faltan 85: cada uno cedería 42.5, pero A no baja de 280 y B absorbe el resto.
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tItem:", "\t\t\tBasis: 300",
                "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tMin width: 280", "\t\t\t\t\tHeight: 10",
                "\t\tItem:", "\t\t\tBasis: 300",
                "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10");
        assertRect(40, 40, 280, 10, l.rect("A"));
        assertRect(320, 40, 235, 10, l.rect("B"));
    }

    @Test
    void autoMinWidthIsTheMinContent() {
        // La palabra de A mide 300: A no encoge por debajo y B cede el resto.
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tItem:", "\t\t\tBasis: 500",
                "\t\t\tText: " + "a".repeat(60), "\t\t\t\tId: A",
                "\t\tItem:", "\t\t\tBasis: 500",
                "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10");
        assertEquals(300, l.rect("A").width(), 1e-9);
        assertRect(340, 40, 215, 10, l.rect("B"));
    }

    @Test
    void itemWithWidthShrinksToItsContent() {
        // Faltan 285: los dos ceden 142.5. Su contenido mide 10, así que Width no los frena.
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tItem:", "\t\t\tText: aa", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 400",
                "\t\tItem:", "\t\t\tText: bb", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 400");
        assertRect(40, 40, 257.5, 12, l.rect("A"));
        assertRect(297.5, 40, 257.5, 12, l.rect("B"));
    }

    @Test
    void itemWithWidthDoesNotShrinkBelowItsContent() {
        // A cedería 142.5, pero su palabra mide 300: se congela ahí y B cede el resto.
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tItem:", "\t\t\tText: " + "a".repeat(60), "\t\t\t\tId: A", "\t\t\t\tBox:",
                "\t\t\t\t\tWidth: 400",
                "\t\tItem:", "\t\t\tText: b", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 400");
        assertEquals(300, l.rect("A").width(), 1e-9);
        assertRect(340, 40, 215, 12, l.rect("B"));
    }

    @Test
    void columnItemWithHeightShrinksToItsContent() {
        // En 100 de alto faltan 60: los dos ceden 30. Su contenido mide 12 (una línea).
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tBox:", "\t\t\tHeight: 100", "\t\tDirection: column",
                "\t\tItem:", "\t\t\tText: a", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 80",
                "\t\tItem:", "\t\t\tText: b", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 80");
        assertRect(40, 40, 515, 50, l.rect("A"));
        assertRect(40, 90, 515, 50, l.rect("B"));
    }

    @Test
    void columnItemWithHeightDoesNotShrinkBelowItsContent() {
        // A cedería 30, pero sus tres líneas miden 36 (con 10 de ancho, una palabra por línea).
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tBox:", "\t\t\tHeight: 50", "\t\tDirection: column",
                "\t\tItem:", "\t\t\tText: aa bb cc", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 40",
                "\t\t\t\t\tWidth: 10",
                "\t\tItem:", "\t\t\tText: d", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 40");
        assertRect(40, 40, 10, 36, l.rect("A"));
        assertRect(40, 76, 515, 14, l.rect("B"));
    }

    @Test
    void itemMarginsAreOutsideTheMainSize() {
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 100",
                "\t\t\t\t\tHeight: 10", "\t\t\t\t\tMargin: 10",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 100",
                "\t\t\t\t\tHeight: 10");
        assertRect(50, 50, 100, 10, l.rect("A"));
        // B declara Height: no se estira a la línea (30).
        assertRect(160, 40, 100, 10, l.rect("B"));
    }

    @Test
    void mainAxisDistribution() {
        // Dos ítems de 100 en 515: sobran 315.
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tMain axis: center",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: c1", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 100",
                "\t\t\t\t\tHeight: 10",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: c2", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 100",
                "\t\t\t\t\tHeight: 10",
                "\tFlex:", "\t\tMain axis: between",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: b1", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 100",
                "\t\t\t\t\tHeight: 10",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: b2", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 100",
                "\t\t\t\t\tHeight: 10",
                "\tFlex:", "\t\tMain axis: evenly",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: e1", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 100",
                "\t\t\t\t\tHeight: 10",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: e2", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 100",
                "\t\t\t\t\tHeight: 10");
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
                "\tFlex:", "\t\tBox:", "\t\t\tHeight: 100",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: S", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 50",
                "\t\tItem:", "\t\t\tCross axis: start",
                "\t\t\tBlock:", "\t\t\t\tId: T", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 50", "\t\t\t\t\tHeight: 20",
                "\t\tItem:", "\t\t\tCross axis: center",
                "\t\t\tBlock:", "\t\t\t\tId: C", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 50", "\t\t\t\t\tHeight: 20",
                "\t\tItem:", "\t\t\tCross axis: end",
                "\t\t\tBlock:", "\t\t\t\tId: E", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 50", "\t\t\t\t\tHeight: 20");
        assertRect(40, 40, 50, 100, l.rect("S"));
        assertRect(90, 40, 50, 20, l.rect("T"));
        assertRect(140, 80, 50, 20, l.rect("C"));
        assertRect(190, 120, 50, 20, l.rect("E"));
    }

    @Test
    void stretchToTheTallestItem() {
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tId: F",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 50",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 50",
                "\t\t\t\t\tHeight: 30",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: M", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 50",
                "\t\t\t\t\tMax height: 25");
        assertRect(40, 40, 515, 30, l.rect("F"));
        assertRect(40, 40, 50, 30, l.rect("A"));
        assertRect(140, 40, 50, 25, l.rect("M"));
    }

    @Test
    void wrapBreaksIntoLines() {
        // 200 + 10 + 200 caben en 515; el tercero pasa a la segunda línea.
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tId: F", "\t\tWrap: true", "\t\tGap: 10",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 200",
                "\t\t\t\t\tHeight: 20",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 200",
                "\t\t\t\t\tHeight: 20",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: C", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 200",
                "\t\t\t\t\tHeight: 20");
        assertRect(40, 40, 515, 50, l.rect("F"));
        assertRect(250, 40, 200, 20, l.rect("B"));
        assertRect(40, 70, 200, 20, l.rect("C"));
    }

    @Test
    void linesDistributionInADefiniteContainer() {
        // Líneas de 20 + hueco 10 + 20 en 100: sobran 50, centradas.
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tBox:", "\t\t\tHeight: 100", "\t\tWrap: true", "\t\tGap: 10", "\t\tLines: center",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 300",
                "\t\t\t\t\tHeight: 20",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 300",
                "\t\t\t\t\tHeight: 20");
        assertRect(40, 65, 300, 20, l.rect("A"));
        assertRect(40, 95, 300, 20, l.rect("B"));
    }

    @Test
    void linesStretchByDefault() {
        // Líneas de 0 y 20 en 100 con un hueco de 10: sobran 70, 35 para cada línea.
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tBox:", "\t\t\tHeight: 100", "\t\tWrap: true", "\t\tGap: 10",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 300",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 300",
                "\t\t\t\t\tHeight: 20");
        assertRect(40, 40, 300, 35, l.rect("A"));
        assertRect(40, 85, 300, 20, l.rect("B"));
    }

    @Test
    void columnDirection() {
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tId: F", "\t\tDirection: column", "\t\tGap: 5",
                "\t\tItem:", "\t\t\tText: aaa", "\t\t\t\tId: A",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 100",
                "\t\t\t\t\tHeight: 20",
                "\t\tItem:", "\t\t\tCross axis: center",
                "\t\t\tText: aaaa", "\t\t\t\tId: C");
        assertRect(40, 40, 515, 54, l.rect("F"));
        assertRect(40, 40, 515, 12, l.rect("A"));
        assertRect(40, 57, 100, 20, l.rect("B"));
        assertRect(287.5, 82, 20, 12, l.rect("C"));
    }

    @Test
    void columnGrowsWithADefiniteHeight() {
        // Sin Height en el contenedor, Grow es un error (catálogo de errores).
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tBox:", "\t\t\tHeight: 100", "\t\tDirection: column",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 20",
                "\t\tItem:", "\t\t\tGrow: 1",
                "\t\t\tBlock:", "\t\t\t\tId: G");
        assertRect(40, 60, 515, 80, l.rect("G"));
    }

    @Test
    void intrinsicWidths() {
        // A: min 10, max 25. B: min y max 15. Hueco 10.
        String a = "\t\tItem:\n\t\t\tText: aa aa\n\t\t\t\tId: %s";
        String b = "\t\tItem:\n\t\t\tText: bbb\n\t\t\t\tId: %s";
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tId: max", "\t\tBox:", "\t\t\tWidth: max-content", "\t\tGap: 10",
                a.formatted("maxA"), b.formatted("maxB"),
                "\tFlex:", "\t\tId: min", "\t\tBox:", "\t\t\tWidth: min-content", "\t\tGap: 10",
                a.formatted("minA"), b.formatted("minB"),
                "\tFlex:", "\t\tId: wrap", "\t\tBox:", "\t\t\tWidth: min-content", "\t\tGap: 10", "\t\tWrap: true",
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
                "\tFlex:", "\t\tId: F",
                "\t\tItem:", "\t\t\tText: aaa bbb", "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:",
                "\t\t\t\t\tWidth: 100", "\t\t\t\t\tHeight: 30");
        assertRect(75, 40, 100, 30, l.rect("B"));
    }

    @Test
    void positionedChildren() {
        LayoutFixture l = layOut(
                // Un flex posicionado: Relative con el relleno y, dentro, el Flex con el alto.
                "\tRelative:", "\t\tBox:", "\t\t\tPadding: 5",
                "\t\tFlex:", "\t\t\tBox:", "\t\t\t\tHeight: 40",
                "\t\t\tItem:", "\t\t\t\tRelative:", "\t\t\t\t\tId: R", "\t\t\t\t\tBox:", "\t\t\t\t\t\tWidth: 100",
                "\t\t\t\t\tOffset: 3 auto auto 4",
                "\t\t\tAbsolute:", "\t\t\t\tId: A", "\t\t\t\tOffset: auto 0 0 auto", "\t\t\t\tBox:",
                "\t\t\t\t\tWidth: 10", "\t\t\t\t\tHeight: 10",
                "\t\t\tItem:", "\t\t\t\tBlock:", "\t\t\t\t\tId: B", "\t\t\t\t\tBox:", "\t\t\t\t\t\tWidth: 100");
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
