package dev.stxt.pdf.layout.block;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Rect;
import org.junit.jupiter.api.Test;

import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pages of 180 pt with margins of 40: page area of 515 × 100 from (40, 40). */
class BlockFragmentationTest {

    private static final double PAGE = 180;

    private static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(), new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height())).toString());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    @Test
    void fixedHeightIsTheTotalOfAllFragments() {
        // 250 = 100 + 100 + 50.
        LayoutFixture l = layOutPaged(PAGE, "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 250");
        assertEquals(3, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "A"));
        assertRect(40, 40, 515, 100, l.rect(2, "A"));
        assertRect(40, 40, 515, 50, l.rect(3, "A"));
    }

    @Test
    void boxThatDoesNotFitIsSliced() {
        // B starts at 60: 40 of its 60 pt fit and the rest moves to page 2.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 60",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 60");
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 60, l.rect(1, "A"));
        assertRect(40, 100, 515, 40, l.rect(1, "B"));
        assertRect(40, 40, 515, 20, l.rect(2, "B"));
    }

    @Test
    void keepTogetherMovesTheBox() {
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 60",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 60", "\t\tBreak:", "\t\t\tKeep together: true");
        assertEquals(2, l.pages());
        assertFalse(l.has(1, "B"));
        assertRect(40, 40, 515, 60, l.rect(2, "B"));
    }

    @Test
    void openEdgesDropBorderAtTheBreak() {
        // Q: border 2 and padding 3. First fragment: 5 + C1 (40) + 55 of C2 up to the end of the page.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: Q", "\t\tBox:", "\t\t\tBorder: 2", "\t\t\tPadding: 3",
                "\t\tBlock:", "\t\t\tId: C1", "\t\t\tBox:", "\t\t\t\tHeight: 40",
                "\t\tBlock:", "\t\t\tId: C2", "\t\t\tBox:", "\t\t\t\tHeight: 80");
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "Q"));
        assertRect(45, 45, 505, 40, l.rect(1, "C1"));
        assertRect(45, 85, 505, 55, l.rect(1, "C2"));
        // Second fragment: without top border or padding; 25 of C2 + 5 at the bottom.
        assertRect(40, 40, 515, 30, l.rect(2, "Q"));
        assertRect(45, 40, 505, 25, l.rect(2, "C2"));
        Fragment first = l.fragment(1, "Q");
        Fragment second = l.fragment(2, "Q");
        assertEquals(new Insets(2, 2, 0, 2), first.border());
        assertEquals(new Insets(0, 2, 2, 2), second.border());
        assertTrue(first.first() && !first.last());
        assertTrue(!second.first() && second.last());
    }

    @Test
    void sliceReservesBottomDecorationWhenTheBoxEnds() {
        // The content (40 + 55) fits in 100, but the bottom padding (10) does not: with 90 for the
        // content, C2 (Keep together) moves to page 2 and Q ends there with its padding.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: Q", "\t\tBox:", "\t\t\tPadding: 0 0 10 0",
                "\t\tBlock:", "\t\t\tId: C1", "\t\t\tBox:", "\t\t\t\tHeight: 40",
                "\t\tBlock:", "\t\t\tId: C2", "\t\t\tBox:", "\t\t\t\tHeight: 55", "\t\t\tBreak:",
                "\t\t\t\tKeep together: true");
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 40, l.rect(1, "Q"));
        assertRect(40, 40, 515, 65, l.rect(2, "Q"));
        assertRect(40, 40, 515, 55, l.rect(2, "C2"));
    }

    @Test
    void sliceReservesBottomDecorationForText() {
        // Five lines of 20 fit in 100, but the bottom padding does not. With 90, four fit, which
        // would leave a widow: three are broken off and the last two move over with the padding.
        LayoutFixture l = layOutPaged(PAGE,
                "\tText: aaaaaaaa bbbbbbbb cccccccc dddddddd eeeeeeee", "\t\tId: Q", "\t\tBox:", "\t\t\tWidth: 50",
                "\t\t\tPadding: 0 0 10 0", "\t\tTypography:", "\t\t\tLine height: 20");
        assertEquals(2, l.pages());
        assertRect(40, 40, 50, 60, l.rect(1, "Q"));
        assertRect(40, 40, 50, 50, l.rect(2, "Q"));
    }

    @Test
    void sliceDoesNotReserveWhenTheBoxContinues() {
        // Q is split anyway: the first fragment reaches the end of the page, without a gap.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: Q", "\t\tBox:", "\t\t\tPadding: 0 0 10 0",
                "\t\tBlock:", "\t\t\tId: C1", "\t\t\tBox:", "\t\t\t\tHeight: 150");
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "Q"));
        assertRect(40, 40, 515, 60, l.rect(2, "Q"));
    }

    @Test
    void closedEdgesRepeatBorderInEveryFragment() {
        // Like the previous one, with clone: the content of each page fits between 5 at the top and 5 at the bottom
        // (90 pt). Page 1: C1 (40) + 50 of C2. Page 2: 5 + the 30 of C2 that are missing + 5.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: Q", "\t\tBox:", "\t\t\tBorder: 2", "\t\t\tPadding: 3", "\t\tBreak:",
                "\t\t\tEdges: closed",
                "\t\tBlock:", "\t\t\tId: C1", "\t\t\tBox:", "\t\t\t\tHeight: 40",
                "\t\tBlock:", "\t\t\tId: C2", "\t\t\tBox:", "\t\t\t\tHeight: 80");
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "Q"));
        assertRect(45, 85, 505, 50, l.rect(1, "C2"));
        assertRect(40, 40, 515, 40, l.rect(2, "Q"));
        assertRect(45, 45, 505, 30, l.rect(2, "C2"));
        assertEquals(new Insets(2, 2, 2, 2), l.fragment(1, "Q").border());
        assertEquals(new Insets(3, 3, 3, 3), l.fragment(2, "Q").padding());
    }

    @Test
    void cloneFixedHeightIsTheTotalOfTheContent() {
        // Height 250 with border 5: 240 of content in pieces of 90, each one with 10 of border.
        LayoutFixture l = layOutPaged(PAGE, "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 250", "\t\t\tBorder: 5",
                "\t\tBreak:", "\t\t\tEdges: closed");
        assertEquals(3, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "A"));
        assertRect(40, 40, 515, 100, l.rect(2, "A"));
        assertRect(40, 40, 515, 70, l.rect(3, "A"));
    }

    @Test
    void closedTextKeepsItsEdgesOnEveryPage() {
        // Padding 10 with clone: 80 / 12 = 6 lines fit per page; 10 lines = 6 + 4.
        LayoutFixture l = layOutPaged(PAGE, "\tText: " + String.join(" ", java.util.Collections.nCopies(10, "aaaaa")),
                "\t\tId: P", "\t\tBox:", "\t\t\tWidth: 50", "\t\t\tPadding: 10", "\t\tBreak:",
                "\t\t\tEdges: closed");
        assertEquals(2, l.pages());
        assertRect(40, 40, 50, 92, l.rect(1, "P"));
        assertRect(40, 40, 50, 68, l.rect(2, "P"));
        assertEquals(6, l.fragment(1, "P").lines().size());
    }

    @Test
    void unforcedBreakTruncatesTheMargin() {
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 95",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tMargin: 10 0 0 0", "\t\tBreak:",
                "\t\t\tKeep together: true");
        assertRect(40, 40, 515, 10, l.rect(2, "B"));
    }

    @Test
    void forcedBreakKeepsTheMargin() {
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 10",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tMargin: 10 0 0 0", "\t\tBreak:",
                "\t\t\tBefore: page");
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 10, l.rect(1, "A"));
        assertRect(40, 50, 515, 10, l.rect(2, "B"));
    }

    @Test
    void forcedBreakOfTheFirstChildPropagates() {
        // The forced break of C comes out of P: the whole of P moves to page 2.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 10",
                "\tBlock:", "\t\tId: P",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 10", "\t\t\tBreak:", "\t\t\t\tBefore: page",
                "\tBlock:", "\t\tId: D", "\t\tBox:", "\t\t\tHeight: 10", "\t\tBreak:", "\t\t\tAfter: page");
        assertEquals(2, l.pages());
        assertFalse(l.has(1, "P"));
        assertRect(40, 40, 515, 10, l.rect(2, "P"));
        assertRect(40, 50, 515, 10, l.rect(2, "D"));
    }

    @Test
    void forcedBreakAtTheStartOfThePageIsIgnored() {
        LayoutFixture l = layOutPaged(PAGE, "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 10", "\t\tBreak:",
                "\t\t\tBefore: page");
        assertEquals(1, l.pages());
        assertRect(40, 40, 515, 10, l.rect(1, "A"));
    }

    @Test
    void keepWithNextKeepsTheTitleWithTheNextBox() {
        // T cannot be left alone at the end: the break goes before T, which is a break without violated rules.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: X", "\t\tBox:", "\t\t\tHeight: 50",
                "\tBlock:", "\t\tId: T", "\t\tBox:", "\t\t\tHeight: 20", "\t\tBreak:", "\t\t\tKeep with next: true",
                "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tHeight: 60", "\t\tBreak:", "\t\t\tKeep together: true");
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 50, l.rect(1, "X"));
        assertFalse(l.has(1, "T"));
        assertRect(40, 40, 515, 20, l.rect(2, "T"));
        assertRect(40, 60, 515, 60, l.rect(2, "P"));
    }

    @Test
    void aHeadingStaysWithTheNextBoxWithoutDeclaringIt() {
        // The engine sets Keep with next: true on H (24 pt, two lines of 12): the break goes before H.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: X", "\t\tBox:", "\t\t\tHeight: 70",
                "\tHeading 1:", "\t\tId: H", "\t\tText: Uno", "\t\tText: Dos",
                "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tHeight: 30", "\t\tBreak:", "\t\t\tKeep together: true");
        assertEquals(2, l.pages());
        assertFalse(l.has(1, "H"));
        assertRect(40, 40, 515, 24, l.rect(2, "H"));
        assertRect(40, 64, 515, 30, l.rect(2, "P"));
    }

    @Test
    void aHeadingIsNotSplit() {
        // After X, 20 pt remain: the first line of H fits, but the engine sets Keep together: true.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: X", "\t\tBox:", "\t\t\tHeight: 80",
                "\tHeading 1:", "\t\tId: H", "\t\tText: Uno", "\t\tText: Dos");
        assertEquals(2, l.pages());
        assertFalse(l.has(1, "H"));
        assertRect(40, 40, 515, 24, l.rect(2, "H"));
    }

    @Test
    void keepsAreRelaxedWhenThereIsNoOtherBreak() {
        // T is the first thing on the page: there is no better break than the one that violates its Keep with next.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: T", "\t\tBox:", "\t\t\tHeight: 20", "\t\tBreak:", "\t\t\tKeep with next: true",
                "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tHeight: 90", "\t\tBreak:", "\t\t\tKeep together: true");
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 20, l.rect(1, "T"));
        assertRect(40, 40, 515, 90, l.rect(2, "P"));
    }

    @Test
    void keepWithNextOfTheLastChildPropagatesToItsBlock() {
        // T ends W and keeps with what follows W: the break between W and P violates it, so W moves to page 2 whole
        // (a previous child is never split to make room).
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: X", "\t\tBox:", "\t\t\tHeight: 50",
                "\tBlock:", "\t\tId: W",
                "\t\tBlock:", "\t\t\tId: A", "\t\t\tBox:", "\t\t\t\tHeight: 10",
                "\t\tBlock:", "\t\t\tId: T", "\t\t\tBox:", "\t\t\t\tHeight: 20", "\t\t\tBreak:",
                "\t\t\t\tKeep with next: true",
                "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tHeight: 60", "\t\tBreak:", "\t\t\tKeep together: true");
        assertEquals(2, l.pages());
        assertFalse(l.has(1, "T"));
        assertRect(40, 40, 515, 30, l.rect(2, "W"));
        assertRect(40, 50, 515, 20, l.rect(2, "T"));
        assertRect(40, 70, 515, 60, l.rect(2, "P"));
    }

    @Test
    void keepWithPreviousOfTheFirstChildPropagatesToItsBlock() {
        // T starts W and keeps with Q: the break between Q and W violates it, so Q moves to page 2 with W.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: X", "\t\tBox:", "\t\t\tHeight: 70",
                "\tBlock:", "\t\tId: Q", "\t\tBox:", "\t\t\tHeight: 20",
                "\tBlock:", "\t\tId: W",
                "\t\tBlock:", "\t\t\tId: T", "\t\t\tBox:", "\t\t\t\tHeight: 20", "\t\t\tBreak:",
                "\t\t\t\tKeep with previous: true", "\t\t\t\tKeep together: true");
        assertEquals(2, l.pages());
        assertFalse(l.has(1, "Q"));
        assertRect(40, 40, 515, 20, l.rect(2, "Q"));
        assertRect(40, 60, 515, 20, l.rect(2, "T"));
    }

    @Test
    void keepTogetherIsRelaxedWhenTheBoxIsTallerThanThePage() {
        // B moves to page 2 and, since it does not fit whole there either, it is split anyway.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 10",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 150", "\t\tBreak:", "\t\t\tKeep together: true",
                "\tBlock:", "\t\tId: C", "\t\tBox:", "\t\t\tHeight: 10");
        assertEquals(3, l.pages());
        assertRect(40, 40, 515, 10, l.rect(1, "A"));
        assertRect(40, 40, 515, 100, l.rect(2, "B"));
        assertRect(40, 40, 515, 50, l.rect(3, "B"));
        assertRect(40, 90, 515, 10, l.rect(3, "C"));
    }

    @Test
    void overlayChildFallsOnThePageOfItsPosition() {
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 90",
                "\tFrame:", "\t\tId: P", "\t\tBreak:", "\t\t\tKeep together: true",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 30",
                "\t\tOverlay:", "\t\t\tId: S", "\t\t\tOffset: 0 0 auto auto", "\t\t\tBox:", "\t\t\t\tWidth: 20",
                "\t\t\t\tHeight: 20");
        assertFalse(l.has(1, "S"));
        assertRect(535, 40, 20, 20, l.rect(2, "S"));
    }

    @Test
    void framePercentOffsetUsesTheWholeHeightOfAFragmentedParent() {
        // The 10 % is of the whole Height of P (30), not of the fragment of the first page (10): R
        // is in its place (80-95) inside the page and Overflow cannot move it with another base.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tHeight: 300",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 80",
                "\t\tFrame:", "\t\t\tId: R", "\t\t\tOffset: 10% auto auto auto", "\t\t\tBox:",
                "\t\t\t\tHeight: 15");
        assertEquals(3, l.pages());
        assertRect(40, 150, 515, 15, l.rect(1, "R"));
    }
}
