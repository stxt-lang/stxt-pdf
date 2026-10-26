package dev.stxt.boxes.layout;

import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.stxt.DocumentReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maqueta un documento continuo de 595 pt con márgenes de 40 (área útil de 515 pt desde
 * (40, 40)) y da los fragmentos con {@code Id} y su caja de borde en coordenadas de página.
 */
public final class LayoutFixture {

    private static final DocumentReader READER = new DocumentReader();

    private final Map<String, Fragment> fragments = new HashMap<>();
    private final Map<String, Rect> rects = new HashMap<>();

    private LayoutFixture(Page page) {
        walk(page.content(), 40, 40);
    }

    /** Líneas de las cajas de primer nivel, con tabuladores como en el documento. */
    public static LayoutFixture layOut(String... boxLines) {
        String doc = "Document (dev.stxt.boxes):\n\tMedia:\n\t\tType: continuous\n\t\tWidth: 595\n\t\tMargin: 40\n"
                + String.join("\n", boxLines);
        List<Page> pages = new Engine().layOutDocument(READER.read(doc));
        return new LayoutFixture(pages.get(0));
    }

    public Rect rect(String id) {
        Rect r = rects.get(id);
        if (r == null) {
            throw new IllegalArgumentException("No hay fragmento con Id " + id + ": " + rects.keySet());
        }
        return r;
    }

    public Fragment fragment(String id) {
        rect(id);
        return fragments.get(id);
    }

    /** Recorre los hijos de un fragmento cuya caja de contenido empieza en (x, y). */
    private void walk(Fragment parent, double x, double y) {
        for (Fragment f : parent.children()) {
            Rect border = new Rect(x + f.x(), y + f.y(), f.width(), f.height());
            if (f.box().id != null) {
                fragments.put(f.box().id, f);
                rects.put(f.box().id, border);
            }
            Rect content = border.shrink(f.border()).shrink(f.box().padding);
            walk(f, content.x(), content.y());
        }
    }
}
