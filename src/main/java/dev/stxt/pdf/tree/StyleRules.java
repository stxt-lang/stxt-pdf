package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.pdf.model.StxtPdfException;

import java.util.ArrayList;
import java.util.Arrays;
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
import static dev.stxt.pdf.tree.PropertyReader.itemProperty;
import static dev.stxt.pdf.tree.PropertyReader.typography;

/**
 * The reusable rules of {@code Styles} (reference/common/styles.md): {@code For element} (of an element kind, of a
 * position in a table or {@code Document}), {@code For style}, {@code For id} and {@code For column}, read once per
 * call. Besides the groups ({@code Box}, {@code Typography} and {@code Break}) they give element properties
 * ({@link #ELEMENT_PROPERTIES}), which are read as if the element declared them.
 *
 * <p>They can come from several places: from the {@code Styles} of the document itself and from separate styles
 * documents (root {@code Styles}, reference/common/styles.md). At each priority level the rules of the separate
 * documents go first, in the order the documents are given, and those of the document after, so if several declare the
 * same property the last one wins, and the document wins over them all. Each source is read separately: a repeated
 * rule is an error within one source, not between two.</p>
 */
final class StyleRules {

    /** {@code For element} rules, by element and in the order they are applied. */
    private final Map<Element, List<InlineNode>> elementRules = new EnumMap<>(Element.class);
    /** {@code For element: Document} rules, in the order they are applied. */
    private final List<InlineNode> documentRules = new ArrayList<>();
    /** {@code For style} rules, in the order they are applied: those of the separate documents first. */
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
    /** The rules that come from a separate styles document, each with its document: their errors say so. */
    private final Map<InlineNode, SourceLine> external = new IdentityHashMap<>();
    /** The {@code Named color} of all the sources, read before the rules, which can use them. */
    private final NamedColors namedColors;

    /** The {@link CellRule#source} of a rule of the document, which goes after those of every styles document. */
    private static final int DOCUMENT = Integer.MAX_VALUE;

    /** A {@code For style} rule. */
    private record ClassRule(String name, InlineNode rule) {
    }

    /**
     * A rule of a position in a table; {@code source} is the place of its styles document, from 1, or
     * {@link #DOCUMENT}: within their level the rules are applied in that order.
     */
    private record CellRule(CellSelector selector, InlineNode rule, int source) {
    }

    /** A {@code For column} rule: columns {@code from} to {@code to}, both included. */
    private record ColumnRule(int from, int to, InlineNode rule) {
        String key() {
            return from + " " + to;
        }
    }

    /**
     * The element a rule is applied to, for its element properties: its node, its type and what wraps it
     * ({@code Item}, {@code Area} or the {@code Row} of a cell; {@code null} if nothing does).
     */
    private record Target(InlineNode node, Element kind, InlineNode wrapper) {
        boolean inItem() {
            return wrapper != null && wrapper.getCanonicalName().equals("item");
        }

        /** For the messages: {@code the Text (line 12)}. */
        String what() {
            return "the " + node.getName() + " (line " + node.getLine() + ")";
        }
    }

    /** The groups of a rule, which it can give any box. */
    private static final Set<String> GROUPS = Set.of("box", "typography", "break");

    /**
     * The element properties a rule allows besides the groups (reference/common/styles.md), by canonical name: those of
     * how an element looks and distributes its content. Not those of its identity, content, structure or position
     * ({@code Id}, {@code Link}, {@code Alt}, {@code Span}, {@code Start}, {@code Offset}, {@code Rotate}...). A rule
     * gives one to an element only if the element could declare it ({@link #declares}). {@code Horizontal} and
     * {@code Vertical} are those of a {@code Stack} or a {@code Grid}, never those of the {@code Item} or the
     * {@code Area} that wraps the element.
     */
    static final Set<String> ELEMENT_PROPERTIES = Set.of(
            "direction", "wrap", "gap", "horizontal", "vertical", "line-distribution", "split-items",
            "column-sizes", "row-sizes", "areas", "flow", "split-rows", "column-align",
            "count", "min-column-width", "separator", "separator-color", "balance",
            "marker", "marker-image", "fit", "format", "error-correction", "quiet-zone",
            "caption-position", "figure-counter", "in-table-of-contents", "levels", "leader",
            "drop-cap", "side", "clear", "isolate-margins",
            "bullet-markers", "number-markers", "table-columns",
            "grow", "shrink", "base-size");

