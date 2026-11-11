package dev.stxt.pdf.model;

/**
 * {@code Break long words} (reference/common/typography.md): what is done with a part of a word that cannot be
 * split further and is wider than a whole line of its box. {@code never} leaves it whole, and it overflows;
 * {@code new-line} cuts it at the last character that fits, with no hyphen, and starts it in a line of its own;
 * {@code same-line} cuts it the same way and starts it in the current line, after the words before it.
 */
public enum BreakLongWords {
    NEVER, NEW_LINE, SAME_LINE;

    /** Whether the long parts of the words are cut. */
    public boolean enabled() {
        return this != NEVER;
    }
}
