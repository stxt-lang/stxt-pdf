package dev.stxt.boxes.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.TextNode;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Sides;
import dev.stxt.boxes.model.Value;
import dev.stxt.boxes.model.ValueParser;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.stxt.boxes.tree.ListBuilder.itemNumber;
import static dev.stxt.boxes.tree.ListBuilder.marker;
import static dev.stxt.boxes.tree.ListBuilder.markerLine;
import static dev.stxt.boxes.tree.ListBuilder.validateList;
import static dev.stxt.boxes.tree.ModelValidator.definiteHeight;
import static dev.stxt.boxes.tree.ModelValidator.font;
import static dev.stxt.boxes.tree.ModelValidator.validateBox;
import static dev.stxt.boxes.tree.ModelValidator.validateContainer;
import static dev.stxt.boxes.tree.ModelValidator.validateFlex;
import static dev.stxt.boxes.tree.ModelValidator.validateMulticol;
import static dev.stxt.boxes.tree.Nodes.child;
import static dev.stxt.boxes.tree.Nodes.error;
import static dev.stxt.boxes.tree.Nodes.inline;
import static dev.stxt.boxes.tree.Nodes.is;
import static dev.stxt.boxes.tree.Nodes.isElement;
import static dev.stxt.boxes.tree.Nodes.parse;
import static dev.stxt.boxes.tree.Nodes.required;
import static dev.stxt.boxes.tree.Nodes.unread;
import static dev.stxt.boxes.tree.PropertyReader.FLEX;
import static dev.stxt.boxes.tree.PropertyReader.GRID;
import static dev.stxt.boxes.tree.PropertyReader.MULTICOL;
import static dev.stxt.boxes.tree.PropertyReader.box;
import static dev.stxt.boxes.tree.PropertyReader.flex;
import static dev.stxt.boxes.tree.PropertyReader.fragmentation;
import static dev.stxt.boxes.tree.PropertyReader.grid;
import static dev.stxt.boxes.tree.PropertyReader.item;
import static dev.stxt.boxes.tree.PropertyReader.multicol;
import static dev.stxt.boxes.tree.PropertyReader.typography;
import static dev.stxt.boxes.tree.TableBuilder.cellText;
import static dev.stxt.boxes.tree.TableBuilder.table;
import static dev.stxt.boxes.tree.ValueReader.LENGTH_OR_AUTO;
import static dev.stxt.boxes.tree.ValueReader.bool;
import static dev.stxt.boxes.tree.ValueReader.enumValue;
import static dev.stxt.boxes.tree.ValueReader.integer;
import static dev.stxt.boxes.tree.ValueReader.points;
import static dev.stxt.boxes.tree.ValueReader.sides;

/**
 * Construye el árbol de cajas a partir del documento (ESPECIFICACION.md §5.1): traduce cada
 * elemento a una caja con el {@link Style.Layout} y la {@link Style.Position} de su
 * {@link Element}, pasa las propiedades de {@code Item} y {@code Area} al elemento que envuelven,
 * aplica la herencia y las reglas de {@code Styles}, y comprueba las reglas que el schema no
 * puede expresar (§6).
 *
 * <p>Es el orquestador: lee los valores con {@link ValueReader} y las propiedades con
 * {@link PropertyReader}, aplica las reglas con {@link StyleRules}, construye tablas y listas con
 * {@link TableBuilder} y {@link ListBuilder} y valida con {@link ModelValidator}.</p>
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
 * <p>Trabaja sobre los nodos de {@code stxt-core} ({@link Nodes}). Antes de construir valida el
 * documento contra el schema ({@link DocumentSchema}), aunque ya venga validado de
 * {@code DocumentReader}: así la estructura (qué hijos admite cada nodo) solo está definida en el
 * schema, y el resto del código puede darla por buena. No guarda estado: cada llamada a
 * {@link #build} lleva el suyo en un {@link BuildContext}.</p>
 */
public final class BoxTreeBuilder {

    /** Etiqueta de idioma BCP 47: la lengua (2 o 3 letras) y subetiquetas de 1 a 8 letras o cifras. */
    private static final Pattern LANGUAGE = Pattern.compile("[a-zA-Z]{2,3}(-[a-zA-Z0-9]{1,8})*");

