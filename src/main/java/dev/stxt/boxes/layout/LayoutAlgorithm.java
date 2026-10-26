package dev.stxt.boxes.layout;

import dev.stxt.boxes.arbol.Box;
import dev.stxt.boxes.fragmentos.BreakToken;

/**
 * Interfaz común de los algoritmos de layout: bloque, línea, flex y grid (módulos 7 y 8).
 */
public interface LayoutAlgorithm {

    /** min-content y max-content de la caja de margen. No depende del espacio disponible. */
    IntrinsicSizes intrinsic(Box caja);

    /**
     * Calcula la geometría de la caja y de sus descendientes.
     *
     * @param desde token desde el que reanudar, o {@code null} para empezar
     */
    LayoutResult layout(Box caja, ConstraintSpace espacio, BreakToken desde);
}
