package dev.stxt.pdf.paint;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.OutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

/** The transparency groups of a page in the PDF ({@link PdfContent}, reference/implementation/painting.md). */
class PdfContentTest {

    /** Inside a group, the stream is that of its form; when it ends, the one that was being written before. */
    @Test
    void aGroupWritesToItsFormUntilItEnds() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream out = new PDPageContentStream(doc, page)) {
                PdfContent content = new PdfContent(doc, out, PDRectangle.A4, PDRectangle.A4.getHeight());
                content.beginGroup(0.5);
                PDPageContentStream outer = content.out();
                assertNotSame(out, outer);
                content.beginGroup(0.2);
                assertNotSame(outer, content.out());
                content.endGroup();
                assertSame(outer, content.out());
                content.endGroup();
                assertSame(out, content.out());
            }
            assertDoesNotThrow(() -> doc.save(OutputStream.nullOutputStream()));
        }
    }

    /**
     * A failure while painting inside two groups leaves their forms open: abandoning them closes their streams and
     * goes back to that of the page, which whoever opened it can close.
     */
    @Test
    void abandoningTheGroupsLeftOpenGoesBackToTheStreamOfThePage() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream out = new PDPageContentStream(doc, page)) {
                PdfContent content = new PdfContent(doc, out, PDRectangle.A4, PDRectangle.A4.getHeight());
                content.beginGroup(0.5);
                content.beginGroup(0.2);
                content.abandonGroups();
                assertSame(out, content.out());
                // With no group open, it does nothing.
                content.abandonGroups();
                assertSame(out, content.out());
            }
            assertDoesNotThrow(() -> doc.save(OutputStream.nullOutputStream()));
        }
    }
}
