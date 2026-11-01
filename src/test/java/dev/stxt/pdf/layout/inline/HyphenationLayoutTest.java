package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code Hyphenate} and the soft hyphen in the layout (reference/common/typography.md). With {@link FixedMetrics},
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
                texts(lines(TEXT, 70, "Hyphenate: true", "Line breaking: balanced")));
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
        List<TextLine> l = lines("ul­tra", 100, "Preformatted: true");
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
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> layOut("\tText: Donaudampfschiff",
                "\t\tTypography:", "\t\t\tLanguage: de", "\t\t\tHyphenate: true"));
        assertEquals(ErrorCode.HYPHENATION_LANGUAGE_UNSUPPORTED, e.errorCode());
        assertTrue(e.getMessage().contains("'de'") && e.getMessage().contains("es, ca, en-US and en-GB"), e.getMessage());
    }

    @Test
    void onlyBoxesWithTextNeedPatterns() {
        // Hyphenate and a language without patterns on a Block are fine if its texts are in a language with them.
        LayoutFixture f = layOut("\tBlock:", "\t\tTypography:", "\t\t\tLanguage: de", "\t\t\tHyphenate: true",
                "\t\tText: constitución", "\t\t\tId: T", "\t\t\tBox:", "\t\t\t\tWidth: 40",
                "\t\t\tTypography:", "\t\t\t\tLanguage: es");
        assertEquals(List.of("consti-", "tución"), texts(f.fragment("T").lines()));
    }
}
