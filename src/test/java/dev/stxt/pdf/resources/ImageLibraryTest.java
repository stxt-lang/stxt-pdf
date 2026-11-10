package dev.stxt.pdf.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The test images of {@code resources/images/test/} (reference/components/image.md). */
class ImageLibraryTest {

    private final ImageLibrary images = new Resources().images();

    @TempDir
    Path dir;

    @Test
    void readsPngSize() {
        ImageFile f = images.find("test/wide.png");
        assertEquals(ImageFile.Format.PNG, f.format());
        assertEquals(200, f.pixelWidth());
        assertEquals(100, f.pixelHeight());
        // 96 pixels per inch: 0.75 pt per pixel.
        assertEquals(150, f.width(), 1e-9);
        assertEquals(75, f.height(), 1e-9);
    }

    @Test
    void readsJpegSize() {
        ImageFile f = images.find("test/tall.jpg");
        assertEquals(ImageFile.Format.JPEG, f.format());
        assertEquals(90, f.width(), 1e-9);
        assertEquals(120, f.height(), 1e-9);
    }

    @Test
    void readsEachFileOnce() {
        assertSame(images.find("test/square.png"), images.find("test/square.png"));
    }

    /**
     * Two spellings of the same file are one image, read once; and a document with many spellings of the same files
     * cannot make the library grow without bound (it lives as long as its {@link Resources}: a whole service).
     */
    @Test
    void theSpellingsOfAFileAreOneImage() {
        ImageLibrary library = new Resources().images();
        ImageFile image = library.find("test/square.png");
        for (String spelling : new String[]{"./test/square.png", "././test/square.png", ".//test/square.png",
                "test\\square.png", "test/./square.png"}) {
            assertSame(image, library.find(spelling), spelling);
        }
        if (Files.exists(Path.of("resources/images/TEST/SQUARE.PNG"))) {
            // A file system that does not tell capitals from small letters, as on Windows and macOS.
            assertSame(image, library.find("TEST/SQUARE.PNG"));
        }
        assertEquals(1, library.files());
        // Enough new spellings to reach the limit once, all short: each one costs a toRealPath, slow on Windows, and
        // there normalizing a long path costs more than the file system.
        for (int i = 0; i < ImageLibrary.MAX_SPELLINGS + 2; i++) {
            library.find(spelling(i) + "test/square.png");
        }
        assertEquals(1, library.files());
        assertTrue(library.spellings() <= ImageLibrary.MAX_SPELLINGS, "spellings: " + library.spellings());
    }

    /** A different spelling of the current folder for each {@code i} below 2048: one {@code ./} or {@code .//} per bit. */
    private static String spelling(int i) {
        StringBuilder spelling = new StringBuilder();
        for (int bit = 0; bit < 11; bit++) {
            spelling.append((i >> bit & 1) == 0 ? "./" : ".//");
        }
        return spelling.toString();
    }

    @Test
    void recognizesTheFormatByContent() {
        ImageFormatException gif = assertThrows(ImageFormatException.class, () -> images.find("test/drawing.gif"));
        assertTrue(gif.getMessage().contains("is not PNG, JPEG, SVG or PDF (it is GIF)"), gif.getMessage());
        ImageFormatException text = assertThrows(ImageFormatException.class, () -> images.find("test/broken.png"));
        assertTrue(text.getMessage().contains("test/broken.png is not PNG, JPEG, SVG or PDF"), text.getMessage());
    }

    @Test
    void damagedImage() throws IOException {
        Path root = Files.createDirectories(dir.resolve("resources/images"));
        byte[] png = Files.readAllBytes(Path.of("resources/images/test/wide.png"));
        Files.write(root.resolve("cut.png"), java.util.Arrays.copyOf(png, 10));
        ImageFormatException e = assertThrows(ImageFormatException.class,
                () -> new Resources(dir.resolve("resources")).images().find("cut.png"));
        assertTrue(e.getMessage().contains("looks like PNG but is damaged"), e.getMessage());
    }

    @Test
    void pathsAreRelativeToTheImagesFolder() {
        ResourceException missing = assertThrows(ResourceException.class, () -> images.find("test/missing.png"));
        assertFalse(missing instanceof ImageFormatException);
        assertTrue(missing.getMessage().startsWith("The resource test/missing.png does not exist"), missing.getMessage());
        // It cannot leave images/, neither towards the resources nor with an absolute path.
        for (String path : new String[] {"../fonts/IBM Plex Sans/IBMPlexSans-Regular.ttf", "/test/wide.png",
                "images/test/wide.png"}) {
            assertInstanceOf(ResourceException.class, assertThrows(RuntimeException.class, () -> images.find(path)),
                    path);
        }
    }

    @Test
    void linksMustStayInsideTheImagesFolder() throws IOException {
        Path root = Files.createDirectories(dir.resolve("resources"));
        Files.createDirectories(root.resolve("images"));
        Files.copy(Path.of("resources/images/test/square.png"), root.resolve("logo.png"));
        Path link = root.resolve("images/logo.png");
        try {
            Files.createSymbolicLink(link, root.resolve("logo.png"));
        } catch (UnsupportedOperationException | IOException e) {
            assumeTrue(false, "No symbolic links: " + e.getMessage());
        }
        ResourceException e = assertThrows(ResourceException.class, () -> new Resources(root).images().find("logo.png"));
        assertTrue(e.getMessage().contains("is a link that leaves the folder images"), e.getMessage());
    }
}
