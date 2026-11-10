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
import java.util.concurrent.ConcurrentHashMap;
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
 * <p>A rule written inside another is nested: it applies to the elements it names that are inside an element the outer
 * one applies to ({@link Ancestor}). Within each priority level the nested rules go after the loose ones, and among
 * them the one of the closest ancestor goes last, so it wins. Rules nest one level.</p>
 *
 * <p>They can come from several places: from the {@code Styles} of the document itself and from separate styles
 * documents (root {@code Styles}, reference/common/styles.md). At each priority level the rules of the separate
 * documents go first, in the order the documents are given, and those of the document after, so if several declare the
 * same property the last one wins, and the document wins over them all. Each source is read separately: a repeated
 * rule is an error within one source, not between two.</p>
 */
final class StyleRules {

    /** {@code For element} rules, by element and in the order they are applied. */
    private final Map<Element, List<ElementRule>> elementRules = new EnumMap<>(Element.class);
    /** {@code For element: Document} rules, in the order they are applied. */
    private final List<InlineNode> documentRules = new ArrayList<>();
    /** {@code For style} rules, in the order they are applied: those of the separate documents first. */
    private final List<ClassRule> classRules = new ArrayList<>();
    /** The classes that have some loose rule; one with only nested rules has a rule inside their outer rules. */
    private final Set<String> classNames = new HashSet<>();
    /** What each rule that contains nested rules applies to, by rule. */
    private final Map<InlineNode, Outer> outers = new IdentityHashMap<>();
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

    /** A rule and, if it is nested, the rule it is written inside ({@code outer}); {@code null} if it is loose. */
    private interface Placed {
        InlineNode rule();

        InlineNode outer();
    }

    /** A {@code For element} rule of an element kind. */
    private record ElementRule(InlineNode rule, InlineNode outer) implements Placed {
    }

    /** A {@code For style} rule. */
    private record ClassRule(String name, InlineNode rule, InlineNode outer) implements Placed {
    }

    /**
     * A rule of a position in a table; {@code source} is the place of its styles document, from 1, or
     * {@link #DOCUMENT}: within their level the rules are applied in that order.
     */
    private record CellRule(CellSelector selector, InlineNode rule, int source, InlineNode outer) implements Placed {
    }

    /** A {@code For column} rule: columns {@code from} to {@code to}, both included. */
    private record ColumnRule(int from, int to, InlineNode rule, InlineNode outer) implements Placed {
        String key() {
            return from + " " + to;
        }
    }

    /**
     * What a rule that contains nested rules applies to: the elements of a kind ({@code For element}, also
     * {@code Section}), those with a style ({@code For style}) or the one with an {@code Id} ({@code For id}). The
     * nested rules apply within the ancestors it matches.
     */
    private record Outer(Element kind, String className, String id) {
        boolean matches(Ancestor a) {
            return kind != null ? a.kind() == kind
                    : className != null ? a.classes().contains(className)
                    : id.equals(a.id());
        }

        /** For the messages: what an element has to be inside of. */
        String within() {
            return kind != null ? "a " + kind.label()
                    : className != null ? "an element with Style: " + className
                    : "the element with Id: " + id;
        }
    }

