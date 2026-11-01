package dev.stxt.pdf.paint;

import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.model.Attachments;
import dev.stxt.pdf.model.Limits;
import dev.stxt.pdf.model.NumberFormat;
import dev.stxt.pdf.model.PdfA;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import dev.stxt.pdf.tree.PageSetup;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDPageLabelRange;
import org.apache.pdfbox.pdmodel.common.PDPageLabels;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The PDF document: creates the pages, paints each one with its {@link PdfCanvas}, and sets the page labels, the
 * link destinations, the structure tree ({@link PdfStructure}), the metadata ({@link PdfMetadata}) and the embedded
 * files ({@link PdfAttachments}) before saving it.
 * One PDF page per page of the result, in pt, with the size of the paper of its section. The file identifier comes from
 * the content ({@link PdfId}): the same document gives the same bytes.
 *
 * <p><strong>Threads:</strong> {@link #write} is static and all its state (the PDF document, the embedded
 * fonts, the structure, the links) belongs to that call, so it can be called concurrently from several
 * threads, also with the same {@link LaidOutDocument} and the same {@link Resources}. The only exception is
 * global to the JVM: the font cache of JSVG, which {@link PdfImages} uses under a lock.</p>
 */
public final class PdfDocumentWriter {

    private PdfDocumentWriter() {
    }

    /**
     * Paints a document laid out with {@code new PdfMetrics(resources)}, with the title and the
     * language of its metadata, and writes the PDF to {@code target}, which is not closed. To lay out
     * and paint in one call there is {@link dev.stxt.pdf.convert.Converter}.
     *
     * @param limits those of the SVG images, which are drawn with them (the engine that laid out checked them first)
     * @throws IOException an input and output failure, also those of the canvas primitives (which cannot
     *         declare it and throw it as {@link UncheckedIOException}: here they come out as what they are)
     */
    public static void write(LaidOutDocument document, Resources resources, Limits limits, OutputStream target,
            boolean debug) throws IOException {
        write(document, resources, limits, target, debug, 1);
    }

    /**
     * Like {@link #write(LaidOutDocument, Resources, Limits, OutputStream, boolean)}, complying with part {@code ua} of
     * PDF/UA: 1 (PDF 1.7) or 2 (PDF 2.0), reference/implementation/pdf.md.
     */
    public static void write(LaidOutDocument document, Resources resources, Limits limits, OutputStream target,
            boolean debug, int ua) throws IOException {
        write(document, resources, limits, target, debug, ua, null, Attachments.NONE);
    }

