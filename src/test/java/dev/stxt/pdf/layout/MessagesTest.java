package dev.stxt.pdf.layout;

import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagesTest {

    @Test
    void pointsWithoutTrailingZeros() {
        assertEquals("100", Messages.pt(100));
        assertEquals("12.5", Messages.pt(12.5));
        assertEquals("0.33", Messages.pt(1 / 3.0));
        assertEquals("762", Messages.pt(761.999999));
        assertEquals("10000000", Messages.pt(1e7));
    }

    @Test
    void aShortExcerptIsKeptWhole() {
        assertEquals("palabra", Messages.excerpt("palabra"));
        assertEquals("x".repeat(40), Messages.excerpt("x".repeat(40)));
    }

    @Test
    void aLongExcerptIsCutAndSaysItsLength() {
        assertEquals("x".repeat(40) + "... (41 characters)", Messages.excerpt("x".repeat(41)));
        // Counts characters, not UTF-16 units: it does not split a surrogate pair.
        String emoji = "\uD83D\uDE00".repeat(50);
        assertEquals("\uD83D\uDE00".repeat(40) + "... (50 characters)", Messages.excerpt(emoji));
    }

    @Test
    void theMessageOfAWordThatDoesNotFitDoesNotGrowWithTheWord() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> LayoutFixture.layOut("\tText: " + "x".repeat(1000)));
        InvalidModelException longer = assertThrows(InvalidModelException.class,
                () -> LayoutFixture.layOut("\tText: " + "x".repeat(2000)));
        assertEquals(ErrorCode.TEXT_OVERFLOW, e.errorCode());
        assertTrue(e.getMessage().length() < 400, e.getMessage());
        assertTrue(e.getMessage().contains("(1000 characters)"), e.getMessage());
        assertTrue(longer.getMessage().contains("(2000 characters)"), longer.getMessage());
        // Only the figures change: the message does not grow with the word.
        assertTrue(Math.abs(longer.getMessage().length() - e.getMessage().length()) < 10);
    }
}
