package dev.stxt.pdf.resources;

import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
import org.apache.fontbox.ttf.CmapLookup;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The kerning pairs of the fonts of the repository ({@link KerningPairs}, {@link FontFile#kerning}), against the values
 * of their {@code GPOS} tables as fontTools reads them, and the {@code kern} table of a font that has no {@code GPOS}.
 */
class KerningPairsTest {

    private static final Resources RESOURCES = new Resources();
    private static final FontFace SANS = new FontFace("IBM Plex Sans", FontWeight.NORMAL, FontStyle.NORMAL);
    private static final FontFace SERIF = new FontFace("IBM Plex Serif", FontWeight.NORMAL, FontStyle.NORMAL);
    private static final FontFace MONO = new FontFace("IBM Plex Mono", FontWeight.NORMAL, FontStyle.NORMAL);
    private static final FontFace SEMIBOLD = new FontFace("IBM Plex Sans", new FontWeight(600), FontStyle.NORMAL);

    @Test
    void pairsOfTheClassesAndOfTheGlyphs() {
        FontFile sans = RESOURCES.fonts().find(SANS);
        // A-V and T-A are pairs of classes (format 2); P-. and L-' come from the pairs of glyphs (format 1).
        assertArrayEquals(new int[]{0, -41, -41, -65, -65, 0}, sans.kerning("AVATAR", false));
        assertArrayEquals(new int[]{0, -100}, sans.kerning("P.", false));
        assertArrayEquals(new int[]{0, -70}, sans.kerning("L'", false));
        assertArrayEquals(new int[]{0, 0, 0, -4, 0, 0, 0}, sans.kerning("Informe", false));
    }

    @Test
    void positiveAndOtherFonts() {
        FontFile serif = RESOURCES.fonts().find(SERIF);
        assertArrayEquals(new int[]{0, -50}, serif.kerning("AV", false));
        assertArrayEquals(new int[]{0, 5}, serif.kerning("Ty", false));
        // IBM Plex Mono has a GPOS table, without kern feature, and no kern table.
        assertNull(RESOURCES.fonts().find(MONO).kerning("AVATAR", false));
    }

    @Test
    void theCffOutlinesHaveTheSamePairsAsTheTrueTypeOnes() {
        FontFile ttf = RESOURCES.fonts().find(SEMIBOLD);
        FontFile otf = new Resources(FontLibraryTest.OTF_RESOURCES).fonts().find(SEMIBOLD);
        assertArrayEquals(new int[]{0, -50, 4, -65}, ttf.kerning("AVTo", false));
        assertArrayEquals(ttf.kerning("AVTo", false), otf.kerning("AVTo", false));
    }

    @Test
    void theWidthAddsThePairs() {
        FontFile sans = RESOURCES.fonts().find(SANS);
        double plain = sans.width("AVATAR", 10, false, false);
        assertEquals(plain - 2.12, sans.width("AVATAR", 10, false, true), 1e-9);
        assertEquals(plain, sans.width("AVATAR", 10), 1e-9);
    }

    @Test
    void noPairAcrossASpace() {
        FontFile sans = RESOURCES.fonts().find(SANS);
        assertNull(sans.kerning("A V", false));
        assertEquals(sans.width("A V", 10), sans.width("A V", 10, false, true), 1e-9);
        // Each word on its own: the text adds what its words add.
        assertArrayEquals(new int[]{0, -41, 0, 0, -65}, sans.kerning("AV To", false));
        assertEquals(sans.width("AV", 10, false, true) + sans.width(" ", 10) + sans.width("To", 10, false, true),
                sans.width("AV To", 10, false, true), 1e-9);
    }

    @Test
    void withLigaturesThePairsAreThoseOfTheGlyphsLeft() {
        FontFile sans = RESOURCES.fonts().find(SANS);
        // T and the fi ligature are a pair (-15); between f and i, inside the ligature, there is none.
        assertArrayEquals(new int[]{0, -15, 0}, sans.kerning("Tfi", true));
        assertEquals(sans.width("Tfi", 10, true, false) - 0.15, sans.width("Tfi", 10, true, true), 1e-9);
        // Without ligatures, T-f is the pair, with the same value, and f-i has none.
        assertArrayEquals(new int[]{0, -15, 0}, sans.kerning("Tfi", false));
    }

    @Test
    void theKernTableOfAFontWithoutGpos() throws IOException {
        byte[] plex = Files.readAllBytes(RESOURCES.fonts().find(SANS).path());
        TrueTypeFont original = FontFile.parse(plex);
        CmapLookup cmap = original.getUnicodeCmapLookup();
        int a = cmap.getGlyphId('A');
        int v = cmap.getGlyphId('V');
        Map<String, byte[]> tables = tables(plex);
        tables.remove("GPOS");
        tables.put("kern", kern(a, v, -80));
        FontFile font = font(rebuild(tables));
        assertArrayEquals(new int[]{0, -80, 0}, font.kerning("AVA", false));
        assertEquals(font.width("AVA", 10) - 0.8, font.width("AVA", 10, false, true), 1e-9);
    }

    @Test
    void aFontWithoutPairsMeasuresAsWithoutKerning() throws IOException {
        Map<String, byte[]> tables = tables(Files.readAllBytes(RESOURCES.fonts().find(SANS).path()));
        tables.remove("GPOS");
        FontFile font = font(rebuild(tables));
        assertNull(font.kerning("AVATAR", false));
        assertEquals(font.width("AVATAR", 10), font.width("AVATAR", 10, false, true), 1e-9);
    }

    @Test
    void aGposTableThatCannotBeReadIsAProblemOfTheFont() throws IOException {
        Map<String, byte[]> tables = tables(Files.readAllBytes(RESOURCES.fonts().find(SANS).path()));
        // A header whose lists are outside the table.
        tables.put("GPOS", new byte[]{0, 1, 0, 0, 0x7F, 0, 0x7F, 0, 0x7F, 0});
        FontFile font = font(rebuild(tables));
        ResourceException e = assertThrows(ResourceException.class, () -> font.kerning("AV", false));
        assertTrue(e.getMessage().startsWith("Cannot read the kerning of the font"), e.getMessage());
        // Without kerning, the font measures as always: the table is not read.
        assertEquals(RESOURCES.fonts().find(SANS).width("AV", 10), font.width("AV", 10), 1e-9);
    }

    private static FontFile font(byte[] content) throws IOException {
        return new FontFile(Path.of("test.ttf"), SANS, FontFile.parse(content));
    }

    /** The tables of a TrueType file, by tag. */
    private static Map<String, byte[]> tables(byte[] font) {
        ByteBuffer in = ByteBuffer.wrap(font);
        int count = in.getShort(4) & 0xFFFF;
        Map<String, byte[]> tables = new TreeMap<>();
        for (int i = 0; i < count; i++) {
            int record = 12 + 16 * i;
            String tag = new String(font, record, 4, StandardCharsets.US_ASCII);
            int offset = in.getInt(record + 8);
            int length = in.getInt(record + 12);
            byte[] data = new byte[length];
            System.arraycopy(font, offset, data, 0, length);
            tables.put(tag, data);
        }
        return tables;
    }

    /** A TrueType file with those tables, in order of tag, each one at a multiple of 4; the checksums are not used. */
    private static byte[] rebuild(Map<String, byte[]> tables) {
        int count = tables.size();
        int offset = 12 + 16 * count;
        ByteBuffer directory = ByteBuffer.allocate(offset);
        directory.putInt(0x00010000).putShort((short) count);
        int power = Integer.highestOneBit(count);
        directory.putShort((short) (power * 16)).putShort((short) Integer.numberOfTrailingZeros(power))
                .putShort((short) (count * 16 - power * 16));
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        for (Map.Entry<String, byte[]> table : tables.entrySet()) {
            directory.put(table.getKey().getBytes(StandardCharsets.US_ASCII)).putInt(0).putInt(offset + data.size())
                    .putInt(table.getValue().length);
            data.writeBytes(table.getValue());
            while (data.size() % 4 != 0) {
                data.write(0);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(directory.array());
        out.writeBytes(data.toByteArray());
        return out.toByteArray();
    }

    /** A {@code kern} table (version 0) with a horizontal subtable of format 0 and a single pair. */
    private static byte[] kern(int left, int right, int value) {
        ByteBuffer kern = ByteBuffer.allocate(4 + 14 + 6);
        kern.putShort((short) 0).putShort((short) 1);
        kern.putShort((short) 0).putShort((short) 20).putShort((short) 0x0001);
        kern.putShort((short) 1).putShort((short) 6).putShort((short) 0).putShort((short) 0);
        kern.putShort((short) left).putShort((short) right).putShort((short) value);
        return kern.array();
    }
}
