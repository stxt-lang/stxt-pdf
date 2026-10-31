package dev.stxt.pdf.layout.grid;

import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.Rect;
import org.junit.jupiter.api.Test;

import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Pages of 180 pt with margins of 40: page area of 515 × 100 from (40, 40). */
class GridFragmentationTest {

    private static final double PAGE = 180;

    private static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(), new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height())).toString());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private static final String[] GRID = {"\tGrid:", "\t\tId: G", "\t\tColumn sizes: * *"};

    private static String[] cell(String id, String cell) {
        return new String[]{"\t\tArea: " + cell, "\t\t\tBlock:", "\t\t\t\tId: " + id, "\t\t\t\tBox:",
                "\t\t\t\t\tHeight: 40"};
    }

    private static String[] concat(String[]... parts) {
        return java.util.Arrays.stream(parts).flatMap(java.util.Arrays::stream).toArray(String[]::new);
    }

    @Test
    void breaksBetweenRows() {
        // Rows of 40 at 0, 40 and 80: the third moves to page 2.
        LayoutFixture l = layOutPaged(PAGE, concat(GRID,
                cell("A", "1 1"), cell("B", "1 2"), cell("C", "2 1"), cell("D", "2 2"), cell("E", "3 1")));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 80, l.rect(1, "G"));
        assertRect(297.5, 80, 257.5, 40, l.rect(1, "D"));
        assertRect(40, 40, 515, 40, l.rect(2, "G"));
        assertRect(40, 40, 257.5, 40, l.rect(2, "E"));
    }

    private static String[] box(String id, String height, String cell) {
        return new String[]{"\t\tArea: " + cell, "\t\t\tBlock:", "\t\t\t\tId: " + id, "\t\t\t\tBox:",
                "\t\t\t\t\tHeight: " + height};
    }

    @Test
    void rowTallerThanAPageBreaksItsItemsInParallel() {
        // Row 1 measures 150: it does not fit even at the start of a page. A is split (100 + 50) and B fits
        // whole. On page 2, A continues from the top, and row 2 goes after what remains
        // of A: 150 − 100 = 50.
        LayoutFixture l = layOutPaged(PAGE, concat(GRID,
                box("A", "150", "1 1"), box("B", "60", "1 2"), box("C", "40", "2 1")));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "G"));
        assertRect(40, 40, 257.5, 100, l.rect(1, "A"));
        assertRect(297.5, 40, 257.5, 60, l.rect(1, "B"));
        assertRect(40, 40, 515, 90, l.rect(2, "G"));
        assertRect(40, 40, 257.5, 50, l.rect(2, "A"));
        assertFalse(l.has(2, "B"));
        assertRect(40, 90, 257.5, 40, l.rect(2, "C"));
    }

    @Test
    void rowThatFitsInAPageIsNotBrokenInParallel() {
        // After X (30), the row of 80 does not fit in what remains of the page but does fit in a whole one: it moves
        // whole to page 2 (like breaksBetweenRows).
        LayoutFixture l = layOutPaged(PAGE, concat(
                new String[]{"\tBlock:", "\t\tId: X", "\t\tBox:", "\t\t\tHeight: 30"}, GRID,
                box("A", "80", "1 1"), box("B", "40", "1 2")));
        assertEquals(2, l.pages());
        assertFalse(l.has(1, "G"));
        assertRect(40, 40, 257.5, 80, l.rect(2, "A"));
    }

    @Test
    void parallelBreakCanSpanSeveralPages() {
        // A measures 250 and its row does not fit on any page: 100 + 100 + 50, always from the top.
        LayoutFixture l = layOutPaged(PAGE, concat(GRID, box("A", "250", "1 1"), box("B", "120", "1 2")));
        assertEquals(3, l.pages());
        assertRect(40, 40, 257.5, 100, l.rect(1, "A"));
        assertRect(297.5, 40, 257.5, 100, l.rect(1, "B"));
        assertRect(40, 40, 257.5, 100, l.rect(2, "A"));
        assertRect(297.5, 40, 257.5, 20, l.rect(2, "B"));
        assertRect(40, 40, 515, 50, l.rect(3, "G"));
        assertFalse(l.has(3, "B"));
    }

    @Test
    void doesNotBreakInsideASpanningItem() {
        // S spans rows 2 and 3: a break between them is not possible, so the break goes before row 2.
        LayoutFixture l = layOutPaged(PAGE, concat(GRID,
                cell("A", "1 1"), cell("B", "1 2"), cell("S", "2 1 2 1"), cell("D", "2 2"), cell("E", "3 2")));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 40, l.rect(1, "G"));
        assertRect(40, 40, 515, 80, l.rect(2, "G"));
        assertRect(40, 40, 257.5, 40, l.rect(2, "S"));
        assertRect(297.5, 40, 257.5, 40, l.rect(2, "D"));
        assertRect(297.5, 80, 257.5, 40, l.rect(2, "E"));
    }

    @Test
    void framePercentOffsetUsesTheWholeHeightOfAFragmentedGrid() {
        // As in block flow: the 10 % is of the whole Height of the grid (30).
        LayoutFixture l = layOutPaged(PAGE,
                "\tGrid:", "\t\tId: G", "\t\tBox:", "\t\t\tHeight: 300", "\t\tRow sizes: 80 15",
                "\t\tArea: 2 1", "\t\t\tFrame:", "\t\t\t\tId: R", "\t\t\t\tOffset: 10% auto auto auto",
                "\t\t\t\tBox:", "\t\t\t\t\tHeight: 15");
        assertEquals(3, l.pages());
        assertRect(40, 150, 515, 15, l.rect(1, "R"));
    }

    /**
     * A Frame item of a row that is split in parallel is broken at its place in the flow, as in block flow: its
     * Offset moves its fragments afterwards and does not take space away from it. F (14 lines of 12) places 8 lines
     * on page 1 (96), as the same text without Offset would, and its fragments are 10 lower.
     */
    @Test
    void frameOffsetDoesNotChangeTheParallelBreak() {
        String text = String.join(" ", java.util.Collections.nCopies(40, "aaaaa"));
        LayoutFixture l = layOutPaged(PAGE, "\tGrid:", "\t\tColumn sizes: 100 100",
                "\t\tArea:", "\t\t\tVertical: start", "\t\t\tFrame:", "\t\t\t\tId: F",
                "\t\t\t\tOffset: 10 auto auto auto", "\t\t\t\tText: " + text,
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 150");
        assertEquals(2, l.pages());
        assertRect(40, 50, 100, 96, l.rect(1, "F"));
        assertRect(40, 50, 100, 72, l.rect(2, "F"));
        assertRect(140, 40, 100, 100, l.rect(1, "B"));
        assertRect(140, 40, 100, 50, l.rect(2, "B"));
    }

    /**
     * The second row (20 lines of 12 in a column of 30) does not fit on any page: it starts on page 2, where it is
     * split in parallel from the top even though in the whole grid it starts at 30 (mutation testing). T does not
     * stretch (8, 8 and 4 lines); B stretches to the row and is split with it: 100, 100 and 40.
     */
    @Test
    void aTallRowAfterTheFirstPageIsSplitFromTheTop() {
        String text = String.join(" ", java.util.Collections.nCopies(20, "aaaaa"));
        LayoutFixture l = layOutPaged(PAGE, "\tGrid:", "\t\tColumn sizes: 30 100",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 30",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 30",
                "\t\tArea:", "\t\t\tVertical: start", "\t\t\tText: " + text, "\t\t\t\tId: T",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: B");
        assertEquals(4, l.pages());
        assertFalse(l.has(1, "T"));
        assertRect(40, 40, 30, 96, l.rect(2, "T"));
        assertRect(70, 40, 100, 100, l.rect(2, "B"));
        assertRect(70, 40, 100, 40, l.rect(4, "B"));
    }

    /**
     * A text stretched to a row that is split has the height of the row (240) and its lines leave 4 at the end of each
     * page: that gap is not taken from it, and its last 4 lines fit on page 4 (it used to be BOX_OVERFLOW).
     */
    @Test
    void aStretchedTextInATallRowFitsItsLines() {
        String text = String.join(" ", java.util.Collections.nCopies(20, "aaaaa"));
        LayoutFixture l = layOutPaged(PAGE, "\tGrid:", "\t\tColumn sizes: 30 100",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 30",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 30",
                "\t\tArea:", "\t\t\tText: " + text, "\t\t\t\tId: T",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: B");
        assertEquals(4, l.pages());
        assertEquals(4, l.fragment(4, "T").lines().size());
        assertRect(40, 40, 30, 100, l.rect(2, "T"));
    }
}
