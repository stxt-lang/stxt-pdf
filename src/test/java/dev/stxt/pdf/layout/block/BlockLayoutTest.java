package dev.stxt.pdf.layout.block;

import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.Rect;
import org.junit.jupiter.api.Test;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockLayoutTest {

    private static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(), new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height())).toString());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    @Test
    void parentAndFirstChildMarginsCollapse() {
        // max(10, 20) sale de P: P y C empiezan en 40 + 20.
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tMargin: 10 0 0 0",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 20 0 0 0",
                "\tBlock:", "\t\tId: S", "\t\tBox:", "\t\t\tHeight: 10");
        assertRect(40, 60, 515, 30, l.rect("P"));
        assertRect(40, 60, 515, 30, l.rect("C"));
        assertRect(40, 90, 515, 10, l.rect("S"));
    }

    @Test
    void stylesRulesShapeTheLayout() {
        // La regla da a cada Block 30 de alto y 10 de margen inferior; B la sobrescribe con 50.
        LayoutFixture l = layOut(
                "\tStyles:", "\t\tFor element: Block", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 0 0 10 0",
                "\t\tFor style: narrow", "\t\t\tBox:", "\t\t\t\tWidth: 100",
                "\tBlock:", "\t\tId: A",
                "\tBlock:", "\t\tId: B", "\t\tStyle: narrow", "\t\tBox:", "\t\t\tHeight: 50",
                "\tBlock:", "\t\tId: C");
        assertRect(40, 40, 515, 30, l.rect("A"));
        assertRect(40, 80, 100, 50, l.rect("B"));
        assertRect(40, 140, 515, 30, l.rect("C"));
    }

    @Test
    void paddingStopsTheCollapse() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tMargin: 10 0 0 0", "\t\t\tPadding: 5 0 0 0",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 20 0 0 0");
        assertRect(40, 50, 515, 55, l.rect("P"));
        assertRect(40, 75, 515, 30, l.rect("C"));
    }

    @Test
    void containMarginsStopsTheCollapse() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tMargin: 10 0 0 0", "\t\tContain margins: true",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 20 0 20 0");
        assertRect(40, 50, 515, 70, l.rect("P"));
        assertRect(40, 70, 515, 30, l.rect("C"));
    }

    @Test
    void lastChildBottomMarginLeavesTheParent() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 0 0 20 0",
                "\tBlock:", "\t\tId: S", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tMargin: 5 0 0 0");
        assertRect(40, 40, 515, 30, l.rect("P"));
        assertRect(40, 90, 515, 10, l.rect("S"));
    }

    @Test
    void bottomBorderKeepsTheLastMarginInside() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tBorder: 0 0 1 0",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 0 0 20 0",
                "\tBlock:", "\t\tId: S", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tMargin: 5 0 0 0");
        assertRect(40, 40, 515, 51, l.rect("P"));
        assertRect(40, 96, 515, 10, l.rect("S"));
    }

    @Test
    void emptyBoxLetsItsMarginsCollapseThrough() {
        // A (inferior 0), E vacía (25 y 15), C (superior 10): un solo margen de 25.
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 10",
                "\tBlock:", "\t\tId: E", "\t\tBox:", "\t\t\tMargin: 25 0 15 0",
                "\tBlock:", "\t\tId: C", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tMargin: 10 0 0 0");
        assertRect(40, 75, 515, 0, l.rect("E"));
        assertRect(40, 75, 515, 10, l.rect("C"));
    }

    @Test
    void negativeMargins() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tMargin: 0 0 20 0",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tMargin: -5 0 0 0");
        assertRect(40, 65, 515, 10, l.rect("B"));
    }

    @Test
    void autoHeightAddsPaddingAndBorder() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tPadding: 10", "\t\t\tBorder: 1",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 30");
        assertRect(40, 40, 515, 52, l.rect("P"));
        assertRect(51, 51, 493, 30, l.rect("C"));
    }

    @Test
    void widths() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: half", "\t\tBox:", "\t\t\tWidth: 50%", "\t\t\tHeight: 10",
                "\tBlock:", "\t\tId: sized", "\t\tBox:", "\t\t\tWidth: 100", "\t\t\tPadding: 10", "\t\t\tBorder: 2",
                "\t\t\tHeight: 40",
                "\tBlock:", "\t\tId: right", "\t\tBox:", "\t\t\tWidth: 200", "\t\t\tMargin: 0 0 0 auto",
                "\t\t\tHeight: 10",
                "\tBlock:", "\t\tId: margins", "\t\tBox:", "\t\t\tMargin: 0 15 0 25", "\t\t\tHeight: 10");
        assertEquals(257.5, l.rect("half").width(), 1e-9);
        // Width y Height miden siempre la caja de borde, con el relleno y el borde dentro.
        assertRect(40, 50, 100, 40, l.rect("sized"));
        assertRect(355, 90, 200, 10, l.rect("right"));
        assertRect(65, 100, 475, 10, l.rect("margins"));
    }

    @Test
    void minWinsWhenItCannotBeComparedBeforeLayout() {
        // 150 frente al 20 % de 515 (103): si se pudieran comparar al leer, sería un error.
        LayoutFixture l = layOut("\tBlock:", "\t\tId: minWins", "\t\tBox:", "\t\t\tMax width: 20%",
                "\t\t\tMin width: 150", "\t\t\tHeight: 10");
        assertEquals(150, l.rect("minWins").width(), 1e-9);
    }

    @Test
    void percentHeightOfADefiniteContainingBlock() {
        // El área útil siempre tiene alto definido; un padre con alto auto, no (catálogo de errores).
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 200",
                "\t\tBlock:", "\t\t\tId: half", "\t\t\tBox:", "\t\t\t\tHeight: 50%");
        assertEquals(100, l.rect("half").height(), 1e-9);
        assertEquals(100, layOutPaged(180, "\tBlock:", "\t\tId: page", "\t\tBox:", "\t\t\tHeight: 100%").rect("page").height(), 1e-9);
    }

    @Test
    void aspectRatioDeducesTheHeight() {
        LayoutFixture l = layOut("\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tWidth: 200", "\t\t\tAspect ratio: 16/9");
        assertEquals(112.5, l.rect("A").height(), 1e-9);
    }

    @Test
    void intrinsicWidths() {
        // FixedMetrics: 5 pt por carácter con Size 10.
        LayoutFixture l = layOut(
                "\tText: aaa bbbbbb", "\t\tId: min", "\t\tBox:", "\t\t\tWidth: narrowest", "\t\t\tPadding: 5",
                "\tText: aaa bbbbbb", "\t\tId: max", "\t\tBox:", "\t\t\tWidth: natural",
                "\tBlock:", "\t\tId: block", "\t\tBox:", "\t\t\tWidth: natural",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tWidth: 120", "\t\t\t\tMargin: 0 10",
                "\t\tText: aaa bbbbbb");
        // Ancho más estrecho: la palabra más larga (30) + relleno; el texto ocupa dos líneas.
        assertEquals(40, l.rect("min").width(), 1e-9);
        assertEquals(34, l.rect("min").height(), 1e-9);
        assertEquals(50, l.rect("max").width(), 1e-9);
        assertEquals(140, l.rect("block").width(), 1e-9);
    }

    @Test
    void outOfFlowBoxesTakeNoSpace() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 10",
                "\tOverlay:", "\t\tBox:", "\t\t\tHeight: 50",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 10");
        assertRect(40, 50, 515, 10, l.rect("B"));
    }
}
