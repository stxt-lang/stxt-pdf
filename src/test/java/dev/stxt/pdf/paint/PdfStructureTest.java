package dev.stxt.pdf.paint;

import static dev.stxt.pdf.PdfStructures.kids;
import static dev.stxt.pdf.PdfStructures.section;
import static dev.stxt.pdf.PdfStructures.types;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkedContentReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDLayoutAttributeObject;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDListAttributeObject;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDTableAttributeObject;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.Pdfs;
import dev.stxt.pdf.golden.PdfUa;

/**
 * The structure tree of the tagged PDF (reference/implementation/pdf.md): one element per box in document order,
 * with the marked content of its text hanging from it, and what is not content as an artifact. The {@code Link}
 * elements are in {@link PdfStructureLinksTest}; what PDF/UA-2 does differently, in {@link PdfUa2Test}.
 */
class PdfStructureTest {

    private static final String DOCUMENT = Docs.header(300, 200, 20) + Docs.TYPOGRAPHY;

    private static final String HEADINGS = Docs.header(300, 400, 40) + Docs.TYPOGRAPHY
            + "\tPage overlay:\n\t\tOffset: auto 0 -30 0\n\t\tText: Pie\n"
            + "\tHeading 1:\n\t\tText: Uno\n"
            + "\tParagraph: Hola.\n"
            + "\tHeading 2:\n\t\tText: Uno\n\t\tText: punto uno\n"
            + "\tParagraph: Adiós.\n\t\tBreak:\n\t\t\tAfter: page\n"
            + "\tHeading 1:\n\t\tText: Dos\n";

