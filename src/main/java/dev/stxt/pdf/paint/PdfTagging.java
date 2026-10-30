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
 * El etiquetado de una página PDF: el contenido marcado del PDF etiquetado, que {@link PdfStructure} cuelga de su
 * elemento, y las anotaciones de los enlaces (referencia/implementacion/pdf.md), que crea {@link
 * PdfLinks} y se ponen en la página al terminar de pintarla ({@link #annotations}).
 */
final class PdfTagging implements Tagging {

    private final PdfContent content;
    private final PDPageContentStream out;
    /** Árbol de estructura del documento, compartido por todas sus páginas. */
    private final PdfStructure structure;
    /** Anotaciones de enlace del documento, compartidas por todas sus páginas. */
    private final PdfLinks links;
    /** Las anotaciones de esta página, que se le ponen al terminar de pintarla. */
    private final List<PDAnnotation> annotations = new ArrayList<>();
    /** Si lo que se pinta es el texto de un enlace ({@link #beginLinkText}): sus zonas cuelgan de su {@code Link}. */
    private boolean linkText;

    PdfTagging(PdfContent content, PdfStructure structure, PdfLinks links) {
        this.content = content;
        this.out = content.out();
        this.structure = structure;
        this.links = links;
    }

    /** Las anotaciones de enlace de la página, que {@link PdfDocumentWriter} le pone al terminar de pintarla. */
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
     * Empieza la imagen de una {@code Image}, que termina con {@link #endText()}: con {@code Alt}, el contenido de
     * su {@code Figure}, cuyo {@code BBox} es su caja de contenido {@code area} (girada, la que la envuelve); sin
     * él, un <em>artifact</em>.
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
     * <p>Una anotación {@code Link} sin borde; la estructura la cuelga de la caja ({@link PdfStructure#link}).
     * Girada, su rectángulo es el que envuelve la zona y sus {@code QuadPoints}, la zona girada: un
     * visor que no los entiende la hace clicable entera.</p>
     */
    @Override
    public void link(Box box, Rect area, String destination, String description) {
        PDAnnotationLink annotation = links.create(content.turnedRect(area), destination, description);
        if (!content.turn().isIdentity()) {
            // Las cuatro esquinas en sentido antihorario desde abajo a la izquierda, con el origen abajo.
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