    /**
     * Hijos de un elemento que no son propiedades suyas: los envoltorios y el texto, las clases
     * (aplicadas ya con sus reglas), los grupos de filas y el {@code Span}, que lee {@link TableBuilder},
     * y lo que lee {@link MarkdownBuilder}.
     */
    private static final Set<String> NOT_PROPERTIES = Set.of("item", "area", "content", "class", "head", "body",
            "foot", "span", "bullet", "numbering", "table-columns");

    /** Una línea en blanco dentro del texto: separa párrafos (§5.4). */
    private static final Pattern BLANK_LINE = Pattern.compile("\\n[ \\t]*\\n");

    /** Resultado: el medio, una caja raíz anónima que representa el área útil, y el título y el idioma. */
    public record Result(Media media, Box root, Metadata metadata) {
    }

    /**
     * Construye el árbol de un documento, validado o no.
     *
     * @throws dev.stxt.exceptions.ValidationException si no cumple el schema (§9)
     * @throws InvalidModelException si incumple una regla del modelo (§6)
     */
    public Result build(InlineNode document) {
        // Sin el namespace, el schema no validaría nada.
        if (!is(document, "document") || !DocumentSchema.NAMESPACE.equals(document.getNamespace())) {
            throw error(document, ErrorCode.DOCUMENT_ROOT,
                    "El nodo raíz debe ser Document (" + DocumentSchema.NAMESPACE + "):");
        }
        DocumentSchema.validate(document);
        Media media = media(inline(child(document, "media").orElseThrow()));
        Metadata metadata = metadata(document);
        BuildContext ctx = new BuildContext(new StyleRules(document));

        // El Typography de Document lo heredan todas las cajas.
        Style rootStyle = new Style();
        child(document, "typography").map(Nodes::inline).ifPresent(t -> typography(t, rootStyle));
        Box root = new Box(null, null, rootStyle, true, document.getLine());
        // El área útil tiene el alto de la página, menos los márgenes: siempre definido.
        ctx.definiteHeight.put(root, true);
        buildContent(ctx, document, root);
        ctx.rules.checkIds(ctx.ids.keySet());
        return new Result(media, root, metadata);
    }

