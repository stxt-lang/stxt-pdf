package dev.stxt.pdf.convert;

import dev.stxt.InlineNode;
import dev.stxt.pdf.layout.Engine;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.layout.image.Images;
import dev.stxt.pdf.layout.inline.PdfMetrics;
import dev.stxt.pdf.model.Attachments;
import dev.stxt.pdf.model.Cancellation;
import dev.stxt.pdf.model.Check;
import dev.stxt.pdf.model.InternalException;
import dev.stxt.pdf.model.Limits;
import dev.stxt.pdf.model.PdfA;
import dev.stxt.pdf.model.Progress;
import dev.stxt.pdf.model.StxtPdfException;
import dev.stxt.pdf.paint.ContrastCheck;
import dev.stxt.pdf.paint.PdfDocumentWriter;
import dev.stxt.pdf.paint.SvgCanvas;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.stxt.DocumentReader;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Entry point of the engine: STXT document → PDF (or SVG) with the real metrics of the fonts and the images of the
 * resources folder. It gathers in one object the resources folder ({@link Resources}), the separate styles documents
 * (reference/common/styles.md) and the debug outlines.
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
 * <p><strong>Changes in the resources folder</strong> (reference/implementation/api.md): a converter reads each font
 * and each image the first time it uses it and keeps it, so the files added, removed or replaced afterwards are seen
 * by the converter {@link #reloaded()} gives, which does not read again what has not changed. Built with
 * {@link Builder#preloadResources}, it reads the whole folder when it is created and reloaded instead of file by file;
 * built with {@link Builder#reloadResourcesEachConversion}, each conversion sees the folder as it is when it starts,
 * without calling anything. A {@link LaidOutDocument} keeps the resources it was laid out with, and
 * {@link #toPdf(LaidOutDocument, OutputStream)} and {@link #toSvg(LaidOutDocument)} paint it with them.</p>
 *
 * <p>What belongs to one document and not to the configuration, the files to embed and their XMP
 * ({@link Attachments}, for Factur-X), goes with each call: {@link #toPdf(InlineNode, OutputStream, Attachments)}.
 * Embedding a file needs {@link Builder#pdfA PDF/A-3}.</p>
 *
 * <p>A conversion stops before the end when its {@link Cancellation} asks for it (a deadline, an interrupted
 * thread): {@link #toPdf(InlineNode, OutputStream, Attachments, Cancellation)} for one, or
 * {@link Builder#cancellation} for all of them. It ends with a {@link dev.stxt.pdf.model.CancelledException} and
 * nothing written. A {@link Progress} given with the call is told each page laid out and painted, for a progress
 * bar: {@link #toPdf(InlineNode, OutputStream, Attachments, Cancellation, Progress)}.</p>
 *
 * <p>To add to the PDF what the engine does not write (keys of its own in the information dictionary, a stamp,
 * encryption), the document is written into an open {@link PDDocument} that the caller saves afterwards:
 * {@link #toPdf(LaidOutDocument, PDDocument)}. A single laid out page is painted on a page of any document with
 * {@link dev.stxt.pdf.paint.PdfPainter}.</p>
 */
public final class Converter {

    /** The resource with the version of the engine, written by Maven ({@code src/main/resources-filtered}). */
    private static final String VERSION_RESOURCE = "/dev/stxt/pdf/version.properties";

    private static final String VERSION = readVersion();

    private final Resources resources;
    /** Whether each conversion takes a snapshot of the resources ({@link Builder#reloadResourcesEachConversion}). */
    private final boolean reloadEachConversion;
    /** Whether the snapshot is read whole when it is taken ({@link Builder#preloadResources}). */
    private final boolean preload;
    /**
     * The snapshot of the resources every conversion works with, taken when it is created (and read whole if
     * {@link #preload}); {@code null} if each conversion takes its own.
     */
    private final Resources current;
    /** The images its conversions have besides those of the resources, by their path ({@link #withImages}). */
    private final Map<String, byte[]> images;
    /** The separate styles documents, in the order they are applied ({@link Builder#styles(InlineNode)}). */
    private final List<InlineNode> styles;
    private final boolean debug;
    private final Limits limits;
    private final Set<Check> excluded;
    private final int pdfUa;
    private final PdfA pdfA;
    private final Cancellation cancellation;

    private Converter(Builder builder) {
        this.resources = builder.resources;
        this.reloadEachConversion = builder.reloadEachConversion;
        this.preload = builder.preload;
        this.limits = builder.limits;
        this.images = builder.images;
        // Built on another converter ({@link #toBuilder}) with the same resources options, it shares its snapshot.
        this.current = builder.keepsTheSnapshot() ? builder.base.current : snapshot();
        this.styles = List.copyOf(builder.styles);
        this.debug = builder.debug;
        this.excluded = Collections.unmodifiableSet(EnumSet.copyOf(builder.excluded));
        this.pdfUa = builder.pdfUa;
        this.pdfA = builder.pdfA;
        this.cancellation = builder.cancellation;
    }

    /** The same configuration as {@code other}, with a new snapshot of its resources ({@link #reloaded()}). */
    private Converter(Converter other) {
        this(other, null);
    }

    /**
     * The same configuration as {@code other}: with a new snapshot of its resources and its images if {@code images}
     * is {@code null} ({@link #reloaded()}), or with its snapshot and those images ({@link #withImages}).
     */
    private Converter(Converter other, Map<String, byte[]> images) {
        this.resources = other.resources;
        this.reloadEachConversion = other.reloadEachConversion;
        this.preload = other.preload;
        this.limits = other.limits;
        this.images = images != null ? Collections.unmodifiableMap(images) : other.images;
        this.current = images != null ? other.current : snapshot();
        this.styles = other.styles;
        this.debug = other.debug;
        this.excluded = other.excluded;
        this.pdfUa = other.pdfUa;
        this.pdfA = other.pdfA;
        this.cancellation = other.cancellation;
    }

    /**
     * {@return the version of the engine, the one of its jar ({@code 0.13.0})} It is read from a resource that Maven
     * writes when it builds the jar, not from the manifest: a jar that joins the engine with other dependencies keeps a
     * single manifest, that of the jar that joins them.
     */
    public static String version() {
        return VERSION;
    }

    /**
     * {@return a converter with the default configuration: resources in {@code resources/}, no separate styles, no
     * debug}
     */
    public static Converter create() {
        return builder().build();
    }

    /** {@return a new builder with every option at its default, to configure a converter} */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@return a builder with the configuration of this converter, to build one that differs in some options: the
     * styles of one request, its {@code debug}, its part of PDF/UA} What it builds shares the resources of this
     * converter as it has read them (its snapshot), without looking at the folders again, unless
     * {@link Builder#resources}, {@link Builder#reloadResourcesEachConversion} or {@link Builder#preloadResources}
     * change: then it takes a snapshot of its own, as {@link #builder()} would. The styles of this converter are in
     * the builder, and {@link Builder#styles} adds after them, so what is added weighs more; so are its images
     * ({@link #withImages}). This converter does not change. It is what a service does with a shared converter and the
     * options of each request (reference/implementation/api.md): build one on it for that request and throw it away.
     */
    public Builder toBuilder() {
        Builder b = new Builder();
        b.base = this;
        b.resources = resources;
        b.reloadEachConversion = reloadEachConversion;
        b.preload = preload;
        b.images = images;
        b.styles.addAll(styles);
        b.debug = debug;
        b.limits = limits;
        b.excluded.addAll(excluded);
        b.pdfUa = pdfUa;
        b.pdfA = pdfA;
        b.cancellation = cancellation;
        return b;
    }

    /** {@return the resources folder it converts with} */
    public Resources resources() {
        return resources;
    }

    /**
     * {@return a converter with the same configuration that sees the resources folder as it is now} The fonts and the
     * images whose files have not changed are not read again (reference/implementation/api.md). This converter does
     * not change: what it converts afterwards still uses the files it had read. It is what has to be called after
     * adding, removing or replacing files of the folder, once they are all in place; with
     * {@link Builder#reloadResourcesEachConversion} it is not needed, and it gives an equivalent converter. With
     * {@link Builder#preloadResources} it reads the folder before returning.
     *
     * @throws dev.stxt.pdf.resources.ResourceException with {@link Builder#preloadResources}, if the fonts cannot be
     *         indexed; this converter can still be used
     */
    public Converter reloaded() {
        return new Converter(this);
    }

    /** {@return whether each conversion sees the resources folder as it is when it starts} */
    public boolean reloadsResourcesEachConversion() {
        return reloadEachConversion;
    }

    /** {@return whether it reads the fonts and the images of the folder when it is created and when it is reloaded} */
    public boolean preloadsResources() {
        return preload;
    }

    /**
     * The snapshot of the resources the conversions of a new converter work with, read whole if {@link #preload}; or
     * {@code null} if each conversion takes its own.
     *
     * @throws dev.stxt.pdf.resources.ResourceException if it preloads and the fonts cannot be indexed
     */
    private Resources snapshot() {
        if (reloadEachConversion) {
            return null;
        }
        Resources snapshot = resources.snapshot();
        if (preload) {
            preload(snapshot);
        }
        return snapshot;
    }

    /**
     * Reads the fonts of {@code fonts/} and every image of {@code images/} that can be read, so that the snapshot
     * keeps the files as they are now. A font that cannot be indexed is an error, as in the first conversion. An image
     * that cannot be read (it is not an image, its format is not supported, it is a PDF beyond
     * {@link Limits#maxPdfBytes()}, it leaves the folder through a link) is left for when a document uses it, which
     * reports it with the line of its {@code Image}.
     */
    private void preload(Resources snapshot) {
        snapshot.fonts();
        // A folder that cannot be walked gives no paths: the documents that use its images report it.
        for (String path : snapshot.imagePaths()) {
            try {
                if (snapshot.images().pdfBytes(path) <= limits.maxPdfBytes()) {
                    snapshot.images().find(path);
                }
            } catch (StxtPdfException e) {
                // Left for the document that uses it.
            }
        }
    }

    /**
     * The resources a conversion works with from start to end: those of the converter, or a new snapshot, with the
     * images added for its conversions ({@link #withImages}).
     */
    private Resources forConversion() {
        Resources snapshot = reloadEachConversion ? resources.snapshot() : current;
        return images.isEmpty() ? snapshot : snapshot.withImages(images);
    }

    /**
     * {@return a converter with the same configuration and the same resources, already read or not, whose
     * conversions also have the images of {@code images}, each at the path a document writes for it} They are the
     * images one document brings with it, which are in no folder (reference/implementation/api.md): the converter
     * that is shared stays as it is, and this one is used for that document and thrown away. An image given here goes
     * before one of the resources with the same path, and before one given to the converter it comes from. Nothing is
     * read again: creating it costs copying the bytes. Each image is read when a document uses it, and what is wrong
     * with it is an error of that document, with the line of its {@code Image}.
     *
     * @param images the content of each image (a PNG, a JPEG, an SVG or a PDF as it is) by its path, relative and
     *        without {@code ..}; not {@code null}. The bytes are copied
     * @throws IllegalArgumentException if a path is blank, absolute or has {@code ..}
     */
    public Converter withImages(Map<String, byte[]> images) {
        Map<String, byte[]> copy = new LinkedHashMap<>(this.images);
        images.forEach((path, content) -> copy.put(path, content.clone()));
        // The paths are checked here, not in the first conversion.
        resources.withImages(copy);
        return new Converter(this, copy);
    }

    /** {@return the most it accepts ({@link Limits})} */
    public Limits limits() {
        return limits;
    }

    /** {@return the checks it does not run ({@link Builder#exclude}); empty if it runs all of them} */
    public Set<Check> excluded() {
        return excluded;
    }

    /** {@return the part of PDF/UA its PDFs comply with ({@link Builder#pdfUa}): 1 or 2} */
    public int pdfUa() {
        return pdfUa;
    }

    /** {@return the PDF/A its PDFs comply with ({@link Builder#pdfA}), or {@code null} if they declare none} */
    public PdfA pdfA() {
        return pdfA;
    }

    /**
     * {@return what stops every conversion of this converter ({@link Builder#cancellation}); {@link Cancellation#NONE}
     * if nothing}
     */
    public Cancellation cancellation() {
        return cancellation;
    }

    /**
     * Lays out an already validated document ({@link DocumentReader}) with the real metrics of the fonts of the
     * resources folder. It fails with the errors of the document (reference/common/errors.md) before anything is
     * written, also with those of the checks that have not been turned off ({@link Builder#exclude}): the low contrast
     * of a text and whatever prevents the page of a PDF from being drawn faithfully.
     *
     * @param document the document as {@link DocumentReader} returns it; not {@code null}
     * @return the laid out pages, ready to paint to PDF or SVG with the fonts and images they were measured with
     */
    public LaidOutDocument layOut(InlineNode document) {
        return layOut(document, Cancellation.NONE);
    }

    /**
     * Like {@link #layOut(InlineNode)}, stopping with a {@link dev.stxt.pdf.model.CancelledException} once
     * {@code cancellation}, or the one of the converter ({@link Builder#cancellation}), asks for it
     * (reference/implementation/api.md).
     *
     * @param document the document as {@link DocumentReader} returns it; not {@code null}
     * @param cancellation what stops this conversion, besides the one of the converter; {@link Cancellation#NONE}
     *        for nothing, not {@code null}
     * @return the laid out pages
     */
    public LaidOutDocument layOut(InlineNode document, Cancellation cancellation) {
        return layOut(document, cancellation, Progress.NONE);
    }

    /**
     * Like {@link #layOut(InlineNode, Cancellation)}, telling {@code progress} each page of the flow it lays out and,
     * at the end, the number of pages (reference/implementation/api.md).
     *
     * @param document the document as {@link DocumentReader} returns it; not {@code null}
     * @param cancellation what stops this conversion, besides the one of the converter; {@link Cancellation#NONE}
     *        for nothing, not {@code null}
     * @param progress what is told how the layout goes; {@link Progress#NONE} for nothing, not {@code null}
     * @return the laid out pages
     */
    public LaidOutDocument layOut(InlineNode document, Cancellation cancellation, Progress progress) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(progress, "progress");
        Cancellation stop = this.cancellation.or(Objects.requireNonNull(cancellation, "cancellation"));
        return guarded(() -> {
            // The same resources for this whole conversion: the document is painted with them too.
            Resources current = forConversion();
            LaidOutDocument laidOut = new Engine(new PdfMetrics(current), current, limits, excluded, stop,
                    progress).layOutDocument(document, styles);
            if (!excluded.contains(Check.CONTRAST)) {
                ContrastCheck.check(laidOut, stop);
            }
            if (pdfA != null) {
                new Images(current, limits, excluded).validatePdfA(laidOut.root(), pdfA);
            }
            return laidOut;
        });
    }

    /**
     * Lays out {@code document} and writes the PDF to {@code target}, which is not closed.
     *
     * @param document the document as {@link DocumentReader} returns it; not {@code null}
     * @param target where the PDF is written; not {@code null}
     * @throws IOException if writing to {@code target} fails
     */
    public void toPdf(InlineNode document, OutputStream target) throws IOException {
        toPdf(document, target, Attachments.NONE);
    }

    /**
     * Lays out {@code document} and writes the PDF, with the files and the XMP of {@code attachments} embedded
     * (reference/implementation/api.md), to {@code target}, which is not closed. A file to embed needs
     * {@link Builder#pdfA PDF/A-3}: otherwise, {@code ATTACHMENT_REQUIRES_PDFA3}.
     *
     * @param document the document as {@link DocumentReader} returns it; not {@code null}
     * @param target where the PDF is written; not {@code null}
     * @param attachments the files and the XMP of this document; {@link Attachments#NONE} for none, not {@code null}
     * @throws IOException if writing to {@code target} fails
     */
    public void toPdf(InlineNode document, OutputStream target, Attachments attachments) throws IOException {
        toPdf(document, target, attachments, Cancellation.NONE);
    }

    /**
     * Like {@link #toPdf(InlineNode, OutputStream, Attachments)}, stopping with a
     * {@link dev.stxt.pdf.model.CancelledException} once {@code cancellation}, or the one of the converter
     * ({@link Builder#cancellation}), asks for it (reference/implementation/api.md), in any phase; nothing has been
     * written to {@code target} then.
     *
     * @param document the document as {@link DocumentReader} returns it; not {@code null}
     * @param target where the PDF is written; not {@code null}
     * @param attachments the files and the XMP of this document; {@link Attachments#NONE} for none, not {@code null}
     * @param cancellation what stops this conversion, besides the one of the converter; {@link Cancellation#NONE}
     *        for nothing, not {@code null}
     * @throws IOException if writing to {@code target} fails
     */
    public void toPdf(InlineNode document, OutputStream target, Attachments attachments, Cancellation cancellation)
            throws IOException {
        toPdf(document, target, attachments, cancellation, Progress.NONE);
    }

    /**
     * Like {@link #toPdf(InlineNode, OutputStream, Attachments, Cancellation)}, telling {@code progress} each page
     * laid out and painted and, once the PDF is written, that it is done (reference/implementation/api.md).
     *
     * @param document the document as {@link DocumentReader} returns it; not {@code null}
     * @param target where the PDF is written; not {@code null}
     * @param attachments the files and the XMP of this document; {@link Attachments#NONE} for none, not {@code null}
     * @param cancellation what stops this conversion, besides the one of the converter; {@link Cancellation#NONE}
     *        for nothing, not {@code null}
     * @param progress what is told how the conversion goes; {@link Progress#NONE} for nothing, not {@code null}
     * @throws IOException if writing to {@code target} fails
     */
    public void toPdf(InlineNode document, OutputStream target, Attachments attachments, Cancellation cancellation,
                      Progress progress) throws IOException {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(attachments, "attachments");
        toPdf(layOut(document, cancellation, progress), target, attachments, cancellation, progress);
    }

    /**
     * Lays out {@code document} and writes the PDF into {@code target}, an open PDFBox document without pages, which
     * the caller saves ({@link #toPdf(LaidOutDocument, PDDocument)}).
     *
     * @param document the document as {@link DocumentReader} returns it; not {@code null}
     * @param target the open PDFBox document, without pages, that receives the PDF; not {@code null}
     * @throws IOException if PDFBox fails while writing into {@code target}
     * @throws IllegalArgumentException if {@code target} already has pages
     */
    public void toPdf(InlineNode document, PDDocument target) throws IOException {
        toPdf(document, target, Attachments.NONE, Cancellation.NONE, Progress.NONE);
    }

    /**
     * Like {@link #toPdf(InlineNode, PDDocument)}, with the files and the XMP of {@code attachments} embedded,
     * stopping once {@code cancellation} asks for it and telling {@code progress} how it goes
     * ({@link #toPdf(LaidOutDocument, PDDocument, Attachments, Cancellation, Progress)}).
     *
     * @param document the document as {@link DocumentReader} returns it; not {@code null}
     * @param target the open PDFBox document, without pages, that receives the PDF; not {@code null}
     * @param attachments the files and the XMP of this document; {@link Attachments#NONE} for none, not {@code null}
     * @param cancellation what stops this conversion, besides the one of the converter; {@link Cancellation#NONE}
     *        for nothing, not {@code null}
     * @param progress what is told how the conversion goes; {@link Progress#NONE} for nothing, not {@code null}
     * @throws IOException if PDFBox fails while writing into {@code target}
     * @throws IllegalArgumentException if {@code target} already has pages
     */
    public void toPdf(InlineNode document, PDDocument target, Attachments attachments, Cancellation cancellation,
                      Progress progress) throws IOException {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(attachments, "attachments");
        toPdf(layOut(document, cancellation, progress), target, attachments, cancellation, progress);
    }

    /**
     * Writes the PDF of a document already laid out by this converter (with the same fonts, which are the ones
     * that get embedded) to {@code target}, which is not closed.
     *
     * @param document a document laid out by {@link #layOut(InlineNode)}; not {@code null}
     * @param target where the PDF is written; not {@code null}
     * @throws IOException if writing to {@code target} fails
     */
    public void toPdf(LaidOutDocument document, OutputStream target) throws IOException {
        toPdf(document, target, Attachments.NONE);
    }

    /**
     * Like {@link #toPdf(LaidOutDocument, OutputStream)}, with the files and the XMP of {@code attachments}
     * embedded ({@link #toPdf(InlineNode, OutputStream, Attachments)}).
     *
     * @param document a document laid out by {@link #layOut(InlineNode)}; not {@code null}
     * @param target where the PDF is written; not {@code null}
     * @param attachments the files and the XMP of this document; {@link Attachments#NONE} for none, not {@code null}
     * @throws IOException if writing to {@code target} fails
     */
    public void toPdf(LaidOutDocument document, OutputStream target, Attachments attachments) throws IOException {
        toPdf(document, target, attachments, Cancellation.NONE);
    }

    /**
     * Like {@link #toPdf(LaidOutDocument, OutputStream, Attachments)}, stopping with a
     * {@link dev.stxt.pdf.model.CancelledException} once {@code cancellation}, or the one of the converter
     * ({@link Builder#cancellation}), asks for it: at each page and at each image, before anything is written.
     *
     * @param document a document laid out by {@link #layOut(InlineNode)}; not {@code null}
     * @param target where the PDF is written; not {@code null}
     * @param attachments the files and the XMP of this document; {@link Attachments#NONE} for none, not {@code null}
     * @param cancellation what stops this conversion, besides the one of the converter; {@link Cancellation#NONE}
     *        for nothing, not {@code null}
     * @throws IOException if writing to {@code target} fails
     */
    public void toPdf(LaidOutDocument document, OutputStream target, Attachments attachments,
                      Cancellation cancellation) throws IOException {
        toPdf(document, target, attachments, cancellation, Progress.NONE);
    }

    /**
     * Like {@link #toPdf(LaidOutDocument, OutputStream, Attachments, Cancellation)}, telling {@code progress} each
     * page painted and, once the PDF is written, that it is done (reference/implementation/api.md).
     *
     * @param document a document laid out by {@link #layOut(InlineNode)}; not {@code null}
     * @param target where the PDF is written; not {@code null}
     * @param attachments the files and the XMP of this document; {@link Attachments#NONE} for none, not {@code null}
     * @param cancellation what stops this conversion, besides the one of the converter; {@link Cancellation#NONE}
     *        for nothing, not {@code null}
     * @param progress what is told how the painting goes; {@link Progress#NONE} for nothing, not {@code null}
     * @throws IOException if writing to {@code target} fails
     */
    public void toPdf(LaidOutDocument document, OutputStream target, Attachments attachments,
                      Cancellation cancellation, Progress progress) throws IOException {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(attachments, "attachments");
        Objects.requireNonNull(progress, "progress");
        Cancellation stop = this.cancellation.or(Objects.requireNonNull(cancellation, "cancellation"));
        guarded(() -> PdfDocumentWriter.write(document, resources(document), limits, target, debug, pdfUa, pdfA,
                attachments, stop, progress));
    }

    /**
     * Writes the PDF of a document already laid out by this converter into {@code target}, an open PDFBox document
     * without pages, instead of into a file (reference/implementation/api.md): the same pages, fonts, structure
     * tree, bookmarks, links, page labels, metadata and PDF/A declaration as
     * {@link #toPdf(LaidOutDocument, OutputStream)}, but the document is not saved. The caller adds what the engine
     * does not write (keys of its own in the information dictionary, a stamp on every page, encryption) and saves it;
     * the file identifier is then the one PDFBox gives it, not the one of the content, so two saves do not give the
     * same bytes. What {@code target} had before is kept, except what the document writes (its title, subject,
     * language and XMP packet). A single page is painted on a page of any document with
     * {@link dev.stxt.pdf.paint.PdfPainter}.
     *
     * @param document a document laid out by {@link #layOut(InlineNode)}; not {@code null}
     * @param target the open PDFBox document, without pages, that receives the PDF; not {@code null}
     * @throws IOException if PDFBox fails while writing into {@code target}
     * @throws IllegalArgumentException if {@code target} already has pages
     */
    public void toPdf(LaidOutDocument document, PDDocument target) throws IOException {
        toPdf(document, target, Attachments.NONE, Cancellation.NONE, Progress.NONE);
    }

    /**
     * Like {@link #toPdf(LaidOutDocument, PDDocument)}, with the files and the XMP of {@code attachments} embedded
     * ({@link #toPdf(InlineNode, OutputStream, Attachments)}), stopping with a
     * {@link dev.stxt.pdf.model.CancelledException} once {@code cancellation}, or the one of the converter, asks for
     * it (the document is then half written) and telling {@code progress} each page painted and, at the end, that it
     * is done.
     *
     * @param document a document laid out by {@link #layOut(InlineNode)}; not {@code null}
     * @param target the open PDFBox document, without pages, that receives the PDF; not {@code null}
     * @param attachments the files and the XMP of this document; {@link Attachments#NONE} for none, not {@code null}
     * @param cancellation what stops this conversion, besides the one of the converter; {@link Cancellation#NONE}
     *        for nothing, not {@code null}
     * @param progress what is told how the painting goes; {@link Progress#NONE} for nothing, not {@code null}
     * @throws IOException if PDFBox fails while writing into {@code target}
     * @throws IllegalArgumentException if {@code target} already has pages
     */
    public void toPdf(LaidOutDocument document, PDDocument target, Attachments attachments, Cancellation cancellation,
                      Progress progress) throws IOException {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(attachments, "attachments");
        Objects.requireNonNull(progress, "progress");
        if (target.getNumberOfPages() > 0) {
            throw new IllegalArgumentException("The document is written into a PDDocument without pages, and this "
                    + "one has " + target.getNumberOfPages() + "; one page at a time goes with PdfPainter");
        }
        Cancellation stop = this.cancellation.or(Objects.requireNonNull(cancellation, "cancellation"));
        guarded(() -> PdfDocumentWriter.write(document, resources(document), limits, target, debug, pdfUa, pdfA,
                attachments, stop, progress));
    }

    /**
     * {@return the SVG of each page of an already laid out document, in order}
     *
     * @param document a document laid out by {@link #layOut(InlineNode)}; not {@code null}
     */
    public List<String> toSvg(LaidOutDocument document) {
        return toSvg(document, Cancellation.NONE);
    }

    /**
     * Like {@link #toSvg(LaidOutDocument)}, stopping once {@code cancellation}, or the one of the converter, asks for
     * it, which is asked at each page.
     *
     * @param document a document laid out by {@link #layOut(InlineNode)}; not {@code null}
     * @param cancellation what stops this conversion, besides the one of the converter; {@link Cancellation#NONE}
     *        for nothing, not {@code null}
     * @return the SVG of each page, in order
     */
    public List<String> toSvg(LaidOutDocument document, Cancellation cancellation) {
        return toSvg(document, cancellation, Progress.NONE);
    }

    /**
     * Like {@link #toSvg(LaidOutDocument, Cancellation)}, telling {@code progress} each page made and, at the end,
     * that it is done.
     *
     * @param document a document laid out by {@link #layOut(InlineNode)}; not {@code null}
     * @param cancellation what stops this conversion, besides the one of the converter; {@link Cancellation#NONE}
     *        for nothing, not {@code null}
     * @param progress what is told how the painting goes; {@link Progress#NONE} for nothing, not {@code null}
     * @return the SVG of each page, in order
     */
    public List<String> toSvg(LaidOutDocument document, Cancellation cancellation, Progress progress) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(progress, "progress");
        Cancellation stop = this.cancellation.or(Objects.requireNonNull(cancellation, "cancellation"));
        return guarded(() -> SvgCanvas.pages(document, debug, resources(document), stop, progress));
    }

    /**
     * The resources a document is painted with: those it was laid out with ({@link LaidOutDocument#resources()}), so
     * that it is painted with the fonts and the images it was measured with, or, if they are not known, those a
     * conversion would work with.
     */
    private Resources resources(LaidOutDocument document) {
        return document.resources() != null ? document.resources() : forConversion();
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
    private static String readVersion() {
        try (InputStream in = Converter.class.getResourceAsStream(VERSION_RESOURCE)) {
            if (in == null) {
                throw new InternalException("Resource " + VERSION_RESOURCE + " not found in the classpath");
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties.getProperty("version");
        } catch (IOException e) {
            throw new InternalException("Resource " + VERSION_RESOURCE + " cannot be read", e);
        }
    }

    public static final class Builder {

        /** The converter this builder comes from ({@link Converter#toBuilder()}); {@code null} if from scratch. */
        private Converter base;
        private Resources resources = new Resources();
        private boolean reloadEachConversion;
        private boolean preload;
        /** The images of the conversions of {@link #base} ({@link Converter#withImages}); none from scratch. */
        private Map<String, byte[]> images = Map.of();
        private final List<InlineNode> styles = new ArrayList<>();
        private boolean debug;
        private Limits limits = Limits.DEFAULT;
        private final Set<Check> excluded = EnumSet.noneOf(Check.class);
        private int pdfUa = 1;
        private PdfA pdfA;
        private Cancellation cancellation = Cancellation.NONE;

        private Builder() {
        }

        /**
         * Whether what it builds can share the snapshot of {@link #base}: it comes from a converter and the options
         * of the resources (the folder, reloading each conversion, preloading) are those of that converter.
         */
        private boolean keepsTheSnapshot() {
            return base != null && base.resources == resources && base.reloadEachConversion == reloadEachConversion
                    && base.preload == preload;
        }

        /**
         * Resources folder (reference/common/resources.md); by default {@link Resources#DEFAULT_ROOT}.
         *
         * @param resources the folder with the fonts and the images; not {@code null}
         * @return this builder
         */
        public Builder resources(Resources resources) {
            this.resources = Objects.requireNonNull(resources, "resources");
            return this;
        }

        /**
         * Whether each conversion sees the resources folder as it is when it starts (reference/implementation/api.md).
         * Off by default: the converter keeps the fonts and the images it reads, whatever happens to their files, and
         * sees the changes only in the converter {@link Converter#reloaded()} gives; until then, a PNG, a JPEG or a PDF
         * that was replaced, which is read again when painting, makes its conversions fail with
         * {@code IMAGE_FORMAT}. On, each conversion looks at
         * {@code fonts/} and at each image it uses, and reads again those that changed: the changes are seen without
         * calling anything, at the cost of a look at the disk per conversion, and a conversion that starts while the
         * files are being copied can find some of them and not others, or one half written.
         *
         * @param reload {@code true} to look at the folder in each conversion
         * @return this builder
         */
        public Builder reloadResourcesEachConversion(boolean reload) {
            this.reloadEachConversion = reload;
            return this;
        }

        /**
         * Whether the converter reads the resources folder when it is created and in {@link Converter#reloaded()},
         * instead of reading each font and each image the first time a conversion uses it
         * (reference/implementation/api.md). Off by default. On, it indexes {@code fonts/} and reads every image of
         * {@code images/} that can be read, so it keeps the folder as it was then, and an invalid font makes
         * {@link #build} or {@code reloaded()} fail instead of the first conversion. An image that cannot be read, or a
         * PDF beyond {@link Limits#maxPdfBytes()}, is left for when a document uses it. Of a PNG, a JPEG or a PDF it
         * keeps the format and the size: its content is still read when painting. It costs reading the whole folder,
         * also the images that no document uses.
         *
         * @param preload {@code true} to read the folder when the converter is created and reloaded
         * @return this builder
         */
        public Builder preloadResources(boolean preload) {
            this.preload = preload;
            return this;
        }

        /**
         * Adds a separate styles document, already read ({@link DocumentReader#readStyles(String)}), whose rules weigh
         * less than those of the document (reference/common/styles.md). Each call adds one, and they are applied in
         * the order of the calls: the rules of each one weigh more than those of the ones before it. Without any, the
         * document carries all its styles.
         *
         * @param styles the styles document as {@link DocumentReader#readStyles(String)} returns it; not {@code null}
         * @return this builder
         */
        public Builder styles(InlineNode styles) {
            this.styles.add(Objects.requireNonNull(styles, "styles"));
            return this;
        }

        /**
         * Like {@link #styles(InlineNode)}, reading it from an STXT text. An error of reading it says which styles
         * document it is ({@code StxtPdfException.stylesDocument()}): the place this call gives it.
         *
         * @param content the STXT text of the styles document
         * @return this builder
         */
        public Builder styles(String content) {
            return styles(new DocumentReader().readStyles(content, styles.size() + 1));
        }

        /**
         * Like {@link #styles(InlineNode)}, reading it from a UTF-8 file. An error of reading it says which styles
         * document it is ({@code StxtPdfException.stylesDocument()}): the place this call gives it.
         *
         * @param file the STXT file of the styles document, in UTF-8
         * @return this builder
         */
        public Builder styles(Path file) {
            return styles(new DocumentReader().readStyles(file, styles.size() + 1));
        }

        /**
         * The most the conversion accepts (pages, pixels of an image, elements and levels of an SVG, bytes of
         * a PDF); by default {@link Limits#DEFAULT}. A document that exceeds it fails with a {@code LIMIT} error.
         *
         * @param limits {@link Limits#DEFAULT} with the values to change ({@link Limits#withMaxPages}...); not
         *        {@code null}
         * @return this builder
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
         *
         * @param checks the checks not to run; none of them {@code null}
         * @return this builder
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
         * @param part 1 or 2
         * @return this builder
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
         *
         * @param pdfA the part and the level, as {@link PdfA#A3B}; not {@code null}
         * @return this builder
         */
        public Builder pdfA(PdfA pdfA) {
            this.pdfA = Objects.requireNonNull(pdfA, "pdfA");
            return this;
        }

        /**
         * Stops every conversion of this converter, with a {@link dev.stxt.pdf.model.CancelledException}, once
         * {@code cancellation} asks for it (reference/implementation/api.md); by default none
         * ({@link Cancellation#NONE}). All the conversions share it, so it is the place for one that depends on the
         * thread that converts ({@link Cancellation#whenInterrupted()}); the deadline of one document goes with its
         * call ({@link Converter#toPdf(InlineNode, OutputStream, Attachments, Cancellation)}), and either stops it.
         *
         * @param cancellation what stops every conversion; not {@code null}
         * @return this builder
         */
        public Builder cancellation(Cancellation cancellation) {
            this.cancellation = Objects.requireNonNull(cancellation, "cancellation");
            return this;
        }

        /**
         * Draws the outline of each box, to review a document by hand; off by default.
         *
         * @param debug {@code true} to draw the outlines
         * @return this builder
         */
        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        /**
         * {@return the converter with the options given, immutable and independent of this builder}
         *
         * @throws IllegalArgumentException with {@link #pdfUa pdfUa(2)} and a {@link #pdfA}: PDF/UA-2 is a PDF 2.0
         *         file and PDF/A-2 and PDF/A-3 are PDF 1.7 ones; with {@link #preloadResources} and
         *         {@link #reloadResourcesEachConversion}, which would read the whole folder in each conversion
         * @throws dev.stxt.pdf.resources.ResourceException with {@link #preloadResources}, if the fonts cannot be
         *         indexed
         */
        public Converter build() {
            if (pdfUa == 2 && pdfA != null) {
                throw new IllegalArgumentException("PDF/UA-2 is a PDF 2.0 file and " + pdfA
                        + " a PDF 1.7 one: a PDF cannot comply with both");
            }
            if (preload && reloadEachConversion) {
                throw new IllegalArgumentException("Preloading the resources and reloading them in each conversion "
                        + "would read the whole folder in each conversion: only one of them can be chosen");
            }
            return new Converter(this);
        }
    }
}
