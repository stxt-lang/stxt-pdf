package dev.stxt.boxes.layout;

import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.layout.inline.PageNumbers;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.model.Reference;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.ErrorCode;
import dev.stxt.boxes.tree.InvalidModelException;
import dev.stxt.boxes.tree.Media;
import dev.stxt.boxes.tree.References;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bucle de paginación (ESPECIFICACION.md §5.10): maqueta la raíz una vez por página, desde el
 * {@link BreakToken} de la anterior, hasta que vuelve como {@code null}.
 *
 * <p>Las cajas {@code fixed} no ocupan espacio, así que se resuelven después de paginar el flujo
 * y se repiten en las páginas que dice su {@code Pages}, todas por defecto (ESPECIFICACION.md
 * §5.5). Su containing block es el área útil.</p>
 *
 * <p>Al terminar, si el documento usa {@code {page}} o {@code {pages}}, se sustituyen en las
 * líneas de cada página ({@link PageNumbers}). Si el total de páginas tiene más cifras de las reservadas, antes se vuelve a paginar con las suyas.</p>
 *
 * <p>Las referencias a la página de otra caja ({@code {page of: id}}, §5.19) se resuelven
 * paginando hasta que converjan: cada pasada supone una página para cada caja referida (la
 * primera, un número de prueba), maqueta con ella y mira dónde ha quedado la caja; si no coincide,
 * se vuelve a paginar con la que ha salido. Termina cuando coinciden, o con
 * {@link ErrorCode#REFERENCE_UNSTABLE} tras {@value #MAX_REFERENCE_PASSES} pasadas que cambian
 * alguna página.</p>
 */
public final class Paginator {

    /** Límite de seguridad ante errores de implementación en los algoritmos. */
    private static final int MAX_PAGES = 10_000;
    /** Pasadas de paginación que pueden cambiar las páginas referidas antes de darlas por inestables (§5.19). */
    static final int MAX_REFERENCE_PASSES = 10;

    private final Engine engine;

    public Paginator(Engine engine) {
        this.engine = engine;
    }

    public List<Page> paginate(Box root, Media media) {
        // Cada caja fixed una sola vez, con la posición estática de la página donde aparece.
        Map<Box, PendingAbsolute> fixed = new LinkedHashMap<>();
        boolean numbered = hasPageNumbers(root);
        List<References.Site> sites = References.sites(root);
        Set<String> targets = new LinkedHashSet<>();
        sites.stream().filter(s -> s.reference().kind() == Reference.Kind.PAGE)
                .forEach(s -> targets.add(s.reference().id()));
        Map<String, String> numbers = numbers(root, sites);

        // Un número de prueba de las mismas cifras que {page}, hasta saber en qué página está cada caja.
        Map<String, String> assumed = new LinkedHashMap<>();
        for (String id : targets) {
            assumed.put(id, "0".repeat(PageNumbers.DIGITS));
        }
        engine.assume(assumed, numbers);

        // Se vuelve a paginar mientras algo cambie: las cifras del total de páginas (solo crecen) y
        // las páginas referidas, que cambian el ancho de los textos que las escriben.
        int digits = PageNumbers.DIGITS;
        int changes = 0;
        List<LayoutResult> results;
        while (true) {
            fixed.clear();
            results = flow(root, media, fixed);
            boolean again = false;
            if (numbered && PageNumbers.digits(results.size()) > digits) {
                digits = PageNumbers.digits(results.size());
                engine.reservePageDigits(digits);
                again = true;
            }
            Map<String, String> actual = pagesOf(results, targets);
            if (!actual.equals(assumed)) {
                if (++changes > MAX_REFERENCE_PASSES) {
                    throw unstable(sites, assumed, actual);
                }
                assumed = actual;
                engine.assume(assumed, numbers);
                again = true;
            }
            if (!again) {
                break;
            }
        }

        // Segunda fase de la numeración: ya se sabe cuántas páginas hay.
        int total = results.size();
        Rect area = media.pageArea();
        List<Page> pages = new ArrayList<>();
        for (LayoutResult r : results) {
            int number = pages.size() + 1;
            List<Fragment> fixedFragments = new ArrayList<>();
            // Una fixed dentro de otra sale en su resultado y se coloca después; solo salen las
            // que Pages incluye en esta página (§5.5), y no ocupan sitio donde no salen.
            List<PendingAbsolute> queue = new ArrayList<>(fixed.values());
            for (int i = 0; i < queue.size(); i++) {
                if (!queue.get(i).box().style.pages.includes(number, total)) {
                    continue;
                }
                LayoutResult f = engine.absolute().layOut(queue.get(i), area.width(), area.height(), Insets.ZERO);
                fixedFragments.add(numbered ? engine.number(f.fragment(), number, total) : f.fragment());
                queue.addAll(f.pending());
            }
            Fragment content = numbered ? engine.number(r.fragment(), number, total) : r.fragment();
            pages.add(new Page(number, content, fixedFragments));
        }
        return pages;
    }

    /** El número de cada caja referida con {@code {number of: id}}; ya validado por el builder. */
    private static Map<String, String> numbers(Box root, List<References.Site> sites) {
        Map<String, Box> boxes = References.byId(root);
        Map<String, String> numbers = new LinkedHashMap<>();
        for (References.Site site : sites) {
            if (site.reference().kind() == Reference.Kind.NUMBER) {
                numbers.put(site.reference().id(), boxes.get(site.reference().id()).number);
            }
        }
        return numbers;
    }

    /**
     * La página en que empieza cada caja referida: la de su primer fragmento (§5.19). Una caja
     * que no sale en ninguna página (no debería pasar: las cajas con {@code Fixed} se rechazan al
     * construir el árbol) es un error.
     */
    private static Map<String, String> pagesOf(List<LayoutResult> results, Set<String> targets) {
        Map<String, String> pages = new LinkedHashMap<>();
        if (targets.isEmpty()) {
            return pages;
        }
        for (int i = 0; i < results.size() && pages.size() < targets.size(); i++) {
            firstPages(results.get(i).fragment(), Integer.toString(i + 1), targets, pages);
        }
        for (String id : targets) {
            if (!pages.containsKey(id)) {
                throw new InvalidModelException(0, ErrorCode.REFERENCE_NOT_PLACEABLE, "La caja " + id
                        + " no sale en ninguna página");
            }
        }
        return pages;
    }

    private static void firstPages(Fragment f, String page, Set<String> targets, Map<String, String> pages) {
        String id = f.box().key();
        if (id != null && targets.contains(id)) {
            pages.putIfAbsent(id, page);
        }
        f.children().forEach(child -> firstPages(child, page, targets, pages));
    }

    private static InvalidModelException unstable(List<References.Site> sites, Map<String, String> assumed,
                                                  Map<String, String> actual) {
        References.Site culprit = sites.stream().filter(s -> s.reference().kind() == Reference.Kind.PAGE
                && !actual.get(s.reference().id()).equals(assumed.get(s.reference().id()))).findFirst().orElseThrow();
        String id = culprit.reference().id();
        return new InvalidModelException(culprit.line(), ErrorCode.REFERENCE_UNSTABLE, "La página de " + id
                + " no se estabiliza: tras " + MAX_REFERENCE_PASSES + " pasadas sigue cambiando (suponiendo "
                + assumed.get(id) + " sale en la " + actual.get(id) + "); el texto de la referencia cambia de "
                + "ancho con la página y mueve la caja de página");
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
