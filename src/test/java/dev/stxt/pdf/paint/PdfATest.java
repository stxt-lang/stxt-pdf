package dev.stxt.pdf.paint;

import dev.stxt.InlineNode;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.golden.PdfUa;
import dev.stxt.pdf.model.Attachment;
import dev.stxt.pdf.model.Attachment.Relationship;
import dev.stxt.pdf.model.Attachments;
import dev.stxt.pdf.model.PdfA;
import dev.stxt.pdf.model.StxtPdfException;
import dev.stxt.pdf.stxt.DocumentReader;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The PDF with PDF/A (reference/implementation/pdf.md): the declaration in the XMP packet, the output intent with the
 * sRGB profile, and, with PDF/A-3, the embedded files (Factur-X) and the extra XMP. veraPDF validates each part and
 * level with its profile, on a document with what a PDF/A restricts: transparency (an opacity and a gradient with
 * opacities), links, a table of contents, bitmaps, an SVG with text, a page of a PDF and a barcode.
 */
class PdfATest {

    private static final String DOCUMENT = String.join("\n",
            "Document (dev.stxt.pdf):",
            "\tLanguage: es",
            "\tTitle: Prueba & PDF/A",
            "\tPage:",
            "\t\tWidth: 400",
            "\t\tHeight: 500",
            "\t\tMargin: 20",
            "\tStyles:",
            "\t\tFor element: Document",
            "\t\t\tTypography:",
            "\t\t\t\tFont: IBM Plex Sans",
            "\t\t\t\tSize: 10",
            "\tSection:",
            "\t\tTable of contents:",
            "\t\tHeading 1:",
            "\t\t\tId: uno",
            "\t\t\tText: Uno con eñe",
            "\t\tBlock:",
            "\t\t\tBox:",
            "\t\t\t\tBackground: linear(180, #FFE08000, #FFE08080)",
            "\t\t\t\tPadding: 10",
            "\t\t\tText: Sobre un degradado con opacidad",
            "\t\tText: Medio transparente",
            "\t\t\tTypography:",
            "\t\t\t\tColor: #000000A0",
            "\t\tParagraph: Un [enlace](https://stxt.dev) y otro [interno](#dos).",
            "\t\tList:",
            "\t\t\tMarker: dash",
            "\t\t\tBox:",
            "\t\t\t\tPadding: 0 0 0 16",
            "\t\t\tList item:",
            "\t\t\t\tText: Un ítem",
            "\t\tImage: test/square.png",
            "\t\t\tAlt: Un cuadrado",
            "\t\tImage: test/tall.jpg",
            "\t\t\tBox:",
            "\t\t\t\tHeight: 60",
            "\t\tImage: test/label.svg",
            "\t\t\tAlt: Una etiqueta",
            "\t\tImage: test/pages.pdf",
            "\t\t\tPdf page: 1",
            "\t\t\tAlt: La página 1",
            "\t\t\tBox:",
            "\t\t\t\tHeight: 80",
            "\t\tBarcode: https://stxt.dev",
            "\t\t\tAlt: QR con la web",
            "\t\t\tBox:",
            "\t\t\t\tWidth: 60",
            "\t\tHeading 1:",
            "\t\t\tId: dos",
            "\t\t\tText: Dos",
            "\t\tPage overlay:",
            "\t\t\tOffset: auto 0 0 auto",
            "\t\t\tText: Página {page}",
            "");

    private static final String FACTUR_X_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<rsm:CrossIndustryInvoice xmlns:rsm=\"urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100\"/>\n";

