package dev.stxt.boxes.examples;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.boxes.golden.ReferenceFiles;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.LaidOutDocument;
import dev.stxt.boxes.layout.inline.PdfMetrics;
import dev.stxt.boxes.output.StxtDump;
import dev.stxt.boxes.paint.PdfCanvas;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.stxt.DocumentReader;
import dev.stxt.runtime.STXT;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

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
 * 0.01 pt; el PDF ({@code output.pdf}, ignorado por git, se regenera en cada ejecución) y una
 * imagen por página ({@code page-N.png}, versionadas, que solo se reescriben si cambian sus
 * píxeles).</p>
 *
 * <p>El volcado esperado no se calcula a mano: es una instantánea de lo que da el motor, que el
 * autor revisa mirando las imágenes. Se regenera con {@code -Dexamples.update=true}, y se puede
 * probar un solo ejemplo con {@code -Dexamples.only=01-invoice}.</p>
 */
class ExamplesTest {

    private static final Path ROOT = Path.of("test", "examples");
    private static final float IMAGE_DPI = 110;

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
        String dump = StxtDump.dump(document.pages(), document.media().pageArea(), false);

        Path expectedFile = dir.resolve("expected.stxt");
        if (Boolean.getBoolean("examples.update")) {
            Files.writeString(expectedFile, dump, StandardCharsets.UTF_8);
        } else if (!Files.exists(expectedFile)) {
            fail(dir + ": falta expected.stxt; genéralo con -Dexamples.update=true y revisa las imágenes");
        } else {
            Node expected = STXT.rawParser().parseFile(expectedFile.toFile()).get(0);
            ReferenceFiles.compare(expected, STXT.rawParser().parse(dump).get(0), dir.getFileName() + "/");
        }
        writePdfAndImages(dir, document, resources);
    }

    private static void writePdfAndImages(Path dir, LaidOutDocument document, Resources resources) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfCanvas.write(document, resources, out, false);
        Files.write(dir.resolve("output.pdf"), out.toByteArray());
        ReferenceFiles.writePageImages(dir, out.toByteArray(), IMAGE_DPI);
    }
}
