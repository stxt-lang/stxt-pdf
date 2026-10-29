package dev.stxt.boxes.tree;

import dev.stxt.boxes.model.Corners;
import dev.stxt.boxes.model.Paint;
import dev.stxt.boxes.model.Sides;
import dev.stxt.boxes.model.Value;

/**
 * El grupo {@code Box} (ESPECIFICACION.md §4.3): el modelo de caja (tamaños, márgenes, relleno y borde) y su aspecto
 * (fondo, color del borde y esquinas redondeadas), que solo cambia el pintado. No se hereda.
 */
public final class BoxStyle extends StyleGroup {

    private Value width = Value.AUTO;
    private Value height = Value.AUTO;
    private Value minWidth = Value.AUTO;
    private Value minHeight = Value.AUTO;
    /** {@code null} = sin máximo. */
    private Value maxWidth;
    /** {@code null} = sin máximo. */
    private Value maxHeight;
    /** Ancho / alto; {@code null} = sin proporción. */
    private Double aspectRatio;
    private Sides<Value> margin = Sides.all(Value.pt(0));
    private Sides<Value> padding = Sides.all(Value.pt(0));
    private Sides<Value> border = Sides.all(Value.pt(0));
    /** {@code null} = transparente. */
    private Paint background;
    /** Un color por lado; {@code null} = igual que {@link #color}. */
    private Sides<String> borderColor;
    /** Esquinas redondeadas, de la caja de borde; solo pintado. */
    private Corners<Value> radius = Corners.all(Value.pt(0));

    BoxStyle() {
    }

    public Value width() {
        return width;
    }

    void setWidth(Value v) {
        writable();
        width = v;
    }

    public Value height() {
        return height;
    }

    void setHeight(Value v) {
        writable();
        height = v;
    }

    public Value minWidth() {
        return minWidth;
    }

    void setMinWidth(Value v) {
        writable();
        minWidth = v;
    }

    public Value minHeight() {
        return minHeight;
    }

    void setMinHeight(Value v) {
        writable();
        minHeight = v;
    }

    public Value maxWidth() {
        return maxWidth;
    }

    void setMaxWidth(Value v) {
        writable();
        maxWidth = v;
    }

    public Value maxHeight() {
        return maxHeight;
    }

    void setMaxHeight(Value v) {
        writable();
        maxHeight = v;
    }

    public Double aspectRatio() {
        return aspectRatio;
    }

    void setAspectRatio(Double v) {
        writable();
        aspectRatio = v;
    }

    public Sides<Value> margin() {
        return margin;
    }

    void setMargin(Sides<Value> v) {
        writable();
        margin = v;
    }

    public Sides<Value> padding() {
        return padding;
    }

    void setPadding(Sides<Value> v) {
        writable();
        padding = v;
    }

    public Sides<Value> border() {
        return border;
    }

    void setBorder(Sides<Value> v) {
        writable();
        border = v;
    }

    public Paint background() {
        return background;
    }

    void setBackground(Paint v) {
        writable();
        background = v;
    }

    public Sides<String> borderColor() {
        return borderColor;
    }

    void setBorderColor(Sides<String> v) {
        writable();
        borderColor = v;
    }

    public Corners<Value> radius() {
        return radius;
    }

    void setRadius(Corners<Value> v) {
        writable();
        radius = v;
    }
}
