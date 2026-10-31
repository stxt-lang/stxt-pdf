package dev.stxt.pdf.model;

/**
 * The checks of a conversion that the caller can turn off (reference/implementation/api.md). By default all of them
 * run; the ones turned off do not fail, and the result may not look as expected or may not be accessible. The rules of
 * the document are not here: a document that breaks them cannot be laid out.
 */
public enum Check {

    /** The contrast of the text with its background (reference/common/pdf.md): when it is low, {@code CONTRAST_LOW}. */
    CONTRAST,

    /**
     * The fonts of the page of a PDF that an {@code Image} draws (reference/components/image.md): that they comply
     * with PDF/UA, embedded, with the mapping from each glyph to its character and the other rules of that page;
     * otherwise, {@code IMAGE_FORMAT}. Without this check, the viewer may substitute a font and the document may not be
     * PDF/UA.
     */
    PDF_IMAGE_FONTS,

    /**
     * The visible annotations on the page of a PDF that an {@code Image} draws, such as a filled form field or a
     * comment (reference/components/image.md): if it has any, {@code IMAGE_FORMAT}. Without this check, the page is
     * drawn without them.
     */
    PDF_IMAGE_ANNOTATIONS
}
