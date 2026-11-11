package dev.stxt.pdf.paint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDNumberTreeNode;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.golden.PdfUa;
import dev.stxt.pdf.model.Check;
import dev.stxt.pdf.model.PdfA;
import dev.stxt.pdf.stxt.DocumentReader;

/**
 * The {@code Opacity} of a box in the PDF (reference/common/layers-and-rotation.md and reference/implementation/pdf.md):
 * a transparency group, a form painted once with the opacity of the box, whose marked content hangs from the
 * structure through the stream of the form. veraPDF validates it with PDF/UA-1, PDF/UA-2 and each PDF/A.
 */
class PdfOpacityTest {

    private static final String DOCUMENT = Docs.header(300, 300, 20) + Docs.TYPOGRAPHY + String.join("\n",
            "\tParagraph: Antes del sello.",
            "\t\tId: antes",
            "\tOverlay:",
            "\t\tId: sello",
            "\t\tOffset: 60 auto auto 40",
            "\t\tOpacity: 30%",
            "\t\tRotate: -20",
            "\t\tBox:",
            "\t\t\tBackground: #FFE080",
            "\t\t\tBorder: 2",
            "\t\t\tPadding: 6",
            "\t\tText: [COPIA](https://stxt.dev) sin valor",
            "\t\t\tId: copia",
            "\t\t\tTypography:",
            "\t\t\t\tMarkup: markdown",
            "\t\tImage: test/label.svg",
            "\t\t\tAlt: Una etiqueta",
            "\t\tOverlay:",
            "\t\t\tOffset: 0 0 auto auto",
            "\t\t\tOpacity: 50%",
            "\t\t\tText: Dentro",
            "\tImage: test/square.png",
            "\t\tOpacity: 12.5%",
            "\t\tAlt: Un cuadrado atenuado",
            "\t\tBox:",
            "\t\t\tBackground: #CCCCCC",
            "\t\t\tPadding: 4",
            "\tParagraph: Después del sello.",
            "\tPage overlay:",
            "\t\tOffset: auto 0 0 auto",
            "\t\tOpacity: 40%",
            "\t\tText: Página {page}",
            "");

