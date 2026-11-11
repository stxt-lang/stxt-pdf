package dev.stxt.pdf.layout.inline;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.tree.DocumentSyntaxException;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;

/**
 * {@code Hyphenate}, the soft hyphen, the written hyphen and {@code Break long words} in the layout
 * (reference/common/typography.md). With {@link FixedMetrics},
 * each character measures 5 pt with {@code Size: 10}, the hyphen too; the fixture document is in Spanish.
 */
class HyphenationLayoutTest {

    private static final String TEXT = "la programación extraordinaria";

    /** The lines of a {@code Text} of {@code width} pt with these properties in its {@code Typography}. */
    private static List<TextLine> lines(String text, double width, String... typography) {
        List<String> doc = new ArrayList<>(List.of("\tText: " + text, "\t\tId: T", "\t\tBox:", "\t\t\tWidth: " + width,
                "\t\tTypography:"));
        for (String p : typography) {
            doc.add("\t\t\t" + p);
        }
        return layOut(doc.toArray(String[]::new)).fragment("T").lines();
    }

    private static List<String> texts(List<TextLine> lines) {
        return lines.stream().map(TextLine::text).toList();
    }

    @Test
    void hyphenateSplitsWordsWithThePatternsOfTheLanguage() {
        // Width 70 = 14 characters: the breaks of LineBreakerTest.greedySplitsTheWordThatDoesNotFitAtItsLongestFittingPart,
        // here with the real Spanish patterns (pro-gra-ma-ción, ex-tra-or-di-na-ria).
        List<TextLine> l = lines(TEXT, 70, "Hyphenate: true");
        assertEquals(List.of("la programa-", "ción extraor-", "dinaria"), texts(l));
        assertEquals(List.of(true, true, false), l.stream().map(TextLine::hyphenated).toList());
        assertEquals(60, l.get(0).width(), 1e-9);
        assertEquals(List.of("la programa-", "ción extraor-", "dinaria"),
                texts(lines(TEXT, 70, "Hyphenate: true", "Line breaking: optimal")));
    }

    @Test
    void wordsStayWholeByDefault() {
        List<TextLine> l = lines(TEXT, 70);
        assertEquals(List.of("la", "programación", "extraordinaria"), texts(l));
        assertTrue(l.stream().noneMatch(TextLine::hyphenated));
    }

    @Test
    void hyphenateIsInheritedAndUsesTheLanguageOfEachBox() {
        // Width 40 = 8 characters. British: neigh-bour-hood, "neighbour-" (10) does not fit and "neigh-" (6) does;
        // the rest, "bourhood" (8). American: hy-phen-ation, "hyphen-" (7) and "ation".
        LayoutFixture f = layOut("\tBlock:", "\t\tTypography:", "\t\t\tHyphenate: true",
                "\t\tText: neighbourhood", "\t\t\tId: GB", "\t\t\tBox:", "\t\t\t\tWidth: 40",
                "\t\t\tTypography:", "\t\t\t\tLanguage: en-GB",
                "\t\tText: hyphenation", "\t\t\tId: US", "\t\t\tBox:", "\t\t\t\tWidth: 40",
                "\t\t\tTypography:", "\t\t\t\tLanguage: en");
        assertEquals(List.of("neigh-", "bourhood"), texts(f.fragment("GB").lines()));
        assertEquals(List.of("hyphen-", "ation"), texts(f.fragment("US").lines()));
    }

    @Test
    void softHyphenIsPaintedOnlyWhereTheWordIsSplit() {
        List<TextLine> whole = lines("ultra­moderno", 60);
        assertEquals(List.of("ultramoderno"), texts(whole));
        assertEquals(60, whole.get(0).width(), 1e-9);
        List<TextLine> split = lines("ultra­moderno", 55);
        assertEquals(List.of("ultra-", "moderno"), texts(split));
        assertTrue(split.get(0).hyphenated());
    }

    @Test
    void preformattedTextDropsTheSoftHyphens() {
        List<TextLine> l = lines("ul­tra", 100, "Preformatted: keep");
        assertEquals(List.of("ultra"), texts(l));
        assertEquals(25, l.get(0).width(), 1e-9);
    }

