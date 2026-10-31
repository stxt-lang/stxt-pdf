package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.stxt.pdf.tree.Nodes.child;
import static dev.stxt.pdf.tree.Nodes.error;
import static dev.stxt.pdf.tree.Nodes.inline;
import static dev.stxt.pdf.tree.Nodes.position;
import static dev.stxt.pdf.tree.Nodes.unread;
import static dev.stxt.pdf.tree.PropertyReader.box;
import static dev.stxt.pdf.tree.PropertyReader.fragmentation;
import static dev.stxt.pdf.tree.PropertyReader.typography;

/**
 * Las reglas reutilizables de {@code Styles} (referencia/comun/estilos.md): {@code For element} (de un
 * tipo de elemento, de una posición en una tabla o {@code Document}), {@code For style},
 * {@code For id} y {@code For column}, leídas una vez por llamada.
 *
 * <p>Pueden venir de dos sitios: del {@code Styles} del propio documento y de un documento de
 * estilos aparte (raíz {@code Styles}, referencia/comun/estilos.md). En cada nivel de prioridad van antes las reglas
 * del documento aparte y después las del documento, así que si las dos declaran la misma propiedad gana el documento.
 * Cada origen se lee por separado: una regla repetida es un error dentro de un origen, no entre los dos.</p>
 */
final class StyleRules {

    /** Reglas {@code For element}, por elemento y en el orden en que se aplican. */
    private final Map<Element, List<InlineNode>> elementRules = new EnumMap<>(Element.class);
    /** Reglas {@code For element: Document}, en el orden en que se aplican. */
    private final List<InlineNode> documentRules = new ArrayList<>();
    /** Reglas {@code For style}, en el orden en que se aplican: primero las del documento aparte. */
    private final List<ClassRule> classRules = new ArrayList<>();
    /** Las clases que tienen alguna regla. */
    private final Set<String> classNames = new HashSet<>();
    /**
     * Reglas {@code For id}, por {@code Id}, en el orden en que se aplican. Que ningún elemento tenga ese
     * {@code Id} no es un error: el elemento puede llegar después, de una plantilla.
     */
    private final Map<String, List<InlineNode>> idRules = new HashMap<>();
    /** Reglas {@code For element} de una posición en una tabla ({@code Head cell}, {@code Even row cell}…). */
    private final List<CellRule> cellRules = new ArrayList<>();
    /** Reglas {@code For column}, en el orden en que se aplican. */
    private final List<ColumnRule> columnRules = new ArrayList<>();
    /** Las reglas que vienen del documento de estilos aparte: sus errores lo dicen. */
    private final Set<InlineNode> external = Collections.newSetFromMap(new IdentityHashMap<>());

    /** Una regla {@code For style}. */
    private record ClassRule(String name, InlineNode rule) {
    }

    /** Una regla de posición en una tabla; {@code external} la sitúa antes de las del documento. */
    private record CellRule(CellSelector selector, InlineNode rule, boolean external) {
    }

    /** Una regla {@code For column}: las columnas {@code from} a {@code to}, ambas incluidas. */
    private record ColumnRule(int from, int to, InlineNode rule) {
        String key() {
            return from + " " + to;
        }
    }

    /**
     * Lee las reglas de {@code Styles} del documento y, si lo hay, del documento de estilos
     * aparte ({@code styles}, de raíz {@code Styles}; {@code null} si no hay). Sus valores se
     * comprueban aquí, aunque la regla no se aplique a ningún elemento; lo que depende del
     * elemento (referencia/comun/errores.md), al aplicarla.
     */
    StyleRules(InlineNode document, InlineNode styles) {
        if (styles != null) {
            load(styles, true);
        }
        child(document, "styles").map(Nodes::inline).ifPresent(own -> load(own, false));
    }

    /** Las reglas de un origen; una regla repetida es un error dentro de él. */
    private void load(InlineNode styles, boolean fromFile) {
        Map<String, InlineNode> seen = new HashMap<>();
        for (Node r : styles.getChildren()) {
            InlineNode rule = inline(r);
            String selector = rule.getText().trim();
            String key = rule.getCanonicalName() + ": " + selector;
            if (rule.getCanonicalName().equals("for-element")) {
                key = "element: " + selector.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
            } else if (rule.getCanonicalName().equals("for-column")) {
                key = "for-column: " + columns(rule, fromFile).key();
            }
            InlineNode previous = seen.putIfAbsent(key, rule);
            if (previous != null) {
                throw located(error(rule, ErrorCode.STYLE_DUPLICATE, "Regla repetida: " + rule.getName() + ": "
                        + selector + " (ya está en la línea " + previous.getLine() + ")"), fromFile);
            }
            if (fromFile) {
                external.add(rule);
            }
            switch (rule.getCanonicalName()) {
                case "for-element" -> element(rule, selector, fromFile);
                case "for-column" -> columnRules.add(columns(rule, fromFile));
                case "for-style" -> {
                    String name = name(rule, "For style", fromFile);
                    classRules.add(new ClassRule(name, rule));
                    classNames.add(name);
                }
                case "for-id" -> {
                    String id = name(rule, "For id", fromFile);
                    idRules.computeIfAbsent(id, k -> new ArrayList<>()).add(rule);
                }
                default -> throw unread(rule);
            }
            read(rule, new Style());
        }
    }

