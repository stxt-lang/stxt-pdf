package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.TextAlign;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.tree.StyleEditor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.stxt.pdf.tree.TextRuns.run;
import static dev.stxt.pdf.tree.TextRuns.spaced;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** The geometry of a line (reference/common/text-formatting.md): measures, alignment and justification. */
class LinePlacerTest {

    // Factor 1 and size 1: each character measures 1 pt; a space too.
    private final TextMeasurer measurer = new FixedMetrics(1);
    private static final FontFace F = new FontFace("F", FontWeight.NORMAL, FontStyle.NORMAL);
    /** Like {@link #measurer}, but each bold character measures 2 pt. */
    private final TextMeasurer boldIsWider = (text, font, fontSize) ->
            text.codePointCount(0, text.length()) * fontSize * (font.weight().equals(FontWeight.BOLD) ? 2 : 1);

    @Test
    void alignment() {
        assertEquals(15, LinePlacer.offset(70, 100, TextAlign.CENTER), 1e-9);
        assertEquals(30, LinePlacer.offset(70, 100, TextAlign.RIGHT), 1e-9);
        assertEquals(0, LinePlacer.offset(70, 100, TextAlign.JUSTIFY), 1e-9);
    }

    @Test
    void justifySpreadsTheRemainderBetweenSpaces() {
        assertEquals(10, LinePlacer.wordSpacing("uno dos tres", 70, 90, TextAlign.JUSTIFY, false), 1e-9);
    }

    @Test
    void justifyLeavesSomeLinesAlone() {
        // Last line of the text, a single word, a line that overflows and another alignment.
        assertEquals(0, LinePlacer.wordSpacing("uno dos", 70, 90, TextAlign.JUSTIFY, true), 1e-9);
        assertEquals(0, LinePlacer.wordSpacing("palabra", 70, 90, TextAlign.JUSTIFY, false), 1e-9);
        assertEquals(0, LinePlacer.wordSpacing("uno dos", 100, 90, TextAlign.JUSTIFY, false), 1e-9);
        assertEquals(0, LinePlacer.wordSpacing("uno dos", 70, 90, TextAlign.LEFT, false), 1e-9);
    }

    @Test
    void placeMeasuresAlignsAndJustifies() {
        StyleEditor s = new StyleEditor();
        s.font("F");
        s.fontSize(1);
        s.textAlign(TextAlign.JUSTIFY);
        // "uno dos tres" measures 12 in 20: 8 left over between 2 spaces, 4 per space.
        TextLine l = Lines.place("uno dos tres", measurer, s.style(), 20, true, 30, 12);
        assertEquals(new TextLine(0, 30, 20, 12, 1, "uno dos tres", 4), l);
        // The last line of the text is not justified.
        assertEquals(new TextLine(0, 30, 12, 12, 1, "uno dos tres", 0),
                Lines.place("uno dos tres", measurer, s.style(), 20, false, 30, 12));
        s.textAlign(TextAlign.CENTER);
        assertEquals(new TextLine(4, 0, 12, 12, 1, "uno dos tres", 0),
                Lines.place("uno dos tres", measurer, s.style(), 20, true, 0, 12));
    }

    @Test
    void placePositionsEachRunAndSpreadsTheJustification() {
        StyleEditor s = new StyleEditor();
        s.font("F");
        s.fontSize(1);
        s.textAlign(TextAlign.JUSTIFY);
        // "uno " (4) + "dos" in bold (6) + " tres" (5) = 15 in 20: 2.5 more per space.
        TextLine l = Lines.place(List.of(run("uno "), run("dos", "b"), run(" tres")), boldIsWider, s.style(), 20,
                true, 0, 12);
        assertEquals(List.of(new TextLine.Run(0, 6.5, "uno ", TextFormat.PLAIN),
                        new TextLine.Run(6.5, 6, "dos", run("dos", "b").format()),
                        new TextLine.Run(12.5, 7.5, " tres", TextFormat.PLAIN)),
                l.runs());
        assertEquals(new TextLine(0, 0, 20, 12, 1, "uno dos tres", 2.5, l.runs()), l);
    }

    @Test
    void smallCapsAreMeasuredAtTheirScale() {
        TextRun small = new TextRun("BC", TextFormat.PLAIN.withSmallCaps(true));
        // "A" measures 1 and "BC", 2 × 0.8.
        assertEquals(2.6, LinePlacer.width(List.of(run("A"), small), measurer, F, 1, 0), 1e-9);
        // With letter spacing, that of each character is the same, also in small caps.
        assertEquals(2.6 + 2, LinePlacer.width(List.of(run("A"), small), measurer, F, 1, 1), 1e-9);
    }

    @Test
    void aPlacedLineDropsTheLastLetterSpacingAndAlignsWithIt() {
        StyleEditor s = new StyleEditor();
        s.font("F");
        s.fontSize(1);
        s.letterSpacing(2);
        s.textAlign(TextAlign.RIGHT);
        TextLine line = Lines.place("ab", measurer, s.style(), 20, false, 0, 1);
        // a (1) + 2 + b (1) = 4, to the right of 20.
        assertEquals(4, line.width(), 1e-9);
        assertEquals(16, line.x(), 1e-9);
        assertEquals(4, line.runs().get(0).width(), 1e-9);
    }

    @Test
    void aPlacedLineDropsTheLetterSpacingOfItsLastRun() {
        StyleEditor s = new StyleEditor();
        s.font("F");
        s.fontSize(1);
        s.textAlign(TextAlign.RIGHT);
        TextLine line = Lines.place(List.of(run("ab"), spaced("cd", 2)), measurer, s.style(), 20, false, 0, 1);
        // ab (2) + c (1 + 2) + d (1) = 6, to the right of 20.
        assertEquals(6, line.width(), 1e-9);
        assertEquals(14, line.x(), 1e-9);
        assertEquals(4, line.runs().get(1).width(), 1e-9);
    }

    @Test
    void justifyDistributesWhatLetterSpacingLeaves() {
        StyleEditor s = new StyleEditor();
        s.font("F");
        s.fontSize(1);
        s.letterSpacing(1);
        s.textAlign(TextAlign.JUSTIFY);
        // "ab cd" measures 5 + 4 = 9; in 15, 6 are left over for a single space.
        TextLine line = Lines.place("ab cd", measurer, s.style(), 15, true, 0, 1);
        assertEquals(6, line.wordSpacing(), 1e-9);
        assertEquals(15, line.width(), 1e-9);
    }
}
