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
 * The reusable rules of {@code Styles} (reference/common/styles.md): {@code For element} (of an element kind, of a
 * position in a table or {@code Document}), {@code For style}, {@code For id} and {@code For column}, read once per
 * call.
 *
 * <p>They can come from two places: from the {@code Styles} of the document itself and from a separate styles document
 * (root {@code Styles}, reference/common/styles.md). At each priority level the rules of the separate document go first
 * and those of the document after, so if both declare the same property the document wins. Each source is read
 * separately: a repeated rule is an error within one source, not between the two.</p>
 */
final class StyleRules {

    /** {@code For element} rules, by element and in the order they are applied. */
    private final Map<Element, List<InlineNode>> elementRules = new EnumMap<>(Element.class);
    /** {@code For element: Document} rules, in the order they are applied. */
    private final List<InlineNode> documentRules = new ArrayList<>();
    /** {@code For style} rules, in the order they are applied: those of the separate document first. */
    private final List<ClassRule> classRules = new ArrayList<>();
    /** The classes that have some rule. */
    private final Set<String> classNames = new HashSet<>();
    /**
     * {@code For id} rules, by {@code Id}, in the order they are applied. That no element has that {@code Id} is not an
     * error: the element can come later, from a template.
     */
    private final Map<String, List<InlineNode>> idRules = new HashMap<>();
    /** {@code For element} rules of a position in a table ({@code Header cell}, {@code Even row cell}...). */
    private final List<CellRule> cellRules = new ArrayList<>();
    /** {@code For column} rules, in the order they are applied. */
    private final List<ColumnRule> columnRules = new ArrayList<>();
    /** The rules that come from the separate styles document: their errors say so. */
    private final Set<InlineNode> external = Collections.newSetFromMap(new IdentityHashMap<>());

    /** A {@code For style} rule. */
    private record ClassRule(String name, InlineNode rule) {
    }

    /** A rule of a position in a table; {@code external} places it before those of the document. */
    private record CellRule(CellSelector selector, InlineNode rule, boolean external) {
    }

    /** A {@code For column} rule: columns {@code from} to {@code to}, both included. */
    private record ColumnRule(int from, int to, InlineNode rule) {
        String key() {
            return from + " " + to;
        }
    }

    /**
     * Reads the rules of {@code Styles} of the document and, if there is one, of the separate styles document
     * ({@code styles}, with root {@code Styles}; {@code null} if there is none). Their values are checked here, even if
     * the rule applies to no element; what depends on the element (reference/common/errors.md), when it is applied.
     */
    StyleRules(InlineNode document, InlineNode styles) {
        if (styles != null) {
            load(styles, true);
        }
        child(document, "styles").map(Nodes::inline).ifPresent(own -> load(own, false));
    }

    /** The rules of one source; a repeated rule is an error within it. */
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
                throw located(error(rule, ErrorCode.STYLE_DUPLICATE, "Repeated rule: " + rule.getName() + ": "
                        + selector + " (already on line " + previous.getLine() + ")"), fromFile);
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

