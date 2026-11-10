package dev.stxt.pdf.paint;

import static dev.stxt.pdf.PdfStructures.document;
import static dev.stxt.pdf.PdfStructures.types;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDPageLabelRange;
import org.apache.pdfbox.pdmodel.common.PDPageLabels;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.Pdfs;
import dev.stxt.pdf.stxt.DocumentReader;

/**
 * The PDF document (reference/implementation/pdf.md): one PDF page per page of the result, with the size of the paper
 * of its section, and the page labels with the printed numbers (reference/components/document.md).
 */
class PdfDocumentWriterTest {

    private static final String DOCUMENT = Docs.header(300, 200, 20) + Docs.TYPOGRAPHY;

    /** A document with sections, and a style that changes the language. */
    private static final String SECTIONS_HEAD = DOCUMENT
            + "\t\tFor style: english\n\t\t\tTypography:\n\t\t\t\tLanguage: en\n";

    private final DocumentReader reader = new DocumentReader();

    @Test
    void reportExample() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(reader.read(Path.of("test", "spec", "report.stxt"))))) {
            assertEquals(1, pdf.getNumberOfPages());
            PDRectangle box = pdf.getPage(0).getMediaBox();
            assertEquals(210 * 72 / 25.4, box.getWidth(), 1e-3);
            assertEquals(297 * 72 / 25.4, box.getHeight(), 1e-3);
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("Resultados del trimestre"), text);
            assertTrue(text.contains("Informe trimestral"), text);
            assertTrue(text.contains("BORRADOR"), text);
            // Justified paragraph: the extra space goes in TJ, and the text is extracted the same.
            assertTrue(text.contains("Primer párrafo del informe."), text);
        }
    }

    @Test
    void onePdfPagePerPage() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(DOCUMENT
                + "\tText: Uno\n\tText: Dos\n\t\tBreak:\n\t\t\tBefore: page\n"))) {
            assertEquals(2, pdf.getNumberOfPages());
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(2);
            stripper.setEndPage(2);
            assertEquals("Dos", stripper.getText(pdf).strip());
        }
    }

    @Test
    void pageHasTheSizeOfThePageSetup() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(Docs.header(300, 842, 20)
                + "\tBlock:\n\t\tBox:\n\t\t\tHeight: 500\n\t\t\tBackground: #F1EFE8\n"))) {
            assertEquals(1, pdf.getNumberOfPages());
            assertEquals(300, pdf.getPage(0).getMediaBox().getWidth(), 1e-3);
            assertEquals(842, pdf.getPage(0).getMediaBox().getHeight(), 1e-3);
        }
    }

    /**
     * The page labels of the PDF are the printed number (reference/components/document.md): none on the
     * cover, and the appendix restarts.
     */
    @Test
    void pageLabelsAreThePrintedNumbers() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(SECTIONS_HEAD
                + "\tSection:\n\t\tPage numbering: none\n\t\tText: Portada\n"
                + "\tSection:\n\t\tText: Uno\n\t\tText: Dos\n\t\t\tBreak:\n\t\t\t\tBefore: page\n"
                + "\tSection:\n\t\tPage numbering: restart\n\t\tText: Anexo\n"))) {
            assertEquals(4, pdf.getNumberOfPages());
            assertArrayEquals(new String[] {"", "1", "2", "1"},
                    pdf.getDocumentCatalog().getPageLabels().getLabelsByPageIndices());
        }
        // With nothing to change, no label: the printed number is the page number.
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(SECTIONS_HEAD
                + "\tSection:\n\t\tText: Uno\n\t\tText: Dos\n\t\t\tBreak:\n\t\t\t\tBefore: page\n"))) {
            assertNull(pdf.getDocumentCatalog().getPageLabels());
        }
    }

    /**
     * With {@code Page number format} and {@code First number}, the label carries the style and the number of each page
     * sequence (i, ii, 5, VI). The PDF entries are checked and not the text that PDFBox generates, which uppercases
     * with the language of the JVM: in Turkish, "vi" would be "Vİ".
     */
    @Test
    void pageLabelsKeepThePageStyleAndTheFirstNumber() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(SECTIONS_HEAD
                + "\tSection:\n\t\tPage number format: lower-roman\n\t\tText: Prefacio\n\t\tText: Más\n\t\t\tBreak:\n\t\t\t\tBefore: page\n"
                + "\tSection:\n\t\tPage numbering: restart\n\t\tFirst number: 5\n\t\tText: Cuerpo\n"
                + "\tSection:\n\t\tPage number format: upper-roman\n\t\tText: Anexo\n"))) {
            PDPageLabels labels = pdf.getDocumentCatalog().getPageLabels();
            List<String> ranges = new ArrayList<>();
            for (int index : new TreeSet<>(labels.getPageIndices())) {
                PDPageLabelRange range = labels.getPageLabelRange(index);
                ranges.add(index + ":" + range.getStyle() + ":" + range.getStart());
            }
            assertEquals(List.of("0:r:1", "2:D:5", "3:R:6"), ranges);
        }
    }

    /** Each PDF page has the size of its section, and each section is a {@code Sect} with its language. */
    @Test
    void eachSectionIsASectWithItsOwnPageSizeAndLanguage() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(SECTIONS_HEAD
                + "\tSection:\n\t\tText: Uno\n"
                + "\tSection:\n\t\tStyle: english\n\t\tPage:\n\t\t\tOrientation: landscape\n\t\t\tPaper: A4\n\t\tText: Two\n"))) {
            assertEquals(300, pdf.getPage(0).getMediaBox().getWidth(), 1e-3);
            assertEquals(297 * 72 / 25.4, pdf.getPage(1).getMediaBox().getWidth(), 1e-3);
            assertEquals(210 * 72 / 25.4, pdf.getPage(1).getMediaBox().getHeight(), 1e-3);
            PDStructureElement outer = document(pdf);
            assertEquals(List.of("Sect", "Sect"), types(outer));
            assertNull(((PDStructureElement) outer.getKids().get(0)).getLanguage());
            assertEquals("en", ((PDStructureElement) outer.getKids().get(1)).getLanguage());
        }
    }
}
