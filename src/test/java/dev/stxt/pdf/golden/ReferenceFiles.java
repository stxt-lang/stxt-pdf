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
 * What the reference tests ({@link GoldenTest}) and the example tests ({@code ExamplesTest}) share: comparing
 * a dump (reference/implementation/testing.md) with the expected one, and the images of each page of the PDF with the
 * ones versioned in the folder of the case.
 *
 * <p>The PDF and the new images are always written to {@code target/reference/} ({@link #writeActual}), never to
 * {@code test/}: {@code mvn test} changes no versioned file. If a new image is not the versioned one (beyond
 * antialiasing, {@link #PIXEL_TOLERANCE}), or there are pages too many or too few, the test fails
 * ({@link #checkImages}) and says how to accept the change: with the update property of its test
 * ({@code -Dgolden.update=true} or {@code -Dexamples.update=true}), which rewrites the versioned ones.</p>
 */
public final class ReferenceFiles {

    /** The {@code Rect} values are compared with this tolerance, in points: the model's ({@link Tolerance#MODEL}). */
    public static final double TOLERANCE = Tolerance.MODEL;
    /**
     * Maximum difference per channel (from 0 to 255) between two pixels that are considered equal. The
     * antialiasing of PDFBox changes from one machine to another: between two computers up to 30
     * pixels per image have been seen with differences of up to 10, at the corners of the outlines. A real
     * layout change moves lines and text, with differences close to 255.
     */
    public static final int PIXEL_TOLERANCE = 16;

    /** Where the PDF and the images of each case are written: {@code target/reference/<group>/<case>/}. */
    public static final Path ACTUAL_ROOT = Path.of("target", "reference");

    private static final Pattern PAGE_IMAGE = Pattern.compile("page-(\\d+)\\.png");

    private ReferenceFiles() {
    }

    /**
     * Writes to {@code dir} (in {@code target/}) the PDF as {@code output.pdf} and one {@code page-N.png} per page at
     * the given resolution, and deletes those of pages that no longer exist.
     *
     * @return the images of the pages, in order
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
     * Compares the images of a case with the {@code page-N.png} versioned in {@code reference}. With the property
     * {@code update} set to {@code true} it rewrites them instead: only those that change (so that git only sees real
     * changes), and it deletes those of pages that no longer exist.
     *
     * @param actual where {@link #writeActual} left the new images, for the message
     * @throws AssertionError if any page changes, is missing or is extra, and no update is requested
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
                problems.add(page(i + 1) + (old == null ? " is not versioned"
                        : differences < 0 ? " changes size" : " changes in " + differences + " pixels"));
            }
        }
        for (Path extra : pageImages(reference)) {
            if (pageNumber(extra) > images.size()) {
                if (updating) {
                    Files.delete(extra);
                } else {
                    problems.add(extra.getFileName() + " is extra: the PDF has " + images.size() + " pages");
                }
            }
        }
        if (!problems.isEmpty()) {
            fail(reference + ": " + String.join("; ", problems) + ". The new images are in " + actual
                    + ". If the change is the intended one, once they are reviewed it is accepted with -D" + update + "=true");
        }
    }

    private static String page(int number) {
        return "page-" + number + ".png";
    }

    /** The {@code page-N.png} files of a folder. */
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
     * Pixels that differ in some channel by more than {@link #PIXEL_TOLERANCE}; -1 if the images do not have the same
     * size.
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

    /** Compares two dumps: equal names and values, and the {@code Rect} values within {@link #TOLERANCE}. */
    public static void compare(Node expected, Node actual, String path) {
        String here = path + expected.getName();
        assertEquals(expected.getCanonicalName(), actual.getCanonicalName(), here);
        if (expected.getCanonicalName().equals("rect")) {
            double[] e = numbers(expected.getText());
            double[] a = numbers(actual.getText());
            assertEquals(4, a.length, here);
            for (int i = 0; i < 4; i++) {
                assertEquals(e[i], a[i], TOLERANCE, here + " (component " + i + ")");
            }
        } else {
            assertEquals(expected.getText().trim(), actual.getText().trim(), here);
        }
        List<Node> e = children(expected);
        List<Node> a = children(actual);
        assertEquals(e.size(), a.size(), here + ": number of children");
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
