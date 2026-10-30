import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.stxt.DocumentReader;
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
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Genera los PDF que usan las {@code Image} de la documentación y de los tests (referencia/componentes/image.md), para
 * no subir binarios sin origen:
 *
 * <ul>
 * <li>{@code resources/images/examples/letterhead/letterhead.pdf}: el papel de carta de Taller Nogal, maquetado con el
 * propio motor desde {@code tools/letterhead.stxt} (un PDF etiquetado, con fuentes incrustadas).</li>
 * <li>{@code resources/images/test/*.pdf}: los casos de los tests, escritos con PDFBox: páginas de tamaños distintos,
 * con recorte y giradas ({@code pages.pdf}), una fuente sin incrustar, un campo de formulario y un PDF cifrado.</li>
 * </ul>
 *
 * <p>Necesita las clases del motor y sus dependencias:</p>
 *
 * <pre>
 * mvn -q compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
 * java -cp "target/classes:$(cat target/classpath.txt)" tools/ExamplePdfs.java    # desde la raíz del repositorio
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
    }

    /** El papel de carta, con el motor. */
    static void letterhead() throws IOException {
        Path out = IMAGES.resolve("examples/letterhead/letterhead.pdf");
        Files.createDirectories(out.getParent());
        try (OutputStream target = Files.newOutputStream(out)) {
            Converter.create().toPdf(new DocumentReader().read(Path.of("tools", "letterhead.stxt")), target);
        }
    }

    /**
     * Tres páginas: la 1 de 200 × 100; la 2, de 300 × 300 con un recorte de 150 × 50 en (50, 100); la 3, de 200 × 100
     * girada 90°, que se ve de 100 × 200. Cada una dice su número y lleva una franja de color en su borde de arriba tal
     * como se ve.
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
                // Girada 90° en el sentido de las agujas del reloj, el borde izquierdo (x = 0) queda arriba.
                cs.setNonStrokingColor(new Color(0x4C8C4A));
                cs.addRect(0, 0, 10, 100);
                cs.fill();
                cs.beginText();
                cs.setFont(font, 14);
                cs.setNonStrokingColor(Color.BLACK);
                // El texto avanza hacia arriba (+y), que en la página girada es hacia la derecha: se lee derecho.
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

    /** Un fondo, una franja de 10 pt arriba y el texto, dentro del rectángulo visible de la página. */
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

    /** Un texto en Helvetica, una de las fuentes estándar que no se incrustan. */
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

    /** Un campo de texto relleno: su valor está en la anotación, no en el contenido de la página. */
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

    /** Un PDF que se abre sin contraseña pero está cifrado (con la de propietario). */
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
