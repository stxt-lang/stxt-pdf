package dev.stxt.boxes.layout.flex;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class FlexResolverTest {

    private static final double AVAILABLE = 515 - 2 * 20; // contenedor 515, dos huecos de 20

    @Test
    void module5Growth() {
        double[] r = FlexResolver.resolve(List.of(
                FlexItem.of(100, 1, 1, 60),
                FlexItem.of(150, 2, 1, 80),
                FlexItem.of(65, 0, 0, 40)), AVAILABLE);
        assertArrayEquals(new double[]{153.3333, 256.6667, 65}, r, 1e-3);
    }

    @Test
    void shrinkProportionalToBasis() {
        double[] r = FlexResolver.resolve(List.of(
                FlexItem.of(300, 0, 1, 60),
                FlexItem.of(200, 0, 1, 80),
                FlexItem.of(100, 0, 0, 40)), AVAILABLE);
        assertArrayEquals(new double[]{225, 150, 100}, r, 1e-9);
    }

    @Test
    void zeroBasisExercise() {
        double[] r = FlexResolver.resolve(List.of(
                FlexItem.of(0, 1, 1, 0),
                FlexItem.of(0, 1, 1, 0),
                FlexItem.of(0, 2, 1, 0)), AVAILABLE);
        assertArrayEquals(new double[]{118.75, 118.75, 237.5}, r, 1e-9);
    }

    @Test
    void freezesAtMinimum() {
        // Faltan 100 pt; B cedería 50 y bajaría a 150, por debajo de su mínimo 160:
        // se congela en 160 y A absorbe el resto.
        double[] r = FlexResolver.resolve(List.of(
                FlexItem.of(200, 0, 1, 50),
                FlexItem.of(200, 0, 1, 160)), 300);
        assertArrayEquals(new double[]{140, 160}, r, 1e-9);
    }

    @Test
    void factorsSummingLessThanOne() {
        // Solo se reparte la mitad del espacio libre.
        double[] r = FlexResolver.resolve(List.of(FlexItem.of(100, 0.5, 1, 0)), 300);
        assertArrayEquals(new double[]{200}, r, 1e-9);
    }
}
