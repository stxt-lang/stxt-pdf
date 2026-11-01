package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.stxt.DocumentReader;
import dev.stxt.pdf.tree.BoxTreeBuilder;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import dev.stxt.pdf.tree.TextRuns;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static dev.stxt.pdf.tree.TextRuns.run;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** With the fonts of the repository ({@code resources/fonts}): IBM Plex Sans in its four variants. */
class PdfMetricsTest {

    private static final FontFace REGULAR = face(FontWeight.NORMAL, FontStyle.NORMAL);
    private static final FontFace BOLD = face(FontWeight.BOLD, FontStyle.NORMAL);

    private final PdfMetrics metrics = new PdfMetrics(new Resources());

    private static FontFace face(FontWeight weight, FontStyle style) {
        return new FontFace("IBM Plex Sans", weight, style);
    }

    private void validate(String... boxLines) {
        String doc = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tSize: 10\n" + String.join("\n", boxLines);
        metrics.validate(new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(doc))).root());
    }

    @Test
    void widthsFromTheTrueTypeFile() {
        // hmtx of IBM Plex Sans (1000 units per em): H + o + l + a = 2073 in regular, 2160 in bold.
        assertEquals(20.73, metrics.width("Hola", REGULAR, 10), 1e-9);
        assertEquals(41.46, metrics.width("Hola", REGULAR, 20), 1e-9);
        assertEquals(21.60, metrics.width("Hola", BOLD, 10), 1e-9);
        assertEquals(2.36, metrics.width(" ", REGULAR, 10), 1e-9);
    }

    @Test
    void validDocument() {
        assertDoesNotThrow(() -> validate("\tBlock:", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans",
                "\t\tText: Página {page} de {pages} → ñ", "\t\tText: Negrita cursiva", "\t\t\tTypography:",
                "\t\t\t\tBold: true", "\t\t\t\tItalic: true"));
    }

    @Test
    void unknownFamilyAtTheFontLineWithSuggestion() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> validate("\tBlock:", "\t\tTypography:", "\t\t\tFont: ibm plex sans",
                        "\t\tText: Hola"));
        assertEquals(ErrorCode.FONT_UNKNOWN, e.errorCode());
        assertEquals(14, e.line());
        assertTrue(e.getMessage().contains("Unknown font: ibm plex sans. The closest name is 'IBM Plex Sans'."),
                e.getMessage());
    }

    @Test
    void missingVariantAtTheTextLine(@TempDir Path dir) throws IOException {
        Path fonts = Files.createDirectories(dir.resolve("fonts"));
        Files.copy(Resources.DEFAULT_ROOT.resolve("fonts/IBM Plex Sans/IBMPlexSans-Regular.ttf"), fonts.resolve("r.ttf"));
        PdfMetrics regularOnly = new PdfMetrics(new Resources(dir));
        String doc = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + "\tBlock:\n\t\tTypography:\n\t\t\tBold: true\n\t\tText: Hola";
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> regularOnly.validate(new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(doc))).root()));
        assertEquals(ErrorCode.FONT_VARIANT_MISSING, e.errorCode());
        assertEquals(16, e.line());
        assertTrue(e.getMessage().endsWith("(Font on line 10, Bold or Weight on line 15)"), e.getMessage());
    }

    @Test
    void missingVariantNamesTheStylesDocumentOfEachDeclaration(@TempDir Path dir) throws IOException {
        Path fonts = Files.createDirectories(dir.resolve("fonts"));
        Files.copy(Resources.DEFAULT_ROOT.resolve("fonts/IBM Plex Sans/IBMPlexSans-Regular.ttf"), fonts.resolve("r.ttf"));
        PdfMetrics regularOnly = new PdfMetrics(new Resources(dir));
        String styles = "Styles (dev.stxt.pdf):\n\tFor element: Document\n\t\tTypography:\n\t\t\tFont: IBM Plex Sans\n"
                + "\t\t\tSize: 10\n";
        String doc = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tBlock:\n\t\tTypography:\n\t\t\tBold: true\n\t\tText: Hola";
        DocumentReader reader = new DocumentReader();
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> regularOnly.validate(
                new BoxTreeBuilder().build(reader.read(Docs.sectioned(doc)), reader.readStyles(styles)).root()));
        assertEquals(ErrorCode.FONT_VARIANT_MISSING, e.errorCode());
        // The error belongs to the text, in the document; the family is declared in the styles.
        assertFalse(e.inStyles());
        assertTrue(e.getMessage().endsWith("(Font on line 4 of the styles, Bold or Weight on line 10)"),
                e.getMessage());
    }

    @Test
    void missingCharacterAtItsLineInsideTheBlock() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> validate("\tBlock:", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans",
                        "\t\tText:", "\t\t\tTypography:", "\t\t\t\tBold: true", "\t\t\tContent >>",
                        "\t\t\t\tPrimera línea", "\t\t\t\tKanji 漢"));
        assertEquals(ErrorCode.GLYPH_MISSING, e.errorCode());
        assertEquals(20, e.line());
        assertEquals("line 20: The font IBM Plex Sans bold has no character '漢' (U+6F22)", e.getMessage());
    }

    @Test
    void missingVariantOfARunAtTheLineOfTheRun(@TempDir Path dir) throws IOException {
        Path fonts = Files.createDirectories(dir.resolve("fonts"));
        Files.copy(Resources.DEFAULT_ROOT.resolve("fonts/IBM Plex Sans/IBMPlexSans-Regular.ttf"), fonts.resolve("r.ttf"));
        PdfMetrics regularOnly = new PdfMetrics(new Resources(dir));
        String doc = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + "\tText:\n\t\tId: T\n\t\tContent >>\n\t\t\tPrimera línea\n\t\t\tsegunda línea";
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> regularOnly.validate(TextRuns.build(new DocumentReader().read(Docs.sectioned(doc)),
                        Map.of("T", List.of(run("Primera línea\nsegunda "), run("línea", "bi")))).root()));
        assertEquals(ErrorCode.FONT_VARIANT_MISSING, e.errorCode());
        assertEquals(17, e.line());
        assertTrue(e.getMessage().endsWith("(text with bold italic formatting)"), e.getMessage());
    }

    @Test
    void missingCharacterOfARunAtItsLine() {
        String doc = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + "\tText:\n\t\tId: T\n\t\tContent >>\n\t\t\tPrimera línea\n\t\t\tKanji 漢";
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> metrics.validate(TextRuns.build(new DocumentReader().read(Docs.sectioned(doc)),
                        Map.of("T", List.of(run("Primera "), run("línea\nKanji 漢", "i")))).root()));
        assertEquals(ErrorCode.GLYPH_MISSING, e.errorCode());
        assertEquals("line 17: The font IBM Plex Sans italic has no character '漢' (U+6F22)", e.getMessage());
    }

    @Test
    void theCodeFontOfAMarkdownIsCheckedAndMeasuredWithItsOwnFile() {
        assertDoesNotThrow(() -> validate("\tMarkdown:", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans", "\t\t\tCode font: IBM Plex Mono", "\t\tContent >>",
                "\t\t\tFont Plex Sans y `mono`.", "", "\t\t\t```", "\t\t\tint x;", "\t\t\t```"));
        // hmtx of IBM Plex Mono: 600 units per character.
        assertEquals(12, metrics.width("ab", new FontFace("IBM Plex Mono", FontWeight.NORMAL, FontStyle.NORMAL), 10),
                1e-9);
    }

    @Test
    void anUnknownCodeFontIsReportedWhereItIsDeclared() {
        InvalidModelException block = assertThrows(InvalidModelException.class, () -> validate("\tMarkdown:",
                "\t\tTypography:", "\t\t\tFont: IBM Plex Sans", "\t\t\tCode font: Courier", "\t\tContent >>", "\t\t\t```", "\t\t\tint x;", "\t\t\t```"));
        assertEquals(ErrorCode.FONT_UNKNOWN, block.errorCode());
        assertEquals(15, block.line());
        InvalidModelException inline = assertThrows(InvalidModelException.class, () -> validate("\tMarkdown:",
                "\t\tTypography:", "\t\t\tFont: IBM Plex Sans", "\t\t\tCode font: Courier", "\t\tContent >>", "\t\t\tUsa `mvn`."));
        assertEquals(ErrorCode.FONT_UNKNOWN, inline.errorCode());
        assertEquals(15, inline.line());
        assertTrue(inline.getMessage().contains("Code font"), inline.getMessage());
    }
}
