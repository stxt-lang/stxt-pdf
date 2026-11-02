package dev.stxt.pdf.paint;

import dev.stxt.InlineNode;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.golden.PdfUa;
import dev.stxt.pdf.stxt.DocumentReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkedContentReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDLayoutAttributeObject;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDListAttributeObject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

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

    private static final String DOCUMENT = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 300\n"
            + "\t\tMargin: 20\n"
            + "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
            + "\t\tFor element: List\n\t\t\tBox:\n\t\t\t\tPadding: 0 0 0 20\n"
            + "\tList:\n\t\tMarker: image\n\t\tMarker image: test/check.svg\n\t\t\tAlt: Incluido\n"
            + "\t\tList item:\n\t\t\tText: Robo\n"
            + "\t\tList item:\n\t\t\tMarker image: test/cross.svg\n\t\t\tText: Lunas\n";

    private static byte[] render(int ua) throws Exception {
        InlineNode document = new DocumentReader().read(Docs.sectioned(DOCUMENT));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.builder().pdfUa(ua).build().toPdf(document, out);
        return out.toByteArray();
    }

    private static List<String> types(PDStructureElement e) {
        return e.getKids().stream().map(k -> k instanceof PDStructureElement s ? s.getStructureType()
                : k instanceof PDMarkedContentReference ? "MCR" : k.getClass().getSimpleName()).toList();
    }

    private static PDStructureElement list(PDDocument pdf) {
        PDStructureTreeRoot root = pdf.getDocumentCatalog().getStructureTreeRoot();
        PDStructureElement document = (PDStructureElement) root.getKids().get(0);
        PDStructureElement section = (PDStructureElement) document.getKids().get(0);
        return (PDStructureElement) section.getKids().get(0);
    }

    @Test
    void anImageMarkerWithAltIsAFigureInTheLabelAndWithoutAltThereIsNoLabel() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(render(1))) {
            PDStructureElement list = list(pdf);
            assertEquals("L", list.getStructureType());
            assertEquals(PDListAttributeObject.LIST_NUMBERING_NONE,
                    ((PDListAttributeObject) list.getAttributes().getObject(0)).getListNumbering());
            assertEquals(List.of("LI", "LI"), types(list));
            PDStructureElement first = (PDStructureElement) list.getKids().get(0);
            assertEquals(List.of("Lbl", "LBody"), types(first));
            PDStructureElement label = (PDStructureElement) first.getKids().get(0);
            assertEquals(List.of("Figure"), types(label));
            PDStructureElement figure = (PDStructureElement) label.getKids().get(0);
            assertEquals("Incluido", figure.getAlternateDescription());
            assertEquals(List.of("MCR"), types(figure));
            PDLayoutAttributeObject layout = (PDLayoutAttributeObject) figure.getAttributes().getObject(0);
            assertNotNull(layout.getBBox());
            // The second item replaces the image with a decorative one: no Lbl.
            PDStructureElement second = (PDStructureElement) list.getKids().get(1);
            assertEquals(List.of("LBody"), types(second));
            String content = new String(pdf.getPage(0).getContents().readAllBytes(), "ISO-8859-1");
            assertTrue(content.contains("/Figure <</MCID"), content);
            assertTrue(content.contains("/Artifact BMC"), content);
        }
    }

    @Test
    void itPassesPdfUa1AndPdfUa2() throws Exception {
        assertEquals(Map.of(), PdfUa.failedRules(render(1), 1));
        byte[] ua2 = render(2);
        assertEquals(Map.of(), PdfUa.failedRules(ua2, 2));
        try (PDDocument pdf = Loader.loadPDF(ua2)) {
            assertEquals("Unordered", ((PDListAttributeObject) list(pdf).getAttributes().getObject(0)).getListNumbering());
        }
    }
}
