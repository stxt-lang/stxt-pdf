package dev.stxt.pdf.tree;

/**
 * What {@link Style} and its property groups ({@link StackStyle}...) have in common: only {@code tree} writes them,
 * while {@link BoxTreeBuilder} builds the tree, and they are closed with their box ({@link Box#close}). Writing to a
 * closed one is an {@code IllegalStateException}: after the tree is built, a style is only read and can be shared
 * between threads.
 */
abstract class StyleGroup {

    private boolean closed;

    void close() {
        closed = true;
    }

    /** Called before writing a property. */
    void writable() {
        if (closed) {
            throw new IllegalStateException("The style is already closed: the tree does not change after it is built");
        }
    }
}
