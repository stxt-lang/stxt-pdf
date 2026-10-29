package dev.stxt.boxes.golden;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.boxes.model.Tolerance;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Lo que comparten los tests de referencia ({@link GoldenTest}) y los de los ejemplos
 * ({@code ExamplesTest}): comparar un volcado (ESPECIFICACION.md §7) con el esperado y dejar en
 * la carpeta del caso una imagen por página del PDF.
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

    private static final Pattern PAGE_IMAGE = Pattern.compile("page-(\\d+)\\.png");

    private ReferenceFiles() {
    }

    /**
     * Escribe en {@code dir} un {@code page-N.png} por página del PDF a la resolución dada y
     * borra los de páginas que ya no existen. Un PNG solo se reescribe si cambian sus píxeles
     * más allá del antialiasing ({@link #PIXEL_TOLERANCE}), de modo que git solo ve cambios reales.
     *
     * @return si se escribió o se borró alguna imagen
     */
    public static boolean writePageImages(Path dir, byte[] pdfBytes, float dpi) throws IOException {
        boolean changed = false;
        int pages;
        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(pdf);
            pages = pdf.getNumberOfPages();
            for (int i = 0; i < pages; i++) {
                changed |= writeIfChanged(renderer.renderImageWithDPI(i, dpi, ImageType.RGB),
                        dir.resolve("page-" + (i + 1) + ".png"));
            }
        }
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.toList()) {
                Matcher m = PAGE_IMAGE.matcher(f.getFileName().toString());
                if (m.matches() && Integer.parseInt(m.group(1)) > pages) {
                    Files.delete(f);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static boolean writeIfChanged(BufferedImage image, Path file) throws IOException {
        if (Files.exists(file)) {
            BufferedImage old = ImageIO.read(file.toFile());
            if (old != null && samePixels(old, image)) {
                return false;
            }
        }
        ImageIO.write(image, "png", file.toFile());
        return true;
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
