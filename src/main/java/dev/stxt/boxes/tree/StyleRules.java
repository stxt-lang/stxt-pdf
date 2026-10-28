package dev.stxt.boxes.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.stxt.boxes.tree.Nodes.child;
import static dev.stxt.boxes.tree.Nodes.error;
import static dev.stxt.boxes.tree.Nodes.inline;
import static dev.stxt.boxes.tree.Nodes.position;
import static dev.stxt.boxes.tree.Nodes.unread;
import static dev.stxt.boxes.tree.PropertyReader.box;
import static dev.stxt.boxes.tree.PropertyReader.fragmentation;
import static dev.stxt.boxes.tree.PropertyReader.typography;

/**
 * Las reglas reutilizables de {@code Styles} (ESPECIFICACION.md §4.10): {@code Element} (de un
 * tipo de elemento o de una posición en una tabla), {@code For class}, {@code For id} y
 * {@code For column}, leídas una vez por documento.
 */
final class StyleRules {

    /** Reglas {@code Element}, por elemento. */
    private final Map<Element, InlineNode> elementRules = new EnumMap<>(Element.class);
    /** Reglas {@code For class}, por clase y en el orden del documento: en ese orden se aplican. */
    private final Map<String, InlineNode> classRules = new LinkedHashMap<>();
    /** Reglas {@code For id}, por {@code Id}. */
    private final Map<String, InlineNode> idRules = new HashMap<>();
    /** Reglas {@code Element} de una posición en una tabla ({@code Head cell}, {@code Even row cell}…). */
    private final Map<CellSelector, InlineNode> cellRules = new EnumMap<>(CellSelector.class);
    /** Reglas {@code For column}, por rango y en el orden del documento: en ese orden se aplican. */
    private final Map<String, ColumnRule> columnRules = new LinkedHashMap<>();

    /** Una regla {@code For column}: las columnas {@code from} a {@code to}, ambas incluidas. */
    private record ColumnRule(int from, int to, InlineNode rule) {
    }

    /**
     * Lee las reglas de {@code Styles} del documento. Sus valores se comprueban aquí, aunque la
     * regla no se aplique a ningún elemento; lo que depende del elemento (§6), al aplicarla.
     */
    StyleRules(InlineNode document) {
        Optional<Node> styles = child(document, "styles");
        if (styles.isEmpty()) {
            return;
        }
        for (Node r : inline(styles.get()).getChildren()) {
            InlineNode rule = inline(r);
            String selector = rule.getText().trim();
            InlineNode previous = switch (rule.getCanonicalName()) {
                case "element" -> {
                    String canonical = selector.toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
                    Optional<CellSelector> cell = CellSelector.of(canonical);
                    if (cell.isPresent()) {
                        yield cellRules.putIfAbsent(cell.get(), rule);
                    }
                    Element element = Element.of(canonical)
                            .orElseThrow(() -> error(rule, ErrorCode.STYLE_UNKNOWN_ELEMENT, "Element: '" + selector
                                    + "' no es un elemento de caja ni una posición en una tabla ("
                                    + Stream.concat(Element.selectable().stream().map(Element::label),
                                            Arrays.stream(CellSelector.values()).map(CellSelector::label))
                                    .collect(Collectors.joining(", ")) + ")"));
                    yield elementRules.putIfAbsent(element, rule);
                }
                case "for-column" -> {
                    ColumnRule column = columns(rule);
                    ColumnRule other = columnRules.putIfAbsent(column.from() + " " + column.to(), column);
                    yield other == null ? null : other.rule();
                }
                case "for-class" -> classRules.putIfAbsent(name(rule, "For class"), rule);
                case "for-id" -> idRules.putIfAbsent(name(rule, "For id"), rule);
                default -> throw unread(rule);
            };
            if (previous != null) {
                throw error(rule, ErrorCode.STYLE_DUPLICATE, "Regla repetida: " + rule.getName() + ": " + selector
                        + " (ya está en la línea " + previous.getLine() + ")");
            }
            read(rule, new Style());
        }
    }

    /**
     * Aplica la regla {@code Element} de un elemento generado (una entrada del índice, §5.19), si
     * la hay: no tiene nodo, así que no hay clases ni {@code Id} que mirar.
     */
    void apply(Element kind, Style s) {
        InlineNode rule = elementRules.get(kind);
        if (rule != null) {
            read(rule, s);
        }
    }

    /** Cada regla {@code For id} tiene su elemento: {@code ids} son los del documento ya construido. */
    void checkIds(Set<String> ids) {
        for (Map.Entry<String, InlineNode> rule : idRules.entrySet()) {
            if (!ids.contains(rule.getKey())) {
                throw error(rule.getValue(), ErrorCode.STYLE_UNKNOWN_ID,
                        "Ningún elemento tiene el Id " + rule.getKey() + " de esta regla For id");
            }
        }
    }

    /** El rango de una regla {@code For column}: {@code n} o {@code n m}, enteros positivos con {@code n <= m}. */
    private static ColumnRule columns(InlineNode rule) {
        String text = rule.getText().trim();
        String[] t = text.split("\\s+");
        if (t.length > 2 || !t[0].matches("[1-9]\\d*") || (t.length == 2 && !t[1].matches("[1-9]\\d*"))) {
            throw error(rule, ErrorCode.VALUE_SYNTAX, "For column espera una columna o un rango 'desde hasta', "
                    + "enteros positivos: '" + text + "'");
        }
        int from = Integer.parseInt(t[0]);
        int to = t.length == 2 ? Integer.parseInt(t[1]) : from;
        if (to < from) {
            throw error(rule, ErrorCode.VALUE_NOT_ALLOWED, "For column: el rango va de menor a mayor: '" + text + "'");
        }
        return new ColumnRule(from, to, rule);
    }

