package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Corners;
import dev.stxt.pdf.model.Overflow;
import dev.stxt.pdf.model.Paint;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.Value;

/**
 * The {@code Box} group (reference/common/box.md): the box model (sizes, margins, padding and border), its
 * appearance (background, border color and rounded corners), which only changes the painting, and what is done with
 * what does not fit in it ({@code Overflow}, reference/common/overflow.md). It is not inherited.
 */
public final class BoxStyle extends StyleGroup {

    private Value width = Value.AUTO;
    private Value height = Value.AUTO;
    private Value minWidth = Value.AUTO;
    private Value minHeight = Value.AUTO;
    /** {@code null} = no maximum. */
    private Value maxWidth;
    /** {@code null} = no maximum. */
    private Value maxHeight;
    /** Width / height; {@code null} = no ratio. */
    private Double aspectRatio;
    private Sides<Value> margin = Sides.all(Value.pt(0));
    private Sides<Value> padding = Sides.all(Value.pt(0));
    private Sides<Value> border = Sides.all(Value.pt(0));
    /** {@code null} = transparent. */
    private Paint background;
    /** One color per side; {@code null} = the {@code Color} of the text ({@link TypographyStyle#color()}). */
    private Sides<String> borderColor;
    /** Rounded corners, of the border box; painting only. */
    private Corners<Value> cornerRadius = Corners.all(Value.pt(0));
    /** What is done with what does not fit in the box (reference/common/overflow.md). */
    private Overflow overflow = Overflow.ERROR;

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

    public Corners<Value> cornerRadius() {
        return cornerRadius;
    }

    void setCornerRadius(Corners<Value> v) {
        writable();
        cornerRadius = v;
    }

    public Overflow overflow() {
        return overflow;
    }

    void setOverflow(Overflow v) {
        writable();
        overflow = v;
    }

    /** Whether the text of the box is reduced until it fits ({@code Overflow: shrink}). */
    public boolean shrinks() {
        return overflow == Overflow.SHRINK;
    }

    /** Whether what sticks out of the box is painted clipped and not checked ({@code Overflow: clip}). */
    public boolean clips() {
        return overflow == Overflow.CLIP;
    }
}
