package dev.stxt.pdf.resources;

/**
 * A file of {@code images/} that is not PNG, JPEG, PDF or a supported SVG, or that cannot be read
 * (reference/components/image.md).
 */
public class ImageFormatException extends ResourceException {

    public static final String CODE = "IMAGE_FORMAT";

    public ImageFormatException(String message) {
        super(CODE, message, null);
    }

    public ImageFormatException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
