package dev.stxt.pdf.paint;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Rotation;
import dev.stxt.pdf.tree.Box;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;

import java.awt.geom.AffineTransform;
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
            find(page.content(), area.x(), area.y(), new AffineTransform(), pdfPages.get(i), lefts[i], tops[i],
                    targets);
            // The notes are not in the flow: they are in the footnote area, relative to the page area like the flow.
            Fragment footnotes = page.footnotes();
            if (footnotes != null) {
                find(footnotes, area.x() + footnotes.x(), area.y() + footnotes.y(), new AffineTransform(),
                        pdfPages.get(i), lefts[i], tops[i], targets);
            }
        }
        return targets;
    }

    /**
     * {@code x}, {@code y}: the corner of the border box of the fragment on the page, unrotated;
     * {@code turn}, the rotations of its ancestors (reference/common/box.md). The corner of a rotated box, or of one
     * inside a rotated one, is that of the rectangle that encloses what is painted. {@code left} and {@code top},
     * where the top left corner of the page is on the PDF page.
     */
    private static void find(Fragment f, double x, double y, AffineTransform turn, PDPage page, double left,
                             double top, Map<Box, PDPageXYZDestination> targets) {
        Box box = f.box();
        Rect borderBox = f.borderBox(x, y);
        Rotation rotation = box.style().rotation();
        if (rotation != null) {
            turn = new AffineTransform(turn);
            turn.concatenate(rotation.transform(borderBox));
        }
        if (f.first() && (box.headingLevel() > 0 || box.key() != null) && !targets.containsKey(box)) {
            Rect painted = turn.isIdentity() ? borderBox : Rotation.envelope(turn, borderBox);
            PDPageXYZDestination d = new PDPageXYZDestination();
            d.setPage(page);
            d.setLeft((int) Math.floor(left + painted.x()));
            d.setTop((int) Math.ceil(top - painted.y()));
            targets.put(box, d);
        }
        Rect content = borderBox.shrink(f.border()).shrink(f.padding());
        for (Fragment c : f.children()) {
            find(c, content.x() + c.x(), content.y() + c.y(), turn, page, left, top, targets);
        }
    }
}
