package dev.stxt.boxes.layout.position;

import dev.stxt.boxes.layout.LayoutFixture;
import dev.stxt.boxes.model.Rect;
import org.junit.jupiter.api.Test;

import static dev.stxt.boxes.layout.LayoutFixture.layOut;
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
                "\tRelative:", "\t\tId: R", "\t\tOffset: 5 auto auto 10", "\t\tBox:", "\t\t\tHeight: 20",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 10");
        assertRect(40, 40, 515, 10, l.rect("A"));
        assertRect(50, 55, 515, 20, l.rect("R"));
        assertRect(40, 70, 515, 10, l.rect("B"));
    }

    @Test
    void relativeOffsets() {
        // Arriba y abajo, o izquierda y derecha, a la vez son un error (catálogo de errores).
        LayoutFixture l = layOut(
                "\tRelative:", "\t\tId: start", "\t\tOffset: 5 auto auto 11", "\t\tBox:", "\t\t\tHeight: 20",
                "\tRelative:", "\t\tId: endOnly", "\t\tOffset: auto 7 9 auto", "\t\tBox:", "\t\t\tHeight: 20");
        assertRect(51, 45, 515, 20, l.rect("start"));
        assertRect(33, 51, 515, 20, l.rect("endOnly"));
    }

    @Test
    void relativePercentages() {
        // Izquierda: 10 % de 515. Arriba: 10 % del alto del padre (200). Un porcentaje vertical
        // con el alto del padre indefinido es un error (catálogo de errores).
        LayoutFixture l = layOut(
                "\tRelative:", "\t\tId: top", "\t\tOffset: auto auto auto 10%", "\t\tBox:", "\t\t\tHeight: 20",
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 200",
                "\t\tRelative:", "\t\t\tId: inner", "\t\t\tOffset: 10% auto auto auto", "\t\t\tBox:",
                "\t\t\t\tHeight: 10");
        assertRect(91.5, 40, 515, 20, l.rect("top"));
        assertRect(40, 80, 515, 10, l.rect("inner"));
    }

    @Test
    void absoluteWithoutPositionedAncestorUsesThePageArea() {
        // En modo continuo, el alto del área útil es el del flujo: 100.
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 100",
                "\tAbsolute:", "\t\tId: S", "\t\tOffset: 0 0 auto auto", "\t\tBox:", "\t\t\tWidth: 80",
                "\t\t\tHeight: 30",
                "\tAbsolute:", "\t\tId: F", "\t\tOffset: auto 0 0 auto", "\t\tBox:", "\t\t\tWidth: 80",
                "\t\t\tHeight: 30");
        assertRect(475, 40, 80, 30, l.rect("S"));
        assertRect(475, 110, 80, 30, l.rect("F"));
    }

    @Test
    void containingBlockIsThePaddingBoxOfThePositionedAncestor() {
        // Caja de relleno de R: (42, 42), 511 × 96.
        LayoutFixture l = layOut(
                "\tRelative:", "\t\tId: R", "\t\tBox:", "\t\t\tHeight: 100", "\t\t\tBorder: 2", "\t\t\tPadding: 10",
                "\t\tAbsolute:", "\t\t\tId: TL", "\t\t\tOffset: 0 auto auto 0", "\t\t\tBox:", "\t\t\t\tWidth: 50",
                "\t\t\t\tHeight: 20",
                "\t\tAbsolute:", "\t\t\tId: BR", "\t\t\tOffset: auto 0 0 auto", "\t\t\tBox:", "\t\t\t\tWidth: 50",
                "\t\t\t\tHeight: 20");
        assertRect(40, 40, 515, 100, l.rect("R"));
        assertRect(42, 42, 50, 20, l.rect("TL"));
        assertRect(503, 118, 50, 20, l.rect("BR"));
    }

    @Test
    void absoluteBoxesDoNotContributeToTheParentHeight() {
        LayoutFixture l = layOut(
                "\tRelative:", "\t\tId: R",
                "\t\tAbsolute:", "\t\t\tId: A", "\t\t\tBox:", "\t\t\t\tWidth: 10", "\t\t\t\tHeight: 50");
        assertRect(40, 40, 515, 0, l.rect("R"));
        assertRect(40, 40, 10, 50, l.rect("A"));
    }

    @Test
    void staticPositionAndShrinkToFit() {
        // A estaría en la caja de contenido de M: (40 + 5, 40 + 10 + 5). Su texto mide 30.
        LayoutFixture l = layOut(
                "\tRelative:", "\t\tId: R",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 10",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tPadding: 5",
                "\t\t\tAbsolute:", "\t\t\t\tId: A",
                "\t\t\t\tText: aaa bb");
        assertRect(40, 40, 515, 20, l.rect("R"));
        assertRect(45, 55, 30, 12, l.rect("A"));
    }

    @Test
    void autoSizeStretchesBetweenOffsets() {
        LayoutFixture l = layOut(
                "\tRelative:", "\t\tBox:", "\t\t\tHeight: 100",
                "\t\tAbsolute:", "\t\t\tId: S", "\t\t\tOffset: 10 20 30 15");
        assertRect(55, 50, 480, 60, l.rect("S"));
    }

    @Test
    void autoMarginsCenter() {
        LayoutFixture l = layOut(
                "\tRelative:", "\t\tBox:", "\t\t\tHeight: 200",
                "\t\tAbsolute:", "\t\t\tId: C", "\t\t\tOffset: 0", "\t\t\tBox:", "\t\t\t\tWidth: 100",
                "\t\t\t\tHeight: 50", "\t\t\t\tMargin: auto");
        assertRect(247.5, 115, 100, 50, l.rect("C"));
    }

    @Test
    void minAndMaxWidthLimitTheAutomaticWidth() {
        // El máximo convierte el estiramiento en un ancho fijo: sobre-restringida, se ignora la derecha.
        LayoutFixture l = layOut(
                "\tAbsolute:", "\t\tId: min", "\t\tBox:", "\t\t\tMin width: 100",
                "\t\tText: aaa", "\tAbsolute:", "\t\tId: max", "\t\tOffset: 0 0 auto 0", "\t\tBox:",
                "\t\t\tMax width: 200", "\t\t\tHeight: 10");
        assertRect(40, 40, 100, 12, l.rect("min"));
        assertRect(40, 40, 200, 10, l.rect("max"));
    }

    @Test
    void percentagesUseTheContainingBlock() {
        LayoutFixture l = layOut(
                "\tRelative:", "\t\tBox:", "\t\t\tWidth: 200", "\t\t\tHeight: 100",
                "\t\tAbsolute:", "\t\t\tId: P", "\t\t\tOffset: 50% auto auto 25%", "\t\t\tBox:", "\t\t\t\tWidth: 50%",
                "\t\t\t\tHeight: 10%");
        assertRect(90, 90, 100, 10, l.rect("P"));
    }

    @Test
    void absoluteMovesWithItsRelativeContainingBlock() {
        LayoutFixture l = layOut(
                "\tRelative:", "\t\tOffset: 10 auto auto 20", "\t\tBox:", "\t\t\tHeight: 50",
                "\t\tAbsolute:", "\t\t\tId: A", "\t\t\tOffset: 0 auto auto 0", "\t\t\tBox:", "\t\t\t\tWidth: 10",
                "\t\t\t\tHeight: 10");
        assertRect(60, 50, 10, 10, l.rect("A"));
    }

    @Test
    void nestedAbsoluteUsesTheNearestPositionedAncestor() {
        // B es absolute y por tanto containing block de C, aunque esté dentro de R.
        LayoutFixture l = layOut(
                "\tRelative:", "\t\tBox:", "\t\t\tHeight: 100",
                "\t\tAbsolute:", "\t\t\tId: B", "\t\t\tOffset: 10 auto auto 10", "\t\t\tBox:", "\t\t\t\tWidth: 100",
                "\t\t\t\tHeight: 50", "\t\t\t\tBorder: 1",
                "\t\t\tAbsolute:", "\t\t\t\tId: C", "\t\t\t\tOffset: auto 0 0 auto", "\t\t\t\tBox:",
                "\t\t\t\t\tWidth: 10", "\t\t\t\t\tHeight: 10");
        assertRect(50, 50, 100, 50, l.rect("B"));
        assertRect(139, 89, 10, 10, l.rect("C"));
    }

    @Test
    void fixedInContinuousModeUsesTheDocumentHeight() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 100",
                "\tFixed:", "\t\tId: F", "\t\tOffset: auto 0 0 0", "\t\tBox:", "\t\t\tHeight: 20");
        assertRect(40, 120, 515, 20, l.rect("F"));
    }

    @Test
    void fixedIgnoresPositionedAncestors() {
        // G no tiene desplazamientos: queda en su posición estática, en la caja de contenido de R.
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 30",
                "\tRelative:", "\t\tBox:", "\t\t\tPadding: 10", "\t\t\tHeight: 100",
                "\t\tFixed:", "\t\t\tId: F", "\t\t\tOffset: 0 auto auto 0", "\t\t\tBox:", "\t\t\t\tWidth: 10",
                "\t\t\t\tHeight: 10",
                "\t\tFixed:", "\t\t\tId: G", "\t\t\tBox:", "\t\t\t\tWidth: 10", "\t\t\t\tHeight: 10");
        assertRect(40, 40, 10, 10, l.rect("F"));
        assertRect(50, 80, 10, 10, l.rect("G"));
    }
}
