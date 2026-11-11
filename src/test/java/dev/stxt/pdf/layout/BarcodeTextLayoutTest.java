package dev.stxt.pdf.layout;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.inline.FixedMetrics;
import dev.stxt.pdf.layout.inline.TextMeasurer;
import dev.stxt.pdf.tree.Box;

/**
 * The readable text of a {@code Barcode} ({@link BarcodeTextLayout}, reference/components/barcode.md): its height and
 * its line in a content box, each piece centered under its modules. That it fits is checked by {@link ModuleSizes}
 * ({@code ModuleSizesTest}).
 */
class BarcodeTextLayoutTest {

    private static final TextMeasurer METRICS = new FixedMetrics();

    private static Box barcode(String data, String format, boolean text) {
        return layOut("\tBarcode: " + data, "\t\tId: B", "\t\tAlt: Código", "\t\tBarcode format: " + format,
                text ? "\t\tReadable text: true" : "\t\tQuiet zone: 10", "\t\tTypography:",
                text ? "\t\t\tSize: 8" : "\t\t\tColor: #000000").fragment("B").box();
    }

    /** The ascent plus the descent of the font at its size, which with the test metrics is the size. */
    @Test
    void theTextIsAsTallAsItsFont() {
        assertEquals(8, BarcodeTextLayout.height(barcode("STXT", "code-128", true), METRICS), 1e-9);
    }

    @Test
    void aBarcodeWithoutReadableTextHasNoLine() {
        Box box = barcode("STXT", "code-128", false);
        assertEquals(0, BarcodeTextLayout.height(box, METRICS), 1e-9);
        assertEquals(List.of(), BarcodeTextLayout.lines(box, 198, 50, METRICS));
    }

    /** A Code 128 has its data in one piece, centered under the whole drawing, at the bottom of the box. */
    @Test
    void theTextOfACode128IsOneCenteredPieceAtTheBottom() {
        Box box = barcode("STXT", "code-128", true);
        List<TextLine> lines = BarcodeTextLayout.lines(box, 198, 50, METRICS);
        assertEquals(1, lines.size());
        TextLine line = lines.get(0);
        assertEquals(42, line.y(), 1e-9);
        assertEquals(8, line.height(), 1e-9);
        assertEquals(198, line.width(), 1e-9);
        assertEquals(8, line.fontSize(), 1e-9);
        assertEquals(1, line.runs().size());
        TextLine.Run run = line.runs().get(0);
        assertEquals("STXT", run.text());
        assertEquals(METRICS.width("STXT", box.style().typography().fontFace(), 8), run.width(), 1e-9);
        assertEquals(99, run.x() + run.width() / 2, 1e-9);
    }

    /**
     * An EAN-13 has 113 modules: in a box of 226, each one is 2 wide. Its first digit goes centered under the 7
     * modules before the bars (from module 4 to 11), the second under those from 14 to 21, after the 3 of the start
     * guard, and the eighth under those from 61 to 68, after the 5 of the middle guard.
     */
    @Test
    void eachDigitOfAnEan13GoesUnderItsModules() {
        Box box = barcode("8412345678905", "ean-13", true);
        TextLine line = BarcodeTextLayout.lines(box, 226, 60, METRICS).get(0);
        assertEquals("8412345678905", line.text());
        assertEquals(13, line.runs().size());
        assertEquals(List.of("8", "4", "6"),
                List.of(line.runs().get(0).text(), line.runs().get(1).text(), line.runs().get(7).text()));
        assertEquals(7.5 * 2, center(line.runs().get(0)), 1e-9);
        assertEquals(17.5 * 2, center(line.runs().get(1)), 1e-9);
        assertEquals(64.5 * 2, center(line.runs().get(7)), 1e-9);
        assertTrue(line.runs().get(12).x() + line.runs().get(12).width() < 226);
    }

    private static double center(TextLine.Run run) {
        return run.x() + run.width() / 2;
    }
}
