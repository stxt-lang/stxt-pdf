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
 * {@code Ligatures} (reference/common/text-formatting.md): without it, one glyph per character; with it, the text is
 * measured and painted with the glyphs that the GSUB table of the font leaves, and what is measured is what is painted.
 */
class LigaturesTest {

    private static final Resources RESOURCES = new Resources();
    private static final DocumentReader READER = new DocumentReader();
    private static final PdfMetrics METRICS = new PdfMetrics(RESOURCES);

    private static FontFace serif(boolean ligatures) {
        return new FontFace("IBM Plex Serif", FontWeight.NORMAL, FontStyle.NORMAL, ligatures);
    }

    /** A document with a single {@code Text} with the typography of {@code typography} (lines with tabs). */
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

    /** The glyphs (two bytes each, Identity-H) that the PDF writes for the text of the first page. */
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
        // A word without a ligature measures the same; spaces and words are measured separately.
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
        // The character that the font does not have measures what .notdef does; no ligature is possible.
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
        assertEquals(6, glyphs(layOut(document("office"))), "one glyph per character");
        assertTrue(glyphs(layOut(document("office", "Ligatures: true"))) < 6, "ligatures join glyphs");
        assertEquals(6, glyphs(layOut(document("office", "Ligatures: true", "Letter spacing: 1"))));
        assertFalse(width("office", "Ligatures: true") == width("office"));
    }
}
