package dev.stxt.pdf.model;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * What is added to the PDF of one document when it is written ({@code Converter.toPdf(document, out, attachments)},
 * reference/implementation/api.md): the embedded files ({@link Attachment}) and, with them, the XMP that describes them.
 * {@link #NONE} is the usual case. A {@code Converter} is shared and does not carry them, because they belong to one
 * document: the invoice XML of Factur-X is that of one invoice. Immutable.
 *
 * <pre>{@code
 * Attachments facturX = Attachments.of(Attachment.of("factur-x.xml", xml, "text/xml", Relationship.ALTERNATIVE))
 *         .withXmp(facturXDescriptions);
 * converter.toPdf(document, out, facturX);
 * }</pre>
 */
public final class Attachments {

    /** No attachments and no extra XMP. */
    public static final Attachments NONE = new Attachments(List.of(), null);

    private static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    private final List<Attachment> files;
    private final String xmp;

    private Attachments(List<Attachment> files, String xmp) {
        this.files = files;
        this.xmp = xmp;
    }

    /**
     * {@return these files, in this order, whose names are all different, without extra XMP}
     *
     * @param files the files to embed; none of them {@code null}
     * @throws IllegalArgumentException if two files have the same name
     */
    public static Attachments of(Attachment... files) {
        List<Attachment> list = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (Attachment file : files) {
            Objects.requireNonNull(file, "attachment");
            if (!names.add(file.name())) {
                throw new IllegalArgumentException("Two attachments with the same name: " + file.name());
            }
            list.add(file);
        }
        return new Attachments(List.copyOf(list), null);
    }

    /**
     * The same attachments with extra XMP metadata: one or more {@code rdf:Description} elements, written as they
     * go inside the {@code rdf:RDF} of the packet, which declare the namespaces they use (the {@code rdf} prefix is
     * already declared). For Factur-X, the fragment its specification gives, with the {@code fx} properties and the
     * {@code pdfaExtension} schema that describes them. The engine writes it after its own metadata, as it is except
     * for the extension schemas ({@code pdfaExtension:schemas}), which it joins with its own, because a packet admits
     * that property once (reference/implementation/pdf.md).
     *
     * @param descriptions the {@code rdf:Description} elements, as XML text without a document type declaration;
     *        not {@code null}
     * @return the same files with this XMP, which replaces any given before
     * @throws IllegalArgumentException if it is not well-formed XML
     */
    public Attachments withXmp(String descriptions) {
        Objects.requireNonNull(descriptions, "descriptions");
        checkWellFormed(descriptions);
        return new Attachments(files, descriptions);
    }

    /**
     * Checks that {@code fragment} is well-formed XML inside the {@code rdf:RDF} of a packet, with {@code rdf} as the
     * only prefix declared from outside: every other prefix it uses has to be declared in it. A document type
     * declaration is not admitted, so that no entity is expanded.
     *
     * @throws IllegalArgumentException if it is not
     */
    private static void checkWellFormed(String fragment) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(true);
            String wrapped = "<rdf:RDF xmlns:rdf=\"" + RDF + "\">" + fragment + "</rdf:RDF>";
            // A handler that stops at any error; without one, the parser also prints it to the standard error.
            factory.newSAXParser().parse(new InputSource(new StringReader(wrapped)), new DefaultHandler() {
                @Override
                public void error(SAXParseException e) throws SAXException {
                    throw e;
                }
            });
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalArgumentException("The XMP fragment is not well-formed XML: " + e.getMessage(), e);
        }
    }

    /** {@return the files, in the order they were given} */
    public List<Attachment> files() {
        return files;
    }

    /** {@return the extra XMP ({@link #withXmp}), or {@code null}} */
    public String xmp() {
        return xmp;
    }

    /** {@return whether there is nothing to add: no files and no extra XMP} */
    public boolean isEmpty() {
        return files.isEmpty() && xmp == null;
    }

    @Override
    public String toString() {
        return files + (xmp != null ? " with XMP" : "");
    }
}
