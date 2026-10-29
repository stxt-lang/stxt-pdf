package dev.stxt.boxes.tree;

import dev.stxt.boxes.model.Marker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListMarkersTest {

    private Locale previous;

    /** La numeración no depende del idioma de la JVM: en turco, {@code "i".toUpperCase()} es {@code İ}. */
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
        assertEquals("• ", ListMarkers.text(Marker.DISC, 0));
        assertEquals("– ", ListMarkers.text(Marker.DASH, 0));
        assertNull(ListMarkers.text(Marker.NONE, 0));
    }

    @Test
    void decimal() {
        assertEquals("0. ", ListMarkers.text(Marker.DECIMAL, 0));
        assertEquals("12. ", ListMarkers.text(Marker.DECIMAL, 12));
    }

    @Test
    void lettersContinueAfterZ() {
        assertEquals("a. ", ListMarkers.text(Marker.LOWER_ALPHA, 1));
        assertEquals("z. ", ListMarkers.text(Marker.LOWER_ALPHA, 26));
        assertEquals("aa. ", ListMarkers.text(Marker.LOWER_ALPHA, 27));
        assertEquals("AZ. ", ListMarkers.text(Marker.UPPER_ALPHA, 52));
        assertEquals("BA. ", ListMarkers.text(Marker.UPPER_ALPHA, 53));
    }

    @Test
    void romanNumerals() {
        assertEquals("iv. ", ListMarkers.text(Marker.LOWER_ROMAN, 4));
        assertEquals("xiv. ", ListMarkers.text(Marker.LOWER_ROMAN, 14));
        assertEquals("MCMXCIV. ", ListMarkers.text(Marker.UPPER_ROMAN, 1994));
        assertEquals("MMMCMXCIX. ", ListMarkers.text(Marker.UPPER_ROMAN, 3999));
    }

    @Test
    void theNumberHasNoSuffixAndOnlyNumberingsHaveOne() {
        assertEquals("12", ListMarkers.number(Marker.DECIMAL, 12));
        assertEquals("c", ListMarkers.number(Marker.LOWER_ALPHA, 3));
        assertEquals("AA", ListMarkers.number(Marker.UPPER_ALPHA, 27));
        assertEquals("iv", ListMarkers.number(Marker.LOWER_ROMAN, 4));
        assertEquals("XIV", ListMarkers.number(Marker.UPPER_ROMAN, 14));
        assertNull(ListMarkers.number(Marker.DISC, 1));
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
