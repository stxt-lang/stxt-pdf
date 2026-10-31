package dev.stxt.pdf.tree;

import dev.stxt.Node;
import dev.stxt.pdf.model.TextRun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.stxt.pdf.tree.Nodes.error;

/**
 * State of a call to {@link BoxTreeBuilder#build}: what becomes known about the document
 * while the tree is built, in order. One per document, so the builder keeps no
 * state between calls.
 */
final class BuildContext {

    /** The {@code Styles} rules of the document. */
    final StyleRules rules;
    /** Line of the {@code Id} of each box, to detect duplicates. */
    final Map<String, Integer> ids = new HashMap<>();
    /**
     * Whether the height of the content box is definite: base of vertical percentages (reference/common/values.md).
     */
    final Map<Box, Boolean> definiteHeight = new HashMap<>();
    /** Number of each {@code List item} already built: the next one continues from it. */
    final Map<Box, Integer> itemNumbers = new HashMap<>();
    /** Level of the last heading, in document order; 0 before the first. */
    int headingLevel;
    /** The counters of the heading numbering ({@code Numbered}, reference/components/heading.md). */
    final HeadingNumbers headingNumbers = new HeadingNumbers();
    /** Whether the content of a {@code Page overlay} is being built. */
    boolean insidePageOverlay;
    /** Whether the content of a {@code Columns} is being built. */
    boolean insideColumns;
    /**
     * The box that is not split across pages ({@code an Overlay}, {@code a Page overlay} or {@code a rotated Frame})
     * whose content is being built, or {@code null}: inside it, a {@code Break} has no effect, except in the
     * columns of a {@code Columns} (reference/common/breaks.md).
     */
    String unbreakable;
    /**
     * Whether the content of a {@code Markdown} is being built: its texts come already interpreted
     * (reference/components/markdown.md).
     */
    boolean insideMarkdown;
    /**
     * The {@code Table of contents} of the document, in order: their entries are generated at the end, with all the
     * headings.
     */
    final List<Box> contents = new ArrayList<>();
    /**
     * Runs of the formatted texts that {@link MarkdownBuilder} generates, by node
     * (reference/common/text-formatting.md).
     */
    final Map<Node, List<TextRun>> runs = new IdentityHashMap<>();
    /**
     * The {@code Text} that {@link MarkdownBuilder} generates as code blocks: their element is {@code Code block}
     * (reference/components/markdown.md).
     */
    final Set<Node> codeBlocks = Collections.newSetFromMap(new IdentityHashMap<>());

    BuildContext(StyleRules rules) {
        this.rules = rules;
    }

    /** The {@code Id} of a box, which is not repeated in the document. */
    String id(Node p) {
        String id = p.getText().trim();
        Integer previous = ids.putIfAbsent(id, p.getLine());
        if (previous != null) {
            throw error(p, ErrorCode.DUPLICATE_ID, "Duplicate Id: " + id + " (already used on line " + previous + ")");
        }
        return id;
    }

    /** Whether the height of the content box is definite; the box is already built. */
    boolean isDefinite(Box box) {
        return definiteHeight.get(box);
    }
}
