package dev.stxt.boxes.convert;

import dev.stxt.InlineNode;
import dev.stxt.boxes.Docs;
import dev.stxt.boxes.model.Limits;
import dev.stxt.boxes.model.StxtPdfException;
import dev.stxt.boxes.model.StxtPdfException.Kind;
import dev.stxt.boxes.resources.ImageFile;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.stxt.DocumentReader;
import dev.stxt.boxes.tree.ErrorCode;
import dev.stxt.boxes.tree.InvalidModelException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Los {@link Limits} de una conversión: se aplican justo donde se dicen (un documento
 * que está en el límite pasa, uno por encima falla con {@code LIMIT} y la línea de la {@code Image}) y lo que el
 * motor acepta es lo que se puede dibujar.
 */
class LimitsTest {

    private static final DocumentReader READER = new DocumentReader();
    private static final Resources RESOURCES = new Resources();
    private static final String HEADER = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tTitle: Prueba\n\tMedia:\n\t\tWidth: 200\n\t\tHeight: 200\n";
    private static final String BASE = "Styles (dev.stxt.boxes):\n\tElement: Document\n\t\tTypography:\n\t\t\tFont: IBM Plex Sans\n"
            + "\t\t\tSize: 10\n";

    private static InlineNode document(String... lines) {
        return READER.read(Docs.sectioned(HEADER + String.join("\n", lines) + "\n"));
    }

    private static InlineNode image(String path) {
        return document("\tImage: " + path);
    }

    private static Converter converter(Limits limits) {
        return Converter.builder().styles(BASE).limits(limits).build();
    }

    private static void convert(Converter converter, InlineNode document) throws IOException {
        converter.toPdf(document, new ByteArrayOutputStream());
    }

    @Test
    void theDefaultsAreTheConservativeOnes() {
        Limits d = Limits.DEFAULT;
        assertEquals(2_000, d.maxPages());
        assertEquals(25_000_000L, d.maxImagePixels());
        assertEquals(20_000, d.maxSvgElements());
        assertEquals(64, d.maxSvgDepth());
        assertSame(d, Converter.create().limits());
    }

    @Test
    void everyLimitIsAtLeastOne() {
        assertThrows(IllegalArgumentException.class, () -> Limits.DEFAULT.withMaxPages(0));
        assertThrows(IllegalArgumentException.class, () -> Limits.DEFAULT.withMaxImagePixels(0));
        assertThrows(IllegalArgumentException.class, () -> Limits.DEFAULT.withMaxSvgElements(-1));
        assertThrows(IllegalArgumentException.class, () -> Limits.DEFAULT.withMaxSvgDepth(0));
        assertThrows(NullPointerException.class, () -> Converter.builder().limits(null));
        Limits changed = Limits.DEFAULT.withMaxPages(5).withMaxSvgDepth(9);
        assertEquals(5, changed.maxPages());
        assertEquals(9, changed.maxSvgDepth());
        assertEquals(Limits.DEFAULT.maxImagePixels(), changed.maxImagePixels());
    }

    /** Un bloque de 500 pt en páginas de 200 ocupa tres. */
    @Test
    void thePageLimitIsExactlyTheNumberOfPages() throws IOException {
        InlineNode threePages = document("\tBlock:", "\t\tBox:", "\t\t\tHeight: 500");
        convert(converter(Limits.DEFAULT.withMaxPages(3)), threePages);
        StxtPdfException e = assertThrows(StxtPdfException.class,
                () -> convert(converter(Limits.DEFAULT.withMaxPages(2)), threePages));
        assertEquals(Kind.LIMIT, e.kind());
        assertEquals("PAGE_LIMIT", e.code());
        assertTrue(e.getMessage().contains("más de 2 páginas"), e.getMessage());
    }

    /** El límite cuenta las páginas de todas las secciones: dos de 2 páginas son 4. */
    @Test
    void thePageLimitCountsEverySection() throws IOException {
        InlineNode twoSections = document("\tSection:", "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 300",
                "\tSection:", "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 300");
        convert(converter(Limits.DEFAULT.withMaxPages(4)), twoSections);
        StxtPdfException e = assertThrows(StxtPdfException.class,
                () -> convert(converter(Limits.DEFAULT.withMaxPages(3)), twoSections));
        assertEquals("PAGE_LIMIT", e.code());
    }

    @Test
    void theDefaultPageLimitIsTwoThousand() {
        // 2001 páginas de 200 pt de alto: se rechaza antes de pintar nada.
        InlineNode document = document("\tBlock:", "\t\tBox:", "\t\t\tHeight: " + (200 * 2_001));
        StxtPdfException e = assertThrows(StxtPdfException.class, () -> Converter.create().layOut(document));
        assertEquals("PAGE_LIMIT", e.code());
        assertTrue(e.getMessage().contains("más de 2000 páginas"), e.getMessage());
    }

