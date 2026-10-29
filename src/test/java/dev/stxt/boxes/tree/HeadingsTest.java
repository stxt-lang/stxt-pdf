package dev.stxt.boxes.tree;

import dev.stxt.boxes.Docs;
import dev.stxt.boxes.model.Break;
import dev.stxt.boxes.model.Layout;
import dev.stxt.boxes.model.TextAlign;
import dev.stxt.boxes.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Encabezados y párrafos al construir el árbol (ESPECIFICACION.md §5.13). */
class HeadingsTest {

    private static Box root(String... lines) {
        String doc = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tTitle: Prueba\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tStyles:\n\t\tElement: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n" + String.join("\n", lines);
        return new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(Docs.mergeStyles(doc)))).sections().get(0).box();
    }

    @Test
    void headingIsABlockOfTexts() {
        Box h = root("\tHeading 1:", "\t\tId: title", "\t\tText: Resultados", "\t\tText: del trimestre").children().get(0);
        assertEquals(Element.HEADING_1, h.element());
        assertEquals(1, h.headingLevel());
        assertEquals(Layout.BLOCK, h.style().layout);
        assertEquals(2, h.children().size());
        assertEquals(Element.TEXT, h.children().get(0).element());
        assertEquals("Resultados del trimestre", h.headingText());
    }

    @Test
    void theEngineAvoidsBreaksInsideAndAfterAHeading() {
        Box h = root("\tHeading 1:", "\t\tText: Título").children().get(0);
        assertEquals(Break.AVOID, h.style().breaks().after());
        assertTrue(h.style().breaks().avoidInside());
        assertEquals(Break.AUTO, h.style().breaks().before());
    }

    @Test
    void paragraphIsATextWithMeaning() {
        Box p = root("\tParagraph: Hola.").children().get(0);
        assertEquals(Element.PARAGRAPH, p.element());
        assertEquals(Layout.INLINE, p.style().layout);
        assertEquals("Hola.", p.text());
        assertEquals(0, p.headingLevel());
    }

    @Test
    void blankLinesAtTheEdgesOfAParagraphAreAllowed() {
        Box p = root("\tParagraph:", "\t\tContent >>", "", "\t\t\tUna línea", "\t\t\tque sigue.", "").children().get(0);
        assertTrue(p.text().contains("Una línea"));
    }

    @Test
    void levelsMayGoDownAnyNumberOfLevels() {
        Box root = root(
                "\tHeading 1:", "\t\tText: A",
                "\tHeading 2:", "\t\tText: B",
                "\tBlock:", "\t\tHeading 3:", "\t\t\tText: C",
                "\tHeading 1:", "\t\tText: D",
                "\tHeading 2:", "\t\tText: E");
        assertEquals(5, root.children().size());
    }

    @Test
    void rulesNameHeadingsAndParagraphs() {
        Box root = root(
                "\tStyles:",
                "\t\tElement: Heading 2", "\t\t\tBox:", "\t\t\t\tMargin: 8 0 4 0", "\t\t\tTypography:", "\t\t\t\tSize: 14",
                "\t\tElement: paragraph", "\t\t\tTypography:", "\t\t\t\tAlign: justify",
                "\tHeading 1:", "\t\tText: A",
                "\tHeading 2:", "\t\tText: B",
                "\tParagraph: C");
        Box h2 = root.children().get(1);
        assertEquals(14, h2.style().fontSize, 1e-9);
        // El Typography del encabezado lo heredan sus Text.
        assertEquals(14, h2.children().get(0).style().fontSize, 1e-9);
        assertEquals(10, root.children().get(0).style().fontSize, 1e-9);
        assertEquals(TextAlign.JUSTIFY, root.children().get(2).style().textAlign);
    }

    @Test
    void headingsCanBeItems() {
        Box flex = root("\tFlex:", "\t\tItem:", "\t\t\tGrow: 1", "\t\t\tHeading 1:", "\t\t\t\tText: A").children().get(0);
        assertEquals(Element.HEADING_1, flex.children().get(0).element());
        assertEquals(1, flex.children().get(0).style().item().grow(), 1e-9);
    }
}
