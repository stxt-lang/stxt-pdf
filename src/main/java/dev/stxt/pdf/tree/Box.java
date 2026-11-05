package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Modules;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.Stroke;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A box of the box tree (reference/common/structure.md): each box element of the document
 * ({@code Block}, {@code Stack}, {@code Text}...) is one; the root, anonymous, is the page area. A
 * {@code Text} holds its text directly.
 *
 * <p>It holds no geometry: the result of the layout, by pages, is the
 * {@link dev.stxt.pdf.fragment.Fragment}s.</p>
 *
 * <p>Equality is by identity: the layout cache uses the box as a key.</p>
 *
 * <p><strong>Writing:</strong> the fields are private and only the {@code tree} package writes them, with the
 * {@code set...} methods and {@link #addChild}, while {@link BoxTreeBuilder} builds the tree. When it finishes,
 * {@link #close()} closes it entirely: the children become an immutable list, the in-flow ones are computed
 * once and any later write is an {@link IllegalStateException}. Outside {@code tree} a box is only
 * read.</p>
 *
 * <p><strong>Threads:</strong> a closed tree does not change. It can be read from several threads if it is handed
 * over safely (a {@code Future}, an executor, a {@code volatile} field...).</p>
 */
public final class Box {

    private final String id;
    private final Element element;
    private final Style style;
    private final boolean anonymous;
    private final int line;
    /** An {@code ArrayList} while it is built; on closing, an immutable list. */
    private List<Box> children = new ArrayList<>();
    /** The in-flow children, computed on closing; {@code null} before. */
    private List<Box> inFlowChildren;
    private Box parent;
    private String text;
    private List<TextRun> runs;
    /** The styles of its styled runs, by name ({@link #runStyle}). */
    private Map<String, Style> runStyles = Map.of();
    private String image;
    private Modules modules;
    private String anchor;
    private String number;
    private int textLine;
    private boolean textBlock;
    private Box marker;
    private int headerRows;
    private int footerRows;
    private List<Box> rows = new ArrayList<>();
    private Sides<Stroke> mergedBorder;
    private boolean closed;

    Box(String id, Element element, Style style, boolean anonymous, int line) {
        this.id = id;
        this.element = element;
        this.style = style;
        this.anonymous = anonymous;
        this.line = line;
    }

    // --- Reading ---------------------------------------------------------------------------------------

    /** {@return the {@code Id} of the box; {@code null} if it has none} */
    public String id() {
        return id;
    }

    /**
     * {@return the element it comes from; {@code null} in the root. It gives the meaning: {@code For element}
     * rules, bookmarks and PDF tags}
     */
    public Element element() {
        return element;
    }

    public Style style() {
        return style;
    }

    /** {@return whether it is the anonymous root, which represents the page area} */
    public boolean anonymous() {
        return anonymous;
    }

    /** {@return the line of the document it comes from, for error messages} */
    public int line() {
        return line;
    }

    /** {@return the children, in document order; the list cannot be changed} */
    public List<Box> children() {
        return closed ? children : Collections.unmodifiableList(children);
    }

    /** {@return the parent box; {@code null} in the root} */
    public Box parent() {
        return parent;
    }

    /** {@return the inline content, if the box contains text directly; {@code null} otherwise} */
    public String text() {
        return text;
    }

    /**
     * Runs of the text with their format (reference/common/text-formatting.md), whose joined text is
     * {@link #text()}; {@code null} if the text has no formatting. See {@link #runs()}.
     */
    public List<TextRun> formattedRuns() {
        return runs;
    }

    /**
     * The style of the styled runs of the text with that name ({@code [text]{name}},
     * reference/common/text-formatting.md): what its {@code For style} rules declare, each property with its line, for
     * the errors that the layout and the painting find in a run (a family that does not exist, a low contrast).
     *
     * @throws IllegalStateException if the text has no styled run with that name
     */
    public Style runStyle(String name) {
        Style s = runStyles.get(name);
        if (s == null) {
            throw new IllegalStateException("The text of " + describe() + " has no styled run " + name);
        }
        return s;
    }

    /**
     * Path of the image of an {@code Image} (reference/components/image.md), relative to the
     * {@code images/} folder of the resources; {@code null} in the other boxes.
     */
    public String image() {
        return image;
    }

    /**
     * The drawing of a generated image, such as a {@code Barcode} (reference/components/barcode.md), which the tree
     * generates from its value when it is built ({@link ImageGenerator}); {@code null} in the other boxes, also in an
     * {@code Image}, whose file is {@link #image()}.
     */
    public Modules modules() {
        return modules;
    }

    /**
     * Name by which a table of contents (reference/components/table-of-contents.md) refers to the box when it has
     * no {@code Id}: that of a heading listed by a {@code Table of contents}. {@code null} in the rest.
     */
    public String anchor() {
        return anchor;
    }

    /**
     * Number of the box in the document, as {@code {number of: id}} gives it
     * (reference/common/references.md); {@code null} if it has none.
     */
    public String number() {
        return number;
    }

    /** Line of the {@code Text} node. */
    public int textLine() {
        return textLine;
    }

    /** The text is a block ({@code Text >>}): its content starts on the next line. */
    public boolean textBlock() {
        return textBlock;
    }

    /**
     * Box of the marker of a {@code List item} (reference/components/list.md), generated by the engine:
     * a text box ({@link Element#MARKER}) with the typography of the item and the text of the
     * marker plus the space that separates it from the content ({@code "3. "}, {@code "• "}) or, with
     * {@code Marker: image}, an image box ({@link #isImage()}, with its file in {@link #image()} and its {@code Alt}
     * and {@code Height} in {@code style().list()}). It is not among the children: the layout of the item hangs it
     * on its left. {@code null} if the box has no marker.
     */
    public Box marker() {
        return marker;
    }

    /**
     * Rows of the grid of a {@code Table} that belong to its {@code Header rows} (reference/components/table.md): the
     * first ones. 0 in the other boxes.
     */
    public int headerRows() {
        return headerRows;
    }

    /**
     * Rows of the grid of a {@code Table} that belong to its {@code Footer rows}: the last ones. 0 in the other boxes.
     */
    public int footerRows() {
        return footerRows;
    }

    /**
     * The boxes of the rows of a {@code Table} that have {@code Id} or {@code Box}
     * (reference/components/table.md), in order; the row of each one is that of its {@code Area}. They are not
     * children of the table, which only has cells, but they have it as parent. Empty in the other boxes.
     */
    public List<Box> rows() {
        return rows;
    }

    /**
     * The border a cell of a table with {@code Merge borders} declares, side by side with its color
     * (reference/components/table.md#merged-borders). The table draws the lines between its cells with it, and the
     * {@code Border} of the cell's style is then only the room the layout leaves for them, which the cell does not
     * paint. {@code null} in the other boxes.
     */
    public Sides<Stroke> mergedBorder() {
        return mergedBorder;
    }

    // --- Writing, only in tree while it is built ------------------------------------------------------

    void addChild(Box child) {
        writable();
        child.parent = this;
        children.add(child);
    }

    void setText(String text, int line, boolean block) {
        writable();
        this.text = text;
        this.textLine = line;
        this.textBlock = block;
    }

    /** Sets the formatted runs of the text; {@code null} if it has no formatting. */
    void setRuns(List<TextRun> runs) {
        writable();
        this.runs = runs;
    }

    /** Sets the styles of the styled runs of the text, by name. */
    void setRunStyles(Map<String, Style> styles) {
        writable();
        this.runStyles = Map.copyOf(styles);
    }

    /** Writes {@code prefix} before the text; if the text has formatting, as a run without formatting. */
    void prependText(String prefix) {
        writable();
        text = prefix + text;
        if (runs != null) {
            List<TextRun> prefixed = new ArrayList<>(runs.size() + 1);
            prefixed.add(new TextRun(prefix, TextFormat.PLAIN));
            prefixed.addAll(runs);
            runs = List.copyOf(prefixed);
        }
    }

    /**
     * Writes {@code prefix} before the text, run by run: the number of a footnote (reference/components/footnote.md).
     * The text without formatting becomes a run after them.
     */
    void prependRuns(List<TextRun> prefix) {
        writable();
        List<TextRun> prefixed = new ArrayList<>(prefix.size() + (runs != null ? runs.size() : 1));
        prefixed.addAll(prefix);
        prefixed.addAll(runs != null ? runs : TextRun.plain(text));
        text = TextRun.text(prefixed);
        runs = List.copyOf(prefixed);
    }

    void setImage(String image) {
        writable();
        this.image = image;
    }

    void setModules(Modules modules) {
        writable();
        this.modules = modules;
    }

    void setAnchor(String anchor) {
        writable();
        this.anchor = anchor;
    }

    void setNumber(String number) {
        writable();
        this.number = number;
    }

    /** Sets the marker of the item, which has the box as parent without being its child. */
    void setMarker(Box marker) {
        writable();
        marker.parent = this;
        this.marker = marker;
    }

    void setHeaderRows(int rows) {
        writable();
        this.headerRows = rows;
    }

    void setFooterRows(int rows) {
        writable();
        this.footerRows = rows;
    }

    void addRow(Box row) {
        writable();
        row.parent = this;
        rows.add(row);
    }

    void setMergedBorder(Sides<Stroke> border) {
        writable();
        this.mergedBorder = border;
    }

    /**
     * Closes the box, its marker and its whole subtree: the children become an immutable list, the in-flow
     * ones are computed once and they can no longer be written.
     */
    void close() {
        if (closed) {
            return;
        }
        children = List.copyOf(children);
        inFlowChildren = children.stream().filter(h -> !h.style.isOutOfFlow()).toList();
        rows = List.copyOf(rows);
        closed = true;
        style.close();
        runStyles.values().forEach(Style::close);
        children.forEach(Box::close);
        rows.forEach(Box::close);
        if (marker != null) {
            marker.close();
        }
    }

    private void writable() {
        if (closed) {
            throw new IllegalStateException("The box is already closed: the tree does not change after it is built ("
                    + describe() + ")");
        }
    }

    // --- Queries ---------------------------------------------------------------------------------------

    /**
     * How a reference names it (reference/common/references.md): its {@code Id} or, if it has none, its {@link
     * #anchor}; it can be {@code null}.
     */
    public String key() {
        return id != null ? id : anchor;
    }

    /** Level of a heading ({@code Heading 1} ... {@code Heading 6}); 0 if it is not one. */
    public int headingLevel() {
        return element == null ? 0 : element.headingLevel();
    }

    /**
     * The {@code Caption} of a {@code Figure} (reference/components/figure.md), the child that carries its text;
     * {@code null} in the other boxes.
     */
    public Box caption() {
        for (Box child : children) {
            if (child.element() == Element.CAPTION) {
                return child;
            }
        }
        return null;
    }

    /**
     * Text of a heading for the table of contents, the bookmarks and the running heads: that of its {@code Text},
     * joined and with spaces normalized (a forced line break is a space too). Of a {@code Figure}, the text of its
     * caption, the only child with text.
     */
    public String headingText() {
        // An image inside the text (reference/common/text-formatting.md) is not read: its place is a space.
        return children.stream().filter(Box::hasText).map(b -> b.text.strip())
                .collect(Collectors.joining(" ")).replaceAll("[\\s\\u2028\\uFFFC]+", " ").strip();
    }

    /** Children that take part in the flow of the parent (in flow and {@code Frame}). */
    public List<Box> inFlowChildren() {
        if (inFlowChildren != null) {
            return inFlowChildren;
        }
        return children.stream().filter(h -> !h.style.isOutOfFlow()).toList();
    }

    /**
     * Whether it is the root of a layout, which represents the page area: the box of a {@code Section}
     * (reference/components/document.md). So is a box without a parent: the root of the document, which is not laid
     * out and is the reference container of its {@code Page overlay} boxes, and the root of a tree without sections,
     * which a layout algorithm receives as it is.
     */
    public boolean isLayoutRoot() {
        return parent == null || element == Element.SECTION;
    }

    /**
     * Whether the box isolates margins, that is, whether it stops their merging (reference/components/block.md): the
     * page area (the root of the layout), the {@code Overlay} and {@code Page overlay}, the items of stack and grid,
     * the boxes with {@code Isolate margins: true} and the rotated ones in flow.
     */
    public boolean isolatesMargins() {
        if (isLayoutRoot() || style.isolateMargins() || style.isOutOfFlow() || style.rotatesInFlow()) {
            return true;
        }
        Layout l = parent.style.layout();
        return l == Layout.STACK || l == Layout.GRID;
    }

    /**
     * Reference container of an out-of-flow box (reference/common/box.md): for an {@code Overlay},
     * the closest {@code Frame}, {@code Overlay} or {@code Page overlay} ancestor; if there is none,
     * or if the box is a {@code Page overlay}, the root of its layout, which represents the page area.
     */
    public Box referenceContainer() {
        Box b = parent;
        boolean pageOverlay = style.position() == Position.PAGE_OVERLAY;
        // A box rotated in flow is one too, like the positioned ones (reference/common/layers-and-rotation.md).
        while (!b.isLayoutRoot() && (pageOverlay || !(b.style.isPositioned() || b.style.rotatesInFlow()))) {
            b = b.parent;
        }
        return b;
    }

    /** Line of the document where character {@code index} of the text is. */
    public int textLineAt(int index) {
        if (!textBlock) {
            return textLine;
        }
        int line = textLine + 1;
        for (int i = 0; i < index; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    /**
     * {@return what the box means ({@link Element#role()}); the anonymous root of the document is
     * {@link Element.Role#DOCUMENT}}
     */
    public Element.Role role() {
        return element == null ? Element.Role.DOCUMENT : element.role();
    }

    /**
     * Whether it is an image: an {@code Image}, with its file in {@link #image()}, a generated image such as a
     * {@code Barcode}, with its drawing in {@link #modules()}, or the marker of a {@code List item} with
     * {@code Marker: image}, with its file in {@link #image()}. The first two are laid out the same way; the marker
     * is placed by the layout of its item and painted like the others.
     */
    public boolean isImage() {
        return element == Element.IMAGE || element == Element.BARCODE || element == Element.MARKER && image != null;
    }

    /** Whether it is the generated box of the marker of a {@code List item}. */
    public boolean isMarker() {
        return element == Element.MARKER;
    }

    /**
     * Whether the box can run beside the side boxes of its parent (reference/components/block.md) instead of going
     * below them: a text box that is not preformatted, or a box in block flow whose children can (a {@code Block}, a
     * heading, a {@code Markdown}, a list and its items), so that the text inside it runs beside them too. Not a box
     * with background or border, which would be painted over the side box; not a side box itself, a cleared one
     * ({@code Clear}), a rotated one or one with {@code Overflow: clip}, which are placed whole; and not a table of
     * contents or a container, whose entries or items are not text that shortens its lines. A list only runs beside
     * side boxes on the right: its markers hang at its left, where a side box on the left would be
     * ({@link dev.stxt.pdf.layout.block.BlockLayout} decides that with the side boxes in sight).
     */
    public boolean wrapsBesideSideBoxes() {
        if (style.isSideBox() || style.clear() || style.position() != Position.STATIC || style.rotation() != null
                || style.box().clips() || style.box().background() != null
                || Insets.resolve(style.box().border(), 0).horizontal() + Insets.resolve(style.box().border(), 0).vertical() > 0) {
            return false;
        }
        if (style.layout() == Layout.INLINE) {
            return !style.typography().preformatted().enabled();
        }
        Element.Role r = role();
        return style.layout() == Layout.BLOCK && (r == Element.Role.BLOCK || r == Element.Role.HEADING
                || r == Element.Role.LIST || r == Element.Role.LIST_ITEM);
    }

    /** Runs of the text: those of {@link #formattedRuns()} or, without formatting, a single run with all the text. */
    public List<TextRun> runs() {
        return runs != null ? runs : TextRun.plain(text);
    }

    public boolean hasText() {
        return text != null && !text.isBlank();
    }

    /**
     * The box in a message, the only way to name it: {@code main (line 12)} if it has an {@code Id},
     * {@code at line 12} if not, and {@code (page area)} the anonymous root.
     */
    public String describe() {
        if (anonymous) {
            return "(page area)";
        }
        return id != null ? id + " (line " + line + ")" : "at line " + line;
    }

    @Override
    public String toString() {
        return "Box[" + describe() + ", " + style.layout() + "]";
    }
}
