package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.Tolerance;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.model.ValueParser;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static dev.stxt.pdf.tree.Nodes.child;
import static dev.stxt.pdf.tree.Nodes.error;
import static dev.stxt.pdf.tree.Nodes.parse;
import static dev.stxt.pdf.tree.ValueReader.enumValue;
import static dev.stxt.pdf.tree.ValueReader.points;

/**
 * What the document says about itself and its pages: the {@code Page} of the document and of each {@code Section}
 * (reference/components/document.md) and the {@code Title} and the {@code Language} (reference/components/document.md).
 */
final class PageSetupReader {

    private PageSetupReader() {
    }

    /**
     * A medium and the named paper it comes from, if it comes from one: a section can rotate it
     * (reference/components/document.md).
     */
    record Spec(PageSetup pageSetup, PageSetup.Paper paper) {
    }

    /** {@code Title} and {@code Language} of {@code Document}: a non-empty title and a BCP 47 tag. */
    static Metadata metadata(InlineNode document) {
        String title = child(document, "title").map(n -> {
            String t = n.getText().trim();
            if (t.isEmpty()) {
                throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Title needs a text");
            }
            return t;
        }).orElse(null);
        String language = child(document, "language").map(ValueReader::language).orElse(null);
        return new Metadata(title, language);
    }

    /**
     * The medium (reference/components/document.md): its size is a {@code Paper} with its {@code Orientation} or a
     * {@code Width} and a {@code Height}, never both; the schema only knows that each one appears once. The one of a
     * section inherits by groups the one of the document, {@code base} ({@code null} in the one of the document): the
     * size, and the margins with their {@code Mirror margins}.
     */
    static Spec pageSetup(InlineNode n, Spec base) {
        Optional<Node> orientation = child(n, "orientation");
        Optional<Node> widthNode = child(n, "width");
        Optional<Node> heightNode = child(n, "height");
        PageSetup.Paper named = paper(n, base);
        double width;
        double height;
        if (named != null) {
            boolean landscape = orientation.map(o -> enumValue(PageSetup.Orientation.class, o))
                    .orElse(PageSetup.Orientation.PORTRAIT) == PageSetup.Orientation.LANDSCAPE;
            width = landscape ? named.height : named.width;
            height = landscape ? named.width : named.height;
        } else if (base != null && widthNode.isEmpty() && heightNode.isEmpty() && orientation.isEmpty()) {
            // The size is the one of the document.
            width = base.pageSetup().width();
            height = base.pageSetup().height();
            named = base.paper();
        } else {
            if (orientation.isPresent()) {
                throw error(orientation.get(), ErrorCode.PAGE_SIZE, "Orientation rotates the paper of Paper, and the "
                        + "document does not declare it: Paper is needed, or the Width and the Height swapped");
            }
            if (widthNode.isEmpty() || heightNode.isEmpty()) {
                throw error(widthNode.or(() -> heightNode).orElse(n), ErrorCode.PAGE_SIZE, "Page needs "
                        + "its size: a Paper (A4, Letter...) or a Width and a Height");
            }
            width = points(widthNode.get());
            height = points(heightNode.get());
        }
        // The margins are a group too: Margin with its Mirror margins, inherited together or declared together.
        Optional<Node> marginNode = child(n, "margin");
        Optional<Node> mirrorNode = child(n, "mirror-margins");
        Insets margin;
        boolean mirror;
        if (marginNode.isPresent() || base == null) {
            margin = marginNode.map(PageSetupReader::margin).orElse(Insets.ZERO);
            mirror = mirrorNode.map(ValueReader::bool).orElse(false);
        } else if (mirrorNode.isPresent()) {
            throw error(mirrorNode.get(), ErrorCode.PAGE_SIZE, "Mirror margins goes with Margin: the section inherits "
                    + "both from the document or declares both");
        } else {
            margin = base.pageSetup().margin();
            mirror = base.pageSetup().mirrorMargins();
        }
        if (mirror && Tolerance.same(margin.left(), margin.right())) {
            throw error(mirrorNode.orElseThrow(), ErrorCode.MIRROR_MARGINS_WITHOUT_EFFECT, "Mirror margins: true has "
                    + "no effect with equal left and right margins (" + pt(margin.left()) + "): the even pages swap "
                    + "them, so the margins have to differ, or Mirror margins is not needed");
        }
        PageSetup pageSetup = new PageSetup(width, height, margin, mirror);
        if (pageSetup.pageArea().width() <= 0 || pageSetup.pageArea().height() <= 0) {
            throw error(marginNode.orElse(n), ErrorCode.PAGE_SIZE, "The margins of the page leave no page area");
        }
        return new Spec(pageSetup, named);
    }

    private static String pt(double v) {
        return String.format(Locale.ROOT, "%.2f", v).replaceAll("\\.?0+$", "") + " pt";
    }

    /**
     * The named paper of the medium: its {@code Paper} or, in a section that only declares {@code Orientation}, the
     * one of the document; {@code null} if the size does not come from a paper.
     */
    private static PageSetup.Paper paper(InlineNode n, Spec base) {
        Optional<Node> paper = child(n, "paper");
        Optional<Node> orientation = child(n, "orientation");
        Optional<Node> widthNode = child(n, "width");
        Optional<Node> heightNode = child(n, "height");
        if (paper.isPresent()) {
            Optional<Node> both = widthNode.isPresent() ? widthNode : heightNode;
            if (both.isPresent()) {
                throw error(both.get(), ErrorCode.PAGE_SIZE, "Page declares Paper and " + both.get().getName()
                        + ": the size of the page is one of the two, Paper (with Orientation) or Width and Height");
            }
            return enumValue(PageSetup.Paper.class, paper.get());
        }
        if (base != null && orientation.isPresent() && widthNode.isEmpty() && heightNode.isEmpty()) {
            // A section that only rotates the paper of the document.
            if (base.paper() == null) {
                throw error(orientation.get(), ErrorCode.PAGE_SIZE, "Orientation rotates the paper of Paper, and the "
                        + "document does not declare it (it gives Width and Height): Paper is declared in the section or in the document");
            }
            return base.paper();
        }
        return null;
    }

    private static Insets margin(Node n) {
        List<Double> v = parse(n, () -> ValueParser.list(n.getText())).stream().map(x -> {
            if (!(x instanceof Value.Points p)) {
                throw error(n, ErrorCode.PAGE_SIZE, "The margin of the page only allows lengths in points or "
                        + "millimeters: " + n.getText().trim());
            }
            return p.pt();
        }).toList();
        Sides<Double> s = parse(n, () -> Sides.shorthand(v));
        return new Insets(s.top(), s.right(), s.bottom(), s.left());
    }
}
