package dev.stxt.pdf.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

/** {@code Area map} and the value of {@code Area} (reference/components/grid.md). */
class AreaMapTest {

    @Test
    void pageAreas() {
        AreaMap g = AreaMap.parse("head head\nside main\nfoot foot");
        Map<String, Area> a = g.named();
        assertEquals(new Area(1, 1, 1, 2), a.get("head"));
        assertEquals(new Area(2, 1, 1, 1), a.get("side"));
        assertEquals(new Area(2, 2, 1, 1), a.get("main"));
        assertEquals(new Area(3, 1, 1, 2), a.get("foot"));
        assertEquals(3, g.rows());
        assertEquals(2, g.columns());
    }

    @Test
    void nonRectangularAreaOrUnevenRows() {
        assertThrows(IllegalArgumentException.class, () -> AreaMap.parse("a a\na b\nb b"));
        assertThrows(IllegalArgumentException.class, () -> AreaMap.parse("a a\nb"));
        assertEquals(new Area(1, 2, 1, 1), AreaMap.parse(". a").named().get("a"));
    }

    @Test
    void cellSyntax() {
        AreaMap a = AreaMap.parse("head head");
        assertEquals(new Cell(1, 1, 1, 2), Cell.parse("head", a));
        assertEquals(new Cell(2, 3, 1, 1), Cell.parse("2 3", a));
        assertEquals(new Cell(null, null, 1, 2), Cell.parse("auto auto 1 2", a));
        assertThrows(IllegalArgumentException.class, () -> Cell.parse("foot", a));
        assertThrows(IllegalArgumentException.class, () -> Cell.parse("0 1", a));
    }
}
