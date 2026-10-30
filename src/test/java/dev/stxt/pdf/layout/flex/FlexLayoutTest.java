package dev.stxt.pdf.layout.flex;

import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.Distribution;
import dev.stxt.pdf.model.Rect;
import org.junit.jupiter.api.Test;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
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
                // Un flex posicionado: Frame con el relleno y, dentro, el Flex con el alto.
                "\tFrame:", "\t\tBox:", "\t\t\tPadding: 5",
                "\t\tFlex:", "\t\t\tBox:", "\t\t\t\tHeight: 40",
                "\t\t\tItem:", "\t\t\t\tFrame:", "\t\t\t\t\tId: R", "\t\t\t\t\tBox:", "\t\t\t\t\t\tWidth: 100",
                "\t\t\t\t\tOffset: 3 auto auto 4",
                "\t\t\tOverlay:", "\t\t\t\tId: A", "\t\t\t\tOffset: auto 0 0 auto", "\t\t\t\tBox:",
                "\t\t\t\t\tWidth: 10", "\t\t\t\t\tHeight: 10",
                "\t\t\tItem:", "\t\t\t\tBlock:", "\t\t\t\t\tId: B", "\t\t\t\t\tBox:", "\t\t\t\t\t\tWidth: 100");
        assertRect(49, 48, 100, 40, l.rect("R"));
        // B no se mueve con R; A no ocupa sitio en la línea.
        assertRect(145, 45, 100, 40, l.rect("B"));
        assertRect(545, 80, 10, 10, l.rect("A"));
    }

    @Test
    void distribution() {
        assertArrayEquals(new double[]{0, 0}, FlexLayout.distribute(Distribution.START, 30, 3), 1e-9);
        assertArrayEquals(new double[]{30, 0}, FlexLayout.distribute(Distribution.END, 30, 3), 1e-9);
        assertArrayEquals(new double[]{0, 15}, FlexLayout.distribute(Distribution.BETWEEN, 30, 3), 1e-9);
        assertArrayEquals(new double[]{5, 10}, FlexLayout.distribute(Distribution.AROUND, 30, 3), 1e-9);
        assertArrayEquals(new double[]{7.5, 7.5}, FlexLayout.distribute(Distribution.EVENLY, 30, 3), 1e-9);
        // Sin sitio: between al principio, evenly centrado.
        assertArrayEquals(new double[]{0, 0}, FlexLayout.distribute(Distribution.BETWEEN, -30, 3), 1e-9);
        assertArrayEquals(new double[]{-15, 0}, FlexLayout.distribute(Distribution.EVENLY, -30, 3), 1e-9);
    }

    // Casos que faltaban según las pruebas de mutación: huecos distintos en cada eje,
    // tamaños intrínsecos con más de dos ítems y márgenes en los ítems.

    private static String block(String id, String... box) {
        StringBuilder sb = new StringBuilder("\t\t\tBlock:\n\t\t\t\tId: " + id + (box.length > 0 ? "\n\t\t\t\tBox:" : ""));
        for (String b : box) {
            sb.append("\n\t\t\t\t\t").append(b);
        }
        return sb.toString();
    }

    @Test
    void rowAndColumnGapsInARow() {
        // Gap: filas 7, columnas 13. 200 + 13 + 200 = 413 caben en 515; el tercero pasa a otra línea, 7 más abajo.
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tId: F", "\t\tWrap: true", "\t\tGap: 7 13",
                "\t\tItem:", block("A", "Width: 200", "Height: 20"),
                "\t\tItem:", block("B", "Width: 200", "Height: 20"),
                "\t\tItem:", block("C", "Width: 200", "Height: 20"));
        assertRect(40, 40, 515, 47, l.rect("F"));
        assertRect(253, 40, 200, 20, l.rect("B"));
        assertRect(40, 67, 200, 20, l.rect("C"));
    }

    @Test
    void rowAndColumnGapsInAColumn() {
        // En columna, el hueco entre ítems es el de las filas (7) y entre líneas, el de las columnas (13): 40 + 7 + 40
        // = 87 caben en 100; el tercero pasa a la línea siguiente, 100 + 13 a la derecha.
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tId: F", "\t\tBox:", "\t\t\tHeight: 100", "\t\tDirection: column", "\t\tWrap: true",
                "\t\tLines: start", "\t\tGap: 7 13",
                "\t\tItem:", block("A", "Width: 100", "Height: 40"),
                "\t\tItem:", block("B", "Width: 100", "Height: 40"),
                "\t\tItem:", block("C", "Width: 100", "Height: 40"));
        assertRect(40, 87, 100, 40, l.rect("B"));
        assertRect(153, 40, 100, 40, l.rect("C"));
        assertRect(40, 40, 515, 100, l.rect("F"));
    }

    @Test
    void intrinsicWidthsWithThreeItems() {
        // max-content de 5 + 10 + 15 y dos huecos de 10, con y sin Wrap; min-content sin Wrap, igual con 5 + 10 + 15.
        String items = "\t\tItem:\n\t\t\tText: a\n\t\tItem:\n\t\t\tText: bb\n\t\tItem:\n\t\t\tText: ccc";
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tId: max", "\t\tBox:", "\t\t\tWidth: max-content", "\t\tGap: 10", items,
                "\tFlex:", "\t\tId: wrap", "\t\tBox:", "\t\t\tWidth: max-content", "\t\tGap: 10", "\t\tWrap: true",
                items,
                "\tFlex:", "\t\tId: min", "\t\tBox:", "\t\t\tWidth: min-content", "\t\tGap: 10", items);
        assertEquals(50, l.rect("max").width(), 1e-9);
        assertEquals(50, l.rect("wrap").width(), 1e-9);
        assertEquals(50, l.rect("min").width(), 1e-9);
    }

    @Test
    void basisCountsTheMargins() {
        // Tamaños hipotéticos exteriores 100 + 20 y 100: sobran 295, 147.5 para cada uno.
        LayoutFixture l = layOut(
                "\tFlex:",
                "\t\tItem:", "\t\t\tBasis: 100", "\t\t\tGrow: 1", block("A", "Height: 10", "Margin: 0 10"),
                "\t\tItem:", "\t\t\tBasis: 100", "\t\t\tGrow: 1", block("B", "Height: 10"));
        assertRect(50, 40, 247.5, 10, l.rect("A"));
        assertRect(307.5, 40, 247.5, 10, l.rect("B"));
    }

    @Test
    void shrinkingStopsAtTheMinimumWithItsMargins() {
        // Exteriores 320 y 300 en 515: faltan 105. A no baja de 280 (300 con márgenes) y B se queda con 215.
        LayoutFixture l = layOut(
                "\tFlex:",
                "\t\tItem:", "\t\t\tBasis: 300", block("A", "Min width: 280", "Height: 10", "Margin: 0 10"),
                "\t\tItem:", "\t\t\tBasis: 300", block("B", "Height: 10"));
        assertRect(50, 40, 280, 10, l.rect("A"));
        assertRect(340, 40, 215, 10, l.rect("B"));
    }

    @Test
    void automaticMinimumWithMargins() {
        // La palabra de A mide 300 y sus márgenes 20: no baja de 320 exteriores, y B se queda con 195.
        LayoutFixture l = layOut(
                "\tFlex:",
                "\t\tItem:", "\t\t\tBasis: 500", "\t\t\tText: " + "a".repeat(60), "\t\t\t\tId: A", "\t\t\t\tBox:",
                "\t\t\t\t\tMargin: 0 10",
                "\t\tItem:", "\t\t\tBasis: 500", block("B", "Height: 10"));
        assertEquals(300, l.rect("A").width(), 1e-9);
        assertRect(360, 40, 195, 10, l.rect("B"));
    }

    @Test
    void shrinkIsWeightedByTheBasisWithoutMargins() {
        // Exteriores 320 y 300 en 515: faltan 105, que se reparten por Shrink × Basis (300 y 300), no por los
        // tamaños con márgenes (referencia/componentes/flex.md): 52.5 cada uno.
        LayoutFixture l = layOut(
                "\tFlex:",
                "\t\tItem:", "\t\t\tBasis: 300", block("A", "Height: 10", "Margin: 0 10"),
                "\t\tItem:", "\t\t\tBasis: 300", block("B", "Height: 10"));
        assertRect(50, 40, 247.5, 10, l.rect("A"));
        assertRect(307.5, 40, 247.5, 10, l.rect("B"));
    }

    @Test
    void stretchingLeavesTheMargins() {
        // Fila: la línea mide 30 y A, con 5 de margen arriba y abajo, se estira a 20.
        // Columna: el ancho es 515 menos los márgenes de C (505).
        LayoutFixture l = layOut(
                "\tFlex:",
                "\t\tItem:", block("A", "Width: 50", "Margin: 5"),
                "\t\tItem:", block("B", "Width: 50", "Height: 30"),
                "\tFlex:", "\t\tDirection: column",
                "\t\tItem:", block("C", "Height: 10", "Margin: 5"));
        assertRect(45, 45, 50, 20, l.rect("A"));
        assertRect(45, 75, 505, 10, l.rect("C"));
    }

    @Test
    void threeLinesWithLinesBetween() {
        // Tres líneas de 20 con huecos de 10 en 100: sobran 20, 10 entre cada dos líneas.
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tBox:", "\t\t\tHeight: 100", "\t\tWrap: true", "\t\tGap: 10", "\t\tLines: between",
                "\t\tItem:", block("A", "Width: 300", "Height: 20"),
                "\t\tItem:", block("B", "Width: 300", "Height: 20"),
                "\t\tItem:", block("C", "Width: 300", "Height: 20"));
        assertRect(40, 40, 300, 20, l.rect("A"));
        assertRect(40, 80, 300, 20, l.rect("B"));
        assertRect(40, 120, 300, 20, l.rect("C"));
    }

    @Test
    void wrapCountsTheGapsExactly() {
        // 200 + 10 + 200 + 10 + 95 = 515: una línea. Con 110, el tercero pasa a otra.
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tWrap: true", "\t\tGap: 10",
                "\t\tItem:", block("A", "Width: 200", "Height: 20"),
                "\t\tItem:", block("B", "Width: 200", "Height: 20"),
                "\t\tItem:", block("C", "Width: 95", "Height: 20"),
                "\tFlex:", "\t\tWrap: true", "\t\tGap: 10",
                "\t\tItem:", block("D", "Width: 200", "Height: 20"),
                "\t\tItem:", block("E", "Width: 200", "Height: 20"),
                "\t\tItem:", block("G", "Width: 110", "Height: 20"));
        assertRect(460, 40, 95, 20, l.rect("C"));
        assertRect(40, 90, 110, 20, l.rect("G"));
    }

    @Test
    void automaticMinimumCountsBorderAndPadding() {
        // La palabra de A mide 300, más 2 + 2 de borde y 10 + 10 de relleno: no baja de 324.
        LayoutFixture l = layOut(
                "\tFlex:",
                "\t\tItem:", "\t\t\tBasis: 500", "\t\t\tText: " + "a".repeat(60), "\t\t\t\tId: A", "\t\t\t\tBox:",
                "\t\t\t\t\tBorder: 2", "\t\t\t\t\tPadding: 0 10",
                "\t\tItem:", "\t\t\tBasis: 500", block("B", "Height: 10"));
        assertEquals(324, l.rect("A").width(), 1e-9);
        assertRect(364, 40, 191, 10, l.rect("B"));
    }

    @Test
    void maxWidthCountsTheMargins() {
        // Crecerían 147.5 cada uno, pero A no pasa de 150 (170 con márgenes): B se queda con 345.
        LayoutFixture l = layOut(
                "\tFlex:",
                "\t\tItem:", "\t\t\tBasis: 100", "\t\t\tGrow: 1", block("A", "Max width: 150", "Height: 10", "Margin: 0 10"),
                "\t\tItem:", "\t\t\tBasis: 100", "\t\t\tGrow: 1", block("B", "Height: 10"));
        assertRect(50, 40, 150, 10, l.rect("A"));
        assertRect(210, 40, 345, 10, l.rect("B"));
    }

    @Test
    void columnLimitsCountTheMargins() {
        // Columnas de 100. Max height 30 (40 con márgenes): B se queda con 60. Min height 70 (80): D, con 20.
        // Mínimo automático: el texto de E, en cuatro líneas, mide 48 (58 con márgenes): F, con 42.
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tBox:", "\t\t\tHeight: 100", "\t\tDirection: column",
                "\t\tItem:", "\t\t\tBasis: 20", "\t\t\tGrow: 1", block("A", "Max height: 30", "Margin: 5 0"),
                "\t\tItem:", "\t\t\tBasis: 20", "\t\t\tGrow: 1", block("B"),
                "\tFlex:", "\t\tBox:", "\t\t\tHeight: 100", "\t\tDirection: column",
                "\t\tItem:", "\t\t\tBasis: 80", block("C", "Min height: 70", "Margin: 5 0"),
                "\t\tItem:", "\t\t\tBasis: 80", block("D"),
                "\tFlex:", "\t\tBox:", "\t\t\tHeight: 100", "\t\tDirection: column",
                "\t\tItem:", "\t\t\tBasis: 80", "\t\t\tText: aa aa aa aa", "\t\t\t\tId: E", "\t\t\t\tBox:",
                "\t\t\t\t\tWidth: 10", "\t\t\t\t\tMargin: 5 0",
                "\t\tItem:", "\t\t\tBasis: 80", block("F"));
        assertRect(40, 45, 515, 30, l.rect("A"));
        assertRect(40, 80, 515, 60, l.rect("B"));
        assertRect(40, 145, 515, 70, l.rect("C"));
        assertRect(40, 220, 515, 20, l.rect("D"));
        assertRect(40, 245, 10, 48, l.rect("E"));
        assertRect(40, 298, 515, 42, l.rect("F"));
    }

    @Test
    void columnItemsThatDoNotStretchFitTheirContentInsideTheirMargins() {
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tDirection: column",
                "\t\tItem:", "\t\t\tCross axis: start", "\t\t\tText: aaa", "\t\t\t\tId: A", "\t\t\t\tBox:",
                "\t\t\t\t\tMargin: 0 10");
        assertRect(50, 40, 15, 12, l.rect("A"));
    }

    @Test
    void columnLinesMeasureTheirItemsWithMargins() {
        // Columna con Wrap en 60 de alto: A (50 de ancho y 5 de margen a cada lado) y B, en otra línea. La primera
        // línea mide 60, así que B empieza en 40 + 60.
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tBox:", "\t\t\tHeight: 60", "\t\tDirection: column", "\t\tWrap: true", "\t\tLines: start",
                "\t\tItem:", block("A", "Width: 50", "Height: 40", "Margin: 0 5"),
                "\t\tItem:", block("B", "Width: 80", "Height: 40"));
        assertRect(45, 40, 50, 40, l.rect("A"));
        assertRect(100, 40, 80, 40, l.rect("B"));
    }

    @Test
    void stretchingWithDifferentMarginsInEachAxis() {
        // La línea mide 40 (B). A tiene 10 de margen arriba y abajo y 5 a los lados: se estira a 40 − 20.
        LayoutFixture l = layOut(
                "\tFlex:",
                "\t\tItem:", block("A", "Width: 50", "Padding: 5 0", "Margin: 10 5"),
                "\t\tItem:", block("B", "Width: 50", "Height: 40"));
        assertRect(45, 50, 50, 20, l.rect("A"));
    }

    @Test
    void wrapCountsOneGapLessThanItems() {
        // 100 + 10 + 100 + 10 + 100 = 320; con el cuarto (190) y su hueco, 520: pasa a otra línea.
        LayoutFixture l = layOut(
                "\tFlex:", "\t\tWrap: true", "\t\tGap: 10",
                "\t\tItem:", block("A", "Width: 100", "Height: 20"),
                "\t\tItem:", block("B", "Width: 100", "Height: 20"),
                "\t\tItem:", block("C", "Width: 100", "Height: 20"),
                "\t\tItem:", block("D", "Width: 190", "Height: 20"));
        assertRect(40, 70, 190, 20, l.rect("D"));
    }
}
