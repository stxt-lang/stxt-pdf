package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.Reference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The references from the texts of the tree to other boxes ({@code {page of: id}}, reference/common/references.md):
 * where they are and the check that all of them point to something that can be referenced. Their value is decided by
 * whoever lays out: the page, by the {@link dev.stxt.pdf.layout.Paginator}; the number, by the builder
 * ({@link Box#number()}).
 */
public final class References {

    /** A reference inside the text of a box. */
    public record Site(Box box, Reference reference) {

        /** Line of the document where the reference is. */
        public int line() {
            return box.textLineAt(reference.start());
        }
    }

    private References() {
    }

    /** All the references in the texts of a tree, in document order. */
    public static List<Site> sites(Box root) {
        List<Site> sites = new ArrayList<>();
        collect(root, sites);
        return sites;
    }

    private static void collect(Box box, List<Site> sites) {
        if (box.text() != null) {
            for (Reference r : Reference.find(box.text())) {
                sites.add(new Site(box, r));
            }
        }
        box.children().forEach(child -> collect(child, sites));
    }

    /**
     * The boxes of a tree that can be referenced (with {@code Id}, or headings of a table of contents), by their
     * {@link Box#key}.
     */
    public static Map<String, Box> byId(Box root) {
        Map<String, Box> boxes = new LinkedHashMap<>();
        index(root, boxes);
        return boxes;
    }

    private static void index(Box box, Map<String, Box> boxes) {
        if (box.key() != null) {
            boxes.put(box.key(), box);
        }
        box.children().forEach(child -> index(child, boxes));
        box.rows().forEach(row -> index(row, boxes));
    }

    /**
     * Checks that each reference points to a box with that {@code Id} that can be referenced: the page, of a box that
     * is not in a {@code Page overlay} (which is on every page), and the number, of one that has a number.
     *
     * @throws InvalidModelException with the line of the reference
     */
    static void validate(Box root) {
        List<Site> sites = sites(root);
        if (sites.isEmpty()) {
            return;
        }
        Map<String, Box> boxes = byId(root);
        for (Site site : sites) {
            Reference r = site.reference();
            String text = text(r);
            Box target = boxes.get(r.id());
            if (r.id().isEmpty()) {
                throw new InvalidModelException(site.line(), ErrorCode.REFERENCE_UNKNOWN, text
                        + " needs the Id of a box: {" + r.kind().keyword() + " of: id}");
            }
            if (target == null) {
                throw new InvalidModelException(site.line(), ErrorCode.REFERENCE_UNKNOWN, text
                        + ": no box has the Id " + r.id());
            }
            switch (r.kind()) {
                case PAGE -> {
                    if (insidePageOverlay(target)) {
                        throw new InvalidModelException(site.line(), ErrorCode.REFERENCE_NOT_PLACEABLE, text
                                + ": " + r.id() + " is in a Page overlay, which is repeated on every page");
                    }
                }
                case NUMBER -> {
                    if (target.number() == null) {
                        throw new InvalidModelException(site.line(), ErrorCode.REFERENCE_NOT_NUMBERED, text
                                + ": " + r.id() + " has no number (only the items of a numbered list "
                                + "and the headings with Numbered: true have one)");
                    }
                }
            }
        }
    }

    /** How the reference was written, for messages. */
    private static String text(Reference r) {
        return "{" + r.kind().keyword() + " of: " + r.id() + "}";
    }

    /** Whether the box is a {@code Page overlay} or is inside one. */
    static boolean insidePageOverlay(Box box) {
        for (Box b = box; b != null; b = b.parent()) {
            if (b.style().position() == Position.PAGE_OVERLAY) {
                return true;
            }
        }
        return false;
    }
}
