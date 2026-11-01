package dev.stxt.pdf.model;

/**
 * {@code Overflow} of a box (reference/common/overflow.md): what the engine does when what the box contains does not
 * fit in it. {@code error} (by default) stops the conversion; {@code shrink}, in a box with text, reduces the
 * {@code Size} of the text until it fits, without going below {@code Min size}; {@code clip}, in a box without text,
 * paints what sticks out of its padding box clipped.
 */
public enum Overflow {
    ERROR, SHRINK, CLIP
}
