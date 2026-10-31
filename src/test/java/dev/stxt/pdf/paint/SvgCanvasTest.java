package dev.stxt.pdf.paint;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.layout.Engine;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Las imágenes en la salida SVG (referencia/componentes/image.md). */
class SvgCanvasTest {

    private static final String DOC = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 300\n"
            + "\t\tMargin: 20\n\tImage: test/wide.png\n\t\tFit: fill\n\t\tBox:\n\t\t\tWidth: 100\n\t\t\tHeight: 100\n";

    private final LaidOutDocument document = new Engine().layOutDocument(new DocumentReader().read(Docs.sectioned(DOC)));

    @Test
    void imagesAreEmbeddedAndClippedToTheirContentBox() {
        String svg = SvgCanvas.page(document.pages().get(0), document.pageSetup(), false, new Resources());
        // La caja de contenido recorta; con fill, la imagen mide 200 × 100 y empieza 50 a la izquierda.
        assertTrue(svg.contains("<svg x=\"20.00\" y=\"20.00\" width=\"100.00\" height=\"100.00\">"
                + "<image x=\"-50.00\" y=\"0.00\" width=\"200.00\" height=\"100.00\" preserveAspectRatio=\"none\" "
                + "href=\"data:image/png;base64,"), svg);
    }

    @Test
    void svgImagesAreEmbeddedAsSvg() {
        LaidOutDocument vector = new Engine().layOutDocument(new DocumentReader().read(Docs.sectioned(
                "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 300\n"
                + "\t\tMargin: 20\n\tImage: test/shapes.svg\n")));
        String svg = SvgCanvas.page(vector.pages().get(0), vector.pageSetup(), false, new Resources());
        assertTrue(svg.contains("<image x=\"0.00\" y=\"0.00\" width=\"150.00\" height=\"75.00\" "
                + "preserveAspectRatio=\"none\" href=\"data:image/svg+xml;base64,"), svg);
    }

    @Test
    void letterSpacingIsAnAttributeOfTheText() {
        LaidOutDocument spaced = new Engine().layOutDocument(new DocumentReader().read(Docs.sectioned(
                "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 300\n"
                + "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + "\tText: Hola\n\t\tTypography:\n\t\t\tLetter spacing: 1.5\n"
                + "\tText: Adiós\n")));
        String svg = SvgCanvas.page(spaced.pages().get(0), spaced.pageSetup(), false);
        assertTrue(svg.contains("letter-spacing=\"1.50\">Hola</text>"), svg);
        assertFalse(svg.contains("letter-spacing=\"0"), svg);
    }

    @Test
    void withoutResourcesOnlyTheBoxIsPainted() {
        String svg = SvgCanvas.page(document.pages().get(0), document.pageSetup(), true);
        assertFalse(svg.contains("<image"), svg);
    }

    /**
     * Una caja girada es un grupo con {@code rotate(ángulo x y)} alrededor de su origen
     * (referencia/comun/capas-y-giro.md).
     */
    @Test
    void rotationIsAGroupTransform() {
        LaidOutDocument turned = new Engine().layOutDocument(new DocumentReader().read(Docs.sectioned(
                "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 300\n"
                + "\t\tMargin: 20\n\tOverlay:\n\t\tOffset: 100 auto auto 100\n\t\tRotate: -12.5\n"
                + "\t\tRotate origin: bottom\n\t\tBox:\n\t\t\tWidth: 40\n\t\t\tHeight: 10\n\t\t\tBackground: #F00\n")));
        String svg = SvgCanvas.page(turned.pages().get(0), turned.pageSetup(), false);
        // La caja va de (120, 120) a (160, 130): el centro de abajo es (140, 130).
        assertTrue(svg.contains("<g transform=\"rotate(-12.5 140.00 130.00)\">\n<rect x=\"120.00\" y=\"120.00\""), svg);
        assertTrue(svg.contains("</g>"), svg);
    }

    /** El SVG solo dibuja: no marca el texto ni crea enlaces, y lo dice con {@link Tagging#NONE}. */
    @Test
    void theSvgCanvasOnlyDraws() {
        assertSame(Tagging.NONE, new SvgCanvas(false).tagging());
    }
}
