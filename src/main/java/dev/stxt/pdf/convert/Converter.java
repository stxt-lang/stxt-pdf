package dev.stxt.pdf.convert;

import dev.stxt.InlineNode;
import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.layout.Engine;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.layout.inline.PdfMetrics;
import dev.stxt.pdf.model.Attachments;
import dev.stxt.pdf.model.Check;
import dev.stxt.pdf.model.InternalException;
import dev.stxt.pdf.model.Limits;
import dev.stxt.pdf.model.PdfA;
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
 * Entry point of the engine: STXT document → PDF (or SVG) with the real metrics of the fonts and the images of the
 * resources folder. It gathers in one object what used to be passed separately to each call: the resources folder
 * ({@link Resources}), the separate styles document (reference/common/styles.md) and the debug outlines.
 *
 * <p><strong>It is immutable and thread-safe</strong>: it is created once and shared. Each conversion
 * creates its own {@link Engine}, which cannot be shared (it holds the layout cache), so two
 * threads that convert at the same time with the same {@code Converter} share no mutable state.</p>
 *
 * <pre>{@code
 * Converter converter = Converter.builder().resources(new Resources(Path.of("other"))).build();
 * InlineNode document = new DocumentReader().read(Path.of("report.stxt"));
 * converter.toPdf(document, out);
 * }</pre>
 *
 * <p><strong>Errors:</strong> every failure of the conversion comes out as a {@link StxtPdfException}, so a single
 * {@code catch} is enough; its {@link StxtPdfException#kind() kind} says whose fault it is (the document, the
 * resources, a limit or the engine itself), and {@code line()} and {@code code()} where and what. An unexpected error
 * of any other class is turned into an {@link InternalException} that keeps the original. Input and output failures
 * when writing come out as {@link IOException}.</p>
 *
 * <p>To use the pages before painting them (or to paint them to PDF and SVG at once), the document is laid out with
 * {@link #layOut(InlineNode)} and the result is painted with {@link #toPdf(LaidOutDocument, OutputStream)} and
 * {@link #toSvg(LaidOutDocument)}. A {@link LaidOutDocument} does not change after it is laid out.</p>
 *
 * <p>What belongs to one document and not to the configuration, the files to embed and their XMP
 * ({@link Attachments}, for Factur-X), goes with each call: {@link #toPdf(InlineNode, OutputStream, Attachments)}.
 * Embedding a file needs {@link Builder#pdfA PDF/A-3}.</p>
 */
public final class Converter {

    private final Resources resources;
    private final InlineNode styles;
    private final boolean debug;
    private final Limits limits;
    private final Set<Check> excluded;
    private final int pdfUa;
    private final PdfA pdfA;

    private Converter(Builder builder) {
        this.resources = builder.resources;
        this.styles = builder.styles;
        this.debug = builder.debug;
        this.limits = builder.limits;
        this.excluded = Collections.unmodifiableSet(EnumSet.copyOf(builder.excluded));
        this.pdfUa = builder.pdfUa;
        this.pdfA = builder.pdfA;
    }

    /** A converter with the default configuration: resources in {@code resources/}, no separate styles, no debug. */
    public static Converter create() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The resources folder it converts with. */
    public Resources resources() {
        return resources;
    }

    /** The most it accepts ({@link Limits}). */
    public Limits limits() {
        return limits;
    }

    /** The checks it does not run ({@link Builder#exclude}); empty if it runs all of them. */
    public Set<Check> excluded() {
        return excluded;
    }

    /** The part of PDF/UA its PDFs comply with ({@link Builder#pdfUa}): 1 or 2. */
    public int pdfUa() {
        return pdfUa;
    }

    /** The PDF/A its PDFs comply with ({@link Builder#pdfA}), or {@code null} if they declare none. */
    public PdfA pdfA() {
        return pdfA;
    }

    /**
     * Lays out an already validated document ({@link DocumentReader}) with the real metrics of the fonts of the
     * resources folder. It fails with the errors of the document (reference/common/errors.md) before anything is
     * written, also with those of the checks that have not been turned off ({@link Builder#exclude}): the low contrast
     * of a text and whatever prevents the page of a PDF from being drawn faithfully.
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

    /** Lays out {@code document} and writes the PDF to {@code target}, which is not closed. */
    public void toPdf(InlineNode document, OutputStream target) throws IOException {
        toPdf(document, target, Attachments.NONE);
    }

    /**
     * Lays out {@code document} and writes the PDF, with the files and the XMP of {@code attachments} embedded
     * (reference/implementation/api.md), to {@code target}, which is not closed. A file to embed needs
     * {@link Builder#pdfA PDF/A-3}: otherwise, {@code ATTACHMENT_REQUIRES_PDFA3}.
     */
    public void toPdf(InlineNode document, OutputStream target, Attachments attachments) throws IOException {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(attachments, "attachments");
        toPdf(layOut(document), target, attachments);
    }

    /**
     * Writes the PDF of a document already laid out by this converter (with the same fonts, which are the ones
     * that get embedded) to {@code target}, which is not closed.
     */
    public void toPdf(LaidOutDocument document, OutputStream target) throws IOException {
        toPdf(document, target, Attachments.NONE);
    }

    /**
     * Like {@link #toPdf(LaidOutDocument, OutputStream)}, with the files and the XMP of {@code attachments}
     * embedded ({@link #toPdf(InlineNode, OutputStream, Attachments)}).
     */
    public void toPdf(LaidOutDocument document, OutputStream target, Attachments attachments) throws IOException {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(attachments, "attachments");
        guarded(() -> PdfDocumentWriter.write(document, resources, limits, target, debug, pdfUa, pdfA, attachments));
    }

    /** The SVG of each page of an already laid out document, in order. */
    public List<String> toSvg(LaidOutDocument document) {
        Objects.requireNonNull(document, "document");
        return guarded(() -> document.pages().stream()
                .map((Page page) -> SvgCanvas.page(page, page.pageSetup(), debug, resources))
                .toList());
    }

    /** A job that writes to a destination. */
    private interface Writing {
        void run() throws IOException;
    }

    /**
     * Runs {@code work}, letting the errors of the engine ({@link StxtPdfException}) through and turning any
     * other runtime exception into an {@link InternalException}.
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

    /** Like {@link #guarded(Supplier)}, for what writes: input and output failures come out as they are. */
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
        return new InternalException("Unexpected failure while converting: " + e, e);
    }

    /** Configures a {@link Converter}; every option is optional. */
    public static final class Builder {

        private Resources resources = new Resources();
        private InlineNode styles;
        private boolean debug;
        private Limits limits = Limits.DEFAULT;
        private final Set<Check> excluded = EnumSet.noneOf(Check.class);
        private int pdfUa = 1;
        private PdfA pdfA;

        private Builder() {
        }

        /** Resources folder (reference/common/resources.md); by default {@link Resources#DEFAULT_ROOT}. */
        public Builder resources(Resources resources) {
            this.resources = Objects.requireNonNull(resources, "resources");
            return this;
        }

        /**
         * Separate styles document, already read ({@link DocumentReader#readStyles(String)}), whose rules weigh less
         * than those of the document (reference/common/styles.md). Without it, the document carries all its styles.
         */
        public Builder styles(InlineNode styles) {
            this.styles = Objects.requireNonNull(styles, "styles");
            return this;
        }

        /** Like {@link #styles(InlineNode)}, reading it from an STXT text. */
        public Builder styles(String content) {
            return styles(new DocumentReader().readStyles(content));
        }

        /** Like {@link #styles(InlineNode)}, reading it from a UTF-8 file. */
        public Builder styles(Path file) {
            return styles(new DocumentReader().readStyles(file));
        }

        /**
         * The most the conversion accepts (pages, pixels of an image, elements and levels of an SVG, bytes of
         * a PDF); by default {@link Limits#DEFAULT}. A document that exceeds it fails with a {@code LIMIT} error.
         */
        public Builder limits(Limits limits) {
            this.limits = Objects.requireNonNull(limits, "limits");
            return this;
        }

        /**
         * Turns off these checks ({@link Check}): the contrast of the text with its background
         * (reference/common/pdf.md) and whatever prevents the page of a PDF from being drawn faithfully
         * (reference/components/image.md). By default all of them run; each call adds its own to those already turned
         * off.
         */
        public Builder exclude(Check... checks) {
            for (Check check : checks) {
                excluded.add(Objects.requireNonNull(check, "check"));
            }
            return this;
        }

        /**
         * The part of PDF/UA the PDF conforms to (reference/common/pdf.md): 1, in PDF 1.7, or 2, in PDF 2.0. By
         * default 1.
         *
         * @throws IllegalArgumentException if it is neither 1 nor 2
         */
        public Builder pdfUa(int part) {
            if (part != 1 && part != 2) {
                throw new IllegalArgumentException("PDF/UA has parts 1 and 2: " + part);
            }
            this.pdfUa = part;
            return this;
        }

        /**
         * The PDF/A the PDF conforms to, besides PDF/UA (reference/common/pdf.md): PDF/A-2 or PDF/A-3, level
         * {@code b} or {@code a}. By default none. PDF/A-3 is the one that admits files of any kind
         * ({@link Attachments}). The PDF/A parts are PDF 1.7 files: with {@link #pdfUa pdfUa(2)}, which is PDF 2.0,
         * {@link #build} fails.
         */
        public Builder pdfA(PdfA pdfA) {
            this.pdfA = Objects.requireNonNull(pdfA, "pdfA");
            return this;
        }

        /** Draws the outline of each box, to review a document by hand; off by default. */
        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        /**
         * @throws IllegalArgumentException with {@link #pdfUa pdfUa(2)} and a {@link #pdfA}: PDF/UA-2 is a PDF 2.0
         *         file and PDF/A-2 and PDF/A-3 are PDF 1.7 ones
         */
        public Converter build() {
            if (pdfUa == 2 && pdfA != null) {
                throw new IllegalArgumentException("PDF/UA-2 is a PDF 2.0 file and " + pdfA
                        + " a PDF 1.7 one: a PDF cannot comply with both");
            }
            return new Converter(this);
        }
    }
}
