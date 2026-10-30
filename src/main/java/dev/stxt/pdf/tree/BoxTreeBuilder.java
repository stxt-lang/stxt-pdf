package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.pdf.model.Break;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.NumberStyle;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.Rotation;
import dev.stxt.pdf.model.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static dev.stxt.pdf.tree.ListBuilder.itemNumber;
import static dev.stxt.pdf.tree.ListBuilder.marker;
import static dev.stxt.pdf.tree.ListBuilder.markerLine;
import static dev.stxt.pdf.tree.ModelValidator.definiteHeight;
import static dev.stxt.pdf.tree.ModelValidator.font;
import static dev.stxt.pdf.tree.ModelValidator.validateBox;
import static dev.stxt.pdf.tree.ModelValidator.validateContainer;
import static dev.stxt.pdf.tree.ModelValidator.validateRotation;
import static dev.stxt.pdf.tree.Nodes.child;
import static dev.stxt.pdf.tree.Nodes.error;
import static dev.stxt.pdf.tree.Nodes.inline;
import static dev.stxt.pdf.tree.Nodes.is;
import static dev.stxt.pdf.tree.Nodes.isElement;
import static dev.stxt.pdf.tree.PropertyReader.box;
import static dev.stxt.pdf.tree.PropertyReader.fragmentation;
import static dev.stxt.pdf.tree.PropertyReader.item;
import static dev.stxt.pdf.tree.PropertyReader.typography;
import static dev.stxt.pdf.tree.TableBuilder.table;
import static dev.stxt.pdf.tree.ValueReader.angle;
import static dev.stxt.pdf.tree.ValueReader.bool;
import static dev.stxt.pdf.tree.ValueReader.enumValue;
import static dev.stxt.pdf.tree.ValueReader.positiveInteger;

