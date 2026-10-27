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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Construye el árbol de cajas a partir del documento (ESPECIFICACION.md §5.1): traduce cada
 * elemento a una caja con su {@link Style.Layout} y su {@link Style.Position}, pasa las
 * propiedades de {@code Item} y {@code Cell} al elemento que envuelven, interpreta los valores,
 * aplica la herencia y las reglas de {@code Styles}, y comprueba las reglas que el schema no
 * puede expresar (§6).
 *
 * <p>Prioridad de cada propiedad, de menos a más: el valor por defecto, lo heredado, la regla
 * {@code Element} del tipo, las reglas {@code For class} de sus clases, la regla {@code For id}
 * y lo que declara el propio elemento. Lo que aporta una regla se valida como si lo declarara
 * el elemento, y sus errores señalan la línea de la regla.</p>
 *
 * <p>Los errores señalan la línea del nodo culpable: la de la propiedad, no la de su caja.
 * Son errores también las combinaciones que no tendrían efecto o se contradicen: el modelo las
 * rechaza en lugar de resolverlas con una regla por defecto.</p>
 *
 * <p>Trabaja sobre los nodos de {@code stxt-core}. Los nombres se comparan por su nombre
 * canónico ({@link Node#getCanonicalName()}: minúsculas y los espacios convertidos en guiones,
 * de modo que {@code Min width} es {@code min-width}). Antes de construir valida el documento
 * contra el schema ({@link DocumentSchema}), aunque ya venga validado de {@code DocumentReader}:
 * así la estructura (qué hijos admite cada nodo) solo está definida en el schema, y el resto
 * del código puede darla por buena. No es reentrante: cada llamada a {@link #build} usa su propio
 * estado.</p>
 */
public final class BoxTreeBuilder {

    private static final Pattern COLOR = Pattern.compile("#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})");

    /** Clases de valor, para comprobar lo que admite cada propiedad (§3.1 y §4). */
    private enum Kind { LENGTH, AUTO, INTRINSIC, FRACTION, MINMAX }

    private static final Set<Kind> SIZE = EnumSet.of(Kind.LENGTH, Kind.AUTO, Kind.INTRINSIC);
    private static final Set<Kind> LENGTH_OR_AUTO = EnumSet.of(Kind.LENGTH, Kind.AUTO);
    private static final Set<Kind> LENGTH = EnumSet.of(Kind.LENGTH);

    // --- Clases de nodos (la estructura completa está en el schema, §9) --------------------------
    private static final Set<String> FLEX = Set.of("direction", "wrap", "gap", "main-axis", "cross-axis", "lines");
    private static final Set<String> GRID = Set.of("columns", "rows", "gap", "areas", "flow", "horizontal", "vertical");
    /** Elementos de caja. */
    private static final Set<String> ELEMENTS = Set.of("block", "flex", "grid", "text", "relative", "absolute", "fixed");
    /** Los elementos de caja como se escriben, para los mensajes. */
    private static final List<String> ELEMENT_NAMES =
            List.of("Block", "Flex", "Grid", "Text", "Relative", "Absolute", "Fixed");
    /** Elementos en flujo: los que pueden ser un ítem. */
    private static final Set<String> IN_FLOW = Set.of("block", "flex", "grid", "text", "relative");

    /**
     * Nombre interno de cada propiedad de {@code Typography} y {@code Break} cuyo nombre en el
     * documento es corto: {@link Style#line} y los mensajes usan el interno ({@code font-size}).
     */
    private static final Map<String, String> INTERNAL = Map.of(
            "weight", "font-weight", "style", "font-style", "size", "font-size", "align", "text-align",
            "before", "break-before", "after", "break-after", "inside", "break-inside");

    /** Resultado: el medio y una caja raíz anónima que representa el área útil. */
    public record Result(Media media, Box root) {
    }

    /** Línea del {@code Id} de cada caja, para detectar los repetidos. */
    private Map<String, Integer> ids;
    /** Reglas {@code Element}, por el nombre canónico del elemento ({@code heading-1}). */
    private Map<String, InlineNode> elementRules;
    /** Reglas {@code For class}, por clase y en el orden del documento: en ese orden se aplican. */
    private Map<String, InlineNode> classRules;
    /** Reglas {@code For id}, por {@code Id}. */
    private Map<String, InlineNode> idRules;
    /** Si el alto de la caja de contenido es definido: base de los porcentajes verticales (§3.6). */
    private Map<Box, Boolean> definiteHeight;

    /**
     * Construye el árbol de un documento, validado o no.
     *
     * @throws dev.stxt.exceptions.ValidationException si no cumple el schema (§9)
     * @throws InvalidModelException si incumple una regla del modelo (§6)
     */
    public Result build(InlineNode document) {
        ids = new HashMap<>();
        definiteHeight = new HashMap<>();
        // Sin el namespace, el schema no validaría nada.
        if (!is(document, "document") || !DocumentSchema.NAMESPACE.equals(document.getNamespace())) {
            throw error(document, ErrorCode.DOCUMENT_ROOT,
                    "El nodo raíz debe ser Document (" + DocumentSchema.NAMESPACE + "):");
        }
        DocumentSchema.validate(document);
        Media media = media(inline(child(document, "media").orElseThrow()));
        styles(document);

        // El Typography de Document lo heredan todas las cajas.
        Style rootStyle = new Style();
        child(document, "typography").map(BoxTreeBuilder::inline).ifPresent(t -> typography(t, rootStyle));
        Box root = new Box(null, rootStyle, true, document.getLine());
        // El área útil tiene el alto de la página, menos los márgenes: siempre definido.
        definiteHeight.put(root, true);
        buildContent(document, root);
        for (Map.Entry<String, InlineNode> rule : idRules.entrySet()) {
            if (!ids.containsKey(rule.getKey())) {
                throw error(rule.getValue(), ErrorCode.STYLE_UNKNOWN_ID,
                        "Ningún elemento tiene el Id " + rule.getKey() + " de esta regla For id");
            }
        }
        return new Result(media, root);
    }

    // ------------------------------------------------------------------------------------------
    // Reglas reutilizables (Styles)
    // ------------------------------------------------------------------------------------------

    /**
     * Lee las reglas de {@code Styles}. Sus valores se comprueban aquí, aunque la regla no se
     * aplique a ningún elemento; lo que depende del elemento (§6), al aplicarla.
     */
    private void styles(InlineNode document) {
        elementRules = new HashMap<>();
        classRules = new LinkedHashMap<>();
        idRules = new HashMap<>();
        Optional<Node> styles = child(document, "styles");
        if (styles.isEmpty()) {
            return;
        }
        for (Node r : inline(styles.get()).getChildren()) {
            InlineNode rule = inline(r);
            String selector = rule.getText().trim();
            Map<String, InlineNode> rules;
            String key;
            switch (rule.getCanonicalName()) {
                case "element" -> {
                    rules = elementRules;
                    key = selector.toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
                    if (!ELEMENTS.contains(key)) {
                        throw error(rule, ErrorCode.STYLE_UNKNOWN_ELEMENT, "Element: '" + selector
                                + "' no es un elemento de caja (" + String.join(", ", ELEMENT_NAMES) + ")");
                    }
                }
                case "for-class" -> {
                    rules = classRules;
                    key = name(rule, "For class");
                }
                default -> {
                    rules = idRules;
                    key = name(rule, "For id");
                }
            }
            InlineNode previous = rules.putIfAbsent(key, rule);
            if (previous != null) {
                throw error(rule, ErrorCode.STYLE_DUPLICATE, "Regla repetida: " + rule.getName() + ": " + selector
                        + " (ya está en la línea " + previous.getLine() + ")");
            }
            apply(rule, new Style());
        }
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
     * Aplica las reglas que corresponden a un elemento, de menos a más prioridad: la de su tipo,
     * las de sus clases (en el orden de {@code Styles}) y la de su {@code Id}. Van después de la
     * herencia y antes de lo que declara el propio elemento, y se mezclan propiedad a propiedad.
     */
    private void applyRules(InlineNode n, String kind, Style s) {
        List<InlineNode> rules = new ArrayList<>();
        Optional.ofNullable(elementRules.get(kind)).ifPresent(rules::add);
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
        for (InlineNode rule : rules) {
            if (s.isOutOfFlow()) {
                child(rule, "break").ifPresent(b -> {
                    throw error(b, ErrorCode.STYLE_NOT_ALLOWED, "La regla " + rule.getName() + ": "
                            + rule.getText().trim() + " aplica Break a un " + position(s) + " (línea " + n.getLine()
                            + "), que no se parte ni está en el flujo de la página");
                });
            }
            apply(rule, s);
        }
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
    private static void apply(InlineNode rule, Style s) {
        for (Node g : rule.getChildren()) {
            switch (g.getCanonicalName()) {
                case "box" -> box(inline(g), s);
                case "typography" -> typography(inline(g), s);
                default -> fragmentation(inline(g), s);
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Media
    // ------------------------------------------------------------------------------------------

    private Media media(InlineNode n) {
        double width = points(required(n, "Width"));
        double height = points(required(n, "Height"));
        Optional<Node> marginNode = child(n, "margin");
        Insets margin = marginNode.map(BoxTreeBuilder::mediaMargin).orElse(Insets.ZERO);
        Media media = new Media(width, height, margin);
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
    // Elementos
    // ------------------------------------------------------------------------------------------

    /** Elementos hijos en orden: cada uno es una caja; {@code Item} y {@code Cell} envuelven a la suya. */
    private void buildContent(InlineNode node, Box box) {
        for (Node h : node.getChildren()) {
            String name = h.getCanonicalName();
            if (ELEMENTS.contains(name)) {
                box.addChild(element(inline(h), box, null));
            } else if (name.equals("item") || name.equals("cell")) {
                InlineNode wrapper = inline(h);
                List<Node> elements = wrapper.getChildren().stream()
                        .filter(e -> IN_FLOW.contains(e.getCanonicalName()))
                        .toList();
                if (elements.size() != 1) {
                    throw error(elements.isEmpty() ? wrapper : elements.get(1), ErrorCode.SINGLE_ELEMENT,
                            "Un " + h.getName() + " contiene exactamente un elemento, y este tiene " + elements.size());
                }
                box.addChild(element(inline(elements.get(0)), box, wrapper));
            }
        }
    }

    /** Un elemento de caja; {@code wrapper} es su {@code Item} o su {@code Cell}, si es un ítem. */
    private Box element(InlineNode n, Box parent, InlineNode wrapper) {
        String kind = n.getCanonicalName();
        Style s = Style.inheriting(parent.style);
        s.layout = switch (kind) {
            case "flex" -> Style.Layout.FLEX;
            case "grid" -> Style.Layout.GRID;
            case "text" -> Style.Layout.INLINE;
            default -> Style.Layout.BLOCK;
        };
        s.position = switch (kind) {
            case "relative" -> Style.Position.RELATIVE;
            case "absolute" -> Style.Position.ABSOLUTE;
            case "fixed" -> Style.Position.FIXED;
            default -> Style.Position.STATIC;
        };
        if (wrapper != null) {
            item(wrapper, s);
        }
        applyRules(n, kind, s);
        String id = null;
        for (Node p : n.getChildren()) {
            String name = p.getCanonicalName();
            // Las clases ya se han aplicado con sus reglas.
            if (ELEMENTS.contains(name) || name.equals("item") || name.equals("cell") || name.equals("content")
                    || name.equals("class")) {
                continue;
            }
            if (kind.equals("flex") && FLEX.contains(name)) {
                flex(p, s);
                continue;
            }
            if (kind.equals("grid") && GRID.contains(name)) {
                grid(p, s);
                continue;
            }
            switch (name) {
                case "box" -> box(inline(p), s);
                case "typography" -> typography(inline(p), s);
                case "break" -> fragmentation(inline(p), s);
                default -> {
                    s.declared(name, p.getLine());
                    switch (name) {
                        case "id" -> id = id(p);
                        case "offset" -> s.offset = sides(p, LENGTH_OR_AUTO, true);
                        case "layer" -> s.layer = integer(p);
                        default -> s.isolate = bool(p);
                    }
                }
            }
        }
        if (kind.equals("flex")) {
            validateFlex(s);
        }
        validateBox(s, parent, wrapper);

        Box box = new Box(id, s, false, n.getLine());
        definiteHeight.put(box, definiteHeight(s, parent));
        validateContainer(s, box);
        if (kind.equals("text")) {
            text(n, box);
        } else {
            buildContent(n, box);
        }
        return box;
    }

    /** El texto de un {@code Text}: en su valor o en {@code Content}, uno de los dos. */
    private static void text(InlineNode n, Box box) {
        Optional<Node> content = child(n, "content");
        boolean value = !n.getText().isBlank();
        if (value && content.isPresent()) {
            throw error(content.get(), ErrorCode.TEXT_CONTENT, "Text tiene el texto en el valor y en Content "
                    + "(línea " + n.getLine() + "): deja solo uno");
        }
        if (!value && content.isEmpty()) {
            throw error(n, ErrorCode.TEXT_CONTENT,
                    "Text necesita su texto: en el valor (Text: …) o en un bloque Content >>");
        }
        Node source = content.orElse(n);
        if (box.style.font == null) {
            throw error(source, ErrorCode.TEXT_WITHOUT_FONT,
                    "El texto necesita Font: declárala en el Typography del Text, de un antecesor o de Document");
        }
        if (!box.style.isDeclared("font-size")) {
            throw error(source, ErrorCode.TEXT_WITHOUT_FONT_SIZE,
                    "El texto necesita Size: decláralo en el Typography del Text, de un antecesor o de Document");
        }
        box.text = source.getText();
        box.textLine = source.getLine();
        box.textBlock = source instanceof TextNode;
    }

    /** Grupo {@code Box}: el modelo de caja, que no se hereda. */
    private static void box(InlineNode group, Style s) {
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
                case "border-color" -> s.borderColor = color(p);
                default -> s.background = color(p);
            }
        }
    }

    /**
     * Grupo {@code Typography}, en un elemento o en {@code Document}: se hereda. Las líneas se
     * guardan con el nombre interno ({@code Size} es {@code font-size}).
     */
    private static void typography(InlineNode group, Style s) {
        for (Node p : group.getChildren()) {
            String name = p.getCanonicalName();
            s.declared(INTERNAL.getOrDefault(name, name), p.getLine());
            switch (name) {
                case "font" -> s.font = fontFamily(p);
                case "weight" -> s.fontWeight = enumValue(Style.FontWeight.class, p);
                case "style" -> s.fontStyle = enumValue(Style.FontStyle.class, p);
                case "size" -> s.fontSize = positive(p);
                case "line-height" -> s.lineHeight = value(p, LENGTH, false);
                case "align" -> s.textAlign = enumValue(Style.TextAlign.class, p);
                case "color" -> s.color = color(p);
                case "text-wrap" -> s.textWrap = enumValue(Style.TextWrap.class, p);
                case "widows" -> s.widows = positiveInteger(p);
                default -> s.orphans = positiveInteger(p);
            }
        }
    }

    /** Grupo {@code Break}: los cortes de página, que no se heredan. */
    private static void fragmentation(InlineNode group, Style s) {
        for (Node p : group.getChildren()) {
            String name = p.getCanonicalName();
            s.declared(INTERNAL.getOrDefault(name, name), p.getLine());
            switch (name) {
                case "before" -> s.breakBefore = enumValue(Style.Break.class, p);
                case "after" -> s.breakAfter = enumValue(Style.Break.class, p);
                case "inside" -> s.avoidBreakInside = breakInside(p);
                default -> s.decoration = enumValue(Style.Decoration.class, p);
            }
        }
    }

    /** Propiedades de flex, hijas del elemento {@code Flex}; su línea se guarda con prefijo ({@code flex.gap}). */
    private static void flex(Node p, Style s) {
        s.declared("flex." + p.getCanonicalName(), p.getLine());
        switch (p.getCanonicalName()) {
            case "direction" -> s.flexDirection = enumValue(Style.Direction.class, p);
            case "wrap" -> s.flexWrap = bool(p);
            case "gap" -> gap(p, s);
            case "main-axis" -> s.flexMainAxis = enumValue(Style.Distribution.class, p);
            case "cross-axis" -> s.flexCrossAxis = enumValue(Style.Alignment.class, p);
            default -> s.flexLines = enumValue(Style.Distribution.class, p);
        }
    }

    private static void validateFlex(Style s) {
        if (s.flexMainAxis == Style.Distribution.STRETCH) {
            throw error(s.line("flex.main-axis"), ErrorCode.VALUE_NOT_ALLOWED,
                    "Main axis no admite 'stretch'; usa Grow en los ítems");
        }
        if (s.isDeclared("flex.lines") && !s.flexWrap) {
            throw error(s.line("flex.lines"), ErrorCode.LINES_WITHOUT_WRAP,
                    "Lines solo tiene efecto con Wrap: true, porque sin Wrap hay una sola línea");
        }
    }

    /** Propiedades de grid, hijas del elemento {@code Grid}; su línea se guarda con prefijo ({@code grid.rows}). */
    private static void grid(Node p, Style s) {
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
            default -> s.gridVertical = enumValue(Style.Alignment.class, p);
        }
    }

    /**
     * Propiedades de {@code Item} o {@code Cell}, que pasan al elemento que envuelven; su línea se
     * guarda con prefijo ({@code item.grow}), salvo {@code Layer}. El valor de {@code Cell} es la
     * colocación ({@code item.cell}, con la línea del {@code Cell}).
     */
    private static void item(InlineNode w, Style s) {
        if (w.getCanonicalName().equals("cell") && !w.getText().isBlank()) {
            s.declared("item.cell", w.getLine());
            s.itemCell = w.getText().trim();
        }
        for (Node p : w.getChildren()) {
            String name = p.getCanonicalName();
            if (IN_FLOW.contains(name)) {
                continue;
            }
            if (name.equals("layer")) {
                s.declared("layer", p.getLine());
                s.layer = integer(p);
                continue;
            }
            s.declared("item." + name, p.getLine());
            switch (name) {
                case "grow" -> s.itemGrow = nonNegative(p);
                case "shrink" -> s.itemShrink = nonNegative(p);
                case "basis" -> s.itemBasis = value(p, LENGTH_OR_AUTO, false);
                case "cross-axis" -> s.itemCrossAxis = enumValue(Style.Alignment.class, p);
                case "horizontal" -> s.itemHorizontal = enumValue(Style.Alignment.class, p);
                default -> s.itemVertical = enumValue(Style.Alignment.class, p);
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Reglas que el schema no puede expresar (§6)
    // ------------------------------------------------------------------------------------------

    private void validateBox(Style s, Box parent, InlineNode wrapper) {
        Style.Layout parentLayout = parent.style.layout;
        boolean flexItem = wrapper != null && parentLayout == Style.Layout.FLEX;
        boolean gridItem = wrapper != null && parentLayout == Style.Layout.GRID;

        if (gridItem && s.itemCell != null) {
            Map<String, Area> areas = parent.style.gridAreas == null
                    ? Map.of() : GridAreas.parse(parent.style.gridAreas);
            placement(s.line("item.cell"), () -> Cell.parse(s.itemCell, areas));
        }
        validateOffset(s);
        validateMargins(s, parentLayout, flexItem, gridItem);
        if (s.isDeclared("aspect-ratio") && !s.width.isAuto() && !s.height.isAuto()) {
            throw error(s.line("aspect-ratio"), ErrorCode.ASPECT_RATIO_CONFLICT,
                    "Aspect ratio no tiene efecto si Width y Height están declarados");
        }
        validateMinMax(s, "width", s.width, "min-width", s.minWidth, "max-width", s.maxWidth);
        validateMinMax(s, "height", s.height, "min-height", s.minHeight, "max-height", s.maxHeight);
        validatePercentages(s, parent, flexItem, gridItem);
    }

    private static void validateOffset(Style s) {
        if (!s.isDeclared("offset")) {
            return;
        }
        int line = s.line("offset");
        Sides<Value> o = s.offset;
        if (s.position == Style.Position.RELATIVE) {
            if (!o.top().isAuto() && !o.bottom().isAuto()) {
                throw error(line, ErrorCode.OFFSET_CONFLICT, "Un Relative no puede desplazarse a la vez "
                        + "desde arriba y desde abajo: deja uno de los dos en auto");
            }
            if (!o.left().isAuto() && !o.right().isAuto()) {
                throw error(line, ErrorCode.OFFSET_CONFLICT, "Un Relative no puede desplazarse a la vez "
                        + "desde la izquierda y desde la derecha: deja uno de los dos en auto");
            }
            return;
        }
        // Absolute y Fixed: inicio + tamaño + fin no puede fijar la posición dos veces.
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

    /** Los márgenes {@code auto} solo centran: en flujo de bloque, en horizontal y con ancho limitado; en Absolute y Fixed, con inicio, tamaño y fin. */
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
                throw error(line, ErrorCode.MARGIN_AUTO_WITHOUT_EFFECT, "Margin auto en horizontal solo centra un "
                        + position(s) + " con izquierda, Width y derecha declarados");
            }
            if (verticalAuto && (o.top().isAuto() || s.height.isAuto() || o.bottom().isAuto())) {
                throw error(line, ErrorCode.MARGIN_AUTO_WITHOUT_EFFECT, "Margin auto en vertical solo centra un "
                        + position(s) + " con arriba, Height y abajo declarados");
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
     * definido para Absolute y Fixed) o el área (los ítems de grid); la de los desplazamientos
     * de un Relative y de {@code Basis} en un flex {@code column}, la caja de contenido del padre.
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
     * {@code auto}, un Absolute o un Fixed estirado entre arriba y abajo, o un ítem de grid
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

    /** Compara con un nombre ya canónico ({@code "min-width"}). */
    private static boolean is(Node n, String canonicalName) {
        return n.getCanonicalName().equals(canonicalName);
    }

    /** Primer hijo con ese nombre canónico. */
    private static Optional<Node> child(InlineNode n, String canonicalName) {
        return n.getChildren().stream().filter(h -> is(h, canonicalName)).findFirst();
    }

    /** Un nodo con hijos: el schema no admite que se escriba como bloque de texto ({@code >>}). */
    private static InlineNode inline(Node n) {
        return (InlineNode) n;
    }

    /** Hijo obligatorio ({@code "Type"}), que el schema garantiza. */
    private static Node required(InlineNode n, String child) {
        return child(n, child.toLowerCase(Locale.ROOT)).orElseThrow();
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
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Inside solo admite auto o avoid");
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

    /** Nombre del elemento posicionado: {@code Absolute} o {@code Fixed}. */
    private static String position(Style s) {
        return s.position == Style.Position.FIXED ? "Fixed" : "Absolute";
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
