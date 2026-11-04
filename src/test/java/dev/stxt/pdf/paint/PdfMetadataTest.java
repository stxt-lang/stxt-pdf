package dev.stxt.pdf.paint;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.Pdfs;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the PDF says about itself (reference/implementation/pdf.md): the title, in the document information and in the
 * XMP packet, the language of the catalog and the PDF/UA declaration. The PDF/A declarations and the output intent are
 * in {@link PdfATest}.
 */
class PdfMetadataTest {

    @Test
    void titleAndLanguage() throws Exception {
        // The header is written by hand: the title and the language are what is tested.
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of("Document (dev.stxt.pdf):\n\tPage:\n\t\tWidth: 300\n\t\tHeight: 200\n"
                + "\tTitle: Informe <trimestral> & resumen\n\tLanguage: es-ES\n" + Docs.TYPOGRAPHY + "\tText: Hola\n"))) {
            PDDocumentCatalog catalog = pdf.getDocumentCatalog();
            assertEquals("es-ES", catalog.getLanguage());
            assertEquals("Informe <trimestral> & resumen", pdf.getDocumentInformation().getTitle());
            assertTrue(catalog.getViewerPreferences().displayDocTitle());
            String xmp = new String(catalog.getMetadata().toByteArray(), StandardCharsets.UTF_8);
            assertTrue(xmp.contains("<rdf:li xml:lang=\"x-default\">Informe &lt;trimestral&gt; &amp; resumen</rdf:li>"), xmp);
            // With a title, the PDF declares that it is PDF/UA-1.
            assertTrue(xmp.contains("<pdfuaid:part>1</pdfuaid:part>"), xmp);
        }
    }
}
