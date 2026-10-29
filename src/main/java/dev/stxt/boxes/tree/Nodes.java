package dev.stxt.boxes.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.boxes.model.Position;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Acceso a los nodos de {@code stxt-core} y errores del builder: lo comparten
 * {@link BoxTreeBuilder} y las clases en que se apoya.
 *
 * <p>Los nombres se comparan por su nombre canónico ({@link Node#getCanonicalName()}:
 * minúsculas y los espacios convertidos en guiones, de modo que {@code Min width} es
 * {@code min-width}). La estructura ya está validada contra el schema, así que los nodos
 * obligatorios existen y los que tienen hijos no son bloques de texto.</p>
 */
final class Nodes {

    private Nodes() {
    }

    /** Compara con un nombre ya canónico ({@code "min-width"}). */
    static boolean is(Node n, String canonicalName) {
        return n.getCanonicalName().equals(canonicalName);
    }

    /** Si el nodo es un elemento de caja. */
    static boolean isElement(Node n) {
        return Element.of(n.getCanonicalName()).filter(Element::isWritten).isPresent();
    }

    /** Si el nodo es el elemento de un {@code Item} o un {@code Area}. */
    static boolean isItemElement(Node n) {
        return Element.of(n.getCanonicalName()).filter(Element::isWritten).filter(Element::canBeItem).isPresent();
    }

    /** Primer hijo con ese nombre canónico. */
    static Optional<Node> child(InlineNode n, String canonicalName) {
        return n.getChildren().stream().filter(h -> is(h, canonicalName)).findFirst();
    }

    /** Un nodo con hijos: el schema no admite que se escriba como bloque de texto ({@code >>}). */
    static InlineNode inline(Node n) {
        return (InlineNode) n;
    }

    /** Hijo obligatorio ({@code "Type"}), que el schema garantiza. */
    static Node required(InlineNode n, String child) {
        return child(n, child.toLowerCase(Locale.ROOT)).orElseThrow();
    }

    /** Nombre de una propiedad para los mensajes: {@code min-width} → {@code Min width}. */
    static String label(String property) {
        String s = property.replace('-', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Nombre del elemento posicionado: {@code Absolute} o {@code Fixed}. */
    static String position(Style s) {
        return s.position == Position.FIXED ? "Fixed" : "Absolute";
    }

    /** Interpreta un valor; los errores de sintaxis llevan la línea del nodo. */
    static <T> T parse(Node n, Supplier<T> parser) {
        try {
            return parser.get();
        } catch (IllegalArgumentException ex) {
            throw error(n, ErrorCode.VALUE_SYNTAX, ex.getMessage());
        }
    }

    /** Interpreta {@code Areas} o {@code Area}: sus errores son de colocación y llevan la línea del nodo. */
    static <T> T placement(Node n, Supplier<T> parser) {
        try {
            return parser.get();
        } catch (IllegalArgumentException ex) {
            throw error(n, ErrorCode.GRID_PLACEMENT, ex.getMessage());
        }
    }

    /**
     * Un nodo que el schema admite y el builder no lee: un error del motor, no del documento.
     * Al añadir una propiedad al schema hay que leerla en {@link PropertyReader} o en
     * {@link BoxTreeBuilder} (lo comprueba {@code SchemaCoverageTest}).
     */
    static IllegalStateException unread(Node n) {
        return new IllegalStateException("El schema admite " + n.getName() + " (línea " + n.getLine()
                + "), pero el builder no lo lee");
    }

    static InvalidModelException error(Node n, ErrorCode code, String message) {
        return error(n.getLine(), code, message);
    }

    static InvalidModelException error(int line, ErrorCode code, String message) {
        return new InvalidModelException(line, code, message);
    }
}