    /** The rules that can be written inside another rule. */
    private static final Set<String> NESTED = Set.of("for-element", "for-style", "for-column");

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
            "drop-cap", "side", "clear", "isolate-margins", "trim-margins",
            "bullet-markers", "number-markers", "table-columns",
            "grow", "shrink", "base-size");

    /**
     * Of {@link #ELEMENT_PROPERTIES}, those of the {@code Item} that wraps an element in a {@code Stack}: a rule gives
     * them to the element it wraps.
     */
    private static final Set<String> ITEM_PROPERTIES = Set.of("grow", "shrink", "base-size");

    /**
     * The children the schema allows in each type of element, by its name in the schema ({@code Stack}), read from the
     * schema once per type: {@link #declares} looks them up for every element property a rule gives an element.
     */
    private static final Map<String, Set<String>> CHILDREN = new ConcurrentHashMap<>();

    /**
     * The types of element that can declare each of {@link #ELEMENT_PROPERTIES}, in the order of {@link Element},
     * found when a {@code For style} or a {@code For id} first gives that property ({@link #declaredBy}): not at the
     * start, so that loading this class costs nothing to a document without them.
     */
    private static final Map<String, List<Element>> DECLARED_BY = new ConcurrentHashMap<>();

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
            load(inline(r), null, seen, source);
        }
    }

    /**
     * One rule, loose ({@code outer} is {@code null}) or nested in {@code outer}; {@code seen} are the selectors already
     * read at its level, in its source: a repeated one is an error.
     */
    private void load(InlineNode rule, InlineNode outer, Map<String, InlineNode> seen, SourceLine source) {
        String selector = rule.getText().trim();
        String key = rule.getCanonicalName() + ": " + selector;
        if (rule.getCanonicalName().equals("for-element")) {
            key = "element: " + selector.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        } else if (rule.getCanonicalName().equals("for-column")) {
            key = "for-column: " + columns(rule, outer, source).key();
        }
        InlineNode previous = seen.putIfAbsent(key, rule);
        if (previous != null) {
            throw located(error(rule, ErrorCode.STYLE_DUPLICATE, "Repeated rule: " + rule.getName() + ": "
                    + selector + (outer != null ? " inside " + selector(outer) : "") + " (already on line "
                    + previous.getLine() + ")"), source);
        }
        if (source != null) {
            external.put(rule, source);
        }
        String type = null;
        switch (rule.getCanonicalName()) {
            case "for-element" -> type = element(rule, selector, outer, source);
            case "for-column" -> columnRules.add(columns(rule, outer, source));
            case "for-style" -> {
                String name = name(rule, "For style", source);
                classRules.add(new ClassRule(name, rule, outer));
                if (outer == null) {
                    classNames.add(name);
                }
            }
            case "for-id" -> {
                if (outer != null) {
                    throw unread(rule); // the schema does not allow it inside a rule
                }
                String id = name(rule, "For id", source);
                idRules.computeIfAbsent(id, k -> new ArrayList<>()).add(rule);
            }
            default -> throw unread(rule);
        }
        read(rule, new Style(namedColors), null, null);
        checkElementProperties(rule, type, source);
        nested(rule, outer, source);
    }

    /**
     * The rules written inside {@code rule} (reference/common/styles.md), which apply within the elements it applies
     * to: only a rule of an element written in the document, of a style or of an {@code Id} contains them, and only one
     * level deep. {@code outer} is the rule {@code rule} is inside, or {@code null}.
     */
    private void nested(InlineNode rule, InlineNode outer, SourceLine source) {
        List<InlineNode> inner = rule.getChildren().stream()
                .filter(c -> NESTED.contains(c.getCanonicalName())).map(Nodes::inline).toList();
        if (inner.isEmpty()) {
            return;
        }
        if (outer != null) {
            throw located(error(inner.get(0), ErrorCode.STYLE_NESTED_TOO_DEEP, "The rule " + selector(rule)
                    + " is inside " + selector(outer) + " and cannot contain " + selector(inner.get(0))
                    + ": rules nest one level"), source);
        }
        outers.put(rule, container(rule, inner.get(0), source));
        Map<String, InlineNode> seen = new HashMap<>();
        for (InlineNode r : inner) {
            load(r, rule, seen, source);
        }
    }

    /**
     * What {@code rule} applies to, if it can contain rules ({@link Outer}): a {@code For style}, a {@code For id} or
     * the {@code For element} of an element written in the document, {@code Section} included. Not a position in a
     * table, a column, {@code Document} or an element the engine generates. {@code inner} is its first nested rule, for
     * the error.
     */
    private static Outer container(InlineNode rule, InlineNode inner, SourceLine source) {
        String what = switch (rule.getCanonicalName()) {
            case "for-style" -> null;
            case "for-id" -> null;
            case "for-element" -> {
                String canonical = rule.getText().trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
                if (canonical.equals("document")) {
                    yield "the document is not an element";
                }
                if (CellSelector.of(canonical).isPresent()) {
                    yield "a position in a table is not an element";
                }
                yield Element.of(canonical).filter(Element::isWritten).isPresent() ? null
                        : "it is generated by the engine, not written in the document";
            }
            default -> "a column is not an element";
        };
        if (what != null) {
            throw located(error(inner, ErrorCode.STYLE_NOT_ALLOWED, "The rule " + selector(rule) + " cannot contain "
                    + selector(inner) + ": " + what + ". A rule inside another applies within the elements the outer "
                    + "one applies to: For style, For id or the For element of an element of the document"), source);
        }
        return switch (rule.getCanonicalName()) {
            case "for-style" -> new Outer(null, rule.getText().trim(), null);
            case "for-id" -> new Outer(null, null, rule.getText().trim());
            default -> new Outer(Element.of(canonical(rule.getText().trim())).orElseThrow(), null, null);
        };
    }

    /** A rule, for the messages: {@code For style: card}. */
    private static String selector(InlineNode rule) {
        return rule.getName() + ": " + rule.getText().trim();
    }

    /**
     * {@code n} as an ancestor of what is inside it ({@link Ancestor}): its kind, the names of its {@code Style} and
     * its {@code Id}. The names are checked when its own rules are applied, not here.
     */
    static Ancestor ancestor(InlineNode n, Element kind) {
        Set<String> classes = child(n, "style")
                .map(c -> new HashSet<>(Arrays.asList(c.getText().trim().split("\\s+")))).orElseGet(HashSet::new);
        return new Ancestor(kind, classes, child(n, "id").map(i -> i.getText().trim()).orElse(null));
    }

    /**
     * A {@code For element} rule: of an element kind, of a position in a table or {@code Document}. Returns the name in
     * the schema of what it applies to, for its element properties: that of the element, {@code Cell} for a position
     * in a table. {@code outer} is the rule it is nested in, or {@code null}: nested, it cannot name {@code Document},
     * {@code Section} or an element the engine builds apart (the entries of a table of contents, the footnote area),
     * which are never inside an element.
     */
    private String element(InlineNode rule, String selector, InlineNode outer, SourceLine source) {
        String canonical = selector.toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
        if (canonical.equals("document")) {
            if (outer != null) {
                throw located(error(rule, ErrorCode.STYLE_NOT_ALLOWED, "For element: Document cannot be inside "
                        + selector(outer) + ": the document is inside nothing"), source);
            }
            for (Node g : rule.getChildren()) {
                if (NESTED.contains(g.getCanonicalName())) {
                    throw located(error(g, ErrorCode.STYLE_NOT_ALLOWED, "For element: Document cannot contain rules: "
                            + "the document is not an element. A rule inside another applies within the elements the "
                            + "outer one applies to"), source);
                }
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
            cellRules.add(new CellRule(cell.get(), rule, source != null ? source.styles() : DOCUMENT, outer));
            return Element.CELL.label();
        }
        Element element = Element.of(canonical)
                .orElseThrow(() -> located(error(rule, ErrorCode.STYLE_UNKNOWN_ELEMENT, "For element: '" + selector
                        + "' is not a box element or a position in a table ("
                        + Stream.concat(Stream.concat(Stream.of("Document"),
                                Element.selectable().stream().map(Element::label)),
                                Arrays.stream(CellSelector.values()).map(CellSelector::label))
                        .collect(Collectors.joining(", ")) + ")"), source));
        if (outer != null && (element == Element.SECTION || !(element.isWritten() || element == Element.CODE_BLOCK))) {
            throw located(error(rule, ErrorCode.STYLE_NOT_ALLOWED, selector(rule) + " cannot be inside "
                    + selector(outer) + ": " + (element == Element.SECTION ? "a Section is inside nothing"
                    : "the engine builds " + element.label() + " apart, inside no element")), source);
        }
        elementRules.computeIfAbsent(element, k -> new ArrayList<>()).add(new ElementRule(rule, outer));
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
            if (GROUPS.contains(name) || NESTED.contains(name)) {
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
                    : declaredBy(name);
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
        return ITEM_PROPERTIES.contains(name) ? children("Item").contains(canonical(type))
                : children(type).contains(name);
    }

    /** The types of element that can declare the element property {@code name} ({@link #DECLARED_BY}). */
    private static List<Element> declaredBy(String name) {
        return DECLARED_BY.computeIfAbsent(name, p -> Arrays.stream(Element.values())
                .filter(k -> k.label() != null && declares(k.label(), p)).toList());
    }

    /** The children the schema allows in the type {@code type} ({@link #CHILDREN}). */
    private static Set<String> children(String type) {
        return CHILDREN.computeIfAbsent(type, DocumentSchema::children);
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
        for (ElementRule rule : elementRules.getOrDefault(kind, List.of())) {
            if (rule.outer() == null) {
                read(rule.rule(), s, null, null);
            }
        }
    }

    /** The range of a {@code For column} rule: {@code n} or {@code n m}, positive integers with {@code n <= m}. */
    private static ColumnRule columns(InlineNode rule, InlineNode outer, SourceLine source) {
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
        return new ColumnRule(from, to, rule, outer);
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
        // A section is inside nothing: only the loose rules reach it. Those nested in its rules apply to its content.
        List<InlineNode> rules = new ArrayList<>();
        select(elementRules.getOrDefault(Element.SECTION, List.of()), List.of(), rules);
        child(n, "style").ifPresent(c -> rules.addAll(classRules(c, List.of())));
        child(n, "id").map(id -> idRules.get(id.getText().trim())).ifPresent(rules::addAll);
        for (InlineNode rule : rules) {
            for (Node g : rule.getChildren()) {
                if (!g.getCanonicalName().equals("typography") && !NESTED.contains(g.getCanonicalName())) {
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
     * the element, or {@code null}; {@code position} is that of a {@code Cell}, {@code null} for the others;
     * {@code ancestors} are those of the element in the document, the closest first, for the nested rules
     * ({@link #select}).
     *
     * <p>The element properties of the rules (reference/common/styles.md) are read as if the element declared them; an
     * element property that the element could not declare is an error, with its line. Returns those that its content
     * reads from its node ({@link BuildContext#property}), by canonical name: the one of higher priority.</p>
     */
    Map<String, PropertyNode> apply(InlineNode n, Element kind, Style s, Box parent, InlineNode wrapper,
                                    CellPosition position, List<Ancestor> ancestors) {
        String noBreak = noBreak(n, kind, s, parent);
        Target target = new Target(n, kind, wrapper);
        Map<String, PropertyNode> content = new HashMap<>();
        List<InlineNode> rules = new ArrayList<>();
        select(elementRules.getOrDefault(kind, List.of()), ancestors, rules);
        applyAll(rules, noBreak, s, target, content);
        rules.clear();
        if (position != null) {
            if (position.columnAlign() != null) {
                // It is the default value of the column: the table declares it, on the line of its Column align.
                s.typography().setTextAlign(position.columnAlign());
                s.declared(PropertyKey.internal("align"), position.columnAlignLine());
            }
            // Within this level those of the separate styles documents go first, in the order of the documents, and
            // in each source, by position; the nested ones after, those of the closest ancestor last.
            List<CellRule> cells = cellRules.stream().filter(c -> c.selector().matches(position)).toList();
            Comparator<CellRule> order = Comparator.comparingInt(CellRule::source)
                    .thenComparing(c -> c.selector().ordinal());
            cells.stream().filter(c -> c.outer() == null).sorted(order).forEach(c -> rules.add(c.rule()));
            cells.stream().filter(c -> c.outer() != null && depth(c.outer(), ancestors) >= 0)
                    .sorted(Comparator.comparingInt((CellRule c) -> depth(c.outer(), ancestors)).reversed()
                            .thenComparing(order))
                    .forEach(c -> rules.add(c.rule()));
            select(columnRules.stream()
                    .filter(c -> position.column() >= c.from() && position.column() <= c.to()).toList(),
                    ancestors, rules);
        }
        // The classes of the row of a cell weigh less than those of the cell itself.
        if (position != null && position.rowClass() != null) {
            rules.addAll(classRules(position.rowClass(), ancestors));
        }
        child(n, "style").ifPresent(c -> rules.addAll(classRules(c, ancestors)));
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
     * @param line      line of the document where the run is, for the messages
     * @param ancestors the ancestors of the run in the document, the closest first (its {@code Text} is the first), for
     *                  the nested rules
     */
    Style runStyle(String name, int line, List<Ancestor> ancestors) {
        checkStyle(name, line, ancestors, "The style " + name + " of the styled run [...]{" + name + "}");
        Style s = new Style(namedColors);
        List<InlineNode> rules = new ArrayList<>();
        select(classRules.stream().filter(r -> r.name().equals(name)).toList(), ancestors, rules);
        for (InlineNode rule : rules) {
            SourceLine source = external.get(rule);
            for (Node g : rule.getChildren()) {
                if (NESTED.contains(g.getCanonicalName())) {
                    continue; // it applies within the elements with the style, not to the run
                }
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
            read(rule, s, null, null);
        }
        return s;
    }

    /**
     * The {@code For style} rules of a {@code Style} that apply to an element with {@code ancestors}, in the order of
     * {@code Styles}, the nested ones after ({@link #select}).
     */
    private List<InlineNode> classRules(Node classNode, List<Ancestor> ancestors) {
        Set<String> classes = classes(classNode, ancestors);
        List<InlineNode> rules = new ArrayList<>();
        select(classRules.stream().filter(r -> classes.contains(r.name())).toList(), ancestors, rules);
        return rules;
    }

    /**
     * Of the rules of one priority level, those that apply to an element with {@code ancestors}, added to {@code out}
     * in the order they are applied: the loose ones as given, then the nested ones whose outer rule applies to an
     * ancestor, those of the farthest ancestor first, so that the one of the closest ancestor wins; with the same
     * ancestor, as given.
     */
    private void select(List<? extends Placed> candidates, List<Ancestor> ancestors, List<InlineNode> out) {
        List<Placed> nested = new ArrayList<>();
        for (Placed c : candidates) {
            if (c.outer() == null) {
                out.add(c.rule());
            } else if (depth(c.outer(), ancestors) >= 0) {
                nested.add(c);
            }
        }
        nested.sort(Comparator.comparingInt((Placed c) -> depth(c.outer(), ancestors)).reversed());
        nested.forEach(c -> out.add(c.rule()));
    }

    /**
     * How far up is the closest of {@code ancestors} that the rule {@code outer} applies to: 0 is the parent; -1 if
     * none.
     */
    private int depth(InlineNode outer, List<Ancestor> ancestors) {
        Outer o = outers.get(outer);
        for (int i = 0; i < ancestors.size(); i++) {
            if (o.matches(ancestors.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * That the style {@code name} has a rule where an element with {@code ancestors} is: a loose {@code For style} or
     * a nested one whose outer rule applies to an ancestor. {@code what} starts the message: {@code The style x}.
     */
    private void checkStyle(String name, int line, List<Ancestor> ancestors, String what) {
        if (classNames.contains(name)) {
            return;
        }
        List<ClassRule> nested = classRules.stream().filter(r -> r.name().equals(name)).toList();
        if (nested.isEmpty()) {
            throw error(line, ErrorCode.STYLE_WITHOUT_RULE, what + " has no rule: 'For style: " + name
                    + "' is missing in Styles");
        }
        if (nested.stream().noneMatch(r -> depth(r.outer(), ancestors) >= 0)) {
            InlineNode outer = nested.get(0).outer();
            throw error(line, ErrorCode.STYLE_WITHOUT_RULE, what + " has no rule here: its rule For style: " + name
                    + " is inside " + selector(outer) + " (line " + outer.getLine() + ") and applies within "
                    + outers.get(outer).within() + ", which this is not inside of");
        }
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

    /**
     * The classes of a {@code Style}: each one with its {@code For style} rule, loose or nested in a rule of one of
     * {@code ancestors} ({@link #checkStyle}), and without repetition.
     */
    private Set<String> classes(Node n, List<Ancestor> ancestors) {
        String[] names = n.getText().trim().split("\\s+");
        if (names[0].isEmpty()) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Style needs at least one style");
        }
        Set<String> classes = new HashSet<>();
        for (String name : names) {
            if (!classes.add(name)) {
                throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Repeated style in Style: " + name);
            }
            checkStyle(name, n.getLine(), ancestors, "The style " + name);
        }
        return classes;
    }

    /**
     * A rule, read the same way as in an element: each value with its line, which in a rule of a separate styles
     * document is of that document. An error of a rule of a separate styles document says so. The element properties
     * are given to the element of {@code target} ({@link #elementProperty}); with {@code target} {@code null}, only the
     * groups are read, and the element properties were already checked when the rule was read
     * ({@link #checkElementProperties}) or rejected where they cannot go (a {@code Section}, a styled run). The rules
     * nested in it are not read here: they apply within the element.
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
                        if (target != null && !NESTED.contains(g.getCanonicalName())) {
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
