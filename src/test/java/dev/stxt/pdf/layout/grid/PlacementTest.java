package dev.stxt.pdf.layout.grid;

import dev.stxt.pdf.model.Area;
import dev.stxt.pdf.model.Cell;
import dev.stxt.pdf.model.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** Automatic placement of the items in the grid (reference/components/grid.md). */
class PlacementTest {

    private static List<Area> place(List<Cell> cells, int columns) {
        return assertInstanceOf(Placement.Placed.class, Placement.place(cells, columns)).areas();
    }

    @Test
    void autoItemsFlowAroundExplicitOne() {
        List<Cell> c = List.of(Cell.AUTO, new Cell(1, 2, 1, 1), Cell.AUTO,
                Cell.AUTO, Cell.AUTO);
        List<Area> r = place(c, 3);
        assertEquals(new Area(1, 1, 1, 1), r.get(0));
        assertEquals(new Area(1, 2, 1, 1), r.get(1));
        assertEquals(new Area(1, 3, 1, 1), r.get(2));
        assertEquals(new Area(2, 1, 1, 1), r.get(3));
        assertEquals(new Area(2, 2, 1, 1), r.get(4));
    }

    @Test
    void sparseModeLeavesHolesBehind() {
        // An item of 2 columns does not fit at (1,3): it moves to row 2 and the next one
        // continues from the cursor, without going back to the gap (1,3).
        List<Cell> c = List.of(Cell.AUTO, Cell.AUTO,
                new Cell(null, null, 1, 2), Cell.AUTO);
        List<Area> r = place(c, 3);
        assertEquals(new Area(2, 1, 1, 2), r.get(2));
        assertEquals(new Area(2, 3, 1, 1), r.get(3));
    }

    @Test
    void fixedRowTakesTheFirstFreeColumnWithoutMovingTheCursor() {
        // Row 1 has column 1 occupied: the item goes to column 2, and the automatic one continues from (1, 1).
        List<Cell> c = List.of(new Cell(1, 1, 1, 1), new Cell(1, null, 1, 1), Cell.AUTO);
        List<Area> r = place(c, 3);
        assertEquals(new Area(1, 2, 1, 1), r.get(1));
        assertEquals(new Area(1, 3, 1, 1), r.get(2));
    }

    @Test
    void fixedColumnGoesDownFromTheCursor() {
        // After two automatic ones the cursor is at (1, 3): column 1 is behind it and moves to row 2;
        // column 3 is free in row 2, to the right of the cursor.
        List<Cell> c = List.of(Cell.AUTO, Cell.AUTO, new Cell(null, 1, 1, 1), new Cell(null, 3, 1, 1));
        List<Area> r = place(c, 3);
        assertEquals(new Area(2, 1, 1, 1), r.get(2));
        assertEquals(new Area(2, 3, 1, 1), r.get(3));
        // Occupied in the row of the cursor: it goes down to the first free one.
        List<Area> down = place(List.of(new Cell(1, 2, 2, 1), new Cell(null, 2, 1, 1)), 2);
        assertEquals(new Area(3, 2, 1, 1), down.get(1));
    }

    @Test
    void fixedRowWithoutRoomIsAnError() {
        // The two columns of row 1 are occupied; before, the loop did not end.
        Placement.NoRoom e = assertInstanceOf(Placement.NoRoom.class, Placement.place(
                List.of(new Cell(1, 1, 1, 1), new Cell(1, 2, 1, 1), new Cell(1, null, 1, 1)), 2));
        assertEquals(2, e.item());
        assertEquals(1, e.track());
        // A gap of one column is not enough for an item of two.
        e = assertInstanceOf(Placement.NoRoom.class, Placement.place(
                List.of(new Cell(1, 2, 1, 1), new Cell(1, null, 1, 2)), 3));
        assertEquals(2, e.span());
        assertEquals(3, e.tracks());
    }

    @Test
    void fixedColumnWithoutRoomInColumnFlow() {
        // With Flow: vertical, the fixed column is the one that does not grow: two rows, both occupied.
        assertInstanceOf(Placement.NoRoom.class, Placement.place(
                List.of(new Cell(1, 1, 1, 1), new Cell(2, 1, 1, 1), new Cell(null, 1, 1, 1)), 1, 2,
                Direction.VERTICAL));
    }

    // Cases that were missing according to mutation testing.

    @Test
    void aFixedRowItemMayEndInTheLastColumn() {
        // Column 1 of row 1 is occupied: the item of two columns goes from column 2 to 3, right up to the end.
        List<Area> r = place(List.of(new Cell(1, 1, 1, 1), new Cell(1, null, 1, 2)), 3);
        assertEquals(new Area(1, 2, 1, 2), r.get(1));
    }

    @Test
    void aFixedColumnAtTheCursorStaysInItsRow() {
        // The automatic one leaves the cursor in column 2: the one in column 2 continues in row 1.
        List<Area> r = place(List.of(Cell.AUTO, new Cell(null, 2, 1, 1)), 3);
        assertEquals(new Area(1, 2, 1, 1), r.get(1));
    }

    @Test
    void theCursorGoesOnAfterAFixedColumnItem() {
        // The one in column 1, of two columns, leaves the cursor in column 3: the automatic one goes to (1, 3).
        List<Area> r = place(List.of(new Cell(null, 1, 1, 2), Cell.AUTO), 3);
        assertEquals(new Area(1, 3, 1, 1), r.get(1));
    }

    @Test
    void anExplicitCellInsideTheGridAddsNoColumns() {
        // With two columns and an explicit cell in column 2, the automatic ones continue in two columns.
        List<Area> r = place(List.of(new Cell(1, 2, 1, 1), Cell.AUTO, Cell.AUTO), 2);
        assertEquals(new Area(1, 1, 1, 1), r.get(1));
        assertEquals(new Area(2, 1, 1, 1), r.get(2));
    }
}
