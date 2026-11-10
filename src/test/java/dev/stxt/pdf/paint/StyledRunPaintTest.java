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
}
