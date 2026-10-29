package dev.stxt.pdf.resources;

/**
 * Un fichero de {@code images/} que no es PNG, JPEG ni un SVG admitido, o que no se puede leer
 * (referencia/componentes/image.md).
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
