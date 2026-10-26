package dev.stxt.boxes.stxt;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Nodo STXT neutral, independiente de la librería de parseo. El resto del motor solo
 * trabaja con este tipo, de modo que cambiar de lector no afecta al layout.
 *
 * @param name      nombre del nodo tal como aparece en el documento
 * @param namespace namespace efectivo (heredado si no se declara), o {@code null}
 * @param value     valor inline ({@code Nombre: valor}), o {@code null}
 * @param block     contenido de un bloque ({@code Nombre >>}), o {@code null}
 * @param children  hijos en orden de aparición (STXT preserva el orden)
 * @param line      línea del documento, para los mensajes de error
 */
public record StxtNode(String name, String namespace, String value, String block,
                       List<StxtNode> children, int line) {

    public StxtNode {
        children = List.copyOf(children);
    }

    /** Compara por nombre canónico simplificado (espacios compactados, sin mayúsculas). */
    public boolean is(String otherName) {
        return canonical(name).equals(canonical(otherName));
    }

    public Optional<StxtNode> child(String childName) {
        return children.stream().filter(h -> h.is(childName)).findFirst();
    }

    public List<StxtNode> children(String childName) {
        return children.stream().filter(h -> h.is(childName)).toList();
    }

    /** Valor inline o contenido de bloque, el que exista. */
    public String content() {
        return block != null ? block : (value != null ? value : "");
    }

    public Optional<String> contentOf(String childName) {
        return child(childName).map(StxtNode::content);
    }

    /**
     * Forma canónica simplificada para comparar nombres. La canonicalización completa la define
     * STXT-SPEC §4.3 y la aplica la librería oficial; aquí basta para el subconjunto del modelo.
     */
    public static String canonical(String s) {
        return s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
