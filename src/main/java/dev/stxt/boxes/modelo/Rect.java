package dev.stxt.boxes.modelo;

/** Rectángulo en puntos. Con altura infinita representa el medio continuo. */
public record Rect(double x, double y, double ancho, double alto) {

    public Rect expandir(Insets i) {
        return new Rect(x - i.izquierda(), y - i.arriba(), ancho + i.horizontal(), alto + i.vertical());
    }

    public Rect contraer(Insets i) {
        return new Rect(x + i.izquierda(), y + i.arriba(), ancho - i.horizontal(), alto - i.vertical());
    }

    public Rect desplazar(double dx, double dy) {
        return new Rect(x + dx, y + dy, ancho, alto);
    }

    public double derecha() {
        return x + ancho;
    }

    public double abajo() {
        return y + alto;
    }
}
