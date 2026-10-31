package dev.stxt.pdf.layout.position;

import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code Rotate} in the layout (reference/common/layers-and-rotation.md, reference/common/breaks.md and
 * reference/common/overflow.md): a rotated positioned element moves nothing, a rotated box in flow takes up the
 * rectangle that wraps it, a rotated box is not split, and what is checked against the page is what is painted.
 */
class RotationLayoutTest {

    /**
     * The rotated box takes up its place unrotated: its rectangle and that of its sibling are the usual ones.
     * It goes after a block of 100 because, rotated 30 degrees, it rises about 72 pt above its center.
     */
    @Test
    void rotationDoesNotChangeTheLayout() {
        LayoutFixture l = LayoutFixture.layOut("\tBlock:", "\t\tBox:", "\t\t\tHeight: 100",
                "\tFrame:", "\t\tId: R", "\t\tRotate: 30", "\t\tBox:", "\t\t\tWidth: 200", "\t\t\tHeight: 50",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 10");
        assertEquals(new Rect(40, 140, 200, 50), l.rect("R"));
        assertEquals(new Rect(40, 190, 515, 10), l.rect("B"));
    }

    /**
     * Page area of 100: after a block of 60, 40 remain, and the rotated {@code Frame} measures 60. Unrotated
     * it would be split between its two blocks; rotated, it moves whole to the next page.
     */
    @Test
    void aRotatedFrameIsNotSplit() {
        String[] content = {
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 60",
                "\tFrame:", "\t\tId: R", "{}", "\t\tBlock:", "\t\t\tId: A", "\t\t\tBox:", "\t\t\t\tHeight: 30",
                "\t\tBlock:", "\t\t\tId: Z", "\t\t\tBox:", "\t\t\t\tHeight: 30"};
        LayoutFixture plain = LayoutFixture.layOutPaged(180, with(content, "\t\tLayer: 1"));
        assertTrue(plain.has(1, "R"));
        assertTrue(plain.has(1, "A"));
        assertTrue(plain.has(2, "Z"));

        LayoutFixture turned = LayoutFixture.layOutPaged(180, with(content, "\t\tRotate: 180"));
        assertFalse(turned.has(1, "R"));
        assertEquals(new Rect(40, 40, 515, 60), turned.rect(2, "R"));
        assertEquals(new Rect(40, 70, 515, 30), turned.rect(2, "Z"));
    }

    /** At the start of a page there is nowhere to move to: if it fits, it is placed whole. */
    @Test
    void aRotatedFrameThatFitsStaysOnItsPage() {
        LayoutFixture l = LayoutFixture.layOutPaged(180,
                "\tFrame:", "\t\tId: R", "\t\tRotate: -5", "\t\tBox:", "\t\t\tHeight: 100");
        assertEquals(1, l.pages());
        assertEquals(new Rect(40, 40, 515, 100), l.rect("R"));
    }

    /**
     * An {@code Overlay} of 60 × 100 at y = 760 of the page overflows at the bottom unrotated (it reaches
     * 860 of 842), but rotated 90 degrees around its center (30, 50) it spans from 780 to 840: it fits.
     */
    @Test
    void aRotatedBoxIsCheckedAsItIsPainted() {
        String[] lines = {"\tOverlay:", "\t\tId: S", "\t\tOffset: 720 auto auto 0", "{}",
                "\t\tBox:", "\t\t\tWidth: 60", "\t\t\tHeight: 100"};
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> LayoutFixture.layOut(with(lines, "\t\tLayer: 1")));
        assertEquals(ErrorCode.BOX_OVERFLOW, e.errorCode());
        LayoutFixture l = LayoutFixture.layOut(with(lines, "\t\tRotate: 90"));
        assertEquals(new Rect(40, 760, 60, 100), l.rect("S"));
    }

