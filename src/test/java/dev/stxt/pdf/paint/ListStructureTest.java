package dev.stxt.pdf.paint;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.Pdfs;
import dev.stxt.pdf.golden.PdfUa;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDLayoutAttributeObject;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDListAttributeObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static dev.stxt.pdf.PdfStructures.kids;
import static dev.stxt.pdf.PdfStructures.section;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The image markers of a list in the tagged PDF (reference/implementation/pdf.md): with {@code Alt}, the {@code Lbl}
 * of the item holds a {@code Figure} with the alternative text and the {@code BBox} of the image; without it, the
 * image is an artifact and the item has no {@code Lbl}. The list is {@code ListNumbering: None} (PDF/UA-1) or
 * {@code Unordered} (PDF/UA-2), and it passes both.
 */
class ListStructureTest {

    private static final String DOCUMENT = Docs.header(300, 300, 20) + Docs.TYPOGRAPHY
            + "\t\tFor element: List\n\t\t\tBox:\n\t\t\t\tPadding: 0 0 0 20\n"
            + "\tList:\n\t\tMarker: image\n\t\tMarker image: test/check.svg\n\t\t\tAlt: Incluido\n"
            + "\t\tList item:\n\t\t\tText: Robo\n"
            + "\t\tList item:\n\t\t\tMarker image: test/cross.svg\n\t\t\tText: Lunas\n";

    private static PDStructureElement list(PDDocument pdf) {
        return (PDStructureElement) section(pdf).getKids().get(0);
    }

    @Test
    void anImageMarkerWithAltIsAFigureInTheLabelAndWithoutAltThereIsNoLabel() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(DOCUMENT, 1))) {
            PDStructureElement list = list(pdf);
            assertEquals("L", list.getStructureType());
            assertEquals(PDListAttributeObject.LIST_NUMBERING_NONE,
                    ((PDListAttributeObject) list.getAttributes().getObject(0)).getListNumbering());
            assertEquals(List.of("LI", "LI"), kids(list));
            PDStructureElement first = (PDStructureElement) list.getKids().get(0);
            assertEquals(List.of("Lbl", "LBody"), kids(first));
            PDStructureElement label = (PDStructureElement) first.getKids().get(0);
            assertEquals(List.of("Figure"), kids(label));
            PDStructureElement figure = (PDStructureElement) label.getKids().get(0);
            assertEquals("Incluido", figure.getAlternateDescription());
            assertEquals(List.of("MCR"), kids(figure));
            PDLayoutAttributeObject layout = (PDLayoutAttributeObject) figure.getAttributes().getObject(0);
            assertNotNull(layout.getBBox());
            // The second item replaces the image with a decorative one: no Lbl.
            PDStructureElement second = (PDStructureElement) list.getKids().get(1);
            assertEquals(List.of("LBody"), kids(second));
            String content = new String(pdf.getPage(0).getContents().readAllBytes(), "ISO-8859-1");
            assertTrue(content.contains("/Figure <</MCID"), content);
            assertTrue(content.contains("/Artifact BMC"), content);
        }
    }

    @Test
    void itPassesPdfUa1AndPdfUa2() throws Exception {
        assertEquals(Map.of(), PdfUa.failedRules(Pdfs.of(DOCUMENT, 1), 1));
        byte[] ua2 = Pdfs.of(DOCUMENT, 2);
        assertEquals(Map.of(), PdfUa.failedRules(ua2, 2));
        try (PDDocument pdf = Loader.loadPDF(ua2)) {
            assertEquals("Unordered", ((PDListAttributeObject) list(pdf).getAttributes().getObject(0)).getListNumbering());
        }
    }
}