    /**
     * Like {@link #write(LaidOutDocument, Resources, Limits, OutputStream, boolean, int)}, complying also with
     * {@code pdfA} ({@code null} for no PDF/A) and with the embedded files and the extra XMP of {@code attachments}
     * ({@link Attachments#NONE} for none), reference/implementation/pdf.md.
     *
     * @throws IllegalArgumentException if {@code ua} is neither 1 nor 2, or if it is 2 (PDF 2.0) with a PDF/A, which
     *         are PDF 1.7 files
     * @throws InvalidModelException {@code ATTACHMENT_REQUIRES_PDFA3} if there are files to embed and the PDF is not
     *         PDF/A-3
     */
    public static void write(LaidOutDocument document, Resources resources, Limits limits, OutputStream target,
            boolean debug, int ua, PdfA pdfA, Attachments attachments) throws IOException {
        if (ua != 1 && ua != 2) {
            throw new IllegalArgumentException("PDF/UA has parts 1 and 2: " + ua);
        }
        if (ua == 2 && pdfA != null) {
            throw new IllegalArgumentException("PDF/UA-2 is a PDF 2.0 file and " + pdfA + " a PDF 1.7 one: "
                    + "a PDF cannot comply with both");
        }
        if (!attachments.files().isEmpty() && (pdfA == null || !pdfA.allowsAttachments())) {
            throw new InvalidModelException(InvalidModelException.NO_LINE, ErrorCode.ATTACHMENT_REQUIRES_PDFA3,
                    "Embedding a file (" + attachments.files().get(0).name() + ") needs PDF/A-3, "
                    + "Converter.builder().pdfA(PdfA.A3B) or PdfA.A3A; the PDF is "
                    + (pdfA == null ? "not PDF/A" : pdfA));
        }
        try {
            paintAndSave(document, resources, limits, target, debug, ua, pdfA, attachments);
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private static void paintAndSave(LaidOutDocument document, Resources resources, Limits limits,
            OutputStream target, boolean debug, int ua, PdfA pdfA, Attachments attachments) throws IOException {
        List<Page> pages = document.pages();
        try (PDDocument doc = new PDDocument(); PdfImages images = new PdfImages(doc, limits)) {
            if (ua == 2) {
                // The file header and the catalog version.
                doc.getDocument().setVersion(2.0f);
                doc.getDocumentCatalog().setVersion("2.0");
            }
            PdfFonts fonts = new PdfFonts(doc, resources);
            PdfStructure structure = new PdfStructure(doc, document.root(), ua);
            PdfLinks links = new PdfLinks();
            PdfMetadata.write(doc, document.metadata(), ua, pdfA, attachments.xmp());
            // All the pages exist before any is painted: a link may point forward.
            List<PDPage> pdfPages = new ArrayList<>();
            for (int i = 0; i < pages.size(); i++) {
                PageSetup pageSetup = pages.get(i).pageSetup();
                PDPage page = new PDPage(new PDRectangle((float) pageSetup.width(), (float) pageSetup.height()));
                doc.addPage(page);
                pdfPages.add(page);
            }
            for (int i = 0; i < pages.size(); i++) {
                Page p = pages.get(i);
                // Each page has the paper of its section (reference/components/document.md).
                PageSetup pageSetup = p.pageSetup();
                Rect area = pageSetup.pageArea();
                PDPage page = pdfPages.get(i);
                structure.startPage(page);
                PdfCanvas canvas;
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    canvas = new PdfCanvas(doc, cs, pageSetup.height(), resources, fonts, images, structure, links,
                            debug);
                    StackingContext.build(p.content(), p.pageOverlays(), area.x(), area.y()).paint(canvas);
                }
                if (!canvas.tagging().annotations().isEmpty()) {
                    page.setAnnotations(canvas.tagging().annotations());
                    // Annotations are traversed in structure order (PDF/UA, 7.18.3).
                    page.getCOSObject().setName(COSName.getPDFName("Tabs"), "S");
                }
            }
            labels(doc, pages);
            Map<Box, PDPageXYZDestination> destinations = PdfDestinations.find(pages, pdfPages);
            links.resolve(destinations, structure);
            structure.finish(doc, destinations);
            PdfAttachments.write(doc, attachments);
            PdfId.prepare(doc);
            ByteArrayOutputStream pdf = new ByteArrayOutputStream();
            doc.save(pdf);
            byte[] bytes = pdf.toByteArray();
            PdfId.fill(bytes);
            target.write(bytes);
        }
    }

    /**
     * The page labels (reference/components/document.md): the viewer shows the printed number of each page, and none
     * on the pages that are not numbered. They are only written if some page is not its physical number; otherwise
     * they add nothing. A new label starts where the number does not follow that of the previous page.
     */
    private static void labels(PDDocument doc, List<Page> pages) {
        if (pages.stream().allMatch(p -> p.printed() == p.number() && p.style() == NumberFormat.DECIMAL)) {
            return;
        }
        PDPageLabels labels = new PDPageLabels(doc);
        Page previous = null;
        for (int i = 0; i < pages.size(); i++) {
            Page p = pages.get(i);
            boolean follows = previous != null
                    && (p.numbered() ? previous.numbered() && p.printed() == previous.printed() + 1
                    && p.style() == previous.style() : !previous.numbered());
            if (!follows) {
                PDPageLabelRange range = new PDPageLabelRange();
                if (p.numbered()) {
                    range.setStyle(switch (p.style()) {
                        case LOWER_ROMAN -> PDPageLabelRange.STYLE_ROMAN_LOWER;
                        case UPPER_ROMAN -> PDPageLabelRange.STYLE_ROMAN_UPPER;
                        default -> PDPageLabelRange.STYLE_DECIMAL;
                    });
                    range.setStart(p.printed());
                }
                labels.setLabelItem(i, range);
            }
            previous = p;
        }
        doc.getDocumentCatalog().setPageLabels(labels);
    }
}
