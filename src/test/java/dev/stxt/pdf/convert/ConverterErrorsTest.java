package dev.stxt.pdf.convert;

import dev.stxt.InlineNode;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.model.InternalException;
import dev.stxt.pdf.model.NumberFormat;
import dev.stxt.pdf.model.StxtPdfException;
import dev.stxt.pdf.model.StxtPdfException.Kind;
import dev.stxt.pdf.resources.ResourceException;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.stxt.DocumentReader;
import dev.stxt.pdf.tree.DocumentSyntaxException;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import dev.stxt.exceptions.LimitException;
import dev.stxt.exceptions.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Los errores de una conversión (reference/common/errors.md): todos son un {@link StxtPdfException} y su
 * {@link StxtPdfException#kind() kind} dice de quién es la culpa. Una sola {@code catch} basta en un servicio.
 */
class ConverterErrorsTest {

    private static final DocumentReader READER = new DocumentReader();
    private static final String HEADER = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 200\n\t\tHeight: 200\n";
    private static final String BASE = "Styles (dev.stxt.pdf):\n\tFor element: Document\n\t\tTypography:\n\t\t\tFont: IBM Plex Sans\n"
            + "\t\t\tSize: 10\n";

    private static Converter converter() {
        return Converter.builder().styles(BASE).build();
    }

    private static InlineNode document(String... lines) {
        return READER.read(Docs.sectioned(HEADER + String.join("\n", lines) + "\n"));
    }

    /** Lo que una {@code catch (StxtPdfException)} de un servicio vería de {@code conversion}. */
    private static StxtPdfException failure(org.junit.jupiter.api.function.Executable conversion) {
        return assertThrows(StxtPdfException.class, conversion);
    }

    @Test
    void anInvalidDocumentIsTheDocumentsFaultAndSaysWhere() {
        InlineNode document = document("\tText: Hola"); // sin la fuente de BASE
        StxtPdfException e = failure(() -> Converter.create().toPdf(document, new ByteArrayOutputStream()));
        assertEquals(Kind.INVALID_DOCUMENT, e.kind());
        assertEquals("TEXT_WITHOUT_FONT", e.code());
        assertTrue(e.line() > 0);
        assertTrue(e.getMessage().startsWith("línea " + e.line() + ": "), e.getMessage());
        assertEquals(ErrorCode.TEXT_WITHOUT_FONT, assertInstanceOf(InvalidModelException.class, e).errorCode());
    }

    @Test
    void aSyntaxOrSchemaErrorOfTheLibraryKeepsItsCodeLineAndCause() {
        StxtPdfException e = failure(() -> READER.read(Docs.sectioned(HEADER + "\tText: a\n\t\tTypography:\n\t\t\tAlign: middle\n")));
        DocumentSyntaxException syntax = assertInstanceOf(DocumentSyntaxException.class, e);
        assertEquals(Kind.INVALID_DOCUMENT, e.kind());
        assertEquals("INVALID_VALUE", e.code());
        assertEquals(10, e.line());
        ParseException original = assertInstanceOf(ParseException.class, syntax.getCause());
        assertEquals(original.getCode(), e.code());
        assertEquals(original.getLine(), e.line());
    }

    /** Un documento anidado más de lo que admite la librería no es inválido: es demasiado. */
    @Test
    void aDocumentTheLibraryRefusesAsTooBigIsALimit() {
        StringBuilder deep = new StringBuilder("Document (dev.stxt.pdf):\n");
        for (int level = 1; level <= 150; level++) {
            deep.append("\t".repeat(level)).append("Block:\n");
        }
        StxtPdfException e = failure(() -> READER.read(deep.toString()));
        assertEquals(Kind.LIMIT, e.kind());
        assertInstanceOf(LimitException.class, e.getCause());
    }

    @Test
    void aMissingDocumentFileIsAResourceProblem(@TempDir Path folder) {
        StxtPdfException e = failure(() -> READER.read(folder.resolve("no-existe.stxt")));
        assertEquals(Kind.RESOURCE, e.kind());
        assertEquals(StxtPdfException.NO_LINE, e.line());
    }

    @Test
    void anUnusableResourcesFolderIsAResourceProblemWithoutLine(@TempDir Path empty) {
        Converter converter = Converter.builder().resources(new Resources(empty)).styles(BASE).build();
        InlineNode document = document("\tText: Hola");
        StxtPdfException e = failure(() -> converter.toPdf(document, new ByteArrayOutputStream()));
        assertInstanceOf(ResourceException.class, e);
        assertEquals(Kind.RESOURCE, e.kind());
        assertEquals("RESOURCE", e.code());
        assertEquals(StxtPdfException.NO_LINE, e.line());
    }

    @Test
    void tooManyPagesIsALimitWithoutLine() {
        InlineNode document = document("\tBlock:", "\t\tBox:", "\t\t\tHeight: 10000000");
        StxtPdfException e = failure(() -> converter().toPdf(document, new ByteArrayOutputStream()));
        assertEquals(Kind.LIMIT, e.kind());
        assertEquals("PAGE_LIMIT", e.code());
        assertEquals(StxtPdfException.NO_LINE, e.line());
        assertTrue(e.getMessage().startsWith("El documento tiene más de"), e.getMessage());
    }

    /** Un error de ejecución que no es del motor sale como {@code INTERNAL}, con el original como causa. */
    @Test
    void anUnexpectedFailureIsInternalAndKeepsItsCause() {
        Converter converter = converter();
        LaidOutDocument good = converter.layOut(document("\tText: Hola"));
        LaidOutDocument broken = new LaidOutDocument(good.pageSetup(), good.metadata(), good.root(),
                List.of(new Page(1, null, List.of(), null, 0, 1, NumberFormat.DECIMAL, false)));
        StxtPdfException e = failure(() -> converter.toSvg(broken));
        assertInstanceOf(InternalException.class, e);
        assertEquals(Kind.INTERNAL, e.kind());
        assertEquals("INTERNAL", e.code());
        assertInstanceOf(RuntimeException.class, e.getCause());
    }

    /** Un destino que falla no es un error del documento ni del motor: es la {@link IOException} de quien escribe. */
    @Test
    void aFailingTargetIsAnIOExceptionNotAnEngineError() {
        InlineNode document = document("\tText: Hola");
        IOException broken = new IOException("disco lleno");
        OutputStream target = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw broken;
            }
        };
        assertSame(broken, assertThrows(IOException.class, () -> converter().toPdf(document, target)));
    }

    @Test
    void anErrorWithoutLineHasNoLinePrefixAndTheStylesDocumentKeepsItsLine() {
        InvalidModelException withoutLine = new InvalidModelException(StxtPdfException.NO_LINE, ErrorCode.PAGE_LIMIT, "demasiado");
        assertEquals("demasiado", withoutLine.getMessage());
        assertFalse(withoutLine.inStyles());
        InvalidModelException inStyles = new InvalidModelException(8, ErrorCode.VALUE_SYNTAX, "mal").inStylesDocument();
        assertEquals("línea 8 de los estilos: mal", inStyles.getMessage());
        assertEquals(8, inStyles.line());
        assertTrue(inStyles.inStyles());
        assertEquals("VALUE_SYNTAX", inStyles.code());
        assertEquals(Kind.INVALID_DOCUMENT, inStyles.kind());
        assertEquals(Kind.LIMIT, ErrorCode.PAGE_LIMIT.kind());
    }
}
