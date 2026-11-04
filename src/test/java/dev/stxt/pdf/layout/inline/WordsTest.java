package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.layout.inline.hyphenation.Hyphenations;
import dev.stxt.pdf.layout.inline.hyphenation.Hyphenator;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

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
}
