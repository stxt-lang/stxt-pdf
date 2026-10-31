package dev.stxt.pdf.layout.image;

import dev.stxt.pdf.fragment.BreakToken;
import dev.stxt.pdf.layout.ConstraintSpace;
import dev.stxt.pdf.layout.IntrinsicSizes;
import dev.stxt.pdf.layout.LayoutContext;
import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.layout.LayoutResult;
import dev.stxt.pdf.layout.inline.TextMeasurer;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Element;
import dev.stxt.pdf.tree.StyleEditor;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Sizing and placement of {@code Image} (reference/components/image.md), with the images of
 * {@code resources/images/test/}: {@code wide.png} of 200 × 100 pixels (150 × 75 pt, ratio
 * 2), {@code tall.jpg} of 120 × 160 (90 × 120 pt) and {@code square.png} of 40 × 40 (30 × 30 pt), and the SVGs
 * {@code shapes.svg} (200 × 100 px: 150 × 75 pt) and {@code label.svg}, without {@code width} or {@code height}
 * and with {@code viewBox="0 0 160 48"} (120 × 36 pt), and {@code pages.pdf}.
 */
class ImageLayoutTest {

    private static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(), r.toString());
    }

    @Test
    void naturalSizeAlsoInBlockFlow() {
        LayoutFixture f = layOut(
                "\tImage: test/wide.png",
                "\t\tId: I",
                "\tImage: test/tall.jpg",
                "\t\tId: T");
        // It does not stretch to the width of the page area, like an <img>.
        assertRect(40, 40, 150, 75, f.rect("I"));
        assertRect(40, 115, 90, 120, f.rect("T"));
    }

    /**
     * An SVG is sized like a PNG: its natural size at 96 dpi, from {@code width} and {@code height} or from the
     * {@code viewBox}.
     */
    @Test
    void svgHasItsNaturalSizeAndItsRatio() {
        LayoutFixture f = layOut(
                "\tImage: test/shapes.svg",
                "\t\tId: S",
                "\tImage: test/label.svg",
                "\t\tId: L",
                "\tImage: test/label.svg",
                "\t\tId: H",
                "\t\tBox:",
                "\t\t\tHeight: 18");
        assertRect(40, 40, 150, 75, f.rect("S"));
        assertRect(40, 115, 120, 36, f.rect("L"));
        assertRect(40, 151, 60, 18, f.rect("H"));
    }

    /**
     * A PDF measures what its page does, already in points: that of {@code Pdf page}, cropped and rotated
     * ({@code pages.pdf}: 200 × 100, 150 × 50 and 100 × 200), or the first one.
     */
    @Test
    void pdfHasTheSizeOfItsPage() {
        LayoutFixture f = layOut(
                "\tImage: test/pages.pdf",
                "\t\tId: A",
                "\tImage: test/pages.pdf",
                "\t\tId: B",
                "\t\tPdf page: 2",
                "\tImage: test/pages.pdf",
                "\t\tId: C",
                "\t\tPdf page: 3",
                "\t\tBox:",
                "\t\t\tWidth: 50");
        assertRect(40, 40, 200, 100, f.rect("A"));
        assertRect(40, 140, 150, 50, f.rect("B"));
        assertRect(40, 190, 50, 100, f.rect("C"));
    }

    @Test
    void oneDeclaredAxisGivesTheOther() {
        LayoutFixture f = layOut(
                "\tImage: test/wide.png",
                "\t\tId: W",
                "\t\tBox:",
                "\t\t\tWidth: 300",
                "\tImage: test/wide.png",
                "\t\tId: H",
                "\t\tBox:",
                "\t\t\tHeight: 60",
                "\tImage: test/wide.png",
                "\t\tId: P",
                "\t\tBox:",
                "\t\t\tWidth: 100%");
        assertRect(40, 40, 300, 150, f.rect("W"));
        assertRect(40, 190, 120, 60, f.rect("H"));
        assertRect(40, 250, 515, 257.5, f.rect("P"));
    }

    @Test
    void theRatioIsThatOfTheContentBox() {
        LayoutFixture f = layOut(
                "\tImage: test/wide.png",
                "\t\tId: I",
                "\t\tBox:",
                "\t\t\tWidth: 172",
                "\t\t\tPadding: 10",
                "\t\t\tBorder: 1");
        // Content box of 150: 75 high, plus padding and border.
        assertRect(40, 40, 172, 97, f.rect("I"));
    }

    @Test
    void aspectRatioReplacesTheImageRatio() {
        LayoutFixture f = layOut(
                "\tImage: test/wide.png",
                "\t\tId: I",
                "\t\tBox:",
                "\t\t\tWidth: 200",
                "\t\t\tAspect ratio: 4");
        assertRect(40, 40, 200, 50, f.rect("I"));
    }

    @Test
    void bothDeclaredKeepTheirSize() {
        LayoutFixture f = layOut(
                "\tImage: test/wide.png",
                "\t\tId: I",
                "\t\tBox:",
                "\t\t\tWidth: 100",
                "\t\t\tHeight: 100");
        assertRect(40, 40, 100, 100, f.rect("I"));
    }

    @Test
    void minAndMaxKeepTheRatio() {
        LayoutFixture f = layOut(
                "\tImage: test/wide.png",
                "\t\tId: MW",
                "\t\tBox:",
                "\t\t\tMax width: 100",
                "\tImage: test/wide.png",
                "\t\tId: MH",
                "\t\tBox:",
                "\t\t\tMax height: 50",
                "\tImage: test/wide.png",
                "\t\tId: NH",
                "\t\tBox:",
                "\t\t\tMin height: 100");
        assertRect(40, 40, 100, 50, f.rect("MW"));
        // The limited height fixes the width again.
        assertRect(40, 90, 100, 50, f.rect("MH"));
        assertRect(40, 140, 200, 100, f.rect("NH"));
    }

    @Test
    void autoMarginsCenterTheNaturalWidth() {
        LayoutFixture f = layOut(
                "\tImage: test/wide.png",
                "\t\tId: I",
                "\t\tBox:",
                "\t\t\tMargin: 0 auto");
        assertRect(222.5, 40, 150, 75, f.rect("I"));
    }

    @Test
    void stackRowUsesTheNaturalWidthAndDoesNotStretchTheHeight() {
        LayoutFixture f = layOut(
                "\tStack:",
                "\t\tId: F",
                "\t\tItem:",
                "\t\t\tImage: test/wide.png",
                "\t\t\t\tId: I",
                "\t\tItem:",
                "\t\t\tImage: test/tall.jpg",
                "\t\t\t\tId: T",
                "\t\tItem:",
                "\t\t\tImage: test/square.png",
                "\t\t\t\tId: S",
                "\t\t\t\tBox:",
                "\t\t\t\t\tHeight: 60");
        assertRect(40, 40, 150, 75, f.rect("I"));
        assertRect(190, 40, 90, 120, f.rect("T"));
        // With Height, its intrinsic width comes from the ratio.
        assertRect(280, 40, 60, 60, f.rect("S"));
        assertRect(40, 40, 515, 120, f.rect("F"));
    }

    @Test
    void stackGrowWidensAndTheHeightFollows() {
        LayoutFixture f = layOut(
                "\tStack:",
                "\t\tItem:",
                "\t\t\tGrow: 1",
                "\t\t\tImage: test/wide.png",
                "\t\t\t\tId: I",
                "\t\tItem:",
                "\t\t\tBlock:",
                "\t\t\t\tBox:",
                "\t\t\t\t\tWidth: 115",
                "\t\t\t\t\tHeight: 10");
        assertRect(40, 40, 400, 200, f.rect("I"));
    }

    @Test
    void stackColumnStretchesTheWidth() {
        LayoutFixture f = layOut(
                "\tStack:",
                "\t\tDirection: vertical",
                "\t\tItem:",
                "\t\t\tImage: test/wide.png",
                "\t\t\t\tId: I",
                "\t\tItem:",
                "\t\t\tHorizontal: start",
                "\t\t\tImage: test/wide.png",
                "\t\t\t\tId: J");
        assertRect(40, 40, 515, 257.5, f.rect("I"));
        assertRect(40, 297.5, 150, 75, f.rect("J"));
    }

    @Test
    void gridStretchesTheWidthAndTheRowFollowsTheHeight() {
        LayoutFixture f = layOut(
                "\tGrid:",
                "\t\tId: G",
                "\t\tColumn sizes: 200 100",
                "\t\tArea:",
                "\t\t\tImage: test/wide.png",
                "\t\t\t\tId: I",
                "\t\tArea:",
                "\t\t\tImage: test/tall.jpg",
                "\t\t\t\tId: T",
                "\t\tArea:",
                "\t\t\tHorizontal: start",
                "\t\t\tImage: test/square.png",
                "\t\t\t\tId: S");
        assertRect(40, 40, 200, 100, f.rect("I"));
        assertEquals(133.33, f.rect("T").height(), 0.01);
        Rect s = f.rect("S");
        assertRect(40, 40 + f.rect("T").height(), 30, 30, s);
    }

    @Test
    void overlayShrinksToTheImage() {
        LayoutFixture f = layOut(
                "\tOverlay:",
                "\t\tId: A",
                "\t\tOffset: 10 auto auto 20",
                "\t\tImage: test/square.png",
                "\t\t\tId: S");
        assertRect(60, 50, 30, 30, f.rect("A"));
        assertRect(60, 50, 30, 30, f.rect("S"));
    }

    @Test
    void anImageThatDoesNotFitMovesWhole() {
        // Page area 100 high.
        LayoutFixture f = layOutPaged(180,
                "\tBlock:",
                "\t\tId: B",
                "\t\tBox:",
                "\t\t\tHeight: 60",
                "\tImage: test/wide.png",
                "\t\tId: I");
        assertEquals(2, f.pages());
        assertFalse(f.has(1, "I"));
        assertRect(40, 40, 150, 75, f.rect(2, "I"));
    }

    @Test
    void anImageMovesWithItsParentBlock() {
        LayoutFixture f = layOutPaged(180,
                "\tBlock:",
                "\t\tBox:",
                "\t\t\tHeight: 60",
                "\tBlock:",
                "\t\tId: B",
                "\t\tBox:",
                "\t\t\tBorder: 2",
                "\t\tImage: test/square.png",
                "\t\t\tId: S",
                "\t\tImage: test/square.png",
                "\t\t\tId: T");
        // The first one fits on page 1; the second does not: the block is split between the two.
        assertRect(42, 102, 30, 30, f.rect(1, "S"));
        assertFalse(f.has(1, "T"));
        assertRect(42, 40, 30, 30, f.rect(2, "T"));
    }

    @Test
    void anImageInATableCell() {
        LayoutFixture f = layOut(
                "\tTable:",
                "\t\tColumn sizes: 100 *",
                "\t\tBody:",
                "\t\t\tRow:",
                "\t\t\t\tCell:",
                "\t\t\t\t\tId: C",
                "\t\t\t\t\tImage: test/square.png",
                "\t\t\t\t\t\tId: S",
                "\t\t\t\tCell: Texto");
        // In the cell, a block, the image measures its natural size.
        assertRect(40, 40, 30, 30, f.rect("S"));
        assertRect(40, 40, 100, 30, f.rect("C"));
    }

    @Test
    void anImageThatDoesNotFitInAColumnMovesToTheNext() {
        LayoutFixture f = layOut(
                "\tColumns:",
                "\t\tId: M",
                "\t\tCount: 2",
                "\t\tBalance: false",
                "\t\tBox:",
                "\t\t\tHeight: 100",
                "\t\tImage: test/wide.png",
                "\t\t\tId: A",
                "\t\t\tBox:",
                "\t\t\t\tWidth: 120",
                "\t\tImage: test/wide.png",
                "\t\t\tId: B",
                "\t\t\tBox:",
                "\t\t\t\tWidth: 120");
        // Columns of 257.5: A (120 × 60) fills the first and B moves whole to the second.
        assertRect(40, 40, 120, 60, f.rect("A"));
        assertRect(297.5, 40, 120, 60, f.rect("B"));
    }

    /**
     * The algorithm depends on {@link LayoutContext}, not on the engine: it is tested with a double that only gives the
     * natural size of the image, without a resources folder or a file.
     */
    @Test
    void anImageIsLaidOutWithAContextDouble() {
        LayoutContext context = new LayoutContext() {
            @Override
            public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
                throw new AssertionError("an image does not lay out other boxes");
            }

            @Override
            public IntrinsicSizes intrinsic(Box box) {
                throw new AssertionError("not needed with Width auto");
            }

            @Override
            public IntrinsicSizes contentIntrinsic(Box box) {
                throw new AssertionError("not needed with Width auto");
            }

            @Override
            public TextMeasurer measurer() {
                throw new AssertionError("an image has no text");
            }

            @Override
            public NaturalSize natural(Box box) {
                return new NaturalSize(120, 60);
            }
        };
        Box image = new StyleEditor().box("I", Element.IMAGE);
        LayoutResult r = new ImageLayout(context).layout(image,
                ConstraintSpace.free(400, 400, OptionalDouble.empty(), false), null);
        assertEquals(120, r.fragment().width(), 1e-9);
        assertEquals(60, r.fragment().height(), 1e-9);
        assertEquals(new IntrinsicSizes(120, 120), new ImageLayout(context).contentIntrinsic(image));
    }

    // Cases that were missing according to mutation testing: the ratio is that of the content
    // box, without the border (2) or the padding (3).

    @Test
    void aHeightKeepsTheRatioOfTheContentBox() {
        // Height: 50 leaves 40 for the content, which measures 80 wide: 90 with border and padding, also as a stack
        // item.
        LayoutFixture f = layOut(
                "\tImage: test/wide.png", "\t\tId: I", "\t\tBox:", "\t\t\tHeight: 50", "\t\t\tBorder: 2", "\t\t\tPadding: 3",
                "\tStack:", "\t\tItem:", "\t\t\tImage: test/wide.png", "\t\t\t\tId: F", "\t\t\t\tBox:", "\t\t\t\t\tHeight: 50",
                "\t\t\t\t\tBorder: 2", "\t\t\t\t\tPadding: 3");
        assertRect(40, 40, 90, 50, f.rect("I"));
        assertRect(40, 90, 90, 50, f.rect("F"));
    }

    @Test
    void aMaxHeightNarrowsTheImage() {
        // Natural: 150 + 10 wide and 85 high; Max height: 30 leaves 20 for the content, which measures 40: 50 wide.
        LayoutFixture f = layOut(
                "\tImage: test/wide.png", "\t\tId: I", "\t\tBox:", "\t\t\tMax height: 30", "\t\t\tBorder: 2",
                "\t\t\tPadding: 3");
        assertRect(40, 40, 50, 30, f.rect("I"));
    }
}
