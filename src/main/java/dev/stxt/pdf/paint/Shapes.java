package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Radii;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Sides;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The shapes the canvases paint (reference/common/box.md): a rectangle with rounded
 * corners, the ring of a border, the four trapezoids it is split into and the line along the middle of a border that
 * is not solid. They are described as a path that
 * each canvas draws its own way ({@link Path}), so that the PDF and the SVG paint the same.
 */
final class Shapes {

    /** Approximation of a quarter ellipse with a cubic Bézier curve. */
    private static final double KAPPA = 0.5522847498307936;

    private Shapes() {
    }

    /** Where a path is drawn, in page coordinates (origin at the top left). */
    interface Path {
        void moveTo(double x, double y);

        void lineTo(double x, double y);

        void curveTo(double x1, double y1, double x2, double y2, double x3, double y3);

        void close();

        /** A rectangle, which some canvases write with their own operator. By default, four lines. */
        default void rect(double x, double y, double width, double height) {
            moveTo(x, y);
            lineTo(x + width, y);
            lineTo(x + width, y + height);
            lineTo(x, y + height);
            close();
        }
    }

    /** A side of the box, to split the border. */
    enum Side {
        TOP, RIGHT, BOTTOM, LEFT;

        /** The width of this side in {@code widths}. */
        double of(Insets widths) {
            return switch (this) {
                case TOP -> widths.top();
                case RIGHT -> widths.right();
                case BOTTOM -> widths.bottom();
                case LEFT -> widths.left();
            };
        }

        /** The value of this side in {@code sides}. */
        <T> T of(Sides<T> sides) {
            return switch (this) {
                case TOP -> sides.top();
                case RIGHT -> sides.right();
                case BOTTOM -> sides.bottom();
                case LEFT -> sides.left();
            };
        }
    }

    /**
     * The color of a border painted as a whole: that of its sides with a width, if they all have the same one;
     * {@code null} if two of them differ, so that each side is painted with its own, or if no side has a width.
     */
    static String uniformColor(Insets widths, Sides<String> colors) {
        String uniform = null;
        for (Side side : Side.values()) {
            if (side.of(widths) > 0) {
                String color = side.of(colors);
                if (uniform == null) {
                    uniform = color;
                } else if (!uniform.equals(color)) {
                    return null;
                }
            }
        }
        return uniform;
    }

    /**
     * The ring of a border, to be filled with the even-odd rule: the border box {@code r} with its corners and, inside
     * it, the padding box, which is left out if nothing remains of it.
     */
    static void ring(Path path, Rect r, Insets widths, Radii radii) {
        roundedRect(path, r, radii);
        Rect inner = r.shrink(widths);
        if (inner.width() > 0 && inner.height() > 0) {
            roundedRect(path, inner, radii.inset(widths));
        }
    }

    /** The rectangle {@code r} with the corners {@code radii}, clockwise. */
    static void roundedRect(Path target, Rect r, Radii radii) {
        if (radii.isSquare()) {
            target.rect(r.x(), r.y(), r.width(), r.height());
            return;
        }
        Path path = new Pen(target);
        double x0 = r.x();
        double y0 = r.y();
        double x1 = r.right();
        double y1 = r.bottom();
        Radii.Corner tl = radii.topLeft();
        Radii.Corner tr = radii.topRight();
        Radii.Corner br = radii.bottomRight();
        Radii.Corner bl = radii.bottomLeft();
        path.moveTo(x0 + tl.rx(), y0);
        path.lineTo(x1 - tr.rx(), y0);
        corner(path, x1 - tr.rx(), y0, x1, y0 + tr.ry(), true, tr);
        path.lineTo(x1, y1 - br.ry());
        corner(path, x1, y1 - br.ry(), x1 - br.rx(), y1, false, br);
        path.lineTo(x0 + bl.rx(), y1);
        corner(path, x0 + bl.rx(), y1, x0, y1 - bl.ry(), true, bl);
        path.lineTo(x0, y0 + tl.ry());
        corner(path, x0, y0 + tl.ry(), x0 + tl.rx(), y0, false, tl);
        path.close();
    }

    /**
     * A line along the middle of a border, which a border that is not solid strokes with its pattern.
     *
     * @param closed whether it goes all the way round the box and ends where it starts
     * @param path   draws it
     */
    record Run(boolean closed, Consumer<Path> path) {
    }

