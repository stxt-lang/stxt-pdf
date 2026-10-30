package dev.stxt.pdf.resources;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDAbstractPattern;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDTilingPattern;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lee un PDF de {@code images/} (referencia/componentes/image.md): el tamaño de cada página, en puntos y ya girado,
 * y lo que impide dibujarla igual dentro del documento. Una página se dibuja con su contenido, no con sus anotaciones,
 * así que una anotación que se ve (un campo de formulario, un comentario, un sello) es un problema; un enlace, no,
 * porque no se ve. Una fuente sin incrustar tampoco se admite: el visor la sustituiría por otra y el PDF no sería accesible.
 * Un PDF cifrado, sin páginas o que no se puede leer es un error del fichero.
 */
public final class PdfInspector {

    /**
     * Una página del PDF.
     *
     * @param width   ancho en puntos, el del recorte de la página ({@code CropBox}) ya girado
     * @param height  alto en puntos, igual
     * @param problem por qué no se puede dibujar, para el mensaje; {@code null} si se puede
     */
    record PdfPage(double width, double height, String problem) {
    }

    private PdfInspector() {
    }

    /**
     * Las páginas del PDF; {@code name} es su ruta en el documento, para los mensajes.
     *
     * @throws ImageFormatException si está cifrado, no tiene páginas o no se puede leer
     */
    static List<PdfPage> inspect(Path path, String name) {
        try (PDDocument doc = Loader.loadPDF(path.toFile())) {
            if (doc.isEncrypted()) {
                throw new ImageFormatException("El PDF " + name + " está cifrado: guárdalo sin protección");
            }
            if (doc.getNumberOfPages() == 0) {
                throw new ImageFormatException("El PDF " + name + " no tiene páginas");
            }
            List<PdfPage> pages = new ArrayList<>();
            for (PDPage page : doc.getPages()) {
                PDRectangle crop = page.getCropBox();
                boolean turned = rotation(page) % 180 != 0;
                double width = turned ? crop.getHeight() : crop.getWidth();
                double height = turned ? crop.getWidth() : crop.getHeight();
                pages.add(new PdfPage(width, height, problem(page)));
            }
            return Collections.unmodifiableList(pages);
        } catch (InvalidPasswordException e) {
            throw new ImageFormatException("El PDF " + name + " está cifrado: guárdalo sin protección", e);
        } catch (IOException | RuntimeException e) {
            if (e instanceof ImageFormatException f) {
                throw f;
            }
            throw new ImageFormatException("No se puede leer la imagen " + name + ", que parece PDF pero está dañada: "
                    + e.getMessage(), e);
        }
    }

    /**
     * El giro de la página ({@code Rotate}) en grados, de 0 a 270, en el sentido de las agujas del reloj; uno que no es
     * múltiplo de 90 no gira. Es el que da el tamaño de la página y el que aplica quien la dibuja.
     */
    public static int rotation(PDPage page) {
        int degrees = Math.floorMod(page.getRotation(), 360);
        return degrees % 90 == 0 ? degrees : 0;
    }

    /** Lo primero que impide dibujar la página igual que en su PDF, o {@code null}. */
    private static String problem(PDPage page) throws IOException {
        for (PDAnnotation annotation : page.getAnnotations()) {
            if (!(annotation instanceof PDAnnotationLink) && !annotation.isHidden() && !annotation.isNoView()
                    && annotation.getNormalAppearanceStream() != null) {
                return "tiene una anotación que se ve (" + annotation.getSubtype() + ", como un campo de formulario o "
                        + "un comentario), que no se dibujaría: aplánala en el PDF antes de usarlo";
            }
        }
        String font = unembeddedFont(page.getResources(), new HashSet<>());
        if (font != null) {
            return "usa la fuente " + font + " sin incrustar, que el visor sustituiría por otra: crea el PDF con las "
                    + "fuentes incrustadas";
        }
        return null;
    }

    /**
     * El nombre de la primera fuente sin incrustar de unos recursos, de los formularios y de los patrones que usan, o
     * {@code null}. {@code seen} evita recorrer dos veces unos recursos compartidos (o un formulario que se usa a sí
     * mismo).
     */
    private static String unembeddedFont(PDResources resources, Set<COSBase> seen) throws IOException {
        if (resources == null || !seen.add(resources.getCOSObject())) {
            return null;
        }
        for (COSName name : resources.getFontNames()) {
            PDFont font = resources.getFont(name);
            if (font != null && !font.isEmbedded()) {
                return font.getName();
            }
        }
        for (COSName name : resources.getXObjectNames()) {
            PDXObject object = resources.getXObject(name);
            if (object instanceof PDFormXObject form) {
                String font = unembeddedFont(form.getResources(), seen);
                if (font != null) {
                    return font;
                }
            }
        }
        for (COSName name : resources.getPatternNames()) {
            PDAbstractPattern pattern = resources.getPattern(name);
            if (pattern instanceof PDTilingPattern tiling) {
                String font = unembeddedFont(tiling.getResources(), seen);
                if (font != null) {
                    return font;
                }
            }
        }
        return null;
    }
}
