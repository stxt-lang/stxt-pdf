package dev.stxt.pdf.resources;

import dev.stxt.pdf.model.DecorationMetrics;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.VerticalMetrics;
import org.apache.fontbox.ttf.CmapLookup;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.fontbox.ttf.gsub.GsubWorker;
import org.apache.fontbox.ttf.gsub.GsubWorkerFactory;
import org.apache.fontbox.ttf.model.GsubData;

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
 * A TTF file of the fonts folder, already read, with its metrics (reference/common/resources.md).
 *
 * <p>The widths are those PDFBox writes in the PDF when it embeds the font (the advance of each
 * glyph in thousandths of the font size, rounded): this way the text measures the same when it is laid out as when it
 * is painted. Kerning is not applied. With ligatures (see {@link #width(String, double, boolean)}) the text
 * is measured with the same glyphs PDFBox writes, because the same GSUB table is applied.</p>
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
    /** Maximum number of words memoized with ligatures; past this, the memo is cleared. */
    private static final int MAX_LIGATURE_WORDS = 50_000;
    /** Advance not yet computed in {@link #bmpAdvance}. */
    private static final int UNKNOWN = -1;
    private static final byte HAS = 1;
    private static final byte LACKS = 2;

    private final Path path;
    private final FontFace face;
    private final TrueTypeFont ttf;
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
    /** The words already measured with ligatures, with their width in thousandths: applying GSUB is expensive. */
    private final ConcurrentHashMap<String, Integer> ligatureWords = new ConcurrentHashMap<>();
    /** The same one PDFBox applies when painting; {@code null} until requested and if the font has no GSUB. */
    private volatile GsubWorker gsub;
    private volatile boolean gsubRead;
    /** See {@link #digest()}; {@code null} until requested. */
    private volatile String digest;

    FontFile(Path path, FontFace face, TrueTypeFont ttf) throws IOException {
        this.path = path;
        this.face = face;
        this.ttf = ttf;
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
        return width(text, fontSize, false);
    }

    /**
     * Width of the text in points. With {@code ligatures}, each word (what lies between two white spaces)
     * is measured with the glyphs left after applying the GSUB table of the font, which are the ones
     * PDFBox writes when painting: {@code fi} can be a single glyph, of a different width. A font without GSUB, or
     * a word with some character without a glyph, is measured as usual.
     */
    public double width(String text, double fontSize, boolean ligatures) {
        GsubWorker worker = ligatures ? ligatureWorker() : null;
        long thousandths = 0;
        for (int i = 0; i < text.length(); ) {
            if (worker != null && !isSpace(text.charAt(i))) {
                int end = i;
                while (end < text.length() && !isSpace(text.charAt(end))) {
                    end++;
                }
                thousandths += ligatureWord(worker, text.substring(i, end));
                i = end;
                continue;
            }
            int cp = text.codePointAt(i);
            thousandths += advance(cp);
            i += Character.charCount(cp);
        }
        return thousandths / 1000.0 * fontSize;
    }

    /** What PDFBox takes as a space when splitting the text into words before applying GSUB ({@code \s}). */
    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\u000B' || c == '\f' || c == '\r';
    }

    /** The width of a word with the ligatures of the font, in thousandths of the font size. */
    private int ligatureWord(GsubWorker worker, String word) {
        Integer known = ligatureWords.get(word);
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
        int thousandths = 0;
        if (complete) {
            for (int glyph : worker.applyTransforms(glyphs)) {
                thousandths += glyphAdvance(glyph);
            }
        } else {
            for (int cp : codePoints) {
                thousandths += advance(cp);
            }
        }
        if (ligatureWords.size() > MAX_LIGATURE_WORDS) {
            ligatureWords.clear();
        }
        ligatureWords.put(word, thousandths);
        return thousandths;
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
