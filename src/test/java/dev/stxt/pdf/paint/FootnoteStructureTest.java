package dev.stxt.pdf.paint;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.Pdfs;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.golden.PdfUa;
import dev.stxt.pdf.stxt.DocumentReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static dev.stxt.pdf.PdfStructures.kids;
import static dev.stxt.pdf.PdfStructures.section;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The footnotes in the tagged PDF (reference/implementation/pdf.md): the note after the paragraph that calls it, as a
 * {@code Note} (PDF/UA-1) or {@code FENote} (PDF/UA-2) with its label, and the call as a {@code Reference}.
 */
class FootnoteStructureTest {

    private static final String DOCUMENT = Docs.header(300, 200) + Docs.TYPOGRAPHY + "\t\t\t\tMarkup: markdown\n"
            + "\tParagraph: Uno[^n] dos.\n\tParagraph: Tres.\n\tFootnote:\n\t\tId: n\n\t\tParagraph: La nota.\n";

    @Test
    void theNoteFollowsItsParagraphWithItsLabelAndTheCallIsAReference() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(DOCUMENT, 1))) {
            PDStructureElement section = section(pdf);
            assertEquals(List.of("P", "Note", "P"), kids(section));
            PDStructureElement p = (PDStructureElement) section.getKids().get(0);
            // "Uno", the call and " dos.": the call is a Reference with its own marked content.
            assertEquals(List.of("MCR", "Reference", "MCR"), kids(p));
            PDStructureElement reference = (PDStructureElement) p.getKids().get(1);
            assertEquals(List.of("MCR"), kids(reference));
            PDStructureElement note = (PDStructureElement) section.getKids().get(1);
            assertEquals("footnote-n", note.getElementIdentifier());
            assertEquals(List.of("Lbl", "P"), kids(note));
            assertEquals(List.of("MCR"), kids((PDStructureElement) note.getKids().get(0)));
            // The reference points to the note and the note to the reference.
            COSArray ref = reference.getCOSObject().getCOSArray(COSName.getPDFName("Ref"));
            assertNotNull(ref);
            assertSame(note.getCOSObject(), ref.getObject(0));
            COSArray back = note.getCOSObject().getCOSArray(COSName.getPDFName("Ref"));
            assertSame(reference.getCOSObject(), back.getObject(0));
            // The IDTree maps the identifier to the note.
            assertNotNull(pdf.getDocumentCatalog().getStructureTreeRoot().getIDTree());
            assertSame(note.getCOSObject(), pdf.getDocumentCatalog().getStructureTreeRoot().getIDTree()
                    .getValue("footnote-n").getCOSObject());
            String content = new String(pdf.getPage(0).getContents().readAllBytes(), StandardCharsets.ISO_8859_1);
            assertTrue(content.contains("/Reference <</MCID"), content);
            assertTrue(content.contains("/Lbl <</MCID"), content);
        }
    }

    /**
     * {@code Link: #id} to a note goes to the note in the footnote area of the page of its call
     * (reference/components/footnote.md), with the corner of its fragment like any other box.
     */
    @Test
    void aLinkToANoteGoesToTheNoteInTheFootnoteArea() throws Exception {
        String source = Docs.sectioned(DOCUMENT.replace("\tParagraph: Tres.\n", "\tParagraph: Ir a la nota\n\t\tLink: #n\n"));
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

    @Test
    void withPdfUa2TheNoteIsAnFENote() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(DOCUMENT, 2))) {
            PDStructureElement section = section(pdf);
            assertEquals(List.of("P", "FENote", "P"), kids(section));
            PDStructureElement note = (PDStructureElement) section.getKids().get(1);
            // The attribute, with its revision number after it: NoteType Footnote, owned by FENote.
            COSArray attributes = note.getCOSObject().getCOSArray(COSName.A);
            COSDictionary noteType = (COSDictionary) attributes.getObject(0);
            assertEquals("FENote", noteType.getNameAsString(COSName.O));
            assertEquals("Footnote", noteType.getNameAsString("NoteType"));
            assertEquals(List.of("Lbl", "P"), kids(note));
        }
    }
}