    /** The other way around: it fits unrotated, but rotated it goes off the top of the page. */
    @Test
    void aRotatedBoxThatLeavesThePageIsAnOverflow() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> LayoutFixture.layOut(
                "\tFrame:", "\t\tRotate: -90", "\t\tRotate origin: top left", "\t\tBox:", "\t\t\tWidth: 100",
                "\t\t\tHeight: 10"));
        assertEquals(ErrorCode.BOX_OVERFLOW, e.errorCode());
        assertTrue(e.getMessage().contains("rotated"), e.getMessage());
    }

    // Rotation in flow: Block, Text, Paragraph and Image take up the rectangle that wraps them rotated. With
    // FixedMetrics and Size 10, each character measures 5 pt and each line, 12.

    /**
     * A {@code Text} rotated −90 without {@code Width} measures its text on one line, 25 × 12, and takes up 12 × 25:
     * what follows goes 25 lower. Its lines are not broken.
     */
    @Test
    void aTextRotatedInFlowTakesTheRectangleAroundIt() {
        LayoutFixture l = LayoutFixture.layOut("\tText: Lunes", "\t\tId: T", "\t\tRotate: -90",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 10");
        assertEquals(new Rect(40, 40, 12, 25), l.rect("T"));
        assertEquals(new Rect(40, 65, 515, 10), l.rect("B"));
        // Its unrotated box, centered in the one it takes up.
        assertEquals(new Rect(-6.5, 6.5, 25, 12), l.fragment("T").turned());
        assertEquals(1, l.fragment("T").lines().size());
    }

    /** With {@code Width}, the rotated box breaks its text in that width: two lines of 50 take up 24 × 50. */
    @Test
    void aRotatedBoxWithAWidthWrapsItsText() {
        LayoutFixture l = LayoutFixture.layOut("\tText: aaa bbb ccc", "\t\tId: T", "\t\tRotate: 90", "\t\tBox:",
                "\t\t\tWidth: 50");
        assertEquals(new Rect(40, 40, 24, 50), l.rect("T"));
        assertEquals(2, l.fragment("T").lines().size());
    }

    /**
     * What it contributes to its container is the rotated width: a Stack item with a rotated box inside measures 12.
     */
    @Test
    void theIntrinsicWidthOfARotatedBoxIsItsTurnedWidth() {
        LayoutFixture l = LayoutFixture.layOut("\tStack:", "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: I",
                "\t\t\t\tText: Lunes", "\t\t\t\t\tRotate: -90");
        assertEquals(new Rect(40, 40, 12, 25), l.rect("I"));
    }

    /** The {@code auto} margins center it by what it takes up: (515 − 12) / 2. */
    @Test
    void autoMarginsCenterTheTurnedRectangle() {
        LayoutFixture l = LayoutFixture.layOut("\tText: Lunes", "\t\tId: T", "\t\tRotate: 90", "\t\tBox:",
                "\t\t\tMargin: 0 auto");
        assertEquals(new Rect(291.5, 40, 12, 25), l.rect("T"));
    }

    /**
     * A {@code Block} of 100 × 20 rotated 90 takes up 20 × 100 at (40, 40), and its unrotated box is at (0, 80). It is
     * the reference container of the {@code Overlay} inside, which is placed in its unrotated box; the margin of its
     * child does not leave it.
     */
    @Test
    void aRotatedBlockContainsItsOverlaysAndItsMargins() {
        LayoutFixture l = LayoutFixture.layOut("\tBlock:", "\t\tId: R", "\t\tRotate: 90", "\t\tBox:",
                "\t\t\tWidth: 100",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 10", "\t\t\t\tMargin: 10 0 0 0",
                "\t\tOverlay:", "\t\t\tId: A", "\t\t\tOffset: 0 0 auto auto", "\t\t\tBox:", "\t\t\t\tWidth: 10",
                "\t\t\t\tHeight: 10");
        assertEquals(new Rect(40, 40, 20, 100), l.rect("R"));
        assertEquals(new Rect(0, 90, 100, 10), l.rect("C"));
        assertEquals(new Rect(90, 80, 10, 10), l.rect("A"));
    }

    /** It is not split: with 40 free and 50 of rotated height, it moves whole to the next page. */
    @Test
    void aBoxRotatedInFlowIsNotSplit() {
        LayoutFixture l = LayoutFixture.layOutPaged(180, "\tBlock:", "\t\tBox:", "\t\t\tHeight: 60",
                "\tText: aaa bbb ccc", "\t\tId: T", "\t\tRotate: 90", "\t\tBox:", "\t\t\tWidth: 50");
        assertFalse(l.has(1, "T"));
        assertEquals(new Rect(40, 40, 24, 50), l.rect(2, "T"));
    }

    /** Without {@code Width}, a long text does not break its lines: slightly rotated, it does not fit horizontally. */
    @Test
    void aLongRotatedTextWithoutAWidthIsTooWide() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> LayoutFixture.layOut(
                "\tText: " + "palabra ".repeat(20).strip(), "\t\tRotate: 5"));
        assertEquals(ErrorCode.BOX_OVERFLOW, e.errorCode());
        assertTrue(e.getMessage().contains("It needs a Width"), e.getMessage());
    }

    /** The lines of the document with {@code {}} replaced by {@code line}. */
    private static String[] with(String[] lines, String line) {
        return java.util.Arrays.stream(lines).map(s -> s.equals("{}") ? line : s).toArray(String[]::new);
    }
}
