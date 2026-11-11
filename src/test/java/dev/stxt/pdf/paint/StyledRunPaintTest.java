package dev.stxt.pdf.paint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import dev.stxt.InlineNode;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.layout.Engine;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.stxt.DocumentReader;

/**
 * Styled runs, {@code [text]{name}} (reference/common/text-formatting.md), painted: each one with the color, the font
 * and the letter spacing of its rule, and the rest of the text with those of its box.
 */
class StyledRunPaintTest {

    private static final String DOCUMENT = """
            Document (dev.stxt.pdf):
            	Language: es
            	Title: Prueba
            	Page:
            		Width: 300
            		Height: 100
            		Margin: 20
            	Styles:
            		For element: Document
            			Typography:
            				Font: IBM Plex Sans
            				Size: 10
            				Color: #222222
            				Markup: markdown
            		For style: price
            			Typography:
            				Color: #B3261E
            				Bold: true
            		For style: serif
            			Typography:
            				Font: IBM Plex Serif
            				Italic: true
            		For style: wide
            			Typography:
            				Letter spacing: 2
            				Case: uppercase
            	Section:
            		Text: Total [12,50 €]{price}, [en letra]{serif} y [aparte]{wide}.
            """;

    /** The {@code <text>} element of the SVG whose text is {@code text}. */
    private static String element(String svg, String text) {
        Matcher m = Pattern.compile("<text [^>]*>" + Pattern.quote(text) + "</text>").matcher(svg);
        assertTrue(m.find(), svg);
        return m.group();
    }

    @Test
    void eachRunIsPaintedWithItsStyle() {
        InlineNode input = new DocumentReader().read(DOCUMENT);
        LaidOutDocument laidOut = new Engine().layOutDocument(input);
        String svg = SvgCanvas.page(laidOut.pages().get(0), laidOut.pageSetup(), false);
        assertTrue(element(svg, "Total ").contains("#222222"), svg);
        String price = element(svg, "12,50 €");
        assertTrue(price.contains("#B3261E") && price.contains("font-weight=\"bold\""), price);
        String serif = element(svg, "en letra");
        assertTrue(serif.contains("font-family=\"IBM Plex Serif\"") && serif.contains("font-style=\"italic\"")
                && serif.contains("#222222"), serif);
        String wide = element(svg, "APARTE");
        assertTrue(wide.contains("letter-spacing=\"2.00\""), wide);
    }

    /** What is copied from the PDF is the text as written, with its case: the styles only change how it looks. */
    @Test
    void thePdfKeepsTheText() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.create().toPdf(new DocumentReader().read(DOCUMENT), out);
        try (PDDocument pdf = Loader.loadPDF(out.toByteArray())) {
            assertEquals("Total 12,50 €, en letra y APARTE.", new PDFTextStripper().getText(pdf).strip());
        }
    }

    private static final String SCRIPTS = """
            Document (dev.stxt.pdf):
            	Language: es
            	Title: Prueba
            	Page:
            		Width: 300
            		Height: 100
            		Margin: 20
            	Styles:
            		For element: Document
            			Typography:
            				Font: IBM Plex Sans
            				Size: 10
            				Line height: 15
            				Markup: markdown
            		For style: sup
            			Typography:
            				Position: superscript
            		For style: sub
            			Typography:
            				Position: subscript
            	Section:
            		Text: Son m[2]{sup} y CO[2]{sub}.
            		Text: Son m2 y CO2.
            """;

    private static double attribute(String element, String name) {
        Matcher m = Pattern.compile(" " + name + "=\"([-0-9.]+)\"").matcher(element);
        assertTrue(m.find(), element);
        return Double.parseDouble(m.group(1));
    }

    /**
     * A superscript and a subscript are painted at 70 % of the size, raised 0.33 or lowered 0.15 of the size of the box
     * from the baseline of their line, and the line is as tall as without them.
     */
    @Test
    void aSuperscriptAndASubscriptArePaintedSmallerAndOffTheBaseline() {
        LaidOutDocument laidOut = new Engine().layOutDocument(new DocumentReader().read(SCRIPTS));
        String svg = SvgCanvas.page(laidOut.pages().get(0), laidOut.pageSetup(), false);
        String plain = element(svg, "Son m");
        String sup = element(svg, "2");
        String sub = svg.substring(svg.indexOf(element(svg, " y CO")));
        sub = element(sub, "2");
        double baseline = attribute(plain, "y");
        assertEquals(10, attribute(plain, "font-size"), 0.001);
        assertEquals(7, attribute(sup, "font-size"), 0.001);
        assertEquals(baseline - 3.3, attribute(sup, "y"), 0.011);
        assertEquals(7, attribute(sub, "font-size"), 0.001);
        assertEquals(baseline + 1.5, attribute(sub, "y"), 0.011);
        // The second text starts one line below: the first is 15 tall.
        assertEquals(baseline + 15, attribute(element(svg, "Son m2 y CO2."), "y"), 0.011);
    }

    /** What is copied is the text, with the superscript and the subscript in their place. */
    @Test
    void thePdfKeepsTheTextOfASuperscript() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.create().toPdf(new DocumentReader().read(SCRIPTS), out);
        try (PDDocument pdf = Loader.loadPDF(out.toByteArray())) {
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("Son m2 y CO2."), text);
        }
    }
}
