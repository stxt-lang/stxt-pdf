package dev.stxt.boxes.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.boxes.model.Alignment;
import dev.stxt.boxes.model.Break;
import dev.stxt.boxes.model.Cell;
import dev.stxt.boxes.model.ColumnFill;
import dev.stxt.boxes.model.Decoration;
import dev.stxt.boxes.model.Direction;
import dev.stxt.boxes.model.Distribution;
import dev.stxt.boxes.model.Fit;
import dev.stxt.boxes.model.FontStyle;
import dev.stxt.boxes.model.FontWeight;
import dev.stxt.boxes.model.GridAreas;
import dev.stxt.boxes.model.Inline;
import dev.stxt.boxes.model.Leader;
import dev.stxt.boxes.model.Links;
import dev.stxt.boxes.model.Marker;
import dev.stxt.boxes.model.Pages;
import dev.stxt.boxes.model.TextAlign;
import dev.stxt.boxes.model.TextWrap;
import dev.stxt.boxes.model.Transform;
import dev.stxt.boxes.model.Value;
import dev.stxt.boxes.model.ValueParser;
import dev.stxt.boxes.model.WhiteSpace;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static dev.stxt.boxes.tree.Nodes.error;
import static dev.stxt.boxes.tree.Nodes.isItemElement;
import static dev.stxt.boxes.tree.Nodes.parse;
import static dev.stxt.boxes.tree.Nodes.placement;
import static dev.stxt.boxes.tree.Nodes.unread;
import static java.util.Map.entry;
import static dev.stxt.boxes.tree.ValueReader.LENGTH;
import static dev.stxt.boxes.tree.ValueReader.LENGTH_OR_AUTO;
import static dev.stxt.boxes.tree.ValueReader.SIZE;
import static dev.stxt.boxes.tree.ValueReader.aspectRatio;
import static dev.stxt.boxes.tree.ValueReader.bool;
import static dev.stxt.boxes.tree.ValueReader.breakInside;
import static dev.stxt.boxes.tree.ValueReader.check;
import static dev.stxt.boxes.tree.ValueReader.color;
import static dev.stxt.boxes.tree.ValueReader.colorSides;
import static dev.stxt.boxes.tree.ValueReader.enumValue;
import static dev.stxt.boxes.tree.ValueReader.fontFamily;
import static dev.stxt.boxes.tree.ValueReader.integer;
import static dev.stxt.boxes.tree.ValueReader.language;
import static dev.stxt.boxes.tree.ValueReader.nonNegative;
import static dev.stxt.boxes.tree.ValueReader.nonNegativeSides;
import static dev.stxt.boxes.tree.ValueReader.paint;
import static dev.stxt.boxes.tree.ValueReader.radius;
import static dev.stxt.boxes.tree.ValueReader.positive;
import static dev.stxt.boxes.tree.ValueReader.signedPoints;
import static dev.stxt.boxes.tree.ValueReader.positiveInteger;
import static dev.stxt.boxes.tree.ValueReader.sides;
import static dev.stxt.boxes.tree.ValueReader.tracks;
import static dev.stxt.boxes.tree.ValueReader.value;

/**
 * Lee las propiedades de una caja en su {@link Style}: los grupos {@code Box}, {@code Typography}
 * y {@code Break} (ESPECIFICACION.md §4), que comparten los elementos y las reglas de
 * {@code Styles}, las propiedades de {@code Flex}, {@code Grid} y {@code Multicol} y las de los envoltorios
 * {@code Item} y {@code Area}. Cada propiedad guarda su línea ({@link Style#declared}).
 *
 * <p>Los {@code switch} no tienen una rama {@code default} que asigne, y las tablas
 * ({@code Typography} y las propiedades sueltas) no tienen entrada por defecto: lo que el schema admite
 * y no se lee es un error del motor ({@code SchemaCoverageTest}).</p>
 */
final class PropertyReader {

