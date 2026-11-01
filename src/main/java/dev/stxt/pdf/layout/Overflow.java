package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Rotation;
import dev.stxt.pdf.model.Tolerance;
import dev.stxt.pdf.model.Preformatted;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Element.Role;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import dev.stxt.pdf.tree.SourceLine;
import dev.stxt.pdf.tree.PageSetup;
import dev.stxt.pdf.tree.Style;

import java.awt.geom.AffineTransform;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static dev.stxt.pdf.layout.Messages.excerpt;
import static dev.stxt.pdf.layout.Messages.pt;
import static dev.stxt.pdf.tree.PropertyKey.ASPECT_RATIO;
import static dev.stxt.pdf.tree.PropertyKey.BORDER;
import static dev.stxt.pdf.tree.PropertyKey.FONT_SIZE;
import static dev.stxt.pdf.tree.PropertyKey.GRID_ROWS;
import static dev.stxt.pdf.tree.PropertyKey.HEIGHT;
import static dev.stxt.pdf.tree.PropertyKey.LINE_HEIGHT;
import static dev.stxt.pdf.tree.PropertyKey.MARGIN;
import static dev.stxt.pdf.tree.PropertyKey.MAX_HEIGHT;
import static dev.stxt.pdf.tree.PropertyKey.MIN_WIDTH;
import static dev.stxt.pdf.tree.PropertyKey.OFFSET;
import static dev.stxt.pdf.tree.PropertyKey.PADDING;
import static dev.stxt.pdf.tree.PropertyKey.ROTATE;
import static dev.stxt.pdf.tree.PropertyKey.WIDTH;

/**
 * Checks, once paginated, that nothing overflows (reference/common/overflow.md): each line of text and
 * each in-flow child (with its horizontal margins) fit in the content box of its parent;
 * the flow fits in the page area of each page, and every box fits on the physical page: the {@code Overlay} and
 * {@code Page overlay} boxes, the rotated ones (they, and those inside a rotated one, with the rectangle that
 * encloses what is painted) and the in-flow ones that the {@code Offset} of a {@code Frame} or a negative
 * {@code Margin} takes out of it. It looks at the final result, not at the measuring passes.
 *
 * <p>The error points to the deepest culprit: the word that does not fit on its line, the innermost
 * box that sticks out, or the {@code Height} that falls short. Where the text could shrink ({@code Overflow: shrink}),
 * the message says so; where it already does and still does not fit at {@code Min size}, it says that too.</p>
 *
 * <p>What a box with {@code Overflow: clip} contains is painted clipped and is not checked against it nor against the
 * page (reference/common/overflow.md); each box inside it is still checked against its own parent.</p>
 */
public final class Overflow {

    /** How far a box can stick out without it being an error ({@link Tolerance#MODEL}). */
    private static final double TOLERANCE = Tolerance.MODEL;

    /** No rotations. Nobody modifies it: a rotation is composed on a copy. */
    private static final AffineTransform NONE = new AffineTransform();

    private Overflow() {
    }

    /** @throws InvalidModelException with the first overflow it finds */
    public static void check(List<Page> pages) {
        for (Page p : pages) {
            // Each page has the medium of its section (reference/components/document.md).
            PageSetup pageSetup = p.pageSetup();
            Rect area = pageSetup.pageArea();
            Insets m = pageSetup.margin();
            // The physical page, in page area coordinates.
            Rect page = new Rect(-m.left(), -m.top(), pageSetup.width(), pageSetup.height());
            content(p.content(), 0, 0, area.width(), area.height(), true, page, NONE, false);
            for (Fragment f : p.pageOverlays()) {
                fragment(f, 0, 0, page, NONE, false);
            }
        }
    }

