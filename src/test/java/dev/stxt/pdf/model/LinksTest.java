package dev.stxt.pdf.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The targets of a link (reference/common/links.md). */
class LinksTest {

    @ParameterizedTest
    @ValueSource(strings = {"https://stxt.dev", "http://example.com/a?b=c#d", "HTTPS://Example.COM",
            "mailto:soporte@airemas.example", "mailto:a@b.c?subject=Hola", "#intro", "#anexo-1",
            "https://example.com/envíos?de=almacén#sección", "mailto:josé@example.com?subject=Año", "#sección"})
    void validDestinations(String destination) {
        assertTrue(Links.problem(destination).isEmpty(), () -> Links.problem(destination).orElse(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "javascript:alert(1)", "file:///etc/passwd", "ftp://example.com", "tel:+34600000000",
            "docs/es/manual.pdf", "www.example.com", "https://", "https:///ruta", "http://", "mailto:", "mailto:?subject=x",
            "#", "# intro", "#dos palabras", "https://exa mple.com", "https://example.com/dos palabras",
            "https://example.com/a\u00A0b", "https://españa.example/"})
    void invalidDestinations(String destination) {
        assertTrue(Links.problem(destination).isPresent(), destination);
    }

    /** What is not ASCII goes as the bytes of its UTF-8 form, and the rest stays as the author wrote it. */
    @Test
    void anExternalDestinationIsWrittenInAscii() {
        assertEquals("https://example.com/env%C3%ADos?de=almac%C3%A9n#secci%C3%B3n",
                Links.ascii("https://example.com/envíos?de=almacén#sección"));
        assertEquals("mailto:jos%C3%A9@example.com?subject=A%C3%B1o",
                Links.ascii("mailto:josé@example.com?subject=Año"));
        // Three bytes for the euro sign and four for a character outside the basic plane.
        assertEquals("https://example.com/%E2%82%AC/%F0%9F%98%80", Links.ascii("https://example.com/€/\uD83D\uDE00"));
        // Not normalized: an e with a combining accent is still two characters.
        assertEquals("https://example.com/e%CC%81", Links.ascii("https://example.com/e\u0301"));
        // The reserved characters and the escapes the author wrote are not touched.
        String written = "https://user@example.com:8080/a%20b/%c3%ad?x=1&y=%25#f~_.-!$'()*+,;=:@/?[]";
        assertEquals(written, Links.ascii(written));
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
        assertTrue(Links.problem("javascript:alert(1)").orElseThrow().contains("scheme"));
        assertTrue(Links.problem("https://").orElseThrow().contains("URL"));
        assertTrue(Links.problem("mailto:?subject=x").orElseThrow().contains("recipient"));
        assertTrue(Links.problem("").orElseThrow().contains("empty"));
        // A host with an ñ has a host: what it lacks is its ASCII form.
        assertTrue(Links.problem("https://españa.example/").orElseThrow().contains("xn--"));
    }
}
