package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.pdf.model.Alignment;
import dev.stxt.pdf.model.Break;
import dev.stxt.pdf.model.Cell;
import dev.stxt.pdf.model.Edges;
import dev.stxt.pdf.model.Direction;
import dev.stxt.pdf.model.Distribution;
import dev.stxt.pdf.model.Fit;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.GridAreas;
import dev.stxt.pdf.model.Markup;
import dev.stxt.pdf.model.Leader;
import dev.stxt.pdf.model.Links;
import dev.stxt.pdf.model.Marker;
import dev.stxt.pdf.model.NumberFormat;
import dev.stxt.pdf.model.Pages;
import dev.stxt.pdf.model.TextAlign;
import dev.stxt.pdf.model.LineBreaking;
import dev.stxt.pdf.model.TextCase;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.model.ValueParser;
import dev.stxt.pdf.model.Preformatted;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static dev.stxt.pdf.tree.Nodes.error;
import static dev.stxt.pdf.tree.Nodes.isItemElement;
import static dev.stxt.pdf.tree.Nodes.parse;
import static dev.stxt.pdf.tree.Nodes.placement;
import static dev.stxt.pdf.tree.Nodes.unread;
import static java.util.Map.entry;
import static dev.stxt.pdf.tree.ValueReader.LENGTH;
import static dev.stxt.pdf.tree.ValueReader.LENGTH_OR_AUTO;
import static dev.stxt.pdf.tree.ValueReader.SIZE;
import static dev.stxt.pdf.tree.ValueReader.aspectRatio;
import static dev.stxt.pdf.tree.ValueReader.bool;
import static dev.stxt.pdf.tree.ValueReader.check;
import static dev.stxt.pdf.tree.ValueReader.color;
import static dev.stxt.pdf.tree.ValueReader.colorSides;
import static dev.stxt.pdf.tree.ValueReader.enumValue;
import static dev.stxt.pdf.tree.ValueReader.factor;
import static dev.stxt.pdf.tree.ValueReader.fontFamily;
import static dev.stxt.pdf.tree.ValueReader.integer;
import static dev.stxt.pdf.tree.ValueReader.language;
import static dev.stxt.pdf.tree.ValueReader.nonNegative;
import static dev.stxt.pdf.tree.ValueReader.nonNegativeSides;
import static dev.stxt.pdf.tree.ValueReader.paint;
import static dev.stxt.pdf.tree.ValueReader.radius;
import static dev.stxt.pdf.tree.ValueReader.positive;
import static dev.stxt.pdf.tree.ValueReader.signedPoints;
import static dev.stxt.pdf.tree.ValueReader.positiveInteger;
import static dev.stxt.pdf.tree.ValueReader.sides;
import static dev.stxt.pdf.tree.ValueReader.tracks;
import static dev.stxt.pdf.tree.ValueReader.value;

/**
 * Reads the properties of a box into its {@link Style}: the {@code Box}, {@code Typography} and {@code Break} groups
 * (reference/common/structure.md), shared by the elements and the rules of {@code Styles}, the properties of
 * {@code Stack}, {@code Grid} and {@code Columns} and those of the {@code Item} and {@code Area} wrappers. Each
 * property keeps its line ({@link Style#declared}).
 *
 * <p>The {@code switch} statements have no {@code default} branch that assigns, and the tables ({@code Typography} and
 * the standalone properties) have no default entry: what the schema allows and is not read is an error of the engine
 * ({@code SchemaCoverageTest}).</p>
 */
final class PropertyReader {

    /**
     * Properties of the {@code Stack} element (the full structure is in the schema, reference/common/structure.md).
     */
    static final Set<String> STACK = Set.of("direction", "wrap", "gap", "horizontal", "vertical", "line-distribution");
    /** Properties of the {@code Grid} and {@code Table} elements. */
    static final Set<String> GRID = Set.of("column-sizes", "row-sizes", "gap", "areas", "flow", "horizontal", "vertical");
    /** Properties of the {@code Columns} element. */
    static final Set<String> COLUMNS = Set.of("count", "min-column-width", "gap", "separator", "separator-color", "balance");

    /** Reads a property into the style; its line is already recorded. */
    @FunctionalInterface
    private interface Property {
        void read(Node p, Style s);
    }

