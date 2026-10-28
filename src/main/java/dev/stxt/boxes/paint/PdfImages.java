package dev.stxt.boxes.paint;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.parser.resources.ResourcePolicy;
import com.github.weisj.jsvg.view.ViewBox;
import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import dev.stxt.boxes.resources.FontFile;
import dev.stxt.boxes.resources.ImageFile;
import dev.stxt.boxes.resources.Resources;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Las imágenes que lleva un PDF (ESPECIFICACION.md §5.18), cada fichero una sola vez aunque se use
 * en varias páginas.
 *
 * <p>Un PNG o un JPEG se incrusta tal cual ({@link PDImageXObject}: el JPEG sin recomprimir y el
 * PNG sin pérdida). Un SVG se dibuja con JSVG en un {@code Graphics2D} que convierte pdfbox-graphics2d
 * en rutas, rellenos y degradados del PDF: es un {@link PDFormXObject} del tamaño natural del
 * SVG, vectorial, que se coloca escalado donde va la imagen. JSVG dibuja el texto como contornos
 * de la fuente de {@code fonts/} (que antes se da de alta en AWT, ver {@link #registerFonts}): se
 * ve bien a cualquier tamaño, pero no se puede seleccionar. Las referencias externas están
 * prohibidas ({@link ResourcePolicy#DENY_ALL}), aunque el SVG ya las ha rechazado al leerlo.</p>
 */
final class PdfImages {

    /** Las fuentes de AWT ya dadas de alta en esta JVM, por fichero. */
    private static final Set<Path> REGISTERED = new HashSet<>();

    private final PDDocument doc;
    private final Resources resources;
    private final Map<Path, PDImageXObject> bitmaps = new HashMap<>();
    private final Map<Path, PDFormXObject> drawings = new HashMap<>();

    PdfImages(PDDocument doc, Resources resources) {
        this.doc = doc;
        this.resources = resources;
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
        if (file.hasText()) {
            registerFonts();
        }
        LoaderContext context = LoaderContext.builder().externalResourcePolicy(ResourcePolicy.DENY_ALL).build();
        SVGDocument svg = new SVGLoader().load(new ByteArrayInputStream(Files.readAllBytes(file.path())),
                URI.create("about:blank"), context);
        if (svg == null) {
            throw new IOException("No se puede dibujar el SVG " + file.path().getFileName());
        }
        PdfBoxGraphics2D graphics = new PdfBoxGraphics2D(doc, (float) file.width(), (float) file.height());
        try {
            svg.render(null, graphics, new ViewBox(0, 0, (float) file.width(), (float) file.height()));
        } finally {
            graphics.dispose();
        }
        return graphics.getXFormObject();
    }

    /**
     * Da de alta en AWT las fuentes de {@code fonts/}, para que JSVG encuentre por su familia la
     * que nombra el SVG y no sustituya ninguna. Es un efecto en toda la JVM, pero solo añade esas
     * fuentes y solo se hace una vez por fichero. Como es global a la JVM, dos {@code Resources} con carpetas
     * distintas pero la misma familia comparten el registro (ROADMAP-QA-ARQ.md, TS-03); y AWT necesita
     * {@code -Djava.awt.headless=true} en un servidor sin pantalla.
     */
    private void registerFonts() throws IOException {
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        synchronized (REGISTERED) {
            for (FontFile font : resources.fonts().all()) {
                if (REGISTERED.add(font.path().toAbsolutePath())) {
                    try {
                        environment.registerFont(Font.createFont(Font.TRUETYPE_FONT, font.path().toFile()));
                    } catch (FontFormatException e) {
                        throw new IOException("No se puede dar de alta la fuente " + font.face() + ": "
                                + e.getMessage(), e);
                    }
                }
            }
        }
    }
}
