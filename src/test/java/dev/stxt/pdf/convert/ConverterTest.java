package dev.stxt.pdf.convert;

import dev.stxt.InlineNode;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.model.Check;
import dev.stxt.pdf.resources.ResourceException;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.stxt.DocumentReader;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link Converter}: the configuration of a conversion in an immutable object. */
class ConverterTest {

    private static final DocumentReader READER = new DocumentReader();
    private static final String HEADER = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 200\n\t\tHeight: 200\n";
    private static final String BASE = "Styles (dev.stxt.pdf):\n\tFor element: Document\n\t\tTypography:\n\t\t\tFont: IBM Plex Sans\n"
            + "\t\t\tSize: 10\n";

    /** A document with a single box of text, which needs the typography of {@link #BASE}. */
    private static InlineNode document() {
        return READER.read(Docs.sectioned(HEADER + "\tText: Hola"));
    }

    private static byte[] pdf(Converter converter, InlineNode document) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        converter.toPdf(document, out);
        return out.toByteArray();
    }

    @Test
    void writesAPdf() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.builder().styles(BASE).build().toPdf(document(), out);
        assertTrue(out.size() > 1000);
        assertEquals("%PDF-", new String(out.toByteArray(), 0, 5, StandardCharsets.ISO_8859_1));
    }

    /** Without the base font there is no text to lay out: the document alone is not enough. */
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

    /** The fonts are read when the folder is indexed: the PDF does not change even if the file disappears later. */
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
        assertArrayEquals(pdf(converter, document), out.toByteArray());
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
        // A resources folder without fonts cannot lay out the text, and says so instead of substituting the font.
        assertThrows(ResourceException.class, () -> converter.toPdf(document(), new ByteArrayOutputStream()));
    }

    @Test
    void theOptionsAreNeverNull() {
        Converter.Builder builder = Converter.builder();
        assertThrows(NullPointerException.class, () -> builder.resources(null));
        assertThrows(NullPointerException.class, () -> builder.styles((InlineNode) null));
        assertThrows(NullPointerException.class, () -> builder.exclude((Check) null));
        Converter converter = builder.build();
        assertThrows(NullPointerException.class, () -> converter.layOut(null));
        assertThrows(NullPointerException.class, () -> converter.toPdf((LaidOutDocument) null, new ByteArrayOutputStream()));
        assertThrows(NullPointerException.class, () -> converter.toSvg(null));
    }

    /** Changing the builder after building does not change the converter already created. */
    @Test
    void aBuiltConverterDoesNotChangeWithItsBuilder() throws IOException {
        Converter.Builder builder = Converter.builder().styles(BASE);
        Converter converter = builder.build();
        builder.debug(true);
        assertArrayEquals(pdf(Converter.builder().styles(BASE).build(), document()), pdf(converter, document()));
        assertFalse(converter.toSvg(converter.layOut(document())).isEmpty());
    }

    /** By default all the checks are done; {@code exclude} removes the ones it is given, and they accumulate. */
    @Test
    void everyCheckIsDoneUnlessExcluded() {
        assertEquals(Set.of(), Converter.create().excluded());
        Converter converter = Converter.builder().exclude(Check.CONTRAST).exclude(Check.PDF_IMAGE_FONTS).build();
        assertEquals(Set.of(Check.CONTRAST, Check.PDF_IMAGE_FONTS), converter.excluded());
        assertThrows(UnsupportedOperationException.class, () -> converter.excluded().add(Check.PDF_IMAGE_ANNOTATIONS));
    }

    /** A font not embedded in the page of a PDF is an error, unless {@link Check#PDF_IMAGE_FONTS} is removed. */
    @Test
    void theFontsOfAPdfImageAreCheckedUnlessExcluded() throws IOException {
        InlineNode document = READER.read(Docs.sectioned(HEADER + "\tImage: test/standard-font.pdf"));
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> Converter.create().layOut(document));
        assertEquals(ErrorCode.IMAGE_FORMAT, e.errorCode());
        assertTrue(e.getMessage().contains("Helvetica not embedded"), e.getMessage());
        assertTrue(e.getMessage().contains("PDF_IMAGE_FONTS"), e.getMessage());
        // Removing the other one is not enough.
        assertThrows(InvalidModelException.class,
                () -> Converter.builder().exclude(Check.PDF_IMAGE_ANNOTATIONS).build().layOut(document));
        assertFalse(pdf(Converter.builder().exclude(Check.PDF_IMAGE_FONTS).build(), document).length == 0);
    }

    /**
     * A form field visible on the page of a PDF is an error, unless {@link Check#PDF_IMAGE_ANNOTATIONS} is
     * removed: then the page is drawn without it.
     */
    @Test
    void theAnnotationsOfAPdfImageAreCheckedUnlessExcluded() throws IOException {
        InlineNode document = READER.read(Docs.sectioned(HEADER + "\tImage: test/form-field.pdf"));
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> Converter.create().layOut(document));
        assertEquals(ErrorCode.IMAGE_FORMAT, e.errorCode());
        assertTrue(e.getMessage().contains("PDF_IMAGE_ANNOTATIONS"), e.getMessage());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.builder().exclude(Check.PDF_IMAGE_ANNOTATIONS).build().toPdf(document, out);
        try (PDDocument pdf = Loader.loadPDF(out.toByteArray())) {
            assertTrue(pdf.getPage(0).getAnnotations().isEmpty());
            assertEquals(null, pdf.getDocumentCatalog().getAcroForm());
        }
    }
}
