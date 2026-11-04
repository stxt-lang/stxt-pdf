package dev.stxt.pdf.paint;

import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.resources.Resources;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.util.Matrix;

import java.io.IOException;
import java.util.List;

/**
 * What the pages of a PDF document share while they are painted, and the painting of one of them: the document, the
 * resources folder, the embedded fonts and images, the structure tree and the link annotations.
 * {@link PdfDocumentWriter} creates one per document and {@link PdfPainter}, one per painter.
 *
 * @param doc       the document the pages belong to
 * @param resources the folder of the fonts and the images
 * @param fonts     the fonts embedded in the document
 * @param images    the images embedded in the document, by file
 * @param structure the structure tree, or {@link PdfStructure#none()} if nothing is tagged
 * @param links     the link annotations, which get their destination when every page has been painted
 */
record PdfPageWriter(PDDocument doc, Resources resources, PdfFonts fonts, PdfImages images, PdfStructure structure,
                     PdfLinks links) {

    /**
     * Paints {@code page} on {@code target}, with the top left corner of the page at ({@code left}, {@code top}) in PDF
     * coordinates: the content of the PDF page is written anew or, with {@code append}, after what it already has, in
     * its own graphics state. Returns the link annotations of the page, which the caller sets on {@code target}.
     *
     * @throws IOException an input and output failure; those of the canvas primitives come out as
     *         {@link java.io.UncheckedIOException} ({@link PdfContent#io})
     */
    List<PDAnnotation> paint(Page page, PDPage target, boolean append, double left, double top, boolean debug)
            throws IOException {
        structure.startPage(target);
        PdfCanvas canvas;
        try (PDPageContentStream out = new PDPageContentStream(doc, target,
                append ? AppendMode.APPEND : AppendMode.OVERWRITE, true, append)) {
            if (left != 0) {
                out.transform(Matrix.getTranslateInstance((float) left, 0));
            }
            canvas = new PdfCanvas(this, out, top, debug);
            Rect area = page.pageSetup().pageArea();
            StackingContext.build(page.content(), page.footnotes(), page.pageOverlays(), area.x(), area.y())
                    .paint(canvas);
        }
        return canvas.tagging().annotations();
    }
}
