package dev.stxt.boxes.layout;

import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.tree.Box;

/**
 * Interfaz común de los algoritmos de layout: bloque, línea, flex y grid (módulos 7 y 8).
 */
public interface LayoutAlgorithm {

    /**
     * min-content y max-content del contenido de la caja, sin su borde, su relleno, su margen ni
     * su {@code Width}. No depende del espacio disponible. Los de la caja de margen los da
     * {@link Engine#intrinsic} con {@link BoxModel#intrinsic}.
     */
    IntrinsicSizes contentIntrinsic(Box box);

    /**
     * Calcula la geometría de la caja y de sus descendientes.
     *
     * @param from token desde el que reanudar, o {@code null} para empezar
     */
    LayoutResult layout(Box box, ConstraintSpace space, BreakToken from);
}
