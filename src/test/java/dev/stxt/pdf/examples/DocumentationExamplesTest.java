package dev.stxt.pdf.examples;

import dev.stxt.pdf.golden.PdfUa;
import dev.stxt.pdf.golden.ReferenceFiles;
import dev.stxt.pdf.layout.Engine;
import dev.stxt.pdf.paint.ContrastCheck;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.layout.inline.PdfMetrics;
import dev.stxt.pdf.model.Limits;
import dev.stxt.pdf.paint.PdfDocumentWriter;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The complete documents of the documentation (each code block of the READMEs, the manual and the reference, in every
 * language, that starts with {@code Document (dev.stxt.pdf):}) and {@code test/spec/report.stxt} are treated like those
 * of {@code test/examples/}: they are laid out with the real metrics and the {@code resources/} folder, written to PDF,
 * and the PDF complies with PDF/UA-1 according to veraPDF. This way no example of the manual or the reference stops
 * working unnoticed. They have no reference images or dump: what is seen of them is the documentation. The PDF is left
 * in {@code target/reference/docs/} for inspection.
 *
 * <p>The documentation is in {@code docs/<language>/}. Spanish ({@code docs/es/}) is the original and the other
 * languages are its mirror, with the same files.</p>
 */
class DocumentationExamplesTest {

    private static final Path DOCS = Path.of("docs");
    private static final String ORIGINAL = "es";
    private static final Path REPORT = Path.of("test", "spec", "report.stxt");
    private static final Path ACTUAL_DIR = ReferenceFiles.ACTUAL_ROOT.resolve("docs");
    private static final Pattern EXAMPLE =
            Pattern.compile("```(?:stxt)?\n(Document \\(dev\\.stxt\\.pdf\\):\n.*?)```", Pattern.DOTALL);
    private static final Pattern LEADING_TABS = Pattern.compile("(?m)^\t+");

    private final DocumentReader reader = new DocumentReader();

    /** A document of the documentation: a name for the test and the PDF file, and its text. */
    private record DocDocument(String name, String text) {
    }

    @TestFactory
    Stream<DynamicTest> documentationDocuments() throws IOException {
        List<DocDocument> documents = new ArrayList<>();
        List<Path> pages = new ArrayList<>(readmes());
        for (Path language : languages()) {
            pages.addAll(pages(language.resolve("manual")));
            pages.addAll(pages(language.resolve("reference")));
        }
        for (Path page : pages) {
            documents.addAll(examples(page));
        }
        documents.add(new DocDocument("report", Files.readString(REPORT)));
        return documents.stream().map(d -> DynamicTest.dynamicTest(d.name(), () -> check(d)));
    }

    /** Each component page of the manual has at least one complete document, which can be copied and tried. */
    @Test
    void everyComponentOfTheManualHasACompleteDocument() throws IOException {
        for (Path language : languages()) {
            List<Path> components = pages(language.resolve("manual").resolve("components"));
            assertFalse(components.isEmpty());
            for (Path page : components) {
                assertFalse(examples(page).isEmpty(), page + " has no complete document");
            }
        }
    }

    /** Each language has the same files as the original. */
    @Test
    void everyLanguageMirrorsTheOriginal() throws IOException {
        Path original = DOCS.resolve(ORIGINAL);
        List<String> expected = relativeFiles(original);
        assertFalse(expected.isEmpty());
        for (Path language : languages()) {
            assertEquals(expected, relativeFiles(language), language + " does not have the same files as " + original);
        }
    }

    /**
     * The complete example of the original reference is {@code test/spec/report.stxt}, without the comments of its
     * header. The {@code .stxt} indents with tabs and the STXT blocks of the {@code .md} files with four spaces per
     * level.
     */
    @Test
    void theReportIsTheCompleteExample() throws IOException {
        String report = Files.readString(REPORT).replace("\r\n", "\n").replaceFirst("\\A(#[^\n]*\n)+", "");
        report = LEADING_TABS.matcher(report).replaceAll(m -> "    ".repeat(m.group().length()));
        Path completeExample = DOCS.resolve(ORIGINAL).resolve("reference").resolve("common").resolve("complete-example.md");
        List<DocDocument> examples = examples(completeExample);
        assertEquals(1, examples.size());
        assertEquals(examples.get(0).text(), report);
    }

    /** {@code README.md} and its translations ({@code README.es.md}...), in order. */
    private static List<Path> readmes() throws IOException {
        try (Stream<Path> list = Files.list(Path.of("."))) {
            return list.map(p -> Path.of(p.getFileName().toString()))
                    .filter(p -> p.toString().matches("README(\\.[a-z]{2})?\\.md")).sorted().toList();
        }
    }

    /** The language folders of {@code docs/}, in order. */
    private static List<Path> languages() throws IOException {
        try (Stream<Path> list = Files.list(DOCS)) {
            return list.filter(Files::isDirectory).sorted().toList();
        }
    }

    /** The files of a folder and its subfolders, relative to it, in order. */
    private static List<String> relativeFiles(Path folder) throws IOException {
        try (Stream<Path> walk = Files.walk(folder)) {
            return walk.filter(Files::isRegularFile).map(p -> folder.relativize(p).toString().replace('\\', '/'))
                    .sorted().toList();
        }
    }

    /** The Markdown pages of a folder and its subfolders, in order. */
    private static List<Path> pages(Path folder) throws IOException {
        try (Stream<Path> walk = Files.walk(folder)) {
            return walk.filter(p -> p.toString().endsWith(".md")).sorted().toList();
        }
    }

    /** The complete documents of a page, in order, named after the page and the line where they start. */
    private static List<DocDocument> examples(Path page) {
        String text;
        try {
            // With core.autocrlf, the working copy can have CRLF.
            text = Files.readString(page).replace("\r\n", "\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String name = (page.startsWith(DOCS) ? DOCS.relativize(page) : page).toString().replace('\\', '/').replace(".md", "").replace('/', '-');
        Matcher m = EXAMPLE.matcher(text);
        List<DocDocument> examples = new ArrayList<>();
        while (m.find()) {
            int line = 1 + (int) text.substring(0, m.start(1)).chars().filter(c -> c == '\n').count();
            examples.add(new DocDocument(name + "-" + line, m.group(1)));
        }
        return examples;
    }

    private void check(DocDocument document) throws IOException {
        Resources resources = new Resources();
        LaidOutDocument laidOut = new Engine(new PdfMetrics(resources), resources)
                .layOutDocument(reader.read(document.text()));
        ContrastCheck.check(laidOut);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfDocumentWriter.write(laidOut, resources, Limits.DEFAULT, out, false);
        byte[] pdf = out.toByteArray();
        Files.createDirectories(ACTUAL_DIR);
        Files.write(ACTUAL_DIR.resolve(document.name() + ".pdf"), pdf);
        assertEquals(Map.of(), PdfUa.failedRules(pdf), document.name() + ": PDF/UA-1 rules failed");
        ByteArrayOutputStream ua2 = new ByteArrayOutputStream();
        PdfDocumentWriter.write(laidOut, resources, Limits.DEFAULT, ua2, false, 2);
        assertEquals(Map.of(), PdfUa.failedRules(ua2.toByteArray(), 2),
                document.name() + ": PDF/UA-2 rules failed");
    }
}
