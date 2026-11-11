package dev.stxt.pdf.layout.inline;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;

/**
 * {@code First line indent} in the layout (reference/common/typography.md). Page area of 515 from (40, 40); lines of
 * 12 (Size 10) and 5 pt per character ({@link FixedMetrics}): a word of four letters and its space take 25, so a line
 * of 515 holds 20 words (495) and one of 485, 19 (470).
 */
class FirstLineIndentLayoutTest {

    private static String words(int n) {
        return "aaaa ".repeat(n).strip();
    }

    private static int count(TextLine line) {
        return line.text().split(" ").length;
    }

    private static List<String> texts(List<TextLine> lines) {
        return lines.stream().map(TextLine::text).toList();
    }

    @Test
    void theFirstLineStartsLaterAndIsShorter() {
        List<TextLine> lines = layOut("\tParagraph: " + words(50), "\t\tId: P", "\t\tTypography:",
                "\t\t\tFirst line indent: 30").fragment("P").lines();
        assertEquals(3, lines.size());
        assertEquals(30, lines.get(0).x(), 1e-9);
        assertEquals(30, lines.get(0).left(), 1e-9);
        assertEquals(19, count(lines.get(0)));
        assertEquals(0, lines.get(1).x(), 1e-9);
        assertEquals(0, lines.get(1).left(), 1e-9);
        assertEquals(20, count(lines.get(1)));
        assertEquals(11, count(lines.get(2)));
    }

    @Test
    void theTwoLineBreakingsIndentTheSame() {
        List<TextLine> lines = layOut("\tParagraph: " + words(50), "\t\tId: P", "\t\tTypography:",
                "\t\t\tFirst line indent: 30", "\t\t\tLine breaking: optimal").fragment("P").lines();
        assertEquals(30, lines.get(0).x(), 1e-9);
        assertTrue(count(lines.get(0)) <= 19);
        assertEquals(0, lines.get(1).x(), 1e-9);
    }

