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
        assertEquals(new Value.Fraction(2), ValueParser.value("2*"));
        assertEquals(new Value.Fraction(1), ValueParser.value("*"));
        assertEquals(new Value.Fraction(2.5), ValueParser.value("2.5*"));
        assertThrows(IllegalArgumentException.class, () -> ValueParser.value("1fr"));
        assertEquals(Value.AUTO, ValueParser.value("auto"));
        assertEquals(Value.Keyword.MIN_CONTENT, ValueParser.value("min-content"));
    }

    @Test
    void decimalCommaIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> ValueParser.value("12,5"));
    }

    @Test
    void between() {
        Value between = ValueParser.value("between(100, *)");
        assertEquals(new Value.Between(new Value.Points(100), new Value.Fraction(1)), between);
        assertThrows(IllegalArgumentException.class, () -> ValueParser.value("between(*, 100)"));
        assertThrows(IllegalArgumentException.class, () -> ValueParser.value("minmax(100, *)"));
    }

    @Test
    void minMaxAndClampAreNotValues() {
        // Lo hacen Min width y Max width (referencia/comun/valores.md).
        assertThrows(IllegalArgumentException.class, () -> ValueParser.value("min(300, 50%)"));
        assertThrows(IllegalArgumentException.class, () -> ValueParser.value("max(200, 30%)"));
        assertThrows(IllegalArgumentException.class, () -> ValueParser.value("clamp(200, 50%, 400)"));
    }

    @Test
    void listsWithRepeat() {
        List<Value> v = ValueParser.list("150 repeat(2, * auto) between(100, 2*)");
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
