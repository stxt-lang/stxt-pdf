package dev.stxt.pdf.model;

import org.junit.jupiter.api.Test;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The stack of the rotations a painting is inside (reference/common/layers-and-rotation.md). */
class TurnsTest {

    private static final double E = 1e-9;

    @Test
    void withoutRotationsTheEnvelopeIsTheRectangleItself() {
        Turns turns = new Turns();
        Rect r = new Rect(10, 20, 30, 40);
        assertSame(r, turns.envelope(r));
        assertTrue(turns.current().isIdentity());
    }

    /** A positive angle rotates clockwise as seen, with y pointing down: (10, 0) goes to (0, 10). */
    @Test
    void eachRotationIsInsideTheOnesBeforeAndPopRestoresThem() {
        Turns turns = new Turns();
        turns.push(90, 0, 0);
        assertPoint(0, 10, turns, 10, 0);
        turns.push(90, 0, 0);
        assertPoint(-10, 0, turns, 10, 0);
        turns.pop();
        assertPoint(0, 10, turns, 10, 0);
        turns.pop();
        assertTrue(turns.current().isIdentity());
    }

    @Test
    void theEnvelopeEnclosesTheRotatedRectangle() {
        Turns turns = new Turns();
        turns.push(90, 20, 10);
        Rect e = turns.envelope(new Rect(0, 0, 40, 20));
        assertEquals(10, e.x(), E);
        assertEquals(-10, e.y(), E);
        assertEquals(20, e.width(), E);
        assertEquals(40, e.height(), E);
    }

    /** What the destinations composed by hand before: the rotation of the box around its origin, after its ancestors. */
    @Test
    void aPushIsTheRotationOfTheBoxAfterThoseOfItsAncestors() {
        Rect outer = new Rect(0, 0, 200, 100);
        Rect inner = new Rect(30, 40, 50, 20);
        Rotation first = new Rotation(-8, 0.5, 0.5);
        Rotation second = new Rotation(45, 0, 1);
        AffineTransform expected = first.transform(outer);
        expected.concatenate(second.transform(inner));
        Turns turns = new Turns();
        turns.push(first.degrees(), first.pivotX(outer), first.pivotY(outer));
        turns.push(second.degrees(), second.pivotX(inner), second.pivotY(inner));
        double[] want = new double[6];
        double[] got = new double[6];
        expected.getMatrix(want);
        turns.current().getMatrix(got);
        for (int i = 0; i < 6; i++) {
            assertEquals(want[i], got[i], E);
        }
    }

    @Test
    void theCurrentRotationsAreACopy() {
        Turns turns = new Turns();
        turns.current().rotate(1);
        assertTrue(turns.current().isIdentity());
    }

    @Test
    void popWithoutPushIsAnInternalMisuse() {
        assertThrows(IllegalStateException.class, () -> new Turns().pop());
    }

    private static void assertPoint(double x, double y, Turns turns, double fromX, double fromY) {
        Point2D p = Rotation.point(turns.current(), fromX, fromY);
        assertEquals(x, p.getX(), E);
        assertEquals(y, p.getY(), E);
    }
}
