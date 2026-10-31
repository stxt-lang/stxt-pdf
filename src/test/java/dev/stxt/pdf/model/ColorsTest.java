package dev.stxt.pdf.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Los colores del documento (reference/common/values.md): con y sin opacidad. */
class ColorsTest {

    @Test
    void acceptsThreeFourSixAndEightDigits() {
        for (String c : new String[]{"#F00", "#F008", "#A32D2D", "#A32D2D80", "#abc", "#12315a00"}) {
            assertTrue(Colors.isValid(c), c);
        }
    }

    @Test
    void rejectsAnythingElse() {
        for (String c : new String[]{"F00", "#", "#12", "#12345", "#1234567", "#123456789", "#GGG", "red", "#F0 0"}) {
            assertFalse(Colors.isValid(c), c);
        }
    }

    @Test
    void shortFormsRepeatEachDigit() {
        assertArrayEquals(new float[]{1, 0, 0, 1}, Colors.rgba("#F00"));
        assertArrayEquals(new float[]{1, 0, 0, 0x88 / 255f}, Colors.rgba("#F008"));
    }

    @Test
    void theLastPairIsTheOpacity() {
        float[] c = Colors.rgba("#A32D2D80");
        assertEquals(0xA3 / 255f, c[0]);
        assertEquals(0x2D / 255f, c[1]);
        assertEquals(0x80 / 255f, c[3]);
    }

    @Test
    void anOpaqueColorIsNotTranslucent() {
        assertFalse(Colors.isTranslucent("#A32D2D"));
        assertFalse(Colors.isTranslucent("#A32D2DFF"));
        assertTrue(Colors.isTranslucent("#A32D2DFE"));
        assertTrue(Colors.isTranslucent("#0000"));
    }
}
