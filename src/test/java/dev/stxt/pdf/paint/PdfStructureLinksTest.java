package dev.stxt.pdf.paint;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.Pdfs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDObjectReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.stxt.pdf.PdfStructures.document;
import static dev.stxt.pdf.PdfStructures.kids;
import static dev.stxt.pdf.PdfStructures.section;
import static dev.stxt.pdf.PdfStructures.types;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code Link} elements of the structure tree (reference/implementation/pdf.md, "Links"): what each one wraps,
 * the {@code OBJR} of its annotations and the ones that stand in for what is not content. The annotations themselves
 * are in {@link PdfLinksTest}.
 */
class PdfStructureLinksTest {

    private static final String DOCUMENT = Docs.header(300, 200, 20) + Docs.TYPOGRAPHY;

    private static List<PDAnnotationLink> links(PDDocument pdf, int page) throws Exception {
        return pdf.getPage(page).getAnnotations().stream().map(a -> (PDAnnotationLink) a).toList();
    }

    private static String uri(PDAnnotationLink link) {
        return ((PDActionURI) link.getAction()).getURI();
    }

    /**
     * A text link that spans several lines is a single {@code Link} with one marked content and one
     * {@code OBJR} per line; there is one area per line (reference/implementation/pdf.md).
     */
    @Test
    void aLinkOverSeveralLinesIsOneLinkElement() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(DOCUMENT.replace("Width: 300", "Width: 100")
                + "\tMarkdown:\n\t\tContent >>\n\t\t\tAntes [un enlace que ocupa bastantes líneas de texto](https://a.example) después.\n"))) {
            List<PDAnnotationLink> links = links(pdf, 0);
            assertTrue(links.size() >= 3, "" + links.size());
            PDStructureElement paragraph = (PDStructureElement) ((PDStructureElement) section(pdf).getKids().get(0))
                    .getKids().get(0);
            assertEquals(List.of("MCR", "Link", "MCR"), kids(paragraph));
            List<String> link = kids((PDStructureElement) paragraph.getKids().get(1));
            assertEquals(List.of("MCR"), link.subList(0, 1));
            assertEquals(links.size(), link.stream().filter("OBJR"::equals).count());
        }
    }

    /**
     * A box with {@code Link} that contains others (a block with two texts) wraps them in its
     * {@code Link}: {@code Div → Link → [P, P, OBJR]}; with nothing to read inside, the {@code Link}
     * carries its text as {@code Alt}.
     */
    @Test
    void aBoxLinkWrapsItsChildren() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(DOCUMENT
                + "\tBlock:\n\t\tLink: https://a.example\n\t\tText: Uno\n\t\tText: Dos\n"
                + "\tBlock:\n\t\tLink: https://b.example\n\t\tBox:\n\t\t\tHeight: 10\n\t\t\tBackground: #CCC\n"))) {
            PDStructureElement document = section(pdf);
            assertEquals(List.of("Div", "Div"), types(document));
            PDStructureElement full = (PDStructureElement) document.getKids().get(0);
            assertEquals(List.of("Link"), types(full));
            PDStructureElement link = (PDStructureElement) full.getKids().get(0);
            assertEquals(List.of("P", "P", "OBJR"), kids(link));
            assertNull(link.getAlternateDescription());
            PDStructureElement empty = (PDStructureElement) document.getKids().get(1);
            PDStructureElement emptyLink = (PDStructureElement) empty.getKids().get(0);
            assertEquals(List.of("OBJR"), kids(emptyLink));
            assertEquals("https://b.example", emptyLink.getAlternateDescription());
        }
    }

    /**
     * The header of a table is repeated on each page: its link is clickable on all of them, but
     * it is content (and wraps the text of the cell) only on the first; the annotations of the others go to the same
     * {@code Link} (reference/implementation/pdf.md).
     */
    @Test
    void aLinkInARepeatedTableHeaderIsTaggedOnce() throws Exception {
        StringBuilder body = new StringBuilder();
        for (int i = 1; i <= 15; i++) {
            body.append("\t\t\tRow:\n\t\t\t\tCell: Fila ").append(i).append("\n\t\t\t\tCell: ").append(i).append('\n');
        }
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(DOCUMENT
                + "\tTable:\n\t\tColumn sizes: * 60\n"
                + "\t\tHeader rows:\n\t\t\tRow:\n\t\t\t\tCell: Concepto\n\t\t\t\t\tLink: https://c.example\n\t\t\t\tCell: Valor\n"
                + "\t\tBody rows:\n" + body))) {
            assertEquals(2, pdf.getNumberOfPages());
            assertEquals(List.of("https://c.example"), links(pdf, 0).stream().map(PdfStructureLinksTest::uri).toList());
            assertEquals(List.of("https://c.example"), links(pdf, 1).stream().map(PdfStructureLinksTest::uri).toList());
            PDStructureElement outer = document(pdf);
            PDStructureElement table = (PDStructureElement) section(pdf).getKids().get(0);
            PDStructureElement headRow = (PDStructureElement) ((PDStructureElement) table.getKids().get(0)).getKids().get(0);
            PDStructureElement th = (PDStructureElement) headRow.getKids().get(0);
            assertEquals(List.of("Link"), types(th));
            // The Link of the cell wraps the text of the first page and carries the annotations of both.
            PDStructureElement cellLink = (PDStructureElement) th.getKids().get(0);
            assertEquals(List.of("MCR", "OBJR", "OBJR"), kids(cellLink));
            assertEquals(pdf.getPage(0), ((PDObjectReference) cellLink.getKids().get(1)).getPage());
            assertEquals(pdf.getPage(1), ((PDObjectReference) cellLink.getKids().get(2)).getPage());
            assertEquals(List.of("Sect"), types(outer));
            PDStructureTreeRoot tree = pdf.getDocumentCatalog().getStructureTreeRoot();
            assertEquals(cellLink.getCOSObject(), ((COSObjectable) tree.getParentTree()
                    .getValue(links(pdf, 1).get(0).getStructParent())).getCOSObject());
        }
    }

    /**
     * What is not content has nothing to wrap: the link of a {@code Page overlay} or of a decorative image is a
     * {@code Link} without content in its place in the tree, with one {@code OBJR} per page, and it is announced once
     * in its reading order (reference/implementation/pdf.md).
     */
    @Test
    void theLinkOfWhatIsNotContentIsInItsPlaceOnce() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(DOCUMENT
                + "\tPage overlay:\n\t\tOffset: auto 0 0 auto\n\t\tText: Más en [la web](https://pie.example)\n"
                + "\t\t\tTypography:\n\t\t\t\tMarkup: markdown\n"
                + "\tText: Uno\n"
                + "\tImage: test/wide.png\n\t\tLink: https://logo.example\n"
                + "\tText: Dos\n\t\tBreak:\n\t\t\tBefore: page\n"))) {
            assertEquals(2, pdf.getNumberOfPages());
            assertEquals(List.of("Sect"), types(document(pdf)));
            PDStructureElement section = section(pdf);
            // The Page overlay is before the text in the document, and the image between the two texts.
            assertEquals(List.of("Link", "P", "Link", "P"), types(section));
            PDStructureElement footer = (PDStructureElement) section.getKids().get(0);
            assertEquals(List.of("OBJR", "OBJR"), kids(footer));
            assertEquals("la web", footer.getAlternateDescription());
            assertEquals(pdf.getPage(0), ((PDObjectReference) footer.getKids().get(0)).getPage());
            assertEquals(pdf.getPage(1), ((PDObjectReference) footer.getKids().get(1)).getPage());
            PDStructureElement logo = (PDStructureElement) section.getKids().get(2);
            assertEquals(List.of("OBJR"), kids(logo));
            assertEquals("https://logo.example", logo.getAlternateDescription());
        }
    }

    /** A {@code Page overlay} that appears on no page leaves no empty {@code Link}. */
    @Test
    void aPageOverlayThatIsNeverPaintedLeavesNoEmptyLink() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(DOCUMENT
                + "\tPage overlay:\n\t\tPages: even\n\t\tText: pie\n\t\t\tLink: https://pie.example\n"
                + "\tText: Uno\n"))) {
            assertEquals(List.of("P"), types(section(pdf)));
        }
    }
}
