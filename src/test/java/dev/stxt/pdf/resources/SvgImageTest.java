package dev.stxt.pdf.resources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The SVG images of {@code images/} (reference/components/image.md): their natural size and the subset that is
 * supported.
 */
class SvgImageTest {

    private static final String NS = "xmlns=\"http://www.w3.org/2000/svg\"";

    @TempDir
    Path dir;

    private ImageFile read(String svg) throws IOException {
        Path file = dir.resolve("image.svg");
        Files.writeString(file, svg, StandardCharsets.UTF_8);
        return ImageFile.read(file, "image.svg", new Resources());
    }

    private String error(String svg) throws IOException {
        return assertThrows(ImageFormatException.class, () -> read(svg)).getMessage();
    }

    @Test
    void sizeIsWidthAndHeightAt96Dpi() throws IOException {
        ImageFile f = read("<svg " + NS + " width=\"200\" height=\"100\" viewBox=\"0 0 20 10\"/>");
        assertEquals(ImageFile.Format.SVG, f.format());
        assertTrue(f.isVector());
        assertEquals(200, f.pixelWidth(), 1e-9);
        assertEquals(150, f.width(), 1e-9);
        assertEquals(75, f.height(), 1e-9);
    }

    @Test
    void sizeUnits() throws IOException {
        // 1 in = 96 px = 72 pt; 36 pt = 48 px; 25.4 mm = 1 in.
        ImageFile f = read("<svg " + NS + " width=\"1in\" height=\"36pt\"/>");
        assertEquals(72, f.width(), 1e-9);
        assertEquals(36, f.height(), 1e-9);
        f = read("<svg " + NS + " width=\"25.4mm\" height=\"2.54cm\"/>");
        assertEquals(72, f.width(), 1e-9);
        assertEquals(72, f.height(), 1e-9);
        f = read("<svg " + NS + " width=\"6pc\" height=\"10px\"/>");
        assertEquals(72, f.width(), 1e-9);
        assertEquals(7.5, f.height(), 1e-9);
    }

    @Test
    void sizeFromTheViewBoxWhenThereIsNoWidthOrHeight() throws IOException {
        ImageFile f = read("<svg " + NS + " viewBox=\"0 0 160 48\"/>");
        assertEquals(120, f.width(), 1e-9);
        assertEquals(36, f.height(), 1e-9);
    }

    @Test
    void oneSideAndTheViewBoxGiveTheOther() throws IOException {
        ImageFile f = read("<svg " + NS + " width=\"200\" viewBox=\"0 0 20 10\"/>");
        assertEquals(75, f.height(), 1e-9);
        f = read("<svg " + NS + " height=\"100\" viewBox=\"0 0 20 10\"/>");
        assertEquals(150, f.width(), 1e-9);
    }

    @Test
    void noSizeIsAnError() throws IOException {
        assertTrue(error("<svg " + NS + "/>").contains("has no size"));
        assertTrue(error("<svg " + NS + " width=\"10\"/>").contains("only has width and has no viewBox"));
        assertTrue(error("<svg " + NS + " width=\"50%\" height=\"10\"/>").contains("is not an absolute length"));
        assertTrue(error("<svg " + NS + " width=\"0\" height=\"10\"/>").contains("must be positive"));
        assertTrue(error("<svg " + NS + " viewBox=\"0 0 10\"/>").contains("is not four numbers"));
        assertTrue(error("<svg " + NS + " viewBox=\"0 0 0 10\"/>").contains("positive width and height"));
    }

    @Test
    void recognizedByContentWithABomAndWhitespace() throws IOException {
        Path file = dir.resolve("bom.png");
        Files.write(file, ("﻿\n  <svg " + NS + " width=\"10\" height=\"10\"/>").getBytes(StandardCharsets.UTF_8));
        assertEquals(ImageFile.Format.SVG, ImageFile.read(file, "bom.png", new Resources()).format());
    }

    @Test
    void theFilesOfTheRepositoryRead() {
        ImageLibrary images = new Resources().images();
        assertEquals(150, images.find("test/shapes.svg").width(), 1e-9);
        assertEquals(75, images.find("test/shapes.svg").height(), 1e-9);
        assertEquals(120, images.find("test/label.svg").width(), 1e-9);
        assertEquals(36, images.find("test/label.svg").height(), 1e-9);
    }

