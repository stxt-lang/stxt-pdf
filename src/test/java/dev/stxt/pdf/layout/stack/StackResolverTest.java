package dev.stxt.pdf.layout.stack;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class StackResolverTest {

    private static final double AVAILABLE = 515 - 2 * 20; // container 515, two gaps of 20

    @Test
    void module5Growth() {
        double[] r = StackResolver.resolve(List.of(
                StackItem.of(100, 1, 1, 60),
                StackItem.of(150, 2, 1, 80),
                StackItem.of(65, 0, 0, 40)), AVAILABLE);
        assertArrayEquals(new double[]{153.3333, 256.6667, 65}, r, 1e-3);
    }

    @Test
    void shrinkProportionalToBasis() {
        double[] r = StackResolver.resolve(List.of(
                StackItem.of(300, 0, 1, 60),
                StackItem.of(200, 0, 1, 80),
                StackItem.of(100, 0, 0, 40)), AVAILABLE);
        assertArrayEquals(new double[]{225, 150, 100}, r, 1e-9);
    }

    @Test
    void zeroBasisExercise() {
        double[] r = StackResolver.resolve(List.of(
                StackItem.of(0, 1, 1, 0),
                StackItem.of(0, 1, 1, 0),
                StackItem.of(0, 2, 1, 0)), AVAILABLE);
        assertArrayEquals(new double[]{118.75, 118.75, 237.5}, r, 1e-9);
    }

    @Test
    void freezesAtMinimum() {
        // 100 pt are missing; B would give up 50 and go down to 150, below its minimum 160:
        // it freezes at 160 and A absorbs the rest.
        double[] r = StackResolver.resolve(List.of(
                StackItem.of(200, 0, 1, 50),
                StackItem.of(200, 0, 1, 160)), 300);
        assertArrayEquals(new double[]{140, 160}, r, 1e-9);
    }

    @Test
    void factorsSummingLessThanOne() {
        // Only half of the free space is distributed.
        double[] r = StackResolver.resolve(List.of(StackItem.of(100, 0.5, 1, 0)), 300);
        assertArrayEquals(new double[]{200}, r, 1e-9);
    }

    /** When growing, A reaches its maximum (150): it freezes and B and C share the rest (175 each). */
    @Test
    void anItemAtItsMaximumLeavesTheRestToTheOthers() {
        double[] r = StackResolver.resolve(List.of(
                new StackItem(100, 1, 1, 0, 150, 0),
                StackItem.of(100, 1, 1, 0),
                StackItem.of(100, 1, 1, 0)), 500);
        assertArrayEquals(new double[]{150, 175, 175}, r, 1e-9);
    }

    /** When shrinking, A does not go below its minimum (250): B and C give up the rest (175 each). */
    @Test
    void anItemAtItsMinimumLeavesTheRestToTheOthers() {
        double[] r = StackResolver.resolve(List.of(
                StackItem.of(300, 0, 1, 250),
                StackItem.of(300, 0, 1, 0),
                StackItem.of(300, 0, 1, 0)), 600);
        assertArrayEquals(new double[]{250, 175, 175}, r, 1e-9);
    }
}
