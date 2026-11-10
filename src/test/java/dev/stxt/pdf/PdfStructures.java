package dev.stxt.pdf;

import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkedContentReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDObjectReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;

/** Reading the structure tree of a tagged PDF (reference/implementation/pdf.md) in the tests. */
public final class PdfStructures {

    private PdfStructures() {
    }

    /** The {@code Document} element: the first kid of the structure tree root. */
    public static PDStructureElement document(PDDocument pdf) {
        PDStructureTreeRoot root = pdf.getDocumentCatalog().getStructureTreeRoot();
        return (PDStructureElement) root.getKids().get(0);
    }

    /** The first section of the document: the first kid of {@link #document}. */
    public static PDStructureElement section(PDDocument pdf) {
        return (PDStructureElement) document(pdf).getKids().get(0);
    }

    /** The structure types of the kids of {@code e} that are structure elements, in order. */
    public static List<String> types(PDStructureElement e) {
        return e.getKids().stream().filter(k -> k instanceof PDStructureElement)
                .map(k -> ((PDStructureElement) k).getStructureType()).toList();
    }

    /**
     * Every kid of {@code e}, in order: the structure type of an element, {@code MCR} for a marked content
     * reference, {@code OBJR} for an object reference (a link annotation) and the class name of anything else.
     */
    public static List<String> kids(PDStructureElement e) {
        return e.getKids().stream().map(k -> k instanceof PDStructureElement s ? s.getStructureType()
                : k instanceof PDMarkedContentReference ? "MCR"
                : k instanceof PDObjectReference ? "OBJR" : k.getClass().getSimpleName()).toList();
    }
}
