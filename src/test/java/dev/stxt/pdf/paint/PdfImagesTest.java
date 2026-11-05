package dev.stxt.pdf.paint;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.Pdfs;
import dev.stxt.pdf.model.Limits;
import dev.stxt.pdf.resources.Resources;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDLayoutAttributeObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static dev.stxt.pdf.PdfStructures.section;
import static dev.stxt.pdf.PdfStructures.types;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The images embedded in the PDF (reference/components/image.md): each file once, the PNG with its transparency and
 * the JPEG as is, and the SVG as one vector form scaled to its box. The text of an SVG is drawn with the file from
 * {@code fonts/} that was checked when reading it, without JVM state: neither the default language nor another
 * resources folder with the same family changes it.
 */
class PdfImagesTest {

    private static final Path FONTS = Resources.DEFAULT_ROOT.resolve("fonts");
    private static final String NS = "xmlns=\"http://www.w3.org/2000/svg\"";

    @TempDir
    Path dir;

    @BeforeAll
    static void headless() {
        System.setProperty("java.awt.headless", "true");
    }

    /** The content of the form in which the SVG {@code images/name} of the resources is drawn. */
    private static byte[] draw(Resources resources, String name) throws IOException {
        try (PDDocument doc = new PDDocument(); PdfImages pdfImages = new PdfImages(doc, Limits.DEFAULT);) {
            return pdfImages.drawing(resources.images().find(name))
                    .getContentStream().toByteArray();
        }
    }

    /** A resources folder with those fonts from {@code resources/fonts/} and an SVG {@code image.svg}. */
    private Resources folder(String name, String svg, String... fonts) throws IOException {
        Path root = dir.resolve(name);
        Files.createDirectories(root.resolve("fonts/f"));
        Files.createDirectories(root.resolve("images"));
        for (String font : fonts) {
            Path file = FONTS.resolve(font);
            Files.copy(file, root.resolve("fonts/f").resolve(file.getFileName()));
        }
        Files.writeString(root.resolve("images/image.svg"), svg, StandardCharsets.UTF_8);
        return new Resources(root);
    }

    private static String text(String family) {
        return "<svg " + NS + " viewBox=\"0 0 120 30\"><text x=\"4\" y=\"20\" font-family=\"" + family
                + "\" font-size=\"16\">Nogal fina</text></svg>";
    }

    @Tag("slow")
    @Test
    void theLanguageOfTheJvmDoesNotChangeTheText() throws Exception {
        // In a JVM that starts in Turkish, AWT does not find "IBM Plex Sans" by its name (the capital I becomes ı) and
        // JSVG drew with Dialog. Changing the Locale at runtime does not reproduce it: each drawing runs in its JVM.
        assertEquals(drawIn("en", "US"), drawIn("tr", "TR"));
    }

