package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paginating again because the total needs more figures or because the page of a reference changes only lays out
 * again the texts that write them and their ancestors; the rest comes from the cache.
 * It is measured with the computed layouts: the whole cache used to be emptied and the document cost twice as much.
 */
class PaginationCacheTest {

    private static final String PARAGRAPH = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod "
            + "tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation.";

    /** A document with a footer {@code footer} and {@code first} before {@code paragraphs} paragraphs. */
    private static String document(String footer, String first, int paragraphs) {
        StringBuilder sb = new StringBuilder("Document (dev.stxt.pdf):\n\tPage:\n\t\tPaper: A4\n\t\tMargin: 40\n"
                + "\tTitle: Caché\n\tLanguage: es\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n"
                + "\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n\tPage overlay:\n\t\tOffset: auto 0 0 0\n\t\tText: " + footer
                + "\n\tSection:\n\t\tParagraph: " + first + "\n");
        for (int i = 0; i < paragraphs; i++) {
            sb.append("\t\tParagraph: ").append(PARAGRAPH).append(' ').append(i).append('\n');
        }
        sb.append("\t\tParagraph: Fin\n\t\t\tId: end\n");
        return sb.toString();
    }

    /** The pages and the computed layouts when laying out the document with a new engine. */
    private static int[] layOut(String text) {
        Engine engine = new Engine();
        int pages = engine.layOutDocument(new DocumentReader().read(text)).pages().size();
        return new int[] {pages, engine.cacheMisses()};
    }

    @Test
    void aThirdDigitOfPagesOnlyRelaysOutTheTextsThatWriteIt() {
        int[] plain = layOut(document("Pie de página", "Primero", 3100));
        int[] numbered = layOut(document("Página {page} de {pages}", "Primero", 3100));
        assertEquals(plain[0], numbered[0]);
        assertTrue(numbered[0] > 99, "the test needs more than 99 pages: " + numbered[0]);
        // The second pagination only lays out again the footer and the section of each page.
        assertTrue(numbered[1] < plain[1] * 1.25, numbered[1] + " layouts versus " + plain[1]);
    }

    @Test
    void aReferenceThatChangesOnlyRelaysOutItsText() {
        // The first pass assumes page 00 and the second writes the real one, the last.
        int[] referred = layOut(document("Pie de página", "El final está en la página {page of: end}", 600));
        int[] plain = layOut(document("Pie de página", "El final está en la página " + referred[0], 600));
        assertEquals(plain[0], referred[0]);
        assertTrue(referred[1] < plain[1] * 1.25, referred[1] + " layouts versus " + plain[1]);
    }

    /**
     * With 101 pages, the text with {page} in the flow is measured again with three figures: "Página 000" (50) does not
     * fit in its box of 48 and takes two lines; measured with two (45) it would fit in one. The cache has to forget it
     * (mutation testing).
     */
    @Test
    void aThirdDigitIsMeasuredInTheTextsThatWriteIt() {
        StringBuilder sb = new StringBuilder("Document (dev.stxt.pdf):\n\tPage:\n\t\tPaper: A4\n\t\tMargin: 40\n"
                + "\tTitle: Cifras\n\tLanguage: es\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n"
                + "\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n\tSection:\n"
                + "\t\tText: Página {page}\n\t\t\tId: P\n\t\t\tBox:\n\t\t\t\tWidth: 48\n");
        for (int i = 0; i < 100; i++) {
            sb.append("\t\tBlock:\n\t\t\tBox:\n\t\t\t\tHeight: 10\n\t\t\tBreak:\n\t\t\t\tBefore: page\n");
        }
        LaidOutDocument d = new Engine().layOutDocument(new DocumentReader().read(sb.toString()));
        assertEquals(101, d.pages().size());
        Fragment text = d.pages().get(0).content().children().get(0);
        assertEquals("P", text.box().id());
        assertEquals(2, text.lines().size());
    }
}
