package dev.stxt.pdf.model;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Properties of {@link ValueParser} with random texts: the only thing it can throw is an
 * {@link IllegalArgumentException}, which the builder turns into an error with a line; never another exception. The
 * seed is fixed: a failure repeats.
 */
class ValueParserPropertiesTest {

    /** Pieces the values are written with: numbers, units, functions, separators and some garbage. */
    private static final String[] PIECES = {"0", "1", "7", "12", "250", ".", ".5", "-", "+", "e", "e3", " ", "  ", ",",
            "(", ")", "%", "pt", "px", "em", "*", "auto", "narrowest", "natural", "repeat", "repeat(",
            "between(", "fit(", "x", "#", "linear(", "deg", "\t", "1e400", "-0", "NaN", "Infinity"};

    private static String randomValue(Random random) {
        StringBuilder sb = new StringBuilder();
        int pieces = 1 + random.nextInt(8);
        for (int i = 0; i < pieces; i++) {
            sb.append(PIECES[random.nextInt(PIECES.length)]);
        }
        return sb.toString();
    }

    @Test
    void onlyIllegalArgumentExceptionEscapes() {
        Random random = new Random(20261029);
        for (int i = 0; i < 50_000; i++) {
            String text = randomValue(random);
            try {
                ValueParser.value(text);
            } catch (IllegalArgumentException expected) {
                // A badly written value: it is what is expected.
            } catch (RuntimeException e) {
                fail("ValueParser.value(\"" + text + "\") throws " + e, e);
            }
            try {
                ValueParser.list(text);
            } catch (IllegalArgumentException expected) {
                // Same.
            } catch (RuntimeException e) {
                fail("ValueParser.list(\"" + text + "\") throws " + e, e);
            }
        }
    }

    /**
     * A number of the document is finite and written as {@code 12}, {@code 12.5} or {@code -30}
     * (reference/common/values.md): {@code NaN}, {@code Infinity}, a hexadecimal number, an exponent or more digits than
     * a double holds are an {@link IllegalArgumentException}, although {@link Double#parseDouble} reads them.
     */
    @Test
    void aFactorIsFiniteAndWrittenAsTheDocumentWritesNumbers() {
        String[] pieces = {"0", "1", "9", "-", "+", ".", "e", "E", "x", "p", "0x", "NaN", "Infinity", "d", "f", " ",
                "1".repeat(400)};
        Random random = new Random(20261103);
        for (int i = 0; i < 50_000; i++) {
            StringBuilder sb = new StringBuilder();
            int count = 1 + random.nextInt(6);
            for (int k = 0; k < count; k++) {
                sb.append(pieces[random.nextInt(pieces.length)]);
            }
            String text = sb.toString();
            double v;
            try {
                v = ValueParser.factor(text);
            } catch (IllegalArgumentException expected) {
                continue;
            } catch (RuntimeException e) {
                fail("ValueParser.factor(\"" + text + "\") throws " + e, e);
                return;
            }
            if (!Double.isFinite(v) || !text.trim().matches("-?\\d+(\\.\\d+)?")) {
                fail("ValueParser.factor(\"" + text + "\") accepts " + v);
            }
        }
    }
}
