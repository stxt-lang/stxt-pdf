package dev.stxt.boxes.golden;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.LaidOutDocument;
import dev.stxt.boxes.output.StxtDump;
import dev.stxt.boxes.convert.Converter;
import dev.stxt.boxes.paint.SvgCanvas;
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
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests de referencia (ESPECIFICACION.md §7): cada carpeta de {@code golden/} contiene
 * {@code input.stxt} y {@code expected.stxt}. Se comparan nombres y valores; los números de
 * {@code Rect} con una tolerancia de 0.01 pt. La entrada se valida contra el schema; el
 * volcado no tiene namespace y se lee sin validación.
 *
 * <p>Cada caso deja además un SVG de depuración en {@code target/golden-svg/} y, en su propia
 * carpeta de {@code test/golden/}, el PDF y una imagen por página (ver
 * {@link #writePdfAndImages}).</p>
 */
class GoldenTest {

    private static final Path SVG_DIR = Path.of("target", "golden-svg");
    /** Carpeta de los casos en el código fuente, donde se escriben el PDF y sus imágenes. */
    private static final Path SOURCE_DIR = Path.of("test", "golden");
    private static final float IMAGE_DPI = 100;

    /** Casos que necesitan un hito pendiente: se saltan con el motivo. */
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
                writePdfAndImages(SOURCE_DIR.resolve(name), input, styles);
                String dump = StxtDump.dump(document.pages(), false);
                ReferenceFiles.compare(expected, STXT.rawParser().parse(dump).get(0), name + "/");
            }));
        }
    }

    private static void writeSvg(String name, LaidOutDocument document) throws IOException {
        Files.createDirectories(SVG_DIR);
        for (Page p : document.pages()) {
            String svg = SvgCanvas.page(p, p.media(), true, new Resources());
            Files.writeString(SVG_DIR.resolve(name + "-" + p.number() + ".svg"), svg, StandardCharsets.UTF_8);
        }
    }

    /**
     * Escribe en la carpeta del caso, en el código fuente, {@code output.pdf} (ignorado por git)
     * y un {@code page-N.png} por página (versionados): así un cambio en el resultado se ve en el
     * diff ({@link ReferenceFiles#writePageImages}). El PDF lleva los contornos de depuración de
     * las cajas y se maqueta con las métricas reales de las fuentes de {@code resources/fonts},
     * que se incrustan ({@link Converter}),
     * no con las de la comparación.
     */
    private static void writePdfAndImages(Path dir, InlineNode input, InlineNode styles) throws IOException {
        if (!Files.isDirectory(dir)) {
            return;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.Builder converter = Converter.builder().debug(true);
        if (styles != null) {
            converter.styles(styles);
        }
        converter.build().toPdf(input, out);
        Files.write(dir.resolve("output.pdf"), out.toByteArray());
        ReferenceFiles.writePageImages(dir, out.toByteArray(), IMAGE_DPI);
    }
}
