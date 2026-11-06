package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.Reference;
import dev.stxt.pdf.tree.Box;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PageMode;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * The bookmarks (<em>outline</em>) of the PDF (reference/implementation/pdf.md, "Bookmarks"): one per heading, nested
 * by level (the hierarchy is correct, reference/common/errors.md), that lead to the top left corner of their first
 * fragment ({@link PdfDestinations}). The catalog asks the viewer to show them when it opens the document.
 */
final class PdfOutline {

    private PdfOutline() {
    }

    /**
     * Writes the bookmarks of {@code headings}, in document order, with the destination of each one in
     * {@code destinations}. {@code goTo} gives the action that leads to a heading instead of its destination, or
     * {@code null} to use the destination itself ({@link PdfStructure#goTo}: with PDF/UA-2, the action with the
     * structure destination). Without headings there is no outline.
     */
    static void write(PDDocument doc, List<Box> headings, Map<Box, PDPageXYZDestination> destinations,
                      BiFunction<Box, PDPageXYZDestination, PDActionGoTo> goTo) {
        if (headings.isEmpty()) {
            return;
        }
        PDDocumentOutline outline = new PDDocumentOutline();
        // Stack of the open headings: the parent of each one is the last one of a lower level.
        Deque<PDOutlineItem> open = new ArrayDeque<>();
        Deque<Integer> levels = new ArrayDeque<>();
        for (Box h : headings) {
            while (!levels.isEmpty() && levels.peek() >= h.headingLevel()) {
                open.pop();
                levels.pop();
            }
            PDOutlineNode parent = open.isEmpty() ? outline : open.peek();
            PDOutlineItem item = new PDOutlineItem();
            item.setTitle(Reference.shown(h.headingText()));
            PDActionGoTo action = goTo.apply(h, destinations.get(h));
            if (action != null) {
                item.setAction(action);
            } else {
                item.setDestination(destinations.get(h));
            }
            parent.addLast(item);
            open.push(item);
            levels.push(h.headingLevel());
        }
        outline.openNode();
        PDDocumentCatalog catalog = doc.getDocumentCatalog();
        catalog.setDocumentOutline(outline);
        catalog.setPageMode(PageMode.USE_OUTLINES);
    }
}
