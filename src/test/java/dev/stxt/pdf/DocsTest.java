package dev.stxt.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import dev.stxt.InlineNode;

/**
 * {@link Docs}: the rewriting of the test documents. Most test classes write their documents through it, so a
 * silent change here (a content node left out of the {@code Section}) would change what all of them test.
 */
class DocsTest {

    private static final String HEADER = Docs.header(200, 200);
    private static final String RULE = "\tStyles:\n\t\tFor element: Text\n\t\t\tBox:\n\t\t\t\tPadding: 2\n";

    @Test
    void theContentGoesIntoASectionAndTheDocumentLevelNodesStayOutside() {
        // Styles after the content stays at the document level, before the section.
        assertEquals(HEADER + RULE + "\tSection:\n\t\tText: Hola\n\t\tParagraph:\n\t\t\tId: p\n\t\t\tText: Dos",
                Docs.sectioned(HEADER + "\tText: Hola\n" + RULE + "\tParagraph:\n\t\tId: p\n\t\tText: Dos"));
    }

    @Test
    void aDocumentAlreadySectionedDoesNotChange() {
        String document = HEADER + "\tSection:\n\t\tText: Hola\n";
        assertSame(document, Docs.sectioned(document));
    }

    @Test
    void aDocumentWithoutContentDoesNotChange() {
        assertSame(HEADER, Docs.sectioned(HEADER));
        String withStyles = HEADER + RULE;
        assertSame(withStyles, Docs.sectioned(withStyles));
    }

    @Test
    void whatIsNotADocumentDoesNotChange() {
        assertSame(Docs.STYLES, Docs.sectioned(Docs.STYLES));
    }

    @Test
    void twoStylesAreMergedIntoTheFirstOne() {
        String merged = Docs.mergeStyles(HEADER + Docs.TYPOGRAPHY + "\tText: Hola\n" + RULE + "\tText: Dos");
        assertEquals(HEADER + Docs.TYPOGRAPHY + "\t\tFor element: Text\n\t\t\tBox:\n\t\t\t\tPadding: 2\n"
                + "\tSection:\n\t\tText: Hola\n\t\tText: Dos", merged);
    }

    @Test
    void oneStylesIsOnlySectioned() {
        String document = HEADER + RULE + "\tText: Hola";
        assertEquals(Docs.sectioned(document), Docs.mergeStyles(document));
    }

    @Test
    void theHeaderTheDocumentAndItsReading() {
        assertEquals("Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 200\n",
                Docs.header(300, 200));
        assertEquals(Docs.header(300, 200) + "\t\tMargin: 20\n", Docs.header(300, 200, 20));
        assertEquals(HEADER + "\tSection:\n\t\tText: Hola\n\t\tText: Dos\n", Docs.document(HEADER, "\tText: Hola", "\tText: Dos"));

        InlineNode document = Docs.read(HEADER + Docs.TYPOGRAPHY, "\tText: Hola");
        assertEquals("Document", document.getName());
        assertNotNull(document.getChild("Styles"));
        assertNull(document.getChild("Text"));
        InlineNode section = assertInstanceOf(InlineNode.class, document.getChild("Section"));
        InlineNode text = assertInstanceOf(InlineNode.class, section.getChild("Text"));
        assertEquals("Hola", text.getValue());
    }
}