    /** Una regla {@code For element}: de un tipo de elemento, de una posición en una tabla o {@code Document}. */
    private void element(InlineNode rule, String selector, boolean fromFile) {
        String canonical = selector.toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
        if (canonical.equals("document")) {
            for (String group : List.of("box", "break")) {
                child(rule, group).ifPresent(g -> {
                    throw located(error(g, ErrorCode.STYLE_NOT_ALLOWED, "For element: Document solo admite Typography, "
                            + "que heredan todas las cajas: el documento no tiene " + g.getName()), fromFile);
                });
            }
            child(rule, "typography").ifPresent(t -> inline(t).getChildren().stream()
                    .filter(c -> c.getCanonicalName().equals("language")).findFirst().ifPresent(l -> {
                        throw located(error(l, ErrorCode.STYLE_NOT_ALLOWED, "For element: Document no admite Language: "
                                + "el idioma del documento es el Language de Document"), fromFile);
                    }));
            documentRules.add(rule);
            return;
        }
        Optional<CellSelector> cell = CellSelector.of(canonical);
        if (cell.isPresent()) {
            cellRules.add(new CellRule(cell.get(), rule, fromFile));
            return;
        }
        Element element = Element.of(canonical)
                .orElseThrow(() -> located(error(rule, ErrorCode.STYLE_UNKNOWN_ELEMENT, "For element: '" + selector
                        + "' no es un elemento de caja ni una posición en una tabla ("
                        + Stream.concat(Stream.concat(Stream.of("Document"),
                                Element.selectable().stream().map(Element::label)),
                                Arrays.stream(CellSelector.values()).map(CellSelector::label))
                        .collect(Collectors.joining(", ")) + ")"), fromFile));
        elementRules.computeIfAbsent(element, k -> new ArrayList<>()).add(rule);
    }

    /** Un error de una regla; si viene del documento de estilos aparte, lo dice. */
    private static InvalidModelException located(InvalidModelException e, boolean fromFile) {
        return fromFile ? e.inStylesDocument() : e;
    }

    /**
     * Aplica al texto de la raíz las reglas {@code For element: Document}: la tipografía que heredan
     * todas las cajas.
     */
    void applyDocument(Style root) {
        for (InlineNode rule : documentRules) {
            read(rule, root);
        }
    }

    /**
     * Aplica la regla {@code For element} de un elemento generado (una entrada del índice,
     * referencia/componentes/table-of-contents.md), si la hay: no tiene nodo, así que no hay clases ni {@code Id} que
     * mirar.
     */
    void apply(Element kind, Style s) {
        for (InlineNode rule : elementRules.getOrDefault(kind, List.of())) {
            read(rule, s);
        }
    }

    /** El rango de una regla {@code For column}: {@code n} o {@code n m}, enteros positivos con {@code n <= m}. */
    private static ColumnRule columns(InlineNode rule, boolean fromFile) {
        String text = rule.getText().trim();
        String[] t = text.split("\\s+");
        if (t.length > 2 || !t[0].matches("[1-9]\\d*") || (t.length == 2 && !t[1].matches("[1-9]\\d*"))) {
            throw located(error(rule, ErrorCode.VALUE_SYNTAX, "For column espera una columna o un rango 'desde hasta', "
                    + "enteros positivos: '" + text + "'"), fromFile);
        }
        int from = Integer.parseInt(t[0]);
        int to = t.length == 2 ? Integer.parseInt(t[1]) : from;
        if (to < from) {
            throw located(error(rule, ErrorCode.VALUE_NOT_ALLOWED,
                    "For column: el rango va de menor a mayor: '" + text + "'"), fromFile);
        }
        return new ColumnRule(from, to, rule);
    }

    /** El nombre de una clase o de un {@code Id} en una regla: una sola palabra. */
    private static String name(InlineNode rule, String what, boolean fromFile) {
        String name = rule.getText().trim();
        if (name.isEmpty() || name.chars().anyMatch(Character::isWhitespace)) {
            throw located(error(rule, ErrorCode.VALUE_NOT_ALLOWED, what + " necesita un solo nombre, sin espacios: '"
                    + name + "'"), fromFile);
        }
        return name;
    }

