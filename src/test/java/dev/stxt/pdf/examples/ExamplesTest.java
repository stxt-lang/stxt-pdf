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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Examples of complete documents ({@code test/examples/NN-name/}). Unlike the golden cases,
 * which check an algorithm against a result calculated by hand, an example is a
 * document like the ones an author would write, laid out with the real fonts and images of
 * {@code resources/} and without debug outlines.
 *
 * <p>Each example is written in every language of {@link #LANGUAGES}, one subfolder per language: Spanish
 * ({@code es/}) is the original and English ({@code en/}) its mirror, with the same structure. The folder of the example
 * can have {@code styles.stxt}, the separate styles document that is passed along with the document of every language
 * (reference/common/styles.md). Each language folder has {@code input.stxt}, which must validate and lay out without
 * errors; the dump of the boxes with {@code Id} ({@code expected.stxt},
 * reference/implementation/testing.md), which is compared with a tolerance of 0.01 pt; one image per page ({@code
 * page-N.png}, versioned) and the PDF ({@code output.pdf}, versioned and compared byte for byte: the same document
 * gives the same bytes). The PDF and the new images are written to
 * {@code target/reference/examples/NN-name/<language>/}, and if the PDF or any image is not the versioned one the
 * example fails.</p>
 *
 * <p>The expected dump is not calculated by hand: it is a snapshot of what the engine gives, which the
 * author reviews by looking at the images. {@code -Dexamples.update=true} accepts the changes (it rewrites the dump,
 * the PDF and the images that change). {@code -Dexamples.only} limits the run to an example
 * ({@code 01-invoice}), a language ({@code en}) or both ({@code 01-invoice/en}).</p>
 */
class ExamplesTest {

    private static final Path ROOT = Path.of("test", "examples");
    private static final Path ACTUAL_DIR = ReferenceFiles.ACTUAL_ROOT.resolve("examples");
    private static final float IMAGE_DPI = 110;
    /** The languages of each example, the original first. */
    private static final List<String> LANGUAGES = List.of("es", "en");

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
        List<DynamicTest> tests = new ArrayList<>();
        for (Path example : exampleFolders()) {
            String name = example.getFileName().toString();
            for (String language : LANGUAGES) {
                String id = name + "/" + language;
                if (only == null || only.equals(name) || only.equals(language) || only.equals(id)) {
                    tests.add(DynamicTest.dynamicTest(id, () -> check(example.resolve(language), id)));
                }
            }
        }
        return tests.stream();
    }

    /**
     * Each example has exactly one folder per language, and the document of each language mirrors the original: the
     * same nodes in the same order, the same {@code Id}, {@code Style} and {@code Image} values (an image of the
     * original in {@code .../es/} is the one in {@code .../en/}), and its {@code Language}. Only the texts change.
     */
    @TestFactory
    Stream<DynamicTest> everyLanguageMirrorsTheOriginal() throws IOException {
        List<Path> examples = exampleFolders();
        assertFalse(examples.isEmpty());
        return examples.stream().map(example -> DynamicTest.dynamicTest(example.getFileName().toString(), () -> {
            try (Stream<Path> dirs = Files.list(example)) {
                assertEquals(LANGUAGES.stream().sorted().toList(),
                        dirs.filter(Files::isDirectory).map(d -> d.getFileName().toString()).sorted().toList(),
                        example + ": language folders");
            }
            String original = LANGUAGES.get(0);
            Node source = STXT.rawParser().parseFile(example.resolve(original).resolve("input.stxt").toFile()).get(0);
            for (String language : LANGUAGES) {
                Path file = example.resolve(language).resolve("input.stxt");
                Node mirror = STXT.rawParser().parseFile(file.toFile()).get(0);
                mirrors(source, mirror, original, language, file + ": ");
            }
        }));
    }

    /** The folders of the examples, in order. */
    private static List<Path> exampleFolders() throws IOException {
        try (Stream<Path> dirs = Files.list(ROOT)) {
            return dirs.filter(Files::isDirectory).sorted().toList();
        }
    }

    private static void mirrors(Node source, Node mirror, String original, String language, String path) {
        String here = path + mirror.getName() + " (line " + mirror.getLine() + ")";
        assertEquals(source.getCanonicalName(), mirror.getCanonicalName(), here);
        switch (source.getCanonicalName()) {
            case "id", "style" -> assertEquals(source.getText(), mirror.getText(), here);
            case "image" -> assertEquals(source.getText().replace("/" + original + "/", "/" + language + "/"),
                    mirror.getText(), here);
            case "language" -> assertEquals(language, mirror.getText(), here);
            default -> {
            }
        }
        List<Node> s = source instanceof InlineNode i ? i.getChildren() : List.of();
        List<Node> m = mirror instanceof InlineNode i ? i.getChildren() : List.of();
        assertEquals(s.size(), m.size(), here + ": number of children");
        for (int i = 0; i < s.size(); i++) {
            mirrors(s.get(i), m.get(i), original, language, here + "/");
        }
    }

    private void check(Path dir, String id) throws IOException {
        InlineNode input = reader.read(dir.resolve("input.stxt"));
        Path stylesFile = dir.getParent().resolve("styles.stxt");
        InlineNode styles = Files.exists(stylesFile) ? reader.readStyles(stylesFile) : null;
        Resources resources = new Resources();
        LaidOutDocument document = new Engine(new PdfMetrics(resources), resources).layOutDocument(input, styles);
        ContrastCheck.check(document);
        String dump = StxtDump.dump(document.pages(), false);
        Path actual = ACTUAL_DIR.resolve(id);
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
            ReferenceFiles.compare(expected, STXT.rawParser().parse(dump).get(0), id + "/");
        }
        ReferenceFiles.checkImages(dir, actual, images, "examples.update");
        assertArrayEquals(Files.readAllBytes(pdfFile), pdf, id + ": output.pdf is not the versioned one (the new one "
                + "is in " + actual + "); if the change is intended, -Dexamples.update=true accepts it");
        accessibility(id, pdf, 1);
        accessibility(id, pdf(document, resources, 2), 2);
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
    private static void accessibility(String id, byte[] pdf, int ua) {
        Map<String, String> unexpected = new LinkedHashMap<>(PdfUa.failedRules(pdf, ua));
        unexpected.keySet().removeAll(KNOWN_UA_FAILURES);
        assertEquals(Map.of(), unexpected, id + ": PDF/UA-" + ua + " rules failed");
    }
}
