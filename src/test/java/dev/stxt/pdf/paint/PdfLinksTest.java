package dev.stxt.pdf.paint;

import static dev.stxt.pdf.PdfStructures.document;
import static dev.stxt.pdf.PdfStructures.kids;
import static dev.stxt.pdf.PdfStructures.section;
import static dev.stxt.pdf.PdfStructures.types;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDObjectReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.Pdfs;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.golden.PdfUa;
import dev.stxt.pdf.stxt.DocumentReader;

/**
 * The link annotations of the PDF (reference/implementation/pdf.md, reference/common/links.md): one per clickable
 * area, borderless, with a {@code URI} action or a destination to a box of the document. The {@code Link} elements
 * they hang from are in {@link PdfStructureLinksTest}.
 */
class PdfLinksTest {

    private static final String DOCUMENT = Docs.header(300, 200, 20) + Docs.TYPOGRAPHY;

    private static List<PDAnnotationLink> links(PDDocument pdf, int page) throws Exception {
        return pdf.getPage(page).getAnnotations().stream().map(a -> (PDAnnotationLink) a).toList();
    }

    private static String uri(PDAnnotationLink link) {
        return ((PDActionURI) link.getAction()).getURI();
    }

    /**
     * A link to a URL is a borderless {@code Link} annotation with its {@code URI} action, the
     * size of the box or the run. In the tagged PDF, the {@code Link} wraps what is clicked
     * (the text of the box, the {@code Figure} of the image, the text of the run inside the paragraph) and
     * carries the {@code OBJR} of the annotation; the one of a {@code Page overlay}, which is not content, is a
     * {@code Link} without content in its place in the tree, with its {@code Alt} (reference/implementation/pdf.md).
     */
    @Test
    void externalLinksAreAnnotationsTaggedAsLinks() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(DOCUMENT
                + "\tText: Escribe a soporte\n\t\tLink: mailto:soporte@airemas.example\n"
                + "\tImage: test/wide.png\n\t\tAlt: Logotipo\n\t\tLink: https://stxt.dev\n"
                + "\tPage overlay:\n\t\tOffset: auto 0 0 auto\n\t\tText: pie\n\t\t\tLink: https://pie.example\n"
                + "\tMarkdown:\n\t\tContent >>\n\t\t\tVisita [la web](https://web.example) hoy.\n"))) {
            List<PDAnnotationLink> links = links(pdf, 0);
            // A Page overlay is painted last, on top of everything.
            assertEquals(List.of("mailto:soporte@airemas.example", "https://stxt.dev", "https://web.example",
                    "https://pie.example"), links.stream().map(PdfLinksTest::uri).toList());
            PDAnnotationLink text = links.get(0);
            // The border box of the text: the whole page area wide, 12 pt tall.
            assertEquals(20, text.getRectangle().getLowerLeftX(), 0.01);
            assertEquals(200 - 20 - 12, text.getRectangle().getLowerLeftY(), 0.01);
            assertEquals(260, text.getRectangle().getWidth(), 0.01);
            assertEquals(12, text.getRectangle().getHeight(), 0.01);
            assertEquals("Escribe a soporte", text.getContents());
            assertEquals(0, text.getBorderStyle().getWidth(), 0.001);
            assertEquals("Logotipo", links.get(1).getContents());
            // The Markdown run with a link does not take the whole line: "la web" and nothing else.
            PDRectangle run = links.get(2).getRectangle();
            assertTrue(run.getWidth() > 20 && run.getWidth() < 60, run.toString());
            assertEquals("la web", links.get(2).getContents());
            // PDF/UA: the four are on the page with the annotations traversed in structure order.
            assertEquals("S", pdf.getPage(0).getCOSObject().getNameAsString("Tabs"));

            PDStructureTreeRoot tree = pdf.getDocumentCatalog().getStructureTreeRoot();
            PDStructureElement document = section(pdf);
            // The Link of the Page overlay wraps no content, but it is in its place in the section: after the image.
            assertEquals(List.of("Sect"), types(document(pdf)));
            assertEquals(List.of("P", "Link", "Link", "Div"), types(document));
            // The box with Link: P → Link → [marked content of the text, OBJR]; no Alt, the text already says it.
            PDStructureElement span = (PDStructureElement) document.getKids().get(0);
            assertEquals(List.of("Link"), types(span));
            PDStructureElement link = (PDStructureElement) span.getKids().get(0);
            assertEquals(List.of("MCR", "OBJR"), kids(link));
            assertNull(link.getAlternateDescription());
            PDObjectReference reference = (PDObjectReference) link.getKids().get(1);
            assertEquals(text.getCOSObject(), ((PDAnnotationLink) reference.getReferencedObject()).getCOSObject());
            // The parent tree leads from the annotation to its Link.
            assertEquals(link.getCOSObject(), ((COSObjectable) tree.getParentTree().getValue(text.getStructParent())).getCOSObject());
            // The image: Link → Figure (with its Alt) and the OBJR.
            PDStructureElement imageLink = (PDStructureElement) document.getKids().get(1);
            assertEquals(List.of("Figure", "OBJR"), kids(imageLink));
            assertEquals("Logotipo", ((PDStructureElement) imageLink.getKids().get(0)).getAlternateDescription());
            assertNull(imageLink.getAlternateDescription());
            // The Markdown run: P → [text, Link → [link text, OBJR], text].
            PDStructureElement paragraph = (PDStructureElement) ((PDStructureElement) document.getKids().get(3))
                    .getKids().get(0);
            assertEquals("P", paragraph.getStructureType());
            assertEquals(List.of("MCR", "Link", "MCR"), kids(paragraph));
            assertEquals(List.of("MCR", "OBJR"), kids((PDStructureElement) paragraph.getKids().get(1)));
            // The Page overlay: a Link without content, with the text as Alt and the OBJR.
            PDStructureElement overlay = (PDStructureElement) document.getKids().get(2);
            assertEquals(List.of("OBJR"), kids(overlay));
            assertEquals("pie", overlay.getAlternateDescription());
            assertEquals(overlay.getCOSObject(), ((COSObjectable) tree.getParentTree()
                    .getValue(links.get(3).getStructParent())).getCOSObject());
            String extracted = new PDFTextStripper().getText(pdf);
            assertTrue(extracted.contains("Visita la web hoy."), extracted);
        }
    }

    /**
     * A link to a box of the document leads to its page and its corner, forward and
     * backward (reference/common/links.md).
     */
    @Test
    void internalLinksGoToThePageOfTheirBox() throws Exception {
        // Page area of 160: the text (12) and the block (140) fill the first page; the rest, the second.
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(DOCUMENT
                + "\tText: Ir al final\n\t\tId: inicio\n\t\tLink: #fin\n"
                + "\tBlock:\n\t\tBox:\n\t\t\tHeight: 140\n"
                + "\tText: Final\n\t\tId: fin\n"
                + "\tText: Volver\n\t\tLink: #inicio\n"))) {
            assertEquals(2, pdf.getNumberOfPages());
            PDPageXYZDestination forward = (PDPageXYZDestination) links(pdf, 0).get(0).getDestination();
            assertEquals(1, pdf.getPages().indexOf(forward.getPage()));
            assertEquals(20, forward.getLeft());
            assertEquals(200 - 20, forward.getTop());
            PDPageXYZDestination backward = (PDPageXYZDestination) links(pdf, 1).get(0).getDestination();
            assertEquals(0, pdf.getPages().indexOf(backward.getPage()));
            // A destination action, not a URI one.
            assertNull(links(pdf, 0).get(0).getAction());
        }
    }

    /**
     * Each table of contents entry leads to the page of its heading and its {@code TOCI} has the {@code Link}
     * (reference/components/table-of-contents.md and reference/common/links.md).
     */
    @Test
    void contentsEntriesLinkToTheirHeadings() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(DOCUMENT
                + "\tTable of contents:\n\t\tLevels: 1 1\n"
                + "\tHeading 1:\n\t\tText: Uno\n"
                + "\tBlock:\n\t\tBreak:\n\t\t\tAfter: page\n"
                + "\tHeading 1:\n\t\tText: Dos\n"))) {
            assertEquals(2, pdf.getNumberOfPages());
            List<PDAnnotationLink> links = links(pdf, 0);
            assertEquals(2, links.size());
            assertEquals(List.of(0, 1), links.stream().map(l -> {
                try {
                    return pdf.getPages().indexOf(((PDPageXYZDestination) l.getDestination()).getPage());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).toList());
            // The text of the entry, without its page number.
            assertEquals(List.of("Uno", "Dos"), links.stream().map(PDAnnotation::getContents).toList());
            PDStructureElement toc = (PDStructureElement) section(pdf).getKids().get(0);
            toc.getKids().forEach(k -> assertTrue(types((PDStructureElement) k).contains("Link"),
                    () -> "types: " + types((PDStructureElement) k)));
        }
    }

    /**
     * {@code Link: #id} to a note goes to the note in the footnote area of the page of its call
     * (reference/components/footnote.md), with the corner of its fragment like any other box.
     */
    @Test
    void aLinkToANoteGoesToTheNoteInTheFootnoteArea() throws Exception {
        String source = Docs.sectioned(DOCUMENT + "\t\t\t\tMarkup: markdown\n"
                + "\tParagraph: Uno[^n] dos.\n\tParagraph: Ir a la nota\n\t\tLink: #n\n"
                + "\tFootnote:\n\t\tId: n\n\t\tParagraph: La nota.\n");
        Converter converter = Converter.builder().build();
        Page page = converter.layOut(new DocumentReader().read(source)).pages().get(0);
        Fragment area = page.footnotes();
        Fragment note = area.children().get(0);
        double noteY = page.pageSetup().pageArea().y() + area.y() + area.border().top() + area.padding().top()
                + note.y();
        for (int ua = 1; ua <= 2; ua++) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Converter.builder().pdfUa(ua).build().toPdf(new DocumentReader().read(source), out);
            byte[] pdf = out.toByteArray();
            try (PDDocument doc = Loader.loadPDF(pdf)) {
                PDAnnotationLink link = (PDAnnotationLink) doc.getPage(0).getAnnotations().get(0);
                PDPageXYZDestination destination = (PDPageXYZDestination) (ua == 1 ? link.getDestination()
                        : ((PDActionGoTo) link.getAction()).getDestination());
                assertSame(doc.getPage(0).getCOSObject(), destination.getPage().getCOSObject());
                assertEquals((int) Math.ceil(page.pageSetup().height() - noteY), destination.getTop());
            }
            assertEquals(Map.of(), PdfUa.failedRules(pdf, ua));
        }
    }
}
