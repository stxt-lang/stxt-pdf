package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Position;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Box elements of the document (reference/common/structure.md) and the ones the engine generates: the marker of each
 * {@code List item} (reference/components/list.md), the column flow of each {@code Columns}
 * (reference/components/columns.md), the rows of a table, the code blocks of a {@code Markdown}, the table of contents
 * entries and the footnote area. Each one gives its {@code Layout} and its {@code Position}, which are not
 * declared, and the meaning used by the {@code For element} rules, the bookmarks and the PDF tags.
 *
 * <p>The structure (which children each element accepts) is only in the schema; {@code ElementTest}
 * checks that this enum has the same elements as the schema.</p>
 */
public enum Element {

    /**
     * A section of the document (reference/components/document.md): a block that is the root of its own layout, with
     * its {@code Page}. It is only a child of the root of the document.
     */
    SECTION("Section", Layout.BLOCK, Position.STATIC, false, false, 0),
    /** A box that stacks its children vertically (reference/components/block.md). */
    BLOCK("Block", Layout.BLOCK, Position.STATIC, true, false, 0),
    /**
     * A box that places its items in lines along its main axis and distributes among them the space that is left
     * over or missing (reference/components/stack.md).
     */
    STACK("Stack", Layout.STACK, Position.STATIC, true, false, 0),
    /** A box that places its items in columns and rows (reference/components/grid.md). */
    GRID("Grid", Layout.GRID, Position.STATIC, true, false, 0),
    /**
     * Standalone text that is not a paragraph of the content: a label, a line of a header, the pieces of a heading
     * (reference/components/text.md).
     */
    TEXT("Text", Layout.INLINE, Position.STATIC, true, true, 0),
    /** A paragraph of the content (reference/components/text.md). */
    PARAGRAPH("Paragraph", Layout.INLINE, Position.STATIC, true, true, 0),
    /** A heading of level 1, the highest: a block of one or more {@code Text} (reference/components/heading.md). */
    HEADING_1("Heading 1", Layout.BLOCK, Position.STATIC, true, false, 1),
    /** A heading of level 2 (reference/components/heading.md). */
    HEADING_2("Heading 2", Layout.BLOCK, Position.STATIC, true, false, 2),
    /** A heading of level 3 (reference/components/heading.md). */
    HEADING_3("Heading 3", Layout.BLOCK, Position.STATIC, true, false, 3),
    /** A heading of level 4 (reference/components/heading.md). */
    HEADING_4("Heading 4", Layout.BLOCK, Position.STATIC, true, false, 4),
    /** A heading of level 5 (reference/components/heading.md). */
    HEADING_5("Heading 5", Layout.BLOCK, Position.STATIC, true, false, 5),
    /** A heading of level 6, the lowest (reference/components/heading.md). */
    HEADING_6("Heading 6", Layout.BLOCK, Position.STATIC, true, false, 6),
    /**
     * A list: a block that stacks its {@code List item} children, each with a marker that the engine generates
     * (reference/components/list.md).
     */
    LIST("List", Layout.BLOCK, Position.STATIC, true, false, 0),
    /** An item of a {@code List}: a block that carries its {@link #MARKER} on the left. */
    LIST_ITEM("List item", Layout.BLOCK, Position.STATIC, false, false, 0),
    /** A table is laid out as a grid with explicit placement (reference/components/table.md). */
    TABLE("Table", Layout.GRID, Position.STATIC, true, false, 0),
    /** Without a value, a block; with a value, a text box ({@link BoxTreeBuilder} changes the {@code Layout}). */
    CELL("Cell", Layout.BLOCK, Position.STATIC, false, false, 0),
    /**
     * Block in columns (reference/components/columns.md): its content goes in the column flow, {@link
     * #COLUMN_FLOW}.
     */
    COLUMNS("Columns", Layout.COLUMNS, Position.STATIC, true, false, 0),
    /**
     * Content in Markdown (reference/components/markdown.md): a block whose children are the elements that
     * {@link MarkdownBuilder} generates from its {@code Content}.
     */
    MARKDOWN("Markdown", Layout.BLOCK, Position.STATIC, true, false, 0),
    /**
     * Image from the resources folder (reference/components/image.md): a box without children with the path in {@link
     * Box#image()}.
     */
    IMAGE("Image", Layout.IMAGE, Position.STATIC, true, false, 0),
    /**
     * A QR code or a barcode (reference/components/barcode.md): an image that the engine generates from its value,
     * with the modules in {@link Box#modules()}. It is laid out and painted as an {@code Image}.
     */
    BARCODE("Barcode", Layout.IMAGE, Position.STATIC, true, false, 0),
    /**
     * A figure with a caption (reference/components/figure.md): a block with exactly one element ({@code Image},
     * {@code Barcode}, {@code Table} or {@code Block}) and its {@link #CAPTION}, above or below it, numbered with the
     * counter of the figures or the tables.
     */
    FIGURE("Figure", Layout.BLOCK, Position.STATIC, true, false, 0),
    /** The caption of a {@link #FIGURE}: a text, with its number before it if the figure is numbered. */
    CAPTION("Caption", Layout.INLINE, Position.STATIC, false, true, 0),
    /**
     * Automatic table of contents (reference/common/references.md): a block whose children, generated when the tree is
     * built, are one entry per heading ({@link #TOC_1} ... {@link #TOC_6}), or per figure or table.
     */
    TABLE_OF_CONTENTS("Table of contents", Layout.BLOCK, Position.STATIC, true, false, 0),
    /**
     * A footnote (reference/components/footnote.md): a block with the content of a note, which the paginator places at
     * the bottom of the page where its call ({@code [^id]}) falls. It is written anywhere in its section, but it is
     * not a child of the box that holds it: it hangs from the footnote area of the section ({@link #FOOTNOTES}).
     */
    FOOTNOTE("Footnote", Layout.BLOCK, Position.STATIC, false, false, 0),
    /**
     * A positioned block that is placed in the flow and then shifted by its {@code Offset}, keeping its space
     * (reference/components/frame-and-overlays.md).
     */
    FRAME("Frame", Layout.BLOCK, Position.FRAME, true, false, 0),
    /**
     * A positioned block out of the flow, placed by its {@code Offset} in its reference container
     * (reference/components/frame-and-overlays.md).
     */
    OVERLAY("Overlay", Layout.BLOCK, Position.OVERLAY, false, false, 0),
    /**
     * A positioned block placed in the page area and painted on the pages that {@code Pages} says: headers, footers,
     * watermarks (reference/components/frame-and-overlays.md).
     */
    PAGE_OVERLAY("Page overlay", Layout.BLOCK, Position.PAGE_OVERLAY, false, false, 0),
    /**
     * The footnote area of a section (reference/components/footnote.md), generated by the engine: a child of the
     * section, out of its flow, whose children are its {@code Footnote} boxes in document order. The paginator lays
     * out at the bottom of each page the notes called on it, and the {@code For element} rules name it
     * ({@code Footnote area}) to style the rule that separates it from the text, its spacing and the typography the notes
     * inherit.
     */
    FOOTNOTES("Footnote area", Layout.BLOCK, Position.FOOTNOTES, false, false, 0, true),
    /** The generated box of the marker of a {@code List item}: it is not written in the document. */
    MARKER(null, Layout.INLINE, Position.STATIC, false, false, 0),
    /**
     * The {@code Row} of a {@code Table} with {@code Id} or {@code Box} (reference/components/table.md): a band
     * that is painted under its cells. It is not named in the {@code For element} rules.
     */
    ROW(null, Layout.BLOCK, Position.STATIC, false, false, 0),
    /**
     * The column flow of a {@code Columns}, generated by the engine: a block with the children of the {@code Columns},
     * which is its only child. Each column is one of its fragments (reference/components/columns.md).
     */
    COLUMN_FLOW(null, Layout.BLOCK, Position.STATIC, false, false, 0),
    /**
     * A code block of a {@link #MARKDOWN} (reference/components/markdown.md): a generated preformatted
     * text. It is not written in the document, but the {@code For element} rules name it ({@code Code block}) to
     * style it apart from the {@code Text} (reference/common/styles.md).
     */
    CODE_BLOCK("Code block", Layout.INLINE, true),
    /**
     * The entries of a {@link #TABLE_OF_CONTENTS}, one per heading level (those of a table of contents of figures or
     * tables are {@link #TOC_1}): a generated {@code Stack} with the title, the leader and the page number. They are
     * not written in the document, but the {@code For element} rules name them ({@code TOC 2}) to style them
     * (reference/components/table-of-contents.md).
     */
    TOC_1("TOC 1", Layout.STACK),
    /** The entry of a {@link #TABLE_OF_CONTENTS} for a {@code Heading 2}. */
    TOC_2("TOC 2", Layout.STACK),
    /** The entry of a {@link #TABLE_OF_CONTENTS} for a {@code Heading 3}. */
    TOC_3("TOC 3", Layout.STACK),
    /** The entry of a {@link #TABLE_OF_CONTENTS} for a {@code Heading 4}. */
    TOC_4("TOC 4", Layout.STACK),
    /** The entry of a {@link #TABLE_OF_CONTENTS} for a {@code Heading 5}. */
    TOC_5("TOC 5", Layout.STACK),
    /** The entry of a {@link #TABLE_OF_CONTENTS} for a {@code Heading 6}. */
    TOC_6("TOC 6", Layout.STACK),
    /** The title of a table of contents entry: the text of the heading. */
    TOC_TITLE(null, Layout.INLINE, Position.STATIC, false, false, 0),
    /** The leader (dots or line) between the title and the number of an entry: an empty box that is painted. */
    TOC_LEADER(null, Layout.BLOCK, Position.STATIC, false, false, 0),
    /** The page number of a table of contents entry: {@code {page of: ...}} of the heading. */
    TOC_PAGE(null, Layout.INLINE, Position.STATIC, false, false, 0);

