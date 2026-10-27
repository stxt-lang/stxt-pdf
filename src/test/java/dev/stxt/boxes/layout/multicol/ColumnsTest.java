package dev.stxt.boxes.layout.multicol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Número y ancho de las columnas (ESPECIFICACION.md §5.16). */
class ColumnsTest {

    @Test
    void countSplitsTheWidthBetweenTheGaps() {
        assertEquals(new Columns(2, 250, 15), Columns.of(2, null, 15, 515));
        assertEquals(new Columns(3, 165, 10), Columns.of(3, null, 10, 515));
    }

    @Test
    void columnWidthFitsAsManyColumnsAsPossible() {
        // (515 + 10) / (150 + 10) = 3,28: tres columnas, que se ensanchan hasta 165.
        assertEquals(new Columns(3, 165, 10), Columns.of(null, 150.0, 10, 515));
        // Justo cuatro de 121,25 con tres huecos de 10.
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
}