    /** {@code Title} y {@code Language} de {@code Document}: un título no vacío y una etiqueta BCP 47. */
    private static Metadata metadata(InlineNode document) {
        String title = child(document, "title").map(n -> {
            String t = n.getText().trim();
            if (t.isEmpty()) {
                throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Title necesita un texto");
            }
            return t;
        }).orElse(null);
        String language = child(document, "language").map(n -> {
            String l = n.getText().trim();
            if (!LANGUAGE.matcher(l).matches()) {
                throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Language espera una etiqueta de idioma BCP 47 "
                        + "(es, en-US, ca…): '" + l + "'");
            }
            return l;
        }).orElse(null);
        return new Metadata(title, language);
    }

    private static Media media(InlineNode n) {
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

    /** Elementos hijos en orden: cada uno es una caja; {@code Item} y {@code Area} envuelven a la suya. */
    private void buildContent(BuildContext ctx, InlineNode node, Box box) {
        for (Node h : node.getChildren()) {
            String name = h.getCanonicalName();
            if (isElement(h)) {
                box.addChild(element(ctx, inline(h), box, null));
            } else if (name.equals("item") || name.equals("area")) {
                InlineNode wrapper = inline(h);
                List<Node> elements = wrapper.getChildren().stream().filter(Nodes::isItemElement).toList();
                if (elements.size() != 1) {
                    throw error(elements.isEmpty() ? wrapper : elements.get(1), ErrorCode.SINGLE_ELEMENT,
                            "Un " + h.getName() + " contiene exactamente un elemento, y este tiene " + elements.size());
                }
                box.addChild(element(ctx, inline(elements.get(0)), box, wrapper));
            }
        }
    }

    /**
     * Un elemento de caja; {@code wrapper} es su {@code Item} o su {@code Area}, si es un ítem, o
     * la {@code Row} de una {@code Cell}.
     */
    private Box element(BuildContext ctx, InlineNode n, Box parent, InlineNode wrapper) {
        Element kind = Element.of(n.getCanonicalName()).orElseThrow();
        if (ctx.insideMulticol && kind.position() != Style.Position.STATIC && kind.position() != Style.Position.RELATIVE) {
            throw error(n, ErrorCode.POSITIONED_IN_MULTICOL, "Un " + n.getName() + " no puede estar dentro de un "
                    + "Multicol: su posición no depende de las columnas; sácalo del Multicol");
        }
        int level = kind.headingLevel();
        if (level > 0) {
            heading(ctx, n, level);
        }
        Style s = Style.inheriting(parent.style);
        // Con valor, la celda es una caja de texto; sin valor, un bloque.
        s.layout = kind == Element.CELL && !n.getText().isBlank() ? Style.Layout.INLINE : kind.layout();
        s.position = kind.position();
        if (kind == Element.CELL) {
            // El Typography de la fila lo heredan sus celdas: va antes que las reglas.
            child(wrapper, "typography").map(Nodes::inline).ifPresent(t -> typography(t, s));
        } else if (wrapper != null) {
            item(wrapper, s, parent.style.gridAreas);
        }
        ctx.rules.apply(n, kind, s, parent);
        String id = null;
        for (Node p : n.getChildren()) {
            String name = p.getCanonicalName();
            if (isElement(p) || NOT_PROPERTIES.contains(name)) {
                continue;
            }
            if (kind == Element.FLEX && FLEX.contains(name)) {
                flex(p, s);
                continue;
            }
            if ((kind == Element.GRID || kind == Element.TABLE) && GRID.contains(name)) {
                grid(p, s);
                continue;
            }
            if (kind == Element.MULTICOL && MULTICOL.contains(name)) {
                multicol(p, s);
                continue;
            }
            switch (name) {
                case "box" -> box(inline(p), s);
                case "typography" -> typography(inline(p), s);
                case "break" -> {
                    if (parent.headingLevel() > 0) {
                        throw error(p, ErrorCode.BREAK_IN_HEADING, "Un Text de un encabezado no admite Break: "
                                + "el motor ya evita cortar dentro del encabezado y después de él");
                    }
                    fragmentation(inline(p), s);
                }
                default -> {
                    s.declared(name, p.getLine());
                    switch (name) {
                        case "id" -> id = ctx.id(p);
                        case "offset" -> s.offset = sides(p, LENGTH_OR_AUTO, true);
                        case "layer" -> s.layer = integer(p);
                        case "marker" -> s.listMarker = enumValue(Style.Marker.class, p);
                        case "start" -> s.listStart = integer(p);
                        case "number" -> s.listNumber = integer(p);
                        case "isolate" -> s.isolate = bool(p);
                        default -> throw unread(p);
                    }
                }
            }
        }
        if (kind == Element.CELL) {
            // El Break de la fila es el de sus celdas: con él se corta entre filas, como en un grid.
            child(wrapper, "break").map(Nodes::inline).ifPresent(b -> fragmentation(b, s));
        }
        if (kind == Element.FLEX) {
            validateFlex(s);
        }
        if (kind == Element.LIST) {
            validateList(s);
        }
        if (kind == Element.MULTICOL) {
            validateMulticol(s, n.getLine());
        }
        Integer number = kind == Element.LIST_ITEM ? itemNumber(ctx, n, s, parent) : null;
        validateBox(ctx, s, parent);
        if (level > 0) {
            // El motor evita cortar dentro del encabezado y después de él (§5.13); no se declara.
            s.breakAfter = Style.Break.AVOID;
            s.avoidBreakInside = true;
        }

        Box box = new Box(id, kind, s, false, n.getLine());
        ctx.definiteHeight.put(box, definiteHeight(ctx, s, parent));
        validateContainer(ctx, s, box);
        if (number != null) {
            ctx.itemNumbers.put(box, number);
            if (n.getChildren().stream().noneMatch(Nodes::isElement)) {
                throw error(n, ErrorCode.LIST_ITEM_EMPTY, "Un List item necesita al menos un elemento: "
                        + "un Paragraph, un Text, un Block…");
            }
            String marker = ListMarkers.text(parent.style.listMarker, number);
            if (marker != null) {
                box.marker = marker(n, box, marker);
            }
        }
        if (kind.hasText()) {
            text(n, box);
            box.runs = ctx.runs.get(n);
        } else if (kind == Element.CELL && s.layout == Style.Layout.INLINE) {
            cellText(n, box);
            box.runs = ctx.runs.get(n);
        } else {
            boolean outerFixed = ctx.insideFixed;
            ctx.insideFixed |= s.position == Style.Position.FIXED;
            if (kind == Element.TABLE) {
                table(n, box, (cell, row) -> element(ctx, cell, box, row));
            } else if (kind == Element.MULTICOL) {
                columns(ctx, n, box);
            } else if (kind == Element.MARKDOWN) {
                markdown(ctx, n, box);
            } else {
                buildContent(ctx, n, box);
            }
            ctx.insideFixed = outerFixed;
        }
        if (box.marker != null) {
            markerLine(box);
        }
        return box;
    }

    /**
     * El contenido de un {@code Multicol} (§5.16) va en su flujo de columnas, una caja de bloque
     * generada que es su único hijo: cada columna es un fragmento suyo. Crea un contexto de
     * formato de bloque, para que los márgenes no salgan de las columnas, y su alto no es
     * definido: lo decide el reparto en columnas.
     */
    private void columns(BuildContext ctx, InlineNode n, Box multicol) {
        Style s = Style.inheriting(multicol.style);
        s.isolate = true;
        Box flow = new Box(null, Element.COLUMNS, s, false, n.getLine());
        multicol.addChild(flow);
        ctx.definiteHeight.put(flow, false);
        boolean outer = ctx.insideMulticol;
        ctx.insideMulticol = true;
        buildContent(ctx, n, flow);
        ctx.insideMulticol = outer;
    }

    /**
     * El contenido de un {@code Markdown} (§5.17): los elementos que genera {@link MarkdownBuilder},
     * construidos como si los hubiera escrito el autor; sus textos con formato llevan sus tramos.
     */
    private void markdown(BuildContext ctx, InlineNode n, Box box) {
        MarkdownBuilder.Translation t = MarkdownBuilder.translate(n);
        ctx.runs.putAll(t.runs());
        for (InlineNode e : t.elements()) {
            box.addChild(element(ctx, e, box, null));
        }
    }

    /**
     * Comprueba que un encabezado está en su sitio (§5.13): fuera de un {@code Fixed} y sin
     * saltos de nivel. En orden del documento, el primero es {@code Heading 1} y cada uno sube
     * como mucho un nivel respecto al anterior.
     */
    private static void heading(BuildContext ctx, InlineNode n, int level) {
        if (ctx.insideFixed) {
            throw error(n, ErrorCode.HEADING_IN_FIXED, "Un " + n.getName() + " no puede estar dentro de un Fixed, "
                    + "que se repite en cada página y no forma parte del contenido: usa un Text");
        }
        if (level > ctx.headingLevel + 1) {
            throw error(n, ErrorCode.HEADING_LEVEL, ctx.headingLevel == 0
                    ? "El primer encabezado del documento debe ser Heading 1, no " + n.getName()
                    : "Salto de nivel: " + n.getName() + " después de Heading " + ctx.headingLevel
                            + "; como mucho, Heading " + (ctx.headingLevel + 1));
        }
        ctx.headingLevel = level;
    }

    /** El texto de un {@code Text} o un {@code Paragraph}: en su valor o en {@code Content}, uno de los dos. */
    private static void text(InlineNode n, Box box) {
        Optional<Node> content = child(n, "content");
        boolean value = !n.getText().isBlank();
        if (value && content.isPresent()) {
            throw error(content.get(), ErrorCode.TEXT_CONTENT, n.getName() + " tiene el texto en el valor y en Content "
                    + "(línea " + n.getLine() + "): deja solo uno");
        }
        if (!value && content.isEmpty()) {
            throw error(n, ErrorCode.TEXT_CONTENT, n.getName() + " necesita su texto: en el valor ("
                    + n.getName() + ": …) o en un bloque Content >>");
        }
        Node source = content.orElse(n);
        font(n, source, box);
        if (box.element == Element.PARAGRAPH) {
            // Las líneas en blanco al principio o al final no separan nada.
            String text = source.getText();
            Matcher blank = BLANK_LINE.matcher(text)
                    .region(text.length() - text.stripLeading().length(), text.stripTrailing().length());
            if (blank.find()) {
                // El bloque empieza en la línea siguiente a Content; la línea en blanco, tras el salto encontrado.
                int line = source.getLine() + 2 + (int) text.substring(0, blank.start()).chars().filter(c -> c == '\n').count();
                throw error(line, ErrorCode.PARAGRAPH_BLANK_LINE, "Un Paragraph es un solo párrafo: quita la línea "
                        + "en blanco o usa un Paragraph para cada uno");
            }
        }
        box.text = source.getText();
        box.textLine = source.getLine();
        box.textBlock = source instanceof TextNode;
    }
}
