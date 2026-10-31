package dev.stxt.pdf.paint;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.model.Edges;
import dev.stxt.pdf.model.DecorationMetrics;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.Radii;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Rotation;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.VerticalMetrics;
import dev.stxt.pdf.model.Preformatted;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Element.Role;
import dev.stxt.pdf.tree.Style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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
 * contains, and a box with {@code Corner radius} clips by its curve the backgrounds, borders and images of what it has
 * in flow (reference/common/box.md). Each piece carries the chain of rotations and clips of its ancestors, from the
 * page inwards ({@code Piece.frames}), and each painting step applies it: background, border and image, the whole
 * chain; text, only the rotations.</p>
 *
 * <p>The "tree order" is that of the document, not that of the fragments: {@code Overlay}
 * boxes hang from the fragment of their reference container, after its in-flow children, and
 * {@code Page overlay} boxes are added at the end, so contexts are sorted by their box.</p>
 */
public final class StackingContext {

    /**
     * Fragment with its absolute origin already computed and the rotations and clips of its ancestors, from the page
     * inwards, applied to it when painting it. Its own rotation is not included: it is applied afterwards.
     */
    private record Piece(Fragment f, double x, double y, List<Frame> frames) {
        Rect borderBox() {
            return f.borderBox(x, y);
        }

        /** The frames of what it contains: its own, its rotation and, if it has rounded corners, its clip. */
        List<Frame> inner() {
            List<Frame> inner = new ArrayList<>(frames);
            if (f.box().style().rotation() != null) {
                inner.add(new Frame(this, false));
            }
            if (!radii(this).isSquare()) {
                inner.add(new Frame(this, true));
            }
            return List.copyOf(inner);
        }
    }

    /** The rotation ({@code clip} false) or the clip by the curve of its padding box of the piece of an ancestor. */
    private record Frame(Piece piece, boolean clip) {
    }

    /** The frames without the clips: what an {@code Overlay} inherits, which the outer boxes do not clip. */
    private static List<Frame> turnsOnly(List<Frame> frames) {
        return frames.stream().filter(f -> !f.clip()).toList();
    }

    private final Piece root;
    private final int layer;
    private final List<StackingContext> negative = new ArrayList<>();
    private final List<Piece> inFlow = new ArrayList<>();
    private final List<StackingContext> positionedZero = new ArrayList<>();
    private final List<StackingContext> positive = new ArrayList<>();

    private StackingContext(Piece root, int layer) {
        this.root = root;
        this.layer = layer;
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
        StackingContext sc = new StackingContext(new Piece(root, originX, originY, List.of()), 0);
        sc.walkChildren(sc.root);
        for (Fragment f : pageOverlays) {
            sc.addContext(new Piece(f, originX + f.x(), originY + f.y(), List.of()));
        }
        sc.sort();
        return sc;
    }

