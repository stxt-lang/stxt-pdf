package dev.stxt.boxes.tree;

import dev.stxt.boxes.Docs;
import dev.stxt.boxes.model.FontWeight;
import dev.stxt.boxes.model.Value;
import dev.stxt.boxes.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tablas al construir el árbol (ESPECIFICACION.md §5.15): traducción a un grid, filas y celdas. */
class TablesTest {

    private static Box root(String... lines) {
        String doc = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tTitle: Prueba\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tStyles:\n\t\tElement: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n" + String.join("\n", lines);
        return new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(Docs.mergeStyles(doc)))).sections().get(0).box();
    }

    /** Colocación de cada celda como {@code fila columna filas columnas}. */
    private static List<String> areas(Box table) {
        return table.children.stream().map(c -> c.style.itemArea)
                .map(a -> a.row() + " " + a.column() + " " + a.rows() + " " + a.columns()).toList();
    }

    @Test
    void tableIsAGridWithExplicitPlacement() {
        Box table = root("\tTable:", "\t\tColumns: 120 1fr 1fr", "\t\tGap: 2 4",
                "\t\tHead:", "\t\t\tRow:", "\t\t\t\tCell: Concepto", "\t\t\t\tCell: T1", "\t\t\t\tCell: T2",
                "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: Ventas", "\t\t\t\tCell: 100", "\t\t\t\tCell: 120",
                "\t\tFoot:", "\t\t\tRow:", "\t\t\t\tCell: Total", "\t\t\t\t\tSpan: 1 3").children.get(0);
        assertEquals(Element.TABLE, table.element);
        assertEquals(Style.Layout.GRID, table.style.layout);
        assertEquals(List.of(Value.pt(120), new Value.Fraction(1), new Value.Fraction(1)), table.style.gridColumns);
        assertEquals(Value.pt(2), table.style.rowGap);
        assertEquals(Value.pt(4), table.style.columnGap);
        assertEquals(List.of("1 1 1 1", "1 2 1 1", "1 3 1 1", "2 1 1 1", "2 2 1 1", "2 3 1 1", "3 1 1 3"),
                areas(table));
        assertEquals(1, table.headRows);
        assertEquals(1, table.footRows);
        Box cell = table.children.get(3);
        assertEquals(Element.CELL, cell.element);
        assertEquals(Style.Layout.INLINE, cell.style.layout);
        assertEquals("Ventas", cell.text.trim());
    }

    /** {@code Header} es de la celda, no se hereda y por defecto es falso (§5.15). */
    @Test
    void headerMarksTheRowHeaderCells() {
        Box table = root("\tTable:", "\t\tColumns: 100 100",
                "\t\tHead:", "\t\t\tRow:", "\t\t\t\tCell: Concepto", "\t\t\t\tCell: Importe",
                "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: Mesa", "\t\t\t\t\tHeader: true", "\t\t\t\tCell: 120",
                "\t\tFoot:", "\t\t\tRow:", "\t\t\t\tCell: Total", "\t\t\t\t\tHeader: true", "\t\t\t\tCell: 120").children.get(0);
        assertEquals(List.of(false, false, true, false, true, false),
                table.children.stream().map(c -> c.style.header).toList());
        assertEquals(22, table.children.get(2).style.line("header"));
    }

    @Test
    void headerInAHeadCellIsAnError() {
        InvalidModelException e = org.junit.jupiter.api.Assertions.assertThrows(InvalidModelException.class,
                () -> root("\tTable:", "\t\tColumns: 100", "\t\tHead:", "\t\t\tRow:", "\t\t\t\tCell: Concepto",
                        "\t\t\t\t\tHeader: true", "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: Mesa"));
        assertEquals(ErrorCode.TABLE_HEADER, e.errorCode());
        assertEquals(18, e.line());
    }

    @Test
    void spansFromAboveTakeTheirColumns() {
        Box table = root("\tTable:", "\t\tColumns: 1fr 1fr 1fr", "\t\tBody:",
                "\t\t\tRow:", "\t\t\t\tCell: a", "\t\t\t\t\tSpan: 2 1", "\t\t\t\tCell: b", "\t\t\t\tCell: c",
                "\t\t\t\t\tSpan: 3 1",
                "\t\t\tRow:", "\t\t\t\tCell: d",
                "\t\t\tRow:", "\t\t\t\tCell: e", "\t\t\t\t\tSpan: 1 2").children.get(0);
        assertEquals(List.of("1 1 2 1", "1 2 1 1", "1 3 3 1", "2 2 1 1", "3 1 1 2"), areas(table));
        assertEquals(0, table.headRows);
        assertEquals(0, table.footRows);
    }

    @Test
    void cellWithoutValueIsABlockOfElements() {
        Box table = root("\tTable:", "\t\tColumns: 1fr 1fr", "\t\tBody:", "\t\t\tRow:",
                "\t\t\t\tCell:", "\t\t\t\t\tParagraph: Uno", "\t\t\t\t\tList:", "\t\t\t\t\t\tMarker: disc",
                "\t\t\t\t\t\tList item:", "\t\t\t\t\t\t\tText: dos",
                "\t\t\t\tCell:").children.get(0);
        Box full = table.children.get(0);
        assertEquals(Style.Layout.BLOCK, full.style.layout);
        assertNull(full.text);
        assertEquals(List.of(Element.PARAGRAPH, Element.LIST), full.children.stream().map(b -> b.element).toList());
        Box empty = table.children.get(1);
        assertEquals(Style.Layout.BLOCK, empty.style.layout);
        assertTrue(empty.children.isEmpty());
    }

    @Test
    void rowTypographyIsInheritedAndRulesWin() {
        Box table = root("\tStyles:", "\t\tElement: Cell", "\t\t\tTypography:", "\t\t\t\tSize: 8",
                "\t\t\tBox:", "\t\t\t\tPadding: 3",
                "\tTable:", "\t\tTypography:", "\t\t\tColor: #333333", "\t\tColumns: 1fr 1fr",
                "\t\tBody:", "\t\t\tRow:", "\t\t\t\tTypography:", "\t\t\t\t\tWeight: bold", "\t\t\t\t\tSize: 12",
                "\t\t\t\tCell: a", "\t\t\t\tCell: b", "\t\t\t\t\tTypography:", "\t\t\t\t\t\tWeight: normal")
                .children.get(0);
        Box a = table.children.get(0);
        Box b = table.children.get(1);
        assertEquals("#333333", a.style.color);
        assertEquals(FontWeight.BOLD, a.style.fontWeight);
        // La regla gana a lo heredado de la fila; lo propio de la celda, a los dos.
        assertEquals(8, a.style.fontSize);
        assertEquals(Value.pt(3), a.style.padding.top());
        assertEquals(FontWeight.NORMAL, b.style.fontWeight);
    }

    @Test
    void rowBreakGoesToItsCells() {
        Box table = root("\tTable:", "\t\tColumns: 1fr 1fr", "\t\tBody:",
                "\t\t\tRow:", "\t\t\t\tCell: a", "\t\t\t\tCell: b",
                "\t\t\tRow:", "\t\t\t\tBreak:", "\t\t\t\t\tBefore: page", "\t\t\t\t\tInside: avoid",
                "\t\t\t\tCell: c", "\t\t\t\tCell: d").children.get(0);
        assertEquals(Style.Break.AUTO, table.children.get(1).style.breakBefore);
        for (Box cell : table.children.subList(2, 4)) {
            assertEquals(Style.Break.PAGE, cell.style.breakBefore);
            assertTrue(cell.style.avoidBreakInside);
            assertEquals(21, cell.style.line("break-before"));
        }
        assertFalse(table.children.get(0).style.avoidBreakInside);
    }

    @Test
    void tablesNestAndGoInItems() {
        Box root = root("\tFlex:", "\t\tItem:", "\t\t\tTable:", "\t\t\t\tId: t", "\t\t\t\tColumns: auto",
                "\t\t\t\tBody:", "\t\t\t\t\tRow:", "\t\t\t\t\t\tCell:", "\t\t\t\t\t\t\tTable:",
                "\t\t\t\t\t\t\t\tColumns: auto", "\t\t\t\t\t\t\t\tBody:", "\t\t\t\t\t\t\t\t\tRow:",
                "\t\t\t\t\t\t\t\t\t\tCell: x");
        Box outer = root.children.get(0).children.get(0);
        assertEquals("t", outer.id);
        Box inner = outer.children.get(0).children.get(0);
        assertEquals(Element.TABLE, inner.element);
        assertEquals("x", inner.children.get(0).text.trim());
    }
}
