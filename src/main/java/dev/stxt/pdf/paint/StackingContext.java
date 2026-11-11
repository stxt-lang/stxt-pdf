package dev.stxt.pdf.paint;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Rule;
import dev.stxt.pdf.model.Edges;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.Radii;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Rotation;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.Turns;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Element.Role;
import dev.stxt.pdf.tree.Style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Tree of stacking contexts (reference/common/layers-and-rotation.md), built on the
 * fragments of a page. It is independent of the box tree.
 *
 * <p>Painting order inside a context, from bottom to top:</p>
 * <ol>
 *   <li>Background and border of the box that creates the context (and, in a {@code Columns}, the line between
 *       columns).</li>
 *   <li>Child contexts with a negative {@code Layer}, from lowest to highest.</li>
 *   <li>In-flow boxes in tree order: first backgrounds and borders, then the text and the
 *       images.</li>
 *   <li>Positioned boxes without {@code Layer} (or with 0), in tree order.</li>
 *   <li>Child contexts with a positive {@code Layer}, from lowest to highest.</li>
 * </ol>
 *
 * <p>Simplification: a positioned box without {@code Layer} is painted complete in step 4, with
 * its descendants, also those of them that create a context, which stay inside it.</p>
 *
 * <p>A box with {@code Rotate} (reference/common/layers-and-rotation.md) is painted rotated with everything it
 * contains; a box with {@code Corner radius} clips by its curve the backgrounds, borders and images of what it has
 * in flow (reference/common/box.md); and a box with {@code Overflow: clip} (reference/common/overflow.md) clips by its
 * padding box everything it contains, the {@code Overlay} boxes inside it and the text included. Each piece carries
 * the chain of rotations and clips of its ancestors, from the page inwards ({@code Piece.frames}), and each painting
 * step applies it: background, border and image, the whole chain; text, the rotations and the clips of
 * {@code Overflow: clip}.</p>
 *
 * <p>The "tree order" is that of the document, not that of the fragments: {@code Overlay}
 * boxes hang from the fragment of their reference container, after its in-flow children, and
 * {@code Page overlay} boxes are added at the end, so contexts are sorted by their box.</p>
 *
 * <p>It is the only place where the position of a fragment on the page is computed: whoever else needs it (the
 * destinations of the PDF, {@link PdfDestinations}) goes through the fragments with {@link #visit}. The text lines of a
 * fragment are painted by {@link TextPainter}.</p>
 */
public final class StackingContext {

    /** What {@link #visit} gives of each fragment. */
    interface FragmentVisitor {

        /**
         * {@code borderBox}: the border box of the fragment on the page, unrotated; {@code turns}: the rotations within
         * which it is painted, those of its ancestors and its own, if it has {@code Rotate}. {@code turns} only holds
         * them during the call.
         */
        void visit(Fragment fragment, Rect borderBox, Turns turns);
    }

    /**
     * Fragment with its absolute origin already computed and the rotations and clips of its ancestors, from the page
     * inwards, applied to it when painting it. Its own rotation is not included: it is applied afterwards.
     *
     * <p>With {@code rules}, the piece is only the lines of the fragment ({@link Fragment#rules}, the merged borders of
     * a table), which go in the flow after the backgrounds of its descendants and before their text; its frames are
     * then those of what the fragment contains, its own rotation included.</p>
     */
    private record Piece(Fragment f, double x, double y, List<Frame> frames, boolean rules) {

        Piece(Fragment f, double x, double y, List<Frame> frames) {
            this(f, x, y, frames, false);
        }

        Rect borderBox() {
            return f.borderBox(x, y);
        }

        /**
         * The frames of what it contains: its own, its rotation and its clip: by its padding box if it has
         * {@code Overflow: clip} (which also follows its corners), or by the curve of its corners if they are rounded.
         */
        List<Frame> inner() {
            List<Frame> inner = new ArrayList<>(frames);
            if (f.box().style().rotation() != null) {
                inner.add(new Frame(this, Frame.Kind.ROTATION));
            }
            if (f.box().style().box().clips()) {
                inner.add(new Frame(this, Frame.Kind.WINDOW));
            } else if (!radii(this).isSquare()) {
                inner.add(new Frame(this, Frame.Kind.CORNERS));
            }
            return List.copyOf(inner);
        }
    }

    /** What the piece of an ancestor applies to what it contains. */
    private record Frame(Piece piece, Kind kind) {
        enum Kind {
            /** Its {@code Rotate}. */
            ROTATION,
            /** The clip by the curve of its padding box: backgrounds, borders and images of its flow. */
            CORNERS,
            /** The clip of {@code Overflow: clip} by its padding box: everything, {@code Overlay} boxes and text too. */
            WINDOW
        }
    }

    /** What a painting step applies of the clips of its ancestors. */
    private enum Clips {
        /** The corners and the windows: backgrounds, borders and images. */
        ALL,
        /** Only the windows of {@code Overflow: clip}: the text, which the rounded corners do not clip. */
        WINDOWS,
        /** None: the debugging outlines, so that the whole box is visible. */
        NONE
    }

    /**
     * The frames without the clips of the rounded corners: what an {@code Overlay} inherits, which the outer boxes
     * do not clip by their corners but do by {@code Overflow: clip}.
     */
    private static List<Frame> forOverlay(List<Frame> frames) {
        return frames.stream().filter(f -> f.kind() != Frame.Kind.CORNERS).toList();
    }

    private final Piece root;
    private final int layer;
    /**
     * The pieces of the flow and of the footnote area of the page, in document order (an ancestor before its
     * descendants), without the lines of the fragments: what {@link #visit} goes through. All the contexts of the
     * flow of a page share it; those of the {@code Page overlay} boxes have their own, which nobody reads.
     */
    private final List<Piece> visited;
    private final List<StackingContext> negative = new ArrayList<>();
    private final List<Piece> inFlow = new ArrayList<>();
    private final List<StackingContext> positionedZero = new ArrayList<>();
    private final List<StackingContext> positive = new ArrayList<>();

    private StackingContext(Piece root, int layer, List<Piece> visited) {
        this.root = root;
        this.layer = layer;
        this.visited = visited;
    }

    /** Root context of a page; {@code originX/Y} is the corner of the page area. */
    public static StackingContext build(Fragment root, double originX, double originY) {
        return build(root, List.of(), originX, originY);
    }

    /**
     * Root context of a page with its {@code Page overlay} boxes, which create child contexts of the
     * root; their positions are relative to the page area.
     */
    public static StackingContext build(Fragment root, List<Fragment> pageOverlays, double originX, double originY) {
        return build(root, null, pageOverlays, originX, originY);
    }

    /**
     * Root context of a page with its footnote area ({@code footnotes}, relative to the page area like the root;
     * {@code null} without notes), which is painted after the flow and before the {@code Page overlay} boxes, as one
     * more in-flow box, and its {@code Page overlay} boxes.
     */
    public static StackingContext build(Fragment root, Fragment footnotes, List<Fragment> pageOverlays, double originX,
                                        double originY) {
        StackingContext sc = new StackingContext(new Piece(root, originX, originY, List.of()), 0, new ArrayList<>());
        sc.visited.add(sc.root);
        sc.walkChildren(sc.root);
        if (footnotes != null) {
            Piece p = new Piece(footnotes, originX + footnotes.x(), originY + footnotes.y(), List.of());
            sc.inFlow.add(p);
            sc.visited.add(p);
            sc.walkChildren(p);
        }
        for (Fragment f : pageOverlays) {
            // They repeat on every page: visit does not go through them.
            sc.addContext(new Piece(f, originX + f.x(), originY + f.y(), List.of()), new ArrayList<>());
        }
        sc.sort();
        return sc;
    }

    /**
     * Goes through the fragments of the flow and of the footnote area of the page, in document order (an ancestor
     * before its descendants), with the coordinates with which they are painted; not through those of the
     * {@code Page overlay} boxes, which repeat on every page.
     */
    void visit(FragmentVisitor visitor) {
        Turns turns = new Turns();
        for (Piece p : visited) {
            int pushed = 0;
            for (Frame frame : p.frames()) {
                if (frame.kind() == Frame.Kind.ROTATION) {
                    pushed += push(turns, frame.piece());
                }
            }
            pushed += push(turns, p);
            visitor.visit(p.f(), p.borderBox(), turns);
            for (; pushed > 0; pushed--) {
                turns.pop();
            }
        }
    }

    /** Adds the rotation of the box of {@code p} to {@code turns}, if it has {@code Rotate}; returns how many it added. */
    private static int push(Turns turns, Piece p) {
        Rotation rotation = p.f().box().style().rotation();
        if (rotation == null) {
            return 0;
        }
        Rect borderBox = p.borderBox();
        turns.push(rotation.degrees(), rotation.pivotX(borderBox), rotation.pivotY(borderBox));
        return 1;
    }

    private void addContext(Piece p) {
        addContext(p, visited);
    }

    private void addContext(Piece p, List<Piece> visited) {
        Style s = p.f().box().style();
        StackingContext child = new StackingContext(p, s.layer() != null ? s.layer() : 0, visited);
        child.walkChildren(p);
        child.sort();
        if (child.layer < 0) {
            negative.add(child);
        } else if (child.layer > 0) {
            positive.add(child);
        } else {
            positionedZero.add(child);
        }
    }

    /**
     * Distributes the descendants of {@code parent}. Those of its flow, a {@code Frame} or an item with {@code
     * Layer} included, inherit its rotations and its clips; an {@code Overlay}, only its rotations; a
     * {@code Page overlay}, nothing.
     */
    private void walkChildren(Piece parent) {
        Rect content = parent.borderBox().shrink(parent.f().border()).shrink(parent.f().padding());
        List<Frame> inner = parent.inner();
        for (Fragment h : parent.f().children()) {
            Style s = h.box().style();
            List<Frame> frames = s.position() == Position.PAGE_OVERLAY ? List.of()
                    : s.position() == Position.OVERLAY ? forOverlay(inner) : inner;
            Piece p = new Piece(h, content.x() + h.x(), content.y() + h.y(), frames);
            visited.add(p);
            if (createsContext(h)) {
                addContext(p);
            } else if (s.isPositioned()) {
                StackingContext pseudo = new StackingContext(p, 0, visited);
                pseudo.walkChildren(p);
                pseudo.sort();
                positionedZero.add(pseudo);
            } else {
                inFlow.add(p);
                walkChildren(p);
            }
        }
        if (!parent.f().rules().isEmpty()) {
            inFlow.add(new Piece(parent.f(), parent.x(), parent.y(), inner, true));
        }
    }

    private static boolean createsContext(Fragment f) {
        Style s = f.box().style();
        if (s.position() == Position.PAGE_OVERLAY) {
            return true;
        }
        if (s.layer() == null) {
            return false;
        }
        Layout parent = f.box().parent() != null ? f.box().parent().style().layout() : null;
        boolean item = parent == Layout.STACK || parent == Layout.GRID;
        return s.isPositioned() || item;
    }

    private void sort() {
        // With equal Layer, in document order.
        Comparator<StackingContext> byLayer =
                Comparator.<StackingContext>comparingInt(s -> s.layer).thenComparing(TREE_ORDER);
        negative.sort(byLayer);
        positionedZero.sort(TREE_ORDER);
        positive.sort(byLayer);
    }

    private static final Comparator<StackingContext> TREE_ORDER =
            (a, b) -> compareTreeOrder(a.root.f().box(), b.root.f().box());

    /** Document order (preorder traversal of the box tree): an ancestor goes first. */
    static int compareTreeOrder(Box a, Box b) {
        List<Box> pathA = ancestry(a);
        List<Box> pathB = ancestry(b);
        int i = 0;
        while (i < pathA.size() && i < pathB.size() && pathA.get(i) == pathB.get(i)) {
            i++;
        }
        if (i == pathA.size() || i == pathB.size()) {
            return Integer.compare(pathA.size(), pathB.size());
        }
        List<Box> siblings = pathA.get(i - 1).children();
        return Integer.compare(siblings.indexOf(pathA.get(i)), siblings.indexOf(pathB.get(i)));
    }

    /** The box and its ancestors, from the root. */
    private static List<Box> ancestry(Box box) {
        List<Box> path = new ArrayList<>();
        for (Box b = box; b != null; b = b.parent()) {
            path.add(b);
        }
        Collections.reverse(path);
        return path;
    }

    /**
     * Paints the context. Each step is painted with the rotations and clips of its piece ({@link #framed}): a rotated
     * box ({@code Rotate}, reference/common/layers-and-rotation.md) is painted rotated with everything it contains,
     * a box with rounded corners clips the backgrounds, borders and images of its flow (reference/common/box.md) and
     * a box with {@code Overflow: clip} clips everything it contains (reference/common/overflow.md).
     */
    public void paint(Canvas canvas) {
        box(canvas, root);
        negative.forEach(s -> s.paint(canvas));
        inFlow.forEach(p -> box(canvas, p));
        framed(canvas, root, contentClips(root), () -> paintContent(canvas, root));
        inFlow.stream().filter(p -> !p.rules())
                .forEach(p -> framed(canvas, p, contentClips(p), () -> paintContent(canvas, p)));
        positionedZero.forEach(s -> s.paint(canvas));
        positive.forEach(s -> s.paint(canvas));
    }

    /**
     * What the content step of the piece clips: an image, by the corners and the windows of its ancestors; text or a
     * leader, only by the windows; a box with nothing to paint there, nothing (no empty clip is written).
     */
    private static Clips contentClips(Piece p) {
        if (p.f().box().isImage()) {
            return Clips.ALL;
        }
        return p.f().lines().isEmpty() && p.f().box().role() != Role.LEADER ? Clips.NONE : Clips.WINDOWS;
    }

    /** The background and the border of the piece, with its clips, and its debugging outline, without them. */
    private static void box(Canvas canvas, Piece p) {
        if (p.rules()) {
            framed(canvas, p, Clips.ALL, () -> paintFragmentRules(canvas, p));
            return;
        }
        framed(canvas, p, paintsBox(p) ? Clips.ALL : Clips.NONE, () -> paintBackgroundAndBorder(canvas, p));
        if (canvas.outlines()) {
            framed(canvas, p, Clips.NONE, () -> canvas.outline(p.borderBox()));
        }
    }

    /**
     * Whether the box paints something in the backgrounds step: its background, its border (not that of a cell with
     * merged borders, which its table draws) or the column rule.
     */
    private static boolean paintsBox(Piece p) {
        Style s = p.f().box().style();
        Insets border = p.f().border();
        boolean paintsBorder = p.f().box().mergedBorder() == null
                && (border.top() > 0 || border.right() > 0 || border.bottom() > 0 || border.left() > 0);
        return s.box().background() != null || paintsBorder || s.columns().separator() > 0;
    }

    /**
     * Paints {@code body} with the rotations of the ancestors of {@code p} and its own, if it has {@code Rotate}, and
     * with the clips of its ancestors that {@code clips} says: those of the rounded corners do not reach the text
     * (reference/common/box.md), those of {@code Overflow: clip} reach everything (reference/common/overflow.md).
     */
    private static void framed(Canvas canvas, Piece p, Clips clips, Runnable body) {
        List<Runnable> ends = new ArrayList<>();
        for (Frame frame : p.frames()) {
            if (frame.kind() == Frame.Kind.ROTATION) {
                if (beginRotation(canvas, frame.piece()) > 0) {
                    ends.add(canvas::endRotation);
                }
            } else if (clips == Clips.ALL || clips == Clips.WINDOWS && frame.kind() == Frame.Kind.WINDOW) {
                Piece c = frame.piece();
                canvas.beginClip(c.borderBox().shrink(c.f().border()), radii(c).inset(c.f().border()));
                ends.add(canvas::endClip);
            }
        }
        // The frames of the lines of a fragment already carry its rotation.
        if (!p.rules() && beginRotation(canvas, p) > 0) {
            ends.add(canvas::endRotation);
        }
        body.run();
        for (int i = ends.size() - 1; i >= 0; i--) {
            ends.get(i).run();
        }
    }

    /** Starts the rotation of the box of {@code p}, if it has {@code Rotate}; returns how many rotations it started. */
    private static int beginRotation(Canvas canvas, Piece p) {
        Rotation rotation = p.f().box().style().rotation();
        if (rotation == null) {
            return 0;
        }
        Rect borderBox = p.borderBox();
        canvas.beginRotation(rotation.degrees(), rotation.pivotX(borderBox), rotation.pivotY(borderBox));
        return 1;
    }

    private static void paintBackgroundAndBorder(Canvas canvas, Piece p) {
        Style s = p.f().box().style();
        Rect borderBox = p.borderBox();
        Radii radii = radii(p);
        if (s.box().background() != null) {
            canvas.fill(borderBox.shrink(p.f().border()), radii.inset(p.f().border()), s.box().background());
        }
        // The border of a cell with merged borders is only the room for the lines that its table draws.
        if (p.f().box().mergedBorder() == null) {
            canvas.border(borderBox, p.f().border(), radii, borderColors(s), s.box().borderStyle());
        }
        if (s.columns().separator() > 0) {
            paintRules(canvas, p);
        }
    }

    /**
     * The lines of a fragment ({@link Fragment#rules}): the merged borders of a table
     * (reference/components/table.md#merged-borders), each one a line of its color and its style relative to the content box.
     * Like backgrounds, they are not content of the tagged PDF.
     */
    private static void paintFragmentRules(Canvas canvas, Piece p) {
        Rect content = p.borderBox().shrink(p.f().border()).shrink(p.f().padding());
        for (Rule r : p.f().rules()) {
            canvas.line(new Rect(content.x() + r.x(), content.y() + r.y(), r.width(), r.height()), r.vertical(),
                    r.color(), r.style());
        }
    }

    /**
     * The corners of the border box of the fragment (reference/common/box.md): {@code Corner radius} resolved against
     * its size and, in a box broken across pages with {@code Edges: open}, only the top ones
     * in the first fragment and the bottom ones in the last.
     */
    private static Radii radii(Piece p) {
        Style s = p.f().box().style();
        Radii radii = Radii.resolve(s.box().cornerRadius(), p.borderBox().width(), p.borderBox().height());
        if (s.breaks().edges() == Edges.CLOSED) {
            return radii;
        }
        return radii.forFragment(p.f().first(), p.f().last());
    }

    /** The color of each side of the border: its own or, if it does not declare one, that of the text. */
    private static Sides<String> borderColors(Style s) {
        if (s.box().borderColor() == null) {
            return Sides.all(s.typography().color());
        }
        return new Sides<>(orText(s.box().borderColor().top(), s), orText(s.box().borderColor().right(), s),
                orText(s.box().borderColor().bottom(), s), orText(s.box().borderColor().left(), s));
    }

    private static String orText(String color, Style s) {
        return color != null ? color : s.typography().color();
    }

    /**
     * The line between each two columns with content of a fragment of a {@code Columns}
     * (reference/components/columns.md): centered in the gap, from the top of the content box and with the height of
     * the tallest column. Its children are the columns, in order.
     */
    private static void paintRules(Canvas canvas, Piece p) {
        Style s = p.f().box().style();
        List<Fragment> columns = p.f().children();
        Rect content = p.borderBox().shrink(p.f().border()).shrink(p.f().padding());
        double height = columns.stream().mapToDouble(Fragment::height).max().orElse(0);
        String color = s.columns().separatorColor() != null ? s.columns().separatorColor() : s.typography().color();
        for (int i = 1; i < columns.size(); i++) {
            Fragment left = columns.get(i - 1);
            double middle = (left.x() + left.width() + columns.get(i).x()) / 2;
            canvas.line(new Rect(content.x() + middle - s.columns().separator() / 2, content.y(),
                    s.columns().separator(), height), true, color, s.columns().separatorStyle());
        }
    }

    /**
     * The content of a box: its text lines or, in an {@code Image}, the image
     * (reference/components/image.md), and then its link.
     */
    private static void paintContent(Canvas canvas, Piece p) {
        if (p.f().box().isImage()) {
            Insets inside = new Insets(p.f().border().top() + p.f().padding().top(),
                    p.f().border().right() + p.f().padding().right(),
                    p.f().border().bottom() + p.f().padding().bottom(),
                    p.f().border().left() + p.f().padding().left());
            canvas.image(p.f().box(), p.borderBox().shrink(inside), radii(p).inset(inside));
        } else if (p.f().box().role() == Role.LEADER) {
            paintLeader(canvas, p);
        } else {
            TextPainter.paint(canvas, p.f().box(), p.borderBox().shrink(p.f().border()).shrink(p.f().padding()),
                    p.f().lines());
        }
        paintLinks(canvas, p);
    }

    /**
     * The clickable area of the whole box (reference/common/links.md): its border box if the box has {@code Link}.
     * Those of the runs with a link in its text lines are painted by {@link TextPainter}, inside the content of the
     * link. A box broken across pages has one area per fragment.
     */
    private static void paintLinks(Canvas canvas, Piece p) {
        Box box = p.f().box();
        if (box.style().link() != null && p.f().width() > 0 && p.f().height() > 0) {
            canvas.tagging().link(box, p.borderBox(), box.style().link(), TextPainter.describe(box));
        }
    }

    /**
     * The leader of a table of contents entry (reference/components/table-of-contents.md): dots or a line on the
     * baseline of the text of its entry, with its color. Like backgrounds, it is not content of the tagged PDF.
     */
    private static void paintLeader(Canvas canvas, Piece p) {
        Style s = p.f().box().style();
        Rect content = p.borderBox().shrink(p.f().border()).shrink(p.f().padding());
        double baseline = canvas.vertical(s.typography().fontFace()).baseline(content.y(), s.typography().usedLineHeight(), s.typography().fontSize());
        for (Rect shape : Leaders.shapes(s.tableOfContents().leader(), content, baseline, s.typography().fontSize())) {
            canvas.background(shape, s.typography().color());
        }
    }
}
