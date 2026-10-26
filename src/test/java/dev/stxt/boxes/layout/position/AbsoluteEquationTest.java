package dev.stxt.boxes.layout.position;

import dev.stxt.boxes.layout.IntrinsicSizes;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbsoluteEquationTest {

    private static final OptionalDouble AUTO = OptionalDouble.empty();
    private static final IntrinsicSizes CONTENT = new IntrinsicSizes(80, 180);

    private static OptionalDouble v(double x) {
        return OptionalDouble.of(x);
    }

    @Test
    void module4ExerciseAnchoredRight() {
        AbsoluteEquation.Solution s = AbsoluteEquation.horizontal(AUTO, v(180), v(0), AUTO, AUTO, 400, 20, CONTENT);
        assertEquals(220, s.start(), 1e-9);
    }

    @Test
    void autoRightIsSolved() {
        AbsoluteEquation.Solution s = AbsoluteEquation.horizontal(v(40), v(180), AUTO, AUTO, AUTO, 400, 20, CONTENT);
        assertEquals(180, s.end(), 1e-9);
    }

    @Test
    void allAutoUsesStaticPositionAndFitContent() {
        AbsoluteEquation.Solution s = AbsoluteEquation.horizontal(AUTO, AUTO, AUTO, AUTO, AUTO, 400, 20, CONTENT);
        assertEquals(20, s.start(), 1e-9);
        assertEquals(180, s.size(), 1e-9);
    }

    @Test
    void autoWidthBetweenLeftAndRightStretches() {
        AbsoluteEquation.Solution s = AbsoluteEquation.horizontal(v(10), AUTO, v(10), AUTO, AUTO, 400, 0, CONTENT);
        assertEquals(380, s.size(), 1e-9);
    }

    @Test
    void autoMarginsCenter() {
        AbsoluteEquation.Solution s = AbsoluteEquation.horizontal(v(0), v(200), v(0), AUTO, AUTO, 400, 0, CONTENT);
        assertEquals(100, s.marginStart(), 1e-9);
        assertEquals(100, s.marginEnd(), 1e-9);
    }

    @Test
    void overConstrainedIgnoresEnd() {
        AbsoluteEquation.Solution s = AbsoluteEquation.horizontal(v(50), v(200), v(50), v(0), v(0), 400, 0, CONTENT);
        assertEquals(150, s.end(), 1e-9);
    }

    @Test
    void verticalUsesContentHeight() {
        AbsoluteEquation.Solution s = AbsoluteEquation.vertical(AUTO, AUTO, v(0), AUTO, AUTO, 300, 0, 45);
        assertEquals(45, s.size(), 1e-9);
        assertEquals(255, s.start(), 1e-9);
    }
}