    @Test
    void itIsALengthInPointsOrMillimetersAndNotAPercentage() {
        List<TextLine> lines = layOut("\tParagraph: " + words(3), "\t\tId: P", "\t\tTypography:",
                "\t\t\tFirst line indent: 10mm").fragment("P").lines();
        assertEquals(10 * 72 / 25.4, lines.get(0).x(), 1e-9);
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> layOut("\tParagraph: " + words(3),
                "\t\tTypography:", "\t\t\tFirst line indent: 5%"));
        assertEquals(ErrorCode.VALUE_NOT_ALLOWED, e.errorCode());
    }

    @Test
    void itIsInheritedAndABoxCanTakeItBack() {
        LayoutFixture f = layOut("\tBlock:", "\t\tTypography:", "\t\t\tFirst line indent: 30",
                "\t\tParagraph: " + words(3), "\t\t\tId: P",
                "\t\tParagraph: " + words(3), "\t\t\tId: Q", "\t\t\tTypography:", "\t\t\t\tFirst line indent: 0");
        assertEquals(30, f.fragment("P").lines().get(0).x(), 1e-9);
        assertEquals(0, f.fragment("Q").lines().get(0).x(), 1e-9);
    }

    @Test
    void theLineIsAlignedInWhatTheIndentLeaves() {
        // A box of 100 and a word of 20: the indent leaves 80.
        for (String[] align : new String[][] {{"right", "80"}, {"center", "50"}, {"justify", "20"}}) {
            List<TextLine> lines = layOut("\tText: aaaa", "\t\tId: T", "\t\tBox:", "\t\t\tWidth: 100",
                    "\t\tTypography:", "\t\t\tFirst line indent: 20", "\t\t\tAlign: " + align[0]).fragment("T").lines();
            assertEquals(Double.parseDouble(align[1]), lines.get(0).x(), 1e-9, align[0]);
        }
    }

    @Test
    void aNegativeIndentStartsTheFirstLineInTheLeftPadding() {
        // A hanging indent: the content box is 485 wide and the first line, 515.
        List<TextLine> lines = layOut("\tParagraph: " + words(50), "\t\tId: P", "\t\tBox:", "\t\t\tPadding: 0 0 0 30",
                "\t\tTypography:", "\t\t\tFirst line indent: -30").fragment("P").lines();
        assertEquals(-30, lines.get(0).x(), 1e-9);
        assertEquals(-30, lines.get(0).left(), 1e-9);
        assertEquals(20, count(lines.get(0)));
        assertEquals(0, lines.get(1).x(), 1e-9);
        assertEquals(19, count(lines.get(1)));
    }

    @Test
    void aNegativeIndentGreaterThanTheLeftPaddingIsAnError() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> layOut("\tParagraph: " + words(3),
                "\t\tBox:", "\t\t\tPadding: 0 0 0 20", "\t\tTypography:", "\t\t\tFirst line indent: -30"));
        assertEquals(ErrorCode.FIRST_LINE_INDENT_OVERFLOW, e.errorCode());
        // Inherited, it is checked in each box with text.
        e = assertThrows(InvalidModelException.class, () -> layOut("\tBlock:", "\t\tBox:", "\t\t\tPadding: 0 0 0 30",
                "\t\tTypography:", "\t\t\tFirst line indent: -30", "\t\tParagraph: " + words(3)));
        assertEquals(ErrorCode.FIRST_LINE_INDENT_OVERFLOW, e.errorCode());
        // A padding in percentage is the one the box ends up with: 10 % of 515 holds 30.
        List<TextLine> lines = layOut("\tParagraph: " + words(3), "\t\tId: P", "\t\tBox:", "\t\t\tPadding: 0 0 0 10%",
                "\t\tTypography:", "\t\t\tFirst line indent: -30").fragment("P").lines();
        assertEquals(-30, lines.get(0).x(), 1e-9);
        e = assertThrows(InvalidModelException.class, () -> layOut("\tParagraph: " + words(3),
                "\t\tBox:", "\t\t\tPadding: 0 0 0 5%", "\t\tTypography:", "\t\t\tFirst line indent: -30"));
        assertEquals(ErrorCode.FIRST_LINE_INDENT_OVERFLOW, e.errorCode());
    }

    @Test
    void onlyTheFirstLineOfTheParagraphIsIndented() {
        // Not the line after a forced line break.
        List<TextLine> forced = layOut("\tText:", "\t\tId: T", "\t\tTypography:", "\t\t\tFirst line indent: 30",
                "\t\t\tMarkup: markdown", "\t\tContent >>", "\t\t\taaaa\\", "\t\t\taaaa").fragment("T").lines();
        assertEquals(2, forced.size());
        assertEquals(30, forced.get(0).x(), 1e-9);
        assertEquals(0, forced.get(1).x(), 1e-9);
        // Nor the first one of the fragment that goes on in the next page: a page area of 100 holds 8 lines.
        LayoutFixture f = layOutPaged(180, "\tParagraph: " + words(250), "\t\tId: P", "\t\tTypography:",
                "\t\t\tFirst line indent: 30");
        assertEquals(2, f.pages());
        assertEquals(30, f.fragment(1, "P").lines().get(0).x(), 1e-9);
        assertEquals(8, f.fragment(1, "P").lines().size());
        TextLine next = f.fragment(2, "P").lines().get(0);
        assertEquals(0, next.x(), 1e-9);
        assertEquals(0, next.left(), 1e-9);
        assertEquals(20, count(next));
    }

    @Test
    void besideASideBoxTheIndentIsAddedToItsRoom() {
        LayoutFixture f = layOut("\tImage: test/square.png", "\t\tSide: left", "\t\tBox:", "\t\t\tWidth: 60",
                "\t\t\tHeight: 60", "\t\t\tMargin: 0 10 0 0",
                "\tParagraph: " + words(60), "\t\tId: P", "\t\tTypography:", "\t\t\tFirst line indent: 20");
        List<TextLine> lines = f.fragment("P").lines();
        assertEquals(90, lines.get(0).left(), 1e-9);
        assertEquals(90, lines.get(0).x(), 1e-9);
        // 515 − 90 = 425: 17 words; beside the box alone, 445: 18.
        assertEquals(17, count(lines.get(0)));
        assertEquals(70, lines.get(1).left(), 1e-9);
        assertEquals(18, count(lines.get(1)));
    }

    @Test
    void besideASideBoxTheRestOfTheTextIsBrokenAgainWithoutTheIndent() {
        // A page area of 100: a spacer of 40 and the side box, 60 tall; the paragraph has 5 lines beside it (17 and
        // 4 × 18 words) and the rest, 61 words, is broken again on the next page at the whole width, not indented.
        LayoutFixture f = layOutPaged(180, "\tBlock:", "\t\tBox:", "\t\t\tHeight: 40",
                "\tImage: test/square.png", "\t\tSide: left", "\t\tBox:", "\t\t\tWidth: 60",
                "\t\t\tHeight: 60", "\t\t\tMargin: 0 10 0 0",
                "\tParagraph: " + words(150), "\t\tId: P", "\t\tTypography:", "\t\t\tFirst line indent: 20");
        assertEquals(5, f.fragment(1, "P").lines().size());
        assertEquals(90, f.fragment(1, "P").lines().get(0).x(), 1e-9);
        List<TextLine> next = f.fragment(2, "P").lines();
        assertEquals(0, next.get(0).x(), 1e-9);
        assertEquals(List.of(20, 20, 20, 1), next.stream().map(FirstLineIndentLayoutTest::count).toList());
    }

    @Test
    void aPreformattedTextIndentsItsFirstLine() {
        List<TextLine> lines = layOut("\tText:", "\t\tId: T", "\t\tTypography:", "\t\t\tPreformatted: true",
                "\t\t\tFirst line indent: 20", "\t\tContent >>", "\t\t\tab", "\t\t\tcd").fragment("T").lines();
        assertEquals(List.of("ab", "cd"), texts(lines));
        assertEquals(20, lines.get(0).x(), 1e-9);
        assertEquals(0, lines.get(1).x(), 1e-9);
        // With wrap, the first line is broken at what the indent leaves: 70 − 30 = 40, one word; then two.
        List<TextLine> wrap = layOut("\tText: " + words(3), "\t\tId: T", "\t\tBox:", "\t\t\tWidth: 70",
                "\t\tTypography:", "\t\t\tPreformatted: wrap", "\t\t\tFirst line indent: 30").fragment("T").lines();
        assertEquals(List.of("aaaa", "aaaa aaaa"), texts(wrap));
        assertEquals(30, wrap.get(0).x(), 1e-9);
    }

    @Test
    void theIntrinsicWidthsCountTheIndent() {
        // "aa aaaa": the natural width is 35 plus the indent; the narrowest, the greater of "aaaa" (20) and "aa"
        // plus the indent (10 + 15).
        LayoutFixture f = layOut("\tText: aa aaaa", "\t\tId: N", "\t\tBox:", "\t\t\tWidth: natural",
                "\t\tTypography:", "\t\t\tFirst line indent: 15",
                "\tText: aa aaaa", "\t\tId: M", "\t\tBox:", "\t\t\tWidth: narrowest",
                "\t\tTypography:", "\t\t\tFirst line indent: 15");
        assertEquals(50, f.rect("N").width(), 1e-9);
        assertEquals(1, f.fragment("N").lines().size());
        assertEquals(25, f.rect("M").width(), 1e-9);
        assertEquals(List.of("aa", "aaaa"), texts(f.fragment("M").lines()));
    }

    @Test
    void anIndentThatLeavesNoRoomForTheFirstWordOverflows() {
        // The word (20) fits in a line of the box (30), but not after the indent: it does not move to the next line.
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> layOut("\tText: aaaa aaaa",
                "\t\tBox:", "\t\t\tWidth: 30", "\t\tTypography:", "\t\t\tFirst line indent: 15"));
        assertEquals(ErrorCode.TEXT_OVERFLOW, e.errorCode());
    }

    @Test
    void withADropCapItIsAnError() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> layOut("\tParagraph: " + words(40),
                "\t\tDrop cap: 2", "\t\tTypography:", "\t\t\tFirst line indent: 20"));
        assertEquals(ErrorCode.FIRST_LINE_INDENT_WITH_DROP_CAP, e.errorCode());
        // Inherited too; the paragraph with the drop cap takes it back with 0.
        e = assertThrows(InvalidModelException.class, () -> layOut("\tBlock:", "\t\tTypography:",
                "\t\t\tFirst line indent: 20", "\t\tParagraph: " + words(40), "\t\t\tDrop cap: 2"));
        assertEquals(ErrorCode.FIRST_LINE_INDENT_WITH_DROP_CAP, e.errorCode());
        LayoutFixture f = layOut("\tBlock:", "\t\tTypography:", "\t\t\tFirst line indent: 20",
                "\t\tParagraph: " + words(40), "\t\t\tId: P", "\t\t\tDrop cap: 2", "\t\t\tTypography:",
                "\t\t\t\tFirst line indent: 0");
        assertEquals(0, f.fragment("P").lines().get(0).x(), 1e-9);
    }
}
