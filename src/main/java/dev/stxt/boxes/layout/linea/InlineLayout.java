package dev.stxt.boxes.layout.linea;

import dev.stxt.boxes.arbol.Box;
import dev.stxt.boxes.fragmentos.BreakToken;
import dev.stxt.boxes.layout.ConstraintSpace;
import dev.stxt.boxes.layout.IntrinsicSizes;
import dev.stxt.boxes.layout.LayoutAlgorithm;
import dev.stxt.boxes.layout.LayoutResult;

/**
 * Layout {@code linea}: texto repartido en line boxes (módulo 2, hito 2).
 *
 * <p>Pasos: resolver el ancho de contenido, partir con {@link LineBreaker}, alinear cada línea,
 * altura = número de líneas × {@code Interlineado}. En modo paginado, colocar solo las líneas
 * que indique {@link dev.stxt.boxes.fragmentos.ReglasCorte#lineasQueCaben} y devolver un
 * {@link BreakToken.Linea} con la siguiente.</p>
 */
public final class InlineLayout implements LayoutAlgorithm {

    private final TextMeasurer medidor;

    public InlineLayout(TextMeasurer medidor) {
        this.medidor = medidor;
    }

    @Override
    public IntrinsicSizes intrinsic(Box caja) {
        // TODO(hito 2): sumar relleno, borde y márgenes de la caja a estos valores.
        if (!caja.tieneTexto()) {
            return IntrinsicSizes.CERO;
        }
        return LineBreaker.intrinsecos(caja.texto, medidor, caja.estilo.fuente, caja.estilo.tamano);
    }

    @Override
    public LayoutResult layout(Box caja, ConstraintSpace espacio, BreakToken desde) {
        // TODO(hito 2): implementar según ESPECIFICACION.md §5.4 y §5.10.
        throw new UnsupportedOperationException("InlineLayout: pendiente del hito 2");
    }
}
