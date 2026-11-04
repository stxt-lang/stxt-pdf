package dev.stxt.pdf.model;

/**
 * {@code Case}: the capitalization of the text (reference/common/text-formatting.md): {@code none}, {@code uppercase},
 * {@code lowercase} or {@code capitalize}.
 */
public enum TextCase {
    /** {@code none}: the letters as written. */
    NONE,
    /** {@code uppercase}: every letter in capitals, by the rules of the {@code Language} of the box. */
    UPPERCASE,
    /** {@code lowercase}: every letter in lowercase, by the rules of the {@code Language} of the box. */
    LOWERCASE,
    /** {@code capitalize}: the first letter of each word in capitals, and the rest as written. */
    CAPITALIZE
}
