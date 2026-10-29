package dev.stxt.boxes.layout;

import dev.stxt.boxes.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Volver a paginar porque el total necesita más cifras o porque cambia la página de una referencia solo vuelve a
 * maquetar los textos que las escriben y sus antepasados; el resto sale de la caché (ROADMAP-QA-ARQ.md, PF-03).
 * Se mide con los layouts calculados: antes de PF-03 se vaciaba la caché entera y el documento costaba el doble.
 */
class PaginationCacheTest {

    private static final String PARAGRAPH = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod "
            + "tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation.";

    /** Un documento con un pie {@code footer} y {@code first} delante de {@code paragraphs} párrafos. */
    private static String document(String footer, String first, int paragraphs) {
        StringBuilder sb = new StringBuilder("Document (dev.stxt.boxes):\n\tMedia:\n\t\tPaper: A4\n\t\tMargin: 40\n"
                + "\tTitle: Caché\n\tLanguage: es\n\tStyles:\n\t\tElement: Document\n\t\t\tTypography:\n"
                + "\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n\tFixed:\n\t\tOffset: auto 0 0 0\n\t\tText: " + footer
                + "\n\tSection:\n\t\tParagraph: " + first + "\n");
        for (int i = 0; i < paragraphs; i++) {
            sb.append("\t\tParagraph: ").append(PARAGRAPH).append(' ').append(i).append('\n');
        }
        sb.append("\t\tParagraph: Fin\n\t\t\tId: end\n");
        return sb.toString();
    }

    /** Las páginas y los layouts calculados al maquetar el documento con un motor nuevo. */
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
        assertTrue(numbered[0] > 99, "la prueba necesita más de 99 páginas: " + numbered[0]);
        // La segunda paginación solo maqueta otra vez el pie y la sección de cada página.
        assertTrue(numbered[1] < plain[1] * 1.25, numbered[1] + " layouts frente a " + plain[1]);
    }

    @Test
    void aReferenceThatChangesOnlyRelaysOutItsText() {
        // La primera pasada supone la página 00 y la segunda escribe la de verdad, la última.
        int[] referred = layOut(document("Pie de página", "El final está en la página {page of: end}", 600));
        int[] plain = layOut(document("Pie de página", "El final está en la página " + referred[0], 600));
        assertEquals(plain[0], referred[0]);
        assertTrue(referred[1] < plain[1] * 1.25, referred[1] + " layouts frente a " + plain[1]);
    }
}