    /** A {@code For element} rule: of an element kind, of a position in a table or {@code Document}. */
    private void element(InlineNode rule, String selector, boolean fromFile) {
        String canonical = selector.toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
        if (canonical.equals("document")) {
            for (String group : List.of("box", "break")) {
                child(rule, group).ifPresent(g -> {
                    throw located(error(g, ErrorCode.STYLE_NOT_ALLOWED, "For element: Document only allows Typography, "
                            + "which all boxes inherit: the document has no " + g.getName()), fromFile);
                });
            }
            child(rule, "typography").ifPresent(t -> inline(t).getChildren().stream()
                    .filter(c -> c.getCanonicalName().equals("language")).findFirst().ifPresent(l -> {
                        throw located(error(l, ErrorCode.STYLE_NOT_ALLOWED, "For element: Document does not allow Language: "
                                + "the language of the document is the Language of Document"), fromFile);
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
                        + "' is not a box element or a position in a table ("
                        + Stream.concat(Stream.concat(Stream.of("Document"),
                                Element.selectable().stream().map(Element::label)),
                                Arrays.stream(CellSelector.values()).map(CellSelector::label))
                        .collect(Collectors.joining(", ")) + ")"), fromFile));
        elementRules.computeIfAbsent(element, k -> new ArrayList<>()).add(rule);
    }

    /** An error of a rule; if it comes from the separate styles document, it says so. */
    private static InvalidModelException located(InvalidModelException e, boolean fromFile) {
        return fromFile ? e.inStylesDocument() : e;
    }

    /**
     * Applies the {@code For element: Document} rules to the text of the root: the typography that all boxes inherit.
     */
    void applyDocument(Style root) {
        for (InlineNode rule : documentRules) {
            read(rule, root);
        }
    }

    /**
     * Applies the {@code For element} rule of a generated element (an entry of the table of contents,
     * reference/components/table-of-contents.md), if there is one: it has no node, so there are no classes or
     * {@code Id} to look at.
     */
    void apply(Element kind, Style s) {
        for (InlineNode rule : elementRules.getOrDefault(kind, List.of())) {
            read(rule, s);
        }
    }

    /** The range of a {@code For column} rule: {@code n} or {@code n m}, positive integers with {@code n <= m}. */
    private static ColumnRule columns(InlineNode rule, boolean fromFile) {
        String text = rule.getText().trim();
        String[] t = text.split("\\s+");
        if (t.length > 2 || !t[0].matches("[1-9]\\d*") || (t.length == 2 && !t[1].matches("[1-9]\\d*"))) {
            throw located(error(rule, ErrorCode.VALUE_SYNTAX, "For column expects a column or a range 'from to', "
                    + "positive integers: '" + text + "'"), fromFile);
        }
        int from = Integer.parseInt(t[0]);
        int to = t.length == 2 ? Integer.parseInt(t[1]) : from;
        if (to < from) {
            throw located(error(rule, ErrorCode.VALUE_NOT_ALLOWED,
                    "For column: the range goes from lower to higher: '" + text + "'"), fromFile);
        }
        return new ColumnRule(from, to, rule);
    }

    /** The name of a class or of an {@code Id} in a rule: a single word. */
    private static String name(InlineNode rule, String what, boolean fromFile) {
        String name = rule.getText().trim();
        if (name.isEmpty() || name.chars().anyMatch(Character::isWhitespace)) {
            throw located(error(rule, ErrorCode.VALUE_NOT_ALLOWED, what + " needs a single name, without spaces: '"
                    + name + "'"), fromFile);
        }
        return name;
    }

    /**
     * Applies to a {@code Section} (reference/components/document.md) the rules that reach it:
     * {@code For element: Section}, those of its classes and that of its {@code Id}. They can only give
     * {@code Typography}, which all its content inherits; with {@code Box} or {@code Break} they are an error (a
     * section is the page area of its pages, not a box).
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
                    throw located(error(g, ErrorCode.STYLE_NOT_ALLOWED, "The rule " + rule.getName() + ": "
                            + rule.getText().trim() + " applies to a Section (line " + n.getLine()
                            + "), which only allows Typography: it is the page area of its pages, not a box"),
                            external.contains(rule));
                });
            }
            read(rule, s);
        }
    }

    /**
     * Applies the rules that correspond to an element that is not a table cell.
     *
     * @see #apply(InlineNode, Element, Style, Box, CellPosition)
     */
    void apply(InlineNode n, Element kind, Style s, Box parent) {
        apply(n, kind, s, parent, null);
    }

    /**
     * Applies the rules that correspond to an element, from lower to higher priority: that of its kind, those of the
     * position of a cell in its table (first the {@code Column align} of its column, then {@code Header cell},
     * {@code Body cell} or {@code Footer cell}, and {@code Odd row cell} or {@code Even row cell}), those of its column
     * ({@code For column}, in the order of {@code Styles}), those of its classes (in the order of {@code Styles}) and
     * that of its {@code Id}. They go after inheritance and before what the element itself declares, and they are
     * merged property by property. {@code position} is that of a {@code Cell}; {@code null} for the others.
     */
    void apply(InlineNode n, Element kind, Style s, Box parent, CellPosition position) {
        String noBreak = noBreak(n, kind, s, parent);
        List<InlineNode> rules = new ArrayList<>(elementRules.getOrDefault(kind, List.of()));
        applyAll(rules, noBreak, s);
        rules.clear();
        if (position != null) {
            if (position.columnAlign() != null) {
                // It is the default value of the column: the table declares it, on the line of its Column align.
                s.typography().setTextAlign(position.columnAlign());
                s.declared(PropertyKey.internal("align"), position.columnAlignLine());
            }
            // Within this level those of the separate styles document go first, and in each source, by position.
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
        // The classes of the row of a cell weigh less than those of the cell itself.
        if (position != null && position.rowClass() != null) {
            rules.addAll(classRules(position.rowClass()));
        }
        child(n, "style").ifPresent(c -> rules.addAll(classRules(c)));
        // The For id of the row weighs more than the classes and less than the For id of the cell itself.
        if (position != null && position.rowId() != null) {
            Optional.ofNullable(idRules.get(position.rowId().getText().trim())).ifPresent(rules::addAll);
        }
        child(n, "id").map(id -> idRules.get(id.getText().trim())).ifPresent(rules::addAll);
        applyAll(rules, noBreak, s);
    }

    /** The {@code For style} rules of a {@code Style}, in the order of {@code Styles}. */
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
                    throw located(error(b, ErrorCode.STYLE_NOT_ALLOWED, "The rule " + rule.getName() + ": "
                            + rule.getText().trim() + " applies Break to " + noBreak), external.contains(rule));
                });
            }
            read(rule, s);
        }
    }

    /**
     * Why the element does not allow {@code Break}, for the message; {@code null} if it allows it. What the schema does
     * not allow to be declared cannot come from a rule either.
     */
    private static String noBreak(InlineNode n, Element kind, Style s, Box parent) {
        String where = " (line " + n.getLine() + "), ";
        if (s.isOutOfFlow()) {
            return "the " + position(s) + where + "which does not split and is not in the flow of the page";
        }
        if (kind.headingLevel() > 0) {
            return "the " + n.getName() + where + "in which the engine already avoids the break";
        }
        if (parent.headingLevel() > 0) {
            return "the Text of a heading" + where + "in which the engine already avoids the break";
        }
        if (kind == Element.CELL) {
            return "the Cell" + where + "whose breaks are those of its Row";
        }
        return null;
    }

    /** The classes of a {@code Style}: each one with its {@code For style} rule and without repetition. */
    private Set<String> classes(Node n) {
        String[] names = n.getText().trim().split("\\s+");
        if (names[0].isEmpty()) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Style needs at least one style");
        }
        Set<String> classes = new HashSet<>();
        for (String name : names) {
            if (!classes.add(name)) {
                throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Repeated style in Style: " + name);
            }
            if (!classNames.contains(name)) {
                throw error(n, ErrorCode.STYLE_WITHOUT_RULE, "The style " + name + " has no rule: "
                        + "'For style: " + name + "' is missing in Styles");
            }
        }
        return classes;
    }

    /**
     * The groups of a rule, read the same way as in an element: each value with its line, which in a rule of the
     * separate styles document is of that document. An error of a rule of the separate styles document says so.
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
