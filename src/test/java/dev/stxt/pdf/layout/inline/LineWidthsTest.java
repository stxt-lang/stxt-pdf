package dev.stxt.pdf.layout.inline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.IntrinsicSizes;
import dev.stxt.pdf.model.Exclusion;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.LineBreaking;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.tree.StyleEditor;

/**
 * Lines of different widths (reference/components/block.md), with {@link FixedMetrics} at size 10: a word of four
 * letters measures 20 and a space, 5.
 */
class LineWidthsTest {

    private static final FontFace F = new FontFace("F", FontWeight.NORMAL, FontStyle.NORMAL);
    private final TextMeasurer measurer = new FixedMetrics();

    private static List<TextRun> words(int n) {
        return TextRun.plain("aaaa ".repeat(n).strip());
    }

    private static List<String> texts(List<LineBreaker.Line> lines) {
        return lines.stream().map(LineBreaker.Line::text).toList();
    }

    private Paragraph paragraph(int words) {
        return Paragraph.of(words(words), measurer, F, 10, 0, null);
    }

    @Test
    void theExclusionsThatOverlapALineTakeTheirRoom() {
        // A band of 30 with lines of 12 overlaps the first three lines.
        LineWidths w = LineWidths.of(100, 12, List.of(new Exclusion(0, 30, 40, 0), new Exclusion(12, 20, 0, 15)));
        assertEquals(3, w.varying());
        assertEquals(40, w.left(0), 1e-9);
        assertEquals(0, w.right(0), 1e-9);
        assertEquals(15, w.right(1), 1e-9);
        assertEquals(45, w.width(1), 1e-9);
        assertEquals(40, w.left(2), 1e-9);
        assertEquals(0, w.left(3), 1e-9);
        assertEquals(100, w.width(3), 1e-9);
        assertTrue(w.varies());
        assertEquals(0, LineWidths.uniform(100).varying());
    }

    @Test
    void theDropCapTakesItsRoomAfterTheExclusions() {
        LineWidths w = LineWidths.of(100, 12, List.of(new Exclusion(0, 24, 40, 0))).withDropCap(3, 10);
        assertEquals(3, w.varying());
        assertEquals(50, w.left(0), 1e-9);
        assertEquals(10, w.left(2), 1e-9);
        assertEquals(0, w.left(3), 1e-9);
    }

    // --- First line indent (reference/common/typography.md) ----------------------------------------------------------

    @Test
    void theFirstLineIndentShortensLineZero() {
        LineWidths w = LineWidths.uniform(100).withFirstLineIndent(20);
        assertEquals(1, w.varying());
        assertEquals(20, w.left(0), 1e-9);
        assertEquals(0, w.right(0), 1e-9);
        assertEquals(80, w.width(0), 1e-9);
        assertEquals(0, w.left(1), 1e-9);
        assertEquals(100, w.width(1), 1e-9);
        assertTrue(w.varies());
        // No side box and no drop cap: the lines after the first are not broken again on another page.
        assertFalse(w.shortened());
        assertEquals(0, LineWidths.uniform(100).withFirstLineIndent(0).varying());
    }

    @Test
    void aNegativeFirstLineIndentStartsLineZeroBeforeTheContentBox() {
        LineWidths w = LineWidths.uniform(100).withFirstLineIndent(-20);
        assertEquals(1, w.varying());
        assertEquals(-20, w.left(0), 1e-9);
        assertEquals(120, w.width(0), 1e-9);
        assertEquals(0, w.left(1), 1e-9);
        assertEquals(100, w.width(1), 1e-9);
    }

    @Test
    void theFirstLineIndentIsAddedToTheRoomOfTheSideBoxes() {
        LineWidths w = LineWidths.of(100, 12, List.of(new Exclusion(0, 24, 40, 0))).withFirstLineIndent(10);
        assertEquals(2, w.varying());
        assertEquals(50, w.left(0), 1e-9);
        assertEquals(50, w.width(0), 1e-9);
        assertEquals(40, w.left(1), 1e-9);
        assertEquals(0, w.left(2), 1e-9);
        assertTrue(w.shortened());
        // A negative one takes from that room.
        assertEquals(25, w.withFirstLineIndent(-15).left(0), 1e-9);
    }

