package dev.stxt.pdf.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarginStrutTest {

    @Test
    void module2Cases() {
        assertEquals(30, MarginStrut.collapse(20, 30), 1e-9);
        assertEquals(15, MarginStrut.collapse(20, -5), 1e-9);
        assertEquals(-10, MarginStrut.collapse(-10, -4), 1e-9);
    }

    @Test
    void emptyBoxBetweenBAndC() {
        // B (bottom 0), empty D (25 and 15), C (top 10): 4 margins merge → 25.
        assertEquals(25, MarginStrut.collapse(0, 25, 15, 10), 1e-9);
    }

    @Test
    void keepsPositiveAndNegativeApart() {
        // 20 and −5 give 15; adding −10 gives 20 − 10 = 10, not 15 − 10.
        MarginStrut s = MarginStrut.EMPTY.add(20).add(-5);
        assertEquals(15, s.resolve(), 1e-9);
        assertEquals(10, s.add(-10).resolve(), 1e-9);
        assertEquals(10, s.add(MarginStrut.EMPTY.add(-10).add(8)).resolve(), 1e-9);
        assertEquals(0, MarginStrut.EMPTY.resolve(), 1e-9);
    }
}
