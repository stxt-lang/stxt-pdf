package dev.stxt.pdf.golden;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Las imágenes de los tests de referencia se comparan con las versionadas y fallan si cambian. */
class ReferenceFilesTest {

    private static final String UPDATE = "reference-files-test.update";

    @TempDir
    Path dir;

    @AfterEach
    void noUpdate() {
        System.clearProperty(UPDATE);
    }

    private static BufferedImage image(int rgb) {
        BufferedImage image = new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 10; y++) {
                image.setRGB(x, y, rgb);
            }
        }
        return image;
    }

    private Path versioned(BufferedImage... pages) throws IOException {
        Path reference = Files.createDirectories(dir.resolve("reference"));
        for (int i = 0; i < pages.length; i++) {
            ImageIO.write(pages[i], "png", reference.resolve("page-" + (i + 1) + ".png").toFile());
        }
        return reference;
    }

    private String failure(Path reference, BufferedImage... pages) {
        return assertThrows(AssertionError.class,
                () -> ReferenceFiles.checkImages(reference, dir.resolve("actual"), List.of(pages), UPDATE)).getMessage();
    }

    @Test
    void theSameImagesPassAndTheAntialiasingIsTolerated() throws IOException {
        Path reference = versioned(image(0xffffff), image(0x000000));
        assertDoesNotThrow(() -> ReferenceFiles.checkImages(reference, dir, List.of(image(0xffffff),
                image(ReferenceFiles.PIXEL_TOLERANCE << 16)), UPDATE));
    }

    @Test
    void aChangedMissingOrExtraPageFailsAndSaysHowToAcceptIt() throws IOException {
        Path reference = versioned(image(0xffffff), image(0xffffff));
        String changed = failure(reference, image(0xffffff), image(Color.RED.getRGB()));
        assertTrue(changed.contains("page-2.png cambia en 200 píxeles"), changed);
        assertTrue(changed.contains("-D" + UPDATE + "=true") && changed.contains(dir.resolve("actual").toString()),
                changed);
        assertTrue(failure(reference, image(0xffffff), image(0xffffff), image(0xffffff))
                .contains("page-3.png no está versionada"));
        assertTrue(failure(reference, image(0xffffff)).contains("page-2.png sobra: el PDF tiene 1 páginas"));
        assertTrue(failure(reference, image(0xffffff), new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB))
                .contains("page-2.png cambia de tamaño"));
    }

    @Test
    void updatingRewritesOnlyWhatChanges() throws IOException {
        Path reference = versioned(image(0xffffff), image(0xffffff), image(0xffffff));
        Path first = reference.resolve("page-1.png");
        long before = Files.getLastModifiedTime(first).toMillis();
        System.setProperty(UPDATE, "true");
        ReferenceFiles.checkImages(reference, dir, List.of(image(0xfffffe), image(Color.RED.getRGB())), UPDATE);
        assertEquals(before, Files.getLastModifiedTime(first).toMillis(), "una diferencia de antialiasing no se escribe");
        assertEquals(Color.RED.getRGB(), ImageIO.read(reference.resolve("page-2.png").toFile()).getRGB(0, 0));
        assertFalse(Files.exists(reference.resolve("page-3.png")));
    }
}
