package dev.stxt.pdf.model;

/**
 * Preformatted text (reference/common/text-formatting.md): {@code none} collapses white space; {@code keep} keeps
 * the spaces and the line breaks and does not break the lines; {@code wrap} also, but breaks lines that are too wide
 * between words.
 */
public enum Preformatted {
    NONE, KEEP, WRAP;

    /** Whether it keeps the spaces and the line breaks of the text. */
    public boolean enabled() {
        return this != NONE;
    }
}
