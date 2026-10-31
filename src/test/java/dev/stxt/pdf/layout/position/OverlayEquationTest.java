package dev.stxt.pdf.layout.position;

import dev.stxt.pdf.layout.IntrinsicSizes;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverlayEquationTest {

    private static final OptionalDouble AUTO = OptionalDouble.empty();
    private static final IntrinsicSizes CONTENT = new IntrinsicSizes(80, 180);

    private static OptionalDouble v(double x) {
        return OptionalDouble.of(x);
    }

    @Test
    void module4ExerciseAnchoredRight() {
        OverlayEquation.Solution s = OverlayEquation.horizontal(AUTO, v(180), v(0), AUTO, AUTO, 400, 20, CONTENT);
        assertEquals(220, s.start(), 1e-9);
    }

    @Test
    void autoRightIsSolved() {
        OverlayEquation.Solution s = OverlayEquation.horizontal(v(40), v(180), AUTO, AUTO, AUTO, 400, 20, CONTENT);
        assertEquals(180, s.end(), 1e-9);
    }

    @Test
    void allAutoUsesStaticPositionAndFit() {
        OverlayEquation.Solution s = OverlayEquation.horizontal(AUTO, AUTO, AUTO, AUTO, AUTO, 400, 20, CONTENT);
        assertEquals(20, s.start(), 1e-9);
        assertEquals(180, s.size(), 1e-9);
    }

    @Test
    void autoWidthBetweenLeftAndRightStretches() {
        OverlayEquation.Solution s = OverlayEquation.horizontal(v(10), AUTO, v(10), AUTO, AUTO, 400, 0, CONTENT);
        assertEquals(380, s.size(), 1e-9);
    }

    @Test
    void autoMarginsCenter() {
        OverlayEquation.Solution s = OverlayEquation.horizontal(v(0), v(200), v(0), AUTO, AUTO, 400, 0, CONTENT);
        assertEquals(100, s.marginStart(), 1e-9);
        assertEquals(100, s.marginEnd(), 1e-9);
    }

    @Test
    void overConstrainedIgnoresEnd() {
        OverlayEquation.Solution s = OverlayEquation.horizontal(v(50), v(200), v(50), v(0), v(0), 400, 0, CONTENT);
        assertEquals(150, s.end(), 1e-9);
    }

    /** The five values of the solution, and that they add up to the reference container. */
    private static void assertSolution(OverlayEquation.Solution s, double cb, double start, double marginStart,
                                       double size, double marginEnd, double end) {
        assertEquals(new OverlayEquation.Solution(start, marginStart, size, marginEnd, end), s);
        assertEquals(cb, s.start() + s.marginStart() + s.size() + s.marginEnd() + s.end(), 1e-9);
    }

    /**
     * Each case of the equation (reference/components/frame-and-overlays.md) with margins other than 0 and all
     * the values checked (mutation testing: the tests above look at one value and with zero margins).
     */
    @Test
    void everyCaseWithMargins() {
        double cb = 300;
        OptionalDouble ms = v(7);
        OptionalDouble me = v(11);
        // All auto: static position; the size, the content (vertical) or what is available if it fits (horizontal).
        assertSolution(OverlayEquation.vertical(AUTO, AUTO, AUTO, ms, me, cb, 20, 45), cb, 20, 7, 45, 11, 217);
        assertSolution(OverlayEquation.horizontal(AUTO, AUTO, AUTO, ms, me, cb, 20, new IntrinsicSizes(80, 400)),
                cb, 20, 7, 262, 11, 0);
        // Nothing auto: the auto margins share the leftover (or the end absorbs it), or the end is ignored.
        assertSolution(OverlayEquation.vertical(v(30), v(100), v(50), AUTO, AUTO, cb, 20, 45), cb, 30, 60, 100, 60, 50);
        assertSolution(OverlayEquation.vertical(v(30), v(250), v(50), AUTO, AUTO, cb, 20, 45), cb, 30, 0, 250, -30, 50);
        assertSolution(OverlayEquation.vertical(v(30), v(100), v(50), AUTO, me, cb, 20, 45), cb, 30, 109, 100, 11, 50);
        assertSolution(OverlayEquation.vertical(v(30), v(100), v(50), ms, AUTO, cb, 20, 45), cb, 30, 7, 100, 113, 50);
        assertSolution(OverlayEquation.vertical(v(30), v(100), v(50), ms, me, cb, 20, 45), cb, 30, 7, 100, 11, 152);
        // Start and size auto: the size of the content, against the end.
        assertSolution(OverlayEquation.vertical(AUTO, AUTO, v(50), ms, me, cb, 20, 45), cb, 187, 7, 45, 11, 50);
        assertSolution(OverlayEquation.horizontal(AUTO, AUTO, v(50), ms, me, cb, 20, CONTENT), cb, 52, 7, 180, 11, 50);
        assertSolution(OverlayEquation.horizontal(AUTO, AUTO, v(50), ms, me, cb, 20, new IntrinsicSizes(80, 400)),
                cb, 0, 7, 232, 11, 50);
        // Start and end auto: static position.
        assertSolution(OverlayEquation.vertical(AUTO, v(100), AUTO, ms, me, cb, 20, 45), cb, 20, 7, 100, 11, 162);
        // Size and end auto: the size of the content from the start.
        assertSolution(OverlayEquation.vertical(v(30), AUTO, AUTO, ms, me, cb, 20, 45), cb, 30, 7, 45, 11, 207);
        assertSolution(OverlayEquation.horizontal(v(30), AUTO, AUTO, ms, me, cb, 20, CONTENT), cb, 30, 7, 180, 11, 72);
        assertSolution(OverlayEquation.horizontal(v(30), AUTO, AUTO, ms, me, cb, 20, new IntrinsicSizes(80, 400)),
                cb, 30, 7, 252, 11, 0);
        // Only one auto: it is solved for.
        assertSolution(OverlayEquation.vertical(AUTO, v(100), v(50), ms, me, cb, 20, 45), cb, 132, 7, 100, 11, 50);
        assertSolution(OverlayEquation.vertical(v(30), AUTO, v(50), ms, me, cb, 20, 45), cb, 30, 7, 202, 11, 50);
        assertSolution(OverlayEquation.vertical(v(30), v(100), AUTO, ms, me, cb, 20, 45), cb, 30, 7, 100, 11, 152);
        // A negative solved size is 0, and the end gives way.
        assertSolution(OverlayEquation.vertical(v(200), AUTO, v(150), ms, me, cb, 20, 45), cb, 200, 7, 0, 11, 82);
    }

    @Test
    void verticalUsesContentHeight() {
        OverlayEquation.Solution s = OverlayEquation.vertical(AUTO, AUTO, v(0), AUTO, AUTO, 300, 0, 45);
        assertEquals(45, s.size(), 1e-9);
        assertEquals(255, s.start(), 1e-9);
    }
}
