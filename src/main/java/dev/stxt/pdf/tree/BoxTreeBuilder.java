package dev.stxt.pdf.tree;

import static dev.stxt.pdf.tree.ListBuilder.itemNumber;
import static dev.stxt.pdf.tree.ListBuilder.marker;
import static dev.stxt.pdf.tree.ListBuilder.markerLine;
import static dev.stxt.pdf.tree.ModelValidator.definiteHeight;
import static dev.stxt.pdf.tree.ModelValidator.validateBox;
import static dev.stxt.pdf.tree.ModelValidator.validateClip;
import static dev.stxt.pdf.tree.ModelValidator.validateOverflow;
import static dev.stxt.pdf.tree.ModelValidator.validateShrink;
import static dev.stxt.pdf.tree.ModelValidator.validateContainer;
import static dev.stxt.pdf.tree.ModelValidator.validateRotation;
import static dev.stxt.pdf.tree.Nodes.child;
import static dev.stxt.pdf.tree.Nodes.error;
import static dev.stxt.pdf.tree.Nodes.inline;
import static dev.stxt.pdf.tree.Nodes.is;
import static dev.stxt.pdf.tree.Nodes.isElement;
import static dev.stxt.pdf.tree.PropertyReader.box;
import static dev.stxt.pdf.tree.PropertyReader.fragmentation;
import static dev.stxt.pdf.tree.PropertyReader.typography;
import static dev.stxt.pdf.tree.ValueReader.angle;
import static dev.stxt.pdf.tree.ValueReader.bool;
import static dev.stxt.pdf.tree.ValueReader.enumValue;
import static dev.stxt.pdf.tree.ValueReader.positiveInteger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.NumberFormat;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.Rotation;
import dev.stxt.pdf.model.Value;

/**
 * Builds the box tree from the document (reference/common/structure.md): translates each
 * element into a box with the {@link Layout} and the {@link Position} of its
 * {@link Element}, passes the properties of {@code Item} and {@code Area} to the element they wrap,
 * applies inheritance and the {@code Styles} rules, and checks the rules that the schema cannot
 * express (reference/common/errors.md).
 *
 * <p>It is the orchestrator: it reads values with {@link ValueReader} and properties with
 * {@link PropertyReader}, applies the rules with {@link StyleRules}, builds tables and lists with
 * {@link TableBuilder} and {@link ListBuilder} and validates with {@link ModelValidator}.</p>
 *
 * <p>Priority of each property, from lowest to highest: the default value, the inherited value, the
 * {@code For element} rule of the type, the {@code For style} rules of its classes, the {@code For id} rule
 * and what the element itself declares. What a rule contributes is validated as if the element declared
 * it, and its errors point to the line of the rule.</p>
 *
 * <p>Errors point to the line of the offending node: that of the property, not that of its box.
 * Combinations that would have no effect or contradict each other are errors too: the model rejects
 * them instead of resolving them with a default rule.</p>
 *
 * <p>It works on the nodes of {@code stxt-core} ({@link Nodes}). Before building, it validates the
 * document against the schema ({@link DocumentSchema}), even if it comes already validated from
 * {@code DocumentReader}: this way the structure (which children each node accepts) is only defined in the
 * schema, and the rest of the code can take it as correct. It keeps no state: each call to
 * {@link #build} carries its own in a {@link BuildContext}; that is why the same builder can be used from
 * several threads, although each conversion usually creates its own.</p>
 */
public final class BoxTreeBuilder {

    /** BCP 47 language tag: the language (2 or 3 letters) and subtags of 1 to 8 letters or digits. */

    /**
     * Children of an element that are not its properties: the wrappers and the text, the classes
     * (already applied with their rules), the row groups and the {@code Span}, which {@link TableBuilder} reads,
     * and what {@link MarkdownBuilder} reads.
     */
    private static final Set<String> NOT_PROPERTIES = Set.of("item", "area", "content", "style", "header-rows", "body",
            "footer-rows", "span", "bullet-markers", "number-markers", "table-columns", "column-align");

    /**
     * Result: the medium of the document, the anonymous root from which its {@code Page overlay} and its sections hang
     * (reference/components/document.md), the sections in order and the title and the language. Each section is the
     * root of its layout and represents the page area of its pages; the root of the document is not laid out.
     */
    public record Result(PageSetup pageSetup, Box root, List<Section> sections, Metadata metadata) {
    }

