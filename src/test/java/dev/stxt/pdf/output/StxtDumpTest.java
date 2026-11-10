package dev.stxt.pdf.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.layout.Engine;
import dev.stxt.pdf.stxt.DocumentReader;

/**
 * The dump of a layout (reference/implementation/testing.md, "The dump"), the format of the {@code expected.stxt}:
 * only the boxes with an {@code Id}, nested, with their border box in page coordinates; the marker of an item and the
 * merged borders of a table after their {@code Rect}. The documents are laid out with the default metrics, in which
 * each character of 10 pt measures 5 and a line of text 12, on pages of 300 × 200 with margins of 20: the page area
 * is 260 × 160 from (20, 20).
 */
class StxtDumpTest {

    private static final String DOCUMENT = Docs.header(300, 200, 20) + Docs.TYPOGRAPHY;

    private static List<Page> layOut(String document) {
        return new Engine().layOutDocument(new DocumentReader().read(Docs.sectioned(document))).pages();
    }

    @Test
    void onlyTheBoxesWithAnIdAreDumpedNestedAndInPageCoordinates() {
        List<Page> pages = layOut(DOCUMENT
                + "\tBlock:\n\t\tId: outer\n\t\tBox:\n\t\t\tPadding: 5\n"
                + "\t\tText: Hola\n\t\t\tId: inner\n"
                + "\t\tText: Sin id\n"
                + "\tText: Fuera\n\t\tId: next\n\t\tBox:\n\t\t\tWidth: 100.5\n");
        // The block: padding 5, two lines of 12 and padding 5. The text inside it, at (25, 25); the one after it, at
        // 20 + 34. The text without Id is not dumped, but its space is there.
        assertEquals("Result:\n"
                + "\tPage: 1\n"
                + "\t\tFragment: outer\n"
                + "\t\t\tRect: 20 20 260 34\n"
                + "\t\t\tFragment: inner\n"
                + "\t\t\t\tRect: 25 25 250 12\n"
                + "\t\tFragment: next\n"
                + "\t\t\tRect: 20 54 100.5 12\n", StxtDump.dump(pages, false));
        // With the boxes without Id, each one is named by its line.
        String all = StxtDump.dump(pages, true);
        assertTrue(all.contains("\t\t\tFragment: at line "), all);
        assertTrue(all.contains("\t\t\tFragment: inner\n"), all);
        assertTrue(all.contains("\t\tFragment: next\n"), all);
    }

    @Test
    void theMarkerOfAnItemGoesInsideItAndEachPageIsDumped() {
        List<Page> pages = layOut(DOCUMENT
                + "\tList:\n\t\tMarker: decimal\n\t\tBox:\n\t\t\tPadding: 0 0 0 20\n"
                + "\t\tList item:\n\t\t\tId: first\n\t\t\tText: Uno\n"
                + "\t\tList item:\n\t\t\tText: Dos\n"
                + "\tText: Tres\n\t\tId: second\n\t\tBreak:\n\t\t\tBefore: page\n");
        // The items start at 40 (the padding of the list); the marker "1." (10 wide and a gap of 5) hangs to their
        // left, ending where they start. The second item has no Id: neither it nor its marker is dumped.
        assertEquals("Result:\n"
                + "\tPage: 1\n"
                + "\t\tFragment: first\n"
                + "\t\t\tRect: 40 20 240 12\n"
                + "\t\t\tMarker: 1.\n"
                + "\t\t\t\tRect: 25 20 15 12\n"
                + "\tPage: 2\n"
                + "\t\tFragment: second\n"
                + "\t\t\tRect: 20 20 260 12\n", StxtDump.dump(pages, false));
    }

    @Test
    void theMergedBordersOfATableAreRulesInTheOrderTheyArePainted() {
        List<Page> pages = layOut(DOCUMENT
                + "\t\tFor element: Cell\n\t\t\tBox:\n\t\t\t\tPadding: 0\n\t\t\t\tBorder: 1\n\t\t\t\tBorder color: #333333\n"
                + "\t\tFor element: Table\n\t\t\tBox:\n\t\t\t\tMerge borders: true\n"
                + "\tTable:\n\t\tId: t\n\t\tColumn sizes: 100 100\n\t\tBody rows:\n\t\t\tRow:\n"
                + "\t\t\t\tCell:\n\t\t\t\t\tId: a\n\t\t\t\t\tBlock:\n\t\t\t\t\t\tBox:\n\t\t\t\t\t\t\tHeight: 20\n"
                + "\t\t\t\tCell:\n\t\t\t\t\tBlock:\n\t\t\t\t\t\tBox:\n\t\t\t\t\t\t\tHeight: 20\n");
        // The table takes the width of the page area and its columns, 200, start at its left; the row measures 20 plus
        // the two edge lines. The vertical lines first (the edges inside the table, the middle one centered on 120),
        // then the horizontal ones; the lines are not nested in the cells.
        assertEquals("Result:\n"
                + "\tPage: 1\n"
                + "\t\tFragment: t\n"
                + "\t\t\tRect: 20 20 260 22\n"
                + "\t\t\tRule: 20 20 1 22 #333333\n"
                + "\t\t\tRule: 119.5 20 1 22 #333333\n"
                + "\t\t\tRule: 219 20 1 22 #333333\n"
                + "\t\t\tRule: 20 20 200 1 #333333\n"
                + "\t\t\tRule: 20 41 200 1 #333333\n"
                + "\t\t\tFragment: a\n"
                + "\t\t\t\tRect: 20 20 100 22\n", StxtDump.dump(pages, false));
    }

    @Test
    void numbersHaveUpToTwoDecimalsWithoutTrailingZeros() {
        assertEquals("40", StxtDump.format(40));
        assertEquals("10.5", StxtDump.format(10.50));
        assertEquals("12.35", StxtDump.format(12.345));
        assertEquals("0.3", StxtDump.format(0.1 + 0.2));
        assertEquals("0", StxtDump.format(-0.004));
        assertEquals("-1.5", StxtDump.format(-1.5));
    }
}
