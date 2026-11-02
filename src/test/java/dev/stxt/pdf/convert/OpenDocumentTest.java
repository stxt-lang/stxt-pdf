package dev.stxt.pdf.convert;

import dev.stxt.InlineNode;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.golden.PdfUa;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.model.Attachment;
import dev.stxt.pdf.model.Attachment.Relationship;
import dev.stxt.pdf.model.Attachments;
import dev.stxt.pdf.model.Cancellation;
import dev.stxt.pdf.model.PdfA;
import dev.stxt.pdf.model.Progress;
import dev.stxt.pdf.stxt.DocumentReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Writing a document into an open PDFBox document (reference/implementation/api.md): the same PDF as into a file,
 * except that the caller adds what the engine does not write and saves it.
 */
class OpenDocumentTest {

    private static final DocumentReader READER = new DocumentReader();
    private static final String HEADER = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 300\n";
    private static final String BASE = "Styles (dev.stxt.pdf):\n\tFor element: Document\n\t\tTypography:\n\t\t\tFont: IBM Plex Sans\n"
            + "\t\t\tSize: 10\n\t\t\tMarkup: markdown\n";

    private static InlineNode document(String... lines) {
        return READER.read(Docs.sectioned(HEADER + String.join("\n", lines) + "\n"));
    }

    /** Two pages with text, a heading with a link to it, a bitmap and a page of a PDF. */
    private static InlineNode everything() {
        return document("\tHeading 1:", "\t\tId: uno", "\t\tText: Uno",
                "\tParagraph: Un texto con un [enlace](#dos) y un [cuadrado](https://stxt.dev).",
                "\tImage: test/square.png", "\t\tAlt: Un cuadrado", "\t\tBox:", "\t\t\tWidth: 40",
                "\tImage: test/pages.pdf", "\t\tPdf page: 1", "\t\tAlt: La página 1", "\t\tBox:", "\t\t\tHeight: 80",
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 200",
                "\tHeading 1:", "\t\tId: dos", "\t\tText: Dos");
    }

    private static byte[] saved(PDDocument doc) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.save(out);
        return out.toByteArray();
    }

    @Test
    void theCallerAddsItsOwnKeysAndSavesAValidPdf() throws IOException {
        Converter converter = Converter.builder().styles(BASE).build();
        byte[] pdf;
        try (PDDocument target = new PDDocument()) {
            converter.toPdf(everything(), target);
            // What the engine does not write: a key for a document management system (pdf_gen's Meta).
            target.getDocumentInformation().setCustomMetadataValue("poliza", "123");
            pdf = saved(target);
        }
        try (PDDocument read = Loader.loadPDF(pdf)) {
            assertEquals(2, read.getNumberOfPages());
            assertEquals("Prueba", read.getDocumentInformation().getTitle());
            assertEquals("123", read.getDocumentInformation().getCustomMetadataValue("poliza"));
            assertEquals("es", read.getDocumentCatalog().getLanguage());
            assertNotNull(read.getDocumentCatalog().getStructureTreeRoot(), "tagged");
            assertNotNull(read.getDocumentCatalog().getDocumentOutline(), "bookmarks");
            assertTrue(new PDFTextStripper().getText(read).contains("Un texto con un enlace"));
        }
        assertEquals(Map.of(), PdfUa.failedRules(pdf, 1));
    }

    /** The same pages and text as the file; only the identifier, which PDFBox gives when saving, differs. */
    @Test
    void itIsTheSameDocumentAsTheFile() throws IOException {
        Converter converter = Converter.builder().styles(BASE).build();
        LaidOutDocument laidOut = converter.layOut(everything());
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        converter.toPdf(laidOut, file);
        byte[] open;
        try (PDDocument target = new PDDocument()) {
            converter.toPdf(laidOut, target);
            open = saved(target);
        }
        try (PDDocument a = Loader.loadPDF(file.toByteArray()); PDDocument b = Loader.loadPDF(open)) {
            assertEquals(a.getNumberOfPages(), b.getNumberOfPages());
            assertEquals(new PDFTextStripper().getText(a), new PDFTextStripper().getText(b));
            assertEquals(a.getPage(0).getAnnotations().size(), b.getPage(0).getAnnotations().size());
        }
    }

    @Test
    void pdfAAndAttachmentsGoWithIt() throws IOException {
        Converter converter = Converter.builder().styles(BASE).pdfA(PdfA.A3B).build();
        Attachments attachments = Attachments.of(Attachment.of("data.csv", "a;b\n".getBytes(StandardCharsets.UTF_8),
                "text/csv", Relationship.DATA));
        byte[] pdf;
        try (PDDocument target = new PDDocument()) {
            converter.toPdf(everything(), target, attachments, Cancellation.NONE, Progress.NONE);
            pdf = saved(target);
        }
        assertEquals(Map.of(), PdfUa.failedRules(pdf, PdfUa.flavour(PdfA.A3B)));
        try (PDDocument read = Loader.loadPDF(pdf)) {
            assertNotNull(read.getDocumentCatalog().getNames().getEmbeddedFiles().getNames().get("data.csv"));
        }
    }

    @Test
    void aDocumentWithPagesIsRefused() throws IOException {
        Converter converter = Converter.builder().styles(BASE).build();
        try (PDDocument target = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            target.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(target, page, AppendMode.APPEND, true)) {
                cs.addRect(10, 10, 100, 100);
                cs.fill();
            }
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> converter.toPdf(everything(), target));
            assertTrue(e.getMessage().contains("PdfPainter"), e.getMessage());
            assertEquals(1, target.getNumberOfPages());
        }
    }
}
