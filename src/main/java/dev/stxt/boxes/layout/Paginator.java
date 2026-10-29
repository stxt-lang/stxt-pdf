package dev.stxt.boxes.layout;

import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.layout.inline.PageNumbers;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.model.Reference;
import dev.stxt.boxes.model.InternalException;
import dev.stxt.boxes.model.StxtPdfException;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.BoxTreeBuilder;
import dev.stxt.boxes.tree.Element;
import dev.stxt.boxes.tree.ErrorCode;
import dev.stxt.boxes.tree.InvalidModelException;
import dev.stxt.boxes.tree.Media;
import dev.stxt.boxes.tree.References;
import dev.stxt.boxes.tree.Section;

import java.util.ArrayList;
import java.util.Arrays;
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

    /** Pasadas de paginación que pueden cambiar las páginas referidas antes de darlas por inestables (§5.19). */
    static final int MAX_REFERENCE_PASSES = 10;

    private final Engine engine;

    public Paginator(Engine engine) {
        this.engine = engine;
    }

    /**
     * Pagina el documento: cada sección desde su primera página, con su {@link Media}, y sus
     * {@code Fixed} y los del documento (§5.21). Las referencias convergen para todo el documento a la vez.
     */
    public List<Page> paginate(BoxTreeBuilder.Result tree) {
        Box root = tree.root();
        List<Section> sections = tree.sections();
        // Los Fixed del documento: los hijos de la raíz que no son una sección.
        List<Box> documentFixed = root.children.stream().filter(b -> b.element != Element.SECTION).toList();
        // Cada caja fixed de una sección una sola vez, con la posición estática de la página donde aparece.
        List<Map<Box, PendingAbsolute>> fixed = new ArrayList<>();
        sections.forEach(x -> fixed.add(new LinkedHashMap<>()));
        // Los textos con {page} o {pages}, que cambian de ancho si el total necesita más cifras.
        List<Box> numberedTexts = withPageNumbers(root, new ArrayList<>());
        boolean numbered = !numberedTexts.isEmpty();
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
        engine.assume(assumed, numbers, List.of());

        // Se vuelve a paginar mientras algo cambie: las cifras del total de páginas (solo crecen) y
        // las páginas referidas, que cambian el ancho de los textos que las escriben. Solo se vuelve a
        // maquetar lo que depende de esos textos (PF-03): el resto sale de la caché.
        int digits = PageNumbers.DIGITS;
        int changes = 0;
        List<List<LayoutResult>> results;
        Numbering numbering;
        while (true) {
            fixed.forEach(Map::clear);
            results = flow(sections, fixed);
            numbering = Numbering.of(sections, results);
            boolean again = false;
            if (numbered && PageNumbers.digits(numbering.pages()) > digits) {
                digits = PageNumbers.digits(numbering.pages());
                engine.reservePageDigits(digits, numberedTexts);
                again = true;
            }
            Map<String, String> actual = pagesOf(results, numbering, targets, sites);
            if (!actual.equals(assumed)) {
                if (++changes > MAX_REFERENCE_PASSES) {
                    throw unstable(sites, assumed, actual);
                }
                engine.assume(actual, numbers, changedTexts(sites, assumed, actual));
                assumed = actual;
                again = true;
            }
            if (!again) {
                break;
            }
        }

        // Segunda fase de la numeración: ya se sabe cuántas páginas hay.
        int total = numbering.pages();
        List<Page> pages = new ArrayList<>();
        for (int k = 0; k < sections.size(); k++) {
            Section section = sections.get(k);
            Rect area = section.media().pageArea();
            List<LayoutResult> own = results.get(k);
            for (int i = 0; i < own.size(); i++) {
                int number = pages.size() + 1;
                int printed = numbering.printed(number);
                int tanda = numbering.total(number);
                // Una fixed dentro de otra sale en su resultado y se coloca después; solo salen las
                // que Pages incluye en esta página (§5.5), y no ocupan sitio donde no salen. Pages
                // cuenta dentro del alcance donde se declara: el documento o la sección (§5.21).
                List<Queued> queue = new ArrayList<>();
                if (section.inheritFixed()) {
                    documentFixed.forEach(b -> queue.add(new Queued(new PendingAbsolute(b, 0, 0), number, total)));
                }
                for (PendingAbsolute p : fixed.get(k).values()) {
                    queue.add(new Queued(p, i + 1, own.size()));
                }
                List<Fragment> fixedFragments = new ArrayList<>();
                for (int q = 0; q < queue.size(); q++) {
                    Queued item = queue.get(q);
                    if (!item.pending().box().style.pages.includes(item.number(), item.total())) {
                        continue;
                    }
                    if (numbered && printed == 0) {
                        unavailable(item.pending().box());
                    }
                    LayoutResult f = engine.absolute().layOut(item.pending(), area.width(), area.height(), Insets.ZERO);
                    fixedFragments.add(numbered ? engine.number(f.fragment(), printed, tanda) : f.fragment());
                    f.pending().forEach(p -> queue.add(new Queued(p, item.number(), item.total())));
                }
                if (numbered && printed == 0) {
                    unavailable(section.box());
                }
                Fragment content = numbered && printed > 0
                        ? engine.number(own.get(i).fragment(), printed, tanda) : own.get(i).fragment();
                pages.add(new Page(number, content, fixedFragments, section.media(), k, printed));
            }
        }
        return pages;
    }

    /** Una caja fixed por colocar y las páginas de su alcance: el documento o su sección. */
    private record Queued(PendingAbsolute pending, int number, int total) {
    }

    /**
     * El número impreso de cada página física y el último de su tanda (§5.21): las páginas desde el último
     * {@code Page numbering: restart}; {@code none} no numera ni avanza el contador.
     */
    private record Numbering(int[] printed, int[] totals) {

        static Numbering of(List<Section> sections, List<List<LayoutResult>> results) {
            int pages = results.stream().mapToInt(List::size).sum();
            int[] printed = new int[pages];
            int[] totals = new int[pages];
            int counter = 0;
            int tandaFirst = 0;
            int i = 0;
            for (int k = 0; k < sections.size(); k++) {
                Section.PageNumbering mode = sections.get(k).numbering();
                if (mode == Section.PageNumbering.RESTART) {
                    Arrays.fill(totals, tandaFirst, i, counter);
                    counter = 0;
                    tandaFirst = i;
                }
                for (int j = 0; j < results.get(k).size(); j++) {
                    printed[i++] = mode == Section.PageNumbering.NONE ? 0 : ++counter;
                }
            }
            Arrays.fill(totals, tandaFirst, i, counter);
            return new Numbering(printed, totals);
        }

        int pages() {
            return printed.length;
        }

        /** Número impreso de la página física {@code number} (desde 1); 0 si no se numera. */
        int printed(int number) {
            return printed[number - 1];
        }

        /** Último número de la tanda de la página física {@code number}. */
        int total(int number) {
            return totals[number - 1];
        }
    }

    /**
     * Un texto con {@code {page}} o {@code {pages}} en una página sin número: no hay qué escribir y no se
     * deja en blanco (§5.21).
     */
    private static void unavailable(Box box) {
        Box found = withPageNumbers(box);
        if (found != null) {
            throw new InvalidModelException(found.line, ErrorCode.PAGE_NUMBER_UNAVAILABLE, "El texto de la caja "
                    + (found.id != null ? found.id : "(sin Id)") + " tiene {page} o {pages} y sale en una página "
                    + "sin número (Page numbering: none): quita el marcador, o pon la sección con Inherit fixed: false "
                    + "o el Fixed en las secciones que numeran");
        }
    }

    /** La primera caja, ella o sus descendientes, con {@code {page}} o {@code {pages}}; {@code null} si no hay. */
    private static Box withPageNumbers(Box box) {
        if (PageNumbers.contains(box.text)) {
            return box;
        }
        for (Box child : box.children) {
            Box found = withPageNumbers(child);
            if (found != null) {
                return found;
            }
        }
        return null;
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
     * La página en que empieza cada caja referida: la de su primer fragmento (§5.19), con su número
     * impreso (§5.21). Una caja que no sale en ninguna página (no debería pasar: las cajas con {@code Fixed}
     * se rechazan al construir el árbol) o que está en una página sin número es un error.
     */
    private static Map<String, String> pagesOf(List<List<LayoutResult>> results, Numbering numbering,
                                               Set<String> targets, List<References.Site> sites) {
        Map<String, String> pages = new LinkedHashMap<>();
        if (targets.isEmpty()) {
            return pages;
        }
        int number = 0;
        for (List<LayoutResult> section : results) {
            for (LayoutResult r : section) {
                number++;
                if (pages.size() < targets.size()) {
                    firstPages(r.fragment(), number, targets, pages, numbering, sites);
                }
            }
        }
        for (String id : targets) {
            if (!pages.containsKey(id)) {
                throw new InvalidModelException(0, ErrorCode.REFERENCE_NOT_PLACEABLE, "La caja " + id
                        + " no sale en ninguna página");
            }
        }
        return pages;
    }

    private static void firstPages(Fragment f, int number, Set<String> targets, Map<String, String> pages,
                                   Numbering numbering, List<References.Site> sites) {
        String id = f.box().key();
        if (id != null && targets.contains(id) && !pages.containsKey(id)) {
            if (numbering.printed(number) == 0) {
                int line = sites.stream().filter(s -> id.equals(s.reference().id())).mapToInt(References.Site::line)
                        .findFirst().orElse(0);
                throw new InvalidModelException(line, ErrorCode.REFERENCE_NOT_PLACEABLE, "La caja " + id
                        + " está en una sección sin números de página (Page numbering: none): no tiene página que dar");
            }
            pages.put(id, Integer.toString(numbering.printed(number)));
        }
        f.children().forEach(child -> firstPages(child, number, targets, pages, numbering, sites));
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

    /**
     * Pagina el flujo de cada sección: un resultado por página y sección. Apunta en {@code fixed}, por
     * sección, las cajas {@code fixed}.
     */
    private List<List<LayoutResult>> flow(List<Section> sections, List<Map<Box, PendingAbsolute>> fixed) {
        List<List<LayoutResult>> all = new ArrayList<>();
        int pages = 0;
        for (int k = 0; k < sections.size(); k++) {
            Section section = sections.get(k);
            List<LayoutResult> results = new ArrayList<>();
            BreakToken token = null;
            do {
                ConstraintSpace space = ConstraintSpace.forPage(section.media());
                LayoutResult r = engine.layout(section.box(), space, token);
                BreakToken next = perPage(r.breakToken());
                if (next instanceof BreakToken.Before || (next != null && next.equals(token))) {
                    throw new InternalException("Sin progreso en la página " + (pages + results.size() + 1)
                            + ": se ha incumplido la garantía de progreso (ESPECIFICACION.md §5.10)");
                }
                results.add(r);
                int section0 = k;
                r.pending().forEach(p -> fixed.get(section0).putIfAbsent(p.box(), p));
                token = next;
                int maxPages = engine.limits().maxPages();
                if (pages + results.size() > maxPages) {
                    throw new InvalidModelException(StxtPdfException.NO_LINE, ErrorCode.PAGE_LIMIT,
                            "El documento tiene más de " + maxPages + " páginas, el máximo que admite el motor "
                                    + "(Limits.maxPages lo cambia)");
                }
            } while (token != null);
            pages += results.size();
            all.add(results);
        }
        return all;
    }

    /** Los textos del árbol que contienen {@code {page}} o {@code {pages}}, en orden del documento. */
    private static List<Box> withPageNumbers(Box box, List<Box> found) {
        if (PageNumbers.contains(box.text)) {
            found.add(box);
        }
        box.children.forEach(child -> withPageNumbers(child, found));
        return found;
    }

    /** Los textos con una referencia {@code {page of: id}} cuya página no es la que se suponía. */
    private static List<Box> changedTexts(List<References.Site> sites, Map<String, String> assumed,
                                          Map<String, String> actual) {
        return sites.stream().filter(s -> s.reference().kind() == Reference.Kind.PAGE)
                .filter(s -> !actual.get(s.reference().id()).equals(assumed.get(s.reference().id())))
                .map(References.Site::box).distinct().toList();
    }

    /**
     * La raíz representa el área útil, y su alto es el de cada página, no el total de todas: su
     * token no acumula alto consumido. Así cada página vuelve a tener el área útil completa.
     */
    private static BreakToken perPage(BreakToken token) {
        return token instanceof BreakToken.Block b ? new BreakToken.Block(b.nextChild(), b.childToken(), 0) : token;
    }
}
