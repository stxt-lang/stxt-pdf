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
 * Los documentos completos de la documentación (cada bloque de código del README, el manual y la referencia que empieza por
 * {@code Document (dev.stxt.pdf):}) y {@code test/spec/report.stxt} se tratan como los de {@code test/examples/}: se
 * maquetan con las métricas reales y la carpeta {@code resources/}, se escriben en PDF y el PDF cumple PDF/UA-1 según
 * veraPDF. Así ningún ejemplo del manual ni de la referencia deja de funcionar sin que se note. No tienen imágenes ni
 * volcado de referencia: lo que se ve de ellos es la documentación. El PDF queda en {@code target/reference/docs/} para
 * mirarlo.
 */
class DocumentationExamplesTest {

    private static final Path README = Path.of("README.md");
    private static final Path DOCS = Path.of("docs");
    private static final Path MANUAL = DOCS.resolve("manual");
    private static final Path REFERENCE = DOCS.resolve("referencia");
    private static final Path COMPONENTS = MANUAL.resolve("componentes");
    private static final Path COMPLETE_EXAMPLE = REFERENCE.resolve("comun").resolve("ejemplo-completo.md");
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
        List<Path> pages = new ArrayList<>(List.of(README));
        pages.addAll(pages(MANUAL));
        pages.addAll(pages(REFERENCE));
        for (Path page : pages) {
            documents.addAll(examples(page));
        }
        documents.add(new DocDocument("report", Files.readString(REPORT)));
        return documents.stream().map(d -> DynamicTest.dynamicTest(d.name(), () -> check(d)));
    }

    /** Cada página de componente del manual tiene al menos un documento completo, que se puede copiar y probar. */
    @Test
    void everyComponentOfTheManualHasACompleteDocument() throws IOException {
        List<Path> components = pages(COMPONENTS);
        assertFalse(components.isEmpty());
        for (Path page : components) {
            assertFalse(examples(page).isEmpty(), page + " no tiene ningún documento completo");
        }
    }

    /**
     * El ejemplo completo de la referencia es {@code test/spec/report.stxt}, sin los comentarios de su cabecera. El
     * {@code .stxt} indenta con tabuladores y los bloques STXT de los {@code .md} con cuatro espacios por nivel.
     */
    @Test
    void theReportIsTheCompleteExample() throws IOException {
        String report = Files.readString(REPORT).replace("\r\n", "\n").replaceFirst("\\A(#[^\n]*\n)+", "");
        report = LEADING_TABS.matcher(report).replaceAll(m -> "    ".repeat(m.group().length()));
        List<DocDocument> examples = examples(COMPLETE_EXAMPLE);
        assertEquals(1, examples.size());
        assertEquals(examples.get(0).text(), report);
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