    /** {@code Typography} group, by canonical name. */
    private static final Map<String, Property> TYPOGRAPHY = Map.ofEntries(
            entry("font", (p, s) -> s.typography().setFont(fontFamily(p))),
            entry("code-font", (p, s) -> s.typography().setCodeFont(fontFamily(p))),
            entry("bold", (p, s) -> s.typography().setFontWeight(bool(p) ? FontWeight.BOLD : FontWeight.NORMAL)),
            entry("italic", (p, s) -> s.typography().setFontStyle(bool(p) ? FontStyle.ITALIC : FontStyle.NORMAL)),
            entry("size", (p, s) -> s.typography().setFontSize(positive(p))),
            entry("line-height", (p, s) -> s.typography().setLineHeight(value(p, LENGTH, false))),
            entry("align", (p, s) -> s.typography().setTextAlign(enumValue(TextAlign.class, p))),
            entry("color", (p, s) -> s.typography().setColor(color(p))),
            entry("line-breaking", (p, s) -> s.typography().setLineBreaking(enumValue(LineBreaking.class, p))),
            entry("hyphenate", (p, s) -> s.typography().setHyphenate(bool(p))),
            entry("markup", (p, s) -> s.typography().setMarkup(enumValue(Markup.class, p))),
            entry("case", (p, s) -> s.typography().setTextCase(enumValue(TextCase.class, p))),
            entry("letter-spacing", (p, s) -> s.typography().setLetterSpacing(signedPoints(p))),
            entry("small-caps", (p, s) -> s.typography().setSmallCaps(bool(p))),
            entry("ligatures", (p, s) -> s.typography().setLigatures(bool(p))),
            entry("preformatted", (p, s) -> s.typography().setPreformatted(enumValue(Preformatted.class, p))),
            entry("tab-size", (p, s) -> s.typography().setTabSize(positiveInteger(p))),
            entry("numbered", (p, s) -> s.typography().setNumbered(bool(p))),
            entry("number-format", (p, s) -> s.typography().setNumberFormat(enumValue(NumberFormat.class, p))),
            entry("number-label", (p, s) -> s.typography().setNumberLabel(numberLabel(p))),
            entry("widows", (p, s) -> s.typography().setWidows(positiveInteger(p))),
            entry("orphans", (p, s) -> s.typography().setOrphans(positiveInteger(p))),
            entry("language", (p, s) -> s.typography().setLocale(Locale.forLanguageTag(language(p)))));

    /** Standalone properties of the elements ({@link #single}), by canonical name. */
    private static final Map<String, Property> SINGLE = Map.ofEntries(
            entry("offset", (p, s) -> s.setOffset(sides(p, LENGTH_OR_AUTO, true))),
            entry("layer", (p, s) -> s.setLayer(integer(p))),
            entry("pages", (p, s) -> s.setPages(enumValue(Pages.class, p))),
            entry("marker", (p, s) -> s.list().setMarker(enumValue(Marker.class, p))),
            entry("start", (p, s) -> s.list().setStart(integer(p))),
            entry("number", (p, s) -> s.list().setNumber(integer(p))),
            entry("isolate-margins", (p, s) -> s.setIsolateMargins(bool(p))),
            entry("link", (p, s) -> s.setLink(link(p))),
            entry("row-header", (p, s) -> s.setRowHeader(bool(p))),
            entry("in-table-of-contents", (p, s) -> s.setInTableOfContents(bool(p))),
            entry("alt", (p, s) -> s.image().setAlt(alt(p))),
            entry("fit", (p, s) -> s.image().setFit(enumValue(Fit.class, p))),
            entry("pdf-page", (p, s) -> s.image().setPdfPage(positiveInteger(p))),
            entry("levels", TableOfContentsBuilder::levels),
            entry("leader", (p, s) -> s.tableOfContents().setLeader(enumValue(Leader.class, p))));

    private PropertyReader() {
    }

    /** {@code Box} group: the box model, which is not inherited. */
    static void box(InlineNode group, Style s) {
        for (Node p : group.getChildren()) {
            s.declared(p.getCanonicalName(), p.getLine());
            switch (p.getCanonicalName()) {
                case "width" -> s.box().setWidth(value(p, SIZE, false));
                // The content keywords only measure widths: in a height they would be another way of writing auto
                // (reference/common/box.md).
                case "height" -> s.box().setHeight(value(p, LENGTH_OR_AUTO, false));
                case "min-width" -> s.box().setMinWidth(value(p, LENGTH_OR_AUTO, false));
                case "max-width" -> s.box().setMaxWidth(value(p, LENGTH, false));
                case "min-height" -> s.box().setMinHeight(value(p, LENGTH_OR_AUTO, false));
                case "max-height" -> s.box().setMaxHeight(value(p, LENGTH, false));
                case "aspect-ratio" -> s.box().setAspectRatio(aspectRatio(p));
                case "margin" -> s.box().setMargin(sides(p, LENGTH_OR_AUTO, true));
                case "padding" -> s.box().setPadding(nonNegativeSides(p, true));
                case "border" -> s.box().setBorder(nonNegativeSides(p, false));
                case "border-color" -> s.box().setBorderColor(colorSides(p));
                case "corner-radius" -> s.box().setCornerRadius(radius(p));
                case "background" -> s.box().setBackground(paint(p));
                default -> throw unread(p);
            }
        }
    }

