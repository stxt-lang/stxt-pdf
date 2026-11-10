package dev.stxt.pdf.paint;

import static dev.stxt.pdf.PdfStructures.section;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDLayoutAttributeObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.Pdfs;

/**
 * The canvas of a PDF page: the colors it writes and the rotation of a box, a {@code cm} matrix that turns its content
 * and what hangs from it. The fonts, the images, the tagging, the structure, the links, the bookmarks, the metadata
 * and the pages of the document have their own tests ({@link PdfFontsTest}, {@link PdfImagesTest},
 * {@link PdfTaggingTest}, {@link PdfStructureTest}, {@link PdfLinksTest}, {@link PdfOutlineTest},
 * {@link PdfMetadataTest}, {@link PdfDocumentWriterTest}).
 */
class PdfCanvasTest {

    @Test
    void colors() {
        assertArrayEquals(new float[]{1, 0, 0}, PdfCanvas.rgb("#F00"));
        assertArrayEquals(new float[]{0xA3 / 255f, 0x2D / 255f, 0x2D / 255f}, PdfCanvas.rgb("#A32D2D"));
    }

    /**
     * A rotated box (reference/common/layers-and-rotation.md) rotates its content with a {@code cm} matrix; its link is
     * the rectangle that encloses the rotated area with the {@code QuadPoints} of the area, the {@code BBox} of a
     * rotated figure encloses what is painted, and a destination leads to the corner of that enclosing rectangle.
     * Rotated text is still text.
     */
    @Test
    void rotatedBoxesTurnTheirContentLinksAndFigures() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(Docs.header(300, 300, 40) + Docs.TYPOGRAPHY
                + "\tImage: test/wide.png\n\t\tAlt: Paisaje\n\t\tRotate: 90\n"
                + "\tText: Ir al sello\n\t\tLink: #sello\n"
                + "\tOverlay:\n\t\tId: sello\n\t\tOffset: 60 auto auto 60\n\t\tRotate: 90\n\t\tLink: https://stxt.dev\n"
                + "\t\tBox:\n\t\t\tWidth: 100\n\t\t\tHeight: 20\n\t\tText: Sello\n"))) {
            List<PDAnnotationLink> links = pdf.getPage(0).getAnnotations().stream()
                    .map(a -> (PDAnnotationLink) a).toList();
            PDAnnotationLink stamp = links.stream().filter(l -> l.getAction() != null).findFirst().orElseThrow();
            // The box goes from (100, 100) to (200, 120); rotated 90 degrees around (150, 110), from
            // (140, 60) to (160, 160): with the origin at the bottom, from y = 140 to y = 240.
            PDRectangle area = stamp.getRectangle();
            assertEquals(140, area.getLowerLeftX(), 0.01);
            assertEquals(140, area.getLowerLeftY(), 0.01);
            assertEquals(20, area.getWidth(), 0.01);
            assertEquals(100, area.getHeight(), 0.01);
            float[] expected = {140, 240, 140, 140, 160, 140, 160, 240};
            float[] quad = stamp.getQuadPoints();
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], quad[i], 0.01, "QuadPoints " + i);
            }

            // The destination of the internal link: the top left corner of the enclosing rectangle.
            PDAnnotationLink internal = links.stream().filter(l -> l.getAction() == null).findFirst().orElseThrow();
            PDPageXYZDestination target = (PDPageXYZDestination) internal.getDestination();
            assertEquals(140, target.getLeft());
            assertEquals(240, target.getTop());

            // The image, 150 × 75 and rotated 90 in flow, takes 75 × 150 at (40, 40): with the origin at the bottom,
            // from y = 110 to y = 260.
            PDStructureElement figure = (PDStructureElement) section(pdf).getKids().get(0);
            assertEquals("Figure", figure.getStructureType());
            PDRectangle box = ((PDLayoutAttributeObject) figure.getAttributes().getObject(0)).getBBox();
            assertEquals(40, box.getLowerLeftX(), 0.01);
            assertEquals(110, box.getLowerLeftY(), 0.01);
            assertEquals(75, box.getWidth(), 0.01);
            assertEquals(150, box.getHeight(), 0.01);

            String content = new String(pdf.getPage(0).getContents().readAllBytes(), StandardCharsets.ISO_8859_1);
            assertTrue(content.contains(" cm\n"), content);
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("Sello"), text);
        }
    }
}