    /** Propiedades del elemento {@code Flex} (la estructura completa está en el schema, §9). */
    static final Set<String> FLEX = Set.of("direction", "wrap", "gap", "main-axis", "cross-axis", "lines");
    /** Propiedades de los elementos {@code Grid} y {@code Table}. */
    static final Set<String> GRID = Set.of("columns", "rows", "gap", "areas", "flow", "horizontal", "vertical");
    /** Propiedades del elemento {@code Multicol}. */
    static final Set<String> MULTICOL = Set.of("count", "column-width", "column-gap", "rule", "rule-color", "fill");

    /** Lee una propiedad en el estilo; su línea ya está apuntada. */
    @FunctionalInterface
    private interface Property {
        void read(Node p, Style s);
    }

    /** Grupo {@code Typography}, por nombre canónico. */
    private static final Map<String, Property> TYPOGRAPHY = Map.ofEntries(
            entry("font", (p, s) -> s.font = fontFamily(p)),
            entry("weight", (p, s) -> s.fontWeight = enumValue(FontWeight.class, p)),
            entry("style", (p, s) -> s.fontStyle = enumValue(FontStyle.class, p)),
            entry("size", (p, s) -> s.fontSize = positive(p)),
            entry("line-height", (p, s) -> s.lineHeight = value(p, LENGTH, false)),
            entry("align", (p, s) -> s.textAlign = enumValue(TextAlign.class, p)),
            entry("color", (p, s) -> s.color = color(p)),
            entry("text-wrap", (p, s) -> s.textWrap = enumValue(TextWrap.class, p)),
            entry("inline", (p, s) -> s.inline = enumValue(Inline.class, p)),
            entry("transform", (p, s) -> s.transform = enumValue(Transform.class, p)),
            entry("letter-spacing", (p, s) -> s.letterSpacing = signedPoints(p)),
            entry("small-caps", (p, s) -> s.smallCaps = bool(p)),
            entry("ligatures", (p, s) -> s.ligatures = bool(p)),
            entry("white-space", (p, s) -> s.whiteSpace = enumValue(WhiteSpace.class, p)),
            entry("tab-size", (p, s) -> s.tabSize = positiveInteger(p)),
            entry("numbered", (p, s) -> s.numbered = bool(p)),
            entry("widows", (p, s) -> s.widows = positiveInteger(p)),
            entry("orphans", (p, s) -> s.orphans = positiveInteger(p)),
            entry("language", (p, s) -> s.locale = Locale.forLanguageTag(language(p))));

    /** Propiedades sueltas de los elementos ({@link #single}), por nombre canónico. */
    private static final Map<String, Property> SINGLE = Map.ofEntries(
            entry("offset", (p, s) -> s.offset = sides(p, LENGTH_OR_AUTO, true)),
            entry("layer", (p, s) -> s.layer = integer(p)),
            entry("pages", (p, s) -> s.pages = enumValue(Pages.class, p)),
            entry("marker", (p, s) -> s.list().setMarker(enumValue(Marker.class, p))),
            entry("start", (p, s) -> s.list().setStart(integer(p))),
            entry("number", (p, s) -> s.list().setNumber(integer(p))),
            entry("isolate", (p, s) -> s.isolate = bool(p)),
            entry("link", (p, s) -> s.link = link(p)),
            entry("header", (p, s) -> s.header = bool(p)),
            entry("in-contents", (p, s) -> s.inContents = bool(p)),
            entry("alt", (p, s) -> s.image().setAlt(alt(p))),
            entry("fit", (p, s) -> s.image().setFit(enumValue(Fit.class, p))),
            entry("levels", ContentsBuilder::levels),
            entry("leader", (p, s) -> s.contents().setLeader(enumValue(Leader.class, p))));

    private PropertyReader() {
    }

