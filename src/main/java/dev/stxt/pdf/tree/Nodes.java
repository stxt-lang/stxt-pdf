package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.pdf.model.Position;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Access to the {@code stxt-core} nodes and builder errors: shared by {@link BoxTreeBuilder} and the classes it
 * relies on.
 *
 * <p>Names are compared by their canonical name ({@link Node#getCanonicalName()}: lowercase, with spaces turned into
 * hyphens, so {@code Min width} is {@code min-width}). The structure is already validated against the schema, so the
 * required nodes exist and the ones with children are not text blocks.</p>
 */
final class Nodes {

    private Nodes() {
    }

    /** Compares with a name that is already canonical ({@code "min-width"}). */
    static boolean is(Node n, String canonicalName) {
        return n.getCanonicalName().equals(canonicalName);
    }

    /** Whether the node is a box element. */
    static boolean isElement(Node n) {
        return Element.of(n.getCanonicalName()).filter(Element::isWritten).isPresent();
    }

    /** Whether the node is the element of an {@code Item} or an {@code Area}. */
    static boolean isItemElement(Node n) {
        return Element.of(n.getCanonicalName()).filter(Element::isWritten).filter(Element::canBeItem).isPresent();
    }

    /** First child with that canonical name. */
    static Optional<Node> child(InlineNode n, String canonicalName) {
        return n.getChildren().stream().filter(h -> is(h, canonicalName)).findFirst();
    }

    /** A node with children: the schema does not allow it to be written as a text block ({@code >>}). */
    static InlineNode inline(Node n) {
        return (InlineNode) n;
    }

    /** Required child ({@code "Type"}), guaranteed by the schema. */
    static Node required(InlineNode n, String child) {
        return child(n, child.toLowerCase(Locale.ROOT)).orElseThrow();
    }

    /** Name of a property for messages: {@code min-width} → {@code Min width}. */
    static String label(String property) {
        String s = property.replace('-', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Name of the positioned element: {@code Overlay}, {@code Page overlay} or the footnote area, {@code Footnotes}. */
    static String position(Style s) {
        return switch (s.position()) {
            case PAGE_OVERLAY -> "Page overlay";
            case FOOTNOTES -> "Footnotes";
            default -> "Overlay";
        };
    }

    /** Parses a value; syntax errors carry the line of the node. */
    static <T> T parse(Node n, Supplier<T> parser) {
        try {
            return parser.get();
        } catch (IllegalArgumentException ex) {
            throw error(n, ErrorCode.VALUE_SYNTAX, ex.getMessage());
        }
    }

    /** Parses {@code Areas} or {@code Area}: its errors are placement errors and carry the line of the node. */
    static <T> T placement(Node n, Supplier<T> parser) {
        try {
            return parser.get();
        } catch (IllegalArgumentException ex) {
            throw error(n, ErrorCode.GRID_PLACEMENT, ex.getMessage());
        }
    }

    /**
     * A node that the schema allows and the builder does not read: an error of the engine, not of the document. A
     * property added to the schema has to be read in {@link PropertyReader} or in {@link BoxTreeBuilder} (checked by
     * {@code SchemaCoverageTest}).
     */
    static IllegalStateException unread(Node n) {
        return new IllegalStateException("The schema allows " + n.getName() + " (line " + n.getLine()
                + "), but the builder does not read it");
    }

    static InvalidModelException error(Node n, ErrorCode code, String message) {
        return error(n.getLine(), code, message);
    }

    static InvalidModelException error(int line, ErrorCode code, String message) {
        return new InvalidModelException(line, code, message);
    }

    /** An error on the line of a property, which can be in the separate styles document. */
    static InvalidModelException error(SourceLine line, ErrorCode code, String message) {
        return new InvalidModelException(line, code, message);
    }
}
