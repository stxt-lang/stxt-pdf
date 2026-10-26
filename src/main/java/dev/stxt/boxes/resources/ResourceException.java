package dev.stxt.boxes.resources;

/**
 * Recurso no válido o que no se encuentra: una ruta que sale de la carpeta de recursos, un
 * fichero que no existe, una fuente desconocida… (ESPECIFICACION.md §5.11). Quien conoce la
 * línea del documento la convierte en {@link dev.stxt.boxes.tree.InvalidModelException}.
 */
public class ResourceException extends RuntimeException {

    public ResourceException(String message) {
        super(message);
    }

    public ResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
