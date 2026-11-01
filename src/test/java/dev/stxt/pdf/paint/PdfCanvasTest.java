package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.Limits;
import dev.stxt.pdf.Docs;
import dev.stxt.InlineNode;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.golden.PdfUa;
import dev.stxt.pdf.layout.Engine;
import dev.stxt.pdf.layout.inline.PdfMetrics;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.stxt.DocumentReader;
import dev.stxt.pdf.tree.TextRuns;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.common.PDPageLabelRange;
import org.apache.pdfbox.pdmodel.common.PDPageLabels;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkedContentReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDObjectReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDLayoutAttributeObject;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDListAttributeObject;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDTableAttributeObject;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static dev.stxt.pdf.tree.TextRuns.run;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Writes the PDFs to {@code target/pdf/} for manual review. */
class PdfCanvasTest {

    private static final Path PDF_DIR = Path.of("target", "pdf");

    private final DocumentReader reader = new DocumentReader();

    private static byte[] render(InlineNode document, String name) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.create().toPdf(document, out);
        Files.createDirectories(PDF_DIR);
        Files.write(PDF_DIR.resolve(name + ".pdf"), out.toByteArray());
        return out.toByteArray();
    }

    /** Like {@link #render(InlineNode, String)}, with the runs of each text by its {@code Id}. */
    private static byte[] render(InlineNode document, Map<String, List<TextRun>> runs, String name)
            throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Resources resources = new Resources();
        PdfDocumentWriter.write(new Engine(new PdfMetrics(resources)).layOutDocument(TextRuns.build(document, runs)),
                resources, Limits.DEFAULT, out, false);
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
            assertEquals(210 * 72 / 25.4, box.getWidth(), 1e-3);
            assertEquals(297 * 72 / 25.4, box.getHeight(), 1e-3);
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("Resultados del trimestre"), text);
            assertTrue(text.contains("Informe trimestral"), text);
            assertTrue(text.contains("BORRADOR"), text);
            // Justified paragraph: the extra space goes in TJ, and the text is extracted the same.
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
            // The title in bold; the rest, normal.
            assertEquals(List.of("IBMPlexSans", "IBMPlexSans-Bold"), names);
        }
    }

    /**
     * Fonts with TrueType and with CFF outlines (reference/implementation/pdf.md): Light and Medium are the TTF of the
     * repository and are embedded as subsets; SemiBold is the OTF with CFF outlines of the resources of the tests,
     * which PDFBox cannot subset: it is embedded whole, as a {@code CIDFontType0} with the file in {@code FontFile3}
     * and subtype {@code OpenType}. The dictionaries are read as they are written, without {@code PDFontFactory},
     * which would change them when loading. The text is extracted and the PDF passes PDF/UA-1 and PDF/UA-2.
     */
    @Test
    void embedsTheTrueTypeFontsAsSubsetsAndTheCffOnesWhole(@TempDir Path dir) throws Exception {
        Path fonts = Files.createDirectories(dir.resolve("fonts"));
        for (String variant : List.of("Light", "Medium")) {
            Files.copy(Resources.DEFAULT_ROOT.resolve("fonts/IBM Plex Sans/IBMPlexSans-" + variant + ".ttf"),
                    fonts.resolve(variant + ".ttf"));
        }
        Files.copy(Path.of("test/resources/fonts/IBM Plex Sans/IBMPlexSans-SemiBold.otf"),
                fonts.resolve("SemiBold.otf"));
        Resources resources = new Resources(dir);
        StringBuilder texts = new StringBuilder();
        for (int weight : List.of(300, 500, 600)) {
            texts.append("\tText: Peso ").append(weight).append(" con eñe\n\t\tTypography:\n\t\t\tWeight: ")
                    .append(weight).append('\n');
        }
        String source = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n"
                + "\t\tHeight: 200\n\t\tMargin: 20\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n"
                + "\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n" + texts;
        byte[] bytes = null;
        for (int ua : List.of(1, 2)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Converter.builder().resources(resources).pdfUa(ua).build().toPdf(reader.read(Docs.sectioned(source)), out);
            assertEquals(Map.of(), PdfUa.failedRules(out.toByteArray(), ua), "PDF/UA-" + ua);
            bytes = bytes == null ? out.toByteArray() : bytes;
        }
        Files.createDirectories(PDF_DIR);
        Files.write(PDF_DIR.resolve("weights.pdf"), bytes);
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            COSDictionary dictionaries = pdf.getPage(0).getResources().getCOSObject().getCOSDictionary(COSName.FONT);
            Map<String, String> kinds = new java.util.TreeMap<>();
            for (COSName name : dictionaries.keySet()) {
                COSDictionary font = dictionaries.getCOSDictionary(name);
                COSDictionary descendant = (COSDictionary) font.getCOSArray(COSName.DESCENDANT_FONTS).getObject(0);
                COSDictionary descriptor = descendant.getCOSDictionary(COSName.FONT_DESC);
                COSName fileKey = descriptor.containsKey(COSName.FONT_FILE3) ? COSName.FONT_FILE3 : COSName.FONT_FILE2;
                COSStream file = (COSStream) descriptor.getDictionaryObject(fileKey);
                String subtype = file.getNameAsString(COSName.SUBTYPE);
                kinds.put(font.getNameAsString(COSName.BASE_FONT), descendant.getNameAsString(COSName.SUBTYPE) + " "
                        + fileKey.getName() + (subtype == null ? "" : " " + subtype));
            }
            Map<String, String> byWeight = new java.util.TreeMap<>();
            kinds.forEach((base, kind) -> byWeight.put(base.substring(base.indexOf('+') + 1), kind));
            assertEquals(Map.of("IBMPlexSans-Light", "CIDFontType2 FontFile2",
                    "IBMPlexSans-Medm", "CIDFontType2 FontFile2",
                    "IBMPlexSans-SmBld", "CIDFontType0 FontFile3 OpenType"), byWeight);
            // The TrueType ones are subsets (a prefix of six letters and +); the CFF one is not.
            assertTrue(kinds.keySet().stream().filter(k -> k.contains("Light") || k.contains("Medm"))
                    .allMatch(k -> k.matches("[A-Z]{6}\\+.*")), kinds.toString());
            assertTrue(kinds.containsKey("IBMPlexSans-SmBld"), kinds.toString());
            String text = new PDFTextStripper().getText(pdf);
            for (int weight : List.of(300, 500, 600)) {
                assertTrue(text.contains("Peso " + weight + " con eñe"), text);
            }
        }
    }

    @Test
    void letterSpacingIsATjShiftBetweenCharactersButNotAfterTheLast() {
        // 1.5 pt at size 10: 150 thousandths; a positive number in TJ brings closer, a negative one separates.
        assertArrayEquals(new Object[]{"a", -150f, "b", -150f, "c"}, PdfFonts.spaced("abc", 0, 1.5, 10, null));
        // With justify, the extra space is added after a space.
        assertArrayEquals(new Object[]{"a", -150f, " ", -650f, "b"}, PdfFonts.spaced("a b", 5, 1.5, 10, null));
        // Without letter spacing it is the usual TJ.
        assertArrayEquals(PdfFonts.justified("uno dos", 5, 10), PdfFonts.spaced("uno dos", 5, 0, 10, null));
    }

    @Test
    void justifiedTextUsesTj() {
        assertArrayEquals(new Object[]{"uno ", -500f, "dos ", -500f, "tres"},
                PdfFonts.justified("uno dos tres", 5, 10));
        assertArrayEquals(new Object[]{"palabra"}, PdfFonts.justified("palabra", 5, 10));
    }

    @Test
    void kerningIsATjShiftBetweenThePairs() {
        // -41 thousandths between A and V: in TJ, 41 brings V closer.
        int[] kerning = {0, -41, 0, 0};
        assertArrayEquals(new Object[]{"A", 41f, "V a"}, PdfFonts.spaced("AV a", 0, 0, 10, kerning));
        // With justify and with letter spacing, the kerning is added to the shift of its pair.
        assertArrayEquals(new Object[]{"A", 41f, "V ", -500f, "a"}, PdfFonts.spaced("AV a", 5, 0, 10, kerning));
        assertArrayEquals(new Object[]{"A", -109f, "V", -150f, " ", -650f, "a"},
                PdfFonts.spaced("AV a", 5, 1.5, 10, kerning));
    }

    /**
     * With {@code Kerning} (reference/common/typography.md), the pairs are shifts of {@code TJ} between the glyphs of
     * the same embedded font as without it; the text is extracted as it is written, and the PDF is still PDF/UA, also
     * with ligatures and with a word split with a hyphen.
     */
    @Test
    void kernedTextIsExtractedWholeWithTheSameFont() throws Exception {
        InlineNode doc = reader.read(Docs.sectioned("Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 200\n"
                + "\t\tMargin: 20\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + "\t\t\t\tKerning: true\n"
                + "\tParagraph: AVATAR Tofi\n"
                + "\tParagraph: AVATAR Tofi\n\t\tTypography:\n\t\t\tKerning: false\n"
                + "\tParagraph: Tofi Tofi\n\t\tTypography:\n\t\t\tLigatures: true\n"
                + "\tParagraph: la programación\n\t\tBox:\n\t\t\tWidth: 60\n\t\tTypography:\n\t\t\tHyphenate: true\n"));
        byte[] bytes = render(doc, "kerning");
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            assertEquals(List.of("AVATAR Tofi", "AVATAR Tofi", "Tofi Tofi", "la programa", "ción"),
                    new PDFTextStripper().getText(pdf).strip().lines().map(String::stripTrailing).toList());
            PDPage page = pdf.getPage(0);
            // The regular font with and without kerning, and with ligatures: the same file, twice.
            assertEquals(2, page.getResources().getCOSObject().getCOSDictionary(COSName.FONT).size());
            List<List<Object>> tj = tjArguments(page);
            // The first paragraph: A-V, V-A, A-T and T-A of AVATAR (41, 41, 65, 65) and T-o and o-f of Tofi (65, 6).
            assertEquals(List.of(41f, 41f, 65f, 65f, 65f, 6f), numbers(tj.get(0)));
            // Without kerning, a plain Tj; with ligatures, the pair is o and the fi ligature, with its own value.
            assertEquals(List.of(65f, 6f, 65f, 6f), numbers(tj.get(1)));
        }
        assertEquals(Map.of(), PdfUa.failedRules(bytes));
    }

    /** The arguments of each {@code TJ} of the page, in order. */
    private static List<List<Object>> tjArguments(PDPage page) throws IOException {
        List<List<Object>> result = new ArrayList<>();
        PDFStreamParser parser = new PDFStreamParser(page);
        List<Object> operands = new ArrayList<>();
        Object token;
        while ((token = parser.parseNextToken()) != null) {
            if (!(token instanceof Operator op)) {
                operands.add(token);
                continue;
            }
            if (op.getName().equals("TJ") && operands.get(0) instanceof COSArray array) {
                List<Object> items = new ArrayList<>();
                array.forEach(items::add);
                result.add(items);
            }
            operands.clear();
        }
        return result;
    }

    /** The shifts of the arguments of a {@code TJ}. */
    private static List<Float> numbers(List<Object> tj) {
        return tj.stream().filter(COSNumber.class::isInstance).map(n -> ((COSNumber) n).floatValue()).toList();
    }

    @Test
    void onePdfPagePerPage() throws Exception {
        InlineNode doc = reader.read(Docs.sectioned("Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n"
                + "\t\tHeight: 200\n\t\tMargin: 20\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + "\tText: Uno\n\tText: Dos\n\t\tBreak:\n\t\t\tBefore: page\n"));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "two-pages"))) {
            assertEquals(2, pdf.getNumberOfPages());
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(2);
            stripper.setEndPage(2);
            assertEquals("Dos", stripper.getText(pdf).strip());
        }
    }

    @Test
    void pageHasTheSizeOfThePageSetup() throws Exception {
        InlineNode doc = reader.read(Docs.sectioned("Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 842\n"
                + "\t\tMargin: 20\n\tBlock:\n\t\tBox:\n\t\t\tHeight: 500\n\t\t\tBackground: #F1EFE8\n"));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "page-size"))) {
            assertEquals(1, pdf.getNumberOfPages());
            assertEquals(300, pdf.getPage(0).getMediaBox().getWidth(), 1e-3);
            assertEquals(842, pdf.getPage(0).getMediaBox().getHeight(), 1e-3);
        }
    }

    private static final String HEADINGS = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 400\n"
            + "\t\tMargin: 40\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
            + "\tPage overlay:\n\t\tOffset: auto 0 -30 0\n\t\tText: Pie\n"
            + "\tHeading 1:\n\t\tText: Uno\n"
            + "\tParagraph: Hola.\n"
            + "\tHeading 2:\n\t\tText: Uno\n\t\tText: punto uno\n"
            + "\tParagraph: Adiós.\n\t\tBreak:\n\t\t\tAfter: page\n"
            + "\tHeading 1:\n\t\tText: Dos\n";

    @Test
    void headingsBecomeNestedBookmarks() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(render(reader.read(Docs.sectioned(HEADINGS)), "headings"))) {
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

    @Test
    void theDocumentIsTagged() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(render(reader.read(Docs.sectioned(HEADINGS)), "headings"))) {
            assertTrue(pdf.getDocumentCatalog().getMarkInfo().isMarked());
            PDStructureTreeRoot treeRoot = pdf.getDocumentCatalog().getStructureTreeRoot();
            PDStructureElement document = firstSection((PDStructureElement) treeRoot.getKids().get(0));
            assertEquals("Sect", document.getStructureType());
            assertEquals("Document", ((PDStructureElement) treeRoot.getKids().get(0)).getStructureType());
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
        InlineNode doc = reader.read(Docs.sectioned(LINKS
                + "\tParagraph: Hola\n"
                + "\tBlock:\n\t\tTypography:\n\t\t\tLanguage: en-GB\n\t\tParagraph: Hello\n\t\tText: World\n"
                + "\tParagraph: Adiós\n"));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "lang"))) {
            assertEquals("es", pdf.getDocumentCatalog().getLanguage());
            PDStructureElement document = firstSection((PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0));
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
    void titleAndLanguage() throws Exception {
        InlineNode doc = reader.read(Docs.sectioned("Document (dev.stxt.pdf):\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 200\n"
                + "\tTitle: Informe <trimestral> & resumen\n\tLanguage: es-ES\n"
                + "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n\tText: Hola\n"));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "title-language"))) {
            PDDocumentCatalog catalog = pdf.getDocumentCatalog();
            assertEquals("es-ES", catalog.getLanguage());
            assertEquals("Informe <trimestral> & resumen", pdf.getDocumentInformation().getTitle());
            assertTrue(catalog.getViewerPreferences().displayDocTitle());
            String xmp = new String(catalog.getMetadata().toByteArray(), StandardCharsets.UTF_8);
            assertTrue(xmp.contains("<rdf:li xml:lang=\"x-default\">Informe &lt;trimestral&gt; &amp; resumen</rdf:li>"), xmp);
            // With a title, the PDF declares that it is PDF/UA-1.
            assertTrue(xmp.contains("<pdfuaid:part>1</pdfuaid:part>"), xmp);
        }
    }

    @Test
    void listsAreTaggedWithLabelAndBody() throws Exception {
        InlineNode doc = reader.read(Docs.sectioned("Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 200\n"
                + "\t\tMargin: 20\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + "\tList:\n\t\tMarker: decimal\n\t\tBox:\n\t\t\tPadding: 0 0 0 20\n"
                + "\t\tList item:\n\t\t\tText: Uno\n"
                + "\t\tList item:\n\t\t\tText: Dos\n"
                + "\t\t\tList:\n\t\t\t\tMarker: bullet\n\t\t\t\tBox:\n\t\t\t\t\tPadding: 0 0 0 20\n"
                + "\t\t\t\tList item:\n\t\t\t\t\tText: Dentro\n"));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "lists"))) {
            PDStructureElement document = firstSection((PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0));
            PDStructureElement list = (PDStructureElement) document.getKids().get(0);
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

    @Test
    void tablesAreTaggedAndTheirRepeatedRowsAreArtifacts() throws Exception {
        StringBuilder body = new StringBuilder();
        for (int i = 1; i <= 15; i++) {
            body.append("\t\t\tRow:\n\t\t\t\tCell: Fila ").append(i).append("\n\t\t\t\tCell: ").append(i).append('\n');
        }
        // Page area of 160: the header and the footer (12 each) leave 136, eleven rows per page.
        InlineNode doc = reader.read(Docs.sectioned("Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 200\n"
                + "\t\tMargin: 20\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + "\tTable:\n\t\tColumn sizes: * 60\n"
                + "\t\tHeader rows:\n\t\t\tRow:\n\t\t\t\tCell: Concepto\n\t\t\t\tCell: Valor\n"
                + "\t\tBody:\n" + body
                + "\t\tFooter rows:\n\t\t\tRow:\n\t\t\t\tCell: Total\n\t\t\t\t\tSpan: 1 2\n"));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "tables"))) {
            assertEquals(2, pdf.getNumberOfPages());
            PDStructureElement document = firstSection((PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0));
            PDStructureElement table = (PDStructureElement) document.getKids().get(0);
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
        InlineNode doc = reader.read(Docs.sectioned(LINKS
                + "\tTable:\n\t\tColumn sizes: 100 100\n"
                + "\t\tHeader rows:\n\t\t\tRow:\n\t\t\t\tCell: Concepto\n\t\t\t\tCell: Importe\n"
                + "\t\tBody:\n\t\t\tRow:\n\t\t\t\tCell: Mesa\n\t\t\t\t\tRow header: true\n\t\t\t\tCell: 120\n"
                + "\t\tFooter rows:\n\t\t\tRow:\n\t\t\t\tCell: Total\n\t\t\t\t\tRow header: true\n\t\t\t\tCell: 120\n"));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "row-headers"))) {
            PDStructureElement document = firstSection((PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0));
            PDStructureElement table = (PDStructureElement) document.getKids().get(0);
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
     * A heading without an entry in the table of contents is still an {@code H1} and a bookmark
     * (reference/components/heading.md and reference/components/table-of-contents.md).
     */
    @Test
    void aHeadingOutOfTheContentsIsStillAHeadingAndABookmark() throws Exception {
        InlineNode doc = reader.read(Docs.sectioned(LINKS
                + "\tHeading 1:\n\t\tIn table of contents: false\n\t\tText: Portada\n"
                + "\tTable of contents:\n\t\tLevels: 1 1\n"
                + "\tHeading 1:\n\t\tText: Uno\n"));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "out-of-contents"))) {
            PDStructureElement document = firstSection((PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0));
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
        InlineNode doc = reader.read(Docs.mergeStyles(LINKS
                + "\tStyles:\n\t\tFor element: Heading 1\n\t\t\tTypography:\n\t\t\t\tNumbered: true\n"
                + "\t\tFor element: Heading 2\n\t\t\tTypography:\n\t\t\t\tNumbered: true\n"
                + "\tHeading 1:\n\t\tIn table of contents: false\n\t\tText: Portada\n\t\tTypography:\n\t\t\tNumbered: false\n"
                + "\tTable of contents:\n\t\tLevels: 1 2\n"
                + "\tHeading 1:\n\t\tText: Uno\n"
                + "\tHeading 2:\n\t\tText: Dos\n"));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "numbered-headings"))) {
            PDOutlineItem first = pdf.getDocumentCatalog().getDocumentOutline().getFirstChild();
            assertEquals("Portada", first.getTitle());
            // The cover is neither numbered nor counted: Uno is 1, and Dos, 1.1, nested in it.
            PDOutlineItem one = first.getNextSibling();
            assertEquals("1 Uno", one.getTitle());
            assertEquals("1.1 Dos", one.getFirstChild().getTitle());
        }
    }

    /**
     * A {@code Columns} is a {@code Div} and its content hangs from it, without one element per column
     * (reference/components/columns.md).
     */
    @Test
    void columnsIsADivWithoutOneElementPerColumn() throws Exception {
        InlineNode doc = reader.read(Docs.sectioned("Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 200\n"
                + "\t\tMargin: 20\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + "\tColumns:\n\t\tCount: 2\n\t\tGap: 10\n\t\tSeparator: 0.5\n"
                + "\t\tParagraph: Uno\n\t\tParagraph: Dos\n"));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "columns"))) {
            PDStructureElement document = firstSection((PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0));
            PDStructureElement columnsBox = (PDStructureElement) document.getKids().get(0);
            assertEquals("Div", columnsBox.getStructureType());
            assertEquals(List.of("P", "P"), types(columnsBox));
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("Uno") && text.contains("Dos"), text);
        }
    }

    private static final String LINKS = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 200\n"
            + "\t\tMargin: 20\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n";

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
        InlineNode doc = reader.read(Docs.sectioned(LINKS
                + "\tText: Escribe a soporte\n\t\tLink: mailto:soporte@airemas.example\n"
                + "\tImage: test/wide.png\n\t\tAlt: Logotipo\n\t\tLink: https://stxt.dev\n"
                + "\tPage overlay:\n\t\tOffset: auto 0 0 auto\n\t\tText: pie\n\t\t\tLink: https://pie.example\n"
                + "\tMarkdown:\n\t\tContent >>\n\t\t\tVisita [la web](https://web.example) hoy.\n"));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "links"))) {
            List<PDAnnotationLink> links = links(pdf, 0);
            // A Page overlay is painted last, on top of everything.
            assertEquals(List.of("mailto:soporte@airemas.example", "https://stxt.dev", "https://web.example",
                    "https://pie.example"), links.stream().map(PdfCanvasTest::uri).toList());
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
            PDStructureElement outer = (PDStructureElement) tree.getKids().get(0);
            PDStructureElement document = firstSection(outer);
            // The Link of the Page overlay wraps no content, but it is in its place in the section: after the image.
            assertEquals(List.of("Sect"), types(outer));
            assertEquals(List.of("P", "Link", "Link", "Div"), types(document));
            // The box with Link: P → Link → [marked content of the text, OBJR]; no Alt, the text already says it.
            PDStructureElement span = (PDStructureElement) document.getKids().get(0);
            assertEquals(List.of("Link"), types(span));
            PDStructureElement link = (PDStructureElement) span.getKids().get(0);
            assertEquals(List.of("MCR", "OBJR"), shape(link));
            assertNull(link.getAlternateDescription());
            PDObjectReference reference = (PDObjectReference) link.getKids().get(1);
            assertEquals(text.getCOSObject(), ((PDAnnotationLink) reference.getReferencedObject()).getCOSObject());
            // The parent tree leads from the annotation to its Link.
            assertEquals(link.getCOSObject(), ((COSObjectable) tree.getParentTree().getValue(text.getStructParent())).getCOSObject());
            // The image: Link → Figure (with its Alt) and the OBJR.
            PDStructureElement imageLink = (PDStructureElement) document.getKids().get(1);
            assertEquals(List.of("Figure", "OBJR"), shape(imageLink));
            assertEquals("Logotipo", ((PDStructureElement) imageLink.getKids().get(0)).getAlternateDescription());
            assertNull(imageLink.getAlternateDescription());
            // The Markdown run: P → [text, Link → [link text, OBJR], text].
            PDStructureElement paragraph = (PDStructureElement) ((PDStructureElement) document.getKids().get(3))
                    .getKids().get(0);
            assertEquals("P", paragraph.getStructureType());
            assertEquals(List.of("MCR", "Link", "MCR"), shape(paragraph));
            assertEquals(List.of("MCR", "OBJR"), shape((PDStructureElement) paragraph.getKids().get(1)));
            // The Page overlay: a Link without content, with the text as Alt and the OBJR.
            PDStructureElement overlay = (PDStructureElement) document.getKids().get(2);
            assertEquals(List.of("OBJR"), shape(overlay));
            assertEquals("pie", overlay.getAlternateDescription());
            assertEquals(overlay.getCOSObject(), ((COSObjectable) tree.getParentTree()
                    .getValue(links.get(3).getStructParent())).getCOSObject());
            String extracted = new PDFTextStripper().getText(pdf);
            assertTrue(extracted.contains("Visita la web hoy."), extracted);
        }
    }

    /**
     * A text link that spans several lines is a single {@code Link} with one marked content and one
     * {@code OBJR} per line; there is one area per line (reference/implementation/pdf.md).
     */
    @Test
    void aLinkOverSeveralLinesIsOneLinkElement() throws Exception {
        InlineNode doc = reader.read(Docs.sectioned(LINKS.replace("Width: 300", "Width: 100")
                + "\tMarkdown:\n\t\tContent >>\n\t\t\tAntes [un enlace que ocupa bastantes líneas de texto](https://a.example) después.\n"));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "link-lines"))) {
            List<PDAnnotationLink> links = links(pdf, 0);
            assertTrue(links.size() >= 3, "" + links.size());
            PDStructureElement document = firstSection((PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0));
            PDStructureElement paragraph = (PDStructureElement) ((PDStructureElement) document.getKids().get(0))
                    .getKids().get(0);
            assertEquals(List.of("MCR", "Link", "MCR"), shape(paragraph));
            List<String> link = shape((PDStructureElement) paragraph.getKids().get(1));
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
        InlineNode doc = reader.read(Docs.sectioned(LINKS
                + "\tBlock:\n\t\tLink: https://a.example\n\t\tText: Uno\n\t\tText: Dos\n"
                + "\tBlock:\n\t\tLink: https://b.example\n\t\tBox:\n\t\t\tHeight: 10\n\t\t\tBackground: #CCC\n"));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "box-link"))) {
            PDStructureElement document = firstSection((PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0));
            assertEquals(List.of("Div", "Div"), types(document));
            PDStructureElement full = (PDStructureElement) document.getKids().get(0);
            assertEquals(List.of("Link"), types(full));
            PDStructureElement link = (PDStructureElement) full.getKids().get(0);
            assertEquals(List.of("P", "P", "OBJR"), shape(link));
            assertNull(link.getAlternateDescription());
            PDStructureElement empty = (PDStructureElement) document.getKids().get(1);
            PDStructureElement emptyLink = (PDStructureElement) empty.getKids().get(0);
            assertEquals(List.of("OBJR"), shape(emptyLink));
            assertEquals("https://b.example", emptyLink.getAlternateDescription());
        }
    }

    /**
     * A link to a box of the document leads to its page and its corner, forward and
     * backward (reference/common/links.md).
     */
    @Test
    void internalLinksGoToThePageOfTheirBox() throws Exception {
        // Page area of 160: the text (12) and the block (140) fill the first page; the rest, the second.
        InlineNode doc = reader.read(Docs.sectioned(LINKS
                + "\tText: Ir al final\n\t\tId: inicio\n\t\tLink: #fin\n"
                + "\tBlock:\n\t\tBox:\n\t\t\tHeight: 140\n"
                + "\tText: Final\n\t\tId: fin\n"
                + "\tText: Volver\n\t\tLink: #inicio\n"));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "internal-links"))) {
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
        InlineNode doc = reader.read(Docs.sectioned(LINKS
                + "\tTable of contents:\n\t\tLevels: 1 1\n"
                + "\tHeading 1:\n\t\tText: Uno\n"
                + "\tBlock:\n\t\tBreak:\n\t\t\tAfter: page\n"
                + "\tHeading 1:\n\t\tText: Dos\n"));
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
            // The text of the entry, without its page number.
            assertEquals(List.of("Uno", "Dos"), links.stream().map(PDAnnotation::getContents).toList());
            PDStructureElement document = firstSection((PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0));
            PDStructureElement toc = (PDStructureElement) document.getKids().get(0);
            toc.getKids().forEach(k -> assertTrue(types((PDStructureElement) k).contains("Link")));
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
        InlineNode doc = reader.read(Docs.sectioned("Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 200\n"
                + "\t\tMargin: 20\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + "\tTable:\n\t\tColumn sizes: * 60\n"
                + "\t\tHeader rows:\n\t\t\tRow:\n\t\t\t\tCell: Concepto\n\t\t\t\t\tLink: https://c.example\n\t\t\t\tCell: Valor\n"
                + "\t\tBody:\n" + body));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "table-links"))) {
            assertEquals(2, pdf.getNumberOfPages());
            assertEquals(List.of("https://c.example"), links(pdf, 0).stream().map(PdfCanvasTest::uri).toList());
            assertEquals(List.of("https://c.example"), links(pdf, 1).stream().map(PdfCanvasTest::uri).toList());
            PDStructureElement outer = (PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0);
            PDStructureElement document = firstSection(outer);
            PDStructureElement table = (PDStructureElement) document.getKids().get(0);
            PDStructureElement headRow = (PDStructureElement) ((PDStructureElement) table.getKids().get(0)).getKids().get(0);
            PDStructureElement th = (PDStructureElement) headRow.getKids().get(0);
            assertEquals(List.of("Link"), types(th));
            // The Link of the cell wraps the text of the first page and carries the annotations of both.
            PDStructureElement cellLink = (PDStructureElement) th.getKids().get(0);
            assertEquals(List.of("MCR", "OBJR", "OBJR"), shape(cellLink));
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
        InlineNode doc = reader.read(Docs.sectioned(LINKS
                + "\tPage overlay:\n\t\tOffset: auto 0 0 auto\n\t\tText: Más en [la web](https://pie.example)\n"
                + "\t\t\tTypography:\n\t\t\t\tMarkup: markdown\n"
                + "\tText: Uno\n"
                + "\tImage: test/wide.png\n\t\tLink: https://logo.example\n"
                + "\tText: Dos\n\t\tBreak:\n\t\t\tBefore: page\n"));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "links-not-content"))) {
            assertEquals(2, pdf.getNumberOfPages());
            PDStructureElement outer = (PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0);
            assertEquals(List.of("Sect"), types(outer));
            PDStructureElement section = firstSection(outer);
            // The Page overlay is before the text in the document, and the image between the two texts.
            assertEquals(List.of("Link", "P", "Link", "P"), types(section));
            PDStructureElement footer = (PDStructureElement) section.getKids().get(0);
            assertEquals(List.of("OBJR", "OBJR"), shape(footer));
            assertEquals("la web", footer.getAlternateDescription());
            assertEquals(pdf.getPage(0), ((PDObjectReference) footer.getKids().get(0)).getPage());
            assertEquals(pdf.getPage(1), ((PDObjectReference) footer.getKids().get(1)).getPage());
            PDStructureElement logo = (PDStructureElement) section.getKids().get(2);
            assertEquals(List.of("OBJR"), shape(logo));
            assertEquals("https://logo.example", logo.getAlternateDescription());
        }
    }

    /** A {@code Page overlay} that appears on no page leaves no empty {@code Link}. */
    @Test
    void aPageOverlayThatIsNeverPaintedLeavesNoEmptyLink() throws Exception {
        InlineNode doc = reader.read(Docs.sectioned(LINKS
                + "\tPage overlay:\n\t\tPages: even\n\t\tText: pie\n\t\t\tLink: https://pie.example\n"
                + "\tText: Uno\n"));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "links-never-painted"))) {
            PDStructureElement outer = (PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0);
            assertEquals(List.of("P"), types(firstSection(outer)));
        }
    }

    /** The children of an element in order: the type of each element, {@code MCR} and {@code OBJR}. */
    private static List<String> shape(PDStructureElement e) {
        List<String> kids = new ArrayList<>();
        for (Object k : e.getKids()) {
            if (k instanceof PDStructureElement child) {
                kids.add(child.getStructureType());
            } else if (k instanceof PDObjectReference) {
                kids.add("OBJR");
            } else {
                kids.add("MCR");
            }
        }
        return kids;
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
     * Each run with its font, in the same marked content of the paragraph; the underline and the strikethrough,
     * <em>artifacts</em> (reference/common/text-formatting.md). It is reviewed by eye in {@code target/pdf/runs.pdf}.
     */
    @Test
    void runsUseTheirFontsAndDecorationsAreArtifacts() throws Exception {
        InlineNode doc = reader.read(Docs.sectioned("Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 200\n"
                + "\t\tMargin: 40\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 12\n"
                + "\t\t\t\tAlign: justify\n\tParagraph: " + RUNS + "\n\t\tId: P\n"));
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
            // A single P with a single marked content: the runs have no element of their own.
            PDStructureElement document = firstSection((PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0));
            assertEquals(List.of("P"), types(document));
            PDStructureElement p = (PDStructureElement) document.getKids().get(0);
            assertEquals(1, p.getKids().size());
            String content = new String(pdf.getPage(0).getContents().readAllBytes(), StandardCharsets.ISO_8859_1);
            assertTrue(content.contains("/Artifact BMC"), content);
        }
    }

    /**
     * What a Markdown generates is tagged and gives bookmarks as if the author had written it
     * (reference/components/markdown.md).
     */
    @Test
    void markdownIsTaggedAndItsHeadingsAreBookmarks() throws Exception {
        InlineNode doc = reader.read(Docs.sectioned("Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 300\n"
                + "\t\tMargin: 40\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + "\t\tFor element: List\n\t\t\tBox:\n\t\t\t\tPadding: 0 0 0 15\n"
                + "\tMarkdown:\n\t\tBullet markers: bullet\n\t\tTable columns: equal\n\t\tContent >>\n"
                + "\t\t\t# Informe\n\n\t\t\tCon **negrita** y ++subrayado++.\n\n\t\t\t## Datos\n\n"
                + "\t\t\t- uno\n\t\t\t- dos\n\n\t\t\t| A | B |\n\t\t\t|---|---|\n\t\t\t| 1 | 2 |\n"));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "markdown"))) {
            PDStructureElement document = firstSection((PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0));
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
        InlineNode doc = reader.read(Docs.sectioned("Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 200\n"
                + "\t\tMargin: 20\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + "\tParagraph: Emisor y destinatario\n\t\tTypography:\n\t\t\tCase: uppercase\n"
                + "\t\t\tLetter spacing: 1\n"
                + "\tParagraph: Año nuevo\n\t\tTypography:\n\t\t\tSmall caps: true\n"
                + "\tParagraph: Hoja {page}\n\t\tTypography:\n\t\t\tCase: uppercase\n"
                + "\tParagraph: Sin cambios\n"));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "caps"))) {
            PDStructureElement document = firstSection((PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0));
            List<String> actual = new ArrayList<>();
            for (Object kid : document.getKids()) {
                actual.add(((PDStructureElement) kid).getActualText());
            }
            assertEquals(java.util.Arrays.asList("Emisor y destinatario", "Año nuevo", null, null), actual);
            // What is painted is capitals, and letter spacing does not split the text into letters (pdfbox reads it
            // whole).
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("EMISOR Y DESTINATARIO"), text);
            assertTrue(text.contains("AÑO NUEVO"), text);
            assertTrue(text.contains("HOJA 1"), text);
        }
    }

    /**
     * With large letter spacing, an extractor would put spaces between the letters; each run carries in its marked
     * content the {@code ActualText} with its text (reference/implementation/pdf.md), which pdfbox respects.
     */
    @Test
    void letterSpacedTextIsExtractedWhole() throws Exception {
        InlineNode doc = reader.read(Docs.sectioned("Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 200\n"
                + "\t\tMargin: 20\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + "\tParagraph: Informe del trimestre\n\t\tTypography:\n\t\t\tLetter spacing: 3\n"
                + "\tParagraph: Informe **del** trimestre\n\t\tTypography:\n\t\t\tLetter spacing: 3\n\t\t\tMarkup: markdown\n"));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "letter-spacing"))) {
            // pdfbox leaves spaces after the ActualText of each sequence: the text is that of each line without them.
            assertEquals(List.of("Informe del trimestre", "Informe del trimestre"),
                    new PDFTextStripper().getText(pdf).strip().lines().map(String::stripTrailing).toList());
        }
    }

    /**
     * A word split at the end of a line (reference/common/typography.md) is painted with a hyphen, but the text that
     * is read has the soft hyphen U+00AD in its place and no space after it: the two parts are one word for whoever
     * extracts it (reference/implementation/pdf.md), and the PDF is still PDF/UA.
     */
    @Test
    void hyphenatedWordsAreReadWithASoftHyphen() throws Exception {
        InlineNode doc = reader.read(Docs.sectioned("Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 200\n"
                + "\t\tMargin: 20\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + "\t\t\t\tHyphenate: true\n"
                + "\tParagraph: la programación extraordinaria\n\t\tBox:\n\t\t\tWidth: 60\n"));
        byte[] bytes = render(doc, "hyphenation");
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            // pdfbox reads the ActualText and drops its soft hyphen: each line is its part of the word, bare.
            assertEquals(List.of("la programa", "ción extraor", "dinaria"),
                    new PDFTextStripper().getText(pdf).strip().lines().map(String::stripTrailing).toList());
            // The sequence that ends each split line carries its text with the soft hyphen.
            assertEquals(List.of("la programa\u00AD", "ción extraor\u00AD"), actualTexts(pdf.getPage(0)));
        }
        assertEquals(Map.of(), PdfUa.failedRules(bytes));
    }

    /** The {@code ActualText} of each marked content of the page, in order. */
    private static List<String> actualTexts(PDPage page) throws IOException {
        List<String> texts = new ArrayList<>();
        PDFStreamParser parser = new PDFStreamParser(page);
        List<Object> operands = new ArrayList<>();
        Object token;
        while ((token = parser.parseNextToken()) != null) {
            if (!(token instanceof Operator op)) {
                operands.add(token);
                continue;
            }
            if (op.getName().equals("BDC") && operands.size() == 2 && operands.get(1) instanceof COSName name) {
                PDPropertyList properties = page.getResources().getProperties(name);
                String actual = properties == null ? null : properties.getCOSObject().getString(COSName.ACTUAL_TEXT);
                if (actual != null) {
                    texts.add(actual);
                }
            }
            operands.clear();
        }
        return texts;
    }

    /**
     * A table of contents (reference/components/table-of-contents.md) is a {@code TOC} with one {@code TOCI} per entry
     * (title and number, two {@code Span}); the leader is an artifact. With the real metrics, it writes the page of
     * each heading, also that of a heading in a Markdown.
     */
    @Test
    void contentsIsATocWithOneTociPerEntry() throws Exception {
        InlineNode doc = reader.read(Docs.sectioned("Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 300\n"
                + "\t\tMargin: 40\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + "\tTable of contents:\n\t\tLevels: 1 2\n"
                + "\tMarkdown:\n\t\tContent >>\n\t\t\t# Uno\n\n\t\t\tTexto.\n"
                + "\tBlock:\n\t\tBox:\n\t\t\tHeight: 250\n"
                + "\tHeading 1:\n\t\tText: Dos\n\tHeading 2:\n\t\tText: Dos punto uno\n"));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "table-of-contents"))) {
            PDStructureElement document = firstSection((PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0));
            assertEquals(List.of("TOC", "Div", "Div", "H1", "H2"), types(document));
            PDStructureElement toc = (PDStructureElement) document.getKids().get(0);
            assertEquals(List.of("TOCI", "TOCI", "TOCI"), types(toc));
            // The entry: TOCI → Link → [title, number, OBJR].
            PDStructureElement entry = (PDStructureElement) toc.getKids().get(0);
            assertEquals(List.of("Link"), types(entry));
            assertEquals(List.of("Span", "Span", "OBJR"), shape((PDStructureElement) entry.getKids().get(0)));
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("Uno"), text);
            assertTrue(text.matches("(?s).*Dos punto uno\\s*2.*"), text);
        }
    }

    /**
     * An image with {@code Alt} is a {@code Figure} with its alternative text and its {@code BBox}; without it, and in
     * a {@code Page overlay}, it is an artifact (reference/components/image.md). Each file is embedded once.
     */
    @Test
    void imagesAreFiguresOrArtifactsAndAreEmbeddedOnce() throws Exception {
        InlineNode doc = reader.read(Docs.sectioned("Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 300\n"
                + "\t\tMargin: 40\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + "\tPage overlay:\n\t\tOffset: -30 -30 auto auto\n\t\tImage: test/square.png\n"
                + "\tImage: test/wide.png\n\t\tAlt: Paisaje\n"
                + "\tImage: test/tall.jpg\n\t\tBox:\n\t\t\tHeight: 60\n"
                + "\tParagraph: Fin.\n"
                + "\tImage: test/wide.png\n\t\tBreak:\n\t\t\tBefore: page\n"));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "images"))) {
            assertEquals(2, pdf.getNumberOfPages());
            PDStructureElement document = firstSection((PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0));
            assertEquals(List.of("Figure", "P"), types(document));
            PDStructureElement figure = (PDStructureElement) document.getKids().get(0);
            assertEquals("Paisaje", figure.getAlternateDescription());
            PDLayoutAttributeObject layout = (PDLayoutAttributeObject) figure.getAttributes().getObject(0);
            PDRectangle box = layout.getBBox();
            // The content box of the image: 150 × 75 at (40, 40), with the origin at the bottom.
            assertEquals(40, box.getLowerLeftX(), 0.01);
            assertEquals(300 - 40 - 75, box.getLowerLeftY(), 0.01);
            assertEquals(150, box.getWidth(), 0.01);

            List<PDImageXObject> first = images(pdf, 0);
            assertEquals(3, first.size());
            PDImageXObject wide = first.stream().filter(i -> i.getWidth() == 200).findFirst().orElseThrow();
            // The PNG keeps its transparency; the JPEG goes as is.
            assertTrue(wide.getSoftMask() != null);
            assertTrue(first.stream().anyMatch(i -> i.getHeight() == 160 && "jpg".equals(i.getSuffix())));
            List<PDImageXObject> second = images(pdf, 1);
            assertTrue(second.stream().anyMatch(i -> i.getCOSObject() == wide.getCOSObject()),
                    "The same image on two pages is embedded once");

            String content = new String(pdf.getPage(0).getContents().readAllBytes(), StandardCharsets.ISO_8859_1);
            assertTrue(content.contains("/Figure <</MCID 0>> BDC"), content);
        }
    }

    /**
     * An SVG (reference/components/image.md) is a vector drawing: a PDF form, not an image, which is embedded once
     * even if used on two pages and is scaled to its box; with {@code Alt} it is a {@code Figure}. The text of the
     * SVG is drawn with the font from {@code fonts/}: the pixels of the letters are where they belong (white on the
     * dark background).
     */
    @Test
    void svgIsDrawnAsOneVectorFormAndScaledToItsBox() throws Exception {
        InlineNode doc = reader.read(Docs.sectioned("Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 400\n\t\tHeight: 300\n"
                + "\t\tMargin: 40\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + "\tImage: test/shapes.svg\n\t\tAlt: Un paisaje\n\t\tBox:\n\t\t\tWidth: 300\n"
                + "\tImage: test/label.svg\n\t\tBox:\n\t\t\tMargin: 10 0 0 0\n"
                + "\tImage: test/shapes.svg\n\t\tBreak:\n\t\t\tBefore: page\n"));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "svg"))) {
            assertEquals(2, pdf.getNumberOfPages());
            PDResources first = pdf.getPage(0).getResources();
            List<PDFormXObject> forms = new ArrayList<>();
            for (COSName name : first.getXObjectNames()) {
                assertInstanceOf(PDFormXObject.class, first.getXObject(name), "An SVG is not a pixel image");
                forms.add((PDFormXObject) first.getXObject(name));
            }
            assertEquals(2, forms.size());
            PDResources second = pdf.getPage(1).getResources();
            PDFormXObject shapes = forms.stream().filter(f -> f.getBBox().getWidth() == 150).findFirst().orElseThrow();
            assertTrue(second.getXObjectNames().iterator().hasNext());
            assertEquals(shapes.getCOSObject(), second.getXObject(second.getXObjectNames().iterator().next())
                    .getCOSObject(), "The same SVG on two pages is embedded once");
            assertEquals(75, shapes.getBBox().getHeight(), 1e-3);

            // 300 pt wide for a natural 150: it is scaled by two, from the bottom corner of the box.
            String content = new String(pdf.getPage(0).getContents().readAllBytes(), StandardCharsets.ISO_8859_1);
            assertTrue(content.matches("(?s).*\\b2 0 0 2 40 \\S+ cm\\b.*"), content);
            assertTrue(content.contains("/Figure <</MCID 0>> BDC"), content);
            // The form inherits the graphics state (like the dashes of debug mode): it starts from scratch.
            assertTrue(content.contains("[] 0 d"), content);

            // Vector content carries paths, not an image; the text of label.svg is visible (white letters on #3b2a1e).
            String shapesContent = new String(shapes.getContentStream().createInputStream().readAllBytes(),
                    StandardCharsets.ISO_8859_1);
            assertTrue(shapesContent.contains(" c\n") || shapesContent.contains(" l\n"), shapesContent);
            BufferedImage image = new PDFRenderer(pdf).renderImageWithDPI(0, 72);
            int label = 40 + 150 + 10;
            int whites = 0;
            for (int y = label; y < label + 36; y++) {
                for (int x = 40; x < 40 + 120; x++) {
                    int rgb = image.getRGB(x, y) & 0xFFFFFF;
                    if ((rgb >> 16 & 0xFF) > 230 && (rgb >> 8 & 0xFF) > 230 && (rgb & 0xFF) > 230) {
                        whites++;
                    }
                }
            }
            assertTrue(whites > 30, "The letters of label.svg have to be visible: " + whites);
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

    /**
     * A rotated box (reference/common/layers-and-rotation.md) rotates its content with a {@code cm} matrix; its link is
     * the rectangle that encloses the rotated area with the {@code QuadPoints} of the area, the {@code BBox} of a
     * rotated figure encloses what is painted, and a destination leads to the corner of that enclosing rectangle.
     * Rotated text is still text.
     */
    @Test
    void rotatedBoxesTurnTheirContentLinksAndFigures() throws Exception {
        InlineNode doc = reader.read(Docs.sectioned("Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 300\n"
                + "\t\tMargin: 40\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + "\tImage: test/wide.png\n\t\tAlt: Paisaje\n\t\tRotate: 90\n"
                + "\tText: Ir al sello\n\t\tLink: #sello\n"
                + "\tOverlay:\n\t\tId: sello\n\t\tOffset: 60 auto auto 60\n\t\tRotate: 90\n\t\tLink: https://stxt.dev\n"
                + "\t\tBox:\n\t\t\tWidth: 100\n\t\t\tHeight: 20\n\t\tText: Sello\n"));
        try (PDDocument pdf = Loader.loadPDF(render(doc, "rotation"))) {
            List<PDAnnotationLink> links = links(pdf, 0);
            PDAnnotationLink stamp = links.stream().filter(l -> l.getAction() != null).findFirst().orElseThrow();
            // The box goes from (100, 100) to (200, 120); rotated 90 degrees around (150, 110), from
            // (140, 60) to (160, 160): with the origin at the bottom, from y = 140 to y = 240.
            PDRectangle area = stamp.getRectangle();
            assertEquals(140, area.getLowerLeftX(), 0.01);
            assertEquals(140, area.getLowerLeftY(), 0.01);
            assertEquals(20, area.getWidth(), 0.01);
            assertEquals(100, area.getHeight(), 0.01);
            float[] expected = {140, 240, 140, 140, 160, 140, 160, 240};
            float[] quad = stamp.getQuadPoints();
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], quad[i], 0.01, "QuadPoints " + i);
            }

            // The destination of the internal link: the top left corner of the enclosing rectangle.
            PDAnnotationLink internal = links.stream().filter(l -> l.getAction() == null).findFirst().orElseThrow();
            PDPageXYZDestination target = (PDPageXYZDestination) internal.getDestination();
            assertEquals(140, target.getLeft());
            assertEquals(240, target.getTop());

            // The image, 150 × 75 and rotated 90 in flow, takes 75 × 150 at (40, 40): with the origin at the bottom,
            // from y = 110 to y = 260.
            PDStructureElement document = firstSection((PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot()
                    .getKids().get(0));
            PDStructureElement figure = (PDStructureElement) document.getKids().get(0);
            assertEquals("Figure", figure.getStructureType());
            PDRectangle box = ((PDLayoutAttributeObject) figure.getAttributes().getObject(0)).getBBox();
            assertEquals(40, box.getLowerLeftX(), 0.01);
            assertEquals(110, box.getLowerLeftY(), 0.01);
            assertEquals(75, box.getWidth(), 0.01);
            assertEquals(150, box.getHeight(), 0.01);

            String content = new String(pdf.getPage(0).getContents().readAllBytes(), StandardCharsets.ISO_8859_1);
            assertTrue(content.contains(" cm\n"), content);
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("Sello"), text);
        }
    }

    private static final String SECTIONS_HEAD = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 300\n"
            + "\t\tHeight: 200\n\t\tMargin: 20\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n"
            + "\t\t\t\tSize: 10\n\t\tFor style: english\n\t\t\tTypography:\n\t\t\t\tLanguage: en\n";

    /**
     * The page labels of the PDF are the printed number (reference/components/document.md): none on the
     * cover, and the appendix restarts.
     */
    @Test
    void pageLabelsAreThePrintedNumbers() throws Exception {
        InlineNode doc = reader.read(SECTIONS_HEAD
                + "\tSection:\n\t\tPage numbering: none\n\t\tText: Portada\n"
                + "\tSection:\n\t\tText: Uno\n\t\tText: Dos\n\t\t\tBreak:\n\t\t\t\tBefore: page\n"
                + "\tSection:\n\t\tPage numbering: restart\n\t\tText: Anexo\n");
        try (PDDocument pdf = Loader.loadPDF(render(doc, "page-labels"))) {
            assertEquals(4, pdf.getNumberOfPages());
            assertArrayEquals(new String[] {"", "1", "2", "1"},
                    pdf.getDocumentCatalog().getPageLabels().getLabelsByPageIndices());
        }
        // With nothing to change, no label: the printed number is the page number.
        InlineNode plain = reader.read(SECTIONS_HEAD + "\tSection:\n\t\tText: Uno\n\t\tText: Dos\n\t\t\tBreak:\n\t\t\t\tBefore: page\n");
        try (PDDocument pdf = Loader.loadPDF(render(plain, "page-labels-plain"))) {
            assertNull(pdf.getDocumentCatalog().getPageLabels());
        }
    }

    /**
     * With {@code Page number format} and {@code Start}, the label carries the style and the number of each page
     * sequence (i, ii, 5, VI). The PDF entries are checked and not the text that PDFBox generates, which uppercases
     * with the language of the JVM: in Turkish, "vi" would be "Vİ".
     */
    @Test
    void pageLabelsKeepThePageStyleAndTheStart() throws Exception {
        InlineNode doc = reader.read(SECTIONS_HEAD
                + "\tSection:\n\t\tPage number format: lower-roman\n\t\tText: Prefacio\n\t\tText: Más\n\t\t\tBreak:\n\t\t\t\tBefore: page\n"
                + "\tSection:\n\t\tPage numbering: restart\n\t\tStart: 5\n\t\tText: Cuerpo\n"
                + "\tSection:\n\t\tPage number format: upper-roman\n\t\tText: Anexo\n");
        try (PDDocument pdf = Loader.loadPDF(render(doc, "page-labels-styles"))) {
            PDPageLabels labels = pdf.getDocumentCatalog().getPageLabels();
            List<String> ranges = new ArrayList<>();
            for (int index : new TreeSet<>(labels.getPageIndices())) {
                PDPageLabelRange range = labels.getPageLabelRange(index);
                ranges.add(index + ":" + range.getStyle() + ":" + range.getStart());
            }
            assertEquals(List.of("0:r:1", "2:D:5", "3:R:6"), ranges);
        }
    }

    /** Each PDF page has the size of its section, and each section is a {@code Sect} with its language. */
    @Test
    void eachSectionIsASectWithItsOwnPageSizeAndLanguage() throws Exception {
        InlineNode doc = reader.read(SECTIONS_HEAD
                + "\tSection:\n\t\tText: Uno\n"
                + "\tSection:\n\t\tStyle: english\n\t\tPage:\n\t\t\tOrientation: landscape\n\t\t\tPaper: A4\n\t\tText: Two\n");
        try (PDDocument pdf = Loader.loadPDF(render(doc, "sections-structure"))) {
            assertEquals(300, pdf.getPage(0).getMediaBox().getWidth(), 1e-3);
            assertEquals(297 * 72 / 25.4, pdf.getPage(1).getMediaBox().getWidth(), 1e-3);
            assertEquals(210 * 72 / 25.4, pdf.getPage(1).getMediaBox().getHeight(), 1e-3);
            PDStructureElement outer = (PDStructureElement) pdf.getDocumentCatalog().getStructureTreeRoot().getKids().get(0);
            assertEquals(List.of("Sect", "Sect"), types(outer));
            assertNull(((PDStructureElement) outer.getKids().get(0)).getLanguage());
            assertEquals("en", ((PDStructureElement) outer.getKids().get(1)).getLanguage());
        }
    }

    /**
     * The first {@code Sect} of a tagged document (reference/components/document.md): what hangs from it is the
     * content.
     */
    private static PDStructureElement firstSection(PDStructureElement document) {
        assertEquals("Document", document.getStructureType());
        PDStructureElement section = (PDStructureElement) document.getKids().get(0);
        assertEquals("Sect", section.getStructureType());
        return section;
    }
}
