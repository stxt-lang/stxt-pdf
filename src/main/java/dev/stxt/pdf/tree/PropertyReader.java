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
import dev.stxt.pdf.model.WhiteSpace;

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
import static dev.stxt.pdf.tree.ValueReader.breakInside;
import static dev.stxt.pdf.tree.ValueReader.check;
import static dev.stxt.pdf.tree.ValueReader.color;
import static dev.stxt.pdf.tree.ValueReader.colorSides;
import static dev.stxt.pdf.tree.ValueReader.enumValue;
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
 * Lee las propiedades de una caja en su {@link Style}: los grupos {@code Box}, {@code Typography}
 * y {@code Break} (referencia/comun/estructura.md), que comparten los elementos y las reglas de
 * {@code Styles}, las propiedades de {@code Flex}, {@code Grid} y {@code Columns} y las de los envoltorios
 * {@code Item} y {@code Area}. Cada propiedad guarda su línea ({@link Style#declared}).
 *
 * <p>Los {@code switch} no tienen una rama {@code default} que asigne, y las tablas
 * ({@code Typography} y las propiedades sueltas) no tienen entrada por defecto: lo que el schema admite
 * y no se lee es un error del motor ({@code SchemaCoverageTest}).</p>
 */
final class PropertyReader {

    /**
     * Propiedades del elemento {@code Flex} (la estructura completa está en el schema, referencia/comun/estructura.md).
     */
    static final Set<String> FLEX = Set.of("direction", "wrap", "gap", "horizontal", "vertical", "lines");
    /** Propiedades de los elementos {@code Grid} y {@code Table}. */
    static final Set<String> GRID = Set.of("column-sizes", "row-sizes", "gap", "areas", "flow", "horizontal", "vertical");
    /** Propiedades del elemento {@code Columns}. */
    static final Set<String> COLUMNS = Set.of("count", "min-column-width", "gap", "separator", "separator-color", "balance");

    /** Lee una propiedad en el estilo; su línea ya está apuntada. */
    @FunctionalInterface
    private interface Property {
        void read(Node p, Style s);
    }

    /** Grupo {@code Typography}, por nombre canónico. */
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
            entry("markup", (p, s) -> s.typography().setMarkup(enumValue(Markup.class, p))),
            entry("case", (p, s) -> s.typography().setTextCase(enumValue(TextCase.class, p))),
            entry("letter-spacing", (p, s) -> s.typography().setLetterSpacing(signedPoints(p))),
            entry("small-caps", (p, s) -> s.typography().setSmallCaps(bool(p))),
            entry("ligatures", (p, s) -> s.typography().setLigatures(bool(p))),
            entry("white-space", (p, s) -> s.typography().setWhiteSpace(enumValue(WhiteSpace.class, p))),
            entry("tab-size", (p, s) -> s.typography().setTabSize(positiveInteger(p))),
            entry("numbered", (p, s) -> s.typography().setNumbered(bool(p))),
            entry("number-format", (p, s) -> s.typography().setNumberFormat(enumValue(NumberFormat.class, p))),
            entry("number-label", (p, s) -> s.typography().setNumberLabel(numberLabel(p))),
            entry("widows", (p, s) -> s.typography().setWidows(positiveInteger(p))),
            entry("orphans", (p, s) -> s.typography().setOrphans(positiveInteger(p))),
            entry("language", (p, s) -> s.typography().setLocale(Locale.forLanguageTag(language(p)))));

    /** Propiedades sueltas de los elementos ({@link #single}), por nombre canónico. */
    private static final Map<String, Property> SINGLE = Map.ofEntries(
            entry("offset", (p, s) -> s.setOffset(sides(p, LENGTH_OR_AUTO, true))),
            entry("layer", (p, s) -> s.setLayer(integer(p))),
            entry("pages", (p, s) -> s.setPages(enumValue(Pages.class, p))),
            entry("marker", (p, s) -> s.list().setMarker(enumValue(Marker.class, p))),
            entry("start", (p, s) -> s.list().setStart(integer(p))),
            entry("number", (p, s) -> s.list().setNumber(integer(p))),
            entry("contain-margins", (p, s) -> s.setContainMargins(bool(p))),
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

    /** Grupo {@code Box}: el modelo de caja, que no se hereda. */
    static void box(InlineNode group, Style s) {
        for (Node p : group.getChildren()) {
            s.declared(p.getCanonicalName(), p.getLine());
            switch (p.getCanonicalName()) {
                case "width" -> s.box().setWidth(value(p, SIZE, false));
                case "height" -> s.box().setHeight(value(p, SIZE, false));
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
     * Una propiedad suelta de un elemento, fuera de los grupos (referencia/comun/estructura.md y siguientes): {@code
     * Offset}, {@code Marker}, {@code Link}, {@code Alt}… Su línea se guarda con su nombre. {@code Id}, {@code Rotate}
     * y {@code Rotate origin} no van en el estilo de la misma forma y los lee {@link BoxTreeBuilder}.
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

    /**
     * El destino de un {@code Link} (referencia/comun/enlaces.md): una URL admitida o {@code #id}; que el {@code Id}
     * exista se comprueba al final.
     */
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
                case "edges" -> s.breaks().setEdges(enumValue(Edges.class, p));
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
                s.flex().setGap(gap.get(0), gap.get(gap.size() - 1), gap.size() == 2);
            }
            case "horizontal" -> s.flex().setHorizontal(enumValue(Distribution.class, p));
            case "vertical" -> s.flex().setVertical(enumValue(Distribution.class, p));
            case "lines" -> s.flex().setLines(enumValue(Distribution.class, p));
            default -> throw unread(p);
        }
    }

    /** Propiedades de grid, hijas del elemento {@code Grid}; su línea se guarda con prefijo ({@code grid.rows}). */
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
     * Propiedades de {@code Columns}, hijas del elemento (referencia/componentes/columns.md); su línea se guarda sin
     * prefijo ({@code separator}, {@code gap}): el {@code Gap} de {@code Flex} y {@code Grid} lo lleva.
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
                s.setLayer(integer(p));
                continue;
            }
            s.declared(PropertyKey.item(name), p.getLine());
            switch (name) {
                case "grow" -> s.item().setGrow(nonNegative(p));
                case "shrink" -> s.item().setShrink(nonNegative(p));
                case "basis" -> s.item().setBasis(value(p, LENGTH_OR_AUTO, false));
                case "horizontal" -> s.item().setHorizontal(alignment(p));
                case "vertical" -> s.item().setVertical(alignment(p));
                default -> throw unread(p);
            }
        }
    }

    /**
     * {@code Horizontal} o {@code Vertical} de un {@code Grid}, un {@code Area} o un {@code Item}: una alineación.
     * {@code between}, {@code around} y {@code evenly} reparten el eje principal de un {@code Flex} y solo valen en él.
     */
    private static Alignment alignment(Node p) {
        Distribution d = enumValue(Distribution.class, p);
        if (d == Distribution.BETWEEN || d == Distribution.AROUND || d == Distribution.EVENLY) {
            throw error(p, ErrorCode.VALUE_NOT_ALLOWED, p.getName() + " alinea en su sitio: admite start, end, center o "
                    + "stretch; " + p.getText().trim() + " reparte el eje principal de un Flex y solo vale en él");
        }
        return Alignment.valueOf(d.name());
    }

    /** {@code Number label}: una plantilla con {@code {number}} una sola vez (referencia/componentes/heading.md). */
    private static String numberLabel(Node p) {
        String label = p.getText().strip();
        int at = label.indexOf(HeadingNumbers.NUMBER);
        if (at < 0 || label.indexOf(HeadingNumbers.NUMBER, at + 1) >= 0) {
            throw error(p, ErrorCode.VALUE_NOT_ALLOWED, "Number label lleva {number} una vez, donde va el número: "
                    + "Number label: Anexo {number}.");
        }
        return label;
    }

    /** {@code Gap} de {@code Columns}: un solo valor, el hueco entre columnas, en puntos. */
    private static double columnGap(Node p) {
        if (p.getText().trim().split("\\s+").length > 1) {
            throw error(p, ErrorCode.VALUE_NOT_ALLOWED, "Gap de un Columns admite un solo valor, el hueco entre "
                    + "columnas: " + p.getText().trim());
        }
        return nonNegative(p);
    }

    /**
     * {@code Gap} de {@code Flex} o {@code Grid}, como se escribió: un valor para los dos ejes, o el hueco entre filas
     * y el hueco entre columnas.
     */
    private static List<Value> gap(Node p) {
        List<Value> v = parse(p, () -> ValueParser.list(p.getText()));
        if (v.size() > 2) {
            throw error(p, ErrorCode.VALUE_NOT_ALLOWED, "Gap admite 1 o 2 valores (filas columnas): " + p.getText().trim());
        }
        v.forEach(x -> check(p, x, LENGTH, false));
        return v;
    }
}