    @Test
    void theIndentedLineIsNotNarrowerThanTheNextOnesForAWordThatDoesNotFit() {
        // Line 1 is wider than line 0 only by the indent: that is not a line a word leaves empty.
        assertFalse(LineWidths.uniform(100).withFirstLineIndent(20).widerLater(0));
        // Beside a side box that ends, it is.
        assertTrue(LineWidths.of(100, 12, List.of(new Exclusion(0, 12, 40, 0))).withFirstLineIndent(20).widerLater(0));
    }

    @Test
    void theTwoLineBreakingsShortenTheIndentedLine() {
        // Lines of 100 hold four words (95); the first, with 80, three (70).
        LineWidths w = LineWidths.uniform(100).withFirstLineIndent(20);
        for (LineBreaking wrap : LineBreaking.values()) {
            List<LineBreaker.Line> lines = LineBreaker.breakLines(paragraph(6), 0, w, wrap);
            assertEquals(List.of("aaaa aaaa aaaa", "aaaa aaaa aaaa"), texts(lines), wrap.toString());
        }
    }

    @Test
    void theTwoLineBreakingsLengthenTheLineOfANegativeIndent() {
        // The first line has 125: five words (120).
        LineWidths w = LineWidths.uniform(100).withFirstLineIndent(-25);
        for (LineBreaking wrap : LineBreaking.values()) {
            List<LineBreaker.Line> lines = LineBreaker.breakLines(paragraph(6), 0, w, wrap);
            assertEquals(List.of("aaaa aaaa aaaa aaaa aaaa", "aaaa"), texts(lines), wrap.toString());
        }
    }

    @Test
    void aWordThatDoesNotFitInTheIndentedLineOverflowsAndDoesNotLeaveItEmpty() {
        // Lines of 30 and the first of 15: the word (20) takes it and overflows.
        LineWidths w = LineWidths.uniform(30).withFirstLineIndent(15);
        for (LineBreaking wrap : LineBreaking.values()) {
            List<LineBreaker.Line> lines = LineBreaker.breakLines(paragraph(2), 0, w, wrap);
            assertEquals(List.of("aaaa", "aaaa"), texts(lines), wrap.toString());
        }
    }

    @Test
    void theIntrinsicSizesCountTheFirstLineIndent() {
        // "aa aaaa": 10, a space of 5 and 20.
        List<TextRun> runs = TextRun.plain("aa aaaa");
        IntrinsicSizes plain = LineBreaker.intrinsics(runs, measurer, F, 10, 0, null, 0);
        assertEquals(20, plain.narrowest(), 1e-9);
        assertEquals(35, plain.natural(), 1e-9);
        // The natural width adds the indent; the narrowest is the greater of the widest word and the first one
        // plus the indent.
        IntrinsicSizes indented = LineBreaker.intrinsics(runs, measurer, F, 10, 0, null, 15);
        assertEquals(25, indented.narrowest(), 1e-9);
        assertEquals(50, indented.natural(), 1e-9);
        assertEquals(20, LineBreaker.intrinsics(runs, measurer, F, 10, 0, null, 5).narrowest(), 1e-9);
        // A negative one: the first line is shorter, and the box is never narrower than its widest word.
        IntrinsicSizes hanging = LineBreaker.intrinsics(runs, measurer, F, 10, 0, null, -10);
        assertEquals(20, hanging.narrowest(), 1e-9);
        assertEquals(25, hanging.natural(), 1e-9);
        assertEquals(20, LineBreaker.intrinsics(TextRun.plain("aaaa"), measurer, F, 10, 0, null, -10).natural(), 1e-9);
        // With forced line breaks, only the first line has it.
        List<TextRun> forced = TextRun.plain("aa" + TextRun.LINE_SEPARATOR + "aaaa aaaa");
        assertEquals(45, LineBreaker.intrinsics(forced, measurer, F, 10, 0, null, 15).natural(), 1e-9);
        assertEquals(50, LineBreaker.intrinsics(forced, measurer, F, 10, 0, null, 40).natural(), 1e-9);
    }

