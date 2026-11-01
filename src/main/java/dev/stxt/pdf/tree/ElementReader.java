package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.TextNode;
import dev.stxt.pdf.model.Markup;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.TextRun;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import static dev.stxt.pdf.tree.PropertyReader.stack;
import static dev.stxt.pdf.tree.PropertyReader.fragmentation;
import static dev.stxt.pdf.tree.PropertyReader.grid;
import static dev.stxt.pdf.tree.PropertyReader.item;
import static dev.stxt.pdf.tree.PropertyReader.columns;
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

    /** {@code Text}, {@code Paragraph} and the code block of a {@code Markdown}: the text, with its runs. */
    TEXT {
        @Override
        void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
            text(n, box);
            box.setRuns(ctx.runs.get(n));
            inlineFormat(ctx, box);
            codeFont(box);
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
     * block. Its row ({@code wrapper}) gives it its {@code Typography}, as inheritance, and its {@code Break}.
     */
    CELL {
        @Override
        Layout layout(InlineNode n, Element kind) {
            return n.getText().isBlank() ? kind.layout() : Layout.INLINE;
        }

        @Override
        void wrapper(InlineNode wrapper, Style s, Box parent) {
            // The Typography of the row is inherited by its cells: it goes before the rules.
            child(wrapper, "typography").map(Nodes::inline).ifPresent(t -> typography(t, s));
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
                box.setRuns(ctx.runs.get(n));
                inlineFormat(ctx, box);
                codeFont(box);
            } else {
                builder.buildContent(ctx, n, box);
            }
        }
    },

    /** A {@code Table} (reference/components/table.md): a grid whose cells {@link TableBuilder} places. */
    TABLE {
        @Override
        boolean property(Node p, Style s) {
            if (!PropertyReader.GRID.contains(p.getCanonicalName())) {
                return false;
            }
            grid(p, s);
            return true;
        }

        @Override
        void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
            table(ctx, n, box, (cell, row, at) -> builder.element(ctx, cell, box, row, at));
        }
    },

    GRID {
        @Override
        boolean property(Node p, Style s) {
            if (!PropertyReader.GRID.contains(p.getCanonicalName())) {
                return false;
            }
            grid(p, s);
            return true;
        }
    },

    STACK {
        @Override
        boolean property(Node p, Style s) {
            if (!PropertyReader.STACK.contains(p.getCanonicalName())) {
                return false;
            }
            stack(p, s);
            return true;
        }

        @Override
        void check(InlineNode n, InlineNode wrapper, Style s, CellPosition position) {
            validateStack(s);
        }
    },

    LIST {
        @Override
        void check(InlineNode n, InlineNode wrapper, Style s, CellPosition position) {
            validateList(s);
        }
    },

    /**
     * A {@code Columns} (reference/components/columns.md): its content goes in its column flow, a generated block
     * box that is its only child and of which each column is a fragment. It isolates margins, so that they
     * do not leave the columns, and its height is not definite: the distribution into
     * columns decides it.
     */
    COLUMNS {
        @Override
        boolean property(Node p, Style s) {
            if (!PropertyReader.COLUMNS.contains(p.getCanonicalName())) {
                return false;
            }
            columns(p, s);
            return true;
        }

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
            boolean outer = ctx.insideColumns;
            String outerUnbreakable = ctx.unbreakable;
            ctx.insideColumns = true;
            ctx.unbreakable = null;
            builder.buildContent(ctx, n, flow);
            ctx.insideColumns = outer;
            ctx.unbreakable = outerUnbreakable;
        }
    },

    /**
     * A {@code Markdown} (reference/components/markdown.md): the elements that {@link MarkdownBuilder} generates,
     * built as if the author had written them; its formatted texts carry their runs.
     */
    MARKDOWN {
        @Override
        void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
            MarkdownBuilder.Translation t = MarkdownBuilder.translate(n);
            ctx.runs.putAll(t.runs());
            ctx.codeBlocks.addAll(t.codeBlocks());
            ctx.insideMarkdown = true;
            for (InlineNode e : t.elements()) {
                box.addChild(builder.element(ctx, e, box, null, null));
            }
            ctx.insideMarkdown = false;
        }
    },

    /**
     * A {@code Table of contents} (reference/components/table-of-contents.md): its entries are generated by {@link
     * TableOfContentsBuilder} at the end, with all the headings.
     */
    TABLE_OF_CONTENTS {
        @Override
        void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
            builder.buildContent(ctx, n, box);
            ctx.contents.add(box);
        }
    },

    /** The rest: {@code Block}, the headings, {@code List item}, the positioned boxes..., with their children. */
    CONTAINER;

    /** A blank line inside the text: it separates paragraphs (reference/common/typography.md). */
    private static final Pattern BLANK_LINE = Pattern.compile("\\n[ \\t]*\\n");

    /** The family of an element. */
    static ElementReader of(Element kind) {
        if (kind == Element.CODE_BLOCK) {
            return CODE_BLOCK;
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
            case TABLE_OF_CONTENTS -> TABLE_OF_CONTENTS;
            default -> CONTAINER;
        };
    }

    /** The layout of the box: that of the element, except in a {@code Cell}. */
    Layout layout(InlineNode n, Element kind) {
        return kind.layout();
    }

    /**
     * What its wrapper gives it before the rules are applied: to an item, the properties of its {@code Item} or its
     * {@code Area}; {@code wrapper} is {@code null} if it has none.
     */
    void wrapper(InlineNode wrapper, Style s, Box parent) {
        if (wrapper != null) {
            item(wrapper, s, parent.style().grid().areas());
        }
    }

    /**
     * Reads {@code p} if it is a property of the type of the element ({@code Stack}, {@code Grid}...); {@code false}
     * if not.
     */
    boolean property(Node p, Style s) {
        return false;
    }

    /** What the family checks of the properties already read, before the box is created. */
    void check(InlineNode n, InlineNode wrapper, Style s, CellPosition position) {
    }

    /** The content of the box: by default, its child elements. */
    void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
        builder.buildContent(ctx, n, box);
    }

    /**
     * With {@code Markup: markdown} (reference/common/text-formatting.md), the text of the box is inline Markdown:
     * it becomes the joined text, without the marks, and the formatted runs go in {@link Box#formattedRuns()}. Inside
     * a {@code Markdown} it does nothing, because its texts come already interpreted. The text stops being a block:
     * its errors point to the line where it starts.
     */
    private static void inlineFormat(BuildContext ctx, Box box) {
        if (box.style().typography().markup() != Markup.MARKDOWN || ctx.insideMarkdown) {
            return;
        }
        int first = box.textBlock() ? box.textLine() + 1 : box.textLine();
        List<TextRun> runs = MarkdownBuilder.inline(box.text(), first);
        box.setText(TextRun.text(runs), first, false);
        box.setRuns(TextRun.formatted(runs) ? runs : null);
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
        String family = box.style().typography().codeFont();
        List<TextRun> resolved = new ArrayList<>(runs.size());
        int index = 0;
        for (TextRun r : runs) {
            if (r.format().code()) {
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
