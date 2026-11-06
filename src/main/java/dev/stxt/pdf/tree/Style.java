package dev.stxt.pdf.tree;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Marker;
import dev.stxt.pdf.model.Pages;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.Rotation;
import dev.stxt.pdf.model.Side;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.TextAlign;
import dev.stxt.pdf.model.Value;

/**
 * Specified properties of a box, with the default values of reference/common/structure.md.
 *
 * <p>The properties go in groups, each in its own class: the document ones ({@link TypographyStyle},
 * {@link BoxStyle} and {@link BreakStyle}, reference/common/structure.md) and the ones of an element kind
 * ({@link StackStyle}, {@link GridStyle}, {@link ItemStyle}, {@link ListStyle}, {@link ColumnsStyle},
 * {@link ImageStyle}, {@link FigureStyle} and {@link TableOfContentsStyle}). An algorithm reads only its own:
 * {@code style.stack().direction()}, {@code style.box().margin()}. {@code Style} itself keeps the properties that
 * define the element (reference/common/structure.md and reference/components/frame-and-overlays.md: the layout and the
 * position, which come from the element, {@code Offset}, {@code Layer}, {@code Pages}, {@code Isolate margins}, the
 * rotation, {@code Link}, {@code Row header}, {@code In table of contents}, {@code Side}, {@code Clear} and
 * {@code Drop cap}) and the line where each property was declared, with its document ({@link SourceLine}).</p>
 *
 * <p>There is no cascade: each box declares its properties. Only {@link TypographyStyle} is
 * <strong>inherited</strong>: each box starts with a copy of its parent's ({@link #inheriting}).</p>
 *
 * <p>The values of the properties with a closed set are enums of {@code model} ({@link TextAlign},
 * {@link Marker}...): the names of their constants match the document values in uppercase and with the hyphen as an
 * underscore ({@code lower-alpha} is {@code LOWER_ALPHA}), which is how {@link PropertyReader} parses them.
 * {@link Layout} and {@link Position} are not document values: they come from the element ({@code Text} is
 * {@code INLINE}; {@code Overlay}, a {@code BLOCK} {@code OVERLAY}).</p>
 *
 * <p>Only {@code tree} writes a style, while {@link BoxTreeBuilder} builds the tree; at the end it is closed with its
 * box ({@link Box#close}) and from then on it is only read, also from several threads.</p>
 */
public final class Style extends StyleGroup {

    /** The fewest lines a drop cap takes up ({@code Drop cap}, reference/components/text.md). */
    public static final int MIN_DROP_CAP = 2;
    /** The most lines a drop cap takes up. */
    public static final int MAX_DROP_CAP = 5;

    private TypographyStyle typography = new TypographyStyle();
    private final BoxStyle box = new BoxStyle();
    private final BreakStyle breaks = new BreakStyle();
    private final StackStyle stack = new StackStyle();
    private final GridStyle grid = new GridStyle();
    private final ItemStyle item = new ItemStyle();
    private final ListStyle list = new ListStyle();
    private final ColumnsStyle columns = new ColumnsStyle();
    private final ImageStyle image = new ImageStyle();
    private final FigureStyle figure = new FigureStyle();
    private final TableOfContentsStyle tableOfContents = new TableOfContentsStyle();

    // --- Properties of the element -----------------------------------------------------------------
    private Layout layout = Layout.BLOCK;
    private Position position = Position.STATIC;
    private Sides<Value> offset = Sides.all(Value.AUTO);
    /** {@code null} = auto. */
    private Integer layer;
    /** Only in a {@code Page overlay}: the pages it appears on. */
    private Pages pages = Pages.ALL;
    private boolean isolateMargins;
    /**
     * {@code Rotate} and {@code Rotate origin} (reference/common/layers-and-rotation.md), in {@code Frame},
     * {@code Overlay}, {@code Page overlay}, {@code Image} and the in-flow boxes that {@link #rotatesInFlow()} admits;
     * {@code null} = no rotation. Not inherited.
     */
    private Rotation rotation;
    /**
     * Target of the {@code Link} of the box ({@link dev.stxt.pdf.model.Links}, reference/common/links.md);
     * {@code null} = no link. Not inherited: what is inside a box with a link is clickable through it, but does not
     * carry a link of its own.
     */
    private String link;
    /**
     * {@code Row header} of a {@code Cell}: it is the header of its row (reference/components/table.md). Not
     * inherited.
     */
    private boolean rowHeader;
    /**
     * {@code In table of contents} of a heading or a {@code Figure}: whether it has an entry in tables of contents
     * (reference/components/table-of-contents.md). Not inherited.
     */
    private boolean inTableOfContents = true;
    /**
     * {@code Side} of an {@code Image}, a {@code Barcode} or a {@code Block} in block flow (reference/components/block.md):
     * the box goes to that side and the text of the siblings that follow it runs beside it; {@code null} = in the flow.
     * Not inherited.
     */
    private Side side;
    /** {@code Clear}: the box goes below the side boxes before it (reference/components/block.md). Not inherited. */
    private boolean clear;
    /**
     * {@code Drop cap} of a {@code Paragraph} (reference/components/text.md): the lines its first letter takes up;
     * 0 = no drop cap. Not inherited.
     */
    private int dropCap;
    /** The names of {@code Named color} with which its colors are read; all the boxes share those of the root. */
    private NamedColors namedColors = NamedColors.NONE;