    /**
     * Builds the tree of a document, validated or not.
     *
     * @throws dev.stxt.exceptions.ValidationException if it does not conform to the schema
     *         (reference/common/structure.md)
     * @throws InvalidModelException if it breaks a rule of the model (reference/common/errors.md)
     */
    public Result build(InlineNode document) {
        return build(document, null);
    }

    /**
     * Like {@link #build(InlineNode)}, with a separate styles document (root {@code Styles},
     * reference/common/styles.md) whose rules weigh less than those of the document itself. {@code styles} can be
     * {@code null}.
     *
     * @throws dev.stxt.exceptions.ValidationException if either does not conform to the schema
     *         (reference/common/structure.md)
     * @throws InvalidModelException if it breaks a rule of the model (reference/common/errors.md); the line of an
     *         error in the rules of the styles document is its own, and the message says so
     */
    public Result build(InlineNode document, InlineNode styles) {
        return build(document, styles, root -> {
        });
    }

    /**
     * Like {@link #build(InlineNode, InlineNode)}, with {@code finish} on the root right before the tree is
     * closed: the tests of the package use it to put in it what the document cannot write.
     */
    Result build(InlineNode document, InlineNode styles, Consumer<Box> finish) {
        // Without the namespace, the schema would validate nothing.
        if (!is(document, "document") || !DocumentSchema.NAMESPACE.equals(document.getNamespace())) {
            throw error(document, ErrorCode.DOCUMENT_ROOT,
                    "The root node must be Document (" + DocumentSchema.NAMESPACE + "):");
        }
        DocumentSchema.validate(document);
        if (styles != null) {
            if (!is(styles, "styles") || !DocumentSchema.NAMESPACE.equals(styles.getNamespace())) {
                throw error(styles, ErrorCode.DOCUMENT_ROOT, "The root node of the styles document must be Styles ("
                        + DocumentSchema.NAMESPACE + "):").inStylesDocument();
            }
            DocumentSchema.validate(styles, true);
        }
        PageSetupReader.Spec documentPageSetup = PageSetupReader.pageSetup(inline(child(document, "page").orElseThrow()), null);
        PageSetup pageSetup = documentPageSetup.pageSetup();
        Metadata metadata = PageSetupReader.metadata(document);
        BuildContext ctx = new BuildContext(new StyleRules(document, styles));

        // The typography of For element: Document is inherited by all boxes.
        Style rootStyle = new Style(ctx.rules.namedColors());
        rootStyle.typography().setLocale(Locale.forLanguageTag(metadata.language()));
        ctx.rules.applyDocument(rootStyle);
        Box root = new Box(null, null, rootStyle, true, document.getLine());
        ctx.definiteHeight.put(root, true);
        List<Section> sections = new ArrayList<>();
        for (Node h : document.getChildren()) {
            if (h.getCanonicalName().equals("page-overlay")) {
                root.addChild(element(ctx, inline(h), root, null, null));
            } else if (h.getCanonicalName().equals("section")) {
                Section section = section(ctx, inline(h), root, documentPageSetup, sections.isEmpty());
                sections.add(section);
                // It hangs from the root without being its child (no parent): each section is the root of its layout.
                root.addSection(section.box());
            }
        }
        TableOfContentsBuilder.build(ctx, root);
        References.validate(root);
        LinkTargets.validate(root);
        validateOverflow(root);
        finish.accept(root);
        // From here on the tree does not change: the layout, the painting and other threads only read it.
        root.close();
        return new Result(pageSetup, root, List.copyOf(sections), metadata);
    }