    /** The XMP of Factur-X 1.0 (its specification, annex), with the extension schema that describes its properties. */
    private static final String FACTUR_X_XMP = String.join("\n",
            "<rdf:Description rdf:about=\"\" xmlns:pdfaExtension=\"http://www.aiim.org/pdfa/ns/extension/\"",
            "    xmlns:pdfaSchema=\"http://www.aiim.org/pdfa/ns/schema#\"",
            "    xmlns:pdfaProperty=\"http://www.aiim.org/pdfa/ns/property#\">",
            "  <pdfaExtension:schemas>",
            "    <rdf:Bag>",
            "      <rdf:li rdf:parseType=\"Resource\">",
            "        <pdfaSchema:schema>Factur-X PDFA Extension Schema</pdfaSchema:schema>",
            "        <pdfaSchema:namespaceURI>urn:factur-x:pdfa:CrossIndustryDocument:invoice:1p0#</pdfaSchema:namespaceURI>",
            "        <pdfaSchema:prefix>fx</pdfaSchema:prefix>",
            "        <pdfaSchema:property>",
            "          <rdf:Seq>",
            "            <rdf:li rdf:parseType=\"Resource\">",
            "              <pdfaProperty:name>DocumentFileName</pdfaProperty:name>",
            "              <pdfaProperty:valueType>Text</pdfaProperty:valueType>",
            "              <pdfaProperty:category>external</pdfaProperty:category>",
            "              <pdfaProperty:description>name of the embedded XML invoice file</pdfaProperty:description>",
            "            </rdf:li>",
            "            <rdf:li rdf:parseType=\"Resource\">",
            "              <pdfaProperty:name>DocumentType</pdfaProperty:name>",
            "              <pdfaProperty:valueType>Text</pdfaProperty:valueType>",
            "              <pdfaProperty:category>external</pdfaProperty:category>",
            "              <pdfaProperty:description>INVOICE</pdfaProperty:description>",
            "            </rdf:li>",
            "            <rdf:li rdf:parseType=\"Resource\">",
            "              <pdfaProperty:name>Version</pdfaProperty:name>",
            "              <pdfaProperty:valueType>Text</pdfaProperty:valueType>",
            "              <pdfaProperty:category>external</pdfaProperty:category>",
            "              <pdfaProperty:description>The actual version of the Factur-X XML schema</pdfaProperty:description>",
            "            </rdf:li>",
            "            <rdf:li rdf:parseType=\"Resource\">",
            "              <pdfaProperty:name>ConformanceLevel</pdfaProperty:name>",
            "              <pdfaProperty:valueType>Text</pdfaProperty:valueType>",
            "              <pdfaProperty:category>external</pdfaProperty:category>",
            "              <pdfaProperty:description>The conformance level of the embedded Factur-X data</pdfaProperty:description>",
            "            </rdf:li>",
            "          </rdf:Seq>",
            "        </pdfaSchema:property>",
            "      </rdf:li>",
            "    </rdf:Bag>",
            "  </pdfaExtension:schemas>",
            "</rdf:Description>",
            "<rdf:Description rdf:about=\"\" xmlns:fx=\"urn:factur-x:pdfa:CrossIndustryDocument:invoice:1p0#\">",
            "  <fx:DocumentType>INVOICE</fx:DocumentType>",
            "  <fx:DocumentFileName>factur-x.xml</fx:DocumentFileName>",
            "  <fx:Version>1.0</fx:Version>",
            "  <fx:ConformanceLevel>BASIC</fx:ConformanceLevel>",
            "</rdf:Description>");

    private static final DocumentReader READER = new DocumentReader();

    private static InlineNode document() {
        return READER.read(Docs.sectioned(DOCUMENT));
    }

