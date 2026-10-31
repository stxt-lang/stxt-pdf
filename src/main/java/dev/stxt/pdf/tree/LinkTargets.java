package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Links;
import dev.stxt.pdf.model.TextFormat;

import java.util.Map;

/**
 * The links of the tree to other boxes ({@code #id}, reference/common/links.md): checks that each
 * one points to a box with that {@code Id} and that it has a single place to lead to. The syntax of the
 * target and its scheme are checked when it is read; this needs the whole tree, because a link
 * can point forward. Links to an outside URL are not checked any further.
 */
final class LinkTargets {

    private LinkTargets() {
    }

    /**
     * Checks the internal links of the box and of its descendants: those of {@code Link} and those
     * of the runs of its text.
     *
     * @throws InvalidModelException with the line of the {@code Link} or, in a run, that of its text
     */
    static void validate(Box root) {
        Map<String, Box> boxes = References.byId(root);
        check(root, boxes);
    }

    private static void check(Box box, Map<String, Box> boxes) {
        String link = box.style().link();
        if (link != null && Links.internal(link)) {
            SourceLine line = box.style().isDeclared("link") ? box.style().line("link") : SourceLine.of(box.line());
            target(link, line, boxes);
        }
        if (box.formattedRuns() != null) {
            for (var run : box.formattedRuns()) {
                TextFormat format = run.format();
                if (format.link() != null && Links.internal(format.link())) {
                    target(format.link(), SourceLine.of(box.textLine()), boxes);
                }
            }
        }
        box.children().forEach(child -> check(child, boxes));
    }

    private static void target(String destination, SourceLine line, Map<String, Box> boxes) {
        String id = Links.target(destination);
        Box target = boxes.get(id);
        if (target == null) {
            throw new InvalidModelException(line, ErrorCode.LINK_UNKNOWN, "The link " + destination
                    + " leads to an Id that does not exist: no box has the Id " + id);
        }
        if (References.insidePageOverlay(target)) {
            throw new InvalidModelException(line, ErrorCode.LINK_UNKNOWN, "The link " + destination + " leads to "
                    + id + ", which is in a Page overlay and is repeated on all the pages: it has no single target");
        }
    }
}
