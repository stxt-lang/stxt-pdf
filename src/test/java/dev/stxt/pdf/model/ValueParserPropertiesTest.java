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
}
