package dev.stxt.pdf.paint;

import static dev.stxt.pdf.PdfStructures.section;
import static dev.stxt.pdf.PdfStructures.types;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.junit.jupiter.api.Test;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.Pdfs;
import dev.stxt.pdf.golden.PdfUa;

/**
 * The block quotes and the horizontal rules of a {@code Markdown} in the tagged PDF (reference/common/pdf.md): a quote
 * is a {@code BlockQuote} with what it contains, and a rule is painted as an artifact, with no element. veraPDF
 * validates both parts of PDF/UA.
 */
class PdfBlockQuoteTest {

    private static final String DOCUMENT = Docs.header(300, 200, 20) + Docs.TYPOGRAPHY
            + "\t\tFor element: List\n\t\t\tBox:\n\t\t\t\tPadding: 0 0 0 16\n"
            + "\tMarkdown:\n\t\tBullet markers: bullet\n\t\tContent >>\n"
            + "\t\t\tAntes de la cita.\n\n"
            + "\t\t\t> La primera línea de la cita.\n"
            + "\t\t\t>\n"
            + "\t\t\t> - con una lista\n\n"
            + "\t\t\t---\n\n"
            + "\t\t\tDespués de la línea.\n";

    @Test
    void aQuoteIsABlockQuoteAndARuleHasNoElement() throws Exception {
        byte[] pdf = Pdfs.of(DOCUMENT);
        assertEquals(Map.of(), PdfUa.failedRules(pdf));
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDStructureElement markdown = (PDStructureElement) section(doc).getKids().get(0);
            // The paragraph, the quote and the last paragraph: the rule between them is not content.
            assertEquals(List.of("P", "BlockQuote", "P"), types(markdown));
            PDStructureElement quote = (PDStructureElement) markdown.getKids().get(1);
            assertEquals(List.of("P", "L"), types(quote));
        }
    }

    @Test
    void withPdfUa2TheBlockQuoteStaysInThePdf17Namespace() throws Exception {
        byte[] pdf = Pdfs.of(DOCUMENT, 2);
        assertEquals(Map.of(), PdfUa.failedRules(pdf, 2));
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDStructureElement markdown = (PDStructureElement) section(doc).getKids().get(0);
            assertEquals(List.of("P", "BlockQuote", "P"), types(markdown));
            // BlockQuote is not in the PDF 2.0 namespace: it stays in the default one; a paragraph is in that of 2.0.
            PDStructureElement quote = (PDStructureElement) markdown.getKids().get(1);
            assertNull(quote.getCOSObject().getDictionaryObject("NS"));
            assertNotNull(((PDStructureElement) markdown.getKids().get(0)).getCOSObject().getDictionaryObject("NS"));
        }
    }
}
