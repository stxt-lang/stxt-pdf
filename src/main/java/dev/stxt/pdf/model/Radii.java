package dev.stxt.pdf.model;

/**
 * The resolved rounded corners of a box, in points (reference/common/box.md): a quarter
 * of an ellipse per corner, with its horizontal and its vertical radius. It is painting only.
 */
public record Radii(Corner topLeft, Corner topRight, Corner bottomRight, Corner bottomLeft) {

    /** The quarter ellipse of a corner; both radii are 0 if it is not rounded. */
    public record Corner(double rx, double ry) {

        public static final Corner SQUARE = new Corner(0, 0);

        /** A corner with a zero radius is square. */
        public static Corner of(double rx, double ry) {
            return rx > 0 && ry > 0 ? new Corner(rx, ry) : SQUARE;
        }

        public boolean isSquare() {
            return rx <= 0 || ry <= 0;
        }
    }

    public static final Radii NONE = new Radii(Corner.SQUARE, Corner.SQUARE, Corner.SQUARE, Corner.SQUARE);

    /**
     * Resolves {@code Corner radius} against the border box: a horizontal percentage is of the width and a
     * vertical one, of the height, so {@code 50%} on a box that is not square is an ellipse. As in
     * CSS, if two adjacent radii of a side add up to more than the side, all of them are reduced by the same
     * factor until they fit.
     */
    public static Radii resolve(Corners<Value> radius, double width, double height) {
        Radii r = new Radii(corner(radius.topLeft(), width, height), corner(radius.topRight(), width, height),
                corner(radius.bottomRight(), width, height), corner(radius.bottomLeft(), width, height));
        double factor = Math.min(1, Math.min(
                Math.min(fit(width, r.topLeft.rx + r.topRight.rx), fit(width, r.bottomLeft.rx + r.bottomRight.rx)),
                Math.min(fit(height, r.topLeft.ry + r.bottomLeft.ry), fit(height, r.topRight.ry + r.bottomRight.ry))));
        return factor < 1 ? r.scale(factor) : r;
    }

    private static Corner corner(Value v, double width, double height) {
        return Corner.of(v.resolve(width).orElse(0), v.resolve(height).orElse(0));
    }

    private static double fit(double side, double sum) {
        return sum > side ? side / sum : 1;
    }

    private Radii scale(double f) {
        return new Radii(Corner.of(topLeft.rx * f, topLeft.ry * f), Corner.of(topRight.rx * f, topRight.ry * f),
                Corner.of(bottomRight.rx * f, bottomRight.ry * f), Corner.of(bottomLeft.rx * f, bottomLeft.ry * f));
    }

    public boolean isSquare() {
        return topLeft.isSquare() && topRight.isSquare() && bottomRight.isSquare() && bottomLeft.isSquare();
    }

    /**
     * The radii of the box that remains after removing {@code i} on the inside (the border to get the
     * padding box, the border and the padding to get the content box): each radius loses the width of the
     * side it touches, and a corner left without radius is square.
     */
    public Radii inset(Insets i) {
        return new Radii(
                Corner.of(topLeft.rx - i.left(), topLeft.ry - i.top()),
                Corner.of(topRight.rx - i.right(), topRight.ry - i.top()),
                Corner.of(bottomRight.rx - i.right(), bottomRight.ry - i.bottom()),
                Corner.of(bottomLeft.rx - i.left(), bottomLeft.ry - i.bottom()));
    }

    /**
     * The corners of a fragment of a broken box (reference/common/breaks.md): with {@code Edges: open},
     * only the first fragment has the top ones and only the last one the bottom ones.
     */
    public Radii forFragment(boolean topEdge, boolean bottomEdge) {
        return new Radii(topEdge ? topLeft : Corner.SQUARE, topEdge ? topRight : Corner.SQUARE,
                bottomEdge ? bottomRight : Corner.SQUARE, bottomEdge ? bottomLeft : Corner.SQUARE);
    }
}
