package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private String image;
    private String anchor;
    private String number;
    private int textLine;
    private boolean textBlock;
    private Box marker;
    private int headerRows;
    private int footerRows;
    private List<Box> rows = new ArrayList<>();
    private boolean closed;

    Box(String id, Element element, Style style, boolean anonymous, int line) {
        this.id = id;
        this.element = element;
        this.style = style;
        this.anonymous = anonymous;
        this.line = line;
    }

    // --- Reading ---------------------------------------------------------------------------------------

    /** The {@code Id} of the box; {@code null} if it has none. */
    public String id() {
        return id;
    }

    /**
     * Element it comes from; {@code null} in the root. It gives the meaning: {@code For element}
     * rules, bookmarks and PDF tags.
     */
    public Element element() {
        return element;
    }

    public Style style() {
        return style;
    }

    /** Whether it is the anonymous root, which represents the page area. */
    public boolean anonymous() {
        return anonymous;
    }

    /** Line of the document it comes from, for error messages. */
    public int line() {
        return line;
    }

    /**
     * The children, in document order; the list cannot be changed. In the root of the document, also the
     * sections, which hang from it without being its children (reference/components/document.md).
     */
    public List<Box> children() {
        return closed ? children : Collections.unmodifiableList(children);
    }

    /** The parent box; {@code null} in the root and in the box of a section, which is the root of its layout. */
    public Box parent() {
        return parent;
    }

    /** Inline content, if the box contains text directly; {@code null} otherwise. */
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
     * Path of the image of an {@code Image} (reference/components/image.md), relative to the
     * {@code images/} folder of the resources; {@code null} in the other boxes.
     */
    public String image() {
        return image;
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
     * marker plus the space that separates it from the content ({@code "3. "}, {@code "• "}). It is not
     * among the children: the layout of the item hangs it on its left. {@code null} if the box has
     * no marker.
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

    // --- Writing, only in tree while it is built ------------------------------------------------------

    void addChild(Box child) {
        writable();
        child.parent = this;
        children.add(child);
    }

    /**
     * Hangs the box of a section from the root of the document without being its child: it sets no parent
     * (reference/components/document.md).
     */
    void addSection(Box section) {
        writable();
        children.add(section);
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

    void setImage(String image) {
        writable();
        this.image = image;
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

    /** Text of a heading for the table of contents: that of its {@code Text}, joined and with spaces normalized. */
    public String headingText() {
        return children.stream().filter(Box::hasText).map(b -> b.text.strip())
                .collect(Collectors.joining(" ")).replaceAll("\\s+", " ");
    }

    /** Children that take part in the flow of the parent (in flow and {@code Frame}). */
    public List<Box> inFlowChildren() {
        if (inFlowChildren != null) {
            return inFlowChildren;
        }
        return children.stream().filter(h -> !h.style.isOutOfFlow()).toList();
    }

    /**
     * Whether the box isolates margins, that is, whether it stops their merging (reference/components/block.md): the
     * page area (the root), the {@code Overlay} and {@code Page overlay}, the items of stack and grid, the boxes with
     * {@code Isolate margins: true} and the rotated ones in flow.
     */
    public boolean isolatesMargins() {
        if (parent == null || style.isolateMargins() || style.isOutOfFlow() || style.rotatesInFlow()) {
            return true;
        }
        Layout l = parent.style.layout();
        return l == Layout.STACK || l == Layout.GRID;
    }

    /**
     * Reference container of an out-of-flow box (reference/common/box.md): for an {@code Overlay},
     * the closest {@code Frame}, {@code Overlay} or {@code Page overlay} ancestor; if there is none,
     * or if the box is a {@code Page overlay}, the root, which represents the page area.
     */
    public Box referenceContainer() {
        Box b = parent;
        boolean pageOverlay = style.position() == Position.PAGE_OVERLAY;
        // A box rotated in flow is one too, like the positioned ones (reference/common/layers-and-rotation.md).
        while (b.parent != null && (pageOverlay || !(b.style.isPositioned() || b.style.rotatesInFlow()))) {
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
     * What the box means ({@link Element#role()}); the anonymous root of the document is
     * {@link Element.Role#DOCUMENT}.
     */
    public Element.Role role() {
        return element == null ? Element.Role.DOCUMENT : element.role();
    }

    /** Whether it is an {@code Image}. */
    public boolean isImage() {
        return element == Element.IMAGE;
    }

    /** Whether it is the generated box of the marker of a {@code List item}. */
    public boolean isMarker() {
        return element == Element.MARKER;
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
