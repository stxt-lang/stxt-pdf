package dev.stxt.pdf.paint;

import dev.stxt.InlineNode;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.stxt.DocumentReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkedContentReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The footnotes in the tagged PDF (reference/implementation/pdf.md): the note after the paragraph that calls it, as a
 * {@code Note} (PDF/UA-1) or {@code FENote} (PDF/UA-2) with its label, and the call as a {@code Reference}.
 */
class FootnoteStructureTest {

    private static final String DOCUMENT = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 200\n"
            + "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n\t\t\t\tMarkup: markdown\n"
            + "\tParagraph: Uno[^n] dos.\n\tParagraph: Tres.\n\tFootnote:\n\t\tId: n\n\t\tParagraph: La nota.\n";

    private static byte[] render(int ua) throws Exception {
        InlineNode document = new DocumentReader().read(Docs.sectioned(DOCUMENT));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.builder().pdfUa(ua).build().toPdf(document, out);
        return out.toByteArray();
    }

    private static List<String> types(PDStructureElement e) {
        return e.getKids().stream().map(k -> k instanceof PDStructureElement s ? s.getStructureType()
                : k instanceof PDMarkedContentReference ? "MCR" : k.getClass().getSimpleName()).toList();
    }

    private static PDStructureElement section(PDDocument pdf) {
        PDStructureTreeRoot root = pdf.getDocumentCatalog().getStructureTreeRoot();
        PDStructureElement document = (PDStructureElement) root.getKids().get(0);
        return (PDStructureElement) document.getKids().get(0);
    }

    @Test
    void theNoteFollowsItsParagraphWithItsLabelAndTheCallIsAReference() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(render(1))) {
            PDStructureElement section = section(pdf);
            assertEquals(List.of("P", "Note", "P"), types(section));
            PDStructureElement p = (PDStructureElement) section.getKids().get(0);
            // "Uno", the call and " dos.": the call is a Reference with its own marked content.
            assertEquals(List.of("MCR", "Reference", "MCR"), types(p));
            PDStructureElement reference = (PDStructureElement) p.getKids().get(1);
            assertEquals(List.of("MCR"), types(reference));
            PDStructureElement note = (PDStructureElement) section.getKids().get(1);
            assertEquals("footnote-n", note.getElementIdentifier());
            assertEquals(List.of("Lbl", "P"), types(note));
            assertEquals(List.of("MCR"), types((PDStructureElement) note.getKids().get(0)));
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

    @Test
    void withPdfUa2TheNoteIsAnFENote() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(render(2))) {
            PDStructureElement section = section(pdf);
            assertEquals(List.of("P", "FENote", "P"), types(section));
            PDStructureElement note = (PDStructureElement) section.getKids().get(1);
            // The attribute, with its revision number after it: NoteType Footnote, owned by FENote.
            COSArray attributes = note.getCOSObject().getCOSArray(COSName.A);
            COSDictionary noteType = (COSDictionary) attributes.getObject(0);
            assertEquals("FENote", noteType.getNameAsString(COSName.O));
            assertEquals("Footnote", noteType.getNameAsString("NoteType"));
            assertEquals(List.of("Lbl", "P"), types(note));
        }
    }
}