    @Test
    void aPreformattedTextIndentsOnlyItsFirstLine() {
        // Two lines of 10 and 20. With true, the lines are the ones written, and the first counts with its indent.
        List<TextRun> runs = TextRun.plain("ab\ncdef");
        IntrinsicSizes pre = PreformattedText.intrinsics(runs, 4, false, 15, measurer, F, 10, 0);
        assertEquals(25, pre.narrowest(), 1e-9);
        assertEquals(25, pre.natural(), 1e-9);
        assertEquals(20, PreformattedText.intrinsics(runs, 4, false, 5, measurer, F, 10, 0).natural(), 1e-9);
        // With wrap, the first word of the first line.
        IntrinsicSizes wrap = PreformattedText.intrinsics(TextRun.plain("ab cdef\nab"), 4, true, 15, measurer, F, 10, 0);
        assertEquals(25, wrap.narrowest(), 1e-9);
        assertEquals(50, wrap.natural(), 1e-9);
        // Three words in lines of 70 hold in one (70); with an indent of 30, the first line holds one.
        List<TextRun> text = words(3);
        assertEquals(List.of("aaaa aaaa aaaa"), texts(PreformattedText.lines(text, 4, true, 70, 0, measurer, F, 10, 0)));
        assertEquals(List.of("aaaa", "aaaa aaaa"),
                texts(PreformattedText.lines(text, 4, true, 70, 30, measurer, F, 10, 0)));
        assertEquals(List.of("aaaa aaaa aaaa"), texts(PreformattedText.lines(text, 4, false, 70, 30, measurer, F, 10, 0)));
    }

    @Test
    void greedyUsesTheWidthOfEachLine() {
        // Lines 0 and 1 have 40 (one word); from line 2 on, 100 (four words).
        LineWidths w = LineWidths.of(100, 12, List.of(new Exclusion(0, 24, 60, 0)));
        List<LineBreaker.Line> lines = LineBreaker.breakLines(paragraph(6), 0, w, LineBreaking.SIMPLE);
        assertEquals(List.of("aaaa", "aaaa", "aaaa aaaa aaaa aaaa"), texts(lines));
        assertEquals(List.of(0, 1, 2), lines.stream().map(LineBreaker.Line::start).toList());
    }

    @Test
    void knuthPlassUsesTheWidthOfEachLine() {
        LineWidths w = LineWidths.of(100, 12, List.of(new Exclusion(0, 24, 60, 0)));
        List<LineBreaker.Line> lines = LineBreaker.breakLines(paragraph(6), 0, w, LineBreaking.OPTIMAL);
        assertEquals(List.of("aaaa", "aaaa", "aaaa aaaa aaaa aaaa"), texts(lines));
    }

    @Test
    void aWordThatDoesNotFitLeavesTheNarrowLinesEmpty() {
        // Lines 0 and 1 have 10: no word fits, and they stay empty.
        LineWidths w = LineWidths.of(100, 12, List.of(new Exclusion(0, 24, 90, 0)));
        for (LineBreaking wrap : LineBreaking.values()) {
            List<LineBreaker.Line> lines = LineBreaker.breakLines(paragraph(6), 0, w, wrap);
            assertEquals(List.of("", "", "aaaa aaaa aaaa aaaa", "aaaa aaaa"), texts(lines), wrap.toString());
            assertTrue(lines.get(0).isEmpty());
            assertEquals(0, lines.get(1).start());
            assertEquals(4, lines.get(3).start());
        }
    }

    @Test
    void theTextCanBeBrokenAgainFromAPosition() {
        List<LineBreaker.Line> lines = LineBreaker.breakLines(paragraph(6), 2, LineWidths.uniform(100),
                LineBreaking.SIMPLE);
        assertEquals(List.of("aaaa aaaa aaaa aaaa"), texts(lines));
        assertEquals(2, lines.get(0).start());
    }

    @Test
    void aLineIsPlacedInItsLineBox() {
        StyleEditor s = new StyleEditor();
        s.font("F");
        s.fontSize(10);
        TextLine l = LinePlacer.place(words(2), measurer, s.style(), 10, 30, 20, 50, false, 0, 12, false, false);
        assertEquals(30, l.x(), 1e-9);
        assertEquals(45, l.width(), 1e-9);
        assertEquals(30, l.left(), 1e-9);
        assertEquals(20, l.right(), 1e-9);
    }
}
