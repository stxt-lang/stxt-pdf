package dev.stxt.boxes.layout.grid;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AreasAndPlacementTest {

    @Test
    void module6PageAreas() {
        Map<String, Area> a = GridAreas.parse("head head\nside main\nfoot foot");
        assertEquals(new Area(1, 1, 1, 2), a.get("head"));
        assertEquals(new Area(2, 1, 1, 1), a.get("side"));
        assertEquals(new Area(2, 2, 1, 1), a.get("main"));
        assertEquals(new Area(3, 1, 1, 2), a.get("foot"));
    }

    @Test
    void nonRectangularAreaOrUnevenRows() {
        assertThrows(IllegalArgumentException.class, () -> GridAreas.parse("a a\na b\nb b"));
        assertThrows(IllegalArgumentException.class, () -> GridAreas.parse("a a\nb"));
        assertEquals(new Area(1, 2, 1, 1), GridAreas.parse(". a").get("a"));
    }

    @Test
    void cellSyntax() {
        Map<String, Area> a = GridAreas.parse("head head");
        assertEquals(new Cell(1, 1, 1, 2), Cell.parse("head", a));
        assertEquals(new Cell(2, 3, 1, 1), Cell.parse("2 3", a));
        assertEquals(new Cell(null, null, 1, 2), Cell.parse("auto auto 1 2", a));
        assertThrows(IllegalArgumentException.class, () -> Cell.parse("foot", a));
        assertThrows(IllegalArgumentException.class, () -> Cell.parse("0 1", a));
    }

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
