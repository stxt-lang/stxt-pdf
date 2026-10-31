package dev.stxt.pdf.layout.stack;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class StackResolverTest {

    private static final double AVAILABLE = 515 - 2 * 20; // contenedor 515, dos huecos de 20

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
        // Faltan 100 pt; B cedería 50 y bajaría a 150, por debajo de su mínimo 160:
        // se congela en 160 y A absorbe el resto.
        double[] r = StackResolver.resolve(List.of(
                StackItem.of(200, 0, 1, 50),
                StackItem.of(200, 0, 1, 160)), 300);
        assertArrayEquals(new double[]{140, 160}, r, 1e-9);
    }

    @Test
    void factorsSummingLessThanOne() {
        // Solo se reparte la mitad del espacio libre.
        double[] r = StackResolver.resolve(List.of(StackItem.of(100, 0.5, 1, 0)), 300);
        assertArrayEquals(new double[]{200}, r, 1e-9);
    }

    /** Al crecer, A llega a su máximo (150): se congela y B y C se reparten el resto (175 cada uno). */
    @Test
    void anItemAtItsMaximumLeavesTheRestToTheOthers() {
        double[] r = StackResolver.resolve(List.of(
                new StackItem(100, 1, 1, 0, 150, 0),
                StackItem.of(100, 1, 1, 0),
                StackItem.of(100, 1, 1, 0)), 500);
        assertArrayEquals(new double[]{150, 175, 175}, r, 1e-9);
    }

    /** Al encoger, A no baja de su mínimo (250): B y C ceden el resto (175 cada uno). */
    @Test
    void anItemAtItsMinimumLeavesTheRestToTheOthers() {
        double[] r = StackResolver.resolve(List.of(
                StackItem.of(300, 0, 1, 250),
                StackItem.of(300, 0, 1, 0),
                StackItem.of(300, 0, 1, 0)), 600);
        assertArrayEquals(new double[]{250, 175, 175}, r, 1e-9);
    }
}
