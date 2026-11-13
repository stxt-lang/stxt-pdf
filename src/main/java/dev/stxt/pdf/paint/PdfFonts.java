package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.resources.FontFile;
import dev.stxt.pdf.resources.Resources;
import org.apache.fontbox.ttf.CmapLookup;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.fontbox.ttf.gsub.GsubWorker;
import org.apache.fontbox.ttf.gsub.GsubWorkerFactory;
import org.apache.fontbox.ttf.model.GsubData;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdfwriter.COSWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.util.StringUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
    /** What applies the {@code GSUB} table of each font with ligatures: the same PDFBox would use ({@link #show}). */
    private final Map<PDType0Font, GsubWorker> workers = new HashMap<>();
    /** The words already written with each font with ligatures: their bytes and the glyphs {@code GSUB} leaves. */
    private final Map<PDType0Font, Map<String, Shaped>> shaped = new HashMap<>();

    /**
     * The fonts with ligatures a text has been written with: the engine names them in the page itself ({@link #select})
     * and makes their subset ({@link #finish}), so that PDFBox does not make it again when the document is saved.
     */
    private final Set<PDType0Font> own = new LinkedHashSet<>();
    /**
     * The text of the glyphs that {@code GSUB} puts in place of others, by font: {@code fi} for the glyph of that
     * ligature. It is what is copied from the PDF ({@link #finish}).
     */
    private final Map<PDType0Font, Map<Integer, String>> substituted = new HashMap<>();

    /** A word as it is written with ligatures: the codes of its glyphs and those glyphs, for the subset. */
    private record Shaped(byte[] bytes, Set<Integer> glyphs) {
    }

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

    /**
     * Writes a text with the font it was set with, if the font has ligatures: {@code text}, a {@code String} (shown
     * with {@code Tj}) or the argument of {@code TJ} ({@link #spaced}). PDFBox applies the {@code GSUB} table to each
     * word of each text it shows, and each time it builds the regular expression of all the substitutions of the
     * font: a page of running text took fourteen times longer to write with {@code Ligatures: true}. Here each word
     * of each font is worked out once per document, with the same steps ({@code PDAbstractContentStream}:
     * {@code showTextInternal}, {@code encodeForGsub} and {@code applyGSUBRules}), and the bytes written and the
     * glyphs marked for the subset are the same.
     *
     * @return {@code false} if the font has no {@code GSUB} to apply: nothing is written, and the text is shown as
     *         always
     */
    @SuppressWarnings("deprecation") // appendRawCommands: the only way to write what showText would, without it.
    boolean show(PDPageContentStream out, PDFont font, Object text) throws IOException {
        if (!(font instanceof PDType0Font type0) || type0.getGsubData() == GsubData.NO_DATA_FOUND) {
            return false;
        }
        if (text instanceof String s) {
            out.appendRawCommands(string(type0, s));
            out.appendRawCommands(" Tj\n");
            return true;
        }
        out.appendRawCommands("[");
        for (Object part : (Object[]) text) {
            if (part instanceof String s) {
                out.appendRawCommands(string(type0, s));
            } else {
                out.appendRawCommands((float) (Float) part);
            }
        }
        out.appendRawCommands("] TJ\n");
        return true;
    }

    /** The string of a text with ligatures, as the content stream has it, with its glyphs marked for the subset. */
    private byte[] string(PDType0Font font, String text) throws IOException {
        GsubWorker worker = workers.get(font);
        if (worker == null) {
            worker = new GsubWorkerFactory().getGsubWorker(font.getCmapLookup(), font.getGsubData());
            workers.put(font, worker);
        }
        Map<String, Shaped> known = shaped.computeIfAbsent(font, f -> new HashMap<>());
        ByteArrayOutputStream codes = new ByteArrayOutputStream(2 * text.length());
        Set<Integer> glyphs = new HashSet<>();
        for (String word : StringUtil.tokenizeOnSpace(text)) {
            if (word == null) {
                continue;
            }
            if (word.length() == 1 && Character.isWhitespace(word.charAt(0))) {
                codes.write(font.encode(word));
                continue;
            }
            Shaped w = known.get(word);
            if (w == null) {
                w = shape(font, worker, word, substituted.computeIfAbsent(font, f -> new TreeMap<>()));
                known.put(word, w);
            }
            codes.write(w.bytes());
            glyphs.addAll(w.glyphs());
        }
        if (font.willBeSubset()) {
            font.addGlyphsToSubset(glyphs);
            for (int i = 0; i < text.length(); ) {
                int codePoint = text.codePointAt(i);
                font.addToSubset(codePoint);
                i += Character.charCount(codePoint);
            }
        }
        ByteArrayOutputStream string = new ByteArrayOutputStream(2 * codes.size() + 2);
        COSWriter.writeString(codes.toByteArray(), string);
        return string.toByteArray();
    }

    /**
     * The glyphs of a word after {@code GSUB}, encoded. The glyphs that are not those of its characters are added
     * to {@code substituted} with the characters they stand for.
     */
    private static Shaped shape(PDType0Font font, GsubWorker worker, String word, Map<Integer, String> substituted)
            throws IOException {
        CmapLookup cmap = font.getCmapLookup();
        List<Integer> original = new ArrayList<>(word.length());
        int[] codePoints = word.codePoints().toArray();
        for (int codePoint : codePoints) {
            int glyph = cmap.getGlyphId(codePoint);
            if (glyph <= 0) {
                // What PDFBox does: the text was measured with this font, so the glyph exists (GLYPH_MISSING).
                throw new IllegalStateException(String.format("could not find the glyphId for the character, "
                        + "codePoint: %d (0x%X)", codePoint, codePoint));
            }
            original.add(glyph);
        }
        List<Integer> glyphs = worker.applyTransforms(original);
        if (!glyphs.equals(original)) {
            sources(worker, original, glyphs, codePoints, substituted);
        }
        ByteArrayOutputStream codes = new ByteArrayOutputStream(2 * glyphs.size());
        for (Integer glyph : glyphs) {
            codes.write(font.encodeGlyphId(glyph));
        }
        return new Shaped(codes.toByteArray(), Set.copyOf(glyphs));
    }

    /**
     * The characters each glyph of {@code glyphs} stands for, for those that are not the glyph of a character of the
     * word: the shortest run of characters, from where the previous glyph ends, that {@code GSUB} turns into that
     * glyph alone. If a glyph has none (a substitution that depends on its neighbors), the rest of the word is left
     * as PDFBox has it.
     */
    private static void sources(GsubWorker worker, List<Integer> original, List<Integer> glyphs, int[] codePoints,
                                Map<Integer, String> substituted) {
        int at = 0;
        for (Integer glyph : glyphs) {
            if (at < original.size() && original.get(at).equals(glyph)) {
                at++;
                continue;
            }
            int length = 0;
            for (int k = 1; at + k <= original.size() && length == 0; k++) {
                if (worker.applyTransforms(original.subList(at, at + k)).equals(List.of(glyph))) {
                    length = k;
                }
            }
            if (length == 0) {
                return;
            }
            substituted.putIfAbsent(glyph, new String(codePoints, at, length));
            at += length;
        }
    }

    /**
     * Sets the font of the text that follows. A font with ligatures is named in the resources and set by the engine
     * ({@code Tf}), with what {@code setFont} would write: PDFBox then does not know that the document uses it, and
     * leaves its subset to {@link #finish}.
     */
    @SuppressWarnings("deprecation") // appendRawCommands, as in show.
    void select(PDPageContentStream out, PDResources resources, PDFont font, float size) throws IOException {
        if (!(font instanceof PDType0Font type0) || type0.getGsubData() == GsubData.NO_DATA_FOUND) {
            out.setFont(font, size);
            return;
        }
        own.add(type0);
        ByteArrayOutputStream name = new ByteArrayOutputStream();
        resources.add(font).writePDF(name);
        name.write(' ');
        out.appendRawCommands(name.toByteArray());
        out.appendRawCommands(size);
        out.appendRawCommands("Tf\n");
    }

    /**
     * Ends the fonts with ligatures, once every page is painted: makes their subsets, which PDFBox would make when
     * saving, and gives the glyphs of their ligatures the text they stand for. PDFBox gives each glyph one character,
     * the one of the font for that glyph ({@code U+FB01} for the {@code fi} ligature), and that is what would be
     * copied from the PDF and what a search would have to find; with the letters ({@code fi}), the text of the PDF is
     * the text of the document (reference/common/text-formatting.md).
     */
    void finish() throws IOException {
        for (PDType0Font font : own) {
            if (font.willBeSubset()) {
                font.subset();
            }
            Map<Integer, String> texts = substituted.getOrDefault(font, Map.of());
            if (!texts.isEmpty()) {
                ToUnicode.replace(doc, font, texts);
            }
        }
        own.clear();
    }
}
