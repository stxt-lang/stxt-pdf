package dev.stxt.boxes.resources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Las imágenes SVG de {@code images/} (ESPECIFICACION.md §5.18): su tamaño natural y el subconjunto que se admite. */
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
        // 1 in = 96 px = 72 pt; 36 pt = 48 px; 25,4 mm = 1 in.
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
        assertTrue(error("<svg " + NS + "/>").contains("no tiene tamaño"));
        assertTrue(error("<svg " + NS + " width=\"10\"/>").contains("solo tiene width y no tiene viewBox"));
        assertTrue(error("<svg " + NS + " width=\"50%\" height=\"10\"/>").contains("no es una medida absoluta"));
        assertTrue(error("<svg " + NS + " width=\"0\" height=\"10\"/>").contains("debe ser positivo"));
        assertTrue(error("<svg " + NS + " viewBox=\"0 0 10\"/>").contains("no son cuatro números"));
        assertTrue(error("<svg " + NS + " viewBox=\"0 0 0 10\"/>").contains("ancho y alto positivos"));
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
        assertTrue(error("<html " + NS + "/>").contains("no es un SVG: su raíz es <html>"));
        assertTrue(error("<svg width=\"10\" height=\"10\"/>").contains("no está en el espacio de nombres"));
        assertTrue(error("<svg " + NS + " width=\"10\" height=\"10\">").contains("no es un SVG bien formado (línea 1)"));
    }

    @Test
    void elementsOutsideTheSubset() throws IOException {
        for (String element : new String[] {"script", "style", "filter", "mask", "pattern", "marker", "image",
                "foreignObject", "a", "animate", "set", "symbol", "switch", "metadata"}) {
            String message = error("<svg " + NS + " width=\"10\" height=\"10\">\n<" + element + "/>\n</svg>");
            assertTrue(message.contains("(línea 2)") && message.contains("el elemento <" + element
                    + "> no está en el subconjunto"), element + ": " + message);
        }
        assertTrue(error("<svg " + NS + " width=\"10\" height=\"10\"><svg/></svg>")
                .contains("un <svg> dentro de otro"));
        assertTrue(error("<svg " + NS + " xmlns:x=\"urn:x\" width=\"10\" height=\"10\"><x:foo/></svg>")
                .contains("el elemento <x:foo> no es de SVG"));
        assertTrue(error("<svg " + NS + " width=\"10\" height=\"10\"><tspan>a</tspan></svg>")
                .contains("un <tspan> solo va dentro de un <text>"));
    }

    @Test
    void propertiesOutsideTheSubset() throws IOException {
        for (String property : new String[] {"filter", "mask", "marker-start"}) {
            String message = error("<svg " + NS + " width=\"10\" height=\"10\"><rect " + property
                    + "=\"url(#x)\"/></svg>");
            assertTrue(message.contains(property + " (filtros, máscaras y marcadores)"), message);
        }
        assertTrue(error("<svg " + NS + " width=\"10\" height=\"10\"><rect style=\"fill:red; filter:url(#f)\"/></svg>")
                .contains("filter"));
        assertTrue(error("<svg " + NS + " width=\"10\" height=\"10\"><rect onclick=\"x()\"/></svg>")
                .contains("manejador de eventos"));
        assertTrue(error("<svg " + NS + " xmlns:i=\"urn:i\" width=\"10\" height=\"10\"><rect i:x=\"1\"/></svg>")
                .contains("es de otro espacio de nombres"));
        assertTrue(error("<svg " + NS + " width=\"10\" height=\"10\"><rect style=\"fill\"/></svg>")
                .contains("no es propiedad: valor"));
    }

    @Test
    void externalReferencesAreErrors() throws IOException {
        String xlink = " xmlns:xlink=\"http://www.w3.org/1999/xlink\"";
        for (String use : new String[] {"<use xlink:href=\"logo.svg#mark\"/>", "<use href=\"https://example.com/a.svg\"/>",
                "<use href=\"file:///etc/passwd\"/>"}) {
            String message = error("<svg " + NS + xlink + " width=\"10\" height=\"10\">" + use + "</svg>");
            assertTrue(message.contains("sale del fichero"), message);
        }
        for (String fill : new String[] {"url(http://example.com/g.svg#g)", "url('other.svg#g')", "url( x.svg )"}) {
            String message = error("<svg " + NS + " width=\"10\" height=\"10\"><rect fill=\"" + fill + "\"/></svg>");
            assertTrue(message.contains("sale del fichero"), message);
        }
        assertTrue(error("<svg " + NS + " width=\"10\" height=\"10\"><rect style=\"fill:url(x.svg#g)\"/></svg>")
                .contains("sale del fichero"));
        assertTrue(error("<svg " + NS + " width=\"10\" height=\"10\"><rect href=\"#a\"/></svg>")
                .contains("href no tiene sentido en <rect>"));
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
        assertTrue(message.contains("no se admiten DTD ni entidades"), message);
    }

    @Test
    void textNeedsAFontOfTheFontsFolder() throws IOException {
        String head = "<svg " + NS + " width=\"100\" height=\"50\">";
        read(head + "<text font-family=\"IBM Plex Sans\">Hola</text></svg>");
        read(head + "<g font-family=\"'IBM Plex Serif'\" font-style=\"italic\"><text style=\"font-weight:bold\">"
                + "Hola <tspan font-family=\"IBM Plex Mono, monospace\" font-style=\"normal\">mundo</tspan></text></g></svg>");

        String unknown = error(head + "\n<text font-family=\"Comic Sans MS\">Hola</text></svg>");
        assertTrue(unknown.contains("El texto \"Hola\" del SVG image.svg (línea 2)") && unknown.contains("Fuente desconocida: "
                + "Comic Sans MS"), unknown);
        assertTrue(error(head + "<text>Hola</text></svg>").contains("no declara font-family"));
        assertTrue(error(head + "<text font-family=\"sans-serif\">Hola</text></svg>")
                .contains("familia genérica sans-serif"));
        // Solo cuenta la primera familia de la lista.
        read(head + "<text font-family=\"IBM Plex Sans, sans-serif\" font-style=\"italic\" "
                + "font-weight=\"bold\">Hola</text></svg>");
        String glyph = error(head + "<text font-family=\"IBM Plex Sans\">Hola 中</text></svg>");
        assertTrue(glyph.contains("'中' (U+4E2D)"), glyph);
        // Un tspan de otra fuente que no existe.
        assertTrue(error(head + "<text font-family=\"IBM Plex Sans\">Hola <tspan font-family=\"Nope\">x</tspan></text></svg>")
                .contains("Fuente desconocida: Nope"));
    }
}
