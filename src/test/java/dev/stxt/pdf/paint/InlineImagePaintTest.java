package dev.stxt.pdf.paint;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.Pdfs;
import dev.stxt.pdf.golden.PdfUa;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureNode;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The PDF of an image inside a text (reference/common/text-formatting.md): drawn in the place of its run, a
 * {@code Figure} with its {@code Alt} inside the element of the text (or inside a {@code Link}), an artifact when it
 * is decorative, never a character of the text; valid PDF/UA-1 and PDF/UA-2.
 */
class InlineImagePaintTest {

    private static final String DOCUMENT = Docs.header(300, 300, 20) + Docs.TYPOGRAPHY + "\t\t\t\tMarkup: markdown\n"
            + "\tHeading 1:\n\t\tText: Informe ![Logo](test/shapes.svg) anual\n"
            + "\tParagraph: Ver ![Un cuadrado](test/square.png) y ![](test/check.svg) o "
            + "[![Web](test/shapes.svg)](https://stxt.dev) y ![Alta](test/tall.jpg) al final.\n";

    /** The structure types of the kids of {@code node}, in order, with the {@code Alt} of the figures. */
    private static List<String> kids(PDStructureNode node) {
        List<String> out = new ArrayList<>();
        for (Object kid : node.getKids()) {
            if (kid instanceof PDStructureElement e) {
                out.add(e.getStructureType() + (e.getAlternateDescription() != null
                        ? "(" + e.getAlternateDescription() + ")" : ""));
            } else {
                out.add("content");
            }
        }
        return out;
    }

    @Test
    void itPassesPdfUa1And2() throws Exception {
        assertEquals(Map.of(), PdfUa.failedRules(Pdfs.of(DOCUMENT, 1), 1));
        assertEquals(Map.of(), PdfUa.failedRules(Pdfs.of(DOCUMENT, 2), 2));
    }

    @Test
    void theImageIsNotACharacterOfTheText() throws Exception {
        try (PDDocument doc = Loader.loadPDF(Pdfs.of(DOCUMENT, 1))) {
            String text = new PDFTextStripper().getText(doc);
            assertFalse(text.contains("￼"), text);
            assertTrue(text.contains("Ver  y  o  y  al final."), text);
            PDOutlineItem heading = doc.getDocumentCatalog().getDocumentOutline().getFirstChild();
            assertEquals("Informe anual", heading.getTitle());
        }
    }

    @Test
    void theFiguresHangFromTheTextInReadingOrder() throws Exception {
        try (PDDocument doc = Loader.loadPDF(Pdfs.of(DOCUMENT, 1))) {
            PDStructureElement document = (PDStructureElement) doc.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0);
            PDStructureElement section = (PDStructureElement) document.getKids().get(0);
            PDStructureElement h1 = (PDStructureElement) section.getKids().get(0);
            assertEquals("H1", h1.getStructureType());
            // The Text of a heading is a Span inside its H1.
            PDStructureElement span = (PDStructureElement) h1.getKids().get(0);
            assertEquals(List.of("content", "Figure(Logo)", "content"), kids(span));
            PDStructureElement p = (PDStructureElement) section.getKids().get(1);
            assertEquals("P", p.getStructureType());
            // The decorative image is an artifact without an element, which splits the text around it in two
            // marked contents; the linked one hangs from its Link.
            assertEquals(List.of("content", "Figure(Un cuadrado)", "content", "content", "Link", "content",
                    "Figure(Alta)", "content"), kids(p));
            PDStructureElement link = (PDStructureElement) p.getKids().get(4);
            assertEquals(List.of("Figure(Web)", "content"), kids(link));
            assertEquals(1, doc.getPage(0).getAnnotations().size(), "the link of the image");
        }
    }
}
