package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.IntrinsicSizes;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.TextAlign;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.model.LineBreaking;
import dev.stxt.pdf.tree.StyleEditor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.stxt.pdf.tree.TextRuns.run;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LineBreakerTest {

    // Factor 1 and size 1: each character measures 1 pt; a space too.
    private final TextMeasurer measurer = new FixedMetrics(1);
    private static final FontFace F = new FontFace("F", FontWeight.NORMAL, FontStyle.NORMAL);
    /** Like {@link #measurer}, but each bold character measures 2 pt. */
    private final TextMeasurer boldIsWider = (text, font, fontSize) ->
            text.codePointCount(0, text.length()) * fontSize * (font.weight() == FontWeight.BOLD ? 2 : 1);

    private static List<String> texts(List<LineBreaker.Line> lines) {
        return lines.stream().map(LineBreaker.Line::text).toList();
    }

    @Test
    void greedyFirstFit() {
        List<String> l = LineBreaker.breakLines("uno dos tres cuatro", 8, measurer, F, 1);
        assertEquals(List.of("uno dos", "tres", "cuatro"), l);
    }

    @Test
    void prettyMinimizesTheDemerits() {
        // Width 10, spaces of 1 that stretch up to 0.5. The greedy algorithm leaves 3 left over in the
        // first two lines: badness 100 × (3/1)³ = 2700 and the maximum (3 / 0.5 = 6).
        // Knuth-Plass prefers one line with the maximum and another with 100 × (1/1)³ = 100.
        String text = "a bbb c ddddd e fff";
        assertEquals(List.of("a bbb c", "ddddd e", "fff"),
                LineBreaker.breakLines(text, 10, measurer, F, 1, LineBreaking.SIMPLE));
        assertEquals(List.of("a bbb", "c ddddd e", "fff"),
                LineBreaker.breakLines(text, 10, measurer, F, 1, LineBreaking.BALANCED));
    }

    @Test
    void prettyKeepsOverflowingWords() {
        assertEquals(List.of("a", "larguísima", "b"),
                LineBreaker.breakLines("a larguísima b", 4, measurer, F, 1, LineBreaking.BALANCED));
        assertEquals(List.of("uno dos tres"),
                LineBreaker.breakLines("uno dos tres", 100, measurer, F, 1, LineBreaking.BALANCED));
    }

    @Test
    void lineBreaksAreSpaces() {
        // A text is a single paragraph: line breaks count as a space (a blank line is an error when reading it).
        assertEquals(List.of("uno dos tres"), LineBreaker.breakLines("uno\ndos\n\ntres", 100, measurer, F, 1));
    }

    @Test
    void emptyTextHasNoLines() {
        assertEquals(List.of(), LineBreaker.breakLines(" \n ", 100, measurer, F, 1));
        assertEquals(IntrinsicSizes.ZERO, LineBreaker.intrinsics(" ", measurer, F, 1));
    }

    @Test
    void wordWiderThanLineOverflows() {
        assertEquals(List.of("a", "larguísima", "b"), LineBreaker.breakLines("a larguísima b", 4, measurer, F, 1));
    }

    @Test
    void intrinsics() {
        IntrinsicSizes s = LineBreaker.intrinsics("uno dos tres", measurer, F, 1);
        assertEquals(4, s.narrowest(), 1e-9);
        assertEquals(12, s.natural(), 1e-9);
        assertEquals(8, s.fit(8), 1e-9);
    }

    @Test
    void alignment() {
        assertEquals(15, LineBreaker.offset(70, 100, TextAlign.CENTER), 1e-9);
        assertEquals(30, LineBreaker.offset(70, 100, TextAlign.RIGHT), 1e-9);
        assertEquals(0, LineBreaker.offset(70, 100, TextAlign.JUSTIFY), 1e-9);
    }

    @Test
    void justifySpreadsTheRemainderBetweenSpaces() {
        assertEquals(10, LineBreaker.wordSpacing("uno dos tres", 70, 90, TextAlign.JUSTIFY, false), 1e-9);
    }

    @Test
    void justifyLeavesSomeLinesAlone() {
        // Last line of the text, a single word, a line that overflows and another alignment.
        assertEquals(0, LineBreaker.wordSpacing("uno dos", 70, 90, TextAlign.JUSTIFY, true), 1e-9);
        assertEquals(0, LineBreaker.wordSpacing("palabra", 70, 90, TextAlign.JUSTIFY, false), 1e-9);
        assertEquals(0, LineBreaker.wordSpacing("uno dos", 100, 90, TextAlign.JUSTIFY, false), 1e-9);
        assertEquals(0, LineBreaker.wordSpacing("uno dos", 70, 90, TextAlign.LEFT, false), 1e-9);
    }

    @Test
    void placeMeasuresAlignsAndJustifies() {
        StyleEditor s = new StyleEditor();
        s.font("F");
        s.fontSize(1);
        s.textAlign(TextAlign.JUSTIFY);
        // "uno dos tres" measures 12 in 20: 8 left over between 2 spaces, 4 per space.
        TextLine l = LineBreaker.place("uno dos tres", measurer, s.style(), 20, true, 30, 12);
        assertEquals(new TextLine(0, 30, 20, 12, "uno dos tres", 4), l);
        // The last line of the text is not justified.
        assertEquals(new TextLine(0, 30, 12, 12, "uno dos tres", 0),
                LineBreaker.place("uno dos tres", measurer, s.style(), 20, false, 30, 12));
        s.textAlign(TextAlign.CENTER);
        assertEquals(new TextLine(4, 0, 12, 12, "uno dos tres", 0),
                LineBreaker.place("uno dos tres", measurer, s.style(), 20, true, 0, 12));
    }

    @Test
    void aWordCanHavePartsOfSeveralRuns() {
        // "uno ne**gri**ta dos": negrita is a single word of three parts.
        List<TextRun> runs = List.of(run("uno ne"), run("gri", "b"), run("ta dos"));
        List<LineBreaker.Word> words = LineBreaker.words(runs);
        assertEquals(3, words.size());
        assertEquals(List.of(run("ne"), run("gri", "b"), run("ta")), words.get(1).parts());
        // Each part with its font: ne (2) + gri (6) + ta (2).
        IntrinsicSizes s = LineBreaker.intrinsics(runs, boldIsWider, F, 1);
        assertEquals(10, s.narrowest(), 1e-9);
        assertEquals(3 + 1 + 10 + 1 + 3, s.natural(), 1e-9);
    }

    @Test
    void greedyMeasuresEachRunWithItsFont() {
        // Without formatting, "aa bb" (5) fits in 6; with "bb cc" in bold, "aa bb" measures 7.
        List<TextRun> runs = List.of(run("aa "), run("bb cc", "b"));
        assertEquals(List.of("aa bb", "cc"),
                texts(LineBreaker.breakLines(runs, 6, measurer, F, 1, LineBreaking.SIMPLE)));
        List<LineBreaker.Line> lines = LineBreaker.breakLines(runs, 6, boldIsWider, F, 1, LineBreaking.SIMPLE);
        assertEquals(List.of("aa", "bb", "cc"), texts(lines));
        assertEquals(List.of(run("bb", "b")), lines.get(1).runs());
    }

    @Test
    void prettyMeasuresEachRunWithItsFont() {
        // "a bb c": with bb in bold, the word measures 4 and does not fit with the others in 6.
        List<TextRun> runs = List.of(run("a "), run("bb", "b"), run(" c"));
        assertEquals(List.of("a bb c"),
                texts(LineBreaker.breakLines(runs, 6, measurer, F, 1, LineBreaking.BALANCED)));
        assertEquals(2, LineBreaker.breakLines(runs, 6, boldIsWider, F, 1, LineBreaking.BALANCED).size());
    }

    @Test
    void theSpaceTakesTheFormatOfItsFirstWhitespace() {
        TextFormat underline = new TextFormat(false, false, true, false);
        // Inside an underlined run, the space is underlined too.
        assertEquals(List.of(new TextRun("a b", underline)),
                LineBreaker.breakLines(List.of(run("a b", "u")), 100, measurer, F, 1, LineBreaking.SIMPLE)
                        .get(0).runs());
        // Between two underlined runs separated by an unformatted space, it is not.
        assertEquals(List.of(run("a", "u"), run(" "), run("b c", "u")),
                LineBreaker.breakLines(List.of(run("a", "u"), run(" "), run("b c", "u")), 100, measurer, F, 1,
                        LineBreaking.SIMPLE).get(0).runs());
        // Consecutive spaces count as one, with the formatting of the first.
        assertEquals(List.of(run("a ", "u"), run("b")),
                LineBreaker.breakLines(List.of(run("a  ", "u"), run("\n b")), 100, measurer, F, 1,
                        LineBreaking.SIMPLE).get(0).runs());
    }

    @Test
    void placePositionsEachRunAndSpreadsTheJustification() {
        StyleEditor s = new StyleEditor();
        s.font("F");
        s.fontSize(1);
        s.textAlign(TextAlign.JUSTIFY);
        // "uno " (4) + "dos" in bold (6) + " tres" (5) = 15 in 20: 2.5 more per space.
        TextLine l = LineBreaker.place(List.of(run("uno "), run("dos", "b"), run(" tres")), boldIsWider, s.style(), 20,
                true, 0, 12);
        assertEquals(List.of(new TextLine.Run(0, 6.5, "uno ", TextFormat.PLAIN),
                        new TextLine.Run(6.5, 6, "dos", run("dos", "b").format()),
                        new TextLine.Run(12.5, 7.5, " tres", TextFormat.PLAIN)),
                l.runs());
        assertEquals(new TextLine(0, 0, 20, 12, "uno dos tres", 2.5, l.runs()), l);
    }

    // --- Letter spacing and small caps (reference/common/typography.md)
    // ----------------------------------------------------

    @Test
    void letterSpacingAddsToEveryCharacterButTheLastOfTheLine() {
        // "uno dos" measures 7; with a letter spacing of 1, 7 + 6 = 13.
        assertEquals(13, LineBreaker.width(List.of(run("uno dos")), measurer, F, 1, 1), 1e-9);
        assertEquals(7, LineBreaker.width(List.of(run("uno dos")), measurer, F, 1, 0), 1e-9);
        assertEquals(new IntrinsicSizes(5, 13),
                LineBreaker.intrinsics(List.of(run("uno dos")), measurer, F, 1, 1));
    }

    @Test
    void letterSpacingChangesTheBreaks() {
        // Width 8: "uno dos" fits (7), but with letter spacing 1 it measures 13.
        assertEquals(List.of("uno dos"),
                texts(LineBreaker.breakLines(List.of(run("uno dos")), 8, measurer, F, 1, 0, LineBreaking.SIMPLE)));
        assertEquals(List.of("uno", "dos"),
                texts(LineBreaker.breakLines(List.of(run("uno dos")), 8, measurer, F, 1, 1, LineBreaking.SIMPLE)));
    }

    @Test
    void letterSpacingWorksWithKnuthPlass() {
        // With letter spacing 1 and width 9, two words of two letters fit (2+1 + 1+1 + 2... = 8) and not three.
        List<LineBreaker.Line> l = LineBreaker.breakLines(List.of(run("ab cd ef gh")), 9, measurer, F, 1, 1,
                LineBreaking.BALANCED);
        assertEquals(List.of("ab cd", "ef gh"), texts(l));
    }

    @Test
    void smallCapsAreMeasuredAtTheirScale() {
        TextRun small = new TextRun("BC", TextFormat.PLAIN.withSmallCaps(true));
        // "A" measures 1 and "BC", 2 × 0.8.
        assertEquals(2.6, LineBreaker.width(List.of(run("A"), small), measurer, F, 1, 0), 1e-9);
        // With letter spacing, that of each character is the same, also in small caps.
        assertEquals(2.6 + 2, LineBreaker.width(List.of(run("A"), small), measurer, F, 1, 1), 1e-9);
    }

    @Test
    void aPlacedLineDropsTheLastLetterSpacingAndAlignsWithIt() {
        StyleEditor s = new StyleEditor();
        s.font("F");
        s.fontSize(1);
        s.letterSpacing(2);
        s.textAlign(TextAlign.RIGHT);
        TextLine line = LineBreaker.place("ab", measurer, s.style(), 20, false, 0, 1);
        // a (1) + 2 + b (1) = 4, to the right of 20.
        assertEquals(4, line.width(), 1e-9);
        assertEquals(16, line.x(), 1e-9);
        assertEquals(4, line.runs().get(0).width(), 1e-9);
    }

    @Test
    void justifyDistributesWhatLetterSpacingLeaves() {
        StyleEditor s = new StyleEditor();
        s.font("F");
        s.fontSize(1);
        s.letterSpacing(1);
        s.textAlign(TextAlign.JUSTIFY);
        // "ab cd" measures 5 + 4 = 9; in 15, 6 are left over for a single space.
        TextLine line = LineBreaker.place("ab cd", measurer, s.style(), 15, true, 0, 1);
        assertEquals(6, line.wordSpacing(), 1e-9);
        assertEquals(15, line.width(), 1e-9);
    }
}
