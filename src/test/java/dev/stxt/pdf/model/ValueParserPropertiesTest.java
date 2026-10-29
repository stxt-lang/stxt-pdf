package dev.stxt.pdf.model;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Propiedades de {@link ValueParser} con textos al azar: lo único que puede lanzar es un
 * {@link IllegalArgumentException}, que el builder convierte en un error con línea; nunca otra excepción. La semilla
 * es fija: un fallo se repite.
 */
class ValueParserPropertiesTest {

    /** Trozos con que se escriben los valores: números, unidades, funciones, separadores y algo de basura. */
    private static final String[] PIECES = {"0", "1", "7", "12", "250", ".", ".5", "-", "+", "e", "e3", " ", "  ", ",",
            "(", ")", "%", "pt", "px", "em", "fr", "auto", "min-content", "max-content", "repeat", "repeat(",
            "minmax(", "fit-content(", "x", "#", "linear(", "deg", "\t", "1e400", "-0", "NaN", "Infinity"};

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
                // Un valor mal escrito: es lo esperado.
            } catch (RuntimeException e) {
                fail("ValueParser.value(\"" + text + "\") lanza " + e, e);
            }
            try {
                ValueParser.list(text);
            } catch (IllegalArgumentException expected) {
                // Ídem.
            } catch (RuntimeException e) {
                fail("ValueParser.list(\"" + text + "\") lanza " + e, e);
            }
        }
    }
}
