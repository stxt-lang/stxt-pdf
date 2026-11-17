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
            				Text position: superscript
            		For style: sub
            			Typography:
            				Text position: subscript
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

    private static final String HIGHLIGHT = """
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
            		For style: mark
            			Typography:
            				Highlight: #FFE680
            	Section:
            """;

    /** The {@code <rect>} elements of the SVG filled with the color of the highlight, in order. */
    private static java.util.List<String> highlights(String svg) {
        return Pattern.compile("<rect [^>]*#FFE680[^>]*/>").matcher(svg).results().map(r -> r.group()).toList();
    }

    /**
     * A highlight is one rectangle behind the run, also with marks inside it: as wide as the run, taller than its
     * letters and no taller than the font, painted before the text.
     */
    @Test
    void aHighlightIsARectangleBehindTheRun() {
        LaidOutDocument laidOut = new Engine().layOutDocument(
                new DocumentReader().read(HIGHLIGHT + "\t\tText: Vence el [15 de **mayo**]{mark} sin falta.\n"));
        String svg = SvgCanvas.page(laidOut.pages().get(0), laidOut.pageSetup(), false);
        java.util.List<String> rects = highlights(svg);
        assertEquals(1, rects.size(), svg);
        String rect = rects.get(0);
        assertTrue(svg.indexOf(rect) < svg.indexOf("<text "), svg);
        // From the start of the run to the start of the text after it.
        assertEquals(attribute(element(svg, "15 de "), "x"), attribute(rect, "x"), 0.011);
        assertEquals(attribute(element(svg, " sin falta."), "x"), attribute(rect, "x") + attribute(rect, "width"),
                0.011);
        // Around the baseline: above the capitals and below it, within the 15 of the line.
        double baseline = attribute(element(svg, "15 de "), "y");
        assertTrue(attribute(rect, "y") < baseline - 7, rect);
        assertTrue(attribute(rect, "y") + attribute(rect, "height") > baseline + 1, rect);
        assertTrue(attribute(rect, "height") < 15, rect);
    }

    /** A run that goes on in another line has one rectangle in each, and the lines are those of the text without it. */
    @Test
    void aHighlightThatSpansTwoLinesIsTwoRectangles() {
        String words = "uno dos tres cuatro cinco seis siete ocho nueve diez once doce trece catorce quince";
        LaidOutDocument laidOut = new Engine().layOutDocument(
                new DocumentReader().read(HIGHLIGHT + "\t\tText: [" + words + "]{mark}\n"));
        String svg = SvgCanvas.page(laidOut.pages().get(0), laidOut.pageSetup(), false);
        java.util.List<String> rects = highlights(svg);
        assertEquals(2, rects.size(), svg);
        assertEquals(20, attribute(rects.get(0), "x"), 0.011);
        assertEquals(20, attribute(rects.get(1), "x"), 0.011);
        assertEquals(attribute(rects.get(0), "y") + 15, attribute(rects.get(1), "y"), 0.011);
        LaidOutDocument plain = new Engine().layOutDocument(
                new DocumentReader().read(HIGHLIGHT + "\t\tText: " + words + "\n"));
        assertEquals(plain.pages().get(0).content().height(), laidOut.pages().get(0).content().height());
    }

    /** The highlight is not text: what is copied from the PDF is the text alone. */
    @Test
    void thePdfKeepsTheTextOfAHighlight() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.create().toPdf(new DocumentReader().read(HIGHLIGHT + "\t\tText: Vence el [15 de mayo]{mark}.\n"), out);
        try (PDDocument pdf = Loader.loadPDF(out.toByteArray())) {
            assertEquals("Vence el 15 de mayo.", new PDFTextStripper().getText(pdf).strip());
        }
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
