package dev.stxt.boxes.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.boxes.model.Cell;
import dev.stxt.boxes.model.FontStyle;
import dev.stxt.boxes.model.FontWeight;
import dev.stxt.boxes.model.GridAreas;
import dev.stxt.boxes.model.Value;
import dev.stxt.boxes.model.ValueParser;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static dev.stxt.boxes.tree.Nodes.error;
import static dev.stxt.boxes.tree.Nodes.isItemElement;
import static dev.stxt.boxes.tree.Nodes.parse;
import static dev.stxt.boxes.tree.Nodes.placement;
import static dev.stxt.boxes.tree.Nodes.unread;
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
 * <p>Los {@code switch} no tienen una rama {@code default} que asigne: lo que el schema admite
 * y no se lee es un error del motor ({@code SchemaCoverageTest}).</p>
 */
final class PropertyReader {

    /** Propiedades del elemento {@code Flex} (la estructura completa está en el schema, §9). */
    static final Set<String> FLEX = Set.of("direction", "wrap", "gap", "main-axis", "cross-axis", "lines");
    /** Propiedades de los elementos {@code Grid} y {@code Table}. */
    static final Set<String> GRID = Set.of("columns", "rows", "gap", "areas", "flow", "horizontal", "vertical");
    /** Propiedades del elemento {@code Multicol}. */
    static final Set<String> MULTICOL = Set.of("count", "column-width", "column-gap", "rule", "rule-color", "fill");

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
            switch (name) {
                case "font" -> s.font = fontFamily(p);
                case "weight" -> s.fontWeight = enumValue(FontWeight.class, p);
                case "style" -> s.fontStyle = enumValue(FontStyle.class, p);
                case "size" -> s.fontSize = positive(p);
                case "line-height" -> s.lineHeight = value(p, LENGTH, false);
                case "align" -> s.textAlign = enumValue(Style.TextAlign.class, p);
                case "color" -> s.color = color(p);
                case "text-wrap" -> s.textWrap = enumValue(Style.TextWrap.class, p);
                case "inline" -> s.inline = enumValue(Style.Inline.class, p);
                case "transform" -> s.transform = enumValue(Style.Transform.class, p);
                case "letter-spacing" -> s.letterSpacing = signedPoints(p);
                case "small-caps" -> s.smallCaps = bool(p);
                case "white-space" -> s.whiteSpace = enumValue(Style.WhiteSpace.class, p);
                case "tab-size" -> s.tabSize = positiveInteger(p);
                case "widows" -> s.widows = positiveInteger(p);
                case "orphans" -> s.orphans = positiveInteger(p);
                case "language" -> s.locale = Locale.forLanguageTag(language(p));
                default -> throw unread(p);
            }
        }
    }

    /** Grupo {@code Break}: los cortes de página, que no se heredan. */
    static void fragmentation(InlineNode group, Style s) {
        for (Node p : group.getChildren()) {
            String name = p.getCanonicalName();
            s.declared(PropertyKey.internal(name), p.getLine());
            switch (name) {
                case "before" -> s.breakBefore = enumValue(Style.Break.class, p);
                case "after" -> s.breakAfter = enumValue(Style.Break.class, p);
                case "inside" -> s.avoidBreakInside = breakInside(p);
                case "decoration" -> s.decoration = enumValue(Style.Decoration.class, p);
                default -> throw unread(p);
            }
        }
    }

    /** Propiedades de flex, hijas del elemento {@code Flex}; su línea se guarda con prefijo ({@code flex.gap}). */
    static void flex(Node p, Style s) {
        s.declared(PropertyKey.flex(p.getCanonicalName()), p.getLine());
        switch (p.getCanonicalName()) {
            case "direction" -> s.flexDirection = enumValue(Style.Direction.class, p);
            case "wrap" -> s.flexWrap = bool(p);
            case "gap" -> gap(p, s);
            case "main-axis" -> s.flexMainAxis = enumValue(Style.Distribution.class, p);
            case "cross-axis" -> s.flexCrossAxis = enumValue(Style.Alignment.class, p);
            case "lines" -> s.flexLines = enumValue(Style.Distribution.class, p);
            default -> throw unread(p);
        }
    }

    /** Propiedades de grid, hijas del elemento {@code Grid}; su línea se guarda con prefijo ({@code grid.rows}). */
    static void grid(Node p, Style s) {
        s.declared(PropertyKey.grid(p.getCanonicalName()), p.getLine());
        switch (p.getCanonicalName()) {
            case "columns" -> s.gridColumns = tracks(p);
            case "rows" -> s.gridRows = tracks(p);
            case "gap" -> gap(p, s);
            case "areas" -> s.gridAreas = placement(p, () -> GridAreas.parse(p.getText()));
            case "flow" -> s.gridFlow = enumValue(Style.Direction.class, p);
            case "horizontal" -> s.gridHorizontal = enumValue(Style.Alignment.class, p);
            case "vertical" -> s.gridVertical = enumValue(Style.Alignment.class, p);
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
            case "count" -> s.multicolCount = positiveInteger(p);
            case "column-width" -> s.multicolWidth = positive(p);
            case "column-gap" -> s.multicolGap = nonNegative(p);
            case "rule" -> s.multicolRule = positive(p);
            case "rule-color" -> s.multicolRuleColor = color(p);
            case "fill" -> s.multicolFill = enumValue(Style.Fill.class, p);
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
            s.itemArea = placement(w, () -> Cell.parse(w.getText(), areas));
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
                case "grow" -> s.itemGrow = nonNegative(p);
                case "shrink" -> s.itemShrink = nonNegative(p);
                case "basis" -> s.itemBasis = value(p, LENGTH_OR_AUTO, false);
                case "cross-axis" -> s.itemCrossAxis = enumValue(Style.Alignment.class, p);
                case "horizontal" -> s.itemHorizontal = enumValue(Style.Alignment.class, p);
                case "vertical" -> s.itemVertical = enumValue(Style.Alignment.class, p);
                default -> throw unread(p);
            }
        }
    }

    private static void gap(Node p, Style s) {
        List<Value> v = parse(p, () -> ValueParser.list(p.getText()));
        if (v.size() > 2) {
            throw error(p, ErrorCode.VALUE_NOT_ALLOWED, "Gap admite 1 o 2 valores (filas columnas): " + p.getText().trim());
        }
        v.forEach(x -> check(p, x, LENGTH, false));
        s.rowGap = v.get(0);
        s.columnGap = v.size() == 2 ? v.get(1) : v.get(0);
    }
}
