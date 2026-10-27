package dev.stxt.boxes.golden;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.output.StxtDump;
import dev.stxt.boxes.paint.PdfCanvas;
import dev.stxt.boxes.paint.SvgCanvas;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.stxt.DocumentReader;
import dev.stxt.boxes.tree.BoxTreeBuilder;
import dev.stxt.boxes.tree.Media;
import dev.stxt.runtime.STXT;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests de referencia (ESPECIFICACION.md §7): cada carpeta de {@code golden/} contiene
 * {@code input.stxt} y {@code expected.stxt}. Se comparan nombres y valores; los números de
 * {@code Rect} con una tolerancia de 0.01 pt. La entrada se valida contra el schema; el
 * volcado no tiene namespace y se lee sin validación.
 *
 * <p>Cada caso deja además un SVG de depuración en {@code target/golden-svg/} y, en su propia
 * carpeta de {@code src/test/resources/golden/}, el PDF y una imagen por página (ver
 * {@link #writePdfAndImages}).</p>
 */
class GoldenTest {

    private static final double TOLERANCE = 0.01;
    private static final Path SVG_DIR = Path.of("target", "golden-svg");
    /** Carpeta de los casos en el código fuente, donde se escriben el PDF y sus imágenes. */
    private static final Path SOURCE_DIR = Path.of("src", "test", "resources", "golden");
    private static final float IMAGE_DPI = 100;
    private static final Pattern PAGE_IMAGE = Pattern.compile("page-(\\d+)\\.png");
    /**
     * Diferencia máxima por canal (de 0 a 255) entre dos píxeles que se consideran iguales. El
     * antialiasing de PDFBox cambia de una máquina a otra: entre dos equipos se han visto hasta 30
     * píxeles por imagen con diferencias de hasta 10, en las esquinas de los contornos. Un cambio
     * real de layout mueve líneas y texto, con diferencias cercanas a 255.
     */
    private static final int PIXEL_TOLERANCE = 16;

    /** Casos que necesitan un hito pendiente: se saltan con el motivo. */
    private static final Map<String, String> PENDING = Map.of();

    private final DocumentReader reader = new DocumentReader();

    @TestFactory
    Stream<DynamicTest> cases() throws IOException, URISyntaxException {
        Path root = Path.of(getClass().getResource("/golden").toURI());
        try (Stream<Path> dirs = Files.list(root)) {
            List<Path> cases = dirs.filter(Files::isDirectory).sorted().toList();
            return cases.stream().map(dir -> DynamicTest.dynamicTest(dir.getFileName().toString(), () -> {
                String name = dir.getFileName().toString();
                assumeTrue(!PENDING.containsKey(name), () -> name + ": " + PENDING.get(name));
                InlineNode input = reader.read(dir.resolve("input.stxt"));
                Node expected = STXT.rawParser().parseFile(dir.resolve("expected.stxt").toFile()).get(0);
                List<Page> pages = new Engine().layOutDocument(input);
                Media media = new BoxTreeBuilder().build(input).media();
                writeSvg(name, pages, media);
                writePdfAndImages(SOURCE_DIR.resolve(name), input);
                String dump = StxtDump.dump(pages, media.pageArea(), false);
                compare(expected, STXT.rawParser().parse(dump).get(0), name + "/");
            }));
        }
    }

    private static void writeSvg(String name, List<Page> pages, Media media) throws IOException {
        Files.createDirectories(SVG_DIR);
        for (Page p : pages) {
            String svg = SvgCanvas.page(p, media, true);
            Files.writeString(SVG_DIR.resolve(name + "-" + p.number() + ".svg"), svg, StandardCharsets.UTF_8);
        }
    }

    /**
     * Escribe en la carpeta del caso, en el código fuente, {@code output.pdf} (ignorado por git)
     * y un {@code page-N.png} por página (versionados): así un cambio en el resultado se ve en el
     * diff. Un PNG solo se reescribe si cambian sus píxeles más allá del antialiasing
     * ({@link #PIXEL_TOLERANCE}), y se borran los de páginas que ya no
     * existen. El PDF lleva los contornos de depuración de las cajas y se maqueta con las métricas
     * reales de las fuentes de {@code resources/fonts}, que se incrustan ({@link PdfCanvas#write(InlineNode,
     * Resources, java.io.OutputStream, boolean)}), no con las de la comparación.
     */
    private static void writePdfAndImages(Path dir, InlineNode input) throws IOException {
        if (!Files.isDirectory(dir)) {
            return;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfCanvas.write(input, new Resources(), out, true);
        Files.write(dir.resolve("output.pdf"), out.toByteArray());
        int pages;
        try (PDDocument pdf = Loader.loadPDF(out.toByteArray())) {
            PDFRenderer renderer = new PDFRenderer(pdf);
            pages = pdf.getNumberOfPages();
            for (int i = 0; i < pages; i++) {
                writeIfChanged(renderer.renderImageWithDPI(i, IMAGE_DPI, ImageType.RGB),
                        dir.resolve("page-" + (i + 1) + ".png"));
            }
        }
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.toList()) {
                Matcher m = PAGE_IMAGE.matcher(f.getFileName().toString());
                if (m.matches() && Integer.parseInt(m.group(1)) > pages) {
                    Files.delete(f);
                }
            }
        }
    }

    private static void writeIfChanged(BufferedImage image, Path file) throws IOException {
        if (Files.exists(file)) {
            BufferedImage old = ImageIO.read(file.toFile());
            if (old != null && samePixels(old, image)) {
                return;
            }
        }
        ImageIO.write(image, "png", file.toFile());
    }

    private static boolean samePixels(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return false;
        }
        int w = a.getWidth();
        int h = a.getHeight();
        int[] p = a.getRGB(0, 0, w, h, null, 0, w);
        int[] q = b.getRGB(0, 0, w, h, null, 0, w);
        for (int i = 0; i < p.length; i++) {
            for (int shift = 0; shift < 24; shift += 8) {
                if (Math.abs((p[i] >> shift & 0xff) - (q[i] >> shift & 0xff)) > PIXEL_TOLERANCE) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void compare(Node expected, Node actual, String path) {
        String here = path + expected.getName();
        assertEquals(expected.getCanonicalName(), actual.getCanonicalName(), here);
        if (expected.getCanonicalName().equals("rect")) {
            double[] e = numbers(expected.getText());
            double[] a = numbers(actual.getText());
            assertEquals(4, a.length, here);
            for (int i = 0; i < 4; i++) {
                assertEquals(e[i], a[i], TOLERANCE, here + " (componente " + i + ")");
            }
        } else {
            assertEquals(expected.getText().trim(), actual.getText().trim(), here);
        }
        List<Node> e = children(expected);
        List<Node> a = children(actual);
        assertEquals(e.size(), a.size(), here + ": número de hijos");
        for (int i = 0; i < e.size(); i++) {
            compare(e.get(i), a.get(i), here + "/");
        }
    }

    private static List<Node> children(Node n) {
        return n instanceof InlineNode i ? i.getChildren() : List.of();
    }

    private static double[] numbers(String s) {
        return Stream.of(s.trim().split("\\s+")).mapToDouble(Double::parseDouble).toArray();
    }
}
