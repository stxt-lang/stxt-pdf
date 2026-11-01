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
        assertEquals(Value.Keyword.NARROWEST, ValueParser.value("narrowest"));
    }

    @Test
    void millimetersAreConvertedToPoints() {
        assertEquals(72, ((Value.Points) ValueParser.value("25.4mm")).pt(), 1e-9);
        assertEquals(595.2756, ((Value.Points) ValueParser.value("210mm")).pt(), 1e-4);
        assertEquals(-14.1732, ((Value.Points) ValueParser.value("-5mm")).pt(), 1e-4);
        assertEquals(new Value.Points(0), ValueParser.value("0mm"));
        List<Value> margin = ValueParser.list("10mm 12");
        assertEquals(28.3465, ((Value.Points) margin.get(0)).pt(), 1e-4);
        assertEquals(new Value.Points(12), margin.get(1));
        Value between = ValueParser.value("between(20mm, *)");
        assertEquals(new Value.Between(Value.mm(20), new Value.Fraction(1)), between);
    }

    @Test
    void theUnitGoesAttachedToTheNumber() {
        assertThrows(IllegalArgumentException.class, () -> ValueParser.value("12 mm"));
        // In a list the space separates values: "mm" alone is not a value.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ValueParser.list("10 mm"));
        assertTrue(ex.getMessage().contains("attached"), ex.getMessage());
        assertThrows(IllegalArgumentException.class, () -> ValueParser.value("mm"));
    }

    @Test
    void otherUnitsAreInvalid() {
        for (String text : List.of("1cm", "12pt", "1in", "12MM", "12px")) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ValueParser.value(text));
            assertTrue(ex.getMessage().contains("12mm"), ex.getMessage());
        }
        assertThrows(IllegalArgumentException.class, () -> ValueParser.value("5mm%"));
        assertThrows(IllegalArgumentException.class, () -> ValueParser.value("2mm*"));
        assertThrows(IllegalArgumentException.class, () -> ValueParser.value("12,5mm"));
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
        // Min width and Max width do it (reference/common/values.md).
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