    /**
     * A {@code Section} (reference/components/document.md): its box, which is the root of its layout and has the height
     * of the page minus the margins (always definite), with its medium, its numbering and the {@code Page overlay} it
     * carries.
     */
    private Section section(BuildContext ctx, InlineNode n, Box documentRoot, PageSetupReader.Spec documentPageSetup,
                            boolean first) {
        Style s = Style.inheriting(documentRoot.style());
        s.setLayout(Layout.BLOCK);
        s.setPosition(Position.STATIC);
        ctx.rules.applySection(n, s);
        String id = null;
        PageSetupReader.Spec pageSetup = documentPageSetup;
        Section.PageNumbering numbering = Section.PageNumbering.CONTINUE;
        NumberFormat pageNumberFormat = NumberFormat.DECIMAL;
        Node pageNumberFormatNode = null;
        Node startNode = null;
        int start = 1;
        boolean inheritPageOverlays = true;
        Section.BeginOn beginOn = Section.BeginOn.ANY;
        for (Node p : n.getChildren()) {
            switch (p.getCanonicalName()) {
                case "id" -> id = ctx.id(p);
                case "page" -> pageSetup = PageSetupReader.pageSetup(inline(p), documentPageSetup);
                case "page-numbering" -> numbering = enumValue(Section.PageNumbering.class, p);
                case "page-number-format" -> {
                    pageNumberFormat = enumValue(NumberFormat.class, p);
                    pageNumberFormatNode = p;
                }
                case "start" -> {
                    start = positiveInteger(p);
                    startNode = p;
                }
                case "inherit-page-overlays" -> inheritPageOverlays = bool(p);
                case "begin-on" -> {
                    beginOn = enumValue(Section.BeginOn.class, p);
                    if (first && beginOn == Section.BeginOn.ODD) {
                        // The first page of the document is already odd (reference/components/document.md).
                        throw error(p, ErrorCode.BEGIN_ON_FIRST_SECTION, "Begin on: odd has no effect in the first "
                                + "section, which already starts on page 1: it is not needed");
                    }
                }
                default -> {
                }
            }
        }
        // What would have no effect (reference/components/document.md#page-numbering).
        if (startNode != null && numbering != Section.PageNumbering.RESTART) {
            throw error(startNode, ErrorCode.START_WITHOUT_RESTART, "Start only has an effect with Page numbering: "
                    + "restart, which is when the numbering starts again: restart is missing or Start is not needed");
        }
        if (pageNumberFormatNode != null && numbering == Section.PageNumbering.NONE) {
            throw error(pageNumberFormatNode, ErrorCode.PAGE_NUMBER_FORMAT_WITHOUT_NUMBERS, "Page number format has no effect with Page "
                    + "numbering: none, because the pages of the section have no number: it is not needed");
        }
        Box box = new Box(id, Element.SECTION, s, false, n.getLine());
        ctx.definiteHeight.put(box, true);
        buildContent(ctx, n, box);
        if (box.children().stream().allMatch(c -> c.style().position() == Position.PAGE_OVERLAY)) {
            throw error(n, ErrorCode.SECTION_EMPTY, "The section has no content: without elements (Page overlays do not "
                    + "count, they take no space) it has no page");
        }
        return new Section(box, pageSetup.pageSetup(), numbering, pageNumberFormat, start, inheritPageOverlays, beginOn);
    }

