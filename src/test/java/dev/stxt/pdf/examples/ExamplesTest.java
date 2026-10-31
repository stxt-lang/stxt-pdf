package dev.stxt.pdf.examples;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.pdf.model.Limits;
import dev.stxt.pdf.golden.PdfUa;
import dev.stxt.pdf.golden.ReferenceFiles;
import dev.stxt.pdf.layout.Engine;
import dev.stxt.pdf.paint.ContrastCheck;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.layout.inline.PdfMetrics;
import dev.stxt.pdf.output.StxtDump;
import dev.stxt.pdf.paint.PdfDocumentWriter;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Examples of complete documents ({@code test/examples/NN-name/}). Unlike the golden cases,
 * which check an algorithm against a result calculated by hand, an example is a
 * document like the ones an author would write, laid out with the real fonts and images of
 * {@code resources/} and without debug outlines.
 *
 * <p>Each folder has {@code input.stxt}, which must validate and lay out without errors, and can
 * have {@code styles.stxt}, the separate styles document that is passed along with it
 * (reference/common/styles.md); the dump of the boxes with {@code Id} ({@code expected.stxt},
 * reference/implementation/testing.md), which is compared with a tolerance of 0.01 pt; one image per page ({@code
 * page-N.png}, versioned) and the PDF ({@code output.pdf}, versioned so it can be opened from the repository, but not
 * compared: it carries the date and an identifier that change on every run). The PDF and the new images are
 * written to {@code target/reference/examples/}, and if any image is not the versioned one the example fails.</p>
 *
 * <p>The expected dump is not calculated by hand: it is a snapshot of what the engine gives, which the
 * author reviews by looking at the images. {@code -Dexamples.update=true} accepts the changes (it rewrites the dump,
 * the PDF and the images that change), and a single example can be tested with
 * {@code -Dexamples.only=01-invoice}.</p>
 */
class ExamplesTest {

    private static final Path ROOT = Path.of("test", "examples");
    private static final Path ACTUAL_DIR = ReferenceFiles.ACTUAL_ROOT.resolve("examples");
    private static final float IMAGE_DPI = 110;

    /**
     * PDF/UA-1 rules that the examples still fail and that are not what is checked here; the rest
     * must be met. None at the moment: each example, which declares PDF/UA-1 (it has {@code Title}), complies with it
     * according to veraPDF. A rule is added here only with its reason written next to it.
     */
    private static final Set<String> KNOWN_UA_FAILURES = Set.of();

    private final DocumentReader reader = new DocumentReader();

    @TestFactory
    Stream<DynamicTest> examples() throws IOException {
        String only = System.getProperty("examples.only");
        try (Stream<Path> dirs = Files.list(ROOT)) {
            List<Path> examples = dirs.filter(Files::isDirectory)
                    .filter(d -> only == null || d.getFileName().toString().equals(only))
                    .sorted().toList();
            return examples.stream().map(dir -> DynamicTest.dynamicTest(dir.getFileName().toString(), () -> check(dir)));
        }
    }

    private void check(Path dir) throws IOException {
        InlineNode input = reader.read(dir.resolve("input.stxt"));
        Path stylesFile = dir.resolve("styles.stxt");
        InlineNode styles = Files.exists(stylesFile) ? reader.readStyles(stylesFile) : null;
        Resources resources = new Resources();
        LaidOutDocument document = new Engine(new PdfMetrics(resources), resources).layOutDocument(input, styles);
        ContrastCheck.check(document);
        String dump = StxtDump.dump(document.pages(), false);
        Path actual = ACTUAL_DIR.resolve(dir.getFileName());
        byte[] pdf = pdf(document, resources);
        List<BufferedImage> images = ReferenceFiles.writeActual(actual, pdf, IMAGE_DPI);

        Path expectedFile = dir.resolve("expected.stxt");
        Path pdfFile = dir.resolve("output.pdf");
        if (Boolean.getBoolean("examples.update")) {
            Files.writeString(expectedFile, dump, StandardCharsets.UTF_8);
            Files.write(pdfFile, pdf);
        } else if (!Files.exists(expectedFile) || !Files.exists(pdfFile)) {
            fail(dir + ": expected.stxt or output.pdf is missing; -Dexamples.update=true generates them, and the images have to be reviewed");
        } else {
            Node expected = STXT.rawParser().parseFile(expectedFile.toFile()).get(0);
            ReferenceFiles.compare(expected, STXT.rawParser().parse(dump).get(0), dir.getFileName() + "/");
        }
        ReferenceFiles.checkImages(dir, actual, images, "examples.update");
        accessibility(dir, pdf, 1);
        accessibility(dir, pdf(document, resources, 2), 2);
    }

    private static byte[] pdf(LaidOutDocument document, Resources resources) throws IOException {
        return pdf(document, resources, 1);
    }

    private static byte[] pdf(LaidOutDocument document, Resources resources, int ua) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfDocumentWriter.write(document, resources, Limits.DEFAULT, out, false, ua);
        return out.toByteArray();
    }

    /** The tagged PDF complies with part {@code ua} of PDF/UA except for {@link #KNOWN_UA_FAILURES}. */
    private static void accessibility(Path dir, byte[] pdf, int ua) {
        Map<String, String> unexpected = new LinkedHashMap<>(PdfUa.failedRules(pdf, ua));
        unexpected.keySet().removeAll(KNOWN_UA_FAILURES);
        assertEquals(Map.of(), unexpected, dir.getFileName() + ": PDF/UA-" + ua + " rules failed");
    }
}