    @Test
    void theDocumentIsTagged() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(HEADINGS))) {
            assertTrue(pdf.getDocumentCatalog().getMarkInfo().isMarked());
            PDStructureTreeRoot treeRoot = pdf.getDocumentCatalog().getStructureTreeRoot();
            assertEquals("Document", ((PDStructureElement) treeRoot.getKids().get(0)).getStructureType());
            PDStructureElement document = section(pdf);
            assertEquals("Sect", document.getStructureType());
            // The Page overlay is an artifact: it is not in the tree. The order is the document order.
            assertEquals(List.of("H1", "P", "H2", "P", "H1"), types(document));
            PDStructureElement h2 = (PDStructureElement) document.getKids().get(2);
            assertEquals(List.of("Span", "Span"), types(h2));
            // Each Span has its text marked on page 1.
            PDStructureElement span = (PDStructureElement) h2.getKids().get(0);
            PDMarkedContentReference ref = (PDMarkedContentReference) span.getKids().get(0);
            assertEquals(pdf.getPage(0), ref.getPage());
            String content = new String(pdf.getPage(0).getContents().readAllBytes(), StandardCharsets.ISO_8859_1);
            assertTrue(content.contains("/Span <</MCID " + ref.getMCID() + " >>BDC")
                    || content.contains("/Span <</MCID " + ref.getMCID() + ">> BDC"), content);
            assertTrue(content.contains("/Artifact BMC"), content);
            assertEquals(2, pdf.getPage(1).getStructParents() + 1);
        }
    }

    /**
     * A box that declares another language carries its {@code Lang}, which the elements inside inherit; the others do
     * not repeat it (reference/common/typography.md and reference/components/heading.md).
     */
    @Test
    void aBoxInAnotherLanguageHasLang() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(DOCUMENT
                + "\tParagraph: Hola\n"
                + "\tBlock:\n\t\tTypography:\n\t\t\tLanguage: en-GB\n\t\tParagraph: Hello\n\t\tText: World\n"
                + "\tParagraph: Adiós\n"))) {
            assertEquals("es", pdf.getDocumentCatalog().getLanguage());
            PDStructureElement document = section(pdf);
            assertEquals(List.of("P", "Div", "P"), types(document));
            PDStructureElement english = (PDStructureElement) document.getKids().get(1);
            assertEquals("en-GB", english.getLanguage());
            // Lang is inherited through the structure tree: the ones inside do not repeat it.
            english.getKids().forEach(k -> assertNull(((PDStructureElement) k).getLanguage()));
            assertNull(((PDStructureElement) document.getKids().get(0)).getLanguage());
            assertNull(((PDStructureElement) document.getKids().get(2)).getLanguage());
        }
    }

    @Test
    void listsAreTaggedWithLabelAndBody() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(DOCUMENT
                + "\tList:\n\t\tMarker: decimal\n\t\tBox:\n\t\t\tPadding: 0 0 0 20\n"
                + "\t\tList item:\n\t\t\tText: Uno\n"
                + "\t\tList item:\n\t\t\tText: Dos\n"
                + "\t\t\tList:\n\t\t\t\tMarker: bullet\n\t\t\t\tBox:\n\t\t\t\t\tPadding: 0 0 0 20\n"
                + "\t\t\t\tList item:\n\t\t\t\t\tText: Dentro\n"))) {
            PDStructureElement list = (PDStructureElement) section(pdf).getKids().get(0);
            assertEquals("L", list.getStructureType());
            assertEquals(PDListAttributeObject.LIST_NUMBERING_DECIMAL,
                    ((PDListAttributeObject) list.getAttributes().getObject(0)).getListNumbering());
            assertEquals(List.of("LI", "LI"), types(list));
            PDStructureElement second = (PDStructureElement) list.getKids().get(1);
            assertEquals(List.of("Lbl", "LBody"), types(second));
            PDStructureElement body = (PDStructureElement) second.getKids().get(1);
            assertEquals(List.of("P", "L"), types(body));
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("1."), text);
            assertTrue(text.contains("2."), text);
            assertTrue(text.contains("•"), text);
        }
    }

    /** A table with fifteen rows of two cells: with a page area of 160, eleven rows fit on the first page. */
    private static String rows() {
        StringBuilder body = new StringBuilder();
        for (int i = 1; i <= 15; i++) {
            body.append("\t\t\tRow:\n\t\t\t\tCell: Fila ").append(i).append("\n\t\t\t\tCell: ").append(i).append('\n');
        }
        return body.toString();
    }

    @Test
    void tablesAreTaggedAndTheirRepeatedRowsAreArtifacts() throws Exception {
        // Page area of 160: the header and the footer (12 each) leave 136, eleven rows per page.
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(DOCUMENT
                + "\tTable:\n\t\tColumn sizes: * 60\n"
                + "\t\tHeader rows:\n\t\t\tRow:\n\t\t\t\tCell: Concepto\n\t\t\t\tCell: Valor\n"
                + "\t\tBody rows:\n" + rows()
                + "\t\tFooter rows:\n\t\t\tRow:\n\t\t\t\tCell: Total\n\t\t\t\t\tSpan: 1 2\n"))) {
            assertEquals(2, pdf.getNumberOfPages());
            PDStructureElement table = (PDStructureElement) section(pdf).getKids().get(0);
            assertEquals("Table", table.getStructureType());
            assertEquals(List.of("THead", "TBody", "TFoot"), types(table));
            PDStructureElement headRow = (PDStructureElement) ((PDStructureElement) table.getKids().get(0)).getKids().get(0);
            assertEquals(List.of("TH", "TH"), types(headRow));
            PDStructureElement th = (PDStructureElement) headRow.getKids().get(0);
            assertEquals(PDTableAttributeObject.SCOPE_COLUMN,
                    ((PDTableAttributeObject) th.getAttributes().getObject(0)).getScope());
            // The header is content only on page 1; on page 2 it is repeated as an artifact.
            assertEquals(1, th.getKids().size());
            assertEquals(pdf.getPage(0), ((PDMarkedContentReference) th.getKids().get(0)).getPage());
            PDStructureElement tbody = (PDStructureElement) table.getKids().get(1);
            assertEquals(15, types(tbody).size());
            assertEquals(List.of("TD", "TD"), types((PDStructureElement) tbody.getKids().get(14)));
            PDStructureElement footRow = (PDStructureElement) ((PDStructureElement) table.getKids().get(2)).getKids().get(0);
            assertEquals(List.of("TD"), types(footRow));
            PDStructureElement total = (PDStructureElement) footRow.getKids().get(0);
            assertEquals(2, ((PDTableAttributeObject) total.getAttributes().getObject(0)).getColSpan());

            String second = new String(pdf.getPage(1).getContents().readAllBytes(), StandardCharsets.ISO_8859_1);
            assertTrue(second.contains("/Artifact BMC"), second);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(2);
            stripper.setEndPage(2);
            String text = stripper.getText(pdf);
            assertTrue(text.contains("Concepto") && text.contains("Fila 15") && text.contains("Total"), text);
        }
    }

    /**
     * A cell with {@code Row header} in the body or the footer is a {@code TH} with {@code Scope: Row}; the ones in the
     * header are still {@code Scope: Column} and the others, {@code TD} (reference/components/table.md).
     */
    @Test
    void headerCellsAreRowHeaders() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(DOCUMENT
                + "\tTable:\n\t\tColumn sizes: 100 100\n"
                + "\t\tHeader rows:\n\t\t\tRow:\n\t\t\t\tCell: Concepto\n\t\t\t\tCell: Importe\n"
                + "\t\tBody rows:\n\t\t\tRow:\n\t\t\t\tCell: Mesa\n\t\t\t\t\tRow header: true\n\t\t\t\tCell: 120\n"
                + "\t\tFooter rows:\n\t\t\tRow:\n\t\t\t\tCell: Total\n\t\t\t\t\tRow header: true\n\t\t\t\tCell: 120\n"))) {
            PDStructureElement table = (PDStructureElement) section(pdf).getKids().get(0);
            PDStructureElement bodyRow = (PDStructureElement) ((PDStructureElement) table.getKids().get(1)).getKids().get(0);
            assertEquals(List.of("TH", "TD"), types(bodyRow));
            assertEquals(PDTableAttributeObject.SCOPE_ROW,
                    ((PDTableAttributeObject) ((PDStructureElement) bodyRow.getKids().get(0)).getAttributes().getObject(0)).getScope());
            assertEquals(0, ((PDStructureElement) bodyRow.getKids().get(1)).getAttributes().size());
            PDStructureElement footRow = (PDStructureElement) ((PDStructureElement) table.getKids().get(2)).getKids().get(0);
            assertEquals(List.of("TH", "TD"), types(footRow));
            PDStructureElement head = (PDStructureElement) ((PDStructureElement) ((PDStructureElement) table.getKids().get(0))
                    .getKids().get(0)).getKids().get(0);
            assertEquals(PDTableAttributeObject.SCOPE_COLUMN,
                    ((PDTableAttributeObject) head.getAttributes().getObject(0)).getScope());
        }
    }

    /**
     * A {@code Columns} is a {@code Div} and its content hangs from it, without one element per column
     * (reference/components/columns.md).
     */
    @Test
    void columnsIsADivWithoutOneElementPerColumn() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(DOCUMENT
                + "\tColumns:\n\t\tColumn count: 2\n\t\tGap: 10\n\t\tSeparator: 0.5\n"
                + "\t\tParagraph: Uno\n\t\tParagraph: Dos\n"))) {
            PDStructureElement columnsBox = (PDStructureElement) section(pdf).getKids().get(0);
            assertEquals("Div", columnsBox.getStructureType());
            assertEquals(List.of("P", "P"), types(columnsBox));
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("Uno") && text.contains("Dos"), text);
        }
    }

    /**
     * What a Markdown generates is tagged and gives bookmarks as if the author had written it
     * (reference/components/markdown.md).
     */
    @Test
    void markdownIsTaggedAndItsHeadingsAreBookmarks() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(Docs.header(300, 300, 40) + Docs.TYPOGRAPHY
                + "\t\tFor element: List\n\t\t\tBox:\n\t\t\t\tPadding: 0 0 0 15\n"
                + "\tMarkdown:\n\t\tBullet markers: bullet\n\t\tTable column sizes: equal\n\t\tContent >>\n"
                + "\t\t\t# Informe\n\n\t\t\tCon **negrita** y ++subrayado++.\n\n\t\t\t## Datos\n\n"
                + "\t\t\t- uno\n\t\t\t- dos\n\n\t\t\t| A | B |\n\t\t\t|---|---|\n\t\t\t| 1 | 2 |\n"))) {
            PDStructureElement document = section(pdf);
            assertEquals(List.of("Div"), types(document));
            PDStructureElement md = (PDStructureElement) document.getKids().get(0);
            assertEquals(List.of("H1", "P", "H2", "L", "Table"), types(md));
            PDOutlineItem informe = pdf.getDocumentCatalog().getDocumentOutline().getFirstChild();
            assertEquals("Informe", informe.getTitle());
            assertEquals("Datos", informe.getFirstChild().getTitle());
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("Con negrita y subrayado."), text);
        }
    }

    /**
     * With {@code Case} and {@code Small caps} what is seen is capitals, but the structure keeps the written text
     * (reference/common/text-formatting.md): {@code ActualText}. A text with the page number does not carry it,
     * because it changes with the page.
     */
    @Test
    void capsAreOnlyPaintedAndTheStructureKeepsTheWrittenText() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(DOCUMENT
                + "\tParagraph: Emisor y destinatario\n\t\tTypography:\n\t\t\tCase: uppercase\n"
                + "\t\t\tLetter spacing: 1\n"
                + "\tParagraph: Año nuevo\n\t\tTypography:\n\t\t\tSmall caps: true\n"
                + "\tParagraph: Hoja {page}\n\t\tTypography:\n\t\t\tCase: uppercase\n"
                + "\tParagraph: Sin cambios\n"))) {
            List<String> actual = new ArrayList<>();
            for (Object kid : section(pdf).getKids()) {
                actual.add(((PDStructureElement) kid).getActualText());
            }
            assertEquals(Arrays.asList("Emisor y destinatario", "Año nuevo", null, null), actual);
            // What is painted is capitals, and letter spacing does not split the text into letters (pdfbox reads it
            // whole).
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("EMISOR Y DESTINATARIO"), text);
            assertTrue(text.contains("AÑO NUEVO"), text);
            assertTrue(text.contains("HOJA 1"), text);
        }
    }

    /**
     * A table of contents (reference/components/table-of-contents.md) is a {@code TOC} with one {@code TOCI} per entry
     * (title and number, two {@code Span}); the leader is an artifact. With the real metrics, it writes the page of
     * each heading, also that of a heading in a Markdown.
     */
    @Test
    void contentsIsATocWithOneTociPerEntry() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(Docs.header(300, 300, 40) + Docs.TYPOGRAPHY
                + "\tTable of contents:\n\t\tLevels: 1 2\n"
                + "\tMarkdown:\n\t\tContent >>\n\t\t\t# Uno\n\n\t\t\tTexto.\n"
                + "\tBlock:\n\t\tBox:\n\t\t\tHeight: 250\n"
                + "\tHeading 1:\n\t\tText: Dos\n\tHeading 2:\n\t\tText: Dos punto uno\n"))) {
            PDStructureElement document = section(pdf);
            assertEquals(List.of("TOC", "Div", "Div", "H1", "H2"), types(document));
            PDStructureElement toc = (PDStructureElement) document.getKids().get(0);
            assertEquals(List.of("TOCI", "TOCI", "TOCI"), types(toc));
            // The entry: TOCI → Link → [title, number, OBJR].
            PDStructureElement entry = (PDStructureElement) toc.getKids().get(0);
            assertEquals(List.of("Link"), types(entry));
            assertEquals(List.of("Span", "Span", "OBJR"), kids((PDStructureElement) entry.getKids().get(0)));
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("Uno"), text);
            assertTrue(text.matches("(?s).*Dos punto uno\\s*2.*"), text);
        }
    }

    // Figures (reference/components/figure.md): a Figure with an image or a block is a Figure with its Alt, its BBox
    // and its Caption; with a table, the Table carries the Caption.

    private static final String FIGURES = Docs.header(300, 300, 20) + Docs.TYPOGRAPHY
            + "\tTable of contents:\n\t\tEntries: tables\n"
            + "\tFigure:\n\t\tCaption: Esquema\n\t\tImage: test/wide.png\n\t\t\tAlt: Paisaje\n\t\t\tBox:\n\t\t\t\tWidth: 100\n"
            + "\tFigure:\n\t\tCaption: Bornes\n\t\tTable:\n\t\t\tColumn sizes: *\n\t\t\tBody rows:\n\t\t\t\tRow:\n\t\t\t\t\tCell: a\n"
            + "\tFigure:\n\t\tAlt: Dos fotos\n\t\tCaption: Fotos\n\t\tBlock:\n\t\t\tText: Hola\n";

    @Test
    void aFigureHasItsAltItsBBoxAndItsCaption() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(FIGURES, 1))) {
            PDStructureElement section = section(pdf);
            assertEquals(List.of("TOC", "Figure", "Table", "Figure"), kids(section));
            PDStructureElement image = (PDStructureElement) section.getKids().get(1);
            assertEquals("Paisaje", image.getAlternateDescription());
            // The caption and the content of the image, which has no element of its own.
            assertEquals(List.of("Caption", "MCR"), kids(image));
            assertEquals(List.of("MCR"), kids((PDStructureElement) image.getKids().get(0)));
            PDLayoutAttributeObject layout = (PDLayoutAttributeObject) image.getAttributes().getObject(0);
            assertNotNull(layout.getBBox());
            // The table carries its caption, first because it is above.
            PDStructureElement table = (PDStructureElement) section.getKids().get(2);
            assertEquals(List.of("Caption", "TBody"), kids(table));
            // The figure with a block: its own Alt, the block as a Div and the caption after it.
            PDStructureElement block = (PDStructureElement) section.getKids().get(3);
            assertEquals("Dos fotos", block.getAlternateDescription());
            assertEquals(List.of("Div", "Caption"), kids(block));
            // A block is not an image: its Figure has no BBox.
            assertTrue(block.getCOSObject().getItem(COSName.A) == null);
        }
    }

    /**
     * The figures pass PDF/UA-1 and PDF/UA-2; with PDF/UA-2, the entry of a table of tables points with {@code Ref}
     * to the {@code Table} of its figure.
     */
    @Test
    void figuresPassPdfUa1AndPdfUa2() throws Exception {
        assertEquals(Map.of(), PdfUa.failedRules(Pdfs.of(FIGURES, 1), 1));
        byte[] ua2 = Pdfs.of(FIGURES, 2);
        assertEquals(Map.of(), PdfUa.failedRules(ua2, 2));
        try (PDDocument pdf = Loader.loadPDF(ua2)) {
            PDStructureElement toc = (PDStructureElement) section(pdf).getKids().get(0);
            PDStructureElement entry = (PDStructureElement) toc.getKids().get(0);
            PDStructureElement table = (PDStructureElement) section(pdf).getKids().get(2);
            assertEquals(table.getCOSObject(), entry.getCOSObject().getCOSArray(COSName.getPDFName("Ref")).getObject(0));
        }
    }

    // The image markers of a list (reference/components/list.md): with Alt, the Lbl of the item holds a Figure with
    // the alternative text and the BBox of the image; without it, the image is an artifact and the item has no Lbl.

    private static final String IMAGE_MARKERS = Docs.header(300, 300, 20) + Docs.TYPOGRAPHY
            + "\t\tFor element: List\n\t\t\tBox:\n\t\t\t\tPadding: 0 0 0 20\n"
            + "\tList:\n\t\tMarker: image\n\t\tMarker image: test/check.svg\n\t\t\tAlt: Incluido\n"
            + "\t\tList item:\n\t\t\tText: Robo\n"
            + "\t\tList item:\n\t\t\tMarker image: test/cross.svg\n\t\t\tText: Lunas\n";

    private static PDStructureElement list(PDDocument pdf) {
        return (PDStructureElement) section(pdf).getKids().get(0);
    }

    @Test
    void anImageMarkerWithAltIsAFigureInTheLabelAndWithoutAltThereIsNoLabel() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(IMAGE_MARKERS, 1))) {
            PDStructureElement list = list(pdf);
            assertEquals("L", list.getStructureType());
            assertEquals(PDListAttributeObject.LIST_NUMBERING_NONE,
                    ((PDListAttributeObject) list.getAttributes().getObject(0)).getListNumbering());
            assertEquals(List.of("LI", "LI"), kids(list));
            PDStructureElement first = (PDStructureElement) list.getKids().get(0);
            assertEquals(List.of("Lbl", "LBody"), kids(first));
            PDStructureElement label = (PDStructureElement) first.getKids().get(0);
            assertEquals(List.of("Figure"), kids(label));
            PDStructureElement figure = (PDStructureElement) label.getKids().get(0);
            assertEquals("Incluido", figure.getAlternateDescription());
            assertEquals(List.of("MCR"), kids(figure));
            PDLayoutAttributeObject layout = (PDLayoutAttributeObject) figure.getAttributes().getObject(0);
            assertNotNull(layout.getBBox());
            // The second item replaces the image with a decorative one: no Lbl.
            PDStructureElement second = (PDStructureElement) list.getKids().get(1);
            assertEquals(List.of("LBody"), kids(second));
            String content = new String(pdf.getPage(0).getContents().readAllBytes(), StandardCharsets.ISO_8859_1);
            assertTrue(content.contains("/Figure <</MCID"), content);
            assertTrue(content.contains("/Artifact BMC"), content);
        }
    }

    /**
     * A list with image markers passes PDF/UA-1 and PDF/UA-2: {@code ListNumbering: None} with the first,
     * {@code Unordered} with the second.
     */
    @Test
    void imageMarkersPassPdfUa1AndPdfUa2() throws Exception {
        assertEquals(Map.of(), PdfUa.failedRules(Pdfs.of(IMAGE_MARKERS, 1), 1));
        byte[] ua2 = Pdfs.of(IMAGE_MARKERS, 2);
        assertEquals(Map.of(), PdfUa.failedRules(ua2, 2));
        try (PDDocument pdf = Loader.loadPDF(ua2)) {
            assertEquals("Unordered", ((PDListAttributeObject) list(pdf).getAttributes().getObject(0)).getListNumbering());
        }
    }

    // The footnotes (reference/components/footnote.md): the note after the paragraph that calls it, as a Note
    // (PDF/UA-1) or FENote (PDF/UA-2) with its label, and the call as a Reference.

    private static final String FOOTNOTE = DOCUMENT + "\t\t\t\tMarkup: markdown\n"
            + "\tParagraph: Uno[^n] dos.\n\tParagraph: Tres.\n\tFootnote:\n\t\tId: n\n\t\tParagraph: La nota.\n";

    @Test
    void theNoteFollowsItsParagraphWithItsLabelAndTheCallIsAReference() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(FOOTNOTE, 1))) {
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

    @Test
    void withPdfUa2TheNoteIsAnFENote() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(FOOTNOTE, 2))) {
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
