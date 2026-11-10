package dev.stxt.pdf.golden;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The images and the PDF of the reference tests are compared with the versioned ones and fail if they change; a case
 * can version only its first pages, and a changed PDF leaves the versioned one and its pages in {@code before/}.
 */
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
        assertTrue(changed.contains("page-2.png changes in 200 pixels"), changed);
        assertTrue(changed.contains("-D" + UPDATE + "=true") && changed.contains(dir.resolve("actual").toString()),
                changed);
        assertTrue(failure(reference, image(0xffffff), image(0xffffff), image(0xffffff))
                .contains("page-3.png is not versioned"));
        assertTrue(failure(reference, image(0xffffff)).contains("page-2.png is extra: the PDF has 1 pages"));
        assertTrue(failure(reference, image(0xffffff), new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB))
                .contains("page-2.png changes size"));
    }

    @Test
    void updatingRewritesOnlyWhatChanges() throws IOException {
        Path reference = versioned(image(0xffffff), image(0xffffff), image(0xffffff));
        Path first = reference.resolve("page-1.png");
        long before = Files.getLastModifiedTime(first).toMillis();
        System.setProperty(UPDATE, "true");
        ReferenceFiles.checkImages(reference, dir, List.of(image(0xfffffe), image(Color.RED.getRGB())), UPDATE);
        assertEquals(before, Files.getLastModifiedTime(first).toMillis(), "an antialiasing difference is not written");
        assertEquals(Color.RED.getRGB(), ImageIO.read(reference.resolve("page-2.png").toFile()).getRGB(0, 0));
        assertFalse(Files.exists(reference.resolve("page-3.png")));
    }

    @Test
    void onlyTheFirstPagesAreVersionedWhenSaid() throws IOException {
        Path reference = versioned(image(0xffffff));
        assertDoesNotThrow(() -> ReferenceFiles.checkImages(reference, dir, List.of(image(0xffffff),
                image(Color.RED.getRGB())), UPDATE, 1), "the second page is not versioned, so it is not compared");
        String changed = assertThrows(AssertionError.class, () -> ReferenceFiles.checkImages(reference, dir,
                List.of(image(Color.RED.getRGB()), image(Color.RED.getRGB())), UPDATE, 1)).getMessage();
        assertTrue(changed.contains("page-1.png changes in 200 pixels") && !changed.contains("page-2.png"), changed);

        Path two = versioned(image(0xffffff), image(0xffffff));
        String extra = assertThrows(AssertionError.class, () -> ReferenceFiles.checkImages(two, dir,
                List.of(image(0xffffff), image(0xffffff)), UPDATE, 1)).getMessage();
        assertTrue(extra.contains("page-2.png is extra: only the first page is versioned"), extra);
        System.setProperty(UPDATE, "true");
        ReferenceFiles.checkImages(two, dir, List.of(image(0xffffff), image(0xffffff)), UPDATE, 1);
        assertTrue(Files.exists(two.resolve("page-1.png")));
        assertFalse(Files.exists(two.resolve("page-2.png")), "updating deletes the page that is no longer versioned");
    }

    private static byte[] pdf(int pages) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < pages; i++) {
                document.addPage(new PDPage());
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    @Test
    void aChangedPdfLeavesTheVersionedOneAndItsPagesInBefore() throws IOException {
        Path reference = Files.createDirectories(dir.resolve("reference"));
        Path actual = dir.resolve("actual");
        byte[] versioned = pdf(1);
        Files.write(reference.resolve("output.pdf"), versioned);
        Path before = ReferenceFiles.before(actual);

        assertTrue(ReferenceFiles.checkPdf(reference, actual, versioned, 20, UPDATE));
        assertFalse(Files.exists(before));

        assertFalse(ReferenceFiles.checkPdf(reference, actual, pdf(2), 20, UPDATE));
        assertArrayEquals(versioned, Files.readAllBytes(before.resolve("output.pdf")));
        assertTrue(Files.exists(before.resolve("page-1.png")));
        assertFalse(Files.exists(before.resolve("page-2.png")), "the pages are those of the versioned PDF");
        assertArrayEquals(versioned, Files.readAllBytes(reference.resolve("output.pdf")), "nothing versioned changes");

        assertTrue(ReferenceFiles.checkPdf(reference, actual, versioned, 20, UPDATE));
        assertFalse(Files.exists(before), "the same PDF again deletes the before/ of the previous run");

        System.setProperty(UPDATE, "true");
        byte[] changed = pdf(2);
        assertTrue(ReferenceFiles.checkPdf(reference, actual, changed, 20, UPDATE));
        assertArrayEquals(changed, Files.readAllBytes(reference.resolve("output.pdf")), "updating rewrites it");
        assertFalse(Files.exists(before));
    }
}
