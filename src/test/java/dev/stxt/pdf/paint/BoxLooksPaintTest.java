package dev.stxt.pdf.paint;

import dev.stxt.pdf.Docs;
import dev.stxt.InlineNode;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.layout.Engine;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.stxt.DocumentReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El aspecto de las cajas (reference/common/box.md) en el PDF, mirando los píxeles de la
 * página que se renderiza a 72 ppp (un píxel por punto), y en el SVG, mirando su texto.
 */
class BoxLooksPaintTest {

    private static final String HEAD = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 200\n\t\tHeight: 400\n"
            + "\t\tMargin: 20\n";

    /** Los bloques de 100 × 40 en x = 20 e y = 20, 70, 120… (10 de hueco), con las propiedades de cada uno. */
    private static String doc(String... boxes) {
        StringBuilder out = new StringBuilder(HEAD);
        for (String box : boxes) {
            out.append("\tBlock:\n\t\tBox:\n\t\t\tWidth: 100\n\t\t\tHeight: 40\n\t\t\tMargin: 0 0 10 0\n");
            for (String property : box.split("\n")) {
                out.append("\t\t\t").append(property).append('\n');
            }
        }
        return out.toString();
    }

    private static byte[] pdf(String document) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.create().toPdf(new DocumentReader().read(Docs.sectioned(document)), out);
        Files.createDirectories(Path.of("target", "pdf"));
        Files.write(Path.of("target", "pdf", "box-looks.pdf"), out.toByteArray());
        return out.toByteArray();
    }

    private static BufferedImage render(String document) throws Exception {
        try (PDDocument pdf = Loader.loadPDF(pdf(document))) {
            return new PDFRenderer(pdf).renderImageWithDPI(0, 72);
        }
    }

    private static int[] rgb(BufferedImage image, int x, int y) {
        int p = image.getRGB(x, y);
        return new int[]{(p >> 16) & 0xFF, (p >> 8) & 0xFF, p & 0xFF};
    }

    private static void assertColor(BufferedImage image, int x, int y, int r, int g, int b, int tolerance) {
        int[] c = rgb(image, x, y);
        String at = " en (" + x + ", " + y + "): " + c[0] + " " + c[1] + " " + c[2];
        assertTrue(Math.abs(c[0] - r) <= tolerance && Math.abs(c[1] - g) <= tolerance
                && Math.abs(c[2] - b) <= tolerance, "esperado " + r + " " + g + " " + b + at);
    }

    @BeforeAll
    static void headless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void roundedCornersLeaveTheCornerOfTheBoxEmpty() throws Exception {
        BufferedImage page = render(doc("Corner radius: 20\nBackground: #FF0000"));
        // La esquina (20, 20) está fuera de la curva; el centro y el medio de un lado, dentro.
        assertColor(page, 21, 21, 255, 255, 255, 0);
        assertColor(page, 119, 59, 255, 255, 255, 0);
        assertColor(page, 70, 40, 255, 0, 0, 0);
        assertColor(page, 21, 40, 255, 0, 0, 0);
    }

    @Test
    void aSquareBoxFillsItsCorner() throws Exception {
        assertColor(render(doc("Background: #FF0000")), 21, 21, 255, 0, 0, 0);
    }

    @Test
    void aFiftyPercentRadiusOnASquareIsACircle() throws Exception {
        String square = HEAD + "\tBlock:\n\t\tBox:\n\t\t\tWidth: 60\n\t\t\tHeight: 60\n\t\t\tCorner radius: 50%\n"
                + "\t\t\tBackground: #0000FF\n";
        BufferedImage page = render(square);
        assertColor(page, 50, 50, 0, 0, 255, 0);
        assertColor(page, 22, 22, 255, 255, 255, 0);
        assertColor(page, 77, 77, 255, 255, 255, 0);
    }

    @Test
    void aBorderWithRadiusIsARingAroundTheBackground() throws Exception {
        BufferedImage page = render(doc("Corner radius: 20\nBorder: 6\nBorder color: #00FF00\nBackground: #FF0000"));
        assertColor(page, 70, 22, 0, 255, 0, 0);
        assertColor(page, 70, 40, 255, 0, 0, 0);
        assertColor(page, 21, 21, 255, 255, 255, 0);
    }

    @Test
    void eachSideHasItsOwnColor() throws Exception {
        BufferedImage page = render(doc("Border: 10\nBorder color: #FF0000 #00FF00 #0000FF #FFFF00"));
        assertColor(page, 70, 24, 255, 0, 0, 0);
        assertColor(page, 115, 40, 0, 255, 0, 0);
        assertColor(page, 70, 56, 0, 0, 255, 0);
        assertColor(page, 24, 40, 255, 255, 0, 0);
        // El interior no tiene fondo.
        assertColor(page, 70, 40, 255, 255, 255, 0);
    }

    @Test
    void sidesMeetOnTheDiagonalOfTheCorner() throws Exception {
        BufferedImage page = render(doc("Border: 10\nBorder color: #FF0000 #00FF00 #0000FF #FFFF00"));
        // Arriba a la derecha, la esquina (110..120, 20..30): por encima de la diagonal es del lado de arriba, por debajo del derecho.
        assertColor(page, 113, 22, 255, 0, 0, 0);
        assertColor(page, 117, 26, 0, 255, 0, 0);
    }

    /**
     * Con el borde redondeado, el anillo entra en la caja de relleno (entre la curva de dentro y su
     * esquina): lo cubre el lado que le toca, sin dejar un hueco blanco entre el borde y el fondo.
     */
    @Test
    void roundedInnerCornersLeaveNoGapBetweenTheSidesAndTheBackground() throws Exception {
        BufferedImage page = render(doc("Corner radius: 20\nBorder: 6\nBorder color: #00FF00 #0000FF #00FF00 #0000FF\n"
                + "Background: #FF0000"));
        // (27, 27) está dentro de la caja de relleno (desde 26) y fuera de su curva (a 18 pt del centro, 14 de radio).
        int[] c = rgb(page, 27, 27);
        assertFalse(c[0] == 255 && c[1] == 255 && c[2] == 255, "hueco blanco: " + c[0] + " " + c[1] + " " + c[2]);
        assertTrue(c[0] < 60, "es del borde, no del fondo rojo: " + c[0] + " " + c[1] + " " + c[2]);
        // Y la diagonal reparte la cuña: arriba del color de arriba, a la izquierda del de la izquierda.
        assertColor(page, 31, 27, 0, 255, 0, 0);
        assertColor(page, 27, 31, 0, 0, 255, 0);
    }

    @Test
    void aGradientGoesFromTheFirstToTheLastColor() throws Exception {
        BufferedImage page = render(doc("Background: linear(90, #000000, #FFFFFF)"));
        assertColor(page, 21, 40, 0, 0, 0, 6);
        assertColor(page, 70, 40, 127, 127, 127, 6);
        assertColor(page, 118, 40, 255, 255, 255, 12);
    }

    @Test
    void aGradientAtOneEightyDegreesGoesDown() throws Exception {
        BufferedImage page = render(doc("Background: linear(180, #FF0000, #0000FF)"));
        assertColor(page, 70, 21, 255, 0, 0, 12);
        assertColor(page, 70, 58, 0, 0, 255, 20);
        assertColor(page, 20, 21, 255, 0, 0, 12);
    }

    @Test
    void aGradientWithThreeColorsPassesThroughTheMiddleOne() throws Exception {
        BufferedImage page = render(doc("Background: linear(90, #FF0000, #00FF00, #0000FF)"));
        assertColor(page, 70, 40, 0, 255, 0, 10);
        assertColor(page, 21, 40, 255, 0, 0, 12);
        assertColor(page, 118, 40, 0, 0, 255, 20);
    }

    @Test
    void aGradientToTransparentShowsWhatIsBelow() throws Exception {
        BufferedImage page = render(doc("Background: linear(90, #FF000000, #FF0000)"));
        assertColor(page, 21, 40, 255, 255, 255, 8);
        assertColor(page, 70, 40, 255, 127, 127, 8);
        assertColor(page, 118, 40, 255, 8, 8, 8);
    }

    @Test
    void aGradientIsClippedToTheRoundedBox() throws Exception {
        BufferedImage page = render(doc("Corner radius: 20\nBackground: linear(90, #000000, #FFFFFF)"));
        assertColor(page, 21, 21, 255, 255, 255, 0);
        assertColor(page, 70, 40, 127, 127, 127, 6);
    }

    @Test
    void aTranslucentColorLetsTheBackgroundShowThrough() throws Exception {
        BufferedImage page = render(doc("Background: #0000FF80"));
        assertColor(page, 70, 40, 127, 127, 255, 3);
        // Debajo de otra caja, se mezcla con su color.
        String over = HEAD + "\tBlock:\n\t\tBox:\n\t\t\tWidth: 100\n\t\t\tHeight: 40\n\t\t\tBackground: #FF0000\n"
                + "\t\tBlock:\n\t\t\tBox:\n\t\t\t\tHeight: 40\n\t\t\t\tBackground: #0000FF80\n";
        assertColor(render(over), 70, 40, 127, 0, 127, 3);
    }

    @Test
    void aTranslucentBorderIsNotPaintedTwiceAtTheCorners() throws Exception {
        BufferedImage page = render(doc("Border: 10\nBorder color: #0000FF80"));
        // Una esquina del borde pintada una sola vez: la misma mezcla que el lado.
        assertColor(page, 24, 24, 127, 127, 255, 3);
        assertColor(page, 70, 24, 127, 127, 255, 3);
    }

    @Test
    void translucentTextIsPaintedWithItsOpacity() throws Exception {
        String text = HEAD + "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 40\n\t\t\t\tColor: #00000080\n"
                + "\tText: MMM\n";
        BufferedImage page = render(text);
        boolean gray = false;
        for (int y = 20; y < 60 && !gray; y++) {
            for (int x = 20; x < 60; x++) {
                int[] c = rgb(page, x, y);
                gray |= Math.abs(c[0] - 127) <= 3;
            }
        }
        assertTrue(gray, "El texto con opacidad 50 % se ve gris, no negro");
    }

    @Test
    void imagesAreClippedToTheRoundedBox() throws Exception {
        String image = HEAD + "\tImage: test/wide.png\n\t\tFit: fill\n\t\tBox:\n\t\t\tWidth: 60\n\t\t\tHeight: 60\n"
                + "\t\t\tCorner radius: 50%\n";
        BufferedImage page = render(image);
        assertColor(page, 22, 22, 255, 255, 255, 0);
        int[] center = rgb(page, 50, 50);
        assertFalse(center[0] == 255 && center[1] == 255 && center[2] == 255, "El centro de la imagen no está en blanco");
    }

    @Test
    void thePdfStaysTaggedAndTheDecorationsAreArtifacts() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(pdf(doc("Corner radius: 8\nBorder: 2\nBorder color: #111111 #222222\n"
                + "Background: linear(90, #FF000000, #FF0000)")))) {
            String content = new String(pdf.getPage(0).getContents().readAllBytes(), StandardCharsets.ISO_8859_1);
            assertTrue(content.contains("/Artifact BMC"), content);
            assertTrue(content.contains(" sh"), "El degradado es un sombreado: " + content);
            assertTrue(content.contains("f*"), "El anillo del borde se rellena con la regla par-impar");
            assertTrue(content.contains(" c\n"), "Las esquinas son curvas");
            assertEquals(1, pdf.getNumberOfPages());
        }
    }

    @Test
    void svgPaintsRoundedBoxesAsPaths() {
        String svg = svg(doc("Corner radius: 8\nBackground: #FF0000"));
        assertTrue(svg.contains("<path d=\"M28.00 20.00 L112.00 20.00 C"), svg);
        assertTrue(svg.contains(" fill=\"#FF0000\"/>"), svg);
    }

    @Test
    void svgKeepsSquareSolidBoxesAsRects() {
        String svg = svg(doc("Background: #FF0000"));
        assertTrue(svg.contains("<rect x=\"20.00\" y=\"20.00\" width=\"100.00\" height=\"40.00\" fill=\"#FF0000\"/>"), svg);
    }

    @Test
    void svgWritesTheOpacityOfAColor() {
        String svg = svg(doc("Background: #FF000080"));
        assertTrue(svg.contains("fill=\"#ff0000\" fill-opacity=\"0.502\""), svg);
    }

    @Test
    void svgDefinesGradientsInPageCoordinates() {
        String svg = svg(doc("Background: linear(90, #12315A00, #12315A)"));
        assertTrue(svg.contains("<linearGradient id=\"g0\" gradientUnits=\"userSpaceOnUse\" x1=\"20.00\" y1=\"40.00\" "
                + "x2=\"120.00\" y2=\"40.00\">"), svg);
        assertTrue(svg.contains("<stop offset=\"0.0000\" stop-color=\"#12315a\" stop-opacity=\"0.000\"/>"), svg);
        assertTrue(svg.contains("<stop offset=\"1.0000\" stop-color=\"#12315a\" stop-opacity=\"1.000\"/>"), svg);
        assertTrue(svg.contains("fill=\"url(#g0)\""), svg);
    }

    @Test
    void svgSplitsTheBorderPerSideWhenTheColorsDiffer() {
        String svg = svg(doc("Border: 4\nBorder color: #111111 #222222 #333333 #444444"));
        assertEquals(4, svg.split("<clipPath", -1).length - 1, svg);
        assertTrue(svg.contains("fill-rule=\"evenodd\" clip-path=\"url(#c0)\" fill=\"#111111\""), svg);
    }

    @Test
    void svgPaintsAUniformBorderAsASingleRing() {
        String svg = svg(doc("Border: 4\nBorder color: #111111"));
        assertEquals(0, svg.split("<clipPath", -1).length - 1, svg);
        assertTrue(svg.contains("fill-rule=\"evenodd\" fill=\"#111111\""), svg);
    }

    @Test
    void svgClipsARoundedImage() {
        String image = HEAD + "\tImage: test/wide.png\n\t\tBox:\n\t\t\tWidth: 60\n\t\t\tHeight: 60\n\t\t\tCorner radius: 50%\n";
        LaidOutDocument document = new Engine().layOutDocument(new DocumentReader().read(Docs.sectioned(image)));
        String svg = SvgCanvas.page(document.pages().get(0), document.pageSetup(), false, new Resources());
        assertTrue(svg.contains("<clipPath id=\"c0\">"), svg);
        assertTrue(svg.contains("<g clip-path=\"url(#c0)\"><svg x=\"20.00\""), svg);
    }

    private static String svg(String document) {
        InlineNode input = new DocumentReader().read(Docs.sectioned(document));
        LaidOutDocument laidOut = new Engine().layOutDocument(input);
        return SvgCanvas.page(laidOut.pages().get(0), laidOut.pageSetup(), false);
    }
}
