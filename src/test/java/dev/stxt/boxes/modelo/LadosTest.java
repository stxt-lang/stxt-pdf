package dev.stxt.boxes.modelo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LadosTest {

    @Test
    void abreviadoComoCss() {
        assertEquals(new Lados<>(10, 20, 5, 20), Lados.abreviado(List.of(10, 20, 5)));
        assertEquals(new Lados<>(10, 20, 10, 20), Lados.abreviado(List.of(10, 20)));
        assertThrows(IllegalArgumentException.class, () -> Lados.abreviado(List.of(1, 2, 3, 4, 5)));
    }

    @Test
    void porcentajesVerticalesSobreElAncho() {
        // Módulo 1: los porcentajes de margen y relleno, también verticales, usan el ancho.
        Insets i = Insets.resolver(Lados.abreviado(ValorParser.lista("10% auto")), 200);
        assertEquals(new Insets(20, 0, 20, 0), i);
    }
}
