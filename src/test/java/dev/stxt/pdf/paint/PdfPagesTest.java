package dev.stxt.pdf.paint;

import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.golden.PdfUa;
import dev.stxt.pdf.stxt.DocumentReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentGroup;
import org.apache.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentProperties;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDTilingPattern;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The page of a PDF as an image (reference/components/image.md): a form with its content, its text and its rotation,
 * without the tagging of its PDF, which is tagged as the {@code Image} that draws it.
 */
class PdfPagesTest {

    /** Where a point of the crop ends up with the matrix of the rotated page. */
    private static Point2D at(AffineTransform m, double x, double y) {
        return m.transform(new Point2D.Double(x, y), null);
    }

    private static void assertPoint(double x, double y, Point2D p) {
        assertEquals(x, p.getX(), 1e-9, "x of " + p);
        assertEquals(y, p.getY(), 1e-9, "y of " + p);
    }

    /**
     * The matrix takes the crop (here, 200 × 100 at (10, 20)) to a rectangle with its corner at the origin, rotated as
     * a viewer shows it: with 90°, the top left corner moves to the top right one.
     */
    @Test
    void placementTurnsTheCropToTheOrigin() {
        PDRectangle crop = new PDRectangle(10, 20, 200, 100);
        AffineTransform none = PdfPages.placement(crop, 0);
        assertPoint(0, 0, at(none, 10, 20));
        assertPoint(200, 100, at(none, 210, 120));

        // It is seen as 100 × 200.
        AffineTransform quarter = PdfPages.placement(crop, 90);
        assertPoint(100, 200, at(quarter, 10, 120));
        assertPoint(0, 200, at(quarter, 10, 20));
        assertPoint(0, 0, at(quarter, 210, 20));
        assertPoint(100, 0, at(quarter, 210, 120));

        AffineTransform half = PdfPages.placement(crop, 180);
        assertPoint(200, 100, at(half, 10, 20));
        assertPoint(0, 0, at(half, 210, 120));

        // The opposite of 90°: the top left corner moves to the bottom left one.
        AffineTransform threeQuarters = PdfPages.placement(crop, 270);
        assertPoint(0, 0, at(threeQuarters, 10, 120));
        assertPoint(100, 0, at(threeQuarters, 10, 20));
        assertPoint(100, 200, at(threeQuarters, 210, 20));
    }

    private static final String DOCUMENT = String.join("\n",
            "Document (dev.stxt.pdf):",
            "\tLanguage: es",
            "\tTitle: Prueba",
            "\tPage:",
            "\t\tWidth: 400",
            "\t\tHeight: 300",
            "\t\tMargin: 40",
            "\tStyles:",
            "\t\tFor element: Document",
            "\t\t\tTypography:",
            "\t\t\t\tFont: IBM Plex Sans",
            "\t\t\t\tSize: 10",
            "\tSection:",
            "\t\tPage overlay:",
            "\t\t\tOffset: -40 -40 -40 -40",
            "\t\t\tLayer: -1",
            "\t\t\tImage: examples/letterhead/letterhead.pdf",
            "\t\t\t\tPdf page: 2",
            "\t\t\t\tFit: stretch",
            "\t\t\t\tBox:",
            "\t\t\t\t\tWidth: 100%",
            "\t\t\t\t\tHeight: 100%",
            "\t\tImage: test/pages.pdf",
            "\t\t\tPdf page: 3",
            "\t\t\tAlt: La página 3 de la prueba",
            "\t\tImage: test/pages.pdf",
            "\t\t\tPdf page: 3",
            "\t\t\tBreak:",
            "\t\t\t\tBefore: page",
            "");