    /**
     * The lines along the middle of the border of the border box {@code r}, through the sides with a width in
     * {@code widths} and round the corners {@code radii} between two of them (reference/common/box.md#border-style):
     * one closed line if the four sides have a width and, if not (a fragment of a box broken across pages), one open
     * line per group of sides in a row, clockwise, which reaches the edge of the box at its two ends.
     */
    static List<Run> centerlines(Rect r, Insets widths, Radii radii) {
        Insets half = new Insets(widths.top() / 2, widths.right() / 2, widths.bottom() / 2, widths.left() / 2);
        Rect c = r.shrink(half);
        Radii inner = radii.inset(half);
        Side[] sides = Side.values();
        boolean all = true;
        for (Side side : sides) {
            all &= side.of(widths) > 0;
        }
        if (all) {
            return List.of(new Run(true, path -> roundedRect(path, c, inner)));
        }
        List<Run> runs = new ArrayList<>();
        for (int i = 0; i < sides.length; i++) {
            if (sides[i].of(widths) > 0 && sides[(i + 3) % 4].of(widths) <= 0) {
                int count = 1;
                while (sides[(i + count) % 4].of(widths) > 0) {
                    count++;
                }
                int first = i;
                int sidesInRun = count;
                runs.add(new Run(false, path -> openRun(new Pen(path), c, inner, first, sidesInRun)));
            }
        }
        return runs;
    }

    /** {@code count} sides in a row of the rectangle {@code c}, clockwise from the side {@code first}. */
    private static void openRun(Path path, Rect c, Radii radii, int first, int count) {
        double x0 = c.x();
        double y0 = c.y();
        double x1 = c.right();
        double y1 = c.bottom();
        // The corner each side starts at, clockwise from the top left; each side ends at the next one.
        double[][] corners = {{x0, y0}, {x1, y0}, {x1, y1}, {x0, y1}};
        Radii.Corner[] rounded = {radii.topLeft(), radii.topRight(), radii.bottomRight(), radii.bottomLeft()};
        path.moveTo(corners[first][0], corners[first][1]);
        for (int k = 0; k < count; k++) {
            int side = (first + k) % 4;
            int end = (side + 1) % 4;
            double[] to = corners[end];
            if (k == count - 1) {
                path.lineTo(to[0], to[1]);
                break;
            }
            // Round the corner into the next side. Top and bottom are horizontal, and leave their corner vertically.
            Radii.Corner corner = rounded[end];
            boolean horizontal = side % 2 == 0;
            double[] from = corners[side];
            double[] next = corners[(end + 1) % 4];
            double fromX = to[0] - Math.signum(to[0] - from[0]) * corner.rx();
            double fromY = to[1] - Math.signum(to[1] - from[1]) * corner.ry();
            double toX = to[0] + Math.signum(next[0] - to[0]) * corner.rx();
            double toY = to[1] + Math.signum(next[1] - to[1]) * corner.ry();
            path.lineTo(fromX, fromY);
            corner(path, fromX, fromY, toX, toY, horizontal, corner);
        }
    }

    /** Steps a curve is split into to measure it. */
    private static final int FLATTENING = 16;

    /** The length of what {@code path} draws, with its curves measured as {@value #FLATTENING} straight pieces. */
    static double length(Consumer<Path> path) {
        double[] state = new double[5];
        path.accept(new Path() {
            @Override
            public void moveTo(double x, double y) {
                state[0] = x;
                state[1] = y;
                state[2] = x;
                state[3] = y;
            }

            @Override
            public void lineTo(double x, double y) {
                state[4] += Math.hypot(x - state[0], y - state[1]);
                state[0] = x;
                state[1] = y;
            }

            @Override
            public void curveTo(double x1, double y1, double x2, double y2, double x3, double y3) {
                double fromX = state[0];
                double fromY = state[1];
                for (int i = 1; i <= FLATTENING; i++) {
                    double t = (double) i / FLATTENING;
                    double u = 1 - t;
                    lineTo(u * u * u * fromX + 3 * u * u * t * x1 + 3 * u * t * t * x2 + t * t * t * x3,
                            u * u * u * fromY + 3 * u * u * t * y1 + 3 * u * t * t * y2 + t * t * t * y3);
                }
            }

            @Override
            public void close() {
                lineTo(state[2], state[3]);
            }
        });
        return state[4];
    }

