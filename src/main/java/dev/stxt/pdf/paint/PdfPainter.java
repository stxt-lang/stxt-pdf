package dev.stxt.pdf.paint;

import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.model.Cancellation;
import dev.stxt.pdf.model.Limits;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.tree.Box;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Paints laid out pages, one at a time, on pages of a PDFBox document that is not the engine's
 * (reference/implementation/api.md): to stamp a page on a form, or to put the pages of a document in another PDF.
 * Each {@link #paint} appends the content of a {@link Page} to a {@link PDPage} of the document, with the top left
 * corner of the page at the top left corner of the media box of the PDF page and its fonts and images embedded once
 * per painter. The page is laid out by the engine ({@link LaidOutDocument#pages()}, with
 * {@code new PdfMetrics(resources)}, the same {@code resources}).
 *
 * <p>Nothing is tagged: the document the pages are painted on has its own structure tree, or none, and the engine
 * does not touch it, so every content is an <em>artifact</em> (the text is still copied and searched) and the
 * link annotations hang from no {@code Link}. A document that has to be PDF/UA, with its structure tree, bookmarks,
 * page labels and metadata, is written whole with {@link PdfDocumentWriter} or {@code Converter.toPdf}.</p>
 *
 * <p>The links to a URL work at once. Those to a box of the document ({@code #id}) get their destination when the
 * painter is {@link #close() closed}, if the page that has it was painted with this painter; otherwise their
 * annotation is removed. Closing the painter also closes the PDFs it drew pages of; it does not save the document,
 * which the caller saves.</p>
 *
 * <p><strong>Threads:</strong> a painter belongs to one document and one thread.</p>
 */
public final class PdfPainter implements Closeable {

    /** The document with its fonts, images and links, without a structure tree: nothing is tagged. */
    private final PdfPageWriter pages;
    private final Cancellation cancellation;
    private final List<Page> painted = new ArrayList<>();
    private final List<PDPage> targets = new ArrayList<>();
    private final List<double[]> corners = new ArrayList<>();

    /** A painter for {@code doc}, with the fonts and images of {@code resources} and the {@code limits} of the SVG drawing. */
    public PdfPainter(PDDocument doc, Resources resources, Limits limits) {
        this(doc, resources, limits, Cancellation.NONE);
    }

    /**
     * Like {@link #PdfPainter(PDDocument, Resources, Limits)}, stopping with a
     * {@link dev.stxt.pdf.model.CancelledException} once {@code cancellation} asks for it: before each page and at
     * each image the first time it is embedded or drawn.
     */
    public PdfPainter(PDDocument doc, Resources resources, Limits limits, Cancellation cancellation) {
        Objects.requireNonNull(doc, "doc");
        PdfFonts fonts = new PdfFonts(doc, Objects.requireNonNull(resources, "resources"));
        this.cancellation = Objects.requireNonNull(cancellation, "cancellation");
        PdfImages images = new PdfImages(doc, Objects.requireNonNull(limits, "limits"), cancellation);
        this.pages = new PdfPageWriter(doc, resources, fonts, images, PdfStructure.none(), new PdfLinks());
    }

    /**
     * Appends the content of {@code page} to {@code target}, a page of the document of this painter, after what it
     * already has, with the top left corner of the page at the top left corner of the media box of {@code target}
     * (its rotation, {@code Rotate}, is not taken into account). The link annotations of the page are added to those
     * of {@code target}.
     *
     * @throws IOException an input and output failure, also those of the canvas primitives
     */
    public void paint(Page page, PDPage target) throws IOException {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(target, "target");
        cancellation.check();
        PDRectangle box = target.getMediaBox();
        double left = box.getLowerLeftX();
        double top = box.getUpperRightY();
        List<PDAnnotation> added;
        try {
            added = pages.paint(page, target, true, left, top, false);
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        if (!added.isEmpty()) {
            List<PDAnnotation> annotations = new ArrayList<>(target.getAnnotations());
            for (PDAnnotation annotation : added) {
                // The content was translated to the media box; the annotations are outside the content stream.
                if (left != 0) {
                    shift(annotation, (float) left);
                }
                annotations.add(annotation);
            }
            target.setAnnotations(annotations);
        }
        painted.add(page);
        targets.add(target);
        corners.add(new double[] {left, top});
    }

    /** Moves the annotation {@code dx} to the right: its rectangle and, if it has them, its {@code QuadPoints}. */
    private static void shift(PDAnnotation annotation, float dx) {
        PDRectangle r = annotation.getRectangle();
        annotation.setRectangle(new PDRectangle(r.getLowerLeftX() + dx, r.getLowerLeftY(), r.getWidth(),
                r.getHeight()));
        if (annotation instanceof PDAnnotationLink link && link.getQuadPoints() != null) {
            float[] quad = link.getQuadPoints();
            for (int i = 0; i < quad.length; i += 2) {
                quad[i] += dx;
            }
            link.setQuadPoints(quad);
        }
    }

    /**
     * Gives the internal links their destination, removes those whose destination is on a page that was not painted
     * and closes the PDFs whose pages were drawn. The document is not saved.
     */
    @Override
    public void close() throws IOException {
        try {
            double[] lefts = new double[painted.size()];
            double[] tops = new double[painted.size()];
            for (int i = 0; i < painted.size(); i++) {
                lefts[i] = corners.get(i)[0];
                tops[i] = corners.get(i)[1];
            }
            Map<Box, PDPageXYZDestination> destinations = PdfDestinations.find(painted, targets, lefts, tops);
            List<PDAnnotationLink> unresolved = pages.links().resolve(destinations);
            if (!unresolved.isEmpty()) {
                Set<PDAnnotation> dropped = Set.copyOf(unresolved);
                for (PDPage target : targets) {
                    List<PDAnnotation> kept = new ArrayList<>(target.getAnnotations());
                    if (kept.removeIf(dropped::contains)) {
                        target.setAnnotations(kept);
                    }
                }
            }
        } finally {
            pages.images().close();
        }
    }
}
