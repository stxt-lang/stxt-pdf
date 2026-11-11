package dev.stxt.pdf.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.stxt.pdf.model.Marker;
import dev.stxt.pdf.model.MarkerSuffix;

class ListMarkersTest {

    private Locale previous;

    /** Numbering does not depend on the language of the JVM: in Turkish, {@code "i".toUpperCase()} is {@code İ}. */
    @BeforeEach
    void turkishJvm() {
        previous = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(previous);
    }

    @Test
    void symbolsEndWithASpace() {
        assertEquals("• ", ListMarkers.text(Marker.BULLET, MarkerSuffix.PERIOD, 0));
        assertEquals("– ", ListMarkers.text(Marker.DASH, MarkerSuffix.PERIOD, 0));
        assertNull(ListMarkers.text(Marker.NONE, MarkerSuffix.PERIOD, 0));
    }

    @Test
    void decimal() {
        assertEquals("0. ", ListMarkers.text(Marker.DECIMAL, MarkerSuffix.PERIOD, 0));
        assertEquals("12. ", ListMarkers.text(Marker.DECIMAL, MarkerSuffix.PERIOD, 12));
    }

    @Test
    void lettersContinueAfterZ() {
        assertEquals("a. ", ListMarkers.text(Marker.LOWER_ALPHA, MarkerSuffix.PERIOD, 1));
        assertEquals("z. ", ListMarkers.text(Marker.LOWER_ALPHA, MarkerSuffix.PERIOD, 26));
        assertEquals("aa. ", ListMarkers.text(Marker.LOWER_ALPHA, MarkerSuffix.PERIOD, 27));
        assertEquals("AZ. ", ListMarkers.text(Marker.UPPER_ALPHA, MarkerSuffix.PERIOD, 52));
        assertEquals("BA. ", ListMarkers.text(Marker.UPPER_ALPHA, MarkerSuffix.PERIOD, 53));
    }

    @Test
    void theSuffixFollowsTheNumberOfANumbering() {
        assertEquals("12) ", ListMarkers.text(Marker.DECIMAL, MarkerSuffix.PARENTHESIS, 12));
        assertEquals("c) ", ListMarkers.text(Marker.LOWER_ALPHA, MarkerSuffix.PARENTHESIS, 3));
        assertEquals("IV) ", ListMarkers.text(Marker.UPPER_ROMAN, MarkerSuffix.PARENTHESIS, 4));
        // A symbol has no suffix, and the number of {number of: id} does not carry it.
        assertEquals("• ", ListMarkers.text(Marker.BULLET, MarkerSuffix.PARENTHESIS, 0));
        assertNull(ListMarkers.text(Marker.IMAGE, MarkerSuffix.PARENTHESIS, 0));
        assertEquals("c", ListMarkers.number(Marker.LOWER_ALPHA, 3));
    }

    @Test
    void romanNumerals() {
        assertEquals("iv. ", ListMarkers.text(Marker.LOWER_ROMAN, MarkerSuffix.PERIOD, 4));
        assertEquals("xiv. ", ListMarkers.text(Marker.LOWER_ROMAN, MarkerSuffix.PERIOD, 14));
        assertEquals("MCMXCIV. ", ListMarkers.text(Marker.UPPER_ROMAN, MarkerSuffix.PERIOD, 1994));
        assertEquals("MMMCMXCIX. ", ListMarkers.text(Marker.UPPER_ROMAN, MarkerSuffix.PERIOD, 3999));
    }

    @Test
    void theNumberHasNoSuffixAndOnlyNumberingsHaveOne() {
        assertEquals("12", ListMarkers.number(Marker.DECIMAL, 12));
        assertEquals("c", ListMarkers.number(Marker.LOWER_ALPHA, 3));
        assertEquals("AA", ListMarkers.number(Marker.UPPER_ALPHA, 27));
        assertEquals("iv", ListMarkers.number(Marker.LOWER_ROMAN, 4));
        assertEquals("XIV", ListMarkers.number(Marker.UPPER_ROMAN, 14));
        assertNull(ListMarkers.number(Marker.BULLET, 1));
        assertNull(ListMarkers.number(Marker.NONE, 1));
    }

    @Test
    void ranges() {
        assertTrue(ListMarkers.inRange(Marker.DECIMAL, 0));
        assertFalse(ListMarkers.inRange(Marker.DECIMAL, -1));
        assertFalse(ListMarkers.inRange(Marker.LOWER_ALPHA, 0));
        assertTrue(ListMarkers.inRange(Marker.UPPER_ROMAN, 3999));
        assertFalse(ListMarkers.inRange(Marker.UPPER_ROMAN, 4000));
    }
}
