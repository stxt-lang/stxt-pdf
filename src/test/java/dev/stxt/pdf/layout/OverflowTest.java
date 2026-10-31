package dev.stxt.pdf.layout;

import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Overflows (reference/common/overflow.md): the culprit that {@link Overflow} points to and the measurements in its
 * message. The error catalog checks the code and the line of each case; these tests, with {@code FixedMetrics}
 * (each character measures 5 with {@code Size: 10} and a line 12), check what the catalog does not: which box is
 * named and which measurements the message gives (mutation testing).
 */
class OverflowTest {

    private static final double PAGE = 180;

    private static InvalidModelException overflow(ErrorCode code, InvalidModelException e) {
        assertEquals(code, e.errorCode(), e.getMessage());
        return e;
    }

    private static void assertContains(String expected, InvalidModelException e) {
        assertTrue(e.getMessage().contains(expected), "\"" + expected + "\" is not in: " + e.getMessage());
    }

    /** The line of the box with that {@code Id}, which gives its name in the message ("A (line 12)"). */
    private static int boxLine(String id, InvalidModelException e) {
        Matcher m = Pattern.compile(Pattern.quote(id) + " \\(line (\\d+)\\)").matcher(e.getMessage());
        assertTrue(m.find(), e.getMessage());
        return Integer.parseInt(m.group(1));
    }

    /**
     * R, an item that does not shrink, measures 600 and overflows; it does not declare a width, so the search goes down
     * to what overflows the page: S, from the content of R (border 4 and padding 6) and its margin of 4, spans from 14
     * to 517 in an area of 515.
     */
    @Test
    void tooWideNamesTheInnermostBoxOutsideThePage() {
        InvalidModelException e = overflow(ErrorCode.BOX_OVERFLOW, assertThrows(InvalidModelException.class, () -> layOut(
                "\tStack:", "\t\tItem:", "\t\t\tBase size: 600", "\t\t\tShrink: 0",
                "\t\t\tBlock:", "\t\t\t\tId: R", "\t\t\t\tBox:", "\t\t\t\t\tBorder: 4", "\t\t\t\t\tPadding: 6",
                "\t\t\t\tBlock:", "\t\t\t\t\tId: S", "\t\t\t\t\tBox:", "\t\t\t\t\t\tWidth: 503", "\t\t\t\t\t\tHeight: 10",
                "\t\t\t\t\t\tMargin: 0 0 0 4")));
        assertContains("Box S (line", e);
        assertContains("does not fit in width: it measures 503 and the content of its container measures 515", e);
    }

    /** If what is inside does not overflow the page, the culprit is the box that overflows (S, from 14 to 507). */
    @Test
    void tooWideNamesTheBoxWhenItsContentFits() {
        InvalidModelException e = overflow(ErrorCode.BOX_OVERFLOW, assertThrows(InvalidModelException.class, () -> layOut(
                "\tStack:", "\t\tItem:", "\t\t\tBase size: 600", "\t\t\tShrink: 0",
                "\t\t\tBlock:", "\t\t\t\tId: R", "\t\t\t\tBox:", "\t\t\t\t\tBorder: 4", "\t\t\t\t\tPadding: 6",
                "\t\t\t\tBlock:", "\t\t\t\t\tId: S", "\t\t\t\t\tBox:", "\t\t\t\t\t\tWidth: 493", "\t\t\t\t\t\tHeight: 10",
                "\t\t\t\t\t\tMargin: 0 0 0 4")));
        assertContains("Box R (line", e);
        assertContains("measures 600 and the content of its container measures 515", e);
    }