    /** The fingerprint of the drawing of {@code test/label.svg} in a new JVM with that language. */
    private static String drawIn(String language, String country) throws Exception {
        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        Process process = new ProcessBuilder(java.toString(), "-Djava.awt.headless=true",
                "-Duser.language=" + language, "-Duser.country=" + country,
                "-cp", System.getProperty("java.class.path"), Draw.class.getName(), "test/label.svg")
                .redirectErrorStream(true).start();
        String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), out);
        return out.lines().filter(l -> l.startsWith("sha-256 ")).findFirst().orElseThrow(() -> new AssertionError(out));
    }

    /** Draws an SVG from {@code resources/images/} and prints the fingerprint of the form. */
    static final class Draw {
        public static void main(String[] args) throws Exception {
            byte[] content = draw(new Resources(), args[0]);
            System.out.println("sha-256 " + java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256").digest(content)) + " " + content.length);
        }
    }

    /**
     * An image with {@code Alt} is a {@code Figure} with its alternative text and its {@code BBox}; without it, and in
     * a {@code Page overlay}, it is an artifact (reference/components/image.md). Each file is embedded once.
     */
    @Test
    void imagesAreFiguresOrArtifactsAndAreEmbeddedOnce() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(Docs.header(300, 300, 40) + Docs.TYPOGRAPHY
                + "\tPage overlay:\n\t\tOffset: -30 -30 auto auto\n\t\tImage: test/square.png\n"
                + "\tImage: test/wide.png\n\t\tAlt: Paisaje\n"
                + "\tImage: test/tall.jpg\n\t\tBox:\n\t\t\tHeight: 60\n"
                + "\tParagraph: Fin.\n"
                + "\tImage: test/wide.png\n\t\tBreak:\n\t\t\tBefore: page\n"))) {
            assertEquals(2, pdf.getNumberOfPages());
            PDStructureElement document = section(pdf);
            assertEquals(List.of("Figure", "P"), types(document));
            PDStructureElement figure = (PDStructureElement) document.getKids().get(0);
            assertEquals("Paisaje", figure.getAlternateDescription());
            PDLayoutAttributeObject layout = (PDLayoutAttributeObject) figure.getAttributes().getObject(0);
            PDRectangle box = layout.getBBox();
            // The content box of the image: 150 × 75 at (40, 40), with the origin at the bottom.
            assertEquals(40, box.getLowerLeftX(), 0.01);
            assertEquals(300 - 40 - 75, box.getLowerLeftY(), 0.01);
            assertEquals(150, box.getWidth(), 0.01);

            List<PDImageXObject> first = images(pdf, 0);
            assertEquals(3, first.size());
            PDImageXObject wide = first.stream().filter(i -> i.getWidth() == 200).findFirst().orElseThrow();
            // The PNG keeps its transparency; the JPEG goes as is.
            assertTrue(wide.getSoftMask() != null);
            assertTrue(first.stream().anyMatch(i -> i.getHeight() == 160 && "jpg".equals(i.getSuffix())));
            List<PDImageXObject> second = images(pdf, 1);
            assertTrue(second.stream().anyMatch(i -> i.getCOSObject() == wide.getCOSObject()),
                    "The same image on two pages is embedded once");

            String content = new String(pdf.getPage(0).getContents().readAllBytes(), StandardCharsets.ISO_8859_1);
            assertTrue(content.contains("/Figure <</MCID 0>> BDC"), content);
        }
    }

    private static List<PDImageXObject> images(PDDocument pdf, int page) throws IOException {
        PDResources resources = pdf.getPage(page).getResources();
        List<PDImageXObject> images = new ArrayList<>();
        for (COSName name : resources.getXObjectNames()) {
            images.add((PDImageXObject) resources.getXObject(name));
        }
        return images;
    }

    /**
     * An SVG (reference/components/image.md) is a vector drawing: a PDF form, not an image, which is embedded once
     * even if used on two pages and is scaled to its box; with {@code Alt} it is a {@code Figure}. The text of the
     * SVG is drawn with the font from {@code fonts/}: the pixels of the letters are where they belong (white on the
     * dark background).
     */
    @Test
    void svgIsDrawnAsOneVectorFormAndScaledToItsBox() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(Docs.header(400, 300, 40) + Docs.TYPOGRAPHY
                + "\tImage: test/shapes.svg\n\t\tAlt: Un paisaje\n\t\tBox:\n\t\t\tWidth: 300\n"
                + "\tImage: test/label.svg\n\t\tBox:\n\t\t\tMargin: 10 0 0 0\n"
                + "\tImage: test/shapes.svg\n\t\tBreak:\n\t\t\tBefore: page\n"))) {
            assertEquals(2, pdf.getNumberOfPages());
            PDResources first = pdf.getPage(0).getResources();
            List<PDFormXObject> forms = new ArrayList<>();
            for (COSName name : first.getXObjectNames()) {
                assertInstanceOf(PDFormXObject.class, first.getXObject(name), "An SVG is not a pixel image");
                forms.add((PDFormXObject) first.getXObject(name));
            }
            assertEquals(2, forms.size());
            PDResources second = pdf.getPage(1).getResources();
            PDFormXObject shapes = forms.stream().filter(f -> f.getBBox().getWidth() == 150).findFirst().orElseThrow();
            assertTrue(second.getXObjectNames().iterator().hasNext());
            assertEquals(shapes.getCOSObject(), second.getXObject(second.getXObjectNames().iterator().next())
                    .getCOSObject(), "The same SVG on two pages is embedded once");
            assertEquals(75, shapes.getBBox().getHeight(), 1e-3);

            // 300 pt wide for a natural 150: it is scaled by two, from the bottom corner of the box.
            String content = new String(pdf.getPage(0).getContents().readAllBytes(), StandardCharsets.ISO_8859_1);
            assertTrue(content.matches("(?s).*\\b2 0 0 2 40 \\S+ cm\\b.*"), content);
            assertTrue(content.contains("/Figure <</MCID 0>> BDC"), content);
            // The form inherits the graphics state (like the dashes of debug mode): it starts from scratch.
            assertTrue(content.contains("[] 0 d"), content);

            // Vector content carries paths, not an image; the text of label.svg is visible (white letters on #3b2a1e).
            String shapesContent = new String(shapes.getContentStream().createInputStream().readAllBytes(),
                    StandardCharsets.ISO_8859_1);
            assertTrue(shapesContent.contains(" c\n") || shapesContent.contains(" l\n"), shapesContent);
            BufferedImage image = new PDFRenderer(pdf).renderImageWithDPI(0, 72);
            int label = 40 + 150 + 10;
            int whites = 0;
            for (int y = label; y < label + 36; y++) {
                for (int x = 40; x < 40 + 120; x++) {
                    int rgb = image.getRGB(x, y) & 0xFFFFFF;
                    if ((rgb >> 16 & 0xFF) > 230 && (rgb >> 8 & 0xFF) > 230 && (rgb & 0xFF) > 230) {
                        whites++;
                    }
                }
            }
            assertTrue(whites > 30, "The letters of label.svg have to be visible: " + whites);
        }
    }

    @Test
    void twoFoldersWithTheSameFamilyDrawEachItsOwnFile() throws IOException {
        Resources sans = folder("sans", text("IBM Plex Sans"), "IBM Plex Sans/IBMPlexSans-Regular.ttf");
        // Another folder with an "IBM Plex Sans" that is actually IBM Plex Serif with the name table changed.
        Resources fake = folder("fake", text("IBM Plex Sans"));
        Path serif = FONTS.resolve("IBM Plex Serif/IBMPlexSerif-Regular.ttf");
        Files.write(dir.resolve("fake/fonts/f/Fake-Regular.ttf"), renamed(Files.readAllBytes(serif), "IBM Plex Sans"));
        Resources real = folder("serif", text("IBM Plex Serif"), "IBM Plex Serif/IBMPlexSerif-Regular.ttf");

        byte[] first = draw(sans, "image.svg");
        byte[] second = draw(fake, "image.svg");
        assertFalse(java.util.Arrays.equals(first, second), "the second folder is drawn with the font of the first");
        assertArrayEquals(draw(real, "image.svg"), second);
        assertArrayEquals(first, draw(sans, "image.svg"));
    }

    @Test
    void theFontOfStyleAndOfTheAncestorsIsTheOneDrawn() throws IOException {
        String declared = "<svg " + NS + " viewBox=\"0 0 200 60\">"
                + "<text x=\"4\" y=\"20\" style=\"font-size: 14px; font-family: 'IBM Plex Serif'; font-weight: bold\">"
                + "Serif</text>"
                + "<g font-family=\"IBM Plex Mono\" style=\"font-style: italic\"><text x=\"4\" y=\"40\" font-size=\"14\">"
                + "Mono <tspan font-weight=\"700\">negrita</tspan> fin</text></g></svg>";
        String explicit = "<svg " + NS + " viewBox=\"0 0 200 60\">"
                + "<text x=\"4\" y=\"20\" font-size=\"14\" font-family=\"IBM Plex Serif\" font-weight=\"bold\">Serif</text>"
                + "<g><text x=\"4\" y=\"40\" font-size=\"14\" font-family=\"IBM Plex Mono\" font-style=\"italic\">"
                + "Mono <tspan font-family=\"IBM Plex Mono\" font-weight=\"bold\" font-style=\"italic\">negrita</tspan>"
                + " fin</text></g></svg>";
        String[] fonts = {"IBM Plex Serif/IBMPlexSerif-Bold.ttf", "IBM Plex Mono/IBMPlexMono-Italic.ttf",
                "IBM Plex Mono/IBMPlexMono-BoldItalic.ttf"};
        byte[] a = draw(folder("declared", declared, fonts), "image.svg");
        byte[] b = draw(folder("explicit", explicit, fonts), "image.svg");
        assertArrayEquals(b, a);
        assertTrue(a.length > 1000, "the text is drawn as outlines");
    }

    @Test
    void aTextRepeatedByAUseKeepsTheFontOfItsPlaceInTheFile() throws IOException {
        // The text is checked with the font of its place in the file (Regular), and that is the one drawn even though
        // the <use> is inside a bold group (reference/components/image.md).
        String used = "<svg " + NS + " viewBox=\"0 0 120 30\"><defs><text id=\"t\" x=\"4\" y=\"20\" "
                + "font-family=\"IBM Plex Sans\" font-size=\"16\">Nogal</text></defs>"
                + "<g font-weight=\"bold\"><use href=\"#t\"/></g></svg>";
        String regular = used.replace("<g font-weight=\"bold\">", "<g>");
        String[] fonts = {"IBM Plex Sans/IBMPlexSans-Regular.ttf"};
        assertArrayEquals(draw(folder("regular", regular, fonts), "image.svg"),
                draw(folder("used", used, fonts), "image.svg"));
    }

    /**
     * A copy of the font whose {@code name} table says it belongs to another family (Regular): the new table goes at
     * the end of the file and the directory entry points to it. It does not recompute the checksums, which are not
     * checked.
     */
    static byte[] renamed(byte[] ttf, String family) {
        String[][] names = {{"1", family}, {"2", "Regular"}, {"4", family + " Regular"},
                {"6", family.replace(" ", "") + "-Regular"}, {"16", family}, {"17", "Regular"}};
        ByteBuffer strings = ByteBuffer.allocate(1024);
        ByteBuffer table = ByteBuffer.allocate(6 + 12 * names.length + 1024);
        table.putShort((short) 0).putShort((short) names.length).putShort((short) (6 + 12 * names.length));
        for (String[] name : names) {
            byte[] value = name[1].getBytes(StandardCharsets.UTF_16BE);
            table.putShort((short) 3).putShort((short) 1).putShort((short) 0x409).putShort(Short.parseShort(name[0]))
                    .putShort((short) value.length).putShort((short) strings.position());
            strings.put(value);
        }
        table.put(strings.array(), 0, strings.position());
        int length = table.position();
        int offset = (ttf.length + 3) & ~3;
        ByteBuffer out = ByteBuffer.allocate(offset + length);
        out.put(ttf).position(offset);
        out.put(table.array(), 0, length);
        int tables = Short.toUnsignedInt(out.getShort(4));
        for (int i = 0; i < tables; i++) {
            int entry = 12 + 16 * i;
            if (out.getInt(entry) == 0x6E616D65) { // 'name'
                out.putInt(entry + 8, offset).putInt(entry + 12, length);
            }
        }
        return out.array();
    }
}
