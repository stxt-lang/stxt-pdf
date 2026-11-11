package dev.stxt.pdf.model;

/**
 * Alignment of an item within its line or cell. {@code BASELINE} is only of the items of a horizontal {@code Stack}
 * (reference/components/stack.md): the baselines of their first lines of text are on the same line.
 */
public enum Alignment { START, END, CENTER, STRETCH, BASELINE }
