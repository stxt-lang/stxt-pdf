package dev.stxt.boxes.tree;

/**
 * Lo común a {@link Style} y a sus grupos de propiedades ({@link FlexStyle}…): solo {@code tree} los
 * escribe, mientras {@link BoxTreeBuilder} construye el árbol, y se cierran con su caja ({@link Box#close}).
 * Escribir en uno cerrado es un {@code IllegalStateException}: después de construir el árbol, un estilo solo
 * se lee y se puede compartir entre hilos.
 */
abstract class StyleGroup {

    private boolean closed;

    void close() {
        closed = true;
    }

    /** Se llama antes de escribir una propiedad. */
    void writable() {
        if (closed) {
            throw new IllegalStateException("El estilo ya está cerrado: el árbol no cambia después de construirlo");
        }
    }
}
