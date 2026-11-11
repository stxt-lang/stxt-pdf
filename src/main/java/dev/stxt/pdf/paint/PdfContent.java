package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.Colors;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Turns;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.util.Matrix;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * The content of a PDF page, what its canvas ({@link PdfCanvas}) and its tagging ({@link PdfTagging}) share: the
 * content stream, the conversion from page coordinates (with y pointing down) to PDF coordinates (with the origin at
 * the bottom), the paths, the color with its opacity, the current rotations and the transparency groups, inside which
 * the stream being written is that of the group.
 */
final class PdfContent {

    /** A PDF operation that may fail when writing. */
    @FunctionalInterface
    interface Operation {
        void run() throws IOException;
    }

    private final PDDocument doc;
    /** The stream being written: that of the page or, inside a transparency group, that of its form. */
    private PDPageContentStream out;
    /** The paper in PDF coordinates: the {@code BBox} of the transparency groups. */
    private final PDRectangle paper;
    private final double pageHeight;
    /** The transparency groups being painted ({@link #beginGroup}), the innermost first. */
    private final Deque<Group> groups = new ArrayDeque<>();
    /** Graphics states of this page that give a transparency group its opacity, by opacity (in thousandths). */
    private final Map<Integer, PDExtendedGraphicsState> groupOpacities = new HashMap<>();

    /**
     * A transparency group being painted: its form, the opacity it is painted with when it ends and the stream that
     * was being written when it started.
     */
    private record Group(PDFormXObject form, double opacity, PDPageContentStream outer) {
    }
    /** Opacity graphics states of this page, by opacity (in thousandths). */
    private final Map<Integer, PDExtendedGraphicsState> opacities = new HashMap<>();
    /** Opacity graphics states of this page for what is stroked, by opacity (in thousandths). */
    private final Map<Integer, PDExtendedGraphicsState> strokeOpacities = new HashMap<>();
    /**
     * The current rotations ({@link #rotate}), in page coordinates (with y pointing down); needed by the link areas and
     * the {@code BBox} of the figures, which do not follow the content matrix.
     */
    private final Turns turns = new Turns();

    /**
     * @param doc        the document of the page
     * @param out        the content stream of the page
     * @param paper      the paper in PDF coordinates
     * @param pageHeight the height at which the top of the paper is, in PDF coordinates
     */
    PdfContent(PDDocument doc, PDPageContentStream out, PDRectangle paper, double pageHeight) {
        this.doc = doc;
        this.out = out;
        this.paper = paper;
        this.pageHeight = pageHeight;
    }

    /**
     * The stream being written. It changes inside a transparency group ({@link #beginGroup}): nobody keeps it.
     */
    PDPageContentStream out() {
        return out;
    }

    double pageHeight() {
        return pageHeight;
    }

    /** The current rotations, composed; the identity if there are none. */
    AffineTransform turn() {
        return turns.current();
    }

