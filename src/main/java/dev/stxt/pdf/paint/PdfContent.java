package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.Colors;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Rotation;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
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
 * the bottom), the paths, the color with its opacity and the current rotations.
 */
final class PdfContent {

    /** A PDF operation that may fail when writing. */
    @FunctionalInterface
    interface Operation {
        void run() throws IOException;
    }

    private final PDPageContentStream out;
    private final double pageHeight;
    /** Opacity graphics states of this page, by opacity (in thousandths). */
    private final Map<Integer, PDExtendedGraphicsState> opacities = new HashMap<>();
    /**
     * The current rotations ({@link #rotate}), composed, in page coordinates (with y
     * pointing down); needed by the link areas and the {@code BBox} of the figures, which
     * do not follow the content matrix.
     */
    private AffineTransform turn = new AffineTransform();
    private final Deque<AffineTransform> turns = new ArrayDeque<>();

    PdfContent(PDPageContentStream out, double pageHeight) {
        this.out = out;
        this.pageHeight = pageHeight;
    }

    PDPageContentStream out() {
        return out;
    }

    double pageHeight() {
        return pageHeight;
    }

    /** The current rotations, composed; the identity if there are none. */
    AffineTransform turn() {
        return turn;
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

    private PDExtendedGraphicsState opacity(float alpha) {
        return opacities.computeIfAbsent(Math.round(alpha * 1000), key -> {
            PDExtendedGraphicsState state = new PDExtendedGraphicsState();
            state.setNonStrokingAlphaConstant(alpha);
            return state;
        });
    }

    /**
     * Starts painting rotated {@code degrees} degrees around ({@code x}, {@code y}): a {@code cm} matrix
     * inside a {@code q}/{@code Q}. With the origin at the bottom, a clockwise rotation is a negative
     * angle.
     */
    void rotate(double degrees, double x, double y) throws IOException {
        turns.push(turn);
        turn = new AffineTransform(turn);
        turn.rotate(Math.toRadians(degrees), x, y);
        out.saveGraphicsState();
        out.transform(new Matrix(AffineTransform.getRotateInstance(-Math.toRadians(degrees), x, pageHeight - y)));
    }

    /** Ends the rotation started with {@link #rotate}. */
    void unrotate() throws IOException {
        turn = turns.pop();
        out.restoreGraphicsState();
    }

    /** The rectangle of the page that {@code r} takes with the current rotations, in PDF coordinates. */
    PDRectangle turnedRect(Rect r) {
        return pdfRect(turn.isIdentity() ? r : Rotation.envelope(turn, r));
    }

    /** A rectangle in PDF coordinates, with the origin at the bottom. */
    PDRectangle pdfRect(Rect r) {
        return new PDRectangle((float) r.x(), (float) (pageHeight - r.bottom()), (float) r.width(), (float) r.height());
    }
}