    @Test
    void notSvg() throws IOException {
        assertTrue(error("<html " + NS + "/>").contains("is not an SVG: its root is <html>"));
        assertTrue(error("<svg width=\"10\" height=\"10\"/>").contains("is not in the namespace"));
        assertTrue(error("<svg " + NS + " width=\"10\" height=\"10\">").contains("is not a well-formed SVG (line 1)"));
    }

    @Test
    void elementsOutsideTheSubset() throws IOException {
        for (String element : new String[] {"script", "style", "filter", "mask", "pattern", "marker", "image",
                "foreignObject", "a", "animate", "set", "symbol", "switch", "metadata"}) {
            String message = error("<svg " + NS + " width=\"10\" height=\"10\">\n<" + element + "/>\n</svg>");
            assertTrue(message.contains("(line 2)") && message.contains("the element <" + element
                    + "> is not in the subset"), element + ": " + message);
        }
        assertTrue(error("<svg " + NS + " width=\"10\" height=\"10\"><svg/></svg>")
                .contains("an <svg> inside another one"));
        assertTrue(error("<svg " + NS + " xmlns:x=\"urn:x\" width=\"10\" height=\"10\"><x:foo/></svg>")
                .contains("the element <x:foo> is not SVG"));
        assertTrue(error("<svg " + NS + " width=\"10\" height=\"10\"><tspan>a</tspan></svg>")
                .contains("a <tspan> only goes inside a <text>"));
    }

    @Test
    void propertiesOutsideTheSubset() throws IOException {
        for (String property : new String[] {"filter", "mask", "marker-start"}) {
            String message = error("<svg " + NS + " width=\"10\" height=\"10\"><rect " + property
                    + "=\"url(#x)\"/></svg>");
            assertTrue(message.contains(property + " (filters, masks and markers)"), message);
        }
        assertTrue(error("<svg " + NS + " width=\"10\" height=\"10\"><rect style=\"fill:red; filter:url(#f)\"/></svg>")
                .contains("filter"));
        assertTrue(error("<svg " + NS + " width=\"10\" height=\"10\"><rect onclick=\"x()\"/></svg>")
                .contains("event handler"));
        assertTrue(error("<svg " + NS + " xmlns:i=\"urn:i\" width=\"10\" height=\"10\"><rect i:x=\"1\"/></svg>")
                .contains("is from another namespace"));
        assertTrue(error("<svg " + NS + " width=\"10\" height=\"10\"><rect style=\"fill\"/></svg>")
                .contains("is not property: value"));
    }

    @Test
    void externalReferencesAreErrors() throws IOException {
        String xlink = " xmlns:xlink=\"http://www.w3.org/1999/xlink\"";
        for (String use : new String[] {"<use xlink:href=\"logo.svg#mark\"/>", "<use href=\"https://example.com/a.svg\"/>",
                "<use href=\"file:///etc/passwd\"/>"}) {
            String message = error("<svg " + NS + xlink + " width=\"10\" height=\"10\">" + use + "</svg>");
            assertTrue(message.contains("leaves the file"), message);
        }
        for (String fill : new String[] {"url(http://example.com/g.svg#g)", "url('other.svg#g')", "url( x.svg )"}) {
            String message = error("<svg " + NS + " width=\"10\" height=\"10\"><rect fill=\"" + fill + "\"/></svg>");
            assertTrue(message.contains("leaves the file"), message);
        }
        assertTrue(error("<svg " + NS + " width=\"10\" height=\"10\"><rect style=\"fill:url(x.svg#g)\"/></svg>")
                .contains("leaves the file"));
        assertTrue(error("<svg " + NS + " width=\"10\" height=\"10\"><rect href=\"#a\"/></svg>")
                .contains("href has no meaning in <rect>"));
    }

    @Test
    void internalReferencesAreFine() throws IOException {
        read("<svg " + NS + " width=\"10\" height=\"10\"><defs><linearGradient id=\"g\"/></defs>"
                + "<rect fill=\"url(#g)\"/><use href=\"#r\"/><rect id=\"r\" style=\"fill:url('#g')\"/></svg>");
    }