    /** Grupo {@code Box}: el modelo de caja, que no se hereda. */
    static void box(InlineNode group, Style s) {
        for (Node p : group.getChildren()) {
            s.declared(p.getCanonicalName(), p.getLine());
            switch (p.getCanonicalName()) {
                case "width" -> s.width = value(p, SIZE, false);
                case "height" -> s.height = value(p, SIZE, false);
                case "min-width" -> s.minWidth = value(p, LENGTH_OR_AUTO, false);
                case "max-width" -> s.maxWidth = value(p, LENGTH, false);
                case "min-height" -> s.minHeight = value(p, LENGTH_OR_AUTO, false);
                case "max-height" -> s.maxHeight = value(p, LENGTH, false);
                case "aspect-ratio" -> s.aspectRatio = aspectRatio(p);
                case "margin" -> s.margin = sides(p, LENGTH_OR_AUTO, true);
                case "padding" -> s.padding = nonNegativeSides(p, true);
                case "border" -> s.border = nonNegativeSides(p, false);
                case "border-color" -> s.borderColor = colorSides(p);
                case "radius" -> s.radius = radius(p);
                case "background" -> s.background = paint(p);
                default -> throw unread(p);
            }
        }
    }

    /**
     * Grupo {@code Typography}, en un elemento o en {@code Document}: se hereda. Las líneas se
     * guardan con el nombre interno ({@code Size} es {@code font-size}).
     */
    static void typography(InlineNode group, Style s) {
        for (Node p : group.getChildren()) {
            String name = p.getCanonicalName();
            s.declared(PropertyKey.internal(name), p.getLine());
            read(TYPOGRAPHY, p, s);
        }
    }

    /**
     * Una propiedad suelta de un elemento, fuera de los grupos (§4.2 y siguientes): {@code Offset}, {@code Marker},
     * {@code Link}, {@code Alt}… Su línea se guarda con su nombre. {@code Id}, {@code Rotate} y
     * {@code Rotate origin} no van en el estilo de la misma forma y los lee {@link BoxTreeBuilder}.
     */
    static void single(Node p, Style s) {
        s.declared(p.getCanonicalName(), p.getLine());
        read(SINGLE, p, s);
    }

    /** Lee la propiedad con su entrada de la tabla; si no la tiene, es una propiedad del schema que no se lee. */
    private static void read(Map<String, Property> table, Node p, Style s) {
        Property property = table.get(p.getCanonicalName());
        if (property == null) {
            throw unread(p);
        }
        property.read(p, s);
    }

    /** El destino de un {@code Link} (§5.20): una URL admitida o {@code #id}; que el {@code Id} exista se comprueba al final. */
    private static String link(Node p) {
        String destination = p.getText().trim();
        Links.problem(destination).ifPresent(problem -> {
            throw error(p, ErrorCode.LINK_SCHEME, "Link: " + problem);
        });
        return destination;
    }

    /** El texto alternativo de una {@code Image}: no vacío; para una imagen decorativa, no se declara. */
    private static String alt(Node p) {
        String alt = p.getText().trim();
        if (alt.isEmpty()) {
            throw error(p, ErrorCode.VALUE_NOT_ALLOWED, "Alt necesita un texto; si la imagen es decorativa, "
                    + "quita Alt");
        }
        return alt;
    }

    /** Grupo {@code Break}: los cortes de página, que no se heredan. */
    static void fragmentation(InlineNode group, Style s) {
        for (Node p : group.getChildren()) {
            String name = p.getCanonicalName();
            s.declared(PropertyKey.internal(name), p.getLine());
            switch (name) {
                case "before" -> s.breaks().setBefore(enumValue(Break.class, p));
                case "after" -> s.breaks().setAfter(enumValue(Break.class, p));
                case "inside" -> s.breaks().setAvoidInside(breakInside(p));
                case "decoration" -> s.breaks().setDecoration(enumValue(Decoration.class, p));
                default -> throw unread(p);
            }
        }
    }

