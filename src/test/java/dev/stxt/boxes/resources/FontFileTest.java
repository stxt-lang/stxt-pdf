package dev.stxt.boxes.resources;

import org.apache.fontbox.ttf.CmapLookup;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
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

/** Los anchos de {@link FontFile} sobre las fuentes del repositorio, frente a lo que contesta fontbox. */
class FontFileTest {

    private static final Resources RESOURCES = new Resources();

    /** El ancho que escribe PDFBox: el avance de cada glifo en milésimas del cuerpo, redondeado. */
    private static double expected(TrueTypeFont ttf, CmapLookup cmap, int codePoint, double size) throws IOException {
        long thousandths = Math.round(ttf.getAdvanceWidth(cmap.getGlyphId(codePoint)) * 1000 / (double) ttf.getUnitsPerEm());
        return thousandths / 1000.0 * size;
    }

    @Test
    void everyCharacterMeasuresWhatFontboxAdvances() throws IOException {
        List<FontFile> fonts = RESOURCES.fonts().all();
        assertFalse(fonts.isEmpty());
        for (FontFile font : fonts) {
            TrueTypeFont ttf = new TTFParser().parse(new RandomAccessReadBuffer(Files.readAllBytes(font.path())));
            CmapLookup cmap = ttf.getUnicodeCmapLookup();
            // Dos pasadas: la segunda lee la tabla de avances ya memorizada.
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
        // Una fuente nueva por cada medida de referencia no sirve: la tabla de avances se llena al medir.
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
