package dev.stxt.boxes.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.TextNode;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Links;
import dev.stxt.boxes.model.Rotation;
import dev.stxt.boxes.model.Sides;
import dev.stxt.boxes.model.TextRun;
import dev.stxt.boxes.model.Value;
import dev.stxt.boxes.model.ValueParser;

import java.util.List;
import java.util.Locale;
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
import static dev.stxt.boxes.tree.ModelValidator.validateImage;
import static dev.stxt.boxes.tree.ModelValidator.validateMulticol;
import static dev.stxt.boxes.tree.ModelValidator.validateRotation;
import static dev.stxt.boxes.tree.Nodes.child;
import static dev.stxt.boxes.tree.Nodes.error;
import static dev.stxt.boxes.tree.Nodes.inline;
import static dev.stxt.boxes.tree.Nodes.is;
import static dev.stxt.boxes.tree.Nodes.isElement;
import static dev.stxt.boxes.tree.Nodes.parse;
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
import static dev.stxt.boxes.tree.ValueReader.angle;
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

    /**
     * Hijos de un elemento que no son propiedades suyas: los envoltorios y el texto, las clases
     * (aplicadas ya con sus reglas), los grupos de filas y el {@code Span}, que lee {@link TableBuilder},
     * y lo que lee {@link MarkdownBuilder}.
     */
    private static final Set<String> NOT_PROPERTIES = Set.of("item", "area", "content", "class", "head", "body",
            "foot", "span", "bullet", "numbering", "table-columns", "code-font", "column-align");

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
        return build(document, null);
    }

    /**
     * Como {@link #build(InlineNode)}, con un documento de estilos aparte (raíz {@code Styles}, §4.10)
     * cuyas reglas valen menos que las del propio documento. {@code styles} puede ser {@code null}.
     *
     * @throws dev.stxt.exceptions.ValidationException si alguno no cumple el schema (§9)
     * @throws InvalidModelException si incumple una regla del modelo (§6); la línea de un error de
     *         las reglas del documento de estilos es la suya, y el mensaje lo dice
     */
    public Result build(InlineNode document, InlineNode styles) {
        // Sin el namespace, el schema no validaría nada.
        if (!is(document, "document") || !DocumentSchema.NAMESPACE.equals(document.getNamespace())) {
            throw error(document, ErrorCode.DOCUMENT_ROOT,
                    "El nodo raíz debe ser Document (" + DocumentSchema.NAMESPACE + "):");
        }
        DocumentSchema.validate(document);
        if (styles != null) {
            if (!is(styles, "styles") || !DocumentSchema.NAMESPACE.equals(styles.getNamespace())) {
                throw error(styles, ErrorCode.DOCUMENT_ROOT, "El nodo raíz del documento de estilos debe ser Styles ("
                        + DocumentSchema.NAMESPACE + "):").inSource(StyleRules.EXTERNAL);
            }
            DocumentSchema.validate(styles);
        }
        Media media = media(inline(child(document, "media").orElseThrow()));
        Metadata metadata = metadata(document);
        BuildContext ctx = new BuildContext(new StyleRules(document, styles));

        // La tipografía de Element: Document la heredan todas las cajas.
        Style rootStyle = new Style();
        rootStyle.locale = Locale.forLanguageTag(metadata.language());
        ctx.rules.applyDocument(rootStyle);
        Box root = new Box(null, null, rootStyle, true, document.getLine());
        // El área útil tiene el alto de la página, menos los márgenes: siempre definido.
        ctx.definiteHeight.put(root, true);
        buildContent(ctx, document, root);
        ContentsBuilder.build(ctx, root);
        References.validate(root);
        LinkTargets.validate(root);
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
        String language = child(document, "language").map(ValueReader::language).orElse(null);
        return new Metadata(title, language);
    }

    /**
     * El medio (§4.1): su tamaño es un {@code Paper} con su {@code Orientation} o un {@code Width}
     * y un {@code Height}, nunca las dos cosas; el schema solo sabe que cada uno va una vez.
     */
    private static Media media(InlineNode n) {
        Optional<Node> paper = child(n, "paper");
        Optional<Node> orientation = child(n, "orientation");
        Optional<Node> widthNode = child(n, "width");
        Optional<Node> heightNode = child(n, "height");
        double width;
        double height;
        if (paper.isPresent()) {
            Optional<Node> both = widthNode.isPresent() ? widthNode : heightNode;
            if (both.isPresent()) {
                throw error(both.get(), ErrorCode.MEDIA_SIZE, "El medio declara Paper y " + both.get().getName()
                        + ": el tamaño de la página es uno de los dos, Paper (con Orientation) o Width y Height");
            }
            Media.Paper p = enumValue(Media.Paper.class, paper.get());
            boolean landscape = orientation.map(o -> enumValue(Media.Orientation.class, o))
                    .orElse(Media.Orientation.PORTRAIT) == Media.Orientation.LANDSCAPE;
            width = landscape ? p.height : p.width;
            height = landscape ? p.width : p.height;
        } else {
            if (orientation.isPresent()) {
                throw error(orientation.get(), ErrorCode.MEDIA_SIZE, "Orientation gira el papel de Paper, y el "
                        + "medio no lo declara: usa Paper o cambia el Width por el Height");
            }
            if (widthNode.isEmpty() || heightNode.isEmpty()) {
                throw error(widthNode.or(() -> heightNode).orElse(n), ErrorCode.MEDIA_SIZE, "El medio necesita "
                        + "su tamaño: un Paper (A4, Letter…) o un Width y un Height");
            }
            width = points(widthNode.get());
            height = points(heightNode.get());
        }
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
                box.addChild(element(ctx, inline(h), box, null, null));
            } else if (name.equals("item") || name.equals("area")) {
                InlineNode wrapper = inline(h);
                List<Node> elements = wrapper.getChildren().stream().filter(Nodes::isItemElement).toList();
                if (elements.size() != 1) {
                    throw error(elements.isEmpty() ? wrapper : elements.get(1), ErrorCode.SINGLE_ELEMENT,
                            "Un " + h.getName() + " contiene exactamente un elemento, y este tiene " + elements.size());
                }
                box.addChild(element(ctx, inline(elements.get(0)), box, wrapper, null));
            }
        }
    }

    /**
     * Un elemento de caja; {@code wrapper} es su {@code Item} o su {@code Area}, si es un ítem, o
     * la {@code Row} de una {@code Cell}, y {@code position} es entonces el sitio de la celda en su tabla.
     */
    private Box element(BuildContext ctx, InlineNode n, Box parent, InlineNode wrapper, CellPosition position) {
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
        ctx.rules.apply(n, kind, s, parent, position);
        String id = null;
        Double rotate = null;
        double[] rotateOrigin = Rotation.CENTER;
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
                        case "pages" -> s.pages = enumValue(Style.Pages.class, p);
                        case "rotate" -> rotate = angle(p);
                        case "rotate-origin" -> rotateOrigin = ValueReader.rotateOrigin(p);
                        case "marker" -> s.listMarker = enumValue(Style.Marker.class, p);
                        case "start" -> s.listStart = integer(p);
                        case "number" -> s.listNumber = integer(p);
                        case "isolate" -> s.isolate = bool(p);
                        case "link" -> s.link = link(p);
                        case "header" -> s.header = bool(p);
                        case "in-contents" -> s.inContents = bool(p);
                        case "alt" -> s.alt = alt(p);
                        case "fit" -> s.fit = enumValue(Style.Fit.class, p);
                        case "levels" -> ContentsBuilder.levels(p, s);
                        case "leader" -> s.contentsLeader = enumValue(Style.Leader.class, p);
                        default -> throw unread(p);
                    }
                }
            }
        }
        if (kind == Element.CELL) {
            // El Break de la fila es el de sus celdas: con él se corta entre filas, como en un grid.
            child(wrapper, "break").map(Nodes::inline).ifPresent(b -> fragmentation(b, s));
        }
        if (kind == Element.CELL && s.header && position != null && position.group() == CellPosition.HEAD) {
            throw error(s.line("header"), ErrorCode.TABLE_HEADER, "Header no tiene efecto en una celda de Head: "
                    + "las celdas de la cabecera ya son cabeceras de columna");
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
        if (kind == Element.IMAGE) {
            validateImage(s);
        }
        if (rotate != null) {
            s.rotation = new Rotation(rotate, rotateOrigin[0], rotateOrigin[1]);
        }
        validateRotation(s);
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
            box.number = ListMarkers.number(parent.style.listMarker, number);
            String marker = ListMarkers.text(parent.style.listMarker, number);
            if (marker != null) {
                box.marker = marker(n, box, marker);
            }
        }
        if (kind.hasText()) {
            text(n, box);
            box.runs = ctx.runs.get(n);
            inlineFormat(ctx, box);
        } else if (kind == Element.IMAGE) {
            image(n, box);
        } else if (kind == Element.CELL && s.layout == Style.Layout.INLINE) {
            cellText(n, box);
            box.runs = ctx.runs.get(n);
            inlineFormat(ctx, box);
        } else {
            boolean outerFixed = ctx.insideFixed;
            ctx.insideFixed |= s.position == Style.Position.FIXED;
            if (kind == Element.TABLE) {
                table(n, box, (cell, row, at) -> element(ctx, cell, box, row, at));
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
        if (kind == Element.CONTENTS) {
            ctx.contents.add(box);
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
        ctx.insideMarkdown = true;
        for (InlineNode e : t.elements()) {
            box.addChild(element(ctx, e, box, null, null));
        }
        ctx.insideMarkdown = false;
    }

    /**
     * Con {@code Inline: markdown} (§5.4), el texto de la caja es Markdown en línea: pasa a ser el texto
     * seguido, sin las marcas, y los tramos con formato van en {@link Box#runs}. Dentro de un
     * {@code Markdown} no hace nada, porque sus textos ya vienen interpretados. El texto deja de ser un
     * bloque: sus errores señalan la línea donde empieza.
     */
    private static void inlineFormat(BuildContext ctx, Box box) {
        if (box.style.inline != Style.Inline.MARKDOWN || ctx.insideMarkdown) {
            return;
        }
        int first = box.textBlock ? box.textLine + 1 : box.textLine;
        List<TextRun> runs = MarkdownBuilder.inline(box.text, first);
        box.text = TextRun.text(runs);
        box.textLine = first;
        box.textBlock = false;
        box.runs = TextRun.formatted(runs) ? runs : null;
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

    /**
     * La ruta de una {@code Image} (§5.18), relativa a {@code images/}. Que el fichero exista y
     * sea una imagen se comprueba al maquetar, porque hace falta la carpeta de recursos.
     */
    private static void image(InlineNode n, Box box) {
        String path = n.getText().trim();
        if (path.isEmpty()) {
            throw error(n, ErrorCode.IMAGE_UNKNOWN, "Image necesita la ruta de la imagen, relativa a la carpeta "
                    + "images/ de los recursos: Image: logos/acme.png");
        }
        box.image = path;
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