    /**
     * Of {@link #ELEMENT_PROPERTIES}, those of the {@code Item} that wraps an element in a {@code Stack}: a rule gives
     * them to the element it wraps.
     */
    private static final Set<String> ITEM_PROPERTIES = Set.of("grow", "shrink", "base-size");

    /** The types of element that can declare each of {@link #ELEMENT_PROPERTIES}, in the order of {@link Element}. */
    private static final Map<String, List<Element>> DECLARED_BY = ELEMENT_PROPERTIES.stream()
            .collect(Collectors.toMap(p -> p, p -> Arrays.stream(Element.values())
                    .filter(k -> k.label() != null && declares(k.label(), p)).toList()));

    /**
     * Reads the rules of {@code Styles} of the document and of the separate styles documents ({@code styles}, each
     * with root {@code Styles}, in the order they were given; empty if there is none). Their values are checked here,
     * even if the rule applies to no element; what depends on the element (reference/common/errors.md), when it is
     * applied.
     */
    StyleRules(InlineNode document, List<InlineNode> styles) {
        namedColors = NamedColors.read(document, styles);
        for (int i = 0; i < styles.size(); i++) {
            load(styles.get(i), source(i, styles.size()));
        }
        child(document, "styles").map(Nodes::inline).ifPresent(own -> load(own, null));
    }

    /**
     * The separate styles document at {@code index}, from 0, of the {@code count} of a conversion, for the lines and
     * the errors of what comes from it: with several, the messages say which one it is.
     */
    static SourceLine source(int index, int count) {
        return SourceLine.inStyles(StxtPdfException.NO_LINE, index + 1, count > 1);
    }

    /**
     * The rules of one source; a repeated rule is an error within it. {@code source} is the separate styles document
     * they are in; {@code null} if they are of the document.
     */
    private void load(InlineNode styles, SourceLine source) {
        Map<String, InlineNode> seen = new HashMap<>();
        for (Node r : styles.getChildren()) {
            if (r.getCanonicalName().equals("named-color")) {
                continue; // already read by NamedColors
            }
            InlineNode rule = inline(r);
            String selector = rule.getText().trim();
            String key = rule.getCanonicalName() + ": " + selector;
            if (rule.getCanonicalName().equals("for-element")) {
                key = "element: " + selector.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
            } else if (rule.getCanonicalName().equals("for-column")) {
                key = "for-column: " + columns(rule, source).key();
            }
            InlineNode previous = seen.putIfAbsent(key, rule);
            if (previous != null) {
                throw located(error(rule, ErrorCode.STYLE_DUPLICATE, "Repeated rule: " + rule.getName() + ": "
                        + selector + " (already on line " + previous.getLine() + ")"), source);
            }
            if (source != null) {
                external.put(rule, source);
            }
            String type = null;
            switch (rule.getCanonicalName()) {
                case "for-element" -> type = element(rule, selector, source);
                case "for-column" -> columnRules.add(columns(rule, source));
                case "for-style" -> {
                    String name = name(rule, "For style", source);
                    classRules.add(new ClassRule(name, rule));
                    classNames.add(name);
                }
                case "for-id" -> {
                    String id = name(rule, "For id", source);
                    idRules.computeIfAbsent(id, k -> new ArrayList<>()).add(rule);
                }
                default -> throw unread(rule);
            }
            read(rule, new Style(namedColors), null, null);
            checkElementProperties(rule, type, source);
        }
    }

