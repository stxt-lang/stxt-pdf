package dev.stxt.pdf.layout.image;

/**
 * Natural size of an image in points (reference/components/image.md): that of its pixels, at 96 per
 * inch, or that of the modules of a generated image (reference/components/barcode.md). It is the size of its content
 * box if no other is declared.
 */
public record NaturalSize(double width, double height) {

    /** Width / height ratio. */
    public double ratio() {
        return width / height;
    }
}
