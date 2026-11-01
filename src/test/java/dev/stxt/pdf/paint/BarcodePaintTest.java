package dev.stxt.pdf.paint;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.stxt.DocumentReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureNode;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A {@code Barcode} in the PDF (reference/components/barcode.md): the page is drawn to pixels and ZXing reads back the
 * data from them, as a phone would read the printed page. It is a figure of the tagged PDF with its {@code Alt}.
 */
class BarcodePaintTest {

    @BeforeAll
    static void headless() {
        System.setProperty("java.awt.headless", "true");
    }

    private static byte[] pdf(String... body) throws Exception {
        String document = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n"
                + "\t\tHeight: 300\n\t\tMargin: 20\n" + String.join("\n", body);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.create().toPdf(new DocumentReader().read(Docs.sectioned(document)), out);
        return out.toByteArray();
    }

    /** What ZXing reads on the first page, drawn at 150 pixels per inch. */
    private static Result read(byte[] pdf, BarcodeFormat format) throws Exception {
        BufferedImage image;
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            image = new PDFRenderer(doc).renderImageWithDPI(0, 150);
        }
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(
                new RGBLuminanceSource(image.getWidth(), image.getHeight(), pixels)));
        Result result = new MultiFormatReader().decode(bitmap, Map.of(DecodeHintType.TRY_HARDER, Boolean.TRUE,
                DecodeHintType.POSSIBLE_FORMATS, List.of(format)));
        assertEquals(format, result.getBarcodeFormat());
        return result;
    }

    @Test
    void aQrCodeIsReadFromThePage() throws Exception {
        byte[] pdf = pdf("\tBarcode: https://stxt.dev/verify/2025-0187", "\t\tAlt: Verificación del certificado",
                "\t\tBox:", "\t\t\tWidth: 120");
        assertEquals("https://stxt.dev/verify/2025-0187", read(pdf, BarcodeFormat.QR_CODE).getText());
    }

    /** Colored modules on a background, inside rounded corners and rotated: still a QR code that is read. */
    @Test
    void aColoredQrCodeOnABackgroundIsRead() throws Exception {
        byte[] pdf = pdf("\tBarcode: Peña Ceràmica, 120 €", "\t\tAlt: Datos", "\t\tError correction: quartile",
                "\t\tRotate: 10", "\t\tBox:", "\t\t\tWidth: 120", "\t\t\tPadding: 6", "\t\t\tCorner radius: 6",
                "\t\t\tBackground: #EEF2F7", "\t\tTypography:", "\t\t\tColor: #1F3A5F");
        assertEquals("Peña Ceràmica, 120 €", read(pdf, BarcodeFormat.QR_CODE).getText());
    }

    @Test
    void code128AndEan13AreReadFromThePage() throws Exception {
        assertEquals("STXT-2026-0042", read(pdf("\tBarcode: STXT-2026-0042", "\t\tAlt: Referencia",
                "\t\tFormat: code-128", "\t\tBox:", "\t\t\tWidth: 240", "\t\t\tHeight: 50"), BarcodeFormat.CODE_128).getText());
        assertEquals("8412345678905", read(pdf("\tBarcode: 841234567890", "\t\tAlt: Producto",
                "\t\tFormat: ean-13", "\t\tBox:", "\t\t\tWidth: 160"), BarcodeFormat.EAN_13).getText());
    }

    @Test
    void itIsAFigureWithItsAlt() throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf("\tBarcode: https://stxt.dev", "\t\tAlt: Enlace a stxt.dev",
                "\t\tBox:", "\t\t\tWidth: 60"))) {
            List<PDStructureElement> figures = new ArrayList<>();
            collect(doc.getDocumentCatalog().getStructureTreeRoot(), "Figure", figures);
            assertEquals(1, figures.size());
            assertEquals("Enlace a stxt.dev", figures.get(0).getAlternateDescription());
            assertTrue(figures.get(0).getKids().size() > 0);
        }
    }

    private static void collect(PDStructureNode node, String type, List<PDStructureElement> found) {
        for (Object kid : node.getKids()) {
            if (kid instanceof PDStructureElement e) {
                if (type.equals(e.getStructureType())) {
                    found.add(e);
                }
                collect(e, type, found);
            }
        }
    }
}
