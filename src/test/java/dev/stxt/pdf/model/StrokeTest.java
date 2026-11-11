package dev.stxt.pdf.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The line between two cells with merged borders (reference/components/table.md#merged-borders). */
class StrokeTest {

    private static final Stroke THIN_BLUE = new Stroke(1, "#1F6FB2");
    private static final Stroke THIN_GRAY = new Stroke(1, "#333333");
    private static final Stroke WIDE_RED = new Stroke(2, "#C00000");

    @Test
    void theWiderSideWins() {
        assertEquals(WIDE_RED, Stroke.merge(THIN_BLUE, WIDE_RED));
        assertEquals(WIDE_RED, Stroke.merge(WIDE_RED, THIN_BLUE));
    }

    @Test
    void withTheSameWidthTheFirstWinsThatOfTheCellAboveOrOnTheLeft() {
        assertEquals(THIN_BLUE, Stroke.merge(THIN_BLUE, THIN_GRAY));
        assertEquals(THIN_GRAY, Stroke.merge(THIN_GRAY, THIN_BLUE));
    }

    @Test
    void aSideWithoutBorderLeavesTheOther() {
        assertEquals(THIN_GRAY, Stroke.merge(Stroke.NONE, THIN_GRAY));
        assertEquals(THIN_GRAY, Stroke.merge(THIN_GRAY, Stroke.NONE));
        assertTrue(Stroke.merge(Stroke.NONE, Stroke.NONE).isNone());
        assertFalse(THIN_GRAY.isNone());
    }

    /** The style goes with the width and the color: the line between two sides is drawn like the wider one. */
    @Test
    void theStyleIsThatOfTheSideThatWins() {
        Stroke thinDashed = new Stroke(1, "#333333", LineStyle.DASHED);
        assertEquals(LineStyle.SOLID, THIN_GRAY.style());
        assertEquals(WIDE_RED, Stroke.merge(thinDashed, WIDE_RED));
        assertEquals(thinDashed, Stroke.merge(thinDashed, THIN_BLUE));
        assertEquals(THIN_BLUE, Stroke.merge(THIN_BLUE, thinDashed));
    }
}