    /** El nombre de una clase o de un {@code Id} en una regla: una sola palabra. */
    private static String name(InlineNode rule, String what) {
        String name = rule.getText().trim();
        if (name.isEmpty() || name.chars().anyMatch(Character::isWhitespace)) {
            throw error(rule, ErrorCode.VALUE_NOT_ALLOWED, what + " necesita un solo nombre, sin espacios: '"
                    + name + "'");
        }
        return name;
    }

    /**
     * Aplica las reglas que corresponden a un elemento que no es una celda de tabla.
     *
     * @see #apply(InlineNode, Element, Style, Box, CellPosition)
     */
    void apply(InlineNode n, Element kind, Style s, Box parent) {
        apply(n, kind, s, parent, null);
    }

    /**
     * Aplica las reglas que corresponden a un elemento, de menos a más prioridad: la de su tipo,
     * las de la posición de una celda en su tabla (primero el {@code Column align} de su columna,
     * luego {@code Head cell}, {@code Body cell} o {@code Foot cell}, y {@code Odd row cell} o
     * {@code Even row cell}), las de su columna ({@code For column}, en el orden de
     * {@code Styles}), las de sus clases (en el orden de {@code Styles}) y la de su {@code Id}.
     * Van después de la herencia y antes de lo que declara el propio elemento, y se mezclan
     * propiedad a propiedad. {@code position} es la de una {@code Cell}; {@code null} en los demás.
     */
    void apply(InlineNode n, Element kind, Style s, Box parent, CellPosition position) {
        String noBreak = noBreak(n, kind, s, parent);
        List<InlineNode> rules = new ArrayList<>();
        Optional.ofNullable(elementRules.get(kind)).ifPresent(rules::add);
        applyAll(rules, noBreak, s);
        rules.clear();
        if (position != null) {
            if (position.columnAlign() != null) {
                // Es el valor por defecto de la columna: lo declara la tabla, en la línea de su Column align.
                s.textAlign = position.columnAlign();
                s.declared(PropertyKey.internal("align"), position.columnAlignLine());
            }
            for (CellSelector selector : CellSelector.values()) {
                if (selector.matches(position)) {
                    Optional.ofNullable(cellRules.get(selector)).ifPresent(rules::add);
                }
            }
            for (ColumnRule column : columnRules.values()) {
                if (position.column() >= column.from() && position.column() <= column.to()) {
                    rules.add(column.rule());
                }
            }
        }
        Optional<Node> classNode = child(n, "class");
        if (classNode.isPresent()) {
            Set<String> classes = classes(classNode.get());
            classRules.forEach((name, rule) -> {
                if (classes.contains(name)) {
                    rules.add(rule);
                }
            });
        }
        child(n, "id").map(id -> idRules.get(id.getText().trim())).ifPresent(rules::add);
        applyAll(rules, noBreak, s);
    }

    private static void applyAll(List<InlineNode> rules, String noBreak, Style s) {
        for (InlineNode rule : rules) {
            if (noBreak != null) {
                child(rule, "break").ifPresent(b -> {
                    throw error(b, ErrorCode.STYLE_NOT_ALLOWED, "La regla " + rule.getName() + ": "
                            + rule.getText().trim() + " aplica Break a " + noBreak);
                });
            }
            read(rule, s);
        }
    }

    /**
     * Por qué el elemento no admite {@code Break}, para el mensaje; {@code null} si lo admite.
     * Lo que el schema no deja declarar tampoco puede llegar de una regla.
     */
    private static String noBreak(InlineNode n, Element kind, Style s, Box parent) {
        String where = " (línea " + n.getLine() + "), ";
        if (s.isOutOfFlow()) {
            return "un " + position(s) + where + "que no se parte ni está en el flujo de la página";
        }
        if (kind.headingLevel() > 0) {
            return "un " + n.getName() + where + "en el que el motor ya evita el corte";
        }
        if (parent.headingLevel() > 0) {
            return "un Text de un encabezado" + where + "en el que el motor ya evita el corte";
        }
        if (kind == Element.CELL) {
            return "una Cell" + where + "cuyos cortes son los de su Row";
        }
        return null;
    }

    /** Las clases de un {@code Class}: cada una con su regla {@code For class} y sin repetir. */
    private Set<String> classes(Node n) {
        String[] names = n.getText().trim().split("\\s+");
        if (names[0].isEmpty()) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Class necesita al menos una clase");
        }
        Set<String> classes = new HashSet<>();
        for (String name : names) {
            if (!classes.add(name)) {
                throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Clase repetida en Class: " + name);
            }
            if (!classRules.containsKey(name)) {
                throw error(n, ErrorCode.CLASS_WITHOUT_RULE, "La clase " + name + " no tiene regla: añade "
                        + "'For class: " + name + "' en Styles");
            }
        }
        return classes;
    }

    /** Los grupos de una regla, con la misma lectura que en un elemento: cada valor con su línea. */
    private static void read(InlineNode rule, Style s) {
        for (Node g : rule.getChildren()) {
            switch (g.getCanonicalName()) {
                case "box" -> box(inline(g), s);
                case "typography" -> typography(inline(g), s);
                case "break" -> fragmentation(inline(g), s);
                default -> throw unread(g);
            }
        }
    }
}