    @Test
    void theNarrowestWidthCountsTheParts() {
        // A grid column of the narrowest width: with Hyphenate, the widest part with its hyphen ("pro-", "gra-",
        // "ción": 20) and the word in four lines; without it, the whole word (60) in one.
        LayoutFixture f = layOut("\tGrid:", "\t\tColumn sizes: narrowest", "\t\tArea:",
                "\t\t\tText: programación", "\t\t\t\tId: H", "\t\t\t\tTypography:", "\t\t\t\t\tHyphenate: true",
                "\tGrid:", "\t\tColumn sizes: narrowest", "\t\tArea:", "\t\t\tText: programación", "\t\t\t\tId: W");
        assertEquals(20, f.rect("H").width(), 1e-9);
        assertEquals(List.of("pro-", "gra-", "ma-", "ción"), texts(f.fragment("H").lines()));
        assertEquals(60, f.rect("W").width(), 1e-9);
    }

    @Test
    void aLanguageWithoutPatternsIsAnError() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> layOut("\tText: kaupunginvaltuusto",
                "\t\tTypography:", "\t\t\tLanguage: fi", "\t\t\tHyphenate: true"));
        assertEquals(ErrorCode.HYPHENATION_LANGUAGE_UNSUPPORTED, e.errorCode());
        assertTrue(e.getMessage().contains("'fi'")
                && e.getMessage().contains("es, ca, en-US, en-GB, de, fr, it, nl and pt"), e.getMessage());
    }

    @Test
    void onlyBoxesWithTextNeedPatterns() {
        // Hyphenate and a language without patterns on a Block are fine if its texts are in a language with them.
        LayoutFixture f = layOut("\tBlock:", "\t\tTypography:", "\t\t\tLanguage: fi", "\t\t\tHyphenate: true",
                "\t\tText: constitución", "\t\t\tId: T", "\t\t\tBox:", "\t\t\t\tWidth: 40",
                "\t\t\tTypography:", "\t\t\t\tLanguage: es");
        assertEquals(List.of("consti-", "tución"), texts(f.fragment("T").lines()));
    }

    // --- Written hyphens and Break long words -----------------------------------------------------------------------

    @Test
    void aWordIsSplitAfterItsWrittenHyphenWithoutAnyProperty() {
        // Width 50 = 10 characters: "REF-2026-" (9) and "009812", with the hyphen of the text and no other.
        List<TextLine> l = lines("REF-2026-009812", 50);
        assertEquals(List.of("REF-2026-", "009812"), texts(l));
        assertEquals(List.of(false, false), l.stream().map(TextLine::hyphenated).toList());
        assertEquals(List.of(true, false), l.stream().map(TextLine::splitsWord).toList());
        assertEquals(45, l.get(0).width(), 1e-9);
        // A hyphenated line splits its word too.
        assertEquals(List.of(true, true, false),
                lines(TEXT, 70, "Hyphenate: true").stream().map(TextLine::splitsWord).toList());
    }

    @Test
    void preformattedTextIsNotSplitAfterAWrittenHyphen() {
        // Preformatted: wrap breaks its lines between words only: "REF-2026-009812" (75) goes whole to its line.
        assertEquals(List.of("ref", "REF-2026-009812"),
                texts(lines("ref REF-2026-009812", 80, "Preformatted: wrap")));
    }

    @Test
    void breakLongWordsCutsTheWordWiderThanItsBox() {
        // Width 50 = 10 characters, and the IBAN has 24.
        List<TextLine> l = lines("ES7620770024003102575766", 50, "Break long words: new-line");
        assertEquals(List.of("ES76207700", "2400310257", "5766"), texts(l));
        assertTrue(l.stream().noneMatch(TextLine::hyphenated));
        assertEquals(List.of(true, true, false), l.stream().map(TextLine::splitsWord).toList());
        assertEquals(List.of("ES76207700", "2400310257", "5766"),
                texts(lines("ES7620770024003102575766", 50, "Break long words: new-line", "Line breaking: optimal")));
        // A word that fits in a line of its own is not cut: it goes to the next line.
        assertEquals(List.of("IBAN", "ES76207700"), texts(lines("IBAN ES76207700", 50, "Break long words: new-line")));
    }

    @Test
    void theLongWordStartsALineOfItsOwnOrTheCurrentOne() {
        // Width 50 = 10 characters: "ID " takes 3 of the first line.
        assertEquals(List.of("ID", "ES76207700", "2400"),
                texts(lines("ID ES762077002400", 50, "Break long words: new-line")));
        assertEquals(List.of("ID ES76207", "7002400"),
                texts(lines("ID ES762077002400", 50, "Break long words: same-line")));
        // true and false are not values of the property: the schema rejects them.
        assertThrows(DocumentSyntaxException.class, () -> lines("ID ES762077002400", 50, "Break long words: true"));
    }

    @Test
    void breakLongWordsIsInherited() {
        LayoutFixture f = layOut("\tBlock:", "\t\tTypography:", "\t\t\tBreak long words: new-line",
                "\t\tText: ABCDEFGHIJKLMNOP", "\t\t\tId: T", "\t\t\tBox:", "\t\t\t\tWidth: 50",
                "\t\tText: ABCDEFGHIJ KLMNOP", "\t\t\tId: OFF", "\t\t\tBox:", "\t\t\t\tWidth: 50",
                "\t\t\tTypography:", "\t\t\t\tBreak long words: never");
        assertEquals(List.of("ABCDEFGHIJ", "KLMNOP"), texts(f.fragment("T").lines()));
        assertEquals(List.of("ABCDEFGHIJ", "KLMNOP"), texts(f.fragment("OFF").lines()));
    }

    @Test
    void breakLongWordsDoesNotChangeTheNarrowestWidth() {
        // A grid column of the narrowest width is as wide as the word (80) with or without it: it only acts where
        // the final width is not enough.
        LayoutFixture f = layOut("\tGrid:", "\t\tColumn sizes: narrowest", "\t\tArea:",
                "\t\t\tText: ABCDEFGHIJKLMNOP", "\t\t\t\tId: B", "\t\t\t\tTypography:",
                "\t\t\t\t\tBreak long words: new-line");
        assertEquals(80, f.rect("B").width(), 1e-9);
        assertEquals(List.of("ABCDEFGHIJKLMNOP"), texts(f.fragment("B").lines()));
        // With a written hyphen, the narrowest width is that of the widest part: "2026-" and "00981" (25).
        LayoutFixture h = layOut("\tGrid:", "\t\tColumn sizes: narrowest", "\t\tArea:",
                "\t\t\tText: REF-2026-00981", "\t\t\t\tId: H");
        assertEquals(25, h.rect("H").width(), 1e-9);
        assertEquals(List.of("REF-", "2026-", "00981"), texts(h.fragment("H").lines()));
    }

    @Test
    void aTextThatBreaksItsLongWordsDoesNotShrinkForItsWidth() {
        // With Overflow: shrink the text would go down to the Size at which the word fits; cut, it fits at its Size.
        List<TextLine> l = layOut("\tText: ABCDEFGHIJKLMNOP", "\t\tId: T", "\t\tBox:", "\t\t\tWidth: 50",
                "\t\t\tOverflow: shrink", "\t\tTypography:", "\t\t\tBreak long words: new-line").fragment("T").lines();
        assertEquals(List.of("ABCDEFGHIJ", "KLMNOP"), texts(l));
        assertEquals(10, l.get(0).fontSize(), 1e-9);
    }

    @Test
    void breakLongWordsWrittenByAPreformattedTextIsAnError() {
        for (String preformatted : List.of("keep", "wrap")) {
            InvalidModelException e = assertThrows(InvalidModelException.class, () -> layOut("\tText: abc",
                    "\t\tTypography:", "\t\t\tPreformatted: " + preformatted, "\t\t\tBreak long words: new-line"));
            assertEquals(ErrorCode.BREAK_LONG_WORDS_IN_PREFORMATTED, e.errorCode(), preformatted);
        }
        // Inherited, it is not checked: the preformatted text of a box that has it is left alone.
        LayoutFixture f = layOut("\tBlock:", "\t\tTypography:", "\t\t\tBreak long words: new-line",
                "\t\tText: abc", "\t\t\tId: T", "\t\t\tTypography:", "\t\t\t\tPreformatted: keep");
        assertEquals(List.of("abc"), texts(f.fragment("T").lines()));
        // Nor is Break long words: never, which changes nothing.
        layOut("\tText: abc", "\t\tTypography:", "\t\t\tPreformatted: keep", "\t\t\tBreak long words: never");
    }
}
