package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.InternalException;
import dev.stxt.pdf.model.PdfA;
import dev.stxt.pdf.model.Xmp;
import dev.stxt.pdf.tree.Metadata;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.pdfbox.pdmodel.interactive.viewerpreferences.PDViewerPreferences;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * What the PDF says about itself (reference/implementation/pdf.md): the title and the language of the document, the
 * XMP packet with the declarations of PDF/UA and of PDF/A, and the output intent of PDF/A.
 *
 * <p>The title goes in the document information and in the XMP metadata ({@code dc:title}), and the viewer shows it
 * instead of the file name; the language ({@code Lang} of the catalog) is that of all the content. A document always
 * has a {@code Title} (required): without a title the PDF would not comply with PDF/UA (7.1) and could not claim it.
 * With PDF/A, the packet carries {@code pdfaid:part} and {@code pdfaid:conformance} and the catalog an
 * {@code OutputIntent} with the sRGB profile of the ICC ({@link #ICC_PROFILE}), the color every color of the PDF is
 * in; the viewer needs nothing else to show the colors as they were written.</p>
 */
final class PdfMetadata {

    /** The sRGB profile of the output intent, a resource of the jar (reference/common/resources.md). */
    static final String ICC_PROFILE = "/dev/stxt/pdf/icc/sRGB2014.icc";

    /**
     * The description of the PDF/UA identification schema ({@code pdfuaid}) as an extension schema of PDF/A: PDF/A
     * only admits the schemas it predefines or those the packet describes, and {@code pdfuaid} is not among the
     * former. It is the one the PDF Association gives for PDF/UA-1 (only {@code part}: PDF/UA-2, with {@code rev},
     * is PDF 2.0 and does not go with PDF/A-2 or PDF/A-3). An item of the bag of {@link #extensionSchemas}.
     */
    private static final String PDFUA_SCHEMA = String.join("\n",
            "<rdf:li rdf:parseType=\"Resource\">",
            "<pdfaSchema:schema>PDF/UA identification schema</pdfaSchema:schema>",
            "<pdfaSchema:namespaceURI>http://www.aiim.org/pdfua/ns/id/</pdfaSchema:namespaceURI>",
            "<pdfaSchema:prefix>pdfuaid</pdfaSchema:prefix>",
            "<pdfaSchema:property><rdf:Seq><rdf:li rdf:parseType=\"Resource\">",
            "<pdfaProperty:name>part</pdfaProperty:name>",
            "<pdfaProperty:valueType>Integer</pdfaProperty:valueType>",
            "<pdfaProperty:category>internal</pdfaProperty:category>",
            "<pdfaProperty:description>PDF/UA version identifier</pdfaProperty:description>",
            "</rdf:li></rdf:Seq></pdfaSchema:property>",
            "</rdf:li>");

    private PdfMetadata() {
    }

    /**
     * Writes the title and the language of {@code metadata}, and the XMP packet if there is a title, a PDF/A to
     * declare ({@code pdfA}, or {@code null}) or extra XMP ({@code extraXmp}: {@code rdf:Description} elements, or
     * {@code null}).
     */
    static void write(PDDocument doc, Metadata metadata, int ua, PdfA pdfA, String extraXmp) throws IOException {
        PDDocumentCatalog catalog = doc.getDocumentCatalog();
        if (metadata.language() != null) {
            catalog.setLanguage(metadata.language());
        }
        if (metadata.title() != null) {
            doc.getDocumentInformation().setTitle(metadata.title());
            PDViewerPreferences preferences = new PDViewerPreferences(new COSDictionary());
            preferences.setDisplayDocTitle(true);
            catalog.setViewerPreferences(preferences);
        }
        if (metadata.title() != null || pdfA != null || extraXmp != null) {
            PDMetadata xmp = new PDMetadata(doc);
            xmp.importXMPMetadata(xmp(metadata.title(), ua, pdfA, extraXmp).getBytes(StandardCharsets.UTF_8));
            catalog.setMetadata(xmp);
        }
        if (pdfA != null) {
            catalog.addOutputIntent(outputIntent(doc));
        }
    }

    /**
     * The XMP packet: the title ({@code dc:title}, if there is one) with the PDF/UA declaration ({@code pdfuaid:part},
     * 1 or 2; 2 with the revision of the standard, {@code pdfuaid:rev}), the PDF/A declaration ({@code pdfaid:part}
     * and {@code pdfaid:conformance}, if {@code pdfA} is not {@code null}), the extension schemas (the description
     * of {@code pdfuaid} that PDF/A asks for, and those of {@code extra}) and the rest of {@code extra}
     * ({@link Xmp}), at the end.
     */
    static String xmp(String title, int ua, PdfA pdfA, String extra) {
        StringBuilder packet = new StringBuilder();
        packet.append("<?xpacket begin=\"﻿\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>\n")
                .append("<x:xmpmeta xmlns:x=\"adobe:ns:meta/\">\n")
                .append("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n");
        if (title != null) {
            String escaped = title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            packet.append("<rdf:Description rdf:about=\"\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n")
                    .append("<dc:title><rdf:Alt><rdf:li xml:lang=\"x-default\">").append(escaped)
                    .append("</rdf:li></rdf:Alt></dc:title>\n")
                    .append("</rdf:Description>\n")
                    .append("<rdf:Description rdf:about=\"\" xmlns:pdfuaid=\"http://www.aiim.org/pdfua/ns/id/\">\n")
                    .append("<pdfuaid:part>").append(ua).append("</pdfuaid:part>\n")
                    .append(ua == 2 ? "<pdfuaid:rev>2024</pdfuaid:rev>\n" : "")
                    .append("</rdf:Description>\n");
        }
        if (pdfA != null) {
            packet.append("<rdf:Description rdf:about=\"\" xmlns:pdfaid=\"http://www.aiim.org/pdfa/ns/id/\">\n")
                    .append("<pdfaid:part>").append(pdfA.part()).append("</pdfaid:part>\n")
                    .append("<pdfaid:conformance>").append(pdfA.level()).append("</pdfaid:conformance>\n")
                    .append("</rdf:Description>\n");
        }
        List<String> schemas = new ArrayList<>();
        if (pdfA != null && title != null) {
            schemas.add(PDFUA_SCHEMA);
        }
        String descriptions = "";
        if (extra != null) {
            Xmp xmp = Xmp.parse(extra);
            schemas.addAll(xmp.schemas());
            descriptions = xmp.descriptions();
        }
        if (!schemas.isEmpty()) {
            packet.append(extensionSchemas(schemas));
        }
        packet.append(descriptions);
        return packet.append("</rdf:RDF>\n</x:xmpmeta>\n<?xpacket end=\"w\"?>").toString();
    }

    /**
     * The one {@code pdfaExtension:schemas} of the packet, with the schema descriptions of the engine and of the
     * fragment given from outside in a single bag. It declares the prefixes of the extension schema vocabulary
     * ({@link Xmp#EXTENSION_SCOPE}); an item that uses another declares it itself ({@link Xmp#serialize}).
     */
    private static String extensionSchemas(List<String> schemas) {
        StringBuilder out = new StringBuilder();
        out.append("<rdf:Description rdf:about=\"\"");
        new TreeMap<>(Xmp.EXTENSION_SCOPE).forEach((prefix, uri) -> {
            if (!prefix.equals("rdf")) {
                out.append(" xmlns:").append(prefix).append("=\"").append(uri).append('"');
            }
        });
        out.append(">\n<pdfaExtension:schemas><rdf:Bag>\n");
        for (String schema : schemas) {
            out.append(schema).append('\n');
        }
        return out.append("</rdf:Bag></pdfaExtension:schemas>\n</rdf:Description>\n").toString();
    }

    /** The output intent of PDF/A ({@code GTS_PDFA1}) with the sRGB profile, read from the jar. */
    private static PDOutputIntent outputIntent(PDDocument doc) throws IOException {
        try (InputStream profile = PdfMetadata.class.getResourceAsStream(ICC_PROFILE)) {
            if (profile == null) {
                throw new InternalException("The sRGB profile is missing from the jar: " + ICC_PROFILE, null);
            }
            PDOutputIntent intent = new PDOutputIntent(doc, profile);
            intent.setInfo("sRGB IEC61966-2.1");
            intent.setOutputCondition("sRGB IEC61966-2.1");
            intent.setOutputConditionIdentifier("sRGB IEC61966-2.1");
            intent.setRegistryName("http://www.color.org");
            return intent;
        }
    }
}
