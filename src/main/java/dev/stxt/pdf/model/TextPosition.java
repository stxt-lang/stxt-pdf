package dev.stxt.pdf.model;

/**
 * {@code Position}: where a styled run sits on its line (reference/common/text-formatting.md): {@code normal},
 * {@code superscript} or {@code subscript}. A superscript and a subscript are measured and painted at
 * {@link TextFormat#SUPERSCRIPT_SCALE} of the size of the box, raised or lowered from the baseline, without changing
 * the height of the line.
 */
public enum TextPosition {
    /** {@code normal}: on the baseline, at the size of the box. */
    NORMAL,
    /** {@code superscript}: smaller and raised {@link TextFormat#SUPERSCRIPT_RISE} of the size of the box. */
    SUPERSCRIPT,
    /** {@code subscript}: smaller and lowered {@link TextFormat#SUBSCRIPT_DROP} of the size of the box. */
    SUBSCRIPT
}