    /**
     * A {@code For element} rule: of an element kind, of a position in a table or {@code Document}. Returns the name in
     * the schema of what it applies to, for its element properties: that of the element, {@code Cell} for a position
     * in a table.
     */
    private String element(InlineNode rule, String selector, SourceLine source) {
        String canonical = selector.toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
        if (canonical.equals("document")) {
            for (Node g : rule.getChildren()) {
                if (!g.getCanonicalName().equals("typography")) {
                    throw located(error(g, ErrorCode.STYLE_NOT_ALLOWED, "For element: Document only allows Typography, "
                            + "which all boxes inherit: the document has no " + g.getName()), source);
                }
            }
            child(rule, "typography").ifPresent(t -> inline(t).getChildren().stream()
                    .filter(c -> c.getCanonicalName().equals("language")).findFirst().ifPresent(l -> {
                        throw located(error(l, ErrorCode.STYLE_NOT_ALLOWED, "For element: Document does not allow Language: "
                                + "the language of the document is the Language of Document"), source);
                    }));
            documentRules.add(rule);
            return "Document";
        }
        Optional<CellSelector> cell = CellSelector.of(canonical);
        if (cell.isPresent()) {
            cellRules.add(new CellRule(cell.get(), rule, source != null ? source.styles() : DOCUMENT));
            return Element.CELL.label();
        }
        Element element = Element.of(canonical)
                .orElseThrow(() -> located(error(rule, ErrorCode.STYLE_UNKNOWN_ELEMENT, "For element: '" + selector
                        + "' is not a box element or a position in a table ("
                        + Stream.concat(Stream.concat(Stream.of("Document"),
                                Element.selectable().stream().map(Element::label)),
                                Arrays.stream(CellSelector.values()).map(CellSelector::label))
                        .collect(Collectors.joining(", ")) + ")"), source));
        elementRules.computeIfAbsent(element, k -> new ArrayList<>()).add(rule);
        return element.label();
    }

    /**
     * Checks the element properties of a rule when it is read, even if it applies to no element
     * (reference/common/styles.md). In a {@code For element}, {@code type} is the name in the schema of what it applies
     * to: a property that an element of that type cannot declare is an error, and the value is read as that type reads
     * it. In {@code For style} and {@code For id} ({@code type} is {@code null}) the element is not known until the rule
     * is applied: the value has to be valid in one of the types that can declare the property.
     */
    private void checkElementProperties(InlineNode rule, String type, SourceLine source) {
        for (Node p : rule.getChildren()) {
            String name = p.getCanonicalName();
            if (GROUPS.contains(name)) {
                continue;
            }
            if (!ELEMENT_PROPERTIES.contains(name)) {
                // The schema allows it in the rule and this class does not know it: the two have to agree.
                throw unread(p);
            }
            if (type != null && !declares(type, name)) {
                throw located(error(p, ErrorCode.STYLE_NOT_ALLOWED, rule.getName() + ": " + rule.getText().trim()
                        + " gives " + p.getName() + (ITEM_PROPERTIES.contains(name)
                        ? ", which is of the Item that wraps an element in a Stack, and a " + type + " is never in one"
                        : ", which a " + type + " does not have")), source);
            }
            List<Element> kinds = type != null ? List.of(Element.of(canonical(type)).orElseThrow())
                    : DECLARED_BY.get(name);
            InvalidModelException first = null;
            for (Element kind : kinds) {
                try {
                    Style scratch = new Style(namedColors);
                    if (ITEM_PROPERTIES.contains(name)) {
                        itemProperty(p, scratch);
                    } else if (!ElementReader.of(kind).ruleProperty(p, scratch)) {
                        contentValue(p);
                    }
                    first = null;
                    break;
                } catch (InvalidModelException e) {
                    if (first == null) {
                        first = e;
                    }
                }
            }
            if (first != null) {
                throw located(first, source);
            }
        }
    }

