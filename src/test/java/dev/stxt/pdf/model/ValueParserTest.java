package dev.stxt.pdf.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValueParserTest {

    @Test
    void numbersPercentagesAndFractions() {
        assertEquals(new Value.Points(12.5), ValueParser.value("12.5"));
        assertEquals(new Value.Percentage(50), ValueParser.value(" 50% "));
        assertEquals(new Value.Fraction(2), ValueParser.value("2fr"));
        assertEquals(Value.AUTO, ValueParser.value("auto"));
        assertEquals(Value.Keyword.MIN_CONTENT, ValueParser.value("min-content"));
    }

    @Test
    void decimalCommaIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> ValueParser.value("12,5"));
    }

    @Test
    void functions() {
        Value clamp = ValueParser.value("clamp(200, 50%, 400)");
        assertEquals(300, clamp.resolve(600).getAsDouble(), 1e-9);
        assertEquals(200, clamp.resolve(100).getAsDouble(), 1e-9);
        assertEquals(400, clamp.resolve(1000).getAsDouble(), 1e-9);
        assertTrue(clamp.resolve(OptionalDouble.empty()).isEmpty());

        Value mm = ValueParser.value("minmax(100, 1fr)");
        assertEquals(new Value.MinMax(new Value.Points(100), new Value.Fraction(1)), mm);
        assertThrows(IllegalArgumentException.class, () -> ValueParser.value("minmax(1fr, 100)"));
    }

    @Test
    void listsWithRepeat() {
        List<Value> v = ValueParser.list("150 repeat(2, 1fr auto) minmax(100, 2fr)");
        assertEquals(6, v.size());
        assertEquals(new Value.Fraction(1), v.get(1));
        assertEquals(Value.AUTO, v.get(4));
    }

    @Test
    void percentageOfIndefiniteBase() {
        assertTrue(ValueParser.value("50%").resolve(OptionalDouble.empty()).isEmpty());
        assertEquals(257.5, ValueParser.value("50%").resolve(515).getAsDouble(), 1e-9);
    }
}
