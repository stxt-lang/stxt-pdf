package dev.stxt.boxes.tree;

import dev.stxt.Node;
import dev.stxt.boxes.model.TextRun;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static dev.stxt.boxes.tree.Nodes.error;

/**
 * Estado de una llamada a {@link BoxTreeBuilder#build}: lo que se va sabiendo del documento
 * mientras se construye el árbol, en orden. Uno por documento, de modo que el builder no guarda
 * estado entre llamadas.
 */
final class BuildContext {

    /** Las reglas de {@code Styles} del documento. */
    final StyleRules rules;
    /** Línea del {@code Id} de cada caja, para detectar los repetidos. */
    final Map<String, Integer> ids = new HashMap<>();
    /** Si el alto de la caja de contenido es definido: base de los porcentajes verticales (§3.6). */
    final Map<Box, Boolean> definiteHeight = new HashMap<>();
    /** Número de cada {@code List item} ya construido: el siguiente continúa desde él. */
    final Map<Box, Integer> itemNumbers = new HashMap<>();
    /** Nivel del último encabezado, en orden del documento; 0 antes del primero. */
    int headingLevel;
    /** Si se está construyendo el contenido de un {@code Fixed}. */
    boolean insideFixed;
    /** Si se está construyendo el contenido de un {@code Multicol}. */
    boolean insideMulticol;
    /** Tramos de los textos con formato que genera {@link MarkdownBuilder}, por nodo (§5.4). */
    final Map<Node, List<TextRun>> runs = new IdentityHashMap<>();

    BuildContext(StyleRules rules) {
        this.rules = rules;
    }

    /** El {@code Id} de una caja, que no se repite en el documento. */
    String id(Node p) {
        String id = p.getText().trim();
        Integer previous = ids.putIfAbsent(id, p.getLine());
        if (previous != null) {
            throw error(p, ErrorCode.DUPLICATE_ID, "Id repetido: " + id + " (ya se usa en la línea " + previous + ")");
        }
        return id;
    }

    /** Si el alto de la caja de contenido es definido; la caja ya está construida. */
    boolean isDefinite(Box box) {
        return definiteHeight.get(box);
    }
}
