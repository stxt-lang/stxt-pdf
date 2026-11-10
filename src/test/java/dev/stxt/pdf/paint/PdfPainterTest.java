package dev.stxt.pdf.paint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import dev.stxt.InlineNode;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.model.Cancellation;
import dev.stxt.pdf.model.CancelledException;
import dev.stxt.pdf.model.Limits;
import dev.stxt.pdf.resources.Resources;

/**
 * {@link PdfPainter}: a laid out page painted on a page of a document that is not the engine's
 * (reference/implementation/api.md): after what the page already has, without tagging, with the fonts and images
 * embedded once, and with the internal links resolved among the pages painted with the same painter.
 */
class PdfPainterTest {

    private static final Resources RESOURCES = new Resources();
    private static final String HEADER = Docs.header(300, 300, 20);
    private static final String BASE = Docs.STYLES + "\t\t\tMarkup: markdown\n";

    private static LaidOutDocument laidOut(String... lines) {
        InlineNode document = Docs.read(HEADER, lines);
        return Converter.builder().styles(BASE).resources(RESOURCES).build().layOut(document);
    }

    /** Two pages: a text with a link to the heading of the second page and to a URL, and that heading. */
    private static LaidOutDocument twoPages() {
        return laidOut("\tHeading 1:", "\t\tId: uno", "\t\tText: Uno",
                "\tParagraph: Ver [dos](#dos) y [la web](https://stxt.dev).",
                "\tImage: test/square.png", "\t\tAlt: Un cuadrado", "\t\tBox:", "\t\t\tWidth: 40",
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 200",
                "\tHeading 1:", "\t\tId: dos", "\t\tText: Dos");
    }