    /** Child elements in order: each one is a box; {@code Item} and {@code Area} wrap theirs. */
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
                            "An " + h.getName() + " contains exactly one element, and this one has " + elements.size());
                }
                box.addChild(element(ctx, inline(elements.get(0)), box, wrapper, null));
            }
        }
    }

    /**
     * A box element; {@code wrapper} is its {@code Item} or its {@code Area}, if it is an item, or
     * the {@code Row} of a {@code Cell}, and {@code position} is then the place of the cell in its table.
     * The order is the same for all: the place, inheritance, the rules, the properties and their
     * checks, the box and the content; what is specific to each element is done by its {@link ElementReader}.
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
        validateClip(s);
        withoutEffect(ctx, n, wrapper, s);
        Integer number = kind == Element.LIST_ITEM ? itemNumber(ctx, n, s, parent) : null;
        validateBox(ctx, s, parent);
        if (kind.headingLevel() > 0) {
            // The engine avoids breaking inside the heading and after it (reference/components/heading.md); it is not
            // declared.
            s.breaks().setKeepWithNext(true);
            s.breaks().setKeepTogether(true);
        }

        Box box = new Box(id, kind, s, false, n.getLine());
        ctx.definiteHeight.put(box, definiteHeight(ctx, s, parent));
        validateContainer(ctx, s, box);
        if (number != null) {
            listItem(ctx, n, box, parent, number);
        }
        boolean outerPageOverlay = ctx.insidePageOverlay;
        String outerUnbreakable = ctx.unbreakable;
        ctx.insidePageOverlay |= s.position() == Position.PAGE_OVERLAY;
        if (ctx.unbreakable == null) {
            ctx.unbreakable = unbreakable(s);
        }
        reader.content(this, ctx, n, box);
        validateShrink(n, box);
        ctx.insidePageOverlay = outerPageOverlay;
        ctx.unbreakable = outerUnbreakable;
        if (kind.headingLevel() > 0) {
            // The texts of the heading are already read: the number is written before the first one.
            String headingNumber = ctx.headingNumbers.next(kind.headingLevel(), s.typography().numbered(),
                    s.typography().numberFormat());
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
     * What the element declares that would have no effect because of its place: {@code Isolate margins} on an item of
     * {@code Stack} or {@code Grid}, which already isolates (reference/components/block.md), and its own {@code Break},
     * or that of the {@code Row} of a cell, inside a box that is not split (reference/common/breaks.md). What comes
     * from a rule is not checked here: a rule by type would give an error on each element of that type in a footer.
     */
    private static void withoutEffect(BuildContext ctx, InlineNode n, InlineNode wrapper, Style s) {
        boolean item = wrapper != null && !is(wrapper, "row");
        if (item && s.isolateMargins()) {
            throw error(s.line("isolate-margins"), ErrorCode.ISOLATE_MARGINS_IN_ITEM, "Isolate margins has no effect on an item of "
                    + "Stack or Grid, which already stops margin merging with its children: it is not needed");
        }
        if (ctx.unbreakable == null) {
            return;
        }
        for (InlineNode owner : wrapper == null || item ? List.of(n) : List.of(n, wrapper)) {
            Optional<Node> group = child(owner, "break");
            if (group.isPresent() && !inline(group.get()).getChildren().isEmpty()) {
                Node p = inline(group.get()).getChildren().get(0);
                throw error(p, ErrorCode.BREAK_IN_UNBREAKABLE, p.getName() + " has no effect inside "
                        + ctx.unbreakable + ", which is not split across pages: it is not needed");
            }
        }
    }

    /** How the box is named in messages if it is not split across pages, or {@code null} if it is split. */
    private static String unbreakable(Style s) {
        if (s.box().clips()) {
            return "a box with Overflow: clip";
        }
        return switch (s.position()) {
            case OVERLAY -> "an Overlay";
            case PAGE_OVERLAY -> "a Page overlay";
            case FRAME -> s.rotation() != null ? "a rotated Frame" : null;
            default -> s.rotatesInFlow() ? "a rotated box" : null;
        };
    }

    /**
     * Checks that the element can be where it is: an out-of-flow box does not go inside a {@code Columns}
     * (reference/components/columns.md) and a heading respects the hierarchy (reference/components/heading.md).
     */
    private static void place(BuildContext ctx, InlineNode n, Element kind) {
        if (ctx.insideColumns && kind.position() != Position.STATIC && kind.position() != Position.FRAME) {
            throw error(n, ErrorCode.OVERLAY_IN_COLUMNS, n.getName() + " cannot be inside a "
                    + "Columns: its position does not depend on the columns, so its place is outside the Columns");
        }
        if (kind.headingLevel() > 0) {
            heading(ctx, n, kind.headingLevel());
        }
    }

    /**
     * A {@code List item} (reference/components/list.md): its number, which cannot belong to an empty item, and the box
     * of its marker, if the list has one.
     */
    private static void listItem(BuildContext ctx, InlineNode n, Box box, Box list, int number) {
        ctx.itemNumbers.put(box, number);
        if (n.getChildren().stream().noneMatch(Nodes::isElement)) {
            throw error(n, ErrorCode.LIST_ITEM_EMPTY, "A List item needs at least one element: "
                    + "a Paragraph, a Text, a Block...");
        }
        box.setNumber(ListMarkers.number(list.style().list().marker(), number));
        String marker = ListMarkers.text(list.style().list().marker(), number);
        if (marker != null) {
            box.setMarker(marker(n, box, marker));
        }
    }

    /**
     * The properties the element declares, in order: the groups, those of its type ({@code Stack}, {@code Grid} and
     * {@code Table}, {@code Columns}) and the single ones. It sets the rotation in the style and returns the
     * {@code Id}.
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
                        throw error(p, ErrorCode.BREAK_IN_HEADING, "A Text of a heading does not accept Break: "
                                + "the engine already avoids breaking inside the heading and after it");
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
     * Checks that a heading is in its place (reference/components/heading.md): outside a {@code Page
     * overlay} and without level skips. In document order, the first one is {@code Heading 1} and each one goes up
     * at most one level from the previous one.
     */
    private static void heading(BuildContext ctx, InlineNode n, int level) {
        if (ctx.insidePageOverlay) {
            throw error(n, ErrorCode.HEADING_IN_PAGE_OVERLAY, "A " + n.getName() + " cannot be inside a Page overlay, "
                    + "which is repeated on every page and is not part of the content: a Text belongs there");
        }
        if (level > ctx.headingLevel + 1) {
            throw error(n, ErrorCode.HEADING_LEVEL, ctx.headingLevel == 0
                    ? "The first heading of the document must be Heading 1, not " + n.getName()
                    : "Level skip: " + n.getName() + " after Heading " + ctx.headingLevel
                            + "; at most, Heading " + (ctx.headingLevel + 1));
        }
        ctx.headingLevel = level;
    }
}
