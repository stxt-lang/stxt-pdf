package dev.stxt.pdf.model;

/** Rectángulo en puntos. */
public record Rect(double x, double y, double width, double height) {

    public Rect expand(Insets i) {
        return new Rect(x - i.left(), y - i.top(), width + i.horizontal(), height + i.vertical());
    }

    public Rect shrink(Insets i) {
        return new Rect(x + i.left(), y + i.top(), width - i.horizontal(), height - i.vertical());
    }

    public Rect translate(double dx, double dy) {
        return new Rect(x + dx, y + dy, width, height);
    }

    public double right() {
        return x + width;
    }

    public double bottom() {
        return y + height;
    }
}
