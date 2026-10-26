package dev.stxt.boxes.paint;

import dev.stxt.InlineNode;
import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.inline.PdfMetrics;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.tree.BoxTreeBuilder;
import dev.stxt.boxes.tree.Media;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Salida PDF con PDFBox: una página PDF por página del resultado, en pt. En modo continuo hay
 * una sola página, con el alto del contenido más los márgenes del medio.
 *
 * <p>El texto se pinta con las 14 fuentes estándar de PDF ({@link PdfMetrics#standardFont});
 * para que las líneas midan lo mismo que al maquetar, el layout debe usar las mismas métricas:
 * {@code new Engine(new PdfMetrics())}. PDF tiene el origen abajo a la izquierda: las
 * coordenadas verticales se invierten al pintar.</p>
 *
 * <p>Con {@code debug}, como {@link SvgCanvas}, dibuja además el contorno de cada caja de borde
 * (lo usan las imágenes de los tests de referencia).</p>
 */
public final class PdfCanvas implements Canvas {

    private final PDPageContentStream out;
    private final double pageHeight;
    /** Fuentes del documento, compartidas por todas sus páginas. */
    private final Map<String, PDFont> fonts;
    private final boolean debug;

    private PdfCanvas(PDPageContentStream out, double pageHeight, Map<String, PDFont> fonts, boolean debug) {
        this.out = out;
        this.pageHeight = pageHeight;
        this.fonts = fonts;
        this.debug = debug;
    }

    /**
     * Maqueta un documento ya validado ({@link dev.stxt.boxes.stxt.DocumentReader}) con las
     * métricas de PDF y escribe el PDF en {@code target}, que no se cierra.
     */
    public static void write(InlineNode document, OutputStream target) throws IOException {
        write(document, target, false);
    }

    /** Como {@link #write(InlineNode, OutputStream)}, con los contornos de depuración si {@code debug}. */
    public static void write(InlineNode document, OutputStream target, boolean debug) throws IOException {
        List<Page> pages = new Engine(new PdfMetrics()).layOutDocument(document);
        write(pages, new BoxTreeBuilder().build(document).media(), target, debug);
    }

    /** Pinta las páginas y escribe el PDF en {@code target}, que no se cierra. */
    public static void write(List<Page> pages, Media media, OutputStream target, boolean debug) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            Map<String, PDFont> fonts = new HashMap<>();
            Rect area = media.pageArea();
            for (Page p : pages) {
                double height = media.paged()
                        ? media.height().getAsDouble()
                        : p.content().height() + media.margin().vertical();
                PDPage page = new PDPage(new PDRectangle((float) media.width(), (float) height));
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    StackingContext.build(p.content(), p.fixed(), area.x(), area.y())
                            .paint(new PdfCanvas(cs, height, fonts, debug));
                }
            }
            doc.save(target);
        }
    }

    @Override
    public void background(Rect r, String color) {
        if (r.width() <= 0 || r.height() <= 0) {
            return;
        }
        try {
            setColor(color);
            out.addRect((float) r.x(), (float) (pageHeight - r.bottom()), (float) r.width(), (float) r.height());
            out.fill();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void border(Rect r, Insets w, String color) {
        if (w.top() > 0) {
            background(new Rect(r.x(), r.y(), r.width(), w.top()), color);
        }
        if (w.bottom() > 0) {
            background(new Rect(r.x(), r.bottom() - w.bottom(), r.width(), w.bottom()), color);
        }
        if (w.left() > 0) {
            background(new Rect(r.x(), r.y(), w.left(), r.height()), color);
        }
        if (w.right() > 0) {
            background(new Rect(r.right() - w.right(), r.y(), w.right(), r.height()), color);
        }
        if (debug) {
            try {
                out.setStrokingColor(0xDD / 255f, 0x33 / 255f, 0x33 / 255f);
                out.setLineWidth(0.3f);
                out.setLineDashPattern(new float[]{2, 2}, 0);
                out.addRect((float) r.x(), (float) (pageHeight - r.bottom()), (float) r.width(), (float) r.height());
                out.stroke();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public void text(double x, double baselineY, String text, String font, double fontSize, String color,
                     double wordSpacing) {
        PDFont f = fonts.computeIfAbsent(font, PdfMetrics::standardFont);
        try {
            out.beginText();
            out.setFont(f, (float) fontSize);
            // Tw: se suma a cada byte 32 del texto, que en las fuentes estándar (WinAnsi) es el espacio.
            out.setWordSpacing((float) wordSpacing);
            setColor(color);
            out.newLineAtOffset((float) x, (float) (pageHeight - baselineY));
            out.showText(PdfMetrics.encodable(f, text));
            out.endText();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void setColor(String color) throws IOException {
        float[] rgb = rgb(color);
        out.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
    }

    /** {@code #RGB} o {@code #RRGGBB} (ya validado al construir el árbol) en componentes de 0 a 1. */
    static float[] rgb(String color) {
        String hex = color.substring(1);
        if (hex.length() == 3) {
            hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
        }
        int v = Integer.parseInt(hex, 16);
        return new float[]{((v >> 16) & 0xFF) / 255f, ((v >> 8) & 0xFF) / 255f, (v & 0xFF) / 255f};
    }
}