    /**
     * {@code Typography} group, in an element or in {@code Document}: it is inherited. The lines are kept with the
     * internal name ({@code Size} is {@code font-size}).
     */
    static void typography(InlineNode group, Style s) {
        for (Node p : group.getChildren()) {
            String name = p.getCanonicalName();
            s.declared(PropertyKey.internal(name), p.getLine());
            read(TYPOGRAPHY, p, s);
        }
    }

    /**
     * A standalone property of an element, outside the groups (reference/common/structure.md and following):
     * {@code Offset}, {@code Marker}, {@code Link}, {@code Alt}... Its line is kept with its name. {@code Id},
     * {@code Rotate} and {@code Rotate origin} do not go in the style the same way and {@link BoxTreeBuilder} reads
     * them.
     */
    static void single(Node p, Style s) {
        s.declared(p.getCanonicalName(), p.getLine());
        read(SINGLE, p, s);
    }

    /** Reads the property with its table entry; if it has none, it is a schema property that is not read. */
    private static void read(Map<String, Property> table, Node p, Style s) {
        Property property = table.get(p.getCanonicalName());
        if (property == null) {
            throw unread(p);
        }
        property.read(p, s);
    }

    /**
     * The target of a {@code Link} (reference/common/links.md): an allowed URL or {@code #id}; that the {@code Id}
     * exists is checked at the end.
     */
    private static String link(Node p) {
        String destination = p.getText().trim();
        Links.problem(destination).ifPresent(problem -> {
            throw error(p, ErrorCode.LINK_SCHEME, "Link: " + problem);
        });
        return destination;
    }

    /** The alternative text of an {@code Image}: not empty; for a decorative image, it is not declared. */
    private static String alt(Node p) {
        String alt = p.getText().trim();
        if (alt.isEmpty()) {
            throw error(p, ErrorCode.VALUE_NOT_ALLOWED, "Alt needs a text. A decorative image does not "
                    + "carry Alt");
        }
        return alt;
    }

    /** {@code Break} group: the page breaks, which are not inherited. */
    static void fragmentation(InlineNode group, Style s) {
        for (Node p : group.getChildren()) {
            String name = p.getCanonicalName();
            s.declared(PropertyKey.internal(name), p.getLine());
            switch (name) {
                case "before" -> s.breaks().setBefore(enumValue(Break.class, p));
                case "after" -> s.breaks().setAfter(enumValue(Break.class, p));
                case "keep-with-next" -> s.breaks().setKeepWithNext(bool(p));
                case "keep-with-previous" -> s.breaks().setKeepWithPrevious(bool(p));
                case "keep-together" -> s.breaks().setKeepTogether(bool(p));
                case "edges" -> s.breaks().setEdges(enumValue(Edges.class, p));
                default -> throw unread(p);
            }
        }
    }

    /** Stack properties, children of {@code Stack}; their line is kept with a prefix ({@code stack.gap}). */
    static void stack(Node p, Style s) {
        s.declared(PropertyKey.stack(p.getCanonicalName()), p.getLine());
        switch (p.getCanonicalName()) {
            case "direction" -> s.stack().setDirection(enumValue(Direction.class, p));
            case "wrap" -> s.stack().setWrap(bool(p));
            case "gap" -> {
                List<Value> gap = gap(p);
                s.stack().setGap(gap.get(0), gap.get(gap.size() - 1), gap.size() == 2);
            }
            case "horizontal" -> s.stack().setHorizontal(enumValue(Distribution.class, p));
            case "vertical" -> s.stack().setVertical(enumValue(Distribution.class, p));
            case "line-distribution" -> s.stack().setLineDistribution(enumValue(Distribution.class, p));
            default -> throw unread(p);
        }
    }

    /** Grid properties, children of the {@code Grid} element; their line is kept with a prefix ({@code grid.rows}). */
    static void grid(Node p, Style s) {
        s.declared(PropertyKey.grid(p.getCanonicalName()), p.getLine());
        switch (p.getCanonicalName()) {
            case "column-sizes" -> s.grid().setColumnSizes(tracks(p));
            case "row-sizes" -> s.grid().setRowSizes(tracks(p));
            case "gap" -> {
                List<Value> gap = gap(p);
                s.grid().setGap(gap.get(0), gap.get(gap.size() - 1));
            }
            case "areas" -> s.grid().setAreas(placement(p, () -> GridAreas.parse(p.getText())));
            case "flow" -> s.grid().setFlow(enumValue(Direction.class, p));
            case "horizontal" -> s.grid().setHorizontal(alignment(p));
            case "vertical" -> s.grid().setVertical(alignment(p));
            default -> throw unread(p);
        }
    }

