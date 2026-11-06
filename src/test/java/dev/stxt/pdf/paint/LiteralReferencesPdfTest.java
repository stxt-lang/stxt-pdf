package dev.stxt.pdf.paint;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.Pdfs;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.stxt.DocumentReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static dev.stxt.pdf.PdfStructures.section;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The references written as text (reference/common/references.md) in what is painted and in what is read from the
 * PDF: the text, the bookmarks and the replacement text of a text in another case have the brace, and nothing has
 * the mark the engine keeps it with while it lays out.
 */
class LiteralReferencesPdfTest {

    private static final String DOCUMENT = Docs.header(300, 200, 20) + Docs.TYPOGRAPHY
            + "\tHeading 1:\n\t\tText: Uso de {{page}}\n"
            + "\tParagraph: Hoja {page} y no {{page of: x}}.\n"
            + "\tText: total {{pages}}\n\t\tTypography:\n\t\t\tCase: uppercase\n";

    private static void actualTexts(PDStructureElement e, List<String> texts) {
        if (e.getActualText() != null) {
            texts.add(e.getActualText());
        }
        for (Object kid : e.getKids()) {
            if (kid instanceof PDStructureElement child) {
                actualTexts(child, texts);
            }
        }
    }

    @Test
    void thePdfHasTheBracesInItsTextItsBookmarksAndItsReplacementText() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(DOCUMENT))) {
            String text = new PDFTextStripper().getText(pdf).replace("\r", "");
            assertTrue(text.contains("Uso de {page}\n"), text);
            assertTrue(text.contains("Hoja 1 y no {page of: x}."), text);
            assertTrue(text.contains("TOTAL {PAGES}"), text);
            assertEquals("Uso de {page}", pdf.getDocumentCatalog().getDocumentOutline().getFirstChild().getTitle());
            List<String> actual = new ArrayList<>();
            actualTexts(section(pdf), actual);
            assertEquals(List.of("total {pages}"), actual);
        }
    }

    @Test
    void theSvgHasTheBracesToo() {
        Converter converter = Converter.create();
        String svg = converter.toSvg(converter.layOut(new DocumentReader().read(Docs.sectioned(DOCUMENT)))).get(0);
        assertFalse(svg.indexOf(dev.stxt.pdf.model.Reference.LITERAL_BRACE) >= 0);
        assertTrue(svg.contains("{page}"), svg);
    }
}
