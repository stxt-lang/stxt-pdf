package dev.stxt.pdf.layout.inline;

import static dev.stxt.pdf.tree.TextRuns.run;
import static dev.stxt.pdf.tree.TextRuns.spaced;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.layout.IntrinsicSizes;
import dev.stxt.pdf.layout.inline.hyphenation.Hyphenator;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.LineBreaking;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;

class LineBreakerTest {

    // Factor 1 and size 1: each character measures 1 pt; a space too.
    private final TextMeasurer measurer = new FixedMetrics(1);
    private static final FontFace F = new FontFace("F", FontWeight.NORMAL, FontStyle.NORMAL);
    /** Like {@link #measurer}, but each bold character measures 2 pt. */
    private final TextMeasurer boldIsWider = (text, font, fontSize) ->
            text.codePointCount(0, text.length()) * fontSize * (font.weight().equals(FontWeight.BOLD) ? 2 : 1);

    private static List<String> texts(List<LineBreaker.Line> lines) {
        return lines.stream().map(LineBreaker.Line::text).toList();
    }

    @Test
    void greedyFirstFit() {
        List<String> l = Lines.breakLines("uno dos tres cuatro", 8, measurer, F, 1);
        assertEquals(List.of("uno dos", "tres", "cuatro"), l);
    }

    @Test
    void prettyMinimizesTheDemerits() {
        // Width 10, spaces of 1 that stretch up to 0.5. The greedy algorithm leaves 3 left over in the
        // first two lines: badness 100 × (3/1)³ = 2700 and the maximum (3 / 0.5 = 6).
        // Knuth-Plass prefers one line with the maximum and another with 100 × (1/1)³ = 100.
        String text = "a bbb c ddddd e fff";
        assertEquals(List.of("a bbb c", "ddddd e", "fff"),
                Lines.breakLines(text, 10, measurer, F, 1, LineBreaking.SIMPLE));
        assertEquals(List.of("a bbb", "c ddddd e", "fff"),
                Lines.breakLines(text, 10, measurer, F, 1, LineBreaking.OPTIMAL));
    }

    @Test
    void prettyKeepsOverflowingWords() {
        assertEquals(List.of("a", "larguísima", "b"),
                Lines.breakLines("a larguísima b", 4, measurer, F, 1, LineBreaking.OPTIMAL));
        assertEquals(List.of("uno dos tres"),
                Lines.breakLines("uno dos tres", 100, measurer, F, 1, LineBreaking.OPTIMAL));
    }

    @Test
    void lineBreaksAreSpaces() {
        // A text is a single paragraph: line breaks count as a space (a blank line is an error when reading it).
        assertEquals(List.of("uno dos tres"), Lines.breakLines("uno\ndos\n\ntres", 100, measurer, F, 1));
    }

    // Forced line breaks (reference/components/markdown.md): the line separator a Markdown line break becomes.

    private static final String BR = String.valueOf(TextRun.LINE_SEPARATOR);

    @Test
    void aLineSeparatorEndsTheLineInBothAlgorithms() {
        // "uno dos" would share its line with "tres" at width 12; the separator ends it.
        String text = "uno dos" + BR + "tres cuatro cinco";
        for (LineBreaking wrap : LineBreaking.values()) {
            assertEquals(List.of("uno dos", "tres cuatro", "cinco"),
                    Lines.breakLines(text, 12, measurer, F, 1, wrap), wrap.toString());
        }
    }

    @Test
    void theLineASeparatorEndsIsMarked() {
        for (LineBreaking wrap : LineBreaking.values()) {
            List<LineBreaker.Line> lines = Lines.breakLines(List.of(run("uno " + BR + " dos" + BR + "tres")),
                    100, measurer, F, 1, wrap);
            assertEquals(List.of("uno", "dos", "tres"), texts(lines), wrap.toString());
            assertEquals(List.of(true, true, false), lines.stream().map(LineBreaker.Line::forced).toList());
        }
    }