    /**
     * Performs a PDF operation. The canvas primitives cannot declare {@link IOException}: a write failure comes out
     * as {@link UncheckedIOException}, which {@link PdfDocumentWriter#write} turns back into what it is.
     * It is the only place that converts it.
     */
    static void io(Operation operation) {
        try {
            operation.run();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A path written to the content of the page, with the origin at the bottom. */
    Shapes.Path path() {
        return new Shapes.Path() {
            @Override
            public void moveTo(double x, double y) {
                io(() -> out.moveTo((float) x, (float) (pageHeight - y)));
            }

            @Override
            public void lineTo(double x, double y) {
                io(() -> out.lineTo((float) x, (float) (pageHeight - y)));
            }

            @Override
            public void curveTo(double x1, double y1, double x2, double y2, double x3, double y3) {
                io(() -> out.curveTo((float) x1, (float) (pageHeight - y1), (float) x2, (float) (pageHeight - y2),
                        (float) x3, (float) (pageHeight - y3)));
            }

            @Override
            public void close() {
                io(out::closePath);
            }

            @Override
            public void rect(double x, double y, double width, double height) {
                io(() -> out.addRect((float) x, (float) (pageHeight - y - height), (float) width, (float) height));
            }
        };
    }

    /**
     * Paints with the color {@code color} whatever {@code body} does; if the color is not opaque, inside
     * its own graphics state with its opacity.
     */
    void painting(String color, Operation body) throws IOException {
        float[] rgba = Colors.rgba(color);
        boolean translucent = rgba[3] < 1f;
        if (translucent) {
            out.saveGraphicsState();
            out.setGraphicsStateParameters(opacity(rgba[3]));
        }
        out.setNonStrokingColor(rgba[0], rgba[1], rgba[2]);
        body.run();
        if (translucent) {
            out.restoreGraphicsState();
        }
    }

    /**
     * Strokes with the color {@code color} whatever {@code body} does, inside its own graphics state: the width, the
     * dash pattern and the ends of the line that {@code body} sets end with it.
     */
    void stroking(String color, Operation body) throws IOException {
        float[] rgba = Colors.rgba(color);
        out.saveGraphicsState();
        if (rgba[3] < 1f) {
            out.setGraphicsStateParameters(strokeOpacities.computeIfAbsent(Math.round(rgba[3] * 1000), key -> {
                PDExtendedGraphicsState state = new PDExtendedGraphicsState();
                state.setStrokingAlphaConstant(rgba[3]);
                return state;
            }));
        }
        out.setStrokingColor(rgba[0], rgba[1], rgba[2]);
        body.run();
        out.restoreGraphicsState();
    }

    private PDExtendedGraphicsState opacity(float alpha) {
        return opacities.computeIfAbsent(Math.round(alpha * 1000), key -> {
            PDExtendedGraphicsState state = new PDExtendedGraphicsState();
            state.setNonStrokingAlphaConstant(alpha);
            return state;
        });
    }

    /**
     * Starts a transparency group (ISO 32000-1, 11.6.6), that of a box with {@code Opacity}
     * (reference/common/layers-and-rotation.md): what is painted until {@link #endGroup} goes to the stream of a form
     * with the coordinates of the page, which is then painted on it as a whole with the opacity {@code opacity}. The rotations and clips started inside
     * end inside. Returns the form, whose marked content the structure needs to tell from that of the page.
     */
    PDFormXObject beginGroup(double opacity) throws IOException {
        // An appearance stream is the form that PDPageContentStream can write to.
        PDAppearanceStream form = new PDAppearanceStream(doc);
        form.setResources(new PDResources());
        form.setBBox(paper);
        COSDictionary group = new COSDictionary();
        group.setItem(COSName.S, COSName.TRANSPARENCY);
        // Isolated: the group is painted on its own, and only the result is blended with what lies beneath.
        group.setBoolean(COSName.I, true);
        form.getCOSObject().setItem(COSName.GROUP, group);
        groups.push(new Group(form, opacity, out));
        out = new PDPageContentStream(doc, form, form.getStream().createOutputStream(COSName.FLATE_DECODE));
        return form;
    }

    /** Ends the group started with {@link #beginGroup} and paints it with its opacity. */
    void endGroup() throws IOException {
        Group group = groups.pop();
        double opacity = group.opacity();
        out.close();
        out = group.outer();
        out.saveGraphicsState();
        out.setGraphicsStateParameters(groupOpacities.computeIfAbsent((int) Math.round(opacity * 1000), key -> {
            PDExtendedGraphicsState state = new PDExtendedGraphicsState();
            state.setNonStrokingAlphaConstant((float) opacity);
            state.setStrokingAlphaConstant((float) opacity);
            return state;
        }));
        out.drawForm(group.form());
        out.restoreGraphicsState();
    }

    /**
     * Closes the forms of the groups left open by a failure while painting, so that their streams are not left
     * open; the stream of the page is closed by whoever opened it.
     */
    void abandonGroups() {
        while (!groups.isEmpty()) {
            try {
                out.close();
            } catch (IOException e) {
                // The failure that brought us here is the one that is reported.
            }
            out = groups.pop().outer();
        }
    }

    /**
     * Starts painting rotated {@code degrees} degrees around ({@code x}, {@code y}): a {@code cm} matrix
     * inside a {@code q}/{@code Q}. With the origin at the bottom, a clockwise rotation is a negative
     * angle.
     */
    void rotate(double degrees, double x, double y) throws IOException {
        turns.push(degrees, x, y);
        out.saveGraphicsState();
        out.transform(new Matrix(AffineTransform.getRotateInstance(-Math.toRadians(degrees), x, pageHeight - y)));
    }

    /** Ends the rotation started with {@link #rotate}. */
    void unrotate() throws IOException {
        turns.pop();
        out.restoreGraphicsState();
    }

    /** The rectangle of the page that {@code r} takes with the current rotations, in PDF coordinates. */
    PDRectangle turnedRect(Rect r) {
        return pdfRect(turns.envelope(r));
    }

    /** A rectangle in PDF coordinates, with the origin at the bottom. */
    PDRectangle pdfRect(Rect r) {
        return new PDRectangle((float) r.x(), (float) (pageHeight - r.bottom()), (float) r.width(), (float) r.height());
    }
}
