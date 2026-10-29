package dev.stxt.boxes.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.TextNode;
import dev.stxt.boxes.model.Rotation;
import dev.stxt.boxes.model.TextRun;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
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
import static dev.stxt.boxes.tree.ValueReader.angle;
import static dev.stxt.boxes.tree.ValueReader.bool;
import static dev.stxt.boxes.tree.ValueReader.enumValue;

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
 * {@link #build} lleva el suyo en un {@link BuildContext}; por eso un mismo constructor se puede usar desde
 * varios hilos, aunque cada conversión suele crear el suyo.</p>
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

    /**
     * Resultado: el medio del documento, la raíz anónima de la que cuelgan sus {@code Fixed} y sus
     * secciones (§5.21), las secciones en orden y el título y el idioma. Cada sección es la raíz de
     * su layout y representa el área útil de sus páginas; la raíz del documento no se maqueta.
     */
    public record Result(Media media, Box root, List<Section> sections, Metadata metadata) {
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
        return build(document, styles, root -> {
        });
    }

    /**
     * Como {@link #build(InlineNode, InlineNode)}, con {@code finish} sobre la raíz justo antes de cerrar el
     * árbol: lo usan los tests del package para poner en él lo que el documento no puede escribir.
     */
    Result build(InlineNode document, InlineNode styles, Consumer<Box> finish) {
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
        MediaReader.Spec documentMedia = MediaReader.media(inline(child(document, "media").orElseThrow()), null);
        Media media = documentMedia.media();
        Metadata metadata = MediaReader.metadata(document);
        BuildContext ctx = new BuildContext(new StyleRules(document, styles));

        // La tipografía de Element: Document la heredan todas las cajas.
        Style rootStyle = new Style();
        rootStyle.locale = Locale.forLanguageTag(metadata.language());
        ctx.rules.applyDocument(rootStyle);
        Box root = new Box(null, null, rootStyle, true, document.getLine());
        ctx.definiteHeight.put(root, true);
        List<Section> sections = new ArrayList<>();
        for (Node h : document.getChildren()) {
            if (h.getCanonicalName().equals("fixed")) {
                root.addChild(element(ctx, inline(h), root, null, null));
            } else if (h.getCanonicalName().equals("section")) {
                Section section = section(ctx, inline(h), root, documentMedia);
                sections.add(section);
                // Cuelga de la raíz sin ser su hija (sin padre): cada sección es la raíz de su layout.
                root.addSection(section.box());
            }
        }
        ContentsBuilder.build(ctx, root);
        References.validate(root);
        LinkTargets.validate(root);
        finish.accept(root);
        // Desde aquí el árbol no cambia: el layout, el pintado y otros hilos solo lo leen.
        root.close();
        return new Result(media, root, List.copyOf(sections), metadata);
    }

    /**
     * Una {@code Section} (§5.21): su caja, que es la raíz de su layout y tiene el alto de la página
     * menos los márgenes (siempre definido), con su medio, su numeración y los {@code Fixed} que lleva.
     */
    private Section section(BuildContext ctx, InlineNode n, Box documentRoot, MediaReader.Spec documentMedia) {
        Style s = Style.inheriting(documentRoot.style());
        s.layout = Style.Layout.BLOCK;
        s.position = Style.Position.STATIC;
        ctx.rules.applySection(n, s);
        String id = null;
        MediaReader.Spec media = documentMedia;
        Section.PageNumbering numbering = Section.PageNumbering.CONTINUE;
        boolean inheritFixed = true;
        for (Node p : n.getChildren()) {
            switch (p.getCanonicalName()) {
                case "id" -> id = ctx.id(p);
                case "media" -> media = MediaReader.media(inline(p), documentMedia);
                case "page-numbering" -> numbering = enumValue(Section.PageNumbering.class, p);
                case "inherit-fixed" -> inheritFixed = bool(p);
                default -> {
                }
            }
        }
        Box box = new Box(id, Element.SECTION, s, false, n.getLine());
        ctx.definiteHeight.put(box, true);
        buildContent(ctx, n, box);
        if (box.children().stream().allMatch(c -> c.style().position == Style.Position.FIXED)) {
            throw error(n, ErrorCode.SECTION_EMPTY, "La sección no tiene contenido: sin elementos (los Fixed no "
                    + "cuentan, no ocupan sitio) no tiene ninguna página");
        }
        return new Section(box, media.media(), numbering, inheritFixed);
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
        Element kind = ctx.codeBlocks.contains(n) ? Element.CODE_BLOCK : Element.of(n.getCanonicalName()).orElseThrow();
        if (ctx.insideMulticol && kind.position() != Style.Position.STATIC && kind.position() != Style.Position.RELATIVE) {
            throw error(n, ErrorCode.POSITIONED_IN_MULTICOL, "Un " + n.getName() + " no puede estar dentro de un "
                    + "Multicol: su posición no depende de las columnas; sácalo del Multicol");
        }
        int level = kind.headingLevel();
        if (level > 0) {
            heading(ctx, n, level);
        }
        Style s = Style.inheriting(parent.style());
        // Con valor, la celda es una caja de texto; sin valor, un bloque.
        s.layout = kind == Element.CELL && !n.getText().isBlank() ? Style.Layout.INLINE : kind.layout();
        s.position = kind.position();
        if (kind == Element.CELL) {
            // El Typography de la fila lo heredan sus celdas: va antes que las reglas.
            child(wrapper, "typography").map(Nodes::inline).ifPresent(t -> typography(t, s));
        } else if (wrapper != null) {
            item(wrapper, s, parent.style().gridAreas);
        }
        ctx.rules.apply(n, kind, s, parent, position);
        String id = properties(ctx, n, kind, s, parent);
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
            box.setNumber(ListMarkers.number(parent.style().listMarker, number));
            String marker = ListMarkers.text(parent.style().listMarker, number);
            if (marker != null) {
                box.setMarker(marker(n, box, marker));
            }
        }
        if (kind.hasText()) {
            text(n, box);
            box.setRuns(ctx.runs.get(n));
            inlineFormat(ctx, box);
        } else if (kind == Element.IMAGE) {
            image(n, box);
        } else if (kind == Element.CELL && s.layout == Style.Layout.INLINE) {
            cellText(n, box);
            box.setRuns(ctx.runs.get(n));
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
        if (level > 0) {
            // Los textos del encabezado ya están leídos: el número se escribe delante del primero.
            String headingNumber = ctx.headingNumbers.next(level, s.numbered);
            if (headingNumber != null) {
                HeadingNumbers.prefix(box, headingNumber);
            }
        }
        if (box.marker() != null) {
            markerLine(box);
        }
        if (kind == Element.CONTENTS) {
            ctx.contents.add(box);
        }
        return box;
    }

    /**
     * Las propiedades que declara el elemento, en orden: los grupos, las de su tipo ({@code Flex}, {@code Grid} y
     * {@code Table}, {@code Multicol}) y las sueltas. Pone el giro en el estilo y devuelve el {@code Id}.
     */
    private String properties(BuildContext ctx, InlineNode n, Element kind, Style s, Box parent) {
        String id = null;
        Double rotate = null;
        double[] rotateOrigin = {Rotation.CENTER, Rotation.CENTER};
        for (Node p : n.getChildren()) {
            String name = p.getCanonicalName();
            if (isElement(p) || NOT_PROPERTIES.contains(name) || ownProperty(kind, p, s)) {
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
                case "id" -> {
                    s.declared(name, p.getLine());
                    id = ctx.id(p);
                }
                case "rotate" -> {
                    s.declared(name, p.getLine());
                    rotate = angle(p);
                }
                case "rotate-origin" -> {
                    s.declared(name, p.getLine());
                    rotateOrigin = ValueReader.rotateOrigin(p);
                }
                default -> PropertyReader.single(p, s);
            }
        }
        if (rotate != null) {
            s.rotation = new Rotation(rotate, rotateOrigin[0], rotateOrigin[1]);
        }
        return id;
    }

    /** Lee una propiedad propia del tipo del elemento: las de {@code Flex}, {@code Grid} y {@code Table}, o {@code Multicol}. */
    private static boolean ownProperty(Element kind, Node p, Style s) {
        String name = p.getCanonicalName();
        if (kind == Element.FLEX && FLEX.contains(name)) {
            flex(p, s);
        } else if ((kind == Element.GRID || kind == Element.TABLE) && GRID.contains(name)) {
            grid(p, s);
        } else if (kind == Element.MULTICOL && MULTICOL.contains(name)) {
            multicol(p, s);
        } else {
            return false;
        }
        return true;
    }

    /**
     * El contenido de un {@code Multicol} (§5.16) va en su flujo de columnas, una caja de bloque
     * generada que es su único hijo: cada columna es un fragmento suyo. Crea un contexto de
     * formato de bloque, para que los márgenes no salgan de las columnas, y su alto no es
     * definido: lo decide el reparto en columnas.
     */
    private void columns(BuildContext ctx, InlineNode n, Box multicol) {
        Style s = Style.inheriting(multicol.style());
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
        ctx.codeBlocks.addAll(t.codeBlocks());
        ctx.insideMarkdown = true;
        for (InlineNode e : t.elements()) {
            box.addChild(element(ctx, e, box, null, null));
        }
        ctx.insideMarkdown = false;
    }

    /**
     * Con {@code Inline: markdown} (§5.4), el texto de la caja es Markdown en línea: pasa a ser el texto
     * seguido, sin las marcas, y los tramos con formato van en {@link Box#formattedRuns()}. Dentro de un
     * {@code Markdown} no hace nada, porque sus textos ya vienen interpretados. El texto deja de ser un
     * bloque: sus errores señalan la línea donde empieza.
     */
    private static void inlineFormat(BuildContext ctx, Box box) {
        if (box.style().inline != Style.Inline.MARKDOWN || ctx.insideMarkdown) {
            return;
        }
        int first = box.textBlock() ? box.textLine() + 1 : box.textLine();
        List<TextRun> runs = MarkdownBuilder.inline(box.text(), first);
        box.setText(TextRun.text(runs), first, false);
        box.setRuns(TextRun.formatted(runs) ? runs : null);
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
        box.setImage(path);
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
        if (box.element() == Element.PARAGRAPH) {
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
        box.setText(source.getText(), source.getLine(), source instanceof TextNode);
    }
}