    @Test
    void aSeparatorAtTheEdgesOfTheTextBreaksNothing() {
        // Two together end a single line: the Markdown does not let them through.
        List<LineBreaker.Line> lines = Lines.breakLines(List.of(run(BR + "uno" + BR + BR + "dos" + BR)), 100,
                measurer, F, 1, LineBreaking.SIMPLE);
        assertEquals(List.of("uno", "dos"), texts(lines));
        assertEquals(List.of(true, false), lines.stream().map(LineBreaker.Line::forced).toList());
    }

    @Test
    void theNaturalWidthOfATextWithSeparatorsIsItsWidestLine() {
        IntrinsicSizes s = Lines.intrinsics("uno dos" + BR + "tres", measurer, F, 1);
        assertEquals(4, s.narrowest(), 1e-9);
        assertEquals(7, s.natural(), 1e-9);
    }

    @Test
    void emptyTextHasNoLines() {
        assertEquals(List.of(), Lines.breakLines(" \n ", 100, measurer, F, 1));
        assertEquals(IntrinsicSizes.ZERO, Lines.intrinsics(" ", measurer, F, 1));
    }

    @Test
    void wordWiderThanLineOverflows() {
        assertEquals(List.of("a", "larguísima", "b"), Lines.breakLines("a larguísima b", 4, measurer, F, 1));
    }

    @Test
    void intrinsics() {
        IntrinsicSizes s = Lines.intrinsics("uno dos tres", measurer, F, 1);
        assertEquals(4, s.narrowest(), 1e-9);
        assertEquals(12, s.natural(), 1e-9);
        assertEquals(8, s.fit(8), 1e-9);
    }

    @Test
    void aWordCanHavePartsOfSeveralRuns() {
        // "uno ne**gri**ta dos": negrita is a single word of three parts.
        List<TextRun> runs = List.of(run("uno ne"), run("gri", "b"), run("ta dos"));
        List<Words.Word> words = Lines.words(runs);
        assertEquals(3, words.size());
        assertEquals(List.of(run("ne"), run("gri", "b"), run("ta")), words.get(1).parts());
        // Each part with its font: ne (2) + gri (6) + ta (2).
        IntrinsicSizes s = Lines.intrinsics(runs, boldIsWider, F, 1);
        assertEquals(10, s.narrowest(), 1e-9);
        assertEquals(3 + 1 + 10 + 1 + 3, s.natural(), 1e-9);
    }

    @Test
    void greedyMeasuresEachRunWithItsFont() {
        // Without formatting, "aa bb" (5) fits in 6; with "bb cc" in bold, "aa bb" measures 7.
        List<TextRun> runs = List.of(run("aa "), run("bb cc", "b"));
        assertEquals(List.of("aa bb", "cc"),
                texts(Lines.breakLines(runs, 6, measurer, F, 1, LineBreaking.SIMPLE)));
        List<LineBreaker.Line> lines = Lines.breakLines(runs, 6, boldIsWider, F, 1, LineBreaking.SIMPLE);
        assertEquals(List.of("aa", "bb", "cc"), texts(lines));
        assertEquals(List.of(run("bb", "b")), lines.get(1).runs());
    }

    @Test
    void prettyMeasuresEachRunWithItsFont() {
        // "a bb c": with bb in bold, the word measures 4 and does not fit with the others in 6.
        List<TextRun> runs = List.of(run("a "), run("bb", "b"), run(" c"));
        assertEquals(List.of("a bb c"),
                texts(Lines.breakLines(runs, 6, measurer, F, 1, LineBreaking.OPTIMAL)));
        assertEquals(2, Lines.breakLines(runs, 6, boldIsWider, F, 1, LineBreaking.OPTIMAL).size());
    }

