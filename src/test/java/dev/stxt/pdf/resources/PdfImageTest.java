package dev.stxt.pdf.resources;

import dev.stxt.pdf.model.Check;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Los PDF de {@code images/} (reference/components/image.md): el tamaño de cada página, ya recortada y girada, y lo
 * que impide dibujarla. Los ficheros de {@code resources/images/test/} los genera {@code tools/ExamplePdfs.java}.
 */
class PdfImageTest {

    private final ImageLibrary images = new Resources().images();

    @TempDir
    Path dir;

    @Test
    void eachPageHasItsSizeCroppedAndTurned() {
        ImageFile f = images.find("test/pages.pdf");
        assertEquals(ImageFile.Format.PDF, f.format());
        assertTrue(f.isVector());
        assertEquals(0, f.pixels());
        assertEquals(3, f.pages());
        // La 1, de 200 × 100; la 2, recortada a 150 × 50; la 3, de 200 × 100 girada 90°: se ve de 100 × 200.
        assertEquals(200, f.width(1), 1e-9);
        assertEquals(100, f.height(1), 1e-9);
        assertEquals(150, f.width(2), 1e-9);
        assertEquals(50, f.height(2), 1e-9);
        assertEquals(100, f.width(3), 1e-9);
        assertEquals(200, f.height(3), 1e-9);
        // Sin Page, la primera.
        assertEquals(200, f.width(), 1e-9);
        assertEquals(100, f.height(), 1e-9);
        for (int page = 1; page <= 3; page++) {
            assertNull(f.problem(page, Check.PDF_IMAGE_FONTS));
            assertNull(f.problem(page, Check.PDF_IMAGE_ANNOTATIONS));
        }
    }

    @Test
    void theOtherFormatsHaveOnePage() {
        ImageFile png = images.find("test/wide.png");
        assertEquals(1, png.pages());
        assertNull(png.problem(1, Check.PDF_IMAGE_FONTS));
        assertEquals(png.width(), png.width(1), 1e-9);
    }

    @Test
    void aFontThatIsNotEmbeddedCannotBeDrawn() {
        ImageFile f = images.find("test/standard-font.pdf");
        String problem = f.problem(1, Check.PDF_IMAGE_FONTS);
        assertTrue(problem.contains("Helvetica sin incrustar"), problem);
        assertNull(f.problem(1, Check.PDF_IMAGE_ANNOTATIONS));
    }

    /** Sin {@code ToUnicode}, una fuente Type 0 no dice qué carácter es cada glifo. */
    @Test
    void aFontWithoutUnicodeCannotBeDrawn() {
        String problem = images.find("test/no-unicode.pdf").problem(1, Check.PDF_IMAGE_FONTS);
        assertTrue(problem.contains("sin ToUnicode"), problem);
    }

    @Test
    void aVisibleAnnotationCannotBeDrawn() {
        ImageFile f = images.find("test/form-field.pdf");
        String problem = f.problem(1, Check.PDF_IMAGE_ANNOTATIONS);
        assertTrue(problem.contains("Widget"), problem);
        assertNull(f.problem(1, Check.CONTRAST));
    }

    @Test
    void anEncryptedPdfIsAnError() {
        ImageFormatException e = assertThrows(ImageFormatException.class, () -> images.find("test/encrypted.pdf"));
        assertTrue(e.getMessage().contains("test/encrypted.pdf está cifrado"), e.getMessage());
    }

    @Test
    void aDamagedPdfIsAnError() throws IOException {
        Path file = dir.resolve("broken.pdf");
        Files.writeString(file, "%PDF-1.7\nnada más\n", StandardCharsets.ISO_8859_1);
        ImageFormatException e = assertThrows(ImageFormatException.class,
                () -> ImageFile.read(file, "broken.pdf", new Resources()));
        assertTrue(e.getMessage().contains("broken.pdf, que parece PDF pero está dañada"), e.getMessage());
    }
}
