package dev.stxt.boxes.fragment;

import dev.stxt.boxes.layout.ConstraintSpace;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.LayoutResult;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.Media;

import java.util.ArrayList;
import java.util.List;

/**
 * Bucle de paginación (módulo 8). En modo continuo el espacio no tiene límite, el
 * {@link BreakToken} vuelve siempre como {@code null} y el bucle da una sola vuelta:
 * el mismo motor sirve para los dos modos.
 */
public final class Paginator {

    /** Límite de seguridad ante errores de implementación en los algoritmos. */
    private static final int MAX_PAGES = 10_000;

    private final Engine engine;

    public Paginator(Engine engine) {
        this.engine = engine;
    }

    public List<Page> paginate(Box root, Media media) {
        List<Page> pages = new ArrayList<>();
        BreakToken token = null;
        do {
            ConstraintSpace space = ConstraintSpace.forPage(media);
            LayoutResult r = engine.layout(root, space, token);
            if (r.breakToken() != null && r.breakToken().equals(token)) {
                throw new IllegalStateException("Sin progreso en la página " + (pages.size() + 1)
                        + ": se ha incumplido la garantía de progreso (ESPECIFICACION.md §5.10)");
            }
            // TODO(hito 3): resolver aquí las cajas fixed de cada página (r.pending() y media.pageArea()).
            pages.add(new Page(pages.size() + 1, r.fragment(), List.of()));
            token = r.breakToken();
            if (pages.size() > MAX_PAGES) {
                throw new IllegalStateException("Se ha superado el máximo de " + MAX_PAGES + " páginas");
            }
        } while (token != null);
        return pages;
    }
}
