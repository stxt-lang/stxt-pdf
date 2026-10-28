package dev.stxt.boxes.resources;

/** Un fichero de {@code images/} que no es PNG, JPEG ni un SVG admitido, o que no se puede leer (ESPECIFICACION.md §5.18). */
public class ImageFormatException extends ResourceException {

    public ImageFormatException(String message) {
        super(message);
    }

    public ImageFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
