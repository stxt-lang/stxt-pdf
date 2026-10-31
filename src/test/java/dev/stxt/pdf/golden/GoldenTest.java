package dev.stxt.pdf.golden;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.layout.Engine;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.model.Check;
import dev.stxt.pdf.output.StxtDump;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.paint.SvgCanvas;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.stxt.DocumentReader;
import dev.stxt.runtime.STXT;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Reference tests (reference/implementation/testing.md): each folder of {@code golden/} contains
 * {@code input.stxt} and {@code expected.stxt}. Names and values are compared; the numbers of
 * {@code Rect} with a tolerance of 0.01 pt. The input is validated against the schema; the
 * dump has no namespace and is read without validation.
 *
 * <p>Each case also leaves a debug SVG in {@code target/golden-svg/}, and the PDF and one image per page in
 * {@code target/reference/golden/}, which are compared with the {@code page-N.png} versioned in its folder of
 * {@code test/golden/}: if they change, the case fails; {@code -Dgolden.update=true} accepts them (see
 * {@link #writePdfAndImages}). The expected dump, which is calculated by hand, is never updated.</p>
 */
class GoldenTest {

    private static final Path SVG_DIR = Path.of("target", "golden-svg");
    /** Folder of the cases in the source code, with the versioned images. */
    private static final Path SOURCE_DIR = Path.of("test", "golden");
    private static final Path ACTUAL_DIR = ReferenceFiles.ACTUAL_ROOT.resolve("golden");
    private static final float IMAGE_DPI = 100;

    /** Cases that need a pending milestone: they are skipped with the reason. */
    private static final Map<String, String> PENDING = Map.of();

    private final DocumentReader reader = new DocumentReader();

    @TestFactory
    Stream<DynamicTest> cases() throws IOException {
        Path root = Path.of("test", "golden");
        try (Stream<Path> dirs = Files.list(root)) {
            List<Path> cases = dirs.filter(Files::isDirectory).sorted().toList();
            return cases.stream().map(dir -> DynamicTest.dynamicTest(dir.getFileName().toString(), () -> {
                String name = dir.getFileName().toString();
                assumeTrue(!PENDING.containsKey(name), () -> name + ": " + PENDING.get(name));
                InlineNode input = reader.read(dir.resolve("input.stxt"));
                Path stylesFile = dir.resolve("styles.stxt");
                InlineNode styles = Files.exists(stylesFile) ? reader.readStyles(stylesFile) : null;
                Node expected = STXT.rawParser().parseFile(dir.resolve("expected.stxt").toFile()).get(0);
                LaidOutDocument document = new Engine().layOutDocument(input, styles);
                writeSvg(name, document);
                List<BufferedImage> images = writePdfAndImages(ACTUAL_DIR.resolve(name), input, styles);
                String dump = StxtDump.dump(document.pages(), false);
                ReferenceFiles.compare(expected, STXT.rawParser().parse(dump).get(0), name + "/");
                ReferenceFiles.checkImages(SOURCE_DIR.resolve(name), ACTUAL_DIR.resolve(name), images, "golden.update");
            }));
        }
    }

    private static void writeSvg(String name, LaidOutDocument document) throws IOException {
        Files.createDirectories(SVG_DIR);
        for (Page p : document.pages()) {
            String svg = SvgCanvas.page(p, p.pageSetup(), true, new Resources());
            Files.writeString(SVG_DIR.resolve(name + "-" + p.number() + ".svg"), svg, StandardCharsets.UTF_8);
        }
    }

    /**
     * Writes to {@code dir} ({@code target/reference/golden/<case>/}) {@code output.pdf} and one {@code page-N.png} per
     * page ({@link ReferenceFiles#writeActual}), and returns the images to compare them with the versioned ones. The
     * PDF carries the debug outlines of the boxes and is laid out with the real metrics of the fonts of
     * {@code resources/fonts}, which are embedded ({@link Converter}), not with those of the comparison. Without
     * checking contrast: a golden case tests where each thing goes, and its colors only tell the boxes apart.
     */
    private static List<BufferedImage> writePdfAndImages(Path dir, InlineNode input, InlineNode styles)
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.Builder converter = Converter.builder().debug(true).exclude(Check.CONTRAST);
        if (styles != null) {
            converter.styles(styles);
        }
        converter.build().toPdf(input, out);
        return ReferenceFiles.writeActual(dir, out.toByteArray(), IMAGE_DPI);
    }
}
