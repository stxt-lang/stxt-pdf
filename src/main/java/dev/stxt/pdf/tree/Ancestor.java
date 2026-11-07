package dev.stxt.pdf.tree;

import java.util.Set;

/**
 * An ancestor in the document of the element being built, for the nested rules of {@code Styles}
 * (reference/common/styles.md): a rule written inside another applies to the elements that are inside an element the
 * outer one applies to. They are the ancestors as written: the {@code Section}, the elements and the {@code Row} of a
 * cell, with their {@code Style} and {@code Id}; {@code Item} and {@code Area} are in between and count for nothing.
 *
 * @param kind    the element, {@link Element#SECTION} or {@link Element#ROW}
 * @param classes the names of its {@code Style}; empty without one
 * @param id      its {@code Id}; {@code null} without one
 */
record Ancestor(Element kind, Set<String> classes, String id) {
}
