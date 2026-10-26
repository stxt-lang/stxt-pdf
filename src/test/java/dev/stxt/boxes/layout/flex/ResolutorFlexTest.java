package dev.stxt.boxes.layout.flex;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ResolutorFlexTest {

    private static final double DISPONIBLE = 515 - 2 * 20; // contenedor 515, dos huecos de 20

    @Test
    void crecimientoDelModulo5() {
        double[] r = ResolutorFlex.resolver(List.of(
                ItemFlex.de(100, 1, 1, 60),
                ItemFlex.de(150, 2, 1, 80),
                ItemFlex.de(65, 0, 0, 40)), DISPONIBLE);
        assertArrayEquals(new double[]{153.3333, 256.6667, 65}, r, 1e-3);
    }

    @Test
    void encogimientoProporcionalABase() {
        double[] r = ResolutorFlex.resolver(List.of(
                ItemFlex.de(300, 0, 1, 60),
                ItemFlex.de(200, 0, 1, 80),
                ItemFlex.de(100, 0, 0, 40)), DISPONIBLE);
        assertArrayEquals(new double[]{225, 150, 100}, r, 1e-9);
    }

    @Test
    void ejercicioBaseCero() {
        double[] r = ResolutorFlex.resolver(List.of(
                ItemFlex.de(0, 1, 1, 0),
                ItemFlex.de(0, 1, 1, 0),
                ItemFlex.de(0, 2, 1, 0)), DISPONIBLE);
        assertArrayEquals(new double[]{118.75, 118.75, 237.5}, r, 1e-9);
    }

    @Test
    void congelacionEnElMinimo() {
        // Faltan 100 pt; B cedería 50 y bajaría a 150, por debajo de su mínimo 160:
        // se congela en 160 y A absorbe el resto.
        double[] r = ResolutorFlex.resolver(List.of(
                ItemFlex.de(200, 0, 1, 50),
                ItemFlex.de(200, 0, 1, 160)), 300);
        assertArrayEquals(new double[]{140, 160}, r, 1e-9);
    }

    @Test
    void factoresQueSumanMenosDeUno() {
        // Solo se reparte la mitad del espacio libre.
        double[] r = ResolutorFlex.resolver(List.of(ItemFlex.de(100, 0.5, 1, 0)), 300);
        assertArrayEquals(new double[]{200}, r, 1e-9);
    }
}
