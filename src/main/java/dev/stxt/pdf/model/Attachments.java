package dev.stxt.pdf.model;

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

    private final List<Attachment> files;
    private final String xmp;

    private Attachments(List<Attachment> files, String xmp) {
        this.files = files;
        this.xmp = xmp;
    }

    /**
     * These files, in this order, whose names are all different.
     *
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
     * that property once ({@link Xmp}).
     *
     * @throws IllegalArgumentException if it is not well-formed XML
     */
    public Attachments withXmp(String descriptions) {
        Objects.requireNonNull(descriptions, "descriptions");
        Xmp.parse(descriptions);
        return new Attachments(files, descriptions);
    }

    /** The files, in the order they were given. */
    public List<Attachment> files() {
        return files;
    }

    /** The extra XMP ({@link #withXmp}), or {@code null}. */
    public String xmp() {
        return xmp;
    }

    /** Whether there is nothing to add: no files and no extra XMP. */
    public boolean isEmpty() {
        return files.isEmpty() && xmp == null;
    }

    @Override
    public String toString() {
        return files + (xmp != null ? " with XMP" : "");
    }
}
