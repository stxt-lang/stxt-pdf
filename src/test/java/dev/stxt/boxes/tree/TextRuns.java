package dev.stxt.boxes.tree;

import dev.stxt.InlineNode;
import dev.stxt.boxes.model.TextFormat;
import dev.stxt.boxes.model.TextRun;

import java.util.List;
import java.util.Map;

/**
 * Tramos con formato para los tests (ESPECIFICACION.md §5.4). El documento todavía no tiene
 * sintaxis para escribirlos: se ponen en el árbol ya construido, antes de maquetarlo.
 */
public final class TextRuns {

    private TextRuns() {
    }

    /** Un tramo sin formato. */
    public static TextRun run(String text) {
        return new TextRun(text, TextFormat.PLAIN);
    }

    /** Un tramo con el formato de las letras de {@code format}: {@code b}, {@code i}, {@code u} y {@code s}. */
    public static TextRun run(String text, String format) {
        return new TextRun(text, new TextFormat(format.contains("b"), format.contains("i"), format.contains("u"),
                format.contains("s")));
    }

    /** Un tramo con el formato de {@code format} y un enlace a {@code destination}. */
    public static TextRun link(String text, String format, String destination) {
        return new TextRun(text, run(text, format).format().withLink(destination));
    }

    /** Construye el árbol del documento y pone los tramos de cada caja de texto por su {@code Id}. */
    public static BoxTreeBuilder.Result build(InlineNode document, Map<String, List<TextRun>> runs) {
        BoxTreeBuilder.Result tree = new BoxTreeBuilder().build(document);
        runs.forEach((id, r) -> set(tree.root(), id, r));
        return tree;
    }

    /** Pone los tramos a la caja con este {@code Id}, cuyo texto debe ser el de los tramos seguidos. */
    public static void set(Box root, String id, List<TextRun> runs) {
        Box box = find(root, id);
        if (box == null) {
            throw new IllegalArgumentException("No hay ninguna caja con Id " + id);
        }
        if (!TextRun.text(runs).equals(box.text)) {
            throw new IllegalArgumentException("Los tramos no son el texto de " + id + ": '" + box.text + "'");
        }
        box.runs = List.copyOf(runs);
    }

    private static Box find(Box box, String id) {
        if (id.equals(box.id)) {
            return box;
        }
        for (Box child : box.children) {
            Box found = find(child, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