    /**
     * The PDF of the document, PDF/UA-{@code ua} and, if it is not {@code null}, {@code pdfA}; without the contrast
     * check, which the faint text of the stamp does not pass (reference/common/pdf.md).
     */
    private static byte[] pdf(String document, int ua, PdfA pdfA) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.Builder builder = Converter.builder().pdfUa(ua).exclude(Check.CONTRAST);
        if (pdfA != null) {
            builder.pdfA(pdfA);
        }
        builder.build().toPdf(new DocumentReader().read(Docs.sectioned(document)), out);
        return out.toByteArray();
    }

    private static byte[] pdf() throws Exception {
        return pdf(DOCUMENT, 1, null);
    }

    private static List<Operator> operators(PDPage page) throws Exception {
        List<Operator> operators = new ArrayList<>();
        for (Object token : new PDFStreamParser(page).parse()) {
            if (token instanceof Operator op) {
                operators.add(op);
            }
        }
        return operators;
    }

    private static List<PDFormXObject> groups(PDResources resources) throws Exception {
        List<PDFormXObject> groups = new ArrayList<>();
        for (COSName name : resources.getXObjectNames()) {
            PDXObject x = resources.getXObject(name);
            if (x instanceof PDFormXObject form && form.getGroup() != null) {
                groups.add(form);
            }
        }
        return groups;
    }

    /** Each box with {@code Opacity} is a form with a transparency group, painted once with its opacity. */
    @Test
    void eachBoxWithOpacityIsATransparencyGroup() throws Exception {
        byte[] pdf = pdf();
        Files.write(Files.createDirectories(Path.of("target", "pdf")).resolve("opacity.pdf"), pdf);
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDPage page = doc.getPage(0);
            // The stamp, the image and the page overlay; the Overlay inside the stamp is a group inside its group.
            List<PDFormXObject> groups = groups(page.getResources());
            assertEquals(3, groups.size());
            assertEquals(3, operators(page).stream().filter(op -> op.getName().equals("Do")).count());
            List<Float> opacities = new ArrayList<>();
            for (COSName name : page.getResources().getExtGStateNames()) {
                PDExtendedGraphicsState state = page.getResources().getExtGState(name);
                opacities.add(state.getNonStrokingAlphaConstant());
                assertEquals(state.getNonStrokingAlphaConstant(), state.getStrokingAlphaConstant());
            }
            assertEquals(List.of(0.125f, 0.3f, 0.4f), opacities.stream().sorted().toList());
            int nested = 0;
            for (PDFormXObject group : groups) {
                assertEquals(COSName.TRANSPARENCY, group.getGroup().getCOSObject().getCOSName(COSName.S));
                assertTrue(group.getGroup().isIsolated());
                assertEquals(page.getMediaBox().getWidth(), group.getBBox().getWidth());
                assertEquals(page.getMediaBox().getHeight(), group.getBBox().getHeight());
                nested += groups(group.getResources()).size();
            }
            assertEquals(1, nested);
        }
    }

    /**
     * The marked content inside a group belongs to the stream of its form: its reference names it ({@code Stm}), its
     * {@code MCID} start at 0 in each form, and the form has its own key in the parent tree; a group without content
     * of the structure (that of a {@code Page overlay}, an artifact) has none.
     */
    @Test
    void theMarkedContentOfAGroupHangsFromItsForm() throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf())) {
            PDPage page = doc.getPage(0);
            PDNumberTreeNode parentTree = doc.getDocumentCatalog().getStructureTreeRoot().getParentTree();
            Map<Integer, COSBase> parents = new java.util.TreeMap<>();
            COSArray nums = parentTree.getCOSObject().getCOSArray(COSName.NUMS);
            for (int i = 0; i < nums.size(); i += 2) {
                parents.put(nums.getInt(i), nums.getObject(i + 1));
            }
            int withKey = 0;
            for (PDFormXObject group : groups(page.getResources())) {
                int key = group.getStructParents();
                if (key < 0) {
                    continue;
                }
                withKey++;
                COSArray elements = (COSArray) parents.get(key);
                assertNotNull(elements, "key " + key);
                for (int mcid = 0; mcid < elements.size(); mcid++) {
                    COSDictionary element = (COSDictionary) elements.getObject(mcid);
                    assertTrue(refersTo(element, group, mcid), "MCID " + mcid + " of the form with key " + key);
                }
            }
            // The stamp (its text, its link and its image) and the image; not the page overlay.
            assertEquals(2, withKey);
            // The page keeps its own: the paragraphs outside the groups, without Stm.
            COSArray ofPage = (COSArray) parents.get(page.getStructParents());
            assertEquals(2, ofPage.size());
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("COPIA sin valor") && text.contains("Dentro") && text.contains("Antes"), text);
        }
    }

    /** Whether the structure element has a marked content reference to {@code mcid} of the stream of {@code form}. */
    private static boolean refersTo(COSDictionary element, PDFormXObject form, int mcid) {
        COSBase kids = element.getDictionaryObject(COSName.K);
        List<COSBase> list = new ArrayList<>();
        if (kids instanceof COSArray array) {
            array.forEach(k -> list.add(k instanceof org.apache.pdfbox.cos.COSObject o ? o.getObject() : k));
        } else {
            list.add(kids);
        }
        for (COSBase kid : list) {
            if (kid instanceof COSDictionary mcr && COSName.getPDFName("MCR").equals(mcr.getCOSName(COSName.TYPE))
                    && mcr.getInt(COSName.MCID) == mcid
                    && mcr.getDictionaryObject(COSName.getPDFName("Stm")) == form.getCOSObject()) {
                return true;
            }
        }
        return false;
    }

    @Test
    void itIsPdfUa1AndPdfUa2() throws Exception {
        assertEquals(Map.of(), PdfUa.failedRules(pdf(), 1));
        assertEquals(Map.of(), PdfUa.failedRules(pdf(DOCUMENT, 2, null), 2));
    }

    @ParameterizedTest
    @EnumSource(PdfA.class)
    void itIsPdfA(PdfA pdfA) throws Exception {
        byte[] pdf = pdf(DOCUMENT, 1, pdfA);
        assertEquals(Map.of(), PdfUa.failedRules(pdf, PdfUa.flavour(pdfA)), pdfA.toString());
        assertEquals(Map.of(), PdfUa.failedRules(pdf, 1), pdfA + " and PDF/UA-1");
    }

    /** Without {@code Opacity} there is no group: the page is written as before. */
    @Test
    void withoutOpacityThereAreNoGroups() throws Exception {
        String opaque = DOCUMENT.replaceAll("\t+Opacity: [0-9.]+%\n", "");
        assertFalse(opaque.contains("Opacity"));
        try (PDDocument doc = Loader.loadPDF(pdf(opaque, 1, null))) {
            assertEquals(List.of(), groups(doc.getPage(0).getResources()));
            assertFalse(doc.getPage(0).getResources().getExtGStateNames().iterator().hasNext());
        }
    }
}
