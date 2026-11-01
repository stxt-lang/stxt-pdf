package dev.stxt.pdf.resources;

import dev.stxt.pdf.model.DecorationMetrics;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.VerticalMetrics;
import org.apache.fontbox.ttf.CmapLookup;
import org.apache.fontbox.ttf.OTFParser;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.fontbox.ttf.gsub.GsubWorker;
import org.apache.fontbox.ttf.gsub.GsubWorkerFactory;
import org.apache.fontbox.ttf.model.GsubData;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A TTF or OTF file of the fonts folder, already read, with its metrics (reference/common/resources.md). An OTF file
 * may have TrueType outlines, like a TTF, or CFF ones ({@link #cff()}), which are embedded differently in the PDF.
 *
 * <p>The widths are those PDFBox writes in the PDF when it embeds the font (the advance of each
 * glyph in thousandths of the font size, rounded): this way the text measures the same when it is laid out as when it
 * is painted. With ligatures (see {@link #width(String, double, boolean, boolean)}) the text
 * is measured with the same glyphs PDFBox writes, because the same GSUB table is applied. With kerning, each pair
 * adds its adjustment, rounded to thousandths as well, which is what painting writes between the two glyphs
 * ({@link #kerning(String, boolean)}).</p>
 *
 * <p>It is thread-safe and uses no locks when measuring: when the font is opened the advance of
 * each glyph is computed, and that of each character is memoized on request in a table of integers, whose writes
 * are idempotent (two threads write the same value in the same slot, and an integer is written
 * whole).</p>
 *
 * <p>The file is read only once, when the folder is indexed: {@link #content()} gives those bytes to whoever embeds
 * the font in the PDF or loads it into AWT for an SVG, and {@link #digest()} comes from them. What is embedded and
 * drawn is what was measured even if the file changes or disappears later.</p>
 */
public final class FontFile {

    /** The characters of the Basic Multilingual Plane, which have their own table of advances. */
    private static final int BMP = 0x10000;
    /** Maximum number of words memoized with ligatures or kerning, in each memo; past this, it is cleared. */
    private static final int MAX_WORDS = 50_000;
    /** The tag an OpenType file with CFF outlines starts with; the others start with a version number. */
    private static final int OTTO = 0x4F54544F;
    /** Advance not yet computed in {@link #bmpAdvance}. */
    private static final int UNKNOWN = -1;
    private static final byte HAS = 1;
    private static final byte LACKS = 2;

    private final Path path;
    private final FontFace face;
    private final TrueTypeFont ttf;
    private final boolean cff;
    private final CmapLookup cmap;
    private final double unitsPerEm;
    private final VerticalMetrics vertical;
    private final DecorationMetrics decoration;
    /** Advance of each glyph in thousandths of the font size, rounded as PDFBox writes it. */
    private final int[] glyphAdvance;
    /** Advance of each character of the Basic Multilingual Plane, or {@link #UNKNOWN}. */
    private final int[] bmpAdvance = new int[BMP];
    /** Whether the font has a glyph for each character of the BMP: 0 not checked, {@link #HAS} or {@link #LACKS}. */
    private final byte[] bmpHas = new byte[BMP];
    /**
     * The words already measured with ligatures, with kerning or with both, with their width and their kerning:
     * applying GSUB is expensive, and so is looking up the pairs.
     */
    private final ConcurrentHashMap<String, Shaped> ligatureWords = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Shaped> kernedWords = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Shaped> ligatureKernedWords = new ConcurrentHashMap<>();
    /** The same one PDFBox applies when painting; {@code null} until requested and if the font has no GSUB. */
    private volatile GsubWorker gsub;
    private volatile boolean gsubRead;
    /** See {@link #kerningPairs()}; {@code null} until requested and if the font has no pairs. */
    private volatile KerningPairs kerningPairs;
    private volatile boolean kerningRead;
    /** See {@link #digest()}; {@code null} until requested. */
    private volatile String digest;

    FontFile(Path path, FontFace face, TrueTypeFont ttf) throws IOException {
        this.path = path;
        this.face = face;
        this.ttf = ttf;
        this.cff = ttf.getTableMap().containsKey("CFF ");
        this.cmap = ttf.getUnicodeCmapLookup();
        this.unitsPerEm = ttf.getUnitsPerEm();
        this.vertical = new VerticalMetrics(ttf.getHorizontalHeader().getAscender() / unitsPerEm,
                -ttf.getHorizontalHeader().getDescender() / unitsPerEm);
        this.decoration = new DecorationMetrics(ttf.getPostScript().getUnderlinePosition() / unitsPerEm,
                ttf.getPostScript().getUnderlineThickness() / unitsPerEm,
                ttf.getOS2Windows().getStrikeoutPosition() / unitsPerEm,
                ttf.getOS2Windows().getStrikeoutSize() / unitsPerEm);
        this.glyphAdvance = new int[ttf.getNumberOfGlyphs()];
        for (int glyph = 0; glyph < glyphAdvance.length; glyph++) {
            glyphAdvance[glyph] = (int) Math.round(ttf.getAdvanceWidth(glyph) * 1000 / unitsPerEm);
        }
        Arrays.fill(bmpAdvance, UNKNOWN);
    }

    /**
     * Reads a font file: with the parser of OpenType fonts if it has CFF outlines (it starts with {@code OTTO}) and
     * with that of TrueType otherwise, whatever its extension. Each call gives a new object, which only one thread may
     * use.
     */
    public static TrueTypeFont parse(byte[] content) throws IOException {
        boolean otto = content.length >= 4 && ((content[0] & 0xFF) << 24 | (content[1] & 0xFF) << 16
                | (content[2] & 0xFF) << 8 | content[3] & 0xFF) == OTTO;
        RandomAccessRead in = new RandomAccessReadBuffer(content);
        return otto ? new OTFParser().parse(in) : new TTFParser().parse(in);
    }

    public Path path() {
        return path;
    }

    public FontFace face() {
        return face;
    }

    /**
     * The content of the file as it was read when the folder was indexed, without reading it again. Each call gives a
     * new stream over the bytes fontbox keeps, without copying them ({@link TrueTypeFont#getOriginalData()}), so it
     * can be requested from several threads.
     */
    public InputStream content() {
        try {
            return ttf.getOriginalData();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Whether the outlines are CFF (an OTF file with a {@code CFF } table) and not TrueType: PDFBox cannot embed them
     * as a subset, and the PDF declares them as another kind of font (reference/implementation/pdf.md).
     */
    public boolean cff() {
        return cff;
    }

    /**
     * The fingerprint of the content of the file (SHA-256 in lowercase hexadecimal): two files with the same bytes
     * have the same one, wherever they are, and two different ones do not. It is computed the first time it is
     * requested.
     */
    public String digest() {
        String value = digest;
        if (value == null) {
            try (InputStream in = content()) {
                value = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("The JVM has no SHA-256", e);
            }
            // Two threads may compute it at the same time: both write the same string.
            digest = value;
        }
        return value;
    }

    /** Ascent and descent from the {@code hhea} table. */
    public VerticalMetrics vertical() {
        return vertical;
    }

    /** Underline ({@code post} table) and strikeout ({@code OS/2} table). */
    public DecorationMetrics decoration() {
        return decoration;
    }

    /**
     * Whether the font has a glyph for the character. Those of the Basic Multilingual Plane are memoized: every
     * character of the document is asked about before layout. As with {@link #bmpAdvance}, several threads may write
     * the same answer at the same time.
     */
    public boolean has(int codePoint) {
        if (codePoint >= BMP) {
            return cmap.getGlyphId(codePoint) != 0;
        }
        byte known = bmpHas[codePoint];
        if (known == 0) {
            known = cmap.getGlyphId(codePoint) != 0 ? HAS : LACKS;
            bmpHas[codePoint] = known;
        }
        return known == HAS;
    }

    /** Width of the text in points, without ligatures. Characters without a glyph measure like {@code .notdef}. */
    public double width(String text, double fontSize) {
        return width(text, fontSize, false, false);
    }

    /** Width of the text in points, without kerning. */
    public double width(String text, double fontSize, boolean ligatures) {
        return width(text, fontSize, ligatures, false);
    }

    /**
     * Width of the text in points. With {@code ligatures}, each word (what lies between two white spaces)
     * is measured with the glyphs left after applying the GSUB table of the font, which are the ones
     * PDFBox writes when painting: {@code fi} can be a single glyph, of a different width. A font without GSUB, or
     * a word with some character without a glyph, is measured as usual. With {@code kerning}, each word adds the
     * adjustments of its pairs ({@link #kerning(String, boolean)}); a pair never includes a white space.
     */
    public double width(String text, double fontSize, boolean ligatures, boolean kerning) {
        GsubWorker worker = ligatures ? ligatureWorker() : null;
        KerningPairs pairs = kerning ? kerningPairs() : null;
        long thousandths = 0;
        for (int i = 0; i < text.length(); ) {
            if ((worker != null || pairs != null) && !isSpace(text.charAt(i))) {
                int end = wordEnd(text, i);
                thousandths += shaped(worker, pairs, text.substring(i, end)).width();
                i = end;
                continue;
            }
            int cp = text.codePointAt(i);
            thousandths += advance(cp);
            i += Character.charCount(cp);
        }
        return thousandths / 1000.0 * fontSize;
    }

    /**
     * The kerning of the text: for each position, the adjustment in thousandths of the font size that goes between
     * the character before it and the one that starts there; {@code null} if the text has no pair with an adjustment
     * (or the font has no pairs). Painting writes these same numbers, and {@link #width(String, double, boolean,
     * boolean)} adds them.
     *
     * <p>A pair is two consecutive characters of a word, without white space between them, both with their glyph in
     * the font. With {@code ligatures}, the pairs are those of the glyphs left after GSUB, only between two characters
     * where the word can be split without changing its glyphs: never inside a ligature.</p>
     */
    public int[] kerning(String text, boolean ligatures) {
        KerningPairs pairs = kerningPairs();
        if (pairs == null) {
            return null;
        }
        GsubWorker worker = ligatures ? ligatureWorker() : null;
        int[] result = null;
        for (int i = 0; i < text.length(); ) {
            if (isSpace(text.charAt(i))) {
                i++;
                continue;
            }
            int end = wordEnd(text, i);
            int[] word = shaped(worker, pairs, text.substring(i, end)).kerning();
            if (word != null) {
                if (result == null) {
                    result = new int[text.length()];
                }
                System.arraycopy(word, 0, result, i, word.length);
            }
            i = end;
        }
        return result;
    }

    /** Where the word that starts at {@code i} ends: at the next white space or at the end of the text. */
    private static int wordEnd(String text, int i) {
        int end = i;
        while (end < text.length() && !isSpace(text.charAt(end))) {
            end++;
        }
        return end;
    }

    /** What PDFBox takes as a space when splitting the text into words before applying GSUB ({@code \s}). */
    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\u000B' || c == '\f' || c == '\r';
    }

    /**
     * A word measured with ligatures ({@code worker}), with kerning ({@code pairs}) or with both, memoized: its width
     * in thousandths of the font size, its kerning included, and the kerning of each of its positions.
     */
    private record Shaped(int width, int[] kerning) {
    }

    private Shaped shaped(GsubWorker worker, KerningPairs pairs, String word) {
        ConcurrentHashMap<String, Shaped> memo = pairs == null ? ligatureWords
                : worker == null ? kernedWords : ligatureKernedWords;
        Shaped known = memo.get(word);
        if (known != null) {
            return known;
        }
        int[] codePoints = word.codePoints().toArray();
        List<Integer> glyphs = new ArrayList<>(codePoints.length);
        boolean complete = true;
        for (int cp : codePoints) {
            int glyph = cmap.getGlyphId(cp);
            complete &= glyph > 0;
            glyphs.add(glyph);
        }
        Shaped shaped = worker != null && complete ? withLigatures(worker, pairs, word, codePoints, glyphs)
                : withoutLigatures(pairs, word, codePoints, glyphs);
        if (memo.size() > MAX_WORDS) {
            memo.clear();
        }
        memo.put(word, shaped);
        return shaped;
    }

    /** One glyph per character, and the pairs of each two consecutive ones that have their glyph. */
    private Shaped withoutLigatures(KerningPairs pairs, String word, int[] codePoints, List<Integer> glyphs) {
        int thousandths = 0;
        for (int cp : codePoints) {
            thousandths += advance(cp);
        }
        int[] kerning = null;
        if (pairs != null) {
            int at = Character.charCount(codePoints[0]);
            for (int i = 1; i < codePoints.length; i++) {
                int left = glyphs.get(i - 1);
                int right = glyphs.get(i);
                int k = left > 0 && right > 0 ? thousandths(pairs.adjustment(left, right)) : 0;
                if (k != 0) {
                    if (kerning == null) {
                        kerning = new int[word.length()];
                    }
                    kerning[at] = k;
                    thousandths += k;
                }
                at += Character.charCount(codePoints[i]);
            }
        }
        return new Shaped(thousandths, kerning);
    }

    /**
     * The glyphs left after GSUB and, with {@code pairs}, the kerning between two characters where the word can be
     * split: where what GSUB leaves of the two parts, one after the other, are the glyphs of the whole word, which is
     * what PDFBox writes when painting each part on its own. Inside a ligature the word cannot be split. If the parts
     * of all the positions with kerning together do not give the glyphs of the word, which a font with contextual
     * substitutions may do, the word has no kerning: what is measured is always what is painted.
     */
    private Shaped withLigatures(GsubWorker worker, KerningPairs pairs, String word, int[] codePoints,
                                 List<Integer> glyphs) {
        List<Integer> whole = worker.applyTransforms(glyphs);
        int thousandths = 0;
        for (int glyph : whole) {
            thousandths += glyphAdvance(glyph);
        }
        if (pairs == null) {
            return new Shaped(thousandths, null);
        }
        int[] kerning = null;
        int kerned = 0;
        List<Integer> cuts = new ArrayList<>();
        int at = Character.charCount(codePoints[0]);
        for (int i = 1; i < codePoints.length; i++) {
            List<Integer> left = worker.applyTransforms(glyphs.subList(0, i));
            List<Integer> right = worker.applyTransforms(glyphs.subList(i, glyphs.size()));
            int n = left.size();
            if (!left.isEmpty() && !right.isEmpty() && n + right.size() == whole.size()
                    && whole.subList(0, n).equals(left) && whole.subList(n, whole.size()).equals(right)) {
                int k = thousandths(pairs.adjustment(whole.get(n - 1), whole.get(n)));
                if (k != 0) {
                    if (kerning == null) {
                        kerning = new int[word.length()];
                    }
                    kerning[at] = k;
                    kerned += k;
                    cuts.add(i);
                }
            }
            at += Character.charCount(codePoints[i]);
        }
        if (kerning != null && cuts.size() > 1 && !sameGlyphs(worker, glyphs, cuts, whole)) {
            return new Shaped(thousandths, null);
        }
        return new Shaped(thousandths + kerned, kerning);
    }

    /** Whether the parts of the word between {@code cuts}, each one after GSUB, give the glyphs of the word. */
    private static boolean sameGlyphs(GsubWorker worker, List<Integer> glyphs, List<Integer> cuts,
                                      List<Integer> whole) {
        List<Integer> joined = new ArrayList<>(whole.size());
        int from = 0;
        for (int cut : cuts) {
            joined.addAll(worker.applyTransforms(glyphs.subList(from, cut)));
            from = cut;
        }
        joined.addAll(worker.applyTransforms(glyphs.subList(from, glyphs.size())));
        return joined.equals(whole);
    }

    /** An adjustment in units of the font, in thousandths of the font size, rounded as painting writes it. */
    private int thousandths(int units) {
        return (int) Math.round(units * 1000 / unitsPerEm);
    }

    /**
     * The kerning pairs of the font, read the first time they are requested; {@code null} if it has none. A table
     * that cannot be read is a problem of the font, not of the document.
     *
     * @throws ResourceException if the {@code GPOS} or {@code kern} table cannot be read
     */
    private KerningPairs kerningPairs() {
        if (!kerningRead) {
            // fontbox reads the tables from the file on demand, which cannot be done from several threads.
            synchronized (ttf) {
                if (!kerningRead) {
                    try {
                        kerningPairs = KerningPairs.read(ttf);
                    } catch (IOException e) {
                        throw new ResourceException("Cannot read the kerning of the font " + path + ": "
                                + e.getMessage(), e);
                    }
                    kerningRead = true;
                }
            }
        }
        return kerningPairs;
    }

    /** The GSUB worker of the font, or {@code null} if it has no GSUB table. */
    private GsubWorker ligatureWorker() {
        if (!gsubRead) {
            synchronized (this) {
                if (!gsubRead) {
                    try {
                        GsubData data = ttf.getGsubData();
                        gsub = data == GsubData.NO_DATA_FOUND ? null
                                : new GsubWorkerFactory().getGsubWorker(cmap, data);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    gsubRead = true;
                }
            }
        }
        return gsub;
    }

    /** Advance of a character, in thousandths of the font size. */
    private int advance(int codePoint) {
        if (codePoint >= BMP) {
            return glyphAdvance(cmap.getGlyphId(codePoint));
        }
        int advance = bmpAdvance[codePoint];
        if (advance == UNKNOWN) {
            advance = glyphAdvance(cmap.getGlyphId(codePoint));
            bmpAdvance[codePoint] = advance;
        }
        return advance;
    }

    /** Advance of a glyph, in thousandths of the font size. */
    private int glyphAdvance(int glyph) {
        if (glyph >= 0 && glyph < glyphAdvance.length) {
            return glyphAdvance[glyph];
        }
        // A glyph that does not exist in the font: whatever fontbox answers, which cannot be used across threads.
        synchronized (ttf) {
            try {
                return (int) Math.round(ttf.getAdvanceWidth(glyph) * 1000 / unitsPerEm);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
