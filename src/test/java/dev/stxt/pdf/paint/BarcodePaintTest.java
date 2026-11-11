package dev.stxt.pdf.paint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureNode;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.common.HybridBinarizer;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.stxt.DocumentReader;

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
        return read(pdf, format, false);
    }

    /**
     * What ZXing reads on the first page, drawn at 150 pixels per inch.
     *
     * @param gs1 whether the code is read as a GS1 one: an FNC1 between two values is a group separator in the text
     */
    private static Result read(byte[] pdf, BarcodeFormat format, boolean gs1) throws Exception {
        BufferedImage image;
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            image = new PDFRenderer(doc).renderImageWithDPI(0, 150);
        }
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(
                new RGBLuminanceSource(image.getWidth(), image.getHeight(), pixels)));
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, List.of(format));
        if (gs1) {
            hints.put(DecodeHintType.ASSUME_GS1, Boolean.TRUE);
        }
        Result result = new MultiFormatReader().decode(bitmap, hints);
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
                "\t\tBarcode format: code-128", "\t\tBox:", "\t\t\tWidth: 240", "\t\t\tHeight: 50"), BarcodeFormat.CODE_128).getText());
        assertEquals("8412345678905", read(pdf("\tBarcode: 841234567890", "\t\tAlt: Producto",
                "\t\tBarcode format: ean-13", "\t\tBox:", "\t\t\tWidth: 160"), BarcodeFormat.EAN_13).getText());
    }

    @Test
    void aDataMatrixAndAPdf417AreReadFromThePage() throws Exception {
        byte[] matrix = pdf("\tBarcode: Lote L-2025-0388, Peña Ceràmica", "\t\tAlt: Lote",
                "\t\tBarcode format: data-matrix", "\t\tBox:", "\t\t\tWidth: 100", "\t\t\tMargin: 80");
        assertEquals("Lote L-2025-0388, Peña Ceràmica", read(matrix, BarcodeFormat.DATA_MATRIX).getText());
        byte[] stacked = pdf("\tBarcode: M1NOGAL/CARMEN EABC123 BCNMADIB 2714 320Y012A0042 100", "\t\tAlt: Tarjeta",
                "\t\tBarcode format: pdf417");
        assertEquals("M1NOGAL/CARMEN EABC123 BCNMADIB 2714 320Y012A0042 100",
                read(stacked, BarcodeFormat.PDF_417).getText());
    }

    // The detector of ZXing looks for a Data Matrix from the center of the image: the margin centers it in the page.

    /** The GS1 symbols say they are GS1 (the FNC1 in the first position) and separate the values with a group separator. */
    @Test
    void theGs1CodesAreReadFromThePage() throws Exception {
        String data = "(01)09501101530003(10)AB-123(17)261231";
        String read = "0109501101530003" + "10AB-123" + (char) 0x1D + "17261231";
        Result linear = read(pdf("\tBarcode: " + data, "\t\tAlt: Etiqueta", "\t\tBarcode format: gs1-128",
                "\t\tBox:", "\t\t\tWidth: 250"), BarcodeFormat.CODE_128, true);
        // Read as GS1, the text starts with the symbology identifier.
        assertEquals("]C1" + read, linear.getText());
        assertEquals("]C1", linear.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));
        Result matrix = read(pdf("\tBarcode: " + data, "\t\tAlt: Etiqueta", "\t\tBarcode format: gs1-data-matrix",
                "\t\tBox:", "\t\t\tWidth: 100", "\t\t\tMargin: 80"), BarcodeFormat.DATA_MATRIX);
        // The decoder of a Data Matrix writes the first FNC1 too.
        assertEquals((char) 0x1D + read, matrix.getText());
        assertEquals("]d2", matrix.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));
    }

    /** With its readable text, a barcode is still read, and the text is in the page, in the figure. */
    @Test
    void theReadableTextGoesUnderTheBars() throws Exception {
        byte[] ean = pdf("\tBarcode: 8412345678905", "\t\tAlt: Producto", "\t\tBarcode format: ean-13",
                "\t\tReadable text: true", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans", "\t\t\tSize: 9");
        assertEquals("8412345678905", read(ean, BarcodeFormat.EAN_13).getText());
        assertEquals("8412345678905", text(ean));
        byte[] gs1 = pdf("\tBarcode: (00)395011015300000011", "\t\tAlt: Bulto", "\t\tBarcode format: gs1-128",
                "\t\tReadable text: true", "\t\tBox:", "\t\t\tWidth: 200", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans", "\t\t\tSize: 8");
        assertEquals("00395011015300000011", read(gs1, BarcodeFormat.CODE_128).getText());
        assertEquals("(00)395011015300000011", text(gs1));
        try (PDDocument doc = Loader.loadPDF(gs1)) {
            List<PDStructureElement> figures = new ArrayList<>();
            collect(doc.getDocumentCatalog().getStructureTreeRoot(), "Figure", figures);
            assertEquals(1, figures.size());
            assertEquals("Bulto", figures.get(0).getAlternateDescription());
        }
    }

    /** The text of the first page, without the white space that the extraction puts between its pieces. */
    private static String text(byte[] pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc).replaceAll("\\s", "");
        }
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