    /** A path that does not repeat the point it is already at: a square corner adds no line of length 0. */
    private static final class Pen implements Path {
        private final Path target;
        private double x;
        private double y;

        Pen(Path target) {
            this.target = target;
        }

        @Override
        public void moveTo(double x, double y) {
            this.x = x;
            this.y = y;
            target.moveTo(x, y);
        }

        @Override
        public void lineTo(double x, double y) {
            if (x != this.x || y != this.y) {
                this.x = x;
                this.y = y;
                target.lineTo(x, y);
            }
        }

        @Override
        public void curveTo(double x1, double y1, double x2, double y2, double x3, double y3) {
            this.x = x3;
            this.y = y3;
            target.curveTo(x1, y1, x2, y2, x3, y3);
        }

        @Override
        public void close() {
            target.close();
        }
    }

    /**
     * A quarter ellipse from ({@code fromX}, {@code fromY}) to ({@code toX}, {@code toY}); if
     * {@code horizontalFirst}, it leaves horizontally (top right and bottom left).
     */
    private static void corner(Path path, double fromX, double fromY, double toX, double toY,
                               boolean horizontalFirst, Radii.Corner c) {
        if (c.isSquare()) {
            path.lineTo(toX, toY);
            return;
        }
        double kx = c.rx() * KAPPA;
        double ky = c.ry() * KAPPA;
        double dx = Math.signum(toX - fromX);
        double dy = Math.signum(toY - fromY);
        if (horizontalFirst) {
            path.curveTo(fromX + dx * kx, fromY, toX, toY - dy * ky, toX, toY);
        } else {
            path.curveTo(fromX, fromY + dy * ky, toX - dx * kx, toY, toX, toY);
        }
    }

    /**
     * The part of the border that belongs to a side: the trapezoid that joins the corners of the border
     * box with those of the padding box, so that two sides with different colors meet
     * at the diagonal of the corner. With the ring of the border as fill and this
     * trapezoid as clip, the piece of the side remains.
     *
     * <p>With rounded inner corners, the ring enters the padding box (between the
     * curve and its corner), so each diagonal continues inwards beyond the padding
     * corner, which covers the curve of {@code inner} ({@code Radii.inset} of the border). The two
     * sides of a corner share that diagonal, so they neither overlap nor leave a gap.</p>
     */
    static void sidePolygon(Path path, Side side, Rect outer, Insets widths, Radii inner) {
        double ox0 = outer.x();
        double oy0 = outer.y();
        double ox1 = outer.right();
        double oy1 = outer.bottom();
        double ix0 = ox0 + widths.left();
        double iy0 = oy0 + widths.top();
        double ix1 = ox1 - widths.right();
        double iy1 = oy1 - widths.bottom();
        double[] tl = inward(ix0, iy0, widths.left(), widths.top(), inner.topLeft());
        double[] tr = inward(ix1, iy0, -widths.right(), widths.top(), inner.topRight());
        double[] br = inward(ix1, iy1, -widths.right(), -widths.bottom(), inner.bottomRight());
        double[] bl = inward(ix0, iy1, widths.left(), -widths.bottom(), inner.bottomLeft());
        switch (side) {
            case TOP -> polygon(path, ox0, oy0, ox1, oy0, tr[0], tr[1], tl[0], tl[1]);
            case RIGHT -> polygon(path, ox1, oy0, ox1, oy1, br[0], br[1], tr[0], tr[1]);
            case BOTTOM -> polygon(path, ox1, oy1, ox0, oy1, bl[0], bl[1], br[0], br[1]);
            case LEFT -> polygon(path, ox0, oy1, ox0, oy0, tl[0], tl[1], bl[0], bl[1]);
        }
    }

    /**
     * The point of the diagonal of a corner, in the direction ({@code dx}, {@code dy}) from its
     * padding corner ({@code x}, {@code y}), that lies further in than the curve of {@code c}: twice
     * the larger of its radii. Without a curve, the corner itself.
     */
    private static double[] inward(double x, double y, double dx, double dy, Radii.Corner c) {
        double length = Math.hypot(dx, dy);
        if (c.isSquare() || length == 0) {
            return new double[]{x, y};
        }
        double extra = 2 * Math.max(c.rx(), c.ry());
        return new double[]{x + dx / length * extra, y + dy / length * extra};
    }

    private static void polygon(Path path, double... xy) {
        path.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            path.lineTo(xy[i], xy[i + 1]);
        }
        path.close();
    }
}
