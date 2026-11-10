package dev.stxt.pdf.fragment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.Rect;

/**
 * Where each fragment ended up on the paper ({@link PageFragment}, reference/implementation/api.md). Pages of 595 pt
 * wide with margins of 40: the page area starts at (40, 40) and measures 515 wide. With the fixed metrics, a
 * character measures 5 pt and a line 12.
 */
class PageFragmentTest {

    /**
     * What makes a block of a container whole ({@code Keep together}): a block with only a {@code Height} can be
     * split, and the balancing of the columns splits it by a fraction of a point.
     */
    private static final String[] WHOLE = {"\t\t\tBreak:", "\t\t\t\tKeep together: true"};

    private static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(),
                new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height())).toString());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private static PageFragment only(LaidOutDocument document, String id) {
        List<PageFragment> found = document.fragments(id);
        assertEquals(1, found.size(), id);
        return found.get(0);
    }

    /** A box of 30 inside another with a border of 2 and a padding of 10 20 30 40 (top, right, bottom, left). */
    @Test
    void theBoxesAreOnThePaperWithoutAddingUpTheAncestors() {
        LaidOutDocument document = LayoutFixture.layOut(
                "\tBlock:", "\t\tId: O", "\t\tBox:", "\t\t\tBorder: 2", "\t\t\tPadding: 10 20 30 40",
                "\t\tBlock:", "\t\t\tId: I", "\t\t\tBox:", "\t\t\t\tHeight: 30").document();
        PageFragment outer = only(document, "O");
        assertEquals(1, outer.page());
        assertEquals("O", outer.fragment().box().id());
        assertRect(40, 40, 515, 74, outer.borderBox());
        assertRect(82, 52, 451, 30, outer.contentBox());
        assertEquals(outer.borderBox(), outer.bounds());
        assertTrue(outer.inFlow());

        PageFragment inner = only(document, "I");
        assertRect(82, 52, 451, 30, inner.borderBox());
        // Going down from the parent gives the same box as asking for the child.
        assertEquals(1, outer.children().size());
        assertEquals(inner.borderBox(), outer.children().get(0).borderBox());
        assertEquals(inner.fragment(), outer.children().get(0).fragment());
    }

    @Test
    void aBoxThatIsNotThereGivesNothing() {
        LaidOutDocument document = LayoutFixture.layOut("\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 30")
                .document();
        assertEquals(List.of(), document.fragments("B"));
        assertFalse(document.fragmentsById().containsKey("B"));
        assertThrows(NullPointerException.class, () -> document.fragments(null));
    }

    /** Page area of 100: after a block of 30, the two blocks of 50 of {@code O} do not fit on the first page. */
    @Test
    void aBoxBrokenAcrossPagesHasOneFragmentPerPage() {
        LaidOutDocument document = LayoutFixture.layOutPaged(180,
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 30",
                "\tBlock:", "\t\tId: O",
                "\t\tBlock:", "\t\t\tId: C1", "\t\t\tBox:", "\t\t\t\tHeight: 50", WHOLE[0], WHOLE[1],
                "\t\tBlock:", "\t\t\tId: C2", "\t\t\tBox:", "\t\t\t\tHeight: 50", WHOLE[0], WHOLE[1]).document();
        List<PageFragment> outer = document.fragments("O");
        assertEquals(List.of(1, 2), outer.stream().map(PageFragment::page).toList());
        assertRect(40, 70, 515, 50, outer.get(0).borderBox());
        assertEquals(40, outer.get(1).borderBox().y(), 1e-9);
        assertTrue(outer.get(0).fragment().first());
        assertTrue(outer.get(1).fragment().last());
        assertEquals(1, only(document, "C1").page());
        assertEquals(2, only(document, "C2").page());
    }

    @Test
    void theIndexHasEveryBoxWithAnIdInTheOrderOfTheDocument() {
        LaidOutDocument document = LayoutFixture.layOutPaged(180,
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 30",
                "\tBlock:", "\t\tId: O",
                "\t\tBlock:", "\t\t\tId: C1", "\t\t\tBox:", "\t\t\t\tHeight: 50", WHOLE[0], WHOLE[1],
                "\t\tBlock:", "\t\t\tId: C2", "\t\t\tBox:", "\t\t\t\tHeight: 50", WHOLE[0], WHOLE[1]).document();
        Map<String, List<PageFragment>> byId = document.fragmentsById();
        assertEquals(List.of("A", "O", "C1", "C2"), byId.keySet().stream().filter(id -> id.length() <= 2).toList());
        for (String id : List.of("A", "O", "C1", "C2")) {
            List<PageFragment> asked = document.fragments(id);
            assertEquals(asked.size(), byId.get(id).size(), id);
            for (int i = 0; i < asked.size(); i++) {
                assertEquals(asked.get(i).page(), byId.get(id).get(i).page(), id);
                assertEquals(asked.get(i).borderBox(), byId.get(id).get(i).borderBox(), id);
            }
        }
        assertThrows(UnsupportedOperationException.class, () -> byId.put("X", List.of()));
    }

    /** A {@code Page overlay} is of every page: one fragment on each, apart from the flow. */
    @Test
    void aPageOverlayIsOnEveryPage() {
        LayoutFixture l = LayoutFixture.layOutPaged(180,
                "\tBlock:", "\t\tId: A", "\t\tBox:", "\t\t\tHeight: 80",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 80",
                "\tPage overlay:", "\t\tId: F", "\t\tBox:", "\t\t\tHeight: 10");
        List<PageFragment> overlay = l.document().fragments("F");
        assertEquals(List.of(1, 2), overlay.stream().map(PageFragment::page).toList());
        for (PageFragment each : overlay) {
            assertEquals(l.rect(each.page(), "F"), each.borderBox());
            assertFalse(each.inFlow());
        }
        // The roots of a page: its flow and its Page overlay boxes.
        List<PageFragment> roots = l.document().pages().get(0).fragments();
        assertEquals(2, roots.size());
        assertRect(40, 40, 515, 100, roots.get(0).borderBox());
        assertEquals("F", roots.get(1).fragment().box().id());
    }

    /**
     * Three columns of (515 − 30) / 3 = 161.67 with a gap of 15. Without balancing, the two blocks stay in the first
     * one: the other two are there, empty, as tall as the first.
     */
    @Test
    void theColumnsAreAllThereAlsoTheEmptyOnes() {
        LaidOutDocument document = LayoutFixture.layOut(
                "\tColumns:", "\t\tId: M", "\t\tColumn count: 3", "\t\tGap: 15", "\t\tBalance: false",
                "\t\tBlock:", "\t\t\tId: A", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tBackground: #cccccc",
                "\t\tBlock:", "\t\t\tId: B", "\t\t\tBox:", "\t\t\t\tHeight: 20").document();
        PageFragment columns = only(document, "M");
        List<PageFragment.Column> all = columns.columns();
        assertEquals(3, all.size());
        assertRect(40, 40, 161.67, 50, all.get(0).box());
        assertRect(216.67, 40, 161.67, 50, all.get(1).box());
        assertRect(393.33, 40, 161.67, 50, all.get(2).box());
        assertNotNull(all.get(0).flow());
        assertNull(all.get(1).flow());
        assertNull(all.get(2).flow());
        // What the first column paints ends with A: B takes up room and paints nothing.
        assertRect(40, 40, 161.67, 30, all.get(0).flow().painted().orElseThrow());
        assertEquals(List.of(), only(document, "A").columns());
    }

    /** Balanced, each block goes to its column; with a border and a padding, the columns are inside them. */
    @Test
    void theColumnsAreInsideTheContentBox() {
        LaidOutDocument document = LayoutFixture.layOut(
                "\tColumns:", "\t\tId: M", "\t\tColumn count: 2", "\t\tGap: 15",
                "\t\tBox:", "\t\t\tBorder: 5", "\t\t\tPadding: 10",
                "\t\tBlock:", "\t\t\tId: A", "\t\t\tBox:", "\t\t\t\tHeight: 30", WHOLE[0], WHOLE[1],
                "\t\tBlock:", "\t\t\tId: B", "\t\t\tBox:", "\t\t\t\tHeight: 30", WHOLE[0], WHOLE[1]).document();
        List<PageFragment.Column> all = only(document, "M").columns();
        assertEquals(2, all.size());
        // Content box of 485 from (55, 55): two columns of 235.
        assertRect(55, 55, 235, 30, all.get(0).box());
        assertRect(305, 55, 235, 30, all.get(1).box());
        assertRect(55, 55, 235, 30, all.get(0).flow().borderBox());
        assertRect(305, 55, 235, 30, all.get(1).flow().borderBox());
        assertRect(305, 55, 235, 30, only(document, "B").borderBox());
    }

    /** With {@code Min column width} the number of columns comes out of the width: three of 165 in 515. */
    @Test
    void theColumnsOfAMinimumWidthAreCountedLikeTheLayoutDoes() {
        LaidOutDocument document = LayoutFixture.layOut(
                "\tColumns:", "\t\tId: M", "\t\tMin column width: 150", "\t\tGap: 10",
                "\t\tBlock:", "\t\t\tId: A", "\t\t\tBox:", "\t\t\t\tHeight: 30", WHOLE[0], WHOLE[1]).document();
        List<PageFragment.Column> all = only(document, "M").columns();
        assertEquals(3, all.size());
        assertRect(40, 40, 165, 30, all.get(0).box());
        assertRect(390, 40, 165, 30, all.get(2).box());
    }

    /** A box of 100 with a word: it takes up 100 and what it paints is its line, 20 × 12. */
    @Test
    void whatIsPaintedEndsWithTheContentAndNotWithTheBox() {
        LaidOutDocument document = LayoutFixture.layOut(
                "\tBlock:", "\t\tId: O", "\t\tBox:", "\t\t\tHeight: 100",
                "\t\tText: aaaa", "\t\t\tId: T").document();
        PageFragment outer = only(document, "O");
        assertRect(40, 40, 515, 100, outer.borderBox());
        assertRect(40, 40, 20, 12, outer.painted().orElseThrow());
        assertRect(40, 40, 20, 12, only(document, "T").painted().orElseThrow());
    }

    @Test
    void aBoxPaintsItsBackgroundAndItsBorder() {
        LaidOutDocument document = LayoutFixture.layOut(
                "\tBlock:", "\t\tId: N", "\t\tBox:", "\t\t\tHeight: 20",
                "\tBlock:", "\t\tId: G", "\t\tBox:", "\t\t\tHeight: 20", "\t\t\tBackground: #cccccc",
                "\tBlock:", "\t\tId: T", "\t\tBox:", "\t\t\tHeight: 20", "\t\t\tBackground: #ffffff00",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 20", "\t\t\tBorder: 1").document();
        assertEquals(Optional.empty(), only(document, "N").painted());
        assertRect(40, 60, 515, 20, only(document, "G").painted().orElseThrow());
        // A fully transparent background is not seen.
        assertEquals(Optional.empty(), only(document, "T").painted());
        assertRect(40, 100, 515, 20, only(document, "B").painted().orElseThrow());
    }

    /** What goes over the flow is not content of the flow: the overlay at 60 does not lengthen what {@code O} paints. */
    @Test
    void anOverlayDoesNotCountInWhatItsContainerPaints() {
        LaidOutDocument document = LayoutFixture.layOut(
                "\tBlock:", "\t\tId: O", "\t\tBox:", "\t\t\tHeight: 100",
                "\t\tText: aaaa",
                "\t\tOverlay:", "\t\t\tId: V", "\t\t\tOffset: 60 auto auto 0",
                "\t\t\tBox:", "\t\t\t\tWidth: 50", "\t\t\t\tHeight: 30", "\t\t\t\tBackground: #cccccc").document();
        PageFragment overlay = only(document, "V");
        assertFalse(overlay.inFlow());
        assertRect(40, 100, 50, 30, overlay.borderBox());
        assertRect(40, 100, 50, 30, overlay.painted().orElseThrow());
        assertRect(40, 40, 20, 12, only(document, "O").painted().orElseThrow());
    }

    /**
     * A {@code Frame} of 200 × 50 rotated 90 degrees keeps its place in the flow: its box is the unrotated one, and
     * what is seen is 50 × 200 around its center, (140, 165), after a block of 100. What it contains turns with it.
     */
    @Test
    void aRotatedBoxSaysWhereItIsAndWhereItIsSeen() {
        LaidOutDocument document = LayoutFixture.layOut(
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 100",
                "\tFrame:", "\t\tId: R", "\t\tRotate: 90", "\t\tBox:", "\t\t\tWidth: 200", "\t\t\tHeight: 50",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 20", "\t\t\t\tBackground: #cccccc")
                .document();
        PageFragment frame = only(document, "R");
        assertRect(40, 140, 200, 50, frame.borderBox());
        assertRect(115, 65, 50, 200, frame.bounds());
        PageFragment child = only(document, "C");
        assertRect(40, 140, 200, 20, child.borderBox());
        // The strip at the top of the unrotated box ends up at the right of the rotated one.
        assertRect(145, 65, 20, 200, child.bounds());
        assertRect(145, 65, 20, 200, frame.painted().orElseThrow());
    }

    /**
     * A {@code Block} rotated in flow takes up the rectangle that encloses it, 50 × 200: that is what is seen, and its
     * box is the unrotated one, 200 × 50, centered in it.
     */
    @Test
    void aBoxRotatedInFlowIsSeenInTheRoomItTakesUp() {
        LaidOutDocument document = LayoutFixture.layOut(
                "\tBlock:", "\t\tId: T", "\t\tRotate: 90", "\t\tBox:", "\t\t\tWidth: 200", "\t\t\tHeight: 50",
                "\t\t\tBackground: #cccccc").document();
        PageFragment turned = only(document, "T");
        assertRect(40, 40, 50, 200, turned.bounds());
        assertRect(-35, 115, 200, 50, turned.borderBox());
        assertRect(40, 40, 50, 200, turned.painted().orElseThrow());
    }
}
