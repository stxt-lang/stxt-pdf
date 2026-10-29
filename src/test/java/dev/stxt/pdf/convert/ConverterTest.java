package dev.stxt.pdf.convert;

import dev.stxt.InlineNode;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.PdfBytes;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.resources.ResourceException;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.stxt.DocumentReader;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link Converter}: la configuración de una conversión en un objeto inmutable. */
class ConverterTest {

    private static final DocumentReader READER = new DocumentReader();
    private static final String HEADER = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tMedia:\n\t\tWidth: 200\n\t\tHeight: 200\n";
    private static final String BASE = "Styles (dev.stxt.pdf):\n\tElement: Document\n\t\tTypography:\n\t\t\tFont: IBM Plex Sans\n"
            + "\t\t\tSize: 10\n";

    /** Un documento de una sola caja con texto, que necesita la tipografía de {@link #BASE}. */
    private static InlineNode document() {
        return READER.read(Docs.sectioned(HEADER + "\tText: Hola"));
    }

    private static byte[] pdf(Converter converter, InlineNode document) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        converter.toPdf(document, out);
        return PdfBytes.withoutVolatileFields(out.toByteArray());
    }

    @Test
    void writesAPdf() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.builder().styles(BASE).build().toPdf(document(), out);
        assertTrue(out.size() > 1000);
        assertEquals("%PDF-", new String(out.toByteArray(), 0, 5, StandardCharsets.ISO_8859_1));
    }

    /** Sin la fuente base no hay texto que maquetar: el documento solo no basta. */
    @Test
    void withoutStylesTheDocumentMustCarryItsOwn() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> Converter.create().toPdf(document(), new ByteArrayOutputStream()));
        assertEquals(ErrorCode.TEXT_WITHOUT_FONT, e.errorCode());
    }

    @Test
    void theStylesCanBeGivenReadOrAsTextOrAsAFile(@org.junit.jupiter.api.io.TempDir Path folder) throws IOException {
        Path file = folder.resolve("styles.stxt");
        Files.writeString(file, BASE, StandardCharsets.UTF_8);
        byte[] fromText = pdf(Converter.builder().styles(BASE).build(), document());
        assertArrayEquals(fromText, pdf(Converter.builder().styles(READER.readStyles(BASE)).build(), document()));
        assertArrayEquals(fromText, pdf(Converter.builder().styles(file).build(), document()));
    }

    /** Las fuentes se leen al indexar la carpeta: el PDF no cambia aunque después desaparezca el fichero. */
    @Test
    void theFontsAreReadOnce(@org.junit.jupiter.api.io.TempDir Path folder) throws IOException {
        Path fonts = Files.createDirectories(folder.resolve("fonts"));
        Files.copy(Resources.DEFAULT_ROOT.resolve("fonts/IBM Plex Sans/IBMPlexSans-Regular.ttf"), fonts.resolve("a.ttf"));
        Converter converter = Converter.builder().resources(new Resources(folder)).styles(BASE).build();
        byte[] before = pdf(converter, document());
        Files.delete(fonts.resolve("a.ttf"));
        assertArrayEquals(before, pdf(converter, document()));
    }

    @Test
    void layingOutFirstGivesTheSameBytes() throws IOException {
        Converter converter = Converter.builder().styles(BASE).build();
        InlineNode document = document();
        LaidOutDocument laidOut = converter.layOut(document);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        converter.toPdf(laidOut, out);
        assertArrayEquals(pdf(converter, document), PdfBytes.withoutVolatileFields(out.toByteArray()));
    }

    @Test
    void theSameLayOutGivesAnSvgPerPage() {
        Converter converter = Converter.builder().styles(BASE).build();
        LaidOutDocument laidOut = converter.layOut(document());
        List<String> pages = converter.toSvg(laidOut);
        assertEquals(laidOut.pages().size(), pages.size());
        assertTrue(pages.get(0).contains("<svg"));
    }

    @Test
    void debugDrawsTheBoxOutlines() throws IOException {
        InlineNode document = document();
        assertNotEquals(
                new String(pdf(Converter.builder().styles(BASE).build(), document), StandardCharsets.ISO_8859_1),
                new String(pdf(Converter.builder().styles(BASE).debug(true).build(), document),
                        StandardCharsets.ISO_8859_1));
        Converter debug = Converter.builder().styles(BASE).debug(true).build();
        assertTrue(debug.toSvg(debug.layOut(document)).get(0).length()
                > Converter.builder().styles(BASE).build().toSvg(Converter.builder().styles(BASE).build()
                        .layOut(document)).get(0).length());
    }

    @Test
    void usesTheGivenResourcesFolder(@org.junit.jupiter.api.io.TempDir Path empty) {
        Resources resources = new Resources(empty);
        Converter converter = Converter.builder().resources(resources).styles(BASE).build();
        assertEquals(resources, converter.resources());
        // Una carpeta de recursos sin fuentes no puede maquetar el texto, y lo dice en lugar de sustituir la fuente.
        assertThrows(ResourceException.class, () -> converter.toPdf(document(), new ByteArrayOutputStream()));
    }

    @Test
    void theOptionsAreNeverNull() {
        Converter.Builder builder = Converter.builder();
        assertThrows(NullPointerException.class, () -> builder.resources(null));
        assertThrows(NullPointerException.class, () -> builder.styles((InlineNode) null));
        Converter converter = builder.build();
        assertThrows(NullPointerException.class, () -> converter.layOut(null));
        assertThrows(NullPointerException.class, () -> converter.toPdf((LaidOutDocument) null, new ByteArrayOutputStream()));
        assertThrows(NullPointerException.class, () -> converter.toSvg(null));
    }

    /** Cambiar el constructor después de construir no cambia el conversor ya creado. */
    @Test
    void aBuiltConverterDoesNotChangeWithItsBuilder() throws IOException {
        Converter.Builder builder = Converter.builder().styles(BASE);
        Converter converter = builder.build();
        builder.debug(true);
        assertArrayEquals(pdf(Converter.builder().styles(BASE).build(), document()), pdf(converter, document()));
        assertFalse(converter.toSvg(converter.layOut(document())).isEmpty());
    }
}
