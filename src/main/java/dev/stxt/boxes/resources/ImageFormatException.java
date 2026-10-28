package dev.stxt.boxes.resources;

/** Un fichero de {@code images/} que no es PNG, JPEG ni un SVG admitido, o que no se puede leer (ESPECIFICACION.md §5.18). */
public class ImageFormatException extends ResourceException {

    public static final String CODE = "IMAGE_FORMAT";

    public ImageFormatException(String message) {
        super(CODE, message, null);
    }

    public ImageFormatException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
