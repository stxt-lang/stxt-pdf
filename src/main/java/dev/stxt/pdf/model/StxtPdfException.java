package dev.stxt.pdf.model;

/**
 * Raíz de todos los errores que el motor da a quien lo usa (referencia/comun/errores.md): con una sola
 * {@code catch (StxtPdfException)} basta para tratar cualquier fallo de una conversión, y {@link #kind()} dice
 * de quién es la culpa, que es lo que un servicio necesita para contestar (un 4xx o un 5xx).
 *
 * <ul>
 *   <li>{@link Kind#INVALID_DOCUMENT}: el documento o sus estilos no son válidos (sintaxis, schema, una regla
 *       del modelo, un desbordamiento, un recurso que el documento nombra y no existe). Arreglarlo es cosa de
 *       quien lo escribió, y casi siempre trae {@link #line()}.</li>
 *   <li>{@link Kind#RESOURCE}: la carpeta de recursos no es utilizable (no existe, una fuente o una imagen
 *       estropeada) o no se puede leer el fichero. Es del entorno de quien convierte.</li>
 *   <li>{@link Kind#LIMIT}: el documento es válido pero pide más de lo que se admite (demasiadas páginas…).</li>
 *   <li>{@link Kind#INTERNAL}: un fallo del motor, no del documento ni del entorno. Se informa como un
 *       defecto; {@link #getCause()} guarda el original.</li>
 * </ul>
 *
 * <p>Los errores de entrada y salida al escribir en el destino no son de esta familia: son la
 * {@link java.io.IOException} que declaran los métodos que escriben.</p>
 */
public abstract class StxtPdfException extends RuntimeException {

    /** De quién es la culpa de un error. */
    public enum Kind {
        INVALID_DOCUMENT,
        RESOURCE,
        LIMIT,
        INTERNAL
    }

    /** El valor de {@link #line()} cuando el error no es de ninguna línea del documento. */
    public static final int NO_LINE = 0;

    private final Kind kind;
    private final int line;
    private final String code;

    protected StxtPdfException(Kind kind, int line, String code, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.line = line;
        this.code = code;
    }

    public Kind kind() {
        return kind;
    }

    /** La línea del documento donde está el nodo culpable, o {@link #NO_LINE} si el error no es de una línea. */
    public int line() {
        return line;
    }

    /**
     * Código estable del error, en texto: el de {@code ErrorCode} ({@code BOX_OVERFLOW}), el de la librería
     * {@code stxt-core} ({@code CHILD_NOT_DECLARED}), {@code RESOURCE}, {@code IMAGE_FORMAT} o {@code INTERNAL}.
     */
    public String code() {
        return code;
    }
}
