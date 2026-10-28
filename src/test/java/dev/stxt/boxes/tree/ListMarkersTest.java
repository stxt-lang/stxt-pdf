package dev.stxt.boxes.tree;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListMarkersTest {

    @Test
    void symbolsEndWithASpace() {
        assertEquals("• ", ListMarkers.text(Style.Marker.DISC, 0));
        assertEquals("– ", ListMarkers.text(Style.Marker.DASH, 0));
        assertNull(ListMarkers.text(Style.Marker.NONE, 0));
    }

    @Test
    void decimal() {
        assertEquals("0. ", ListMarkers.text(Style.Marker.DECIMAL, 0));
        assertEquals("12. ", ListMarkers.text(Style.Marker.DECIMAL, 12));
    }

    @Test
    void lettersContinueAfterZ() {
        assertEquals("a. ", ListMarkers.text(Style.Marker.LOWER_ALPHA, 1));
        assertEquals("z. ", ListMarkers.text(Style.Marker.LOWER_ALPHA, 26));
        assertEquals("aa. ", ListMarkers.text(Style.Marker.LOWER_ALPHA, 27));
        assertEquals("AZ. ", ListMarkers.text(Style.Marker.UPPER_ALPHA, 52));
        assertEquals("BA. ", ListMarkers.text(Style.Marker.UPPER_ALPHA, 53));
    }

    @Test
    void romanNumerals() {
        assertEquals("iv. ", ListMarkers.text(Style.Marker.LOWER_ROMAN, 4));
        assertEquals("xiv. ", ListMarkers.text(Style.Marker.LOWER_ROMAN, 14));
        assertEquals("MCMXCIV. ", ListMarkers.text(Style.Marker.UPPER_ROMAN, 1994));
        assertEquals("MMMCMXCIX. ", ListMarkers.text(Style.Marker.UPPER_ROMAN, 3999));
    }

    @Test
    void theNumberHasNoSuffixAndOnlyNumberingsHaveOne() {
        assertEquals("12", ListMarkers.number(Style.Marker.DECIMAL, 12));
        assertEquals("c", ListMarkers.number(Style.Marker.LOWER_ALPHA, 3));
        assertEquals("AA", ListMarkers.number(Style.Marker.UPPER_ALPHA, 27));
        assertEquals("iv", ListMarkers.number(Style.Marker.LOWER_ROMAN, 4));
        assertEquals("XIV", ListMarkers.number(Style.Marker.UPPER_ROMAN, 14));
        assertNull(ListMarkers.number(Style.Marker.DISC, 1));
        assertNull(ListMarkers.number(Style.Marker.NONE, 1));
    }

    @Test
    void ranges() {
        assertTrue(ListMarkers.inRange(Style.Marker.DECIMAL, 0));
        assertFalse(ListMarkers.inRange(Style.Marker.DECIMAL, -1));
        assertFalse(ListMarkers.inRange(Style.Marker.LOWER_ALPHA, 0));
        assertTrue(ListMarkers.inRange(Style.Marker.UPPER_ROMAN, 3999));
        assertFalse(ListMarkers.inRange(Style.Marker.UPPER_ROMAN, 4000));
    }
}
