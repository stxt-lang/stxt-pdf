package dev.stxt.pdf.model;

import java.util.Objects;

/**
 * An image inside a text, {@code ![alt](path)} with {@code Markup: markdown} or in a {@code Markdown}
 * (reference/common/text-formatting.md): the format of a run whose text is the single
 * {@link TextRun#OBJECT_REPLACEMENT} character, which the layout measures as the image and the painting draws as it.
 * The image is as tall as the {@code Size} the line is set at and as wide as its proportion gives, and is centered on
 * the capitals of the font on the baseline, like the image marker of a list (reference/components/list.md).
 *
 * @param path the path of the file, relative to {@code images/} of the resources folder: a PNG, a JPEG or an SVG
 * @param alt  the alternative text, which the {@code Figure} of the image carries; {@code null} if it has none,
 *             and then the image is decorative
 */
public record InlineImage(String path, String alt) {

    /**
     * An image of a text.
     *
     * @param path the path of the file, relative to {@code images/}; not {@code null}
     * @param alt  the alternative text; {@code null} for a decorative image
     * @throws NullPointerException if the path is {@code null}
     */
    public InlineImage {
        Objects.requireNonNull(path, "path");
    }

    /** {@return whether the image is decorative: without alternative text, an <em>artifact</em> of the PDF} */
    public boolean decorative() {
        return alt == null;
    }
}
