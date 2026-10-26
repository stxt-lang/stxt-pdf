package dev.stxt.boxes.layout.inline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PdfMetricsTest {

    private final PdfMetrics metrics = new PdfMetrics();

    @Test
    void helveticaWidths() {
        // AFM de Helvetica: H 722, o 556, l 222, a 556.
        assertEquals(20.56, metrics.width("Hola", "Helvetica", 10), 1e-6);
        assertEquals(41.12, metrics.width("Hola", "Helvetica", 20), 1e-6);
    }

    @Test
    void otherStandardFontsAndAliases() {
        // Courier es monoespaciada: 600 por carácter.
        assertEquals(24, metrics.width("Hola", "Courier", 10), 1e-6);
        assertEquals(metrics.width("Hola", "Helvetica", 10), metrics.width("Hola", "Arial", 10), 1e-6);
    }

    @Test
    void unencodableCharactersMeasureAsQuestionMarks() {
        assertEquals(metrics.width("?", "Helvetica", 10), metrics.width("→", "Helvetica", 10), 1e-6);
        assertEquals("a?b", PdfMetrics.encodable(PdfMetrics.standardFont("Helvetica"), "a→b"));
        // Los acentos y la eñe están en WinAnsi.
        assertEquals("Página año", PdfMetrics.encodable(PdfMetrics.standardFont("Helvetica"), "Página año"));
    }

    @Test
    void unknownFont() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> metrics.width("Hola", "Comic Sans", 10));
        assertEquals("Fuente desconocida: Comic Sans (en la salida PDF se admiten las 14 fuentes estándar: "
                + "Helvetica, Times-Roman, Courier…)", e.getMessage());
    }
}
