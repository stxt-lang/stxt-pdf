package dev.stxt.pdf.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rounded corners already resolved (reference/common/box.md). */
class RadiiTest {

    private static Corners<Value> radius(Value... v) {
        return Corners.shorthand(List.of(v));
    }

    @Test
    void shorthandFollowsCss() {
        Value a = Value.pt(1);
        Value b = Value.pt(2);
        Value c = Value.pt(3);
        Value d = Value.pt(4);
        assertEquals(new Corners<>(a, a, a, a), Corners.shorthand(List.of(a)));
        assertEquals(new Corners<>(a, b, a, b), Corners.shorthand(List.of(a, b)));
        assertEquals(new Corners<>(a, b, c, b), Corners.shorthand(List.of(a, b, c)));
        assertEquals(new Corners<>(a, b, c, d), Corners.shorthand(List.of(a, b, c, d)));
    }

    @Test
    void pointsAreTheSameInBothAxes() {
        Radii r = Radii.resolve(Corners.all(Value.pt(8)), 100, 40);
        assertEquals(new Radii.Corner(8, 8), r.topLeft());
        assertEquals(new Radii.Corner(8, 8), r.bottomRight());
    }

    @Test
    void aPercentageIsOfTheWidthHorizontallyAndOfTheHeightVertically() {
        Radii r = Radii.resolve(Corners.all(new Value.Percentage(50)), 100, 40);
        assertEquals(new Radii.Corner(50, 20), r.topLeft());
        // Equal width and height: a circle.
        Radii circle = Radii.resolve(Corners.all(new Value.Percentage(50)), 60, 60);
        assertEquals(new Radii.Corner(30, 30), circle.topRight());
    }

    @Test
    void radiiThatDoNotFitAreScaledTogether() {
        // 40 + 40 = 80 on a side of 50: all of them times 50/80, not only the one that exceeds it.
        Radii r = Radii.resolve(radius(Value.pt(40), Value.pt(40), Value.pt(10), Value.pt(10)), 50, 200);
        assertEquals(25, r.topLeft().rx(), 1e-9);
        assertEquals(25, r.topRight().rx(), 1e-9);
        assertEquals(6.25, r.bottomRight().rx(), 1e-9);
        assertEquals(6.25, r.bottomLeft().ry(), 1e-9);
    }

    @Test
    void aCornerWithoutOneOfItsRadiiIsSquare() {
        assertTrue(Radii.Corner.of(0, 5).isSquare());
        assertEquals(Radii.Corner.SQUARE, Radii.Corner.of(5, 0));
        assertTrue(Radii.resolve(Corners.all(Value.pt(0)), 10, 10).isSquare());
        assertFalse(Radii.resolve(radius(Value.pt(0), Value.pt(2), Value.pt(0), Value.pt(0)), 10, 10).isSquare());
    }

    @Test
    void insetLosesTheWidthOfTheSidesItTouches() {
        Radii r = Radii.resolve(Corners.all(Value.pt(10)), 100, 100);
        Radii inner = r.inset(new Insets(1, 2, 3, 4));
        assertEquals(new Radii.Corner(6, 9), inner.topLeft());
        assertEquals(new Radii.Corner(8, 9), inner.topRight());
        assertEquals(new Radii.Corner(8, 7), inner.bottomRight());
        assertEquals(new Radii.Corner(6, 7), inner.bottomLeft());
    }

    @Test
    void insetBeyondTheRadiusIsSquare() {
        Radii inner = Radii.resolve(Corners.all(Value.pt(3)), 100, 100).inset(Insets.all(3));
        assertTrue(inner.isSquare());
    }

    @Test
    void aFragmentOnlyKeepsTheCornersOfItsEdges() {
        Radii r = Radii.resolve(Corners.all(Value.pt(5)), 100, 100);
        Radii first = r.forFragment(true, false);
        assertFalse(first.topLeft().isSquare());
        assertTrue(first.bottomLeft().isSquare());
        Radii last = r.forFragment(false, true);
        assertTrue(last.topRight().isSquare());
        assertFalse(last.bottomRight().isSquare());
        assertTrue(r.forFragment(false, false).isSquare());
        assertEquals(r, r.forFragment(true, true));
    }

    @Test
    void noneIsSquare() {
        assertTrue(Radii.NONE.isSquare());
        assertSame(Radii.Corner.SQUARE, Radii.NONE.topLeft());
    }
}
