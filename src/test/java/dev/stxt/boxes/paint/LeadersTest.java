package dev.stxt.boxes.paint;

import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.tree.Style;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeadersTest {

    private static final Rect CONTENT = new Rect(100, 20, 50, 12);

    @Test
    void dotsStickToTheRightAndSitOnTheBaseline() {
        List<Rect> dots = Leaders.shapes(Style.Leader.DOTS, CONTENT, 29, 10);
        // Cuerpo 10: puntos de 0,9 pt cada 5 pt, desde el borde derecho.
        assertEquals(10, dots.size());
        Rect last = dots.get(0);
        assertEquals(150 - 0.9, last.x(), 1e-9);
        assertEquals(0.9, last.width(), 1e-9);
        assertEquals(29 - 0.9, last.y(), 1e-9);
        assertEquals(5, dots.get(0).x() - dots.get(1).x(), 1e-9);
        assertTrue(dots.stream().allMatch(d -> d.x() >= 100 && d.right() <= 150));
    }

    @Test
    void aRuleFillsTheWidth() {
        List<Rect> rule = Leaders.shapes(Style.Leader.RULE, CONTENT, 29, 10);
        assertEquals(List.of(new Rect(100, 28.5, 50, 0.5)), rule);
    }

    @Test
    void aBoxTooNarrowForADotHasNone() {
        assertTrue(Leaders.shapes(Style.Leader.DOTS, new Rect(100, 20, 0, 12), 29, 10).isEmpty());
        assertTrue(Leaders.shapes(Style.Leader.NONE, CONTENT, 29, 10).isEmpty());
    }
}
