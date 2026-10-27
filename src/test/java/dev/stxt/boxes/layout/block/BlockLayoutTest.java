package dev.stxt.boxes.layout.block;

import dev.stxt.boxes.layout.LayoutFixture;
import dev.stxt.boxes.model.Rect;
import org.junit.jupiter.api.Test;

import static dev.stxt.boxes.layout.LayoutFixture.layOut;
import static dev.stxt.boxes.layout.LayoutFixture.layOutPaged;
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
                "\tBlock:", "\t\tId: P", "\t\tMargin: 10 0 0 0",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tHeight: 30", "\t\t\tMargin: 20 0 0 0",
                "\tBlock:", "\t\tId: S", "\t\tHeight: 10");
        assertRect(40, 60, 515, 30, l.rect("P"));
        assertRect(40, 60, 515, 30, l.rect("C"));
        assertRect(40, 90, 515, 10, l.rect("S"));
    }

    @Test
    void paddingStopsTheCollapse() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tMargin: 10 0 0 0", "\t\tPadding: 5 0 0 0",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tHeight: 30", "\t\t\tMargin: 20 0 0 0");
        assertRect(40, 50, 515, 55, l.rect("P"));
        assertRect(40, 75, 515, 30, l.rect("C"));
    }

    @Test
    void isolateStopsTheCollapse() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tMargin: 10 0 0 0", "\t\tIsolate: true",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tHeight: 30", "\t\t\tMargin: 20 0 20 0");
        assertRect(40, 50, 515, 70, l.rect("P"));
        assertRect(40, 70, 515, 30, l.rect("C"));
    }

    @Test
    void lastChildBottomMarginLeavesTheParent() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P",
                "\t\tBlock:", "\t\t\tHeight: 30", "\t\t\tMargin: 0 0 20 0",
                "\tBlock:", "\t\tId: S", "\t\tHeight: 10", "\t\tMargin: 5 0 0 0");
        assertRect(40, 40, 515, 30, l.rect("P"));
        assertRect(40, 90, 515, 10, l.rect("S"));
    }

    @Test
    void bottomBorderKeepsTheLastMarginInside() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tBorder: 0 0 1 0",
                "\t\tBlock:", "\t\t\tHeight: 30", "\t\t\tMargin: 0 0 20 0",
                "\tBlock:", "\t\tId: S", "\t\tHeight: 10", "\t\tMargin: 5 0 0 0");
        assertRect(40, 40, 515, 51, l.rect("P"));
        assertRect(40, 96, 515, 10, l.rect("S"));
    }

    @Test
    void emptyBoxLetsItsMarginsCollapseThrough() {
        // A (inferior 0), E vacía (25 y 15), C (superior 10): un solo margen de 25.
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: A", "\t\tHeight: 10",
                "\tBlock:", "\t\tId: E", "\t\tMargin: 25 0 15 0",
                "\tBlock:", "\t\tId: C", "\t\tHeight: 10", "\t\tMargin: 10 0 0 0");
        assertRect(40, 75, 515, 0, l.rect("E"));
        assertRect(40, 75, 515, 10, l.rect("C"));
    }

    @Test
    void negativeMargins() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tHeight: 10", "\t\tMargin: 0 0 20 0",
                "\tBlock:", "\t\tId: B", "\t\tHeight: 10", "\t\tMargin: -5 0 0 0");
        assertRect(40, 65, 515, 10, l.rect("B"));
    }

    @Test
    void autoHeightAddsPaddingAndBorder() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tPadding: 10", "\t\tBorder: 1",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tHeight: 30");
        assertRect(40, 40, 515, 52, l.rect("P"));
        assertRect(51, 51, 493, 30, l.rect("C"));
    }

    @Test
    void widths() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: half", "\t\tWidth: 50%", "\t\tHeight: 10",
                "\tBlock:", "\t\tId: sized", "\t\tWidth: 100", "\t\tPadding: 10", "\t\tBorder: 2", "\t\tHeight: 40",
                "\tBlock:", "\t\tId: right", "\t\tWidth: 200", "\t\tMargin: 0 0 0 auto", "\t\tHeight: 10",
                "\tBlock:", "\t\tId: margins", "\t\tMargin: 0 15 0 25", "\t\tHeight: 10");
        assertEquals(257.5, l.rect("half").width(), 1e-9);
        // Width y Height miden siempre la caja de borde, con el relleno y el borde dentro.
        assertRect(40, 50, 100, 40, l.rect("sized"));
        assertRect(355, 90, 200, 10, l.rect("right"));
        assertRect(65, 100, 475, 10, l.rect("margins"));
    }

    @Test
    void minWinsWhenItCannotBeComparedBeforeLayout() {
        // 150 frente al 20 % de 515 (103): si se pudieran comparar al leer, sería un error.
        LayoutFixture l = layOut("\tBlock:", "\t\tId: minWins", "\t\tMax width: 20%", "\t\tMin width: 150",
                "\t\tHeight: 10");
        assertEquals(150, l.rect("minWins").width(), 1e-9);
    }

    @Test
    void percentHeightOfADefiniteContainingBlock() {
        // Sin alto definido (el área útil en modo continuo), es un error (catálogo de errores).
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tHeight: 200",
                "\t\tBlock:", "\t\t\tId: half", "\t\t\tHeight: 50%");
        assertEquals(100, l.rect("half").height(), 1e-9);
        assertEquals(100, layOutPaged(180, "\tBlock:", "\t\tId: page", "\t\tHeight: 100%").rect("page").height(), 1e-9);
    }

    @Test
    void aspectRatioDeducesTheHeight() {
        LayoutFixture l = layOut("\tBlock:", "\t\tId: A", "\t\tWidth: 200", "\t\tAspect ratio: 16/9");
        assertEquals(112.5, l.rect("A").height(), 1e-9);
    }

    @Test
    void intrinsicWidths() {
        // FixedMetrics: 5 pt por carácter con Font size 10.
        LayoutFixture l = layOut(
                "\tText: aaa bbbbbb", "\t\tId: min", "\t\tWidth: min-content", "\t\tPadding: 5",
                "\tText: aaa bbbbbb", "\t\tId: max", "\t\tWidth: max-content",
                "\tBlock:", "\t\tId: block", "\t\tWidth: max-content",
                "\t\tBlock:", "\t\t\tWidth: 120", "\t\t\tMargin: 0 10",
                "\t\tText: aaa bbbbbb");
        // min-content: la palabra más larga (30) + relleno; el texto ocupa dos líneas.
        assertEquals(40, l.rect("min").width(), 1e-9);
        assertEquals(34, l.rect("min").height(), 1e-9);
        assertEquals(50, l.rect("max").width(), 1e-9);
        assertEquals(140, l.rect("block").width(), 1e-9);
    }

    @Test
    void outOfFlowBoxesTakeNoSpace() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tHeight: 10",
                "\tAbsolute:", "\t\tHeight: 50",
                "\tBlock:", "\t\tId: B", "\t\tHeight: 10");
        assertRect(40, 50, 515, 10, l.rect("B"));
    }
}
