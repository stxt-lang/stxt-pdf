package dev.stxt.boxes.paint;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.DocumentLimits;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.parser.resources.ResourcePolicy;
import com.github.weisj.jsvg.renderer.NullPlatformSupport;
import com.github.weisj.jsvg.view.ViewBox;
import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import dev.stxt.boxes.model.Limits;
import dev.stxt.boxes.resources.ImageFile;
import dev.stxt.boxes.resources.ImageFormatException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Las imágenes que lleva un PDF (referencia/componentes/image.md), cada fichero una sola vez aunque se use
 * en varias páginas.
 *
 * <p>Un PNG o un JPEG se incrusta tal cual ({@link PDImageXObject}: el JPEG sin recomprimir y el
 * PNG sin pérdida). Un SVG se dibuja con JSVG en un {@code Graphics2D} que convierte pdfbox-graphics2d
 * en rutas, rellenos y degradados del PDF: es un {@link PDFormXObject} del tamaño natural del
 * SVG, vectorial, que se coloca escalado donde va la imagen. JSVG dibuja el texto como contornos
 * de la fuente de {@code fonts/} que se comprobó al leer el SVG ({@link SvgFonts}, sin estado de la JVM): se
 * ve bien a cualquier tamaño, pero no se puede seleccionar. Las referencias externas están
 * prohibidas ({@link ResourcePolicy#DENY_ALL}), aunque el SVG ya las ha rechazado al leerlo.</p>
 */
final class PdfImages {

    /**
     * JSVG guarda las fuentes del texto en una caché de toda la JVM que no está sincronizada ({@code FontResolver}),
     * y también los glifos de cada fuente: los SVG con texto se dibujan de uno en uno.
     */
    private static final Object JSVG_FONTS = new Object();

    private final PDDocument doc;
    private final Limits limits;
    private final Map<Path, PDImageXObject> bitmaps = new HashMap<>();
    private final Map<Path, PDFormXObject> drawings = new HashMap<>();

    PdfImages(PDDocument doc, Limits limits) {
        this.doc = doc;
        this.limits = limits;
    }

    /** El PNG o el JPEG, incrustado la primera vez. */
    PDImageXObject bitmap(ImageFile file) throws IOException {
        PDImageXObject image = bitmaps.get(file.path());
        if (image == null) {
            image = PDImageXObject.createFromFileByContent(file.path().toFile(), doc);
            bitmaps.put(file.path(), image);
        }
        return image;
    }

    /**
     * El SVG dibujado en un formulario de {@code file.width()} × {@code file.height()} puntos, con
     * el origen en su esquina de abajo a la izquierda (como el contenido de una página).
     */
    PDFormXObject drawing(ImageFile file) throws IOException {
        PDFormXObject form = drawings.get(file.path());
        if (form == null) {
            form = draw(file);
            drawings.put(file.path(), form);
        }
        return form;
    }

    private PDFormXObject draw(ImageFile file) throws IOException {
        // Los límites de JSVG son los de Limits (por defecto solo admite 2000 elementos y 30 niveles); el motor ya
        // comprobó los suyos antes de maquetar (Images), así que aquí no deberían saltar.
        LoaderContext.Builder context = LoaderContext.builder().externalResourcePolicy(ResourcePolicy.DENY_ALL)
                .documentLimits(new DocumentLimits(limits.maxSvgDepth(), DocumentLimits.DEFAULT_MAX_USE_NESTING_DEPTH,
                        limits.maxSvgElements()));
        // Basta un <text> sin nada visible para que JSVG resuelva una fuente.
        SvgFonts fonts = file.textFonts().isEmpty() ? null : new SvgFonts(file);
        if (fonts != null) {
            context.preProcessor(fonts::rewrite);
        }
        SVGDocument svg = new SVGLoader().load(new ByteArrayInputStream(Files.readAllBytes(file.path())),
                URI.create("about:blank"), context.build());
        if (svg == null) {
            throw new ImageFormatException("No se puede dibujar el SVG " + file.path().getFileName()
                    + ": JSVG no lo carga (por ejemplo, anida más <use> que su límite de "
                    + DocumentLimits.DEFAULT_MAX_USE_NESTING_DEPTH + " niveles)");
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
