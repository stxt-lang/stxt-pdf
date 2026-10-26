package dev.stxt.boxes.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.boxes.layout.grid.Area;
import dev.stxt.boxes.layout.grid.Cell;
import dev.stxt.boxes.layout.grid.GridAreas;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Sides;
import dev.stxt.boxes.model.Value;
import dev.stxt.boxes.model.ValueParser;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.regex.Pattern;

/**
 * Construye el árbol de cajas a partir del documento (módulos 0 y 1, ESPECIFICACION.md §5.1):
 * interpreta los valores, aplica la herencia, crea las cajas anónimas y comprueba las reglas
 * que el template no puede expresar (§6).
 *
 * <p>Trabaja sobre los nodos de {@code stxt-core}. Los nombres se comparan por su nombre
 * canónico ({@link Node#getCanonicalName()}: minúsculas y los espacios convertidos en guiones,
 * de modo que {@code Box sizing} es {@code box-sizing}).</p>
 */
public final class BoxTreeBuilder {

    private static final Pattern COLOR = Pattern.compile("#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})");

    /** Resultado: el medio y una caja raíz anónima que representa el área útil. */
    public record Result(Media media, Box root) {
    }

    public Result build(InlineNode document) {
        if (!is(document, "document")) {
            throw error(document, "La raíz del documento debe ser Document");
        }
        InlineNode mediaNode = child(document, "media")
                .map(BoxTreeBuilder::inline)
                .orElseThrow(() -> error(document, "Falta el nodo Media"));
        Media media = media(mediaNode);

        Box root = new Box(null, new Style(), true, document.getLine());
        buildContent(document, root);
        if (root.children.isEmpty()) {
            throw error(document, "El documento necesita al menos una Box");
        }
        return new Result(media, root);
    }

    // ------------------------------------------------------------------------------------------
    // Media
    // ------------------------------------------------------------------------------------------

    private Media media(InlineNode n) {
        Media.Type type = enumValue(Media.Type.class, required(n, "Type"), n);
        double width = points(required(n, "Width"), n, "Width");
        OptionalDouble height = contentOf(n, "height")
                .map(v -> OptionalDouble.of(points(v, n, "Height")))
                .orElse(OptionalDouble.empty());
        Insets margin = contentOf(n, "margin")
                .map(v -> mediaMargin(v, n))
                .orElse(Insets.ZERO);
        if (type == Media.Type.PAGED && height.isEmpty()) {
            throw error(n, "Con Type: paged, Media necesita Height");
        }
        Media media = new Media(type, width, height, margin);
        if (media.pageArea().width() <= 0 || media.pageArea().height() <= 0) {
            throw error(n, "Los márgenes del medio no dejan área útil");
        }
        return media;
    }

    private Insets mediaMargin(String text, Node n) {
        List<Double> v = ValueParser.list(text).stream().map(x -> {
            if (!(x instanceof Value.Points p)) {
                throw error(n, "El margen del medio solo admite puntos: " + text);
            }
            return p.pt();
        }).toList();
        Sides<Double> s = Sides.shorthand(v);
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
        boolean hasBoxes = content.stream().anyMatch(h -> is(h, "box"));
        Style.Layout layout = box.style.layout;

        if (layout == Style.Layout.INLINE && hasBoxes) {
            throw error(node, "Una caja con Layout: inline solo puede contener Text");
        }
        boolean textInAnonymous = hasBoxes || layout == Style.Layout.FLEX || layout == Style.Layout.GRID;

        for (Node h : content) {
            if (is(h, "box")) {
                box.addChild(box(inline(h), box));
            } else if (textInAnonymous) {
                // Texto mezclado con cajas, o dentro de flex/grid: caja anónima (módulo 1, §6).
                Style s = Style.inheriting(box.style);
                s.layout = Style.Layout.INLINE;
                Box anonymous = new Box(null, s, true, h.getLine());
                anonymous.text = h.getText();
                box.addChild(anonymous);
            } else {
                box.text = h.getText();
                if (layout == Style.Layout.BLOCK) {
                    // Un bloque que solo contiene texto se maqueta con el algoritmo de línea.
                    box.style.layout = Style.Layout.INLINE;
                }
            }
        }
    }

