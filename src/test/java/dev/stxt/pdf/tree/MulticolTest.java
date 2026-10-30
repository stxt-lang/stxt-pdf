package dev.stxt.pdf.tree;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.Break;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bloques en columnas al construir el árbol (referencia/componentes/columns.md): propiedades y flujo de columnas. */
class MulticolTest {

    private static Box root(String... lines) {
        String doc = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n" + String.join("\n", lines);
        return new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(Docs.mergeStyles(doc)))).sections().get(0).box();
    }

    @Test
    void contentGoesInTheColumnFlow() {
        Box multicol = root("\tColumns:", "\t\tId: m", "\t\tCount: 3", "\t\tGap: 12", "\t\tSeparator: 0.5",
                "\t\tSeparator color: #999999", "\t\tBalance: false", "\t\tTypography:", "\t\t\tSize: 9",
                "\t\tParagraph: Uno", "\t\tBlock:", "\t\t\tId: b").children().get(0);
        assertEquals(Element.MULTICOL, multicol.element());
        assertEquals(Layout.MULTICOL, multicol.style().layout());
        assertEquals(3, multicol.style().multicol().count());
        assertNull(multicol.style().multicol().columnWidth());
        assertEquals(12, multicol.style().multicol().columnGap());
        assertEquals(0.5, multicol.style().multicol().rule());
        assertEquals("#999999", multicol.style().multicol().ruleColor());
        assertFalse(multicol.style().multicol().balance());

        assertEquals(1, multicol.children().size());
        Box flow = multicol.children().get(0);
        assertEquals(Element.COLUMNS, flow.element());
        assertEquals(Layout.BLOCK, flow.style().layout());
        assertTrue(flow.establishesBlockFormattingContext());
        assertEquals(List.of(Element.PARAGRAPH, Element.BLOCK), flow.children().stream().map(b -> b.element()).toList());
        assertSame(flow, flow.children().get(1).parent());
        // La tipografía del Columns llega a su contenido a través del flujo.
        assertEquals(9, flow.children().get(0).style().typography().fontSize());
    }

    @Test
    void defaults() {
        Style s = root("\tColumns:", "\t\tMin column width: 150", "\t\tText: Hola").children().get(0).style();
        assertNull(s.multicol().count());
        assertEquals(150, s.multicol().columnWidth());
        assertEquals(0, s.multicol().columnGap());
        assertEquals(0, s.multicol().rule());
        assertNull(s.multicol().ruleColor());
        assertTrue(s.multicol().balance());
    }

    @Test
    void columnBreaksInsideAMulticol() {
        Box flow = root("\tColumns:", "\t\tCount: 2", "\t\tBlock:", "\t\t\tBreak:", "\t\t\t\tAfter: column",
                "\t\tBlock:", "\t\t\tBreak:", "\t\t\t\tBefore: column").children().get(0).children().get(0);
        assertEquals(Break.COLUMN, flow.children().get(0).style().breaks().after());
        assertEquals(Break.COLUMN, flow.children().get(1).style().breaks().before());
    }

    /** Los casos de error están en el catálogo; aquí, el que solo se da con una regla de {@code Styles}. */
    @Test
    void columnBreakFromARuleOutsideAMulticol() {
        InvalidModelException ex = assertThrows(InvalidModelException.class, () -> root(
                "\tStyles:", "\t\tFor element: Paragraph", "\t\t\tBreak:", "\t\t\t\tBefore: column",
                "\tColumns:", "\t\tCount: 2", "\t\tParagraph: Dentro",
                "\tParagraph: Fuera"));
        assertEquals(ErrorCode.COLUMN_BREAK_OUTSIDE_COLUMNS, ex.errorCode());
        assertEquals(14, ex.line());
    }
}
