package dev.stxt.boxes.layout.grid;

import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.layout.ConstraintSpace;
import dev.stxt.boxes.layout.IntrinsicSizes;
import dev.stxt.boxes.layout.LayoutAlgorithm;
import dev.stxt.boxes.layout.LayoutResult;
import dev.stxt.boxes.tree.Box;

/**
 * Layout {@code grid} (módulo 6, hito 5). Pasos de ESPECIFICACION.md §5.8:
 * <ol>
 *   <li>Colocación: {@link GridAreas}, {@link Cell} y {@link Placement}.</li>
 *   <li>Columnas: {@link TrackSizing} con min/max-content en ancho.</li>
 *   <li>Filas: {@link TrackSizing} con la altura de cada ítem al ancho de sus columnas.</li>
 *   <li>Alinear cada ítem en su área ({@code Horizontal}, {@code Vertical}); estirar = re-layout.</li>
 * </ol>
 */
public final class GridLayout implements LayoutAlgorithm {

    @Override
    public IntrinsicSizes intrinsic(Box box) {
        // TODO(hito 5): dimensionar columnas con restricción min-content y max-content.
        throw new UnsupportedOperationException("GridLayout: pendiente del hito 5");
    }

    @Override
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        // TODO(hito 5)
        throw new UnsupportedOperationException("GridLayout: pendiente del hito 5");
    }
}
