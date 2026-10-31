package dev.stxt.pdf.layout.image;

import dev.stxt.pdf.model.Check;
import dev.stxt.pdf.model.Limits;
import dev.stxt.pdf.resources.ImageFile;
import dev.stxt.pdf.resources.ImageFormatException;
import dev.stxt.pdf.resources.ResourceException;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import dev.stxt.pdf.tree.PropertyKey;

import java.util.EnumSet;
import java.util.Set;

/**
 * The images of the document in the resources folder (reference/components/image.md): the file and
 * the natural size of each {@code Image}. The errors of the file belong to the document and carry the
 * line of the {@code Image}: {@code IMAGE_UNKNOWN} if the path does not exist or leaves the folder, and
 * {@code IMAGE_FORMAT} if it is not PNG, JPEG, PDF or a supported SVG. An image that exceeds the {@link Limits} is
 * {@code IMAGE_TOO_LARGE} (the pixels of a PNG or JPEG) or {@code SVG_TOO_COMPLEX} (the elements and the
 * levels of an SVG), also with the line of the {@code Image}; a PDF with more bytes than the {@link Limits} is
 * {@code PDF_TOO_LARGE}, before opening it. The page of a PDF has to exist, and {@code Pdf page} in another format is
 * an error ({@code IMAGE_PAGE}, with the line of {@code Pdf page}); a page that would not be drawn the same as in its
 * PDF is {@code IMAGE_FORMAT}, unless its check is removed ({@link Check#PDF_IMAGE_FONTS},
 * {@link Check#PDF_IMAGE_ANNOTATIONS}).
 */
public final class Images {

    private final Resources resources;
    private final Limits limits;
    private final Set<Check> excluded;

    /** Without the checks of {@code excluded}. */
    public Images(Resources resources, Limits limits, Set<Check> excluded) {
        this.resources = resources;
        this.limits = limits;
        this.excluded = excluded.isEmpty() ? Set.of() : EnumSet.copyOf(excluded);
    }

    /**
     * Checks, before the layout, that all the images of the tree exist and can be read.
     *
     * @throws InvalidModelException with the line of the first {@code Image} that fails
     */
    public void validate(Box box) {
        if (box.isImage()) {
            checkPdfBytes(box);
            ImageFile file = file(box);
            checkLimits(file, box);
            checkPage(file, box);
        }
        box.children().forEach(this::validate);
    }

    /**
     * That the image does not ask for more than the {@link Limits} allow; read from the header or the SVG count,
     * without drawing it.
     */
    private void checkLimits(ImageFile file, Box box) {
        if (file.pixels() > limits.maxImagePixels()) {
            throw new InvalidModelException(box.line(), ErrorCode.IMAGE_TOO_LARGE, "The image " + box.image() + " has "
                    + (long) file.pixelWidth() + " × " + (long) file.pixelHeight() + " pixels ("
                    + megapixels(file.pixels()) + ") and the maximum is " + megapixels(limits.maxImagePixels())
                    + ": its resolution has to be reduced (Limits.maxImagePixels changes it)");
        }
        if (file.svgDepth() > limits.maxSvgDepth()) {
            throw new InvalidModelException(box.line(), ErrorCode.SVG_TOO_COMPLEX, "The SVG " + box.image() + " nests "
                    + file.svgDepth() + " levels and the maximum is " + limits.maxSvgDepth()
                    + ": it has to be flattened (Limits.maxSvgDepth changes it)");
        }
        if (file.svgElements() > limits.maxSvgElements()) {
            throw new InvalidModelException(box.line(), ErrorCode.SVG_TOO_COMPLEX, "The SVG " + box.image() + " draws "
                    + (file.svgElements() >= (1L << 50) ? "an absurdly large number of" : String.valueOf(file.svgElements()))
                    + " elements, counting each time a <use> repeats a part, and the maximum is "
                    + limits.maxSvgElements() + ": it has to be simplified (Limits.maxSvgElements changes it)");
        }
    }

    /** That a PDF does not have more bytes than the limit: checked before opening it. */
    private void checkPdfBytes(Box box) {
        long bytes;
        try {
            bytes = resources.images().pdfBytes(box.image());
        } catch (ResourceException e) {
            // The path does not exist or leaves the folder: file(box) reports it, with its code.
            return;
        }
        if (bytes > limits.maxPdfBytes()) {
            throw new InvalidModelException(box.line(), ErrorCode.PDF_TOO_LARGE, "The PDF " + box.image() + " takes up "
                    + megabytes(bytes) + " and the maximum is " + megabytes(limits.maxPdfBytes())
                    + ": a smaller PDF is needed, with only the pages that are used (Limits.maxPdfBytes changes it)");
        }
    }

    private static String megabytes(long bytes) {
        return String.format(java.util.Locale.ROOT, "%.1f MB", bytes / 1_000_000.0);
    }

    /**
     * That {@code Pdf page} belongs to a PDF and the page exists, and that it can be drawn with the checks that are
     * made.
     */
    private void checkPage(ImageFile file, Box box) {
        int page = box.style().image().pdfPage();
        if (box.style().isDeclared(PropertyKey.PDF_PAGE)) {
            if (file.format() != ImageFile.Format.PDF) {
                throw new InvalidModelException(box.style().line(PropertyKey.PDF_PAGE), ErrorCode.IMAGE_PAGE, "Pdf page only "
                        + "has an effect on a PDF, and the image " + box.image() + " is " + file.format()
                        + ": it is not needed");
            }
            if (page > file.pages()) {
                throw new InvalidModelException(box.style().line(PropertyKey.PDF_PAGE), ErrorCode.IMAGE_PAGE, "The PDF "
                        + box.image() + " has " + file.pages() + (file.pages() == 1 ? " page" : " pages")
                        + " and has no page " + page);
            }
        }
        for (Check check : new Check[] {Check.PDF_IMAGE_ANNOTATIONS, Check.PDF_IMAGE_FONTS}) {
            String problem = excluded.contains(check) ? null : file.problem(page, check);
            if (problem != null) {
                throw new InvalidModelException(box.line(), ErrorCode.IMAGE_FORMAT, "Page " + page + " of the PDF "
                        + box.image() + " " + problem + " (or the check " + check + " is removed when converting)");
            }
        }
    }

    private static String megapixels(long pixels) {
        return String.format(java.util.Locale.ROOT, "%.1f Mpx", pixels / 1_000_000.0);
    }

    /** The file of an {@code Image}. */
    public ImageFile file(Box box) {
        try {
            return resources.images().find(box.image());
        } catch (ImageFormatException e) {
            throw new InvalidModelException(box.line(), ErrorCode.IMAGE_FORMAT, e.getMessage());
        } catch (ResourceException e) {
            throw new InvalidModelException(box.line(), ErrorCode.IMAGE_UNKNOWN, e.getMessage()
                    + " (the path of an Image is relative to the images/ folder of the resources)");
        }
    }

    /** Natural size of an {@code Image}, in points: for a PDF, the size of its page. */
    public NaturalSize natural(Box box) {
        ImageFile f = file(box);
        int page = box.style().image().pdfPage();
        return new NaturalSize(f.width(page), f.height(page));
    }
}
