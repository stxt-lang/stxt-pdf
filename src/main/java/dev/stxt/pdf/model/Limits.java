package dev.stxt.pdf.model;

/**
 * The most a conversion accepts (reference/common/limits.md). A document that exceeds it is valid but
 * asks for more than one wants to pay in memory and time, and fails with an error of
 * {@link StxtPdfException.Kind#LIMIT}: this is what keeps a document, or an image, small in bytes from
 * taking up a whole server. The default values ({@link #DEFAULT}) are conservative and are measured in
 * {@code tools/qa/BombsProbe.java}; a service with more memory or with larger documents raises them.
 *
 * <p>It does not include the reading limits of the STXT text (10 MB, 100 nesting levels and 10,000 characters per
 * line), which are those of {@code stxt-core} and come out of the reader as {@link StxtPdfException.Kind#LIMIT}.</p>
 *
 * @param maxPages       pages of the whole document ({@code PAGE_LIMIT})
 * @param maxImagePixels pixels (width × height) of a PNG or JPEG image, read from the header without
 *                       decoding it ({@code IMAGE_TOO_LARGE}); SVG has no pixels
 * @param maxSvgElements elements an SVG draws, counting each time a {@code <use>} repeats a piece
 *                       ({@code SVG_TOO_COMPLEX}); an SVG can be small and repeat billions of times
 * @param maxSvgDepth    nesting levels of an SVG, counting the root {@code <svg>}
 *                       ({@code SVG_TOO_COMPLEX}); deeper ones exhaust the drawing stack
 * @param maxPdfBytes    bytes of the file of a PDF that an {@code Image} draws, checked before opening it
 *                       ({@code PDF_TOO_LARGE})
 */
public record Limits(int maxPages, long maxImagePixels, int maxSvgElements, int maxSvgDepth, long maxPdfBytes) {

    /** 2000 pages, 25 million pixels per image, 20,000 elements and 64 levels per SVG and 20 MB per PDF. */
    public static final Limits DEFAULT = new Limits(2_000, 25_000_000L, 20_000, 64, 20_000_000L);

    /**
     * Checks that every limit is at least 1.
     *
     * @param maxPages       pages of the whole document
     * @param maxImagePixels pixels (width by height) of a PNG or JPEG image
     * @param maxSvgElements elements an SVG draws, counting each repetition of a {@code <use>}
     * @param maxSvgDepth    nesting levels of an SVG, counting the root {@code <svg>}
     * @param maxPdfBytes    bytes of the file of a PDF that an {@code Image} draws
     * @throws IllegalArgumentException if a limit is zero or negative
     */
    public Limits {
        require("maxPages", maxPages);
        require("maxImagePixels", maxImagePixels);
        require("maxSvgElements", maxSvgElements);
        require("maxSvgDepth", maxSvgDepth);
        require("maxPdfBytes", maxPdfBytes);
    }

    private static void require(String name, long value) {
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be at least 1 and is " + value);
        }
    }

    /**
     * {@return the same limits with another maximum of pages}
     *
     * @param maxPages pages of the whole document, at least 1
     * @throws IllegalArgumentException if it is less than 1
     */
    public Limits withMaxPages(int maxPages) {
        return new Limits(maxPages, maxImagePixels, maxSvgElements, maxSvgDepth, maxPdfBytes);
    }

    /**
     * {@return the same limits with another maximum of pixels per image}
     *
     * @param maxImagePixels pixels (width by height) of a PNG or JPEG image, at least 1
     * @throws IllegalArgumentException if it is less than 1
     */
    public Limits withMaxImagePixels(long maxImagePixels) {
        return new Limits(maxPages, maxImagePixels, maxSvgElements, maxSvgDepth, maxPdfBytes);
    }

    /**
     * {@return the same limits with another maximum of elements per SVG}
     *
     * @param maxSvgElements elements an SVG draws, counting each repetition of a {@code <use>}, at least 1
     * @throws IllegalArgumentException if it is less than 1
     */
    public Limits withMaxSvgElements(int maxSvgElements) {
        return new Limits(maxPages, maxImagePixels, maxSvgElements, maxSvgDepth, maxPdfBytes);
    }

    /**
     * {@return the same limits with another maximum of nesting levels per SVG}
     *
     * @param maxSvgDepth nesting levels of an SVG, counting the root {@code <svg>}, at least 1
     * @throws IllegalArgumentException if it is less than 1
     */
    public Limits withMaxSvgDepth(int maxSvgDepth) {
        return new Limits(maxPages, maxImagePixels, maxSvgElements, maxSvgDepth, maxPdfBytes);
    }

    /**
     * {@return the same limits with another maximum of bytes per PDF drawn as an image}
     *
     * @param maxPdfBytes bytes of the file of a PDF that an {@code Image} draws, at least 1
     * @throws IllegalArgumentException if it is less than 1
     */
    public Limits withMaxPdfBytes(long maxPdfBytes) {
        return new Limits(maxPages, maxImagePixels, maxSvgElements, maxSvgDepth, maxPdfBytes);
    }
}
