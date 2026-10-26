package dev.stxt.boxes.layout.block;

import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.layout.ConstraintSpace;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.IntrinsicSizes;
import dev.stxt.boxes.layout.LayoutAlgorithm;
import dev.stxt.boxes.layout.LayoutResult;
import dev.stxt.boxes.tree.Box;

/**
 * Layout {@code block}: apilar en vertical (módulo 2, hito 2). Pasos de ESPECIFICACION.md §5.3:
 *
 * <pre>
 * cursor = 0; strut = new MarginStrut()
 * para cada hijo (desde el BreakToken.Block, si lo hay):
 *     si es absolute/fixed: anotar PendingAbsolute con la posición estática; continuar
 *     strut.add(margen superior); cursor += strut.resolveAndReset()   // colapso
 *     resolver ancho: auto = CB − márgenes; márgenes auto = centrar   // Sizing
 *     r = engine.layout(hijo, espacio del hijo, token del hijo)       // recursivo
 *     si r no cabe en fragmentainerRemaining: elegir corte (§5.10) y devolver BreakToken.Block
 *     colocar el fragmento en (margen izquierdo, cursor); cursor += alto
 *     strut.add(margen inferior)
 * altura de contenido = cursor + strut.resolve(); Height auto = eso + relleno + borde
 * </pre>
 *
 * <p>Pendiente también: colapso padre-primer/último hijo, cajas vacías, cortes forzados y
 * truncado de márgenes en los cortes.</p>
 */
public final class BlockLayout implements LayoutAlgorithm {

    private final Engine engine;

    public BlockLayout(Engine engine) {
        this.engine = engine;
    }

    @Override
    public IntrinsicSizes intrinsic(Box box) {
        // TODO(hito 2): máximo de min/max-content de los hijos en flujo + relleno y borde.
        throw new UnsupportedOperationException("BlockLayout: pendiente del hito 2");
    }

    @Override
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        // TODO(hito 2)
        throw new UnsupportedOperationException("BlockLayout: pendiente del hito 2 (engine: " + engine + ")");
    }
}
