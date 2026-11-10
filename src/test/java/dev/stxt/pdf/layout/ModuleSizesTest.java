package dev.stxt.pdf.layout;

import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import org.junit.jupiter.api.Test;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The narrowest module of a {@code Barcode} (reference/components/barcode.md): its content box, once laid out, leaves
 * modules of at least 0.25 mm in a QR code and a Code 128 and 0.264 mm in an EAN-13, or {@link ModuleSizes} gives
 * {@code BARCODE_TOO_SMALL}. The QR code of {@code https://stxt.dev} has 25 + 8 = 33 modules, so it needs
 * 33 × 0.25 mm = 8.25 mm (23.39 pt); the Code 128 of {@code STXT} has 79 + 20 = 99, 24.75 mm (70.16 pt); and an EAN-13
 * has 113, 29.83 mm (84.56 pt).
 */
class ModuleSizesTest {

    private static final String QR = "\tBarcode: https://stxt.dev";
    private static final String CODE_128 = "\tBarcode: STXT";
    private static final String EAN_13 = "\tBarcode: 841234567890";

    private static String[] barcode(String value, String format, String... box) {
        String[] lines = new String[4 + box.length + (format == null ? 0 : 1)];
        int i = 0;
        lines[i++] = value;
        lines[i++] = "\t\tId: B";
        lines[i++] = "\t\tAlt: Código";
        if (format != null) {
            lines[i++] = "\t\tBarcode format: " + format;
        }
        lines[i++] = "\t\tBox:";
        for (String b : box) {
            lines[i++] = "\t\t\t" + b;
        }
        return lines;
    }

    private static InvalidModelException tooSmall(String... lines) {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> layOut(lines));
        assertEquals(ErrorCode.BARCODE_TOO_SMALL, e.errorCode(), e.getMessage());
        return e;
    }

    private static void assertContains(String expected, InvalidModelException e) {
        assertTrue(e.getMessage().contains(expected), "\"" + expected + "\" is not in: " + e.getMessage());
    }

    @Test
    void aQrCodeAtTheMinimumIsAllowed() {
        assertDoesNotThrow(() -> layOut(barcode(QR, null, "Width: 23.39")));
    }

    /** At 20 pt, the modules measure 20 / 33 = 0.606 pt, 0.214 mm; the message gives the box and the one it needs. */
    @Test
    void aQrCodeBelowTheMinimumIsAnError() {
        InvalidModelException e = tooSmall(barcode(QR, null, "Width: 20"));
        assertContains("Barcode B (line ", e);
        assertContains("measure 0.214 mm and the narrowest that can be read is 0.25 mm", e);
        assertContains("its content box measures 7.06 × 7.06 mm (20 × 20 pt)", e);
        assertContains("needs at least 8.25 × 8.25 mm (23.39 × 23.39 pt)", e);
        assertContains("BARCODE_SIZE", e);
    }

    /** A QR code in a wide and low box is drawn as high as the box: the height is what falls short. */
    @Test
    void aQrCodeIsLimitedByTheShorterSide() {
        tooSmall(barcode(QR, null, "Width: 100", "Height: 20"));
        assertDoesNotThrow(() -> layOut(barcode(QR, null, "Width: 100", "Height: 24")));
    }

    /** The padding is not part of the drawing: it is the content box that counts. */
    @Test
    void thePaddingIsNotPartOfTheDrawing() {
        assertDoesNotThrow(() -> layOut(barcode(QR, null, "Width: 24")));
        tooSmall(barcode(QR, null, "Width: 24", "Padding: 1"));
    }

    /** The bars of a Code 128 are stretched to the box: only the width counts, and the height does not. */
    @Test
    void aCode128IsCheckedByItsWidth() {
        assertDoesNotThrow(() -> layOut(barcode(CODE_128, "code-128", "Width: 70.2", "Height: 5")));
        InvalidModelException e = tooSmall(barcode(CODE_128, "code-128", "Width: 69", "Height: 200"));
        assertContains("its content box measures 24.34 mm wide (69 pt)", e);
        assertContains("needs at least 24.75 mm wide (70.16 pt)", e);
    }

    /** An EAN-13 can go down to 80 % of its nominal module, the smallest its standard allows. */
    @Test
    void anEan13GoesDownTo80Percent() {
        assertDoesNotThrow(() -> layOut(barcode(EAN_13, "ean-13", "Width: 84.6")));
        InvalidModelException e = tooSmall(barcode(EAN_13, "ean-13", "Width: 84.4"));
        assertContains("the narrowest that can be read is 0.264 mm", e);
    }

    /** A Barcode in a Page overlay is checked on each page, as the flow. */
    @Test
    void aBarcodeInAPageOverlayIsChecked() {
        tooSmall("\tBlock:", "\t\tBox:", "\t\t\tHeight: 10", "\tPage overlay:", "\t\tOffset: 0 auto auto 0",
                "\t" + QR, "\t\t\tAlt: Código", "\t\t\tBox:", "\t\t\t\tWidth: 10");
    }

    /** A rotated Barcode keeps its modules: it is checked with its box before the rotation. */
    @Test
    void aRotatedBarcodeIsCheckedWithItsBox() {
        String[] lines = barcode(QR, null, "Width: 20");
        String[] rotated = new String[lines.length + 1];
        System.arraycopy(lines, 0, rotated, 0, lines.length);
        rotated[lines.length] = "\t\tRotate: 90";
        tooSmall(rotated);
    }
}
