package dev.stxt.pdf.layout;

import dev.stxt.pdf.model.Alignment;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Value;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SizingTest {

    @Test
    void minWinsOverMax() {
        // Module 7: Width 50 % of 515 = 257.5; Max 250; Min 300 → 300.
        assertEquals(300, Sizing.constrain(257.5, 300, OptionalDouble.of(250)), 1e-9);
        assertEquals(250, Sizing.constrain(257.5, 0, OptionalDouble.of(250)), 1e-9);
    }

    @Test
    void module1Box() {
        // 515 of reference container, margin 20 → border box 475; content 451.
        double border = Sizing.autoWidthInFlow(515, Insets.all(20));
        assertEquals(475, border, 1e-9);
        assertEquals(451, border - 2 * 2 - 2 * 10, 1e-9);
    }

    @Test
    void module1Exercise() {
        // Width 50 % (border-box), padding 10 20, border 1 → content 215.5.
        double border = new Value.Percentage(50).resolve(515).getAsDouble();
        assertEquals(215.5, border - 2 - 40, 1e-9);
    }

    @Test
    void autoMarginsCenter() {
        // Module 2: C of 200 pt in 515 → 157.5 on each side.
        assertArrayEquals(new double[]{157.5, 157.5},
                Sizing.horizontalMargins(515, 200, Value.AUTO, Value.AUTO), 1e-9);
    }

    @Test
    void alignInOneAxis() {
        // A box of 30 in a space of 100; with stretch it already measures 100 and starts at the beginning.
        assertEquals(0, Sizing.align(Alignment.START, 100, 30), 1e-9);
        assertEquals(0, Sizing.align(Alignment.STRETCH, 100, 100), 1e-9);
        assertEquals(70, Sizing.align(Alignment.END, 100, 30), 1e-9);
        assertEquals(35, Sizing.align(Alignment.CENTER, 100, 30), 1e-9);
        // If it does not fit, it overflows on both sides (center) or at the start (end).
        assertEquals(-10, Sizing.align(Alignment.CENTER, 100, 120), 1e-9);
        assertEquals(-20, Sizing.align(Alignment.END, 100, 120), 1e-9);
    }

    @Test
    void sum() {
        assertEquals(0, Sizing.sum(new double[0]), 1e-9);
        assertEquals(6.5, Sizing.sum(new double[]{1, 2.5, 3}), 1e-9);
    }
}
