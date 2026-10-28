package dev.stxt.boxes.paint;

import dev.stxt.InlineNode;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.inline.PdfMetrics;
import dev.stxt.boxes.resources.Resources;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Documento STXT → PDF en una llamada: maqueta con las métricas reales de las fuentes y las
 * imágenes de la carpeta de recursos ({@code new Engine(new PdfMetrics(resources), resources)})
 * y pinta con {@link PdfCanvas}. Para usar
 * las páginas antes de pintarlas, se maqueta con {@link Engine#layOutDocument} y se escribe con
 * {@link PdfCanvas#write(dev.stxt.boxes.layout.LaidOutDocument, Resources, OutputStream, boolean)}.
 */
public final class PdfWriter {

    private PdfWriter() {
    }

    /**
     * Maqueta un documento ya validado ({@link dev.stxt.boxes.stxt.DocumentReader}) con las
     * fuentes de la carpeta de recursos por defecto y escribe el PDF en {@code target}, que no
     * se cierra.
     */
    public static void write(InlineNode document, OutputStream target) throws IOException {
        write(document, new Resources(), target, false);
    }

    /** Como {@link #write(InlineNode, OutputStream)}, con un documento de estilos aparte (puede ser {@code null}). */
    public static void write(InlineNode document, InlineNode styles, OutputStream target) throws IOException {
        write(document, styles, new Resources(), target, false);
    }

    /**
     * Como {@link #write(InlineNode, OutputStream)}, con otra carpeta de recursos y los contornos
     * de depuración si {@code debug}.
     */
    public static void write(InlineNode document, Resources resources, OutputStream target, boolean debug)
            throws IOException {
        write(document, null, resources, target, debug);
    }

    /**
     * Como {@link #write(InlineNode, Resources, OutputStream, boolean)}, con un documento de estilos
     * aparte ({@link dev.stxt.boxes.stxt.DocumentReader#readStyles}) cuyas reglas valen menos que las
     * del documento (§4.10). {@code styles} puede ser {@code null}.
     */
    public static void write(InlineNode document, InlineNode styles, Resources resources, OutputStream target,
            boolean debug) throws IOException {
        PdfCanvas.write(new Engine(new PdfMetrics(resources), resources).layOutDocument(document, styles), resources,
                target, debug);
    }
}
