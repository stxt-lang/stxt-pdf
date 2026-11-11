package dev.stxt.pdf.layout.inline;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.layout.inline.hyphenation.Hyphenations;
import dev.stxt.pdf.layout.inline.hyphenation.Hyphenator;
import dev.stxt.pdf.model.TextRun;

/**
 * The break points of a word by the patterns of its language (reference/common/typography.md). The words of a text are
 * also tested in {@link LineBreakerTest} and {@link LineBreakerPropertiesTest}, through the line breaking.
 */
class WordsTest {

    @Test
    void onlyTheLettersOfAWordAreHyphenated() {
        Hyphenator es = Hyphenations.forLocale(Locale.forLanguageTag("es"));
        // Punctuation around the letters stays with them and does not count: pro-gra-ma-ción shifted by the «.
        assertArrayEquals(new int[]{4, 7, 9}, Words.breaks("«programación».", es));
        // Something that is not a letter among the letters, or a brace, which may hold {page}: no break.
        assertArrayEquals(new int[0], Words.breaks("extra-ordinaria", es));
        assertArrayEquals(new int[0], Words.breaks("col·lecció", es));
        assertArrayEquals(new int[0], Words.breaks("abc123def", es));
        assertArrayEquals(new int[0], Words.breaks("{page}", es));
        assertArrayEquals(new int[0], Words.breaks("2025", es));
    }

    @Test
    void anApostropheAmongTheLettersIsALetterOnlyInTheLanguagesWhosePatternsHaveIt() {
        Hyphenator fr = Hyphenations.forLocale(Locale.forLanguageTag("fr"));
        Hyphenator en = Hyphenations.forLocale(Locale.forLanguageTag("en-US"));
        // l'in-for-ma-tion, with the straight and the typographic apostrophe.
        assertArrayEquals(new int[]{4, 7, 9}, Words.breaks("l'information", fr));
        assertArrayEquals(new int[]{4, 7, 9}, Words.breaks("l’information", fr));
        // At the ends it is punctuation, like a quotation mark: in-for-ma-tion shifted by the opening one.
        assertArrayEquals(new int[]{3, 6, 8}, Words.breaks("‘information’", fr));
        // The English patterns do not have it, and the word is not split.
        assertArrayEquals(new int[0], Words.breaks("hyphenation's", en));
    }

    @Test
    void aHyphenWrittenBetweenLettersOrDigitsIsABreakPointAfterIt() {
        assertArrayEquals(new int[]{4, 9}, Words.afterWrittenHyphens("REF-2026-009812"));
        assertArrayEquals(new int[]{7}, Words.afterWrittenHyphens("físico-químico"));
        assertArrayEquals(new int[]{2}, Words.afterWrittenHyphens("a-1"));
        // Punctuation around the word does not matter, only the two neighbors of the hyphen.
        assertArrayEquals(new int[]{4}, Words.afterWrittenHyphens("(ab-cd)."));
        // At the start or the end, doubled, or beside something that is not a letter or a digit: none.
        assertArrayEquals(new int[0], Words.afterWrittenHyphens("-abc"));
        assertArrayEquals(new int[0], Words.afterWrittenHyphens("abc-"));
        assertArrayEquals(new int[0], Words.afterWrittenHyphens("a--b"));
        assertArrayEquals(new int[0], Words.afterWrittenHyphens("a-(b)"));
        assertArrayEquals(new int[0], Words.afterWrittenHyphens("{page}-{pages}"));
        assertArrayEquals(new int[0], Words.afterWrittenHyphens("-"));
        // Only U+002D: the non-breaking hyphen (U+2011) and the dashes are not break points.
        assertArrayEquals(new int[0], Words.afterWrittenHyphens("a" + Character.toString(0x2011) + "b"));
        assertArrayEquals(new int[0], Words.afterWrittenHyphens("a" + Character.toString(0x2013) + "b"));
        // The neighbors are code points: a letter outside the basic plane (U+1D400) counts.
        String bold = Character.toString(0x1D400);
        assertArrayEquals(new int[]{3}, Words.afterWrittenHyphens(bold + "-" + bold));
    }

    @Test
    void aWordCarriesItsWrittenHyphensWithItsOtherBreakPoints() {
        Words.Word plain = Lines.words(TextRun.plain("REF-2026")).get(0);
        assertArrayEquals(new int[]{4}, plain.breaks());
        assertTrue(plain.written(0));
        // With soft hyphens, in order: "ab(soft)cd-ef(soft)gh" is "abcd-efgh" with breaks at 2, 5 and 7.
        String soft = String.valueOf(LineBreaker.SOFT_HYPHEN);
        Words.Word mixed = Lines.words(TextRun.plain("ab" + soft + "cd-ef" + soft + "gh")).get(0);
        assertEquals("abcd-efgh", mixed.text());
        assertArrayEquals(new int[]{2, 5, 7}, mixed.breaks());
        assertFalse(mixed.written(0));
        assertTrue(mixed.written(1));
        assertFalse(mixed.written(2));
        // A soft hyphen right after the written one is the same break point, and the written one counts.
        Words.Word both = Lines.words(TextRun.plain("ab-" + soft + "cd")).get(0);
        assertArrayEquals(new int[]{3}, both.breaks());
        assertTrue(both.written(0));
        // A word without them has no break point, and the mark of the forced line break survives.
        List<Words.Word> forced = Lines.words(TextRun.plain("a-b" + TextRun.LINE_SEPARATOR + "cd"));
        assertTrue(forced.get(0).forced());
        assertTrue(forced.get(0).written(0));
        assertEquals(0, forced.get(1).breaks().length);
    }
}
