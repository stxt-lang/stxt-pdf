package dev.stxt.boxes.arbol;

/**
 * Regla del modelo incumplida que el template STXT no puede expresar (ESPECIFICACION.md §6),
 * con la línea del documento donde se produce.
 */
public class ModeloInvalidoException extends RuntimeException {

    private final int linea;

    public ModeloInvalidoException(int linea, String mensaje) {
        super("línea " + linea + ": " + mensaje);
        this.linea = linea;
    }

    public int linea() {
        return linea;
    }
}