    /** Only {@code tree} creates it: the style of each box comes from {@link BoxTreeBuilder}. */
    Style() {
    }

    /** A style whose colors can be the names of {@code Named color} (reference/common/styles.md). */
    Style(NamedColors namedColors) {
        this.namedColors = namedColors;
    }

    /** The names of {@code Named color} that its color properties can write. */
    NamedColors namedColors() {
        return namedColors;
    }

    // --- Groups ------------------------------------------------------------------------------------
    /** The {@code Typography} group: it is inherited (reference/common/typography.md). */
    public TypographyStyle typography() {
        return typography;
    }

    /** The {@code Box} group: the box model (reference/common/box.md). */
    public BoxStyle box() {
        return box;
    }

    /** The {@code Break} group: the breaks (reference/common/breaks.md). */
    public BreakStyle breaks() {
        return breaks;
    }

    /** The properties of {@code Stack} (reference/components/stack.md). */
    public StackStyle stack() {
        return stack;
    }

    /** The properties of {@code Grid} and {@code Table} (reference/components/grid.md). */
    public GridStyle grid() {
        return grid;
    }

    /**
     * Those of {@code Item} and {@code Area}, or the cell of a table (reference/components/stack.md and
     * reference/components/grid.md).
     */
    public ItemStyle item() {
        return item;
    }

    /** Those of {@code List} and {@code List item} (reference/components/list.md). */
    public ListStyle list() {
        return list;
    }

    /** Those of {@code Columns} (reference/components/columns.md). */
    public ColumnsStyle columns() {
        return columns;
    }

    /** Those of {@code Image} (reference/components/image.md). */
    public ImageStyle image() {
        return image;
    }

    /** Those of {@code Figure} (reference/components/figure.md). */
    public FigureStyle figure() {
        return figure;
    }

    /** Those of {@code Table of contents} (reference/components/table-of-contents.md). */
    public TableOfContentsStyle tableOfContents() {
        return tableOfContents;
    }

    // --- Properties of the element -----------------------------------------------------------------
    public Layout layout() {
        return layout;
    }

    void setLayout(Layout v) {
        writable();
        layout = v;
    }

    public Position position() {
        return position;
    }

    void setPosition(Position v) {
        writable();
        position = v;
    }

    public Sides<Value> offset() {
        return offset;
    }

    void setOffset(Sides<Value> v) {
        writable();
        offset = v;
    }

    public Integer layer() {
        return layer;
    }

    void setLayer(Integer v) {
        writable();
        layer = v;
    }

    public Pages pages() {
        return pages;
    }

    void setPages(Pages v) {
        writable();
        pages = v;
    }

    public boolean isolateMargins() {
        return isolateMargins;
    }

    void setIsolateMargins(boolean v) {
        writable();
        isolateMargins = v;
    }

    public Rotation rotation() {
        return rotation;
    }

    /**
     * Whether the box rotates in flow (reference/common/layers-and-rotation.md): a {@code Block}, a {@code Text}, a
     * {@code Paragraph} or an {@code Image} with {@code Rotate}, which takes up the rectangle that encloses it rotated.
     */
    public boolean rotatesInFlow() {
        return rotation != null && position == Position.STATIC;
    }

    void setRotation(Rotation v) {
        writable();
        rotation = v;
    }

    public String link() {
        return link;
    }

    void setLink(String v) {
        writable();
        link = v;
    }

    public boolean rowHeader() {
        return rowHeader;
    }

    void setRowHeader(boolean v) {
        writable();
        rowHeader = v;
    }

    public boolean inTableOfContents() {
        return inTableOfContents;
    }

    void setInTableOfContents(boolean v) {
        writable();
        inTableOfContents = v;
    }

    /** The side the box goes to; {@code null} if it is in the flow. */
    public Side side() {
        return side;
    }

    void setSide(Side v) {
        writable();
        side = v;
    }

    /** Whether the box is a side box: it goes to a side and the text runs beside it. */
    public boolean isSideBox() {
        return side != null;
    }

    /**
     * Why a box is not split across pages (reference/common/breaks.md), apart from an {@code Image} or a
     * {@code Barcode}, which have no children: how the messages name it and the error of a {@code Keep together} or
     * an {@code Edges} declared on it, {@code null} where the model does not check them.
     */
    enum Unsplittable {
        ROTATED_BOX("a rotated box", ErrorCode.BREAK_IN_ROTATED),
        ROTATED_FRAME("a rotated Frame", ErrorCode.BREAK_IN_ROTATED),
        CLIPPED("a box with Overflow: clip", ErrorCode.BREAK_IN_CLIPPED),
        // A side box goes whole to a side of the text (reference/components/block.md).
        SIDE_BOX("a box with Side", ErrorCode.BREAK_IN_SIDE_BOX),
        OVERLAY("an Overlay", null),
        PAGE_OVERLAY("a Page overlay", null);

