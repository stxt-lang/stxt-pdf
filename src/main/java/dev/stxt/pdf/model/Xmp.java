package dev.stxt.pdf.model;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An XMP fragment given from outside ({@link Attachments#withXmp}): one or more {@code rdf:Description} elements, as
 * they go inside the {@code rdf:RDF} of the packet. It is parsed to check that it is XML and to take out of it the
 * extension schemas it describes ({@code pdfaExtension:schemas}), because an XMP packet admits that property once and
 * the engine writes its own (reference/implementation/pdf.md).
 *
 * @param schemas      the schema descriptions ({@code rdf:li} of the {@code rdf:Bag} of {@code pdfaExtension:schemas}),
 *                     serialized, in order
 * @param descriptions the rest, serialized: the {@code rdf:Description} elements without their
 *                     {@code pdfaExtension:schemas}, and without those that had nothing else
 */
public record Xmp(List<String> schemas, String descriptions) {

    public static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String PDFA_EXTENSION = "http://www.aiim.org/pdfa/ns/extension/";

    /** The prefixes the packet declares around a fragment: on {@code rdf:RDF}, {@code rdf}. */
    private static final Map<String, String> PACKET_SCOPE = Map.of("rdf", RDF);

    /**
     * The prefixes declared on the {@code rdf:Description} of the extension schemas the engine writes, those of the
     * extension schema vocabulary of PDF/A (ISO 19005-2, 6.6.2.3.2), besides {@code rdf}: a schema description is
     * serialized inside it.
     */
    public static final Map<String, String> EXTENSION_SCOPE = Map.of(
            "rdf", RDF,
            "pdfaExtension", PDFA_EXTENSION,
            "pdfaSchema", "http://www.aiim.org/pdfa/ns/schema#",
            "pdfaProperty", "http://www.aiim.org/pdfa/ns/property#",
            "pdfaType", "http://www.aiim.org/pdfa/ns/type#",
            "pdfaField", "http://www.aiim.org/pdfa/ns/field#");

    /**
     * Parses {@code fragment}, with {@code rdf} as the only prefix declared from outside.
     *
     * @throws IllegalArgumentException if it is not well-formed XML
     */
    public static Xmp parse(String fragment) {
        Element root;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(true);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            // Without a handler the parser also prints the error to the standard error.
            builder.setErrorHandler(new DefaultHandler() {
                @Override
                public void error(SAXParseException e) throws SAXException {
                    throw e;
                }
            });
            String wrapped = "<rdf:RDF xmlns:rdf=\"" + RDF + "\">" + fragment + "</rdf:RDF>";
            Document document = builder.parse(new InputSource(new StringReader(wrapped)));
            root = document.getDocumentElement();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalArgumentException("The XMP fragment is not well-formed XML: " + e.getMessage(), e);
        }
        List<String> schemas = new ArrayList<>();
        StringBuilder descriptions = new StringBuilder();
        for (Element description : children(root)) {
            for (Element schemasProperty : children(description)) {
                if (PDFA_EXTENSION.equals(schemasProperty.getNamespaceURI())
                        && "schemas".equals(schemasProperty.getLocalName())) {
                    for (Element bag : children(schemasProperty)) {
                        for (Element item : children(bag)) {
                            schemas.add(serialize(item, EXTENSION_SCOPE));
                        }
                    }
                    description.removeChild(schemasProperty);
                }
            }
            if (!children(description).isEmpty()) {
                descriptions.append(serialize(description, PACKET_SCOPE)).append('\n');
            }
        }
        return new Xmp(List.copyOf(schemas), descriptions.toString());
    }

    private static List<Element> children(Element element) {
        List<Element> children = new ArrayList<>();
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element child) {
                children.add(child);
            }
        }
        return children;
    }

    /**
     * The element as XML, declaring every namespace it uses that is not declared in {@code scope} (prefix to
     * namespace): the declarations of the elements it was taken out of go with it. Comments and processing
     * instructions are dropped; the text is kept.
     */
    public static String serialize(Element element, Map<String, String> scope) {
        StringBuilder out = new StringBuilder();
        write(element, scope, out);
        return out.toString();
    }

    private static void write(Element element, Map<String, String> outer, StringBuilder out) {
        Map<String, String> scope = new HashMap<>(outer);
        Map<String, String> declared = new LinkedHashMap<>();
        NamedNodeMap attributes = element.getAttributes();
        // The declarations the element already carries, as they are.
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);
            if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attribute.getNamespaceURI())) {
                String prefix = XMLConstants.XMLNS_ATTRIBUTE.equals(attribute.getName()) ? "" : attribute.getLocalName();
                declared.put(prefix, attribute.getValue());
                scope.put(prefix, attribute.getValue());
            }
        }
        declare(element.getPrefix(), element.getNamespaceURI(), scope, declared);
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);
            if (attribute.getNamespaceURI() != null
                    && !XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attribute.getNamespaceURI())) {
                declare(attribute.getPrefix(), attribute.getNamespaceURI(), scope, declared);
            }
        }
        out.append('<').append(element.getTagName());
        declared.forEach((prefix, uri) -> out.append(' ').append(prefix.isEmpty() ? "xmlns" : "xmlns:" + prefix)
                .append("=\"").append(escape(uri, true)).append('"'));
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);
            if (!XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attribute.getNamespaceURI())) {
                out.append(' ').append(attribute.getName()).append("=\"").append(escape(attribute.getValue(), true))
                        .append('"');
            }
        }
        NodeList nodes = element.getChildNodes();
        if (nodes.getLength() == 0) {
            out.append("/>");
            return;
        }
        out.append('>');
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element child) {
                write(child, scope, out);
            } else if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
                out.append(escape(node.getNodeValue(), false));
            }
        }
        out.append("</").append(element.getTagName()).append('>');
    }

    /** Adds the declaration of {@code prefix} if the scope does not already map it to {@code uri}. */
    private static void declare(String prefix, String uri, Map<String, String> scope, Map<String, String> declared) {
        if (uri == null) {
            return;
        }
        String key = prefix == null ? "" : prefix;
        if (!uri.equals(scope.get(key))) {
            declared.put(key, uri);
            scope.put(key, uri);
        }
    }

    private static String escape(String text, boolean attribute) {
        String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return attribute ? escaped.replace("\"", "&quot;") : escaped;
    }
}
