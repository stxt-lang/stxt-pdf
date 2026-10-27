package dev.stxt.boxes.paint;

import dev.stxt.InlineNode;
import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.inline.PdfMetrics;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.model.VerticalMetrics;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.BoxTreeBuilder;
import dev.stxt.boxes.tree.FontFace;
import dev.stxt.boxes.tree.Media;
import dev.stxt.boxes.tree.Metadata;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Salida PDF con PDFBox: una página PDF por página del resultado, en pt, del tamaño del medio.
 *
 * <p>El texto se pinta con las fuentes TTF de la carpeta de recursos (ESPECIFICACION.md §5.11),
 * incrustadas como subconjunto (solo los glifos usados) en una fuente Type 0 con codificación
 * Identity-H: el PDF se ve igual en cualquier máquina. Para que las líneas midan lo mismo que al
 * maquetar, el layout debe usar las mismas métricas: {@code new Engine(new PdfMetrics(resources))}.
 * PDF tiene el origen abajo a la izquierda: las coordenadas verticales se invierten al pintar.</p>
 *
 * <p>La justificación no puede usar el operador {@code Tw} de PDF, que solo afecta al byte 32 y
 * con Identity-H cada carácter ocupa dos: el espacio extra se añade tras cada espacio con
 * {@code TJ}.</p>
 *
 * <p>El PDF va etiquetado (árbol de estructura y contenido marcado) y lleva los marcadores de
 * los encabezados: ver {@link PdfStructure}.</p>
 *
 * <p>Con {@code debug}, como {@link SvgCanvas}, dibuja además el contorno de cada caja de borde
 * (lo usan las imágenes de los tests de referencia).</p>
 */
public final class PdfCanvas implements Canvas {

    private final PDDocument doc;
    private final PDPageContentStream out;
    private final double pageHeight;
    private final Resources resources;
    /** Fuentes incrustadas en el documento, compartidas por todas sus páginas. */
    private final Map<FontFace, PDFont> fonts;
    /** Árbol de estructura del documento, compartido por todas sus páginas. */
    private final PdfStructure structure;
    private final boolean debug;

    private PdfCanvas(PDDocument doc, PDPageContentStream out, double pageHeight, Resources resources,
                      Map<FontFace, PDFont> fonts, PdfStructure structure, boolean debug) {
        this.doc = doc;
        this.out = out;
        this.pageHeight = pageHeight;
        this.resources = resources;
        this.fonts = fonts;
        this.structure = structure;
        this.debug = debug;
    }

    /**
     * Maqueta un documento ya validado ({@link dev.stxt.boxes.stxt.DocumentReader}) con las
     * fuentes de la carpeta de recursos por defecto y escribe el PDF en {@code target}, que no
     * se cierra.
     */
    public static void write(InlineNode document, OutputStream target) throws IOException {
        write(document, new Resources(), target, false);
    }

    /**
     * Como {@link #write(InlineNode, OutputStream)}, con otra carpeta de recursos y los contornos
     * de depuración si {@code debug}.
     */
    public static void write(InlineNode document, Resources resources, OutputStream target, boolean debug)
            throws IOException {
        List<Page> pages = new Engine(new PdfMetrics(resources)).layOutDocument(document);
        BoxTreeBuilder.Result tree = new BoxTreeBuilder().build(document);
        write(pages, tree.media(), tree.metadata(), resources, target, debug);
    }

    /**
     * Pinta las páginas, maquetadas con {@code new PdfMetrics(resources)}, con el título y el
     * idioma de {@code metadata}, y escribe el PDF en {@code target}, que no se cierra.
     */
    public static void write(List<Page> pages, Media media, Metadata metadata, Resources resources,
                             OutputStream target, boolean debug) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            Map<FontFace, PDFont> fonts = new HashMap<>();
            PdfStructure structure = new PdfStructure(doc, root(pages));
            PdfStructure.metadata(doc, metadata);
            Rect area = media.pageArea();
            List<PDPage> pdfPages = new ArrayList<>();
            for (Page p : pages) {
                PDPage page = new PDPage(new PDRectangle((float) media.width(), (float) media.height()));
                doc.addPage(page);
                pdfPages.add(page);
                structure.startPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    StackingContext.build(p.content(), p.fixed(), area.x(), area.y())
                            .paint(new PdfCanvas(doc, cs, media.height(), resources, fonts, structure, debug));
                }
            }
            structure.finish(doc, pages, pdfPages, area, media.height());
            doc.save(target);
        }
    }

    /** La raíz del árbol de cajas: la caja del fragmento raíz de cualquier página. */
    private static Box root(List<Page> pages) {
        Box b = pages.get(0).content().box();
        while (b.parent != null) {
            b = b.parent;
        }
        return b;
    }

    @Override
    public void background(Rect r, String color) {
        if (r.width() <= 0 || r.height() <= 0) {
            return;
        }
        try {
            // Fondos y bordes no son contenido: artifacts del PDF etiquetado.
            out.beginMarkedContent(COSName.ARTIFACT);
            setColor(color);
            out.addRect((float) r.x(), (float) (pageHeight - r.bottom()), (float) r.width(), (float) r.height());
            out.fill();
            out.endMarkedContent();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void beginText(Box box) {
        try {
            int mcid = structure.mark(box);
            if (mcid < 0) {
                out.beginMarkedContent(COSName.ARTIFACT);
            } else {
                out.beginMarkedContent(COSName.getPDFName(PdfStructure.type(box)), mcid);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void endText() {
        try {
            out.endMarkedContent();
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
                out.beginMarkedContent(COSName.ARTIFACT);
                out.setStrokingColor(0xDD / 255f, 0x33 / 255f, 0x33 / 255f);
                out.setLineWidth(0.3f);
                out.setLineDashPattern(new float[]{2, 2}, 0);
                out.addRect((float) r.x(), (float) (pageHeight - r.bottom()), (float) r.width(), (float) r.height());
                out.stroke();
                out.endMarkedContent();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public void text(double x, double baselineY, String text, FontFace font, double fontSize, String color,
                     double wordSpacing) {
        try {
            out.beginText();
            out.setFont(pdfFont(font), (float) fontSize);
            setColor(color);
            out.newLineAtOffset((float) x, (float) (pageHeight - baselineY));
            if (wordSpacing == 0) {
                out.showText(text);
            } else {
                out.showTextWithPositioning(justified(text, wordSpacing, fontSize));
            }
            out.endText();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public VerticalMetrics vertical(FontFace font) {
        return resources.fonts().find(font).vertical();
    }

    /** La fuente incrustada; se carga la primera vez que se usa en el documento. */
    private PDFont pdfFont(FontFace font) throws IOException {
        PDFont f = fonts.get(font);
        if (f == null) {
            f = PDType0Font.load(doc, resources.fonts().find(font).path().toFile());
            fonts.put(font, f);
        }
        return f;
    }

    /**
     * Argumento de {@code TJ}: el texto partido tras cada espacio, con el espacio extra entre
     * los trozos. Un número en {@code TJ} desplaza a la izquierda, en milésimas del cuerpo.
     */
    static Object[] justified(String text, double wordSpacing, double fontSize) {
        Float shift = (float) (-wordSpacing * 1000 / fontSize);
        List<Object> parts = new ArrayList<>();
        int start = 0;
        for (int i = text.indexOf(' '); i >= 0; i = text.indexOf(' ', i + 1)) {
            parts.add(text.substring(start, i + 1));
            parts.add(shift);
            start = i + 1;
        }
        parts.add(text.substring(start));
        return parts.toArray();
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