    /**
     * Checks the value of a property that the content of an element reads ({@link ElementReader#ruleProperty}): what
     * does not depend on the element. That the {@code Column align} has an alignment per column is checked in the
     * table.
     */
    private static void contentValue(Node p) {
        switch (p.getCanonicalName()) {
            case "column-align" -> TableBuilder.alignments(p);
            case "bullet-markers" -> MarkdownBuilder.markers(p, false);
            case "number-markers" -> MarkdownBuilder.markers(p, true);
            case "table-columns" -> {
                // An ENUM: the schema checks its values.
            }
            default -> throw unread(p);
        }
    }

    /**
     * Whether an element of type {@code type} (its name in the schema, {@code Stack}) can declare the element property
     * {@code name}: what a rule gives an element is what the element could declare itself. Those of
     * {@link #ITEM_PROPERTIES}, whether an element of that type can be in an {@code Item}.
     */
    private static boolean declares(String type, String name) {
        if (!ELEMENT_PROPERTIES.contains(name)) {
            return false;
        }
        return ITEM_PROPERTIES.contains(name) ? DocumentSchema.allows("Item", canonical(type))
                : DocumentSchema.allows(type, name);
    }

    /** The canonical name of the name of an element in the schema: {@code Heading 1} is {@code heading-1}. */
    private static String canonical(String type) {
        return type.toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    /** The {@code Named color} of the document and of the separate styles documents, for the style of the root. */
    NamedColors namedColors() {
        return namedColors;
    }

    /** An error of a rule; if it comes from a separate styles document ({@code source}, or {@code null}), it says so. */
    private static InvalidModelException located(InvalidModelException e, SourceLine source) {
        return source != null ? e.in(source) : e;
    }

    /**
     * Applies the {@code For element: Document} rules to the text of the root: the typography that all boxes inherit.
     */
    void applyDocument(Style root) {
        for (InlineNode rule : documentRules) {
            read(rule, root, null, null);
        }
    }

    /**
     * Applies the {@code For element} rule of a generated element (an entry of the table of contents,
     * reference/components/table-of-contents.md), if there is one: it has no node, so there are no classes or
     * {@code Id} to look at.
     */
    void apply(Element kind, Style s) {
        for (InlineNode rule : elementRules.getOrDefault(kind, List.of())) {
            read(rule, s, null, null);
        }
    }

    /** The range of a {@code For column} rule: {@code n} or {@code n m}, positive integers with {@code n <= m}. */
    private static ColumnRule columns(InlineNode rule, SourceLine source) {
        String text = rule.getText().trim();
        String[] t = text.split("\\s+");
        if (t.length > 2 || !t[0].matches("[1-9]\\d*") || (t.length == 2 && !t[1].matches("[1-9]\\d*"))) {
            throw located(error(rule, ErrorCode.VALUE_SYNTAX, "For column expects a column or a range 'from to', "
                    + "positive integers: '" + text + "'"), source);
        }
        int from = Integer.parseInt(t[0]);
        int to = t.length == 2 ? Integer.parseInt(t[1]) : from;
        if (to < from) {
            throw located(error(rule, ErrorCode.VALUE_NOT_ALLOWED,
                    "For column: the range goes from lower to higher: '" + text + "'"), source);
        }
        return new ColumnRule(from, to, rule);
    }

    /** The name of a class or of an {@code Id} in a rule: a single word. */
    private static String name(InlineNode rule, String what, SourceLine source) {
        String name = rule.getText().trim();
        if (name.isEmpty() || name.chars().anyMatch(Character::isWhitespace)) {
            throw located(error(rule, ErrorCode.VALUE_NOT_ALLOWED, what + " needs a single name, without spaces: '"
                    + name + "'"), source);
        }
        return name;
    }

    /**
     * Applies to a {@code Section} (reference/components/document.md) the rules that reach it:
     * {@code For element: Section}, those of its classes and that of its {@code Id}. They can only give
     * {@code Typography}, which all its content inherits; with {@code Box}, {@code Break} or an element property they
     * are an error (a section is the page area of its pages, not a box).
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
            for (Node g : rule.getChildren()) {
                if (!g.getCanonicalName().equals("typography")) {
                    throw located(error(g, ErrorCode.STYLE_NOT_ALLOWED, "The rule " + rule.getName() + ": "
                            + rule.getText().trim() + " applies to a Section (line " + n.getLine()
                            + "), which only allows Typography: it is the page area of its pages, not a box"),
                            external.get(rule));
                }
            }
            read(rule, s, null, null);
        }
    }

    /**
     * Applies the rules that correspond to an element, from lower to higher priority: that of its kind, those of the
     * position of a cell in its table (first the {@code Column align} of its column, then {@code Header cell},
     * {@code Body cell} or {@code Footer cell}, and {@code Odd row cell} or {@code Even row cell}), those of its column
     * ({@code For column}, in the order of {@code Styles}), those of its classes (in the order of {@code Styles}) and
     * that of its {@code Id}. They go after inheritance and before what the element itself declares, and they are
     * merged property by property. {@code wrapper} is the {@code Item}, the {@code Area} or the {@code Row} that wraps
     * the element, or {@code null}; {@code position} is that of a {@code Cell}, {@code null} for the others.
     *
     * <p>The element properties of the rules (reference/common/styles.md) are read as if the element declared them; an
     * element property that the element could not declare is an error, with its line. Returns those that its content
     * reads from its node ({@link BuildContext#property}), by canonical name: the one of higher priority.</p>
     */
    Map<String, PropertyNode> apply(InlineNode n, Element kind, Style s, Box parent, InlineNode wrapper,
                                    CellPosition position) {
        String noBreak = noBreak(n, kind, s, parent);
        Target target = new Target(n, kind, wrapper);
        Map<String, PropertyNode> content = new HashMap<>();
        List<InlineNode> rules = new ArrayList<>(elementRules.getOrDefault(kind, List.of()));
        applyAll(rules, noBreak, s, target, content);
        rules.clear();
        if (position != null) {
            if (position.columnAlign() != null) {
                // It is the default value of the column: the table declares it, on the line of its Column align.
                s.typography().setTextAlign(position.columnAlign());
                s.declared(PropertyKey.internal("align"), position.columnAlignLine());
            }
            // Within this level those of the separate styles documents go first, in the order of the documents, and
            // in each source, by position.
            cellRules.stream().filter(c -> c.selector().matches(position))
                    .sorted(Comparator.comparingInt(CellRule::source)
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
        applyAll(rules, noBreak, s, target, content);
        return content;
    }

    /**
     * The properties of {@code Typography} that a styled run allows (reference/common/text-formatting.md): the ones that
     * change how its letters look and not the height of the line.
     */
    static final List<String> RUN_PROPERTIES = List.of("font", "code-font", "bold", "weight", "italic", "color",
            "case", "small-caps", "letter-spacing");

    private static final String RUN_ALLOWED = "Font, Code font, Bold, Weight, Italic, Color, Case, Small caps and "
            + "Letter spacing";

    /**
     * The style of a styled run, {@code [text]{name}} (reference/common/text-formatting.md): what the {@code For style}
     * rules of that name declare, in the order of {@code Styles}, read into a style that inherits nothing, so that
     * what is declared in it is what the rules declare, each property with its line. A rule with a {@code Box}, a
     * {@code Break} or a property that a run does not allow is an error, with the line of what it declares.
     *
     * @param line line of the document where the run is, for the messages
     */
    Style runStyle(String name, int line) {
        if (!classNames.contains(name)) {
            throw error(line, ErrorCode.STYLE_WITHOUT_RULE, "The style " + name + " of the styled run [...]{" + name
                    + "} has no rule: 'For style: " + name + "' is missing in Styles");
        }
        Style s = new Style(namedColors);
        for (ClassRule rule : classRules) {
            if (!rule.name().equals(name)) {
                continue;
            }
            SourceLine source = external.get(rule.rule());
            for (Node g : rule.rule().getChildren()) {
                if (!g.getCanonicalName().equals("typography")) {
                    throw located(error(g, ErrorCode.STYLE_NOT_ALLOWED_IN_RUN, "The rule For style: " + name
                            + " gives " + g.getName() + " to the styled run on line " + line + ", which is part of a "
                            + "line and not a box: a run only takes " + RUN_ALLOWED), source);
                }
                for (Node p : inline(g).getChildren()) {
                    if (!RUN_PROPERTIES.contains(p.getCanonicalName())) {
                        throw located(error(p, ErrorCode.STYLE_NOT_ALLOWED_IN_RUN, "The rule For style: " + name
                                + " gives " + p.getName() + " to the styled run on line " + line + ", which keeps the "
                                + "size, the line height and the rest of the text of its box: a run only takes "
                                + RUN_ALLOWED), source);
                    }
                }
            }
            read(rule.rule(), s, null, null);
        }
        return s;
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

    private void applyAll(List<InlineNode> rules, String noBreak, Style s, Target target,
                          Map<String, PropertyNode> content) {
        for (InlineNode rule : rules) {
            if (noBreak != null) {
                child(rule, "break").ifPresent(b -> {
                    throw located(error(b, ErrorCode.STYLE_NOT_ALLOWED, "The rule " + rule.getName() + ": "
                            + rule.getText().trim() + " applies Break to " + noBreak), external.get(rule));
                });
            }
            read(rule, s, target, content);
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
     * A rule, read the same way as in an element: each value with its line, which in a rule of a separate styles
     * document is of that document. An error of a rule of a separate styles document says so. The element properties
     * are given to the element of {@code target} ({@link #elementProperty}); with {@code target} {@code null}, only the
     * groups are read, and the element properties were already checked when the rule was read
     * ({@link #checkElementProperties}) or rejected where they cannot go (a {@code Section}, a styled run).
     */
    private void read(InlineNode rule, Style s, Target target, Map<String, PropertyNode> content) {
        SourceLine source = external.get(rule);
        s.readingStyles(source);
        try {
            for (Node g : rule.getChildren()) {
                switch (g.getCanonicalName()) {
                    case "box" -> box(inline(g), s);
                    case "typography" -> typography(inline(g), s);
                    case "break" -> fragmentation(inline(g), s);
                    default -> {
                        if (target != null) {
                            elementProperty(rule, g, s, target, content);
                        }
                    }
                }
            }
        } catch (InvalidModelException e) {
            throw located(e, source);
        } finally {
            s.readingStyles(null);
        }
    }

    /**
     * Gives the element property {@code p} of {@code rule} to the element of {@code target}, as if it declared it, and
     * puts in {@code content} one that its content reads from its node. One that the element could not declare is
     * {@code STYLE_NOT_ALLOWED}, with the line of the property: also {@code Grow}, {@code Shrink} or {@code Base size}
     * to an element that is not in an {@code Item}.
     */
    private void elementProperty(InlineNode rule, Node p, Style s, Target target, Map<String, PropertyNode> content) {
        String name = p.getCanonicalName();
        String gives = "The rule " + rule.getName() + ": " + rule.getText().trim() + " gives " + p.getName() + " to "
                + target.what();
        if (ITEM_PROPERTIES.contains(name)) {
            if (!target.inItem()) {
                throw error(p, ErrorCode.STYLE_NOT_ALLOWED, gives + ", which is not in an Item: Grow, Shrink and Base "
                        + "size are of the Item that wraps an element in a Stack");
            }
            itemProperty(p, s);
        } else if (!declares(target.kind().label(), name)) {
            throw error(p, ErrorCode.STYLE_NOT_ALLOWED, gives + ", which does not have it");
        } else if (!ElementReader.of(target.kind()).ruleProperty(p, s)) {
            content.put(name, new PropertyNode(p, external.get(rule)));
        }
    }
}
