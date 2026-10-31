package dev.stxt.pdf.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The fill of a background (reference/common/box.md): solid colors and gradients. */
class PaintTest {

    private static final Rect BOX = new Rect(10, 20, 100, 50);

    private static void assertLine(double[] expected, double[] actual) {
        assertArrayEquals(expected, actual, 1e-9);
    }

    @Test
    void zeroDegreesGoesUpAndTheLineCrossesTheCenter() {
        // Center (60, 45); the gradient spans the height: it goes from the bottom (70) to the top (20).
        assertLine(new double[]{60, 70, 60, 20}, new Paint.Linear(0, List.of("#000", "#FFF")).line(BOX));
    }

    @Test
    void ninetyDegreesGoesRightAndMeasuresTheWidth() {
        assertLine(new double[]{10, 45, 110, 45}, new Paint.Linear(90, List.of("#000", "#FFF")).line(BOX));
    }

    @Test
    void oneEightyDegreesGoesDown() {
        assertLine(new double[]{60, 20, 60, 70}, new Paint.Linear(180, List.of("#000", "#FFF")).line(BOX));
    }

    @Test
    void aDiagonalReachesTheCornersWithTheFirstAndLastColor() {
        // 45°: the line measures |w sin| + |h cos| = (100 + 50) · 0.7071; the center is halfway.
        double[] line = new Paint.Linear(45, List.of("#000", "#FFF")).line(BOX);
        double half = (100 + 50) * Math.sqrt(0.5) / 2;
        assertLine(new double[]{60 - half * Math.sqrt(0.5), 45 + half * Math.sqrt(0.5),
                60 + half * Math.sqrt(0.5), 45 - half * Math.sqrt(0.5)}, line);
    }

    @Test
    void translucencyLooksAtEveryColor() {
        assertFalse(new Paint.Solid("#112233").isTranslucent());
        assertTrue(new Paint.Solid("#11223380").isTranslucent());
        assertFalse(new Paint.Linear(90, List.of("#000", "#FFF")).isTranslucent());
        assertTrue(new Paint.Linear(90, List.of("#000", "#FFF0")).isTranslucent());
    }
}
