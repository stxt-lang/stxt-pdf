package dev.stxt.pdf.layout.grid;

import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.tree.InvalidModelException;
import org.junit.jupiter.api.Test;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridLayoutTest {

    private static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(), new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height())).toString());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    @Test
    void columnsAndRowsSizing() {
        // auto reaches the natural width of B (45) and the fraction receives the rest: 515 - 100 - 45.
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tColumn sizes: 100 auto *",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10",
                "\t\tArea:", "\t\t\tText: aaaa bbbb", "\t\t\t\tId: B",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: C", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10");
        assertRect(40, 40, 100, 10, l.rect("A"));
        assertRect(140, 40, 45, 12, l.rect("B"));
        assertRect(185, 40, 370, 10, l.rect("C"));
    }

    @Test
    void tableCellsAreGridItems() {
        // Columns 100 and * with a gap of 5: 515 - 100 - 5 = 410. The row of the body measures what
        // the cell with padding does (12 + 8) and stretches the other.
        LayoutFixture l = layOut(
                "\tTable:", "\t\tColumn sizes: 100 *", "\t\tGap: 0 5",
                "\t\tHeader rows:", "\t\t\tRow:", "\t\t\t\tCell: aaa", "\t\t\t\t\tId: H1", "\t\t\t\tCell: bbb", "\t\t\t\t\tId: H2",
                "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: ccc", "\t\t\t\t\tId: B1", "\t\t\t\t\tBox:",
                "\t\t\t\t\t\tPadding: 4", "\t\t\t\tCell:", "\t\t\t\t\tId: B2");
        assertRect(40, 40, 100, 12, l.rect("H1"));
        assertRect(145, 40, 410, 12, l.rect("H2"));
        assertRect(40, 52, 100, 20, l.rect("B1"));
        assertRect(145, 52, 410, 20, l.rect("B2"));
    }

    @Test
    void alignmentInTheArea() {
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tColumn sizes: 200 200", "\t\tRow sizes: 100",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: S",
                "\t\tArea:", "\t\t\tHorizontal: end", "\t\t\tVertical: center",
                "\t\t\tBlock:", "\t\t\t\tId: E", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 50", "\t\t\t\t\tHeight: 20",
                "\tGrid:", "\t\tColumn sizes: 200", "\t\tHorizontal: start",
                "\t\tArea:", "\t\t\tText: aaa", "\t\t\t\tId: T");
        assertRect(40, 40, 200, 100, l.rect("S"));
        assertRect(390, 80, 50, 20, l.rect("E"));
        // Without stretch, the item keeps its fit to the content.
        assertRect(40, 140, 15, 12, l.rect("T"));
    }

    @Test
    void explicitCellsAndAutoPlacement() {
        // A spans columns 2 and 3 of row 1; C does not fit after B and moves to row 2.
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tColumn sizes: 100 100 100",
                "\t\tArea: 1 2 1 2", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: C", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10");
        assertRect(140, 40, 200, 10, l.rect("A"));
        assertRect(40, 40, 100, 10, l.rect("B"));
        assertRect(40, 50, 100, 10, l.rect("C"));
    }

    @Test
    void flowColumn() {
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tColumn sizes: 100 100", "\t\tRow sizes: 20 20", "\t\tFlow: vertical", "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: C");
        assertRect(40, 40, 100, 20, l.rect("A"));
        assertRect(40, 60, 100, 20, l.rect("B"));
        assertRect(140, 40, 100, 20, l.rect("C"));
    }

    @Test
    void fractionRowsWithAutoHeightMeasureTheTallest() {
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tId: G", "\t\tRow sizes: * *",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 30");
        assertRect(40, 40, 515, 60, l.rect("G"));
        assertRect(40, 40, 515, 10, l.rect("A"));
        assertRect(40, 70, 515, 30, l.rect("B"));
    }

    @Test
    void fractionRowsWithDefiniteHeight() {
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tBox:", "\t\t\tHeight: 100", "\t\tRow sizes: * 3*",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: A",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: B");
        assertRect(40, 40, 515, 25, l.rect("A"));
        assertRect(40, 65, 515, 75, l.rect("B"));
    }

    @Test
    void autoRowsStretchInADefiniteHeight() {
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tBox:", "\t\t\tHeight: 100",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: A",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: B");
        assertRect(40, 40, 515, 50, l.rect("A"));
        assertRect(40, 90, 515, 50, l.rect("B"));
    }

    @Test
    void percentagesUseTheArea() {
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tColumn sizes: 200 300", "\t\tRow sizes: 100",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: P", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 50%",
                "\t\t\t\t\tHeight: 50%");
        assertRect(240, 40, 150, 50, l.rect("P"));
    }

    @Test
    void percentageRowsOfADefiniteHeight() {
        // With Height: auto on the container, rows in percentages are an error (error catalog).
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tId: G", "\t\tBox:", "\t\t\tHeight: 100", "\t\tRow sizes: 50% 50%",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: A", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: B", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 30");
        assertRect(40, 40, 515, 100, l.rect("G"));
        assertRect(40, 40, 515, 10, l.rect("A"));
        assertRect(40, 90, 515, 30, l.rect("B"));
    }

    @Test
    void percentageColumnsInIntrinsicWidthsAreAuto() {
        // A: min 10, max 25. B: min and max 15. Without a reference container, 50% counts as auto. With
        // the narrowest width (25) each column would measure 12.5 and B would overflow: it is an error.
        String items = "\t\tArea:\n\t\t\tText: aa aa\n\t\tArea:\n\t\t\tText: bbb";
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tId: max", "\t\tBox:", "\t\t\tWidth: natural", "\t\tColumn sizes: 50% 50%", items);
        assertEquals(40, l.rect("max").width(), 1e-9);
    }

    // An item that spans fractions and does not fit in the container (the columns do not go below their
    // narrowest width) is an overflow: it is in the error catalog.

    @Test
    void itemCrossingFractionsInIntrinsicWidths() {
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tId: G", "\t\tBox:", "\t\t\tWidth: narrowest", "\t\tColumn sizes: * *",
                "\t\tArea: 1 1 1 2", "\t\t\tText: " + "a".repeat(60));
        assertEquals(300, l.rect("G").width(), 1e-9);
    }

    @Test
    void intrinsicWidths() {
        // A: min 10, max 25. B: min and max 15. Gap 10.
        String items = "\t\tArea:\n\t\t\tText: aa aa\n\t\tArea:\n\t\t\tText: bbb";
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tId: max", "\t\tBox:", "\t\t\tWidth: natural", "\t\tColumn sizes: auto auto",
                "\t\tGap: 10", items,
                "\tGrid:", "\t\tId: min", "\t\tBox:", "\t\t\tWidth: narrowest", "\t\tColumn sizes: auto auto",
                "\t\tGap: 10", items,
                "\tGrid:", "\t\tId: fr", "\t\tBox:", "\t\t\tWidth: natural", "\t\tColumn sizes: 150 *",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10",
                "\t\tArea:", "\t\t\tText: aaa");
        assertEquals(50, l.rect("max").width(), 1e-9);
        assertEquals(35, l.rect("min").width(), 1e-9);
        assertEquals(165, l.rect("fr").width(), 1e-9);
    }

    @Test
    void positionedChildren() {
        LayoutFixture l = layOut(
                // A positioned grid: Frame and, inside, the Grid with the height.
                "\tFrame:", "\t\tGrid:", "\t\t\tBox:", "\t\t\t\tHeight: 50", "\t\t\tColumn sizes: 100 100",
                "\t\t\tArea:", "\t\t\t\tFrame:", "\t\t\t\t\tId: R", "\t\t\t\t\tOffset: 3 auto auto 4",
                "\t\t\tOverlay:", "\t\t\t\tId: A", "\t\t\t\tOffset: auto 0 0 auto", "\t\t\t\tBox:",
                "\t\t\t\t\tWidth: 10", "\t\t\t\t\tHeight: 10",
                "\t\t\tArea:", "\t\t\t\tBlock:", "\t\t\t\t\tId: B");
        assertRect(44, 43, 100, 50, l.rect("R"));
        assertRect(140, 40, 100, 50, l.rect("B"));
        assertRect(545, 80, 10, 10, l.rect("A"));
    }

    @Test
    void itemMarginsUseTheAreaWidth() {
        // The 10 % is of the area (200), not of the container (515): 20 per side. The fragment carries the
        // used margin, which is the one Overflow checks.
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tColumn sizes: 200 *",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: M", "\t\t\t\tBox:", "\t\t\t\t\tMargin: 10%",
                "\t\t\t\t\tHeight: 10");
        assertRect(60, 60, 160, 10, l.rect("M"));
        assertEquals(Insets.all(20), l.fragment("M").margin());
    }

    // Cases that were missing according to mutation testing: items with margins and intrinsic
    // sizes with more than two columns.

    @Test
    void alignmentAndStretchKeepTheItemMargins() {
        // Areas of 200 × 100. E (50 × 20, margin 10) at the end and centered: x = 40 + 200 − 10 − 50, y = 40 + 30 + 10.
        // S, without a size, stretches inside its margins: 180 × 80 from (250, 50).
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tColumn sizes: 200 200", "\t\tRow sizes: 100",
                "\t\tArea:", "\t\t\tHorizontal: end", "\t\t\tVertical: center",
                "\t\t\tBlock:", "\t\t\t\tId: E", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 50", "\t\t\t\t\tHeight: 20",
                "\t\t\t\t\tMargin: 10",
                "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: S", "\t\t\t\tBox:", "\t\t\t\t\tMargin: 10");
        assertRect(180, 80, 50, 20, l.rect("E"));
        assertRect(250, 50, 180, 80, l.rect("S"));
    }

    @Test
    void notStretchedItemsFitTheirContentInsideTheirMargins() {
        // Horizontal: start: the text (15) in an area of 200 with a margin of 10 on each side.
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tColumn sizes: 200", "\t\tHorizontal: start",
                "\t\tArea:", "\t\t\tText: aaa", "\t\t\t\tId: T", "\t\t\t\tBox:", "\t\t\t\t\tMargin: 10");
        assertRect(50, 50, 15, 12, l.rect("T"));
    }

    @Test
    void intrinsicWidthsWithThreeColumns() {
        // 5 + 10 + 15 and two gaps of 10.
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tId: G", "\t\tBox:", "\t\t\tWidth: natural", "\t\tColumn sizes: auto auto auto",
                "\t\tGap: 10", "\t\tArea:", "\t\t\tText: a", "\t\tArea:", "\t\t\tText: bb", "\t\tArea:", "\t\t\tText: ccc");
        assertEquals(50, l.rect("G").width(), 1e-9);
    }

    @Test
    void percentageLimitsOfAnItemThatDoesNotStretch() {
        // Areas of 200 × 100 unstretched: Min height: 50% gives 50 to M, and Max height: 50% leaves X at 50 (not 80).
        LayoutFixture l = layOut(
                "\tGrid:", "\t\tColumn sizes: 200 200", "\t\tRow sizes: 100",
                "\t\tArea:", "\t\t\tVertical: start", "\t\t\tBlock:", "\t\t\t\tId: M", "\t\t\t\tBox:",
                "\t\t\t\t\tMin height: 50%",
                "\t\tArea:", "\t\t\tVertical: start", "\t\t\tBlock:", "\t\t\t\tId: X", "\t\t\t\tBox:",
                "\t\t\t\t\tHeight: 80", "\t\t\t\t\tMax height: 50%");
        assertRect(40, 40, 200, 50, l.rect("M"));
        assertRect(240, 40, 200, 50, l.rect("X"));
    }

    @Test
    void theMessageOfAnItemWithoutRoomInItsRow() {
        String two = assertThrows(InvalidModelException.class, () -> layOut(
                "\tGrid:", "\t\tColumn sizes: 100 100",
                "\t\tArea: 1 1", "\t\t\tBlock:",
                "\t\tArea: 1 auto 1 2", "\t\t\tBlock:")).getMessage();
        assertTrue(two.contains("In row 1 there is no room of 2 free columns left for this item (the grid has "
                + "2 columns): another row has to be chosen"), two);
        String one = assertThrows(InvalidModelException.class, () -> layOut(
                "\tGrid:", "\t\tColumn sizes: 100",
                "\t\tArea: 1 1", "\t\t\tBlock:",
                "\t\tArea: 1 auto", "\t\t\tBlock:")).getMessage();
        assertTrue(one.contains("there is no room of 1 free column left for this item (the grid has 1 column)"), one);
        String column = assertThrows(InvalidModelException.class, () -> layOut(
                "\tGrid:", "\t\tFlow: vertical", "\t\tRow sizes: 100", "\t\tBox:", "\t\t\tHeight: 100",
                "\t\tArea: 1 1", "\t\t\tBlock:",
                "\t\tArea: auto 1", "\t\t\tBlock:")).getMessage();
        assertTrue(column.contains("In column 1 there is no room of 1 free row left for this item (the grid has "
                + "1 row): another column has to be chosen"), column);
    }
}