    /**
     * Aplica a una {@code Section} (referencia/componentes/document.md) las reglas que le llegan: {@code For element:
     * Section}, las de sus clases y la de su {@code Id}. Solo pueden dar {@code Typography}, que hereda todo su
     * contenido; con {@code Box} o {@code Break} son un error (una sección es el área útil de sus páginas, no una
     * caja).
     */
    void applySection(InlineNode n, Style s) {
        List<InlineNode> rules = new ArrayList<>(elementRules.getOrDefault(Element.SECTION, List.of()));
        child(n, "style").ifPresent(c -> {
            Set<String> classes = classes(c);
            for (ClassRule rule : classRules) {
                if (classes.contains(rule.name())) {
                    rules.add(rule.rule());
                }
            }
        });
        child(n, "id").map(id -> idRules.get(id.getText().trim())).ifPresent(rules::addAll);
        for (InlineNode rule : rules) {
            for (String group : List.of("box", "break")) {
                child(rule, group).ifPresent(g -> {
                    throw located(error(g, ErrorCode.STYLE_NOT_ALLOWED, "La regla " + rule.getName() + ": "
                            + rule.getText().trim() + " se aplica a una Section (línea " + n.getLine()
                            + "), que solo admite Typography: es el área útil de sus páginas, no una caja"),
                            external.contains(rule));
                });
            }
            read(rule, s);
        }
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
        List<InlineNode> rules = new ArrayList<>(elementRules.getOrDefault(kind, List.of()));
        applyAll(rules, noBreak, s);
        rules.clear();
        if (position != null) {
            if (position.columnAlign() != null) {
                // Es el valor por defecto de la columna: lo declara la tabla, en la línea de su Column align.
                s.typography().setTextAlign(position.columnAlign());
                s.declared(PropertyKey.internal("align"), position.columnAlignLine());
            }
            // Dentro de este nivel van antes las del documento de estilos aparte, y en cada origen, por posición.
            cellRules.stream().filter(c -> c.selector().matches(position))
                    .sorted(Comparator.comparing((CellRule c) -> !c.external())
                            .thenComparing(c -> c.selector().ordinal()))
                    .forEach(c -> rules.add(c.rule()));
            for (ColumnRule column : columnRules) {
                if (position.column() >= column.from() && position.column() <= column.to()) {
                    rules.add(column.rule());
                }
            }
        }
        // Las clases de la fila de una celda valen menos que las de la propia celda.
        if (position != null && position.rowClass() != null) {
            rules.addAll(classRules(position.rowClass()));
        }
        child(n, "style").ifPresent(c -> rules.addAll(classRules(c)));
        // El For id de la fila vale más que las clases y menos que el For id de la propia celda.
        if (position != null && position.rowId() != null) {
            Optional.ofNullable(idRules.get(position.rowId().getText().trim())).ifPresent(rules::addAll);
        }
        child(n, "id").map(id -> idRules.get(id.getText().trim())).ifPresent(rules::addAll);
        applyAll(rules, noBreak, s);
    }

    /** Las reglas {@code For style} de un {@code Style}, en el orden de {@code Styles}. */
    private List<InlineNode> classRules(Node classNode) {
        Set<String> classes = classes(classNode);
        List<InlineNode> rules = new ArrayList<>();
        for (ClassRule rule : classRules) {
            if (classes.contains(rule.name())) {
                rules.add(rule.rule());
            }
        }
        return rules;
    }

    private void applyAll(List<InlineNode> rules, String noBreak, Style s) {
        for (InlineNode rule : rules) {
            if (noBreak != null) {
                child(rule, "break").ifPresent(b -> {
                    throw located(error(b, ErrorCode.STYLE_NOT_ALLOWED, "La regla " + rule.getName() + ": "
                            + rule.getText().trim() + " aplica Break a " + noBreak), external.contains(rule));
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

    /** Las clases de un {@code Style}: cada una con su regla {@code For style} y sin repetir. */
    private Set<String> classes(Node n) {
        String[] names = n.getText().trim().split("\\s+");
        if (names[0].isEmpty()) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Style necesita al menos un estilo");
        }
        Set<String> classes = new HashSet<>();
        for (String name : names) {
            if (!classes.add(name)) {
                throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Estilo repetido en Style: " + name);
            }
            if (!classNames.contains(name)) {
                throw error(n, ErrorCode.STYLE_WITHOUT_RULE, "El estilo " + name + " no tiene regla: falta "
                        + "'For style: " + name + "' en Styles");
            }
        }
        return classes;
    }

    /**
     * Los grupos de una regla, con la misma lectura que en un elemento: cada valor con su línea, que en una regla del
     * documento de estilos aparte es de ese documento. Un error de una regla del documento de estilos aparte lo dice.
     */
    private void read(InlineNode rule, Style s) {
        boolean fromFile = external.contains(rule);
        s.readingStyles(fromFile);
        try {
            for (Node g : rule.getChildren()) {
                switch (g.getCanonicalName()) {
                    case "box" -> box(inline(g), s);
                    case "typography" -> typography(inline(g), s);
                    case "break" -> fragmentation(inline(g), s);
                    default -> throw unread(g);
                }
            }
        } catch (InvalidModelException e) {
            throw located(e, fromFile);
        } finally {
            s.readingStyles(false);
        }
    }
}