    private Box box(InlineNode n, Box parent) {
        Style s = Style.inheriting(parent.style);
        String id = null;
        for (Node p : n.getChildren()) {
            String v = p.getText();
            try {
                switch (p.getCanonicalName()) {
                    case "box", "text" -> {
                        // se procesan en buildContent
                    }
                    case "id" -> id = v.trim();
                    case "layout" -> s.layout = enumValue(Style.Layout.class, v, p);
                    case "position" -> s.position = enumValue(Style.Position.class, v, p);
                    case "offset" -> s.offset = Sides.shorthand(ValueParser.list(v));
                    case "layer" -> s.layer = integer(v, p);
                    case "isolate" -> s.isolate = bool(v, p);
                    case "box-sizing" -> s.boxSizing = enumValue(Style.BoxSizing.class, v, p);
                    case "width" -> s.width = ValueParser.value(v);
                    case "height" -> s.height = ValueParser.value(v);
                    case "min-width" -> s.minWidth = ValueParser.value(v);
                    case "max-width" -> s.maxWidth = ValueParser.value(v);
                    case "min-height" -> s.minHeight = ValueParser.value(v);
                    case "max-height" -> s.maxHeight = ValueParser.value(v);
                    case "aspect-ratio" -> s.aspectRatio = aspectRatio(v, p);
                    case "margin" -> s.margin = Sides.shorthand(ValueParser.list(v));
                    case "padding" -> s.padding = nonNegativeSides(Sides.shorthand(ValueParser.list(v)), p, "Padding", true);
                    case "border" -> s.border = nonNegativeSides(Sides.shorthand(ValueParser.list(v)), p, "Border", false);
                    case "background" -> s.background = color(v, p);
                    case "color" -> s.color = color(v, p);
                    case "border-color" -> s.borderColor = color(v, p);
                    case "font" -> s.font = v.trim();
                    case "font-size" -> s.fontSize = positive(v, p);
                    case "line-height" -> s.lineHeight = ValueParser.value(v);
                    case "text-align" -> s.textAlign = enumValue(Style.TextAlign.class, v, p);
                    case "text-wrap" -> s.textWrap = enumValue(Style.TextWrap.class, v, p);
                    case "flex" -> flex(inline(p), s);
                    case "grid" -> grid(inline(p), s);
                    case "item" -> item(inline(p), s);
                    case "break-before" -> s.breakBefore = enumValue(Style.Break.class, v, p);
                    case "break-after" -> s.breakAfter = enumValue(Style.Break.class, v, p);
                    case "break-inside" -> s.avoidBreakInside = breakInside(v, p);
                    case "widows" -> s.widows = positiveInteger(v, p);
                    case "orphans" -> s.orphans = positiveInteger(v, p);
                    case "decoration" -> s.decoration = enumValue(Style.Decoration.class, v, p);
                    default -> throw error(p, "Propiedad desconocida en Box: " + p.getName());
                }
            } catch (IllegalArgumentException ex) {
                throw error(p, ex.getMessage());
            }
        }
        validateBox(n, s, parent);

        Box box = new Box(id, s, false, n.getLine());
        buildContent(n, box);
        return box;
    }

    private void flex(InlineNode n, Style s) {
        for (Node p : n.getChildren()) {
            String v = p.getText();
            switch (p.getCanonicalName()) {
                case "direction" -> s.flexDirection = enumValue(Style.Direction.class, v, p);
                case "wrap" -> s.flexWrap = bool(v, p);
                case "gap" -> gap(v, s);
                case "main-axis" -> s.flexMainAxis = enumValue(Style.Distribution.class, v, p);
                case "cross-axis" -> s.flexCrossAxis = enumValue(Style.Alignment.class, v, p);
                case "lines" -> s.flexLines = enumValue(Style.Distribution.class, v, p);
                default -> throw error(p, "Propiedad desconocida en Flex: " + p.getName());
            }
        }
        if (s.flexMainAxis == Style.Distribution.STRETCH) {
            throw error(n, "Main axis no admite 'stretch'; usa Grow en los ítems");
        }
    }

