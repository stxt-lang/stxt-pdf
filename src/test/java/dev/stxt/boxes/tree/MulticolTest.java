package dev.stxt.boxes.tree;

import dev.stxt.boxes.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bloques en columnas al construir el árbol (ESPECIFICACION.md §5.16): propiedades y flujo de columnas. */
class MulticolTest {

    private static Box root(String... lines) {
        String doc = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tTypography:\n\t\tFont: IBM Plex Sans\n\t\tSize: 10\n" + String.join("\n", lines);
        return new BoxTreeBuilder().build(new DocumentReader().read(doc)).root();
    }

    @Test
    void contentGoesInTheColumnFlow() {
        Box multicol = root("\tMulticol:", "\t\tId: m", "\t\tCount: 3", "\t\tColumn gap: 12", "\t\tRule: 0.5",
                "\t\tRule color: #999999", "\t\tFill: auto", "\t\tTypography:", "\t\t\tSize: 9",
                "\t\tParagraph: Uno", "\t\tBlock:", "\t\t\tId: b").children.get(0);
        assertEquals(Element.MULTICOL, multicol.element);
        assertEquals(Style.Layout.MULTICOL, multicol.style.layout);
        assertEquals(3, multicol.style.multicolCount);
        assertNull(multicol.style.multicolWidth);
        assertEquals(12, multicol.style.multicolGap);
        assertEquals(0.5, multicol.style.multicolRule);
        assertEquals("#999999", multicol.style.multicolRuleColor);
        assertEquals(Style.Fill.AUTO, multicol.style.multicolFill);

        assertEquals(1, multicol.children.size());
        Box flow = multicol.children.get(0);
        assertEquals(Element.COLUMNS, flow.element);
        assertEquals(Style.Layout.BLOCK, flow.style.layout);
        assertTrue(flow.establishesBlockFormattingContext());
        assertEquals(List.of(Element.PARAGRAPH, Element.BLOCK), flow.children.stream().map(b -> b.element).toList());
        assertSame(flow, flow.children.get(1).parent);
        // La tipografía del Multicol llega a su contenido a través del flujo.
        assertEquals(9, flow.children.get(0).style.fontSize);
    }

    @Test
    void defaults() {
        Style s = root("\tMulticol:", "\t\tColumn width: 150", "\t\tText: Hola").children.get(0).style;
        assertNull(s.multicolCount);
        assertEquals(150, s.multicolWidth);
        assertEquals(0, s.multicolGap);
        assertEquals(0, s.multicolRule);
        assertNull(s.multicolRuleColor);
        assertEquals(Style.Fill.BALANCE, s.multicolFill);
    }

    @Test
    void columnBreaksInsideAMulticol() {
        Box flow = root("\tMulticol:", "\t\tCount: 2", "\t\tBlock:", "\t\t\tBreak:", "\t\t\t\tAfter: column",
                "\t\tBlock:", "\t\t\tBreak:", "\t\t\t\tBefore: column").children.get(0).children.get(0);
        assertEquals(Style.Break.COLUMN, flow.children.get(0).style.breakAfter);
        assertEquals(Style.Break.COLUMN, flow.children.get(1).style.breakBefore);
    }

    /** Los casos de error están en el catálogo; aquí, el que solo se da con una regla de {@code Styles}. */
    @Test
    void columnBreakFromARuleOutsideAMulticol() {
        InvalidModelException ex = assertThrows(InvalidModelException.class, () -> root(
                "\tStyles:", "\t\tElement: Paragraph", "\t\t\tBreak:", "\t\t\t\tBefore: column",
                "\tMulticol:", "\t\tCount: 2", "\t\tParagraph: Dentro",
                "\tParagraph: Fuera"));
        assertEquals(ErrorCode.COLUMN_BREAK_OUTSIDE_MULTICOL, ex.code());
        assertEquals(12, ex.line());
    }
}