    /**
     * A fragment whose parent's content box starts at ({@code ox}, {@code oy});
     * {@code turn} is the rotations of its ancestors (reference/common/layers-and-rotation.md), painted with it, and
     * {@code clipped}, whether an ancestor has {@code Overflow: clip}: then it is not checked against the page, because
     * what sticks out of that ancestor is not painted.
     */
    private static void fragment(Fragment f, double ox, double oy, Rect page, AffineTransform turn, boolean clipped) {
        double x = ox + f.x();
        double y = oy + f.y();
        Style s = f.box().style();
        Rect borderBox = f.borderBox(x, y);
        if (s.rotation() != null) {
            turn = new AffineTransform(turn);
            turn.concatenate(s.rotation().transform(borderBox));
        }
        if (clipped) {
            // Inside a clipped box: what goes off the page is clipped too.
        } else if (s.rotation() != null || (s.isOutOfFlow() && !turn.isIdentity())) {
            // What is rotated is checked as it is painted: the rectangle that encloses it, on the page.
            if (!inside(Rotation.envelope(turn, borderBox), page)) {
                boolean own = s.rotation() != null;
                throw new InvalidModelException(own ? s.line(ROTATE) : SourceLine.of(f.box().line()),
                        ErrorCode.BOX_OVERFLOW,
                        "Box " + f.box().describe() + (own ? ", rotated," : ", inside a rotated box,")
                                + " goes off the page: " + (own ? "its Rotate, its Rotate origin, " : "")
                                + "its position and its size have to be checked");
            }
        } else if (s.isOutOfFlow() && !inside(borderBox, page)) {
            throw new InvalidModelException(s.isDeclared(OFFSET) ? s.line(OFFSET) : SourceLine.of(f.box().line()),
                    ErrorCode.BOX_OVERFLOW, "Box " + f.box().describe() + " goes off the page: its Offset and its "
                    + "size have to be checked");
        } else if (!s.isOutOfFlow() && !inside(turn.isIdentity() ? borderBox : Rotation.envelope(turn, borderBox),
                page)) {
            // In flow it fits in its parent, but the Offset of a Frame or a negative Margin can take it off the page.
            String property = s.isDeclared(OFFSET) ? OFFSET : s.isDeclared(MARGIN) ? MARGIN : null;
            throw new InvalidModelException(property != null ? s.line(property) : SourceLine.of(f.box().line()),
                    ErrorCode.BOX_OVERFLOW, "Box " + f.box().describe() + " goes off the page: its "
                    + (OFFSET.equals(property) ? "Offset" : MARGIN.equals(property) ? "Margin" : "position")
                    + " and its size have to be checked");
        }
        double cx = borderBox.x() + f.border().left() + f.padding().left();
        double cy = borderBox.y() + f.border().top() + f.padding().top();
        content(f, cx, cy, f.contentWidth(), f.contentHeight(), false, page, turn, clipped);
    }

    /** Whether {@code r} is inside {@code page}, with the tolerance. */
    private static boolean inside(Rect r, Rect page) {
        return r.x() >= page.x() - TOLERANCE && r.y() >= page.y() - TOLERANCE
                && r.right() <= page.right() + TOLERANCE && r.bottom() <= page.bottom() + TOLERANCE;
    }