    @Test
    void theSpaceTakesTheFormatOfItsFirstWhitespace() {
        TextFormat underline = new TextFormat(false, false, true, false);
        // Inside an underlined run, the space is underlined too.
        assertEquals(List.of(new TextRun("a b", underline)),
                Lines.breakLines(List.of(run("a b", "u")), 100, measurer, F, 1, LineBreaking.SIMPLE)
                        .get(0).runs());
        // Between two underlined runs separated by an unformatted space, it is not.
        assertEquals(List.of(run("a", "u"), run(" "), run("b c", "u")),
                Lines.breakLines(List.of(run("a", "u"), run(" "), run("b c", "u")), 100, measurer, F, 1,
                        LineBreaking.SIMPLE).get(0).runs());
        // Consecutive spaces count as one, with the formatting of the first.
        assertEquals(List.of(run("a ", "u"), run("b")),
                Lines.breakLines(List.of(run("a  ", "u"), run("\n b")), 100, measurer, F, 1,
                        LineBreaking.SIMPLE).get(0).runs());
    }

    // --- Letter spacing and small caps (reference/common/typography.md)
    // ----------------------------------------------------

    @Test
    void letterSpacingAddsToEveryCharacterButTheLastOfTheLine() {
        // "uno dos" measures 7; with a letter spacing of 1, 7 + 6 = 13.
        assertEquals(13, LinePlacer.width(List.of(run("uno dos")), measurer, F, 1, 1), 1e-9);
        assertEquals(7, LinePlacer.width(List.of(run("uno dos")), measurer, F, 1, 0), 1e-9);
        assertEquals(new IntrinsicSizes(5, 13),
                Lines.intrinsics(List.of(run("uno dos")), measurer, F, 1, 1));
    }

    @Test
    void letterSpacingChangesTheBreaks() {
        // Width 8: "uno dos" fits (7), but with letter spacing 1 it measures 13.
        assertEquals(List.of("uno dos"),
                texts(Lines.breakLines(List.of(run("uno dos")), 8, measurer, F, 1, 0, LineBreaking.SIMPLE)));
        assertEquals(List.of("uno", "dos"),
                texts(Lines.breakLines(List.of(run("uno dos")), 8, measurer, F, 1, 1, LineBreaking.SIMPLE)));
    }

    @Test
    void letterSpacingWorksWithKnuthPlass() {
        // With letter spacing 1 and width 9, two words of two letters fit (2+1 + 1+1 + 2... = 8) and not three.
        List<LineBreaker.Line> l = Lines.breakLines(List.of(run("ab cd ef gh")), 9, measurer, F, 1, 1,
                LineBreaking.OPTIMAL);
        assertEquals(List.of("ab cd", "ef gh"), texts(l));
    }

    @Test
    void aStyledRunCarriesItsOwnLetterSpacing() {
        // "ab" without spacing and "cd" with 2: 2 + (1 + 2) + (1 + 2), less the 2 of the last character.
        assertEquals(6, LinePlacer.width(List.of(run("ab"), spaced("cd", 2)), measurer, F, 1, 0), 1e-9);
        // The last character is of the box, without spacing: nothing is taken off.
        assertEquals(8, LinePlacer.width(List.of(spaced("ab", 2), run("cd")), measurer, F, 1, 0), 1e-9);
        // A styled run with 0 in a box with 1: "ab" carries 1 after each letter, "cd" none.
        assertEquals(6, LinePlacer.width(List.of(run("ab"), spaced("cd", 0)), measurer, F, 1, 1), 1e-9);
        assertEquals(new IntrinsicSizes(6, 6),
                Lines.intrinsics(List.of(run("ab"), spaced("cd", 2)), measurer, F, 1, 0));
    }