    /** Propiedades de flex, hijas del elemento {@code Flex}; su línea se guarda con prefijo ({@code flex.gap}). */
    static void flex(Node p, Style s) {
        s.declared(PropertyKey.flex(p.getCanonicalName()), p.getLine());
        switch (p.getCanonicalName()) {
            case "direction" -> s.flex().setDirection(enumValue(Direction.class, p));
            case "wrap" -> s.flex().setWrap(bool(p));
            case "gap" -> {
                List<Value> gap = gap(p);
                s.flex().setGap(gap.get(0), gap.get(1));
            }
            case "main-axis" -> s.flex().setMainAxis(enumValue(Distribution.class, p));
            case "cross-axis" -> s.flex().setCrossAxis(enumValue(Alignment.class, p));
            case "lines" -> s.flex().setLines(enumValue(Distribution.class, p));
            default -> throw unread(p);
        }
    }

    /** Propiedades de grid, hijas del elemento {@code Grid}; su línea se guarda con prefijo ({@code grid.rows}). */
    static void grid(Node p, Style s) {
        s.declared(PropertyKey.grid(p.getCanonicalName()), p.getLine());
        switch (p.getCanonicalName()) {
            case "columns" -> s.grid().setColumns(tracks(p));
            case "rows" -> s.grid().setRows(tracks(p));
            case "gap" -> {
                List<Value> gap = gap(p);
                s.grid().setGap(gap.get(0), gap.get(1));
            }
            case "areas" -> s.grid().setAreas(placement(p, () -> GridAreas.parse(p.getText())));
            case "flow" -> s.grid().setFlow(enumValue(Direction.class, p));
            case "horizontal" -> s.grid().setHorizontal(enumValue(Alignment.class, p));
            case "vertical" -> s.grid().setVertical(enumValue(Alignment.class, p));
            default -> throw unread(p);
        }
    }

    /**
     * Propiedades de {@code Multicol}, hijas del elemento (§4.13); sus nombres no se repiten en
     * otro elemento y su línea se guarda sin prefijo ({@code column-gap}).
     */
    static void multicol(Node p, Style s) {
        s.declared(p.getCanonicalName(), p.getLine());
        switch (p.getCanonicalName()) {
            case "count" -> s.multicol().setCount(positiveInteger(p));
            case "column-width" -> s.multicol().setColumnWidth(positive(p));
            case "column-gap" -> s.multicol().setColumnGap(nonNegative(p));
            case "rule" -> s.multicol().setRule(positive(p));
            case "rule-color" -> s.multicol().setRuleColor(color(p));
            case "fill" -> s.multicol().setFill(enumValue(ColumnFill.class, p));
            default -> throw unread(p);
        }
    }

    /**
     * Propiedades de {@code Item} o {@code Area}, que pasan al elemento que envuelven; su línea se
     * guarda con prefijo ({@code item.grow}), salvo {@code Layer}. El valor de {@code Area} es la
     * colocación ({@code item.area}, con la línea del {@code Area}), con los nombres buscados en
     * {@code areas}, las del grid padre.
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
                s.layer = integer(p);
                continue;
            }
            s.declared(PropertyKey.item(name), p.getLine());
            switch (name) {
                case "grow" -> s.item().setGrow(nonNegative(p));
                case "shrink" -> s.item().setShrink(nonNegative(p));
                case "basis" -> s.item().setBasis(value(p, LENGTH_OR_AUTO, false));
                case "cross-axis" -> s.item().setCrossAxis(enumValue(Alignment.class, p));
                case "horizontal" -> s.item().setHorizontal(enumValue(Alignment.class, p));
                case "vertical" -> s.item().setVertical(enumValue(Alignment.class, p));
                default -> throw unread(p);
            }
        }
    }

    /** {@code Gap} de {@code Flex} o {@code Grid}: el hueco entre filas y el hueco entre columnas. */
    private static List<Value> gap(Node p) {
        List<Value> v = parse(p, () -> ValueParser.list(p.getText()));
        if (v.size() > 2) {
            throw error(p, ErrorCode.VALUE_NOT_ALLOWED, "Gap admite 1 o 2 valores (filas columnas): " + p.getText().trim());
        }
        v.forEach(x -> check(p, x, LENGTH, false));
        return List.of(v.get(0), v.size() == 2 ? v.get(1) : v.get(0));
    }
}