    /** A document with {@code pages} A4 pages, each with a filled rectangle (a form). */
    private static PDDocument form(int pages) throws IOException {
        PDDocument doc = new PDDocument();
        for (int i = 0; i < pages; i++) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page, AppendMode.APPEND, true)) {
                cs.setNonStrokingColor(0.9f);
                cs.addRect(50, 50, 200, 100);
                cs.fill();
            }
        }
        return doc;
    }

    private static byte[] saved(PDDocument doc) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.save(out);
        return out.toByteArray();
    }

    @Test
    void paintsAfterWhatThePageHasWithoutTagging() throws IOException {
        LaidOutDocument document = twoPages();
        byte[] pdf;
        try (PDDocument doc = form(1)) {
            try (PdfPainter painter = new PdfPainter(doc, RESOURCES, Limits.DEFAULT)) {
                painter.paint(document.pages().get(0), doc.getPage(0));
            }
            pdf = saved(doc);
        }
        try (PDDocument read = Loader.loadPDF(pdf)) {
            assertEquals(1, read.getNumberOfPages());
            PDPage page = read.getPage(0);
            assertTrue(new PDFTextStripper().getText(read).contains("Ver dos y la web"));
            // The rectangle of the form, in its own stream, and the content of the page after it.
            int streams = 0;
            for (var it = page.getContentStreams(); it.hasNext(); it.next()) {
                streams++;
            }
            assertTrue(streams >= 2, "streams: " + streams);
            assertNull(read.getDocumentCatalog().getStructureTreeRoot(), "no structure tree");
            assertEquals(-1, page.getStructParents());
            assertNull(read.getDocumentCatalog().getDocumentOutline(), "no bookmarks");
            // The link to the URL; the one to the second page, which was not painted, is removed.
            assertEquals(1, page.getAnnotations().size());
            assertTrue(page.getResources().getFontNames().iterator().hasNext(), "the font is embedded");
            assertTrue(page.getResources().getXObjectNames().iterator().hasNext(), "the image is embedded");
        }
    }

    /** The cancellation is consulted before each page: once requested, the next page is not painted. */
    @Test
    void theCancellationIsConsultedBeforeEachPage() throws IOException {
        LaidOutDocument document = twoPages();
        try (PDDocument doc = form(2)) {
            int[] painted = {0};
            Cancellation afterTheFirst = () -> painted[0] > 0;
            try (PdfPainter painter = new PdfPainter(doc, RESOURCES, Limits.DEFAULT, afterTheFirst)) {
                painter.paint(document.pages().get(0), doc.getPage(0));
                painted[0]++;
                assertThrows(CancelledException.class, () -> painter.paint(document.pages().get(1), doc.getPage(1)));
            }
            // The second page keeps only the stream of the form.
            int streams = 0;
            for (var it = doc.getPage(1).getContentStreams(); it.hasNext(); it.next()) {
                streams++;
            }
            assertEquals(1, streams);
        }
    }

    @Test
    void internalLinksAreResolvedAmongThePagesPainted() throws IOException {
        LaidOutDocument document = twoPages();
        try (PDDocument doc = form(2)) {
            try (PdfPainter painter = new PdfPainter(doc, RESOURCES, Limits.DEFAULT)) {
                painter.paint(document.pages().get(0), doc.getPage(0));
                painter.paint(document.pages().get(1), doc.getPage(1));
            }
            List<PDAnnotation> annotations = doc.getPage(0).getAnnotations();
            assertEquals(2, annotations.size());
            PDAnnotationLink internal = (PDAnnotationLink) annotations.get(0);
            PDPageXYZDestination destination = (PDPageXYZDestination) internal.getDestination();
            assertSame(doc.getPage(1).getCOSObject(), destination.getPage().getCOSObject());
            PDAnnotationLink external = (PDAnnotationLink) annotations.get(1);
            assertNull(external.getDestination());
            assertEquals("URI", external.getAction().getCOSObject().getNameAsString(COSName.S));
        }
    }

    /** A link to a footnote goes to the note, in the footnote area of the page (reference/components/footnote.md). */
    @Test
    void aLinkToAFootnoteGoesToTheNote() throws IOException {
        LaidOutDocument document = laidOut("\tParagraph: Uno[^a] dos.", "\tParagraph: Ir a la nota", "\t\tLink: #a",
                "\tFootnote:", "\t\tId: a", "\t\tParagraph: Nota a.");
        Page page = document.pages().get(0);
        Fragment area = page.footnotes();
        double noteY = page.pageSetup().pageArea().y() + area.y() + area.border().top() + area.padding().top()
                + area.children().get(0).y();
        try (PDDocument doc = form(1)) {
            try (PdfPainter painter = new PdfPainter(doc, RESOURCES, Limits.DEFAULT)) {
                painter.paint(page, doc.getPage(0));
            }
            List<PDAnnotation> annotations = doc.getPage(0).getAnnotations();
            assertEquals(1, annotations.size(), "the link to the note is not removed");
            PDPageXYZDestination destination = (PDPageXYZDestination) ((PDAnnotationLink) annotations.get(0))
                    .getDestination();
            assertSame(doc.getPage(0).getCOSObject(), destination.getPage().getCOSObject());
            // The top left corner of the page is at the top left corner of the A4 page.
            assertEquals((int) Math.ceil(PDRectangle.A4.getHeight() - noteY), destination.getTop());
        }
    }

    @Test
    void aLinkToAPageNotPaintedIsRemoved() throws IOException {
        LaidOutDocument document = twoPages();
        try (PDDocument doc = form(1)) {
            try (PdfPainter painter = new PdfPainter(doc, RESOURCES, Limits.DEFAULT)) {
                painter.paint(document.pages().get(0), doc.getPage(0));
            }
            List<PDAnnotation> annotations = doc.getPage(0).getAnnotations();
            assertEquals(1, annotations.size(), "only the link to the URL");
            assertEquals("URI", annotations.get(0).getCOSObject().getCOSDictionary(COSName.A).getNameAsString(COSName.S));
        }
    }

    /** The link annotations move with the content when the media box does not start at x = 0. */
    @Test
    void theAnnotationsFollowTheMediaBox() throws IOException {
        LaidOutDocument document = laidOut("\tHeading 1:", "\t\tId: uno", "\t\tText: Uno",
                "\tParagraph: Ver [uno](#uno) y [la web](https://stxt.dev).");
        PDRectangle atZero = linkRectangle(document, new PDRectangle(0, 0, 400, 500));
        PDRectangle shifted = linkRectangle(document, new PDRectangle(100, 50, 400, 500));
        assertEquals(atZero.getLowerLeftX() + 100, shifted.getLowerLeftX(), 1e-3);
        assertEquals(atZero.getWidth(), shifted.getWidth(), 1e-3);
    }

    /** The rectangle of the first link annotation of the first page of {@code document}, painted on {@code mediaBox}. */
    private static PDRectangle linkRectangle(LaidOutDocument document, PDRectangle mediaBox) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(mediaBox);
            doc.addPage(page);
            try (PdfPainter painter = new PdfPainter(doc, RESOURCES, Limits.DEFAULT)) {
                painter.paint(document.pages().get(0), page);
            }
            return page.getAnnotations().get(0).getRectangle();
        }
    }

    /** The top left corner of the page goes to the top left corner of the media box, wherever it is. */
    @Test
    void thePageIsPaintedAtTheTopLeftCornerOfTheMediaBox() throws IOException {
        LaidOutDocument document = laidOut("\tHeading 1:", "\t\tId: uno", "\t\tText: Uno",
                "\tParagraph: Ver [uno](#uno).");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(100, 50, 400, 500));
            doc.addPage(page);
            try (PdfPainter painter = new PdfPainter(doc, RESOURCES, Limits.DEFAULT)) {
                painter.paint(document.pages().get(0), page);
            }
            PDAnnotationLink link = (PDAnnotationLink) page.getAnnotations().get(0);
            PDPageXYZDestination destination = (PDPageXYZDestination) link.getDestination();
            // The heading starts at the margin of the page: 20 pt from the top left corner, which is at (100, 550).
            assertEquals(120, destination.getLeft());
            assertEquals(530, destination.getTop());
            assertTrue(link.getRectangle().getLowerLeftX() >= 120, "the link is inside the media box: "
                    + link.getRectangle());
            assertTrue(link.getRectangle().getUpperRightY() <= 550 - 20, "under the top margin: " + link.getRectangle());
        }
    }
}
