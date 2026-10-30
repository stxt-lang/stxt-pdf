package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.Reference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Las referencias de los textos del árbol a otras cajas ({@code {page of: id}},
 * referencia/comun/referencias.md): dónde están y la comprobación de que todas señalan algo que se puede referir. Lo
 * que valen lo decide quien maqueta: la página, el {@link dev.stxt.pdf.layout.Paginator}; el número, el builder
 * ({@link Box#number()}).
 */
public final class References {

    /** Una referencia dentro del texto de una caja. */
    public record Site(Box box, Reference reference) {

        /** Línea del documento donde está la referencia. */
        public int line() {
            return box.textLineAt(reference.start());
        }
    }

    private References() {
    }

    /** Todas las referencias de los textos de un árbol, en orden del documento. */
    public static List<Site> sites(Box root) {
        List<Site> sites = new ArrayList<>();
        collect(root, sites);
        return sites;
    }

    private static void collect(Box box, List<Site> sites) {
        if (box.text() != null) {
            for (Reference r : Reference.find(box.text())) {
                sites.add(new Site(box, r));
            }
        }
        box.children().forEach(child -> collect(child, sites));
    }

    /** Las cajas que se pueden referir (con {@code Id}, o encabezados de un índice) de un árbol, por su {@link Box#key}. */
    public static Map<String, Box> byId(Box root) {
        Map<String, Box> boxes = new LinkedHashMap<>();
        index(root, boxes);
        return boxes;
    }

    private static void index(Box box, Map<String, Box> boxes) {
        if (box.key() != null) {
            boxes.put(box.key(), box);
        }
        box.children().forEach(child -> index(child, boxes));
        box.rows().forEach(row -> index(row, boxes));
    }

    /**
     * Comprueba que cada referencia señala una caja con ese {@code Id} y que se puede referir:
     * la página, de una caja que no esté en un {@code Fixed} (que está en todas las páginas), y
     * el número, de una que lo tenga.
     *
     * @throws InvalidModelException con la línea de la referencia
     */
    static void validate(Box root) {
        List<Site> sites = sites(root);
        if (sites.isEmpty()) {
            return;
        }
        Map<String, Box> boxes = byId(root);
        for (Site site : sites) {
            Reference r = site.reference();
            String text = text(r);
            Box target = boxes.get(r.id());
            if (r.id().isEmpty()) {
                throw new InvalidModelException(site.line(), ErrorCode.REFERENCE_UNKNOWN, text
                        + " necesita el Id de una caja: {" + r.kind().keyword() + " of: id}");
            }
            if (target == null) {
                throw new InvalidModelException(site.line(), ErrorCode.REFERENCE_UNKNOWN, text
                        + ": ninguna caja tiene el Id " + r.id());
            }
            switch (r.kind()) {
                case PAGE -> {
                    if (insideFixed(target)) {
                        throw new InvalidModelException(site.line(), ErrorCode.REFERENCE_NOT_PLACEABLE, text
                                + ": " + r.id() + " está en un Fixed, que se repite en todas las páginas");
                    }
                }
                case NUMBER -> {
                    if (target.number() == null) {
                        throw new InvalidModelException(site.line(), ErrorCode.REFERENCE_NOT_NUMBERED, text
                                + ": " + r.id() + " no tiene número (solo lo tienen los ítems de una lista "
                                + "numerada y los encabezados con Numbered: true)");
                    }
                }
            }
        }
    }

    /** Cómo se escribió la referencia, para los mensajes. */
    private static String text(Reference r) {
        return "{" + r.kind().keyword() + " of: " + r.id() + "}";
    }

    /** Si la caja es un {@code Fixed} o está dentro de uno. */
    static boolean insideFixed(Box box) {
        for (Box b = box; b != null; b = b.parent()) {
            if (b.style().position() == Position.FIXED) {
                return true;
            }
        }
        return false;
    }
}
