package dev.stxt.boxes.convert;

import dev.stxt.InlineNode;
import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.LaidOutDocument;
import dev.stxt.boxes.layout.inline.PdfMetrics;
import dev.stxt.boxes.paint.PdfCanvas;
import dev.stxt.boxes.paint.SvgCanvas;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.stxt.DocumentReader;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Punto de entrada del motor: documento STXT → PDF (o SVG) con las métricas reales de las fuentes y las
 * imágenes de la carpeta de recursos. Reúne en un objeto lo que antes se pasaba suelto a cada llamada: la
 * carpeta de recursos ({@link Resources}), el documento de estilos aparte (§4.10) y los contornos de
 * depuración.
 *
 * <p><strong>Es inmutable y seguro entre hilos</strong>: se crea una vez y se comparte. Cada conversión
 * crea su propio {@link Engine}, que no se puede compartir (guarda la caché del layout), así que dos
 * hilos que convierten a la vez con el mismo {@code Converter} no comparten ningún estado mutable.</p>
 *
 * <pre>{@code
 * Converter converter = Converter.builder().resources(new Resources(Path.of("otra"))).build();
 * InlineNode document = new DocumentReader().read(Path.of("report.stxt"));
 * converter.toPdf(document, out);
 * }</pre>
 *
 * <p>Para usar las páginas antes de pintarlas (o pintarlas a PDF y a SVG a la vez) se maqueta con
 * {@link #layOut(InlineNode)} y se pinta el resultado con {@link #toPdf(LaidOutDocument, OutputStream)} y
 * {@link #toSvg(LaidOutDocument)}. Un {@link LaidOutDocument} no se cambia después de maquetarlo.</p>
 */
public final class Converter {

    private final Resources resources;
    private final InlineNode styles;
    private final boolean debug;

    private Converter(Builder builder) {
        this.resources = builder.resources;
        this.styles = builder.styles;
        this.debug = builder.debug;
    }

    /** Un conversor con la configuración por defecto: recursos en {@code resources/}, sin estilos aparte ni depuración. */
    public static Converter create() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** La carpeta de recursos con que convierte. */
    public Resources resources() {
        return resources;
    }

    /**
     * Maqueta un documento ya validado ({@link DocumentReader}) con las métricas reales de las fuentes de la
     * carpeta de recursos. Falla con los errores del documento (§6) antes de que se escriba nada.
     */
    public LaidOutDocument layOut(InlineNode document) {
        Objects.requireNonNull(document, "document");
        return new Engine(new PdfMetrics(resources), resources).layOutDocument(document, styles);
    }

    /** Maqueta {@code document} y escribe el PDF en {@code target}, que no se cierra. */
    public void toPdf(InlineNode document, OutputStream target) throws IOException {
        toPdf(layOut(document), target);
    }

    /**
     * Escribe el PDF de un documento ya maquetado por este conversor (con las mismas fuentes, que son las que
     * se incrustan) en {@code target}, que no se cierra.
     */
    public void toPdf(LaidOutDocument document, OutputStream target) throws IOException {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(target, "target");
        PdfCanvas.write(document, resources, target, debug);
    }

    /** El SVG de cada página de un documento ya maquetado, en orden. */
    public List<String> toSvg(LaidOutDocument document) {
        Objects.requireNonNull(document, "document");
        return document.pages().stream()
                .map((Page page) -> SvgCanvas.page(page, page.media(), debug, resources))
                .toList();
    }

    /** Configura un {@link Converter}; cada opción es opcional. */
    public static final class Builder {

        private Resources resources = new Resources();
        private InlineNode styles;
        private boolean debug;

        private Builder() {
        }

        /** Carpeta de recursos (§5.11); por defecto {@link Resources#DEFAULT_ROOT}. */
        public Builder resources(Resources resources) {
            this.resources = Objects.requireNonNull(resources, "resources");
            return this;
        }

        /**
         * Documento de estilos aparte ya leído ({@link DocumentReader#readStyles(String)}), cuyas reglas
         * valen menos que las del documento (§4.10). Sin él, el documento lleva todos sus estilos.
         */
        public Builder styles(InlineNode styles) {
            this.styles = Objects.requireNonNull(styles, "styles");
            return this;
        }

        /** Como {@link #styles(InlineNode)}, leyéndolo de un texto STXT. */
        public Builder styles(String content) {
            return styles(new DocumentReader().readStyles(content));
        }

        /** Como {@link #styles(InlineNode)}, leyéndolo de un fichero UTF-8. */
        public Builder styles(Path file) {
            return styles(new DocumentReader().readStyles(file));
        }

        /** Dibuja el contorno de cada caja, para revisar un documento a mano; por defecto no. */
        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public Converter build() {
            return new Converter(this);
        }
    }
}
