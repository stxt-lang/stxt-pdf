package dev.stxt.pdf.tree;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.Break;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Paint;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tablas al construir el árbol (referencia/componentes/table.md): traducción a un grid, filas y celdas. */
class TablesTest {

    private static Box root(String... lines) {
        String doc = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tStyles:\n\t\tElement: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n" + String.join("\n", lines);
        return new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(Docs.mergeStyles(doc)))).sections().get(0).box();
    }

    /** Colocación de cada celda como {@code fila columna filas columnas}. */
    private static List<String> areas(Box table) {
        return table.children().stream().map(c -> c.style().item().area())
                .map(a -> a.row() + " " + a.column() + " " + a.rows() + " " + a.columns()).toList();
    }

    @Test
    void tableIsAGridWithExplicitPlacement() {
        Box table = root("\tTable:", "\t\tColumns: 120 1fr 1fr", "\t\tGap: 2 4",
                "\t\tHead:", "\t\t\tRow:", "\t\t\t\tCell: Concepto", "\t\t\t\tCell: T1", "\t\t\t\tCell: T2",
                "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: Ventas", "\t\t\t\tCell: 100", "\t\t\t\tCell: 120",
                "\t\tFoot:", "\t\t\tRow:", "\t\t\t\tCell: Total", "\t\t\t\t\tSpan: 1 3").children().get(0);
        assertEquals(Element.TABLE, table.element());
        assertEquals(Layout.GRID, table.style().layout());
        assertEquals(List.of(Value.pt(120), new Value.Fraction(1), new Value.Fraction(1)), table.style().grid().columns());
        assertEquals(Value.pt(2), table.style().grid().rowGap());
        assertEquals(Value.pt(4), table.style().grid().columnGap());
        assertEquals(List.of("1 1 1 1", "1 2 1 1", "1 3 1 1", "2 1 1 1", "2 2 1 1", "2 3 1 1", "3 1 1 3"),
                areas(table));
        assertEquals(1, table.headRows());
        assertEquals(1, table.footRows());
        Box cell = table.children().get(3);
        assertEquals(Element.CELL, cell.element());
        assertEquals(Layout.INLINE, cell.style().layout());
        assertEquals("Ventas", cell.text().trim());
    }

    /** {@code Header} es de la celda, no se hereda y por defecto es falso (referencia/componentes/table.md). */
    @Test
    void headerMarksTheRowHeaderCells() {
        Box table = root("\tTable:", "\t\tColumns: 100 100",
                "\t\tHead:", "\t\t\tRow:", "\t\t\t\tCell: Concepto", "\t\t\t\tCell: Importe",
                "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: Mesa", "\t\t\t\t\tHeader: true", "\t\t\t\tCell: 120",
                "\t\tFoot:", "\t\t\tRow:", "\t\t\t\tCell: Total", "\t\t\t\t\tHeader: true", "\t\t\t\tCell: 120").children().get(0);
        assertEquals(List.of(false, false, true, false, true, false),
                table.children().stream().map(c -> c.style().header()).toList());
        assertEquals(22, table.children().get(2).style().line("header"));
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
                "\t\t\tRow:", "\t\t\t\tCell: e", "\t\t\t\t\tSpan: 1 2").children().get(0);
        assertEquals(List.of("1 1 2 1", "1 2 1 1", "1 3 3 1", "2 2 1 1", "3 1 1 2"), areas(table));
        assertEquals(0, table.headRows());
        assertEquals(0, table.footRows());
    }

    @Test
    void cellWithoutValueIsABlockOfElements() {
        Box table = root("\tTable:", "\t\tColumns: 1fr 1fr", "\t\tBody:", "\t\t\tRow:",
                "\t\t\t\tCell:", "\t\t\t\t\tParagraph: Uno", "\t\t\t\t\tList:", "\t\t\t\t\t\tMarker: disc",
                "\t\t\t\t\t\tList item:", "\t\t\t\t\t\t\tText: dos",
                "\t\t\t\tCell:").children().get(0);
        Box full = table.children().get(0);
        assertEquals(Layout.BLOCK, full.style().layout());
        assertNull(full.text());
        assertEquals(List.of(Element.PARAGRAPH, Element.LIST), full.children().stream().map(b -> b.element()).toList());
        Box empty = table.children().get(1);
        assertEquals(Layout.BLOCK, empty.style().layout());
        assertTrue(empty.children().isEmpty());
    }

    @Test
    void rowTypographyIsInheritedAndRulesWin() {
        Box table = root("\tStyles:", "\t\tElement: Cell", "\t\t\tTypography:", "\t\t\t\tSize: 8",
                "\t\t\tBox:", "\t\t\t\tPadding: 3",
                "\tTable:", "\t\tTypography:", "\t\t\tColor: #333333", "\t\tColumns: 1fr 1fr",
                "\t\tBody:", "\t\t\tRow:", "\t\t\t\tTypography:", "\t\t\t\t\tWeight: bold", "\t\t\t\t\tSize: 12",
                "\t\t\t\tCell: a", "\t\t\t\tCell: b", "\t\t\t\t\tTypography:", "\t\t\t\t\t\tWeight: normal")
                .children().get(0);
        Box a = table.children().get(0);
        Box b = table.children().get(1);
        assertEquals("#333333", a.style().typography().color());
        assertEquals(FontWeight.BOLD, a.style().typography().fontWeight());
        // La regla gana a lo heredado de la fila; lo propio de la celda, a los dos.
        assertEquals(8, a.style().typography().fontSize());
        assertEquals(Value.pt(3), a.style().box().padding().top());
        assertEquals(FontWeight.NORMAL, b.style().typography().fontWeight());
    }

    /**
     * Una {@code Row} con {@code Id} o {@code Box} tiene su caja, que cuelga de la tabla sin ser su hija y sabe su fila
     * (referencia/componentes/table.md#filas); sin ninguno de los dos, no.
     */
    @Test
    void aRowWithAnIdOrABoxHasItsOwnBox() {
        Box table = root("\tTable:", "\t\tColumns: 100 100", "\t\tBody:",
                "\t\t\tRow:", "\t\t\t\tId: first", "\t\t\t\tTypography:", "\t\t\t\t\tColor: #C0392B",
                "\t\t\t\tBox:", "\t\t\t\t\tBackground: #EEEEEE", "\t\t\t\t\tBorder: 0 0 1 0",
                "\t\t\t\tCell: a", "\t\t\t\tCell: b",
                "\t\t\tRow:", "\t\t\t\tCell: c", "\t\t\t\tCell: d",
                "\t\t\tRow:", "\t\t\t\tBox:", "\t\t\t\t\tBackground: #DDDDDD", "\t\t\t\tCell: e",
                "\t\t\t\tCell: f").children().get(0);
        assertEquals(6, table.children().size());
        assertEquals(2, table.rows().size());
        Box first = table.rows().get(0);
        assertEquals("first", first.id());
        assertEquals(Element.ROW, first.element());
        assertEquals(Element.Role.ROW, first.role());
        assertEquals(table, first.parent());
        assertEquals(1, first.style().item().area().row());
        assertEquals(new Paint.Solid("#EEEEEE"), first.style().box().background());
        assertEquals(Value.pt(1), first.style().box().border().bottom());
        // Su Typography es el de sus celdas y el suyo: el color de su borde por defecto.
        assertEquals("#C0392B", first.style().typography().color());
        assertEquals("#C0392B", table.children().get(0).style().typography().color());
        // Su Box no llega a las celdas.
        assertNull(table.children().get(0).style().box().background());
        Box third = table.rows().get(1);
        assertNull(third.id());
        assertEquals(3, third.style().item().area().row());
    }

    /**
     * El {@code For id} de una fila da estilo a sus celdas: más que las clases de la celda, menos que su propio
     * {@code For id} (referencia/componentes/table.md#estilo-de-las-celdas).
     */
    @Test
    void theForIdOfARowStylesItsCells() {
        Box table = root("\tStyles:", "\t\tFor class: small", "\t\t\tTypography:", "\t\t\t\tSize: 8",
                "\t\tFor id: total", "\t\t\tTypography:", "\t\t\t\tSize: 12", "\t\t\t\tWeight: bold",
                "\t\tFor id: grand", "\t\t\tTypography:", "\t\t\t\tSize: 14",
                "\tTable:", "\t\tColumns: 1fr 1fr 1fr", "\t\tBody:",
                "\t\t\tRow:", "\t\t\t\tId: total", "\t\t\t\tCell: a", "\t\t\t\tCell: b",
                "\t\t\t\t\tClass: small", "\t\t\t\tCell: c", "\t\t\t\t\tId: grand").children().get(0);
        assertEquals(List.of(12.0, 12.0, 14.0),
                table.children().stream().map(c -> c.style().typography().fontSize()).toList());
        assertEquals(FontWeight.BOLD, table.children().get(2).style().typography().fontWeight());
    }

    @Test
    void theBoxOfARowOnlyAdmitsItsLook() {
        InvalidModelException e = org.junit.jupiter.api.Assertions.assertThrows(InvalidModelException.class,
                () -> root("\tTable:", "\t\tColumns: 100", "\t\tBody:", "\t\t\tRow:", "\t\t\t\tBox:",
                        "\t\t\t\t\tBackground: #EEEEEE", "\t\t\t\t\tHeight: 40", "\t\t\t\tCell: a"));
        assertEquals(ErrorCode.ROW_BOX, e.errorCode());
        // La línea de Height.
        assertEquals(19, e.line());
    }

    @Test
    void theIdOfARowIsADocumentId() {
        InvalidModelException e = org.junit.jupiter.api.Assertions.assertThrows(InvalidModelException.class,
                () -> root("\tParagraph: a", "\t\tId: x", "\tTable:", "\t\tColumns: 100", "\t\tBody:",
                        "\t\t\tRow:", "\t\t\t\tId: x", "\t\t\t\tCell: a"));
        assertEquals(ErrorCode.DUPLICATE_ID, e.errorCode());
    }

    @Test
    void rowBreakGoesToItsCells() {
        Box table = root("\tTable:", "\t\tColumns: 1fr 1fr", "\t\tBody:",
                "\t\t\tRow:", "\t\t\t\tCell: a", "\t\t\t\tCell: b",
                "\t\t\tRow:", "\t\t\t\tBreak:", "\t\t\t\t\tBefore: page", "\t\t\t\t\tInside: avoid",
                "\t\t\t\tCell: c", "\t\t\t\tCell: d").children().get(0);
        assertEquals(Break.AUTO, table.children().get(1).style().breaks().before());
        for (Box cell : table.children().subList(2, 4)) {
            assertEquals(Break.PAGE, cell.style().breaks().before());
            assertTrue(cell.style().breaks().avoidInside());
            assertEquals(21, cell.style().line("break-before"));
        }
        assertFalse(table.children().get(0).style().breaks().avoidInside());
    }

    @Test
    void tablesNestAndGoInItems() {
        Box root = root("\tFlex:", "\t\tItem:", "\t\t\tTable:", "\t\t\t\tId: t", "\t\t\t\tColumns: auto",
                "\t\t\t\tBody:", "\t\t\t\t\tRow:", "\t\t\t\t\t\tCell:", "\t\t\t\t\t\t\tTable:",
                "\t\t\t\t\t\t\t\tColumns: auto", "\t\t\t\t\t\t\t\tBody:", "\t\t\t\t\t\t\t\t\tRow:",
                "\t\t\t\t\t\t\t\t\t\tCell: x");
        Box outer = root.children().get(0).children().get(0);
        assertEquals("t", outer.id());
        Box inner = outer.children().get(0).children().get(0);
        assertEquals(Element.TABLE, inner.element());
        assertEquals("x", inner.children().get(0).text().trim());
    }
}