    private void grid(InlineNode n, Style s) {
        for (Node p : n.getChildren()) {
            String v = p.getText();
            switch (p.getCanonicalName()) {
                case "columns" -> s.gridColumns = ValueParser.list(v);
                case "rows" -> s.gridRows = ValueParser.list(v);
                case "gap" -> gap(v, s);
                case "areas" -> {
                    s.gridAreas = v;
                    GridAreas.parse(v); // valida rectángulos y filas de igual longitud
                }
                case "flow" -> s.gridFlow = enumValue(Style.Direction.class, v, p);
                case "horizontal" -> s.gridHorizontal = enumValue(Style.Alignment.class, v, p);
                case "vertical" -> s.gridVertical = enumValue(Style.Alignment.class, v, p);
                default -> throw error(p, "Propiedad desconocida en Grid: " + p.getName());
            }
        }
    }

    private void item(InlineNode n, Style s) {
        for (Node p : n.getChildren()) {
            String v = p.getText();
            switch (p.getCanonicalName()) {
                case "grow" -> s.itemGrow = nonNegative(v, p);
                case "shrink" -> s.itemShrink = nonNegative(v, p);
                case "basis" -> s.itemBasis = ValueParser.value(v);
                case "cross-axis" -> s.itemCrossAxis = enumValue(Style.Alignment.class, v, p);
                case "cell" -> s.itemCell = v.trim();
                case "horizontal" -> s.itemHorizontal = enumValue(Style.Alignment.class, v, p);
                case "vertical" -> s.itemVertical = enumValue(Style.Alignment.class, v, p);
                default -> throw error(p, "Propiedad desconocida en Item: " + p.getName());
            }
        }
    }

    /** Reglas que el template no puede expresar (ESPECIFICACION.md §6). */
    private void validateBox(InlineNode n, Style s, Box parent) {
        if (child(n, "flex").isPresent() && s.layout != Style.Layout.FLEX) {
            throw error(n, "Flex solo se aplica con Layout: flex");
        }
        if (child(n, "grid").isPresent() && s.layout != Style.Layout.GRID) {
            throw error(n, "Grid solo se aplica con Layout: grid");
        }
        Style.Layout parentLayout = parent.style.layout;
        boolean parentFlexOrGrid = parentLayout == Style.Layout.FLEX || parentLayout == Style.Layout.GRID;
        if (child(n, "item").isPresent() && (!parentFlexOrGrid || s.isOutOfFlow())) {
            throw error(n, "Item solo tiene efecto en hijos en flujo de un contenedor flex o grid");
        }
        if (s.itemCell != null) {
            if (parentLayout != Style.Layout.GRID) {
                throw error(n, "Cell solo tiene efecto dentro de un contenedor grid");
            }
            Map<String, Area> areas = parent.style.gridAreas == null
                    ? Map.of() : GridAreas.parse(parent.style.gridAreas);
            Cell.parse(s.itemCell, areas);
        }
        if (s.position == Style.Position.STATIC
                && s.offset.asList().stream().anyMatch(v -> !v.isAuto())) {
            throw error(n, "Offset no tiene efecto con Position: static");
        }
        if (s.breakBefore == Style.Break.PAGE || s.breakAfter == Style.Break.PAGE) {
            if (s.isOutOfFlow()) {
                throw error(n, "Los cortes forzados solo se aplican a cajas en flujo");
            }
        }
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

    private static Optional<String> contentOf(InlineNode n, String canonicalName) {
        return child(n, canonicalName).map(Node::getText);
    }

    /** Los nodos con hijos no pueden ser bloques de texto ({@code >>}). */
    private static InlineNode inline(Node n) {
        if (n instanceof InlineNode i) {
            return i;
        }
        throw error(n, n.getName() + " no puede ser un bloque de texto (>>)");
    }

    // ------------------------------------------------------------------------------------------
    // Utilidades de interpretación
    // ------------------------------------------------------------------------------------------

    private static void gap(String text, Style s) {
        List<Value> v = ValueParser.list(text);
        if (v.size() > 2) {
            throw new IllegalArgumentException("Gap admite 1 o 2 valores (filas columnas): " + text);
        }
        s.rowGap = v.get(0);
        s.columnGap = v.size() == 2 ? v.get(1) : v.get(0);
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String text, Node n) {
        try {
            return Enum.valueOf(type, text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw error(n, "Valor no permitido en " + n.getName() + ": " + text.trim());
        }
    }

    private static boolean breakInside(String text, Node n) {
        Style.Break b = enumValue(Style.Break.class, text, n);
        if (b == Style.Break.PAGE) {
            throw error(n, "Break inside solo admite auto o avoid");
        }
        return b == Style.Break.AVOID;
    }

    private static boolean bool(String text, Node n) {
        return switch (text.trim().toLowerCase(Locale.ROOT)) {
            case "true", "yes" -> true;
            case "false", "no" -> false;
            default -> throw error(n, "Se esperaba true o false: " + text.trim());
        };
    }

    private static int integer(String text, Node n) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            throw error(n, "Se esperaba un entero: " + text.trim());
        }
    }

