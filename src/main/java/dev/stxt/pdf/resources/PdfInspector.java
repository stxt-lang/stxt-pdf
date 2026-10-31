package dev.stxt.pdf.resources;

import org.apache.pdfbox.Loader;
import org.apache.fontbox.ttf.CmapSubtable;
import org.apache.fontbox.ttf.CmapTable;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.font.PDCIDFont;
import org.apache.pdfbox.pdmodel.font.PDCIDFontType2;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.pdmodel.font.PDSimpleFont;
import org.apache.pdfbox.pdmodel.font.PDTrueTypeFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.pdmodel.font.encoding.GlyphList;
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
 * with another, or its text could not be copied or read aloud, and the document would not be accessible. Neither is a
 * font that breaks the other rules of PDF/UA that a validator checks most (ISO 14289-1, 7.21): a Type 2 CIDFont without
 * {@code CIDToGIDMap}, a TrueType font with the wrong encoding, and the glyphs that {@link PdfGlyphs} looks at. An
 * encrypted PDF, one without pages or one that cannot be read is an error of the file.
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
                pages.add(new PdfPage(width, height, annotations(page), fonts(page)));
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

    /** Why the fonts of the page prevent drawing it faithfully, or {@code null}: first the fonts, then their glyphs. */
    private static String fonts(PDPage page) throws IOException {
        String problem = fonts(page.getResources(), new HashSet<>());
        return problem != null ? problem : PdfGlyphs.problem(page);
    }

    /**
     * The first font of some resources, of the forms, the patterns and the Type 3 fonts they use, that is not embedded,
     * does not say which character each glyph is or breaks a rule of its dictionary, or {@code null}. {@code seen}
     * avoids walking shared resources twice (or a form that uses itself).
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
            String problem = dictionary(font);
            if (problem != null) {
                return problem;
            }
            if (font instanceof PDType3Font type3) {
                problem = fonts(type3.getResources(), seen);
                if (problem != null) {
                    return problem;
                }
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
     * Why the dictionary of an embedded font breaks PDF/UA (ISO 14289-1, 7.21.3.2 and 7.21.6), or {@code null}: a
     * Type 2 CIDFont needs {@code CIDToGIDMap}; a symbolic TrueType font has no {@code Encoding} and its {@code cmap}
     * table has one encoding or the Microsoft Symbol one (3, 0); a TrueType font that is not symbolic has the encoding
     * {@code WinAnsiEncoding} or {@code MacRomanEncoding}, with glyph names of the Adobe Glyph List if it changes it.
     */
    private static String dictionary(PDFont font) throws IOException {
        if (font instanceof PDType0Font type0) {
            PDCIDFont cid = type0.getDescendantFont();
            if (cid instanceof PDCIDFontType2
                    && cid.getCOSObject().getDictionaryObject(COSName.CID_TO_GID_MAP) == null) {
                return "uses the font " + font.getName() + " without CIDToGIDMap, which says which glyph of the font "
                        + "each character is and PDF/UA requires. The PDF has to be created again, for example as "
                        + "PDF/A";
            }
        } else if (font instanceof PDTrueTypeFont trueType) {
            PDFontDescriptor descriptor = trueType.getFontDescriptor();
            boolean symbolic = descriptor != null && descriptor.isSymbolic();
            if (symbolic && trueType.getCOSObject().containsKey(COSName.ENCODING)) {
                return "uses the symbolic TrueType font " + font.getName() + " with an Encoding, which PDF/UA does not "
                        + "allow. The PDF has to be created again, for example as PDF/A";
            }
            if (symbolic && !symbolCmap(trueType.getTrueTypeFont().getCmap())) {
                return "uses the symbolic TrueType font " + font.getName() + " with several encodings in its cmap "
                        + "table and without the Microsoft Symbol one, which PDF/UA requires. The PDF has to be "
                        + "created again, for example as PDF/A";
            }
            if (!symbolic && !standardEncoding(trueType.getCOSObject().getDictionaryObject(COSName.ENCODING))) {
                return "uses the TrueType font " + font.getName() + " without the encoding WinAnsiEncoding or "
                        + "MacRomanEncoding, or with glyph names outside the Adobe Glyph List, which PDF/UA requires. "
                        + "The PDF has to be created again, for example as PDF/A";
            }
        }
        return null;
    }

    /** Whether the {@code cmap} table has a single encoding or the Microsoft Symbol one. */
    private static boolean symbolCmap(CmapTable cmap) {
        if (cmap == null) {
            return false;
        }
        CmapSubtable[] subtables = cmap.getCmaps();
        return subtables.length == 1
                || cmap.getSubtable(CmapTable.PLATFORM_WINDOWS, CmapTable.ENCODING_WIN_SYMBOL) != null;
    }

    /**
     * Whether the encoding of a TrueType font that is not symbolic is {@code WinAnsiEncoding} or
     * {@code MacRomanEncoding}, by name or as {@code BaseEncoding}, with the names of its {@code Differences} in the
     * Adobe Glyph List.
     */
    private static boolean standardEncoding(COSBase encoding) {
        if (encoding instanceof COSName name) {
            return name.equals(COSName.WIN_ANSI_ENCODING) || name.equals(COSName.MAC_ROMAN_ENCODING);
        }
        if (!(encoding instanceof COSDictionary dictionary)
                || !standardEncoding(dictionary.getDictionaryObject(COSName.BASE_ENCODING))) {
            return false;
        }
        COSArray differences = dictionary.getCOSArray(COSName.DIFFERENCES);
        if (differences != null) {
            for (int i = 0; i < differences.size(); i++) {
                if (differences.getObject(i) instanceof COSName glyph
                        && GlyphList.getAdobeGlyphList().toUnicode(glyph.getName()) == null) {
                    return false;
                }
            }
        }
        return true;
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
