package dev.stxt.pdf.paint;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.DocumentLimits;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.parser.resources.ResourcePolicy;
import com.github.weisj.jsvg.renderer.NullPlatformSupport;
import com.github.weisj.jsvg.view.ViewBox;
import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import dev.stxt.pdf.model.Cancellation;
import dev.stxt.pdf.model.Limits;
import dev.stxt.pdf.resources.ImageFile;
import dev.stxt.pdf.resources.ImageFormatException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * The images a PDF carries (reference/components/image.md), each file only once even if it is used
 * on several pages.
 *
 * <p>A PNG or a JPEG is embedded as is ({@link PDImageXObject}: the JPEG without recompressing and the
 * PNG lossless). An SVG is drawn with JSVG on a {@code Graphics2D} that pdfbox-graphics2d converts
 * into PDF paths, fills and gradients: it is a {@link PDFormXObject} of the natural size of the
 * SVG, vector, placed scaled where the image goes. JSVG draws text as outlines
 * of the {@code fonts/} font checked when reading the SVG ({@link SvgFonts}, without JVM state): it
 * looks right at any size, but cannot be selected. External references are
 * forbidden ({@link ResourcePolicy#DENY_ALL}), although the SVG already rejected them when it was read.</p>
 *
 * <p>The page of a PDF is also a {@link PDFormXObject}: its content and its resources, copied with
 * {@link LayerUtility}, without its annotations or its tagging ({@link PdfPages}). The source PDFs are opened once
 * and closed with {@link #close()}.</p>
 */
final class PdfImages implements Closeable {

    /**
     * JSVG keeps the text fonts in a JVM-wide cache that is not synchronized ({@code FontResolver}),
     * and also the glyphs of each font: SVGs with text are drawn one at a time.
     */
    private static final Object JSVG_FONTS = new Object();

    private final PDDocument doc;
    private final Limits limits;
    /** Asked before each image is embedded or drawn for the first time, which is where the work is. */
    private final Cancellation cancellation;
    private final Map<Path, PDImageXObject> bitmaps = new HashMap<>();
    private final Map<Path, PDFormXObject> drawings = new HashMap<>();
    private final Map<Path, PDDocument> sources = new HashMap<>();
    private final Map<Path, Map<Integer, PDFormXObject>> pages = new HashMap<>();
    private final PdfPages imports;

    PdfImages(PDDocument doc, Limits limits) {
        this(doc, limits, Cancellation.NONE);
    }

    PdfImages(PDDocument doc, Limits limits, Cancellation cancellation) {
        this.doc = doc;
        this.limits = limits;
        this.cancellation = cancellation;
        this.imports = new PdfPages(doc);
    }

    /**
     * The PNG or the JPEG, embedded the first time, from the file as it is now.
     *
     * @throws dev.stxt.pdf.resources.ResourceException if it changed after it was read or cannot be read
     *         ({@link ImageFile#checkUnchanged()})
     */
    PDImageXObject bitmap(ImageFile file) {
        PDImageXObject image = bitmaps.get(file.path());
        if (image == null) {
            cancellation.check();
            file.checkUnchanged();
            try {
                image = PDImageXObject.createFromFileByContent(file.path().toFile(), doc);
            } catch (IOException e) {
                throw file.unreadable(e);
            }
            bitmaps.put(file.path(), image);
        }
        return image;
    }

    /**
     * The SVG drawn on a form of {@code file.width()} × {@code file.height()} points, with
     * the origin at its bottom left corner (like the content of a page).
     */
    PDFormXObject drawing(ImageFile file) throws IOException {
        PDFormXObject form = drawings.get(file.path());
        if (form == null) {
            cancellation.check();
            form = draw(file);
            drawings.put(file.path(), form);
        }
        return form;
    }

    /**
     * Page {@code number} (from 1) of the PDF on a form of {@code file.width(number)} ×
     * {@code file.height(number)} points, with the origin at its bottom left corner, like {@link #drawing}.
     * Each page is copied only once, from the file as it is when the first one is.
     *
     * @throws dev.stxt.pdf.resources.ResourceException if it changed after it was read or cannot be read
     *         ({@link ImageFile#checkUnchanged()})
     */
    PDFormXObject page(ImageFile file, int number) {
        Map<Integer, PDFormXObject> forms = pages.computeIfAbsent(file.path(), p -> new HashMap<>());
        PDFormXObject form = forms.get(number);
        if (form == null) {
            cancellation.check();
            try {
                PDDocument source = sources.get(file.path());
                if (source == null) {
                    file.checkUnchanged();
                    source = Loader.loadPDF(file.path().toFile());
                    sources.put(file.path(), source);
                }
                // PDFBox reads the source as it needs it: what it copies is also read from the file.
                form = imports.page(source, number, file.path().getFileName().toString());
            } catch (IOException e) {
                throw file.unreadable(e);
            }
            forms.put(number, form);
        }
        return form;
    }

    /** Closes the source PDFs; what was copied is already in the document. */
    @Override
    public void close() throws IOException {
        IOException failure = null;
        for (PDDocument source : sources.values()) {
            try {
                source.close();
            } catch (IOException e) {
                failure = e;
            }
        }
        sources.clear();
        if (failure != null) {
            throw failure;
        }
    }

    private PDFormXObject draw(ImageFile file) throws IOException {
        // The limits of JSVG are those of Limits (by default it only accepts 2000 elements and 30 levels); the engine
        // already checked its own before layout (Images), so they should not trip here.
        LoaderContext.Builder context = LoaderContext.builder().externalResourcePolicy(ResourcePolicy.DENY_ALL)
                .documentLimits(new DocumentLimits(limits.maxSvgDepth(), DocumentLimits.DEFAULT_MAX_USE_NESTING_DEPTH,
                        limits.maxSvgElements()));
        // A <text> with nothing visible is enough for JSVG to resolve a font.
        SvgFonts fonts = file.textFonts().isEmpty() ? null : new SvgFonts(file);
        if (fonts != null) {
            context.preProcessor(fonts::rewrite);
        }
        SVGDocument svg = new SVGLoader().load(new ByteArrayInputStream(file.svgContent()), URI.create("about:blank"),
                context.build());
        if (svg == null) {
            throw new ImageFormatException("The SVG " + file.path().getFileName()
                    + " cannot be drawn: JSVG does not load it (for example, it nests more <use> than its limit of "
                    + DocumentLimits.DEFAULT_MAX_USE_NESTING_DEPTH + " levels)");
        }
        PdfBoxGraphics2D graphics = new PdfBoxGraphics2D(doc, (float) file.width(), (float) file.height());
        ViewBox box = new ViewBox(0, 0, (float) file.width(), (float) file.height());
        try {
            if (fonts == null) {
                svg.renderWithPlatform(NullPlatformSupport.INSTANCE, graphics, box);
            } else {
                synchronized (JSVG_FONTS) {
                    svg.renderWithPlatform(fonts, graphics, box);
                }
                fonts.check();
            }
        } finally {
            graphics.dispose();
        }
        return graphics.getXFormObject();
    }

}
