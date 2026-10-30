package dev.stxt.pdf.paint;

import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.golden.PdfUa;
import dev.stxt.pdf.stxt.DocumentReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La página de un PDF como imagen (referencia/componentes/image.md): un formulario con su contenido, su texto y su
 * giro, sin el etiquetado de su PDF, que se etiqueta como la {@code Image} que lo dibuja.
 */
class PdfPagesTest {

    /** Dónde acaba un punto del recorte con la matriz de la página girada. */
    private static Point2D at(AffineTransform m, double x, double y) {
        return m.transform(new Point2D.Double(x, y), null);
    }

    private static void assertPoint(double x, double y, Point2D p) {
        assertEquals(x, p.getX(), 1e-9, "x de " + p);
        assertEquals(y, p.getY(), 1e-9, "y de " + p);
    }

    /**
     * La matriz lleva el recorte (aquí, 200 × 100 en (10, 20)) a un rectángulo con la esquina en el origen, girado como
     * lo enseña un visor: con 90°, la esquina de arriba a la izquierda pasa a la de arriba a la derecha.
     */
    @Test
    void placementTurnsTheCropToTheOrigin() {
        PDRectangle crop = new PDRectangle(10, 20, 200, 100);
        AffineTransform none = PdfPages.placement(crop, 0);
        assertPoint(0, 0, at(none, 10, 20));
        assertPoint(200, 100, at(none, 210, 120));

        // Se ve de 100 × 200.
        AffineTransform quarter = PdfPages.placement(crop, 90);
        assertPoint(100, 200, at(quarter, 10, 120));
        assertPoint(0, 200, at(quarter, 10, 20));
        assertPoint(0, 0, at(quarter, 210, 20));
        assertPoint(100, 0, at(quarter, 210, 120));

        AffineTransform half = PdfPages.placement(crop, 180);
        assertPoint(200, 100, at(half, 10, 20));
        assertPoint(0, 0, at(half, 210, 120));

        // Al revés que con 90°: la esquina de arriba a la izquierda pasa a la de abajo a la izquierda.
        AffineTransform threeQuarters = PdfPages.placement(crop, 270);
        assertPoint(0, 0, at(threeQuarters, 10, 120));
        assertPoint(100, 0, at(threeQuarters, 10, 20));
        assertPoint(100, 200, at(threeQuarters, 210, 20));
    }

    private static final String DOCUMENT = String.join("\n",
            "Document (dev.stxt.pdf):",
            "\tLanguage: es",
            "\tTitle: Prueba",
            "\tPage:",
            "\t\tWidth: 400",
            "\t\tHeight: 300",
            "\t\tMargin: 40",
            "\tStyles:",
            "\t\tFor element: Document",
            "\t\t\tTypography:",
            "\t\t\t\tFont: IBM Plex Sans",
            "\t\t\t\tSize: 10",
            "\tSection:",
            "\t\tPage overlay:",
            "\t\t\tOffset: -40 -40 -40 -40",
            "\t\t\tLayer: -1",
            "\t\t\tImage: examples/letterhead/letterhead.pdf",
            "\t\t\t\tPdf page: 2",
            "\t\t\t\tFit: fill",
            "\t\t\t\tBox:",
            "\t\t\t\t\tWidth: 100%",
            "\t\t\t\t\tHeight: 100%",
            "\t\tImage: test/pages.pdf",
            "\t\t\tPdf page: 3",
            "\t\t\tAlt: La página 3 de la prueba",
            "\t\tImage: test/pages.pdf",
            "\t\t\tPdf page: 3",
            "\t\t\tBreak:",
            "\t\t\t\tBefore: page",
            "");

