package dev.stxt.pdf.model;

/**
 * How the box takes part in the flow of its parent: {@code Frame}, {@code Overlay} and {@code Page overlay}; the rest,
 * {@code STATIC}.
 */
public enum Position { STATIC, FRAME, OVERLAY, PAGE_OVERLAY }
