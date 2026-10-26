package dev.stxt.boxes.layout.inline;

import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.layout.ConstraintSpace;
import dev.stxt.boxes.layout.IntrinsicSizes;
import dev.stxt.boxes.layout.LayoutAlgorithm;
import dev.stxt.boxes.layout.LayoutResult;
import dev.stxt.boxes.tree.Box;

/**
 * Layout {@code inline}: texto repartido en line boxes (módulo 2, hito 2).
 *
 * <p>Pasos: resolver el ancho de contenido, partir con {@link LineBreaker}, alinear cada línea,
 * altura = número de líneas × {@code Line height}. En modo paginado, colocar solo las líneas
 * que indique {@link dev.stxt.boxes.fragment.BreakRules#linesThatFit} y devolver un
 * {@link BreakToken.Line} con la siguiente.</p>
 */
public final class InlineLayout implements LayoutAlgorithm {

    private final TextMeasurer measurer;

    public InlineLayout(TextMeasurer measurer) {
        this.measurer = measurer;
    }

    @Override
    public IntrinsicSizes intrinsic(Box box) {
        // TODO(hito 2): sumar relleno, borde y márgenes de la caja a estos valores.
        if (!box.hasText()) {
            return IntrinsicSizes.ZERO;
        }
        return LineBreaker.intrinsics(box.text, measurer, box.style.font, box.style.fontSize);
    }

    @Override
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        // TODO(hito 2): implementar según ESPECIFICACION.md §5.4 y §5.10.
        throw new UnsupportedOperationException("InlineLayout: pendiente del hito 2");
    }
}
