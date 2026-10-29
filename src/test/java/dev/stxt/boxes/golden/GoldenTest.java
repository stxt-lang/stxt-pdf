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
 * Tests de referencia (referencia/implementacion/pruebas.md): cada carpeta de {@code golden/} contiene
 * {@code input.stxt} y {@code expected.stxt}. Se comparan nombres y valores; los números de
 * {@code Rect} con una tolerancia de 0.01 pt. La entrada se valida contra el schema; el
 * volcado no tiene namespace y se lee sin validación.
 *
 * <p>Cada caso deja además un SVG de depuración en {@code target/golden-svg/}, y el PDF y una imagen por página en
 * {@code target/reference/golden/}, que se comparan con los {@code page-N.png} versionados en su carpeta de
 * {@code test/golden/}: si cambian, el caso falla; {@code -Dgolden.update=true} los acepta (ver
 * {@link #writePdfAndImages}). El volcado esperado, que se calcula a mano, no se actualiza nunca.</p>
 */
class GoldenTest {

    private static final Path SVG_DIR = Path.of("target", "golden-svg");
    /** Carpeta de los casos en el código fuente, con las imágenes versionadas. */
    private static final Path SOURCE_DIR = Path.of("test", "golden");
    private static final Path ACTUAL_DIR = ReferenceFiles.ACTUAL_ROOT.resolve("golden");
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
            String svg = SvgCanvas.page(p, p.media(), true, new Resources());
            Files.writeString(SVG_DIR.resolve(name + "-" + p.number() + ".svg"), svg, StandardCharsets.UTF_8);
        }
    }

    /**
     * Escribe en {@code dir} ({@code target/reference/golden/<caso>/}) {@code output.pdf} y un {@code page-N.png} por
     * página ({@link ReferenceFiles#writeActual}), y devuelve las imágenes para compararlas con las versionadas. El PDF
     * lleva los contornos de depuración de las cajas y se maqueta con las métricas reales de las fuentes de
     * {@code resources/fonts}, que se incrustan ({@link Converter}), no con las de la comparación.
     */
    private static List<BufferedImage> writePdfAndImages(Path dir, InlineNode input, InlineNode styles)
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.Builder converter = Converter.builder().debug(true);
        if (styles != null) {
            converter.styles(styles);
        }
        converter.build().toPdf(input, out);
        return ReferenceFiles.writeActual(dir, out.toByteArray(), IMAGE_DPI);
    }
}