    private void addContext(Piece p) {
        Style s = p.f().box().style();
        StackingContext child = new StackingContext(p, s.layer() != null ? s.layer() : 0);
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
                    : s.position() == Position.OVERLAY ? turnsOnly(inner) : inner;
            Piece p = new Piece(h, content.x() + h.x(), content.y() + h.y(), frames);
            if (createsContext(h)) {
                addContext(p);
            } else if (s.isPositioned()) {
                StackingContext pseudo = new StackingContext(p, 0);
                pseudo.walkChildren(p);
                pseudo.sort();
                positionedZero.add(pseudo);
            } else {
                inFlow.add(p);
                walkChildren(p);
            }
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
        if (i == 0) {
            // Different roots: a Page overlay of the document and one of a section, which is the root of its layout
            // (reference/components/document.md). Those of the document go first.
            return Boolean.compare(pathA.get(0).element() != null, pathB.get(0).element() != null);
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
     * and a box with rounded corners clips the backgrounds, borders and images of its flow (reference/common/box.md).
     */
    public void paint(Canvas canvas) {
        box(canvas, root);
        negative.forEach(s -> s.paint(canvas));
        inFlow.forEach(p -> box(canvas, p));
        framed(canvas, root, root.f().box().isImage(), () -> paintContent(canvas, root));
        inFlow.forEach(p -> framed(canvas, p, p.f().box().isImage(), () -> paintContent(canvas, p)));
        positionedZero.forEach(s -> s.paint(canvas));
        positive.forEach(s -> s.paint(canvas));
    }

    /** The background and the border of the piece, with its clips, and its debugging outline, without them. */
    private static void box(Canvas canvas, Piece p) {
        framed(canvas, p, paintsBox(p), () -> paintBackgroundAndBorder(canvas, p));
        if (canvas.outlines()) {
            framed(canvas, p, false, () -> canvas.outline(p.borderBox()));
        }
    }

    /** Whether the box paints something in the backgrounds step: its background, its border or the column rule. */
    private static boolean paintsBox(Piece p) {
        Style s = p.f().box().style();
        Insets border = p.f().border();
        return s.box().background() != null || border.top() > 0 || border.right() > 0 || border.bottom() > 0
                || border.left() > 0 || s.columns().separator() > 0;
    }

    /**
     * Paints {@code body} with the rotations of the ancestors of {@code p} and its own, if it has {@code Rotate}, and,
     * if {@code clipped}, with the clips of its rounded ancestors; text is painted without clips.
     */
    private static void framed(Canvas canvas, Piece p, boolean clipped, Runnable body) {
        List<Runnable> ends = new ArrayList<>();
        for (Frame frame : p.frames()) {
            if (!frame.clip()) {
                if (beginRotation(canvas, frame.piece()) > 0) {
                    ends.add(canvas::endRotation);
                }
            } else if (clipped) {
                Piece c = frame.piece();
                canvas.beginClip(c.borderBox().shrink(c.f().border()), radii(c).inset(c.f().border()));
                ends.add(canvas::endClip);
            }
        }
        if (beginRotation(canvas, p) > 0) {
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
        canvas.border(borderBox, p.f().border(), radii, borderColors(s));
        if (s.columns().separator() > 0) {
            paintRules(canvas, p);
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
            canvas.background(new Rect(content.x() + middle - s.columns().separator() / 2, content.y(), s.columns().separator(), height),
                    color);
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
            paintText(canvas, p);
        }
        paintLinks(canvas, p);
    }

    /**
     * The clickable area of the whole box (reference/common/links.md): its border box if the box has {@code Link}.
     * Those of the runs with a link in its text lines are painted by {@link #paintText}, inside the content of the
     * link. A box broken across pages has one area per fragment.
     */
    private static void paintLinks(Canvas canvas, Piece p) {
        Box box = p.f().box();
        if (box.style().link() != null && p.f().width() > 0 && p.f().height() > 0) {
            canvas.tagging().link(box, p.borderBox(), box.style().link(), describe(box));
        }
    }

    /**
     * What the box with a link says, for whoever cannot see it: the alternative text of an image or the
     * text of the box and of what it contains (without the page number of the entries of a
     * table of contents); {@code null} if it has no text.
     */
    private static String describe(Box box) {
        if (box.style().image().alt() != null) {
            return box.style().image().alt();
        }
        StringBuilder out = new StringBuilder();
        collectText(box, out);
        String text = out.toString().replaceAll("\\s+", " ").strip();
        return text.isEmpty() ? null : text;
    }

    private static void collectText(Box box, StringBuilder out) {
        if (box.role() == Role.PAGE_NUMBER) {
            return;
        }
        if (box.hasText()) {
            out.append(box.text()).append(' ');
        }
        box.children().forEach(child -> collectText(child, out));
    }

    /** Area of a run of the line {@code l}, from {@code from} to {@code to} (relative to the line). */
    private static Rect linkArea(Rect content, TextLine l, double from, double to) {
        return new Rect(content.x() + l.x() + from, content.y() + l.y(), to - from, l.height());
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

    /**
     * The text lines, each run with its font (reference/common/text-formatting.md). The underline
     * is painted before the text and the strikethrough after it; both outside the marked content, because they are not
     * text (in the tagged PDF, <em>artifacts</em>).
     *
     * <p>Text is painted in sequences of consecutive runs with the same link, also from one line
     * to the next ({@link Canvas#beginText} if they have none, {@link Canvas#beginLinkText} if they have one): each
     * sequence is a separate marked content, and that of a link hangs from the {@code Link} of the tagged PDF
     * (reference/common/links.md). Inside each link sequence, one clickable area per line ({@link
     * #paintLinkZone}).</p>
     */
    private static void paintText(Canvas canvas, Piece p) {
        if (p.f().lines().isEmpty()) {
            return;
        }
        Box box = p.f().box();
        Style s = box.style();
        FontFace font = s.typography().fontFace();
        VerticalMetrics metrics = canvas.vertical(font);
        Rect content = p.borderBox().shrink(p.f().border()).shrink(p.f().padding());
        paintDecorations(canvas, p, content, metrics, true);
        Tagging tags = canvas.tagging();
        // With letter spacing, whoever extracts the text guesses spaces between the letters: each sequence of runs of
        // a line carries its text separately (reference/implementation/pdf.md).
        boolean spaced = s.typography().letterSpacing() != 0;
        boolean open = false;
        String current = null;
        List<TextLine> lines = p.f().lines();
        for (int n = 0; n < lines.size(); n++) {
            TextLine l = lines.get(n);
            // The line break swallows the space: it is written invisible after the last word, so that whoever
            // joins the text of the lines (screen reader, extraction, search) does not run the words together.
            boolean spaceAfter = n < lines.size() - 1 && s.typography().preformatted() == Preformatted.FALSE;
            double baseline = metrics.baseline(content.y() + l.y(), l.height(), s.typography().fontSize());
            List<TextLine.Run> runs = l.runs();
            for (int i = 0; i < runs.size(); i++) {
                TextLine.Run r = runs.get(i);
                String link = r.format().link();
                if (!open || !Objects.equals(link, current)) {
                    if (open) {
                        tags.endText();
                    }
                    if (link == null) {
                        tags.beginText(box);
                    } else {
                        tags.beginLinkText(box);
                    }
                    open = true;
                    current = link;
                }
                boolean first = i == 0 || !Objects.equals(link, runs.get(i - 1).format().link());
                if (link != null && first) {
                    paintLinkZone(tags, box, content, l, i, link);
                }
                if (spaced && first) {
                    tags.beginActualText(sameLinkText(runs, i, spaceAfter));
                }
                String shown = spaceAfter && i == runs.size() - 1 ? r.text() + " " : r.text();
                canvas.text(content.x() + l.x() + r.x(), baseline, shown, r.format().face(font),
                        s.typography().fontSize() * r.format().scale(), s.typography().color(), l.wordSpacing(), s.typography().letterSpacing());
                if (spaced && (i == runs.size() - 1 || !Objects.equals(link, runs.get(i + 1).format().link()))) {
                    tags.endActualText();
                }
            }
        }
        if (open) {
            tags.endText();
        }
        paintDecorations(canvas, p, content, metrics, false);
    }

    /**
     * The text of the runs of a line that start at {@code first} and follow one another with the same link, with the
     * invisible trailing space if they reach the end of the line and it has one ({@code spaceAfter}).
     */
    private static String sameLinkText(List<TextLine.Run> runs, int first, boolean spaceAfter) {
        StringBuilder text = new StringBuilder();
        String link = runs.get(first).format().link();
        int end = first;
        while (end < runs.size() && Objects.equals(link, runs.get(end).format().link())) {
            text.append(runs.get(end).text());
            end++;
        }
        return spaceAfter && end == runs.size() ? text.append(' ').toString() : text.toString();
    }

    /**
     * The clickable area (reference/common/links.md) of the runs of the line {@code l} that start at {@code first}
     * and follow one another with the same destination (a bold part inside a link is a single area).
     */
    private static void paintLinkZone(Tagging tags, Box box, Rect content, TextLine l, int first,
                                      String destination) {
        List<TextLine.Run> runs = l.runs();
        StringBuilder text = new StringBuilder();
        int end = first;
        while (end < runs.size() && destination.equals(runs.get(end).format().link())) {
            text.append(runs.get(end).text());
            end++;
        }
        TextLine.Run last = runs.get(end - 1);
        tags.link(box, linkArea(content, l, runs.get(first).x(), last.x() + last.width()), destination,
                text.toString());
    }

    /** Underline ({@code underline}) or strikethrough lines of the runs, with the color of the text. */
    private static void paintDecorations(Canvas canvas, Piece p, Rect content, VerticalMetrics metrics,
                                         boolean underline) {
        Style s = p.f().box().style();
        for (TextLine l : p.f().lines()) {
            double baseline = metrics.baseline(content.y() + l.y(), l.height(), s.typography().fontSize());
            for (TextLine.Run r : l.runs()) {
                if (underline ? !r.format().underline() : !r.format().strike()) {
                    continue;
                }
                DecorationMetrics d = canvas.decoration(r.format().face(s.typography().fontFace()));
                double x = content.x() + l.x() + r.x();
                canvas.background(underline ? d.underline(x, baseline, r.width(), s.typography().fontSize())
                        : d.strikeout(x, baseline, r.width(), s.typography().fontSize()), s.typography().color());
            }
        }
    }
}