    /**
     * The lines and the children of a fragment, whose content box starts at ({@code cx},
     * {@code cy}) and measures {@code width} × {@code height}; {@code isPage}: it is the flow of a page. If the box
     * has {@code Overflow: clip}, nothing it contains is checked against it: it is painted clipped.
     */
    private static void content(Fragment f, double cx, double cy, double width, double height, boolean isPage,
                                Rect page, AffineTransform turn, boolean clipped) {
        boolean window = f.box().style().box().clips();
        for (TextLine l : f.lines()) {
            if (window) {
                break;
            }
            if (l.x() < -TOLERANCE || l.x() + l.width() > width + TOLERANCE) {
                throw textOverflow(f.box(), l, width);
            }
            if (l.y() + l.height() > height + TOLERANCE) {
                throw tooTall(f, l.y() + l.height(), height, null);
            }
        }
        for (Fragment c : f.children()) {
            if (c.box().isMarker()) {
                // It hangs outside the item: it is checked in the indent of its list (marker).
                fragment(c, cx, cy, page, turn, clipped || window);
                continue;
            }
            if (f.box().role() == Role.LIST && !window) {
                marker(f, c);
            }
            if (!c.box().style().isOutOfFlow() && !window) {
                // The Frame boxes are checked in their place, without the offset.
                double x = c.x() - c.offsetX();
                double y = c.y() - c.offsetY();
                Insets margin = c.margin();
                if (x - margin.left() < -TOLERANCE || x + c.width() + margin.right() > width + TOLERANCE) {
                    throw tooWide(c, cx + x, cx, cx + width);
                }
                // An empty box (no height, border, padding or content) paints nothing and its merged margins can
                // leave it past the end of its parent's content: it never overflows vertically.
                if (c.height() > 0 && y + c.height() > height + TOLERANCE) {
                    if (isPage) {
                        throw pageOverflow(c, cy + y, cy + height, height);
                    }
                    throw tooTall(f, y + c.height(), height, c);
                }
            }
            fragment(c, cx, cy, page, turn, clipped || window);
        }
    }

