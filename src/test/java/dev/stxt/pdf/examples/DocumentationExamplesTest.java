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
 * Los documentos completos de la documentación (cada bloque de código de los README, el manual y la referencia, en todos
 * los idiomas, que empieza por {@code Document (dev.stxt.pdf):}) y {@code test/spec/report.stxt} se tratan como los de
 * {@code test/examples/}: se maquetan con las métricas reales y la carpeta {@code resources/}, se escriben en PDF y el PDF
 * cumple PDF/UA-1 según veraPDF. Así ningún ejemplo del manual ni de la referencia deja de funcionar sin que se note. No
 * tienen imágenes ni volcado de referencia: lo que se ve de ellos es la documentación. El PDF queda en
 * {@code target/reference/docs/} para mirarlo.
 *
 * <p>La documentación está en {@code docs/<idioma>/}. El castellano ({@code docs/es/}) es el original y los demás idiomas
 * son su espejo, con los mismos ficheros.</p>
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

    /** Un documento de la documentación: un nombre para el test y el fichero del PDF, y su texto. */
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

    /** Cada página de componente del manual tiene al menos un documento completo, que se puede copiar y probar. */
    @Test
    void everyComponentOfTheManualHasACompleteDocument() throws IOException {
        for (Path language : languages()) {
            List<Path> components = pages(language.resolve("manual").resolve("components"));
            assertFalse(components.isEmpty());
            for (Path page : components) {
                assertFalse(examples(page).isEmpty(), page + " no tiene ningún documento completo");
            }
        }
    }

    /** Cada idioma tiene los mismos ficheros que el original. */
    @Test
    void everyLanguageMirrorsTheOriginal() throws IOException {
        Path original = DOCS.resolve(ORIGINAL);
        List<String> expected = relativeFiles(original);
        assertFalse(expected.isEmpty());
        for (Path language : languages()) {
            assertEquals(expected, relativeFiles(language), language + " no tiene los mismos ficheros que " + original);
        }
    }

    /**
     * El ejemplo completo de la referencia original es {@code test/spec/report.stxt}, sin los comentarios de su cabecera.
     * El {@code .stxt} indenta con tabuladores y los bloques STXT de los {@code .md} con cuatro espacios por nivel.
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

    /** {@code README.md} y sus traducciones ({@code README.es.md}...), en orden. */
    private static List<Path> readmes() throws IOException {
        try (Stream<Path> list = Files.list(Path.of("."))) {
            return list.map(p -> Path.of(p.getFileName().toString()))
                    .filter(p -> p.toString().matches("README(\\.[a-z]{2})?\\.md")).sorted().toList();
        }
    }

    /** Las carpetas de idioma de {@code docs/}, en orden. */
    private static List<Path> languages() throws IOException {
        try (Stream<Path> list = Files.list(DOCS)) {
            return list.filter(Files::isDirectory).sorted().toList();
        }
    }

    /** Los ficheros de una carpeta y sus subcarpetas, relativos a ella, en orden. */
    private static List<String> relativeFiles(Path folder) throws IOException {
        try (Stream<Path> walk = Files.walk(folder)) {
            return walk.filter(Files::isRegularFile).map(p -> folder.relativize(p).toString().replace('\\', '/'))
                    .sorted().toList();
        }
    }

    /** Las páginas Markdown de una carpeta y sus subcarpetas, en orden. */
    private static List<Path> pages(Path folder) throws IOException {
        try (Stream<Path> walk = Files.walk(folder)) {
            return walk.filter(p -> p.toString().endsWith(".md")).sorted().toList();
        }
    }

    /** Los documentos completos de una página, en orden, con la página y la línea donde empiezan como nombre. */
    private static List<DocDocument> examples(Path page) {
        String text;
        try {
            // Con core.autocrlf, la copia de trabajo puede tener CRLF.
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
        assertEquals(Map.of(), PdfUa.failedRules(pdf), document.name() + ": reglas de PDF/UA-1 incumplidas");
        ByteArrayOutputStream ua2 = new ByteArrayOutputStream();
        PdfDocumentWriter.write(laidOut, resources, Limits.DEFAULT, ua2, false, 2);
        assertEquals(Map.of(), PdfUa.failedRules(ua2.toByteArray(), 2),
                document.name() + ": reglas de PDF/UA-2 incumplidas");
    }
}
