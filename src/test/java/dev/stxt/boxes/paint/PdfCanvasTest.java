package dev.stxt.boxes.paint;

import dev.stxt.InlineNode;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.inline.PdfMetrics;
import dev.stxt.boxes.model.TextRun;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.stxt.DocumentReader;
import dev.stxt.boxes.tree.TextRuns;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkedContentReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDObjectReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDTableAttributeObject;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDListAttributeObject;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDLayoutAttributeObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
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
import java.util.Map;

import static dev.stxt.boxes.tree.TextRuns.run;
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

    /** Como {@link #render(InlineNode, String)}, con los tramos de cada texto por su {@code Id}. */
    private static byte[] render(InlineNode document, Map<String, List<TextRun>> runs, String name)
            throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Resources resources = new Resources();
        PdfCanvas.write(new Engine(new PdfMetrics(resources)).layOutDocument(TextRuns.build(document, runs)),
                resources, out, false);
        Files.createDirectories(PDF_DIR);
        Files.write(PDF_DIR.resolve(name + ".pdf"), out.toByteArray());
        return out.toByteArray();
    }

    @Test
    void reportExample() throws Exception {
        InlineNode doc = reader.read(Path.of("test", "spec", "report.stxt"));
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
        InlineNode doc = reader.read(Path.of("test", "spec", "report.stxt"));
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
    void letterSpacingIsATjShiftBetweenCharactersButNotAfterTheLast() {
        // 1,5 pt en un cuerpo de 10: 150 milésimas; un número positivo en TJ acerca, uno negativo separa.
        assertArrayEquals(new Object[]{"a", -150f, "b", -150f, "c"}, PdfCanvas.spaced("abc", 0, 1.5, 10));
        // Con justify, tras un espacio se suma el espacio extra.
        assertArrayEquals(new Object[]{"a", -150f, " ", -650f, "b"}, PdfCanvas.spaced("a b", 5, 1.5, 10));
        // Sin interletraje es el TJ de siempre.
        assertArrayEquals(PdfCanvas.justified("uno dos", 5, 10), PdfCanvas.spaced("uno dos", 5, 0, 10));
    }

    @Test
    void justifiedTextUsesTj() {
        assertArrayEquals(new Object[]{"uno ", -500f, "dos ", -500f, "tres"},
                PdfCanvas.justified("uno dos tres", 5, 10));
        assertArrayEquals(new Object[]{"palabra"}, PdfCanvas.justified("palabra", 5, 10));
    }

    @Test
    void onePdfPagePerPage() throws Exception {
        InlineNode doc = reader.read("Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 300\n"
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
        InlineNode doc = reader.read("Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 300\n\t\tHeight: 842\n"
                + "\t\tMargin: 20\n\tBlock:\n\t\tBox:\n\t\t\tHeight: 500\n\t\t\tBackground: #F1EFE8\n");
        try (PDDocument pdf = Loader.loadPDF(render(doc, "media-size"))) {
            assertEquals(1, pdf.getNumberOfPages());
            assertEquals(300, pdf.getPage(0).getMediaBox().getWidth(), 1e-3);
            assertEquals(842, pdf.getPage(0).getMediaBox().getHeight(), 1e-3);
        }
    }

    private static final String HEADINGS = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 300\n\t\tHeight: 400\n"
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
    void withoutTitleItIsNotWritten() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(render(reader.read(HEADINGS), "headings"))) {
            assertEquals("es", pdf.getDocumentCatalog().getLanguage());
            assertNull(pdf.getDocumentInformation().getTitle());
            assertNull(pdf.getDocumentCatalog().getMetadata());
        }
    }

    @Test
    void listsAreTaggedWithLabelAndBody() throws Exception {
        InlineNode doc = reader.read("Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 300\n\t\tHeight: 200\n"
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
        InlineNode doc = reader.read("Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 300\n\t\tHeight: 200\n"
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

    /** Un {@code Multicol} es un {@code Div} y su contenido cuelga de él, sin un elemento por columna (§5.16). */
    @Test
    void multicolIsADivWithoutColumns() throws Exception {
        InlineNode doc = reader.read("Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 300\n\t\tHeight: 200\n"
                + "\t\tMargin: 20\n\tTypography:\n\t\tFont: IBM Plex Sans\n\t\tSize: 10\n"
                + "\tMulticol:\n\t\tCount: 2\n\t\tColumn gap: 10\n\t\tRule: 0.5\n"
                + "\t\tParagraph: Uno\n\t\tParagraph: Dos\n");
        try (PDDocument pdf = Loader.loadPDF(render(doc, "multicol"))) {
            PDStructureElement document = (PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0);
            PDStructureElement multicol = (PDStructureElement) document.getKids().get(0);
            assertEquals("Div", multicol.getStructureType());
            assertEquals(List.of("P", "P"), types(multicol));
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("Uno") && text.contains("Dos"), text);
        }
    }

    private static final String LINKS = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 300\n\t\tHeight: 200\n"
            + "\t\tMargin: 20\n\tTypography:\n\t\tFont: IBM Plex Sans\n\t\tSize: 10\n";

    private static List<PDAnnotationLink> links(PDDocument pdf, int page) throws Exception {
        return pdf.getPage(page).getAnnotations().stream().map(a -> (PDAnnotationLink) a).toList();
    }

    private static String uri(PDAnnotationLink link) {
        return ((PDActionURI) link.getAction()).getURI();
    }

    /**
     * Un enlace a una URL es una anotación {@code Link} sin borde con su acción {@code URI}, del
     * tamaño de la caja o del tramo, y cuelga de la caja en el PDF etiquetado como un {@code Link}
     * con su {@code OBJR}; el de un {@code Fixed}, que no es contenido, es clicable pero no está
     * etiquetado (§5.20).
     */
    @Test
    void externalLinksAreAnnotationsTaggedAsLinks() throws Exception {
        InlineNode doc = reader.read(LINKS
                + "\tText: Escribe a soporte\n\t\tLink: mailto:soporte@airemas.example\n"
                + "\tImage: test/wide.png\n\t\tAlt: Logotipo\n\t\tLink: https://stxt.dev\n"
                + "\tFixed:\n\t\tOffset: auto 0 0 auto\n\t\tText: pie\n\t\t\tLink: https://pie.example\n"
                + "\tMarkdown:\n\t\tContent >>\n\t\t\tVisita [la web](https://web.example) hoy.\n");
        try (PDDocument pdf = Loader.loadPDF(render(doc, "links"))) {
            List<PDAnnotationLink> links = links(pdf, 0);
            // Un Fixed se pinta el último, encima de todo.
            assertEquals(List.of("mailto:soporte@airemas.example", "https://stxt.dev", "https://web.example",
                    "https://pie.example"), links.stream().map(PdfCanvasTest::uri).toList());
            PDAnnotationLink text = links.get(0);
            // La caja de borde del texto: 40 pt de ancho de página útil menos... el área útil entera, 12 pt de alto.
            assertEquals(20, text.getRectangle().getLowerLeftX(), 0.01);
            assertEquals(200 - 20 - 12, text.getRectangle().getLowerLeftY(), 0.01);
            assertEquals(260, text.getRectangle().getWidth(), 0.01);
            assertEquals(12, text.getRectangle().getHeight(), 0.01);
            assertEquals("Escribe a soporte", text.getContents());
            assertEquals(0, text.getBorderStyle().getWidth(), 0.001);
            assertEquals("Logotipo", links.get(1).getContents());
            // El tramo con enlace del Markdown no ocupa toda la línea: "la web" y nada más.
            PDRectangle run = links.get(2).getRectangle();
            assertTrue(run.getWidth() > 20 && run.getWidth() < 60, run.toString());
            assertEquals("la web", links.get(2).getContents());

            PDStructureTreeRoot tree = pdf.getDocumentCatalog().getStructureTreeRoot();
            PDStructureElement document = (PDStructureElement) tree.getKids().get(0);
            assertEquals(List.of("Span", "Figure", "Div"), types(document));
            PDStructureElement span = (PDStructureElement) document.getKids().get(0);
            assertEquals(List.of("Link"), types(span));
            PDStructureElement link = (PDStructureElement) span.getKids().stream()
                    .filter(k -> k instanceof PDStructureElement).findFirst().orElseThrow();
            assertEquals("Escribe a soporte", link.getAlternateDescription());
            PDObjectReference reference = (PDObjectReference) link.getKids().get(0);
            assertEquals(text.getCOSObject(), ((PDAnnotationLink) reference.getReferencedObject()).getCOSObject());
            // El árbol de padres lleva de la anotación a su Link.
            assertEquals(link.getCOSObject(), ((COSObjectable) tree.getParentTree().getValue(text.getStructParent())).getCOSObject());
            // El Link del tramo cuelga del párrafo del Markdown; el Fixed no tiene elemento ni Link.
            PDStructureElement paragraph = (PDStructureElement) ((PDStructureElement) document.getKids().get(2))
                    .getKids().get(0);
            assertEquals("P", paragraph.getStructureType());
            assertEquals(List.of("Link"), types(paragraph));
            assertEquals(-1, links.get(3).getStructParent());
            String extracted = new PDFTextStripper().getText(pdf);
            assertTrue(extracted.contains("Visita la web hoy."), extracted);
        }
    }

    /**
     * Un enlace a una caja del documento lleva a su página y a su esquina, hacia delante y hacia
     * atrás (§5.20).
     */
    @Test
    void internalLinksGoToThePageOfTheirBox() throws Exception {
        // Área útil de 160: el texto (12) y el bloque (140) llenan la primera página; el resto, la segunda.
        InlineNode doc = reader.read(LINKS
                + "\tText: Ir al final\n\t\tId: inicio\n\t\tLink: #fin\n"
                + "\tBlock:\n\t\tBox:\n\t\t\tHeight: 140\n"
                + "\tText: Final\n\t\tId: fin\n"
                + "\tText: Volver\n\t\tLink: #inicio\n");
        try (PDDocument pdf = Loader.loadPDF(render(doc, "internal-links"))) {
            assertEquals(2, pdf.getNumberOfPages());
            PDPageXYZDestination forward = (PDPageXYZDestination) links(pdf, 0).get(0).getDestination();
            assertEquals(1, pdf.getPages().indexOf(forward.getPage()));
            assertEquals(20, forward.getLeft());
            assertEquals(200 - 20, forward.getTop());
            PDPageXYZDestination backward = (PDPageXYZDestination) links(pdf, 1).get(0).getDestination();
            assertEquals(0, pdf.getPages().indexOf(backward.getPage()));
            // Una acción de destino, no de URI.
            assertNull(links(pdf, 0).get(0).getAction());
        }
    }

    /** Cada entrada de un índice lleva a la página de su encabezado y su {@code TOCI} tiene el {@code Link} (§5.19, §5.20). */
    @Test
    void contentsEntriesLinkToTheirHeadings() throws Exception {
        InlineNode doc = reader.read(LINKS
                + "\tContents:\n\t\tLevels: 1 1\n"
                + "\tHeading 1:\n\t\tText: Uno\n"
                + "\tBlock:\n\t\tBreak:\n\t\t\tAfter: page\n"
                + "\tHeading 1:\n\t\tText: Dos\n");
        try (PDDocument pdf = Loader.loadPDF(render(doc, "contents-links"))) {
            assertEquals(2, pdf.getNumberOfPages());
            List<PDAnnotationLink> links = links(pdf, 0);
            assertEquals(2, links.size());
            assertEquals(List.of(0, 1), links.stream().map(l -> {
                try {
                    return pdf.getPages().indexOf(((PDPageXYZDestination) l.getDestination()).getPage());
                } catch (java.io.IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            }).toList());
            // El texto de la entrada, sin su número de página.
            assertEquals(List.of("Uno", "Dos"), links.stream().map(PDAnnotation::getContents).toList());
            PDStructureElement document = (PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0);
            PDStructureElement toc = (PDStructureElement) document.getKids().get(0);
            toc.getKids().forEach(k -> assertTrue(types((PDStructureElement) k).contains("Link")));
        }
    }

    /**
     * La cabecera de una tabla se repite en cada página: su enlace es clicable en todas, pero
     * solo es contenido (y tiene {@code Link}) en la primera (§5.20, decisión L5).
     */
    @Test
    void aLinkInARepeatedTableHeaderIsTaggedOnce() throws Exception {
        StringBuilder body = new StringBuilder();
        for (int i = 1; i <= 15; i++) {
            body.append("\t\t\tRow:\n\t\t\t\tCell: Fila ").append(i).append("\n\t\t\t\tCell: ").append(i).append('\n');
        }
        InlineNode doc = reader.read("Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 300\n\t\tHeight: 200\n"
                + "\t\tMargin: 20\n\tTypography:\n\t\tFont: IBM Plex Sans\n\t\tSize: 10\n"
                + "\tTable:\n\t\tColumns: 1fr 60\n"
                + "\t\tHead:\n\t\t\tRow:\n\t\t\t\tCell: Concepto\n\t\t\t\t\tLink: https://c.example\n\t\t\t\tCell: Valor\n"
                + "\t\tBody:\n" + body);
        try (PDDocument pdf = Loader.loadPDF(render(doc, "table-links"))) {
            assertEquals(2, pdf.getNumberOfPages());
            assertEquals(List.of("https://c.example"), links(pdf, 0).stream().map(PdfCanvasTest::uri).toList());
            assertEquals(List.of("https://c.example"), links(pdf, 1).stream().map(PdfCanvasTest::uri).toList());
            PDStructureElement document = (PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0);
            PDStructureElement table = (PDStructureElement) document.getKids().get(0);
            PDStructureElement headRow = (PDStructureElement) ((PDStructureElement) table.getKids().get(0)).getKids().get(0);
            PDStructureElement th = (PDStructureElement) headRow.getKids().get(0);
            assertEquals(List.of("Link"), types(th));
            assertEquals(pdf.getPage(0), ((PDStructureElement) th.getKids().stream()
                    .filter(k -> k instanceof PDStructureElement).findFirst().orElseThrow()).getPage());
            assertEquals(-1, links(pdf, 1).get(0).getStructParent());
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

    private static final String RUNS = "Uno negrita, cursiva, las dos, subrayado y tachado. Una palabra en "
            + "negrita a medias y un texto justificado que ocupa varias líneas con sus tramos.";

    /**
     * Cada tramo con su fuente, en el mismo contenido marcado del párrafo; el subrayado y el
     * tachado, <em>artifacts</em> (§5.4). Se revisa a ojo en {@code target/pdf/runs.pdf}.
     */
    @Test
    void runsUseTheirFontsAndDecorationsAreArtifacts() throws Exception {
        InlineNode doc = reader.read("Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 300\n\t\tHeight: 200\n"
                + "\t\tMargin: 40\n\tTypography:\n\t\tFont: IBM Plex Sans\n\t\tSize: 12\n"
                + "\t\tAlign: justify\n\tParagraph: " + RUNS + "\n\t\tId: P\n");
        List<TextRun> runs = List.of(run("Uno "), run("negrita", "b"), run(", "), run("cursiva", "i"), run(", "),
                run("las dos", "bi"), run(", "), run("subrayado", "u"), run(" y "), run("tachado", "s"),
                run(". Una palabra en "), run("neg", "b"), run("rita a medias y un "),
                run("texto justificado que ocupa varias líneas con sus tramos", "u"), run("."));
        try (PDDocument pdf = Loader.loadPDF(render(doc, Map.of("P", runs), "runs"))) {
            List<String> names = new ArrayList<>();
            PDResources resources = pdf.getPage(0).getResources();
            for (COSName name : resources.getFontNames()) {
                String font = resources.getFont(name).getName();
                names.add(font.substring(font.indexOf('+') + 1));
            }
            names.sort(null);
            assertEquals(List.of("IBMPlexSans", "IBMPlexSans-Bold", "IBMPlexSans-BoldItalic", "IBMPlexSans-Italic"),
                    names);
            String text = new PDFTextStripper().getText(pdf).replaceAll("\\s+", " ");
            assertTrue(text.contains("Uno negrita, cursiva, las dos, subrayado y tachado."), text);
            assertTrue(text.contains("negrita a medias"), text);
            // Un solo P con un solo contenido marcado: los tramos no tienen elemento propio.
            PDStructureElement document = (PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0);
            assertEquals(List.of("P"), types(document));
            PDStructureElement p = (PDStructureElement) document.getKids().get(0);
            assertEquals(1, p.getKids().size());
            String content = new String(pdf.getPage(0).getContents().readAllBytes(), StandardCharsets.ISO_8859_1);
            assertTrue(content.contains("/Artifact BMC"), content);
        }
    }

    /** Lo que genera un Markdown se etiqueta y da marcadores como si lo hubiera escrito el autor (§5.17). */
    @Test
    void markdownIsTaggedAndItsHeadingsAreBookmarks() throws Exception {
        InlineNode doc = reader.read("Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 300\n\t\tHeight: 300\n"
                + "\t\tMargin: 40\n\tTypography:\n\t\tFont: IBM Plex Sans\n\t\tSize: 10\n"
                + "\tStyles:\n\t\tElement: List\n\t\t\tBox:\n\t\t\t\tPadding: 0 0 0 15\n"
                + "\tMarkdown:\n\t\tBullet: disc\n\t\tTable columns: equal\n\t\tContent >>\n"
                + "\t\t\t# Informe\n\n\t\t\tCon **negrita** y ++subrayado++.\n\n\t\t\t## Datos\n\n"
                + "\t\t\t- uno\n\t\t\t- dos\n\n\t\t\t| A | B |\n\t\t\t|---|---|\n\t\t\t| 1 | 2 |\n");
        try (PDDocument pdf = Loader.loadPDF(render(doc, "markdown"))) {
            PDStructureElement document = (PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0);
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
     * Con {@code Transform} y {@code Small caps} lo que se ve son mayúsculas, pero la estructura
     * conserva el texto escrito (§4.4): {@code ActualText}. Un texto con el número de página no lo
     * lleva, porque cambia con la página.
     */
    @Test
    void capsAreOnlyPaintedAndTheStructureKeepsTheWrittenText() throws Exception {
        InlineNode doc = reader.read("Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 300\n\t\tHeight: 200\n"
                + "\t\tMargin: 20\n\tTypography:\n\t\tFont: IBM Plex Sans\n\t\tSize: 10\n"
                + "\tParagraph: Emisor y destinatario\n\t\tTypography:\n\t\t\tTransform: uppercase\n"
                + "\t\t\tLetter spacing: 1\n"
                + "\tParagraph: Año nuevo\n\t\tTypography:\n\t\t\tSmall caps: true\n"
                + "\tParagraph: Hoja {page}\n\t\tTypography:\n\t\t\tTransform: uppercase\n"
                + "\tParagraph: Sin cambios\n");
        try (PDDocument pdf = Loader.loadPDF(render(doc, "caps"))) {
            PDStructureElement document = (PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0);
            List<String> actual = new ArrayList<>();
            for (Object kid : document.getKids()) {
                actual.add(((PDStructureElement) kid).getActualText());
            }
            assertEquals(java.util.Arrays.asList("Emisor y destinatario", "Año nuevo", null, null), actual);
            // Lo que se pinta son mayúsculas, y el interletraje no parte el texto en letras (pdfbox lo lee entero).
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("EMISOR Y DESTINATARIO"), text);
            assertTrue(text.contains("AÑO NUEVO"), text);
            assertTrue(text.contains("HOJA 1"), text);
        }
    }

    /**
     * Un índice (§5.19) es un {@code TOC} con un {@code TOCI} por entrada (título y número, dos
     * {@code Span}); la guía es un artifact. Con las métricas reales, escribe la página de cada
     * encabezado, también la de un encabezado de un Markdown.
     */
    @Test
    void contentsIsATocWithOneTociPerEntry() throws Exception {
        InlineNode doc = reader.read("Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 300\n\t\tHeight: 300\n"
                + "\t\tMargin: 40\n\tTypography:\n\t\tFont: IBM Plex Sans\n\t\tSize: 10\n"
                + "\tContents:\n\t\tLevels: 1 2\n"
                + "\tMarkdown:\n\t\tContent >>\n\t\t\t# Uno\n\n\t\t\tTexto.\n"
                + "\tBlock:\n\t\tBox:\n\t\t\tHeight: 250\n"
                + "\tHeading 1:\n\t\tText: Dos\n\tHeading 2:\n\t\tText: Dos punto uno\n");
        try (PDDocument pdf = Loader.loadPDF(render(doc, "contents"))) {
            PDStructureElement document = (PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0);
            assertEquals(List.of("TOC", "Div", "Div", "H1", "H2"), types(document));
            PDStructureElement toc = (PDStructureElement) document.getKids().get(0);
            assertEquals(List.of("TOCI", "TOCI", "TOCI"), types(toc));
            assertEquals(List.of("Span", "Span", "Link"), types((PDStructureElement) toc.getKids().get(0)));
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("Uno"), text);
            assertTrue(text.matches("(?s).*Dos punto uno\\s*2.*"), text);
        }
    }

    /**
     * Una imagen con {@code Alt} es un {@code Figure} con su texto alternativo y su {@code BBox};
     * sin él, y en un {@code Fixed}, es un artifact (§5.18). Cada fichero se incrusta una vez.
     */
    @Test
    void imagesAreFiguresOrArtifactsAndAreEmbeddedOnce() throws Exception {
        InlineNode doc = reader.read("Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 300\n\t\tHeight: 300\n"
                + "\t\tMargin: 40\n\tTypography:\n\t\tFont: IBM Plex Sans\n\t\tSize: 10\n"
                + "\tFixed:\n\t\tOffset: -30 -30 auto auto\n\t\tImage: test/square.png\n"
                + "\tImage: test/wide.png\n\t\tAlt: Paisaje\n"
                + "\tImage: test/tall.jpg\n\t\tBox:\n\t\t\tHeight: 60\n"
                + "\tParagraph: Fin.\n"
                + "\tImage: test/wide.png\n\t\tBreak:\n\t\t\tBefore: page\n");
        try (PDDocument pdf = Loader.loadPDF(render(doc, "images"))) {
            assertEquals(2, pdf.getNumberOfPages());
            PDStructureElement document = (PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0);
            assertEquals(List.of("Figure", "P"), types(document));
            PDStructureElement figure = (PDStructureElement) document.getKids().get(0);
            assertEquals("Paisaje", figure.getAlternateDescription());
            PDLayoutAttributeObject layout = (PDLayoutAttributeObject) figure.getAttributes().getObject(0);
            PDRectangle box = layout.getBBox();
            // La caja de contenido de la imagen: 150 × 75 en (40, 40), con el origen abajo.
            assertEquals(40, box.getLowerLeftX(), 0.01);
            assertEquals(300 - 40 - 75, box.getLowerLeftY(), 0.01);
            assertEquals(150, box.getWidth(), 0.01);

            List<PDImageXObject> first = images(pdf, 0);
            assertEquals(3, first.size());
            PDImageXObject wide = first.stream().filter(i -> i.getWidth() == 200).findFirst().orElseThrow();
            // El PNG conserva su transparencia; el JPEG va tal cual.
            assertTrue(wide.getSoftMask() != null);
            assertTrue(first.stream().anyMatch(i -> i.getHeight() == 160 && "jpg".equals(i.getSuffix())));
            List<PDImageXObject> second = images(pdf, 1);
            assertTrue(second.stream().anyMatch(i -> i.getCOSObject() == wide.getCOSObject()),
                    "La misma imagen en dos páginas se incrusta una vez");

            String content = new String(pdf.getPage(0).getContents().readAllBytes(), StandardCharsets.ISO_8859_1);
            assertTrue(content.contains("/Figure <</MCID 0>> BDC"), content);
        }
    }

    private static List<PDImageXObject> images(PDDocument pdf, int page) throws Exception {
        PDResources resources = pdf.getPage(page).getResources();
        List<PDImageXObject> images = new ArrayList<>();
        for (COSName name : resources.getXObjectNames()) {
            images.add((PDImageXObject) resources.getXObject(name));
        }
        return images;
    }
}
