package dev.stxt.boxes.layout.flex;

import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.layout.ConstraintSpace;
import dev.stxt.boxes.layout.IntrinsicSizes;
import dev.stxt.boxes.layout.LayoutAlgorithm;
import dev.stxt.boxes.layout.LayoutResult;
import dev.stxt.boxes.tree.Box;

/**
 * Layout {@code flex} (módulo 5, hito 4). Los ocho pasos de ESPECIFICACION.md §5.7:
 * <ol>
 *   <li>Espacio disponible en el eje principal.</li>
 *   <li>Base de cada ítem: {@code Basis}, el tamaño declarado o el max-content.</li>
 *   <li>Tamaño hipotético: base limitada por el mínimo (min-content si {@code Min width: auto}).</li>
 *   <li>Repartir en líneas si {@code Wrap}.</li>
 *   <li>Resolver tamaños flexibles con {@link FlexResolver}.</li>
 *   <li>Tamaño cruzado: layout de cada ítem con su tamaño principal; la línea mide el mayor.</li>
 *   <li>Estirar los ítems con {@code stretch} y repetir su layout.</li>
 *   <li>Alinear en el eje principal, en el cruzado y las líneas.</li>
 * </ol>
 */
public final class FlexLayout implements LayoutAlgorithm {

    @Override
    public IntrinsicSizes intrinsic(Box box) {
        // TODO(hito 4): tabla de ESPECIFICACION.md §5.9.
        throw new UnsupportedOperationException("FlexLayout: pendiente del hito 4");
    }

    @Override
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        // TODO(hito 4)
        throw new UnsupportedOperationException("FlexLayout: pendiente del hito 4");
    }
}