    /**
     * Properties of {@code Columns}, children of the element (reference/components/columns.md); their line is kept
     * without a prefix ({@code separator}, {@code gap}): the {@code Gap} of {@code Stack} and {@code Grid} carries
     * one.
     */
    static void columns(Node p, Style s) {
        s.declared(p.getCanonicalName(), p.getLine());
        switch (p.getCanonicalName()) {
            case "count" -> s.columns().setCount(positiveInteger(p));
            case "min-column-width" -> s.columns().setMinColumnWidth(positive(p));
            case "gap" -> s.columns().setColumnGap(columnGap(p));
            case "separator" -> s.columns().setSeparator(positive(p));
            case "separator-color" -> s.columns().setSeparatorColor(color(p));
            case "balance" -> s.columns().setBalance(bool(p));
            default -> throw unread(p);
        }
    }

    /**
     * Properties of {@code Item} or {@code Area}, which pass to the element they wrap; their line is kept with a prefix
     * ({@code item.grow}), except {@code Layer}. The value of {@code Area} is the placement ({@code item.area}, with
     * the line of the {@code Area}), with the names looked up in {@code areas}, those of the parent grid.
     */
    static void item(InlineNode w, Style s, GridAreas areas) {
        if (w.getCanonicalName().equals("area") && !w.getText().isBlank()) {
            s.declared(PropertyKey.ITEM_AREA, w.getLine());
            s.item().setArea(placement(w, () -> Cell.parse(w.getText(), areas)));
        }
        for (Node p : w.getChildren()) {
            String name = p.getCanonicalName();
            if (isItemElement(p)) {
                continue;
            }
            if (name.equals("layer")) {
                s.declared(PropertyKey.LAYER, p.getLine());
                s.setLayer(integer(p));
                continue;
            }
            s.declared(PropertyKey.item(name), p.getLine());
            switch (name) {
                case "grow" -> s.item().setGrow(factor(p));
                case "shrink" -> s.item().setShrink(factor(p));
                case "base-size" -> s.item().setBaseSize(value(p, LENGTH_OR_AUTO, false));
                case "horizontal" -> s.item().setHorizontal(alignment(p));
                case "vertical" -> s.item().setVertical(alignment(p));
                default -> throw unread(p);
            }
        }
    }

    /**
     * {@code Horizontal} or {@code Vertical} of a {@code Grid}, an {@code Area} or an {@code Item}: an alignment.
     * {@code justify}, {@code around} and {@code distribute} distribute the main axis of a {@code Stack} and are only
     * valid there.
     */
    private static Alignment alignment(Node p) {
        Distribution d = enumValue(Distribution.class, p);
        if (d == Distribution.JUSTIFY || d == Distribution.AROUND || d == Distribution.DISTRIBUTE) {
            throw error(p, ErrorCode.VALUE_NOT_ALLOWED, p.getName() + " aligns in place: it allows start, end, center or "
                    + "stretch, and " + p.getText().trim() + " distributes the main axis of a Stack and is only valid there");
        }
        return Alignment.valueOf(d.name());
    }

    /** {@code Number label}: a template with {@code {number}} exactly once (reference/components/heading.md). */
    private static String numberLabel(Node p) {
        String label = p.getText().strip();
        int at = label.indexOf(HeadingNumbers.NUMBER);
        if (at < 0 || label.indexOf(HeadingNumbers.NUMBER, at + 1) >= 0) {
            throw error(p, ErrorCode.VALUE_NOT_ALLOWED, "Number label carries {number} once, where the number goes, "
                    + "as in Number label: Appendix {number}.");
        }
        return label;
    }

    /** {@code Gap} of {@code Columns}: a single length, the gap between columns. */
    private static double columnGap(Node p) {
        if (p.getText().trim().split("\\s+").length > 1) {
            throw error(p, ErrorCode.VALUE_NOT_ALLOWED, "Gap of a Columns allows a single value, the gap between "
                    + "columns: " + p.getText().trim());
        }
        return nonNegative(p);
    }

    /**
     * {@code Gap} of {@code Stack} or {@code Grid}, as it was written: one value for both axes, or the gap between rows
     * and the gap between columns.
     */
    private static List<Value> gap(Node p) {
        List<Value> v = parse(p, () -> ValueParser.list(p.getText()));
        if (v.size() > 2) {
            throw error(p, ErrorCode.VALUE_NOT_ALLOWED, "Gap allows 1 or 2 values (rows columns): " + p.getText().trim());
        }
        v.forEach(x -> check(p, x, LENGTH, false));
        return v;
    }
}