    /**
     * The elements that are named, by their canonical name ({@code list-item}): the written ones and the table of
     * contents entries.
     */
    private static final Map<String, Element> BY_NAME = Arrays.stream(values())
            .filter(e -> e.label != null)
            .collect(Collectors.toUnmodifiableMap(Element::canonicalName, Function.identity()));

    private final String label;
    private final Layout layout;
    private final Position position;
    private final boolean item;
    private final boolean text;
    private final int headingLevel;
    /** Generated by the engine, but with a name for the {@code For element} rules. */
    private final boolean selectorOnly;

    Element(String label, Layout layout, Position position, boolean item, boolean text, int headingLevel) {
        this(label, layout, position, item, text, headingLevel, false);
    }

    /** A generated element that the {@code For element} rules can name. */
    Element(String label, Layout layout) {
        this(label, layout, false);
    }

    /** A generated element that the {@code For element} rules can name; {@code text}: it carries text. */
    Element(String label, Layout layout, boolean text) {
        this(label, layout, Position.STATIC, false, text, 0, true);
    }

    private Element(String label, Layout layout, Position position, boolean item, boolean text, int headingLevel,
                    boolean selectorOnly) {
        this.label = label;
        this.layout = layout;
        this.position = position;
        this.item = item;
        this.text = text;
        this.headingLevel = headingLevel;
        this.selectorOnly = selectorOnly;
    }

