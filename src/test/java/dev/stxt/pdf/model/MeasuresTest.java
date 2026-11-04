package dev.stxt.pdf.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MeasuresTest {

    @Test
    void pointsWithoutTrailingZerosAndWithTheUnit() {
        assertEquals("100 pt", Measures.pt(100));
        assertEquals("12.5 pt", Measures.pt(12.5));
        assertEquals("0.33 pt", Measures.pt(1 / 3.0));
        assertEquals("762 pt", Measures.pt(761.999999));
        assertEquals("10000000 pt", Measures.pt(1e7));
    }
}
