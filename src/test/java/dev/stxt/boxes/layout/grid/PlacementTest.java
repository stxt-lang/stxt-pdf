package dev.stxt.boxes.layout.grid;

import dev.stxt.boxes.model.Area;
import dev.stxt.boxes.model.Cell;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Colocación automática de los ítems en la rejilla (ESPECIFICACION.md §5.8). */
class PlacementTest {

    @Test
    void autoItemsFlowAroundExplicitOne() {
        List<Cell> c = List.of(Cell.AUTO, new Cell(1, 2, 1, 1), Cell.AUTO,
                Cell.AUTO, Cell.AUTO);
        List<Area> r = Placement.place(c, 3);
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
        List<Area> r = Placement.place(c, 3);
        assertEquals(new Area(2, 1, 1, 2), r.get(2));
        assertEquals(new Area(2, 3, 1, 1), r.get(3));
    }
}
