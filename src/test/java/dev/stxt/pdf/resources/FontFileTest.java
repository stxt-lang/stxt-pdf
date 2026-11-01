package dev.stxt.pdf.resources;

import org.apache.fontbox.ttf.CmapLookup;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** The widths of {@link FontFile} on the fonts of the repository, TTF and OTF, against what fontbox answers. */
class FontFileTest {

    private static final Resources RESOURCES = new Resources();

    /** The width PDFBox writes: the advance of each glyph in thousandths of the font size, rounded. */
    private static double expected(TrueTypeFont ttf, CmapLookup cmap, int codePoint, double size) throws IOException {
        long thousandths = Math.round(ttf.getAdvanceWidth(cmap.getGlyphId(codePoint)) * 1000 / (double) ttf.getUnitsPerEm());
        return thousandths / 1000.0 * size;
    }

    @Test
    void everyCharacterMeasuresWhatFontboxAdvances() throws IOException {
        List<FontFile> fonts = new ArrayList<>(RESOURCES.fonts().all());
        assertFalse(fonts.isEmpty());
        // And the OTF with CFF outlines of the resources of the tests.
        fonts.addAll(new Resources(FontLibraryTest.OTF_RESOURCES).fonts().all());
        for (FontFile font : fonts) {
            // The OTF with CFF outlines needs the OpenType parser; FontFile.parse chooses it.
            TrueTypeFont ttf = FontFile.parse(Files.readAllBytes(font.path()));
            CmapLookup cmap = ttf.getUnicodeCmapLookup();
            // Two passes: the second one reads the advance table already cached.
            for (int pass = 0; pass < 2; pass++) {
                for (int cp = 0; cp < 0x10000; cp++) {
                    if (cp >= 0xD800 && cp < 0xE000) {
                        continue;
                    }
                    assertEquals(expected(ttf, cmap, cp, 10), font.width(new String(Character.toChars(cp)), 10), 1e-9,
                            font.face() + " U+" + Integer.toHexString(cp));
                }
            }
            for (int cp : new int[]{0x1F600, 0x10FFFF, 0x20000}) {
                assertEquals(expected(ttf, cmap, cp, 10), font.width(new String(Character.toChars(cp)), 10), 1e-9);
            }
        }
    }

    @Test
    void aTextIsTheSumOfItsCharacters() {
        FontFile font = RESOURCES.fonts().all().get(0);
        double sum = 0;
        for (char c : "Hola, món".toCharArray()) {
            sum += font.width(String.valueOf(c), 12);
        }
        assertEquals(sum, font.width("Hola, món", 12), 1e-9);
    }

    @Test
    void severalThreadsMeasureTheSameAsOne() throws Exception {
        FontFile font = RESOURCES.fonts().all().get(0);
        StringBuilder all = new StringBuilder();
        for (int cp = 0x20; cp < 0x3000; cp++) {
            all.appendCodePoint(cp);
        }
        String text = all.toString();
        // A new font for each reference measure does not work: the advance table fills up while measuring.
        FontFile fresh = new Resources().fonts().find(font.face());
        double expected = font.width(text, 10);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<Double>> results = new ArrayList<>();
            for (int t = 0; t < 4; t++) {
                results.add(pool.submit(() -> fresh.width(text, 10)));
            }
            for (Future<Double> r : results) {
                assertEquals(expected, r.get(), 1e-9);
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
