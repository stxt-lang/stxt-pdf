package dev.stxt.boxes.layout.flex;

import dev.stxt.boxes.arbol.Box;
import dev.stxt.boxes.fragmentos.BreakToken;
import dev.stxt.boxes.layout.ConstraintSpace;
import dev.stxt.boxes.layout.IntrinsicSizes;
import dev.stxt.boxes.layout.LayoutAlgorithm;
import dev.stxt.boxes.layout.LayoutResult;

/**
 * Layout {@code flex} (módulo 5, hito 4). Los ocho pasos de ESPECIFICACION.md §5.7:
 * <ol>
 *   <li>Espacio disponible en el eje principal.</li>
 *   <li>Base de cada ítem: {@code Base}, el tamaño declarado o el max-content.</li>
 *   <li>Tamaño hipotético: base limitada por el mínimo (min-content si {@code Min ancho: auto}).</li>
 *   <li>Repartir en líneas si {@code Envolver}.</li>
 *   <li>Resolver tamaños flexibles con {@link ResolutorFlex}.</li>
 *   <li>Tamaño cruzado: layout de cada ítem con su tamaño principal; la línea mide el mayor.</li>
 *   <li>Estirar los ítems con {@code estirar} y repetir su layout.</li>
 *   <li>Alinear en el eje principal, en el cruzado y las líneas.</li>
 * </ol>
 */
public final class FlexLayout implements LayoutAlgorithm {

    @Override
    public IntrinsicSizes intrinsic(Box caja) {
        // TODO(hito 4): tabla de ESPECIFICACION.md §5.9.
        throw new UnsupportedOperationException("FlexLayout: pendiente del hito 4");
    }

    @Override
    public LayoutResult layout(Box caja, ConstraintSpace espacio, BreakToken desde) {
        // TODO(hito 4)
        throw new UnsupportedOperationException("FlexLayout: pendiente del hito 4");
    }
}
