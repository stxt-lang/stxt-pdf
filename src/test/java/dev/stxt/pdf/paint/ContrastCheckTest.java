package dev.stxt.pdf.paint;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.model.Check;
import dev.stxt.pdf.model.Colors;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;

/**
 * The contrast of the text with what is below it (reference/common/pdf.md): 4.5:1, or 3:1 for large text, with the
 * backgrounds painted before the text mixed with their opacity over the white page.
 */
class ContrastCheckTest {

    /** Header and typographic base in 12 lines, and the section on line 13: its content starts on line 14. */
    private static final String HEADER = Docs.header(400, 300, 20) + Docs.TYPOGRAPHY;

    private static void layOut(String... lines) {
        Converter.create().layOut(Docs.read(HEADER, lines));
    }

    private static InvalidModelException error(String... lines) {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> layOut(lines));
        assertEquals(ErrorCode.CONTRAST_LOW, e.errorCode());
        return e;
    }

    @Test
    void theRatioIsTheOneOfTheGuidelines() {
        assertEquals(21, ContrastCheck.ratio(Colors.rgba("#000000"), Colors.rgba("#FFFFFF")), 1e-6);
        assertEquals(1, ContrastCheck.ratio(Colors.rgba("#777777"), Colors.rgba("#777777")), 1e-6);
        assertEquals(4.48, ContrastCheck.ratio(Colors.rgba("#777777"), Colors.rgba("#FFFFFF")), 0.01);
        // Half black over white is middle gray.
        float[] half = ContrastCheck.over(Colors.rgba("#00000080"), Colors.rgba("#FFFFFF"));
        assertEquals(0.5, half[0], 0.01);
        assertEquals(1, half[3], 1e-6);
    }

    @Test
    void lightGreyOnWhiteIsAnErrorAtTheLineOfItsColor() {
        InvalidModelException e = error("\tText: hola", "\t\tTypography:", "\t\t\tColor: #999999");
        assertEquals(16, e.line());
        assertTrue(e.getMessage().contains("contrast of 2.84:1 with its background (#999999 on #FFFFFF) and needs 4.5:1"),
                e.getMessage());
    }

    @Test
    void theInheritedColorPointsToWhereItIsDeclared() {
        InvalidModelException e = error("\tBlock:", "\t\tTypography:", "\t\t\tColor: #999999", "\t\tText: hola");
        assertEquals(16, e.line());
    }

    @Test
    void largeTextNeedsThreeToOne() {
        // #949494 on white: 3.03:1.
        error("\tText: hola", "\t\tTypography:", "\t\t\tColor: #949494", "\t\t\tSize: 17");
        assertDoesNotThrow(() -> layOut("\tText: hola", "\t\tTypography:", "\t\t\tColor: #949494", "\t\t\tSize: 18"));
        error("\tText: hola", "\t\tTypography:", "\t\t\tColor: #949494", "\t\t\tSize: 13", "\t\t\tBold: true");
        assertDoesNotThrow(() -> layOut("\tText: hola", "\t\tTypography:", "\t\t\tColor: #949494", "\t\t\tSize: 14",
                "\t\t\tBold: true"));
    }

    @Test
    void theBackgroundOfTheBoxIsWhatIsBelow() {
        assertDoesNotThrow(() -> layOut("\tBlock:", "\t\tBox:", "\t\t\tBackground: #1A2B3C", "\t\tText: hola",
                "\t\t\tTypography:", "\t\t\t\tColor: #FFFFFF"));
        InvalidModelException e = error("\tBlock:", "\t\tBox:", "\t\t\tBackground: #1A2B3C", "\t\tText: hola");
        assertTrue(e.getMessage().contains("(#000000 on #1A2B3C)"), e.getMessage());
    }

    @Test
    void aTranslucentBackgroundMixesWithWhatIsBelow() {
        // Half black over the white page is middle gray: the white on top does not reach the ratio.
        InvalidModelException e = error("\tBlock:", "\t\tBox:", "\t\t\tBackground: #00000080", "\t\tText: hola",
                "\t\t\tTypography:", "\t\t\t\tColor: #FFFFFF");
        assertTrue(e.getMessage().contains("(#FFFFFF on #7F7F7F)"), e.getMessage());
    }

    @Test
    void everyColorOfAGradientCounts() {
        InvalidModelException e = error("\tBlock:", "\t\tBox:", "\t\t\tBackground: linear(90, #FFFFFF, #000000)",
                "\t\tText: hola");
        assertTrue(e.getMessage().contains("(#000000 on #000000)"), e.getMessage());
    }

    /**
     * A block of 200 × 40 rotated 90 degrees in flow takes up the rectangle that encloses it, at the corner of the page
     * area (20, 20): it is painted over x 20 to 60 and y 20 to 220 of the page, and unrotated it would be over x −60 to
     * 140 and y 100 to 140. What lies under a text is what is painted there.
     */
    @Test
    void aRotatedBackgroundIsWhereItIsPainted() {
        String[] block = {"\tBlock:", "\t\tRotate: 90", "\t\tBox:", "\t\t\tWidth: 200", "\t\t\tHeight: 40",
                "\t\t\tBackground: #000000"};
        InvalidModelException e = error(concat(block, "\tOverlay:", "\t\tOffset: 20 auto auto 10", "\t\tText: hola"));
        assertTrue(e.getMessage().contains("(#000000 on #000000)"), e.getMessage());
        assertDoesNotThrow(() -> layOut(concat(block, "\tOverlay:", "\t\tOffset: 100 auto auto 80",
                "\t\tText: hola")));
    }

    private static String[] concat(String[] first, String... rest) {
        String[] all = Arrays.copyOf(first, first.length + rest.length);
        System.arraycopy(rest, 0, all, first.length, rest.length);
        return all;
    }

    @Test
    void textOverAnImageIsNotChecked() {
        // What is below is the image: it is unknown.
        assertDoesNotThrow(() -> layOut("\tBlock:", "\t\tImage: test/wide.png", "\t\t\tBox:", "\t\t\t\tWidth: 300",
                "\t\tOverlay:", "\t\t\tOffset: 0 auto auto 0", "\t\t\tText: hola", "\t\t\t\tTypography:",
                "\t\t\t\t\tColor: #FFFFFF"));
        // With an opaque background over the image, it is known again.
        error("\tBlock:", "\t\tImage: test/wide.png", "\t\t\tBox:", "\t\t\t\tWidth: 300",
                "\t\tOverlay:", "\t\t\tOffset: 0 auto auto 0", "\t\t\tBox:", "\t\t\t\tBackground: #EEEEEE",
                "\t\t\tText: hola", "\t\t\t\tTypography:", "\t\t\t\t\tColor: #FFFFFF");
    }

    /**
     * Inside a box with {@code Opacity} (reference/common/layers-and-rotation.md), the text and the backgrounds count
     * with their opacity multiplied by that of the box: black at 30% over white is #B3B3B3, which does not reach;
     * at 80% it is #333333, which does. An image with {@code Opacity} is still an unknown background.
     */
    @Test
    void theOpacityOfABoxFadesItsTextAndItsBackgrounds() {
        InvalidModelException e = error("\tFrame:", "\t\tOpacity: 30%", "\t\tText: hola");
        assertEquals(16, e.line());
        assertTrue(e.getMessage().contains("(#B3B3B3 on #FFFFFF)"), e.getMessage());
        assertTrue(e.getMessage().contains("its Color, the background or the Opacity of the box it is in has to be "
                + "changed"), e.getMessage());
        assertDoesNotThrow(() -> layOut("\tFrame:", "\t\tOpacity: 80%", "\t\tText: hola"));
        // A black background at 10% is #E6E6E6 under white text; the text of the Overlay inside fades twice.
        e = error("\tFrame:", "\t\tOpacity: 10%", "\t\tBox:", "\t\t\tBackground: #000000",
                "\t\t\tHeight: 40", "\t\tOverlay:", "\t\t\tOpacity: 50%", "\t\t\tOffset: 0 auto auto 0",
                "\t\t\tText: hola", "\t\t\t\tTypography:", "\t\t\t\t\tColor: #FFFFFF");
        assertTrue(e.getMessage().contains(" on #E6E6E6)"), e.getMessage());
        // With 100% the box is opaque, and with 0% its text is not seen: it is not checked.
        assertDoesNotThrow(() -> layOut("\tFrame:", "\t\tOpacity: 100%", "\t\tText: hola"));
        assertDoesNotThrow(() -> layOut("\tFrame:", "\t\tOpacity: 0%", "\t\tText: hola"));
        // After the box, nothing fades.
        assertDoesNotThrow(() -> layOut("\tFrame:", "\t\tOpacity: 10%", "\t\tBox:", "\t\t\tHeight: 10",
                "\tText: hola"));
        assertDoesNotThrow(() -> layOut("\tBlock:", "\t\tImage: test/wide.png", "\t\t\tOpacity: 10%",
                "\t\t\tBox:", "\t\t\t\tWidth: 300", "\t\tOverlay:", "\t\t\tOffset: 0 auto auto 0",
                "\t\t\tText: hola", "\t\t\t\tTypography:", "\t\t\t\t\tColor: #FFFFFF"));
    }

    @Test
    void theCheckCanBeTurnedOff() {
        assertDoesNotThrow(() -> Converter.builder().exclude(Check.CONTRAST).build()
                .layOut(Docs.read(HEADER, "\tText: hola", "\t\tTypography:", "\t\t\tColor: #EEEEEE")));
    }
}
