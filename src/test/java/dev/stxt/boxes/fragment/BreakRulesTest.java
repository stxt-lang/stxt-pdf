package dev.stxt.boxes.fragment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BreakRulesTest {

    @Test
    void module8Exercise() {
        // Área útil 762, interlineado 15, párrafo de 53 líneas que empieza en y = 720.
        int capacity1 = BreakRules.capacity(762 - 720, 15);
        assertEquals(2, capacity1);
        int p1 = BreakRules.linesThatFit(53, capacity1, 2, 2, false);
        int p2 = BreakRules.linesThatFit(53 - p1, BreakRules.capacity(762, 15), 2, 2, true);
        int p3 = BreakRules.linesThatFit(53 - p1 - p2, BreakRules.capacity(762, 15), 2, 2, true);
        assertEquals(2, p1);
        assertEquals(49, p2);
        assertEquals(2, p3);
    }

    @Test
    void orphanMovesTheWholeParagraph() {
        assertEquals(0, BreakRules.linesThatFit(10, 1, 2, 2, false));
    }

    @Test
    void progressGuaranteeAtPageStart() {
        assertEquals(1, BreakRules.linesThatFit(10, 1, 2, 2, true));
    }

    @Test
    void continuousHasNoLimit() {
        assertEquals(Integer.MAX_VALUE, BreakRules.capacity(Double.POSITIVE_INFINITY, 15));
    }
}
