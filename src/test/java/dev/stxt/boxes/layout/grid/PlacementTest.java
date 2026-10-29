package dev.stxt.boxes.layout.grid;

import dev.stxt.boxes.model.Area;
import dev.stxt.boxes.model.Cell;
import dev.stxt.boxes.tree.Style;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** Colocación automática de los ítems en la rejilla (ESPECIFICACION.md §5.8). */
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
        // Un ítem de 2 columnas no cabe en (1,3): pasa a la fila 2 y el siguiente
        // continúa desde el cursor, sin volver al hueco (1,3).
        List<Cell> c = List.of(Cell.AUTO, Cell.AUTO,
                new Cell(null, null, 1, 2), Cell.AUTO);
        List<Area> r = place(c, 3);
        assertEquals(new Area(2, 1, 1, 2), r.get(2));
        assertEquals(new Area(2, 3, 1, 1), r.get(3));
    }

    @Test
    void fixedRowTakesTheFirstFreeColumnWithoutMovingTheCursor() {
        // La fila 1 tiene ocupada la columna 1: el ítem va a la 2, y el automático sigue desde (1, 1).
        List<Cell> c = List.of(new Cell(1, 1, 1, 1), new Cell(1, null, 1, 1), Cell.AUTO);
        List<Area> r = place(c, 3);
        assertEquals(new Area(1, 2, 1, 1), r.get(1));
        assertEquals(new Area(1, 3, 1, 1), r.get(2));
    }

    @Test
    void fixedColumnGoesDownFromTheCursor() {
        // Tras dos automáticos el cursor está en (1, 3): la columna 1 queda detrás y pasa a la fila 2;
        // la columna 3 está libre en la fila 2, a la derecha del cursor.
        List<Cell> c = List.of(Cell.AUTO, Cell.AUTO, new Cell(null, 1, 1, 1), new Cell(null, 3, 1, 1));
        List<Area> r = place(c, 3);
        assertEquals(new Area(2, 1, 1, 1), r.get(2));
        assertEquals(new Area(2, 3, 1, 1), r.get(3));
        // Ocupada en la fila del cursor: baja hasta la primera libre.
        List<Area> down = place(List.of(new Cell(1, 2, 2, 1), new Cell(null, 2, 1, 1)), 2);
        assertEquals(new Area(3, 2, 1, 1), down.get(1));
    }

    @Test
    void fixedRowWithoutRoomIsAnError() {
        // Las dos columnas de la fila 1 están ocupadas; antes, el bucle no terminaba.
        Placement.NoRoom e = assertInstanceOf(Placement.NoRoom.class, Placement.place(
                List.of(new Cell(1, 1, 1, 1), new Cell(1, 2, 1, 1), new Cell(1, null, 1, 1)), 2));
        assertEquals(2, e.item());
        assertEquals(1, e.track());
        // Un hueco de una columna no basta para un ítem de dos.
        e = assertInstanceOf(Placement.NoRoom.class, Placement.place(
                List.of(new Cell(1, 2, 1, 1), new Cell(1, null, 1, 2)), 3));
        assertEquals(2, e.span());
        assertEquals(3, e.tracks());
    }

    @Test
    void fixedColumnWithoutRoomInColumnFlow() {
        // Con Flow: column, la columna fija es la que no crece: dos filas, las dos ocupadas.
        assertInstanceOf(Placement.NoRoom.class, Placement.place(
                List.of(new Cell(1, 1, 1, 1), new Cell(2, 1, 1, 1), new Cell(null, 1, 1, 1)), 1, 2,
                Style.Direction.COLUMN));
    }
}
