package dev.stxt.pdf.convert;

import dev.stxt.InlineNode;
import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.layout.Engine;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.layout.inline.PdfMetrics;
import dev.stxt.pdf.model.Check;
import dev.stxt.pdf.model.InternalException;
import dev.stxt.pdf.model.Limits;
import dev.stxt.pdf.model.StxtPdfException;
import dev.stxt.pdf.paint.ContrastCheck;
import dev.stxt.pdf.paint.PdfDocumentWriter;
import dev.stxt.pdf.paint.SvgCanvas;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.stxt.DocumentReader;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Punto de entrada del motor: documento STXT → PDF (o SVG) con las métricas reales de las fuentes y las imágenes de la
 * carpeta de recursos. Reúne en un objeto lo que antes se pasaba suelto a cada llamada: la carpeta de recursos ({@link
 * Resources}), el documento de estilos aparte (referencia/comun/estilos.md) y los contornos de depuración.
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
 * <p><strong>Errores:</strong> todo fallo de la conversión sale como {@link StxtPdfException}, así que una sola
 * {@code catch} basta; su {@link StxtPdfException#kind() kind} dice de quién es la culpa (el documento, los
 * recursos, un límite o el propio motor), y {@code line()} y {@code code()} dónde y qué. Un error inesperado de
 * cualquier otra clase se convierte en un {@link InternalException} que guarda el original. Los fallos de
 * entrada y salida al escribir salen como {@link IOException}.</p>
 *
 * <p>Para usar las páginas antes de pintarlas (o pintarlas a PDF y a SVG a la vez) se maqueta con
 * {@link #layOut(InlineNode)} y se pinta el resultado con {@link #toPdf(LaidOutDocument, OutputStream)} y
 * {@link #toSvg(LaidOutDocument)}. Un {@link LaidOutDocument} no se cambia después de maquetarlo.</p>
 */
public final class Converter {

    private final Resources resources;
    private final InlineNode styles;
    private final boolean debug;
    private final Limits limits;
    private final Set<Check> excluded;
    private final int pdfUa;

    private Converter(Builder builder) {
        this.resources = builder.resources;
        this.styles = builder.styles;
        this.debug = builder.debug;
        this.limits = builder.limits;
        this.excluded = Collections.unmodifiableSet(EnumSet.copyOf(builder.excluded));
        this.pdfUa = builder.pdfUa;
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

    /** Lo máximo que acepta ({@link Limits}). */
    public Limits limits() {
        return limits;
    }

    /** Las comprobaciones que no hace ({@link Builder#exclude}); vacío si las hace todas. */
    public Set<Check> excluded() {
        return excluded;
    }

    /**
     * Maqueta un documento ya validado ({@link DocumentReader}) con las métricas reales de las fuentes de la carpeta de
     * recursos. Falla con los errores del documento (referencia/comun/errores.md) antes de que se escriba nada,
     * también con los de las comprobaciones que no se han quitado ({@link Builder#exclude}): el poco contraste de un
     * texto y lo que impide dibujar igual la página de un PDF.
     */
    public LaidOutDocument layOut(InlineNode document) {
        Objects.requireNonNull(document, "document");
        return guarded(() -> {
            LaidOutDocument laidOut = new Engine(new PdfMetrics(resources), resources, limits, excluded)
                    .layOutDocument(document, styles);
            if (!excluded.contains(Check.CONTRAST)) {
                ContrastCheck.check(laidOut);
            }
            return laidOut;
        });
    }

    /** Maqueta {@code document} y escribe el PDF en {@code target}, que no se cierra. */
    public void toPdf(InlineNode document, OutputStream target) throws IOException {
        Objects.requireNonNull(target, "target");
        toPdf(layOut(document), target);
    }

    /**
     * Escribe el PDF de un documento ya maquetado por este conversor (con las mismas fuentes, que son las que
     * se incrustan) en {@code target}, que no se cierra.
     */
    public void toPdf(LaidOutDocument document, OutputStream target) throws IOException {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(target, "target");
        guarded(() -> PdfDocumentWriter.write(document, resources, limits, target, debug, pdfUa));
    }

    /** El SVG de cada página de un documento ya maquetado, en orden. */
    public List<String> toSvg(LaidOutDocument document) {
        Objects.requireNonNull(document, "document");
        return guarded(() -> document.pages().stream()
                .map((Page page) -> SvgCanvas.page(page, page.pageSetup(), debug, resources))
                .toList());
    }

    /** Un trabajo que escribe en un destino. */
    private interface Writing {
        void run() throws IOException;
    }

    /**
     * Ejecuta {@code work} dejando pasar los errores del motor ({@link StxtPdfException}) y convirtiendo cualquier
     * otra excepción de ejecución en un {@link InternalException}.
     */
    private static <T> T guarded(Supplier<T> work) {
        try {
            return work.get();
        } catch (StxtPdfException e) {
            throw e;
        } catch (RuntimeException e) {
            throw unexpected(e);
        }
    }

    /** Como {@link #guarded(Supplier)}, para lo que escribe: los fallos de entrada y salida salen como son. */
    private static void guarded(Writing work) throws IOException {
        try {
            work.run();
        } catch (StxtPdfException | IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw unexpected(e);
        }
    }

    private static InternalException unexpected(RuntimeException e) {
        return new InternalException("Fallo inesperado al convertir: " + e, e);
    }

    /** Configura un {@link Converter}; cada opción es opcional. */
    public static final class Builder {

        private Resources resources = new Resources();
        private InlineNode styles;
        private boolean debug;
        private Limits limits = Limits.DEFAULT;
        private final Set<Check> excluded = EnumSet.noneOf(Check.class);
        private int pdfUa = 1;

        private Builder() {
        }

        /** Carpeta de recursos (referencia/comun/recursos.md); por defecto {@link Resources#DEFAULT_ROOT}. */
        public Builder resources(Resources resources) {
            this.resources = Objects.requireNonNull(resources, "resources");
            return this;
        }

        /**
         * Documento de estilos aparte ya leído ({@link DocumentReader#readStyles(String)}), cuyas reglas valen menos
         * que las del documento (referencia/comun/estilos.md). Sin él, el documento lleva todos sus estilos.
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

        /**
         * Lo máximo que acepta la conversión (páginas, píxeles de una imagen, elementos y niveles de un SVG, bytes de
         * un PDF); por defecto {@link Limits#DEFAULT}. Un documento que lo supera falla con un error {@code LIMIT}.
         */
        public Builder limits(Limits limits) {
            this.limits = Objects.requireNonNull(limits, "limits");
            return this;
        }

        /**
         * Quita esas comprobaciones ({@link Check}): el contraste del texto con su fondo (referencia/comun/pdf.md) y lo
         * que impide dibujar igual la página de un PDF (referencia/componentes/image.md). Por defecto se hacen todas;
         * cada llamada añade las suyas a las que ya se quitaron.
         */
        public Builder exclude(Check... checks) {
            for (Check check : checks) {
                excluded.add(Objects.requireNonNull(check, "check"));
            }
            return this;
        }

        /**
         * La parte de PDF/UA que cumple el PDF (referencia/comun/pdf.md): 1, en PDF 1.7, o 2, en PDF 2.0. Por
         * defecto 1.
         *
         * @throws IllegalArgumentException si no es 1 ni 2
         */
        public Builder pdfUa(int part) {
            if (part != 1 && part != 2) {
                throw new IllegalArgumentException("PDF/UA tiene las partes 1 y 2: " + part);
            }
            this.pdfUa = part;
            return this;
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
