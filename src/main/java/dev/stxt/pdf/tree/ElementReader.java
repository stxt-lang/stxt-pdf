package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.TextNode;
import dev.stxt.pdf.model.CaptionPosition;
import dev.stxt.pdf.model.Markup;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Reference;
import dev.stxt.pdf.model.RunStyle;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.model.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.stxt.pdf.tree.ListBuilder.validateList;
import static dev.stxt.pdf.tree.ModelValidator.font;
import static dev.stxt.pdf.tree.ModelValidator.validateStack;
import static dev.stxt.pdf.tree.ModelValidator.validateImage;
import static dev.stxt.pdf.tree.ModelValidator.validateBarcode;
import static dev.stxt.pdf.tree.ModelValidator.validateColumns;
import static dev.stxt.pdf.tree.Nodes.child;
import static dev.stxt.pdf.tree.Nodes.error;
import static dev.stxt.pdf.tree.Nodes.inline;
import static dev.stxt.pdf.tree.Nodes.is;
import static dev.stxt.pdf.tree.Nodes.isElement;
import static dev.stxt.pdf.tree.PropertyReader.fragmentation;
import static dev.stxt.pdf.tree.PropertyReader.item;
import static dev.stxt.pdf.tree.PropertyReader.typography;
import static dev.stxt.pdf.tree.TableBuilder.cellText;
import static dev.stxt.pdf.tree.TableBuilder.table;

/**
 * What is specific to each family of elements when its box is built: the properties of its type, what it
 * checks of the properties already read and its content (a text, an image or its children).
 * {@link BoxTreeBuilder#element} does what is common to all, in order, and calls the family of the element
 * ({@link #of}) at each step. A new element with nothing specific is a {@link #CONTAINER}; if it has something
 * specific, it is a new family here, without touching the order of {@code element}.
 */
enum ElementReader {

