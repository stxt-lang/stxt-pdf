package dev.stxt.boxes.layout;

import dev.stxt.boxes.arbol.Box;
import dev.stxt.boxes.arbol.BoxTreeBuilder;
import dev.stxt.boxes.fragmentos.BreakToken;
import dev.stxt.boxes.fragmentos.Pagina;
import dev.stxt.boxes.fragmentos.Paginator;
import dev.stxt.boxes.layout.bloque.BlockLayout;
import dev.stxt.boxes.layout.flex.FlexLayout;
import dev.stxt.boxes.layout.grid.GridLayout;
import dev.stxt.boxes.layout.linea.InlineLayout;
import dev.stxt.boxes.layout.linea.MetricaFija;
import dev.stxt.boxes.layout.linea.TextMeasurer;
import dev.stxt.boxes.stxt.NodoStxt;

import java.util.List;

/**
 * Punto de entrada del motor y despachador de algoritmos por {@code Layout} (módulo 9).
 * Todas las llamadas recursivas de layout pasan por aquí para usar la caché.
 */
public final class Motor {

    private final LayoutCache cache = new LayoutCache();
    private final BlockLayout bloque;
    private final InlineLayout linea;
    private final FlexLayout flex;
    private final GridLayout grid;

    public Motor() {
        this(new MetricaFija());
    }

    public Motor(TextMeasurer medidor) {
        this.bloque = new BlockLayout(this);
        this.linea = new InlineLayout(medidor);
        this.flex = new FlexLayout();
        this.grid = new GridLayout();
    }

    /** Documento STXT → páginas. En modo continuo devuelve una sola página. */
    public List<Pagina> maquetar(NodoStxt documento) {
        BoxTreeBuilder.Resultado arbol = new BoxTreeBuilder().construir(documento);
        cache.vaciar();
        return new Paginator(this).paginar(arbol.raiz(), arbol.medio());
    }

    public IntrinsicSizes intrinsic(Box caja) {
        return cache.intrinsic(caja, () -> algoritmo(caja).intrinsic(caja));
    }

    public LayoutResult layout(Box caja, ConstraintSpace espacio, BreakToken desde) {
        return cache.layout(caja, espacio, desde, () -> algoritmo(caja).layout(caja, espacio, desde));
    }

    public LayoutCache cache() {
        return cache;
    }

    private LayoutAlgorithm algoritmo(Box caja) {
        return switch (caja.estilo.layout) {
            case BLOQUE -> bloque;
            case LINEA -> linea;
            case FLEX -> flex;
            case GRID -> grid;
        };
    }

    @Override
    public String toString() {
        return "Motor[" + cache.estadisticas() + "]";
    }
}
