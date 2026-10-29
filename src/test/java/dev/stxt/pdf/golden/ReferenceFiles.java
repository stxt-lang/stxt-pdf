package dev.stxt.pdf.golden;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.pdf.model.Tolerance;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Lo que comparten los tests de referencia ({@link GoldenTest}) y los de los ejemplos ({@code ExamplesTest}): comparar
 * un volcado (referencia/implementacion/pruebas.md) con el esperado, y las imágenes de cada página del PDF con las
 * versionadas en la carpeta del caso.
 *
 * <p>El PDF y las imágenes nuevas se escriben siempre en {@code target/reference/} ({@link #writeActual}), nunca en
 * {@code test/}: {@code mvn test} no cambia ningún fichero versionado. Si una imagen nueva no es la versionada (más
 * allá del antialiasing, {@link #PIXEL_TOLERANCE}), o hay páginas de más o de menos, el test falla
 * ({@link #checkImages}) y dice cómo aceptar el cambio: con la propiedad de actualizar de su test
 * ({@code -Dgolden.update=true} o {@code -Dexamples.update=true}), que reescribe las versionadas.</p>
 */
public final class ReferenceFiles {

    /** Los {@code Rect} se comparan con esta tolerancia, en puntos: la del modelo ({@link Tolerance#MODEL}). */
    public static final double TOLERANCE = Tolerance.MODEL;
    /**
     * Diferencia máxima por canal (de 0 a 255) entre dos píxeles que se consideran iguales. El
     * antialiasing de PDFBox cambia de una máquina a otra: entre dos equipos se han visto hasta 30
     * píxeles por imagen con diferencias de hasta 10, en las esquinas de los contornos. Un cambio
     * real de layout mueve líneas y texto, con diferencias cercanas a 255.
     */
    public static final int PIXEL_TOLERANCE = 16;

    /** Donde se escriben el PDF y las imágenes de cada caso: {@code target/reference/<grupo>/<caso>/}. */
    public static final Path ACTUAL_ROOT = Path.of("target", "reference");

    private static final Pattern PAGE_IMAGE = Pattern.compile("page-(\\d+)\\.png");

    private ReferenceFiles() {
    }

    /**
     * Escribe en {@code dir} (de {@code target/}) el PDF como {@code output.pdf} y un {@code page-N.png} por página a la
     * resolución dada, y borra los de páginas que ya no existen.
     *
     * @return las imágenes de las páginas, en orden
     */
    public static List<BufferedImage> writeActual(Path dir, byte[] pdfBytes, float dpi) throws IOException {
        Files.createDirectories(dir);
        Files.write(dir.resolve("output.pdf"), pdfBytes);
        List<BufferedImage> images = new ArrayList<>();
        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(pdf);
            for (int i = 0; i < pdf.getNumberOfPages(); i++) {
                images.add(renderer.renderImageWithDPI(i, dpi, ImageType.RGB));
            }
        }
        for (Path old : pageImages(dir)) {
            Files.delete(old);
        }
        for (int i = 0; i < images.size(); i++) {
            ImageIO.write(images.get(i), "png", dir.resolve(page(i + 1)).toFile());
        }
        return images;
    }

    /**
     * Compara las imágenes de un caso con los {@code page-N.png} versionados en {@code reference}. Con la propiedad
     * {@code update} a {@code true} los reescribe en su lugar: solo los que cambian (para que git solo vea cambios
     * reales) y borra los de páginas que ya no existen.
     *
     * @param actual donde {@link #writeActual} dejó las imágenes nuevas, para el mensaje
     * @throws AssertionError si alguna página cambia, falta o sobra, y no se pide actualizar
     */
    public static void checkImages(Path reference, Path actual, List<BufferedImage> images, String update)
            throws IOException {
        boolean updating = Boolean.getBoolean(update);
        List<String> problems = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            Path file = reference.resolve(page(i + 1));
            BufferedImage old = Files.exists(file) ? ImageIO.read(file.toFile()) : null;
            int differences = old == null ? -1 : differences(old, images.get(i));
            if (differences == 0) {
                continue;
            }
            if (updating) {
                ImageIO.write(images.get(i), "png", file.toFile());
            } else {
                problems.add(page(i + 1) + (old == null ? " no está versionada"
                        : differences < 0 ? " cambia de tamaño" : " cambia en " + differences + " píxeles"));
            }
        }
        for (Path extra : pageImages(reference)) {
            if (pageNumber(extra) > images.size()) {
                if (updating) {
                    Files.delete(extra);
                } else {
                    problems.add(extra.getFileName() + " sobra: el PDF tiene " + images.size() + " páginas");
                }
            }
        }
        if (!problems.isEmpty()) {
            fail(reference + ": " + String.join("; ", problems) + ". Las imágenes nuevas están en " + actual
                    + ". Si el cambio es el que se busca, míralas y acéptalo con -D" + update + "=true");
        }
    }

    private static String page(int number) {
        return "page-" + number + ".png";
    }

    /** Los {@code page-N.png} de una carpeta. */
    private static List<Path> pageImages(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(f -> PAGE_IMAGE.matcher(f.getFileName().toString()).matches()).sorted().toList();
        }
    }

    private static int pageNumber(Path image) {
        Matcher m = PAGE_IMAGE.matcher(image.getFileName().toString());
        m.matches();
        return Integer.parseInt(m.group(1));
    }

    /**
     * Píxeles que difieren en algún canal más que {@link #PIXEL_TOLERANCE}; -1 si las imágenes no tienen el mismo
     * tamaño.
     */
    static int differences(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return -1;
        }
        int w = a.getWidth();
        int h = a.getHeight();
        int[] p = a.getRGB(0, 0, w, h, null, 0, w);
        int[] q = b.getRGB(0, 0, w, h, null, 0, w);
        int count = 0;
        for (int i = 0; i < p.length; i++) {
            for (int shift = 0; shift < 24; shift += 8) {
                if (Math.abs((p[i] >> shift & 0xff) - (q[i] >> shift & 0xff)) > PIXEL_TOLERANCE) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    /** Compara dos volcados: nombres y valores iguales y los {@code Rect} dentro de {@link #TOLERANCE}. */
    public static void compare(Node expected, Node actual, String path) {
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