    /**
     * {@code Text}, {@code Paragraph} and the {@code Caption} of a {@code Figure}: the text, with its runs (the code
     * block of a {@code Markdown} is {@link #CODE_BLOCK}).
     */
    TEXT {
        @Override
        void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
            text(n, box);
            runs(ctx, n, box);
        }
    },

    /**
     * The code block of a {@code Markdown} (reference/components/markdown.md): a text whose font is its
     * {@code Code font}, its own or inherited, with the line of its declaration.
     */
    CODE_BLOCK {
        @Override
        void check(InlineNode n, InlineNode wrapper, Style s, CellPosition position) {
            if (s.typography().codeFont() == null) {
                throw error(n, ErrorCode.CODE_WITHOUT_FONT, "The code block needs Code font: it is declared in the "
                        + "Typography of the Markdown, of an ancestor or of Styles (For element: Document or For element: Code "
                        + "block), as in Code font: IBM Plex Mono");
            }
            s.typography().setFont(s.typography().codeFont());
            s.declared(PropertyKey.FONT, s.line(PropertyKey.CODE_FONT));
        }

        @Override
        void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
            TEXT.content(builder, ctx, n, box);
        }
    },

    IMAGE {
        @Override
        void check(InlineNode n, InlineNode wrapper, Style s, CellPosition position) {
            validateImage(s, "an Image");
        }

        @Override
        void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
            image(n, box);
        }
    },

    /**
     * A {@code Barcode} (reference/components/barcode.md): an image that is generated from its value
     * ({@link Barcodes}), once its properties are checked.
     */
    BARCODE {
        @Override
        void check(InlineNode n, InlineNode wrapper, Style s, CellPosition position) {
            validateImage(s, "a Barcode");
            validateBarcode(n, s);
        }

        @Override
        void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
            box.setModules(Barcodes.GENERATOR.generate(n, box.style()));
        }
    },

    /**
     * The {@code Cell} of a table (reference/components/table.md): with a value it is a text box and, without one, a
     * block. Its row ({@code wrapper}) gives it its {@code Typography}, as inheritance, and its {@code Break}. Its
     * {@code Vertical} places its content in the height its row leaves over.
     */
    CELL(PropertyReader.CELL, PropertyReader::cell, Set.of("span")) {
        @Override
        Layout layout(InlineNode n, Element kind) {
            return n.getText().isBlank() ? kind.layout() : Layout.INLINE;
        }

        @Override
        void inherited(InlineNode wrapper, Style s) {
            // The Typography of the row is inherited by its cells: it goes before the rules.
            child(wrapper, "typography").map(Nodes::inline).ifPresent(t -> typography(t, s));
        }

        @Override
        void wrapper(InlineNode wrapper, Style s, Box parent) {
            // The row is not an Item: what it gives its cells is inherited (above) or its Break (check).
        }

        @Override
        void check(InlineNode n, InlineNode wrapper, Style s, CellPosition position) {
            // The Break of the row is that of its cells: with it the break falls between rows, as in a grid.
            child(wrapper, "break").map(Nodes::inline).ifPresent(b -> fragmentation(b, s));
            if (s.rowHeader() && position != null && position.group() == CellPosition.HEADER) {
                throw error(s.line("row-header"), ErrorCode.ROW_HEADER_IN_HEADER_ROWS, "Row header has no effect on "
                        + "a cell of Header rows: the header cells are already column headers");
            }
        }

        @Override
        void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
            if (box.style().layout() == Layout.INLINE) {
                cellText(n, box);
                runs(ctx, n, box);
            } else {
                builder.buildContent(ctx, n, box);
            }
        }
    },

    /**
     * A {@code Table} (reference/components/table.md): a grid whose cells {@link TableBuilder} places, with its row
     * groups and its {@code Column align}.
     */
    TABLE(PropertyReader.GRID, PropertyReader::grid, Set.of("header-rows", "body-rows", "footer-rows", "column-align")) {
        @Override
        void check(InlineNode n, InlineNode wrapper, Style s, CellPosition position) {
            if (!s.isDeclared(PropertyKey.grid("column-sizes"))) {
                throw error(n, ErrorCode.TABLE_COLUMN_SIZES_MISSING, "The Table needs Column sizes, its own or from a "
                        + "rule of Styles (For element: Table, For style...): the width of each column, as in "
                        + "Column sizes: * 80");
            }
        }

        @Override
        void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
            // The Row is an ancestor of its cells, with its Style and its Id, for the nested rules: read once per row,
            // not once per cell (reading it goes through the cells of the row).
            Map<InlineNode, Ancestor> rows = new IdentityHashMap<>();
            table(ctx, n, box, (cell, row, at) -> ctx.inside(
                    rows.computeIfAbsent(row, r -> StyleRules.ancestor(r, Element.ROW)),
                    () -> builder.element(ctx, cell, box, row, at)));
        }
    },

    GRID(PropertyReader.GRID, PropertyReader::grid, Set.of()),

    STACK(PropertyReader.STACK, PropertyReader::stack, Set.of()) {
        @Override
        void check(InlineNode n, InlineNode wrapper, Style s, CellPosition position) {
            validateStack(s);
        }
    },

    LIST {
        @Override
        void check(InlineNode n, InlineNode wrapper, Style s, CellPosition position) {
            if (!s.isDeclared("marker")) {
                throw error(n, ErrorCode.LIST_MARKER_MISSING, "The List needs Marker, its own or from a rule of "
                        + "Styles (For element: List, For style...), as in Marker: bullet");
            }
            validateList(s);
        }
    },

    /**
     * A {@code Columns} (reference/components/columns.md): its content goes in its column flow, a generated block
     * box that is its only child and of which each column is a fragment. It isolates margins, so that they
     * do not leave the columns, and its height is not definite: the distribution into
     * columns decides it.
     */
    COLUMNS(PropertyReader.COLUMNS, PropertyReader::columns, Set.of()) {
        @Override
        void check(InlineNode n, InlineNode wrapper, Style s, CellPosition position) {
            validateColumns(s, n.getLine());
        }

        @Override
        void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box columns) {
            Style s = Style.inheriting(columns.style());
            s.setIsolateMargins(true);
            Box flow = new Box(null, Element.COLUMN_FLOW, s, false, n.getLine());
            columns.addChild(flow);
            ctx.definiteHeight.put(flow, false);
            ctx.within(ctx.scope().columns(), () -> builder.buildContent(ctx, n, flow));
        }
    },

    /**
     * A {@code Markdown} (reference/components/markdown.md): the elements that {@link MarkdownBuilder} generates,
     * built as if the author had written them; its formatted texts carry their runs. Its markers and its
     * {@code Table column sizes}, its own or those of its rules, are read by the translation.
     */
    MARKDOWN(Set.of(), null, Set.of("bullet-markers", "number-markers", "table-column-sizes")) {
        @Override
        void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
            noCalls(ctx, box);
            MarkdownBuilder.Translation t = MarkdownBuilder.translate(n, ctx.footnotes, name -> ctx.property(n, name));
            ctx.footnotes.noCalls = null;
            ctx.runs.putAll(t.runs());
            ctx.generatedKinds.putAll(t.kinds());
            ctx.sources.putAll(t.sources());
            ctx.within(ctx.scope().markdown(), () -> {
                for (InlineNode e : t.elements()) {
                    box.addChild(builder.element(ctx, e, box, null, null));
                }
            });
        }
    },

    /**
     * A {@code Figure} (reference/components/figure.md): exactly one element and its {@code Caption}, which goes
     * above or below it ({@code Caption position}) wherever it is written. The figure is not split across pages
     * (a table inside is, and its caption keeps with its first or last part), and it is numbered with the counter of
     * the figures or the tables, with the number written before the caption ({@link Figures}).
     */
    FIGURE {
        @Override
        void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
            List<Node> elements = n.getChildren().stream().filter(h -> isElement(h) && !is(h, "caption")).toList();
            if (elements.size() != 1) {
                throw error(elements.isEmpty() ? n : elements.get(1), ErrorCode.FIGURE_CONTENT, "A Figure contains "
                        + "exactly one element besides its Caption (an Image, a Barcode, a Table or a Block), and this "
                        + "one has " + elements.size());
            }
            Box content = builder.element(ctx, inline(elements.get(0)), box, null, null);
            Box caption = builder.element(ctx, inline(child(n, "caption").orElseThrow()), box, null, null);
            Style s = box.style();
            Figures.validateAlt(n, s, content);
            CaptionPosition position = Figures.captionPosition(s, content);
            s.figure().setCaptionPosition(position);
            s.figure().setCounter(Figures.counter(s, content));
            boolean top = position == CaptionPosition.TOP;
            box.addChild(top ? caption : content);
            box.addChild(top ? content : caption);
            if (content.element() == Element.TABLE) {
                // The table breaks as always: the caption goes with its first or its last part.
                caption.style().breaks().setKeepWithNext(top);
                caption.style().breaks().setKeepWithPrevious(!top);
            } else {
                s.breaks().setKeepTogether(true);
            }
            String number = ctx.figureNumbers.next(s.figure().counter(), Figures.numbered(s),
                    s.typography().numberFormat());
            if (number != null) {
                HeadingNumbers.prefix(box, caption, number);
            }
            ctx.figures.add(box);
        }
    },

    /**
     * A {@code Table of contents} (reference/components/table-of-contents.md): its entries are generated by {@link
     * TableOfContentsBuilder} at the end, with all the headings, the figures and the tables.
     */
    TABLE_OF_CONTENTS {
        @Override
        void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
            builder.buildContent(ctx, n, box);
            ctx.contents.add(box);
        }
    },

    /**
     * A {@code Footnote} (reference/components/footnote.md): its children, read as those of a block. It is laid out
     * at the bottom of a page, apart from the box that holds it: what that box forbids ({@code Break} inside an
     * {@code Overlay}, the columns of a {@code Columns}) does not reach the note, and a call inside it is an error.
     */
    FOOTNOTE {
        @Override
        void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
            ctx.within(ctx.scope().footnote(), () -> builder.buildContent(ctx, n, box));
        }
    },

    /**
     * The horizontal rule of a {@code Markdown} (reference/components/markdown.md): an empty block whose top border is
     * the line, which a {@code For element: Divider} rule changes. The line is what the element is, so it has a
     * thickness by default; the block quote of a {@code Markdown}, like the rest of what it generates, has no look of
     * its own and is a {@link #CONTAINER}.
     */
    DIVIDER {
        @Override
        void defaults(Style s) {
            s.box().setBorder(new Sides<>(Value.pt(DIVIDER_BORDER), Value.pt(0), Value.pt(0), Value.pt(0)));
        }
    },

    /** The rest: {@code Block}, the headings, {@code List item}, the positioned boxes..., with their children. */
    CONTAINER;

    /** Thickness of the line of a divider of a {@code Markdown}, by default, in points. */
    static final double DIVIDER_BORDER = 0.5;

    /** A blank line inside the text: it separates paragraphs (reference/common/typography.md). */
    private static final Pattern BLANK_LINE = Pattern.compile("\\n[ \\t]*\\n");

    /** The properties of the type of the element ({@code Stack}, {@code Grid}...), by canonical name. */
    private final Set<String> properties;
    /** Reads one of {@link #properties}; {@code null} if there are none. */
    private final BiConsumer<Node, Style> reader;
    /**
     * The children that are not properties and that the content of the family reads ({@link TableBuilder},
     * {@link MarkdownBuilder}), by canonical name.
     */
    private final Set<String> contentChildren;

    ElementReader() {
        this(Set.of(), null, Set.of());
    }

    ElementReader(Set<String> properties, BiConsumer<Node, Style> reader, Set<String> contentChildren) {
        this.properties = properties;
        this.reader = reader;
        this.contentChildren = contentChildren;
    }

    /** The family of an element. */
    static ElementReader of(Element kind) {
        if (kind == Element.CODE_BLOCK) {
            return CODE_BLOCK;
        }
        if (kind == Element.DIVIDER) {
            return DIVIDER;
        }
        if (kind.hasText()) {
            return TEXT;
        }
        return switch (kind) {
            case IMAGE -> IMAGE;
            case BARCODE -> BARCODE;
            case CELL -> CELL;
            case TABLE -> TABLE;
            case GRID -> GRID;
            case STACK -> STACK;
            case LIST -> LIST;
            case COLUMNS -> COLUMNS;
            case MARKDOWN -> MARKDOWN;
            case FIGURE -> FIGURE;
            case TABLE_OF_CONTENTS -> TABLE_OF_CONTENTS;
            case FOOTNOTE -> FOOTNOTE;
            default -> CONTAINER;
        };
    }

    /** The layout of the box: that of the element, except in a {@code Cell}. */
    Layout layout(InlineNode n, Element kind) {
        return kind.layout();
    }

    /**
     * The look the engine gives it by default, before inheritance and the rules, which replace it property by
     * property: the line of the divider of a {@code Markdown}.
     */
    void defaults(Style s) {
    }

    /**
     * What its wrapper gives it as inheritance, before the rules are applied: to a cell, the {@code Typography} of its
     * {@code Row}. {@code wrapper} is {@code null} if it has none.
     */
    void inherited(InlineNode wrapper, Style s) {
    }

    /**
     * What its wrapper declares for it, after the rules are applied, so that it wins over them: to an item, the
     * properties of its {@code Item} or its {@code Area}. {@code wrapper} is {@code null} if it has none.
     */
    void wrapper(InlineNode wrapper, Style s, Box parent) {
        if (wrapper != null) {
            item(wrapper, s, parent.style().grid().areaMap());
        }
    }

    /**
     * Claims {@code p} if it belongs to the family: reads it if it is a property of the type of the element
     * ({@code Stack}, {@code Grid}...) and leaves it if its content reads it (the row groups of a {@code Table}, the
     * markers of a {@code Markdown}); {@code false} if it is not the family's.
     */
    boolean property(Node p, Style s) {
        String name = p.getCanonicalName();
        if (properties.contains(name)) {
            reader.accept(p, s);
            return true;
        }
        return contentChildren.contains(name);
    }

    /**
     * Reads {@code p}, an element property that a rule of {@code Styles} gives the element
     * (reference/common/styles.md), as if the element declared it: one of its type ({@code Stack}, {@code Grid}...) or
     * a standalone one. One that its content reads ({@code Column align}, the markers of a {@code Markdown}) is not
     * read here, and {@link BuildContext#property} gives it to the content: {@code false}.
     */
    boolean ruleProperty(Node p, Style s) {
        if (contentChildren.contains(p.getCanonicalName())) {
            return false;
        }
        if (!property(p, s)) {
            PropertyReader.single(p, s);
        }
        return true;
    }

    /** What the family checks of the properties already read, before the box is created. */
    void check(InlineNode n, InlineNode wrapper, Style s, CellPosition position) {
    }

    /** The content of the box: by default, its child elements. */
    void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
        builder.buildContent(ctx, n, box);
    }

    /**
     * The runs of a text box whose text is already read: those that {@link MarkdownBuilder} generated for {@code n},
     * those of its inline Markdown, the values of its styled runs and the family of its code.
     */
    private static void runs(BuildContext ctx, InlineNode n, Box box) {
        box.setRuns(ctx.runs.get(n));
        inlineFormat(ctx, box);
        literalReferences(ctx, box);
        runStyles(ctx, box);
        codeFont(box);
    }

    /**
     * With {@code Markup: markdown} (reference/common/text-formatting.md), the text of the box is inline Markdown:
     * it becomes the joined text, without the marks, and the formatted runs go in {@link Box#formattedRuns()}. Inside
     * a {@code Markdown} it does nothing, because its texts come already interpreted. The text stops being a block:
     * its errors point to the line where it starts.
     */
    private static void inlineFormat(BuildContext ctx, Box box) {
        if (box.style().typography().markup() != Markup.MARKDOWN || ctx.scope().inMarkdown()) {
            return;
        }
        int first = box.textBlock() ? box.textLine() + 1 : box.textLine();
        noCalls(ctx, box);
        List<TextRun> runs = MarkdownBuilder.inline(box.text(), first, ctx.footnotes);
        ctx.footnotes.noCalls = null;
        box.setText(TextRun.text(runs), first, false);
        box.setRuns(TextRun.formatted(runs) ? runs : null);
    }

    /**
     * A reference or a page number inside another pair of braces, {@code {{page}}}, is text
     * (reference/common/references.md): the text of the box keeps it without that pair and with
     * {@link Reference#LITERAL_BRACE} as its brace. The texts that are Markdown come with it already done
     * ({@link MarkdownBuilder}), run by run.
     */
    private static void literalReferences(BuildContext ctx, Box box) {
        if (box.text() == null || box.style().typography().markup() == Markup.MARKDOWN || ctx.scope().inMarkdown()) {
            return;
        }
        String text = Reference.literal(box.text());
        if (!text.equals(box.text())) {
            box.setText(text, box.textLine(), box.textBlock());
        }
    }

    /**
     * Where a footnote call is not allowed (reference/components/footnote.md): in a {@code Page overlay}, which is
     * repeated on every page; in a heading, whose text is copied to the tables of contents and the bookmarks; and in
     * another footnote. Set while the text of {@code box} is read.
     */
    private static void noCalls(BuildContext ctx, Box box) {
        BuildContext.Scope scope = ctx.scope();
        ctx.footnotes.noCalls = scope.inPageOverlay() ? "a Page overlay, which is repeated on every page"
                : scope.inHeading() ? "a heading, whose text is copied to the tables of contents and the bookmarks"
                : scope.inFootnote() ? "a Footnote" : null;
    }

    /**
     * The styled runs of the text, {@code [text]{name}} (reference/common/text-formatting.md), get here the values of
     * the {@code For style} rule of their name, which only the builder knows ({@link StyleRules#runStyle}): what the
     * rule declares replaces what the box has, and the family of {@code Font} goes in the family of the run. Its code
     * gets its family afterwards, in {@link #codeFont}. The box keeps the style of each name, with its lines.
     */
    private static void runStyles(BuildContext ctx, Box box) {
        List<TextRun> runs = box.formattedRuns();
        if (runs == null || runs.stream().noneMatch(r -> r.format().style() != null)) {
            return;
        }
        Map<String, Style> styles = new HashMap<>();
        Map<String, RunStyle> values = new HashMap<>();
        List<TextRun> resolved = new ArrayList<>(runs.size());
        int index = 0;
        for (TextRun r : runs) {
            RunStyle named = r.format().style();
            if (named != null) {
                String name = named.name();
                if (!styles.containsKey(name)) {
                    Style s = ctx.rules.runStyle(name, box.textLineAt(index), ctx.ancestors());
                    styles.put(name, s);
                    values.put(name, runStyle(name, s));
                }
                RunStyle style = values.get(name);
                TextFormat f = r.format().withStyle(style);
                if (!f.code() && style.family() != null) {
                    f = f.withFamily(style.family());
                }
                r = new TextRun(r.text(), f);
            }
            resolved.add(r);
            index += r.text().length();
        }
        box.setRuns(List.copyOf(resolved));
        box.setRunStyles(styles);
    }

    /** The values of a styled run: those that its rules declare in {@code s}; {@code null} the others. */
    private static RunStyle runStyle(String name, Style s) {
        TypographyStyle t = s.typography();
        return new RunStyle(name,
                s.isDeclared(PropertyKey.COLOR) ? t.color() : null,
                s.isDeclared(PropertyKey.FONT) ? t.font() : null,
                s.isDeclared(PropertyKey.CODE_FONT) ? t.codeFont() : null,
                s.isDeclared(PropertyKey.FONT_WEIGHT) ? t.fontWeight() : null,
                s.isDeclared(PropertyKey.FONT_STYLE) ? t.fontStyle() : null,
                s.isDeclared(PropertyKey.internal("letter-spacing")) ? t.letterSpacing() : null,
                s.isDeclared(PropertyKey.internal("case")) ? t.textCase() : null,
                s.isDeclared(PropertyKey.internal("small-caps")) ? t.smallCaps() : null);
    }

    /**
     * Inline code (reference/common/text-formatting.md) is written with the family of {@code Code font}, its own
     * or inherited, which only the box knows: its runs receive it here. Without it, it is an error, with the line of
     * the code.
     */
    private static void codeFont(Box box) {
        List<TextRun> runs = box.formattedRuns();
        if (runs == null || runs.stream().noneMatch(r -> r.format().code())) {
            return;
        }
        List<TextRun> resolved = new ArrayList<>(runs.size());
        int index = 0;
        for (TextRun r : runs) {
            if (r.format().code()) {
                // In a styled run, the Code font of its style, if it declares one.
                RunStyle style = r.format().style();
                String family = style != null && style.codeFamily() != null ? style.codeFamily()
                        : box.style().typography().codeFont();
                if (family == null) {
                    throw error(box.textLineAt(index), ErrorCode.CODE_WITHOUT_FONT, "The code needs Code font: "
                            + "it is declared in the Typography of the text, of an ancestor or of Styles (For element: Document), "
                            + "as in Code font: IBM Plex Mono");
                }
                r = new TextRun(r.text(), r.format().withFamily(family));
            }
            resolved.add(r);
            index += r.text().length();
        }
        box.setRuns(List.copyOf(resolved));
    }

    /**
     * The path of an {@code Image} (reference/components/image.md), relative to {@code images/}. That the file exists
     * and is an image is checked during layout, because the resources folder is needed.
     */
    private static void image(InlineNode n, Box box) {
        String path = n.getText().trim();
        if (path.isEmpty()) {
            throw error(n, ErrorCode.IMAGE_UNKNOWN, "Image needs the path of the image, relative to the "
                    + "images/ folder of the resources: Image: logos/acme.png");
        }
        box.setImage(path);
    }

    /**
     * The text of a {@code Text} or a {@code Paragraph}: in its value or in {@code Content}, one of the two, and
     * without blank lines, which would split it into paragraphs (reference/components/text.md). In preformatted text, a
     * blank line is content and is allowed, except in a {@code Paragraph}, which is always a single paragraph.
     */
    private static void text(InlineNode n, Box box) {
        Optional<Node> content = child(n, "content");
        boolean value = !n.getText().isBlank();
        if (value && content.isPresent()) {
            throw error(content.get(), ErrorCode.TEXT_CONTENT, n.getName() + " has the text in the value and in Content "
                    + "(line " + n.getLine() + "): it goes in only one");
        }
        if (!value && content.isEmpty()) {
            throw error(n, ErrorCode.TEXT_CONTENT, n.getName() + " needs its text: in the value ("
                    + n.getName() + ": ...) or in a Content >> block");
        }
        Node source = content.orElse(n);
        font(n, source, box);
        boolean paragraph = box.element() == Element.PARAGRAPH;
        if (paragraph || !box.style().typography().preformatted().enabled()) {
            // Blank lines at the start or at the end separate nothing.
            String text = source.getText();
            Matcher blank = BLANK_LINE.matcher(text)
                    .region(text.length() - text.stripLeading().length(), text.stripTrailing().length());
            if (blank.find()) {
                // The block starts on the line after Content; the blank line, after the line break found.
                int line = source.getLine() + 2 + (int) text.substring(0, blank.start()).chars().filter(c -> c == '\n').count();
                throw error(line, ErrorCode.TEXT_BLANK_LINE, paragraph
                        ? "A Paragraph is a single paragraph: the blank line is not needed, or each one goes in its own Paragraph"
                        : "A Text has no paragraphs: the blank line is not needed, or each paragraph goes in its own "
                        + "Paragraph (in a Block if they go together). In preformatted text (Preformatted: true) it is allowed");
            }
        }
        box.setText(source.getText(), source.getLine(), source instanceof TextNode);
    }
}
