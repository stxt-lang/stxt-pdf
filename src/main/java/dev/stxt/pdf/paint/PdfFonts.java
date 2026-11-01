package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.resources.FontFile;
import dev.stxt.pdf.resources.Resources;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The fonts of a PDF and how text is written with them. Each font of the resources folder
 * (reference/common/resources.md) is embedded once per document and {@link FontFace} combination in a Type 0 font
 * with Identity-H encoding: the PDF looks the same on any machine. A font with TrueType outlines is embedded as a
 * subset (only the glyphs used); one with CFF outlines, whole ({@link #declareCff}). With Identity-H each character
 * takes two bytes and the {@code Tw} operator has no effect, so justification and letter spacing go in the arguments
 * of {@code TJ} ({@link #spaced}), and so does the kerning.
 */
final class PdfFonts {

    private final PDDocument doc;
    private final Resources resources;
    /** Fonts embedded in the document, shared by all its pages. */
    private final Map<FontFace, PDFont> fonts = new HashMap<>();

    PdfFonts(PDDocument doc, Resources resources) {
        this.doc = doc;
        this.resources = resources;
    }

    /**
     * The embedded font; it is loaded the first time it is used in the document. Kerning does not change it: it is
     * written in {@code TJ}, so a font with and without kerning is the same embedded font.
     */
    PDFont font(FontFace requested) throws IOException {
        FontFace font = requested.withKerning(false);
        PDFont f = fonts.get(font);
        if (f == null) {
            // Without ligatures the GSUB table is dropped: PDFBox writes one glyph per character, which is what was
            // measured. From the bytes that were measured, without reading the file again. It is parsed again because
            // TrueTypeFont is not thread-safe and PDFBox changes it when embedding it.
            FontFile file = resources.fonts().find(font);
            TrueTypeFont ttf;
            try (InputStream content = file.content()) {
                ttf = FontFile.parse(content.readAllBytes());
            }
            ttf.setEnableGsub(font.ligatures());
            f = PDType0Font.load(doc, ttf, !file.cff());
            if (file.cff()) {
                declareCff((PDType0Font) f);
            }
            fonts.put(font, f);
        }
        return f;
    }

    /**
     * Declares a font with CFF outlines as what it is. PDFBox cannot make a subset of CFF outlines, so they are
     * embedded whole, and it declares them as TrueType: a {@code CIDFontType2} descendant with the file in
     * {@code FontFile2}, which is only for TrueType outlines (ISO 32000-1, 9.9). The descendant becomes a
     * {@code CIDFontType0} and the file goes to {@code FontFile3} with subtype {@code OpenType} (PDF 1.6). The codes
     * are still the glyph numbers: in a CFF that is not CID-keyed, as are those of the OTF files, a CID is the glyph
     * number. Only the dictionaries change: PDFBox keeps writing the text with the font it loaded.
     */
    private static void declareCff(PDType0Font font) {
        COSDictionary descendant = font.getDescendantFont().getCOSObject();
        descendant.setItem(COSName.SUBTYPE, COSName.CID_FONT_TYPE0);
        descendant.removeItem(COSName.CID_TO_GID_MAP);
        COSDictionary descriptor = descendant.getCOSDictionary(COSName.FONT_DESC);
        COSStream file = (COSStream) descriptor.getDictionaryObject(COSName.FONT_FILE2);
        descriptor.removeItem(COSName.FONT_FILE2);
        file.removeItem(COSName.LENGTH1);
        file.setItem(COSName.SUBTYPE, COSName.OPEN_TYPE);
        descriptor.setItem(COSName.FONT_FILE3, file);
    }

    /**
     * Argument of {@code TJ}: the text split after each space, with the extra space between
     * the pieces. A number in {@code TJ} shifts to the left, in thousandths of the font size.
     */
    static Object[] justified(String text, double wordSpacing, double fontSize) {
        return spaced(text, wordSpacing, 0, fontSize, null);
    }

    /**
     * Argument of {@code TJ} with the extra space of {@code justify} after each space, the
     * letter spacing after each character except the last and the kerning of each pair ({@code kerning}, in
     * thousandths of the font size, by position, as {@link dev.stxt.pdf.resources.FontFile#kerning} gives it; or
     * {@code null}). Without letter spacing, the text is split only after the spaces, if it is justified, and where a
     * pair has kerning; with it, one piece per character. {@code TJ} is used and not {@code Tc}/{@code Tw}
     * because the fonts are Type 0 Identity-H, where {@code Tw} has no effect.
     */
    static Object[] spaced(String text, double wordSpacing, double letterSpacing, double fontSize, int[] kerning) {
        List<Object> parts = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            int kern = kerning != null && i < text.length() ? kerning[i] : 0;
            boolean split = letterSpacing != 0 ? i < text.length() : cp == ' ' && wordSpacing != 0 || kern != 0;
            if (split) {
                double extra = letterSpacing + (cp == ' ' ? wordSpacing : 0);
                parts.add(text.substring(start, i));
                // A positive kerning moves the next glyph to the right: in TJ, a negative number.
                parts.add((float) (-extra * 1000 / fontSize - kern));
                start = i;
            }
        }
        parts.add(text.substring(start));
        return parts.toArray();
    }
}
