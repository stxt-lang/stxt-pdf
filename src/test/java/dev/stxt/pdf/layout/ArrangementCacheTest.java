package dev.stxt.pdf.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.stxt.DocumentReader;

/**
 * What a stack, a grid or a table arranges before breaking it between its units (placement, tracks, items at their
 * sizes, bands, the header and the footer of a table) is computed once per container and not once per page
 * ({@link LayoutContext#arranged}): it depends on the box and on its space without fragmentation, not on what is left
 * of the page. It is measured with the computed arrangements, which used to grow with the pages, and the test checks
 * too that paginating again forgets the arrangement of a container whose text changes.
 */
class ArrangementCacheTest {

    private static final String HEAD = "Document (dev.stxt.pdf):\n\tPage:\n\t\tPaper: A4\n\t\tMargin: 40\n"
            + "\tTitle: Reparto\n\tLanguage: es\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n"
            + "\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n\tSection:\n";

    /** A table of {@code rows} body rows and three columns, with a header row repeated on each page. */
    private static String table(int rows) {
        StringBuilder sb = new StringBuilder(HEAD);
        sb.append("\t\tTable:\n\t\t\tColumn sizes: * * *\n\t\t\tHeader rows:\n\t\t\t\tRow:\n");
        for (int c = 1; c <= 3; c++) {
            sb.append("\t\t\t\t\tCell: Columna ").append(c).append('\n');
        }
        sb.append("\t\t\tBody rows:\n");
        for (int i = 0; i < rows; i++) {
            sb.append("\t\t\t\tRow:\n");
            for (int c = 1; c <= 3; c++) {
                sb.append("\t\t\t\t\tCell: Fila ").append(i).append(", columna ").append(c).append('\n');
            }
        }
        return sb.toString();
    }

    /** A grid of {@code rows} rows and three columns of texts. */
    private static String grid(int rows) {
        StringBuilder sb = new StringBuilder(HEAD);
        sb.append("\t\tGrid:\n\t\t\tColumn sizes: * * *\n");
        for (int i = 0; i < rows; i++) {
            for (int c = 1; c <= 3; c++) {
                sb.append("\t\t\tArea:\n\t\t\t\tText: Fila ").append(i).append(", columna ").append(c).append('\n');
            }
        }
        return sb.toString();
    }

    /** A vertical stack of {@code items} texts. */
    private static String stack(int items) {
        StringBuilder sb = new StringBuilder(HEAD);
        sb.append("\t\tStack:\n\t\t\tDirection: vertical\n");
        for (int i = 0; i < items; i++) {
            sb.append("\t\t\tItem:\n\t\t\t\tText: Elemento ").append(i).append('\n');
        }
        return sb.toString();
    }

    /** The pages and the computed arrangements when laying out the document with a new engine. */
    private static int[] layOut(String text) {
        Engine engine = new Engine();
        int pages = engine.layOutDocument(new DocumentReader().read(text)).pages().size();
        return new int[] {pages, engine.arrangements()};
    }

    /** A container that fills one page and one that fills many are arranged the same number of times. */
    private static void assertArrangedOnce(String name, int[] onePage, int[] manyPages) {
        assertEquals(1, onePage[0], name + " of one page");
        assertTrue(manyPages[0] > 5, name + " needs many pages: " + manyPages[0]);
        assertEquals(onePage[1], manyPages[1], name + ": arrangements on one page versus on " + manyPages[0]);
        // The table is arranged by its grid and then prepared (header, body and footer): two per container.
        assertTrue(manyPages[1] <= 2, name + ": " + manyPages[1] + " arrangements");
    }

    @Test
    void aTableIsArrangedOnceHoweverManyPagesItTakes() {
        assertArrangedOnce("table", layOut(table(20)), layOut(table(500)));
    }

    @Test
    void aGridIsArrangedOnceHoweverManyPagesItTakes() {
        assertArrangedOnce("grid", layOut(grid(20)), layOut(grid(500)));
    }

    @Test
    void aStackIsArrangedOnceHoweverManyPagesItTakes() {
        assertArrangedOnce("stack", layOut(stack(20)), layOut(stack(500)));
    }

    /**
     * A container whose item writes {@code {page}}, followed by 100 page breaks: with 101 pages the text is measured
     * again with three figures ("Página 000", 50, does not fit in its box of 48 and takes two lines; with two, 45, it
     * would fit in one), so the arrangement of the container, which holds the layout of the item, has to be forgotten
     * with it.
     */
    private static String numbered(String container, String item) {
        StringBuilder sb = new StringBuilder(HEAD);
        sb.append("\t\t").append(container).append("\t\t\t").append(item)
                .append("\t\t\t\tText: Página {page}\n\t\t\t\t\tId: P\n\t\t\t\t\tBox:\n\t\t\t\t\t\tWidth: 48\n");
        for (int i = 0; i < 100; i++) {
            sb.append("\t\tBlock:\n\t\t\tBox:\n\t\t\t\tHeight: 10\n\t\t\tBreak:\n\t\t\t\tBefore: page\n");
        }
        return sb.toString();
    }

    private static Fragment find(Fragment f, String id) {
        if (id.equals(f.box().id())) {
            return f;
        }
        for (Fragment child : f.children()) {
            Fragment found = find(child, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static void assertMeasuredAgain(String document) {
        LaidOutDocument d = new Engine().layOutDocument(new DocumentReader().read(document));
        assertEquals(101, d.pages().size());
        Page first = d.pages().get(0);
        Fragment text = find(first.content(), "P");
        assertNotNull(text);
        assertEquals(2, text.lines().size());
    }

    @Test
    void aThirdDigitIsMeasuredInTheItemOfAGrid() {
        assertMeasuredAgain(numbered("Grid:\n\t\t\tColumn sizes: *\n", "Area:\n"));
    }

    @Test
    void aThirdDigitIsMeasuredInTheItemOfAStack() {
        assertMeasuredAgain(numbered("Stack:\n", "Item:\n"));
    }
}
