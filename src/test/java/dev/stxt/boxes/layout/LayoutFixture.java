package dev.stxt.boxes.layout;

import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.stxt.DocumentReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maqueta un documento de 595 pt de ancho con márgenes de 40 (área útil de 515 pt desde
 * (40, 40)) y da los fragmentos con {@code Id} y su caja de borde en coordenadas de página,
 * incluidos los de las cajas {@code fixed}. Sin número de página, se refiere a la primera.
 *
 * <p>Declara {@code Font} en cada caja de primer nivel, porque el texto la necesita (§6); con
 * {@link dev.stxt.boxes.layout.inline.FixedMetrics} la familia no influye en las medidas.</p>
 */
public final class LayoutFixture {

    private static final DocumentReader READER = new DocumentReader();
    private static final String FONT = "\t\tFont: IBM Plex Sans";

    private final List<Page> pages;
    private final List<Map<String, Fragment>> fragments = new ArrayList<>();
    private final List<Map<String, Rect>> rects = new ArrayList<>();

    private LayoutFixture(List<Page> pages) {
        this.pages = pages;
        for (Page page : pages) {
            Map<String, Fragment> f = new HashMap<>();
            Map<String, Rect> r = new HashMap<>();
            walk(page.content().children(), 40, 40, f, r);
            walk(page.fixed(), 40, 40, f, r);
            fragments.add(f);
            rects.add(r);
        }
    }

    /** Documento continuo. Líneas de las cajas de primer nivel, con tabuladores como en el documento. */
    public static LayoutFixture layOut(String... boxLines) {
        return layOut("\t\tType: continuous\n\t\tWidth: 595\n\t\tMargin: 40\n", boxLines);
    }

    /** Documento paginado con páginas de {@code height} pt: el área útil mide {@code height − 80}. */
    public static LayoutFixture layOutPaged(double height, String... boxLines) {
        return layOut("\t\tType: paged\n\t\tWidth: 595\n\t\tHeight: " + height + "\n\t\tMargin: 40\n", boxLines);
    }

    private static LayoutFixture layOut(String media, String... boxLines) {
        List<String> lines = new ArrayList<>();
        for (String line : boxLines) {
            lines.add(line);
            if (line.equals("\tBox:")) {
                lines.add(FONT);
            }
        }
        String doc = "Document (dev.stxt.boxes):\n\tMedia:\n" + media + String.join("\n", lines);
        return new LayoutFixture(new Engine().layOutDocument(READER.read(doc)));
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
            throw new IllegalArgumentException("No hay fragmento con Id " + id + " en la página " + page
                    + ": " + rects.get(page - 1).keySet());
        }
        return r;
    }

    public boolean has(int page, String id) {
        return rects.get(page - 1).containsKey(id);
    }

    public Fragment fragment(String id) {
        return fragment(1, id);
    }

    public Fragment fragment(int page, String id) {
        rect(page, id);
        return fragments.get(page - 1).get(id);
    }

    /** Recorre fragmentos relativos a una caja de contenido que empieza en (x, y). */
    private static void walk(List<Fragment> children, double x, double y,
                             Map<String, Fragment> fragments, Map<String, Rect> rects) {
        for (Fragment f : children) {
            Rect border = new Rect(x + f.x(), y + f.y(), f.width(), f.height());
            if (f.box().id != null) {
                fragments.put(f.box().id, f);
                rects.put(f.box().id, border);
            }
            Rect content = border.shrink(f.border()).shrink(f.padding());
            walk(f.children(), content.x(), content.y(), fragments, rects);
        }
    }
}
