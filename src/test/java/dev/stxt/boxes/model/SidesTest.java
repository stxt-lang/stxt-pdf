package dev.stxt.boxes.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SidesTest {

    @Test
    void shorthandLikeCss() {
        assertEquals(new Sides<>(10, 20, 5, 20), Sides.shorthand(List.of(10, 20, 5)));
        assertEquals(new Sides<>(10, 20, 10, 20), Sides.shorthand(List.of(10, 20)));
        assertThrows(IllegalArgumentException.class, () -> Sides.shorthand(List.of(1, 2, 3, 4, 5)));
    }

    @Test
    void verticalPercentagesUseTheWidth() {
        // Módulo 1: los porcentajes de margen y relleno, también verticales, usan el ancho.
        Insets i = Insets.resolve(Sides.shorthand(ValueParser.list("10% auto")), 200);
        assertEquals(new Insets(20, 0, 20, 0), i);
    }
}
