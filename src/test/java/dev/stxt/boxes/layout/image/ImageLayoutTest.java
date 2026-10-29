package dev.stxt.boxes.layout.image;

import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.layout.ConstraintSpace;
import dev.stxt.boxes.layout.IntrinsicSizes;
import dev.stxt.boxes.layout.LayoutContext;
import dev.stxt.boxes.layout.LayoutFixture;
import dev.stxt.boxes.layout.LayoutResult;
import dev.stxt.boxes.layout.inline.TextMeasurer;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.Element;
import dev.stxt.boxes.tree.StyleEditor;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static dev.stxt.boxes.layout.LayoutFixture.layOut;
import static dev.stxt.boxes.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Dimensionado y colocación de {@code Image} (ESPECIFICACION.md §5.18), con las imágenes de
 * {@code resources/images/test/}: {@code wide.png} de 200 × 100 píxeles (150 × 75 pt, proporción
 * 2), {@code tall.jpg} de 120 × 160 (90 × 120 pt) y {@code square.png} de 40 × 40 (30 × 30 pt), y los SVG
 * {@code shapes.svg} (200 × 100 px: 150 × 75 pt) y {@code label.svg}, sin {@code width} ni {@code height}
 * y con {@code viewBox="0 0 160 48"} (120 × 36 pt).
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
        // No se estira al ancho del área útil, como un <img>.
        assertRect(40, 40, 150, 75, f.rect("I"));
        assertRect(40, 115, 90, 120, f.rect("T"));
    }

    /** Un SVG se dimensiona como un PNG: su tamaño natural a 96 ppp, sea de {@code width} y {@code height} o del {@code viewBox}. */
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
        // Caja de contenido de 150: 75 de alto, más relleno y borde.
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
        // El alto limitado vuelve a fijar el ancho.
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
    void flexRowUsesTheNaturalWidthAndDoesNotStretchTheHeight() {
        LayoutFixture f = layOut(
                "\tFlex:",
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
        // Con Height, su ancho intrínseco sale de la proporción.
        assertRect(280, 40, 60, 60, f.rect("S"));
        assertRect(40, 40, 515, 120, f.rect("F"));
    }

    @Test
    void flexGrowWidensAndTheHeightFollows() {
        LayoutFixture f = layOut(
                "\tFlex:",
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
    void flexColumnStretchesTheWidth() {
        LayoutFixture f = layOut(
                "\tFlex:",
                "\t\tDirection: column",
                "\t\tItem:",
                "\t\t\tImage: test/wide.png",
                "\t\t\t\tId: I",
                "\t\tItem:",
                "\t\t\tCross axis: start",
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
                "\t\tColumns: 200 100",
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
    void absoluteShrinksToTheImage() {
        LayoutFixture f = layOut(
                "\tAbsolute:",
                "\t\tId: A",
                "\t\tOffset: 10 auto auto 20",
                "\t\tImage: test/square.png",
                "\t\t\tId: S");
        assertRect(60, 50, 30, 30, f.rect("A"));
        assertRect(60, 50, 30, 30, f.rect("S"));
    }

    @Test
    void anImageThatDoesNotFitMovesWhole() {
        // Área útil de 100 de alto.
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
        // La primera cabe en la página 1; la segunda, no: el bloque se parte entre las dos.
        assertRect(42, 102, 30, 30, f.rect(1, "S"));
        assertFalse(f.has(1, "T"));
        assertRect(42, 40, 30, 30, f.rect(2, "T"));
    }

    @Test
    void anImageInATableCell() {
        LayoutFixture f = layOut(
                "\tTable:",
                "\t\tColumns: 100 1fr",
                "\t\tBody:",
                "\t\t\tRow:",
                "\t\t\t\tCell:",
                "\t\t\t\t\tId: C",
                "\t\t\t\t\tImage: test/square.png",
                "\t\t\t\t\t\tId: S",
                "\t\t\t\tCell: Texto");
        // En la celda, un bloque, la imagen mide su tamaño natural.
        assertRect(40, 40, 30, 30, f.rect("S"));
        assertRect(40, 40, 100, 30, f.rect("C"));
    }

    @Test
    void anImageThatDoesNotFitInAColumnMovesToTheNext() {
        LayoutFixture f = layOut(
                "\tMulticol:",
                "\t\tId: M",
                "\t\tCount: 2",
                "\t\tFill: auto",
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
        // Columnas de 257,5: A (120 × 60) llena la primera y B pasa entera a la segunda.
        assertRect(40, 40, 120, 60, f.rect("A"));
        assertRect(297.5, 40, 120, 60, f.rect("B"));
    }

    /**
     * El algoritmo depende de {@link LayoutContext}, no del motor (AR-04): se prueba con un doble que solo da el
     * tamaño natural de la imagen, sin carpeta de recursos ni fichero.
     */
    @Test
    void anImageIsLaidOutWithAContextDouble() {
        LayoutContext context = new LayoutContext() {
            @Override
            public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
                throw new AssertionError("una imagen no maqueta otras cajas");
            }

            @Override
            public IntrinsicSizes intrinsic(Box box) {
                throw new AssertionError("con Width auto no hacen falta");
            }

            @Override
            public IntrinsicSizes contentIntrinsic(Box box) {
                throw new AssertionError("con Width auto no hacen falta");
            }

            @Override
            public TextMeasurer measurer() {
                throw new AssertionError("una imagen no tiene texto");
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
}
