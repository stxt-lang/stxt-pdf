package dev.stxt.boxes.examples;

import dev.stxt.InlineNode;
import dev.stxt.boxes.golden.PdfUa;
import dev.stxt.boxes.golden.ReferenceFiles;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.LaidOutDocument;
import dev.stxt.boxes.layout.inline.PdfMetrics;
import dev.stxt.boxes.model.Limits;
import dev.stxt.boxes.paint.PdfDocumentWriter;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.stxt.DocumentReader;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Los documentos de la especificación (los ejemplos de {@code docs/ESPECIFICACION.md} y el del §10,
 * {@code test/spec/report.stxt}) se tratan como los de {@code test/examples/}: se maquetan con las métricas reales y
 * la carpeta {@code resources/}, se escriben en PDF y el PDF cumple PDF/UA-1 según veraPDF (ROADMAP-QA-ARQ.md, QA-08).
 * No tienen imágenes ni volcado de referencia: lo que se ve de ellos es la especificación. El PDF queda en
 * {@code target/reference/spec/} para mirarlo.
 */
class SpecificationDocumentsTest {

    private static final Path SPEC = Path.of("docs", "ESPECIFICACION.md");
    private static final Path REPORT = Path.of("test", "spec", "report.stxt");
    private static final Path ACTUAL_DIR = ReferenceFiles.ACTUAL_ROOT.resolve("spec");
    private static final Pattern EXAMPLE =
            Pattern.compile("```\n(Document \\(dev\\.stxt\\.boxes\\):\n.*?)```", Pattern.DOTALL);

    private final DocumentReader reader = new DocumentReader();

    /** Un documento de la especificación: un nombre para el test y el fichero del PDF, y su texto. */
    private record SpecDocument(String name, String text) {
    }

    @TestFactory
    Stream<DynamicTest> specificationDocuments() throws IOException {
        List<SpecDocument> documents = new ArrayList<>(examples());
        documents.add(new SpecDocument("report", Files.readString(REPORT)));
        return documents.stream().map(d -> DynamicTest.dynamicTest(d.name(), () -> check(d)));
    }

    /** Hay cuatro ejemplos en la especificación; si cambia el número, cambia también este test. */
    @Test
    void theSpecificationHasFourExamples() throws IOException {
        assertEquals(4, examples().size());
    }

    /** El ejemplo del §10 es {@code test/spec/report.stxt}, sin los comentarios de su cabecera. */
    @Test
    void theReportIsTheExampleOfSection10() throws IOException {
        String report = Files.readString(REPORT).replace("\r\n", "\n").replaceFirst("\\A(#[^\n]*\n)+", "");
        assertEquals(examples().get(examples().size() - 1).text(), report);
    }

    /** Los ejemplos de la especificación, en orden, con la línea donde empiezan como nombre. */
    private static List<SpecDocument> examples() throws IOException {
        // Con core.autocrlf, la copia de trabajo puede tener CRLF.
        String spec = Files.readString(SPEC).replace("\r\n", "\n");
        Matcher m = EXAMPLE.matcher(spec);
        List<SpecDocument> examples = new ArrayList<>();
        while (m.find()) {
            int line = 1 + (int) spec.substring(0, m.start(1)).chars().filter(c -> c == '\n').count();
            examples.add(new SpecDocument("ESPECIFICACION-" + line, m.group(1)));
        }
        return examples;
    }

    private void check(SpecDocument document) throws IOException {
        Resources resources = new Resources();
        LaidOutDocument laidOut = new Engine(new PdfMetrics(resources), resources)
                .layOutDocument(reader.read(document.text()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfDocumentWriter.write(laidOut, resources, Limits.DEFAULT, out, false);
        byte[] pdf = out.toByteArray();
        Files.createDirectories(ACTUAL_DIR);
        Files.write(ACTUAL_DIR.resolve(document.name() + ".pdf"), pdf);
        assertEquals(Map.of(), PdfUa.failedRules(pdf), document.name() + ": reglas de PDF/UA-1 incumplidas");
    }
}
