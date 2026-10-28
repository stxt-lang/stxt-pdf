package dev.stxt.boxes.paint;

import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.LaidOutDocument;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Las imágenes en la salida SVG (ESPECIFICACION.md §5.18). */
class SvgCanvasTest {

    private static final String DOC = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 300\n\t\tHeight: 300\n"
            + "\t\tMargin: 20\n\tImage: test/wide.png\n\t\tFit: cover\n\t\tBox:\n\t\t\tWidth: 100\n\t\t\tHeight: 100\n";

    private final LaidOutDocument document = new Engine().layOutDocument(new DocumentReader().read(DOC));

    @Test
    void imagesAreEmbeddedAndClippedToTheirContentBox() {
        String svg = SvgCanvas.page(document.pages().get(0), document.media(), false, new Resources());
        // La caja de contenido recorta; con cover, la imagen mide 200 × 100 y empieza 50 a la izquierda.
        assertTrue(svg.contains("<svg x=\"20.00\" y=\"20.00\" width=\"100.00\" height=\"100.00\">"
                + "<image x=\"-50.00\" y=\"0.00\" width=\"200.00\" height=\"100.00\" preserveAspectRatio=\"none\" "
                + "href=\"data:image/png;base64,"), svg);
    }

    @Test
    void letterSpacingIsAnAttributeOfTheText() {
        LaidOutDocument spaced = new Engine().layOutDocument(new DocumentReader().read(
                "Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 300\n\t\tHeight: 300\n"
                + "\tStyles:\n\t\tElement: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + "\tText: Hola\n\t\tTypography:\n\t\t\tLetter spacing: 1.5\n"
                + "\tText: Adiós\n"));
        String svg = SvgCanvas.page(spaced.pages().get(0), spaced.media(), false);
        assertTrue(svg.contains("letter-spacing=\"1.50\">Hola</text>"), svg);
        assertFalse(svg.contains("letter-spacing=\"0"), svg);
    }

    @Test
    void withoutResourcesOnlyTheBoxIsPainted() {
        String svg = SvgCanvas.page(document.pages().get(0), document.media(), true);
        assertFalse(svg.contains("<image"), svg);
    }
}
