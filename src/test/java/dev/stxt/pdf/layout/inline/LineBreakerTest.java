package dev.stxt.pdf.layout.inline;

import static dev.stxt.pdf.tree.TextRuns.run;
import static dev.stxt.pdf.tree.TextRuns.spaced;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.layout.IntrinsicSizes;
import dev.stxt.pdf.layout.inline.hyphenation.Hyphenator;
import dev.stxt.pdf.model.Exclusion;
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
        // Width 10, spaces of 1 that stretch up to 0.5. The greedy algorithm fills the first line and leaves the
        // second with a single word and 5 left over: as a line with one space, badness 100 × (5/0.5)³ = 100 000.
        // Knuth-Plass prefers 100 × (4/1)³ = 6400 in the first and 100 × (1/0.5)³ = 800 in the second.
        String text = "a b cc ddd eeeee fffff";
        assertEquals(List.of("a b cc ddd", "eeeee", "fffff"),
                Lines.breakLines(text, 10, measurer, F, 1, LineBreaking.SIMPLE));
        assertEquals(List.of("a b cc", "ddd eeeee", "fffff"),
                Lines.breakLines(text, 10, measurer, F, 1, LineBreaking.OPTIMAL));
    }

    /**
     * The badness has no ceiling: where every line breaking has very loose lines, the leftover is spread among
     * them. Until the 2026-11-13, with the ceiling of 10 000, the lines past it cost the same and one line took the
     * leftover of two: {@code a bbb} (5 left over, with one space) and a nearly full {@code c ddddd e}.
     */
    @Test
    void prettySpreadsTheLeftoverAmongLooseLines() {
        assertEquals(List.of("a bbb c", "ddddd e", "fff"),
                Lines.breakLines("a bbb c ddddd e fff", 10, measurer, F, 1, LineBreaking.OPTIMAL));
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

    // --- Written hyphens (reference/common/typography.md) -----------------------------------------------------------

    @Test
    void aWordIsSplitAfterAHyphenWrittenBetweenLettersOrDigits() {
        // Width 10: "REF-2026-009812" is 15. The longest part that fits is "REF-2026-" (9), which ends with the
        // hyphen of the text: no other is added.
        for (LineBreaking wrap : LineBreaking.values()) {
            assertEquals(List.of("REF-2026-", "009812"),
                    Lines.breakLines("REF-2026-009812", 10, measurer, F, 1, wrap), wrap.toString());
            assertEquals(List.of("ver socio-", "cultural"),
                    Lines.breakLines("ver socio-cultural", 10, measurer, F, 1, wrap), wrap.toString());
        }
        // Width 5: one part per line.
        assertEquals(List.of("REF-", "2026-", "009812"), Lines.breakLines("REF-2026-009812", 5, measurer, F, 1));
    }

    @Test
    void aLineThatEndsAfterAWrittenHyphenSplitsTheWordButIsNotHyphenated() {
        List<LineBreaker.Line> lines = Lines.breakLines(TextRun.plain("REF-2026-009812 pagado"), 10, measurer, F, 1,
                LineBreaking.SIMPLE);
        assertEquals(List.of("REF-2026-", "009812", "pagado"), texts(lines));
        assertEquals(List.of(false, false, false), lines.stream().map(LineBreaker.Line::hyphenated).toList());
        assertEquals(List.of(true, false, false), lines.stream().map(LineBreaker.Line::splitsWord).toList());
        // A line that ends with an added hyphen splits its word too.
        List<LineBreaker.Line> hyphenated = Lines.breakLines(TextRun.plain(TEXT), 14, measurer, F, 1, 0,
                LineBreaking.SIMPLE, HYPHENATOR);
        assertEquals(List.of(true, true, false), hyphenated.stream().map(LineBreaker.Line::splitsWord).toList());
    }

    @Test
    void onlyAHyphenBetweenLettersOrDigitsIsABreakPoint() {
        // At the start or the end of the word, doubled or beside punctuation, the hyphen is one more character: the
        // word does not fit in width 3 and overflows whole.
        for (String word : List.of("-abcd", "abcd-", "ab--cd", "ab-(cd)", "(ab)-cd")) {
            assertEquals(List.of(word), Lines.breakLines(word, 3, measurer, F, 1), word);
        }
        // The non-breaking hyphen (U+2011) keeps the word whole.
        String nonBreaking = "ab" + Character.toString(0x2011) + "cd";
        assertEquals(List.of(nonBreaking), Lines.breakLines(nonBreaking, 3, measurer, F, 1));
        assertEquals(List.of("ab-", "cd"), Lines.breakLines("ab-cd", 3, measurer, F, 1));
    }

    @Test
    void writtenAndSoftHyphensAreBothBreakPoints() {
        // "ultra(soft)fino-mate": at the soft hyphen the line breaking adds a hyphen; after the written one, none.
        String word = "ultra" + LineBreaker.SOFT_HYPHEN + "fino-mate";
        List<LineBreaker.Line> lines = Lines.breakLines(TextRun.plain(word), 7, measurer, F, 1, LineBreaking.SIMPLE);
        assertEquals(List.of("ultra-", "fino-", "mate"), texts(lines));
        assertEquals(List.of(true, false, false), lines.stream().map(LineBreaker.Line::hyphenated).toList());
        assertEquals(List.of(true, true, false), lines.stream().map(LineBreaker.Line::splitsWord).toList());
        assertEquals(List.of("ultrafino-", "mate"), Lines.breakLines(word, 10, measurer, F, 1));
        // A soft hyphen right after a written one adds nothing: the line already ends with a hyphen.
        assertEquals(List.of("ab-", "cd"),
                Lines.breakLines("ab-" + LineBreaker.SOFT_HYPHEN + "cd", 3, measurer, F, 1));
    }

    @Test
    void thePatternsDoNotSplitThePartsOfAWordWithAWrittenHyphen() {
        // Width 16: the word is split after its hyphen. Width 13: "extraordinaria-" (15) does not fit, and the
        // patterns, which know the two words apart, do not split a word with a hyphen among its letters.
        String word = "extraordinaria-programación";
        assertEquals(List.of("extraordinaria-", "programación"),
                Lines.breakLines(word, 16, measurer, F, 1, LineBreaking.SIMPLE, HYPHENATOR));
        assertEquals(List.of(word), Lines.breakLines(word, 13, measurer, F, 1, LineBreaking.SIMPLE, HYPHENATOR));
    }

    @Test
    void knuthPlassChargesAWrittenHyphenLikeAnAddedOne() {
        // Width 22. Ending the first line after "x-" fills it exactly (no badness) and ending it before the word
        // leaves 3 for 9 spaces to stretch: badness 100 x (3/4.5)^3 = 29.6, demerits 39.6^2 = 1570 against 10^2 = 100.
        // The penalty of the hyphen, 50^2 = 2500, makes the line end between words; the greedy algorithm fills it.
        String text = "a b c d e f g h i j x-yy";
        assertEquals(List.of("a b c d e f g h i j", "x-yy"),
                Lines.breakLines(text, 22, measurer, F, 1, LineBreaking.OPTIMAL));
        assertEquals(List.of("a b c d e f g h i j x-", "yy"),
                Lines.breakLines(text, 22, measurer, F, 1, LineBreaking.SIMPLE));
    }

    @Test
    void theNarrowestWidthOfAWordWithWrittenHyphensIsItsWidestPart() {
        // "REF-" (4), "2026-" (5) and "009812" (6): no hyphen is added to measure them.
        IntrinsicSizes sizes = Lines.intrinsics("REF-2026-009812", measurer, F, 1);
        assertEquals(6, sizes.narrowest(), 1e-9);
        assertEquals(15, sizes.natural(), 1e-9);
        // "ab-" with its own hyphen (3), not with another one (4).
        assertEquals(3, Lines.intrinsics("ab-c", measurer, F, 1).narrowest(), 1e-9);
    }

    // --- Break long words (reference/common/typography.md) ----------------------------------------------------------

    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    @Test
    void aPartOfAWordWiderThanTheLineIsCutAtTheLastCharacterThatFits() {
        for (LineBreaking wrap : LineBreaking.values()) {
            List<LineBreaker.Line> lines = Lines.breakLongWords(TextRun.plain(LETTERS), 10, measurer, F, 1, wrap);
            assertEquals(List.of("ABCDEFGHIJ", "KLMNOPQRST", "UVWXYZ"), texts(lines), wrap.toString());
            // No hyphen is added, and the lines say that the word goes on.
            assertEquals(List.of(false, false, false), lines.stream().map(LineBreaker.Line::hyphenated).toList());
            assertEquals(List.of(true, true, false), lines.stream().map(LineBreaker.Line::splitsWord).toList());
        }
        // Without Break long words, the word takes a line and overflows.
        assertEquals(List.of(LETTERS), Lines.breakLines(LETTERS, 10, measurer, F, 1));
    }

    @Test
    void aWordThatFitsInALineOfItsOwnIsNeverCut() {
        // "uno ABCDEFGH" is 12 and the width, 10: the word (8) goes to the next line whole, as always.
        for (LineBreaking wrap : LineBreaking.values()) {
            assertEquals(List.of("uno", "ABCDEFGH"),
                    texts(Lines.breakLongWords(TextRun.plain("uno ABCDEFGH"), 10, measurer, F, 1, wrap)),
                    wrap.toString());
        }
    }

    @Test
    void aLongWordStartsALineOfItsOwn() {
        // The word (15) is wider than the line (10): it does not start in the line of the word before it, which
        // has room for some of it, and the words after it go on in the line its last part leaves.
        for (LineBreaking wrap : LineBreaking.values()) {
            assertEquals(List.of("id", "ABCDEFGHIJ", "KLMNO"),
                    texts(Lines.breakLongWords(TextRun.plain("id ABCDEFGHIJKLMNO"), 10, measurer, F, 1, wrap)),
                    wrap.toString());
            assertEquals(List.of("id", "ABCDEFGHIJ", "KLMNO fin"),
                    texts(Lines.breakLongWords(TextRun.plain("id ABCDEFGHIJKLMNO fin"), 10, measurer, F, 1, wrap)),
                    wrap.toString());
        }
    }

    @Test
    void withSameLineALongWordStartsInTheLineThatHasRoomForSomeOfIt() {
        // The word (15) is wider than the line (10): it is cut wherever a line ends, the first one included, which
        // it shares with the word before it.
        for (LineBreaking wrap : LineBreaking.values()) {
            assertEquals(List.of("id ABCDEFG", "HIJKLMNO"), texts(Lines.breakLongWords(
                    TextRun.plain("id ABCDEFGHIJKLMNO"), 10, measurer, F, 1, wrap, false)), wrap.toString());
        }
        // "REF-" fits and "ABCDEFGHIJKLMNOP" (16) does not: the line is filled with the start of the long part.
        assertEquals(List.of("REF-ABCDEF", "GHIJKLMNOP"), texts(Lines.breakLongWords(
                TextRun.plain("REF-ABCDEFGHIJKLMNOP"), 10, measurer, F, 1, LineBreaking.SIMPLE, false)));
    }

    @Test
    void onlyThePartsThatDoNotFitAreCut() {
        // "REF-" fits and "ABCDEFGHIJKLMNOP" (16) does not: the long part starts a line of its own, after the hyphen.
        for (LineBreaking wrap : LineBreaking.values()) {
            assertEquals(List.of("REF-", "ABCDEFGHIJ", "KLMNOP"),
                    texts(Lines.breakLongWords(TextRun.plain("REF-ABCDEFGHIJKLMNOP"), 10, measurer, F, 1, wrap)),
                    wrap.toString());
        }
        // Every part of "REF-2026-009812" fits in width 7: the word is split only after its hyphens.
        for (LineBreaking wrap : LineBreaking.values()) {
            assertEquals(List.of("REF-", "2026-", "009812"),
                    texts(Lines.breakLongWords(TextRun.plain("REF-2026-009812"), 7, measurer, F, 1, wrap)),
                    wrap.toString());
        }
        // A part split by the patterns with its hyphen is not cut either: "extraor-" and "dinaria" fit in 8.
        Paragraph p = Paragraph.of(TextRun.plain("extraordinaria"), measurer, F, 1, 0, HYPHENATOR, 8);
        assertEquals(List.of("extraor-", "dinaria"),
                texts(LineBreaker.breakLines(p, 0, LineWidths.uniform(8), LineBreaking.SIMPLE)));
    }

    @Test
    void aCharacterIsNotSeparatedFromItsCombiningMarks() {
        // Five letters with a combining acute accent (U+0301): ten code points of 1. Width 5: two letters with their
        // marks (4) fit, and the third letter alone would fit too, but its mark would start the next line.
        String accent = Character.toString(0x301);
        String a = "a" + accent;
        String e = "e" + accent;
        String word = a + e + a + e + a;
        for (LineBreaking wrap : LineBreaking.values()) {
            assertEquals(List.of(a + e, a + e, a),
                    texts(Lines.breakLongWords(TextRun.plain(word), 5, measurer, F, 1, wrap)), wrap.toString());
        }
        // Nor the two halves of a surrogate pair: each face (U+1F600) is one character of 1.
        String face = Character.toString(0x1F600);
        assertEquals(List.of(face.repeat(3), face.repeat(2)),
                texts(Lines.breakLongWords(TextRun.plain(face.repeat(5)), 3, measurer, F, 1, LineBreaking.SIMPLE)));
    }

    @Test
    void besideASideBoxALongWordIsCutAtTheWidthOfEachLine() {
        // Lines of 1 pt: the first two have 6 of the 10 of the content box. A word wider than the content box is
        // cut at each line; one that fits in a whole line leaves the short ones empty, as always.
        LineWidths widths = LineWidths.of(10, 1, List.of(new Exclusion(0, 2, 4, 0)));
        for (LineBreaking wrap : LineBreaking.values()) {
            Paragraph longWord = Paragraph.of(TextRun.plain(LETTERS), measurer, F, 1, 0, null, 10);
            assertEquals(List.of("ABCDEF", "GHIJKL", "MNOPQRSTUV", "WXYZ"),
                    texts(LineBreaker.breakLines(longWord, 0, widths, wrap)), wrap.toString());
            Paragraph shortWord = Paragraph.of(TextRun.plain("ABCDEFGH"), measurer, F, 1, 0, null, 10);
            assertEquals(List.of("", "", "ABCDEFGH"),
                    texts(LineBreaker.breakLines(shortWord, 0, widths, wrap)), wrap.toString());
        }
    }

    @Test
    void aCharacterWiderThanTheLineStillOverflows() {
        assertEquals(List.of("abc"),
                texts(Lines.breakLongWords(TextRun.plain("abc"), 0.5, measurer, F, 1, LineBreaking.SIMPLE)));
    }

    @Test
    void aWordWithABraceIsNotCut() {
        // It may hold {page} or a reference, which are replaced later.
        String word = "{page}ABCDEFGHIJ";
        assertEquals(List.of(word),
                texts(Lines.breakLongWords(TextRun.plain(word), 5, measurer, F, 1, LineBreaking.SIMPLE)));
    }

    @Test
    void theCutsTakeTheFormatOfEachRun() {
        // "ABCD" in bold (2 each) and "EFGHIJ": width 6 holds "ABC" (6); then "D" (2) and "EFGH" (4); then "IJ".
        List<TextRun> runs = List.of(run("ABCD", "b"), run("EFGHIJ"));
        List<LineBreaker.Line> lines = Lines.breakLongWords(runs, 6, boldIsWider, F, 1, LineBreaking.SIMPLE);
        assertEquals(List.of("ABC", "DEFGH", "IJ"), texts(lines));
        assertEquals(List.of(run("D", "b"), run("EFGH")), lines.get(1).runs());
    }

    @Test
    void breakLongWordsDoesNotChangeTheIntrinsicSizes() {
        // The narrowest width is asked without it (LineBreaker.intrinsics has no such option): a column does not
        // shrink to one letter for having it.
        assertEquals(26, Lines.intrinsics(LETTERS, measurer, F, 1).narrowest(), 1e-9);
    }

    @Test
    void aVeryLongWordIsCutInTimeProportionalToItsLength() {
        // 20 000 characters in lines of 40: each line only looks at the positions it can reach.
        String word = "0123456789".repeat(2000);
        for (LineBreaking wrap : LineBreaking.values()) {
            List<LineBreaker.Line> lines = org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(
                    java.time.Duration.ofSeconds(20),
                    () -> Lines.breakLongWords(TextRun.plain(word), 40, measurer, F, 1, wrap), wrap.toString());
            assertEquals(500, lines.size(), wrap.toString());
            assertEquals("0123456789".repeat(4), lines.get(499).text(), wrap.toString());
        }
    }
}
