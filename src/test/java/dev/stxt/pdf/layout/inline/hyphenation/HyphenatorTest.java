package dev.stxt.pdf.layout.inline.hyphenation;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Liang's algorithm with the patterns of hyph-utf8 (reference/common/typography.md). The expected breaks are the
 * ones TeX gives with the same patterns: the classic examples of the TeXbook and of Liang's thesis, the exceptions
 * written in the pattern files and the standard syllable division of Spanish and Catalan.
 */
class HyphenatorTest {

    private static Hyphenator of(String tag) {
        return Hyphenations.forLocale(Locale.forLanguageTag(tag));
    }

    /** The word with a hyphen at each break: {@code hy-phen-ation}. */
    private static String hyphenate(Hyphenator h, String word) {
        StringBuilder sb = new StringBuilder(word);
        int[] breaks = h.breaks(word);
        for (int i = breaks.length - 1; i >= 0; i--) {
            sb.insert(breaks[i], '-');
        }
        return sb.toString();
    }

    @Test
    void spanishBreaksLikeTeX() {
        Hyphenator es = of("es");
        assertEquals(2, es.left());
        assertEquals(2, es.right());
        assertEquals("cons-ti-tu-ción", hyphenate(es, "constitución"));
        assertEquals("trans-por-te", hyphenate(es, "transporte"));
        assertEquals("ex-tra-or-di-na-rio", hyphenate(es, "extraordinario"));
        assertEquals("ayun-ta-mien-to", hyphenate(es, "ayuntamiento"));
        assertEquals("pro-gra-ma-ción", hyphenate(es, "programación"));
        assertEquals("pa-la-bra", hyphenate(es, "palabra"));
    }

    @Test
    void catalanBreaksLikeTeX() {
        Hyphenator ca = of("ca");
        assertEquals("uni-ver-si-tat", hyphenate(ca, "universitat"));
        assertEquals("in-for-ma-ció", hyphenate(ca, "informació"));
        assertEquals("ajun-ta-ment", hyphenate(ca, "ajuntament"));
        // Exceptions of hyph-ca.tex.
        assertEquals("cu-rie", hyphenate(ca, "curie"));
        assertEquals("nos-al-tres", hyphenate(ca, "nosaltres"));
    }

    @Test
    void americanEnglishBreaksLikeTeX() {
        Hyphenator en = of("en-US");
        assertEquals(2, en.left());
        assertEquals(3, en.right());
        assertEquals("hy-phen-ation", hyphenate(en, "hyphenation"));
        assertEquals("al-go-rithm", hyphenate(en, "algorithm"));
        // Not com-put-er: three letters have to stay after the hyphen.
        assertEquals("com-puter", hyphenate(en, "computer"));
        assertEquals("su-per-cal-ifrag-ilis-tic-ex-pi-ali-do-cious",
                hyphenate(en, "supercalifragilisticexpialidocious"));
        // Exceptions of hyph-en-us.tex: the patterns alone would give other breaks, or none.
        assertEquals("as-so-ciate", hyphenate(en, "associate"));
        assertEquals("project", hyphenate(en, "project"));
        assertEquals("phil-an-thropic", hyphenate(en, "philanthropic"));
    }

    @Test
    void britishEnglishBreaksLikeTeX() {
        Hyphenator gb = of("en-GB");
        assertEquals("col-our", hyphenate(gb, "colour"));
        assertEquals("neigh-bour-hood", hyphenate(gb, "neighbourhood"));
        // The British patterns give one break more than the American ones here.
        assertEquals("hy-phen-a-tion", hyphenate(gb, "hyphenation"));
        // Exceptions of hyph-en-gb.tex.
        assertEquals("uni-ver-sity", hyphenate(gb, "university"));
        assertEquals("how-ever", hyphenate(gb, "however"));
    }

    @Test
    void theMinimaKeepLettersOnEachSide() {
        assertEquals(0, of("es").breaks("sol").length);
        assertEquals(0, of("es").breaks("a").length);
        // An exception is also subject to the minima: a-bcd-e keeps only the breaks with 2 letters on each side.
        Hyphenator h = new Hyphenator(List.of(), List.of("a-bcd-e", "ab-cd-ef"), 2, 2);
        assertEquals(0, h.breaks("abcde").length);
        assertArrayEquals(new int[]{2, 4}, h.breaks("abcdef"));
    }

    @Test
    void upperCaseIsHyphenatedLikeLowerCase() {
        assertArrayEquals(of("es").breaks("constitución"), of("es").breaks("CONSTITUCIÓN"));
        assertArrayEquals(of("en-US").breaks("associate"), of("en-US").breaks("Associate"));
    }

    @Test
    void resultsAreCopiesOfTheMemorizedOnes() {
        Hyphenator es = of("es");
        int[] first = es.breaks("constitución");
        first[0] = 99;
        assertArrayEquals(new int[]{4, 6, 8}, es.breaks("constitución"));
    }

    @Test
    void readsTheResourceFormat() {
        String resource = "# a comment\nleft 1\nright 1\npatterns\n1b\na1\nexceptions\nca-sa\n";
        Hyphenator h = Hyphenator.read(new ByteArrayInputStream(resource.getBytes(StandardCharsets.UTF_8)));
        assertEquals(1, h.left());
        assertEquals(1, h.right());
        // 1b: an odd level before each b; a1: after each a (between b and a there is none). The exception replaces
        // the patterns for its word.
        assertEquals("a-ba-b", hyphenate(h, "abab"));
        assertEquals("ca-sa", hyphenate(h, "casa"));
        assertEquals("ca-sa-s", hyphenate(h, "casas"));
    }

    @Test
    void languagesMapToTheirPatterns() {
        assertTrue(Hyphenations.supports(Locale.forLanguageTag("es")));
        assertTrue(Hyphenations.supports(Locale.forLanguageTag("es-MX")));
        assertTrue(Hyphenations.supports(Locale.forLanguageTag("ca")));
        assertTrue(Hyphenations.supports(Locale.forLanguageTag("en")));
        assertTrue(Hyphenations.supports(Locale.forLanguageTag("en-GB")));
        assertFalse(Hyphenations.supports(Locale.forLanguageTag("de")));
        assertFalse(Hyphenations.supports(Locale.ROOT));
        assertNull(Hyphenations.forLocale(Locale.forLanguageTag("fr")));
        // Each set of patterns is read once and shared; the region of English chooses between two of them.
        assertSame(of("es"), of("es-MX"));
        assertSame(of("en"), of("en-US"));
        assertSame(of("en-US"), of("en-AU"));
        assertNotSame(of("en-US"), of("en-GB"));
        assertEquals(List.of("es", "ca", "en-US", "en-GB"), Hyphenations.supported());
    }
}
