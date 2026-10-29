package dev.stxt.boxes.layout;

import dev.stxt.boxes.model.Alignment;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Value;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SizingTest {

    @Test
    void minWinsOverMax() {
        // Módulo 7: Width 50 % de 515 = 257.5; Max 250; Min 300 → 300.
        assertEquals(300, Sizing.constrain(257.5, 300, OptionalDouble.of(250)), 1e-9);
        assertEquals(250, Sizing.constrain(257.5, 0, OptionalDouble.of(250)), 1e-9);
    }

    @Test
    void module1Box() {
        // 515 de containing block, margen 20 → caja de borde 475; contenido 451.
        double border = Sizing.autoWidthInFlow(515, Insets.all(20));
        assertEquals(475, border, 1e-9);
        assertEquals(451, border - 2 * 2 - 2 * 10, 1e-9);
    }

    @Test
    void module1Exercise() {
        // Width 50 % (border-box), relleno 10 20, borde 1 → contenido 215.5.
        double border = new Value.Percentage(50).resolve(515).getAsDouble();
        assertEquals(215.5, border - 2 - 40, 1e-9);
    }

    @Test
    void autoMarginsCenter() {
        // Módulo 2: C de 200 pt en 515 → 157.5 a cada lado.
        assertArrayEquals(new double[]{157.5, 157.5},
                Sizing.horizontalMargins(515, 200, Value.AUTO, Value.AUTO), 1e-9);
    }

    @Test
    void alignInOneAxis() {
        // Una caja de 30 en un espacio de 100; con stretch ya mide 100 y empieza al principio.
        assertEquals(0, Sizing.align(Alignment.START, 100, 30), 1e-9);
        assertEquals(0, Sizing.align(Alignment.STRETCH, 100, 100), 1e-9);
        assertEquals(70, Sizing.align(Alignment.END, 100, 30), 1e-9);
        assertEquals(35, Sizing.align(Alignment.CENTER, 100, 30), 1e-9);
        // Si no cabe, se sale por los dos lados (center) o por el principio (end).
        assertEquals(-10, Sizing.align(Alignment.CENTER, 100, 120), 1e-9);
        assertEquals(-20, Sizing.align(Alignment.END, 100, 120), 1e-9);
    }

    @Test
    void sum() {
        assertEquals(0, Sizing.sum(new double[0]), 1e-9);
        assertEquals(6.5, Sizing.sum(new double[]{1, 2.5, 3}), 1e-9);
    }
}
