package dev.stxt.pdf.paint;

import static dev.stxt.pdf.PdfStructures.section;
import static dev.stxt.pdf.PdfStructures.types;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.jupiter.api.Test;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.Pdfs;

/**
 * The bookmarks of the PDF (reference/implementation/pdf.md, "Bookmarks"): one per heading, nested by level, that
 * lead to the corner of their first fragment.
 */
class PdfOutlineTest {

    private static final String DOCUMENT = Docs.header(300, 200, 20) + Docs.TYPOGRAPHY;

    private static final String HEADINGS = Docs.header(300, 400, 40) + Docs.TYPOGRAPHY
            + "\tPage overlay:\n\t\tOffset: auto 0 -30 0\n\t\tText: Pie\n"
            + "\tHeading 1:\n\t\tText: Uno\n"
            + "\tParagraph: Hola.\n"
            + "\tHeading 2:\n\t\tText: Uno\n\t\tText: punto uno\n"
            + "\tParagraph: Adiós.\n\t\tBreak:\n\t\t\tAfter: page\n"
            + "\tHeading 1:\n\t\tText: Dos\n";

    @Test
    void headingsBecomeNestedBookmarks() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(HEADINGS))) {
            assertEquals(2, pdf.getNumberOfPages());
            PDDocumentOutline outline = pdf.getDocumentCatalog().getDocumentOutline();
            PDOutlineItem one = outline.getFirstChild();
            assertEquals("Uno", one.getTitle());
            assertEquals("Uno punto uno", one.getFirstChild().getTitle());
            assertEquals(one.getFirstChild(), one.getLastChild());
            PDOutlineItem two = one.getNextSibling();
            assertEquals("Dos", two.getTitle());
            assertEquals(two, outline.getLastChild());
            assertEquals(0, ((PDPageDestination) one.getDestination()).retrievePageNumber());
            PDPageXYZDestination target = (PDPageXYZDestination) two.getDestination();
            assertEquals(1, target.retrievePageNumber());
            // The top corner of the heading: the page area starts 40 pt below the edge.
            assertEquals(400 - 40, target.getTop());
            assertEquals(40, target.getLeft());
        }
    }

    /**
     * A heading without an entry in the table of contents is still an {@code H1} and a bookmark
     * (reference/components/heading.md and reference/components/table-of-contents.md).
     */
    @Test
    void aHeadingOutOfTheContentsIsStillAHeadingAndABookmark() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(DOCUMENT
                + "\tHeading 1:\n\t\tIn table of contents: false\n\t\tText: Portada\n"
                + "\tTable of contents:\n\t\tLevels: 1 1\n"
                + "\tHeading 1:\n\t\tText: Uno\n"))) {
            PDStructureElement document = section(pdf);
            assertEquals(List.of("H1", "TOC", "H1"), types(document));
            PDStructureElement toc = (PDStructureElement) document.getKids().get(1);
            assertEquals(1, types(toc).size());
            List<String> bookmarks = new ArrayList<>();
            pdf.getDocumentCatalog().getDocumentOutline().children().forEach(i -> bookmarks.add(i.getTitle()));
            assertEquals(List.of("Portada", "Uno"), bookmarks);
        }
    }

    /**
     * The number of a numbered heading is part of its title: also of its bookmark and its table of contents entry
     * (reference/components/heading.md).
     */
    @Test
    void theNumberOfAHeadingIsPartOfItsBookmarkAndItsContentsEntry() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(Docs.mergeStyles(DOCUMENT
                + "\tStyles:\n\t\tFor element: Heading 1\n\t\t\tTypography:\n\t\t\t\tNumbered: true\n"
                + "\t\tFor element: Heading 2\n\t\t\tTypography:\n\t\t\t\tNumbered: true\n"
                + "\tHeading 1:\n\t\tIn table of contents: false\n\t\tText: Portada\n\t\tTypography:\n\t\t\tNumbered: false\n"
                + "\tTable of contents:\n\t\tLevels: 1 2\n"
                + "\tHeading 1:\n\t\tText: Uno\n"
                + "\tHeading 2:\n\t\tText: Dos\n")))) {
            PDOutlineItem first = pdf.getDocumentCatalog().getDocumentOutline().getFirstChild();
            assertEquals("Portada", first.getTitle());
            // The cover is neither numbered nor counted: Uno is 1, and Dos, 1.1, nested in it.
            PDOutlineItem one = first.getNextSibling();
            assertEquals("1 Uno", one.getTitle());
            assertEquals("1.1 Dos", one.getFirstChild().getTitle());
        }
    }
}
