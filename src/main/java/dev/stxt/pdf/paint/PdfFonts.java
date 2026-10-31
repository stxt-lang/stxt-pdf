package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.resources.Resources;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The fonts of a PDF and how text is written with them. Each TTF font of the resources folder
 * (reference/common/resources.md) is embedded once per document and {@link FontFace} combination, as a subset
 * (only the glyphs used) in a Type 0 font with Identity-H encoding: the PDF looks the same on any
 * machine. With Identity-H each character takes two bytes and the {@code Tw} operator has no effect, so
 * justification and letter spacing go in the arguments of {@code TJ} ({@link #spaced}).
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

    /** The embedded font; it is loaded the first time it is used in the document. */
    PDFont font(FontFace font) throws IOException {
        PDFont f = fonts.get(font);
        if (f == null) {
            // Without ligatures the GSUB table is dropped: PDFBox writes one glyph per character, which is what was
            // measured. From the bytes that were measured, without reading the file again. It is parsed again because
            // TrueTypeFont is not thread-safe and PDFBox changes it when embedding it.
            TrueTypeFont ttf = new TTFParser().parse(new RandomAccessReadBuffer(
                    resources.fonts().find(font).content()));
            ttf.setEnableGsub(font.ligatures());
            f = PDType0Font.load(doc, ttf, true);
            fonts.put(font, f);
        }
        return f;
    }

    /**
     * Argument of {@code TJ}: the text split after each space, with the extra space between
     * the pieces. A number in {@code TJ} shifts to the left, in thousandths of the font size.
     */
    static Object[] justified(String text, double wordSpacing, double fontSize) {
        return spaced(text, wordSpacing, 0, fontSize);
    }

    /**
     * Argument of {@code TJ} with the extra space of {@code justify} after each space and the
     * letter spacing after each character except the last. Without letter spacing, the text is split only
     * after the spaces; with it, one piece per character. {@code TJ} is used and not {@code Tc}/{@code Tw}
     * because the fonts are Type 0 Identity-H, where {@code Tw} has no effect.
     */
    static Object[] spaced(String text, double wordSpacing, double letterSpacing, double fontSize) {
        if (letterSpacing != 0) {
            List<Object> parts = new ArrayList<>();
            for (int i = 0; i < text.length(); ) {
                int cp = text.codePointAt(i);
                i += Character.charCount(cp);
                parts.add(new String(Character.toChars(cp)));
                if (i < text.length()) {
                    parts.add((float) (-(letterSpacing + (cp == ' ' ? wordSpacing : 0)) * 1000 / fontSize));
                }
            }
            return parts.toArray();
        }
        Float shift = (float) (-wordSpacing * 1000 / fontSize);
        List<Object> parts = new ArrayList<>();
        int start = 0;
        for (int i = text.indexOf(' '); i >= 0; i = text.indexOf(' ', i + 1)) {
            parts.add(text.substring(start, i + 1));
            parts.add(shift);
            start = i + 1;
        }
        parts.add(text.substring(start));
        return parts.toArray();
    }
}
