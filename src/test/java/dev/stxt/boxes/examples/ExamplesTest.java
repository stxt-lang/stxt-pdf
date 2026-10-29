package dev.stxt.boxes.examples;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.boxes.model.Limits;
import dev.stxt.boxes.golden.PdfUa;
import dev.stxt.boxes.golden.ReferenceFiles;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.LaidOutDocument;
import dev.stxt.boxes.layout.inline.PdfMetrics;
import dev.stxt.boxes.output.StxtDump;
import dev.stxt.boxes.paint.PdfDocumentWriter;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.stxt.DocumentReader;
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
 * Ejemplos de documentos completos ({@code test/examples/NN-nombre/}). A diferencia de los casos
 * golden, que comprueban un algoritmo con un resultado calculado a mano, un ejemplo es un
 * documento como los que escribiría un autor, maquetado con las fuentes y las imágenes reales de
 * {@code resources/} y sin contornos de depuración.
 *
 * <p>Cada carpeta tiene {@code input.stxt}, que debe validar y maquetarse sin errores, y puede
 * tener {@code styles.stxt}, el documento de estilos aparte que se pasa junto a él (ESPECIFICACION.md
 * §4.10); el volcado
 * de las cajas con {@code Id} ({@code expected.stxt}, §7), que se compara con tolerancia de
 * 0.01 pt; una imagen por página ({@code page-N.png}, versionadas) y el PDF ({@code output.pdf}, versionado para
 * poder abrirlo desde el repositorio, pero no se compara: lleva la fecha y un identificador que cambian en cada
 * ejecución). El PDF y las imágenes nuevas se escriben en {@code target/reference/examples/}, y si alguna imagen no
 * es la versionada el ejemplo falla.</p>
 *
 * <p>El volcado esperado no se calcula a mano: es una instantánea de lo que da el motor, que el
 * autor revisa mirando las imágenes. {@code -Dexamples.update=true} acepta los cambios (reescribe el volcado, el PDF
 * y las imágenes que cambian), y se puede probar un solo ejemplo con {@code -Dexamples.only=01-invoice}.</p>
 */
class ExamplesTest {

    private static final Path ROOT = Path.of("test", "examples");
    private static final Path ACTUAL_DIR = ReferenceFiles.ACTUAL_ROOT.resolve("examples");
    private static final float IMAGE_DPI = 110;

    /**
     * Reglas de PDF/UA-1 que los ejemplos todavía incumplen y no son de lo que se comprueba aquí; el resto
     * debe cumplirse. Ahora ninguna: cada ejemplo, que declara PDF/UA-1 (tiene {@code Title}), lo cumple
     * según veraPDF. Se añade aquí una regla solo con un motivo apuntado en {@code TODO.md}.
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
            fail(dir + ": falta expected.stxt u output.pdf; genéralos con -Dexamples.update=true y revisa las imágenes");
        } else {
            Node expected = STXT.rawParser().parseFile(expectedFile.toFile()).get(0);
            ReferenceFiles.compare(expected, STXT.rawParser().parse(dump).get(0), dir.getFileName() + "/");
        }
        ReferenceFiles.checkImages(dir, actual, images, "examples.update");
        accessibility(dir, pdf);
    }

    private static byte[] pdf(LaidOutDocument document, Resources resources) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfDocumentWriter.write(document, resources, Limits.DEFAULT, out, false);
        return out.toByteArray();
    }

    /** El PDF etiquetado cumple PDF/UA-1 salvo en {@link #KNOWN_UA_FAILURES}. */
    private static void accessibility(Path dir, byte[] pdf) {
        Map<String, String> unexpected = new LinkedHashMap<>(PdfUa.failedRules(pdf));
        unexpected.keySet().removeAll(KNOWN_UA_FAILURES);
        assertEquals(Map.of(), unexpected, dir.getFileName() + ": reglas de PDF/UA-1 incumplidas");
    }
}
