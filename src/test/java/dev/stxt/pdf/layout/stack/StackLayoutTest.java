package dev.stxt.pdf.layout.stack;

import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.Distribution;
import dev.stxt.pdf.model.Rect;
import org.junit.jupiter.api.Test;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StackLayoutTest {

    private static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(), new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height())).toString());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    @Test
    void zeroBasisGivesEqualColumns() {
        LayoutFixture l = layOut(
                "\tStack:", "\t\tGap: 10",
                "\t\tItem:", "\t\t\tBase size: 0", "\t\t\tGrow: 1",
                "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10",
                "\t\tItem:", "\t\t\tBase size: 0", "\t\t\tGrow: 1",
                "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10",
                "\t\tItem:", "\t\t\tBase size: 0", "\t\t\tGrow: 1",
                "\t\t\tBlock:", "\t\t\t\tId: C", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10");
        assertRect(40, 40, 165, 10, l.rect("A"));
        assertRect(215, 40, 165, 10, l.rect("B"));
        assertRect(390, 40, 165, 10, l.rect("C"));
    }

    @Test
    void shrinkFreezesAtMinWidth() {
        // 85 are missing: each one would give up 42.5, but A does not go below 280 and B absorbs the rest.
        LayoutFixture l = layOut(
                "\tStack:", "\t\tItem:", "\t\t\tBase size: 300",
                "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tMin width: 280", "\t\t\t\t\tHeight: 10",
                "\t\tItem:", "\t\t\tBase size: 300",
                "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10");
        assertRect(40, 40, 280, 10, l.rect("A"));
        assertRect(320, 40, 235, 10, l.rect("B"));
    }

    @Test
    void autoMinWidthIsTheNarrowest() {
        // The word of A measures 300: A does not shrink below it and B gives up the rest.
        LayoutFixture l = layOut(
                "\tStack:", "\t\tItem:", "\t\t\tBase size: 500",
                "\t\t\tText: " + "a".repeat(60), "\t\t\t\tId: A",
                "\t\tItem:", "\t\t\tBase size: 500",
                "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10");
        assertEquals(300, l.rect("A").width(), 1e-9);
        assertRect(340, 40, 215, 10, l.rect("B"));
    }

    @Test
    void itemWithWidthShrinksToItsContent() {
        // 285 are missing: both give up 142.5. Their content measures 10, so Width does not stop them.
        LayoutFixture l = layOut(
                "\tStack:", "\t\tItem:", "\t\t\tText: aa", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 400",
                "\t\tItem:", "\t\t\tText: bb", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 400");
        assertRect(40, 40, 257.5, 12, l.rect("A"));
        assertRect(297.5, 40, 257.5, 12, l.rect("B"));
    }

    @Test
    void itemWithWidthDoesNotShrinkBelowItsContent() {
        // A would give up 142.5, but its word measures 300: it freezes there and B gives up the rest.
        LayoutFixture l = layOut(
                "\tStack:", "\t\tItem:", "\t\t\tText: " + "a".repeat(60), "\t\t\t\tId: A", "\t\t\t\tBox:",
                "\t\t\t\t\tWidth: 400",
                "\t\tItem:", "\t\t\tText: b", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 400");
        assertEquals(300, l.rect("A").width(), 1e-9);
        assertRect(340, 40, 215, 12, l.rect("B"));
    }

    @Test
    void columnItemWithHeightShrinksToItsContent() {
        // In 100 of height 60 are missing: both give up 30. Their content measures 12 (one line).
        LayoutFixture l = layOut(
                "\tStack:", "\t\tBox:", "\t\t\tHeight: 100", "\t\tDirection: vertical",
                "\t\tItem:", "\t\t\tText: a", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 80",
                "\t\tItem:", "\t\t\tText: b", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 80");
        assertRect(40, 40, 515, 50, l.rect("A"));
        assertRect(40, 90, 515, 50, l.rect("B"));
    }

    @Test
    void columnItemWithHeightDoesNotShrinkBelowItsContent() {
        // A would give up 30, but its three lines measure 36 (with a width of 10, one word per line).
        LayoutFixture l = layOut(
                "\tStack:", "\t\tBox:", "\t\t\tHeight: 50", "\t\tDirection: vertical",
                "\t\tItem:", "\t\t\tText: aa bb cc", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 40",
                "\t\t\t\t\tWidth: 10",
                "\t\tItem:", "\t\t\tText: d", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 40");
        assertRect(40, 40, 10, 36, l.rect("A"));
        assertRect(40, 76, 515, 14, l.rect("B"));
    }

    @Test
    void itemMarginsAreOutsideTheMainSize() {
        LayoutFixture l = layOut(
                "\tStack:", "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 100",
                "\t\t\t\t\tHeight: 10", "\t\t\t\t\tMargin: 10",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 100",
                "\t\t\t\t\tHeight: 10");
        assertRect(50, 50, 100, 10, l.rect("A"));
        // B declares Height: it does not stretch to the line (30).
        assertRect(160, 40, 100, 10, l.rect("B"));
    }

    @Test
    void mainAxisDistribution() {
        // Two items of 100 in 515: 315 are left over.
        LayoutFixture l = layOut(
                "\tStack:", "\t\tHorizontal: center",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: c1", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 100",
                "\t\t\t\t\tHeight: 10",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: c2", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 100",
                "\t\t\t\t\tHeight: 10",
                "\tStack:", "\t\tHorizontal: justify",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: b1", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 100",
                "\t\t\t\t\tHeight: 10",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: b2", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 100",
                "\t\t\t\t\tHeight: 10",
                "\tStack:", "\t\tHorizontal: distribute",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: e1", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 100",
                "\t\t\t\t\tHeight: 10",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: e2", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 100",
                "\t\t\t\t\tHeight: 10");
        assertEquals(197.5, l.rect("c1").x(), 1e-9);
        assertEquals(297.5, l.rect("c2").x(), 1e-9);
        assertEquals(40, l.rect("b1").x(), 1e-9);
        assertEquals(455, l.rect("b2").x(), 1e-9);
        assertEquals(145, l.rect("e1").x(), 1e-9);
        assertEquals(350, l.rect("e2").x(), 1e-9);
    }

    @Test
    void crossAxisAlignmentInADefiniteContainer() {
        // A single line with a definite height: the line measures what the container does (100).
        LayoutFixture l = layOut(
                "\tStack:", "\t\tBox:", "\t\t\tHeight: 100",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: S", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 50",
                "\t\tItem:", "\t\t\tVertical: start",
                "\t\t\tBlock:", "\t\t\t\tId: T", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 50", "\t\t\t\t\tHeight: 20",
                "\t\tItem:", "\t\t\tVertical: center",
                "\t\t\tBlock:", "\t\t\t\tId: C", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 50", "\t\t\t\t\tHeight: 20",
                "\t\tItem:", "\t\t\tVertical: end",
                "\t\t\tBlock:", "\t\t\t\tId: E", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 50", "\t\t\t\t\tHeight: 20");
        assertRect(40, 40, 50, 100, l.rect("S"));
        assertRect(90, 40, 50, 20, l.rect("T"));
        assertRect(140, 80, 50, 20, l.rect("C"));
        assertRect(190, 120, 50, 20, l.rect("E"));
    }

    @Test
    void stretchToTheTallestItem() {
        LayoutFixture l = layOut(
                "\tStack:", "\t\tId: F",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 50",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 50",
                "\t\t\t\t\tHeight: 30",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: M", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 50",
                "\t\t\t\t\tMax height: 25");
        assertRect(40, 40, 515, 30, l.rect("F"));
        assertRect(40, 40, 50, 30, l.rect("A"));
        assertRect(140, 40, 50, 25, l.rect("M"));
    }

    @Test
    void wrapBreaksIntoLines() {
        // 200 + 10 + 200 fit in 515; the third one moves to the second line.
        LayoutFixture l = layOut(
                "\tStack:", "\t\tId: F", "\t\tWrap: true", "\t\tGap: 10",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 200",
                "\t\t\t\t\tHeight: 20",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 200",
                "\t\t\t\t\tHeight: 20",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: C", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 200",
                "\t\t\t\t\tHeight: 20");
        assertRect(40, 40, 515, 50, l.rect("F"));
        assertRect(250, 40, 200, 20, l.rect("B"));
        assertRect(40, 70, 200, 20, l.rect("C"));
    }

    @Test
    void linesDistributionInADefiniteContainer() {
        // Lines of 20 + gap 10 + 20 in 100: 50 left over, centered.
        LayoutFixture l = layOut(
                "\tStack:", "\t\tBox:", "\t\t\tHeight: 100", "\t\tWrap: true", "\t\tGap: 10", "\t\tLine distribution: center",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 300",
                "\t\t\t\t\tHeight: 20",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 300",
                "\t\t\t\t\tHeight: 20");
        assertRect(40, 65, 300, 20, l.rect("A"));
        assertRect(40, 95, 300, 20, l.rect("B"));
    }

    @Test
    void linesStretchByDefault() {
        // Lines of 0 and 20 in 100 with a gap of 10: 70 left over, 35 for each line.
        LayoutFixture l = layOut(
                "\tStack:", "\t\tBox:", "\t\t\tHeight: 100", "\t\tWrap: true", "\t\tGap: 10",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 300",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 300",
                "\t\t\t\t\tHeight: 20");
        assertRect(40, 40, 300, 35, l.rect("A"));
        assertRect(40, 85, 300, 20, l.rect("B"));
    }

    @Test
    void columnDirection() {
        LayoutFixture l = layOut(
                "\tStack:", "\t\tId: F", "\t\tDirection: vertical", "\t\tGap: 5",
                "\t\tItem:", "\t\t\tText: aaa", "\t\t\t\tId: A",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 100",
                "\t\t\t\t\tHeight: 20",
                "\t\tItem:", "\t\t\tHorizontal: center",
                "\t\t\tText: aaaa", "\t\t\t\tId: C");
        assertRect(40, 40, 515, 54, l.rect("F"));
        assertRect(40, 40, 515, 12, l.rect("A"));
        assertRect(40, 57, 100, 20, l.rect("B"));
        assertRect(287.5, 82, 20, 12, l.rect("C"));
    }

    @Test
    void columnGrowsWithADefiniteHeight() {
        // Without Height on the container, Grow is an error (error catalog).
        LayoutFixture l = layOut(
                "\tStack:", "\t\tBox:", "\t\t\tHeight: 100", "\t\tDirection: vertical",
                "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 20",
                "\t\tItem:", "\t\t\tGrow: 1",
                "\t\t\tBlock:", "\t\t\t\tId: G");
        assertRect(40, 60, 515, 80, l.rect("G"));
    }

    @Test
    void intrinsicWidths() {
        // A: min 10, max 25. B: min and max 15. Gap 10.
        String a = "\t\tItem:\n\t\t\tText: aa aa\n\t\t\t\tId: %s";
        String b = "\t\tItem:\n\t\t\tText: bbb\n\t\t\t\tId: %s";
        LayoutFixture l = layOut(
                "\tStack:", "\t\tId: max", "\t\tBox:", "\t\t\tWidth: natural", "\t\tGap: 10",
                a.formatted("maxA"), b.formatted("maxB"),
                "\tStack:", "\t\tId: min", "\t\tBox:", "\t\t\tWidth: narrowest", "\t\tGap: 10",
                a.formatted("minA"), b.formatted("minB"),
                "\tStack:", "\t\tId: wrap", "\t\tBox:", "\t\t\tWidth: narrowest", "\t\tGap: 10", "\t\tWrap: true",
                a.formatted("wrapA"), b.formatted("wrapB"));
        assertEquals(50, l.rect("max").width(), 1e-9);
        assertEquals(35, l.rect("min").width(), 1e-9);
        assertEquals(15, l.rect("wrap").width(), 1e-9);
        // At its narrowest width there is not enough room: B freezes at its minimum (15) and A goes down to its own
        // (10).
        assertEquals(10, l.rect("minA").width(), 1e-9);
        assertEquals(15, l.rect("minB").width(), 1e-9);
    }

    @Test
    void textInAStackContainerIsAnAnonymousItem() {
        LayoutFixture l = layOut(
                "\tStack:", "\t\tId: F",
                "\t\tItem:", "\t\t\tText: aaa bbb", "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:",
                "\t\t\t\t\tWidth: 100", "\t\t\t\t\tHeight: 30");
        assertRect(75, 40, 100, 30, l.rect("B"));
    }

    @Test
    void positionedChildren() {
        LayoutFixture l = layOut(
                // A positioned stack: Frame with the padding and, inside, the Stack with the height.
                "\tFrame:", "\t\tBox:", "\t\t\tPadding: 5",
                "\t\tStack:", "\t\t\tBox:", "\t\t\t\tHeight: 40",
                "\t\t\tItem:", "\t\t\t\tFrame:", "\t\t\t\t\tId: R", "\t\t\t\t\tBox:", "\t\t\t\t\t\tWidth: 100",
                "\t\t\t\t\tOffset: 3 auto auto 4",
                "\t\t\tOverlay:", "\t\t\t\tId: A", "\t\t\t\tOffset: auto 0 0 auto", "\t\t\t\tBox:",
                "\t\t\t\t\tWidth: 10", "\t\t\t\t\tHeight: 10",
                "\t\t\tItem:", "\t\t\t\tBlock:", "\t\t\t\t\tId: B", "\t\t\t\t\tBox:", "\t\t\t\t\t\tWidth: 100");
        assertRect(49, 48, 100, 40, l.rect("R"));
        // B does not move with R; A takes up no room in the line.
        assertRect(145, 45, 100, 40, l.rect("B"));
        assertRect(545, 80, 10, 10, l.rect("A"));
    }

    @Test
    void distribution() {
        assertArrayEquals(new double[]{0, 0}, StackLayout.distribute(Distribution.START, 30, 3), 1e-9);
        assertArrayEquals(new double[]{30, 0}, StackLayout.distribute(Distribution.END, 30, 3), 1e-9);
        assertArrayEquals(new double[]{0, 15}, StackLayout.distribute(Distribution.JUSTIFY, 30, 3), 1e-9);
        assertArrayEquals(new double[]{5, 10}, StackLayout.distribute(Distribution.AROUND, 30, 3), 1e-9);
        assertArrayEquals(new double[]{7.5, 7.5}, StackLayout.distribute(Distribution.DISTRIBUTE, 30, 3), 1e-9);
        // Without room: justify at the start, distribute centered.
        assertArrayEquals(new double[]{0, 0}, StackLayout.distribute(Distribution.JUSTIFY, -30, 3), 1e-9);
        assertArrayEquals(new double[]{-15, 0}, StackLayout.distribute(Distribution.DISTRIBUTE, -30, 3), 1e-9);
    }

    // Cases that were missing according to mutation testing: different gaps on each axis,
    // intrinsic sizes with more than two items and margins on the items.

    private static String block(String id, String... box) {
        StringBuilder sb = new StringBuilder("\t\t\tBlock:\n\t\t\t\tId: " + id + (box.length > 0 ? "\n\t\t\t\tBox:" : ""));
        for (String b : box) {
            sb.append("\n\t\t\t\t\t").append(b);
        }
        return sb.toString();
    }

    @Test
    void rowAndColumnGapsInARow() {
        // Gap: rows 7, columns 13. 200 + 13 + 200 = 413 fit in 515; the third one moves to another line, 7 lower.
        LayoutFixture l = layOut(
                "\tStack:", "\t\tId: F", "\t\tWrap: true", "\t\tGap: 7 13",
                "\t\tItem:", block("A", "Width: 200", "Height: 20"),
                "\t\tItem:", block("B", "Width: 200", "Height: 20"),
                "\t\tItem:", block("C", "Width: 200", "Height: 20"));
        assertRect(40, 40, 515, 47, l.rect("F"));
        assertRect(253, 40, 200, 20, l.rect("B"));
        assertRect(40, 67, 200, 20, l.rect("C"));
    }

    @Test
    void rowAndColumnGapsInAColumn() {
        // In a column, the gap between items is that of the rows (7) and between lines, that of the columns (13):
        // 40 + 7 + 40 = 87 fit in 100; the third one moves to the next line, 100 + 13 to the right.
        LayoutFixture l = layOut(
                "\tStack:", "\t\tId: F", "\t\tBox:", "\t\t\tHeight: 100", "\t\tDirection: vertical", "\t\tWrap: true",
                "\t\tLine distribution: start", "\t\tGap: 7 13",
                "\t\tItem:", block("A", "Width: 100", "Height: 40"),
                "\t\tItem:", block("B", "Width: 100", "Height: 40"),
                "\t\tItem:", block("C", "Width: 100", "Height: 40"));
        assertRect(40, 87, 100, 40, l.rect("B"));
        assertRect(153, 40, 100, 40, l.rect("C"));
        assertRect(40, 40, 515, 100, l.rect("F"));
    }

    @Test
    void intrinsicWidthsWithThreeItems() {
        // Natural width of 5 + 10 + 15 and two gaps of 10, with and without Wrap; narrowest width without Wrap, the
        // same with 5 + 10 + 15.
        String items = "\t\tItem:\n\t\t\tText: a\n\t\tItem:\n\t\t\tText: bb\n\t\tItem:\n\t\t\tText: ccc";
        LayoutFixture l = layOut(
                "\tStack:", "\t\tId: max", "\t\tBox:", "\t\t\tWidth: natural", "\t\tGap: 10", items,
                "\tStack:", "\t\tId: wrap", "\t\tBox:", "\t\t\tWidth: natural", "\t\tGap: 10", "\t\tWrap: true",
                items,
                "\tStack:", "\t\tId: min", "\t\tBox:", "\t\t\tWidth: narrowest", "\t\tGap: 10", items);
        assertEquals(50, l.rect("max").width(), 1e-9);
        assertEquals(50, l.rect("wrap").width(), 1e-9);
        assertEquals(50, l.rect("min").width(), 1e-9);
    }

    @Test
    void basisCountsTheMargins() {
        // Hypothetical outer sizes 100 + 20 and 100: 295 left over, 147.5 for each one.
        LayoutFixture l = layOut(
                "\tStack:",
                "\t\tItem:", "\t\t\tBase size: 100", "\t\t\tGrow: 1", block("A", "Height: 10", "Margin: 0 10"),
                "\t\tItem:", "\t\t\tBase size: 100", "\t\t\tGrow: 1", block("B", "Height: 10"));
        assertRect(50, 40, 247.5, 10, l.rect("A"));
        assertRect(307.5, 40, 247.5, 10, l.rect("B"));
    }

    @Test
    void shrinkingStopsAtTheMinimumWithItsMargins() {
        // Outer sizes 320 and 300 in 515: 105 missing. A does not go below 280 (300 with margins) and B keeps 215.
        LayoutFixture l = layOut(
                "\tStack:",
                "\t\tItem:", "\t\t\tBase size: 300", block("A", "Min width: 280", "Height: 10", "Margin: 0 10"),
                "\t\tItem:", "\t\t\tBase size: 300", block("B", "Height: 10"));
        assertRect(50, 40, 280, 10, l.rect("A"));
        assertRect(340, 40, 215, 10, l.rect("B"));
    }

    @Test
    void automaticMinimumWithMargins() {
        // The word of A measures 300 and its margins 20: it does not go below 320 outer, and B keeps 195.
        LayoutFixture l = layOut(
                "\tStack:",
                "\t\tItem:", "\t\t\tBase size: 500", "\t\t\tText: " + "a".repeat(60), "\t\t\t\tId: A", "\t\t\t\tBox:",
                "\t\t\t\t\tMargin: 0 10",
                "\t\tItem:", "\t\t\tBase size: 500", block("B", "Height: 10"));
        assertEquals(300, l.rect("A").width(), 1e-9);
        assertRect(360, 40, 195, 10, l.rect("B"));
    }

    @Test
    void shrinkIsWeightedByTheBasisWithoutMargins() {
        // Outer sizes 320 and 300 in 515: 105 missing, distributed by Shrink × Base size (300 and 300), not by the
        // sizes with margins (reference/components/stack.md): 52.5 each.
        LayoutFixture l = layOut(
                "\tStack:",
                "\t\tItem:", "\t\t\tBase size: 300", block("A", "Height: 10", "Margin: 0 10"),
                "\t\tItem:", "\t\t\tBase size: 300", block("B", "Height: 10"));
        assertRect(50, 40, 247.5, 10, l.rect("A"));
        assertRect(307.5, 40, 247.5, 10, l.rect("B"));
    }

    @Test
    void stretchingLeavesTheMargins() {
        // Row: the line measures 30 and A, with a margin of 5 at the top and bottom, stretches to 20.
        // Column: the width is 515 minus the margins of C (505).
        LayoutFixture l = layOut(
                "\tStack:",
                "\t\tItem:", block("A", "Width: 50", "Margin: 5"),
                "\t\tItem:", block("B", "Width: 50", "Height: 30"),
                "\tStack:", "\t\tDirection: vertical",
                "\t\tItem:", block("C", "Height: 10", "Margin: 5"));
        assertRect(45, 45, 50, 20, l.rect("A"));
        assertRect(45, 75, 505, 10, l.rect("C"));
    }

    @Test
    void threeLinesWithLinesBetween() {
        // Three lines of 20 with gaps of 10 in 100: 20 left over, 10 between every two lines.
        LayoutFixture l = layOut(
                "\tStack:", "\t\tBox:", "\t\t\tHeight: 100", "\t\tWrap: true", "\t\tGap: 10", "\t\tLine distribution: justify",
                "\t\tItem:", block("A", "Width: 300", "Height: 20"),
                "\t\tItem:", block("B", "Width: 300", "Height: 20"),
                "\t\tItem:", block("C", "Width: 300", "Height: 20"));
        assertRect(40, 40, 300, 20, l.rect("A"));
        assertRect(40, 80, 300, 20, l.rect("B"));
        assertRect(40, 120, 300, 20, l.rect("C"));
    }

    @Test
    void wrapCountsTheGapsExactly() {
        // 200 + 10 + 200 + 10 + 95 = 515: one line. With 110, the third one moves to another.
        LayoutFixture l = layOut(
                "\tStack:", "\t\tWrap: true", "\t\tGap: 10",
                "\t\tItem:", block("A", "Width: 200", "Height: 20"),
                "\t\tItem:", block("B", "Width: 200", "Height: 20"),
                "\t\tItem:", block("C", "Width: 95", "Height: 20"),
                "\tStack:", "\t\tWrap: true", "\t\tGap: 10",
                "\t\tItem:", block("D", "Width: 200", "Height: 20"),
                "\t\tItem:", block("E", "Width: 200", "Height: 20"),
                "\t\tItem:", block("G", "Width: 110", "Height: 20"));
        assertRect(460, 40, 95, 20, l.rect("C"));
        assertRect(40, 90, 110, 20, l.rect("G"));
    }

    @Test
    void automaticMinimumCountsBorderAndPadding() {
        // The word of A measures 300, plus 2 + 2 of border and 10 + 10 of padding: it does not go below 324.
        LayoutFixture l = layOut(
                "\tStack:",
                "\t\tItem:", "\t\t\tBase size: 500", "\t\t\tText: " + "a".repeat(60), "\t\t\t\tId: A", "\t\t\t\tBox:",
                "\t\t\t\t\tBorder: 2", "\t\t\t\t\tPadding: 0 10",
                "\t\tItem:", "\t\t\tBase size: 500", block("B", "Height: 10"));
        assertEquals(324, l.rect("A").width(), 1e-9);
        assertRect(364, 40, 191, 10, l.rect("B"));
    }

    @Test
    void maxWidthCountsTheMargins() {
        // Each would grow 147.5, but A does not exceed 150 (170 with margins): B keeps 345.
        LayoutFixture l = layOut(
                "\tStack:",
                "\t\tItem:", "\t\t\tBase size: 100", "\t\t\tGrow: 1", block("A", "Max width: 150", "Height: 10", "Margin: 0 10"),
                "\t\tItem:", "\t\t\tBase size: 100", "\t\t\tGrow: 1", block("B", "Height: 10"));
        assertRect(50, 40, 150, 10, l.rect("A"));
        assertRect(210, 40, 345, 10, l.rect("B"));
    }

    @Test
    void columnLimitsCountTheMargins() {
        // Columns of 100. Max height 30 (40 with margins): B keeps 60. Min height 70 (80): D, with 20.
        // Automatic minimum: the text of E, in four lines, measures 48 (58 with margins): F, with 42.
        LayoutFixture l = layOut(
                "\tStack:", "\t\tBox:", "\t\t\tHeight: 100", "\t\tDirection: vertical",
                "\t\tItem:", "\t\t\tBase size: 20", "\t\t\tGrow: 1", block("A", "Max height: 30", "Margin: 5 0"),
                "\t\tItem:", "\t\t\tBase size: 20", "\t\t\tGrow: 1", block("B"),
                "\tStack:", "\t\tBox:", "\t\t\tHeight: 100", "\t\tDirection: vertical",
                "\t\tItem:", "\t\t\tBase size: 80", block("C", "Min height: 70", "Margin: 5 0"),
                "\t\tItem:", "\t\t\tBase size: 80", block("D"),
                "\tStack:", "\t\tBox:", "\t\t\tHeight: 100", "\t\tDirection: vertical",
                "\t\tItem:", "\t\t\tBase size: 80", "\t\t\tText: aa aa aa aa", "\t\t\t\tId: E", "\t\t\t\tBox:",
                "\t\t\t\t\tWidth: 10", "\t\t\t\t\tMargin: 5 0",
                "\t\tItem:", "\t\t\tBase size: 80", block("F"));
        assertRect(40, 45, 515, 30, l.rect("A"));
        assertRect(40, 80, 515, 60, l.rect("B"));
        assertRect(40, 145, 515, 70, l.rect("C"));
        assertRect(40, 220, 515, 20, l.rect("D"));
        assertRect(40, 245, 10, 48, l.rect("E"));
        assertRect(40, 298, 515, 42, l.rect("F"));
    }

    @Test
    void columnItemsThatDoNotStretchFitTheirContentInsideTheirMargins() {
        LayoutFixture l = layOut(
                "\tStack:", "\t\tDirection: vertical",
                "\t\tItem:", "\t\t\tHorizontal: start", "\t\t\tText: aaa", "\t\t\t\tId: A", "\t\t\t\tBox:",
                "\t\t\t\t\tMargin: 0 10");
        assertRect(50, 40, 15, 12, l.rect("A"));
    }

    @Test
    void columnLinesMeasureTheirItemsWithMargins() {
        // Column with Wrap in 60 of height: A (50 wide and a margin of 5 on each side) and B, on another line. The
        // first line measures 60, so B starts at 40 + 60.
        LayoutFixture l = layOut(
                "\tStack:", "\t\tBox:", "\t\t\tHeight: 60", "\t\tDirection: vertical", "\t\tWrap: true", "\t\tLine distribution: start",
                "\t\tItem:", block("A", "Width: 50", "Height: 40", "Margin: 0 5"),
                "\t\tItem:", block("B", "Width: 80", "Height: 40"));
        assertRect(45, 40, 50, 40, l.rect("A"));
        assertRect(100, 40, 80, 40, l.rect("B"));
    }

    @Test
    void stretchingWithDifferentMarginsInEachAxis() {
        // The line measures 40 (B). A has a margin of 10 at the top and bottom and 5 at the sides: it stretches to
        // 40 − 20.
        LayoutFixture l = layOut(
                "\tStack:",
                "\t\tItem:", block("A", "Width: 50", "Padding: 5 0", "Margin: 10 5"),
                "\t\tItem:", block("B", "Width: 50", "Height: 40"));
        assertRect(45, 50, 50, 20, l.rect("A"));
    }

    @Test
    void wrapCountsOneGapLessThanItems() {
        // 100 + 10 + 100 + 10 + 100 = 320; with the fourth one (190) and its gap, 520: it moves to another line.
        LayoutFixture l = layOut(
                "\tStack:", "\t\tWrap: true", "\t\tGap: 10",
                "\t\tItem:", block("A", "Width: 100", "Height: 20"),
                "\t\tItem:", block("B", "Width: 100", "Height: 20"),
                "\t\tItem:", block("C", "Width: 100", "Height: 20"),
                "\t\tItem:", block("D", "Width: 190", "Height: 20"));
        assertRect(40, 70, 190, 20, l.rect("D"));
    }
}
