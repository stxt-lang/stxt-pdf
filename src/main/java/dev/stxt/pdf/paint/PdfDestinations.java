package dev.stxt.pdf.paint;

import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.tree.Box;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where the PDF takes whoever looks for a box: the top left corner of its first
 * fragment, on its page. Used by the bookmarks of the headings ({@link PdfStructure}) and the
 * links to a box of the document ({@link PdfLinks}, reference/common/links.md).
 *
 * <p>Headings and the boxes that can be pointed to ({@link Box#key()}: with {@code Id}, or the
 * headings a table of contents lists) have a destination, also a {@code Footnote}, whose destination is the note in
 * the footnote area of the page of its call (reference/components/footnote.md). {@code Page overlay} boxes do not:
 * they repeat on every page and have no single destination.</p>
 */
final class PdfDestinations {

    private PdfDestinations() {
    }

    /**
     * The destination of each box that has one, in document order. {@code pdfPages} are their
     * PDF pages, in the same order as {@code pages}; each one has the paper of its section.
     */
    static Map<Box, PDPageXYZDestination> find(List<Page> pages, List<PDPage> pdfPages) {
        double[] lefts = new double[pages.size()];
        double[] tops = new double[pages.size()];
        for (int i = 0; i < pages.size(); i++) {
            tops[i] = pages.get(i).pageSetup().height();
        }
        return find(pages, pdfPages, lefts, tops);
    }

    /**
     * Like {@link #find(List, List)}, with the top left corner of each page painted at ({@code lefts[i]},
     * {@code tops[i]}) of its PDF page, in PDF coordinates ({@link PdfPainter}).
     */
    static Map<Box, PDPageXYZDestination> find(List<Page> pages, List<PDPage> pdfPages, double[] lefts,
                                               double[] tops) {
        Map<Box, PDPageXYZDestination> targets = new LinkedHashMap<>();
        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);
            Rect area = page.pageSetup().pageArea();
            PDPage pdfPage = pdfPages.get(i);
            double left = lefts[i];
            double top = tops[i];
            // The same positions with which the page is painted, the notes of the footnote area included
            // (reference/components/footnote.md). The corner of a rotated box, or of one inside a rotated one, is that
            // of the rectangle that encloses what is painted (reference/common/box.md).
            StackingContext.build(page.content(), page.footnotes(), List.of(), area.x(), area.y())
                    .visit((f, borderBox, turns) -> {
                        Box box = f.box();
                        if (f.first() && (box.headingLevel() > 0 || box.key() != null) && !targets.containsKey(box)) {
                            Rect painted = turns.envelope(borderBox);
                            PDPageXYZDestination d = new PDPageXYZDestination();
                            d.setPage(pdfPage);
                            d.setLeft((int) Math.floor(left + painted.x()));
                            d.setTop((int) Math.ceil(top - painted.y()));
                            targets.put(box, d);
                        }
                    });
        }
        return targets;
    }
}
