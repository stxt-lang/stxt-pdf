package dev.stxt.pdf.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SidesTest {

    @Test
    void shorthandLikeCss() {
        assertEquals(new Sides<>(10, 20, 5, 20), Sides.shorthand(List.of(10, 20, 5)));
        assertEquals(new Sides<>(10, 20, 10, 20), Sides.shorthand(List.of(10, 20)));
        assertThrows(IllegalArgumentException.class, () -> Sides.shorthand(List.of(1, 2, 3, 4, 5)));
    }

    @Test
    void verticalPercentagesUseTheWidth() {
        // Module 1: the percentages of margin and padding, vertical ones too, use the width.
        Insets i = Insets.resolve(Sides.shorthand(ValueParser.list("10% auto")), 200);
        assertEquals(new Insets(20, 0, 20, 0), i);
    }
}
