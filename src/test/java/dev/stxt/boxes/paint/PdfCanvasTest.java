package dev.stxt.boxes.paint;

import dev.stxt.InlineNode;
import dev.stxt.boxes.stxt.DocumentReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkedContentReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDTableAttributeObject;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDListAttributeObject;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Escribe los PDF en {@code target/pdf/} para revisarlos a mano. */
class PdfCanvasTest {

    private static final Path PDF_DIR = Path.of("target", "pdf");

    private final DocumentReader reader = new DocumentReader();

    private static byte[] render(InlineNode document, String name) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.write(document, out);
        Files.createDirectories(PDF_DIR);
        Files.write(PDF_DIR.resolve(name + ".pdf"), out.toByteArray());
        return out.toByteArray();
    }

    @Test
    void reportExample() throws Exception {
        InlineNode doc = reader.read(Path.of(getClass().getResource("/examples/report.stxt").toURI()));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "report"))) {
            assertEquals(1, pdf.getNumberOfPages());
            PDRectangle box = pdf.getPage(0).getMediaBox();
            assertEquals(595, box.getWidth(), 1e-3);
            assertEquals(842, box.getHeight(), 1e-3);
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("Resultados del trimestre"), text);
            assertTrue(text.contains("Informe trimestral"), text);
            assertTrue(text.contains("BORRADOR"), text);
            // Párrafo justificado: el espacio extra va en TJ, y el texto se extrae igual.
            assertTrue(text.contains("Primer párrafo del informe."), text);
        }
    }

    @Test
    void embedsTheFontsAsSubsets() throws Exception {
        InlineNode doc = reader.read(Path.of(getClass().getResource("/examples/report.stxt").toURI()));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "report"))) {
            PDResources resources = pdf.getPage(0).getResources();
            List<String> names = new ArrayList<>();
            for (COSName name : resources.getFontNames()) {
                PDFont font = resources.getFont(name);
                assertInstanceOf(PDType0Font.class, font);
                assertTrue(font.isEmbedded(), font.getName());
                names.add(font.getName().substring(font.getName().indexOf('+') + 1));
            }
            names.sort(null);
            // El título en negrita; el resto, normal.
            assertEquals(List.of("IBMPlexSans", "IBMPlexSans-Bold"), names);
        }
    }

    @Test
    void justifiedTextUsesTj() {
        assertArrayEquals(new Object[]{"uno ", -500f, "dos ", -500f, "tres"},
                PdfCanvas.justified("uno dos tres", 5, 10));
        assertArrayEquals(new Object[]{"palabra"}, PdfCanvas.justified("palabra", 5, 10));
    }

    @Test
    void onePdfPagePerPage() throws Exception {
        InlineNode doc = reader.read("Document (dev.stxt.boxes):\n\tMedia:\n\t\tWidth: 300\n"
                + "\t\tHeight: 200\n\t\tMargin: 20\n\tTypography:\n\t\tFont: IBM Plex Sans\n\t\tSize: 10\n"
                + "\tText: Uno\n\tText: Dos\n\t\tBreak:\n\t\t\tBefore: page\n");
        try (PDDocument pdf = Loader.loadPDF(render(doc, "two-pages"))) {
            assertEquals(2, pdf.getNumberOfPages());
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(2);
            stripper.setEndPage(2);
            assertEquals("Dos", stripper.getText(pdf).strip());
        }
    }

    @Test
    void pageHasTheSizeOfTheMedia() throws Exception {
        InlineNode doc = reader.read("Document (dev.stxt.boxes):\n\tMedia:\n\t\tWidth: 300\n\t\tHeight: 842\n"
                + "\t\tMargin: 20\n\tBlock:\n\t\tBox:\n\t\t\tHeight: 500\n\t\t\tBackground: #F1EFE8\n");
        try (PDDocument pdf = Loader.loadPDF(render(doc, "media-size"))) {
            assertEquals(1, pdf.getNumberOfPages());
            assertEquals(300, pdf.getPage(0).getMediaBox().getWidth(), 1e-3);
            assertEquals(842, pdf.getPage(0).getMediaBox().getHeight(), 1e-3);
        }
    }

    private static final String HEADINGS = "Document (dev.stxt.boxes):\n\tMedia:\n\t\tWidth: 300\n\t\tHeight: 400\n"
            + "\t\tMargin: 40\n\tTypography:\n\t\tFont: IBM Plex Sans\n\t\tSize: 10\n"
            + "\tFixed:\n\t\tOffset: auto 0 -30 0\n\t\tText: Pie\n"
            + "\tHeading 1:\n\t\tText: Uno\n"
            + "\tParagraph: Hola.\n"
            + "\tHeading 2:\n\t\tText: Uno\n\t\tText: punto uno\n"
            + "\tParagraph: Adiós.\n\t\tBreak:\n\t\t\tAfter: page\n"
            + "\tHeading 1:\n\t\tText: Dos\n";

    @Test
    void headingsBecomeNestedBookmarks() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(render(reader.read(HEADINGS), "headings"))) {
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
            // La esquina superior del encabezado: el área útil empieza 40 pt por debajo del borde.
            assertEquals(400 - 40, target.getTop());
            assertEquals(40, target.getLeft());
        }
    }

    @Test
    void theDocumentIsTagged() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(render(reader.read(HEADINGS), "headings"))) {
            assertTrue(pdf.getDocumentCatalog().getMarkInfo().isMarked());
            PDStructureTreeRoot treeRoot = pdf.getDocumentCatalog().getStructureTreeRoot();
            PDStructureElement document = (PDStructureElement) treeRoot.getKids().get(0);
            assertEquals("Document", document.getStructureType());
            // El Fixed es un artifact: no está en el árbol. El orden es el del documento.
            assertEquals(List.of("H1", "P", "H2", "P", "H1"), types(document));
            PDStructureElement h2 = (PDStructureElement) document.getKids().get(2);
            assertEquals(List.of("Span", "Span"), types(h2));
            // Cada Span tiene su texto marcado en la página 1.
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

    @Test
    void titleAndLanguage() throws Exception {
        InlineNode doc = reader.read("Document (dev.stxt.boxes):\n\tMedia:\n\t\tWidth: 300\n\t\tHeight: 200\n"
                + "\tTitle: Informe <trimestral> & resumen\n\tLanguage: es-ES\n"
                + "\tTypography:\n\t\tFont: IBM Plex Sans\n\t\tSize: 10\n\tText: Hola\n");
        try (PDDocument pdf = Loader.loadPDF(render(doc, "title-language"))) {
            PDDocumentCatalog catalog = pdf.getDocumentCatalog();
            assertEquals("es-ES", catalog.getLanguage());
            assertEquals("Informe <trimestral> & resumen", pdf.getDocumentInformation().getTitle());
            assertTrue(catalog.getViewerPreferences().displayDocTitle());
            String xmp = new String(catalog.getMetadata().toByteArray(), StandardCharsets.UTF_8);
            assertTrue(xmp.contains("<rdf:li xml:lang=\"x-default\">Informe &lt;trimestral&gt; &amp; resumen</rdf:li>"), xmp);
        }
    }

    @Test
    void withoutTitleNorLanguageTheyAreNotWritten() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(render(reader.read(HEADINGS), "headings"))) {
            assertNull(pdf.getDocumentCatalog().getLanguage());
            assertNull(pdf.getDocumentInformation().getTitle());
            assertNull(pdf.getDocumentCatalog().getMetadata());
        }
    }

    @Test
    void listsAreTaggedWithLabelAndBody() throws Exception {
        InlineNode doc = reader.read("Document (dev.stxt.boxes):\n\tMedia:\n\t\tWidth: 300\n\t\tHeight: 200\n"
                + "\t\tMargin: 20\n\tTypography:\n\t\tFont: IBM Plex Sans\n\t\tSize: 10\n"
                + "\tList:\n\t\tMarker: decimal\n\t\tBox:\n\t\t\tPadding: 0 0 0 20\n"
                + "\t\tList item:\n\t\t\tText: Uno\n"
                + "\t\tList item:\n\t\t\tText: Dos\n"
                + "\t\t\tList:\n\t\t\t\tMarker: disc\n\t\t\t\tBox:\n\t\t\t\t\tPadding: 0 0 0 20\n"
                + "\t\t\t\tList item:\n\t\t\t\t\tText: Dentro\n");
        try (PDDocument pdf = Loader.loadPDF(render(doc, "lists"))) {
            PDStructureElement document = (PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0);
            PDStructureElement list = (PDStructureElement) document.getKids().get(0);
            assertEquals("L", list.getStructureType());
            assertEquals(PDListAttributeObject.LIST_NUMBERING_DECIMAL,
                    ((PDListAttributeObject) list.getAttributes().getObject(0)).getListNumbering());
            assertEquals(List.of("LI", "LI"), types(list));
            PDStructureElement second = (PDStructureElement) list.getKids().get(1);
            assertEquals(List.of("Lbl", "LBody"), types(second));
            PDStructureElement body = (PDStructureElement) second.getKids().get(1);
            assertEquals(List.of("Span", "L"), types(body));
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("1."), text);
            assertTrue(text.contains("2."), text);
            assertTrue(text.contains("•"), text);
        }
    }

    @Test
    void tablesAreTaggedAndTheirRepeatedRowsAreArtifacts() throws Exception {
        StringBuilder body = new StringBuilder();
        for (int i = 1; i <= 15; i++) {
            body.append("\t\t\tRow:\n\t\t\t\tCell: Fila ").append(i).append("\n\t\t\t\tCell: ").append(i).append('\n');
        }
        // Área útil de 160: la cabecera y el pie (12 cada uno) dejan 136, once filas por página.
        InlineNode doc = reader.read("Document (dev.stxt.boxes):\n\tMedia:\n\t\tWidth: 300\n\t\tHeight: 200\n"
                + "\t\tMargin: 20\n\tTypography:\n\t\tFont: IBM Plex Sans\n\t\tSize: 10\n"
                + "\tTable:\n\t\tColumns: 1fr 60\n"
                + "\t\tHead:\n\t\t\tRow:\n\t\t\t\tCell: Concepto\n\t\t\t\tCell: Valor\n"
                + "\t\tBody:\n" + body
                + "\t\tFoot:\n\t\t\tRow:\n\t\t\t\tCell: Total\n\t\t\t\t\tSpan: 1 2\n");
        try (PDDocument pdf = Loader.loadPDF(render(doc, "tables"))) {
            assertEquals(2, pdf.getNumberOfPages());
            PDStructureElement document = (PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0);
            PDStructureElement table = (PDStructureElement) document.getKids().get(0);
            assertEquals("Table", table.getStructureType());
            assertEquals(List.of("THead", "TBody", "TFoot"), types(table));
            PDStructureElement headRow = (PDStructureElement) ((PDStructureElement) table.getKids().get(0)).getKids().get(0);
            assertEquals(List.of("TH", "TH"), types(headRow));
            PDStructureElement th = (PDStructureElement) headRow.getKids().get(0);
            assertEquals(PDTableAttributeObject.SCOPE_COLUMN,
                    ((PDTableAttributeObject) th.getAttributes().getObject(0)).getScope());
            // La cabecera es contenido solo en la página 1; en la 2 se repite como artifact.
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

    private static List<String> types(PDStructureElement e) {
        return e.getKids().stream().filter(k -> k instanceof PDStructureElement)
                .map(k -> ((PDStructureElement) k).getStructureType()).toList();
    }

    @Test
    void colors() {
        assertArrayEquals(new float[]{1, 0, 0}, PdfCanvas.rgb("#F00"));
        assertArrayEquals(new float[]{0xA3 / 255f, 0x2D / 255f, 0x2D / 255f}, PdfCanvas.rgb("#A32D2D"));
    }
}
