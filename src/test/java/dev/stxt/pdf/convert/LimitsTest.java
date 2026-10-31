package dev.stxt.pdf.convert;

import dev.stxt.InlineNode;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.Limits;
import dev.stxt.pdf.model.StxtPdfException;
import dev.stxt.pdf.model.StxtPdfException.Kind;
import dev.stxt.pdf.resources.ImageFile;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.stxt.DocumentReader;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@link Limits} of a conversion: they apply exactly where they say (a document
 * at the limit passes, one above it fails with {@code LIMIT} and the line of the {@code Image}) and what the
 * engine accepts is what can be drawn.
 */
class LimitsTest {

    private static final DocumentReader READER = new DocumentReader();
    private static final Resources RESOURCES = new Resources();
    private static final String HEADER = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 200\n\t\tHeight: 200\n";
    private static final String BASE = "Styles (dev.stxt.pdf):\n\tFor element: Document\n\t\tTypography:\n\t\t\tFont: IBM Plex Sans\n"
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
        assertEquals(20_000_000L, d.maxPdfBytes());
        assertSame(d, Converter.create().limits());
    }

    @Test
    void everyLimitIsAtLeastOne() {
        assertThrows(IllegalArgumentException.class, () -> Limits.DEFAULT.withMaxPages(0));
        assertThrows(IllegalArgumentException.class, () -> Limits.DEFAULT.withMaxImagePixels(0));
        assertThrows(IllegalArgumentException.class, () -> Limits.DEFAULT.withMaxSvgElements(-1));
        assertThrows(IllegalArgumentException.class, () -> Limits.DEFAULT.withMaxSvgDepth(0));
        assertThrows(IllegalArgumentException.class, () -> Limits.DEFAULT.withMaxPdfBytes(0));
        assertThrows(NullPointerException.class, () -> Converter.builder().limits(null));
        Limits changed = Limits.DEFAULT.withMaxPages(5).withMaxSvgDepth(9);
        assertEquals(5, changed.maxPages());
        assertEquals(9, changed.maxSvgDepth());
        assertEquals(Limits.DEFAULT.maxImagePixels(), changed.maxImagePixels());
    }

    /** A 500 pt block on 200 pt pages takes three. */
    @Test
    void thePageLimitIsExactlyTheNumberOfPages() throws IOException {
        InlineNode threePages = document("\tBlock:", "\t\tBox:", "\t\t\tHeight: 500");
        convert(converter(Limits.DEFAULT.withMaxPages(3)), threePages);
        StxtPdfException e = assertThrows(StxtPdfException.class,
                () -> convert(converter(Limits.DEFAULT.withMaxPages(2)), threePages));
        assertEquals(Kind.LIMIT, e.kind());
        assertEquals("PAGE_LIMIT", e.code());
        assertTrue(e.getMessage().contains("more than 2 pages"), e.getMessage());
    }

    /** The limit counts the pages of all the sections: two of 2 pages are 4. */
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
        // 2001 pages 200 pt high: it is rejected before painting anything.
        InlineNode document = document("\tBlock:", "\t\tBox:", "\t\t\tHeight: " + (200 * 2_001));
        StxtPdfException e = assertThrows(StxtPdfException.class, () -> Converter.create().layOut(document));
        assertEquals("PAGE_LIMIT", e.code());
        assertTrue(e.getMessage().contains("more than 2000 pages"), e.getMessage());
    }

    /** {@code test/wide.png} measures 200 × 100 pixels. */
    @Test
    void theImagePixelLimitIsExactlyWidthTimesHeight() throws IOException {
        InlineNode document = image("test/wide.png");
        convert(converter(Limits.DEFAULT.withMaxImagePixels(20_000)), document);
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> convert(converter(Limits.DEFAULT.withMaxImagePixels(19_999)), document));
        assertEquals(ErrorCode.IMAGE_TOO_LARGE, e.errorCode());
        assertEquals(Kind.LIMIT, e.kind());
        assertEquals(8, e.line());
        assertTrue(e.getMessage().contains("200 × 100 pixels"), e.getMessage());
    }

    /** The bytes of a PDF file, before opening it: the limit is exactly its size. */
    @Test
    void thePdfLimitIsExactlyTheBytesOfTheFile() throws IOException {
        long bytes = Files.size(RESOURCES.root().resolve("images/test/pages.pdf"));
        InlineNode document = image("test/pages.pdf");
        convert(converter(Limits.DEFAULT.withMaxPdfBytes(bytes)), document);
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> convert(converter(Limits.DEFAULT.withMaxPdfBytes(bytes - 1)), document));
        assertEquals(ErrorCode.PDF_TOO_LARGE, e.errorCode());
        assertEquals(Kind.LIMIT, e.kind());
        assertEquals(8, e.line());
        assertTrue(e.getMessage().contains("Limits.maxPdfBytes"), e.getMessage());
        // It only counts for PDFs: a PNG larger than the limit is not checked.
        convert(converter(Limits.DEFAULT.withMaxPdfBytes(1)), image("test/wide.png"));
    }

    /** It is checked in the header: a huge image is rejected without decoding or drawing it. */
    @Test
    void aHugeImageIsRejectedBeforeLayingOut() {
        long start = System.nanoTime();
        StxtPdfException e = assertThrows(StxtPdfException.class, () -> Converter.create().layOut(image("test/limit-huge.png")));
        assertEquals("IMAGE_TOO_LARGE", e.code());
        assertTrue((System.nanoTime() - start) / 1_000_000 < 5_000, "took too long to read a header");
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
        assertTrue(elements.getMessage().contains("elements"), elements.getMessage());
        InvalidModelException depth = assertThrows(InvalidModelException.class,
                () -> convert(converter(exact.withMaxSvgDepth(shapes.svgDepth() - 1)), document));
        assertEquals(ErrorCode.SVG_TOO_COMPLEX, depth.errorCode());
        assertTrue(depth.getMessage().contains("levels"), depth.getMessage());
    }

    /**
     * What is accepted is drawn: JSVG has its own limits (2000 elements and 30 levels) and the engine raises them to
     * its own, so an SVG exactly at the limit does not fail with a generic error when painting.
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

    /** An SVG of 3000 rectangles used to pass validation and fail with an IOException when drawn. */
    @Test
    void anSvgWithMoreThanJsvgsDefaultIsNowDrawn() throws IOException {
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("limits");
        try {
            java.nio.file.Files.createDirectories(dir.resolve("images"));
            // A copy and not a symbolic link, which needs privileges on Windows.
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
        assertTrue(e.getMessage().contains("(0.0 Mpx) and the maximum is 0.0 Mpx"), e.getMessage());
    }
}
