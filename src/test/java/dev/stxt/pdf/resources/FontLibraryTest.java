package dev.stxt.pdf.resources;

import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** On the fonts of the repository ({@code resources/fonts}) and copies of them in temporary folders. */
class FontLibraryTest {

    private static final Path PLEX = Resources.DEFAULT_ROOT.resolve("fonts/IBM Plex Sans");
    /** The resources of the tests: only IBM Plex Sans SemiBold as an OTF with CFF outlines. */
    static final Path OTF_RESOURCES = Path.of("test", "resources");

    @TempDir
    Path dir;

    private static FontFace plex(FontWeight weight, FontStyle style) {
        return new FontFace("IBM Plex Sans", weight, style);
    }

    /** Temporary resources with some variants of Plex, in {@code fonts/<folder>/}. */
    private Resources copy(String folder, String... variants) throws IOException {
        Path fonts = Files.createDirectories(dir.resolve("resources/fonts").resolve(folder));
        for (String v : variants) {
            Files.copy(PLEX.resolve("IBMPlexSans-" + v + ".ttf"), fonts.resolve(v + ".ttf"));
        }
        return new Resources(dir.resolve("resources"));
    }

    /** The OTF of the resources of the tests. */
    private static Path otf() {
        return OTF_RESOURCES.resolve("fonts/IBM Plex Sans/IBMPlexSans-SemiBold.otf");
    }

    /**
     * Writes {@code usWeightClass} (offset 4 of the {@code OS/2} table) in a font file: the table directory starts at
     * byte 12, with 16 bytes per table (tag, checksum, offset, length). The checksum is not updated: it is not read.
     */
    private static void setWeightClass(Path file, int weight) throws IOException {
        byte[] b = Files.readAllBytes(file);
        int tables = (b[4] & 0xFF) << 8 | b[5] & 0xFF;
        for (int i = 0; i < tables; i++) {
            int entry = 12 + 16 * i;
            if (new String(b, entry, 4, java.nio.charset.StandardCharsets.ISO_8859_1).equals("OS/2")) {
                int offset = (b[entry + 8] & 0xFF) << 24 | (b[entry + 9] & 0xFF) << 16 | (b[entry + 10] & 0xFF) << 8
                        | b[entry + 11] & 0xFF;
                b[offset + 4] = (byte) (weight >> 8);
                b[offset + 5] = (byte) weight;
                Files.write(file, b);
                return;
            }
        }
        throw new IllegalStateException("No OS/2 table in " + file);
    }

    @Test
    void indexByFamilyWeightAndStyle() {
        FontLibrary fonts = new Resources().fonts();
        assertEquals(Set.of("IBM Plex Sans", "IBM Plex Serif", "IBM Plex Mono"), fonts.families());
        assertEquals("IBMPlexSans-Regular.ttf",
                fonts.find(plex(FontWeight.NORMAL, FontStyle.NORMAL)).path().getFileName().toString());
        assertEquals("IBMPlexSans-Bold.ttf",
                fonts.find(plex(FontWeight.BOLD, FontStyle.NORMAL)).path().getFileName().toString());
        assertEquals("IBMPlexSans-Italic.ttf",
                fonts.find(plex(FontWeight.NORMAL, FontStyle.ITALIC)).path().getFileName().toString());
        assertEquals("IBMPlexSans-BoldItalic.ttf",
                fonts.find(plex(FontWeight.BOLD, FontStyle.ITALIC)).path().getFileName().toString());
        // Light, Medium and SemiBold.
        assertEquals("IBMPlexSans-Light.ttf",
                fonts.find(plex(new FontWeight(300), FontStyle.NORMAL)).path().getFileName().toString());
        assertEquals("IBMPlexSans-Medium.ttf",
                fonts.find(plex(new FontWeight(500), FontStyle.NORMAL)).path().getFileName().toString());
        assertEquals("IBMPlexSans-SemiBold.ttf",
                fonts.find(plex(new FontWeight(600), FontStyle.NORMAL)).path().getFileName().toString());
        for (String family : List.of("IBM Plex Serif", "IBM Plex Mono")) {
            for (FontWeight weight : List.of(FontWeight.NORMAL, FontWeight.BOLD)) {
                for (FontStyle style : FontStyle.values()) {
                    assertNotNull(fonts.find(new FontFace(family, weight, style)), family + " " + weight + " " + style);
                }
            }
        }
    }

