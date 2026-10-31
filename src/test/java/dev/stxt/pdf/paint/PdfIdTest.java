package dev.stxt.pdf.paint;

import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The file identifier comes from the content (reference/implementation/pdf.md). */
class PdfIdTest {

    private static final Pattern ID = Pattern.compile("/ID \\[<([0-9A-F]{32})> <([0-9A-F]{32})>]");

    private static byte[] pdf(String text, int ua) throws Exception {
        String document = String.join("\n",
                "Document (dev.stxt.pdf):",
                "\tLanguage: es",
                "\tTitle: Prueba",
                "\tPage:",
                "\t\tPaper: A6",
                "\t\tMargin: 30",
                "\tStyles:",
                "\t\tFor element: Document",
                "\t\t\tTypography:",
                "\t\t\t\tFont: IBM Plex Sans",
                "\t\t\t\tSize: 10",
                "\tSection:",
                "\t\tParagraph: " + text,
                "");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.builder().pdfUa(ua).build().toPdf(new DocumentReader().read(document), out);
        return out.toByteArray();
    }

    /**
     * The same document gives the same bytes; the identifier is the start of the SHA-256 of the file with the
     * placeholder, its two halves are equal, and another document has another one. In PDF 1.7 and in PDF 2.0.
     */
    @Test
    void theIdentifierComesFromTheContent() throws Exception {
        for (int ua = 1; ua <= 2; ua++) {
            byte[] first = pdf("Hola", ua);
            assertArrayEquals(first, pdf("Hola", ua));

            String text = new String(first, StandardCharsets.ISO_8859_1);
            Matcher m = ID.matcher(text);
            assertTrue(m.find(), "/ID");
            assertEquals(m.group(1), m.group(2));
            String zeros = "0".repeat(32);
            byte[] placeholder = (text.substring(0, m.start()) + "/ID [<" + zeros + "> <" + zeros + ">]"
                    + text.substring(m.end())).getBytes(StandardCharsets.ISO_8859_1);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(placeholder);
            assertEquals(HexFormat.of().withUpperCase().formatHex(digest, 0, 16), m.group(1));

            Matcher other = ID.matcher(new String(pdf("Adiós", ua), StandardCharsets.ISO_8859_1));
            assertTrue(other.find());
            assertNotEquals(m.group(1), other.group(1));
        }
    }

    /** Without the placeholder, or with it twice, the identifier cannot be placed: an internal failure. */
    @Test
    void thePlaceholderIsThereExactlyOnce() {
        String zeros = "0".repeat(32);
        String placeholder = "/ID [<" + zeros + "> <" + zeros + ">]";
        assertThrows(IllegalStateException.class,
                () -> PdfId.fill("%PDF-1.7 trailer".getBytes(StandardCharsets.ISO_8859_1)));
        assertThrows(IllegalStateException.class,
                () -> PdfId.fill((placeholder + " " + placeholder).getBytes(StandardCharsets.ISO_8859_1)));
    }
}
