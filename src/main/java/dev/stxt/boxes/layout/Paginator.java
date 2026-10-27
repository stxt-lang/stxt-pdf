package dev.stxt.boxes.layout;

import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.layout.inline.PageNumbers;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.Media;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bucle de paginación (ESPECIFICACION.md §5.10): maqueta la raíz una vez por página, desde el
 * {@link BreakToken} de la anterior, hasta que vuelve como {@code null}.
 *
 * <p>Las cajas {@code fixed} no ocupan espacio, así que se resuelven después de paginar el flujo
 * y se repiten en todas las páginas (ESPECIFICACION.md §5.5). Su containing block es el área
 * útil.</p>
 *
 * <p>Al terminar, si el documento usa {@code {page}} o {@code {pages}}, se sustituyen en las
 * líneas de cada página ({@link PageNumbers}). Si el total de páginas tiene más cifras de las reservadas, antes se vuelve a paginar con las suyas.</p>
 */
public final class Paginator {

    /** Límite de seguridad ante errores de implementación en los algoritmos. */
    private static final int MAX_PAGES = 10_000;

    private final Engine engine;

    public Paginator(Engine engine) {
        this.engine = engine;
    }

    public List<Page> paginate(Box root, Media media) {
        // Cada caja fixed una sola vez, con la posición estática de la página donde aparece.
        Map<Box, PendingAbsolute> fixed = new LinkedHashMap<>();
        List<LayoutResult> results = flow(root, media, fixed);
        boolean numbered = hasPageNumbers(root);
        // Si el total no cabe en las cifras reservadas, se vuelve a paginar reservando las suyas.
        // Las cifras solo crecen, así que el bucle termina.
        int digits = PageNumbers.DIGITS;
        while (numbered && PageNumbers.digits(results.size()) > digits) {
            digits = PageNumbers.digits(results.size());
            engine.reservePageDigits(digits);
            fixed.clear();
            results = flow(root, media, fixed);
        }

        // Segunda fase de la numeración: ya se sabe cuántas páginas hay.
        int total = results.size();
        Rect area = media.pageArea();
        List<Page> pages = new ArrayList<>();
        for (LayoutResult r : results) {
            int number = pages.size() + 1;
            List<Fragment> fixedFragments = new ArrayList<>();
            // Una fixed dentro de otra sale en su resultado y se coloca después.
            List<PendingAbsolute> queue = new ArrayList<>(fixed.values());
            for (int i = 0; i < queue.size(); i++) {
                LayoutResult f = engine.absolute().layOut(queue.get(i), area.width(), area.height(), Insets.ZERO);
                fixedFragments.add(numbered ? engine.number(f.fragment(), number, total) : f.fragment());
                queue.addAll(f.pending());
            }
            Fragment content = numbered ? engine.number(r.fragment(), number, total) : r.fragment();
            pages.add(new Page(number, content, fixedFragments));
        }
        return pages;
    }

    /** Pagina el flujo: un resultado por página. Apunta en {@code fixed} las cajas {@code fixed}. */
    private List<LayoutResult> flow(Box root, Media media, Map<Box, PendingAbsolute> fixed) {
        List<LayoutResult> results = new ArrayList<>();
        BreakToken token = null;
        do {
            ConstraintSpace space = ConstraintSpace.forPage(media);
            LayoutResult r = engine.layout(root, space, token);
            BreakToken next = perPage(r.breakToken());
            if (next instanceof BreakToken.Before || (next != null && next.equals(token))) {
                throw new IllegalStateException("Sin progreso en la página " + (results.size() + 1)
                        + ": se ha incumplido la garantía de progreso (ESPECIFICACION.md §5.10)");
            }
            results.add(r);
            r.pending().forEach(p -> fixed.putIfAbsent(p.box(), p));
            token = next;
            if (results.size() > MAX_PAGES) {
                throw new IllegalStateException("Se ha superado el máximo de " + MAX_PAGES + " páginas");
            }
        } while (token != null);
        return results;
    }

    /** Algún texto del documento contiene {@code {page}} o {@code {pages}}. */
    private static boolean hasPageNumbers(Box box) {
        return PageNumbers.contains(box.text) || box.children.stream().anyMatch(Paginator::hasPageNumbers);
    }

    /**
     * La raíz representa el área útil, y su alto es el de cada página, no el total de todas: su
     * token no acumula alto consumido. Así cada página vuelve a tener el área útil completa.
     */
    private static BreakToken perPage(BreakToken token) {
        return token instanceof BreakToken.Block b ? new BreakToken.Block(b.nextChild(), b.childToken(), 0) : token;
    }
}