    /**
     * An OTF with CFF outlines is read with the OpenType parser and measures with its {@code hmtx} table, like a TTF:
     * the advance of {@code H} is 719 thousandths in the two SemiBold files, the OTF (CFF outlines) of the resources of
     * the tests and the TTF (TrueType outlines) of the repository.
     */
    @Test
    void otfWithCffOutlines() {
        FontFile otf = new Resources(OTF_RESOURCES).fonts().find(plex(new FontWeight(600), FontStyle.NORMAL));
        FontFile ttf = new Resources().fonts().find(plex(new FontWeight(600), FontStyle.NORMAL));
        assertEquals("IBMPlexSans-SemiBold.otf", otf.path().getFileName().toString());
        assertTrue(otf.cff());
        assertFalse(ttf.cff());
        assertEquals(7.19, otf.width("H", 10), 1e-9);
        assertEquals(ttf.width("Hola, eñe", 10), otf.width("Hola, eñe", 10), 1e-9);
        assertTrue(otf.has('ñ'));
    }

    /** A font distributed in TTF and in OTF has the same family, weight and style: the two files are an error. */
    @Test
    void theSameFontInTtfAndOtf() throws IOException {
        Resources r = copy("ttf", "SemiBold");
        Files.createDirectories(r.fontsFolder().resolve("otf"));
        Files.copy(otf(), r.fontsFolder().resolve("otf/SemiBold.otf"));
        ResourceException e = assertThrows(ResourceException.class, r::fonts);
        assertTrue(e.getMessage().startsWith("Two files for the font IBM Plex Sans weight 600: otf"), e.getMessage());
    }

    @Test
    void verticalMetricsFromHhea() {
        FontFile f = new Resources().fonts().find(plex(FontWeight.NORMAL, FontStyle.NORMAL));
        assertEquals(1.025, f.vertical().ascent(), 1e-9);
        assertEquals(0.275, f.vertical().descent(), 1e-9);
    }

    @Test
    void decorationMetricsFromPostAndOs2() {
        // post: underlinePosition −95 and underlineThickness 60; OS/2: yStrikeoutPosition 309 and yStrikeoutSize 60.
        FontFile f = new Resources().fonts().find(plex(FontWeight.NORMAL, FontStyle.NORMAL));
        assertEquals(-0.095, f.decoration().underlinePosition(), 1e-9);
        assertEquals(0.06, f.decoration().underlineThickness(), 1e-9);
        assertEquals(0.309, f.decoration().strikeoutPosition(), 1e-9);
        assertEquals(0.06, f.decoration().strikeoutThickness(), 1e-9);
        // The bold has a thicker line.
        assertEquals(0.1, new Resources().fonts().find(plex(FontWeight.BOLD, FontStyle.NORMAL)).decoration()
                .underlineThickness(), 1e-9);
    }

