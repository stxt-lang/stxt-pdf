package dev.stxt.boxes.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Los destinos de un enlace (referencia/comun/enlaces.md). */
class LinksTest {

    @ParameterizedTest
    @ValueSource(strings = {"https://stxt.dev", "http://example.com/a?b=c#d", "HTTPS://Example.COM",
            "mailto:soporte@airemas.example", "mailto:a@b.c?subject=Hola", "#intro", "#anexo-1"})
    void validDestinations(String destination) {
        assertTrue(Links.problem(destination).isEmpty(), () -> Links.problem(destination).orElse(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "javascript:alert(1)", "file:///etc/passwd", "ftp://example.com", "tel:+34600000000",
            "docs/manual.pdf", "www.example.com", "https://", "https:///ruta", "http://", "mailto:", "mailto:?subject=x",
            "#", "# intro", "#dos palabras", "https://exa mple.com"})
    void invalidDestinations(String destination) {
        assertTrue(Links.problem(destination).isPresent(), destination);
    }

    @Test
    void internalDestinationsPointToABox() {
        assertTrue(Links.internal("#intro"));
        assertFalse(Links.internal("https://stxt.dev"));
        assertEquals("intro", Links.target("#intro"));
        assertEquals("#intro", Links.to("intro"));
    }

    @Test
    void theMessageSaysWhatIsWrong() {
        assertTrue(Links.problem("javascript:alert(1)").orElseThrow().contains("esquema"));
        assertTrue(Links.problem("https://").orElseThrow().contains("URL"));
        assertTrue(Links.problem("mailto:?subject=x").orElseThrow().contains("destinatario"));
        assertTrue(Links.problem("").orElseThrow().contains("vacío"));
    }
}
