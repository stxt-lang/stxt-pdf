package dev.stxt.boxes.stxt;

/** Error de sintaxis STXT con origen y línea. */
public class ErrorStxt extends RuntimeException {

    private final String origen;
    private final int linea;

    public ErrorStxt(String origen, int linea, String mensaje) {
        super(origen + ":" + linea + ": " + mensaje);
        this.origen = origen;
        this.linea = linea;
    }

    public String origen() {
        return origen;
    }

    public int linea() {
        return linea;
    }
}
