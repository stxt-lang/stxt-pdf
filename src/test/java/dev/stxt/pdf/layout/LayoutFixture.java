package dev.stxt.pdf.layout;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.stxt.DocumentReader;
import dev.stxt.pdf.tree.TextRuns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lays out a document 595 pt wide with margins of 40 (page area of 515 pt from
 * (40, 40)) and gives the fragments with an {@code Id} and their border box in page coordinates,
 * including those of {@code Page overlay} boxes. Without a page number, it refers to the first one.
 *
 * <p>It declares {@code Font: IBM Plex Sans} and {@code Size: 10} in the {@code Typography} of {@code Document},
 * because the text needs them (reference/common/errors.md); with {@link
 * dev.stxt.pdf.layout.inline.FixedMetrics} the family does not affect the measurements.</p>
 */
public final class LayoutFixture {

    private static final DocumentReader READER = new DocumentReader();
    private static final String FONT = "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n";
    private static final String A4 = "\t\tWidth: 595\n\t\tHeight: 842\n\t\tMargin: 40\n";
    /** Suffix of the key of the marker of an item. */
    private static final String MARKER = "#marker";

    private final List<Page> pages;
    private final List<Map<String, Fragment>> fragments = new ArrayList<>();
    private final List<Map<String, Rect>> rects = new ArrayList<>();

    private LayoutFixture(List<Page> pages) {
        this.pages = pages;
        for (Page page : pages) {
            Map<String, Fragment> f = new HashMap<>();
            Map<String, Rect> r = new HashMap<>();
            walk(page.content().children(), 40, 40, f, r);
            walk(page.pageOverlays(), 40, 40, f, r);
            fragments.add(f);
            rects.add(r);
        }
    }

    /**
     * A4 document (595 × 842; page area of 515 × 762). Lines of the top-level boxes, with
     * tabs as in the document.
     */
    public static LayoutFixture layOut(String... boxLines) {
        return layOut(A4, boxLines);
    }

    /** Document with pages of {@code height} pt: the page area measures {@code height − 80}. */
    public static LayoutFixture layOutPaged(double height, String... boxLines) {
        return layOut(paged(height), boxLines);
    }

    /** Like {@link #layOut(String...)}, with the runs of each text by its {@code Id} ({@link TextRuns}). */
    public static LayoutFixture layOutRuns(Map<String, List<TextRun>> runs, String... boxLines) {
        return layOut(A4, runs, boxLines);
    }

    /** Like {@link #layOutPaged(double, String...)}, with runs ({@link #layOutRuns}). */
    public static LayoutFixture layOutPagedRuns(double height, Map<String, List<TextRun>> runs, String... boxLines) {
        return layOut(paged(height), runs, boxLines);
    }

    private static String paged(double height) {
        return "\t\tWidth: 595\n\t\tHeight: " + height + "\n\t\tMargin: 40\n";
    }

    private static LayoutFixture layOut(String pageSetup, String... boxLines) {
        return layOut(pageSetup, Map.of(), boxLines);
    }

    private static LayoutFixture layOut(String pageSetup, Map<String, List<TextRun>> runs, String... boxLines) {
        String doc = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n" + pageSetup + FONT + String.join("\n", boxLines);
        return new LayoutFixture(new Engine().layOutDocument(TextRuns.build(READER.read(Docs.sectioned(Docs.mergeStyles(doc))), runs)).pages());
    }

    public int pages() {
        return pages.size();
    }

    public Rect rect(String id) {
        return rect(1, id);
    }

    public Rect rect(int page, String id) {
        Rect r = rects.get(page - 1).get(id);
        if (r == null) {
            throw new IllegalArgumentException("There is no fragment with Id " + id + " on page " + page
                    + ": " + rects.get(page - 1).keySet());
        }
        return r;
    }

    public boolean has(int page, String id) {
        return rects.get(page - 1).containsKey(id);
    }

    /** Box of the marker of the {@code List item} with that {@code Id} (reference/components/list.md). */
    public Rect marker(String id) {
        return marker(1, id);
    }

    public Rect marker(int page, String id) {
        return rect(page, id + MARKER);
    }

    public boolean hasMarker(int page, String id) {
        return has(page, id + MARKER);
    }

    public Fragment fragment(String id) {
        return fragment(1, id);
    }

    public Fragment fragment(int page, String id) {
        rect(page, id);
        return fragments.get(page - 1).get(id);
    }

    /** Walks fragments relative to a content box that starts at (x, y). */
    private static void walk(List<Fragment> children, double x, double y,
                             Map<String, Fragment> fragments, Map<String, Rect> rects) {
        for (Fragment f : children) {
            Rect border = new Rect(x + f.x(), y + f.y(), f.width(), f.height());
            if (f.box().isMarker() && f.box().parent().id() != null) {
                rects.put(f.box().parent().id() + MARKER, border);
            }
            if (f.box().id() != null) {
                fragments.put(f.box().id(), f);
                rects.put(f.box().id(), border);
            }
            // In a rotated box in flow, the rectangle is the one it takes up and its children go in its unrotated box.
            Rect content = f.borderBox(x + f.x(), y + f.y()).shrink(f.border()).shrink(f.padding());
            walk(f.children(), content.x(), content.y(), fragments, rects);
        }
    }
}
