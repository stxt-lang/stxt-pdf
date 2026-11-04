package dev.stxt.pdf.paint;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDNumberTreeNode;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkedContentReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDObjectReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDParentTreeValue;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;

import java.util.HashMap;
import java.util.Map;

/**
 * The parent tree of the tagged PDF (ISO 32000-1, 14.7.4.4): the way back from what is on a page to the structure.
 * Each page has a key ({@code StructParents}) with the element each marked content of its stream ({@code MCID})
 * belongs to, and each link annotation its own key ({@code StructParent}) with its {@code Link}. The keys follow the
 * order in which the pages are painted and the annotations tagged. {@link PdfStructure} starts each page and records
 * its marked content; {@link PdfStructureLinks}, the annotations.
 */
final class PdfParentTree {

    /** Per key, the elements of the marked content of a page, or the {@code Link} of an annotation. */
    private final Map<Integer, PDParentTreeValue> parents = new HashMap<>();
    /** The page being painted. */
    private PDPage page;
    /** The elements of the marked content of the current page, by {@code MCID}. */
    private COSArray pageParents;

    /** The page being painted, since {@link #startPage}. */
    PDPage page() {
        return page;
    }

    /** Starts the content of a page: it gets the next key. */
    void startPage(PDPage page) {
        this.page = page;
        pageParents = new COSArray();
        int key = parents.size();
        page.setStructParents(key);
        parents.put(key, new PDParentTreeValue(pageParents));
    }

    /** A marked content of the current page that hangs from {@code e}; its {@code MCID}. */
    int mark(PDStructureElement e) {
        int mcid = pageParents.size();
        pageParents.add(e);
        PDMarkedContentReference ref = new PDMarkedContentReference();
        ref.setMCID(mcid);
        ref.setPage(page);
        e.appendKid(ref);
        return mcid;
    }

    /**
     * An annotation of the current page that hangs from {@code link} as an object reference ({@code OBJR}); the
     * annotation is visible from the parent tree with its own key, next to those of the pages.
     */
    void annotation(PDAnnotationLink annotation, PDStructureElement link) {
        PDObjectReference reference = new PDObjectReference();
        reference.setReferencedObject(annotation);
        reference.setPage(page);
        link.appendKid(reference);
        int key = parents.size();
        annotation.setStructParent(key);
        parents.put(key, new PDParentTreeValue(link.getCOSObject()));
    }

    /** Writes the tree in the root of the structure, once every page is painted. */
    void write(PDStructureTreeRoot treeRoot) {
        PDNumberTreeNode parentTree = new PDNumberTreeNode(PDParentTreeValue.class);
        parentTree.setNumbers(parents);
        treeRoot.setParentTree(parentTree);
        treeRoot.setParentTreeNextKey(parents.size());
    }
}
