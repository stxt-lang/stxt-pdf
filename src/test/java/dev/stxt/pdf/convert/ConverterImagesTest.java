package dev.stxt.pdf.convert;

import dev.stxt.InlineNode;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.StxtPdfException;
import dev.stxt.pdf.model.StxtPdfException.Kind;
import dev.stxt.pdf.resources.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Conversions with resources that are not a single folder (reference/implementation/api.md): fonts and images given
 * as bytes, folders of images with a prefix, and the images one document brings with it
 * ({@link Converter#withImages}).
 */
class ConverterImagesTest {

    private static final String HEADER = Docs.header(200, 200);
    private static final Path FONTS = Resources.DEFAULT_ROOT.resolve("fonts");
    private static final Path IMAGES = Resources.DEFAULT_ROOT.resolve("images/test");
    private static final Path SANS = FONTS.resolve("IBM Plex Sans/IBMPlexSans-Regular.ttf");

    private static InlineNode document(String... images) {
        String[] lines = new String[1 + images.length * 4];
        lines[0] = "\tText: Hola";
        for (int i = 0; i < images.length; i++) {
            lines[1 + i * 4] = "\tImage: " + images[i];
            lines[2 + i * 4] = "\t\tAlt: Una imagen";
            lines[3 + i * 4] = "\t\tBox:";
            lines[4 + i * 4] = "\t\t\tWidth: 40";
        }
        return Docs.read(HEADER, lines);
    }

    private static Converter converter(Resources resources) {
        return Converter.builder().resources(resources).styles(Docs.STYLES).build();
    }

    private static byte[] pdf(Converter converter, InlineNode document) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        converter.toPdf(document, out);
        return out.toByteArray();
    }

    private static byte[] image(String name) throws IOException {
        return Files.readAllBytes(IMAGES.resolve(name));
    }

    /** The PDF of the document with the resources of a single folder, to compare the others with. */
    private static byte[] reference(String... images) throws IOException {
        return pdf(converter(new Resources()), document(images));
    }

    /**
     * The PDF of {@code document} with a single resources folder, {@code dir}, that has IBM Plex Sans and, at each
     * path of {@code files}, a copy of that image of the tests: two paths, two files, although they are equal.
     */
    private static byte[] reference(Path dir, InlineNode document, Map<String, String> files) throws IOException {
        Path font = dir.resolve("fonts/IBMPlexSans-Regular.ttf");
        Files.createDirectories(font.getParent());
        Files.copy(SANS, font);
        for (Map.Entry<String, String> file : files.entrySet()) {
            Path target = dir.resolve("images").resolve(file.getKey());
            Files.createDirectories(target.getParent());
            Files.copy(IMAGES.resolve(file.getValue()), target);
        }
        return pdf(converter(new Resources(dir)), document);
    }

    @Test
    void resourcesGivenAsBytesGiveTheSamePdfAsTheFiles() throws IOException {
        Resources memory = Resources.builder().font("IBMPlexSans-Regular.ttf", Files.readAllBytes(SANS))
                .image("test/square.png", image("square.png")).image("test/tall.jpg", image("tall.jpg"))
                .image("test/shapes.svg", image("shapes.svg")).image("test/pages.pdf", image("pages.pdf")).build();
        String[] images = {"test/square.png", "test/tall.jpg", "test/shapes.svg", "test/pages.pdf"};
        assertArrayEquals(reference(images), pdf(converter(memory), document(images)));
        // Also read whole when the converter is created.
        Converter preloaded = Converter.builder().resources(memory).styles(Docs.STYLES).preloadResources(true).build();
        assertArrayEquals(reference(images), pdf(preloaded, document(images)));
        assertEquals(1, converter(memory).toSvg(converter(memory).layOut(document(images))).size());
    }

    @Test
    void foldersWithAPrefixGiveTheSamePdfAsASingleFolder() throws IOException {
        // The folder of the test images under the prefix they have in the single folder.
        Resources mounted = Resources.builder().fonts(FONTS).images("test", IMAGES).build();
        String[] images = {"test/square.png", "test/pages.pdf"};
        assertArrayEquals(reference(images), pdf(converter(mounted), document(images)));
        StxtPdfException e = assertThrows(StxtPdfException.class, () -> pdf(converter(mounted), document("square.png")));
        assertEquals("IMAGE_UNKNOWN", e.code());
        assertEquals(Kind.INVALID_DOCUMENT, e.kind());
    }

    @Test
    void aConverterWithImagesHasThemOnlyInItsConversions(@TempDir Path dir) throws IOException {
        Converter shared = converter(new Resources());
        InlineNode document = document("adjunta/foto.png", "test/square.png");
        StxtPdfException e = assertThrows(StxtPdfException.class, () -> pdf(shared, document));
        assertEquals("IMAGE_UNKNOWN", e.code());

        byte[] content = image("wide.png");
        Converter own = shared.withImages(Map.of("adjunta/foto.png", content));
        // The bytes are copied: what happens to the array afterwards does not matter.
        content[0] = 0;
        byte[] pdf = pdf(own, document);
        // The same PDF as with the file in the folder at that path.
        assertArrayEquals(reference("test/wide.png", "test/square.png"), pdf);
        assertArrayEquals(pdf, pdf(own, document));
        // The converter it comes from stays as it was, and so do its resources: nothing is read again.
        assertThrows(StxtPdfException.class, () -> pdf(shared, document));
        assertSame(shared.resources(), own.resources());
        assertSame(shared.layOut(document("test/square.png")).resources().fonts(), own.layOut(document).resources()
                .fonts());
        // It goes before the image of the resources with the same path, and a second one keeps the first.
        Converter both = own.withImages(Map.of("test/square.png", image("wide.png")));
        assertArrayEquals(reference(dir, document, Map.of("adjunta/foto.png", "wide.png", "test/square.png",
                "wide.png")), pdf(both, document));
    }

    @Test
    void anImageOfAConversionThatIsNotAnImageIsAnErrorOfItsDocument() {
        Converter own = converter(new Resources()).withImages(Map.of("foto.png", new byte[] {1, 2, 3}));
        InlineNode document = document("foto.png");
        StxtPdfException e = assertThrows(StxtPdfException.class, () -> pdf(own, document));
        assertEquals("IMAGE_FORMAT", e.code());
        assertEquals(Kind.INVALID_DOCUMENT, e.kind());
        assertTrue(e.line() > 0);
        assertThrows(IllegalArgumentException.class,
                () -> converter(new Resources()).withImages(Map.of("../foto.png", new byte[1])));
    }

    @Test
    void itWorksWithAConverterThatReloadsInEachConversion(@TempDir Path dir) throws IOException {
        Path font = dir.resolve("fonts/IBMPlexSans-Regular.ttf");
        Files.createDirectories(font.getParent());
        Files.copy(SANS, font);
        Path images = Files.createDirectories(dir.resolve("images"));
        Files.copy(IMAGES.resolve("square.png"), images.resolve("a.png"));
        Converter live = Converter.builder().resources(new Resources(dir)).styles(Docs.STYLES)
                .reloadResourcesEachConversion(true).build();
        Converter own = live.withImages(Map.of("b.png", image("wide.png")));
        InlineNode document = document("a.png", "b.png");
        byte[] before = pdf(own, document);
        assertArrayEquals(reference("test/square.png", "test/wide.png"), before);
        // The files of the folder are still seen as they are in each conversion.
        Files.copy(IMAGES.resolve("wide.png"), images.resolve("a.png"),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        assertArrayEquals(reference(dir.resolve("reference"), document, Map.of("a.png", "wide.png", "b.png",
                "wide.png")), pdf(own, document));
    }
}
