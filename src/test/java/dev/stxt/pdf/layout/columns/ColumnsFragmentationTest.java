package dev.stxt.pdf.layout.columns;

import dev.stxt.pdf.layout.LayoutFixture;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static dev.stxt.pdf.layout.columns.ColumnsLayoutTest.assertRect;
import static dev.stxt.pdf.layout.columns.ColumnsLayoutTest.block;
import static dev.stxt.pdf.layout.columns.ColumnsLayoutTest.columnsElement;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Blocks in columns on pages of 180 pt with margins of 40: page area of 515 × 100 from
 * (40, 40). With {@code Column count: 2} and {@code Gap: 15}, the second column starts at x = 305.
 */
class ColumnsFragmentationTest {

    private static final double PAGE = 180;
    private static final List<String> TWO = List.of("Column count: 2", "Gap: 15");

    /** Blocks of 25 with {@code Id} A, B, C...: four fit in a column. */
    private static List<String>[] blocks(int n) {
        return blocks(n, false);
    }

    /** Same; {@code whole}: with {@code Keep together: true}. */
    @SuppressWarnings("unchecked")
    private static List<String>[] blocks(int n, boolean whole) {
        List<List<String>> blocks = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            blocks.add(block(String.valueOf((char) ('A' + i)), 25, whole));
        }
        return blocks.toArray(List[]::new);
    }

    @Test
    void fillsTheColumnsOfThePageAndBalancesTheLastOne() {
        LayoutFixture l = layOutPaged(PAGE, columnsElement(TWO, blocks(10)));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "M"));
        assertRect(40, 115, 250, 25, l.rect(1, "D"));
        assertRect(305, 40, 250, 25, l.rect(1, "E"));
        assertRect(305, 115, 250, 25, l.rect(1, "H"));
        // Page 2: I and J, balanced in two columns of 25.
        assertRect(40, 40, 515, 25, l.rect(2, "M"));
        assertRect(40, 40, 250, 25, l.rect(2, "I"));
        assertRect(305, 40, 250, 25, l.rect(2, "J"));
    }

    @Test
    void fillAutoDoesNotBalanceTheLastPage() {
        LayoutFixture l = layOutPaged(PAGE, columnsElement(List.of("Column count: 2", "Gap: 15", "Balance: false"), blocks(10)));
        assertRect(40, 40, 515, 50, l.rect(2, "M"));
        assertRect(40, 65, 250, 25, l.rect(2, "J"));
    }

    @Test
    void pageBreakLeavesTheRemainingColumns() {
        LayoutFixture l = layOutPaged(PAGE, columnsElement(TWO, block("A", 25, false),
                block("B", 25, false, "\t\t\tBreak:", "\t\t\t\tBefore: page"), block("C", 25, false)));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 25, l.rect(1, "M"));
        l.assertAbsent(1, "B");
        assertRect(40, 40, 250, 25, l.rect(2, "B"));
        assertRect(305, 40, 250, 25, l.rect(2, "C"));
    }

    @Test
    void columnBreakInTheLastColumnGoesToTheNextPage() {
        List<String> column = List.of("\t\t\tBreak:", "\t\t\t\tBefore: column");
        LayoutFixture l = layOutPaged(PAGE, columnsElement(TWO, block("A", 25, false),
                block("B", 25, false, column.toArray(String[]::new)),
                block("C", 25, false, "\t\t\tBreak:", "\t\t\t\tBefore: column", "\t\t\t\tKeep together: true")));
        assertEquals(2, l.pages());
        assertRect(305, 40, 250, 25, l.rect(1, "B"));
        assertRect(40, 40, 250, 25, l.rect(2, "C"));
    }

    /**
     * The first column cannot place any line in the 10 pt that remain: the Columns moves whole to page 2.
     */
    @Test
    void columnsThatCannotStartMoveToTheNextPage() {
        LayoutFixture l = layOutPaged(PAGE, "\tBlock:", "\t\tBox:", "\t\t\tHeight: 90",
                "\tColumns:", "\t\tId: M", "\t\tColumn count: 2", "\t\tText: Hola");
        l.assertAbsent(1, "M");
        assertRect(40, 40, 515, 12, l.rect(2, "M"));
    }

    /** Splitting a block with {@code Keep together: true} is the worst break: the break goes before the Columns. */
    @Test
    void keepTogetherInTheFirstColumnMovesTheColumns() {
        List<String> lines = new ArrayList<>(List.of("\tBlock:", "\t\tBox:", "\t\t\tHeight: 90"));
        lines.addAll(List.of(columnsElement(TWO, block("A", 25, true))));
        LayoutFixture l = layOutPaged(PAGE, lines.toArray(String[]::new));
        l.assertAbsent(1, "M");
        assertRect(40, 40, 250, 25, l.rect(2, "A"));
    }

    /** A {@code Height} of 150 is the total: 100 on page 1 and 50 on page 2, which are filled in order. */
    @Test
    void heightAcrossPages() {
        LayoutFixture l = layOutPaged(PAGE, columnsElement(List.of("Column count: 2", "Gap: 15", "Balance: false", "Box:",
                "\tHeight: 150"), blocks(10)));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "M"));
        assertRect(305, 115, 250, 25, l.rect(1, "H"));
        assertRect(40, 40, 515, 50, l.rect(2, "M"));
        assertRect(40, 65, 250, 25, l.rect(2, "J"));
    }

    @Test
    void closedEdgesRepeatBorderAndPadding() {
        LayoutFixture l = layOutPaged(PAGE, columnsElement(List.of("Column count: 2", "Gap: 15", "Box:", "\tPadding: 5",
                "Break:", "\tEdges: closed"), blocks(8, true)));
        // Columns of 90 between the 5 at the top and at the bottom, and 245 wide: three whole blocks
        // per column on page 1.
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 85, l.rect(1, "M"));
        assertRect(305, 45, 245, 25, l.rect(1, "D"));
        assertRect(305, 95, 245, 25, l.rect(1, "F"));
        assertRect(40, 40, 515, 35, l.rect(2, "M"));
        assertRect(45, 45, 245, 25, l.rect(2, "G"));
        assertRect(305, 45, 245, 25, l.rect(2, "H"));
    }

    // Cases that were missing according to mutation testing.

    /**
     * Height: 160 with clone and padding of 5: on page 1 the columns measure 90 (limited by the page) and three
     * blocks of 25 fit per column, which consume 75 (reference/common/breaks.md); on page 2 they have 160 − 10 − 75 =
     * 75 left, another three per column.
     */
    @Test
    void cloneWithHeightLeavesTheRestToTheLastPage() {
        LayoutFixture l = layOutPaged(PAGE, columnsElement(List.of("Column count: 2", "Gap: 15", "Balance: false", "Box:",
                "\tHeight: 160", "\tPadding: 5", "Break:", "\tEdges: closed"), blocks(10, true)));
        assertEquals(2, l.pages());
        assertRect(305, 95, 245, 25, l.rect(1, "F"));
        assertRect(45, 70, 245, 25, l.rect(2, "H"));
        assertRect(45, 95, 245, 25, l.rect(2, "I"));
        assertRect(305, 45, 245, 25, l.rect(2, "J"));
    }

    /** The content fits on page 1, but Height: 150 continues: page 2 has the rest of the box, empty. */
    @Test
    void theHeightGoesOnAfterTheContent() {
        LayoutFixture l = layOutPaged(PAGE, columnsElement(List.of("Column count: 2", "Gap: 15", "Box:", "\tHeight: 150"),
                blocks(2)));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "M"));
        assertRect(40, 40, 515, 50, l.rect(2, "M"));
    }

    /** Height: 150 with twelve blocks: on page 2, 50 remain, two blocks per column (K starts the second). */
    @Test
    void theRestOfTheHeightLimitsTheLastPage() {
        LayoutFixture l = layOutPaged(PAGE, columnsElement(List.of("Column count: 2", "Gap: 15", "Balance: false", "Box:",
                "\tHeight: 150"), blocks(12, true)));
        assertEquals(2, l.pages());
        assertRect(40, 65, 250, 25, l.rect(2, "J"));
        assertRect(305, 40, 250, 25, l.rect(2, "K"));
        assertRect(305, 65, 250, 25, l.rect(2, "L"));
    }

    // The gap that a child with a definite height leaves at the end of a column (reference/common/breaks.md).

    /**
     * A Columns of Height: 200 with one column contains a block of Height: 200 whose 16 lines of 12 leave 4 at the end
     * of each column of 100: the Columns does not consume that gap either, and on page 3 it still has the 4 that the
     * block needs to finish (it used to be BOX_OVERFLOW).
     */
    @Test
    void aChildWithADefiniteHeightThatLeavesAGapDoesNotOverflowTheColumns() {
        String text = String.join(" ", Collections.nCopies(16, "aaaaa"));
        LayoutFixture l = layOutPaged(PAGE, columnsElement(List.of("Column count: 1", "Box:", "\tHeight: 200"),
                List.of("\t\tBlock:", "\t\t\tId: B", "\t\t\tBox:", "\t\t\t\tHeight: 200", "\t\t\t\tWidth: 30",
                        "\t\t\tText: " + text, "\t\t\t\tId: T")));
        assertEquals(3, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "M"));
        assertRect(40, 40, 30, 100, l.rect(1, "B"));
        assertEquals(8, l.fragment(1, "T").lines().size());
        assertRect(40, 40, 515, 100, l.rect(2, "M"));
        assertEquals(8, l.fragment(2, "T").lines().size());
        assertRect(40, 40, 515, 4, l.rect(3, "M"));
        assertRect(40, 40, 30, 4, l.rect(3, "B"));
    }

    /**
     * The same Columns, stretched to a grid row of 200 (A) that is split on the page it is on: the gap is not taken
     * from it either, and the row goes on to page 3 with the 4 that are left (it used to be BOX_OVERFLOW).
     */
    @Test
    void aStretchedColumnsInASplitRowDoesNotOverflow() {
        String text = String.join(" ", Collections.nCopies(16, "aaaaa"));
        LayoutFixture l = layOutPaged(PAGE, "\tGrid:", "\t\tId: G", "\t\tColumn sizes: * *",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 200",
                "\t\tArea:", "\t\t\tColumns:", "\t\t\t\tId: M", "\t\t\t\tColumn count: 1",
                "\t\t\t\tBlock:", "\t\t\t\t\tId: B", "\t\t\t\t\tBox:", "\t\t\t\t\t\tHeight: 200", "\t\t\t\t\t\tWidth: 30",
                "\t\t\t\t\tText: " + text, "\t\t\t\t\t\tId: T");
        assertEquals(3, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "G"));
        assertRect(297.5, 40, 257.5, 100, l.rect(1, "M"));
        assertEquals(8, l.fragment(1, "T").lines().size());
        assertRect(40, 40, 257.5, 100, l.rect(2, "A"));
        assertEquals(8, l.fragment(2, "T").lines().size());
        assertRect(40, 40, 515, 4, l.rect(3, "G"));
        l.assertAbsent(3, "A");
        assertRect(297.5, 40, 257.5, 4, l.rect(3, "M"));
    }
}
