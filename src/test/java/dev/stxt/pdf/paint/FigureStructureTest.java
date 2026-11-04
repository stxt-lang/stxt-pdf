package dev.stxt.pdf.paint;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.Pdfs;
import dev.stxt.pdf.golden.PdfUa;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDLayoutAttributeObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static dev.stxt.pdf.PdfStructures.kids;
import static dev.stxt.pdf.PdfStructures.section;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The figures in the tagged PDF (reference/implementation/pdf.md): a {@code Figure} with an image or a block is a
 * {@code Figure} with its {@code Alt}, its {@code BBox} and its {@code Caption}; with a table, the {@code Table}
 * carries the {@code Caption}. It passes PDF/UA-1 and PDF/UA-2.
 */
class FigureStructureTest {

    private static final String DOCUMENT = Docs.header(300, 300, 20) + Docs.TYPOGRAPHY
            + "\tTable of contents:\n\t\tOf: tables\n"
            + "\tFigure:\n\t\tCaption: Esquema\n\t\tImage: test/wide.png\n\t\t\tAlt: Paisaje\n\t\t\tBox:\n\t\t\t\tWidth: 100\n"
            + "\tFigure:\n\t\tCaption: Bornes\n\t\tTable:\n\t\t\tColumn sizes: *\n\t\t\tBody:\n\t\t\t\tRow:\n\t\t\t\t\tCell: a\n"
            + "\tFigure:\n\t\tAlt: Dos fotos\n\t\tCaption: Fotos\n\t\tBlock:\n\t\t\tText: Hola\n";

    @Test
    void aFigureHasItsAltItsBBoxAndItsCaption() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(DOCUMENT, 1))) {
            PDStructureElement section = section(pdf);
            assertEquals(List.of("TOC", "Figure", "Table", "Figure"), kids(section));
            PDStructureElement image = (PDStructureElement) section.getKids().get(1);
            assertEquals("Paisaje", image.getAlternateDescription());
            // The caption and the content of the image, which has no element of its own.
            assertEquals(List.of("Caption", "MCR"), kids(image));
            assertEquals(List.of("MCR"), kids((PDStructureElement) image.getKids().get(0)));
            PDLayoutAttributeObject layout = (PDLayoutAttributeObject) image.getAttributes().getObject(0);
            assertNotNull(layout.getBBox());
            // The table carries its caption, first because it is above.
            PDStructureElement table = (PDStructureElement) section.getKids().get(2);
            assertEquals(List.of("Caption", "TBody"), kids(table));
            // The figure with a block: its own Alt, the block as a Div and the caption after it.
            PDStructureElement block = (PDStructureElement) section.getKids().get(3);
            assertEquals("Dos fotos", block.getAlternateDescription());
            assertEquals(List.of("Div", "Caption"), kids(block));
            // A block is not an image: its Figure has no BBox.
            assertTrue(block.getCOSObject().getItem(COSName.A) == null);
        }
    }

    /** With PDF/UA-2, the entry of a table of tables points with {@code Ref} to the {@code Table} of its figure. */
    @Test
    void itPassesPdfUa1AndPdfUa2() throws Exception {
        assertEquals(Map.of(), PdfUa.failedRules(Pdfs.of(DOCUMENT, 1), 1));
        byte[] ua2 = Pdfs.of(DOCUMENT, 2);
        assertEquals(Map.of(), PdfUa.failedRules(ua2, 2));
        try (PDDocument pdf = Loader.loadPDF(ua2)) {
            PDStructureElement toc = (PDStructureElement) section(pdf).getKids().get(0);
            PDStructureElement entry = (PDStructureElement) toc.getKids().get(0);
            PDStructureElement table = (PDStructureElement) section(pdf).getKids().get(2);
            assertEquals(table.getCOSObject(), entry.getCOSObject().getCOSArray(COSName.getPDFName("Ref")).getObject(0));
        }
    }
}
