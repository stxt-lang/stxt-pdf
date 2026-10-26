package dev.stxt.boxes.layout;

import dev.stxt.InlineNode;
import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.fragment.Paginator;
import dev.stxt.boxes.layout.block.BlockLayout;
import dev.stxt.boxes.layout.flex.FlexLayout;
import dev.stxt.boxes.layout.grid.GridLayout;
import dev.stxt.boxes.layout.inline.FixedMetrics;
import dev.stxt.boxes.layout.inline.InlineLayout;
import dev.stxt.boxes.layout.inline.TextMeasurer;
import dev.stxt.boxes.layout.position.AbsoluteLayout;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.BoxTreeBuilder;

import java.util.List;

/**
 * Punto de entrada del motor y despachador de algoritmos por {@code Layout} (módulo 9).
 * Todas las llamadas recursivas de layout pasan por aquí para usar la caché.
 */
public final class Engine {

    private static final double EPSILON = 1e-9;

    private final LayoutCache cache = new LayoutCache();
    private final BlockLayout block;
    private final InlineLayout inline;
    private final FlexLayout flex;
    private final GridLayout grid;
    private final AbsoluteLayout absolute;

    /**
     * Motor con {@link FixedMetrics}, para que los tests y los casos golden sean deterministas y
     * calculables a mano. El PDF se maqueta con {@code new Engine(new PdfMetrics())}.
     */
    public Engine() {
        this(new FixedMetrics());
    }

    public Engine(TextMeasurer measurer) {
        this.block = new BlockLayout(this);
        this.inline = new InlineLayout(measurer);
        this.flex = new FlexLayout(this);
        this.grid = new GridLayout(this);
        this.absolute = new AbsoluteLayout(this);
    }

    /**
     * Documento STXT ya validado ({@link dev.stxt.boxes.stxt.DocumentReader}) → páginas. En modo
     * continuo devuelve una sola página.
     */
    public List<Page> layOutDocument(InlineNode document) {
        BoxTreeBuilder.Result tree = new BoxTreeBuilder().build(document);
        cache.clear();
        return new Paginator(this).paginate(tree.root(), tree.media());
    }

    public IntrinsicSizes intrinsic(Box box) {
        return cache.intrinsic(box, () -> algorithm(box).intrinsic(box));
    }

    /**
     * Layout de una caja con su algoritmo. Si la caja es el containing block de cajas
     * {@code absolute} de su interior, las resuelve al terminar (ESPECIFICACION.md §5.5).
     *
     * <p>Si la caja termina en esta página y solo se salen de ella su borde y su relleno
     * inferiores, se repite el layout con ese espacio reservado (§5.10): la caja termina dentro
     * de la página o se corta antes, y el final pasa a la siguiente con el borde y el relleno.</p>
     */
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        LayoutResult r = layoutOnce(box, space, from);
        if (!space.fragmenting() || r.breakToken() != null) {
            return r;
        }
        Fragment f = r.fragment();
        double bottom = f.border().bottom() + f.padding().bottom();
        double remaining = space.fragmentainerRemaining().getAsDouble();
        if (bottom > 0 && f.height() > remaining + EPSILON && f.height() - bottom <= remaining + EPSILON) {
            return layoutOnce(box, space.withRemaining(remaining - bottom, space.atFragmentainerStart()), from);
        }
        return r;
    }

    private LayoutResult layoutOnce(Box box, ConstraintSpace space, BreakToken from) {
        return cache.layout(box, space, from,
                () -> absolute.resolvePending(box, algorithm(box).layout(box, space, from)));
    }

    public LayoutCache cache() {
        return cache;
    }

    /**
     * Sustituye {@code {page}} y {@code {pages}} en las líneas de un fragmento ya maquetado y en
     * las de sus descendientes; el {@link Paginator} lo usa al terminar de paginar.
     */
    public Fragment number(Fragment fragment, int page, int pages) {
        return inline.number(fragment, page, pages);
    }

    /** Layout de las cajas fuera de flujo; el {@link Paginator} lo usa para las {@code fixed}. */
    public AbsoluteLayout absolute() {
        return absolute;
    }

    private LayoutAlgorithm algorithm(Box box) {
        return switch (box.style.layout) {
            case BLOCK -> block;
            case INLINE -> inline;
            case FLEX -> flex;
            case GRID -> grid;
        };
    }

    @Override
    public String toString() {
        return "Engine[" + cache.stats() + "]";
    }
}
