package dev.stxt.boxes.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.TextNode;
import dev.stxt.boxes.layout.grid.Area;
import dev.stxt.boxes.layout.grid.Cell;
import dev.stxt.boxes.layout.grid.GridAreas;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Sides;
import dev.stxt.boxes.model.Value;
import dev.stxt.boxes.model.ValueParser;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Construye el árbol de cajas a partir del documento (módulos 0 y 1, ESPECIFICACION.md §5.1):
 * interpreta los valores, aplica la herencia, crea las cajas anónimas y comprueba las reglas
 * que el template no puede expresar (§6).
 *
 * <p>Los errores señalan la línea del nodo culpable: la de la propiedad, no la de su caja.
 * Son errores también las combinaciones que no tendrían efecto o se contradicen: el modelo las
 * rechaza en lugar de resolverlas con una regla por defecto.</p>
 *
 * <p>Trabaja sobre los nodos de {@code stxt-core}. Los nombres se comparan por su nombre
 * canónico ({@link Node#getCanonicalName()}: minúsculas y los espacios convertidos en guiones,
 * de modo que {@code Box sizing} es {@code box-sizing}). No es reentrante: cada llamada a
 * {@link #build} usa su propio estado.</p>
 */
public final class BoxTreeBuilder {

    private static final Pattern COLOR = Pattern.compile("#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})");

    /** Clases de valor, para comprobar lo que admite cada propiedad (§3.1 y §4). */
    private enum Kind { LENGTH, AUTO, INTRINSIC, FRACTION, MINMAX }

    private static final Set<Kind> SIZE = EnumSet.of(Kind.LENGTH, Kind.AUTO, Kind.INTRINSIC);
    private static final Set<Kind> LENGTH_OR_AUTO = EnumSet.of(Kind.LENGTH, Kind.AUTO);
    private static final Set<Kind> LENGTH = EnumSet.of(Kind.LENGTH);

    /** Resultado: el medio y una caja raíz anónima que representa el área útil. */
    public record Result(Media media, Box root) {
    }

    /** Línea del {@code Id} de cada caja, para detectar los repetidos. */
    private Map<String, Integer> ids;
    /** Si el alto de la caja de contenido es definido: base de los porcentajes verticales (§3.6). */
    private Map<Box, Boolean> definiteHeight;

    public Result build(InlineNode document) {
        ids = new HashMap<>();
        definiteHeight = new HashMap<>();
        if (!is(document, "document")) {
            throw error(document, ErrorCode.DOCUMENT_ROOT, "La raíz del documento debe ser Document");
        }
        InlineNode mediaNode = child(document, "media")
                .map(BoxTreeBuilder::inline)
                .orElseThrow(() -> error(document, ErrorCode.DOCUMENT_STRUCTURE, "Falta el nodo Media"));
        Media media = media(mediaNode);

        Box root = new Box(null, new Style(), true, document.getLine());
        // El área útil: definida en modo paginado; infinita en continuo.
        definiteHeight.put(root, media.paged());
        buildContent(document, root);
        if (root.children.isEmpty()) {
            throw error(document, ErrorCode.DOCUMENT_STRUCTURE, "El documento necesita al menos una Box");
        }
        return new Result(media, root);
    }

    // ------------------------------------------------------------------------------------------
    // Media
    // ------------------------------------------------------------------------------------------

    private Media media(InlineNode n) {
        Media.Type type = enumValue(Media.Type.class, required(n, "Type"));
        double width = points(required(n, "Width"));
        Optional<Node> heightNode = child(n, "height");
        if (type == Media.Type.CONTINUOUS && heightNode.isPresent()) {
            throw error(heightNode.get(), ErrorCode.MEDIA_HEIGHT_CONTINUOUS,
                    "Height del medio no tiene efecto con Type: continuous, cuya altura es infinita");
        }
        if (type == Media.Type.PAGED && heightNode.isEmpty()) {
            throw error(n, ErrorCode.MEDIA_HEIGHT_REQUIRED, "Con Type: paged, Media necesita Height");
        }
        OptionalDouble height = heightNode.map(h -> OptionalDouble.of(points(h))).orElse(OptionalDouble.empty());
        Optional<Node> marginNode = child(n, "margin");
        Insets margin = marginNode.map(BoxTreeBuilder::mediaMargin).orElse(Insets.ZERO);
        Media media = new Media(type, width, height, margin);
        if (media.pageArea().width() <= 0 || media.pageArea().height() <= 0) {
            throw error(marginNode.orElse(n), ErrorCode.MEDIA_SIZE, "Los márgenes del medio no dejan área útil");
        }
        return media;
    }

    private static Insets mediaMargin(Node n) {
        List<Double> v = parse(n, () -> ValueParser.list(n.getText())).stream().map(x -> {
            if (!(x instanceof Value.Points p)) {
                throw error(n, ErrorCode.MEDIA_SIZE, "El margen del medio solo admite puntos: " + n.getText().trim());
            }
            return p.pt();
        }).toList();
        Sides<Double> s = parse(n, () -> Sides.shorthand(v));
        return new Insets(s.top(), s.right(), s.bottom(), s.left());
    }

    // ------------------------------------------------------------------------------------------
    // Cajas
    // ------------------------------------------------------------------------------------------

    /** Hijos {@code Box} y {@code Text} en orden; crea cajas anónimas si hace falta. */
    private void buildContent(InlineNode node, Box box) {
        List<Node> content = node.getChildren().stream()
                .filter(h -> is(h, "box") || is(h, "text"))
                .toList();
        Optional<Node> firstBox = content.stream().filter(h -> is(h, "box")).findFirst();
        Style.Layout layout = box.style.layout;

        if (layout == Style.Layout.INLINE && firstBox.isPresent()) {
            throw error(firstBox.get(), ErrorCode.INLINE_WITH_BOXES, "Una caja con Layout: inline solo puede contener "
                    + "Text (Layout: inline en la línea " + box.style.line("layout") + ")");
        }
        boolean textInAnonymous = firstBox.isPresent() || layout == Style.Layout.FLEX || layout == Style.Layout.GRID;

        for (Node h : content) {
            if (is(h, "box")) {
                box.addChild(box(inline(h), box));
                continue;
            }
            if (box.style.font == null) {
                throw error(h, ErrorCode.TEXT_WITHOUT_FONT,
                        "El texto necesita Font: declárala en la caja o en una antecesora");
            }
            Box target = box;
            if (textInAnonymous) {
                // Texto mezclado con cajas, o dentro de flex/grid: caja anónima (módulo 1, §6).
                Style s = Style.inheriting(box.style);
                s.layout = Style.Layout.INLINE;
                target = new Box(null, s, true, h.getLine());
                box.addChild(target);
            } else if (layout == Style.Layout.BLOCK) {
                // Un bloque que solo contiene texto se maqueta con el algoritmo de línea.
                box.style.layout = Style.Layout.INLINE;
            }
            target.text = h.getText();
            target.textLine = h.getLine();
            target.textBlock = h instanceof TextNode;
        }
    }

    private Box box(InlineNode n, Box parent) {
        Style s = Style.inheriting(parent.style);
        String id = null;
        for (Node p : n.getChildren()) {
            String name = p.getCanonicalName();
            if (!name.equals("box") && !name.equals("text")) {
                s.declared(name, p.getLine());
            }
            switch (name) {
                case "box", "text" -> {
                    // se procesan en buildContent
                }
                case "id" -> id = id(p);
                case "layout" -> s.layout = enumValue(Style.Layout.class, p);
                case "position" -> s.position = enumValue(Style.Position.class, p);
                case "offset" -> s.offset = sides(p, LENGTH_OR_AUTO, true);
                case "layer" -> s.layer = integer(p);
                case "isolate" -> s.isolate = bool(p);
                case "box-sizing" -> s.boxSizing = enumValue(Style.BoxSizing.class, p);
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
                case "background" -> s.background = color(p);
                case "color" -> s.color = color(p);
                case "border-color" -> s.borderColor = color(p);
                case "font" -> s.font = fontFamily(p);
                case "font-weight" -> s.fontWeight = enumValue(Style.FontWeight.class, p);
                case "font-style" -> s.fontStyle = enumValue(Style.FontStyle.class, p);
                case "font-size" -> s.fontSize = positive(p);
                case "line-height" -> s.lineHeight = value(p, LENGTH, false);
                case "text-align" -> s.textAlign = enumValue(Style.TextAlign.class, p);
                case "text-wrap" -> s.textWrap = enumValue(Style.TextWrap.class, p);
                case "flex" -> flex(inline(p), s);
                case "grid" -> grid(inline(p), s);
                case "item" -> item(inline(p), s);
                case "break-before" -> s.breakBefore = enumValue(Style.Break.class, p);
                case "break-after" -> s.breakAfter = enumValue(Style.Break.class, p);
                case "break-inside" -> s.avoidBreakInside = breakInside(p);
                case "widows" -> s.widows = positiveInteger(p);
                case "orphans" -> s.orphans = positiveInteger(p);
                case "decoration" -> s.decoration = enumValue(Style.Decoration.class, p);
                default -> throw error(p, ErrorCode.DOCUMENT_STRUCTURE, "Propiedad desconocida en Box: " + p.getName());
            }
        }
        validateBox(n, s, parent);

        Box box = new Box(id, s, false, n.getLine());
        definiteHeight.put(box, definiteHeight(s, parent));
        validateContainer(s, box);
        buildContent(n, box);
        return box;
    }

    private void flex(InlineNode n, Style s) {
        for (Node p : n.getChildren()) {
            s.declared("flex." + p.getCanonicalName(), p.getLine());
            switch (p.getCanonicalName()) {
                case "direction" -> s.flexDirection = enumValue(Style.Direction.class, p);
                case "wrap" -> s.flexWrap = bool(p);
                case "gap" -> gap(p, s);
                case "main-axis" -> s.flexMainAxis = enumValue(Style.Distribution.class, p);
                case "cross-axis" -> s.flexCrossAxis = enumValue(Style.Alignment.class, p);
                case "lines" -> s.flexLines = enumValue(Style.Distribution.class, p);
                default -> throw error(p, ErrorCode.DOCUMENT_STRUCTURE, "Propiedad desconocida en Flex: " + p.getName());
            }
        }
        if (s.flexMainAxis == Style.Distribution.STRETCH) {
            throw error(s.line("flex.main-axis"), ErrorCode.VALUE_NOT_ALLOWED,
                    "Main axis no admite 'stretch'; usa Grow en los ítems");
        }
        if (s.isDeclared("flex.lines") && !s.flexWrap) {
            throw error(s.line("flex.lines"), ErrorCode.LINES_WITHOUT_WRAP,
                    "Lines solo tiene efecto con Wrap: true, porque sin Wrap hay una sola línea");
        }
    }

    private void grid(InlineNode n, Style s) {
        for (Node p : n.getChildren()) {
            s.declared("grid." + p.getCanonicalName(), p.getLine());
            switch (p.getCanonicalName()) {
                case "columns" -> s.gridColumns = tracks(p);
                case "rows" -> s.gridRows = tracks(p);
                case "gap" -> gap(p, s);
                case "areas" -> {
                    s.gridAreas = p.getText();
                    // valida rectángulos y filas de igual longitud
                    placement(p, () -> GridAreas.parse(p.getText()));
                }
                case "flow" -> s.gridFlow = enumValue(Style.Direction.class, p);
                case "horizontal" -> s.gridHorizontal = enumValue(Style.Alignment.class, p);
                case "vertical" -> s.gridVertical = enumValue(Style.Alignment.class, p);
                default -> throw error(p, ErrorCode.DOCUMENT_STRUCTURE, "Propiedad desconocida en Grid: " + p.getName());
            }
        }
    }

    private void item(InlineNode n, Style s) {
        for (Node p : n.getChildren()) {
            s.declared("item." + p.getCanonicalName(), p.getLine());
            switch (p.getCanonicalName()) {
                case "grow" -> s.itemGrow = nonNegative(p);
                case "shrink" -> s.itemShrink = nonNegative(p);
                case "basis" -> s.itemBasis = value(p, LENGTH_OR_AUTO, false);
                case "cross-axis" -> s.itemCrossAxis = enumValue(Style.Alignment.class, p);
                case "cell" -> s.itemCell = p.getText().trim();
                case "horizontal" -> s.itemHorizontal = enumValue(Style.Alignment.class, p);
                case "vertical" -> s.itemVertical = enumValue(Style.Alignment.class, p);
                default -> throw error(p, ErrorCode.DOCUMENT_STRUCTURE, "Propiedad desconocida en Item: " + p.getName());
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Reglas que el template no puede expresar (§6)
    // ------------------------------------------------------------------------------------------

    private void validateBox(InlineNode n, Style s, Box parent) {
        Style.Layout parentLayout = parent.style.layout;
        boolean inFlow = !s.isOutOfFlow();
        boolean flexItem = inFlow && parentLayout == Style.Layout.FLEX;
        boolean gridItem = inFlow && parentLayout == Style.Layout.GRID;

        child(n, "flex").filter(f -> s.layout != Style.Layout.FLEX).ifPresent(f -> {
            throw error(f, ErrorCode.CONTAINER_MISMATCH, "Flex solo se aplica con Layout: flex");
        });
        child(n, "grid").filter(g -> s.layout != Style.Layout.GRID).ifPresent(g -> {
            throw error(g, ErrorCode.CONTAINER_MISMATCH, "Grid solo se aplica con Layout: grid");
        });
        validateItem(n, s, parent, flexItem, gridItem);
        validateOffset(s);
        validateMargins(s, parentLayout, flexItem, gridItem);
        if (s.isDeclared("layer") && s.position == Style.Position.STATIC && !flexItem && !gridItem) {
            throw error(s.line("layer"), ErrorCode.LAYER_WITHOUT_EFFECT, "Layer solo tiene efecto en cajas "
                    + "posicionadas (Position distinta de static) y en ítems de flex o grid");
        }
        if (s.isDeclared("aspect-ratio") && !s.width.isAuto() && !s.height.isAuto()) {
            throw error(s.line("aspect-ratio"), ErrorCode.ASPECT_RATIO_CONFLICT,
                    "Aspect ratio no tiene efecto si Width y Height están declarados");
        }
        validateMinMax(s, "width", s.width, "min-width", s.minWidth, "max-width", s.maxWidth);
        validateMinMax(s, "height", s.height, "min-height", s.minHeight, "max-height", s.maxHeight);
        if (s.isOutOfFlow()) {
            for (String b : List.of("break-before", "break-after")) {
                if (s.isDeclared(b) && (b.equals("break-before") ? s.breakBefore : s.breakAfter) == Style.Break.PAGE) {
                    throw error(s.line(b), ErrorCode.BREAK_NOT_ALLOWED,
                            "Los cortes forzados solo se aplican a cajas en flujo");
                }
            }
        }
        validatePercentages(s, parent, flexItem, gridItem);
    }

    private static void validateItem(InlineNode n, Style s, Box parent, boolean flexItem, boolean gridItem) {
        child(n, "item").filter(i -> !flexItem && !gridItem).ifPresent(i -> {
            throw error(i, ErrorCode.ITEM_MISMATCH,
                    "Item solo tiene efecto en hijos en flujo de un contenedor flex o grid");
        });
        if (flexItem) {
            for (String p : List.of("cell", "horizontal", "vertical")) {
                if (s.isDeclared("item." + p)) {
                    throw error(s.line("item." + p), ErrorCode.ITEM_MISMATCH,
                            label(p) + " de Item solo tiene efecto en ítems de grid, y el padre es flex");
                }
            }
        }
        if (gridItem) {
            for (String p : List.of("grow", "shrink", "basis", "cross-axis")) {
                if (s.isDeclared("item." + p)) {
                    throw error(s.line("item." + p), ErrorCode.ITEM_MISMATCH,
                            label(p) + " de Item solo tiene efecto en ítems de flex, y el padre es grid");
                }
            }
            if (s.itemCell != null) {
                Map<String, Area> areas = parent.style.gridAreas == null
                        ? Map.of() : GridAreas.parse(parent.style.gridAreas);
                placement(s.line("item.cell"), () -> Cell.parse(s.itemCell, areas));
            }
        }
    }

    private static void validateOffset(Style s) {
        if (!s.isDeclared("offset")) {
            return;
        }
        int line = s.line("offset");
        Sides<Value> o = s.offset;
        if (s.position == Style.Position.STATIC) {
            throw error(line, ErrorCode.OFFSET_ON_STATIC, "Offset no tiene efecto con Position: static");
        }
        if (s.position == Style.Position.RELATIVE) {
            if (!o.top().isAuto() && !o.bottom().isAuto()) {
                throw error(line, ErrorCode.OFFSET_CONFLICT, "Una caja relative no puede desplazarse a la vez "
                        + "desde arriba y desde abajo: deja uno de los dos en auto");
            }
            if (!o.left().isAuto() && !o.right().isAuto()) {
                throw error(line, ErrorCode.OFFSET_CONFLICT, "Una caja relative no puede desplazarse a la vez "
                        + "desde la izquierda y desde la derecha: deja uno de los dos en auto");
            }
            return;
        }
        // absolute y fixed: inicio + tamaño + fin no puede fijar la posición dos veces.
        if (!o.left().isAuto() && !s.width.isAuto() && !o.right().isAuto()
                && !s.margin.left().isAuto() && !s.margin.right().isAuto()) {
            throw error(line, ErrorCode.OVERCONSTRAINED, "Izquierda, Width y derecha declarados fijan la "
                    + "posición dos veces: deja uno en auto, o usa Margin auto para centrar");
        }
        if (!o.top().isAuto() && !s.height.isAuto() && !o.bottom().isAuto()
                && !s.margin.top().isAuto() && !s.margin.bottom().isAuto()) {
            throw error(line, ErrorCode.OVERCONSTRAINED, "Arriba, Height y abajo declarados fijan la "
                    + "posición dos veces: deja uno en auto, o usa Margin auto para centrar");
        }
    }

    /** Los márgenes {@code auto} solo centran: en flujo de bloque, en horizontal y con ancho limitado; en las absolute, con inicio, tamaño y fin. */
    private static void validateMargins(Style s, Style.Layout parentLayout, boolean flexItem, boolean gridItem) {
        Sides<Value> m = s.margin;
        boolean horizontalAuto = m.left().isAuto() || m.right().isAuto();
        boolean verticalAuto = m.top().isAuto() || m.bottom().isAuto();
        if (!horizontalAuto && !verticalAuto) {
            return;
        }
        int line = s.line("margin");
        if (flexItem || gridItem) {
            throw error(line, ErrorCode.MARGIN_AUTO_WITHOUT_EFFECT, "Margin auto no tiene efecto en ítems de flex o "
                    + "grid; alinea con Main axis, Cross axis, Horizontal o Vertical");
        }
        if (s.isOutOfFlow()) {
            Sides<Value> o = s.offset;
            if (horizontalAuto && (o.left().isAuto() || s.width.isAuto() || o.right().isAuto())) {
                throw error(line, ErrorCode.MARGIN_AUTO_WITHOUT_EFFECT, "Margin auto en horizontal solo centra una "
                        + "caja " + position(s) + " con izquierda, Width y derecha declarados");
            }
            if (verticalAuto && (o.top().isAuto() || s.height.isAuto() || o.bottom().isAuto())) {
                throw error(line, ErrorCode.MARGIN_AUTO_WITHOUT_EFFECT, "Margin auto en vertical solo centra una "
                        + "caja " + position(s) + " con arriba, Height y abajo declarados");
            }
            return;
        }
        if (parentLayout == Style.Layout.BLOCK) {
            if (verticalAuto) {
                throw error(line, ErrorCode.MARGIN_AUTO_WITHOUT_EFFECT,
                        "Margin auto en vertical no tiene efecto en flujo de bloque: usa un número");
            }
            if (s.width.isAuto() && s.maxWidth == null) {
                throw error(line, ErrorCode.MARGIN_AUTO_WITHOUT_EFFECT, "Margin auto no centra una caja que ocupa "
                        + "todo el ancho: declara Width o Max width");
            }
        }
    }

    /** Tamaño, mínimo y máximo comparables (los dos en puntos o los dos en porcentaje) que se contradicen. */
    private static void validateMinMax(Style s, String size, Value value, String min, Value minValue,
                                       String max, Value maxValue) {
        if (maxValue != null && comparable(minValue, maxValue) && amount(minValue) > amount(maxValue)) {
            throw error(s.line(min), ErrorCode.MIN_MAX_CONFLICT,
                    label(min) + " es mayor que " + label(max) + " (línea " + s.line(max) + ")");
        }
        if (comparable(value, minValue) && amount(value) < amount(minValue)) {
            throw error(s.line(size), ErrorCode.MIN_MAX_CONFLICT,
                    label(size) + " es menor que " + label(min) + " (línea " + s.line(min) + ")");
        }
        if (maxValue != null && comparable(value, maxValue) && amount(value) > amount(maxValue)) {
            throw error(s.line(size), ErrorCode.MIN_MAX_CONFLICT,
                    label(size) + " es mayor que " + label(max) + " (línea " + s.line(max) + ")");
        }
    }

    /**
     * Porcentajes verticales de la caja: su base debe ser un alto definido (§3.6). La base de
     * {@code Height}, {@code Min height} y {@code Max height} es el containing block (siempre
     * definido para las absolute y fixed) o el área (los ítems de grid); la de los desplazamientos
     * de una relative y de {@code Basis} en un flex {@code column}, la caja de contenido del padre.
     */
    private void validatePercentages(Style s, Box parent, boolean flexItem, boolean gridItem) {
        boolean ownBase = s.isOutOfFlow() || gridItem || definiteHeight.get(parent);
        for (String p : List.of("height", "min-height", "max-height")) {
            Value v = switch (p) {
                case "height" -> s.height;
                case "min-height" -> s.minHeight;
                default -> s.maxHeight;
            };
            if (!ownBase && v != null && hasPercentage(v)) {
                throw indefinite(s.line(p), label(p));
            }
        }
        boolean parentDefinite = definiteHeight.get(parent);
        if (s.position == Style.Position.RELATIVE && !parentDefinite
                && (hasPercentage(s.offset.top()) || hasPercentage(s.offset.bottom()))) {
            throw indefinite(s.line("offset"), "El desplazamiento vertical de Offset");
        }
        boolean column = parent.style.flexDirection == Style.Direction.COLUMN;
        if (flexItem && column && !parentDefinite) {
            if (hasPercentage(s.itemBasis)) {
                throw indefinite(s.line("item.basis"), "Basis en un flex column");
            }
            if (s.itemGrow > 0) {
                throw error(s.line("item.grow"), ErrorCode.GROW_WITHOUT_EFFECT, "Grow no tiene efecto: el flex "
                        + "column del padre no tiene un alto definido que repartir; declara su Height");
            }
        }
    }

    /** Porcentajes verticales del propio contenedor: filas y hueco entre filas. */
    private void validateContainer(Style s, Box box) {
        if (definiteHeight.get(box)) {
            return;
        }
        if (s.gridRows.stream().anyMatch(BoxTreeBuilder::hasPercentage)) {
            throw indefinite(s.line("grid.rows"), "Rows");
        }
        // En flex, el hueco entre filas solo cuenta en column o con varias líneas.
        boolean rowGapUsed = s.layout == Style.Layout.GRID
                || (s.layout == Style.Layout.FLEX && (s.flexWrap || s.flexDirection == Style.Direction.COLUMN));
        if (rowGapUsed && hasPercentage(s.rowGap)) {
            String gap = s.layout == Style.Layout.GRID ? "grid.gap" : "flex.gap";
            throw indefinite(s.line(gap), "El hueco entre filas de Gap");
        }
    }

    /**
     * Si el alto de la caja de contenido es definido, sin maquetar: un {@code Height} que se
     * resuelve (puntos, o porcentaje de una base definida), {@code Aspect ratio} con alto
     * {@code auto}, una absolute o fixed estirada entre arriba y abajo, o un ítem de grid
     * estirado ({@code Vertical: stretch}) en su área.
     */
    private boolean definiteHeight(Style s, Box parent) {
        boolean gridItem = !s.isOutOfFlow() && parent.style.layout == Style.Layout.GRID;
        boolean base = s.isOutOfFlow() || gridItem || definiteHeight.get(parent);
        Value h = s.height;
        if (!h.isAuto() && !h.isIntrinsic()) {
            return base || !hasPercentage(h);
        }
        if (h.isAuto() && s.aspectRatio != null) {
            return true;
        }
        if (h.isAuto() && s.isOutOfFlow() && !s.offset.top().isAuto() && !s.offset.bottom().isAuto()) {
            return true;
        }
        Style.Alignment vertical = s.itemVertical != null ? s.itemVertical : parent.style.gridVertical;
        return h.isAuto() && gridItem && vertical == Style.Alignment.STRETCH;
    }

    private static InvalidModelException indefinite(int line, String what) {
        return error(line, ErrorCode.INDEFINITE_PERCENTAGE, what + " usa un porcentaje de un alto que no es "
                + "definido: declara el Height del containing block, o usa puntos");
    }

    // ------------------------------------------------------------------------------------------
    // Acceso a los nodos
    // ------------------------------------------------------------------------------------------

    /** Compara con un nombre ya canónico ({@code "box-sizing"}). */
    private static boolean is(Node n, String canonicalName) {
        return n.getCanonicalName().equals(canonicalName);
    }

    /** Primer hijo con ese nombre canónico. */
    private static Optional<Node> child(InlineNode n, String canonicalName) {
        return n.getChildren().stream().filter(h -> is(h, canonicalName)).findFirst();
    }

    /** Los nodos con hijos no pueden ser bloques de texto ({@code >>}). */
    private static InlineNode inline(Node n) {
        if (n instanceof InlineNode i) {
            return i;
        }
        throw error(n, ErrorCode.DOCUMENT_STRUCTURE, n.getName() + " no puede ser un bloque de texto (>>)");
    }

    /** Hijo obligatorio ({@code "Type"}). */
    private static Node required(InlineNode n, String child) {
        return child(n, child.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> error(n, ErrorCode.DOCUMENT_STRUCTURE, n.getName() + " necesita " + child));
    }

    // ------------------------------------------------------------------------------------------
    // Valores
    // ------------------------------------------------------------------------------------------

    private String id(Node p) {
        String id = p.getText().trim();
        Integer previous = ids.putIfAbsent(id, p.getLine());
        if (previous != null) {
            throw error(p, ErrorCode.DUPLICATE_ID, "Id repetido: " + id + " (ya se usa en la línea " + previous + ")");
        }
        return id;
    }

    /** Un valor de una clase admitida; sin {@code negative}, sin números negativos. */
    private static Value value(Node p, Set<Kind> allowed, boolean negative) {
        Value v = parse(p, () -> ValueParser.value(p.getText()));
        check(p, v, allowed, negative);
        return v;
    }

    private static Sides<Value> sides(Node p, Set<Kind> allowed, boolean negative) {
        List<Value> list = parse(p, () -> ValueParser.list(p.getText()));
        list.forEach(v -> check(p, v, allowed, negative));
        return parse(p, () -> Sides.shorthand(list));
    }

    private static void check(Node p, Value v, Set<Kind> allowed, boolean negative) {
        Kind kind = kind(v);
        if (kind == null || !allowed.contains(kind)) {
            throw error(p, ErrorCode.VALUE_NOT_ALLOWED, p.getName() + " no admite " + p.getText().trim()
                    + ": usa " + describe(allowed));
        }
        if (!negative && isNegative(v)) {
            throw error(p, ErrorCode.VALUE_NOT_ALLOWED, p.getName() + " no admite valores negativos: " + p.getText().trim());
        }
    }

    /** Pistas de grid: longitudes, {@code auto}, {@code min-content}, {@code max-content}, fracciones y {@code minmax}. */
    private static List<Value> tracks(Node p) {
        List<Value> list = parse(p, () -> ValueParser.list(p.getText()));
        for (Value v : list) {
            if (v == Value.Keyword.FIT_CONTENT) {
                throw error(p, ErrorCode.FIT_CONTENT_TRACK, "fit-content no es una pista de grid: usa auto, "
                        + "que ya se ajusta al contenido");
            }
            boolean valid = v instanceof Value.MinMax mm
                    ? track(mm.min()) && (track(mm.max()) || kind(mm.max()) == Kind.FRACTION)
                    : track(v) || kind(v) == Kind.FRACTION;
            if (!valid) {
                throw error(p, ErrorCode.VALUE_NOT_ALLOWED, p.getName() + " no admite " + v + " como pista: usa "
                        + "números, porcentajes, fracciones (1fr), auto, min-content, max-content o minmax(a, b)");
            }
            if (isNegative(v)) {
                throw error(p, ErrorCode.VALUE_NOT_ALLOWED, p.getName() + " no admite pistas negativas: " + p.getText().trim());
            }
        }
        return list;
    }

    private static boolean track(Value v) {
        Kind k = kind(v);
        return k == Kind.LENGTH || k == Kind.AUTO || (k == Kind.INTRINSIC && v != Value.Keyword.FIT_CONTENT);
    }

    /** Clase de un valor; {@code null} si es una función con argumentos que no son longitudes. */
    private static Kind kind(Value v) {
        if (v instanceof Value.Points || v instanceof Value.Percentage) {
            return Kind.LENGTH;
        }
        if (v instanceof Value.Function f) {
            return f.arguments().stream().allMatch(a -> kind(a) == Kind.LENGTH) ? Kind.LENGTH : null;
        }
        if (v instanceof Value.Fraction) {
            return Kind.FRACTION;
        }
        if (v instanceof Value.MinMax) {
            return Kind.MINMAX;
        }
        return v.isAuto() ? Kind.AUTO : Kind.INTRINSIC;
    }

    private static String describe(Set<Kind> allowed) {
        return allowed.stream().map(k -> switch (k) {
            case LENGTH -> "un número, un porcentaje o min()/max()/clamp()";
            case AUTO -> "auto";
            case INTRINSIC -> "min-content, max-content o fit-content";
            case FRACTION -> "una fracción";
            case MINMAX -> "minmax(a, b)";
        }).collect(Collectors.joining("; "));
    }

    private static boolean isNegative(Value v) {
        if (v instanceof Value.Points p) {
            return p.pt() < 0;
        }
        if (v instanceof Value.Percentage p) {
            return p.percent() < 0;
        }
        return parts(v).stream().anyMatch(BoxTreeBuilder::isNegative);
    }

    /** El valor depende de una base de porcentajes (directamente o dentro de una función). */
    private static boolean hasPercentage(Value v) {
        return v instanceof Value.Percentage || parts(v).stream().anyMatch(BoxTreeBuilder::hasPercentage);
    }

    /** Argumentos de una función o de {@code minmax}. */
    private static List<Value> parts(Value v) {
        if (v instanceof Value.MinMax m) {
            return List.of(m.min(), m.max());
        }
        return v instanceof Value.Function f ? f.arguments() : List.of();
    }

    private static boolean comparable(Value a, Value b) {
        return (a instanceof Value.Points && b instanceof Value.Points)
                || (a instanceof Value.Percentage && b instanceof Value.Percentage);
    }

    private static double amount(Value v) {
        return v instanceof Value.Points p ? p.pt() : ((Value.Percentage) v).percent();
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

    private static <E extends Enum<E>> E enumValue(Class<E> type, Node n) {
        String text = n.getText().trim();
        try {
            return Enum.valueOf(type, text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Valor no permitido en " + n.getName() + ": " + text);
        }
    }

    private static boolean breakInside(Node n) {
        Style.Break b = enumValue(Style.Break.class, n);
        if (b == Style.Break.PAGE) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Break inside solo admite auto o avoid");
        }
        return b == Style.Break.AVOID;
    }

    private static boolean bool(Node n) {
        return switch (n.getText().trim().toLowerCase(Locale.ROOT)) {
            case "true", "yes" -> true;
            case "false", "no" -> false;
            default -> throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Se esperaba true o false: " + n.getText().trim());
        };
    }

    private static int integer(Node n) {
        try {
            return Integer.parseInt(n.getText().trim());
        } catch (NumberFormatException ex) {
            throw error(n, ErrorCode.VALUE_SYNTAX, "Se esperaba un entero: " + n.getText().trim());
        }
    }

    private static int positiveInteger(Node n) {
        int v = integer(n);
        if (v < 1) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, n.getName() + " debe ser al menos 1");
        }
        return v;
    }

    private static double points(Node n) {
        Value v = parse(n, () -> ValueParser.value(n.getText()));
        if (!(v instanceof Value.Points p) || p.pt() <= 0) {
            throw error(n, ErrorCode.MEDIA_SIZE, n.getName() + " del medio debe ser un número de puntos positivo: "
                    + n.getText().trim());
        }
        return p.pt();
    }

    private static double positive(Node n) {
        double v = nonNegative(n);
        if (v == 0) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, n.getName() + " debe ser mayor que 0");
        }
        return v;
    }

    private static double nonNegative(Node n) {
        Value v = parse(n, () -> ValueParser.value(n.getText()));
        if (!(v instanceof Value.Points p) || p.pt() < 0) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, n.getName() + " debe ser un número no negativo: " + n.getText().trim());
        }
        return p.pt();
    }

    private static Sides<Value> nonNegativeSides(Node n, boolean allowsPercentage) {
        Sides<Value> sides = parse(n, () -> Sides.shorthand(ValueParser.list(n.getText())));
        for (Value v : sides.asList()) {
            boolean valid = (v instanceof Value.Points p && p.pt() >= 0)
                    || (allowsPercentage && v instanceof Value.Percentage q && q.percent() >= 0);
            if (!valid) {
                throw error(n, ErrorCode.VALUE_NOT_ALLOWED, n.getName() + (allowsPercentage
                        ? " solo admite puntos o porcentajes no negativos"
                        : " solo admite puntos no negativos"));
            }
        }
        return sides;
    }

    private static Double aspectRatio(Node n) {
        String t = n.getText().trim();
        try {
            if (t.contains("/")) {
                String[] p = t.split("/");
                double r = Double.parseDouble(p[0].trim()) / Double.parseDouble(p[1].trim());
                if (r > 0 && Double.isFinite(r)) {
                    return r;
                }
            } else {
                double r = Double.parseDouble(t);
                if (r > 0) {
                    return r;
                }
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            // se informa abajo
        }
        throw error(n, ErrorCode.VALUE_SYNTAX, "Aspect ratio espera 'ancho/alto' o un número positivo: " + t);
    }

    private static String color(Node n) {
        String t = n.getText().trim();
        if (!COLOR.matcher(t).matches()) {
            throw error(n, ErrorCode.VALUE_SYNTAX, "Color no válido (usa #RGB o #RRGGBB): " + t);
        }
        return t;
    }

    private static String fontFamily(Node n) {
        String family = n.getText().trim();
        if (family.isEmpty()) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Font necesita el nombre de una familia");
        }
        return family;
    }

    /** Nombre de una propiedad para los mensajes: {@code min-width} → {@code Min width}. */
    private static String label(String property) {
        String s = property.replace('-', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String position(Style s) {
        return s.position.name().toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------------------------------
    // Errores
    // ------------------------------------------------------------------------------------------

    /** Interpreta un valor; los errores de sintaxis llevan la línea del nodo. */
    private static <T> T parse(Node n, java.util.function.Supplier<T> parser) {
        try {
            return parser.get();
        } catch (IllegalArgumentException ex) {
            throw error(n, ErrorCode.VALUE_SYNTAX, ex.getMessage());
        }
    }

    /** {@code Areas} y {@code Cell}: sus errores son de colocación. */
    private static void placement(Node n, Runnable check) {
        placement(n.getLine(), check);
    }

    private static void placement(int line, Runnable check) {
        try {
            check.run();
        } catch (IllegalArgumentException ex) {
            throw error(line, ErrorCode.GRID_PLACEMENT, ex.getMessage());
        }
    }

    private static InvalidModelException error(Node n, ErrorCode code, String message) {
        return error(n.getLine(), code, message);
    }

    private static InvalidModelException error(int line, ErrorCode code, String message) {
        return new InvalidModelException(line, code, message);
    }
}
