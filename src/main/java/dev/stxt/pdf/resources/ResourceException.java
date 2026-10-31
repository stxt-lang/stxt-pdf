package dev.stxt.pdf.resources;

import dev.stxt.pdf.model.StxtPdfException;

/**
 * Recurso no válido o que no se encuentra: una ruta que sale de la carpeta de recursos, un
 * fichero que no existe, una fuente desconocida… (reference/common/resources.md). Quien conoce la
 * línea del documento la convierte en {@link dev.stxt.pdf.tree.InvalidModelException}; la que llega a
 * quien convierte es un problema de su carpeta de recursos ({@link Kind#RESOURCE}), sin línea.
 */
public class ResourceException extends StxtPdfException {

    public static final String CODE = "RESOURCE";

    public ResourceException(String message) {
        this(CODE, message, null);
    }

    public ResourceException(String message, Throwable cause) {
        this(CODE, message, cause);
    }

    /** Para los errores de recurso con un código propio ({@link ImageFormatException}). */
    protected ResourceException(String code, String message, Throwable cause) {
        super(Kind.RESOURCE, NO_LINE, code, message, cause);
    }
}
