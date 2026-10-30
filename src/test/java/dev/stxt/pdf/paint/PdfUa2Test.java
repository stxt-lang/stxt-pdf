package dev.stxt.pdf.paint;

import dev.stxt.InlineNode;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.golden.PdfUa;
import dev.stxt.pdf.stxt.DocumentReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDListAttributeObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El PDF con PDF/UA-2 (referencia/implementacion/pdf.md): PDF 2.0, los elementos en el espacio de nombres de PDF 2.0,
 * las entradas de índice con {@code Reference} y {@code Ref}, los destinos de estructura y {@code Unordered} para las
 * listas con guion. veraPDF lo valida con su perfil de PDF/UA-2.
 */
class PdfUa2Test {

    private static final String DOCUMENT = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n"
            + "\t\tWidth: 300\n\t\tHeight: 300\n\t\tMargin: 20\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n"
            + "\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
            + "\tTable of contents:\n"
            + "\tHeading 1:\n\t\tText: Uno\n"
            + "\tList:\n\t\tMarker: dash\n\t\tBox:\n\t\t\tPadding: 0 0 0 16\n\t\tList item:\n\t\t\tText: Un ítem\n"
            + "\tText: Vuelve al principio\n\t\tLink: #uno\n"
            + "\tHeading 1:\n\t\tId: uno\n\t\tText: Dos\n"
            + "\tFixed:\n\t\tOffset: auto 0 0 auto\n\t\tText: ¿Más? Escríbenos\n\t\t\tLink: mailto:a@b.example\n";

    private static byte[] pdf(int ua) throws Exception {
        InlineNode document = new DocumentReader().read(Docs.sectioned(DOCUMENT));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.builder().pdfUa(ua).build().toPdf(document, out);
        return out.toByteArray();
    }