    private static byte[] pdf(int ua) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.builder().pdfUa(ua).build().toPdf(new DocumentReader().read(DOCUMENT), out);
        Path dir = Path.of("target", "pdf");
        Files.createDirectories(dir);
        Files.write(dir.resolve("pdf-pages-ua" + ua + ".pdf"), out.toByteArray());
        return out.toByteArray();
    }

    private static List<PDFormXObject> forms(PDResources resources) throws Exception {
        List<PDFormXObject> forms = new ArrayList<>();
        for (COSName name : resources.getXObjectNames()) {
            forms.add((PDFormXObject) resources.getXObject(name));
        }
        return forms;
    }

    /**
     * La página va vectorial y con su texto, que se puede extraer; se copia una vez aunque salga en dos páginas; y el
     * etiquetado de su PDF (el papel de carta es un PDF etiquetado del propio motor) no llega al documento, que sigue
     * siendo PDF/UA-1 y PDF/UA-2.
     */
    @Test
    void aPageIsAFormWithItsTextAndWithoutItsTags() throws Exception {
        byte[] bytes = pdf(1);
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            assertEquals(2, pdf.getNumberOfPages());
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("Página 3"), text);
            assertTrue(text.contains("Taller Nogal S.L."), text);

            List<PDFormXObject> first = forms(pdf.getPage(0).getResources());
            assertEquals(2, first.size());
            PDFormXObject turned = first.stream().filter(f -> f.getBBox().getWidth() == 200).findFirst().orElseThrow();
            List<PDFormXObject> second = forms(pdf.getPage(1).getResources());
            assertEquals(turned.getCOSObject(), second.stream().filter(f -> f.getBBox().getWidth() == 200)
                    .findFirst().orElseThrow().getCOSObject(), "La misma página en dos páginas se copia una vez");

            for (PDFormXObject form : first) {
                String content = new String(form.getContentStream().toByteArray(), StandardCharsets.ISO_8859_1);
                assertFalse(content.contains("MCID"), content);
                assertFalse(content.contains("BDC") || content.contains("BMC"), content);
                assertFalse(form.getCOSObject().containsKey(COSName.STRUCT_PARENTS));
            }
            String page = new String(pdf.getPage(0).getContents().readAllBytes(), StandardCharsets.ISO_8859_1);
            assertTrue(page.contains("/Figure <</MCID 0>> BDC"), page);

            // La página 3 está girada: su franja verde es la de arriba de la caja (40, 40, 100 × 200).
            BufferedImage image = new PDFRenderer(pdf).renderImageWithDPI(0, 72);
            int green = image.getRGB(90, 45) & 0xFFFFFF;
            assertTrue((green >> 8 & 0xFF) > (green >> 16 & 0xFF) + 40, Integer.toHexString(green));
            int paper = image.getRGB(90, 150) & 0xFFFFFF;
            assertEquals(0xF4F1EA, paper, Integer.toHexString(paper));
        }
        assertEquals(Map.of(), PdfUa.failedRules(bytes));
        assertEquals(Map.of(), PdfUa.failedRules(pdf(2), 2));
    }

    /**
     * Las capas del PDF de origen (su contenido opcional) llegan al documento y siguen decidiendo lo que se ve; su
     * configuración, que no tenía nombre, se llama como el fichero, porque PDF/UA-1 lo pide.
     */
    @Test
    void theLayersOfThePageKeepTheirMarksAndGetAName() throws Exception {
        String doc = DOCUMENT.substring(0, DOCUMENT.indexOf("\t\tPage overlay:")) + "\t\tImage: test/layers.pdf\n"
                + "\t\t\tAlt: Una página con una capa de notas\n";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.create().toPdf(new DocumentReader().read(doc), out);
        byte[] bytes = out.toByteArray();
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            assertEquals("layers.pdf", pdf.getDocumentCatalog().getOCProperties().getCOSObject()
                    .getCOSDictionary(COSName.D).getString(COSName.NAME));
            assertTrue(new PDFTextStripper().getText(pdf).contains("Capa de notas"));
            PDFormXObject form = forms(pdf.getPage(0).getResources()).get(0);
            String content = new String(form.getContentStream().toByteArray(), StandardCharsets.ISO_8859_1);
            assertTrue(content.contains("/OC /"), content);
        }
        assertEquals(Map.of(), PdfUa.failedRules(bytes));
        out.reset();
        Converter.builder().pdfUa(2).build().toPdf(new DocumentReader().read(doc), out);
        assertEquals(Map.of(), PdfUa.failedRules(out.toByteArray(), 2));
    }
}
