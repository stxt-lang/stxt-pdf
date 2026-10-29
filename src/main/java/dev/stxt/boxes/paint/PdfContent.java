package dev.stxt.boxes.paint;

import dev.stxt.boxes.model.Colors;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.model.Rotation;
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
 * El contenido de una página PDF (AR-06), lo que comparten su lienzo ({@link PdfCanvas}) y su etiquetado
 * ({@link PdfTagging}): el flujo de contenido, el paso de las coordenadas de la página (con la y hacia abajo) a
 * las del PDF (con el origen abajo), los caminos, el color con su opacidad y los giros en curso.
 */
final class PdfContent {

    /** Una operación del PDF que puede fallar al escribir. */
    @FunctionalInterface
    interface Operation {
        void run() throws IOException;
    }

    private final PDPageContentStream out;
    private final double pageHeight;
    /** Estados gráficos de opacidad de esta página, por opacidad (en milésimas). */
    private final Map<Integer, PDExtendedGraphicsState> opacities = new HashMap<>();
    /**
     * Los giros en curso ({@link #rotate}), compuestos, en coordenadas de la página (con la
     * y hacia abajo); lo necesitan las zonas de los enlaces y el {@code BBox} de las figuras, que
     * no siguen la matriz del contenido.
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

    /** Los giros en curso, compuestos; la identidad si no hay ninguno. */
    AffineTransform turn() {
        return turn;
    }

    /**
     * Hace una operación del PDF. Las primitivas del lienzo no pueden declarar {@link IOException}: un fallo de
     * escritura sale como {@link UncheckedIOException}, que {@link PdfDocumentWriter#write} devuelve a lo que es.
     * Es el único sitio que lo convierte.
     */
    static void io(Operation operation) {
        try {
            operation.run();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Un camino que se escribe en el contenido de la página, con el origen abajo. */
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
     * Pinta con el color {@code color} lo que haga {@code body}; si el color no es opaco, dentro de
     * un estado gráfico propio con su opacidad.
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
     * Empieza a pintar girado {@code degrees} grados alrededor de ({@code x}, {@code y}): una matriz {@code cm}
     * dentro de un {@code q}/{@code Q}. Con el origen abajo, el giro en el sentido de las agujas del reloj es un
     * ángulo negativo.
     */
    void rotate(double degrees, double x, double y) throws IOException {
        turns.push(turn);
        turn = new AffineTransform(turn);
        turn.rotate(Math.toRadians(degrees), x, y);
        out.saveGraphicsState();
        out.transform(new Matrix(AffineTransform.getRotateInstance(-Math.toRadians(degrees), x, pageHeight - y)));
    }

    /** Termina el giro empezado con {@link #rotate}. */
    void unrotate() throws IOException {
        turn = turns.pop();
        out.restoreGraphicsState();
    }

    /** El rectángulo de la página que ocupa {@code r} con los giros en curso, en coordenadas del PDF. */
    PDRectangle turnedRect(Rect r) {
        return pdfRect(turn.isIdentity() ? r : Rotation.envelope(turn, r));
    }

    /** Un rectángulo en coordenadas del PDF, con el origen abajo. */
    PDRectangle pdfRect(Rect r) {
        return new PDRectangle((float) r.x(), (float) (pageHeight - r.bottom()), (float) r.width(), (float) r.height());
    }
}
