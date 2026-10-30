package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Links;
import dev.stxt.pdf.model.TextFormat;

import java.util.Map;

/**
 * Los enlaces del árbol a otras cajas ({@code #id}, referencia/comun/enlaces.md): comprueba que cada
 * uno señala una caja con ese {@code Id} y que tiene un único sitio al que llevar. La sintaxis del
 * destino y su esquema se comprueban al leerlo; esto necesita el árbol entero, porque un enlace
 * puede ir hacia delante. Los enlaces a una URL de fuera no se comprueban más.
 */
final class LinkTargets {

    private LinkTargets() {
    }

    /**
     * Comprueba los enlaces internos de la caja y de sus descendientes: los de {@code Link} y los
     * de los tramos de su texto.
     *
     * @throws InvalidModelException con la línea del {@code Link} o, en un tramo, la de su texto
     */
    static void validate(Box root) {
        Map<String, Box> boxes = References.byId(root);
        check(root, boxes);
    }

    private static void check(Box box, Map<String, Box> boxes) {
        String link = box.style().link();
        if (link != null && Links.internal(link)) {
            SourceLine line = box.style().isDeclared("link") ? box.style().line("link") : SourceLine.of(box.line());
            target(link, line, boxes);
        }
        if (box.formattedRuns() != null) {
            for (var run : box.formattedRuns()) {
                TextFormat format = run.format();
                if (format.link() != null && Links.internal(format.link())) {
                    target(format.link(), SourceLine.of(box.textLine()), boxes);
                }
            }
        }
        box.children().forEach(child -> check(child, boxes));
    }

    private static void target(String destination, SourceLine line, Map<String, Box> boxes) {
        String id = Links.target(destination);
        Box target = boxes.get(id);
        if (target == null) {
            throw new InvalidModelException(line, ErrorCode.LINK_UNKNOWN, "El enlace " + destination
                    + " lleva a un Id que no existe: ninguna caja tiene el Id " + id);
        }
        if (References.insideFixed(target)) {
            throw new InvalidModelException(line, ErrorCode.LINK_UNKNOWN, "El enlace " + destination + " lleva a "
                    + id + ", que está en un Page overlay y se repite en todas las páginas: no tiene un único destino");
        }
    }
}