    /** {@code test/wide.png} mide 200 × 100 píxeles. */
    @Test
    void theImagePixelLimitIsExactlyWidthTimesHeight() throws IOException {
        InlineNode document = image("test/wide.png");
        convert(converter(Limits.DEFAULT.withMaxImagePixels(20_000)), document);
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> convert(converter(Limits.DEFAULT.withMaxImagePixels(19_999)), document));
        assertEquals(ErrorCode.IMAGE_TOO_LARGE, e.errorCode());
        assertEquals(Kind.LIMIT, e.kind());
        assertEquals(8, e.line());
        assertTrue(e.getMessage().contains("200 × 100 píxeles"), e.getMessage());
    }

    /** Se mira en la cabecera: una imagen enorme se rechaza sin decodificarla ni dibujarla. */
    @Test
    void aHugeImageIsRejectedBeforeLayingOut() {
        long start = System.nanoTime();
        StxtPdfException e = assertThrows(StxtPdfException.class, () -> Converter.create().layOut(image("test/limit-huge.png")));
        assertEquals("IMAGE_TOO_LARGE", e.code());
        assertTrue((System.nanoTime() - start) / 1_000_000 < 5_000, "tardó demasiado para mirar una cabecera");
    }

    @Test
    void anSvgIsAcceptedExactlyAtItsElementsAndDepth() throws IOException {
        ImageFile shapes = RESOURCES.images().find("test/shapes.svg");
        InlineNode document = image("test/shapes.svg");
        Limits exact = Limits.DEFAULT.withMaxSvgElements((int) shapes.svgElements()).withMaxSvgDepth(shapes.svgDepth());
        convert(converter(exact), document);
        InvalidModelException elements = assertThrows(InvalidModelException.class,
                () -> convert(converter(exact.withMaxSvgElements((int) shapes.svgElements() - 1)), document));
        assertEquals(ErrorCode.SVG_TOO_COMPLEX, elements.errorCode());
        assertTrue(elements.getMessage().contains("elementos"), elements.getMessage());
        InvalidModelException depth = assertThrows(InvalidModelException.class,
                () -> convert(converter(exact.withMaxSvgDepth(shapes.svgDepth() - 1)), document));
        assertEquals(ErrorCode.SVG_TOO_COMPLEX, depth.errorCode());
        assertTrue(depth.getMessage().contains("niveles"), depth.getMessage());
    }

    /**
     * Lo que se acepta se dibuja: JSVG trae sus propios límites (2000 elementos y 30 niveles) y el motor los iguala
     * a los suyos, así que un SVG justo en el límite no falla al pintar con un error genérico.
     */
    @Test
    void anSvgAtItsLimitsIsDrawnByJsvg() throws IOException {
        for (String name : new String[]{"test/limit-depth.svg", "test/limit-uses.svg"}) {
            ImageFile svg = RESOURCES.images().find(name);
            Limits exact = Limits.DEFAULT.withMaxSvgElements((int) svg.svgElements()).withMaxSvgDepth(svg.svgDepth());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            converter(exact).toPdf(image(name), out);
            assertTrue(out.size() > 500, name);
        }
    }

    /** Antes un SVG de 3000 rectángulos pasaba la validación y fallaba al dibujar con una IOException. */
    @Test
    void anSvgWithMoreThanJsvgsDefaultIsNowDrawn() throws IOException {
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("limits");
        try {
            java.nio.file.Files.createDirectories(dir.resolve("images"));
            // Una copia y no un enlace simbólico, que en Windows necesita privilegios.
            java.nio.file.Path font = RESOURCES.fontsFolder().resolve("IBM Plex Sans/IBMPlexSans-Regular.ttf");
            java.nio.file.Files.createDirectories(dir.resolve("fonts"));
            java.nio.file.Files.copy(font, dir.resolve("fonts").resolve(font.getFileName()));
            StringBuilder svg = new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\">");
            for (int i = 0; i < 3_000; i++) {
                svg.append("<rect x=\"").append(i % 100).append("\" width=\"1\" height=\"1\"/>");
            }
            java.nio.file.Files.writeString(dir.resolve("images").resolve("many.svg"), svg.append("</svg>").toString());
            Converter converter = Converter.builder().resources(new Resources(dir)).styles(BASE).build();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            converter.toPdf(image("many.svg"), out);
            assertTrue(out.size() > 500);
        } finally {
            try (var files = java.nio.file.Files.walk(dir)) {
                files.sorted(java.util.Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
            }
        }
    }

    @Test
    void theImageMessageGivesTheMegapixels() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> convert(converter(Limits.DEFAULT.withMaxImagePixels(1_000)), image("test/wide.png")));
        assertTrue(e.getMessage().contains("(0.0 Mpx) y el máximo es 0.0 Mpx"), e.getMessage());
    }
}