/**
 * Construye el árbol de cajas a partir del documento (referencia/comun/estructura.md): traduce cada
 * elemento a una caja con el {@link Layout} y la {@link Position} de su
 * {@link Element}, pasa las propiedades de {@code Item} y {@code Area} al elemento que envuelven,
 * aplica la herencia y las reglas de {@code Styles}, y comprueba las reglas que el schema no
 * puede expresar (referencia/comun/errores.md).
 *
 * <p>Es el orquestador: lee los valores con {@link ValueReader} y las propiedades con
 * {@link PropertyReader}, aplica las reglas con {@link StyleRules}, construye tablas y listas con
 * {@link TableBuilder} y {@link ListBuilder} y valida con {@link ModelValidator}.</p>
 *
 * <p>Prioridad de cada propiedad, de menos a más: el valor por defecto, lo heredado, la regla
 * {@code For element} del tipo, las reglas {@code For style} de sus clases, la regla {@code For id}
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
    private static final Set<String> NOT_PROPERTIES = Set.of("item", "area", "content", "style", "head", "body",
            "foot", "span", "bullet-markers", "number-markers", "table-columns", "column-align");

    /**
     * Resultado: el medio del documento, la raíz anónima de la que cuelgan sus {@code Fixed} y sus secciones
     * (referencia/componentes/document.md), las secciones en orden y el título y el idioma. Cada sección es la raíz de
     * su layout y representa el área útil de sus páginas; la raíz del documento no se maqueta.
     */
    public record Result(Media media, Box root, List<Section> sections, Metadata metadata) {
    }

    /**
     * Construye el árbol de un documento, validado o no.
     *
     * @throws dev.stxt.exceptions.ValidationException si no cumple el schema (referencia/comun/estructura.md)
     * @throws InvalidModelException si incumple una regla del modelo (referencia/comun/errores.md)
     */
    public Result build(InlineNode document) {
        return build(document, null);
    }

    /**
     * Como {@link #build(InlineNode)}, con un documento de estilos aparte (raíz {@code Styles},
     * referencia/comun/estilos.md) cuyas reglas valen menos que las del propio documento. {@code styles} puede ser
     * {@code null}.
     *
     * @throws dev.stxt.exceptions.ValidationException si alguno no cumple el schema (referencia/comun/estructura.md)
     * @throws InvalidModelException si incumple una regla del modelo (referencia/comun/errores.md); la línea de un
     *         error de las reglas del documento de estilos es la suya, y el mensaje lo dice
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
                        + DocumentSchema.NAMESPACE + "):").inStylesDocument();
            }
            DocumentSchema.validate(styles, true);
        }
        MediaReader.Spec documentMedia = MediaReader.media(inline(child(document, "page").orElseThrow()), null);
        Media media = documentMedia.media();
        Metadata metadata = MediaReader.metadata(document);
        BuildContext ctx = new BuildContext(new StyleRules(document, styles));

        // La tipografía de For element: Document la heredan todas las cajas.
        Style rootStyle = new Style();
        rootStyle.typography().setLocale(Locale.forLanguageTag(metadata.language()));
        ctx.rules.applyDocument(rootStyle);
        Box root = new Box(null, null, rootStyle, true, document.getLine());
        ctx.definiteHeight.put(root, true);
        List<Section> sections = new ArrayList<>();
        for (Node h : document.getChildren()) {
            if (h.getCanonicalName().equals("fixed")) {
                root.addChild(element(ctx, inline(h), root, null, null));
            } else if (h.getCanonicalName().equals("section")) {
                Section section = section(ctx, inline(h), root, documentMedia, sections.isEmpty());
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
     * Una {@code Section} (referencia/componentes/document.md): su caja, que es la raíz de su layout y tiene el alto de
     * la página menos los márgenes (siempre definido), con su medio, su numeración y los {@code Fixed} que lleva.
     */
    private Section section(BuildContext ctx, InlineNode n, Box documentRoot, MediaReader.Spec documentMedia,
                            boolean first) {
        Style s = Style.inheriting(documentRoot.style());
        s.setLayout(Layout.BLOCK);
        s.setPosition(Position.STATIC);
        ctx.rules.applySection(n, s);
        String id = null;
        MediaReader.Spec media = documentMedia;
        Section.PageNumbering numbering = Section.PageNumbering.CONTINUE;
        NumberStyle pageStyle = NumberStyle.DECIMAL;
        Node pageStyleNode = null;
        Node startNode = null;
        int start = 1;
        boolean inheritFixed = true;
        Section.BeginOn beginOn = Section.BeginOn.ANY;
        for (Node p : n.getChildren()) {
            switch (p.getCanonicalName()) {
                case "id" -> id = ctx.id(p);
                case "page" -> media = MediaReader.media(inline(p), documentMedia);
                case "page-numbering" -> numbering = enumValue(Section.PageNumbering.class, p);
                case "page-number-format" -> {
                    pageStyle = enumValue(NumberStyle.class, p);
                    pageStyleNode = p;
                }
                case "start" -> {
                    start = positiveInteger(p);
                    startNode = p;
                }
                case "inherit-fixed" -> inheritFixed = bool(p);
                case "begin-on" -> {
                    beginOn = enumValue(Section.BeginOn.class, p);
                    if (first && beginOn == Section.BeginOn.ODD) {
                        // La primera página del documento ya es impar (referencia/componentes/document.md).
                        throw error(p, ErrorCode.BEGIN_ON_FIRST_SECTION, "Begin on: odd no tiene efecto en la primera "
                                + "sección, que ya empieza en la página 1: quítalo");
                    }
                }
                default -> {
                }
            }
        }
        // Lo que no tendría efecto (referencia/componentes/document.md#numeración-de-las-páginas).
        if (startNode != null && numbering != Section.PageNumbering.RESTART) {
            throw error(startNode, ErrorCode.START_WITHOUT_RESTART, "Start solo tiene efecto con Page numbering: "
                    + "restart, que es cuando la numeración vuelve a empezar: decláralo o quita Start");
        }
        if (pageStyleNode != null && numbering == Section.PageNumbering.NONE) {
            throw error(pageStyleNode, ErrorCode.PAGE_NUMBER_FORMAT_WITHOUT_NUMBERS, "Page number format no tiene efecto con Page "
                    + "numbering: none, porque las páginas de la sección no tienen número: quítalo");
        }
        Box box = new Box(id, Element.SECTION, s, false, n.getLine());
        ctx.definiteHeight.put(box, true);
        buildContent(ctx, n, box);
        if (box.children().stream().allMatch(c -> c.style().position() == Position.FIXED)) {
            throw error(n, ErrorCode.SECTION_EMPTY, "La sección no tiene contenido: sin elementos (los Fixed no "
                    + "cuentan, no ocupan sitio) no tiene ninguna página");
        }
        return new Section(box, media.media(), numbering, pageStyle, start, inheritFixed, beginOn);
    }

    /** Elementos hijos en orden: cada uno es una caja; {@code Item} y {@code Area} envuelven a la suya. */
    void buildContent(BuildContext ctx, InlineNode node, Box box) {
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
     * El orden es el mismo para todos: el sitio, la herencia, las reglas, las propiedades y sus
     * comprobaciones, la caja y el contenido; lo propio de cada elemento lo hace su {@link ElementReader}.
     */
    Box element(BuildContext ctx, InlineNode n, Box parent, InlineNode wrapper, CellPosition position) {
        Element kind = ctx.codeBlocks.contains(n) ? Element.CODE_BLOCK : Element.of(n.getCanonicalName()).orElseThrow();
        ElementReader reader = ElementReader.of(kind);
        place(ctx, n, kind);
        Style s = Style.inheriting(parent.style());
        s.setLayout(reader.layout(n, kind));
        s.setPosition(kind.position());
        reader.wrapper(wrapper, s, parent);
        ctx.rules.apply(n, kind, s, parent, position);
        String id = properties(ctx, n, reader, s, parent);
        reader.check(n, wrapper, s, position);
        validateRotation(s);
        withoutEffect(ctx, n, wrapper, s);
        Integer number = kind == Element.LIST_ITEM ? itemNumber(ctx, n, s, parent) : null;
        validateBox(ctx, s, parent);
        if (kind.headingLevel() > 0) {
            // El motor evita cortar dentro del encabezado y después de él (referencia/componentes/heading.md); no se
            // declara.
            s.breaks().setAfter(Break.AVOID);
            s.breaks().setAvoidInside(true);
        }

        Box box = new Box(id, kind, s, false, n.getLine());
        ctx.definiteHeight.put(box, definiteHeight(ctx, s, parent));
        validateContainer(ctx, s, box);
        if (number != null) {
            listItem(ctx, n, box, parent, number);
        }
        boolean outerFixed = ctx.insideFixed;
        String outerUnbreakable = ctx.unbreakable;
        ctx.insideFixed |= s.position() == Position.FIXED;
        if (ctx.unbreakable == null) {
            ctx.unbreakable = unbreakable(s);
        }
        reader.content(this, ctx, n, box);
        ctx.insideFixed = outerFixed;
        ctx.unbreakable = outerUnbreakable;
        if (kind.headingLevel() > 0) {
            // Los textos del encabezado ya están leídos: el número se escribe delante del primero.
            String headingNumber = ctx.headingNumbers.next(kind.headingLevel(), s.typography().numbered(),
                    s.typography().numberStyle());
            if (headingNumber != null) {
                HeadingNumbers.prefix(box, headingNumber);
            }
        }
        if (box.marker() != null) {
            markerLine(box);
        }
        return box;
    }

    /**
     * Lo que el elemento declara y no tendría efecto por su sitio: {@code Isolate} en un ítem de {@code Flex} o {@code
     * Grid}, que ya aísla (referencia/componentes/block.md), y un {@code Break} propio, o el de la {@code Row} de una
     * celda, dentro de una caja que no se parte (referencia/comun/cortes.md). Lo que llega de una regla no se mira
     * aquí: una regla por tipo daría un error en cada elemento de ese tipo que esté en un pie.
     */
    private static void withoutEffect(BuildContext ctx, InlineNode n, InlineNode wrapper, Style s) {
        boolean item = wrapper != null && !is(wrapper, "row");
        if (item && s.isolate()) {
            throw error(s.line("isolate"), ErrorCode.ISOLATE_IN_ITEM, "Isolate no tiene efecto en un ítem de "
                    + "Flex o Grid, que ya corta el colapso de márgenes con sus hijos: quítalo");
        }
        if (ctx.unbreakable == null) {
            return;
        }
        for (InlineNode owner : wrapper == null || item ? List.of(n) : List.of(n, wrapper)) {
            Optional<Node> group = child(owner, "break");
            if (group.isPresent() && !inline(group.get()).getChildren().isEmpty()) {
                Node p = inline(group.get()).getChildren().get(0);
                throw error(p, ErrorCode.BREAK_IN_UNBREAKABLE, p.getName() + " no tiene efecto dentro de "
                        + ctx.unbreakable + ", que no se parte entre páginas: quítalo");
            }
        }
    }

    /** Cómo se nombra la caja en los mensajes si no se parte entre páginas, o {@code null} si se parte. */
    private static String unbreakable(Style s) {
        return switch (s.position()) {
            case ABSOLUTE -> "un Absolute";
            case FIXED -> "un Fixed";
            case RELATIVE -> s.rotation() != null ? "un Relative girado" : null;
            default -> s.rotatesInFlow() ? "una caja girada" : null;
        };
    }

    /**
     * Comprueba que el elemento puede estar donde está: una caja fuera de flujo no va dentro de un {@code Columns}
     * (referencia/componentes/columns.md) y un encabezado respeta la jerarquía (referencia/componentes/heading.md).
     */
    private static void place(BuildContext ctx, InlineNode n, Element kind) {
        if (ctx.insideMulticol && kind.position() != Position.STATIC && kind.position() != Position.RELATIVE) {
            throw error(n, ErrorCode.POSITIONED_IN_COLUMNS, "Un " + n.getName() + " no puede estar dentro de un "
                    + "Columns: su posición no depende de las columnas; sácalo del Columns");
        }
        if (kind.headingLevel() > 0) {
            heading(ctx, n, kind.headingLevel());
        }
    }

    /**
     * Un {@code List item} (referencia/componentes/list.md): su número, que no puede ser de un ítem vacío, y la caja de
     * su marcador, si la lista lo lleva.
     */
    private static void listItem(BuildContext ctx, InlineNode n, Box box, Box list, int number) {
        ctx.itemNumbers.put(box, number);
        if (n.getChildren().stream().noneMatch(Nodes::isElement)) {
            throw error(n, ErrorCode.LIST_ITEM_EMPTY, "Un List item necesita al menos un elemento: "
                    + "un Paragraph, un Text, un Block…");
        }
        box.setNumber(ListMarkers.number(list.style().list().marker(), number));
        String marker = ListMarkers.text(list.style().list().marker(), number);
        if (marker != null) {
            box.setMarker(marker(n, box, marker));
        }
    }

    /**
     * Las propiedades que declara el elemento, en orden: los grupos, las de su tipo ({@code Flex}, {@code Grid} y
     * {@code Table}, {@code Columns}) y las sueltas. Pone el giro en el estilo y devuelve el {@code Id}.
     */
    private String properties(BuildContext ctx, InlineNode n, ElementReader reader, Style s, Box parent) {
        String id = null;
        Double rotate = null;
        Value[] rotateOrigin = {Rotation.CENTER, Rotation.CENTER};
        for (Node p : n.getChildren()) {
            String name = p.getCanonicalName();
            if (isElement(p) || NOT_PROPERTIES.contains(name) || reader.property(p, s)) {
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
            s.setRotation(new Rotation(rotate, rotateOrigin[0], rotateOrigin[1]));
        }
        return id;
    }

    /**
     * Comprueba que un encabezado está en su sitio (referencia/componentes/heading.md): fuera de un {@code Fixed} y sin
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
}
