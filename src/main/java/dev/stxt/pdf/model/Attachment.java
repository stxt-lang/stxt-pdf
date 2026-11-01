package dev.stxt.pdf.model;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A file embedded in the PDF (reference/common/pdf.md): its name, its bytes, its media type and its relationship with
 * the document ({@code AFRelationship}). It is the piece of Factur-X / ZUGFeRD: the XML of the invoice is attached as
 * {@code factur-x.xml}, {@code text/xml}, {@link Relationship#ALTERNATIVE}, and the engine does not generate it, it
 * only embeds it. It needs PDF/A-3 ({@link PdfA}): with any other PDF, {@code ATTACHMENT_REQUIRES_PDFA3}. Immutable.
 *
 * <pre>{@code
 * Attachment invoice = Attachment.of("factur-x.xml", xml, "text/xml", Attachment.Relationship.ALTERNATIVE)
 *         .withDescription("Factur-X invoice");
 * }</pre>
 */
public final class Attachment {

    /** What the file is to the document ({@code AFRelationship}, ISO 32000-2, 14.13.5). */
    public enum Relationship {
        /** The data the document shows (the spreadsheet of a table). */
        DATA("Data"),
        /** Another representation of the document (the XML of an invoice). */
        ALTERNATIVE("Alternative"),
        /** What the document was made from (the source document). */
        SOURCE("Source"),
        /** Something that completes the document. */
        SUPPLEMENT("Supplement");

        private final String pdfName;

        Relationship(String pdfName) {
            this.pdfName = pdfName;
        }

        /** The name written in the PDF: {@code Data}, {@code Alternative}, {@code Source} or {@code Supplement}. */
        public String pdfName() {
            return pdfName;
        }
    }

    private static final Pattern MIME_TYPE = Pattern.compile("[!#$&^_.+\\-\\w]+/[!#$&^_.+\\-\\w]+");

    private final String name;
    private final byte[] bytes;
    private final String mimeType;
    private final Relationship relationship;
    private final String description;
    private final Instant modified;

    private Attachment(String name, byte[] bytes, String mimeType, Relationship relationship, String description,
            Instant modified) {
        this.name = name;
        this.bytes = bytes;
        this.mimeType = mimeType;
        this.relationship = relationship;
        this.description = description;
        this.modified = modified;
    }

    /**
     * A file to embed: {@code name} is the file name the viewer shows and the key it is found by ({@code factur-x.xml}),
     * {@code bytes} its content (copied), {@code mimeType} its media type ({@code text/xml}, {@code application/pdf})
     * and {@code relationship} what it is to the document.
     *
     * @throws IllegalArgumentException if the name is blank or the media type is not {@code type/subtype}
     */
    public static Attachment of(String name, byte[] bytes, String mimeType, Relationship relationship) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(bytes, "bytes");
        Objects.requireNonNull(mimeType, "mimeType");
        Objects.requireNonNull(relationship, "relationship");
        if (name.isBlank()) {
            throw new IllegalArgumentException("The name of an attachment cannot be blank");
        }
        if (!MIME_TYPE.matcher(mimeType).matches()) {
            throw new IllegalArgumentException("The media type of an attachment is type/subtype, as text/xml: " + mimeType);
        }
        return new Attachment(name, bytes.clone(), mimeType, relationship, null, null);
    }

    /** The same file with a description ({@code Desc}), which the viewer shows next to the name. */
    public Attachment withDescription(String description) {
        return new Attachment(name, bytes, mimeType, relationship, Objects.requireNonNull(description, "description"),
                modified);
    }

    /**
     * The same file with its modification date ({@code ModDate}), which some profiles of embedded files ask for.
     * Without it, the PDF carries no date, and the same document gives the same bytes.
     */
    public Attachment withModified(Instant modified) {
        return new Attachment(name, bytes, mimeType, relationship, description,
                Objects.requireNonNull(modified, "modified"));
    }

    /** The file name, unique among the attachments of a PDF. */
    public String name() {
        return name;
    }

    /** The content, a copy. */
    public byte[] bytes() {
        return bytes.clone();
    }

    /** The number of bytes of the content. */
    public int size() {
        return bytes.length;
    }

    /** The media type ({@code text/xml}). */
    public String mimeType() {
        return mimeType;
    }

    public Relationship relationship() {
        return relationship;
    }

    /** The description, or {@code null}. */
    public String description() {
        return description;
    }

    /** The modification date, or {@code null}. */
    public Instant modified() {
        return modified;
    }

    @Override
    public String toString() {
        return name + " (" + mimeType + ", " + relationship.pdfName() + ", " + bytes.length + " bytes)";
    }
}
