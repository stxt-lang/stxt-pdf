package dev.stxt.boxes.layout.inline;

import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.FontStyle;
import dev.stxt.boxes.model.FontWeight;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.stxt.DocumentReader;
import dev.stxt.boxes.tree.BoxTreeBuilder;
import dev.stxt.boxes.tree.ErrorCode;
import dev.stxt.boxes.tree.InvalidModelException;
import dev.stxt.boxes.tree.TextRuns;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static dev.stxt.boxes.tree.TextRuns.run;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Con las fuentes del repositorio ({@code resources/fonts}): IBM Plex Sans en sus cuatro variantes. */
class PdfMetricsTest {

    private static final FontFace REGULAR = face(FontWeight.NORMAL, FontStyle.NORMAL);
    private static final FontFace BOLD = face(FontWeight.BOLD, FontStyle.NORMAL);

    private final PdfMetrics metrics = new PdfMetrics(new Resources());

    private static FontFace face(FontWeight weight, FontStyle style) {
        return new FontFace("IBM Plex Sans", weight, style);
    }

    private void validate(String... boxLines) {
        String doc = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tTypography:\n\t\tSize: 10\n" + String.join("\n", boxLines);
        metrics.validate(new BoxTreeBuilder().build(new DocumentReader().read(doc)).root());
    }

    @Test
    void widthsFromTheTrueTypeFile() {
        // hmtx de IBM Plex Sans (1000 unidades por em): H + o + l + a = 2073 en la normal, 2160 en negrita.
        assertEquals(20.73, metrics.width("Hola", REGULAR, 10), 1e-9);
        assertEquals(41.46, metrics.width("Hola", REGULAR, 20), 1e-9);
        assertEquals(21.60, metrics.width("Hola", BOLD, 10), 1e-9);
        assertEquals(2.36, metrics.width(" ", REGULAR, 10), 1e-9);
    }

    @Test
    void validDocument() {
        assertDoesNotThrow(() -> validate("\tBlock:", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans",
                "\t\tText: Página {page} de {pages} → ñ", "\t\tText: Negrita cursiva", "\t\t\tTypography:",
                "\t\t\t\tWeight: bold", "\t\t\t\tStyle: italic"));
    }

    @Test
    void unknownFamilyAtTheFontLineWithSuggestion() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> validate("\tBlock:", "\t\tTypography:", "\t\t\tFont: ibm plex sans",
                        "\t\tText: Hola"));
        assertEquals(ErrorCode.FONT_UNKNOWN, e.code());
        assertEquals(10, e.line());
        assertTrue(e.getMessage().contains("Fuente desconocida: ibm plex sans. ¿Querías decir 'IBM Plex Sans'?"),
                e.getMessage());
    }

    @Test
    void missingVariantAtTheTextLine(@TempDir Path dir) throws IOException {
        Path fonts = Files.createDirectories(dir.resolve("fonts"));
        Files.copy(Resources.DEFAULT_ROOT.resolve("fonts/IBM Plex Sans/IBMPlexSans-Regular.ttf"), fonts.resolve("r.ttf"));
        PdfMetrics regularOnly = new PdfMetrics(new Resources(dir));
        String doc = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tTypography:\n\t\tFont: IBM Plex Sans\n\t\tSize: 10\n"
                + "\tBlock:\n\t\tTypography:\n\t\t\tWeight: bold\n\t\tText: Hola";
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> regularOnly.validate(new BoxTreeBuilder().build(new DocumentReader().read(doc)).root()));
        assertEquals(ErrorCode.FONT_VARIANT_MISSING, e.code());
        assertEquals(12, e.line());
        assertTrue(e.getMessage().endsWith("(Font en la línea 7, Weight en la línea 11)"), e.getMessage());
    }

    @Test
    void missingCharacterAtItsLineInsideTheBlock() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> validate("\tBlock:", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans",
                        "\t\tText:", "\t\t\tTypography:", "\t\t\t\tWeight: bold", "\t\t\tContent >>",
                        "\t\t\t\tPrimera línea", "", "\t\t\t\tKanji 漢"));
        assertEquals(ErrorCode.GLYPH_MISSING, e.code());
        assertEquals(17, e.line());
        assertEquals("línea 17: La fuente IBM Plex Sans bold no tiene el carácter '漢' (U+6F22)", e.getMessage());
    }

    @Test
    void missingVariantOfARunAtTheLineOfTheRun(@TempDir Path dir) throws IOException {
        Path fonts = Files.createDirectories(dir.resolve("fonts"));
        Files.copy(Resources.DEFAULT_ROOT.resolve("fonts/IBM Plex Sans/IBMPlexSans-Regular.ttf"), fonts.resolve("r.ttf"));
        PdfMetrics regularOnly = new PdfMetrics(new Resources(dir));
        String doc = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tTypography:\n\t\tFont: IBM Plex Sans\n\t\tSize: 10\n"
                + "\tText:\n\t\tId: T\n\t\tContent >>\n\t\t\tPrimera línea\n\t\t\tsegunda línea";
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> regularOnly.validate(TextRuns.build(new DocumentReader().read(doc),
                        Map.of("T", List.of(run("Primera línea\nsegunda "), run("línea", "bi")))).root()));
        assertEquals(ErrorCode.FONT_VARIANT_MISSING, e.code());
        assertEquals(13, e.line());
        assertTrue(e.getMessage().endsWith("(texto con formato bold italic)"), e.getMessage());
    }

    @Test
    void missingCharacterOfARunAtItsLine() {
        String doc = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tTypography:\n\t\tFont: IBM Plex Sans\n\t\tSize: 10\n"
                + "\tText:\n\t\tId: T\n\t\tContent >>\n\t\t\tPrimera línea\n\t\t\tKanji 漢";
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> metrics.validate(TextRuns.build(new DocumentReader().read(doc),
                        Map.of("T", List.of(run("Primera "), run("línea\nKanji 漢", "i")))).root()));
        assertEquals(ErrorCode.GLYPH_MISSING, e.code());
        assertEquals("línea 13: La fuente IBM Plex Sans italic no tiene el carácter '漢' (U+6F22)", e.getMessage());
    }
}