    @Test
    void theLetterSpacingOfARunChangesTheBreaks() {
        // Width 7: "ab cd" with the 2 of "cd" measures 2 + 1 + 3 + 3 - 2 = 7, and fits.
        assertEquals(List.of("ab cd"), texts(Lines.breakLines(List.of(run("ab "), spaced("cd", 2)), 7,
                measurer, F, 1, 0, LineBreaking.SIMPLE)));
        // Width 9: "ab cd ef" measures 8 without spacing, and 16 with the 2 of "cd ef", whose space carries it too.
        assertEquals(List.of("ab cd ef"), texts(Lines.breakLines(List.of(run("ab "), spaced("cd ef", 0)), 9,
                measurer, F, 1, 0, LineBreaking.OPTIMAL)));
        assertEquals(List.of("ab cd", "ef"), texts(Lines.breakLines(List.of(run("ab "), spaced("cd ef", 2)), 9,
                measurer, F, 1, 0, LineBreaking.OPTIMAL)));
    }

    // --- Hyphenation (reference/common/typography.md) ---------------------------------------------------------------

    /** A hyphenator with exceptions only: the words it knows are split exactly there, the rest nowhere. */
    private static final Hyphenator HYPHENATOR = new Hyphenator(List.of(),
            List.of("pro-gra-ma-ción", "ex-tra-or-di-na-ria"), 2, 2);
    private static final String TEXT = "la programación extraordinaria";

    @Test
    void greedySplitsTheWordThatDoesNotFitAtItsLongestFittingPart() {
        // Width 14: "la programación" is 15. The longest part of programación that fits after "la " with its hyphen
        // is "programa-" (12); the rest, "ción", starts the next line, where "extraordinaria" does not fit after it
        // and is split at "extraor-" (13).
        assertEquals(List.of("la programa-", "ción extraor-", "dinaria"),
                Lines.breakLines(TEXT, 14, measurer, F, 1, LineBreaking.SIMPLE, HYPHENATOR));
        // Without a hyphenator, the words stay whole.
        assertEquals(List.of("la", "programación", "extraordinaria"),
                Lines.breakLines(TEXT, 14, measurer, F, 1, LineBreaking.SIMPLE));
    }

    @Test
    void aWordWiderThanTheLineIsSplitInAsManyPartsAsNeeded() {
        // Width 7: "extraor-" (8) does not fit and "extra-" (6) does; of the rest, "ordina-" (7).
        assertEquals(List.of("extra-", "ordina-", "ria"),
                Lines.breakLines("extraordinaria", 7, measurer, F, 1, LineBreaking.SIMPLE, HYPHENATOR));
    }

    @Test
    void aWordThatDoesNotFitEvenSplitOverflowsWhole() {
        // Width 2: no part fits with its hyphen ("ex-" is 3): the word takes a line of its own and overflows.
        assertEquals(List.of("la", "extraordinaria"),
                Lines.breakLines("la extraordinaria", 2, measurer, F, 1, LineBreaking.SIMPLE, HYPHENATOR));
    }

    @Test
    void balancedChoosesAmongTheSamePositions() {
        // Width 14. Every other choice leaves a line of one word that does not fill the width (maximum badness) or
        // stretches a single space beyond its half ("la progra-" leaves 4, "ción extra-" leaves 3: maximum too), so
        // the hyphen penalties do not change the breaks of the greedy algorithm here.
        assertEquals(List.of("la programa-", "ción extraor-", "dinaria"),
                Lines.breakLines(TEXT, 14, measurer, F, 1, LineBreaking.OPTIMAL, HYPHENATOR));
        // Width 9: "aaaa bbbb" fills the line exactly, and "aaaa bb-" would leave 1 to stretch and pay the hyphen.
        Hyphenator h = new Hyphenator(List.of(), List.of("bb-bb"), 2, 2);
        assertEquals(List.of("aaaa bbbb", "cccc"),
                Lines.breakLines("aaaa bbbb cccc", 9, measurer, F, 1, LineBreaking.OPTIMAL, h));
    }

