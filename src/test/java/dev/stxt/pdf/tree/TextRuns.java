package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;

import java.util.List;
import java.util.Map;

/**
 * Formatted runs for the tests (reference/common/text-formatting.md). The document does not have syntax to write
 * them yet: they are set in the already built tree, before laying it out.
 */
public final class TextRuns {

    private TextRuns() {
    }

    /** A run without formatting. */
    public static TextRun run(String text) {
        return new TextRun(text, TextFormat.PLAIN);
    }

    /** A run with the formatting of the letters of {@code format}: {@code b}, {@code i}, {@code u} and {@code s}. */
    public static TextRun run(String text, String format) {
        return new TextRun(text, new TextFormat(format.contains("b"), format.contains("i"), format.contains("u"),
                format.contains("s")));
    }

    /** A run with the formatting of {@code format} and a link to {@code destination}. */
    public static TextRun link(String text, String format, String destination) {
        return new TextRun(text, run(text, format).format().withLink(destination));
    }

    /** Builds the document tree and sets the runs of each text box by its {@code Id}. */
    public static BoxTreeBuilder.Result build(InlineNode document, Map<String, List<TextRun>> runs) {
        return new BoxTreeBuilder().build(document, null, root -> runs.forEach((id, r) -> set(root, id, r)));
    }

    /**
     * Sets the runs on the box with this {@code Id}, whose text must be that of the runs joined. Only before closing
     * the tree: inside the {@code finish} of
     * {@link BoxTreeBuilder#build(InlineNode, InlineNode, java.util.function.Consumer)}.
     */
    public static void set(Box root, String id, List<TextRun> runs) {
        Box box = find(root, id);
        if (box == null) {
            throw new IllegalArgumentException("There is no box with Id " + id);
        }
        if (!TextRun.text(runs).equals(box.text())) {
            throw new IllegalArgumentException("The runs are not the text of " + id + ": '" + box.text() + "'");
        }
        box.setRuns(List.copyOf(runs));
    }

    private static Box find(Box box, String id) {
        if (id.equals(box.id())) {
            return box;
        }
        for (Box child : box.children()) {
            Box found = find(child, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
