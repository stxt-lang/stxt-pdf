package dev.stxt.boxes.paint;

import dev.stxt.InlineNode;
import dev.stxt.boxes.stxt.DocumentReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Escribe los PDF en {@code target/pdf/} para revisarlos a mano. */
class PdfCanvasTest {

    private static final Path PDF_DIR = Path.of("target", "pdf");

    private final DocumentReader reader = new DocumentReader();

    private static byte[] render(InlineNode document, String name) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfCanvas.write(document, out);
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
        InlineNode doc = reader.read("Document (dev.stxt.boxes):\n\tMedia:\n\t\tType: paged\n\t\tWidth: 300\n"
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
    void continuousIsOnePageAsTallAsTheContent() throws Exception {
        InlineNode doc = reader.read("Document (dev.stxt.boxes):\n\tMedia:\n\t\tType: continuous\n\t\tWidth: 300\n"
                + "\t\tMargin: 20\n\tBlock:\n\t\tBox:\n\t\t\tHeight: 500\n\t\t\tBackground: #F1EFE8\n");
        try (PDDocument pdf = Loader.loadPDF(render(doc, "continuous"))) {
            assertEquals(1, pdf.getNumberOfPages());
            assertEquals(540, pdf.getPage(0).getMediaBox().getHeight(), 1e-3);
        }
    }

    @Test
    void colors() {
        assertArrayEquals(new float[]{1, 0, 0}, PdfCanvas.rgb("#F00"));
        assertArrayEquals(new float[]{0xA3 / 255f, 0x2D / 255f, 0x2D / 255f}, PdfCanvas.rgb("#A32D2D"));
    }
}