    private static int positiveInteger(String text, Node n) {
        int v = integer(text, n);
        if (v < 1) {
            throw error(n, n.getName() + " debe ser al menos 1");
        }
        return v;
    }

    private static double points(String text, Node n, String property) {
        Value v = ValueParser.value(text);
        if (!(v instanceof Value.Points p) || p.pt() <= 0) {
            throw error(n, property + " del medio debe ser un número de puntos positivo: " + text);
        }
        return p.pt();
    }

    private static double positive(String text, Node n) {
        double v = nonNegative(text, n);
        if (v == 0) {
            throw error(n, n.getName() + " debe ser mayor que 0");
        }
        return v;
    }

    private static double nonNegative(String text, Node n) {
        Value v = ValueParser.value(text);
        if (!(v instanceof Value.Points p) || p.pt() < 0) {
            throw error(n, n.getName() + " debe ser un número no negativo: " + text.trim());
        }
        return p.pt();
    }

    private static Sides<Value> nonNegativeSides(Sides<Value> sides, Node n, String property, boolean allowsPercentage) {
        for (Value v : sides.asList()) {
            boolean valid = (v instanceof Value.Points p && p.pt() >= 0)
                    || (allowsPercentage && v instanceof Value.Percentage q && q.percent() >= 0);
            if (!valid) {
                throw error(n, property + (allowsPercentage
                        ? " solo admite puntos o porcentajes no negativos"
                        : " solo admite puntos no negativos"));
            }
        }
        return sides;
    }

    private static Double aspectRatio(String text, Node n) {
        String t = text.trim();
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
        throw error(n, "Aspect ratio espera 'ancho/alto' o un número positivo: " + t);
    }

    private static String color(String text, Node n) {
        String t = text.trim();
        if (!COLOR.matcher(t).matches()) {
            throw error(n, "Color no válido (usa #RGB o #RRGGBB): " + t);
        }
        return t;
    }

    /** Hijo obligatorio de una sola palabra ({@code "Type"}), para los mensajes de error. */
    private static String required(InlineNode n, String child) {
        return contentOf(n, child.toLowerCase(Locale.ROOT)).orElseThrow(() -> error(n, n.getName() + " necesita " + child));
    }

    private static InvalidModelException error(Node n, String message) {
        return new InvalidModelException(n.getLine(), message);
    }
}
