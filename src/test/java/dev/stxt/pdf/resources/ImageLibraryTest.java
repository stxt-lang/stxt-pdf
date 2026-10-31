package dev.stxt.pdf.resources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
