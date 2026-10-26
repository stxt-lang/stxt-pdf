package dev.stxt.boxes.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarginStrutTest {

    @Test
    void module2Cases() {
        assertEquals(30, MarginStrut.collapse(20, 30), 1e-9);
        assertEquals(15, MarginStrut.collapse(20, -5), 1e-9);
        assertEquals(-10, MarginStrut.collapse(-10, -4), 1e-9);
    }

    @Test
    void emptyBoxBetweenBAndC() {
        // B (inferior 0), D vacía (25 y 15), C (superior 10): colapsan 4 márgenes → 25.
        assertEquals(25, MarginStrut.collapse(0, 25, 15, 10), 1e-9);
    }

    @Test
    void resolveAndReset() {
        MarginStrut s = new MarginStrut();
        assertTrue(s.isEmpty());
        s.add(20);
        s.add(30);
        assertEquals(30, s.resolveAndReset(), 1e-9);
        assertTrue(s.isEmpty());
        assertEquals(0, s.resolve(), 1e-9);
    }
}
