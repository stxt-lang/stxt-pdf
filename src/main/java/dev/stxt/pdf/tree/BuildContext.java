package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.pdf.model.Cancellation;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.TextRun;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static dev.stxt.pdf.tree.Nodes.error;

/**
 * State of a call to {@link BoxTreeBuilder#build}: what becomes known about the document
 * while the tree is built, in order. One per document, so the builder keeps no
 * state between calls.
 */
final class BuildContext {

    /** The {@code Styles} rules of the document. */
    final StyleRules rules;
    /** Whether the conversion has to stop ({@link Cancellation}); asked at each element, before building it. */
    final Cancellation cancellation;
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
    /** The counters of the figures and the tables (reference/components/figure.md). */
    final Figures figureNumbers = new Figures();
    /** The {@code Figure} boxes of the document, in order: the tables of contents of figures and tables list them. */
    final List<Box> figures = new ArrayList<>();
    /** Where the content being built is; it changes only with {@link #within}. */
    private Scope scope = Scope.DOCUMENT;
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
    /** The footnotes: their calls, their boxes and their numbering (reference/components/footnote.md). */
    final Footnotes footnotes = new Footnotes();
    /**
     * The properties that the content of an element reads from its node ({@link PropertyNode}) and that the rules of
     * {@code Styles} give it, by node of the element and canonical name; only those the element does not declare.
     */
    final Map<Node, Map<String, PropertyNode>> ruleProperties = new IdentityHashMap<>();
    /**
     * The property nodes that {@link MarkdownBuilder} generates from a property of a rule of a separate styles
     * document (the {@code Marker} of a list, from {@code Bullet markers}): their line is of that document.
     */
    final Map<Node, SourceLine> sources = new IdentityHashMap<>();
    /**
     * The ancestors in the document of the content being built, the closest first, for the nested rules of
     * {@code Styles} ({@link Ancestor}); it changes only with {@link #inside}.
     */
    private final ArrayDeque<Ancestor> ancestors = new ArrayDeque<>();

    BuildContext(StyleRules rules, Cancellation cancellation) {
        this.rules = rules;
        this.cancellation = cancellation;
    }

    /**
     * Where the content being built is: inside which boxes, among those that change what their descendants can
     * declare or write.
     *
     * @param inPageOverlay inside a {@code Page overlay}
     * @param inHeading     in the texts of a heading: a footnote call is not allowed in them
     * @param inColumns     inside a {@code Columns}
     * @param inMarkdown    inside a {@code Markdown}: its texts come already interpreted
     *                      (reference/components/markdown.md)
     * @param inFootnote    inside a {@code Footnote}
     * @param unbreakable   the box that is not split across pages ({@link Style#unsplittable()}) whose content is
     *                      being built, or {@code null}: inside it, a {@code Break} has no effect, except in the columns
     *                      of a {@code Columns} and in a {@code Footnote} (reference/common/breaks.md)
     */
    record Scope(boolean inPageOverlay, boolean inHeading, boolean inColumns, boolean inMarkdown, boolean inFootnote,
                 Style.Unsplittable unbreakable) {

        /** The content of a section, outside any element. */
        static final Scope DOCUMENT = new Scope(false, false, false, false, false, null);

        /** The content of an element with the style {@code s}; {@code heading} if it is a heading. */
        Scope element(Style s, boolean heading) {
            return new Scope(inPageOverlay || s.position() == Position.PAGE_OVERLAY, inHeading || heading, inColumns,
                    inMarkdown, inFootnote, unbreakable != null ? unbreakable : s.unsplittable());
        }

        /** The column flow of a {@code Columns}: its columns are split, whatever box holds it. */
        Scope columns() {
            return new Scope(inPageOverlay, inHeading, true, inMarkdown, inFootnote, null);
        }

        /** The elements that a {@code Markdown} generates. */
        Scope markdown() {
            return new Scope(inPageOverlay, inHeading, inColumns, true, inFootnote, unbreakable);
        }

        /**
         * The content of a {@code Footnote}, which is laid out apart from the box that holds it: what that box forbids
         * does not reach it.
         */
        Scope footnote() {
            return new Scope(inPageOverlay, inHeading, false, inMarkdown, true, null);
        }
    }

    /** Where the content being built is. */
    Scope scope() {
        return scope;
    }

    /** Builds {@code content} in {@code inner}, and goes back to the scope it was in. */
    void within(Scope inner, Runnable content) {
        Scope outer = scope;
        scope = inner;
        try {
            content.run();
        } finally {
            scope = outer;
        }
    }

    /** The ancestors of the content being built, the closest first; empty in the content of a section. */
    List<Ancestor> ancestors() {
        return List.copyOf(ancestors);
    }

    /** Builds {@code content} with {@code ancestor} as its closest ancestor, and goes back to the ancestors it had. */
    void inside(Ancestor ancestor, Runnable content) {
        inside(ancestor, () -> {
            content.run();
            return null;
        });
    }

    /** Like {@link #inside(Ancestor, Runnable)}, with what {@code content} gives. */
    <T> T inside(Ancestor ancestor, Supplier<T> content) {
        ancestors.push(ancestor);
        try {
            return content.get();
        } finally {
            ancestors.pop();
        }
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

    /**
     * A property that the content of {@code n} reads from its node ({@code column-align}, {@code bullet-markers}...):
     * the one it declares or, if it does not, the one its rules give it (reference/common/styles.md).
     */
    Optional<PropertyNode> property(InlineNode n, String name) {
        Optional<PropertyNode> own = Nodes.child(n, name).map(p -> new PropertyNode(p, null));
        return own.isPresent() ? own
                : Optional.ofNullable(ruleProperties.getOrDefault(n, Map.of()).get(name));
    }

    /** Whether the height of the content box is definite; the box is already built. */
    boolean isDefinite(Box box) {
        return definiteHeight.get(box);
    }
}
