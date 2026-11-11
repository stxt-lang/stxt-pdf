package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.Marker;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDDefaultAttributeObject;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureNode;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDListAttributeObject;

import java.util.Set;

/**
 * What PDF/UA-2 (ISO 14289-2, on PDF 2.0) does differently from PDF/UA-1 (ISO 14289-1, on PDF 1.7) in the structure
 * tree (reference/implementation/pdf.md, "PDF/UA-2"): the namespace of the elements, the type and the attribute of a
 * footnote, what a table of contents entry contains, the numbering of a list, the text of a described link and
 * whether what leads to a box is a structure destination. {@link PdfStructure} and {@link PdfStructureLinks} ask it and
 * do not know which part they write; the rest of the PDF (the version, the metadata) is decided by
 * {@link PdfDocumentWriter} and {@link PdfMetadata} with the part itself.
 */
final class UaProfile {

    /** The standard namespace of PDF 2.0 (ISO 32000-2, 14.8.6). */
    static final String PDF_2_NAMESPACE = "http://iso.org/pdf2/ssn";

    /** The types that are not in the PDF 2.0 namespace: they stay in that of PDF 1.7, the default one. */
    private static final Set<String> PDF_1_7_ONLY = Set.of("TOC", "TOCI", "Reference", "BlockQuote");

    /**
     * With PDF/UA-2, the PDF 2.0 namespace of the elements, one dictionary shared by all of them; with PDF/UA-1,
     * {@code null}.
     */
    private final COSDictionary pdf2;

    private UaProfile(int part) {
        if (part == 2) {
            pdf2 = new COSDictionary();
            pdf2.setItem(COSName.TYPE, COSName.getPDFName("Namespace"));
            pdf2.setString(COSName.getPDFName("NS"), PDF_2_NAMESPACE);
        } else {
            pdf2 = null;
        }
    }

    /**
     * The profile of that part of PDF/UA, 1 or 2 (reference/implementation/pdf.md); {@link PdfDocumentWriter} checks
     * the part before.
     */
    static UaProfile of(int part) {
        return new UaProfile(part);
    }

    /** Declares in the root of the tree the namespaces its elements use: with PDF/UA-2, that of PDF 2.0. */
    void declare(PDStructureTreeRoot treeRoot) {
        if (pdf2 != null) {
            COSArray namespaces = new COSArray();
            namespaces.add(pdf2);
            treeRoot.getCOSObject().setItem(COSName.getPDFName("Namespaces"), namespaces);
        }
    }

    /**
     * The namespace of an element of that type: with PDF/UA-2, that of PDF 2.0 if the type is in it; {@code null} for
     * the default one, that of PDF 1.7, where every element stays with PDF/UA-1 and the types that PDF 2.0 does not
     * have ({@code TOC}, {@code TOCI}, {@code Reference}) with both.
     */
    COSDictionary namespace(String type) {
        return pdf2 != null && !PDF_1_7_ONLY.contains(type) ? pdf2 : null;
    }

    /**
     * A new structure element of that type, in its namespace ({@link #namespace}). Whoever creates it adds it to its
     * parent.
     */
    PDStructureElement element(String type, PDStructureNode parent) {
        PDStructureElement e = new PDStructureElement(type, parent);
        COSDictionary namespace = namespace(type);
        if (namespace != null) {
            e.getCOSObject().setItem(COSName.getPDFName("NS"), namespace);
        }
        return e;
    }

    /** The structure type of a footnote: {@code Note}, or {@code FENote} with PDF/UA-2, which is PDF 2.0. */
    String noteType() {
        return pdf2 != null ? "FENote" : "Note";
    }

    /**
     * What the element of a footnote declares besides its type: with PDF/UA-2, that its {@code FENote} is a footnote
     * ({@code NoteType: Footnote}); with PDF/UA-1, nothing.
     */
    void describeNote(PDStructureElement note) {
        if (pdf2 != null) {
            COSDictionary noteType = new COSDictionary();
            noteType.setName(COSName.O, "FENote");
            noteType.setName(COSName.getPDFName("NoteType"), "Footnote");
            note.addAttribute(new PDDefaultAttributeObject(noteType));
        }
    }

    /**
     * Whether a table of contents entry ({@code TOCI}) contains a {@code Reference} that contains its {@code Link} and
     * points to its heading with {@code Ref}, as PDF/UA-2 asks (ISO/TS 32005), instead of the {@code Link} itself.
     */
    boolean tocEntryWrapsReference() {
        return pdf2 != null;
    }

    /**
     * {@code ListNumbering} of a list; {@code dash} and {@code image} have no equivalent in PDF/UA-1 and are
     * {@code None}, and in PDF/UA-2 they are {@code Unordered}, from PDF 2.0.
     */
    String listNumbering(Marker marker) {
        return switch (marker) {
            case BULLET -> PDListAttributeObject.LIST_NUMBERING_DISC;
            case DASH, IMAGE -> pdf2 != null ? "Unordered" : PDListAttributeObject.LIST_NUMBERING_NONE;
            case NONE -> PDListAttributeObject.LIST_NUMBERING_NONE;
            case DECIMAL -> PDListAttributeObject.LIST_NUMBERING_DECIMAL;
            case LOWER_ALPHA -> PDListAttributeObject.LIST_NUMBERING_LOWER_ALPHA;
            case UPPER_ALPHA -> PDListAttributeObject.LIST_NUMBERING_UPPER_ALPHA;
            case LOWER_ROMAN -> PDListAttributeObject.LIST_NUMBERING_LOWER_ROMAN;
            case UPPER_ROMAN -> PDListAttributeObject.LIST_NUMBERING_UPPER_ROMAN;
        };
    }

    /**
     * Whether the annotation of a {@code Link} read by its {@code Alt} loses its own text ({@code Contents}): with
     * PDF/UA-2, what is read is the {@code Alt}, and the two would have to match.
     */
    boolean stripContents() {
        return pdf2 != null;
    }

    /**
     * Whether what leads to a box (a link, a bookmark) is a structure destination ({@code SD}, PDF 2.0), with the page
     * destination for the viewers that do not understand it; with PDF/UA-1, the page destination alone.
     */
    boolean structureDestinations() {
        return pdf2 != null;
    }
}
