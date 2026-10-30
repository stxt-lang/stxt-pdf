package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.model.Limits;
import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.Engine;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.paint.PdfDocumentWriter;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.stxt.DocumentReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code Ligatures} (referencia/comun/formato-del-texto.md): sin ella, un glifo por carácter; con ella, el texto se
 * mide y se pinta con los glifos que deja la tabla GSUB de la fuente, y lo medido es lo pintado.
 */
class LigaturesTest {

    private static final Resources RESOURCES = new Resources();
    private static final DocumentReader READER = new DocumentReader();
    private static final PdfMetrics METRICS = new PdfMetrics(RESOURCES);

    private static FontFace serif(boolean ligatures) {
        return new FontFace("IBM Plex Serif", FontWeight.NORMAL, FontStyle.NORMAL, ligatures);
    }

    /** Un documento de un solo {@code Text} con la tipografía de {@code typography} (líneas con tabuladores) en el documento. */
    private static String document(String text, String... typography) {
        StringBuilder t = new StringBuilder();
        for (String line : typography) {
            t.append("\t\t\t\t").append(line).append('\n');
        }
        return "Document (dev.stxt.pdf):\n\tPage:\n\t\tPaper: A4\n\t\tMargin: 40\n\tTitle: T\n\tLanguage: es\n"
                + "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Serif\n\t\t\t\tSize: 20\n" + t
                + "\tSection:\n\t\tText: " + text + "\n";
    }

    private static LaidOutDocument layOut(String document) {
        return new Engine(METRICS, RESOURCES).layOutDocument(READER.read(document));
    }

    private static TextLine firstLine(Fragment f) {
        if (!f.lines().isEmpty()) {
            return f.lines().get(0);
        }
        for (Fragment child : f.children()) {
            TextLine line = firstLine(child);
            if (line != null) {
                return line;
            }
        }
        return null;
    }

    private static double width(String text, String... typography) {
        return firstLine(layOut(document(text, typography)).pages().get(0).content()).width();
    }

    /** Los glifos (dos bytes cada uno, Identity-H) que el PDF escribe para el texto de la primera página. */
    private static int glyphs(LaidOutDocument document) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfDocumentWriter.write(document, RESOURCES, Limits.DEFAULT, out, false);
        int glyphs = 0;
        try (PDDocument pdf = Loader.loadPDF(out.toByteArray())) {
            List<Object> tokens = new PDFStreamParser(pdf.getPage(0)).parse();
            for (Object token : tokens) {
                if (token instanceof COSString s) {
                    glyphs += s.getBytes().length / 2;
                } else if (token instanceof COSArray a) {
                    for (COSBase item : a) {
                        if (item instanceof COSString s) {
                            glyphs += s.getBytes().length / 2;
                        }
                    }
                }
            }
        }
        return glyphs;
    }

    @Test
    void aFontFaceIsTheSameFileWithOrWithoutLigatures() {
        FontFace with = serif(true);
        assertNotEquals(with, serif(false));
        assertEquals(serif(false), with.base());
        assertEquals(RESOURCES.fonts().find(with), RESOURCES.fonts().find(serif(false)));
        assertEquals("IBM Plex Serif normal", with.toString());
    }

    @Test
    void ligaturesChangeTheWidthOfTheWordsThatHaveThem() {
        double plain = METRICS.width("office", serif(false), 20);
        double ligated = METRICS.width("office", serif(true), 20);
        assertTrue(ligated < plain, ligated + " < " + plain);
        // Una palabra sin ligadura mide lo mismo; los espacios y las palabras se miden por separado.
        assertEquals(METRICS.width("Hello world", serif(false), 20), METRICS.width("Hello world", serif(true), 20), 1e-9);
        assertEquals(METRICS.width("office", serif(true), 20) + METRICS.width(" ", serif(true), 20)
                + METRICS.width("office", serif(true), 20), METRICS.width("office office", serif(true), 20), 1e-9);
    }

    @Test
    void monoHasNoLigatures() {
        FontFace plain = new FontFace("IBM Plex Mono", FontWeight.NORMAL, FontStyle.NORMAL);
        assertEquals(METRICS.width("office -> fi", plain, 10), METRICS.width("office -> fi", plain.withLigatures(true), 10), 1e-9);
    }

    @Test
    void aWordWithoutGlyphIsMeasuredAsAlways() {
        // El carácter que la fuente no tiene mide lo que .notdef; no hay ligadura posible.
        String text = "fi";
        assertEquals(METRICS.width(text, serif(false), 20), METRICS.width(text, serif(true), 20), 1e-9);
    }

    @Test
    void theDefaultIsNoLigaturesAndTheLayoutUsesTheOptionInTheDocument() {
        double plain = width("office");
        assertEquals(plain, width("office", "Ligatures: false"), 1e-9);
        assertTrue(width("office", "Ligatures: true") < plain);
    }

    @Test
    void letterSpacingTurnsLigaturesOff() {
        double spaced = width("office", "Letter spacing: 1");
        assertEquals(spaced, width("office", "Letter spacing: 1", "Ligatures: true"), 1e-9);
    }

    @Test
    void thePdfWritesWhatWasMeasured() throws IOException {
        assertEquals(6, glyphs(layOut(document("office"))), "un glifo por carácter");
        assertTrue(glyphs(layOut(document("office", "Ligatures: true"))) < 6, "las ligaduras juntan glifos");
        assertEquals(6, glyphs(layOut(document("office", "Ligatures: true", "Letter spacing: 1"))));
        assertFalse(width("office", "Ligatures: true") == width("office"));
    }
}
