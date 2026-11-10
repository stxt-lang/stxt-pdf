package dev.stxt.pdf.fragment;

import dev.stxt.pdf.model.Colors;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Paint;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Rotation;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.ColumnsStyle;
import dev.stxt.pdf.tree.Element;
import dev.stxt.pdf.tree.Style;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A fragment where it ended up on its page (reference/implementation/api.md): its boxes in points from the top left
 * corner of the paper, y growing downward, which are the coordinates the page is painted with. A {@link Fragment} only
 * says where it is inside the content box of its parent; whoever wants to draw over a laid out box (a stamp, a
 * signature, the filling of the blank it leaves) asks here instead of adding up the borders and the paddings of its
 * ancestors.
 *
 * <p>They come from {@link Page#placed()}, the roots of a page, and from
 * {@code LaidOutDocument.placed(id)}, the fragments of the box with an {@code Id}; {@link #children()} goes down from
 * each one. Immutable, like the fragment it places.</p>
 *
 * <p>The boxes are the unrotated ones: inside a box with {@code Rotate}
 * (reference/common/layers-and-rotation.md), where its content is before the rotation, which is what its children
 * refer to. {@link #bounds()} and {@link #painted()} say where it ends up on the paper, with the rotations applied.
 * {@code Overflow: clip} and the rounded corners are not taken into account: they hide content, they do not move
 * it.</p>
 */
public final class Placed {

    /** No rotation. It is shared: nobody modifies a transform once a {@code Placed} holds it. */
    private static final AffineTransform UNROTATED = new AffineTransform();

    private final int page;
    private final Fragment fragment;
    private final Rect borderBox;
    /** The rotations of its ancestors, from the page inwards, composed; its own is not included. */
    private final AffineTransform outer;

    /**
     * A fragment at ({@code x}, {@code y}) of the paper: the position of its border box or, in a box rotated in flow,
     * of the rectangle that encloses it.
     */
    private Placed(int page, Fragment fragment, double x, double y, AffineTransform outer) {
        this.page = page;
        this.fragment = fragment;
        this.borderBox = fragment.borderBox(x, y);
        this.outer = outer;
    }

    /**
     * The roots of a page, as it is painted: the flow, at the corner of the page area; the footnote area, if the page
     * has one, and the {@code Page overlay} boxes, relative to it.
     */
    static List<Placed> of(Page page) {
        Rect area = page.pageSetup().pageArea();
        List<Placed> roots = new ArrayList<>();
        roots.add(new Placed(page.number(), page.content(), area.x(), area.y(), UNROTATED));
        if (page.footnotes() != null) {
            Fragment notes = page.footnotes();
            roots.add(new Placed(page.number(), notes, area.x() + notes.x(), area.y() + notes.y(), UNROTATED));
        }
        for (Fragment overlay : page.pageOverlays()) {
            roots.add(new Placed(page.number(), overlay, area.x() + overlay.x(), area.y() + overlay.y(), UNROTATED));
        }
        return List.copyOf(roots);
    }

    /** {@return the number of the page it is on, from 1 ({@link Page#number()})} */
    public int page() {
        return page;
    }

    /** {@return the fragment: the piece of its box that falls on this page} */
    public Fragment fragment() {
        return fragment;
    }

    /**
     * {@return its border box on the paper, unrotated: in a box rotated in flow, the box itself and not the rectangle
     * that encloses it ({@link Fragment#turned()})}
     */
    public Rect borderBox() {
        return borderBox;
    }

    /**
     * {@return its content box on the paper: the border box minus the borders and the paddings of this fragment. The
     * positions of its children and of its lines are relative to it}
     */
    public Rect contentBox() {
        return borderBox.shrink(fragment.border()).shrink(fragment.padding());
    }

    /**
     * {@return the rectangle of the paper that encloses its border box as it is painted, with its rotation and those
     * of its ancestors; without rotations, {@link #borderBox()}}
     */
    public Rect bounds() {
        return envelope(inner(), borderBox);
    }

    /**
     * {@return whether it takes part in the flow of its parent: {@code false} in an {@code Overlay}, a
     * {@code Page overlay} and the footnote area, which are placed apart from it (a {@code Frame} is in the flow,
     * moved)}
     */
    public boolean inFlow() {
        return !fragment.box().style().isOutOfFlow();
    }

    /**
     * {@return its child fragments, placed, in tree order: those of its flow and, after them, the {@code Overlay}
     * boxes it resolves}
     */
    public List<Placed> children() {
        if (fragment.children().isEmpty()) {
            return List.of();
        }
        Rect content = contentBox();
        AffineTransform inner = inner();
        List<Placed> children = new ArrayList<>(fragment.children().size());
        for (Fragment child : fragment.children()) {
            // A Page overlay is of the page: it does not turn with the box that declares it.
            boolean ofThePage = child.box().style().position() == Position.PAGE_OVERLAY;
            children.add(new Placed(page, child, content.x() + child.x(), content.y() + child.y(),
                    ofThePage ? UNROTATED : inner));
        }
        return List.copyOf(children);
    }

    /**
     * The columns of this fragment of a {@code Columns} (reference/components/columns.md), from left to right: all of
     * them, also those the content did not reach. Each one is as tall as the content box of the fragment, whatever its
     * content takes up; {@link Column#flow()} says what it has.
     *
     * @return the columns; empty if the box is not a {@code Columns}
     */
    public List<Column> columns() {
        Style style = fragment.box().style();
        if (style.layout() != Layout.COLUMNS) {
            return List.of();
        }
        ColumnsStyle columns = style.columns();
        Rect content = contentBox();
        int count = columns.countIn(content.width());
        double width = columns.widthIn(content.width());
        // The layout fills them in order: the k-th flow is that of the k-th column.
        List<Placed> flows = children().stream()
                .filter(child -> child.fragment().box().role() == Element.Role.FLOW).toList();
        List<Column> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Rect box = new Rect(content.x() + i * (width + columns.columnGap()), content.y(), width, content.height());
            result.add(new Column(box, i < flows.size() ? flows.get(i) : null));
        }
        return List.copyOf(result);
    }

    /**
     * The rectangle of the paper that encloses what this fragment and what it contains in its flow paint, with the
     * rotations applied: the boxes with a border or a background that is not fully transparent, the images and the
     * codes, the line boxes of the text, the lines of the tables and those between columns, and the leaders of a table
     * of contents. What goes over the flow ({@code Overlay}, {@code Page overlay}) does not count, unless it is asked
     * of the overlay itself. It tells where the content of a box really ends on a page: a box with neither border nor
     * background takes up room down to the bottom of its border box and paints nothing there.
     *
     * @return the rectangle; empty if nothing is painted
     */
    public Optional<Rect> painted() {
        double[] reach = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY};
        addPainted(reach);
        return reach[0] > reach[2] || reach[1] > reach[3] ? Optional.empty()
                : Optional.of(new Rect(reach[0], reach[1], reach[2] - reach[0], reach[3] - reach[1]));
    }

    /** Widens {@code reach} (left, top, right, bottom) with what the fragment and its flow paint. */
    private void addPainted(double[] reach) {
        AffineTransform inner = inner();
        Box box = fragment.box();
        Style style = box.style();
        Rect content = contentBox();
        Insets border = fragment.border();
        // The border of a cell with merged borders is only room: its table draws the lines (rules).
        boolean paintsBorder = box.mergedBorder() == null
                && (border.top() > 0 || border.right() > 0 || border.bottom() > 0 || border.left() > 0);
        if (paintsBorder) {
            add(reach, inner, borderBox);
        } else if (visible(style.box().background())) {
            add(reach, inner, borderBox.shrink(border));
        }
        if (box.isImage() || box.role() == Element.Role.LEADER) {
            add(reach, inner, content);
        }
        for (TextLine line : fragment.lines()) {
            add(reach, inner, new Rect(content.x() + line.x(), content.y() + line.y(), line.width(), line.height()));
        }
        for (Rule rule : fragment.rules()) {
            add(reach, inner, new Rect(content.x() + rule.x(), content.y() + rule.y(), rule.width(), rule.height()));
        }
        List<Placed> children = children();
        if (style.layout() == Layout.COLUMNS && style.columns().separator() > 0) {
            addSeparators(reach, inner, content, style.columns().separator(), children);
        }
        for (Placed child : children) {
            if (child.inFlow()) {
                child.addPainted(reach);
            }
        }
    }

    /**
     * The line between each two columns with content: centered in the gap, from the top of the content box and as
     * tall as the tallest column, as it is painted.
     */
    private static void addSeparators(double[] reach, AffineTransform inner, Rect content, double thickness,
                                      List<Placed> children) {
        List<Fragment> flows = children.stream().map(Placed::fragment)
                .filter(f -> f.box().role() == Element.Role.FLOW).toList();
        double height = flows.stream().mapToDouble(Fragment::height).max().orElse(0);
        for (int i = 1; i < flows.size(); i++) {
            Fragment left = flows.get(i - 1);
            double middle = (left.x() + left.width() + flows.get(i).x()) / 2;
            add(reach, inner, new Rect(content.x() + middle - thickness / 2, content.y(), thickness, height));
        }
    }

    /** Whether a background is seen: there is one and it is not a fully transparent color. */
    private static boolean visible(Paint background) {
        return background != null
                && !(background instanceof Paint.Solid solid && Colors.rgba(solid.color())[3] == 0);
    }

    private static void add(double[] reach, AffineTransform rotations, Rect unrotated) {
        Rect r = envelope(rotations, unrotated);
        reach[0] = Math.min(reach[0], r.x());
        reach[1] = Math.min(reach[1], r.y());
        reach[2] = Math.max(reach[2], r.right());
        reach[3] = Math.max(reach[3], r.bottom());
    }

    private static Rect envelope(AffineTransform rotations, Rect unrotated) {
        return rotations.isIdentity() ? unrotated : Rotation.envelope(rotations, unrotated);
    }

    /** The rotations its content is painted inside: those of its ancestors and its own, if it has {@code Rotate}. */
    private AffineTransform inner() {
        Rotation rotation = fragment.box().style().rotation();
        if (rotation == null) {
            return outer;
        }
        AffineTransform inner = new AffineTransform(outer);
        inner.concatenate(rotation.transform(borderBox));
        return inner;
    }

    @Override
    public String toString() {
        Box box = fragment.box();
        return "Placed[page=" + page + ", " + box.element() + (box.id() != null ? " " + box.id() : "") + ", borderBox="
                + borderBox + "]";
    }

    /**
     * A column of a fragment of a {@code Columns}.
     *
     * @param box  the column on the paper, unrotated: its width, and the height of the content box of the fragment
     * @param flow the content the column has on this page, placed; {@code null} in a column the content did not reach
     */
    public record Column(Rect box, Placed flow) {
    }
}