    private static byte[] pdf(PdfA pdfA, Attachments attachments) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.Builder builder = Converter.builder();
        if (pdfA != null) {
            builder.pdfA(pdfA);
        }
        builder.build().toPdf(document(), out, attachments);
        return out.toByteArray();
    }

    private static Attachments facturX() {
        return Attachments.of(Attachment.of("factur-x.xml", FACTUR_X_XML.getBytes(StandardCharsets.UTF_8), "text/xml",
                Relationship.ALTERNATIVE).withDescription("Factur-X invoice")).withXmp(FACTUR_X_XMP);
    }

    /** Each part and level passes its veraPDF profile and still PDF/UA-1, and declares itself in the XMP. */
    @ParameterizedTest
    @EnumSource(PdfA.class)
    void itPassesItsProfileAndPdfUa1(PdfA pdfA) throws Exception {
        byte[] pdf = pdf(pdfA, Attachments.NONE);
        Path dir = Files.createDirectories(Path.of("target", "pdf"));
        Files.write(dir.resolve("pdfa-" + pdfA.name().toLowerCase() + ".pdf"), pdf);
        assertEquals(Map.of(), PdfUa.failedRules(pdf, PdfUa.flavour(pdfA)), pdfA.toString());
        assertEquals(Map.of(), PdfUa.failedRules(pdf, 1), pdfA + " and PDF/UA-1");
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            String xmp = new String(doc.getDocumentCatalog().getMetadata().toByteArray(), StandardCharsets.UTF_8);
            assertTrue(xmp.contains("<pdfaid:part>" + pdfA.part() + "</pdfaid:part>"), xmp);
            assertTrue(xmp.contains("<pdfaid:conformance>" + pdfA.level() + "</pdfaid:conformance>"), xmp);
            assertTrue(xmp.contains("<pdfuaid:part>1</pdfuaid:part>"), xmp);
            assertTrue(xmp.contains("Prueba &amp; PDF/A"), xmp);
            List<PDOutputIntent> intents = doc.getDocumentCatalog().getOutputIntents();
            assertEquals(1, intents.size());
            assertEquals("sRGB IEC61966-2.1", intents.get(0).getOutputConditionIdentifier());
            assertEquals(3, intents.get(0).getDestOutputIntent().getInt(COSName.N));
        }
    }

    /** Without PDF/A nothing changes: no output intent and no declaration. */
    @Test
    void withoutPdfAThereIsNoOutputIntent() throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf(null, Attachments.NONE))) {
            assertEquals(List.of(), doc.getDocumentCatalog().getOutputIntents());
            String xmp = new String(doc.getDocumentCatalog().getMetadata().toByteArray(), StandardCharsets.UTF_8);
            assertFalse(xmp.contains("pdfaid"), xmp);
            assertNull(doc.getDocumentCatalog().getNames());
        }
    }

    /**
     * Factur-X: the invoice XML embedded as an associated file with {@code Alternative}, and its XMP after the
     * engine's own; the PDF passes PDF/A-3b and PDF/A-3a with veraPDF.
     */
    @ParameterizedTest
    @EnumSource(value = PdfA.class, names = {"A3B", "A3A"})
    void embedsTheFilesAndTheirXmpWithPdfA3(PdfA pdfA) throws Exception {
        byte[] pdf = pdf(pdfA, facturX());
        Files.write(Files.createDirectories(Path.of("target", "pdf")).resolve("pdfa-factur-x-" + pdfA.name().toLowerCase() + ".pdf"), pdf);
        assertEquals(Map.of(), PdfUa.failedRules(pdf, PdfUa.flavour(pdfA)), pdfA.toString());
        assertEquals(Map.of(), PdfUa.failedRules(pdf, 1), pdfA + " and PDF/UA-1");
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDDocumentCatalog catalog = doc.getDocumentCatalog();
            Map<String, PDComplexFileSpecification> files = catalog.getNames().getEmbeddedFiles().getNames();
            assertEquals(List.of("factur-x.xml"), List.copyOf(files.keySet()));
            PDComplexFileSpecification specification = files.get("factur-x.xml");
            assertEquals("factur-x.xml", specification.getFile());
            assertEquals("factur-x.xml", specification.getFileUnicode());
            assertEquals("Factur-X invoice", specification.getFileDescription());
            assertEquals("Alternative", specification.getCOSObject().getNameAsString("AFRelationship"));
            PDEmbeddedFile file = specification.getEmbeddedFile();
            assertEquals("text/xml", file.getSubtype());
            assertEquals(FACTUR_X_XML.length(), file.getSize());
            assertArrayEquals(FACTUR_X_XML.getBytes(StandardCharsets.UTF_8), file.toByteArray());
            assertNull(file.getModDate());
            COSArray associated = (COSArray) catalog.getCOSObject().getDictionaryObject("AF");
            assertEquals(1, associated.size());
            assertEquals(specification.getCOSObject(), associated.getObject(0));
            String xmp = new String(catalog.getMetadata().toByteArray(), StandardCharsets.UTF_8);
            assertTrue(xmp.contains("<fx:DocumentFileName>factur-x.xml</fx:DocumentFileName>"), xmp);
            assertTrue(xmp.indexOf("pdfaid:part") < xmp.indexOf("fx:DocumentFileName"), xmp);
        }
    }

    /** The modification date, only if it is given, in UTC. */
    @Test
    void writesTheModificationDateIfGiven() throws Exception {
        Attachment dated = Attachment.of("data.csv", "a;b\n".getBytes(StandardCharsets.UTF_8), "text/csv",
                Relationship.DATA).withModified(Instant.parse("2026-11-01T10:20:30Z"));
        byte[] pdf = pdf(PdfA.A3B, Attachments.of(dated));
        assertEquals(Map.of(), PdfUa.failedRules(pdf, PdfUa.flavour(PdfA.A3B)));
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDEmbeddedFile file = doc.getDocumentCatalog().getNames().getEmbeddedFiles().getNames().get("data.csv")
                    .getEmbeddedFile();
            assertNotNull(file.getModDate());
            assertEquals(Instant.parse("2026-11-01T10:20:30Z"), file.getModDate().toInstant());
        }
    }

    /** A file to embed in a PDF that is not PDF/A-3 is the fault of the configuration, not of the document. */
    @Test
    void anAttachmentNeedsPdfA3() {
        for (PdfA pdfA : new PdfA[] {null, PdfA.A2B, PdfA.A2A}) {
            InvalidModelException e = assertThrows(InvalidModelException.class, () -> pdf(pdfA, facturX()),
                    String.valueOf(pdfA));
            assertEquals(ErrorCode.ATTACHMENT_REQUIRES_PDFA3, e.errorCode());
            assertEquals(StxtPdfException.Kind.RESOURCE, e.kind());
            assertEquals(StxtPdfException.NO_LINE, e.line());
            assertTrue(e.getMessage().contains("factur-x.xml"), e.getMessage());
        }
    }

    /** The extra XMP alone goes in any PDF: it is metadata, not a file. */
    @Test
    void theExtraXmpDoesNotNeedPdfA() throws Exception {
        byte[] pdf = pdf(null, Attachments.NONE.withXmp(FACTUR_X_XMP));
        assertEquals(Map.of(), PdfUa.failedRules(pdf, 1));
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            String xmp = new String(doc.getDocumentCatalog().getMetadata().toByteArray(), StandardCharsets.UTF_8);
            assertTrue(xmp.contains("<fx:Version>1.0</fx:Version>"), xmp);
        }
    }

    /** PDF/A-2 and PDF/A-3 are PDF 1.7 files; PDF/UA-2 is PDF 2.0. */
    @Test
    void pdfA2AndPdfUa2DoNotGoTogether() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Converter.builder().pdfUa(2).pdfA(PdfA.A2B).build());
        assertTrue(e.getMessage().contains("PDF 2.0"), e.getMessage());
        assertEquals(PdfA.A3A, Converter.builder().pdfA(PdfA.A3A).build().pdfA());
        assertNull(Converter.create().pdfA());
    }

    /** The same document with the same attachments gives the same bytes: the profile and the files add no date. */
    @Test
    void isDeterministic() throws Exception {
        assertArrayEquals(pdf(PdfA.A3B, facturX()), pdf(PdfA.A3B, facturX()));
    }

    /** The XMP fragment and the attachments are checked when they are built, not when the PDF is written. */
    @Test
    void rejectsWhatCannotBeWritten() {
        assertThrows(IllegalArgumentException.class, () -> Attachments.NONE.withXmp("<rdf:Description>"));
        assertThrows(IllegalArgumentException.class, () -> Attachments.NONE.withXmp("<a xmlns:b=\"x\"><b:c/></b>"));
        assertThrows(IllegalArgumentException.class, () -> Attachments.NONE.withXmp("<!DOCTYPE a><a/>"));
        Attachment one = Attachment.of("a.xml", new byte[0], "text/xml", Relationship.DATA);
        assertThrows(IllegalArgumentException.class, () -> Attachments.of(one, one));
        assertThrows(IllegalArgumentException.class,
                () -> Attachment.of(" ", new byte[0], "text/xml", Relationship.DATA));
        assertThrows(IllegalArgumentException.class,
                () -> Attachment.of("a.xml", new byte[0], "xml", Relationship.DATA));
        assertEquals("a.xml (text/xml, Data, 0 bytes)", one.toString());
        assertEquals("PDF/A-3b", PdfA.A3B.toString());
    }
}