    /**
     * The file is read once, when the folder is indexed: the content and the hash are those of that moment even if the
     * file changes or disappears.
     */
    @Test
    void theContentIsTheOneIndexed() throws Exception {
        FontFile font = copy("Plex", "Regular").fonts().find(plex(FontWeight.NORMAL, FontStyle.NORMAL));
        byte[] original = Files.readAllBytes(PLEX.resolve("IBMPlexSans-Regular.ttf"));
        Files.copy(PLEX.resolve("IBMPlexSans-Bold.ttf"), font.path(), StandardCopyOption.REPLACE_EXISTING);
        assertArrayEquals(original, font.content().readAllBytes());
        Files.delete(font.path());
        assertArrayEquals(original, font.content().readAllBytes());
        assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(original)), font.digest());
    }

    @Test
    void missingVariant() throws IOException {
        Resources r = copy("plex", "Regular", "Bold");
        ResourceException e = assertThrows(ResourceException.class,
                () -> r.fonts().find(plex(FontWeight.BOLD, FontStyle.ITALIC)));
        assertEquals("The font IBM Plex Sans has no variant bold italic. Variants in "
                + r.fontsFolder() + ": normal, bold", e.getMessage());
    }

    /** The variants of the message go from the lightest to the heaviest, the normal style before the italic. */
    @Test
    void missingWeightListsTheVariantsByWeight() throws IOException {
        Resources r = copy("plex", "Bold", "Italic", "SemiBold", "Regular", "Light");
        ResourceException e = assertThrows(ResourceException.class,
                () -> r.fonts().find(plex(new FontWeight(900), FontStyle.NORMAL)));
        assertEquals("The font IBM Plex Sans has no variant weight 900. Variants in "
                + r.fontsFolder() + ": weight 300, normal, italic, weight 600, bold", e.getMessage());
    }

    /**
     * The weight of a file is its {@code usWeightClass} as it is: one of 450 is indexed but cannot be requested, and
     * one outside 1 to 1000 is not indexed, although its family is known.
     */
    @Test
    void weightOfTheFileAsItIs() throws IOException {
        Resources r = copy("plex", "Regular", "Medium");
        setWeightClass(r.fontsFolder().resolve("plex/Regular.ttf"), 0);
        setWeightClass(r.fontsFolder().resolve("plex/Medium.ttf"), 450);
        ResourceException e = assertThrows(ResourceException.class,
                () -> r.fonts().find(plex(FontWeight.NORMAL, FontStyle.NORMAL)));
        assertEquals("The font IBM Plex Sans has no variant normal. Variants in "
                + r.fontsFolder() + ": weight 450", e.getMessage());
    }

    @Test
    void unknownFamily() throws IOException {
        Resources r = copy("plex", "Regular");
        ResourceException e = assertThrows(ResourceException.class, () -> r.fonts().find(
                new FontFace("Helvetica", FontWeight.NORMAL, FontStyle.NORMAL)));
        assertEquals("Unknown font: Helvetica. Families in " + r.fontsFolder() + ": IBM Plex Sans",
                e.getMessage());
    }

    /** The families of the message are in alphabetical order, so the message is the same on every run. */
    @Test
    void unknownFamilyListsTheFamiliesInOrder() {
        Resources r = new Resources();
        ResourceException e = assertThrows(ResourceException.class, () -> r.fonts().find(
                new FontFace("Arial", FontWeight.NORMAL, FontStyle.NORMAL)));
        assertEquals("Unknown font: Arial. Families in " + r.fontsFolder()
                + ": IBM Plex Mono, IBM Plex Sans, IBM Plex Serif", e.getMessage());
    }

    @Test
    void twoFilesForTheSameFont() throws IOException {
        Resources r = copy("a", "Regular");
        Files.copy(PLEX.resolve("IBMPlexSans-Regular.ttf"), r.fontsFolder().resolve("b.ttf"));
        ResourceException e = assertThrows(ResourceException.class, r::fonts);
        assertTrue(e.getMessage().startsWith("Two files for the font IBM Plex Sans normal"), e.getMessage());
    }

    @Test
    void unreadableFile() throws IOException {
        Resources r = copy("plex", "Regular");
        Files.writeString(r.fontsFolder().resolve("broken.ttf"), "no es una fuente");
        ResourceException e = assertThrows(ResourceException.class, r::fonts);
        assertTrue(e.getMessage().startsWith("Cannot read the font"), e.getMessage());
    }

    @Test
    void missingFolder() {
        ResourceException e = assertThrows(ResourceException.class, () -> new Resources(dir).fonts());
        assertEquals("The fonts folder " + dir.resolve("fonts") + " does not exist", e.getMessage());
    }
}
