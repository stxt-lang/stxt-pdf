package dev.stxt.pdf.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.oned.Code128Reader;
import com.google.zxing.oned.EAN13Reader;
import com.google.zxing.qrcode.decoder.Decoder;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.BarcodeText;
import dev.stxt.pdf.model.Modules;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.stxt.DocumentReader;

/**
 * The image of a {@code Barcode} (reference/components/barcode.md): the modules that are generated are read back with
 * the readers of ZXing, which decode the data, and the quiet zone, the natural size and the errors are those of the
 * reference.
 */
class BarcodesTest {

    private static Box barcode(String... lines) {
        String doc = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + String.join("\n", lines);
        return new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(doc))).sections().get(0).box()
                .children().get(0);
    }

    private static Modules modules(String... lines) {
        return barcode(lines).modules();
    }

    private static InvalidModelException error(String... lines) {
        return assertThrows(InvalidModelException.class, () -> barcode(lines));
    }

    /** The QR code without its quiet zone, as the decoder of ZXing reads it. */
    private static DecoderResult decodeQr(Modules m) throws Exception {
        int side = m.columns() - 2 * Barcodes.QR_QUIET_ZONE;
        BitMatrix matrix = new BitMatrix(side);
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                if (m.isDark(x + Barcodes.QR_QUIET_ZONE, y + Barcodes.QR_QUIET_ZONE)) {
                    matrix.set(x, y);
                }
            }
        }
        return new Decoder().decode(matrix);
    }

    private static BitArray row(Modules m) {
        BitArray row = new BitArray(m.columns());
        for (int x = 0; x < m.columns(); x++) {
            if (m.isDark(x, 0)) {
                row.set(x);
            }
        }
        return row;
    }

    @Test
    void aQrCodeIsReadBack() throws Exception {
        Box box = barcode("\tBarcode: https://stxt.dev", "\t\tAlt: Enlace");
        assertSame(Element.BARCODE, box.element());
        assertSame(Element.Role.FIGURE, box.role());
        assertTrue(box.isImage());
        assertNull(box.image());
        Modules m = box.modules();
        // 16 bytes do not fit in version 1 with medium correction (14): version 2, of 25 modules, and 4 on each side.
        assertEquals(33, m.columns());
        assertEquals(33, m.rows());
        DecoderResult result = decodeQr(m);
        assertEquals("https://stxt.dev", result.getText());
        assertEquals("M", result.getECLevel());
    }

    @Test
    void theQuietZoneIsLight() {
        Modules m = modules("\tBarcode: https://stxt.dev", "\t\tAlt: Enlace");
        for (int i = 0; i < m.columns(); i++) {
            for (int q = 0; q < Barcodes.QR_QUIET_ZONE; q++) {
                assertFalse(m.isDark(i, q));
                assertFalse(m.isDark(q, i));
                assertFalse(m.isDark(i, m.rows() - 1 - q));
                assertFalse(m.isDark(m.columns() - 1 - q, i));
            }
        }
        // The finder pattern starts right after it.
        assertTrue(m.isDark(Barcodes.QR_QUIET_ZONE, Barcodes.QR_QUIET_ZONE));
    }

    @Test
    void naturalSizeOfTheModules() {
        Modules qr = modules("\tBarcode: https://stxt.dev", "\t\tAlt: Enlace");
        assertEquals(33 * 0.5 * Value.POINTS_PER_MM, qr.naturalWidth(), 1e-9);
        assertEquals(qr.naturalWidth(), qr.naturalHeight(), 1e-9);
        Modules ean = modules("\tBarcode: 841234567890", "\t\tAlt: Producto", "\t\tBarcode format: ean-13");
        assertEquals(37.29 * Value.POINTS_PER_MM, ean.naturalWidth(), 1e-9);
        assertEquals(22.85 * Value.POINTS_PER_MM, ean.naturalHeight(), 1e-9);
    }

    /** The narrowest module of each format: 0.25 mm in QR and Code 128, and 80 % of 0.33 mm in EAN-13. */
    @Test
    void minimumModuleOfEachFormat() {
        assertEquals(0.25 * Value.POINTS_PER_MM, modules("\tBarcode: https://stxt.dev", "\t\tAlt: Enlace").minModule(),
                1e-9);
        assertEquals(0.25 * Value.POINTS_PER_MM,
                modules("\tBarcode: STXT", "\t\tAlt: Referencia", "\t\tBarcode format: code-128").minModule(), 1e-9);
        assertEquals(0.264 * Value.POINTS_PER_MM,
                modules("\tBarcode: 841234567890", "\t\tAlt: Producto", "\t\tBarcode format: ean-13").minModule(), 1e-9);
    }

    @Test
    void errorCorrectionChangesTheMatrix() throws Exception {
        Modules low = modules("\tBarcode: https://stxt.dev/pdf/barcode", "\t\tAlt: Enlace", "\t\tError correction: low");
        Modules high = modules("\tBarcode: https://stxt.dev/pdf/barcode", "\t\tAlt: Enlace", "\t\tError correction: high");
        assertTrue(high.columns() > low.columns());
        assertEquals("L", decodeQr(low).getECLevel());
        assertEquals("H", decodeQr(high).getECLevel());
        assertEquals("https://stxt.dev/pdf/barcode", decodeQr(high).getText());
        assertEquals("Q", decodeQr(modules("\tBarcode: A", "\t\tAlt: A", "\t\tError correction: quartile")).getECLevel());
    }

    @Test
    void textOutsideAsciiGoesInUtf8() throws Exception {
        String data = "Peña Ceràmica, 120 €";
        assertEquals(data, decodeQr(modules("\tBarcode: " + data, "\t\tAlt: Texto")).getText());
    }

    @Test
    void dataOfSeveralLinesGoesInContent() throws Exception {
        Modules m = modules("\tBarcode:", "\t\tAlt: Pago", "\t\tContent >>", "\t\t\tBCD", "\t\t\t002", "",
                "\t\t\t  sangría");
        assertEquals("BCD\n002\n\n  sangría", decodeQr(m).getText());
        InvalidModelException both = error("\tBarcode: A", "\t\tAlt: Pago", "\t\tContent >>", "\t\t\tB");
        assertEquals(ErrorCode.BARCODE_DATA, both.errorCode());
        assertTrue(both.getMessage().contains("in the value and in Content"), both.getMessage());
    }

    @Test
    void theSameDataGivesTheSameModules() {
        assertEquals(modules("\tBarcode: https://stxt.dev", "\t\tAlt: Enlace"),
                modules("\tBarcode: https://stxt.dev", "\t\tAlt: Otro"));
    }

    @Test
    void code128IsReadBack() throws Exception {
        Modules m = modules("\tBarcode: STXT-2026-0042", "\t\tAlt: Referencia", "\t\tBarcode format: code-128");
        assertEquals(1, m.rows());
        for (int q = 0; q < 10; q++) {
            assertFalse(m.isDark(q, 0));
            assertFalse(m.isDark(m.columns() - 1 - q, 0));
        }
        assertTrue(m.isDark(10, 0));
        assertEquals("STXT-2026-0042", new Code128Reader().decodeRow(0, row(m), Map.of()).getText());
    }

    @Test
    void ean13AddsOrChecksTheCheckDigit() throws Exception {
        Modules twelve = modules("\tBarcode: 841234567890", "\t\tAlt: Producto", "\t\tBarcode format: ean-13");
        Modules thirteen = modules("\tBarcode: 8412345678905", "\t\tAlt: Producto", "\t\tBarcode format: ean-13");
        assertEquals(twelve, thirteen);
        // 11 modules of quiet zone on the left, 95 of code and 7 on the right.
        assertEquals(113, twelve.columns());
        for (int q = 0; q < 11; q++) {
            assertFalse(twelve.isDark(q, 0));
        }
        assertTrue(twelve.isDark(11, 0));
        assertTrue(twelve.isDark(105, 0));
        for (int q = 106; q < 113; q++) {
            assertFalse(twelve.isDark(q, 0));
        }
        assertEquals("8412345678905", new EAN13Reader().decodeRow(0, row(twelve), Map.of()).getText());
    }

    @Test
    void quietZoneReplacesThatOfTheFormat() throws Exception {
        Modules standard = modules("\tBarcode: STXT-2026-0042", "\t\tAlt: Referencia", "\t\tBarcode format: code-128");
        Modules narrow = modules("\tBarcode: STXT-2026-0042", "\t\tAlt: Referencia", "\t\tBarcode format: code-128",
                "\t\tQuiet zone: 5");
        assertEquals(standard.columns() - 10, narrow.columns());
        for (int q = 0; q < 5; q++) {
            assertFalse(narrow.isDark(q, 0));
            assertFalse(narrow.isDark(narrow.columns() - 1 - q, 0));
        }
        assertTrue(narrow.isDark(5, 0));
        assertEquals("STXT-2026-0042", new Code128Reader().decodeRow(0, row(narrow), Map.of()).getText());
        assertEquals(narrow.columns() * Barcodes.LINEAR_MODULE, narrow.naturalWidth(), 1e-9);

        // In an EAN-13 the same number goes on both sides.
        Modules ean = modules("\tBarcode: 841234567890", "\t\tAlt: Producto", "\t\tBarcode format: ean-13",
                "\t\tQuiet zone: 5");
        assertEquals(105, ean.columns());
        assertTrue(ean.isDark(5, 0));
        assertTrue(ean.isDark(99, 0));
        assertFalse(ean.isDark(100, 0));
        assertEquals("8412345678905", new EAN13Reader().decodeRow(0, row(ean), Map.of()).getText());

        // A QR code without quiet zone starts with its finder pattern.
        Modules qr = modules("\tBarcode: https://stxt.dev", "\t\tAlt: Enlace", "\t\tQuiet zone: 0");
        assertEquals(25, qr.columns());
        assertEquals(25, qr.rows());
        assertTrue(qr.isDark(0, 0));
        assertTrue(qr.isDark(24, 0));
        assertTrue(qr.isDark(0, 24));
    }

    @Test
    void quietZoneOutOfRange() {
        InvalidModelException wide = error("\tBarcode: STXT", "\t\tAlt: Referencia", "\t\tBarcode format: code-128",
                "\t\tQuiet zone: 41");
        assertEquals(ErrorCode.VALUE_NOT_ALLOWED, wide.errorCode());
        assertTrue(wide.getMessage().contains("Quiet zone allows from 0 to 40 modules: 41"), wide.getMessage());
        assertEquals(ErrorCode.VALUE_NOT_ALLOWED, error("\tBarcode: STXT", "\t\tAlt: Referencia",
                "\t\tQuiet zone: -1").errorCode());
        assertEquals(40 * 2 + 25, modules("\tBarcode: https://stxt.dev", "\t\tAlt: Enlace", "\t\tQuiet zone: 40")
                .columns());
    }

    @Test
    void checkDigit() {
        assertEquals(5, Barcodes.checkDigit("841234567890"));
        assertEquals(1, Barcodes.checkDigit("400638133393"));
        assertEquals(0, Barcodes.checkDigit("000000000000"));
    }

    /** The limits of a Code 128: the space and ~ are the first and the last characters it encodes, and 80 of them fit. */
    @Test
    void code128EncodesThePrintableAsciiUpToEightyCharacters() {
        assertNotNull(modules("\tBarcode: ~ !", "\t\tAlt: Referencia", "\t\tBarcode format: code-128"));
        assertNotNull(modules("\tBarcode: " + "A".repeat(80), "\t\tAlt: Referencia", "\t\tBarcode format: code-128"));
    }

    @Test
    void dataThatCannotBeEncoded() {
        InvalidModelException empty = error("\tBarcode:", "\t\tAlt: Código");
        assertEquals(ErrorCode.BARCODE_DATA, empty.errorCode());
        InvalidModelException digit = error("\tBarcode: 8412345678901", "\t\tAlt: Producto", "\t\tBarcode format: ean-13");
        assertTrue(digit.getMessage().contains("The check digit of the EAN-13 8412345678901 is 5, not 1"),
                digit.getMessage());
        InvalidModelException letters = error("\tBarcode: 84123A", "\t\tAlt: Producto", "\t\tBarcode format: ean-13");
        assertTrue(letters.getMessage().contains("12 digits"), letters.getMessage());
        InvalidModelException ascii = error("\tBarcode: Peña", "\t\tAlt: Referencia", "\t\tBarcode format: code-128");
        assertTrue(ascii.getMessage().contains("'ñ' (U+00F1)"), ascii.getMessage());
        InvalidModelException longCode = error("\tBarcode: " + "A".repeat(81), "\t\tAlt: Referencia",
                "\t\tBarcode format: code-128");
        assertTrue(longCode.getMessage().contains("81 characters"), longCode.getMessage());
        InvalidModelException qr = error("\tBarcode: " + "x".repeat(3000), "\t\tAlt: Datos");
        assertTrue(qr.getMessage().contains("3000 bytes") && qr.getMessage().contains("or Error correction lower"),
                qr.getMessage());
        InvalidModelException lowQr = error("\tBarcode: " + "x".repeat(3000), "\t\tAlt: Datos",
                "\t\tError correction: low");
        assertFalse(lowQr.getMessage().contains("Error correction lower"), lowQr.getMessage());
    }

    @Test
    void propertiesWithoutEffect() {
        assertEquals(ErrorCode.BARCODE_WITHOUT_ALT, error("\tBarcode: https://stxt.dev").errorCode());
        assertEquals(ErrorCode.BARCODE_PROPERTY_WITHOUT_EFFECT, error("\tBarcode: STXT", "\t\tAlt: Referencia",
                "\t\tBarcode format: code-128", "\t\tError correction: low").errorCode());
        assertEquals(ErrorCode.BARCODE_PROPERTY_WITHOUT_EFFECT, error("\tBarcode: STXT", "\t\tAlt: Referencia",
                "\t\tTypography:", "\t\t\tSize: 12").errorCode());
    }

    @Test
    void theModulesHaveTheColorOfTheText() {
        Box box = barcode("\tStyles:", "\t\tFor element: Barcode", "\t\t\tTypography:", "\t\t\t\tColor: #1F3A5F",
                "\tBarcode: https://stxt.dev", "\t\tAlt: Enlace");
        assertEquals("#1F3A5F", box.style().typography().color());
    }

    /** The Data Matrix without its quiet zone, as the decoder of ZXing reads it. */
    private static DecoderResult decodeDataMatrix(Modules m) throws Exception {
        int quiet = Barcodes.DATA_MATRIX_QUIET_ZONE;
        int side = m.columns() - 2 * quiet;
        BitMatrix matrix = new BitMatrix(side);
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                if (m.isDark(x + quiet, y + quiet)) {
                    matrix.set(x, y);
                }
            }
        }
        return new com.google.zxing.datamatrix.decoder.Decoder().decode(matrix);
    }

    @Test
    void aDataMatrixIsReadBack() throws Exception {
        Modules m = modules("\tBarcode: L-2025-0388", "\t\tAlt: Lote", "\t\tBarcode format: data-matrix");
        // 11 characters fit in the symbol of 14 × 14 modules, with 1 on each side: square, with square modules.
        assertEquals(16, m.columns());
        assertEquals(16, m.rows());
        assertEquals(m.moduleWidth(), m.moduleHeight(), 1e-9);
        assertEquals(16 * 0.5 * Value.POINTS_PER_MM, m.naturalWidth(), 1e-9);
        assertEquals(0.25 * Value.POINTS_PER_MM, m.minModule(), 1e-9);
        assertEquals("L-2025-0388", decodeDataMatrix(m).getText());
        // The solid sides of the finder pattern are the left one and the bottom one, right after the quiet zone.
        for (int i = 1; i < 15; i++) {
            assertTrue(m.isDark(1, i));
            assertTrue(m.isDark(i, 14));
            assertFalse(m.isDark(0, i));
            assertFalse(m.isDark(i, 15));
        }
        assertEquals("Peña Ceràmica, 120 €", decodeDataMatrix(modules("\tBarcode: Peña Ceràmica, 120 €", "\t\tAlt: Datos",
                "\t\tBarcode format: data-matrix")).getText());
    }

    /** A GS1 Data Matrix starts with FNC1, and has another one after a value of variable length that is not the last. */
    @Test
    void aGs1DataMatrixIsReadBack() throws Exception {
        DecoderResult result = decodeDataMatrix(modules("\tBarcode: (01)09501101530003(10)AB-123(17)261231",
                "\t\tAlt: Etiqueta", "\t\tBarcode format: gs1-data-matrix"));
        // The decoder of ZXing writes each FNC1 as a group separator, the first one too.
        assertEquals((char) 0x1D + "0109501101530003" + "10AB-123" + (char) 0x1D + "17261231", result.getText());
        // The symbology modifier of a GS1 Data Matrix: ]d2.
        assertEquals(2, result.getSymbologyModifier());
        // What does not fit in a GS1-128 fits in a GS1 Data Matrix: capitals, an FNC1 between them and an odd digit.
        String longer = "(10)" + "A".repeat(30) + "(21)" + "B".repeat(30) + "(30)123(01)09501101530003";
        assertEquals((char) 0x1D + "10" + "A".repeat(30) + (char) 0x1D + "21" + "B".repeat(30) + (char) 0x1D + "30123"
                + (char) 0x1D + "0109501101530003", decodeDataMatrix(modules("\tBarcode: " + longer,
                "\t\tAlt: Etiqueta", "\t\tBarcode format: gs1-data-matrix")).getText());
        // A symbol of several regions (more than 26 × 26 modules), each one with its finder pattern.
        String regions = "(10)" + "A".repeat(80) + "(21)" + "B".repeat(80);
        Modules big = modules("\tBarcode: " + regions, "\t\tAlt: Etiqueta", "\t\tBarcode format: gs1-data-matrix");
        assertTrue(big.columns() > 28, "columns: " + big.columns());
        assertEquals((char) 0x1D + "10" + "A".repeat(80) + (char) 0x1D + "21" + "B".repeat(80),
                decodeDataMatrix(big).getText());
    }

    @Test
    void aPdf417HasRowsThreeModulesTall() {
        Modules m = modules("\tBarcode: M1NOGAL/CARMEN EABC123 BCNMADIB 2714", "\t\tAlt: Tarjeta",
                "\t\tBarcode format: pdf417");
        assertTrue(m.rows() >= 3 + 2 * Barcodes.PDF417_QUIET_ZONE, "rows: " + m.rows());
        assertEquals(0.33 * Value.POINTS_PER_MM, m.moduleWidth(), 1e-9);
        assertEquals(3 * m.moduleWidth(), m.moduleHeight(), 1e-9);
        // Every row starts with the start pattern, whose first bar is 8 modules wide, after the quiet zone.
        for (int y = Barcodes.PDF417_QUIET_ZONE; y < m.rows() - Barcodes.PDF417_QUIET_ZONE; y++) {
            assertFalse(m.isDark(Barcodes.PDF417_QUIET_ZONE - 1, y));
            for (int x = 0; x < 8; x++) {
                assertTrue(m.isDark(Barcodes.PDF417_QUIET_ZONE + x, y));
            }
            assertFalse(m.isDark(Barcodes.PDF417_QUIET_ZONE + 8, y));
        }
        assertEquals(2, Barcodes.pdf417Correction(40));
        assertEquals(3, Barcodes.pdf417Correction(41));
        assertEquals(4, Barcodes.pdf417Correction(161));
        assertEquals(5, Barcodes.pdf417Correction(321));
    }

    /** A GS1-128 is a Code 128 whose first character is FNC1: the reader says so with the identifier ]C1. */
    @Test
    void aGs1128IsReadBack() throws Exception {
        Modules m = modules("\tBarcode: (01)09501101530003(10)AB-123(17)261231", "\t\tAlt: Etiqueta",
                "\t\tBarcode format: gs1-128");
        com.google.zxing.Result result = new Code128Reader().decodeRow(0, row(m),
                Map.of(com.google.zxing.DecodeHintType.ASSUME_GS1, Boolean.TRUE));
        assertEquals("]C1" + "0109501101530003" + "10AB-123" + (char) 0x1D + "17261231", result.getText());
        assertEquals(1, m.rows());
        assertFalse(m.isDark(Barcodes.CODE_128_QUIET_ZONE - 1, 0));
        assertTrue(m.isDark(Barcodes.CODE_128_QUIET_ZONE, 0));
        assertNull(m.text());
    }

    @Test
    void gs1DataThatCannotBeEncoded() {
        String[] wrong = {
            "0109501101530003", "is a list of application identifiers",
            "(01)09501101530003(10", "is a list of application identifiers",
            "(1)23", "2 to 4 digits",
            "(10)", "has no value",
            "(10)AB#1", "'#' (U+0023)",
            "(01)0950110153000", "is 14 digits",
            "(01)09501101530004", "The check digit of (01)09501101530004 is 3, not 4",
            "(310)001250", "have 4 digits",
            "(10)" + "A".repeat(91), "91 characters",
            "(10)" + "A".repeat(30) + "(21)" + "B".repeat(30), "a GS1-128 holds at most 48",
        };
        for (int i = 0; i < wrong.length; i += 2) {
            InvalidModelException e = error("\tBarcode: " + wrong[i], "\t\tAlt: Etiqueta", "\t\tBarcode format: gs1-128");
            assertEquals(ErrorCode.BARCODE_DATA, e.errorCode(), wrong[i]);
            assertTrue(e.getMessage().contains(wrong[i + 1]), e.getMessage());
        }
        InvalidModelException longMatrix = error("\tBarcode: (10)" + "A".repeat(90) + "(21)X".repeat(600),
                "\t\tAlt: Etiqueta", "\t\tBarcode format: gs1-data-matrix");
        assertTrue(longMatrix.getMessage().contains("does not fit in a Data Matrix"), longMatrix.getMessage());
        InvalidModelException matrix = error("\tBarcode: " + "x".repeat(3000), "\t\tAlt: Datos",
                "\t\tBarcode format: data-matrix");
        assertTrue(matrix.getMessage().contains("does not fit in a Data Matrix"), matrix.getMessage());
        InvalidModelException stacked = error("\tBarcode:", "\t\tAlt: Datos", "\t\tBarcode format: pdf417",
                "\t\tContent >>", "\t\t\t" + "x".repeat(3000));
        assertTrue(stacked.getMessage().contains("does not fit in a PDF417"), stacked.getMessage());
    }

    @Test
    void checkDigitOfAGtinAndAnSscc() {
        assertEquals(3, Barcodes.checkDigit("0950110153000"));
        assertEquals(1, Barcodes.checkDigit("39501101530000001"));
    }

    /** The text of an EAN-13: the first digit in the quiet zone, the others under their seven modules, long guards. */
    @Test
    void theReadableTextOfAnEan13() {
        Modules m = modules("\tBarcode: 841234567890", "\t\tAlt: Producto", "\t\tBarcode format: ean-13",
                "\t\tReadable text: true", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans", "\t\t\tSize: 9");
        BarcodeText text = m.text();
        assertEquals("8412345678905", text.text());
        assertEquals(13, text.labels().size());
        assertEquals(new BarcodeText.Label("8", 4, 7), text.labels().get(0));
        assertEquals(new BarcodeText.Label("4", 14, 7), text.labels().get(1));
        assertEquals(new BarcodeText.Label("5", 49, 7), text.labels().get(6));
        assertEquals(new BarcodeText.Label("6", 61, 7), text.labels().get(7));
        assertEquals(new BarcodeText.Label("5", 96, 7), text.labels().get(12));
        // Start, middle and end guards: 3, 5 and 3 modules.
        assertEquals(11, text.longBars().cardinality());
        assertTrue(text.isLong(11) && text.isLong(13) && text.isLong(56) && text.isLong(60) && text.isLong(103)
                && text.isLong(105));
        assertFalse(text.isLong(10) || text.isLong(14) || text.isLong(106));
        // The text does not change the bars.
        assertEquals(22.85 * Value.POINTS_PER_MM, m.naturalHeight(), 1e-9);
    }

    @Test
    void theReadableTextOfACode128IsOneCenteredPiece() {
        Modules code = modules("\tBarcode: AL-2025-0388", "\t\tAlt: Albarán", "\t\tBarcode format: code-128",
                "\t\tReadable text: true", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans", "\t\t\tSize: 9");
        assertEquals(java.util.List.of(new BarcodeText.Label("AL-2025-0388", 0, code.columns())), code.text().labels());
        assertTrue(code.text().longBars().isEmpty());
        Modules gs1 = modules("\tBarcode: (00)395011015300000011", "\t\tAlt: Bulto", "\t\tBarcode format: gs1-128",
                "\t\tReadable text: true", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans", "\t\t\tSize: 9");
        assertEquals("(00)395011015300000011", gs1.text().text());
    }

    @Test
    void readableTextNeedsABarcodeOfOneRowAndAFont() {
        InvalidModelException qr = error("\tBarcode: https://stxt.dev", "\t\tAlt: Enlace", "\t\tReadable text: true");
        assertEquals(ErrorCode.BARCODE_PROPERTY_WITHOUT_EFFECT, qr.errorCode());
        assertTrue(qr.getMessage().contains("the Barcode is qr"), qr.getMessage());
        assertEquals(ErrorCode.BARCODE_PROPERTY_WITHOUT_EFFECT, error("\tBarcode: L-1", "\t\tAlt: Lote",
                "\t\tBarcode format: data-matrix", "\t\tReadable text: true").errorCode());
        assertEquals(ErrorCode.BARCODE_PROPERTY_WITHOUT_EFFECT, error("\tBarcode: L-1", "\t\tAlt: Lote",
                "\t\tBarcode format: pdf417", "\t\tError correction: high").errorCode());
        // Readable text: false is the default, and says nothing in any format.
        assertNull(modules("\tBarcode: https://stxt.dev", "\t\tAlt: Enlace", "\t\tReadable text: false").text());
        assertEquals(ErrorCode.TEXT_WITHOUT_FONT, error("\tBarcode: STXT", "\t\tAlt: Referencia",
                "\t\tBarcode format: code-128", "\t\tReadable text: true").errorCode());
        assertEquals(ErrorCode.TEXT_WITHOUT_FONT_SIZE, error("\tBarcode: STXT", "\t\tAlt: Referencia",
                "\t\tBarcode format: code-128", "\t\tReadable text: true", "\t\tTypography:",
                "\t\t\tFont: IBM Plex Sans").errorCode());
        // With a readable text, the Typography allows its font; still not what a line of text needs.
        InvalidModelException align = error("\tBarcode: STXT", "\t\tAlt: Referencia", "\t\tBarcode format: code-128",
                "\t\tReadable text: true", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans", "\t\t\tSize: 9",
                "\t\t\tAlign: center");
        assertEquals(ErrorCode.BARCODE_PROPERTY_WITHOUT_EFFECT, align.errorCode());
        assertTrue(align.getMessage().contains("Align has no effect"), align.getMessage());
    }
}
