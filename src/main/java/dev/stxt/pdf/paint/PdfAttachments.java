package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.Attachment;
import dev.stxt.pdf.model.Attachments;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

/**
 * The files embedded in the PDF (reference/implementation/pdf.md): one file specification per {@link Attachment},
 * with the file in {@code EF} (compressed, with its media type as {@code Subtype} and its size), its name in {@code F}
 * and {@code UF}, its {@code AFRelationship} and its description, listed in the {@code EmbeddedFiles} name tree of
 * the catalog (which is where a viewer looks for them) and in the {@code AF} array of the catalog (which is what makes
 * them associated files of the document, as PDF/A-3 and Factur-X ask).
 */
final class PdfAttachments {

    private PdfAttachments() {
    }

    /** Embeds the files of {@code attachments}, if there are any; whoever calls checked that the PDF is PDF/A-3. */
    static void write(PDDocument doc, Attachments attachments) throws IOException {
        if (attachments.files().isEmpty()) {
            return;
        }
        Map<String, PDComplexFileSpecification> byName = new TreeMap<>();
        COSArray associated = new COSArray();
        for (Attachment attachment : attachments.files()) {
            PDComplexFileSpecification specification = specification(doc, attachment);
            byName.put(attachment.name(), specification);
            associated.add(specification.getCOSObject());
        }
        PDDocumentCatalog catalog = doc.getDocumentCatalog();
        PDEmbeddedFilesNameTreeNode tree = new PDEmbeddedFilesNameTreeNode();
        tree.setNames(byName);
        PDDocumentNameDictionary names = new PDDocumentNameDictionary(catalog);
        names.setEmbeddedFiles(tree);
        catalog.setNames(names);
        catalog.getCOSObject().setItem(COSName.getPDFName("AF"), associated);
    }

    private static PDComplexFileSpecification specification(PDDocument doc, Attachment attachment) throws IOException {
        PDEmbeddedFile file = new PDEmbeddedFile(doc, new ByteArrayInputStream(attachment.bytes()),
                COSName.FLATE_DECODE);
        file.setSubtype(attachment.mimeType());
        file.setSize(attachment.size());
        if (attachment.modified() != null) {
            Calendar modified = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            modified.setTimeInMillis(attachment.modified().toEpochMilli());
            file.setModDate(modified);
        }
        PDComplexFileSpecification specification = new PDComplexFileSpecification();
        specification.setFile(attachment.name());
        specification.setFileUnicode(attachment.name());
        specification.setEmbeddedFile(file);
        specification.setEmbeddedFileUnicode(file);
        specification.getCOSObject().setName(COSName.getPDFName("AFRelationship"),
                attachment.relationship().pdfName());
        if (attachment.description() != null) {
            specification.setFileDescription(attachment.description());
        }
        return specification;
    }
}
