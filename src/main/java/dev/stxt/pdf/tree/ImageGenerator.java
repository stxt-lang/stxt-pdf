package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.pdf.model.Modules;

/**
 * Generates the image of an element from its value and its properties instead of reading it from a file: the
 * extension point of the generated images (reference/implementation/tree.md). {@link Barcodes} is the first one. The
 * box keeps the drawing ({@link Box#modules()}) and its role is that of an image, so the layout sizes it and the
 * painting draws it without knowing which element it comes from: a new generator is a new element with its family in
 * {@link ElementReader}, and nothing changes outside {@code tree}.
 */
interface ImageGenerator {

    /**
     * The drawing of the element {@code n}, whose properties are already read and checked in {@code s}.
     *
     * @throws InvalidModelException if the value cannot be drawn, with the line of the element
     */
    Modules generate(InlineNode n, Style s);
}
