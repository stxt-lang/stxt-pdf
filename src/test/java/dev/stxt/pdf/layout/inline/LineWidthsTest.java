package dev.stxt.pdf.layout.inline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.fragment.TextLine;
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