    /** The element with that canonical name ({@code heading-2}); empty if it is not a box element. */
    public static Optional<Element> of(String canonicalName) {
        return Optional.ofNullable(BY_NAME.get(canonicalName));
    }

    /** The elements that are written in the document, in the order of the reference. */
    public static List<Element> written() {
        return Arrays.stream(values()).filter(Element::isWritten).toList();
    }

    /**
     * The elements that a {@code For element} rule can name: the written ones, the code blocks, the table of
     * contents entries and the footnote area.
     */
    public static List<Element> selectable() {
        return Arrays.stream(values()).filter(e -> e.label != null).toList();
    }

    /** Whether it is written in the document (it is not generated). */
    public boolean isWritten() {
        return label != null && !selectorOnly;
    }

    /** The entry of a {@link #TABLE_OF_CONTENTS} for the headings of that level (1 to 6). */
    public static Element tocEntry(int level) {
        return values()[TOC_1.ordinal() + level - 1];
    }

    /**
     * {@return the name as it is written ({@code List item}); {@code null} in the generated boxes that the
     * {@code For element} rules cannot name}
     */
    public String label() {
        return label;
    }

    /**
     * {@return the name in canonical form, as {@code Node.getCanonicalName()} gives it ({@code list-item});
     * {@code null} when there is no {@link #label()}}
     */
    public String canonicalName() {
        return label == null ? null : label.toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    public Layout layout() {
        return layout;
    }

    public Position position() {
        return position;
    }

    /** Whether it can be the element of an {@code Item} or an {@code Area}: it is in flow and not part of another. */
    public boolean canBeItem() {
        return item;
    }

    /**
     * Whether it carries the text in the value or in {@code Content} ({@code Text}, {@code Paragraph} and
     * {@link #CODE_BLOCK}).
     */
    public boolean hasText() {
        return text;
    }

    /** {@return the level of the heading, 1 to 6 ({@code Heading 2} is 2); 0 if it is not a heading} */
    public int headingLevel() {
        return headingLevel;
    }

    /**
     * What a box means for the reader of the document: the role with which the PDF tags it
     * (reference/common/pdf.md) and with which the painting and the overflow check treat it. Several elements
     * share one ({@code Paragraph} and the code block are paragraphs; {@code Block}, {@code Stack} or {@code Page
     * overlay}, a block with no further meaning), so a new element only has to state its own ({@link
     * #role()}).
     */
    public enum Role {
        /** The anonymous root of the document, which has no element. */
        DOCUMENT,
        /** A {@code Section}: a part of the document with its own pages. */
        SECTION,
        /** A heading, {@code Heading 1} to {@code Heading 6}, at its level. */
        HEADING,
        /** A text of a single paragraph: {@code Paragraph} and the code block. */
        PARAGRAPH,
        /** A {@code Text}: a paragraph or, inside a heading, part of its text. */
        TEXT,
        /** A list, whose items carry their marker on the left (reference/components/list.md). */
        LIST,
        /** A {@code List item}: its marker, as the label of the item, and its content. */
        LIST_ITEM,
        /** A {@code Table} (reference/components/table.md). */
        TABLE,
        /** The band of a row of a table: it has no content, it is only painted. */
        ROW,
        /** A {@code Cell} of a table: a header cell or a data cell. */
        CELL,
        /** An image, read from a file or generated ({@code Image} and {@code Barcode}). */
        FIGURE,
        /**
         * A {@code Figure} element (reference/components/figure.md): an image, a table or a block with its caption.
         * With an image or a block it is the figure of the tagged PDF, with the caption inside; with a table, the
         * table carries the caption and the element has no meaning of its own.
         */
        FIGURE_GROUP,
        /** The caption of a {@code Figure}: a text read with what it describes. */
        CAPTION,
        /** A table of contents ({@code Table of contents}, reference/components/table-of-contents.md). */
        TABLE_OF_CONTENTS,
        /** A footnote ({@code Footnote}, reference/components/footnote.md): read after the paragraph that calls it. */
        NOTE,
        /**
         * The footnote area of a section: it has no meaning of its own, its notes are read where they are called,
         * and it is painted (the rule, the background) without being content.
         */
        FOOTNOTES,
        /** An entry of a table of contents ({@code TOC 1} to {@code TOC 6}): its title, its leader and its page. */
        TOC_ENTRY,
        /** A piece of text inside another: the title of a table of contents entry. */
        SPAN,
        /** The page number of a table of contents entry: a piece of text that is not read in its link. */
        PAGE_NUMBER,
        /**
         * The leader of a table of contents entry: it has no text, it is painted
         * (reference/components/table-of-contents.md) and it is not content.
         */
        LEADER,
        /**
         * The column flow of a {@code Columns}: it has no meaning of its own, its content is that of the {@code
         * Columns}.
         */
        FLOW,
        /** A box with no further meaning. */
        BLOCK
    }

    /** {@return the role of the box ({@link Role}): {@link Role#HEADING} in every heading level} */
    public Role role() {
        if (headingLevel > 0) {
            return Role.HEADING;
        }
        return switch (this) {
            case SECTION -> Role.SECTION;
            case PARAGRAPH, CODE_BLOCK -> Role.PARAGRAPH;
            case TEXT -> Role.TEXT;
            case LIST -> Role.LIST;
            case LIST_ITEM -> Role.LIST_ITEM;
            case TABLE -> Role.TABLE;
            case ROW -> Role.ROW;
            case CELL -> Role.CELL;
            case IMAGE, BARCODE -> Role.FIGURE;
            case FIGURE -> Role.FIGURE_GROUP;
            case CAPTION -> Role.CAPTION;
            case TABLE_OF_CONTENTS -> Role.TABLE_OF_CONTENTS;
            case FOOTNOTE -> Role.NOTE;
            case FOOTNOTES -> Role.FOOTNOTES;
            case TOC_1, TOC_2, TOC_3, TOC_4, TOC_5,
                 TOC_6 -> Role.TOC_ENTRY;
            case TOC_TITLE -> Role.SPAN;
            case TOC_PAGE -> Role.PAGE_NUMBER;
            case TOC_LEADER -> Role.LEADER;
            case COLUMN_FLOW -> Role.FLOW;
            default -> Role.BLOCK;
        };
    }
}
