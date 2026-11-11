package dev.stxt.pdf.fragment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.model.LineStyle;

/** A line of a fragment (reference/components/table.md#merged-borders). */
class RuleTest {

    /** Whoever builds the line says where it runs: a piece of a vertical line can be shorter than it is thick. */
    @Test
    void theDirectionOfALineIsNotThatOfItsRectangle() {
        Rule piece = new Rule(0, 0, 8, 3, "#000000", LineStyle.DASHED, true);
        assertTrue(piece.vertical());
        assertEquals(8, piece.thickness(), 1e-9);
        Rule across = new Rule(0, 0, 3, 8, "#000000", LineStyle.DASHED, false);
        assertFalse(across.vertical());
        assertEquals(8, across.thickness(), 1e-9);
    }

    /** The constructor without a style makes a solid line, vertical if it is taller than it is wide. */
    @Test
    void aLineWithoutAStyleIsSolidAndRunsAlongItsLongSide() {
        Rule vertical = new Rule(0, 0, 1, 63, "#000000");
        assertEquals(LineStyle.SOLID, vertical.style());
        assertTrue(vertical.vertical());
        assertEquals(1, vertical.thickness(), 1e-9);
        assertFalse(new Rule(0, 0, 515, 1, "#000000").vertical());
    }
}
