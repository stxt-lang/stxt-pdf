package dev.stxt.boxes.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessagesTest {

    @Test
    void pointsWithoutTrailingZeros() {
        assertEquals("100", Messages.pt(100));
        assertEquals("12.5", Messages.pt(12.5));
        assertEquals("0.33", Messages.pt(1 / 3.0));
        assertEquals("762", Messages.pt(761.999999));
        assertEquals("10000000", Messages.pt(1e7));
    }
}
