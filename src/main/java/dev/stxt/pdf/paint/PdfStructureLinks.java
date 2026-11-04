package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.tree.Box;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureNode;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The {@code Link} elements of the structure tree and the link annotations that hang from them
 * (reference/implementation/pdf.md, "Links"): PDF/UA asks that every link annotation be in a {@code Link}, as an
 * object reference ({@code OBJR}) with its key in the parent tree ({@link PdfParentTree}).
 *
 * <p>While {@link PdfStructure} builds the tree, it asks here for the {@code Link} of a box with {@code Link}
 * ({@link #wrap}, {@link #newLink}) and for the {@code Link} elements that stand in for what is not content
 * ({@link #standIns}). While painting, the text of a box with {@code Link} hangs from its {@code Link}
 * ({@link #holder}), a text link opens its own {@code Link} ({@link #beginInline}) and each clickable area goes to the
 * {@code Link} it belongs to ({@link #link}, {@link #inlineLink}): that of the box, the one being painted, the one that
 * stands in for a box that is not content or, in the repeated rows of a table, that of the first page.</p>
 */
final class PdfStructureLinks {

    private final UaProfile profile;
    private final PdfParentTree parentTree;

    /** The {@code Link} of each box with {@code Link}: it holds its content and the annotations of its area. */
    private final Map<Box, PDStructureElement> boxLinks = new HashMap<>();
    /** Where the own content (the text) of the boxes with {@code Link} hangs: their {@code Link}. */
    private final Map<Box, PDStructureElement> holders = new HashMap<>();
    /** The {@code Link} elements of boxes without text or figure, read by their {@code Alt}, not by their content. */
    private final Set<PDStructureElement> emptyLinks = new HashSet<>();
    /** The {@code Link} of the text link being painted, to hang the annotation of its area from it. */
    private PDStructureElement inline;
    /**
     * The {@code Link} elements of the links of what is not content (a {@code Page overlay}, a decorative image), by
     * box and destination: they are in the place of the box in the tree, without content, and receive the annotations
     * of all the pages.
     */
    private final Map<Box, Map<String, PDStructureElement>> standIns = new HashMap<>();
    /**
     * The {@code Link} elements of the text links of the boxes that repeat (the header and the footer of a table), by
     * box and destination: those of the first page, which receive the annotations of the repetitions.
     */
    private final Map<Box, Map<String, PDStructureElement>> firstLinks = new HashMap<>();

    PdfStructureLinks(UaProfile profile, PdfParentTree parentTree) {
        this.profile = profile;
        this.parentTree = parentTree;
    }

    /**
     * The {@code Link} elements of the links of a box that is not content and of what it contains, in its place in
     * the tree: that of the box ({@code Link} of the element) and one per destination of its text links, in order.
     */
    void standIns(Box box, PDStructureNode parent) {
        if (box.style().link() != null) {
            standIn(box, box.style().link(), parent);
        }
        if (box.formattedRuns() != null) {
            for (var run : box.formattedRuns()) {
                if (run.format().link() != null) {
                    standIn(box, run.format().link(), parent);
                }
            }
        }
        box.children().forEach(child -> standIns(child, parent));
    }

    private void standIn(Box box, String destination, PDStructureNode parent) {
        standIns.computeIfAbsent(box, k -> new HashMap<>()).computeIfAbsent(destination, k -> {
            PDStructureElement link = profile.element("Link", parent);
            parent.appendKid(link);
            return link;
        });
    }

    /**
     * If the box has {@code Link} (reference/implementation/pdf.md), the {@code Link} that wraps its content (its
     * text and its children) and that receives the annotations of its area; otherwise, {@code content}. An image is
     * the exception: its {@code Link} wraps its {@code Figure} ({@link #newLink}).
     */
    PDStructureNode wrap(Box box, PDStructureNode content) {
        if (box.style().link() == null || box.isImage()) {
            return content;
        }
        PDStructureElement link = newLink(box, content);
        holders.put(box, link);
        return link;
    }

    /** A {@code Link} child of {@code parent} for the box. */
    PDStructureElement newLink(Box box, PDStructureNode parent) {
        PDStructureElement link = profile.element("Link", parent);
        parent.appendKid(link);
        boxLinks.put(box, link);
        if (!hasContent(box)) {
            emptyLinks.add(link);
        }
        return link;
    }

    /**
     * Whether the box or what it contains has something to read: text, a marker (with text, or an image with
     * {@code Alt}) or an image with {@code Alt}.
     */
    private static boolean hasContent(Box box) {
        if (box.hasText() || box.marker() != null && (box.marker().hasText() || box.marker().style().image().alt() != null)
                || box.isImage() && box.style().image().alt() != null) {
            return true;
        }
        return box.children().stream().anyMatch(c -> c.style().position() != Position.PAGE_OVERLAY && hasContent(c));
    }

    /** Where the own content of the box hangs if it has {@code Link}: its {@code Link}; otherwise, {@code null}. */
    PDStructureElement holder(Box box) {
        return holders.get(box);
    }

    /**
     * Starts a text link, or an image in a link, inside the text of a box: a new {@code Link}, child of
     * {@code holder} (the element of the text, or its {@code Link}) on the current page, which the annotations of its
     * areas go to ({@link #inlineLink}) until the next one starts. {@code empty}, if nothing to read will hang from it
     * (a decorative image), so that it is read by its {@code Alt}. With {@code holder} {@code null} nothing starts:
     * the text is not content, and the annotations of its areas go to the {@code Link} that stands in for it.
     */
    PDStructureElement beginInline(PDStructureElement holder, boolean empty) {
        inline = null;
        if (holder == null) {
            return null;
        }
        PDStructureElement link = profile.element("Link", holder);
        link.setPage(parentTree.page());
        holder.appendKid(link);
        if (empty) {
            emptyLinks.add(link);
        }
        inline = link;
        return link;
    }

    /**
     * Tags the area of a box with {@code Link} (reference/implementation/pdf.md): the annotation is an object
     * reference ({@code OBJR}) of the {@code Link} that wraps the content of the box. {@code element} is the element
     * of the box, or {@code null} if it has none. A {@code Link} with nothing to read inside carries its text as
     * {@code Alt}. Where the box is not content, the link goes to the {@code Link} that stands in for it in its place
     * ({@link #standIns}).
     */
    void link(Box box, PDStructureElement element, String destination, PDAnnotationLink annotation,
              String description) {
        if (element == null) {
            outside(box, destination, annotation, description);
            return;
        }
        PDStructureElement link = boxLinks.get(box);
        if (link == null) {
            // A box that cannot wrap its content in a Link (a table): a separate Link, a child of it.
            link = profile.element("Link", element);
            link.setPage(parentTree.page());
            element.appendKid(link);
            emptyLinks.add(link);
            boxLinks.put(box, link);
        }
        if (emptyLinks.contains(link)) {
            describe(link, annotation, description);
        }
        attach(link, annotation);
    }

    /**
     * Tags the area of a line of the text link started by {@link #beginInline}; if the text is not content, it goes to
     * the {@code Link} of its box and its destination that stands in for it in its place ({@link #standIns}) or, in
     * the repeated rows of a table, to that of the first page.
     */
    void inlineLink(Box box, String destination, PDAnnotationLink annotation, String description) {
        if (inline != null) {
            firstLinks.computeIfAbsent(box, k -> new HashMap<>()).putIfAbsent(destination, inline);
            if (emptyLinks.contains(inline)) {
                describe(inline, annotation, description);
            }
            attach(inline, annotation);
            return;
        }
        PDStructureElement first = firstLinks.getOrDefault(box, Map.of()).get(destination);
        if (first != null) {
            attach(first, annotation);
        } else {
            outside(box, destination, annotation, description);
        }
    }

    /**
     * Tags the link of a box that is not content (in a {@code Page overlay}, a decorative image): PDF/UA
     * requires every link annotation to be in a {@code Link}. It is the {@code Link} of the box and its destination
     * placed in its place in the tree when building it, without marked content and with the text of the link as
     * {@code Alt}: a screen reader announces it once, in its reading order, even if it repeats on every page (a footer
     * with a website).
     */
    private void outside(Box box, String destination, PDAnnotationLink annotation, String description) {
        PDStructureElement link = standIns.getOrDefault(box, Map.of()).get(destination);
        if (link == null) {
            throw new IllegalStateException("Link without a Link element in the structure: " + destination);
        }
        describe(link, annotation, description);
        attach(link, annotation);
    }

    /**
     * The {@code Alt} of a {@code Link} with nothing to read inside: the text of its first area. Where what is read
     * is the {@code Alt} ({@link UaProfile#stripContents}), the annotation does not also carry its text.
     */
    private void describe(PDStructureElement link, PDAnnotationLink annotation, String description) {
        if (link.getAlternateDescription() == null) {
            link.setAlternateDescription(description != null ? description : annotation.getContents());
        }
        if (profile.stripContents()) {
            annotation.getCOSObject().removeItem(COSName.CONTENTS);
        }
    }

    private void attach(PDStructureElement link, PDAnnotationLink annotation) {
        parentTree.annotation(annotation, link);
    }

    /** Finishes: a {@code Link} without annotations (that of a {@code Page overlay} on no page) cannot be left empty. */
    void finish() {
        for (Map<String, PDStructureElement> links : standIns.values()) {
            for (PDStructureElement link : links.values()) {
                if (link.getKids().isEmpty()) {
                    link.getParent().removeKid(link);
                }
            }
        }
    }
}
