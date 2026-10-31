package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Rotation;
import dev.stxt.pdf.tree.Box;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;

import java.util.ArrayList;
import java.util.List;

import static dev.stxt.pdf.paint.PdfContent.io;

/**
 * The tagging of a PDF page: the marked content of the tagged PDF, which {@link PdfStructure} hangs from its
 * element, and the link annotations (reference/implementation/pdf.md), which {@link PdfLinks} creates and
 * which are set on the page when its painting ends ({@link #annotations}).
 */
final class PdfTagging implements Tagging {

    private final PdfContent content;
    private final PDPageContentStream out;
    /** Structure tree of the document, shared by all its pages. */
    private final PdfStructure structure;
    /** Link annotations of the document, shared by all its pages. */
    private final PdfLinks links;
    /** The annotations of this page, set on it when its painting ends. */
    private final List<PDAnnotation> annotations = new ArrayList<>();
    /** Whether what is painted is the text of a link ({@link #beginLinkText}): its areas hang from its {@code Link}. */
    private boolean linkText;

    PdfTagging(PdfContent content, PdfStructure structure, PdfLinks links) {
        this.content = content;
        this.out = content.out();
        this.structure = structure;
        this.links = links;
    }

    /** The link annotations of the page, which {@link PdfDocumentWriter} sets on it when its painting ends. */
    List<PDAnnotation> annotations() {
        return annotations;
    }

    @Override
    public void beginLinkText(Box box) {
        io(() -> {
            int mcid = structure.markLink(box);
            linkText = true;
            if (mcid < 0) {
                out.beginMarkedContent(COSName.ARTIFACT);
            } else {
                out.beginMarkedContent(COSName.getPDFName("Link"), mcid);
            }
        });
    }

    @Override
    public void beginText(Box box) {
        io(() -> {
            int mcid = structure.mark(box);
            if (mcid < 0) {
                out.beginMarkedContent(COSName.ARTIFACT);
            } else {
                out.beginMarkedContent(COSName.getPDFName(PdfStructure.type(box)), mcid);
            }
        });
    }

    /**
     * Starts the image of an {@code Image}, which ends with {@link #endText()}: with {@code Alt}, the content of
     * its {@code Figure}, whose {@code BBox} is its content box {@code area} (when rotated, the one enclosing it);
     * without it, an <em>artifact</em>.
     */
    void beginImage(Box box, Rect area) {
        io(() -> {
            int mcid = structure.mark(box);
            if (mcid < 0) {
                out.beginMarkedContent(COSName.ARTIFACT);
            } else {
                out.beginMarkedContent(COSName.getPDFName(PdfStructure.type(box)), mcid);
                structure.figureBox(box, content.turnedRect(area));
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>A {@code Link} annotation without a border; the structure hangs it from the box ({@link PdfStructure#link}).
     * When rotated, its rectangle is the one enclosing the area and its {@code QuadPoints}, the rotated area: a
     * viewer that does not understand them makes the whole rectangle clickable.</p>
     */
    @Override
    public void link(Box box, Rect area, String destination, String description) {
        PDAnnotationLink annotation = links.create(content.turnedRect(area), destination, description);
        if (!content.turn().isIdentity()) {
            // The four corners counterclockwise from the bottom left, with the origin at the bottom.
            double[] c = Rotation.corners(content.turn(), area);
            float[] quad = new float[8];
            int[] order = {3, 2, 1, 0};
            for (int i = 0; i < 4; i++) {
                quad[2 * i] = (float) c[2 * order[i]];
                quad[2 * i + 1] = (float) (content.pageHeight() - c[2 * order[i] + 1]);
            }
            annotation.setQuadPoints(quad);
        }
        annotations.add(annotation);
        if (linkText) {
            structure.inlineLink(box, destination, annotation, description);
        } else {
            structure.link(box, destination, annotation, description);
        }
    }

    @Override
    public void endText() {
        linkText = false;
        io(out::endMarkedContent);
    }

    @Override
    public void beginActualText(String text) {
        COSDictionary properties = new COSDictionary();
        properties.setString(COSName.ACTUAL_TEXT, text);
        io(() -> out.beginMarkedContent(COSName.getPDFName("Span"), PDPropertyList.create(properties)));
    }

    @Override
    public void endActualText() {
        io(out::endMarkedContent);
    }
}
