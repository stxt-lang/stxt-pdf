package dev.stxt.pdf.layout.columns;

import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.tree.InvalidModelException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Blocks in columns on an A4 page (page area of 515 × 762 from (40, 40)),
 * reference/components/columns.md. With {@code Count: 2} and {@code Gap: 15}, the columns measure 250 and the
 * second starts at x = 305.
 */
class ColumnsLayoutTest {

    static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(),
                new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height())).toString());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    /**
     * A {@code Block} child of the {@code Columns} with its height and, if {@code whole}, {@code Keep together: true}.
     */
    static List<String> block(String id, double height, boolean whole, String... more) {
        List<String> lines = new ArrayList<>(List.of("\t\tBlock:", "\t\t\tId: " + id, "\t\t\tBox:",
                "\t\t\t\tHeight: " + height));
        if (whole) {
            lines.addAll(List.of("\t\t\tBreak:", "\t\t\t\tKeep together: true"));
        }
        Collections.addAll(lines, more);
        return lines;
    }

    /** A {@code Block} child of the {@code Columns} with these {@code Box} properties. */
    static List<String> sized(String id, boolean whole, String... box) {
        List<String> lines = new ArrayList<>(List.of("\t\tBlock:", "\t\t\tId: " + id, "\t\t\tBox:"));
        for (String p : box) {
            lines.add("\t\t\t\t" + p);
        }
        if (whole) {
            lines.addAll(List.of("\t\t\tBreak:", "\t\t\t\tKeep together: true"));
        }
        return lines;
    }

    /** A {@code Columns} with {@code M} as {@code Id}, its properties and its blocks. */
    @SafeVarargs
    static String[] columnsElement(List<String> properties, List<String>... blocks) {
        List<String> lines = new ArrayList<>(List.of("\tColumns:", "\t\tId: M"));
        properties.forEach(p -> lines.add("\t\t" + p));
        for (List<String> b : blocks) {
            lines.addAll(b);
        }
        return lines.toArray(String[]::new);
    }

    private static final List<String> TWO = List.of("Count: 2", "Gap: 15");

    @Test
    void balancedByDefault() {
        LayoutFixture l = layOut(columnsElement(TWO, block("A", 30, false), block("B", 30, false), block("C", 30, false),
                block("D", 30, false)));
        assertRect(40, 40, 515, 60, l.rect("M"));
        assertRect(40, 40, 250, 30, l.rect("A"));
        assertRect(40, 70, 250, 30, l.rect("B"));
        assertRect(305, 40, 250, 30, l.rect("C"));
        assertRect(305, 70, 250, 30, l.rect("D"));
    }

    @Test
    void fillAutoFillsInOrder() {
        LayoutFixture l = layOut(columnsElement(List.of("Count: 2", "Gap: 15", "Balance: false"), block("A", 30, false),
                block("B", 30, false), block("C", 30, false), block("D", 30, false)));
        assertRect(40, 40, 515, 120, l.rect("M"));
        assertRect(40, 130, 250, 30, l.rect("D"));
    }

    @Test
    void balanceKeepsWholeTheBoxesThatKeepTogether() {
        LayoutFixture l = layOut(columnsElement(TWO, block("A", 30, true), block("B", 30, true), block("C", 30, true)));
        assertRect(40, 40, 515, 60, l.rect("M"));
        assertRect(40, 70, 250, 30, l.rect("B"));
        assertRect(305, 40, 250, 30, l.rect("C"));
    }

    /** A definite height is split like any box: 90 in two columns of 45. */
    @Test
    void balanceSlicesTheBoxesThatCanBreak() {
        LayoutFixture l = layOut(columnsElement(TWO, block("A", 30, false), block("B", 30, false), block("C", 30, false)));
        assertRect(40, 40, 515, 45, l.rect("M"));
        // The fragment of B in the second column: the 15 that do not fit in the first.
        assertRect(305, 40, 250, 15, l.rect("B"));
        assertRect(305, 55, 250, 30, l.rect("C"));
    }

    @Test
    void columnWidthFitsAsManyColumnsAsPossible() {
        // Three columns of 165 with gaps of 10.
        LayoutFixture l = layOut(columnsElement(List.of("Min column width: 150", "Gap: 10"), block("A", 30, true),
                block("B", 30, true), block("C", 30, true)));
        assertRect(40, 40, 515, 30, l.rect("M"));
        assertRect(40, 40, 165, 30, l.rect("A"));
        assertRect(215, 40, 165, 30, l.rect("B"));
        assertRect(390, 40, 165, 30, l.rect("C"));
    }

    @Test
    void columnBreak() {
        LayoutFixture l = layOut(columnsElement(List.of("Count: 3", "Gap: 10", "Balance: false"), block("A", 30, false),
                block("B", 30, false, "\t\t\tBreak:", "\t\t\t\tBefore: column"), block("C", 30, false)));
        assertRect(40, 40, 515, 60, l.rect("M"));
        assertRect(40, 40, 165, 30, l.rect("A"));
        assertRect(215, 40, 165, 30, l.rect("B"));
        assertRect(215, 70, 165, 30, l.rect("C"));
    }

    @Test
    void paragraphsAreBalancedBetweenColumns() {
        LayoutFixture l = layOut("\tColumns:", "\t\tId: M", "\t\tCount: 2", "\t\tGap: 15", "\t\tParagraph: Uno",
                "\t\tParagraph: Dos", "\t\tParagraph: Tres", "\t\t\tId: T", "\t\tParagraph: Cuatro");
        assertRect(40, 40, 515, 24, l.rect("M"));
        // The third paragraph opens the second column.
        assertRect(305, 40, 250, 12, l.rect("T"));
    }

    @Test
    void heightIsFilledInOrderWithFillAuto() {
        LayoutFixture l = layOut(columnsElement(List.of("Count: 2", "Gap: 15", "Balance: false", "Box:",
                "\tHeight: 100"), block("A", 30, true), block("B", 30, true), block("C", 30, true),
                block("D", 30, true)));
        assertRect(40, 40, 515, 100, l.rect("M"));
        assertRect(40, 100, 250, 30, l.rect("C"));
        assertRect(305, 40, 250, 30, l.rect("D"));
    }

    @Test
    void heightIsBalanced() {
        LayoutFixture l = layOut(columnsElement(List.of("Count: 2", "Gap: 15", "Box:", "\tHeight: 100"),
                block("A", 30, true), block("B", 30, true), block("C", 30, true), block("D", 30, true)));
        assertRect(40, 40, 515, 100, l.rect("M"));
        assertRect(305, 40, 250, 30, l.rect("C"));
    }

    @Test
    void borderAndPaddingSurroundTheColumns() {
        LayoutFixture l = layOut(columnsElement(List.of("Count: 2", "Gap: 15", "Box:", "\tPadding: 10",
                "\tBorder: 1"), block("A", 30, true), block("B", 30, true)));
        // Content 493 wide: columns of 239.
        assertRect(40, 40, 515, 52, l.rect("M"));
        assertRect(51, 51, 239, 30, l.rect("A"));
        assertRect(305, 51, 239, 30, l.rect("B"));
    }

    @Test
    void marginsOfTheContentDoNotLeaveTheColumns() {
        LayoutFixture l = layOut(columnsElement(TWO, sized("A", true, "Height: 30", "Margin: 10 0")));
        assertRect(40, 40, 515, 50, l.rect("M"));
        assertRect(40, 50, 250, 30, l.rect("A"));
    }

    /** In a stack item there is no page to fill: it is balanced. The width is the natural width: 2 × 50 + 10. */
    @Test
    void stackItemIsBalancedAndSizedByItsColumns() {
        List<String> lines = new ArrayList<>(List.of("\tStack:", "\t\tVertical: start", "\t\tItem:"));
        for (String line : columnsElement(List.of("Count: 2", "Gap: 10", "Balance: false"),
                sized("A", true, "Height: 30", "Width: 50"), block("B", 30, true),
                block("C", 30, true), block("D", 30, true))) {
            lines.add("\t\t" + line);
        }
        LayoutFixture l = layOut(lines.toArray(String[]::new));
        assertRect(40, 40, 110, 60, l.rect("M"));
        assertRect(40, 40, 50, 30, l.rect("A"));
        assertRect(100, 40, 50, 30, l.rect("C"));
    }

    /** Three words of 40 characters (200 pt): one per line in a column of 250. */
    private static String[] threeLines(String... typography) {
        List<String> lines = new ArrayList<>(List.of("\tColumns:", "\t\tId: M", "\t\tCount: 2", "\t\tGap: 15",
                "\t\tText: " + "a".repeat(40) + " " + "b".repeat(40) + " " + "c".repeat(40), "\t\t\tId: T"));
        if (typography.length > 0) {
            lines.add("\t\t\tTypography:");
            for (String t : typography) {
                lines.add("\t\t\t\t" + t);
            }
        }
        return lines.toArray(String[]::new);
    }

    /** Any break of a paragraph of three lines violates widows or orphans (2): it is not balanced. */
    @Test
    void balanceKeepsWidowsAndOrphans() {
        LayoutFixture l = layOut(threeLines());
        assertRect(40, 40, 515, 36, l.rect("M"));
        assertRect(40, 40, 250, 36, l.rect("T"));
    }

    @Test
    void balanceSplitsAParagraphThatAllowsIt() {
        LayoutFixture l = layOut(threeLines("Widows: 1", "Orphans: 1"));
        assertRect(40, 40, 515, 24, l.rect("M"));
        assertRect(305, 40, 250, 12, l.rect("T"));
    }

    // Cases that were missing according to mutation testing.

    /**
     * The text measures 20 (narrowest width, "aaaa") and 35 (natural width). With Count: 3 and gaps of 10, three
     * columns of each; with Min column width the minimum is one column and the maximum, columns of at least that width.
     */
    @Test
    void intrinsicWidthsWithThreeColumns() {
        LayoutFixture l = layOut(
                "\tColumns:", "\t\tId: min", "\t\tCount: 3", "\t\tGap: 10", "\t\tBox:", "\t\t\tWidth: narrowest",
                "\t\tText: aaaa bb",
                "\tColumns:", "\t\tId: max", "\t\tCount: 3", "\t\tGap: 10", "\t\tBox:", "\t\t\tWidth: natural",
                "\t\tText: aaaa bb",
                "\tColumns:", "\t\tId: wmin", "\t\tCount: 3", "\t\tMin column width: 50", "\t\tGap: 10", "\t\tBox:",
                "\t\t\tWidth: narrowest", "\t\tText: aaaa bb",
                "\tColumns:", "\t\tId: wmax", "\t\tCount: 3", "\t\tMin column width: 50", "\t\tGap: 10", "\t\tBox:",
                "\t\t\tWidth: natural", "\t\tText: aaaa bb");
        assertEquals(80, l.rect("min").width(), 1e-9);
        assertEquals(125, l.rect("max").width(), 1e-9);
        assertEquals(20, l.rect("wmin").width(), 1e-9);
        assertEquals(170, l.rect("wmax").width(), 1e-9);
    }

    /**
     * Height: 60 with border and padding of 5: the columns measure 40 (60 − 20) and three blocks of 12 fit in each one;
     * the fourth starts the second column, at (305, 50).
     */
    @Test
    void heightWithBorderAndPaddingLeavesTheColumnsTheRest() {
        LayoutFixture l = layOut(columnsElement(List.of("Count: 2", "Gap: 15", "Balance: false", "Box:", "\tHeight: 60",
                        "\tBorder: 5", "\tPadding: 5"), block("A", 12, true), block("B", 12, true), block("C", 12, true),
                block("D", 12, true), block("E", 12, true)));
        assertRect(50, 74, 240, 12, l.rect("C"));
        assertRect(305, 50, 240, 12, l.rect("D"));
        assertRect(305, 62, 240, 12, l.rect("E"));
    }

    @Test
    void theMessageCountsTheColumns() {
        // Whole blocks of 40 in columns of 30: they do not fit.
        String one = assertThrows(InvalidModelException.class, () -> layOut(columnsElement(List.of("Count: 1", "Box:",
                "\tHeight: 30"), block("A", 40, true)))).getMessage();
        assertTrue(one.contains("which leaves 1 column of 30:"), one);
        String two = assertThrows(InvalidModelException.class, () -> layOut(columnsElement(List.of("Count: 2", "Box:",
                "\tHeight: 30"), block("A", 40, true), block("B", 40, true), block("C", 40, true)))).getMessage();
        assertTrue(two.contains("which leaves 2 columns of 30:"), two);
        // Without a page (a stack item is not split) or a height, the column breaks need more columns than there are.
        String breaks = assertThrows(InvalidModelException.class, () -> layOut("\tStack:", "\t\tItem:",
                "\t\t\tColumns:", "\t\t\t\tCount: 1", "\t\t\t\tText: A", "\t\t\t\tText: B", "\t\t\t\t\tBreak:",
                "\t\t\t\t\t\tBefore: column")).getMessage();
        assertTrue(breaks.contains("has 1 column and its column breaks"), breaks);
    }
}
