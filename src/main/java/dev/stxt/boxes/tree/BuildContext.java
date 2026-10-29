package dev.stxt.boxes.tree;

import dev.stxt.Node;
import dev.stxt.boxes.model.TextRun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    /**
     * Si el alto de la caja de contenido es definido: base de los porcentajes verticales (referencia/comun/valores.md).
     */
    final Map<Box, Boolean> definiteHeight = new HashMap<>();
    /** Número de cada {@code List item} ya construido: el siguiente continúa desde él. */
    final Map<Box, Integer> itemNumbers = new HashMap<>();
    /** Nivel del último encabezado, en orden del documento; 0 antes del primero. */
    int headingLevel;
    /** Los contadores de la numeración de los encabezados ({@code Numbered}, referencia/componentes/heading.md). */
    final HeadingNumbers headingNumbers = new HeadingNumbers();
    /** Si se está construyendo el contenido de un {@code Fixed}. */
    boolean insideFixed;
    /** Si se está construyendo el contenido de un {@code Multicol}. */
    boolean insideMulticol;
    /**
     * Si se está construyendo el contenido de un {@code Markdown}: sus textos ya vienen interpretados
     * (referencia/componentes/markdown.md).
     */
    boolean insideMarkdown;
    /** Los {@code Contents} del documento, en orden: sus entradas se generan al final, con todos los encabezados. */
    final List<Box> contents = new ArrayList<>();
    /**
     * Tramos de los textos con formato que genera {@link MarkdownBuilder}, por nodo
     * (referencia/comun/formato-del-texto.md).
     */
    final Map<Node, List<TextRun>> runs = new IdentityHashMap<>();
    /**
     * Los {@code Text} que {@link MarkdownBuilder} genera como bloques de código: su elemento es {@code Code block}
     * (referencia/componentes/markdown.md).
     */
    final Set<Node> codeBlocks = Collections.newSetFromMap(new IdentityHashMap<>());

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
