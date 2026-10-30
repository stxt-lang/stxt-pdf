package dev.stxt.pdf.paint;

import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.model.Limits;
import dev.stxt.pdf.model.NumberStyle;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Media;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDPageLabelRange;
import org.apache.pdfbox.pdmodel.common.PDPageLabels;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * El documento PDF: crea las páginas, pinta cada una con su {@link PdfCanvas}, y pone las etiquetas de
 * las páginas, los destinos de los enlaces y el árbol de estructura ({@link PdfStructure}) antes de guardarlo.
 * Una página PDF por página del resultado, en pt, del tamaño del medio de su sección.
 *
 * <p><strong>Hilos:</strong> {@link #write} es estático y todo su estado (el documento PDF, las fuentes
 * incrustadas, la estructura, los enlaces) es de esa llamada, así que se puede llamar a la vez desde varios
 * hilos, también con el mismo {@link LaidOutDocument} y los mismos {@link Resources}. La única salvedad es
 * global a la JVM: la caché de fuentes de JSVG, que {@link PdfImages} usa con un cerrojo.</p>
 */
public final class PdfDocumentWriter {

    private PdfDocumentWriter() {
    }

    /**
     * Pinta un documento maquetado con {@code new PdfMetrics(resources)}, con el título y el
     * idioma de sus metadatos, y escribe el PDF en {@code target}, que no se cierra. Para maquetar
     * y pintar en una llamada está {@link dev.stxt.pdf.convert.Converter}.
     *
     * @param limits los de las imágenes SVG, que se dibujan con ellos (los comprobó antes el motor que maquetó)
     * @throws IOException un fallo de entrada y salida, también el de las primitivas del lienzo (que no pueden
     *         declararlo y lo lanzan como {@link UncheckedIOException}: aquí salen como lo que son)
     */
    public static void write(LaidOutDocument document, Resources resources, Limits limits, OutputStream target,
            boolean debug) throws IOException {
        write(document, resources, limits, target, debug, 1);
    }

    /**
     * Como {@link #write(LaidOutDocument, Resources, Limits, OutputStream, boolean)}, cumpliendo la parte {@code ua} de
     * PDF/UA: 1 (PDF 1.7) o 2 (PDF 2.0), referencia/implementacion/pdf.md.
     */
    public static void write(LaidOutDocument document, Resources resources, Limits limits, OutputStream target,
            boolean debug, int ua) throws IOException {
        if (ua != 1 && ua != 2) {
            throw new IllegalArgumentException("PDF/UA tiene las partes 1 y 2: " + ua);
        }
        try {
            paintAndSave(document, resources, limits, target, debug, ua);
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private static void paintAndSave(LaidOutDocument document, Resources resources, Limits limits,
            OutputStream target, boolean debug, int ua) throws IOException {
        List<Page> pages = document.pages();
        try (PDDocument doc = new PDDocument()) {
            if (ua == 2) {
                // La cabecera del fichero y la versión del catálogo.
                doc.getDocument().setVersion(2.0f);
                doc.getDocumentCatalog().setVersion("2.0");
            }
            PdfFonts fonts = new PdfFonts(doc, resources);
            PdfImages images = new PdfImages(doc, limits);
            PdfStructure structure = new PdfStructure(doc, document.root(), ua);
            PdfLinks links = new PdfLinks();
            PdfStructure.metadata(doc, document.metadata(), ua);
            // Todas las páginas existen antes de pintar ninguna: un enlace puede ir hacia delante.
            List<PDPage> pdfPages = new ArrayList<>();
            for (int i = 0; i < pages.size(); i++) {
                Media media = pages.get(i).media();
                PDPage page = new PDPage(new PDRectangle((float) media.width(), (float) media.height()));
                doc.addPage(page);
                pdfPages.add(page);
            }
            for (int i = 0; i < pages.size(); i++) {
                Page p = pages.get(i);
                // Cada página tiene el medio de su sección (referencia/componentes/document.md).
                Media media = p.media();
                Rect area = media.pageArea();
                PDPage page = pdfPages.get(i);
                structure.startPage(page);
                PdfCanvas canvas;
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    canvas = new PdfCanvas(doc, cs, media.height(), resources, fonts, images, structure, links,
                            debug);
                    StackingContext.build(p.content(), p.fixed(), area.x(), area.y()).paint(canvas);
                }
                if (!canvas.tagging().annotations().isEmpty()) {
                    page.setAnnotations(canvas.tagging().annotations());
                    // Las anotaciones se recorren en el orden de la estructura (PDF/UA, 7.18.3).
                    page.getCOSObject().setName(COSName.getPDFName("Tabs"), "S");
                }
            }
            labels(doc, pages);
            Map<Box, PDPageXYZDestination> destinations = PdfDestinations.find(pages, pdfPages);
            links.resolve(destinations, structure);
            structure.finish(doc, destinations);
            doc.save(target);
        }
    }

    /**
     * Las etiquetas de las páginas (referencia/componentes/document.md): el visor muestra el número impreso de cada
     * una, y ninguno en las que no se numeran. Solo se escriben si alguna página no es su número físico; si no, no
     * añaden nada. Una etiqueta nueva empieza donde el número no sigue al de la página anterior.
     */
    private static void labels(PDDocument doc, List<Page> pages) {
        if (pages.stream().allMatch(p -> p.printed() == p.number() && p.style() == NumberStyle.DECIMAL)) {
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
