package dev.stxt.pdf.layout.grid;

import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.Rect;

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
        // Rows of 40 at 0, 40 and 80: with Split rows: false, the third moves whole to page 2.
        LayoutFixture l = layOutPaged(PAGE, concat(GRID, new String[]{"\t\tSplit rows: false"},
                cell("A", "1 1"), cell("B", "1 2"), cell("C", "2 1"), cell("D", "2 2"), cell("E", "3 1")));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 80, l.rect(1, "G"));
        assertRect(297.5, 80, 257.5, 40, l.rect(1, "D"));
        assertRect(40, 40, 515, 40, l.rect(2, "G"));
        assertRect(40, 40, 257.5, 40, l.rect(2, "E"));
    }

    @Test
    void keepWithNextOfAnItemMovesTheBreakBeforeItsRow() {
        // The same grid with Keep with next on D, in row 2: the break before row 3 violates it, so the last of the
        // perfect breaks is before row 2, and page 1 only holds the first row.
        LayoutFixture l = layOutPaged(PAGE, concat(GRID, new String[]{"\t\tSplit rows: false"},
                cell("A", "1 1"), cell("B", "1 2"), cell("C", "2 1"),
                cell("D", "2 2"), new String[]{"\t\t\t\tBreak:", "\t\t\t\t\tKeep with next: true"},
                cell("E", "3 1")));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 40, l.rect(1, "G"));
        l.assertAbsent(1, "C");
        assertRect(40, 40, 515, 80, l.rect(2, "G"));
        assertRect(297.5, 40, 257.5, 40, l.rect(2, "D"));
        assertRect(40, 80, 257.5, 40, l.rect(2, "E"));
    }

    @Test
    void splitsTheRowThatDoesNotFitOnThisPage() {
        // Rows of 40 at 0, 40 and 80: the third does not fit in the 20 left and is split, 20 + 20 (Split rows by
        // default).
        LayoutFixture l = layOutPaged(PAGE, concat(GRID,
                cell("A", "1 1"), cell("B", "1 2"), cell("C", "2 1"), cell("D", "2 2"), cell("E", "3 1")));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "G"));
        assertRect(297.5, 80, 257.5, 40, l.rect(1, "D"));
        assertRect(40, 120, 257.5, 20, l.rect(1, "E"));
        assertRect(40, 40, 515, 20, l.rect(2, "G"));
        assertRect(40, 40, 257.5, 20, l.rect(2, "E"));
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
        l.assertAbsent(2, "B");
        assertRect(40, 90, 257.5, 40, l.rect(2, "C"));
    }

    @Test
    void rowThatFitsInAPageMovesWholeWithoutSplitRows() {
        // After X (30), the row of 80 does not fit in what remains of the page but does fit in a whole one: with
        // Split rows: false it moves whole to page 2 (like breaksBetweenRows).
        LayoutFixture l = layOutPaged(PAGE, concat(
                new String[]{"\tBlock:", "\t\tId: X", "\t\tBox:", "\t\t\tHeight: 30"}, GRID,
                new String[]{"\t\tSplit rows: false"}, box("A", "80", "1 1"), box("B", "40", "1 2")));
        assertEquals(2, l.pages());
        l.assertAbsent(1, "G");
        assertRect(40, 40, 257.5, 80, l.rect(2, "A"));
    }

    @Test
    void rowThatDoesNotFitIsSplitOnThisPage() {
        // After X (30), the row of 80 does not fit in the 70 left: A is split (70 + 10) and B (40) fits whole.
        LayoutFixture l = layOutPaged(PAGE, concat(
                new String[]{"\tBlock:", "\t\tId: X", "\t\tBox:", "\t\t\tHeight: 30"}, GRID,
                box("A", "80", "1 1"), box("B", "40", "1 2")));
        assertEquals(2, l.pages());
        assertRect(40, 70, 515, 70, l.rect(1, "G"));
        assertRect(40, 70, 257.5, 70, l.rect(1, "A"));
        assertRect(297.5, 70, 257.5, 40, l.rect(1, "B"));
        assertRect(40, 40, 515, 10, l.rect(2, "G"));
        assertRect(40, 40, 257.5, 10, l.rect(2, "A"));
        l.assertAbsent(2, "B");
    }

    @Test
    void splitRowsRespectKeepTogetherOfAnItem() {
        // The same row, but A cannot be split: the break before the row is better, and the row moves whole.
        LayoutFixture l = layOutPaged(PAGE, concat(
                new String[]{"\tBlock:", "\t\tId: X", "\t\tBox:", "\t\t\tHeight: 30"}, GRID,
                box("A", "80", "1 1"), new String[]{"\t\t\t\tBreak:", "\t\t\t\t\tKeep together: true"},
                box("B", "40", "1 2")));
        assertEquals(2, l.pages());
        l.assertAbsent(1, "G");
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
        l.assertAbsent(3, "B");
    }

    @Test
    void doesNotBreakInsideASpanningItem() {
        // S spans rows 2 and 3: a break between them is not possible, so with Split rows: false the break goes
        // before row 2.
        LayoutFixture l = layOutPaged(PAGE, concat(GRID, new String[]{"\t\tSplit rows: false"},
                cell("A", "1 1"), cell("B", "1 2"), cell("S", "2 1 2 1"), cell("D", "2 2"), cell("E", "3 2")));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 40, l.rect(1, "G"));
        assertRect(40, 40, 515, 80, l.rect(2, "G"));
        assertRect(40, 40, 257.5, 40, l.rect(2, "S"));
        assertRect(297.5, 40, 257.5, 40, l.rect(2, "D"));
        assertRect(297.5, 80, 257.5, 40, l.rect(2, "E"));
    }

    @Test
    void splitsTheGroupOfRowsOfASpanningItemOnThisPage() {
        // The same grid with Split rows: the group of rows 2 and 3 (40 to 120) does not fit in the 60 left and is
        // split in parallel: S (40, not stretched) and D fit whole, and E (80 to 120) is split, 20 + 20.
        LayoutFixture l = layOutPaged(PAGE, concat(GRID,
                cell("A", "1 1"), cell("B", "1 2"), cell("S", "2 1 2 1"), cell("D", "2 2"), cell("E", "3 2")));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "G"));
        assertRect(40, 80, 257.5, 40, l.rect(1, "S"));
        assertRect(297.5, 80, 257.5, 40, l.rect(1, "D"));
        assertRect(297.5, 120, 257.5, 20, l.rect(1, "E"));
        assertRect(40, 40, 515, 20, l.rect(2, "G"));
        assertRect(297.5, 40, 257.5, 20, l.rect(2, "E"));
        l.assertAbsent(2, "S");
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
                "\t\tArea:", "\t\t\tVertical: top", "\t\t\tFrame:", "\t\t\t\tId: F",
                "\t\t\t\tOffset: 10 auto auto auto", "\t\t\t\tText: " + text,
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 150");
        assertEquals(2, l.pages());
        assertRect(40, 50, 100, 96, l.rect(1, "F"));
        assertRect(40, 50, 100, 72, l.rect(2, "F"));
        assertRect(140, 40, 100, 100, l.rect(1, "B"));
        assertRect(140, 40, 100, 50, l.rect(2, "B"));
    }

    /**
     * The second row (20 lines of 12 in a column of 30) does not fit on any page: with Split rows: false it starts
     * on page 2, where it is split in parallel from the top even though in the whole grid it starts at 30 (mutation
     * testing). T does not stretch (8, 8 and 4 lines); B stretches to the row and is split with it: 100, 100 and the
     * 48 of the last 4 lines of T (aStretchedItemEndsWithItsSplitRow).
     */
    @Test
    void aTallRowAfterTheFirstPageIsSplitFromTheTop() {
        String text = String.join(" ", java.util.Collections.nCopies(20, "aaaaa"));
        LayoutFixture l = layOutPaged(PAGE, "\tGrid:", "\t\tColumn sizes: 30 100", "\t\tSplit rows: false",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 30",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 30",
                "\t\tArea:", "\t\t\tVertical: top", "\t\t\tText: " + text, "\t\t\t\tId: T",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: B");
        assertEquals(4, l.pages());
        l.assertAbsent(1, "T");
        assertRect(40, 40, 30, 96, l.rect(2, "T"));
        assertRect(70, 40, 100, 100, l.rect(2, "B"));
        assertRect(70, 40, 100, 48, l.rect(4, "B"));
    }

    /**
     * A column of a grid that is split on the page it is on (Split rows) contains a grid whose text, stretched to its
     * row, reaches the end of page 1 (70) with 5 of its 10 lines (60): the gap of 10 is not taken from the column
     * either, which has the height of the row (120: C is the taller item) and still has 60 for the other 5 lines on
     * page 2 (it used to be BOX_OVERFLOW).
     */
    @Test
    void aStretchedTextInASplitRowDoesNotOverflowItsColumn() {
        String text = String.join(" ", java.util.Collections.nCopies(10, "aaaaa"));
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: X", "\t\tBox:", "\t\t\tHeight: 30",
                "\tGrid:", "\t\tId: G", "\t\tColumn sizes: * *",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 100",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: C",
                "\t\t\t\tGrid:", "\t\t\t\t\tColumn sizes: 20 30",
                "\t\t\t\t\tArea:", "\t\t\t\t\t\tVertical: top", "\t\t\t\t\t\tBlock:", "\t\t\t\t\t\t\tBox:",
                "\t\t\t\t\t\t\t\tHeight: 10",
                "\t\t\t\t\tArea:", "\t\t\t\t\t\tText: " + text, "\t\t\t\t\t\t\tId: T");
        assertEquals(2, l.pages());
        assertRect(40, 70, 515, 70, l.rect(1, "G"));
        assertRect(40, 70, 257.5, 70, l.rect(1, "A"));
        assertRect(297.5, 70, 257.5, 70, l.rect(1, "C"));
        assertEquals(5, l.fragment(1, "T").lines().size());
        assertRect(40, 40, 515, 60, l.rect(2, "G"));
        assertRect(40, 40, 257.5, 30, l.rect(2, "A"));
        assertRect(297.5, 40, 257.5, 60, l.rect(2, "C"));
        assertEquals(5, l.fragment(2, "T").lines().size());
    }

    /**
     * T (20 lines of 12 in a column of 30, one word each) is stretched to its row (240) and leaves 4 at the end of
     * each page: 100, 100 and 48. S, empty and stretched to the same row, measures what the row does on each page,
     * also on the last one: 48, not the 40 that are left of its 240 after 100 and 100.
     */
    @Test
    void aStretchedItemEndsWithItsSplitRow() {
        String text = String.join(" ", java.util.Collections.nCopies(20, "aaaaa"));
        LayoutFixture l = layOutPaged(PAGE, "\tGrid:", "\t\tColumn sizes: 100 30",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: S",
                "\t\tArea:", "\t\t\tText: " + text, "\t\t\t\tId: T");
        assertEquals(3, l.pages());
        assertRect(40, 40, 100, 100, l.rect(1, "S"));
        assertRect(40, 40, 100, 100, l.rect(2, "S"));
        assertRect(140, 40, 30, 48, l.rect(3, "T"));
        assertRect(40, 40, 100, 48, l.rect(3, "S"));
    }

    /**
     * T has 25 lines (300): 8, 8, 7 and 2, so that the last page does not have a single line (widows). S, stretched to
     * the same 300, used up its height at the end of page 3 (100 + 100 + 100) and was missing from page 4: it goes on
     * to the end of its row, with the 24 of the last two lines of T.
     */
    @Test
    void aStretchedItemDoesNotEndBeforeItsSplitRow() {
        String text = String.join(" ", java.util.Collections.nCopies(25, "aaaaa"));
        LayoutFixture l = layOutPaged(PAGE, "\tGrid:", "\t\tColumn sizes: 100 30",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: S",
                "\t\tArea:", "\t\t\tText: " + text, "\t\t\t\tId: T");
        assertEquals(4, l.pages());
        assertRect(40, 40, 100, 100, l.rect(3, "S"));
        assertRect(140, 40, 30, 24, l.rect(4, "T"));
        assertRect(40, 40, 100, 24, l.rect(4, "S"));
    }

    /**
     * L takes up rows 1 (30) and 2 (T, 20 lines: 240), so the two are split together: 5, 8 and 7 lines of T. L, whose
     * area ends with them, measures on each page what they do: 100, 100 and the 84 of the last 7 lines, not the 70 left
     * of its 270. K is stretched to row 1 only and ends with it: 30 on page 1, without reaching the end of the page
     * over T.
     */
    @Test
    void aStretchedItemOfARowBeforeTheLastOfItsGroupEndsWithItsOwnRow() {
        String text = String.join(" ", java.util.Collections.nCopies(20, "aaaaa"));
        LayoutFixture l = layOutPaged(PAGE, "	Grid:", "		Column sizes: 100 30", "		Row sizes: 30 auto",
                "		Area: 1 1 2 1", "			Block:", "				Id: L",
                "		Area: 1 2", "			Block:", "				Id: K",
                "		Area: 2 2", "			Text: " + text, "				Id: T");
        assertEquals(3, l.pages());
        assertRect(40, 40, 100, 100, l.rect(1, "L"));
        assertRect(140, 40, 30, 30, l.rect(1, "K"));
        assertRect(140, 70, 30, 70, l.rect(1, "T"));
        l.assertAbsent(2, "K");
        l.assertAbsent(3, "K");
        assertRect(140, 40, 30, 84, l.rect(3, "T"));
        assertRect(40, 40, 100, 84, l.rect(3, "L"));
    }

    /**
     * S is stretched to the row of T (240), but its Max height keeps it at 50: it does not end with the row, and it is
     * not stretched to the end of each page with it.
     */
    @Test
    void aStretchedItemShorterThanItsRowIsNotStretchedWithIt() {
        String text = String.join(" ", java.util.Collections.nCopies(20, "aaaaa"));
        LayoutFixture l = layOutPaged(PAGE, "	Grid:", "		Column sizes: 100 30",
                "		Area:", "			Block:", "				Id: S", "				Box:", "					Max height: 50",
                "		Area:", "			Text: " + text, "				Id: T");
        assertEquals(3, l.pages());
        assertRect(40, 40, 100, 50, l.rect(1, "S"));
        l.assertAbsent(2, "S");
        l.assertAbsent(3, "S");
    }

    /**
     * The same item with Overflow: clip, which is not broken, after a block of 20: it stays whole on page 1 (stretching
     * it to the end of the page left a token of a box that places nothing among those of the row).
     */
    @Test
    void aStretchedItemThatIsNotBrokenStaysWhole() {
        String text = String.join(" ", java.util.Collections.nCopies(20, "aaaaa"));
        LayoutFixture l = layOutPaged(PAGE, "	Block:", "		Box:", "			Height: 20",
                "	Grid:", "		Column sizes: 100 30",
                "		Area:", "			Block:", "				Id: S", "				Box:", "					Max height: 50",
                "					Overflow: clip",
                "		Area:", "			Text: " + text, "				Id: T");
        assertEquals(3, l.pages());
        assertRect(40, 60, 100, 50, l.rect(1, "S"));
        l.assertAbsent(2, "S");
        assertRect(140, 40, 30, 72, l.rect(3, "T"));
    }

    /**
     * S has a bottom margin of 10: stretched to the row of T (240), it measures 230. On the last page the row ends with
     * the 48 of the last 4 lines of T, and S, 10 before: 38.
     */
    @Test
    void aStretchedItemEndsWithItsSplitRowWithoutItsBottomMargin() {
        String text = String.join(" ", java.util.Collections.nCopies(20, "aaaaa"));
        LayoutFixture l = layOutPaged(PAGE, "	Grid:", "		Column sizes: 100 30",
                "		Area:", "			Block:", "				Id: S", "				Box:", "					Margin: 0 0 10 0",
                "		Area:", "			Text: " + text, "				Id: T");
        assertEquals(3, l.pages());
        assertRect(40, 40, 100, 100, l.rect(2, "S"));
        assertRect(140, 40, 30, 48, l.rect(3, "T"));
        assertRect(40, 40, 100, 38, l.rect(3, "S"));
    }

    /**
     * The column C, stretched to its row (240), holds a grid whose row has S, empty and stretched, and T (20 lines:
     * 8, 8 and 4). S reaches the end of pages 1 and 2 (100 and 100), but the row is only consumed as far as T, the
     * slowest item that goes on (96 and 96): C still has 48 for its last 4 lines (it used to be BOX_OVERFLOW, with
     * 40). A, the other column, ends with them.
     */
    @Test
    void aSplitRowIsConsumedAsFarAsItsSlowestItem() {
        String text = String.join(" ", java.util.Collections.nCopies(20, "aaaaa"));
        LayoutFixture l = layOutPaged(PAGE, "	Grid:", "		Column sizes: * *",
                "		Area:", "			Block:", "				Id: A",
                "		Area:", "			Block:", "				Id: C",
                "				Grid:", "					Column sizes: 100 30",
                "					Area:", "						Block:", "							Id: S",
                "					Area:", "						Text: " + text, "							Id: T");
        assertEquals(3, l.pages());
        assertRect(297.5, 40, 257.5, 100, l.rect(2, "C"));
        assertRect(297.5, 40, 100, 100, l.rect(2, "S"));
        assertRect(297.5, 40, 257.5, 48, l.rect(3, "C"));
        assertRect(297.5, 40, 100, 48, l.rect(3, "S"));
        assertEquals(4, l.fragment(3, "T").lines().size());
        assertRect(40, 40, 257.5, 48, l.rect(3, "A"));
    }

    /**
     * Two texts of 240 in the row of a grid inside the column C: T (20 lines of 12) places 8 lines on each page (96)
     * and U (16 lines of 15), 6 (90). The row is consumed as far as U, so C still has 60 for the last 4 lines of U on
     * page 3 (it used to be BOX_OVERFLOW, with the 48 of T).
     */
    @Test
    void twoTextsThatAdvanceDifferentlyDoNotOverflowTheirColumn() {
        String text = String.join(" ", java.util.Collections.nCopies(20, "aaaaa"));
        String large = String.join(" ", java.util.Collections.nCopies(16, "aaaaa"));
        LayoutFixture l = layOutPaged(PAGE, "	Grid:", "		Column sizes: *",
                "		Area:", "			Block:", "				Id: C",
                "				Grid:", "					Column sizes: 30 60",
                "					Area:", "						Text: " + text, "							Id: T",
                "					Area:", "						Text: " + large, "							Id: U",
                "							Typography:", "								Size: 12.5");
        assertEquals(3, l.pages());
        assertEquals(6, l.fragment(1, "U").lines().size());
        assertEquals(4, l.fragment(3, "T").lines().size());
        assertEquals(4, l.fragment(3, "U").lines().size());
        assertRect(40, 40, 515, 60, l.rect(3, "C"));
    }

    /**
     * B (99, at the top of its area) ends on page 1 beyond the 96 of the 8 lines of T, which goes on: the row is
     * consumed as far as T, not as far as B, and the column C still has 48 for the last 4 lines (it used to be
     * BOX_OVERFLOW, with 45).
     */
    @Test
    void anItemThatEndsBeyondTheOnesThatGoOnDoesNotConsumeTheirRow() {
        String text = String.join(" ", java.util.Collections.nCopies(20, "aaaaa"));
        LayoutFixture l = layOutPaged(PAGE, "	Grid:", "		Column sizes: *",
                "		Area:", "			Block:", "				Id: C",
                "				Grid:", "					Column sizes: 100 30",
                "					Area:", "						Vertical: top", "						Block:", "							Id: B",
                "							Box:", "								Height: 99",
                "					Area:", "						Text: " + text, "							Id: T");
        assertEquals(3, l.pages());
        assertRect(40, 40, 100, 99, l.rect(1, "B"));
        assertEquals(4, l.fragment(3, "T").lines().size());
        assertRect(40, 40, 515, 48, l.rect(3, "C"));
    }

    /**
     * Rows 1 (30) and 2 (T, 20 lines) are split together inside the column C (270): L, which takes up both, reaches
     * the end of page 1 (100), and T, which starts at 30, places 5 lines (90). The rows are consumed as far as T, and
     * C still has 84 for its last 7 lines on page 3 (it used to be BOX_OVERFLOW, with 70).
     */
    @Test
    void rowsSplitTogetherAreConsumedAsFarAsTheirSlowestItem() {
        String text = String.join(" ", java.util.Collections.nCopies(20, "aaaaa"));
        LayoutFixture l = layOutPaged(PAGE, "	Grid:", "		Column sizes: *",
                "		Area:", "			Block:", "				Id: C",
                "				Grid:", "					Column sizes: 100 30", "					Row sizes: 30 auto",
                "					Area: 1 1 2 1", "						Block:", "							Id: L",
                "					Area: 1 2", "						Block:", "							Id: K",
                "					Area: 2 2", "						Text: " + text, "							Id: T");
        assertEquals(3, l.pages());
        assertEquals(5, l.fragment(1, "T").lines().size());
        assertEquals(7, l.fragment(3, "T").lines().size());
        assertRect(40, 40, 100, 84, l.rect(3, "L"));
        assertRect(40, 40, 515, 84, l.rect(3, "C"));
    }

    /**
     * A text stretched to a row that is split has the height of the row (240) and its lines leave 4 at the end of each
     * page: that gap is not taken from it, and its last 4 lines fit on page 4 (it used to be BOX_OVERFLOW).
     */
    @Test
    void aStretchedTextInATallRowFitsItsLines() {
        String text = String.join(" ", java.util.Collections.nCopies(20, "aaaaa"));
        LayoutFixture l = layOutPaged(PAGE, "\tGrid:", "\t\tColumn sizes: 30 100", "\t\tSplit rows: false",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 30",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 30",
                "\t\tArea:", "\t\t\tText: " + text, "\t\t\t\tId: T",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: B");
        assertEquals(4, l.pages());
        assertEquals(4, l.fragment(4, "T").lines().size());
        assertRect(40, 40, 30, 100, l.rect(2, "T"));
    }
}
