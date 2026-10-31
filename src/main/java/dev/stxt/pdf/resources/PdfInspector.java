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
import org.apache.pdfbox.pdmodel.font.PDSimpleFont;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
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
 * Reads a PDF of {@code images/} (reference/components/image.md): the size of each page, in points and already
 * rotated, and whatever prevents it from being drawn faithfully inside the document, in two groups that the caller may
 * choose not to check ({@link dev.stxt.pdf.model.Check}). A page is drawn with its content, not with its annotations,
 * so a visible annotation (a form field, a comment, a stamp) is a problem; a link is not, because it is not visible.
 * So is a font that is not embedded, or that does not say which character each glyph is: the viewer would replace it
 * with another, or its text could not be copied or read aloud, and the document would not be accessible. An encrypted
 * PDF, one without pages or one that cannot be read is an error of the file.
 */
public final class PdfInspector {

    /**
     * A page of the PDF.
     *
     * @param width       width in points, that of the crop box of the page ({@code CropBox}), already rotated
     * @param height      height in points, likewise
     * @param annotations why its annotations prevent drawing it faithfully, for the message; {@code null} otherwise
     * @param fonts       why its fonts prevent drawing it faithfully, for the message; {@code null} otherwise
     */
    record PdfPage(double width, double height, String annotations, String fonts) {
    }

    private PdfInspector() {
    }

    /**
     * The pages of the PDF; {@code name} is its path in the document, for the messages.
     *
     * @throws ImageFormatException if it is encrypted, has no pages or cannot be read
     */
    static List<PdfPage> inspect(Path path, String name) {
        try (PDDocument doc = Loader.loadPDF(path.toFile())) {
            if (doc.isEncrypted()) {
                throw new ImageFormatException("The PDF " + name + " is encrypted: it has to be saved without protection");
            }
            if (doc.getNumberOfPages() == 0) {
                throw new ImageFormatException("The PDF " + name + " has no pages");
            }
            List<PdfPage> pages = new ArrayList<>();
            for (PDPage page : doc.getPages()) {
                PDRectangle crop = page.getCropBox();
                boolean turned = rotation(page) % 180 != 0;
                double width = turned ? crop.getHeight() : crop.getWidth();
                double height = turned ? crop.getWidth() : crop.getHeight();
                pages.add(new PdfPage(width, height, annotations(page), fonts(page.getResources(), new HashSet<>())));
            }
            return Collections.unmodifiableList(pages);
        } catch (InvalidPasswordException e) {
            throw new ImageFormatException("The PDF " + name + " is encrypted: it has to be saved without protection", e);
        } catch (IOException | RuntimeException e) {
            if (e instanceof ImageFormatException f) {
                throw f;
            }
            throw new ImageFormatException("Cannot read the image " + name + ", which looks like a PDF but is damaged: "
                    + e.getMessage(), e);
        }
    }

    /**
     * The rotation of the page ({@code Rotate}) in degrees, from 0 to 270, clockwise; one that is not a multiple of 90
     * does not rotate. It is the one that gives the size of the page and the one applied by whoever draws it.
     */
    public static int rotation(PDPage page) {
        int degrees = Math.floorMod(page.getRotation(), 360);
        return degrees % 90 == 0 ? degrees : 0;
    }

    /** The first visible annotation that would not be drawn, or {@code null}. */
    private static String annotations(PDPage page) throws IOException {
        for (PDAnnotation annotation : page.getAnnotations()) {
            if (!(annotation instanceof PDAnnotationLink) && !annotation.isHidden() && !annotation.isNoView()
                    && annotation.getNormalAppearanceStream() != null) {
                return "has a visible annotation (" + annotation.getSubtype() + ", such as a form field or "
                        + "a comment), which would not be drawn: the PDF has to be flattened before it is used";
            }
        }
        return null;
    }

    /**
     * The first font of some resources, of the forms and of the patterns they use, that is not embedded or does not
     * say which character each glyph is, or {@code null}. {@code seen} avoids walking shared resources twice (or a form
     * that uses itself).
     */
    private static String fonts(PDResources resources, Set<COSBase> seen) throws IOException {
        if (resources == null || !seen.add(resources.getCOSObject())) {
            return null;
        }
        for (COSName name : resources.getFontNames()) {
            PDFont font = resources.getFont(name);
            if (font == null) {
                continue;
            }
            if (!font.isEmbedded()) {
                return "uses the font " + font.getName() + " not embedded, which the viewer would replace "
                        + "with another: the PDF has to be created with the fonts embedded";
            }
            if (!hasUnicode(font)) {
                return "uses the font " + font.getName() + " without ToUnicode, which says which character "
                        + "each glyph is: its text could not be copied or read aloud. The PDF has to be created again, "
                        + "for example as PDF/A";
            }
        }
        for (COSName name : resources.getXObjectNames()) {
            PDXObject object = resources.getXObject(name);
            if (object instanceof PDFormXObject form) {
                String problem = fonts(form.getResources(), seen);
                if (problem != null) {
                    return problem;
                }
            }
        }
        for (COSName name : resources.getPatternNames()) {
            PDAbstractPattern pattern = resources.getPattern(name);
            if (pattern instanceof PDTilingPattern tiling) {
                String problem = fonts(tiling.getResources(), seen);
                if (problem != null) {
                    return problem;
                }
            }
        }
        return null;
    }

    /**
     * Whether the font says which character each glyph is: with a {@code ToUnicode} or, for a simple font that is not
     * symbolic, with the glyph names of its encoding (ISO 32000-1, 9.10.2).
     */
    private static boolean hasUnicode(PDFont font) {
        return font.getCOSObject().containsKey(COSName.TO_UNICODE)
                || font instanceof PDSimpleFont simple && !(font instanceof PDType3Font) && !simple.isSymbolic();
    }
}