    private static byte[] pdf(int ua) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.builder().pdfUa(ua).build().toPdf(new DocumentReader().read(DOCUMENT), out);
        Path dir = Path.of("target", "pdf");
        Files.createDirectories(dir);
        Files.write(dir.resolve("pdf-pages-ua" + ua + ".pdf"), out.toByteArray());
        return out.toByteArray();
    }

    private static List<PDFormXObject> forms(PDResources resources) throws Exception {
        List<PDFormXObject> forms = new ArrayList<>();
        for (COSName name : resources.getXObjectNames()) {
            forms.add((PDFormXObject) resources.getXObject(name));
        }
        return forms;
    }

    /**
     * The page stays vector and keeps its text, which can be extracted; it is copied once even if it appears on two
     * pages; and the tagging of its PDF (the letterhead is a tagged PDF from the engine itself) does not reach the
     * document, which is still PDF/UA-1 and PDF/UA-2.
     */
    @Test
    void aPageIsAFormWithItsTextAndWithoutItsTags() throws Exception {
        byte[] bytes = pdf(1);
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            assertEquals(2, pdf.getNumberOfPages());
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("Página 3"), text);
            assertTrue(text.contains("Taller Nogal S.L."), text);

            List<PDFormXObject> first = forms(pdf.getPage(0).getResources());
            assertEquals(2, first.size());
            PDFormXObject turned = first.stream().filter(f -> f.getBBox().getWidth() == 200).findFirst().orElseThrow();
            List<PDFormXObject> second = forms(pdf.getPage(1).getResources());
            assertEquals(turned.getCOSObject(), second.stream().filter(f -> f.getBBox().getWidth() == 200)
                    .findFirst().orElseThrow().getCOSObject(), "The same page on two pages is copied once");

            for (PDFormXObject form : first) {
                String content = new String(form.getContentStream().toByteArray(), StandardCharsets.ISO_8859_1);
                assertFalse(content.contains("MCID"), content);
                assertFalse(content.contains("BDC") || content.contains("BMC"), content);
                assertFalse(form.getCOSObject().containsKey(COSName.STRUCT_PARENTS));
            }
            String page = new String(pdf.getPage(0).getContents().readAllBytes(), StandardCharsets.ISO_8859_1);
            assertTrue(page.contains("/Figure <</MCID 0>> BDC"), page);

            // Page 3 is rotated: its green band is the top one of the box (40, 40, 100 × 200).
            BufferedImage image = new PDFRenderer(pdf).renderImageWithDPI(0, 72);
            int green = image.getRGB(90, 45) & 0xFFFFFF;
            assertTrue((green >> 8 & 0xFF) > (green >> 16 & 0xFF) + 40, Integer.toHexString(green));
            int paper = image.getRGB(90, 150) & 0xFFFFFF;
            assertEquals(0xF4F1EA, paper, Integer.toHexString(paper));
        }
        assertEquals(Map.of(), PdfUa.failedRules(bytes));
        assertEquals(Map.of(), PdfUa.failedRules(pdf(2), 2));
    }

    /**
     * The layers of the source PDF (its optional content) reach the document and still decide what is visible; their
     * configuration, which had no name, is named after the file, and loses its automatic state ({@code AS}), because
     * PDF/UA requires both.
     */
    @Test
    void theLayersOfThePageKeepTheirMarksAndGetAName() throws Exception {
        String doc = DOCUMENT.substring(0, DOCUMENT.indexOf("\t\tPage overlay:")) + "\t\tImage: test/layers.pdf\n"
                + "\t\t\tAlt: Una página con una capa de notas\n";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.create().toPdf(new DocumentReader().read(doc), out);
        byte[] bytes = out.toByteArray();
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            COSDictionary configuration = pdf.getDocumentCatalog().getOCProperties().getCOSObject()
                    .getCOSDictionary(COSName.D);
            assertEquals("layers.pdf", configuration.getString(COSName.NAME));
            assertFalse(configuration.containsKey(COSName.getPDFName("AS")));
            assertTrue(new PDFTextStripper().getText(pdf).contains("Capa de notas"));
            PDFormXObject form = forms(pdf.getPage(0).getResources()).get(0);
            String content = new String(form.getContentStream().toByteArray(), StandardCharsets.ISO_8859_1);
            assertTrue(content.contains("/OC /"), content);
        }
        assertEquals(Map.of(), PdfUa.failedRules(bytes));
        out.reset();
        Converter.builder().pdfUa(2).build().toPdf(new DocumentReader().read(doc), out);
        assertEquals(Map.of(), PdfUa.failedRules(out.toByteArray(), 2));
    }

    /**
     * The marks of a tiling pattern and of the glyphs of a Type 3 font that the page uses are removed as well, except
     * those of optional content.
     */
    @Test
    void theMarksOfPatternsAndType3GlyphsAreRemoved() throws Exception {
        try (PDDocument source = new PDDocument(); PDDocument target = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(200, 100));
            source.addPage(page);
            COSDictionary resources = new COSDictionary();
            page.getCOSObject().setItem(COSName.RESOURCES, resources);

            COSStream pattern = stream(source, "/P <</MCID 0>> BDC 0 0 10 10 re f EMC /OC /L1 BDC 0 0 5 5 re f EMC");
            pattern.setItem(COSName.TYPE, COSName.PATTERN);
            pattern.setInt(COSName.PATTERN_TYPE, 1);
            pattern.setInt(COSName.PAINT_TYPE, 1);
            pattern.setInt(COSName.TILING_TYPE, 1);
            pattern.setItem(COSName.BBOX, new PDRectangle(0, 0, 10, 10).getCOSArray());
            pattern.setInt(COSName.X_STEP, 10);
            pattern.setInt(COSName.Y_STEP, 10);
            COSDictionary layers = new COSDictionary();
            layers.setItem(COSName.getPDFName("L1"), new PDOptionalContentGroup("Notas"));
            COSDictionary patternResources = new COSDictionary();
            patternResources.setItem(COSName.PROPERTIES, layers);
            pattern.setItem(COSName.RESOURCES, patternResources);
            COSDictionary patterns = new COSDictionary();
            patterns.setItem(COSName.getPDFName("P1"), pattern);
            resources.setItem(COSName.PATTERN, patterns);

            COSDictionary font = new COSDictionary();
            font.setItem(COSName.TYPE, COSName.FONT);
            font.setItem(COSName.SUBTYPE, COSName.TYPE3);
            font.setItem(COSName.FONT_BBOX, new PDRectangle(0, 0, 750, 750).getCOSArray());
            COSArray matrix = new COSArray();
            for (float v : new float[] {0.001f, 0, 0, 0.001f, 0, 0}) {
                matrix.add(new COSFloat(v));
            }
            font.setItem(COSName.FONT_MATRIX, matrix);
            COSDictionary procedures = new COSDictionary();
            procedures.setItem(COSName.getPDFName("a"),
                    stream(source, "1000 0 0 0 750 750 d1 /Span <</MCID 1>> BDC 0 0 750 750 re f EMC"));
            font.setItem(COSName.CHAR_PROCS, procedures);
            COSDictionary encoding = new COSDictionary();
            COSArray differences = new COSArray();
            differences.add(COSInteger.get(97));
            differences.add(COSName.getPDFName("a"));
            encoding.setItem(COSName.DIFFERENCES, differences);
            font.setItem(COSName.ENCODING, encoding);
            font.setInt(COSName.FIRST_CHAR, 97);
            font.setInt(COSName.LAST_CHAR, 97);
            COSArray widths = new COSArray();
            widths.add(COSInteger.get(1000));
            font.setItem(COSName.WIDTHS, widths);
            COSDictionary fonts = new COSDictionary();
            fonts.setItem(COSName.getPDFName("T1"), font);
            resources.setItem(COSName.FONT, fonts);

            page.getCOSObject().setItem(COSName.CONTENTS,
                    stream(source, "/Pattern cs /P1 scn 0 0 100 100 re f BT /T1 20 Tf 10 10 Td (a) Tj ET"));

            PDFormXObject form = new PdfPages(target).page(source, 1, "marks.pdf");
            PDResources copied = form.getResources();
            String tiling = text(((PDTilingPattern) copied.getPattern(COSName.getPDFName("P1"))).getContentStream()
                    .toByteArray());
            assertFalse(tiling.contains("MCID") || tiling.contains("/P "), tiling);
            assertTrue(tiling.contains("/OC /L1 BDC"), tiling);
            PDType3Font type3 = (PDType3Font) copied.getFont(COSName.getPDFName("T1"));
            String glyph = text(type3.getCharProc(97).getContentStream().toByteArray());
            assertFalse(glyph.contains("MCID") || glyph.contains("BDC"), glyph);
            assertTrue(glyph.contains("d1"), glyph);
        }
    }

    /**
     * The automatic states of a layer configuration ({@code AS}), which PDF/UA does not allow, are removed; the
     * configuration keeps its name.
     */
    @Test
    void theAutomaticStatesOfTheLayersAreRemoved() throws Exception {
        try (PDDocument source = new PDDocument(); PDDocument target = new PDDocument()) {
            PDOptionalContentProperties properties = new PDOptionalContentProperties();
            PDOptionalContentGroup notes = new PDOptionalContentGroup("Notas");
            properties.addGroup(notes);
            COSDictionary configuration = properties.getCOSObject().getCOSDictionary(COSName.D);
            configuration.setString(COSName.NAME, "Capas");
            COSDictionary event = new COSDictionary();
            event.setName(COSName.getPDFName("Event"), "View");
            COSArray category = new COSArray();
            category.add(COSName.getPDFName("Zoom"));
            event.setItem(COSName.getPDFName("Category"), category);
            COSArray groups = new COSArray();
            groups.add(notes);
            event.setItem(COSName.getPDFName("OCGs"), groups);
            COSArray states = new COSArray();
            states.add(event);
            configuration.setItem(COSName.getPDFName("AS"), states);
            source.getDocumentCatalog().setOCProperties(properties);
            PDPage page = new PDPage(new PDRectangle(200, 100));
            source.addPage(page);
            page.getCOSObject().setItem(COSName.CONTENTS, stream(source, "0 0 10 10 re f"));

            new PdfPages(target).page(source, 1, "layers.pdf");
            COSDictionary copied = target.getDocumentCatalog().getOCProperties().getCOSObject()
                    .getCOSDictionary(COSName.D);
            assertFalse(copied.containsKey(COSName.getPDFName("AS")));
            assertEquals("Capas", copied.getString(COSName.NAME));
        }
    }

    private static COSStream stream(PDDocument doc, String content) throws IOException {
        COSStream stream = doc.getDocument().createCOSStream();
        try (OutputStream out = stream.createOutputStream()) {
            out.write(content.getBytes(StandardCharsets.ISO_8859_1));
        }
        return stream;
    }

    private static String text(byte[] bytes) {
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }
}
