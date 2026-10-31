package dev.stxt.pdf.layout.columns;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Number and width of the columns (reference/components/columns.md). */
class ColumnsTest {

    @Test
    void countSplitsTheWidthBetweenTheGaps() {
        assertEquals(new Columns(2, 250, 15), Columns.of(2, null, 15, 515));
        assertEquals(new Columns(3, 165, 10), Columns.of(3, null, 10, 515));
    }

    @Test
    void columnWidthFitsAsManyColumnsAsPossible() {
        // (515 + 10) / (150 + 10) = 3.28: three columns, which widen to 165.
        assertEquals(new Columns(3, 165, 10), Columns.of(null, 150.0, 10, 515));
        // Exactly four of 121.25 with three gaps of 10.
        assertEquals(4, Columns.of(null, 121.25, 10, 515).count());
    }

    @Test
    void atLeastOneColumn() {
        assertEquals(new Columns(1, 100, 10), Columns.of(null, 150.0, 10, 100));
    }

    @Test
    void countIsTheMaximumWithColumnWidth() {
        assertEquals(new Columns(2, 252.5, 10), Columns.of(2, 150.0, 10, 515));
        assertEquals(new Columns(1, 100, 10), Columns.of(2, 150.0, 10, 100));
    }

    @Test
    void gapsWithoutRoomLeaveEmptyColumns() {
        assertEquals(new Columns(3, 0, 60), Columns.of(3, null, 60, 100));
    }

    @Test
    void positions() {
        Columns c = Columns.of(3, null, 10, 515);
        assertEquals(0, c.x(0));
        assertEquals(175, c.x(1));
        assertEquals(350, c.x(2));
    }

    /** The columns that fit count the gap with the width of each one: (515 + 50) / (100 + 50) = 3.77. */
    @Test
    void aWideGapLeavesFewerColumns() {
        assertEquals(3, Columns.of(null, 100.0, 50, 515).count());
    }
}
