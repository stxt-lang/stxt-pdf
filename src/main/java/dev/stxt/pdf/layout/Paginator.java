package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.BreakToken;
import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.layout.inline.PageNumbers;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Reference;
import dev.stxt.pdf.model.InternalException;
import dev.stxt.pdf.model.StxtPdfException;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.BoxTreeBuilder;
import dev.stxt.pdf.tree.Element.Role;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import dev.stxt.pdf.tree.PageSetup;
import dev.stxt.pdf.tree.References;
import dev.stxt.pdf.tree.Section;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pagination loop (reference/common/breaks.md): lays out the root once per page, from the
 * {@link BreakToken} of the previous one, until it comes back as {@code null}.
 *
 * <p>The {@code fixed} boxes take up no space, so they are resolved after paginating the flow
 * and repeated on the pages that their {@code Pages} says, all by default
 * (reference/components/frame-and-overlays.md). Their reference container is the page area.</p>
 *
 * <p>When finished, if the document uses {@code {page}} or {@code {pages}}, they are replaced in the
 * lines of each page ({@link PageNumbers}). If the total of pages has more digits than those reserved, the
 * document is paginated again with its own beforehand.</p>
 *
 * <p>The references to the page of another box ({@code {page of: id}}, reference/common/references.md) are resolved
 * by paginating until they converge: each pass assumes a page for each box referred to (the
 * first one, a probe number), lays out with it and looks at where the box ended up; if it does not match,
 * it paginates again with the one that came out. It ends when they match, or with
 * {@link ErrorCode#REFERENCE_UNSTABLE} after {@value #MAX_REFERENCE_PASSES} passes that change
 * some page.</p>
 */
public final class Paginator {

    /**
     * Pagination passes that can change the referred pages before they are considered unstable
     * (reference/common/references.md).
     */
    static final int MAX_REFERENCE_PASSES = 10;

    private final Engine engine;

    public Paginator(Engine engine) {
        this.engine = engine;
    }

    /**
     * Paginates the document: each section from its first page, with its {@link PageSetup}, and its
     * {@code Page overlay}s and those of the document (reference/components/document.md). The references converge
     * for the whole document at once.
     */
    public List<Page> paginate(BoxTreeBuilder.Result tree) {
        Box root = tree.root();
        List<Section> sections = tree.sections();
        // Each fixed box of a section only once, with the static position of the page where it appears.
        List<Map<Box, PendingOverlay>> fixed = new ArrayList<>();
        sections.forEach(x -> fixed.add(new LinkedHashMap<>()));
        // The texts with {page} or {pages}, which change width if the total needs more digits.
        List<Box> numberedTexts = withPageNumbers(root, new ArrayList<>());
        Flow flow = converge(root, sections, fixed, numberedTexts);
        return pages(root, sections, flow, fixed, !numberedTexts.isEmpty());
    }

    /**
     * The flow of each section, one result per page, whether it ends with a blank page ({@code Begin on: odd} in
     * the next one) and the numbering of its pages.
     */
    private record Flow(List<List<LayoutResult>> results, boolean[] blankAfter, Numbering numbering) {
    }

    /**
     * Paginates the flow until nothing changes: the digits of the total of pages (they only grow) and the pages
     * referred to with {@code {page of: id}}, which change the width of the texts that write them. Only what depends
     * on those texts is laid out again: the rest comes from the cache.
     */
    private Flow converge(Box root, List<Section> sections, List<Map<Box, PendingOverlay>> fixed,
                          List<Box> numberedTexts) {
        List<References.Site> sites = References.sites(root);
        Set<String> targets = new LinkedHashSet<>();
        sites.stream().filter(s -> s.reference().kind() == Reference.Kind.PAGE)
                .forEach(s -> targets.add(s.reference().id()));
        Map<String, String> numbers = numbers(root, sites);

        // A probe number with the same digits as {page}, until it is known on which page each box is.
        Map<String, String> assumed = new LinkedHashMap<>();
        for (String id : targets) {
            assumed.put(id, "0".repeat(PageNumbers.DIGITS));
        }
        engine.assume(assumed, numbers, List.of());

        String probe = PageNumbers.firstProbe();
        int changes = 0;
        while (true) {
            fixed.forEach(Map::clear);
            List<List<LayoutResult>> results = flow(sections, fixed);
            boolean[] blankAfter = blankAfter(sections, results);
            Numbering numbering = Numbering.of(sections, results, blankAfter);
            boolean again = false;
            String covering = PageNumbers.cover(probe, numbering.probe());
            if (!numberedTexts.isEmpty() && !covering.equals(probe)) {
                probe = covering;
                engine.reservePageNumbers(probe, numberedTexts);
                again = true;
            }
            Map<String, String> actual = pagesOf(results, blankAfter, numbering, targets, sites);
            if (!actual.equals(assumed)) {
                if (++changes > MAX_REFERENCE_PASSES) {
                    throw unstable(sites, assumed, actual);
                }
                engine.assume(actual, numbers, changedTexts(sites, assumed, actual));
                assumed = actual;
                again = true;
            }
            if (!again) {
                return new Flow(results, blankAfter, numbering);
            }
        }
    }

    /**
     * Second phase, with the total of pages known: each page with its numbered content ({@code {page}} and
     * {@code {pages}}) and its {@code Page overlay}s, those of its section and, if it inherits them, those of the
     * document.
     */
    private List<Page> pages(Box root, List<Section> sections, Flow flow, List<Map<Box, PendingOverlay>> fixed,
                             boolean numbered) {
        // The Page overlays of the document: the children of the root that are not a section.
        List<Box> documentPageOverlays = root.children().stream().filter(b -> b.role() != Role.SECTION).toList();
        Numbering numbering = flow.numbering();
        List<Page> pages = new ArrayList<>();
        for (int k = 0; k < sections.size(); k++) {
            Section section = sections.get(k);
            List<LayoutResult> own = flow.results().get(k);
            for (int i = 0; i < own.size(); i++) {
                int number = pages.size() + 1;
                int printed = numbering.printed(number);
                // Pages counts within the scope where it is declared: the document or the section
                // (reference/components/document.md).
                List<Queued> queue = new ArrayList<>();
                if (section.inheritPageOverlays()) {
                    documentPageOverlays.forEach(b -> queue.add(new Queued(new PendingOverlay(b, 0, 0), number,
                            numbering.pages())));
                }
                for (PendingOverlay p : fixed.get(k).values()) {
                    queue.add(new Queued(p, i + 1, own.size()));
                }
                List<Fragment> pageOverlayFragments = placePageOverlays(queue, section, numbered, printed, numbering.label(number),
                        numbering.totalLabel(number));
                if (numbered && printed == 0) {
                    unavailable(section.box());
                }
                Fragment content = numbered && printed > 0
                        ? engine.number(own.get(i).fragment(), numbering.label(number), numbering.totalLabel(number))
                        : own.get(i).fragment();
                pages.add(new Page(number, content, pageOverlayFragments, section.pageSetup(), k, printed,
                        numbering.style(number), false));
            }
            if (flow.blankAfter()[k]) {
                // The blank page so that the next one starts on an odd page: of this section, without content or
                // Page overlay (reference/components/document.md).
                int number = pages.size() + 1;
                Rect area = section.pageSetup().pageArea();
                Fragment empty = new Fragment(section.box(), 0, 0, area.width(), area.height(), Insets.ZERO,
                        Insets.ZERO, Insets.ZERO, List.of(), List.of(), true, true);
                pages.add(new Page(number, empty, List.of(), section.pageSetup(), k, numbering.printed(number),
                        numbering.style(number), true));
            }
        }
        return pages;
    }

    /**
     * The sections that end with a blank page: those that leave the next one, with {@code Begin on: odd}, on an
     * even physical page (reference/components/document.md).
     */
    private boolean[] blankAfter(List<Section> sections, List<List<LayoutResult>> results) {
        boolean[] blank = new boolean[sections.size()];
        int pages = 0;
        for (int k = 0; k < sections.size(); k++) {
            pages += results.get(k).size();
            if (k + 1 < sections.size() && sections.get(k + 1).beginOn() == Section.BeginOn.ODD && pages % 2 == 1) {
                blank[k] = true;
                pages++;
            }
        }
        if (pages > engine.limits().maxPages()) {
            throw pageLimit();
        }
        return blank;
    }

    private InvalidModelException pageLimit() {
        int maxPages = engine.limits().maxPages();
        return new InvalidModelException(StxtPdfException.NO_LINE, ErrorCode.PAGE_LIMIT,
                "The document has more than " + maxPages + " pages, the maximum the engine allows "
                        + "(Limits.maxPages changes it)");
    }

    /**
     * The {@code Page overlay}s of a page, in the page area of its section: only those that {@code Pages} includes in
     * it (reference/components/frame-and-overlays.md), which take up no room where they do not appear. A fixed one
     * inside another comes out in its result and is placed afterwards.
     */
    private List<Fragment> placePageOverlays(List<Queued> queue, Section section, boolean numbered, int printed, String page,
                                      String tanda) {
        Rect area = section.pageSetup().pageArea();
        List<Fragment> fragments = new ArrayList<>();
        for (int q = 0; q < queue.size(); q++) {
            Queued item = queue.get(q);
            if (!item.pending().box().style().pages().includes(item.number(), item.total())) {
                continue;
            }
            if (numbered && printed == 0) {
                unavailable(item.pending().box());
            }
            LayoutResult f = engine.absolute().layOut(item.pending(), area.width(), area.height(), Insets.ZERO);
            fragments.add(numbered ? engine.number(f.fragment(), page, tanda) : f.fragment());
            f.pending().forEach(p -> queue.add(new Queued(p, item.number(), item.total())));
        }
        return fragments;
    }

    /** A fixed box to be placed and the pages of its scope: the document or its section. */
    private record Queued(PendingOverlay pending, int number, int total) {
    }

    /**
     * A text with {@code {page}} or {@code {pages}} on a page without a number: there is nothing to write and it is
     * not left blank (reference/components/document.md).
     */
    private static void unavailable(Box box) {
        Box found = withPageNumbers(box);
        if (found != null) {
            throw new InvalidModelException(found.line(), ErrorCode.PAGE_NUMBER_UNAVAILABLE, "The text of box "
                    + (found.id() != null ? found.id() : "(without Id)") + " has {page} or {pages} and appears on a "
                    + "page without a number (Page numbering: none): the marker is not needed, or the section needs "
                    + "Inherit page overlays: false, or the Page overlay only in the sections that are numbered");
        }
    }

    /** The first box, itself or its descendants, with {@code {page}} or {@code {pages}}; {@code null} if none. */
    private static Box withPageNumbers(Box box) {
        if (PageNumbers.contains(box.text())) {
            return box;
        }
        for (Box child : box.children()) {
            Box found = withPageNumbers(child);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** The number of each box referred to with {@code {number of: id}}; already validated by the builder. */
    private static Map<String, String> numbers(Box root, List<References.Site> sites) {
        Map<String, Box> boxes = References.byId(root);
        Map<String, String> numbers = new LinkedHashMap<>();
        for (References.Site site : sites) {
            if (site.reference().kind() == Reference.Kind.NUMBER) {
                numbers.put(site.reference().id(), boxes.get(site.reference().id()).number());
            }
        }
        return numbers;
    }

    /**
     * The page on which each referred box starts: that of its first fragment (reference/common/references.md), with
     * its printed number (reference/components/document.md). A box that does not appear on any page (it should not
     * happen: the boxes with {@code Page overlay} are rejected when the tree is built) or that is on a page without a
     * number is an error.
     */
    private static Map<String, String> pagesOf(List<List<LayoutResult>> results, boolean[] blankAfter,
                                               Numbering numbering, Set<String> targets,
                                               List<References.Site> sites) {
        Map<String, String> pages = new LinkedHashMap<>();
        if (targets.isEmpty()) {
            return pages;
        }
        int number = 0;
        for (int k = 0; k < results.size(); k++) {
            for (LayoutResult r : results.get(k)) {
                number++;
                if (pages.size() < targets.size()) {
                    firstPages(r.fragment(), number, targets, pages, numbering, sites);
                }
            }
            // The blank page has nothing to refer to, but it takes up its number.
            number += blankAfter[k] ? 1 : 0;
        }
        for (String id : targets) {
            if (!pages.containsKey(id)) {
                throw new InvalidModelException(0, ErrorCode.REFERENCE_NOT_PLACEABLE, "Box " + id
                        + " does not appear on any page");
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
                throw new InvalidModelException(line, ErrorCode.REFERENCE_NOT_PLACEABLE, "Box " + id
                        + " is in a section without page numbers (Page numbering: none): it has no page to give");
            }
            pages.put(id, numbering.label(number));
        }
        f.children().forEach(child -> firstPages(child, number, targets, pages, numbering, sites));
    }

    private static InvalidModelException unstable(List<References.Site> sites, Map<String, String> assumed,
                                                  Map<String, String> actual) {
        References.Site culprit = sites.stream().filter(s -> s.reference().kind() == Reference.Kind.PAGE
                && !actual.get(s.reference().id()).equals(assumed.get(s.reference().id()))).findFirst().orElseThrow();
        String id = culprit.reference().id();
        return new InvalidModelException(culprit.line(), ErrorCode.REFERENCE_UNSTABLE, "The page of " + id
                + " does not stabilize: after " + MAX_REFERENCE_PASSES + " passes it keeps changing (assuming page "
                + assumed.get(id) + " it appears on page " + actual.get(id) + "); the text of the reference changes "
                + "width with the page and moves the box to another page");
    }

    /**
     * Paginates the flow of each section: one result per page and section. Records in {@code fixed}, per
     * section, the {@code fixed} boxes.
     */
    private List<List<LayoutResult>> flow(List<Section> sections, List<Map<Box, PendingOverlay>> fixed) {
        List<List<LayoutResult>> all = new ArrayList<>();
        int pages = 0;
        for (int k = 0; k < sections.size(); k++) {
            Section section = sections.get(k);
            List<LayoutResult> results = new ArrayList<>();
            BreakToken token = null;
            do {
                ConstraintSpace space = ConstraintSpace.forPage(section.pageSetup());
                LayoutResult r = engine.layout(section.box(), space, token);
                BreakToken next = perPage(r.breakToken());
                if (next instanceof BreakToken.Before || (next != null && next.equals(token))) {
                    throw new InternalException("No progress on page " + (pages + results.size() + 1)
                            + ": the progress guarantee has been broken (reference/common/breaks.md)");
                }
                results.add(r);
                int section0 = k;
                r.pending().forEach(p -> fixed.get(section0).putIfAbsent(p.box(), p));
                token = next;
                if (pages + results.size() > engine.limits().maxPages()) {
                    throw pageLimit();
                }
            } while (token != null);
            pages += results.size();
            all.add(results);
        }
        return all;
    }

    /** The texts of the tree that contain {@code {page}} or {@code {pages}}, in document order. */
    private static List<Box> withPageNumbers(Box box, List<Box> found) {
        if (PageNumbers.contains(box.text())) {
            found.add(box);
        }
        box.children().forEach(child -> withPageNumbers(child, found));
        return found;
    }

    /** The texts with a reference {@code {page of: id}} whose page is not the one assumed. */
    private static List<Box> changedTexts(List<References.Site> sites, Map<String, String> assumed,
                                          Map<String, String> actual) {
        return sites.stream().filter(s -> s.reference().kind() == Reference.Kind.PAGE)
                .filter(s -> !actual.get(s.reference().id()).equals(assumed.get(s.reference().id())))
                .map(References.Site::box).distinct().toList();
    }

    /**
     * The root represents the page area, and its height is that of each page, not the total of all of them: its
     * token does not accumulate consumed height. This way each page has the whole page area again.
     */
    private static BreakToken perPage(BreakToken token) {
        return token instanceof BreakToken.Block b ? new BreakToken.Block(b.nextChild(), b.childToken(), 0) : token;
    }
}
