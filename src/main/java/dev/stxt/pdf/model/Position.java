package dev.stxt.pdf.model;

/**
 * How the box takes part in the flow of its parent: {@code Frame}, {@code Overlay} and {@code Page overlay}; the
 * footnote area of a section ({@code Footnote area}, reference/components/footnote.md), which the paginator places at the
 * bottom of each page; the rest, {@code STATIC}.
 */
public enum Position { STATIC, FRAME, OVERLAY, PAGE_OVERLAY, FOOTNOTES }
