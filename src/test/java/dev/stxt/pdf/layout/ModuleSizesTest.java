package dev.stxt.pdf.layout;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;

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

    private static String[] labeled(String value, String format, String size, String... box) {
        String[] lines = barcode(value, format, box);
        String[] all = new String[lines.length + 3];
        System.arraycopy(lines, 0, all, 0, lines.length);
        all[lines.length] = "\t\tReadable text: true";
        all[lines.length + 1] = "\t\tTypography:";
        all[lines.length + 2] = "\t\t\tSize: " + size;
        return all;
    }

    private static InvalidModelException textOverflow(String... lines) {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> layOut(lines));
        assertEquals(ErrorCode.BARCODE_TEXT_OVERFLOW, e.errorCode(), e.getMessage());
        return e;
    }

    /**
     * The readable text adds its height to the natural one of the code and is placed at the bottom of the content
     * box: with the metrics of the comparison, a text of size 8 is 8 tall and each character, 4.8 wide.
     */
    @Test
    void theReadableTextTakesTheBottomOfTheBox() {
        LayoutFixture ean = layOut(labeled(EAN_13, "ean-13", "8"));
        Fragment f = ean.fragment("B");
        assertEquals(113 * 0.33 * Value.POINTS_PER_MM, f.contentWidth(), 1e-6);
        assertEquals(22.85 * Value.POINTS_PER_MM + 8, f.contentHeight(), 1e-6);
        TextLine line = f.lines().get(0);
        assertEquals(22.85 * Value.POINTS_PER_MM, line.y(), 1e-6);
        assertEquals(8, line.height(), 1e-6);
        assertEquals("8412345678905", line.text());
        assertEquals(13, line.runs().size());
        // The first digit, centered in the 7 modules before the bars (4 to 11); the second, under 14 to 21.
        double module = 0.33 * Value.POINTS_PER_MM;
        double width = line.runs().get(0).width();
        assertEquals(7.5 * module - width / 2, line.runs().get(0).x(), 1e-6);
        assertEquals(17.5 * module - width / 2, line.runs().get(1).x(), 1e-6);
        // With a height, the text is taken from the bars.
        Fragment fixed = layOut(labeled(CODE_128, "code-128", "8", "Width: 100", "Height: 30")).fragment("B");
        assertEquals(30, fixed.contentHeight(), 1e-6);
        assertEquals(22, fixed.lines().get(0).y(), 1e-6);
        TextLine.Run run = fixed.lines().get(0).runs().get(0);
        assertEquals("STXT", run.text());
        assertEquals(50, run.x() + run.width() / 2, 1e-6);
        // Without Readable text, there is no line.
        assertTrue(layOut(barcode(CODE_128, "code-128", "Width: 100")).fragment("B").lines().isEmpty());
    }

    @Test
    void aReadableTextThatDoesNotFitIsAnError() {
        assertContains("does not fit in its content box", textOverflow(labeled(CODE_128, "code-128", "60",
                "Width: 100")));
        assertContains("runs into the piece before it", textOverflow(labeled(EAN_13, "ean-13", "14")));
        assertContains("leaves no height for the bars", textOverflow(labeled(CODE_128, "code-128", "8", "Width: 100",
                "Height: 8")));
        // An EAN-13 without quiet zone has no room for its first digit.
        String[] lines = labeled(EAN_13, "ean-13", "6");
        String[] noZone = new String[lines.length + 1];
        System.arraycopy(lines, 0, noZone, 0, lines.length);
        noZone[lines.length] = "\t\tQuiet zone: 0";
        assertContains("the first digit of an EAN-13 goes in its left quiet zone", textOverflow(noZone));
    }
}