        private final String what;
        private final ErrorCode code;

        Unsplittable(String what, ErrorCode code) {
            this.what = what;
            this.code = code;
        }

        String what() {
            return what;
        }

        ErrorCode code() {
            return code;
        }
    }

    /**
     * Why the box is not split across pages, or {@code null} if it is split. With more than one reason, the first one
     * in the order of {@link Unsplittable}: the rotation, {@code Overflow: clip}, {@code Side} and the position.
     */
    Unsplittable unsplittable() {
        if (rotatesInFlow()) {
            return Unsplittable.ROTATED_BOX;
        }
        if (position == Position.FRAME && rotation != null) {
            return Unsplittable.ROTATED_FRAME;
        }
        if (box.clips()) {
            return Unsplittable.CLIPPED;
        }
        if (isSideBox()) {
            return Unsplittable.SIDE_BOX;
        }
        return switch (position) {
            case OVERLAY -> Unsplittable.OVERLAY;
            case PAGE_OVERLAY -> Unsplittable.PAGE_OVERLAY;
            default -> null;
        };
    }

    public boolean clear() {
        return clear;
    }

    void setClear(boolean v) {
        writable();
        clear = v;
    }

    /** Lines the drop cap of a {@code Paragraph} takes up; 0 without a drop cap. */
    public int dropCap() {
        return dropCap;
    }

    void setDropCap(int v) {
        writable();
        dropCap = v;
    }

    // --- Lines of the properties --------------------------------------------------------------------
    /**
     * Line of each declared property, by its key ({@link PropertyKey}): the canonical name ({@code "height"}; those of
     * {@code Stack}, {@code Grid} and {@code Item}, with a prefix: {@code "grid.row-sizes"}). It is of the document or,
     * if the property comes from one of its rules, of the separate styles document. Those of {@link #LINED_PROPERTIES}
     * are inherited with their value, to point to where it was declared.
     */
    private final Map<String, SourceLine> lines = new HashMap<>();
    /**
     * The separate styles document of the rule that is being read now ({@link #readingStyles}); {@code null} if what
     * is being read is of the document.
     */
    private SourceLine readingStyles;

    /**
     * Properties that are inherited with their line, to point to it in errors: those of the font (the ones of
     * {@code Typography} that choose it, also the code one), {@code Line height}, which set the height of the lines
     * (reference/common/overflow.md), {@code Color}, for the contrast of the text (reference/common/pdf.md), and
     * {@code Hyphenate}, for a language without patterns (reference/common/typography.md).
     */
    public static final List<String> LINED_PROPERTIES = List.of(PropertyKey.FONT, PropertyKey.FONT_WEIGHT,
            PropertyKey.FONT_STYLE, PropertyKey.FONT_SIZE, PropertyKey.MIN_FONT_SIZE, PropertyKey.LINE_HEIGHT,
            PropertyKey.CODE_FONT, PropertyKey.COLOR, PropertyKey.HYPHENATE);

    /** New style with the default values and the inheritable properties of the parent. */
    static Style inheriting(Style parent) {
        Style s = new Style();
        if (parent != null) {
            s.namedColors = parent.namedColors;
            s.typography = new TypographyStyle(parent.typography);
            for (String p : LINED_PROPERTIES) {
                if (parent.lines.containsKey(p)) {
                    s.lines.put(p, parent.lines.get(p));
                }
            }
        }
        return s;
    }

    /**
     * The properties declared from now on come from a rule of the separate styles document of {@code source} (its line
     * number does not count), and their line is of that document; with {@code null}, they are of the document again.
     */
    void readingStyles(SourceLine source) {
        writable();
        readingStyles = source;
    }

    /** Records the line where a property is declared: of a styles document if one of its rules is being read. */
    void declared(String property, int line) {
        declared(property, readingStyles != null ? readingStyles.at(line) : SourceLine.of(line));
    }

    /** Records the line where a property is declared, with its document. */
    void declared(String property, SourceLine line) {
        writable();
        lines.put(property, line);
    }

    /** Whether the property was declared (in this box or, those of {@link #LINED_PROPERTIES}, in an ancestor). */
    public boolean isDeclared(String property) {
        return lines.containsKey(property);
    }

    /**
     * Line where the property was declared, with its document.
     *
     * @throws IllegalStateException if it was not declared
     */
    public SourceLine line(String property) {
        SourceLine line = lines.get(property);
        if (line == null) {
            throw new IllegalStateException("Property not declared: " + property);
        }
        return line;
    }

    public boolean isPositioned() {
        return position != Position.STATIC;
    }

    public boolean isOutOfFlow() {
        return position == Position.OVERLAY || position == Position.PAGE_OVERLAY || position == Position.FOOTNOTES;
    }

    /** Closes the style with its box ({@link Box#close}): it no longer changes. */
    void close() {
        super.close();
        typography.close();
        box.close();
        breaks.close();
        stack.close();
        grid.close();
        item.close();
        list.close();
        columns.close();
        image.close();
        figure.close();
        tableOfContents.close();
    }
}