    /**
     * The marker of an item fits in its indent (reference/components/list.md): the left padding of the list plus
     * the left margin of the item.
     */
    private static void marker(Fragment list, Fragment item) {
        for (Fragment m : item.children()) {
            if (!m.box().isMarker()) {
                continue;
            }
            double room = item.x() + list.padding().left();
            if (m.width() > room + TOLERANCE) {
                throw new InvalidModelException(item.box().line(), ErrorCode.LIST_MARKER_OVERFLOW, "The marker '"
                        + m.box().text().strip() + "' of List item " + item.box().describe() + " measures "
                        + pt(m.width()) + " with its space, and the indent leaves " + pt(room)
                        + ": the left Padding of the list or "
                        + "the left margin of the item has to be increased");
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Culprits
    // ------------------------------------------------------------------------------------------

    /**
     * A child sticks out at the sides: the culprit is the innermost thing that sticks out. If the box
     * declares its width ({@code Width} or {@code Min width}), its children that only fill it are not
     * culprits: it only goes down to a child that also sticks out of it. Only the horizontal position counts:
     * {@code x} is that of the border box of {@code c} in the coordinates of {@code left} and {@code right}.
     */
    private static InvalidModelException tooWide(Fragment c, double x, double left, double right) {
        while (true) {
            Style s = c.box().style();
            if (s.rotatesInFlow()) {
                // What is inside is rotated: what sticks out is the rectangle that encloses it.
                String property = s.isDeclared(WIDTH) ? WIDTH : ROTATE;
                return new InvalidModelException(s.line(property), ErrorCode.BOX_OVERFLOW, "Box "
                        + c.box().describe()
                        + ", rotated, does not fit in width: the rectangle that encloses it measures "
                        + pt(c.width()) + " and the content of its container measures " + pt(right - left) + ". "
                        + (s.isDeclared(WIDTH) ? "Its Width has to be reduced"
                                : "It needs a Width, which breaks its text into lines,")
                        + " or its Rotate has to be changed");
            }
            double cx = x + c.border().left() + c.padding().left();
            // A word that does not fit in its own box is the culprit; if it fits, the box is.
            for (TextLine l : c.lines()) {
                if (l.x() < -TOLERANCE || l.x() + l.width() > c.contentWidth() + TOLERANCE) {
                    throw textOverflow(c.box(), l, c.contentWidth());
                }
            }
            boolean sized = s.isDeclared(WIDTH) || s.isDeclared(MIN_WIDTH);
            Fragment inner = null;
            for (Fragment d : c.children()) {
                if (d.box().style().isOutOfFlow() || d.box().isMarker()) {
                    continue;
                }
                double dx = cx + d.x() - d.offsetX();
                boolean outside = dx < left - TOLERANCE || dx + d.width() > right + TOLERANCE;
                boolean outsideParent = dx - d.margin().left() < cx - TOLERANCE
                        || dx + d.width() + d.margin().right() > cx + c.contentWidth() + TOLERANCE;
                if (outside && (!sized || outsideParent)) {
                    inner = d;
                    x = dx;
                    break;
                }
            }
            if (inner == null) {
                String property = s.isDeclared(WIDTH) ? WIDTH : s.isDeclared(MIN_WIDTH) ? MIN_WIDTH : null;
                return new InvalidModelException(property != null ? s.line(property) : SourceLine.of(c.box().line()),
                        ErrorCode.BOX_OVERFLOW, "Box " + c.box().describe() + " does not fit in width: it measures "
                        + pt(c.width()) + " and the content of its container measures " + pt(right - left)
                        + ": its width or that of its content has to be reduced");
            }
            c = inner;
        }
    }

    /**
     * The content of a box exceeds its height: the culprit is what fixes it, in this order, its
     * {@code Height}, its {@code Max height}, its {@code Aspect ratio} or the rows of a grid
     * ({@code Row sizes}; if they were {@code auto}, they would grow with their content).
     */
    private static InvalidModelException tooTall(Fragment container, double needed, double height, Fragment child) {
        Box box = container.box();
        Style s = box.style();
        String property = Stream.of(HEIGHT, MAX_HEIGHT, ASPECT_RATIO, GRID_ROWS)
                .filter(s::isDeclared).findFirst().orElse(null);
        String what = child == null ? "its text" : "box " + child.box().describe();
        Fragment negative = property == null && child != null ? negativeMargin(container) : null;
        if (negative != null) {
            // Nothing fixes the height: the content is taller than the box because a negative margin takes away more
            // than the content measures, and the height of the content does not go below 0.
            Style n = negative.box().style();
            return new InvalidModelException(n.isDeclared(MARGIN) ? n.line(MARGIN) : SourceLine.of(negative.box().line()),
                    ErrorCode.BOX_OVERFLOW, "The content of box " + box.describe() + " does not fit in its height: "
                    + what + " reaches " + pt(needed) + " and the box leaves " + pt(height) + ", because the negative "
                    + "Margin of box " + negative.box().describe() + " takes away more height than the content has");
        }
        return new InvalidModelException(property != null ? s.line(property) : SourceLine.of(box.line()),
                ErrorCode.BOX_OVERFLOW,
                "The content of box " + box.describe() + " does not fit in its height"
                        + (child == null ? atMinSize(box) : "") + ": " + what + " reaches "
                        + pt(needed) + " and the box leaves " + pt(height) + (child == null ? shrinkHint(box) : ""));
    }

    /**
     * Where the text of the box shrinks ({@code Overflow: shrink}) and still does not fit: that it is already at
     * {@code Min size} (reference/common/overflow.md). Empty otherwise.
     */
    private static String atMinSize(Box box) {
        if (!box.style().box().shrinks()) {
            return "";
        }
        return " even at Min size (" + pt(box.style().typography().minFontSize())
                + "), the smallest size Overflow: shrink allows";
    }

    /** Where the text of the box could shrink and does not: that {@code Overflow: shrink} exists. Empty otherwise. */
    private static String shrinkHint(Box box) {
        if (box.style().box().shrinks()) {
            return "";
        }
        return ", or its Box needs Overflow: shrink, which reduces the Size until the text fits";
    }

    /** The first in-flow fragment inside {@code f}, in tree order, with a negative top or bottom margin; or null. */
    private static Fragment negativeMargin(Fragment f) {
        for (Fragment c : f.children()) {
            if (c.box().style().isOutOfFlow() || c.box().isMarker()) {
                continue;
            }
            if (c.margin().top() < 0 || c.margin().bottom() < 0) {
                return c;
            }
            Fragment inner = negativeMargin(c);
            if (inner != null) {
                return inner;
            }
        }
        return null;
    }

    /**
     * Something in the flow does not fit on the page and could not be broken: the culprit is the innermost thing
     * that goes past the end, and in it, what cannot be broken: the {@code Rotate} of a rotated box, which
     * is not broken (reference/common/breaks.md), its border and its padding if they no longer fit
     * on the page ({@code pageHeight}), its {@code Line height} (or its {@code Size}) if it has text,
     * or its {@code Height} (in an image without it, its {@code Width}). Only the vertical position counts:
     * {@code y} is that of the border box of {@code c} in the coordinates of {@code bottom}.
     */
    private static InvalidModelException pageOverflow(Fragment c, double y, double bottom, double pageHeight) {
        while (true) {
            if (c.box().style().rotation() != null) {
                // What prevents breaking it is the rotation: what it contains would be broken.
                return new InvalidModelException(c.box().style().line(ROTATE), ErrorCode.PAGE_OVERFLOW, "Box "
                        + c.box().describe()
                        + " is rotated, so it is not broken across pages, and it does not fit on one: it is "
                        + pt(c.height()) + " high and extends " + pt(y + c.height() - bottom)
                        + " beyond the page area");
            }
            double cy = y + c.border().top() + c.padding().top();
            Fragment inner = null;
            for (Fragment d : c.children()) {
                double dy = cy + d.y() - d.offsetY();
                if (!d.box().style().isOutOfFlow() && !d.box().isMarker() && dy + d.height() > bottom + TOLERANCE) {
                    inner = d;
                    y = dy;
                    break;
                }
            }
            if (inner == null) {
                Style s = c.box().style();
                String property;
                if (c.border().vertical() + c.padding().vertical() > pageHeight + TOLERANCE) {
                    property = c.padding().vertical() >= c.border().vertical() ? PADDING : BORDER;
                } else if (!c.lines().isEmpty()) {
                    property = s.isDeclared(LINE_HEIGHT) ? LINE_HEIGHT : FONT_SIZE;
                } else if (c.box().isImage() && !s.isDeclared(HEIGHT)) {
                    // The height of an image follows from its width.
                    property = WIDTH;
                } else {
                    property = HEIGHT;
                }
                SourceLine line = s.isDeclared(property) ? s.line(property) : SourceLine.of(c.box().line());
                return new InvalidModelException(line, ErrorCode.PAGE_OVERFLOW, "Box " + c.box().describe()
                        + " does not fit on the page and cannot be broken: it is " + pt(c.height())
                        + " high and extends " + pt(y + c.height() - bottom) + " beyond the page area");
            }
            c = inner;
        }
    }

    /** A line wider than its box: a word that does not fit. Points to the document line of that word. */
    private static InvalidModelException textOverflow(Box box, TextLine l, double width) {
        String word = Arrays.stream(l.text().split(" ")).max(Comparator.comparingInt(String::length)).orElse(l.text());
        // It is not in the text if the layout has changed it, like a page number.
        int index = box.text().indexOf(word);
        int line = index >= 0 ? box.textLineAt(index) : box.textLine();
        boolean shrinks = box.style().box().shrinks();
        if (box.style().typography().preformatted() == Preformatted.TRUE) {
            return new InvalidModelException(line, ErrorCode.TEXT_OVERFLOW, "A line of a text with Preformatted: "
                    + "true ('" + excerpt(word) + "'...) does not fit in the box" + atMinSize(box) + ": the line measures "
                    + pt(l.width()) + " and the box leaves " + pt(width) + ". The box has to be wider, "
                    + (shrinks ? "Min size" : "the Size") + " smaller, or the text needs Preformatted: wrap, which "
                    + "breaks the lines" + shrinkHint(box));
        }
        return new InvalidModelException(line, ErrorCode.TEXT_OVERFLOW, "The word '" + excerpt(word) + "' does not fit "
                + "on the line" + atMinSize(box) + ": the line measures " + pt(l.width()) + " and the box leaves "
                + pt(width) + ": the box has to be wider or " + (shrinks ? "Min size" : "the Size of the text")
                + " smaller" + shrinkHint(box));
    }
}
