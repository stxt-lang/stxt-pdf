package dev.stxt.pdf.layout.block;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.Rect;

class BlockLayoutTest {

    private static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(), new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height())).toString());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    @Test
    void parentAndFirstChildMarginsCollapse() {
        // max(10, 20) comes out of P: P and C start at 40 + 20.
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tMargin: 10 0 0 0",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 20 0 0 0",
                "\tBlock:", "\t\tId: S", "\t\tBox:", "\t\t\tHeight: 10");
        assertRect(40, 60, 515, 30, l.rect("P"));
        assertRect(40, 60, 515, 30, l.rect("C"));
        assertRect(40, 90, 515, 10, l.rect("S"));
    }

    @Test
    void stylesRulesShapeTheLayout() {
        // The rule gives each Block a height of 30 and a bottom margin of 10; B overrides it with 50.
        LayoutFixture l = layOut(
                "\tStyles:", "\t\tFor element: Block", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 0 0 10 0",
                "\t\tFor style: narrow", "\t\t\tBox:", "\t\t\t\tWidth: 100",
                "\tBlock:", "\t\tId: A",
                "\tBlock:", "\t\tId: B", "\t\tStyle: narrow", "\t\tBox:", "\t\t\tHeight: 50",
                "\tBlock:", "\t\tId: C");
        assertRect(40, 40, 515, 30, l.rect("A"));
        assertRect(40, 80, 100, 50, l.rect("B"));
        assertRect(40, 140, 515, 30, l.rect("C"));
    }

    @Test
    void paddingStopsTheCollapse() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tMargin: 10 0 0 0", "\t\t\tPadding: 5 0 0 0",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 20 0 0 0");
        assertRect(40, 50, 515, 55, l.rect("P"));
        assertRect(40, 75, 515, 30, l.rect("C"));
    }

    @Test
    void isolateMarginsStopsTheCollapse() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tMargin: 10 0 0 0", "\t\tIsolate margins: true",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 20 0 20 0");
        assertRect(40, 50, 515, 70, l.rect("P"));
        assertRect(40, 70, 515, 30, l.rect("C"));
    }

    @Test
    void trimMarginsDropsTheMarginsOfTheFirstAndTheLastChildInsideAPadding() {
        // Without Trim margins the padding keeps both margins inside: P would be 80 high and C would start at 65.
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tTrim margins: both", "\t\tBox:", "\t\t\tPadding: 5",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 20 0 20 0",
                "\tBlock:", "\t\tId: S", "\t\tBox:", "\t\t\tHeight: 10");
        assertRect(40, 40, 515, 40, l.rect("P"));
        assertRect(45, 45, 505, 30, l.rect("C"));
        assertRect(40, 80, 515, 10, l.rect("S"));
    }

    @Test
    void trimMarginsDropsTheMarginsThatWouldComeOutOfAnOpenBox() {
        // Without Trim margins those of C come out of P and merge with its own: P would start at 60 and S at 110.
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tTrim margins: both", "\t\tBox:", "\t\t\tMargin: 10 0 0 0",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 20 0 20 0",
                "\tBlock:", "\t\tId: S", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tMargin: 5 0 0 0");
        assertRect(40, 50, 515, 30, l.rect("P"));
        assertRect(40, 50, 515, 30, l.rect("C"));
        assertRect(40, 85, 515, 10, l.rect("S"));
    }

    @Test
    void trimMarginsDropsTheMarginsOfTheFirstDescendantsAtAnyDepth() {
        // The margins of C come out of B, which is open, and touch the top and the bottom of P: they are dropped too.
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tTrim margins: both", "\t\tBox:", "\t\t\tPadding: 5",
                "\t\tBlock:", "\t\t\tId: B", "\t\t\tBox:", "\t\t\t\tMargin: 8 0 8 0",
                "\t\t\tBlock:", "\t\t\t\tId: C", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 30", "\t\t\t\t\tMargin: 20 0 20 0");
        assertRect(40, 40, 515, 40, l.rect("P"));
        assertRect(45, 45, 505, 30, l.rect("C"));
    }

    @Test
    void trimMarginsKeepsTheMarginsBetweenTheChildren() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tTrim margins: both", "\t\tBox:", "\t\t\tPadding: 5",
                "\t\tBlock:", "\t\t\tId: A", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 20 0 20 0",
                "\t\tBlock:", "\t\t\tId: B", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 12 0 20 0");
        assertRect(40, 40, 515, 90, l.rect("P"));
        assertRect(45, 45, 505, 30, l.rect("A"));
        assertRect(45, 95, 505, 30, l.rect("B"));
    }

    @Test
    void theFirstChildOfASectionKeepsItsTopMargin() {
        // The page area isolates the margins: without Trim margins the one of its first child stays inside it.
        LayoutFixture l = layOut(
                "\tSection:",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 20 0 0 0");
        assertRect(40, 60, 515, 30, l.rect("C"));
    }

    @Test
    void trimMarginsInASectionStartsItsFirstChildAtTheTopOfThePageArea() {
        LayoutFixture l = layOut(
                "\tSection:", "\t\tTrim margins: both",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 20 0 12 0",
                "\t\tBlock:", "\t\t\tId: D", "\t\t\tBox:", "\t\t\t\tHeight: 30");
        assertRect(40, 40, 515, 30, l.rect("C"));
        assertRect(40, 82, 515, 30, l.rect("D"));
    }

    @Test
    void trimMarginsInACellDropsTheMarginsOfItsFirstAndItsLastElement() {
        // The cell is as high as its block: without Trim margins it would be 70 high, with both margins inside.
        LayoutFixture l = layOut(
                "\tTable:", "\t\tColumn sizes: *", "\t\tBody rows:", "\t\t\tRow:",
                "\t\t\t\tCell:", "\t\t\t\t\tId: K", "\t\t\t\t\tTrim margins: both",
                "\t\t\t\t\tBlock:", "\t\t\t\t\t\tId: C", "\t\t\t\t\t\tBox:", "\t\t\t\t\t\t\tHeight: 30",
                "\t\t\t\t\t\t\tMargin: 20 0 20 0");
        assertRect(40, 40, 515, 30, l.rect("K"));
        assertRect(40, 40, 515, 30, l.rect("C"));
    }

    @Test
    void trimMarginsInAListItemDropsTheMarginsOfItsFirstAndItsLastElement() {
        LayoutFixture l = layOut(
                "\tList:", "\t\tMarker: none",
                "\t\tList item:", "\t\t\tId: L", "\t\t\tTrim margins: both", "\t\t\tBox:", "\t\t\t\tPadding: 5 0",
                "\t\t\tBlock:", "\t\t\t\tId: C", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 30",
                "\t\t\t\t\tMargin: 20 0 20 0");
        assertRect(40, 40, 515, 40, l.rect("L"));
        assertRect(40, 45, 515, 30, l.rect("C"));
    }

    @Test
    void aRuleGivesTrimMarginsToTheBoxesOfAStyle() {
        LayoutFixture l = layOut(
                "\tStyles:", "\t\tFor style: panel", "\t\t\tTrim margins: both", "\t\t\tBox:", "\t\t\t\tPadding: 5",
                "\tBlock:", "\t\tId: P", "\t\tStyle: panel",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 20 0 20 0");
        assertRect(40, 40, 515, 40, l.rect("P"));
        assertRect(45, 45, 505, 30, l.rect("C"));
    }

    @Test
    void trimMarginsTopDropsOnlyTheMarginOfTheFirstChildInsideAPadding() {
        // The bottom margin of C stays inside the padding: P is 5 + 30 + 20 + 5 high.
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tTrim margins: top", "\t\tBox:", "\t\t\tPadding: 5",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 20 0 20 0",
                "\tBlock:", "\t\tId: S", "\t\tBox:", "\t\t\tHeight: 10");
        assertRect(40, 40, 515, 60, l.rect("P"));
        assertRect(45, 45, 505, 30, l.rect("C"));
        assertRect(40, 100, 515, 10, l.rect("S"));
    }

    @Test
    void trimMarginsBottomDropsOnlyTheMarginOfTheLastChildInsideAPadding() {
        // The top margin of C stays inside the padding: C starts at 40 + 5 + 20 and P is 5 + 20 + 30 + 5 high.
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tTrim margins: bottom", "\t\tBox:", "\t\t\tPadding: 5",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 20 0 20 0",
                "\tBlock:", "\t\tId: S", "\t\tBox:", "\t\t\tHeight: 10");
        assertRect(40, 40, 515, 60, l.rect("P"));
        assertRect(45, 65, 505, 30, l.rect("C"));
        assertRect(40, 100, 515, 10, l.rect("S"));
    }

    @Test
    void trimMarginsTopLetsTheBottomMarginComeOutOfAnOpenBox() {
        // The top margin of C is dropped and P keeps only its own 10; the bottom one comes out and merges with the 5
        // of S, which starts at 50 + 30 + 20.
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tTrim margins: top", "\t\tBox:", "\t\t\tMargin: 10 0 0 0",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 20 0 20 0",
                "\tBlock:", "\t\tId: S", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tMargin: 5 0 0 0");
        assertRect(40, 50, 515, 30, l.rect("P"));
        assertRect(40, 50, 515, 30, l.rect("C"));
        assertRect(40, 100, 515, 10, l.rect("S"));
    }

    @Test
    void trimMarginsBottomLetsTheTopMarginComeOutOfAnOpenBox() {
        // The top margin of C comes out and merges with the 10 of P, which starts at 40 + 20; the bottom one is
        // dropped, and S keeps only its own 5.
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tTrim margins: bottom", "\t\tBox:", "\t\t\tMargin: 10 0 0 0",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 20 0 20 0",
                "\tBlock:", "\t\tId: S", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tMargin: 5 0 0 0");
        assertRect(40, 60, 515, 30, l.rect("P"));
        assertRect(40, 60, 515, 30, l.rect("C"));
        assertRect(40, 95, 515, 10, l.rect("S"));
    }

    @Test
    void theMarginsOfAnEmptyChildComeOutOfABoxThatDoesNotTrim() {
        // The control of the next test: the 20 of the empty child of P separate A from S.
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 10",
                "\tBlock:", "\t\tId: P",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tMargin: 20 0 20 0",
                "\tBlock:", "\t\tId: S", "\t\tBox:", "\t\t\tHeight: 10");
        assertRect(40, 70, 515, 10, l.rect("S"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"both", "top", "bottom"})
    void trimMarginsOnEitherSideDropsTheMarginsOfABoxWithoutContent(String trim) {
        // The margins of the empty child touch the top and the bottom of P at once: any side drops them.
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 10",
                "\tBlock:", "\t\tId: P", "\t\tTrim margins: " + trim,
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tMargin: 20 0 20 0",
                "\tBlock:", "\t\tId: S", "\t\tBox:", "\t\t\tHeight: 10");
        assertRect(40, 50, 515, 10, l.rect("S"));
    }

    @Test
    void trimMarginsNoneIsNotTrimming() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tTrim margins: none", "\t\tBox:", "\t\t\tPadding: 5",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 20 0 20 0");
        assertRect(40, 40, 515, 80, l.rect("P"));
        assertRect(45, 65, 505, 30, l.rect("C"));
    }

    @Test
    void trimMarginsBottomInASectionKeepsTheTopMarginOfItsFirstChild() {
        LayoutFixture l = layOut(
                "\tSection:", "\t\tTrim margins: bottom",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 20 0 0 0");
        assertRect(40, 60, 515, 30, l.rect("C"));
    }

    @Test
    void trimMarginsTopInASectionStartsItsFirstChildAtTheTopOfThePageArea() {
        LayoutFixture l = layOut(
                "\tSection:", "\t\tTrim margins: top",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 20 0 0 0");
        assertRect(40, 40, 515, 30, l.rect("C"));
    }

    @Test
    void lastChildBottomMarginLeavesTheParent() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 0 0 20 0",
                "\tBlock:", "\t\tId: S", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tMargin: 5 0 0 0");
        assertRect(40, 40, 515, 30, l.rect("P"));
        assertRect(40, 90, 515, 10, l.rect("S"));
    }

    @Test
    void bottomBorderKeepsTheLastMarginInside() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tBorder: 0 0 1 0",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tMargin: 0 0 20 0",
                "\tBlock:", "\t\tId: S", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tMargin: 5 0 0 0");
        assertRect(40, 40, 515, 51, l.rect("P"));
        assertRect(40, 96, 515, 10, l.rect("S"));
    }

    @Test
    void emptyBoxLetsItsMarginsCollapseThrough() {
        // A (bottom 0), empty E (25 and 15), C (top 10): a single margin of 25.
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 10",
                "\tBlock:", "\t\tId: E", "\t\tBox:", "\t\t\tMargin: 25 0 15 0",
                "\tBlock:", "\t\tId: C", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tMargin: 10 0 0 0");
        assertRect(40, 75, 515, 0, l.rect("E"));
        assertRect(40, 75, 515, 10, l.rect("C"));
    }

    @Test
    void emptyBoxAtTheEndDoesNotOverflow() {
        // E (no height) sits at 90, past the 20 of P, whose bottom margin is the merged 30: it is not an overflow.
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P",
                "\t\tBlock:", "\t\t\tId: A", "\t\t\tBox:", "\t\t\t\tHeight: 20",
                "\t\tBlock:", "\t\t\tId: E", "\t\t\tBox:", "\t\t\t\tMargin: 30 0 30 0",
                "\tBlock:", "\t\tId: S", "\t\tBox:", "\t\t\tHeight: 10");
        assertRect(40, 40, 515, 20, l.rect("P"));
        assertRect(40, 90, 515, 0, l.rect("E"));
        assertRect(40, 90, 515, 10, l.rect("S"));
    }

    @Test
    void negativeMargins() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tMargin: 0 0 20 0",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tMargin: -5 0 0 0");
        assertRect(40, 65, 515, 10, l.rect("B"));
    }

    @Test
    void autoHeightAddsPaddingAndBorder() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tPadding: 10", "\t\t\tBorder: 1",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 30");
        assertRect(40, 40, 515, 52, l.rect("P"));
        assertRect(51, 51, 493, 30, l.rect("C"));
    }

    @Test
    void widths() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: half", "\t\tBox:", "\t\t\tWidth: 50%", "\t\t\tHeight: 10",
                "\tBlock:", "\t\tId: sized", "\t\tBox:", "\t\t\tWidth: 100", "\t\t\tPadding: 10", "\t\t\tBorder: 2",
                "\t\t\tHeight: 40",
                "\tBlock:", "\t\tId: right", "\t\tBox:", "\t\t\tWidth: 200", "\t\t\tMargin: 0 0 0 auto",
                "\t\t\tHeight: 10",
                "\tBlock:", "\t\tId: margins", "\t\tBox:", "\t\t\tMargin: 0 15 0 25", "\t\t\tHeight: 10");
        assertEquals(257.5, l.rect("half").width(), 1e-9);
        // Width and Height always measure the border box, with the padding and the border inside.
        assertRect(40, 50, 100, 40, l.rect("sized"));
        assertRect(355, 90, 200, 10, l.rect("right"));
        assertRect(65, 100, 475, 10, l.rect("margins"));
    }

    @Test
    void minWinsWhenItCannotBeComparedBeforeLayout() {
        // 150 versus 20 % of 515 (103): if they could be compared when reading, it would be an error.
        LayoutFixture l = layOut("\tBlock:", "\t\tId: minWins", "\t\tBox:", "\t\t\tMax width: 20%",
                "\t\t\tMin width: 150", "\t\t\tHeight: 10");
        assertEquals(150, l.rect("minWins").width(), 1e-9);
    }

    @Test
    void percentHeightOfADefiniteReferenceContainer() {
        // The page area always has a definite height; a parent with auto height does not (error catalog).
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 200",
                "\t\tBlock:", "\t\t\tId: half", "\t\t\tBox:", "\t\t\t\tHeight: 50%");
        assertEquals(100, l.rect("half").height(), 1e-9);
        assertEquals(100, layOutPaged(180, "\tBlock:", "\t\tId: page", "\t\tBox:", "\t\t\tHeight: 100%").rect("page").height(), 1e-9);
    }

    @Test
    void aspectRatioDeducesTheHeight() {
        LayoutFixture l = layOut("\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tWidth: 200", "\t\t\tAspect ratio: 16/9");
        assertEquals(112.5, l.rect("A").height(), 1e-9);
    }

    @Test
    void intrinsicWidths() {
        // FixedMetrics: 5 pt per character with Size 10.
        LayoutFixture l = layOut(
                "\tText: aaa bbbbbb", "\t\tId: min", "\t\tBox:", "\t\t\tWidth: narrowest", "\t\t\tPadding: 5",
                "\tText: aaa bbbbbb", "\t\tId: max", "\t\tBox:", "\t\t\tWidth: natural",
                "\tBlock:", "\t\tId: block", "\t\tBox:", "\t\t\tWidth: natural",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tWidth: 120", "\t\t\t\tMargin: 0 10",
                "\t\tText: aaa bbbbbb");
        // Narrowest width: the longest word (30) + padding; the text takes up two lines.
        assertEquals(40, l.rect("min").width(), 1e-9);
        assertEquals(34, l.rect("min").height(), 1e-9);
        assertEquals(50, l.rect("max").width(), 1e-9);
        assertEquals(140, l.rect("block").width(), 1e-9);
    }

    @Test
    void outOfFlowBoxesTakeNoSpace() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 10",
                "\tOverlay:", "\t\tBox:", "\t\t\tHeight: 50",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 10");
        assertRect(40, 50, 515, 10, l.rect("B"));
    }
}
