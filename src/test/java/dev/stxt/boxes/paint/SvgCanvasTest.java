package dev.stxt.boxes.paint;

import dev.stxt.boxes.Docs;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.LaidOutDocument;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Las imágenes en la salida SVG (ESPECIFICACION.md §5.18). */
class SvgCanvasTest {

    private static final String DOC = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tTitle: Prueba\n\tMedia:\n\t\tWidth: 300\n\t\tHeight: 300\n"
            + "\t\tMargin: 20\n\tImage: test/wide.png\n\t\tFit: cover\n\t\tBox:\n\t\t\tWidth: 100\n\t\t\tHeight: 100\n";

    private final LaidOutDocument document = new Engine().layOutDocument(new DocumentReader().read(Docs.sectioned(DOC)));

    @Test
    void imagesAreEmbeddedAndClippedToTheirContentBox() {
        String svg = SvgCanvas.page(document.pages().get(0), document.media(), false, new Resources());
        // La caja de contenido recorta; con cover, la imagen mide 200 × 100 y empieza 50 a la izquierda.
        assertTrue(svg.contains("<svg x=\"20.00\" y=\"20.00\" width=\"100.00\" height=\"100.00\">"
                + "<image x=\"-50.00\" y=\"0.00\" width=\"200.00\" height=\"100.00\" preserveAspectRatio=\"none\" "
                + "href=\"data:image/png;base64,"), svg);
    }

    @Test
    void svgImagesAreEmbeddedAsSvg() {
        LaidOutDocument vector = new Engine().layOutDocument(new DocumentReader().read(Docs.sectioned(
                "Document (dev.stxt.boxes):\n\tLanguage: es\n\tTitle: Prueba\n\tMedia:\n\t\tWidth: 300\n\t\tHeight: 300\n"
                + "\t\tMargin: 20\n\tImage: test/shapes.svg\n")));
        String svg = SvgCanvas.page(vector.pages().get(0), vector.media(), false, new Resources());
        assertTrue(svg.contains("<image x=\"0.00\" y=\"0.00\" width=\"150.00\" height=\"75.00\" "
                + "preserveAspectRatio=\"none\" href=\"data:image/svg+xml;base64,"), svg);
    }

    @Test
    void letterSpacingIsAnAttributeOfTheText() {
        LaidOutDocument spaced = new Engine().layOutDocument(new DocumentReader().read(Docs.sectioned(
                "Document (dev.stxt.boxes):\n\tLanguage: es\n\tTitle: Prueba\n\tMedia:\n\t\tWidth: 300\n\t\tHeight: 300\n"
                + "\tStyles:\n\t\tElement: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + "\tText: Hola\n\t\tTypography:\n\t\t\tLetter spacing: 1.5\n"
                + "\tText: Adiós\n")));
        String svg = SvgCanvas.page(spaced.pages().get(0), spaced.media(), false);
        assertTrue(svg.contains("letter-spacing=\"1.50\">Hola</text>"), svg);
        assertFalse(svg.contains("letter-spacing=\"0"), svg);
    }

    @Test
    void withoutResourcesOnlyTheBoxIsPainted() {
        String svg = SvgCanvas.page(document.pages().get(0), document.media(), true);
        assertFalse(svg.contains("<image"), svg);
    }

    /** Una caja girada es un grupo con {@code rotate(ángulo x y)} alrededor de su origen (§5.6). */
    @Test
    void rotationIsAGroupTransform() {
        LaidOutDocument turned = new Engine().layOutDocument(new DocumentReader().read(Docs.sectioned(
                "Document (dev.stxt.boxes):\n\tLanguage: es\n\tTitle: Prueba\n\tMedia:\n\t\tWidth: 300\n\t\tHeight: 300\n"
                + "\t\tMargin: 20\n\tAbsolute:\n\t\tOffset: 100 auto auto 100\n\t\tRotate: -12.5\n"
                + "\t\tRotate origin: bottom\n\t\tBox:\n\t\t\tWidth: 40\n\t\t\tHeight: 10\n\t\t\tBackground: #F00\n")));
        String svg = SvgCanvas.page(turned.pages().get(0), turned.media(), false);
        // La caja va de (120, 120) a (160, 130): el centro de abajo es (140, 130).
        assertTrue(svg.contains("<g transform=\"rotate(-12.5 140.00 130.00)\">\n<rect x=\"120.00\" y=\"120.00\""), svg);
        assertTrue(svg.contains("</g>"), svg);
    }

    /** El SVG solo dibuja (AR-05): no marca el texto ni crea enlaces, y lo dice con {@link Tagging#NONE}. */
    @Test
    void theSvgCanvasOnlyDraws() {
        assertSame(Tagging.NONE, new SvgCanvas(false).tagging());
    }
}
