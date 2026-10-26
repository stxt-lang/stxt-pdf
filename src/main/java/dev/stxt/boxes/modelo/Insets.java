package dev.stxt.boxes.modelo;

/** Grosores ya resueltos, en puntos, de margen, borde o relleno. */
public record Insets(double arriba, double derecha, double abajo, double izquierda) {

    public static final Insets CERO = new Insets(0, 0, 0, 0);

    public static Insets todos(double v) {
        return new Insets(v, v, v, v);
    }

    public double horizontal() {
        return izquierda + derecha;
    }

    public double vertical() {
        return arriba + abajo;
    }

    /**
     * Resuelve cuatro valores contra el <strong>ancho</strong> del containing block. En CSS, los
     * porcentajes de margen y relleno, también los verticales, se calculan sobre el ancho
     * (módulo 1). Un {@code auto} vale 0 aquí; los márgenes {@code auto} los reparte el layout.
     */
    public static Insets resolver(Lados<Valor> lados, double anchoContainingBlock) {
        return new Insets(
                lados.arriba().resolver(anchoContainingBlock).orElse(0),
                lados.derecha().resolver(anchoContainingBlock).orElse(0),
                lados.abajo().resolver(anchoContainingBlock).orElse(0),
                lados.izquierda().resolver(anchoContainingBlock).orElse(0));
    }
}
