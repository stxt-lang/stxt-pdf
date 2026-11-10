package dev.stxt.pdf.layout.block;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.Rect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Side boxes (reference/components/block.md). Page area of 515 from (40, 40); lines of 12 and 5 pt per character:
 * a word of four letters and its space take 25, so a line of 515 holds 20 words and one of 445 (beside a box of
 * 60 with a margin of 10), 18.
 */
class SideBoxLayoutTest {

    /** A square image of 60 × 60 with a right margin of 10 at the left: it takes 70 from the left. */
    private static final String[] LEFT = {"\tImage: test/square.png", "\t\tId: I", "\t\tSide: left", "\t\tBox:",
            "\t\t\tWidth: 60", "\t\t\tHeight: 60", "\t\t\tMargin: 0 10 0 0"};

    private static String words(int n) {
        return "aaaa ".repeat(n).strip();
    }

    private static String[] join(String[]... parts) {
        return java.util.Arrays.stream(parts).flatMap(java.util.Arrays::stream).toArray(String[]::new);
    }

    private static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(), new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height())).toString());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private static int wordsOf(TextLine l) {
        return l.text().isEmpty() ? 0 : l.text().split(" ").length;
    }

    @Test
    void leftSideBoxShortensTheLinesBesideIt() {
        LayoutFixture l = layOut(join(LEFT, new String[]{"\tParagraph: " + words(30), "\t\tId: P"}));
        assertRect(40, 40, 60, 60, l.rect("I"));
        // The paragraph takes the whole width; its lines start after the box and its margin.
        assertRect(40, 40, 515, 24, l.rect("P"));
        List<TextLine> lines = l.fragment("P").lines();
        assertEquals(2, lines.size());
        assertEquals(70, lines.get(0).left(), 1e-9);
        assertEquals(70, lines.get(0).x(), 1e-9);
        assertEquals(18, wordsOf(lines.get(0)));
        assertEquals(445, lines.get(0).width(), 1e-9);
        assertEquals(12, wordsOf(lines.get(1)));
    }

    @Test
    void rightSideBoxShortensTheLinesFromTheRight() {
        LayoutFixture l = layOut("\tImage: test/square.png", "\t\tId: I", "\t\tSide: right", "\t\tBox:",
                "\t\t\tWidth: 60", "\t\t\tHeight: 60", "\t\t\tMargin: 0 0 0 10",
                "\tParagraph: " + words(30), "\t\tId: P");
        assertRect(495, 40, 60, 60, l.rect("I"));
        TextLine first = l.fragment("P").lines().get(0);
        assertEquals(0, first.left(), 1e-9);
        assertEquals(70, first.right(), 1e-9);
        assertEquals(18, wordsOf(first));
    }

    @Test
    void linesBelowTheSideBoxHaveTheWholeWidth() {
        LayoutFixture l = layOut(join(LEFT, new String[]{"\tParagraph: " + words(130), "\t\tId: P"}));
        List<TextLine> lines = l.fragment("P").lines();
        // Five lines of 12 overlap the 60 of the box; from the sixth on, 20 words per line.
        assertEquals(70, lines.get(4).left(), 1e-9);
        assertEquals(0, lines.get(5).left(), 1e-9);
        assertEquals(20, wordsOf(lines.get(5)));
        assertEquals(List.of(18, 18, 18, 18, 18, 20, 20), lines.stream().map(SideBoxLayoutTest::wordsOf).toList());
    }

    @Test
    void theTextThatFollowsKeepsRunningBesideTheBox() {
        // P1 takes two lines (up to 24); P2 starts at 24 and its first three lines still overlap the box.
        LayoutFixture l = layOut(join(LEFT, new String[]{"\tParagraph: " + words(30), "\t\tId: P1",
                "\tParagraph: " + words(80), "\t\tId: P2"}));
        assertRect(40, 64, 515, 60, l.rect("P2"));
        assertEquals(List.of(18, 18, 18, 20, 6),
                l.fragment("P2").lines().stream().map(SideBoxLayoutTest::wordsOf).toList());
    }

    @Test
    void aChildThatIsNotTextGoesBelowTheSideBoxes() {
        LayoutFixture l = layOut(join(LEFT, new String[]{"\tStack:", "\t\tId: S", "\t\tItem:", "\t\t\tBlock:",
                "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10"}));
        assertRect(40, 100, 515, 10, l.rect("S"));
    }

    @Test
    void clearMovesTheTextBelowTheSideBoxes() {
        LayoutFixture l = layOut(join(LEFT, new String[]{"\tParagraph: " + words(5), "\t\tId: P", "\t\tClear: true"}));
        assertRect(40, 100, 515, 12, l.rect("P"));
        assertEquals(0, l.fragment("P").lines().get(0).left(), 1e-9);
    }

    @Test
    void aTextWithBackgroundOrBorderGoesBelow() {
        LayoutFixture l = layOut(join(LEFT, new String[]{"\tParagraph: " + words(5), "\t\tId: P", "\t\tBox:",
                "\t\t\tBackground: #EEEEEE"}));
        assertRect(40, 100, 515, 12, l.rect("P"));
    }

    @Test
    void theBlockReachesTheBottomOfItsSideBoxes() {
        LayoutFixture l = layOut("\tBlock:", "\t\tId: B",
                "\t\tImage: test/square.png", "\t\t\tSide: left", "\t\t\tBox:", "\t\t\t\tWidth: 60", "\t\t\t\tHeight: 60",
                "\t\tParagraph: " + words(5), "\t\t\tId: P",
                "\tBlock:", "\t\tId: N", "\t\tBox:", "\t\t\tHeight: 10");
        assertRect(40, 40, 515, 60, l.rect("B"));
        assertRect(40, 40, 515, 12, l.rect("P"));
        assertRect(40, 100, 515, 10, l.rect("N"));
    }

    @Test
    void sideBoxesOfTheSameSideStack() {
        LayoutFixture l = layOut(join(LEFT, new String[]{"\tImage: test/square.png", "\t\tId: J", "\t\tSide: left",
                "\t\tBox:", "\t\t\tWidth: 60", "\t\t\tHeight: 60", "\t\t\tMargin: 0 10 0 0",
                "\tParagraph: " + words(200), "\t\tId: P"}));
        assertRect(40, 40, 60, 60, l.rect("I"));
        assertRect(40, 100, 60, 60, l.rect("J"));
        List<TextLine> lines = l.fragment("P").lines();
        assertEquals(70, lines.get(9).left(), 1e-9);
        assertEquals(0, lines.get(10).left(), 1e-9);
    }

    @Test
    void sideBoxesOnBothSidesLeaveTheMiddle() {
        LayoutFixture l = layOut(join(LEFT, new String[]{"\tImage: test/square.png", "\t\tId: J", "\t\tSide: right",
                "\t\tBox:", "\t\t\tWidth: 60", "\t\t\tHeight: 24", "\t\t\tMargin: 0 0 0 10",
                "\tParagraph: " + words(60), "\t\tId: P"}));
        assertRect(495, 40, 60, 24, l.rect("J"));
        List<TextLine> lines = l.fragment("P").lines();
        // 515 − 70 − 70 = 375: 15 words; then 445: 18.
        assertEquals(70, lines.get(0).left(), 1e-9);
        assertEquals(70, lines.get(0).right(), 1e-9);
        assertEquals(15, wordsOf(lines.get(0)));
        assertEquals(0, lines.get(2).right(), 1e-9);
        assertEquals(18, wordsOf(lines.get(2)));
    }

    @Test
    void aHeadingAndAMarkdownRunBesideTheSideBox() {
        LayoutFixture l = layOut(join(LEFT, new String[]{"\tHeading 1:", "\t\tId: H", "\t\tText: " + words(5),
                "\t\t\tId: T",
                "\tMarkdown:", "\t\tId: M", "\t\tTable column sizes: equal", "\t\tContent >>",
                "\t\t\t" + words(5), "", "\t\t\t| a | b |", "\t\t\t|---|---|", "\t\t\t| c | d |"}));
        assertEquals(70, l.fragment("T").lines().get(0).left(), 1e-9);
        Fragment markdown = l.fragment("M");
        // The paragraph runs beside the box; the table goes below it: the box ends at 60, the Markdown starts at 12.
        assertEquals(70, markdown.children().get(0).lines().get(0).left(), 1e-9);
        assertEquals(48, markdown.children().get(1).y(), 1e-9);
    }

    @Test
    void aListGoesBelowASideBoxOnTheLeft() {
        // Its markers hang at its left, where the box is.
        LayoutFixture l = layOut(join(LEFT, new String[]{"\tList:", "\t\tId: L", "\t\tMarker: bullet",
                "\t\tBox:", "\t\t\tPadding: 0 0 0 20", "\t\tList item:", "\t\t\tParagraph: " + words(3)}));
        assertRect(40, 100, 515, 12, l.rect("L"));
    }

    @Test
    void aListRunsBesideASideBoxOnTheRight() {
        LayoutFixture l = layOut("\tImage: test/square.png", "\t\tId: I", "\t\tSide: right", "\t\tBox:",
                "\t\t\tWidth: 60", "\t\t\tHeight: 60", "\t\t\tMargin: 0 0 0 10",
                "\tList:", "\t\tId: L", "\t\tMarker: bullet", "\t\tBox:", "\t\t\tPadding: 0 0 0 20",
                "\t\tList item:", "\t\t\tParagraph: " + words(40), "\t\t\t\tId: P");
        assertRect(40, 40, 515, 36, l.rect("L"));
        List<TextLine> lines = l.fragment("P").lines();
        // The item is 495 wide and the box takes 70 from its right: 425, 17 words; then 495, 20.
        assertEquals(70, lines.get(0).right(), 1e-9);
        assertEquals(List.of(17, 17, 6), lines.stream().map(SideBoxLayoutTest::wordsOf).toList());
    }

    @Test
    void aSideBoxThatDoesNotFitMovesToTheNextPageWithItsText() {
        // Page area of 100: the Block leaves 40 and the box of 60 does not fit.
        LayoutFixture l = layOutPaged(180, join(new String[]{"\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 60"},
                LEFT, new String[]{"\tParagraph: " + words(20), "\t\tId: P"}));
        assertEquals(2, l.pages());
        l.assertAbsent(1, "I");
        assertRect(40, 40, 60, 60, l.rect(2, "I"));
        assertRect(40, 40, 515, 24, l.rect(2, "P"));
    }

    @Test
    void theSideBoxGoesWithTheFirstLineThatRunsBesideIt() {
        // The Block leaves 10: the box of 8 fits, the first line of 12 does not, and both move to page 2.
        LayoutFixture l = layOutPaged(180, "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 90",
                "\tImage: test/wide.png", "\t\tId: I", "\t\tSide: left", "\t\tBox:", "\t\t\tWidth: 60", "\t\t\tHeight: 8",
                "\tParagraph: " + words(5), "\t\tId: P");
        assertEquals(2, l.pages());
        l.assertAbsent(1, "I");
        assertRect(40, 40, 60, 8, l.rect(2, "I"));
        assertRect(40, 40, 515, 12, l.rect(2, "P"));
    }

    @Test
    void theTextCutBesideASideBoxIsBrokenAgainAtTheWholeWidth() {
        // The Block leaves 60: the box fits exactly and five lines of 18 words with it; the other 30 words go to
        // page 2, where nothing is beside them: 20 and 10.
        LayoutFixture l = layOutPaged(180, join(new String[]{"\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 40"},
                LEFT, new String[]{"\tParagraph: " + words(120), "\t\tId: P"}));
        assertEquals(2, l.pages());
        assertRect(40, 80, 60, 60, l.rect(1, "I"));
        assertRect(40, 80, 515, 60, l.rect(1, "P"));
        assertEquals(List.of(18, 18, 18, 18, 18),
                l.fragment(1, "P").lines().stream().map(SideBoxLayoutTest::wordsOf).toList());
        assertRect(40, 40, 515, 24, l.rect(2, "P"));
        List<TextLine> second = l.fragment(2, "P").lines();
        assertEquals(List.of(20, 10), second.stream().map(SideBoxLayoutTest::wordsOf).toList());
        assertEquals(0, second.get(0).left(), 1e-9);
        assertTrue(l.fragment(1, "P").first() && !l.fragment(1, "P").last());
    }

    @Test
    void aWordThatDoesNotFitBesideTheBoxLeavesTheLineEmpty() {
        // A box of 480 leaves 35: no word of 40 fits beside it, and the lines beside it stay empty.
        LayoutFixture l = layOut("\tImage: test/square.png", "\t\tId: I", "\t\tSide: left", "\t\tBox:",
                "\t\t\tWidth: 480", "\t\t\tHeight: 24",
                "\tParagraph: aaaaaaaa aaaaaaaa", "\t\tId: P");
        List<TextLine> lines = l.fragment("P").lines();
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).isEmpty() && lines.get(1).isEmpty());
        assertEquals("aaaaaaaa aaaaaaaa", lines.get(2).text());
        assertEquals(0, lines.get(2).left(), 1e-9);
    }

    // Cases that were missing according to mutation testing.

    /**
     * The widows of a text cut beside a side box are counted at the whole width. A box of 96 keeps the eight lines of
     * page 1 at 18 words; with 163 words, the cut after eight lines would leave 19 words, two lines beside the box but
     * one at the whole width: fewer than the two widows, so the cut moves up a line and 37 words go to page 2.
     */
    @Test
    void theWidowsOfATextCutBesideASideBoxAreCountedAtTheWholeWidth() {
        LayoutFixture l = layOutPaged(180, "\tImage: test/square.png", "\t\tId: I", "\t\tSide: left", "\t\tBox:",
                "\t\t\tWidth: 60", "\t\t\tHeight: 96", "\t\t\tMargin: 0 10 0 0",
                "\tParagraph: " + words(163), "\t\tId: P");
        assertEquals(2, l.pages());
        assertEquals(List.of(18, 18, 18, 18, 18, 18, 18),
                l.fragment(1, "P").lines().stream().map(SideBoxLayoutTest::wordsOf).toList());
        assertEquals(List.of(20, 17), l.fragment(2, "P").lines().stream().map(SideBoxLayoutTest::wordsOf).toList());
    }

    /** The vertical margins of a side box move it down and count in the height of its block. */
    @Test
    void aSideBoxKeepsItsVerticalMargins() {
        LayoutFixture l = layOut("\tBlock:", "\t\tId: B",
                "\t\tImage: test/square.png", "\t\t\tId: I", "\t\t\tSide: left", "\t\t\tBox:",
                "\t\t\t\tWidth: 60", "\t\t\t\tHeight: 60", "\t\t\t\tMargin: 10 10 10 0",
                "\t\tParagraph: " + words(30), "\t\t\tId: P");
        assertRect(40, 50, 60, 60, l.rect("I"));
        // The block reaches the bottom margin of the box: 10 + 60 + 10.
        assertRect(40, 40, 515, 80, l.rect("B"));
        assertRect(40, 40, 515, 24, l.rect("P"));
        List<TextLine> lines = l.fragment("P").lines();
        assertEquals(List.of(18, 12), lines.stream().map(SideBoxLayoutTest::wordsOf).toList());
        assertEquals(70, lines.get(0).left(), 1e-9);
        assertEquals(70, lines.get(1).left(), 1e-9);
    }
}
