package dev.stxt.pdf.paint;

import dev.stxt.InlineNode;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.golden.PdfUa;
import dev.stxt.pdf.stxt.DocumentReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkedContentReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDLayoutAttributeObject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The figures in the tagged PDF (reference/implementation/pdf.md): a {@code Figure} with an image or a block is a
 * {@code Figure} with its {@code Alt}, its {@code BBox} and its {@code Caption}; with a table, the {@code Table}
 * carries the {@code Caption}. It passes PDF/UA-1 and PDF/UA-2.
 */
class FigureStructureTest {

    private static final String DOCUMENT = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 300\n"
            + "\t\tMargin: 20\n"
            + "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
            + "\tTable of contents:\n\t\tOf: tables\n"
            + "\tFigure:\n\t\tCaption: Esquema\n\t\tImage: test/wide.png\n\t\t\tAlt: Paisaje\n\t\t\tBox:\n\t\t\t\tWidth: 100\n"
            + "\tFigure:\n\t\tCaption: Bornes\n\t\tTable:\n\t\t\tColumn sizes: *\n\t\t\tBody:\n\t\t\t\tRow:\n\t\t\t\t\tCell: a\n"
            + "\tFigure:\n\t\tAlt: Dos fotos\n\t\tCaption: Fotos\n\t\tBlock:\n\t\t\tText: Hola\n";

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

    private static PDStructureElement section(PDDocument pdf) {
        PDStructureTreeRoot root = pdf.getDocumentCatalog().getStructureTreeRoot();
        PDStructureElement document = (PDStructureElement) root.getKids().get(0);
        return (PDStructureElement) document.getKids().get(0);
    }

    @Test
    void aFigureHasItsAltItsBBoxAndItsCaption() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(render(1))) {
            PDStructureElement section = section(pdf);
            assertEquals(List.of("TOC", "Figure", "Table", "Figure"), types(section));
            PDStructureElement image = (PDStructureElement) section.getKids().get(1);
            assertEquals("Paisaje", image.getAlternateDescription());
            // The caption and the content of the image, which has no element of its own.
            assertEquals(List.of("Caption", "MCR"), types(image));
            assertEquals(List.of("MCR"), types((PDStructureElement) image.getKids().get(0)));
            PDLayoutAttributeObject layout = (PDLayoutAttributeObject) image.getAttributes().getObject(0);
            assertNotNull(layout.getBBox());
            // The table carries its caption, first because it is above.
            PDStructureElement table = (PDStructureElement) section.getKids().get(2);
            assertEquals(List.of("Caption", "TBody"), types(table));
            // The figure with a block: its own Alt, the block as a Div and the caption after it.
            PDStructureElement block = (PDStructureElement) section.getKids().get(3);
            assertEquals("Dos fotos", block.getAlternateDescription());
            assertEquals(List.of("Div", "Caption"), types(block));
            // A block is not an image: its Figure has no BBox.
            assertTrue(block.getCOSObject().getItem(COSName.A) == null);
        }
    }

    /** With PDF/UA-2, the entry of a table of tables points with {@code Ref} to the {@code Table} of its figure. */
    @Test
    void itPassesPdfUa1AndPdfUa2() throws Exception {
        assertEquals(Map.of(), PdfUa.failedRules(render(1), 1));
        byte[] ua2 = render(2);
        assertEquals(Map.of(), PdfUa.failedRules(ua2, 2));
        try (PDDocument pdf = Loader.loadPDF(ua2)) {
            PDStructureElement toc = (PDStructureElement) section(pdf).getKids().get(0);
            PDStructureElement entry = (PDStructureElement) toc.getKids().get(0);
            PDStructureElement table = (PDStructureElement) section(pdf).getKids().get(2);
            assertEquals(table.getCOSObject(), entry.getCOSObject().getCOSArray(COSName.getPDFName("Ref")).getObject(0));
        }
    }
}
