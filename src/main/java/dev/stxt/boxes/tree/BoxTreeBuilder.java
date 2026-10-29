package dev.stxt.boxes.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.boxes.model.Rotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import static dev.stxt.boxes.tree.ListBuilder.itemNumber;
import static dev.stxt.boxes.tree.ListBuilder.marker;
import static dev.stxt.boxes.tree.ListBuilder.markerLine;
import static dev.stxt.boxes.tree.ModelValidator.definiteHeight;
import static dev.stxt.boxes.tree.ModelValidator.font;
import static dev.stxt.boxes.tree.ModelValidator.validateBox;
import static dev.stxt.boxes.tree.ModelValidator.validateContainer;
import static dev.stxt.boxes.tree.ModelValidator.validateRotation;
import static dev.stxt.boxes.tree.Nodes.child;
import static dev.stxt.boxes.tree.Nodes.error;
import static dev.stxt.boxes.tree.Nodes.inline;
import static dev.stxt.boxes.tree.Nodes.is;
import static dev.stxt.boxes.tree.Nodes.isElement;
import static dev.stxt.boxes.tree.PropertyReader.box;
import static dev.stxt.boxes.tree.PropertyReader.fragmentation;
import static dev.stxt.boxes.tree.PropertyReader.item;
import static dev.stxt.boxes.tree.PropertyReader.typography;
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
        s.layout = reader.layout(n, kind);
        s.position = kind.position();
        reader.wrapper(wrapper, s, parent);
        ctx.rules.apply(n, kind, s, parent, position);
        String id = properties(ctx, n, reader, s, parent);
        reader.check(n, wrapper, s, position);
        validateRotation(s);
        Integer number = kind == Element.LIST_ITEM ? itemNumber(ctx, n, s, parent) : null;
        validateBox(ctx, s, parent);
        if (kind.headingLevel() > 0) {
            // El motor evita cortar dentro del encabezado y después de él (§5.13); no se declara.
            s.breakAfter = Style.Break.AVOID;
            s.avoidBreakInside = true;
        }

        Box box = new Box(id, kind, s, false, n.getLine());
        ctx.definiteHeight.put(box, definiteHeight(ctx, s, parent));
        validateContainer(ctx, s, box);
        if (number != null) {
            listItem(ctx, n, box, parent, number);
        }
        boolean outerFixed = ctx.insideFixed;
        ctx.insideFixed |= s.position == Style.Position.FIXED;
        reader.content(this, ctx, n, box);
        ctx.insideFixed = outerFixed;
        if (kind.headingLevel() > 0) {
            // Los textos del encabezado ya están leídos: el número se escribe delante del primero.
            String headingNumber = ctx.headingNumbers.next(kind.headingLevel(), s.numbered);
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
     * Comprueba que el elemento puede estar donde está: una caja fuera de flujo no va dentro de un
     * {@code Multicol} (§5.16) y un encabezado respeta la jerarquía (§5.13).
     */
    private static void place(BuildContext ctx, InlineNode n, Element kind) {
        if (ctx.insideMulticol && kind.position() != Style.Position.STATIC && kind.position() != Style.Position.RELATIVE) {
            throw error(n, ErrorCode.POSITIONED_IN_MULTICOL, "Un " + n.getName() + " no puede estar dentro de un "
                    + "Multicol: su posición no depende de las columnas; sácalo del Multicol");
        }
        if (kind.headingLevel() > 0) {
            heading(ctx, n, kind.headingLevel());
        }
    }

    /**
     * Un {@code List item} (§5.14): su número, que no puede ser de un ítem vacío, y la caja de su
     * marcador, si la lista lo lleva.
     */
    private static void listItem(BuildContext ctx, InlineNode n, Box box, Box list, int number) {
        ctx.itemNumbers.put(box, number);
        if (n.getChildren().stream().noneMatch(Nodes::isElement)) {
            throw error(n, ErrorCode.LIST_ITEM_EMPTY, "Un List item necesita al menos un elemento: "
                    + "un Paragraph, un Text, un Block…");
        }
        box.setNumber(ListMarkers.number(list.style().listMarker, number));
        String marker = ListMarkers.text(list.style().listMarker, number);
        if (marker != null) {
            box.setMarker(marker(n, box, marker));
        }
    }

    /**
     * Las propiedades que declara el elemento, en orden: los grupos, las de su tipo ({@code Flex}, {@code Grid} y
     * {@code Table}, {@code Multicol}) y las sueltas. Pone el giro en el estilo y devuelve el {@code Id}.
     */
    private String properties(BuildContext ctx, InlineNode n, ElementReader reader, Style s, Box parent) {
        String id = null;
        Double rotate = null;
        double[] rotateOrigin = {Rotation.CENTER, Rotation.CENTER};
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
            s.rotation = new Rotation(rotate, rotateOrigin[0], rotateOrigin[1]);
        }
        return id;
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
}
