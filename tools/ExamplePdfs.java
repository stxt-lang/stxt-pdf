import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.stxt.DocumentReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentGroup;
import org.apache.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentProperties;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates the PDFs used by the {@code Image} elements of the documentation and the tests
 * (reference/components/image.md), so that no binaries without a source are committed:
 *
 * <ul>
 * <li>{@code resources/images/examples/letterhead/letterhead.pdf}: the letterhead of Taller Nogal, laid out with the
 * engine itself from {@code tools/letterhead.stxt} (a tagged PDF, with embedded fonts).</li>
 * <li>{@code resources/images/test/*.pdf}: the test cases, written with PDFBox: pages of different sizes,
 * cropped and rotated ({@code pages.pdf}), a font not embedded, a font without {@code ToUnicode}, a form
 * field, an encrypted PDF and a page with optional content.</li>
 * </ul>
 *
 * <p>It needs the classes of the engine and its dependencies:</p>
 *
 * <pre>
 * mvn -q compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
 * java -cp "target/classes:$(cat target/classpath.txt)" tools/ExamplePdfs.java    # from the root of the repository
 * </pre>
 */
public class ExamplePdfs {

    static final Path IMAGES = Path.of("resources", "images");
    static final Path PLEX = Path.of("resources", "fonts", "IBM Plex Sans", "IBMPlexSans-Regular.ttf");

    public static void main(String[] args) throws IOException {
        System.setProperty("java.awt.headless", "true");
        letterhead();
        pages();
        standardFont();
        formField();
        encrypted();
        noUnicode();
        layers();
    }

    /** The letterhead, with the engine. */
    static void letterhead() throws IOException {
        Path out = IMAGES.resolve("examples/letterhead/letterhead.pdf");
        Files.createDirectories(out.getParent());
        try (OutputStream target = Files.newOutputStream(out)) {
            Converter.create().toPdf(new DocumentReader().read(Path.of("tools", "letterhead.stxt")), target);
        }
    }

    /**
     * Three pages: page 1, 200 × 100; page 2, 300 × 300 with a crop of 150 × 50 at (50, 100); page 3, 200 × 100
     * rotated 90°, which shows as 100 × 200. Each one says its number and carries a color band on its top edge as
     * it is seen.
     */
    static void pages() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDFont font = PDType0Font.load(doc, PLEX.toFile());
            PDPage first = page(doc, 200, 100);
            draw(doc, first, font, new Color(0x1B6E8C), 0, 0, 200, 100, "Página 1");
            PDPage cropped = page(doc, 300, 300);
            cropped.setCropBox(new PDRectangle(50, 100, 150, 50));
            draw(doc, cropped, font, new Color(0xE07B1F), 50, 100, 150, 50, "Página 2");
            PDPage turned = page(doc, 200, 100);
            turned.setRotation(90);
            try (PDPageContentStream cs = new PDPageContentStream(doc, turned)) {
                cs.setNonStrokingColor(new Color(0xF4F1EA));
                cs.addRect(0, 0, 200, 100);
                cs.fill();
                // Rotated 90° clockwise, the left edge (x = 0) ends up at the top.
                cs.setNonStrokingColor(new Color(0x4C8C4A));
                cs.addRect(0, 0, 10, 100);
                cs.fill();
                cs.beginText();
                cs.setFont(font, 14);
                cs.setNonStrokingColor(Color.BLACK);
                // The text advances upwards (+y), which on the rotated page is to the right: it reads upright.
                cs.setTextMatrix(new org.apache.pdfbox.util.Matrix(0, 1, -1, 0, 188, 10));
                cs.showText("Página 3");
                cs.endText();
            }
            doc.save(IMAGES.resolve("test/pages.pdf").toFile());
        }
    }

    private static PDPage page(PDDocument doc, float width, float height) {
        PDPage page = new PDPage(new PDRectangle(width, height));
        doc.addPage(page);
        return page;
    }

    /** A background, a 10 pt band at the top and the text, inside the visible rectangle of the page. */
    private static void draw(PDDocument doc, PDPage page, PDFont font, Color band, float x, float y, float w, float h,
            String text) throws IOException {
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            cs.setNonStrokingColor(new Color(0xF4F1EA));
            cs.addRect(x, y, w, h);
            cs.fill();
            cs.setNonStrokingColor(band);
            cs.addRect(x, y + h - 10, w, 10);
            cs.fill();
            cs.beginText();
            cs.setFont(font, 14);
            cs.setNonStrokingColor(Color.BLACK);
            cs.newLineAtOffset(x + 10, y + 12);
            cs.showText(text);
            cs.endText();
        }
    }

    /** A text in Helvetica, one of the standard fonts that are not embedded. */
    static void standardFont() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = page(doc, 200, 100);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14);
                cs.newLineAtOffset(10, 40);
                cs.showText("Helvetica");
                cs.endText();
            }
            doc.save(IMAGES.resolve("test/standard-font.pdf").toFile());
        }
    }

    /** A filled text field: its value is in the annotation, not in the content of the page. */
    static void formField() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = page(doc, 200, 100);
            PDAcroForm form = new PDAcroForm(doc);
            doc.getDocumentCatalog().setAcroForm(form);
            PDResources resources = new PDResources();
            resources.put(org.apache.pdfbox.cos.COSName.getPDFName("Helv"),
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA));
            form.setDefaultResources(resources);
            PDTextField field = new PDTextField(form);
            field.setPartialName("name");
            field.setDefaultAppearance("/Helv 12 Tf 0 g");
            form.getFields().add(field);
            PDAnnotationWidget widget = field.getWidgets().get(0);
            widget.setRectangle(new PDRectangle(10, 30, 180, 30));
            widget.setPage(page);
            page.getAnnotations().add(widget);
            field.setValue("Ramon Puig");
            doc.save(IMAGES.resolve("test/form-field.pdf").toFile());
        }
    }

    /**
     * A text in an embedded font whose {@code ToUnicode} is removed: it does not say which character each glyph is.
     */
    static void noUnicode() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = page(doc, 200, 100);
            draw(doc, page, PDType0Font.load(doc, PLEX.toFile()), new Color(0x1B6E8C), 0, 0, 200, 100, "Sin Unicode");
            doc.save(bytes);
        }
        try (PDDocument doc = Loader.loadPDF(bytes.toByteArray())) {
            PDResources resources = doc.getPage(0).getResources();
            for (COSName name : resources.getFontNames()) {
                resources.getFont(name).getCOSObject().removeItem(COSName.TO_UNICODE);
            }
            doc.save(IMAGES.resolve("test/no-unicode.pdf").toFile());
        }
    }

    /**
     * A page with a layer (optional content) turned on, a text that only appears in it, and a configuration of the
     * layers without {@code Name}, which PDF/UA requires.
     */
    static void layers() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDOptionalContentProperties properties = new PDOptionalContentProperties();
            PDOptionalContentGroup notes = new PDOptionalContentGroup("Notas");
            properties.addGroup(notes);
            properties.getCOSObject().getCOSDictionary(COSName.D).removeItem(COSName.NAME);
            doc.getDocumentCatalog().setOCProperties(properties);
            PDPage page = page(doc, 200, 100);
            PDFont font = PDType0Font.load(doc, PLEX.toFile());
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setNonStrokingColor(new Color(0xF4F1EA));
                cs.addRect(0, 0, 200, 100);
                cs.fill();
                cs.beginMarkedContent(COSName.OC, notes);
                cs.beginText();
                cs.setFont(font, 14);
                cs.setNonStrokingColor(Color.BLACK);
                cs.newLineAtOffset(10, 40);
                cs.showText("Capa de notas");
                cs.endText();
                cs.endMarkedContent();
            }
            doc.save(IMAGES.resolve("test/layers.pdf").toFile());
        }
    }

    /** A PDF that opens without a password but is encrypted (with the owner password). */
    static void encrypted() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = page(doc, 200, 100);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.addRect(10, 10, 50, 50);
                cs.fill();
            }
            StandardProtectionPolicy policy = new StandardProtectionPolicy("owner", "", new AccessPermission());
            policy.setEncryptionKeyLength(128);
            doc.protect(policy);
            doc.save(IMAGES.resolve("test/encrypted.pdf").toFile());
        }
    }
}
