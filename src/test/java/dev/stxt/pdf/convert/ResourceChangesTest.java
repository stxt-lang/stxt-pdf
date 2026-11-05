package dev.stxt.pdf.convert;

import dev.stxt.InlineNode;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.model.Check;
import dev.stxt.pdf.model.Limits;
import dev.stxt.pdf.model.StxtPdfException;
import dev.stxt.pdf.model.StxtPdfException.Kind;
import dev.stxt.pdf.resources.ImageFile;
import dev.stxt.pdf.resources.ResourceException;
import dev.stxt.pdf.resources.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Files of the resources folder that change while a {@link Converter} lives (reference/implementation/api.md): a
 * converter keeps what it read and {@link Converter#reloaded()} sees the changes, or, built with
 * {@link Converter.Builder#reloadResourcesEachConversion}, each conversion sees the folder as it is when it starts; a
 * document already laid out is painted with what it was laid out with, or not painted.
 */
class ResourceChangesTest {

    private static final String HEADER = Docs.header(200, 200);
    private static final String BASE = Docs.STYLES;
    private static final Path FONTS = Resources.DEFAULT_ROOT.resolve("fonts");
    private static final Path IMAGES = Resources.DEFAULT_ROOT.resolve("images/test");
    private static final String SERIF = "IBM Plex Serif/IBMPlexSerif-Regular.ttf";

    private static InlineNode image(String path) {
        return Docs.read(HEADER, "\tImage: " + path, "\t\tAlt: Una imagen", "\t\tBox:", "\t\t\tWidth: 40");
    }

    /** A resources folder in {@code dir} with IBM Plex Sans and, in {@code images/}, those images of the tests. */
    private static Resources folder(Path dir, String... images) throws IOException {
        copyFont(dir, "IBM Plex Sans/IBMPlexSans-Regular.ttf");
        Path folder = Files.createDirectories(dir.resolve("images"));
        for (String image : images) {
            Files.copy(IMAGES.resolve(image), folder.resolve(image));
        }
        return new Resources(dir);
    }

    private static void copyFont(Path dir, String font) throws IOException {
        Path target = dir.resolve("fonts").resolve(font);
        Files.createDirectories(target.getParent());
        Files.copy(FONTS.resolve(font), target);
    }

    /** A converter that keeps what it reads until {@link Converter#reloaded()}, the default. */
    private static Converter converter(Resources resources) {
        return Converter.builder().resources(resources).styles(BASE).build();
    }

    /** A converter whose conversions see the folder as it is when they start. */
    private static Converter live(Resources resources) {
        return Converter.builder().resources(resources).styles(BASE).reloadResourcesEachConversion(true).build();
    }

    /** The converter that sees the files as they are now: the reloaded one, or the same if it reloads each time. */
    private static Converter next(Converter converter) {
        return converter.reloadsResourcesEachConversion() ? converter : converter.reloaded();
    }

    private static void replace(Path dir, String image, String with) throws IOException {
        Files.copy(IMAGES.resolve(with), dir.resolve("images").resolve(image), StandardCopyOption.REPLACE_EXISTING);
    }

    private static byte[] pdf(Converter converter, LaidOutDocument laidOut) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        converter.toPdf(laidOut, out);
        return out.toByteArray();
    }

    /** The two ways of painting a document already laid out. */
    private static List<Executable> paintings(Converter converter, LaidOutDocument laidOut) {
        return List.of(() -> converter.toPdf(laidOut, new ByteArrayOutputStream()), () -> converter.toSvg(laidOut));
    }

    /** The image is read again when painting: a file that is gone by then is the resources folder's, with its path. */
    @Test
    void anImageRemovedAfterLayingOutIsAResourceProblem(@TempDir Path dir) throws IOException {
        for (String name : new String[] {"square.png", "tall.jpg", "pages.pdf"}) {
            Converter converter = converter(folder(dir.resolve(name), name));
            LaidOutDocument laidOut = converter.layOut(image(name));
            Files.delete(dir.resolve(name).resolve("images").resolve(name));
            for (Executable paint : paintings(converter, laidOut)) {
                StxtPdfException e = assertThrows(StxtPdfException.class, paint);
                assertEquals(ResourceException.class, e.getClass(), name + ": " + e);
                assertEquals("RESOURCE", e.code());
                assertEquals(StxtPdfException.NO_LINE, e.line());
                assertTrue(e.getMessage().startsWith("Cannot read the image " + name + ": "), e.getMessage());
            }
        }
    }

    private static void assertChanged(String name, Executable paint) {
        StxtPdfException e = assertThrows(StxtPdfException.class, paint);
        assertEquals("IMAGE_FORMAT", e.code(), name + ": " + e);
        assertEquals(Kind.RESOURCE, e.kind());
        assertTrue(e.getMessage().startsWith("The image " + name + " changed after it was read"), e.getMessage());
        assertTrue(e.getMessage().endsWith("(Converter.reloaded())"), e.getMessage());
    }

    /**
     * A PNG, JPEG or PDF replaced between laying out and painting would be laid out with one size and painted with
     * another: it is an error. The default converter keeps laying out with the old file, and failing, until it is
     * reloaded; the reloaded one, or the next conversion of one that reloads each time, reads the new file.
     */
    @Test
    void anImageReplacedAfterLayingOutIsAnErrorUntilItIsReadAgain(@TempDir Path dir) throws IOException {
        String[][] cases = {{"square.png", "wide.png"}, {"tall.jpg", "square.png"}, {"pages.pdf", "layers.pdf"}};
        for (boolean eachConversion : new boolean[] {false, true}) {
            for (String[] replaced : cases) {
                String name = replaced[0];
                Path folder = dir.resolve(eachConversion + "-" + name);
                Resources resources = folder(folder, name);
                Converter converter = eachConversion ? live(resources) : converter(resources);
                LaidOutDocument laidOut = converter.layOut(image(name));
                ImageFile before = laidOut.resources().images().find(name);
                replace(folder, name, replaced[1]);
                for (Executable paint : paintings(converter, laidOut)) {
                    assertChanged(name, paint);
                }
                if (!eachConversion) {
                    LaidOutDocument stale = converter.layOut(image(name));
                    assertSame(before, stale.resources().images().find(name), name);
                    assertChanged(name, () -> converter.toPdf(stale, new ByteArrayOutputStream()));
                }
                Converter next = next(converter);
                LaidOutDocument again = next.layOut(image(name));
                ImageFile after = again.resources().images().find(name);
                assertNotSame(before, after, name);
                assertEquals(IMAGES.resolve(replaced[1]).toFile().length(), Files.size(after.path()), name);
                assertTrue(pdf(next, again).length > 0);
            }
        }
        // The size of the new file is the one the new conversion lays out.
        Converter converter = converter(new Resources(dir.resolve("false-square.png")));
        ImageFile wide = converter.layOut(image("square.png")).resources().images().find("square.png");
        assertNotEquals(wide.width(), wide.height());
    }

    /**
     * An SVG is painted with the bytes that were checked and measured when it was read, whatever the file has now; the
     * default converter keeps laying it out as it was read, and the reloaded one reads the new one, as does the next
     * conversion of one that reloads each time.
     */
    @Test
    void anSvgReplacedAfterLayingOutIsPaintedAsItWasRead(@TempDir Path dir) throws IOException {
        for (boolean eachConversion : new boolean[] {false, true}) {
            Path folder = dir.resolve(String.valueOf(eachConversion));
            Resources resources = folder(folder, "shapes.svg");
            Converter converter = eachConversion ? live(resources) : converter(resources);
            LaidOutDocument laidOut = converter.layOut(image("shapes.svg"));
            byte[] before = pdf(converter, laidOut);
            List<String> svgBefore = converter.toSvg(laidOut);
            Path file = folder.resolve("images/shapes.svg");
            Files.writeString(file, "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"30\" height=\"10\">"
                    + "<rect width=\"30\" height=\"10\" fill=\"#123456\"/></svg>");
            assertArrayEquals(before, pdf(converter, laidOut));
            assertEquals(svgBefore, converter.toSvg(laidOut));
            if (!eachConversion) {
                assertArrayEquals(before, pdf(converter, converter.layOut(image("shapes.svg"))));
            }
            Converter next = next(converter);
            LaidOutDocument again = next.layOut(image("shapes.svg"));
            assertEquals(30 * ImageFile.POINTS_PER_PIXEL, again.resources().images().find("shapes.svg").width(),
                    1e-9);
            assertFalse(java.util.Arrays.equals(before, pdf(next, again)));
            Files.delete(file);
            assertArrayEquals(before, pdf(converter, laidOut));
        }
    }

    /**
     * A font added to {@code fonts/} is seen by the reloaded converter, or by the next conversion of one that reloads
     * each time, and one removed stops being there; the default converter keeps the fonts it read.
     */
    @Test
    void theFontsAddedAndRemovedAreSeenWhenTheFolderIsReadAgain(@TempDir Path dir) throws IOException {
        InlineNode serif = Docs.read(HEADER, "\tText: Hola", "\t\tTypography:", "\t\t\tFont: IBM Plex Serif");
        for (boolean eachConversion : new boolean[] {false, true}) {
            Path folder = dir.resolve(String.valueOf(eachConversion));
            Resources resources = folder(folder);
            Converter converter = eachConversion ? live(resources) : converter(resources);
            assertEquals("FONT_UNKNOWN", assertThrows(StxtPdfException.class, () -> converter.layOut(serif)).code());
            copyFont(folder, SERIF);
            if (!eachConversion) {
                assertEquals("FONT_UNKNOWN",
                        assertThrows(StxtPdfException.class, () -> converter.layOut(serif)).code());
            }
            Converter withSerif = next(converter);
            LaidOutDocument laidOut = withSerif.layOut(serif);
            byte[] painted = pdf(withSerif, laidOut);
            Files.delete(folder.resolve("fonts").resolve(SERIF));
            if (!eachConversion) {
                // The fonts are in memory: the converter that read them goes on laying out with them.
                assertArrayEquals(painted, pdf(withSerif, withSerif.layOut(serif)));
            }
            Converter without = next(withSerif);
            assertEquals("FONT_UNKNOWN", assertThrows(StxtPdfException.class, () -> without.layOut(serif)).code());
            // What was laid out keeps the fonts it was measured with.
            assertArrayEquals(painted, pdf(without, laidOut));
        }
    }

    /** By default every conversion of a converter works with the same resources. */
    @Test
    void theDefaultConverterConvertsWithTheSameResources(@TempDir Path dir) throws IOException {
        Converter converter = converter(folder(dir, "square.png"));
        assertFalse(converter.reloadsResourcesEachConversion());
        assertSame(converter.layOut(image("square.png")).resources(), converter.layOut(image("square.png")).resources());
        assertTrue(live(converter.resources()).reloadsResourcesEachConversion());
    }

    /** What has not changed is not read again: the conversions share the fonts and the images already read. */
    @Test
    void whatHasNotChangedIsNotReadAgain(@TempDir Path dir) throws IOException {
        for (boolean eachConversion : new boolean[] {false, true}) {
            Resources resources = folder(dir.resolve(String.valueOf(eachConversion)), "square.png");
            Converter converter = eachConversion ? live(resources) : converter(resources);
            LaidOutDocument first = converter.layOut(image("square.png"));
            LaidOutDocument second = next(converter).layOut(image("square.png"));
            assertNotSame(first.resources(), second.resources());
            assertSame(first.resources().fonts(), second.resources().fonts());
            assertSame(first.resources().images().find("square.png"),
                    second.resources().images().find("square.png"));
        }
    }

    /** {@link Converter#reloaded()} gives another converter with the same configuration; the first does not change. */
    @Test
    void theReloadedConverterKeepsTheConfiguration() {
        for (boolean eachConversion : new boolean[] {false, true}) {
            Converter converter = Converter.builder().resources(new Resources(Path.of("other"))).pdfUa(2)
                    .limits(Limits.DEFAULT.withMaxPages(3)).exclude(Check.CONTRAST)
                    .reloadResourcesEachConversion(eachConversion).build();
            Converter reloaded = converter.reloaded();
            assertNotSame(converter, reloaded);
            assertSame(converter.resources(), reloaded.resources());
            assertEquals(converter.limits(), reloaded.limits());
            assertEquals(converter.excluded(), reloaded.excluded());
            assertEquals(2, reloaded.pdfUa());
            assertEquals(eachConversion, reloaded.reloadsResourcesEachConversion());
        }
    }

    /** A snapshot keeps what it found while it lives, so that a conversion does not mix two versions of a file. */
    @Test
    void aSnapshotKeepsWhatItFound(@TempDir Path dir) throws IOException {
        Resources resources = folder(dir, "square.png");
        Resources snapshot = resources.snapshot();
        ImageFile square = snapshot.images().find("square.png");
        assertFalse(snapshot.fonts().families().contains("IBM Plex Serif"), () -> "families: " + snapshot.fonts().families());
        replace(dir, "square.png", "wide.png");
        copyFont(dir, SERIF);
        assertSame(square, snapshot.images().find("./square.png"));
        assertFalse(snapshot.fonts().families().contains("IBM Plex Serif"), () -> "families: " + snapshot.fonts().families());
        Resources next = resources.snapshot();
        assertNotSame(square, next.images().find("square.png"));
        assertTrue(next.fonts().families().contains("IBM Plex Serif"), () -> "families: " + next.fonts().families());
    }

    /**
     * A snapshot that reads a file again does not change what the {@code Resources} it comes from found: each one
     * keeps its own images.
     */
    @Test
    void aSnapshotDoesNotChangeWhatTheOthersFound(@TempDir Path dir) throws IOException {
        Resources resources = folder(dir, "square.png");
        ImageFile square = resources.images().find("square.png");
        replace(dir, "square.png", "wide.png");
        ImageFile wide = resources.snapshot().images().find("square.png");
        assertNotSame(square, wide);
        // Another spelling, so that it does not come from the shortcut of the spellings already seen.
        assertSame(square, resources.images().find("./square.png"));
    }

    /** A {@code Resources} created with a constructor reads each file once and does not look at the folder again. */
    @Test
    void aResourcesWithoutSnapshotDoesNotLookAgain(@TempDir Path dir) throws IOException {
        Resources resources = folder(dir, "square.png");
        ImageFile square = resources.images().find("square.png");
        assertFalse(resources.fonts().families().contains("IBM Plex Serif"), () -> "families: " + resources.fonts().families());
        replace(dir, "square.png", "wide.png");
        copyFont(dir, SERIF);
        assertSame(square, resources.images().find("square.png"));
        assertFalse(resources.fonts().families().contains("IBM Plex Serif"), () -> "families: " + resources.fonts().families());
    }

    /** The text of an SVG was checked with the fonts: when they are scanned again, the SVG is read again too. */
    @Test
    void anSvgWithTextIsReadAgainWhenTheFontsChange(@TempDir Path dir) throws IOException {
        Resources resources = folder(dir);
        Files.writeString(dir.resolve("images/label.svg"), "<svg xmlns=\"http://www.w3.org/2000/svg\" "
                + "viewBox=\"0 0 120 30\"><text x=\"4\" y=\"20\" font-family=\"IBM Plex Sans\" font-size=\"16\">"
                + "Nogal</text></svg>");
        Files.writeString(dir.resolve("images/plain.svg"), "<svg xmlns=\"http://www.w3.org/2000/svg\" "
                + "width=\"30\" height=\"10\"><rect width=\"30\" height=\"10\"/></svg>");
        Resources first = resources.snapshot();
        ImageFile label = first.images().find("label.svg");
        ImageFile plain = first.images().find("plain.svg");
        Resources same = resources.snapshot();
        assertSame(label, same.images().find("label.svg"));
        copyFont(dir, SERIF);
        Resources next = resources.snapshot();
        assertNotSame(label, next.images().find("label.svg"));
        assertSame(plain, next.images().find("plain.svg"));
    }
}