    @Test
    void dtdAndEntitiesAreErrors() throws IOException {
        String message = error("<?xml version=\"1.0\"?><!DOCTYPE svg [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>"
                + "<svg " + NS + " width=\"10\" height=\"10\"><title>&x;</title></svg>");
        assertTrue(message.contains("DTDs and entities are not supported"), message);
    }

    @Test
    void textNeedsAFontOfTheFontsFolder() throws IOException {
        String head = "<svg " + NS + " width=\"100\" height=\"50\">";
        read(head + "<text font-family=\"IBM Plex Sans\">Hola</text></svg>");
        read(head + "<g font-family=\"'IBM Plex Serif'\" font-style=\"italic\"><text style=\"font-weight:bold\">"
                + "Hola <tspan font-family=\"IBM Plex Mono, monospace\" font-style=\"normal\">mundo</tspan></text></g></svg>");

        String unknown = error(head + "\n<text font-family=\"Comic Sans MS\">Hola</text></svg>");
        assertTrue(unknown.contains("The text \"Hola\" of the SVG image.svg (line 2)") && unknown.contains("Unknown font: "
                + "Comic Sans MS"), unknown);
        assertTrue(error(head + "<text>Hola</text></svg>").contains("declares no font-family"));
        assertTrue(error(head + "<text font-family=\"sans-serif\">Hola</text></svg>")
                .contains("generic family sans-serif"));
        // Only the first family of the list counts.
        read(head + "<text font-family=\"IBM Plex Sans, sans-serif\" font-style=\"italic\" "
                + "font-weight=\"bold\">Hola</text></svg>");
        String glyph = error(head + "<text font-family=\"IBM Plex Sans\">Hola 中</text></svg>");
        assertTrue(glyph.contains("'中' (U+4E2D)"), glyph);
        // The weights from 100 to 900 in steps of 100, like Weight, if the family has them: Plex Sans has 300 and 600.
        read(head + "<text font-family=\"IBM Plex Sans\" font-weight=\"300\">Hola <tspan font-weight=\"600\">mundo"
                + "</tspan></text></svg>");
        assertTrue(error(head + "<text font-family=\"IBM Plex Sans\" font-weight=\"900\">Hola</text></svg>")
                .contains("The font IBM Plex Sans has no variant weight 900"));
        for (String weight : List.of("350", "bolder")) {
            String text = "<text font-family=\"IBM Plex Sans\" font-weight=\"" + weight + "\">Hola</text></svg>";
            assertTrue(error(head + text).contains("font-weight " + weight
                    + ": only normal, bold and 100 to 900 in steps of 100 are supported"));
        }
        assertTrue(error(head + "<text font-family=\"IBM Plex Sans\" font-style=\"oblique\">Hola</text></svg>")
                .contains("font-style oblique: only normal and italic are supported"));
        // A tspan with another font that does not exist.
        assertTrue(error(head + "<text font-family=\"IBM Plex Sans\">Hola <tspan font-family=\"Nope\">x</tspan></text></svg>")
                .contains("Unknown font: Nope"));
    }

    @Test
    void everyTextAndTspanHasTheFontThatDrawsIt() throws IOException {
        // In document order, with the inheritance of the file and what style declares; null for one without a family.
        ImageFile f = read("<svg " + NS + " width=\"100\" height=\"50\">"
                + "<g font-family=\"'IBM Plex Serif'\" font-style=\"italic\"><text style=\"font-weight:bold\">"
                + "Hola <tspan font-family=\"IBM Plex Mono, monospace\" font-style=\"normal\">mundo</tspan></text></g>"
                + "<text> <tspan font-family=\"IBM Plex Sans\">x</tspan></text></svg>");
        java.util.List<dev.stxt.pdf.model.FontFace> expected = java.util.Arrays.asList(
                face("IBM Plex Serif", true, true), face("IBM Plex Mono", true, false), null,
                face("IBM Plex Sans", false, false));
        assertEquals(expected, f.textFonts().stream().map(font -> font == null ? null : font.face()).toList());
        assertTrue(f.hasText());
        assertTrue(read("<svg " + NS + " width=\"10\" height=\"10\"/>").textFonts().isEmpty());
    }