    @Test
    void hyphenatedLinesAreMarkedAndEndWithTheHyphen() {
        List<LineBreaker.Line> lines = Lines.breakLines(TextRun.plain(TEXT), 14, measurer, F, 1, 0,
                LineBreaking.SIMPLE, HYPHENATOR);
        assertEquals(List.of(true, true, false), lines.stream().map(LineBreaker.Line::hyphenated).toList());
        assertEquals(List.of(run("la programa-")), lines.get(0).runs());
    }

    @Test
    void softHyphensAreBreakPointsAndAreNeverMeasured() {
        // Without a hyphenator, the soft hyphen is the only place where the word may split.
        String text = "ultra\u00ADmoderno";
        assertEquals(List.of("ultramoderno"), Lines.breakLines(text, 12, measurer, F, 1));
        assertEquals(List.of("ultra-", "moderno"), Lines.breakLines(text, 11, measurer, F, 1));
        assertEquals(12, Lines.intrinsics(text, measurer, F, 1).natural(), 1e-9);
        // The narrowest width counts the parts, like with the patterns: "moderno" (7) is wider than "ultra-" (6).
        assertEquals(7, Lines.intrinsics(text, measurer, F, 1).narrowest(), 1e-9);
    }

    @Test
    void aWordWithSoftHyphensIsSplitOnlyThere() {
        // The author decided: "extra\u00ADordinaria" splits at "extra-", and the patterns, which would fill the line
        // with "extraordi-" (13), do not add their break points.
        assertEquals(List.of("la extra-", "ordinaria"), Lines.breakLines("la extra\u00ADordinaria", 13, measurer,
                F, 1, LineBreaking.SIMPLE, HYPHENATOR));
        assertEquals(List.of("la extraordi-", "naria"), Lines.breakLines("la extraordinaria", 13, measurer,
                F, 1, LineBreaking.SIMPLE, HYPHENATOR));
    }

    @Test
    void softHyphensAtTheEdgesOrDoubledAddNoBreakPoint() {
        String text = "\u00ADab\u00AD\u00ADcd\u00AD";
        assertEquals(List.of("abcd"), Lines.breakLines(text, 4, measurer, F, 1));
        assertEquals(List.of("ab-", "cd"), Lines.breakLines(text, 3, measurer, F, 1));
    }

    @Test
    void theHyphenTakesTheFormatOfTheLetterBeforeIt() {
        // Bold characters measure 2. Width 8: "pro-" in bold is 6 + 2 and fits; "progra-" (6 + 3 + 1) does not. Of
        // the rest, "grama-" (6) and "ción".
        List<TextRun> runs = List.of(run("pro", "b"), run("gramación"));
        List<LineBreaker.Line> lines = Lines.breakLines(runs, 8, boldIsWider, F, 1, 0, LineBreaking.SIMPLE,
                HYPHENATOR);
        assertEquals(List.of("pro-", "grama-", "ción"), texts(lines));
        assertEquals(List.of(run("pro-", "b")), lines.get(0).runs());
        // Width 7: not even "pro-" fits, so the word is not split and overflows.
        assertEquals(List.of("programación"), texts(Lines.breakLines(runs, 7, boldIsWider, F, 1, 0,
                LineBreaking.SIMPLE, HYPHENATOR)));
    }

    @Test
    void theNarrowestWidthIsTheWidestPartWithItsHyphen() {
        // Parts of programación with the hyphen: pro- gra- ma- (4, 4, 3) and ción (4); of extraordinaria, ex- tra-
        // or- di- na- (3, 4, 3, 3, 3) and ria (3). Without hyphenation, the widest word: 14.
        assertEquals(4, LineBreaker.intrinsics(TextRun.plain(TEXT), measurer, F, 1, 0, HYPHENATOR).narrowest(), 1e-9);
        assertEquals(30, LineBreaker.intrinsics(TextRun.plain(TEXT), measurer, F, 1, 0, HYPHENATOR).natural(), 1e-9);
        assertEquals(14, LineBreaker.intrinsics(TextRun.plain(TEXT), measurer, F, 1, 0, null).narrowest(), 1e-9);
    }
}
