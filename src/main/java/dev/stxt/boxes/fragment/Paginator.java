package dev.stxt.boxes.fragment;

import dev.stxt.boxes.layout.ConstraintSpace;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.LayoutResult;
import dev.stxt.boxes.layout.PendingAbsolute;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.Media;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bucle de paginación (módulo 8). En modo continuo el espacio no tiene límite, el
 * {@link BreakToken} vuelve siempre como {@code null} y el bucle da una sola vuelta:
 * el mismo motor sirve para los dos modos.
 *
 * <p>Las cajas {@code fixed} no ocupan espacio, así que se resuelven después de paginar el flujo
 * y se repiten en todas las páginas (ESPECIFICACION.md §5.5). Su containing block es el área
 * útil; en modo continuo, que no tiene alto, se toma el alto del flujo del documento.</p>
 */
public final class Paginator {

    /** Límite de seguridad ante errores de implementación en los algoritmos. */
    private static final int MAX_PAGES = 10_000;

    private final Engine engine;

    public Paginator(Engine engine) {
        this.engine = engine;
    }

    public List<Page> paginate(Box root, Media media) {
        List<LayoutResult> results = new ArrayList<>();
        // Cada caja fixed una sola vez, con la posición estática de la página donde aparece.
        Map<Box, PendingAbsolute> fixed = new LinkedHashMap<>();
        BreakToken token = null;
        do {
            ConstraintSpace space = ConstraintSpace.forPage(media);
            LayoutResult r = engine.layout(root, space, token);
            if (r.breakToken() != null && r.breakToken().equals(token)) {
                throw new IllegalStateException("Sin progreso en la página " + (results.size() + 1)
                        + ": se ha incumplido la garantía de progreso (ESPECIFICACION.md §5.10)");
            }
            results.add(r);
            r.pending().forEach(p -> fixed.putIfAbsent(p.box(), p));
            token = r.breakToken();
            if (results.size() > MAX_PAGES) {
                throw new IllegalStateException("Se ha superado el máximo de " + MAX_PAGES + " páginas");
            }
        } while (token != null);

        List<Page> pages = new ArrayList<>();
        for (LayoutResult r : results) {
            Rect area = media.pageArea();
            double height = media.paged() ? area.height() : r.fragment().height();
            List<Fragment> fixedFragments = new ArrayList<>();
            // Una fixed dentro de otra sale en su resultado y se coloca después.
            List<PendingAbsolute> queue = new ArrayList<>(fixed.values());
            for (int i = 0; i < queue.size(); i++) {
                LayoutResult f = engine.absolute().layOut(queue.get(i), area.width(), height, Insets.ZERO);
                fixedFragments.add(f.fragment());
                queue.addAll(f.pending());
            }
            pages.add(new Page(pages.size() + 1, r.fragment(), fixedFragments));
        }
        return pages;
    }
}