    private static dev.stxt.pdf.model.FontFace face(String family, boolean bold, boolean italic) {
        return new dev.stxt.pdf.model.FontFace(family, bold ? dev.stxt.pdf.model.FontWeight.BOLD
                : dev.stxt.pdf.model.FontWeight.NORMAL, italic ? dev.stxt.pdf.model.FontStyle.ITALIC
                : dev.stxt.pdf.model.FontStyle.NORMAL);
    }

    // --- How much an SVG draws --------------------------------------------------

    @Test
    void anSvgCountsItsElementsAndItsDepth() throws IOException {
        ImageFile f = read("<svg " + NS + " width=\"10\" height=\"10\"><g><rect/><rect/></g><circle/></svg>");
        assertEquals(5, f.svgElements());
        assertEquals(3, f.svgDepth());
        // A PNG or a JPEG has no elements and an SVG has no pixels.
        assertEquals(0, f.pixels());
    }

    @Test
    void everyUseCountsWhatItRepeats() throws IOException {
        // svg (1) + defs with a g of two rect (1 + 3) + three use of that g (each 1 + 3).
        ImageFile f = read("<svg " + NS + " width=\"10\" height=\"10\"><defs><g id=\"a\"><rect/><rect/></g></defs>"
                + "<use href=\"#a\"/><use href=\"#a\"/><use href=\"#a\"/></svg>");
        assertEquals(1 + 4 + 3 * 4, f.svgElements());
    }

    @Test
    void aReferenceDefinedAfterItsUseCounts() throws IOException {
        ImageFile f = read("<svg " + NS + " width=\"10\" height=\"10\"><use href=\"#a\"/><g id=\"a\"><rect/></g></svg>");
        assertEquals(1 + (1 + 2) + 2, f.svgElements());
    }

    @Test
    void aSelfRepeatingSvgIsRejected() throws IOException {
        String message = error("<svg " + NS + " width=\"10\" height=\"10\"><g id=\"a\"><use href=\"#a\"/></g></svg>");
        assertTrue(message.contains("repeats itself"), message);
    }

    /**
     * Ten multiplied nine times does not fit in an integer and cannot be traversed: the count saturates and does not
     * overflow.
     */
    @Test
    void theCountSaturatesInsteadOfOverflowing() throws IOException {
        StringBuilder svg = new StringBuilder("<svg " + NS + " width=\"10\" height=\"10\"><defs><g id=\"l0\"><rect/></g>");
        for (int k = 1; k <= 30; k++) {
            svg.append("<g id=\"l").append(k).append("\">");
            for (int j = 0; j < 10; j++) {
                svg.append("<use href=\"#l").append(k - 1).append("\"/>");
            }
            svg.append("</g>");
        }
        svg.append("</defs><use href=\"#l30\"/></svg>");
        assertEquals(SvgInspector.SATURATED, read(svg.toString()).svgElements());
    }

    /** A hostile SVG with tens of thousands of levels, nested or chained, does not exhaust the inspector's stack. */
    @Test
    void veryDeepOrLongChainsDoNotOverflowTheStack() throws IOException {
        StringBuilder nested = new StringBuilder("<svg " + NS + " width=\"10\" height=\"10\">");
        nested.append("<g>".repeat(30_000)).append("<rect/>").append("</g>".repeat(30_000)).append("</svg>");
        ImageFile deep = read(nested.toString());
        assertEquals(30_002, deep.svgDepth());
        assertEquals(30_002, deep.svgElements());

        StringBuilder chain = new StringBuilder("<svg " + NS + " width=\"10\" height=\"10\"><defs><rect id=\"u0\"/>");
        for (int k = 1; k <= 30_000; k++) {
            chain.append("<use id=\"u").append(k).append("\" href=\"#u").append(k - 1).append("\"/>");
        }
        chain.append("</defs><use href=\"#u30000\"/></svg>");
        assertTrue(read(chain.toString()).svgElements() > 30_000);
    }
}
