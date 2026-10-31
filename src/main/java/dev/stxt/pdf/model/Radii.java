package dev.stxt.pdf.model;

/**
 * Las esquinas redondeadas de una caja ya resueltas, en puntos (reference/common/box.md): un cuarto
 * de elipse por esquina, con su radio horizontal y su radio vertical. Es solo pintado.
 */
public record Radii(Corner topLeft, Corner topRight, Corner bottomRight, Corner bottomLeft) {

    /** El cuarto de elipse de una esquina; los dos radios valen 0 si no es redonda. */
    public record Corner(double rx, double ry) {

        public static final Corner SQUARE = new Corner(0, 0);

        /** Una esquina con algún radio nulo es cuadrada. */
        public static Corner of(double rx, double ry) {
            return rx > 0 && ry > 0 ? new Corner(rx, ry) : SQUARE;
        }

        public boolean isSquare() {
            return rx <= 0 || ry <= 0;
        }
    }

    public static final Radii NONE = new Radii(Corner.SQUARE, Corner.SQUARE, Corner.SQUARE, Corner.SQUARE);

    /**
     * Resuelve {@code Corner radius} contra la caja de borde: un porcentaje horizontal es del ancho y uno
     * vertical, del alto, así que {@code 50%} en una caja que no es cuadrada es una elipse. Como en
     * CSS, si dos radios seguidos de un lado suman más que el lado, todos se reducen por el mismo
     * factor hasta que caben.
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
     * Los radios de la caja que queda al quitar {@code i} por dentro (el borde para pasar a la
     * caja de relleno, el borde y el relleno para la de contenido): cada radio pierde el grosor del
     * lado que toca, y una esquina que se queda sin radio es cuadrada.
     */
    public Radii inset(Insets i) {
        return new Radii(
                Corner.of(topLeft.rx - i.left(), topLeft.ry - i.top()),
                Corner.of(topRight.rx - i.right(), topRight.ry - i.top()),
                Corner.of(bottomRight.rx - i.right(), bottomRight.ry - i.bottom()),
                Corner.of(bottomLeft.rx - i.left(), bottomLeft.ry - i.bottom()));
    }

    /**
     * Las esquinas de un fragmento de una caja partida (reference/common/breaks.md): con {@code Edges: open},
     * solo el primer fragmento tiene las de arriba y solo el último las de abajo.
     */
    public Radii forFragment(boolean topEdge, boolean bottomEdge) {
        return new Radii(topEdge ? topLeft : Corner.SQUARE, topEdge ? topRight : Corner.SQUARE,
                bottomEdge ? bottomRight : Corner.SQUARE, bottomEdge ? bottomLeft : Corner.SQUARE);
    }
}
