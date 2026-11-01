package dev.stxt.pdf.tree;

import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.oned.Code128Reader;
import com.google.zxing.oned.EAN13Reader;
import com.google.zxing.qrcode.decoder.Decoder;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.Modules;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        Modules ean = modules("\tBarcode: 841234567890", "\t\tAlt: Producto", "\t\tFormat: ean-13");
        assertEquals(37.29 * Value.POINTS_PER_MM, ean.naturalWidth(), 1e-9);
        assertEquals(22.85 * Value.POINTS_PER_MM, ean.naturalHeight(), 1e-9);
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
        Modules m = modules("\tBarcode: STXT-2026-0042", "\t\tAlt: Referencia", "\t\tFormat: code-128");
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
        Modules twelve = modules("\tBarcode: 841234567890", "\t\tAlt: Producto", "\t\tFormat: ean-13");
        Modules thirteen = modules("\tBarcode: 8412345678905", "\t\tAlt: Producto", "\t\tFormat: ean-13");
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
    void checkDigit() {
        assertEquals(5, Barcodes.checkDigit("841234567890"));
        assertEquals(1, Barcodes.checkDigit("400638133393"));
        assertEquals(0, Barcodes.checkDigit("000000000000"));
    }

    @Test
    void dataThatCannotBeEncoded() {
        InvalidModelException empty = error("\tBarcode:", "\t\tAlt: Código");
        assertEquals(ErrorCode.BARCODE_DATA, empty.errorCode());
        InvalidModelException digit = error("\tBarcode: 8412345678901", "\t\tAlt: Producto", "\t\tFormat: ean-13");
        assertTrue(digit.getMessage().contains("The check digit of the EAN-13 8412345678901 is 5, not 1"),
                digit.getMessage());
        InvalidModelException letters = error("\tBarcode: 84123A", "\t\tAlt: Producto", "\t\tFormat: ean-13");
        assertTrue(letters.getMessage().contains("12 digits"), letters.getMessage());
        InvalidModelException ascii = error("\tBarcode: Peña", "\t\tAlt: Referencia", "\t\tFormat: code-128");
        assertTrue(ascii.getMessage().contains("'ñ' (U+00F1)"), ascii.getMessage());
        InvalidModelException longCode = error("\tBarcode: " + "A".repeat(81), "\t\tAlt: Referencia",
                "\t\tFormat: code-128");
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
                "\t\tFormat: code-128", "\t\tError correction: low").errorCode());
        assertEquals(ErrorCode.BARCODE_PROPERTY_WITHOUT_EFFECT, error("\tBarcode: STXT", "\t\tAlt: Referencia",
                "\t\tTypography:", "\t\t\tSize: 12").errorCode());
    }

    @Test
    void theModulesHaveTheColorOfTheText() {
        Box box = barcode("\tStyles:", "\t\tFor element: Barcode", "\t\t\tTypography:", "\t\t\t\tColor: #1F3A5F",
                "\tBarcode: https://stxt.dev", "\t\tAlt: Enlace");
        assertEquals("#1F3A5F", box.style().typography().color());
    }
}
