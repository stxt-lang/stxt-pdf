package dev.stxt.pdf.layout.position;

import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.Rect;
import org.junit.jupiter.api.Test;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PositioningTest {

    private static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(), new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height())).toString());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    @Test
    void relativeShiftsWithoutMovingSiblings() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 10",
                "\tFrame:", "\t\tId: R", "\t\tOffset: 5 auto auto 10", "\t\tBox:", "\t\t\tHeight: 20",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 10");
        assertRect(40, 40, 515, 10, l.rect("A"));
        assertRect(50, 55, 515, 20, l.rect("R"));
        assertRect(40, 70, 515, 10, l.rect("B"));
    }

    @Test
    void relativeOffsets() {
        // Arriba y abajo, o izquierda y derecha, a la vez son un error (catálogo de errores).
        LayoutFixture l = layOut(
                "\tFrame:", "\t\tId: start", "\t\tOffset: 5 auto auto 11", "\t\tBox:", "\t\t\tHeight: 20",
                "\tFrame:", "\t\tId: endOnly", "\t\tOffset: auto 7 9 auto", "\t\tBox:", "\t\t\tHeight: 20");
        assertRect(51, 45, 515, 20, l.rect("start"));
        assertRect(33, 51, 515, 20, l.rect("endOnly"));
    }

    @Test
    void relativePercentages() {
        // Izquierda: 10 % de 515. Arriba: 10 % del alto del padre (200). Un porcentaje vertical
        // con el alto del padre indefinido es un error (catálogo de errores).
        LayoutFixture l = layOut(
                "\tFrame:", "\t\tId: top", "\t\tOffset: auto auto auto 10%", "\t\tBox:", "\t\t\tHeight: 20",
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 200",
                "\t\tFrame:", "\t\t\tId: inner", "\t\t\tOffset: 10% auto auto auto", "\t\t\tBox:",
                "\t\t\t\tHeight: 10");
        assertRect(91.5, 40, 515, 20, l.rect("top"));
        assertRect(40, 80, 515, 10, l.rect("inner"));
    }

    @Test
    void absoluteWithoutPositionedAncestorUsesThePageArea() {
        // El área útil mide 515 × 762 desde (40, 40), sea cual sea el alto del flujo.
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 100",
                "\tOverlay:", "\t\tId: S", "\t\tOffset: 0 0 auto auto", "\t\tBox:", "\t\t\tWidth: 80",
                "\t\t\tHeight: 30",
                "\tOverlay:", "\t\tId: F", "\t\tOffset: auto 0 0 auto", "\t\tBox:", "\t\t\tWidth: 80",
                "\t\t\tHeight: 30");
        assertRect(475, 40, 80, 30, l.rect("S"));
        assertRect(475, 772, 80, 30, l.rect("F"));
    }

    @Test
    void containingBlockIsThePaddingBoxOfThePositionedAncestor() {
        // Caja de relleno de R: (42, 42), 511 × 96.
        LayoutFixture l = layOut(
                "\tFrame:", "\t\tId: R", "\t\tBox:", "\t\t\tHeight: 100", "\t\t\tBorder: 2", "\t\t\tPadding: 10",
                "\t\tOverlay:", "\t\t\tId: TL", "\t\t\tOffset: 0 auto auto 0", "\t\t\tBox:", "\t\t\t\tWidth: 50",
                "\t\t\t\tHeight: 20",
                "\t\tOverlay:", "\t\t\tId: BR", "\t\t\tOffset: auto 0 0 auto", "\t\t\tBox:", "\t\t\t\tWidth: 50",
                "\t\t\t\tHeight: 20");
        assertRect(40, 40, 515, 100, l.rect("R"));
        assertRect(42, 42, 50, 20, l.rect("TL"));
        assertRect(503, 118, 50, 20, l.rect("BR"));
    }

    @Test
    void absoluteBoxesDoNotContributeToTheParentHeight() {
        LayoutFixture l = layOut(
                "\tFrame:", "\t\tId: R",
                "\t\tOverlay:", "\t\t\tId: A", "\t\t\tBox:", "\t\t\t\tWidth: 10", "\t\t\t\tHeight: 50");
        assertRect(40, 40, 515, 0, l.rect("R"));
        assertRect(40, 40, 10, 50, l.rect("A"));
    }

    @Test
    void staticPositionAndShrinkToFit() {
        // A estaría en la caja de contenido de M: (40 + 5, 40 + 10 + 5). Su texto mide 30.
        LayoutFixture l = layOut(
                "\tFrame:", "\t\tId: R",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 10",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tPadding: 5",
                "\t\t\tOverlay:", "\t\t\t\tId: A",
                "\t\t\t\tText: aaa bb");
        assertRect(40, 40, 515, 20, l.rect("R"));
        assertRect(45, 55, 30, 12, l.rect("A"));
    }

    @Test
    void autoSizeStretchesBetweenOffsets() {
        LayoutFixture l = layOut(
                "\tFrame:", "\t\tBox:", "\t\t\tHeight: 100",
                "\t\tOverlay:", "\t\t\tId: S", "\t\t\tOffset: 10 20 30 15");
        assertRect(55, 50, 480, 60, l.rect("S"));
    }

    @Test
    void autoMarginsCenter() {
        LayoutFixture l = layOut(
                "\tFrame:", "\t\tBox:", "\t\t\tHeight: 200",
                "\t\tOverlay:", "\t\t\tId: C", "\t\t\tOffset: 0", "\t\t\tBox:", "\t\t\t\tWidth: 100",
                "\t\t\t\tHeight: 50", "\t\t\t\tMargin: auto");
        assertRect(247.5, 115, 100, 50, l.rect("C"));
    }

    @Test
    void minAndMaxWidthLimitTheAutomaticWidth() {
        // El máximo convierte el estiramiento en un ancho fijo: sobre-restringida, se ignora la derecha.
        LayoutFixture l = layOut(
                "\tOverlay:", "\t\tId: min", "\t\tBox:", "\t\t\tMin width: 100",
                "\t\tText: aaa", "\tOverlay:", "\t\tId: max", "\t\tOffset: 0 0 auto 0", "\t\tBox:",
                "\t\t\tMax width: 200", "\t\t\tHeight: 10");
        assertRect(40, 40, 100, 12, l.rect("min"));
        assertRect(40, 40, 200, 10, l.rect("max"));
    }

    @Test
    void percentagesUseTheContainingBlock() {
        LayoutFixture l = layOut(
                "\tFrame:", "\t\tBox:", "\t\t\tWidth: 200", "\t\t\tHeight: 100",
                "\t\tOverlay:", "\t\t\tId: P", "\t\t\tOffset: 50% auto auto 25%", "\t\t\tBox:", "\t\t\t\tWidth: 50%",
                "\t\t\t\tHeight: 10%");
        assertRect(90, 90, 100, 10, l.rect("P"));
    }

    @Test
    void absoluteMovesWithItsRelativeContainingBlock() {
        LayoutFixture l = layOut(
                "\tFrame:", "\t\tOffset: 10 auto auto 20", "\t\tBox:", "\t\t\tHeight: 50",
                "\t\tOverlay:", "\t\t\tId: A", "\t\t\tOffset: 0 auto auto 0", "\t\t\tBox:", "\t\t\t\tWidth: 10",
                "\t\t\t\tHeight: 10");
        assertRect(60, 50, 10, 10, l.rect("A"));
    }

    @Test
    void nestedAbsoluteUsesTheNearestPositionedAncestor() {
        // B es absolute y por tanto containing block de C, aunque esté dentro de R.
        LayoutFixture l = layOut(
                "\tFrame:", "\t\tBox:", "\t\t\tHeight: 100",
                "\t\tOverlay:", "\t\t\tId: B", "\t\t\tOffset: 10 auto auto 10", "\t\t\tBox:", "\t\t\t\tWidth: 100",
                "\t\t\t\tHeight: 50", "\t\t\t\tBorder: 1",
                "\t\t\tOverlay:", "\t\t\t\tId: C", "\t\t\t\tOffset: auto 0 0 auto", "\t\t\t\tBox:",
                "\t\t\t\t\tWidth: 10", "\t\t\t\t\tHeight: 10");
        assertRect(50, 50, 100, 50, l.rect("B"));
        assertRect(139, 89, 10, 10, l.rect("C"));
    }

    @Test
    void fixedUsesThePageArea() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 100",
                "\tPage overlay:", "\t\tId: F", "\t\tOffset: auto 0 0 0", "\t\tBox:", "\t\t\tHeight: 20");
        assertRect(40, 782, 515, 20, l.rect("F"));
    }

    @Test
    void fixedIgnoresPositionedAncestors() {
        // G no tiene desplazamientos: queda en su posición estática, en la caja de contenido de R.
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 30",
                "\tFrame:", "\t\tBox:", "\t\t\tPadding: 10", "\t\t\tHeight: 100",
                "\t\tPage overlay:", "\t\t\tId: F", "\t\t\tOffset: 0 auto auto 0", "\t\t\tBox:", "\t\t\t\tWidth: 10",
                "\t\t\t\tHeight: 10",
                "\t\tPage overlay:", "\t\t\tId: G", "\t\t\tBox:", "\t\t\t\tWidth: 10", "\t\t\t\tHeight: 10");
        assertRect(40, 40, 10, 10, l.rect("F"));
        assertRect(50, 80, 10, 10, l.rect("G"));
    }

    // Casos que faltaban según las pruebas de mutación: el borde y el relleno de las cajas
    // entre un Overlay y su containing block, en un bloque, un ítem flex y un área de grid.

    @Test
    void staticPositionInsideABorderedBox() {
        // La caja de contenido de M empieza en (40 + 3 + 5, 40 + 3 + 5).
        String inner = "\t\t\tBox:\n\t\t\t\tBorder: 3\n\t\t\t\tPadding: 5\n\t\t\tOverlay:\n\t\t\t\tId: %s\n"
                + "\t\t\t\tText: aaa";
        LayoutFixture l = layOut(
                "\tFrame:", "\t\tBlock:", inner.formatted("A"));
        assertRect(48, 48, 15, 12, l.rect("A"));
    }

    @Test
    void staticPositionInsideABorderedFlexItemAndGridArea() {
        String inner = "\t\t\t\t\tBox:\n\t\t\t\t\t\tBorder: 3\n\t\t\t\t\t\tPadding: 5\n\t\t\t\t\tOverlay:\n"
                + "\t\t\t\t\t\tId: %s\n\t\t\t\t\t\tText: aaa";
        LayoutFixture flex = layOut("\tFrame:", "\t\tFlex:", "\t\t\tItem:", "\t\t\t\tBlock:", inner.formatted("F"));
        assertRect(48, 48, 15, 12, flex.rect("F"));
        LayoutFixture grid = layOut("\tFrame:", "\t\tGrid:", "\t\t\tColumn sizes: 100", "\t\t\tArea:", "\t\t\t\tBlock:",
                inner.formatted("G"));
        assertRect(48, 48, 15, 12, grid.rect("G"));
    }

    @Test
    void anAbsoluteMayLeaveItsContainingBlockButNotThePage() {
        // La caja de relleno de R empieza en (42, 42); A sale 42 por la izquierda y por arriba, hasta la esquina de la
        // página (0, 0), y no más: no es un desbordamiento (referencia/comun/desbordamiento.md).
        LayoutFixture l = layOut(
                "\tFrame:", "\t\tBox:", "\t\t\tHeight: 100", "\t\t\tBorder: 2", "\t\t\tPadding: 30",
                "\t\tOverlay:", "\t\t\tId: A", "\t\t\tOffset: -42 auto auto -42", "\t\t\tBox:", "\t\t\t\tWidth: 20",
                "\t\t\t\tHeight: 20");
        assertRect(0, 0, 20, 20, l.rect("A"));
    }

    @Test
    void staticPositionInsideAContainingBlockWithPadding() {
        // Sin Offset, A va en la caja de contenido de R: (40 + 3 + 7, 40 + 3 + 7).
        LayoutFixture l = layOut("\tFrame:", "\t\tBox:", "\t\t\tBorder: 3", "\t\t\tPadding: 7",
                "\t\tOverlay:", "\t\t\tId: A", "\t\t\tText: aaa");
        assertRect(50, 50, 15, 12, l.rect("A"));
    }

    @Test
    void staticPositionAfterABottomMargin() {
        // Detrás de un bloque de 10 con 6 de margen inferior: (40, 40 + 10 + 6).
        LayoutFixture l = layOut("\tFrame:", "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 10",
                "\t\t\t\tMargin: 0 0 6 0", "\t\tOverlay:", "\t\t\tId: A", "\t\t\tText: aaa");
        assertRect(40, 56, 15, 12, l.rect("A"));
    }

    @Test
    void shrinkToFitAndStretchLeaveTheMargins() {
        // F: el texto mide 30 (max-content), entre 10 de margen a cada lado. S: 100 − 10 − 20 − 5 − 7 de alto.
        LayoutFixture l = layOut("\tFrame:", "\t\tBox:", "\t\t\tHeight: 100",
                "\t\tOverlay:", "\t\t\tId: F", "\t\t\tOffset: 0 auto auto 0", "\t\t\tBox:", "\t\t\t\tMargin: 0 10",
                "\t\t\tText: aaa bb",
                "\t\tOverlay:", "\t\t\tId: S", "\t\t\tOffset: 10 auto 20 0", "\t\t\tBox:", "\t\t\t\tWidth: 50",
                "\t\t\t\tMargin: 5 0 7 0");
        assertRect(50, 40, 30, 12, l.rect("F"));
        assertRect(40, 55, 50, 58, l.rect("S"));
    }

    @Test
    void aFixedInsideABorderedAbsoluteStartsInItsContentBox() {
        // A, en (50, 50), tiene 3 de borde y 7 de relleno: F, sin Offset, empieza en (60, 60).
        LayoutFixture l = layOut("\tOverlay:", "\t\tOffset: 10 auto auto 10", "\t\tBox:", "\t\t\tWidth: 100",
                "\t\t\tHeight: 50", "\t\t\tBorder: 3", "\t\t\tPadding: 7",
                "\t\tPage overlay:", "\t\t\tId: F", "\t\t\tBox:", "\t\t\t\tWidth: 10", "\t\t\t\tHeight: 10");
        assertRect(60, 60, 10, 10, l.rect("F"));
    }

    @Test
    void autoMarginOnOneSide() {
        // A: 10 a la izquierda y el resto a la derecha. B: 10 a la derecha y el resto (305) a la izquierda.
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tWidth: 200", "\t\t\tHeight: 10", "\t\t\tMargin: 0 auto 0 10",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tWidth: 200", "\t\t\tHeight: 10", "\t\t\tMargin: 0 10 0 auto");
        assertRect(50, 40, 200, 10, l.rect("A"));
        assertRect(345, 50, 200, 10, l.rect("B"));
    }
}