    @Test
    void tooTallGivesWhereTheContentEnds() {
        // Two lines (24) in a box of 20.
        InvalidModelException text = overflow(ErrorCode.BOX_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOut("\tText: aaaa bbbb cccc", "\t\tId: T", "\t\tBox:", "\t\t\tWidth: 50", "\t\t\tHeight: 20")));
        assertContains("The content of box T (line", text);
        assertContains("its text reaches 24 and the box leaves 20", text);
        // C, of 30 after a block of 5, in a box of 20: it reaches 35.
        InvalidModelException child = overflow(ErrorCode.BOX_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOut("\tBlock:", "\t\tId: H", "\t\tBox:", "\t\t\tHeight: 20",
                        "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 5",
                        "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 30")));
        assertContains("The content of box H (line", child);
        assertContains("box C (line", child);
        assertContains("reaches 35 and the box leaves 20", child);
    }

    /**
     * A line of 120 does not fit in a page area of 100: it is in the content of P (border 3 and padding 7), so it ends
     * at 130, 30 beyond. The culprit is its Line height.
     */
    @Test
    void pageOverflowGivesHowFarItGoes() {
        InvalidModelException e = overflow(ErrorCode.PAGE_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOutPaged(PAGE, "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tBorder: 3", "\t\t\tPadding: 7",
                        "\t\tText: a", "\t\t\tId: T", "\t\t\tTypography:", "\t\t\t\tLine height: 120")));
        assertContains("Box T (line", e);
        assertContains("is 120 high and extends 30 beyond the page area", e);
        assertEquals(boxLine("T", e) + 3, e.line());
    }

    /** Border and padding of 60 each do not fit in 100: with both equal, the culprit is the padding. */
    @Test
    void pageOverflowOfTheBorderAndPadding() {
        InvalidModelException e = overflow(ErrorCode.PAGE_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOutPaged(PAGE, "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tBorder: 30", "\t\t\tPadding: 30")));
        assertContains("is 120 high and extends 20 beyond the page area", e);
        assertEquals(boxLine("B", e) + 4, e.line());
        InvalidModelException border = overflow(ErrorCode.PAGE_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOutPaged(PAGE, "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tBorder: 40", "\t\t\tPadding: 20")));
        assertEquals(boxLine("B", border) + 3, border.line());
    }

    @Test
    void aRotatedBoxDoesNotBreak() {
        InvalidModelException e = overflow(ErrorCode.PAGE_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOutPaged(PAGE, "\tFrame:", "\t\tId: R", "\t\tRotate: 180", "\t\tBox:", "\t\t\tHeight: 120")));
        assertContains("Box R (line", e);
        assertContains("is rotated, so it is not broken across pages, and it does not fit on one: it is 120 high and "
                + "extends 20 beyond the page area", e);
    }

    @Test
    void rotatedBoxesOutsideThePage() {
        InvalidModelException own = overflow(ErrorCode.BOX_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOut("\tFrame:", "\t\tId: R", "\t\tRotate: -30", "\t\tRotate origin: bottom left",
                        "\t\tBox:", "\t\t\tHeight: 40")));
        assertContains("Box R (line", own);
        assertContains("rotated, goes off the page: its Rotate, its Rotate origin, its position", own);
        InvalidModelException inside = overflow(ErrorCode.BOX_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOut("\tFrame:", "\t\tRotate: 180", "\t\tBox:", "\t\t\tHeight: 100",
                        "\t\tOverlay:", "\t\t\tId: A", "\t\t\tOffset: 0 auto auto 0", "\t\t\tBox:", "\t\t\t\tWidth: 20",
                        "\t\t\t\tHeight: 150")));
        assertContains("Box A (line", inside);
        // No "its its": what has to be checked starts with "its" in both cases.
        assertContains(", inside a rotated box, goes off the page: its position and its size have to be checked", inside);
    }

    @Test
    void aPreformattedLineThatDoesNotFit() {
        InvalidModelException e = overflow(ErrorCode.TEXT_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOut("\tText: " + "a ".repeat(60), "\t\tTypography:", "\t\t\tPreformatted: true")));
        assertContains("A line of a text with Preformatted: true", e);
        assertContains("the line measures 595 and the box leaves 515", e);
    }

    /** Negative margins leave the border box outside its container, but not the margin box: it does not overflow. */
    @Test
    void negativeMarginsAreNotOverflows() {
        LayoutFixture l = layOut("\tBlock:", "\t\tId: N", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tMargin: 0 -10");
        assertEquals(535, l.rect("N").width(), 1e-9);
    }

    /**
     * The Stack has 10 of padding (its content measures 495) and a first item of 20: R starts at 30 and its content at
     * 40; S, a Frame offset by 60 (which does not count), spans from 44 to 504, inside the page: the culprit is R.
     */
    @Test
    void tooWideMeasuresFromTheContainerAndIgnoresRelativeOffsets() {
        InvalidModelException e = overflow(ErrorCode.BOX_OVERFLOW, assertThrows(InvalidModelException.class, () -> layOut(
                "\tStack:", "\t\tBox:", "\t\t\tPadding: 10",
                "\t\tItem:", "\t\t\tShrink: 0", "\t\t\tBlock:", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 20", "\t\t\t\t\tHeight: 10",
                "\t\tItem:", "\t\t\tBase size: 600", "\t\t\tShrink: 0",
                "\t\t\tBlock:", "\t\t\t\tId: R", "\t\t\t\tBox:", "\t\t\t\t\tBorder: 4", "\t\t\t\t\tPadding: 6",
                "\t\t\t\tFrame:", "\t\t\t\t\tId: S", "\t\t\t\t\tOffset: auto auto auto 60", "\t\t\t\t\tBox:",
                "\t\t\t\t\t\tWidth: 460", "\t\t\t\t\t\tHeight: 10", "\t\t\t\t\t\tMargin: 0 0 0 4")));
        assertContains("Box R (line", e);
        assertContains("measures 600 and the content of its container measures 495", e);
    }

    /** P starts 10 lower (its margin): the line of 120 ends at 10 + 10 + 120 = 140, 40 beyond. */
    @Test
    void pageOverflowCountsWhereTheBoxStarts() {
        InvalidModelException e = overflow(ErrorCode.PAGE_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOutPaged(PAGE, "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tBorder: 3", "\t\t\tPadding: 7",
                        "\t\t\tMargin: 10 0 0 0",
                        "\t\tText: a", "\t\t\tId: T", "\t\t\tTypography:", "\t\t\t\tLine height: 120")));
        assertContains("Box T (line", e);
        assertContains("extends 40 beyond the page area", e);
    }

    /**
     * T goes in a Frame offset 8 downward inside P: the overflow is measured in its place, without the
     * offset (reference/common/overflow.md), so the line of 120 ends at 10 + 120 = 130, 30
     * beyond.
     */
    @Test
    void pageOverflowIgnoresRelativeOffsets() {
        InvalidModelException e = overflow(ErrorCode.PAGE_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOutPaged(PAGE, "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tBorder: 3", "\t\t\tPadding: 7",
                        "\t\tFrame:", "\t\t\tOffset: 8 auto auto auto",
                        "\t\t\tText: a", "\t\t\t\tId: T", "\t\t\t\tTypography:", "\t\t\t\t\tLine height: 120")));
        assertContains("Box T (line", e);
        assertContains("extends 30 beyond the page area", e);
    }

    @Test
    void theContentBoxLeavesBorderAndPadding() {
        // 50 − 2 − 2 − 8 − 8 = 30 for the word of 35.
        InvalidModelException wide = overflow(ErrorCode.TEXT_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOut("\tText: aaaaaaa", "\t\tBox:", "\t\t\tWidth: 50", "\t\t\tBorder: 2", "\t\t\tPadding: 0 8")));
        assertContains("the line measures 35 and the box leaves 30", wide);
        // Two lines (24) in 30 − 2 − 2 − 5 − 5 = 16.
        InvalidModelException tall = overflow(ErrorCode.BOX_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOut("\tText: aaaa bbbb cccc", "\t\tId: T", "\t\tBox:", "\t\t\tWidth: 50", "\t\t\tHeight: 30",
                        "\t\t\tBorder: 2", "\t\t\tPadding: 5 0")));
        assertContains("its text reaches 24 and the box leaves 16", tall);
    }

    @Test
    void headerAndFooterRowsThatLeaveNoRoomForTheBody() {
        // Header of 60 and footer of 50 in a page area of 100.
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> layOutPaged(PAGE,
                "\tTable:", "\t\tId: X", "\t\tColumn sizes: *",
                "\t\tHeader rows:", "\t\t\tRow:", "\t\t\t\tCell: a", "\t\t\t\t\tBox:", "\t\t\t\t\t\tHeight: 60",
                "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: b",
                "\t\tFooter rows:", "\t\t\tRow:", "\t\t\t\tCell: c", "\t\t\t\t\tBox:", "\t\t\t\t\t\tHeight: 50"));
        assertContains("measure 110 and leave no room for the body", e);
    }
}
