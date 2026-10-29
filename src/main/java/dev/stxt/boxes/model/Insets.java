package dev.stxt.boxes.model;

/** Grosores ya resueltos, en puntos, de margen, borde o relleno. */
public record Insets(double top, double right, double bottom, double left) {

    public static final Insets ZERO = new Insets(0, 0, 0, 0);

    public static Insets all(double v) {
        return new Insets(v, v, v, v);
    }

    public double horizontal() {
        return left + right;
    }

    public double vertical() {
        return top + bottom;
    }

    /**
     * Resuelve cuatro valores contra el <strong>ancho</strong> del containing block. En CSS, los
     * porcentajes de margen y relleno, también los verticales, se calculan sobre el ancho
     * (referencia/comun/valores.md). Un {@code auto} vale 0 aquí; los márgenes {@code auto} los reparte el layout.
     */
    public static Insets resolve(Sides<Value> sides, double containingBlockWidth) {
        return new Insets(
                sides.top().resolve(containingBlockWidth).orElse(0),
                sides.right().resolve(containingBlockWidth).orElse(0),
                sides.bottom().resolve(containingBlockWidth).orElse(0),
                sides.left().resolve(containingBlockWidth).orElse(0));
    }
}
