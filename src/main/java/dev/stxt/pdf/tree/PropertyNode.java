package dev.stxt.pdf.tree;

import dev.stxt.Node;

/**
 * A property that the content of an element reads from its node ({@code Column align} of a {@code Table}, the markers
 * and {@code Table column sizes} of a {@code Markdown}): the one the element declares or, if it does not, the one a rule
 * of {@code Styles} gives it (reference/common/styles.md), with the document its line is in.
 *
 * @param node   the node of the property
 * @param source the separate styles document of the rule it is in; {@code null} if it is of the document
 */
record PropertyNode(Node node, SourceLine source) {

    /** The line of the property, with its document. */
    SourceLine line() {
        return source != null ? source.at(node.getLine()) : SourceLine.of(node.getLine());
    }

    /** An error on the line of the property: if it is in a separate styles document, it says so. */
    InvalidModelException located(InvalidModelException e) {
        return source != null ? e.in(source) : e;
    }
}
