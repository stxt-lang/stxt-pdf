package dev.stxt.pdf.convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceCMYK;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.stxt.InlineNode;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.PdfA;
import dev.stxt.pdf.model.StxtPdfException;
import dev.stxt.pdf.model.StxtPdfException.Kind;
import dev.stxt.pdf.resources.Resources;

/**
 * The images a PDF/A cannot carry (reference/common/pdf.md): a JPEG in CMYK is embedded as it is, with the device CMYK
 * color space, which PDF/A only admits with a CMYK output profile. With PDF/A it is an error of the document, with the
 * line of the image; without PDF/A, it is converted as before.
 */
class PdfAImagesTest {

    private static final String HEADER = Docs.header(200, 200);
    private static final Path FONTS = Resources.DEFAULT_ROOT.resolve("fonts");
    private static final Path IMAGES = Resources.DEFAULT_ROOT.resolve("images/test");

    /** A resources folder with IBM Plex Sans, a JPEG in CMYK and one in RGB. */
    private static Resources folder(Path dir) throws IOException {
        Path font = dir.resolve("fonts/IBM Plex Sans/IBMPlexSans-Regular.ttf");
        Files.createDirectories(font.getParent());
        Files.copy(FONTS.resolve("IBM Plex Sans/IBMPlexSans-Regular.ttf"), font);
        Path images = Files.createDirectories(dir.resolve("images"));
        writeCmyk(images.resolve("cmyk.jpg"));
        Files.copy(IMAGES.resolve("tall.jpg"), images.resolve("rgb.jpg"));
        return new Resources(dir);
    }

    /** A JPEG of 8 × 8 pixels with four color components, as the writer of the JDK writes a raster of four bands. */
    private static void writeCmyk(Path file) throws IOException {
        WritableRaster raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 8, 8, 4, null);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                raster.setPixel(x, y, new int[] {40, 200, 120, 10});
            }
        }
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(file.toFile())) {
            writer.setOutput(out);
            writer.write(null, new IIOImage(raster, null, null), null);
        } finally {
            writer.dispose();
        }
    }

    private static Converter converter(Resources resources, PdfA pdfA) {
        Converter.Builder builder = Converter.builder().resources(resources).styles(Docs.STYLES);
        return pdfA == null ? builder.build() : builder.pdfA(pdfA).build();
    }

    private static InlineNode image(String path) {
        return Docs.read(HEADER, "\tText: Una foto", "\tImage: " + path, "\t\tAlt: Una foto", "\t\tBox:",
                "\t\t\tWidth: 40");
    }

    @Test
    void theComponentsOfAJpegAreReadFromItsHeader(@TempDir Path dir) throws IOException {
        Resources resources = folder(dir);
        assertTrue(resources.images().find("cmyk.jpg").cmyk());
        assertFalse(resources.images().find("rgb.jpg").cmyk());
        assertTrue(resources.withImagesInMemory().images().find("cmyk.jpg").cmyk());
        assertFalse(new Resources().images().find("test/square.png").cmyk());
    }

    @Test
    void aJpegInCmykIsAnErrorOfTheDocumentWithPdfA(@TempDir Path dir) throws IOException {
        Resources resources = folder(dir);
        InlineNode document = image("cmyk.jpg");
        for (PdfA pdfA : PdfA.values()) {
            StxtPdfException e = assertThrows(StxtPdfException.class, () -> converter(resources, pdfA).layOut(document));
            assertEquals(Kind.INVALID_DOCUMENT, e.kind());
            assertEquals("IMAGE_FORMAT", e.code());
            // The line of the Image: the header, the section and the text go before it.
            assertEquals(lineOf(document, "Image"), e.line());
            assertTrue(e.getMessage().contains("cmyk.jpg is a JPEG in CMYK") && e.getMessage().contains(pdfA.toString()),
                    e.getMessage());
        }
        // A JPEG in RGB goes in a PDF/A, as before.
        converter(resources, PdfA.A3A).toPdf(image("rgb.jpg"), new ByteArrayOutputStream());
    }

    @Test
    void aJpegInCmykInsideATextOrAsAMarkerIsAnErrorTooWithPdfA(@TempDir Path dir) throws IOException {
        Converter converter = converter(folder(dir), PdfA.A2B);
        InlineNode inText = Docs.read(HEADER, "\tText: Una foto ![foto](cmyk.jpg) en el texto",
                "\t\tTypography:", "\t\t\tMarkup: markdown");
        StxtPdfException e = assertThrows(StxtPdfException.class, () -> converter.layOut(inText));
        assertEquals("IMAGE_FORMAT", e.code());
        assertEquals(lineOf(inText, "Text"), e.line());
        InlineNode marker = Docs.read(HEADER, "\tList:", "\t\tMarker: image", "\t\tBox:", "\t\t\tPadding: 0 0 0 20",
                "\t\tList item:",
                "\t\t\tMarker image: cmyk.jpg", "\t\t\tText: Uno");
        e = assertThrows(StxtPdfException.class, () -> converter.layOut(marker));
        assertEquals("IMAGE_FORMAT", e.code());
        assertTrue(e.getMessage().contains("cmyk.jpg"), e.getMessage());
    }

    @Test
    void withoutPdfAAJpegInCmykIsEmbeddedAsItIs(@TempDir Path dir) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        converter(folder(dir), null).toPdf(image("cmyk.jpg"), out);
        try (PDDocument pdf = Loader.loadPDF(out.toByteArray())) {
            var resources = pdf.getPage(0).getResources();
            PDImageXObject image = null;
            for (var name : resources.getXObjectNames()) {
                if (resources.getXObject(name) instanceof PDImageXObject found) {
                    image = found;
                }
            }
            assertSame(PDDeviceCMYK.INSTANCE, image.getColorSpace());
        }
    }

    /** The line of the first node with that name among the descendants of {@code node}. */
    private static int lineOf(InlineNode node, String name) {
        for (dev.stxt.Node child : node.getChildren()) {
            if (child.getName().equals(name)) {
                return child.getLine();
            }
            if (child instanceof InlineNode inline) {
                int line = lineOf(inline, name);
                if (line > 0) {
                    return line;
                }
            }
        }
        return 0;
    }
}