    @Test
    void itPassesPdfUa2() throws Exception {
        byte[] pdf = pdf(2);
        assertEquals(Map.of(), PdfUa.failedRules(pdf, 2));
        assertEquals("%PDF-2.0", new String(pdf, 0, 8, StandardCharsets.US_ASCII));
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertEquals("2.0", doc.getDocumentCatalog().getVersion());
            String xmp = new String(doc.getDocumentCatalog().getMetadata().toByteArray(), StandardCharsets.UTF_8);
            assertTrue(xmp.contains("<pdfuaid:part>2</pdfuaid:part>"), xmp);
            assertTrue(xmp.contains("<pdfuaid:rev>2024</pdfuaid:rev>"), xmp);
        }
    }

    @Test
    void theElementsAreInThePdf2Namespace() throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf(2))) {
            COSDictionary root = doc.getDocumentCatalog().getStructureTreeRoot().getCOSObject();
            COSDictionary namespace = (COSDictionary) ((COSArray) root.getDictionaryObject("Namespaces")).getObject(0);
            assertEquals("http://iso.org/pdf2/ssn", namespace.getString("NS"));
            PDStructureElement document = document(doc);
            assertEquals(namespace, document.getCOSObject().getDictionaryObject("NS"));
            // TOC, TOCI y Reference no están en PDF 2.0: se quedan en el de PDF 1.7, el de omisión.
            PDStructureElement toc = find(document, "TOC");
            assertNull(toc.getCOSObject().getDictionaryObject("NS"));
        }
    }

    @Test
    void aContentsEntryIsAReferenceThatPointsToItsHeading() throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf(2))) {
            PDStructureElement document = document(doc);
            PDStructureElement toci = find(document, "TOCI");
            assertEquals(List.of("Reference"), types(toci));
            PDStructureElement reference = (PDStructureElement) toci.getKids().get(0);
            assertEquals(List.of("Link"), types(reference));
            COSArray ref = (COSArray) toci.getCOSObject().getDictionaryObject("Ref");
            COSDictionary target = (COSDictionary) ref.getObject(0);
            assertEquals(find(document, "H1").getCOSObject(), target);
        }
    }

    @Test
    void aDashListIsUnordered() throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf(2))) {
            assertEquals("Unordered", numbering(find(document(doc), "L")));
        }
        try (PDDocument doc = Loader.loadPDF(pdf(1))) {
            assertEquals("None", numbering(find(document(doc), "L")));
        }
    }

    @Test
    void internalLinksAndBookmarksGoToTheStructureElement() throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf(2))) {
            PDStructureElement document = document(doc);
            COSDictionary second = headings(document).get(1).getCOSObject();
            PDAnnotationLink back = links(doc).stream()
                    .filter(a -> "Vuelve al principio".equals(a.getContents())).findFirst().orElseThrow();
            COSDictionary action = back.getCOSObject().getCOSDictionary(COSName.A);
            assertEquals("GoTo", action.getNameAsString(COSName.S));
            assertEquals(second, ((COSArray) action.getDictionaryObject("SD")).getObject(0));
            // Y el destino de la página, para los visores que no entienden el de estructura.
            assertNotNull(action.getDictionaryObject(COSName.D));
            COSDictionary bookmark = doc.getDocumentCatalog().getDocumentOutline().getFirstChild().getCOSObject()
                    .getCOSDictionary(COSName.A);
            assertEquals(headings(document).get(0).getCOSObject(),
                    ((COSArray) bookmark.getDictionaryObject("SD")).getObject(0));
        }
    }

    @Test
    void aLinkWithAltHasNoContents() throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf(2))) {
            PDStructureElement footer = find(document(doc), "Link", "¿Más? Escríbenos");
            assertNotNull(footer);
            PDAnnotationLink mail = links(doc).stream().filter(a -> a.getAction() != null
                    && a.getAction().getCOSObject().getString(COSName.URI) != null).findFirst().orElseThrow();
            assertNull(mail.getContents());
        }
        try (PDDocument doc = Loader.loadPDF(pdf(1))) {
            assertTrue(links(doc).stream().anyMatch(a -> "¿Más? Escríbenos".equals(a.getContents())));
        }
    }

    @Test
    void onlyTheTwoPartsExist() {
        assertThrows(IllegalArgumentException.class, () -> Converter.builder().pdfUa(3));
    }

    private static PDStructureElement document(PDDocument doc) {
        return (PDStructureElement) doc.getDocumentCatalog().getStructureTreeRoot().getKids().get(0);
    }

    private static String numbering(PDStructureElement list) {
        return ((PDListAttributeObject) list.getAttributes().getObject(0))
                .getListNumbering();
    }

    private static List<PDAnnotationLink> links(PDDocument doc) throws Exception {
        List<PDAnnotationLink> links = new ArrayList<>();
        for (var page : doc.getPages()) {
            page.getAnnotations().forEach(a -> links.add((PDAnnotationLink) a));
        }
        return links;
    }

    private static List<PDStructureElement> headings(PDStructureElement e) {
        List<PDStructureElement> found = new ArrayList<>();
        collect(e, "H1", null, found);
        return found;
    }

    private static PDStructureElement find(PDStructureElement e, String type) {
        return find(e, type, null);
    }

    /** El primer elemento de ese tipo (y con ese {@code Alt}, si no es {@code null}), en orden del documento. */
    private static PDStructureElement find(PDStructureElement e, String type, String alt) {
        List<PDStructureElement> found = new ArrayList<>();
        collect(e, type, alt, found);
        return found.isEmpty() ? null : found.get(0);
    }

    private static void collect(PDStructureElement e, String type, String alt, List<PDStructureElement> found) {
        if (type.equals(e.getStructureType()) && (alt == null || alt.equals(e.getAlternateDescription()))) {
            found.add(e);
        }
        for (Object k : e.getKids()) {
            if (k instanceof PDStructureElement child) {
                collect(child, type, alt, found);
            }
        }
    }

    private static List<String> types(PDStructureElement e) {
        List<String> types = new ArrayList<>();
        for (Object k : e.getKids()) {
            if (k instanceof PDStructureElement child) {
                types.add(child.getStructureType());
            }
        }
        return types;
    }
}
